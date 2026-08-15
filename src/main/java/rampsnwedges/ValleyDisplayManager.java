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
import org.joml.Matrix4f;
import rampsnwedges.block.BlockShape;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Creates the visual projection for a placed valley.
 *
 * Valleys use the same invisible barrier carrier as wedges. All four logical valley
 * orientations share one generated model; the ItemDisplay rotation selects
 * the required high end of the diagonal trough.
 */
public class ValleyDisplayManager {
	private static final String DISPLAY_TYPE = "rnw_valley_display";

	private final CustomItemFactory itemFactory;

	public ValleyDisplayManager(CustomItemFactory itemFactory) {
		this.itemFactory = itemFactory;
	}

	public ItemDisplay create(Block block, CustomBlockDefinition definition) {
		if(!definition.shape().isValley()) {
			throw new IllegalArgumentException("Valley display requires a valley definition");
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

			display.setTransformationMatrix(transformationFor(definition.shape()));
		});
	}

	public ItemDisplay getValleyAt(Block block) {
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
		ItemDisplay display = getValleyAt(block);
		if(display != null) {
			display.remove();
		}
	}

	private Matrix4f transformationFor(BlockShape shape) {
		/*
		 * The canonical valley template has its high end of the diagonal trough at southeast
		 * (x=16,z=16). Rotate that one model for the other three orientations.
		 */
		float rotation = switch(shape) {
			case VALLEY_SE -> 0.0F;
			case VALLEY_NE -> (float)Math.toRadians(90.0);
			case VALLEY_NW -> (float)Math.toRadians(180.0);
			case VALLEY_SW -> (float)-Math.toRadians(90.0);
			case VALLEY_SE_INVERTED -> 0.0F;
			case VALLEY_NE_INVERTED -> (float)Math.toRadians(90.0);
			case VALLEY_NW_INVERTED -> (float)Math.toRadians(180.0);
			case VALLEY_SW_INVERTED -> (float)-Math.toRadians(90.0);
			default -> throw new IllegalArgumentException("Unsupported valley shape: " + shape);
		};

		Matrix4f trans = new Matrix4f().rotateY(rotation);
		if( shape.isInverted())
			trans.rotateX((float)Math.PI);

		return trans;
	}
}
