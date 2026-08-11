package rampsnwedges;
/*
 * Ramps n Wedges
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

public class Configuration {
	static public final boolean DEBUG_ON_DEFAULT = false;

	final RampsNWedgesPlugin plugin;
	private boolean debugOn = DEBUG_ON_DEFAULT;

	private Material rampCarrier;
	private Material wedgeCarrier;
	private List<Material> blockMaterials;

	Configuration(RampsNWedgesPlugin plugin) {
		this.plugin = plugin;
		loadConfiguration();
	}

	public boolean isDebugOn() {
		return debugOn;
	}

	public Material rampCarrier() {
		return rampCarrier;
	}

	public Material wedgeCarrier() {
		return wedgeCarrier;
	}

	public List<Material> blockMaterials() {
		return blockMaterials;
	}

	public void loadConfiguration() {
		FileConfiguration cfg = plugin.getConfig();
		if(cfg == null) {
			LOG(1, "Configuration should not be null");
			return;
		}

		debugOn = cfg.getBoolean("debug", false);
		rampCarrier = getStairMaterial(cfg.getString("carrier.ramp-stair", "RED_SANDSTONE_STAIRS"));
		wedgeCarrier = getBlockMaterial(cfg.getString("carrier.wedge-block", "BARRIER"));
		blockMaterials = new ArrayList<>();

		/*
		 * Current format: one list of ordinary block materials. Every listed
		 * material automatically receives a ramp and all four wedge shapes.
		 */
		List<String> materialNames = cfg.getStringList("materials");
		if(!materialNames.isEmpty()) {
			for(String name : materialNames) {
				blockMaterials.add(getVisualMaterial(name));
			}
			LOG(0, "Found %d defined block materials", blockMaterials.size());
			return;
		}

		/*
		 * Backwards compatibility with the earlier ramps/wedges section format.
		 * Merge both lists so an existing server can start long enough for the
		 * administrator to migrate config.yml.
		 */
		ConfigurationSection materials = cfg.getConfigurationSection("materials");
		if(materials != null) {
			Set<String> legacyNames = new LinkedHashSet<>();
			legacyNames.addAll(materials.getStringList("ramps"));
			legacyNames.addAll(materials.getStringList("wedges"));

			for(String name : legacyNames) {
				Material material = Material.matchMaterial(name);
				if(material != null && material.name().endsWith("_STAIRS")) {
					String baseName = material.name().substring(0,
						material.name().length() - "_STAIRS".length());
					Material baseMaterial = Material.matchMaterial(baseName);
					if(baseMaterial != null && baseMaterial.isBlock() && baseMaterial.isItem()) {
						blockMaterials.add(baseMaterial);
						continue;
					}
				}
				blockMaterials.add(getVisualMaterial(name));
			}
		}

		if(blockMaterials.isEmpty()) {
			LOG(0, "No block materials defined");
		} else {
			LOG(1, "Using legacy materials.ramps/materials.wedges configuration; migrate to a single materials list");
			LOG(0, "Found %d defined block materials", blockMaterials.size());
		}
	}

	private Material getStairMaterial(String name) {
		Material material = Material.matchMaterial(name);
		if(material == null) {
			throw new IllegalArgumentException("Unknown stair material: " + name);
		}

		BlockData data = Bukkit.createBlockData(material);
		if(!(data instanceof Stairs)) {
			throw new IllegalArgumentException(material + " is not a valid stair material");
		}
		return material;
	}

	private Material getBlockMaterial(String name) {
		Material material = Material.matchMaterial(name);
		if(material == null) {
			throw new IllegalArgumentException("Unknown block material: " + name);
		}
		if(!material.isBlock() || material.isAir()) {
			throw new IllegalArgumentException(material + " is not a valid carrier block");
		}
		return material;
	}

	private Material getVisualMaterial(String name) {
		Material material = Material.matchMaterial(name);
		if(material == null) {
			throw new IllegalArgumentException("Unknown material: " + name);
		}
		if(!material.isBlock() || !material.isItem() || material.isAir()) {
			throw new IllegalArgumentException(material + " must be a placeable block item");
		}
		return material;
	}
}
