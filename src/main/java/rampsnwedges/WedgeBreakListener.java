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
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Supplies player-controlled breaking for barrier-backed wedges.
 *
 * Barrier blocks cannot be mined normally in Survival. This listener therefore
 * uses the logical wedge material's hardness together with the held item's
 * modern ToolComponent mining rules to emulate vanilla-style break time. The
 * player receives the normal crack overlay while progress is accumulated.
 */
public class WedgeBreakListener implements Listener {
	private static final double MAX_BREAK_DISTANCE_SQUARED = 36.0;

	private final RampsNWedgesPlugin plugin;
	private final BlockCatalog catalog;
	private final CustomItemFactory itemFactory;
	private final WedgeDisplayManager wedgeDisplays;
	private final Configuration config;
	private final Map<UUID, BreakSession> sessions = new HashMap<>();

	public WedgeBreakListener(RampsNWedgesPlugin plugin, BlockCatalog catalog, CustomItemFactory itemFactory, Configuration config, WedgeDisplayManager wedgeDisplays) {
		this.plugin = plugin;
		this.catalog = catalog;
		this.itemFactory = itemFactory;
		this.config = config;
		this.wedgeDisplays = wedgeDisplays;
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

		Block block = event.getClickedBlock();
		CustomBlockDefinition definition = wedgeDefinition(block);
		if(definition == null) {
			return;
		}

		if(player.getGameMode() == GameMode.CREATIVE) {
			event.setCancelled(true);
			breakWedge(player, block, definition, false);
			return;
		}

		startBreaking(player, block, definition);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockDamageAbort(BlockDamageAbortEvent event) {
		BreakSession session = sessions.get(event.getPlayer().getUniqueId());
		if(session != null && sameBlock(session.block, event.getBlock())) {
			cancelSession(event.getPlayer(), session);
		}
	}

	private void startBreaking(Player player, Block block, CustomBlockDefinition definition) {
		BreakSession current = sessions.get(player.getUniqueId());
		if(current != null) {
			if(sameBlock(current.block, block)) {
				return;
			}
			cancelSession(player, current);
		}

		float hardness = definition.material().material().getHardness();
		if(hardness < 0.0F) {
			return;
		}

		BreakSession session = new BreakSession(block, definition);
		sessions.put(player.getUniqueId(), session);
		session.task = Bukkit.getScheduler().runTaskTimer(plugin,
			() -> tickBreaking(player, session), 0L, 1L);
	}

	private void tickBreaking(Player player, BreakSession session) {
		if(!player.isOnline() || player.getGameMode() != GameMode.SURVIVAL) {
			cancelSession(player, session);
			return;
		}

		CustomBlockDefinition currentDefinition = wedgeDefinition(session.block);
		if(currentDefinition == null || !currentDefinition.id().equals(session.definition.id())) {
			cancelSession(player, session);
			return;
		}

		if(player.getWorld() != session.block.getWorld()
				|| player.getEyeLocation().distanceSquared(session.block.getLocation().add(0.5, 0.5, 0.5))
					> MAX_BREAK_DISTANCE_SQUARED) {
			cancelSession(player, session);
			return;
		}

		Block target = player.getTargetBlockExact(6);
		if(target == null || !sameBlock(target, session.block)) {
			cancelSession(player, session);
			return;
		}

		MiningInfo mining = miningInfo(player, session.definition.material().material());
		if(mining.progressPerTick <= 0.0F) {
			return;
		}

		session.progress = Math.min(1.0F, session.progress + mining.progressPerTick);
		player.sendBlockDamage(session.block.getLocation(), session.progress, player);

		session.ticks++;
		if(session.ticks % 4 == 0) {
			spawnMiningParticles(session.block,	session.definition.material().material());
		}

		if(session.progress >= 1.0F) {
			finishSession(player, session);
			breakWedge(player, session.block, session.definition, mining.correctForDrops);
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
		ToolResult toolResult = toolResult(tool, material);
		float speed = toolResult.speed;

		int efficiency = tool.getEnchantmentLevel(Enchantment.EFFICIENCY);
		if(speed > 1.0F && efficiency > 0) {
			speed += efficiency * efficiency + 1;
		}

		AttributeInstance breakSpeed = player.getAttribute(Attribute.BLOCK_BREAK_SPEED);
		if(breakSpeed != null) {
			speed *= (float)breakSpeed.getValue();
		}

		/*
		 * Vanilla uses the faster hardness divisor when the block can be
		 * harvested with the current tool, and the slower divisor otherwise.
		 * MINEABLE_PICKAXE is also a useful indication that the block requires
		 * an appropriate mining tool to yield itself (stone, obsidian, etc.).
		 */
		boolean requiresTool = Tag.MINEABLE_PICKAXE.isTagged(material);
		boolean correctForDrops = !requiresTool || toolResult.correctForDrops;
		float divisor = correctForDrops ? 30.0F : 100.0F;
		float progress = speed / hardness / divisor;

		return new MiningInfo(progress, correctForDrops);
	}

	private ToolResult toolResult(ItemStack tool, Material material) {
		float speed = 1.0F;
		Boolean correctForDrops = null;

		if(!tool.isEmpty() && tool.hasItemMeta()) {
			ItemMeta meta = tool.getItemMeta();
			ToolComponent component = meta.getTool();
			speed = component.getDefaultMiningSpeed();

			for(ToolComponent.ToolRule rule : component.getRules()) {
				if(!rule.getBlocks().contains(material)) {
					continue;
				}
				if(rule.getSpeed() != null) {
					speed = rule.getSpeed();
				}
				if(rule.isCorrectForDrops() != null) {
					correctForDrops = rule.isCorrectForDrops();
				}
				break;
			}
		}

		if(correctForDrops == null) {
			BlockData data = Bukkit.createBlockData(material);
			correctForDrops = data.isPreferredTool(tool);
		}

		return new ToolResult(speed, correctForDrops);
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

	private void breakWedge(Player player, Block block, CustomBlockDefinition definition,	boolean dropItem) {

		spawnBreakParticles(block, definition.material().material());
		wedgeDisplays.remove(block);
		block.setType(Material.AIR, false);

		if(dropItem) {
			ItemStack item = itemFactory.create(definition);
			block.getWorld().dropItemNaturally(block.getLocation(), item);
		}
	}

	private CustomBlockDefinition wedgeDefinition(Block block) {
		if(block == null) {
			return null;
		}
		Material blockType = block.getType();
		if(!blockType.equals(config.wedgeCarrier()))
			return null;

		ItemDisplay display = wedgeDisplays.getWedgeAt(block);
		if( display == null ) {
			return null;
		}

		String storeId = display.getPersistentDataContainer().get(Constants.RNW_ID_KEY, PersistentDataType.STRING);
		if( storeId == null ) {
			LOG(0,"Wedge ItemDisplay does not have a RampsNWedges id");
			return null;
		}

		Optional<CustomBlockDefinition> found = catalog.find(storeId);
		if(found.isEmpty() || !found.get().shape().isWedge()) {
			return null;
		}
		return found.get();
	}

	private void finishSession(Player player, BreakSession session) {
		sessions.remove(player.getUniqueId());
		if(session.task != null) {
			session.task.cancel();
		}
		player.sendBlockDamage(session.block.getLocation(), 0.0F, player);
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
																	 0.3, 0.3, 0.3,
																	 0.05,
																	 data);
	}	

	private void spawnBreakParticles(Block block, Material material) {
		BlockData data = Bukkit.createBlockData(material);
		Location location = block.getLocation().add(0.5, 0.5, 0.5);
		
		block.getWorld().spawnParticle(Particle.BLOCK,location, 20,
																	 0.4, 0.4, 0.4,
																	 0.12,
																	 data);
	}
	
	private static class BreakSession {
		private final Block block;
		private final CustomBlockDefinition definition;
		private float progress;
		private BukkitTask task;
		private int ticks;

		private BreakSession(Block block, CustomBlockDefinition definition) {
			this.block = block;
			this.definition = definition;
		}
	}
	

	private record MiningInfo(float progressPerTick, boolean correctForDrops) {
	}

	private record ToolResult(float speed, boolean correctForDrops) {
	}
}
