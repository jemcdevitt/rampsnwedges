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
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;
//import rampsnwedges.block.PlacedBlockStore;
import rampsnwedges.item.CustomItemFactory;

public class BlockPlaceListener implements Listener {
	private final Configuration config;
	private final BlockCatalog catalog;
	private final CustomItemFactory itemFactory;
//	private final PlacedBlockStore placedBlocks;
	private final RampDisplayManager rampDisplays;
	private final WedgeDisplayManager wedgeDisplays;

	public BlockPlaceListener(Configuration config, BlockCatalog catalog, CustomItemFactory itemFactory,
														RampDisplayManager rampDisplays,  WedgeDisplayManager wedgeDisplays) {
		this.config = config;
		this.catalog = catalog;
		this.itemFactory = itemFactory;
//		this.placedBlocks = placedBlocks;
		this.rampDisplays = rampDisplays;
		this.wedgeDisplays = wedgeDisplays;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		String blockId = itemFactory.getBlockId(event.getItemInHand());
		if(blockId == null) {
			return;
		}

		Optional<CustomBlockDefinition> found = catalog.find(blockId);
		if(found.isEmpty()) {
			return;
		}

		CustomBlockDefinition definition = found.get();
		Block block = event.getBlockPlaced();

		if(definition.shape().isRamp()) {
			placeRamp(block, definition);
		} else {
			placeWedge(block, definition);
		}
	}

	private void placeRamp(Block block, CustomBlockDefinition definition) {
		/*
		 * Ramp ItemStacks use the configured stair carrier, so Minecraft has
		 * already selected facing, half and waterlogging before this event.
		 */
		if(!(block.getBlockData() instanceof Stairs stairs)) {
			throw new IllegalStateException("Ramp item did not place stair BlockData at "
										+ block.getLocation());
		}

		/*
		 * Keep ramps visually independent when neighboring stair blocks would
		 * otherwise mutate the hidden carrier to an inner/outer stair shape.
		 */
		stairs.setShape(Stairs.Shape.STRAIGHT);
		block.setBlockData(stairs, false);

		rampDisplays.create(block, definition);
	}

	private void placeWedge(Block block, CustomBlockDefinition definition) {
		/*
		 * Wedge items are ordinary stair items in the inventory so holding one
		 * never reveals nearby barrier carriers. Once Minecraft has accepted the
		 * placement, replace that temporary stair with the invisible wedge carrier.
		 */
		block.setBlockData(Bukkit.createBlockData(config.wedgeCarrier()), false);
		wedgeDisplays.create(block, definition);
	}
}
