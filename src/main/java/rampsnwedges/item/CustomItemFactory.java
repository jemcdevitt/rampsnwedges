package rampsnwedges.item;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import rampsnwedges.Configuration;
import rampsnwedges.Constants;
import rampsnwedges.block.CustomBlockDefinition;

public class CustomItemFactory {
	private final Configuration config;

	public CustomItemFactory(Configuration config) {
		this.config = config;
	}

	public ItemStack create(CustomBlockDefinition definition) {
		Material itemMaterial;

		if(definition.shape().isRamp()) {
			/*
			 * A ramp must be a real stair item so Minecraft performs normal stair
			 * facing and top/bottom placement before BlockPlaceEvent fires.
			 */
			itemMaterial = config.rampCarrier();
		} else {
			/*
			 * A wedge must not be a barrier item; holding a barrier causes every
			 * nearby barrier carrier to display its debug icon. Use the configured
			 * visual material itself as the ordinary placeable backing item and
			 * replace it with the wedge carrier after placement.
			 */
			itemMaterial = definition.material().material();
		}

		ItemStack item = ItemStack.of(itemMaterial);

		item.editPersistentDataContainer(pdc -> {
			pdc.set(Constants.RNW_TYPE_KEY, PersistentDataType.STRING, Constants.RNW_TYPE_BLOCK);
			pdc.set(Constants.RNW_ID_KEY, PersistentDataType.STRING, definition.id());
		});

		item.editMeta(meta -> {
			meta.customName(Component.text(definition.displayName()));
			meta.setItemModel(new NamespacedKey(Constants.NAME_SPACE, definition.id()));
		});

		return item;
	}

	public String getBlockId(ItemStack item) {
		if(item == null || item.isEmpty()) {
			return null;
		}

		return item.getPersistentDataContainer().get(Constants.RNW_ID_KEY, PersistentDataType.STRING);
	}
}
