package rampsnwedges;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Creates the visual projection for a placed wedge.
 *
 * The real block is an invisible barrier supplying full-block collision. The
 * four wedge orientations use the four proven V10 model templates, so the
 * ItemDisplay itself requires no rotation for this first implementation.
 */
public class WedgeDisplayManager {
	private static final String DISPLAY_TYPE = "rnw_wedge_display";

	private final CustomItemFactory itemFactory;

	public WedgeDisplayManager(CustomItemFactory itemFactory) {
		this.itemFactory = itemFactory;
	}

	public ItemDisplay create(Block block, CustomBlockDefinition definition) {
		if(!definition.shape().isWedge()) {
			throw new IllegalArgumentException("Wedge display requires a wedge definition");
		}

		ItemStack item = itemFactory.create(definition);
		Location location = block.getLocation().add(0.5, 0.5, 0.5);

		return block.getWorld().spawn(location, ItemDisplay.class, display -> {
			display.setItemStack(item);
			display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
			display.setPersistent(true);
			display.setInvulnerable(true);
			display.setGravity(false);

			PersistentDataContainer pdc = display.getPersistentDataContainer();
			pdc.set(Constants.RNW_TYPE_KEY, PersistentDataType.STRING, DISPLAY_TYPE);
			pdc.set(Constants.RNW_ID_KEY, PersistentDataType.STRING, definition.id());
			pdc.set(Constants.RNW_WEDGE_ID_KEY, PersistentDataType.STRING, genWedgeId(block));
		});
	}

	public ItemDisplay getWedgeAt(Block block) {
		Location center = block.getLocation().add(0.5, 0.5, 0.5);
		for(Entity entity : block.getWorld().getNearbyEntities(center, 0.49, 0.49, 0.49)) {
			if(!(entity instanceof ItemDisplay display)) {
				continue;
			}

			PersistentDataContainer pdc = display.getPersistentDataContainer();
			String type = pdc.get(Constants.RNW_TYPE_KEY, PersistentDataType.STRING);
			if(!DISPLAY_TYPE.equals(type)) {
				continue;
			}
			return display;
		}
		return null;
	}

	public void remove(Block block) {
		Location center = block.getLocation().add(0.5, 0.5, 0.5);

		for(Entity entity : block.getWorld().getNearbyEntities(center, 0.49, 0.49, 0.49)) {
			if(!(entity instanceof ItemDisplay display)) {
				continue;
			}

			PersistentDataContainer pdc = display.getPersistentDataContainer();
			String type = pdc.get(Constants.RNW_TYPE_KEY, PersistentDataType.STRING);
			if(!DISPLAY_TYPE.equals(type)) {
				LOG(0,"Not a wedge display");
				continue;
			}

			//although the id is based on the original block location, it is possible for the
			//new block location to be different as can happen if the block is part of a SimpleShips ship.
			//so we're only checking that an ID exists.  The getNearbyEntities should have basically limited us
			//to the block we're on so chances of picking up another item display are minimal.
			//keeping the id still based on block location simply to keep it unique.
			String wedgeId = pdc.get(Constants.RNW_WEDGE_ID_KEY, PersistentDataType.STRING);
			LOG(0,"Found wedge id %s", wedgeId);
			if( wedgeId == null )
				continue;

			LOG(0,"Removing wedge id %s", wedgeId);
			display.remove();
		}
	}

	private String genWedgeId(Block block) {
		return "wedge_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
	}
}
