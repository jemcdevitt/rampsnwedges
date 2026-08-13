package rampsnwedges;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.Optional;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

public class BlockBreakListener implements Listener {
	private final BlockCatalog catalog;
	private final CustomItemFactory itemFactory;
	private final Configuration config;
	private final RampDisplayManager rampDisplays;
	private final WedgeDisplayManager wedgeDisplays;

	public BlockBreakListener(BlockCatalog catalog, CustomItemFactory itemFactory, Configuration config,
														RampDisplayManager rampDisplays, WedgeDisplayManager wedgeDisplays) {
		this.catalog = catalog;
		this.itemFactory = itemFactory;
		this.config = config;
		this.rampDisplays = rampDisplays;
		this.wedgeDisplays = wedgeDisplays;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();

		Material blockType = block.getType();
		ItemDisplay itemDisplay = null;
		if( blockType.equals(config.rampCarrier())) {
			itemDisplay = rampDisplays.getRampAt(block);
		} else if( blockType.equals(config.wedgeCarrier())) {
			itemDisplay = wedgeDisplays.getWedgeAt(block);
		} else {
			return;
		}

		if( itemDisplay == null ) {
			LOG(1,"No item display found at carrier location %d,%d,%d", block.getX(), block.getY(), block.getZ());
			return;
		}

		String definitionId = itemDisplay.getPersistentDataContainer().get(Constants.RNW_ID_KEY, PersistentDataType.STRING);
		if( definitionId == null ) {
			LOG(0,"ItemDisplay does not have a RampsNWedges id");
			return;
		}
		
		Optional<CustomBlockDefinition> found = catalog.find(definitionId);
		if(found.isEmpty()) {
			LOG(0,"Not in catalog");
			return;
		}

		CustomBlockDefinition definition = found.get();
		event.setDropItems(false);

		itemDisplay.remove();

		if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			ItemStack item = itemFactory.create(definition);
			block.getWorld().dropItemNaturally(block.getLocation(), item);
		}
	}
}
