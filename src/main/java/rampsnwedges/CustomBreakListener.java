package rampsnwedges;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Supplies player-controlled breaking for every Ramps n Wedges block.
 *
 * The physical carriers are implementation details and must not determine
 * hardness, preferred tool, particles, drops, or tool durability. Mining is
 * therefore based on the logical visual material stored in the ItemDisplay.
 */
public class CustomBreakListener implements Listener {
	private static final double MAX_BREAK_DISTANCE_SQUARED = 36.0;

	private final RampsNWedgesPlugin plugin;
	private final BlockCatalog catalog;
	private final CustomItemFactory itemFactory;
	private final Configuration config;
	private final RampDisplayManager rampDisplays;
	private final WedgeDisplayManager wedgeDisplays;
	private final HipDisplayManager hipDisplays;
	private final PyramidDisplayManager pyramidDisplays;
	private final Map<UUID, BreakSession> sessions = new HashMap<>();

	public CustomBreakListener(RampsNWedgesPlugin plugin, BlockCatalog catalog,
														 CustomItemFactory itemFactory, Configuration config,
														 RampDisplayManager rampDisplays, WedgeDisplayManager wedgeDisplays,
														 HipDisplayManager hipDisplays, PyramidDisplayManager pyramidDisplays) {
		this.plugin = plugin;
		this.catalog = catalog;
		this.itemFactory = itemFactory;
		this.config = config;
		this.rampDisplays = rampDisplays;
		this.wedgeDisplays = wedgeDisplays;
		this.hipDisplays = hipDisplays;
		this.pyramidDisplays = pyramidDisplays;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if(event.getAction() != Action.LEFT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		Player player = event.getPlayer();
		if(player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}

		PlacedCustomBlock placed = customBlockAt(event.getClickedBlock());
		if(placed == null) {
			return;
		}

		/* Prevent the carrier block from participating in vanilla mining. */
		event.setCancelled(true);

		if(player.getGameMode() == GameMode.CREATIVE) {
			breakBlock(placed, false);
			return;
		}

		startBreaking(player, placed);
	}

	/**
	 * Safety net: a custom block should never be broken according to its carrier
	 * material if a native BlockBreakEvent gets through.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		PlacedCustomBlock placed = customBlockAt(event.getBlock());
		if(placed != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockDamageAbort(BlockDamageAbortEvent event) {
		BreakSession session = sessions.get(event.getPlayer().getUniqueId());
		if(session != null && sameBlock(session.placed.block(), event.getBlock())) {
			cancelSession(event.getPlayer(), session);
		}
	}

	private void startBreaking(Player player, PlacedCustomBlock placed) {
		BreakSession current = sessions.get(player.getUniqueId());
		if(current != null) {
			if(sameBlock(current.placed.block(), placed.block())) {
				return;
			}
			cancelSession(player, current);
		}

		float hardness = placed.definition().material().material().getHardness();
		if(hardness < 0.0F) {
			return;
		}

		BreakSession session = new BreakSession(placed);
		sessions.put(player.getUniqueId(), session);
		session.task = Bukkit.getScheduler().runTaskTimer(plugin,
			() -> tickBreaking(player, session), 0L, 1L);
	}

	private void tickBreaking(Player player, BreakSession session) {
		if(!player.isOnline() || player.getGameMode() != GameMode.SURVIVAL) {
			cancelSession(player, session);
			return;
		}

		PlacedCustomBlock current = customBlockAt(session.placed.block());
		if(current == null || !current.definition().id().equals(session.placed.definition().id())) {
			cancelSession(player, session);
			return;
		}

		Block block = current.block();
		if(player.getWorld() != block.getWorld()
				|| player.getEyeLocation().distanceSquared(block.getLocation().add(0.5, 0.5, 0.5))
					> MAX_BREAK_DISTANCE_SQUARED) {
			cancelSession(player, session);
			return;
		}

		Block target = player.getTargetBlockExact(6);
		if(target == null || !sameBlock(target, block)) {
			cancelSession(player, session);
			return;
		}

		MiningInfo mining = miningInfo(player, current.definition().material().material());
		if(mining.progressPerTick() <= 0.0F) {
			return;
		}

		session.progress = Math.min(1.0F, session.progress + mining.progressPerTick());
		player.sendBlockDamage(block.getLocation(), session.progress, player);

		session.ticks++;
		if(session.ticks % 4 == 0) {
			spawnMiningParticles(block, current.definition().material().material());
		}

		if(session.progress >= 1.0F) {
			finishSession(player, session);
			breakBlock(current, mining.correctForDrops());
			damageTool(player);
		}
	}

	private MiningInfo miningInfo(Player player, Material material) {
		float hardness = material.getHardness();
		if(hardness < 0.0F) {
			return new MiningInfo(0.0F, false);
		}
		if(hardness == 0.0F) {
			return new MiningInfo(1.0F, true);
		}

		ItemStack tool = player.getInventory().getItemInMainHand();
		BlockData data = Bukkit.createBlockData(material);

		/*
		 * Paper exposes the vanilla destroy-speed calculation directly from
		 * BlockData. This accounts for the held item's ToolComponent rules and,
		 * with considerEnchants=true, enchantments such as Efficiency.
		 */
		float speed = data.getDestroySpeed(tool, true);

		AttributeInstance breakSpeed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
		if(breakSpeed != null) {
			speed *= (float)breakSpeed.getValue();
		}

		boolean correctForDrops = !data.requiresCorrectToolForDrops() || data.isPreferredTool(tool);
		float divisor = correctForDrops ? 30.0F : 100.0F;
		return new MiningInfo(speed / hardness / divisor, correctForDrops);
	}

