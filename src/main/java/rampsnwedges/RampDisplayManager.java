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
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Matrix4f;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Creates the visual projection for a placed ramp.
 *
 * The real block remains the configured stair carrier and supplies collision,
 * facing and top/bottom placement. The persistent ItemDisplay supplies only
 * the visual model selected by the custom ItemStack's item_model component.
 * The display is tagged with the carrier block coordinates so it can be
 * removed reliably when that ramp is broken.
 */
public class RampDisplayManager {
	private static final String DISPLAY_TYPE = "rnw_ramp_display";

	private final CustomItemFactory itemFactory;

	public RampDisplayManager(CustomItemFactory itemFactory) {
		this.itemFactory = itemFactory;
	}

	public ItemDisplay create(Block block, CustomBlockDefinition definition) {
		if(!definition.shape().isRamp()) {
			throw new IllegalArgumentException("Ramp display requires a ramp definition");
		}

		BlockData blockData = block.getBlockData();
		if(!(blockData instanceof Stairs stairs)) {
			throw new IllegalArgumentException("Ramp display requires stair BlockData at "
			                                   + block.getLocation());
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
			pdc.set(Constants.RNW_RAMP_ID_KEY, PersistentDataType.STRING, genRampId(block));

			display.setTransformationMatrix(transformationFor(stairs));
		});
	}

	public ItemDisplay getRampAt(Block block) {
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

		/*
		 * Displays are centered in their carrier block. A radius slightly
		 * smaller than half a block finds our display without touching displays
		 * attached to neighboring blocks.
		 */
		for(Entity entity : block.getWorld().getNearbyEntities(center, 0.49, 0.49, 0.49)) {
			if(!(entity instanceof ItemDisplay display)) {
				continue;
			}

			PersistentDataContainer pdc = display.getPersistentDataContainer();
			String type = pdc.get(Constants.RNW_TYPE_KEY, PersistentDataType.STRING);
			if(!DISPLAY_TYPE.equals(type)) {
				LOG(0,"Not a ramp display");
				continue;
			}

			//although the id is based on the original block location, it is possible for the
			//new block location to be different as can happen if the block is part of a SimpleShips ship.
			//so we're only checking that an ID exists.  The getNearbyEntities should have basically limited us
			//to the block we're on so chances of picking up another item display are minimal.
			//keeping the id still based on block location simply to keep it unique.
			String rampId = pdc.get(Constants.RNW_RAMP_ID_KEY, PersistentDataType.STRING);
			LOG(0,"Found ramp display id %s", rampId);
			if(rampId == null) {
				continue;
			}

			LOG(0,"Removing ramp display %s", rampId);
			display.remove();
		}
	}

	private Matrix4f transformationFor(Stairs stairs) {
		Matrix4f matrix = new Matrix4f();

		/*
		 * The V10 ramp model's unrotated visual high side corresponds to WEST.
		 * Rotate that base model to match the actual stair carrier facing.
		 */
		matrix.rotateY(yRotation(stairs.getFacing()));

		/*
		 * Rotate rather than negatively scale the top-half model. Negative Y
		 * scaling reverses face winding and causes back-face culling artifacts.
		 */
		if(stairs.getHalf() == Bisected.Half.TOP) {
			matrix.rotateX((float)Math.PI);
		}

		return matrix;
	}

	private String genRampId(Block block) {
		return "ramp_" + block.getX() + "_" + block.getY() + "_" + block.getZ();
	}

	private float yRotation(BlockFace facing) {
		return switch(facing) {
			case WEST -> 0.0F;
			case NORTH -> (float)-Math.toRadians(90.0);
			case EAST -> (float)Math.toRadians(180.0);
			case SOUTH -> (float)Math.toRadians(90.0);
			default -> throw new IllegalArgumentException(
				"Unsupported stair facing for ramp: " + facing);
		};
	}
}
