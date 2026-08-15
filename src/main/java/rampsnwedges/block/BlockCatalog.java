package rampsnwedges.block;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.bukkit.Material;
import rampsnwedges.Configuration;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Catalog of logical ramps, wedges, hips and valleys enabled for this server.
 *
 * Every configured ordinary block material receives one ramp, all four
 * wedge orientations, all four hip orientations and all four valley orientations. Appearance, geometry and physical carrier are therefore
 * independent concepts.
 */
public class BlockCatalog {
	private final List<BlockMaterial> materials;
	private final List<CustomBlockDefinition> blocks;
	private final Map<String, CustomBlockDefinition> blocksById;

	public BlockCatalog(Configuration config) {
		materials = new ArrayList<>();
		blocks = new ArrayList<>();
		blocksById = new LinkedHashMap<>();

		for(Material material : config.blockMaterials()) {
			BlockMaterial blockMaterial = BlockMaterial.from(material);
			materials.add(blockMaterial);

			for(BlockShape shape : BlockShape.values()) {
				add(new CustomBlockDefinition(blockMaterial, shape));
			}
		}
	}

	private void add(CustomBlockDefinition definition) {
		CustomBlockDefinition previous = blocksById.putIfAbsent(definition.id(), definition);
		if(previous != null) {
			LOG(0,"Duplicate custom block id: " + definition.id());
			return;
		}
		blocks.add(definition);
	}

	public List<BlockMaterial> materials() {
		return Collections.unmodifiableList(materials);
	}

	public List<CustomBlockDefinition> blocks() {
		return Collections.unmodifiableList(blocks);
	}

	public Optional<CustomBlockDefinition> find(String id) {
		return Optional.ofNullable(blocksById.get(id));
	}
}
