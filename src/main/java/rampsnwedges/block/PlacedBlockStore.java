package rampsnwedges.block;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.Optional;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import rampsnwedges.Constants;

public class PlacedBlockStore {
	
	public PlacedBlockStore() {
	}

	public void put(Block block, String blockId) {
		if( block == null || blockId == null ) {
			throw new IllegalArgumentException();
		}
		
		Chunk chunk = block.getChunk();
		PersistentDataContainer chunkPdc = chunk.getPersistentDataContainer();

		NamespacedKey locationKey = locationKey(block);

		PersistentDataContainer blockData = chunkPdc.getAdapterContext().newPersistentDataContainer();
		blockData.set(Constants.BLOCK_ID_KEY, PersistentDataType.STRING, blockId);

		chunkPdc.set(locationKey, PersistentDataType.TAG_CONTAINER, blockData);
	}

	public Optional<String> get(Block block) {
		PersistentDataContainer chunkPdc = block.getChunk().getPersistentDataContainer();
		
		PersistentDataContainer blockData = chunkPdc.get(locationKey(block), PersistentDataType.TAG_CONTAINER);
		if( blockData == null) {
			return Optional.empty();
		}
		
		return Optional.ofNullable(blockData.get(Constants.BLOCK_ID_KEY, PersistentDataType.STRING));
	}
	
	public Optional<String> remove(Block block) {
		PersistentDataContainer chunkPdc = block.getChunk().getPersistentDataContainer();
		
		NamespacedKey locationKey = locationKey(block);
		PersistentDataContainer blockData = chunkPdc.get(locationKey, PersistentDataType.TAG_CONTAINER);
		if( blockData == null ) {
			return Optional.empty();
		}
		
		String blockId = blockData.get(Constants.BLOCK_ID_KEY, PersistentDataType.STRING);
		chunkPdc.remove(locationKey);
		return Optional.ofNullable(blockId);
	}
	
	public boolean contains(Block block) {
		return block.getChunk().getPersistentDataContainer().has(locationKey(block), PersistentDataType.TAG_CONTAINER);
	}

	private NamespacedKey locationKey(Block block) {
		int x = block.getX() & 15;
		int y = block.getY();
		int z = block.getZ() & 15;

		return new NamespacedKey(Constants.NAME_SPACE, Constants.BLOCK_KEY_PREFIX + x + "_" + y + "_" + z);
	}
}
