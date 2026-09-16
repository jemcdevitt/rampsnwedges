package rampsnwedges;
/* Ramps n Wedges
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.BlockMaterial;
import rampsnwedges.item.CustomItemFactory;
import rampsnwedges.recipe.RecipeRegistrar;
import rampsnwedges.resource.ResourcePackGenerator;

/**
 * The primary plugin.
 */
public class RampsNWedgesPlugin extends JavaPlugin {
	private static Logger logger;

	public static Configuration configuration;
	private BlockCatalog blockCatalog;
	private CustomItemFactory itemFactory;
	private ResourcePackGenerator resourcePackGenerator;
	private RampDisplayManager rampDisplayManager;
	private WedgeDisplayManager wedgeDisplayManager;
	private HipDisplayManager hipDisplayManager;
	private ValleyDisplayManager valleyDisplayManager;
	private PyramidDisplayManager pyramidDisplayManager;

	@Override
	public void onEnable() {
		logger = getLogger();

		saveDefaultConfig();
		configuration = new Configuration(this);

		blockCatalog = new BlockCatalog(configuration);
		itemFactory = new CustomItemFactory(configuration);
		rampDisplayManager = new RampDisplayManager(itemFactory);
		wedgeDisplayManager = new WedgeDisplayManager(itemFactory);
		hipDisplayManager = new HipDisplayManager(itemFactory);
		valleyDisplayManager = new ValleyDisplayManager(itemFactory);
		pyramidDisplayManager = new PyramidDisplayManager(itemFactory);

		
		if( configuration.shouldRegenResourcePack() ) {
			resourcePackGenerator = new ResourcePackGenerator(this, blockCatalog, configuration);
			try {
				Path resourcePack = resourcePackGenerator.generate();
				LOG(10, "Generated resource pack: %s", resourcePack);
			} catch(IOException | RuntimeException e) {
				logger.severe("Unable to generate resource pack: " + e.getMessage());
				throw new IllegalStateException("Resource pack generation failed", e);
			}
		} else {
			LOG(10,"Resource pack not regenerated");
		}

		if( configuration.isDebugOn() ) {
			logCatalog();
		}

		RecipeRegistrar recipes = new RecipeRegistrar(this, blockCatalog, itemFactory, configuration);
		recipes.registerAll();

		getServer().getPluginManager().registerEvents(new BlockPlaceListener(configuration, blockCatalog, itemFactory,
																																				 rampDisplayManager, wedgeDisplayManager, hipDisplayManager, valleyDisplayManager, pyramidDisplayManager),
																									this);
		getServer().getPluginManager().registerEvents(new CustomBreakListener(this, blockCatalog, itemFactory, configuration,
																																					rampDisplayManager, wedgeDisplayManager, hipDisplayManager, valleyDisplayManager, pyramidDisplayManager),
																									this);

		LOG(10, "Ramps n Wedges plugin startup");
	}

	public Configuration configuration() {
		return configuration;
	}

	@Override
	public void onDisable() {
		LOG(0, "Ramps n Wedges plugin shutdown");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage("This command can only be run by a player");
			return true;
		}
		return true;
	}

	@Override
	public String namespace() {
		return Constants.NAME_SPACE;
	}

	public BlockCatalog blockCatalog() {
		return blockCatalog;
	}

	public CustomItemFactory itemFactory() {
		return itemFactory;
	}

	// public PlacedBlockStore placedBlockStore() {
	// 	return placedBlockStore;
	// }

	// public ResourcePackGenerator resourcePackGenerator() {
	// 	return resourcePackGenerator;
	// }

	private void logCatalog() {
		logger.info("Loaded " + blockCatalog.materials().size() + " materials producing "
			+ blockCatalog.blocks().size() + " custom blocks.");

		for(BlockMaterial material : blockCatalog.materials()) {
			logger.info(" material: " + material.id() + " -> " + material.displayName());
		}
	}

	static public void LOG(int level, String msg, Object... args) {
		try {
			if(level == 0 || level == 10) {
				if(configuration == null || (configuration.isDebugOn() && level == 0) || level == 10) {
					logger.info(String.format(msg, args));
				}
			} else {
				logger.warning(String.format(msg, args));
			}
		} catch(Exception ex) {
			logger.severe("Exception writing log: " + ex.getMessage());
		}
	}

	static public void LOG(int level, Player player, String msg, Object... args) {
		try {
			String toSend = String.format(msg, args);
			if(level == 0 || level == 10) {
				if(configuration == null || (configuration.isDebugOn() && level == 0) || level == 10) {
					logger.info(toSend);
					if(player != null) {
						player.sendMessage(toSend);
					}
				}
			} else {
				logger.warning(toSend);
				if(player != null) {
					player.sendMessage(toSend);
				}
			}
		} catch(Exception ex) {
			logger.severe("Exception writing log to player: " + ex.getMessage());
			if(player != null) {
				player.sendMessage("Exception writing log to player: " + ex.getMessage());
			}
		}
	}
}