	private void damageTool(Player player) {
		ItemStack tool = player.getInventory().getItemInMainHand();
		if(tool.isEmpty() || !tool.hasItemMeta()) {
			return;
		}

		ToolComponent component = tool.getItemMeta().getTool();
		int damage = component.getDamagePerBlock();
		if(damage > 0) {
			player.damageItemStack(EquipmentSlot.HAND, damage);
		}
	}

	private void breakBlock(PlacedCustomBlock placed, boolean dropItem) {
		Block block = placed.block();
		CustomBlockDefinition definition = placed.definition();

		spawnBreakParticles(block, definition.material().material());
		placed.display().remove();
		block.setType(Material.AIR, false);

		if(dropItem) {
			ItemStack item = itemFactory.create(definition);
			block.getWorld().dropItemNaturally(block.getLocation(), item);
		}
	}

	private PlacedCustomBlock customBlockAt(Block block) {
		if(block == null) {
			return null;
		}

		ItemDisplay display = null;
		if(block.getType().equals(config.rampCarrier())) {
			display = rampDisplays.getRampAt(block);
		} else if(block.getType().equals(config.wedgeCarrier())) {
			display = wedgeDisplays.getWedgeAt(block);
			if(display == null) {
				display = hipDisplays.getHipAt(block);
			}
			if( display == null ) {
				display = pyramidDisplays.getPyramidAt(block);
			}
		} else {
			return null;
		}

		if(display == null) {
			return null;
		}

		String definitionId = display.getPersistentDataContainer().get(
			Constants.RNW_ID_KEY, PersistentDataType.STRING);
		if(definitionId == null) {
			LOG(0, "ItemDisplay does not have a RampsNWedges id");
			return null;
		}

		Optional<CustomBlockDefinition> found = catalog.find(definitionId);
		if(found.isEmpty()) {
			LOG(0, "Custom block definition %s is not in the catalog", definitionId);
			return null;
		}

		CustomBlockDefinition definition = found.get();
		if(definition.shape().isRamp() && !block.getType().equals(config.rampCarrier())) {
			return null;
		}
		if(definition.shape().usesBarrierCarrier() && !block.getType().equals(config.wedgeCarrier())) {
			return null;
		}

		return new PlacedCustomBlock(block, display, definition);
	}

	private void finishSession(Player player, BreakSession session) {
		sessions.remove(player.getUniqueId());
		if(session.task != null) {
			session.task.cancel();
		}
		player.sendBlockDamage(session.placed.block().getLocation(), 0.0F, player);
	}

	private void cancelSession(Player player, BreakSession session) {
		if(sessions.get(player.getUniqueId()) != session) {
			return;
		}
		finishSession(player, session);
	}

	private boolean sameBlock(Block first, Block second) {
		return first.getWorld().equals(second.getWorld())
			&& first.getX() == second.getX()
			&& first.getY() == second.getY()
			&& first.getZ() == second.getZ();
	}

	private void spawnMiningParticles(Block block, Material material) {
		BlockData data = Bukkit.createBlockData(material);
		Location location = block.getLocation().add(0.5, 0.5, 0.5);

		block.getWorld().spawnParticle(Particle.BLOCK, location, 4,
			0.3, 0.3, 0.3, 0.05, data);
	}

	private void spawnBreakParticles(Block block, Material material) {
		BlockData data = Bukkit.createBlockData(material);
		Location location = block.getLocation().add(0.5, 0.5, 0.5);

		block.getWorld().spawnParticle(Particle.BLOCK, location, 20,
			0.4, 0.4, 0.4, 0.12, data);
	}

	private static class BreakSession {
		private final PlacedCustomBlock placed;
		private float progress;
		private BukkitTask task;
		private int ticks;

		private BreakSession(PlacedCustomBlock placed) {
			this.placed = placed;
		}
	}

	private record PlacedCustomBlock(Block block, ItemDisplay display,
									 CustomBlockDefinition definition) {
	}

	private record MiningInfo(float progressPerTick, boolean correctForDrops) {
	}
}
