package rampsnwedges.block;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Material;

/**
 * Defines one configured visual material.
 *
 * The material supplies the stonecutter ingredient, display name, generated
 * model texture and the hardness/tool characteristics used when a wedge is
 * mined. Physical ramp/wedge carriers are intentionally independent.
 */
public record BlockMaterial(Material material, String id, String displayName) {

	public static BlockMaterial from(Material material) {
		String materialName = material.getKey().asString();
		int colon = materialName.indexOf(':');

		if(colon >= 0) {
			materialName = materialName.substring(colon + 1);
		}

		return new BlockMaterial(material, materialName, titleCase(materialName));
	}

	private static String titleCase(String value) {
		StringBuilder result = new StringBuilder();

		for(String word : value.split("_")) {
			if(!result.isEmpty()) {
				result.append(' ');
			}
			result.append(Character.toUpperCase(word.charAt(0)));
			result.append(word.substring(1).toLowerCase());
		}
		return result.toString();
	}
}
