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
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.block.PlacedBlockStore;
import rampsnwedges.item.CustomItemFactory;

public class BlockBreakListener implements Listener {
	private final BlockCatalog catalog;
	private final CustomItemFactory itemFactory;
	private final PlacedBlockStore placedBlocks;
	private final RampDisplayManager rampDisplays;
	private final WedgeDisplayManager wedgeDisplays;

	public BlockBreakListener(BlockCatalog catalog, CustomItemFactory itemFactory,
						  PlacedBlockStore placedBlocks, RampDisplayManager rampDisplays,
						  WedgeDisplayManager wedgeDisplays) {
		this.catalog = catalog;
		this.itemFactory = itemFactory;
		this.placedBlocks = placedBlocks;
		this.rampDisplays = rampDisplays;
		this.wedgeDisplays = wedgeDisplays;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Optional<String> storedId = placedBlocks.get(block);
		if(storedId.isEmpty()) {
			return;
		}

		Optional<CustomBlockDefinition> found = catalog.find(storedId.get());
		if(found.isEmpty()) {
			return;
		}

		CustomBlockDefinition definition = found.get();
		event.setDropItems(false);

		removeDisplay(block, definition);
		placedBlocks.remove(block);

		if(event.getPlayer().getGameMode() != GameMode.CREATIVE) {
			ItemStack item = itemFactory.create(definition);
			block.getWorld().dropItemNaturally(block.getLocation(), item);
		}
	}

	private void removeDisplay(Block block, CustomBlockDefinition definition) {
		if(definition.shape().isRamp()) {
			rampDisplays.remove(block);
		} else {
			wedgeDisplays.remove(block);
		}
	}
}
