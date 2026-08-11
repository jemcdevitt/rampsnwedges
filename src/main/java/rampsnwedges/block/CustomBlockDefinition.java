package rampsnwedges.block;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

/**
 * One logical Ramps n Wedges block.
 */
public record CustomBlockDefinition(BlockMaterial material, BlockShape shape) {
	public String id() {
		return material.id() + "_" + shape.id();
	}

	public String displayName() {
		return material.displayName() + " " + shape.displayName();
	}
}
