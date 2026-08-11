package rampsnwedges.recipe;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.StonecuttingRecipe;
import rampsnwedges.Configuration;
import rampsnwedges.RampsNWedgesPlugin;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;
import rampsnwedges.item.CustomItemFactory;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

public class RecipeRegistrar {
	private final RampsNWedgesPlugin plugin;
	private final BlockCatalog catalog;
	private final CustomItemFactory itemFactory;
	private final Configuration config;

	public RecipeRegistrar(RampsNWedgesPlugin plugin, BlockCatalog catalog, CustomItemFactory itemFactory, Configuration config) {
		this.plugin = plugin;
		this.catalog = catalog;
		this.itemFactory = itemFactory;
		this.config = config;
	}

	public void registerAll() {
		removeCarrierRecipes();
		
		int count = 0;

		for(CustomBlockDefinition definition : catalog.blocks()) {
			register(definition);
			count++;
		}

		LOG(0, "Registered %d stonecutter recipes", count);
	}

	private void register(CustomBlockDefinition definition) {
		NamespacedKey key = new NamespacedKey(plugin, "cut_" + definition.id());

		ItemStack result = itemFactory.create(definition);

		StonecuttingRecipe recipe = new StonecuttingRecipe(key, result, definition.material().material());
		Bukkit.addRecipe(recipe);
	}

	/**
	 * This will remove any existing recipes used to make the carrier
	 * blocks we utilize for the new ramps and wedges.
	 */
	private void removeCarrierRecipes() {
		Material carrier = config.rampCarrier();
		List<NamespacedKey> remove = new ArrayList<>();

		Iterator<Recipe> recipes = Bukkit.recipeIterator();

		while(recipes.hasNext()) {
			Recipe recipe = recipes.next();
			if( recipe.getResult().getType() != carrier) {
				continue;
			}

			if( recipe instanceof Keyed keyed) {
				remove.add(keyed.getKey());
			}
		}

		for(NamespacedKey key : remove) {
			if( Bukkit.removeRecipe(key) ) {
				LOG(1, "Removed carrier recipe %s", key);
			}
		}
		LOG(0,"Removed %d recipes for reserved ramp carrier %s", remove.size(), carrier);
	}

	
	
}
