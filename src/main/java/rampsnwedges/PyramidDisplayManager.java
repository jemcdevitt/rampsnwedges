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

public class PyramidDisplayManager {
	private static final String DISPLAY_TYPE = "rnw_pyramid_display";

	private final CustomItemFactory itemFactory;

	public PyramidDisplayManager(CustomItemFactory itemFactory) {
		this.itemFactory = itemFactory;
	}

	public ItemDisplay create(Block block, CustomBlockDefinition definition) {
		if(!definition.shape().isPyramid()) {
			throw new IllegalArgumentException("Pyramid display requires a pyramid definition");
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
			});
	}

	public ItemDisplay getPyramidAt(Block block) {
		Location center = block.getLocation().add(0.5, 0.5, 0.5);

		for(Entity entity : block.getWorld().getNearbyEntities(center, 0.49, 0.49, 0.49)) {
			if(!(entity instanceof ItemDisplay display)) {
				continue;
			}

			PersistentDataContainer pdc = display.getPersistentDataContainer();
			String type = pdc.get(Constants.RNW_TYPE_KEY, PersistentDataType.STRING);

			if(DISPLAY_TYPE.equals(type)) {
				return display;
			}
		}

		return null;
	}

	public void remove(Block block) {
		ItemDisplay display = getPyramidAt(block);
		if(display != null) {
			display.remove();
		}
	}
}
