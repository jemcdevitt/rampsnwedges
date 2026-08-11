package rampsnwedges.resource;
/*
 * Ramps n Wedges
 *
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.bukkit.Material;
import rampsnwedges.Configuration;
import rampsnwedges.RampsNWedgesPlugin;
import rampsnwedges.block.BlockCatalog;
import rampsnwedges.block.CustomBlockDefinition;

import static rampsnwedges.RampsNWedgesPlugin.LOG;

/**
 * Generates the client resource pack used by Ramps n Wedges.
 *
 * Ramps use one configured vanilla stair family as an invisible physical
 * carrier. Wedges use an invisible server-side carrier such as BARRIER and do
 * not require any vanilla model override. Every logical shape gets its own
 * namespaced item/model definition used by both inventory items and persistent
 * ItemDisplays.
 */
public class ResourcePackGenerator {
	private static final String TEMPLATE_TEXTURE = "minecraft:block/stone";

	private final RampsNWedgesPlugin plugin;
	private final BlockCatalog catalog;
	private final Configuration config;

	public ResourcePackGenerator(RampsNWedgesPlugin plugin, BlockCatalog catalog,
								 Configuration config) {
		this.plugin = plugin;
		this.catalog = catalog;
		this.config = config;
	}

	public Path generate() throws IOException {
		Path outputDirectory = plugin.getDataFolder().toPath().resolve("generated-resource-pack");
		Path zipFile = plugin.getDataFolder().toPath().resolve("RampsNWedges-ResourcePack-26.2.zip");

		deleteDirectory(outputDirectory);

		Files.createDirectories(outputDirectory.resolve("assets/minecraft/models/block"));
		Files.createDirectories(outputDirectory.resolve("assets/rampsnwedges/models/block"));
		Files.createDirectories(outputDirectory.resolve("assets/rampsnwedges/items"));

		writePackMetadata(outputDirectory);
		writeReadme(outputDirectory);
		generateRampCarrierModels(outputDirectory);
		generateCustomItemModels(outputDirectory);
		createZip(outputDirectory, zipFile);
		return zipFile;
	}

	/**
	 * Hide the configured stair carrier. Minecraft can select straight, inner,
	 * or outer stair models as surrounding blocks change, so all three model
	 * variants must be invisible.
	 */
	private void generateRampCarrierModels(Path outputDirectory) throws IOException {
		Path modelDirectory = outputDirectory.resolve("assets/minecraft/models/block");
		String baseName = materialName(config.rampCarrier());

		for(String suffix : new String[] { "", "_inner", "_outer" }) {
			Path target = modelDirectory.resolve(baseName + suffix + ".json");
			Files.writeString(target, "{}\n", StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			LOG(0, "Generated invisible ramp carrier model %s", target.getFileName());
		}
	}

	/**
	 * Generate one custom model and item definition for every configured ramp
	 * and wedge. For the generalized material model we intentionally use the
	 * conventional minecraft:block/<material-name> texture path. Blocks with
	 * specialized multi-face models may therefore require future overrides.
	 */
	private void generateCustomItemModels(Path outputDirectory) throws IOException {
		Path modelDirectory = outputDirectory.resolve("assets/rampsnwedges/models/block");
		Path itemDirectory = outputDirectory.resolve("assets/rampsnwedges/items");

		for(CustomBlockDefinition definition : catalog.blocks()) {
			String id = definition.id();
			String texture = "minecraft:block/" + materialName(definition.material().material());
			String template = loadTemplate(definition.shape().templateResource());
			String model = applyMaterial(template, texture);

			Files.writeString(modelDirectory.resolve(id + ".json"), model,
				StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

			String itemDefinition = """
				{
				  "model": {
				    "type": "minecraft:model",
				    "model": "rampsnwedges:block/%s"
				  }
				}
				""".formatted(id);

			Files.writeString(itemDirectory.resolve(id + ".json"), itemDefinition,
				StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);

			LOG(0, "Generated %s item model %s using %s",
				definition.shape().isRamp() ? "ramp" : "wedge", id, texture);
		}
	}

	private String applyMaterial(String template, String texture) {
		if(!template.contains(TEMPLATE_TEXTURE)) {
			throw new IllegalStateException("Model template does not contain expected texture "
				+ TEMPLATE_TEXTURE);
		}

		return template.replace(TEMPLATE_TEXTURE, texture);
	}

	private String materialName(Material material) {
		String key = material.getKey().asString();
		int colon = key.indexOf(':');
		return colon >= 0 ? key.substring(colon + 1) : key;
	}

	private String loadTemplate(String resourceName) throws IOException {
		try(InputStream input = plugin.getResource(resourceName)) {
			if(input == null) {
				throw new IOException("Missing resource template: " + resourceName);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private void writePackMetadata(Path outputDirectory) throws IOException {
		String metadata = """
		{
		    "pack": {
		        "description": "Ramps & Wedges generated resource pack for Minecraft Java 26.2",
		        "min_format" : [ 88, 0 ],
		        "max_format" : [ 88, 0 ]
		    }
		}
		""";

		Files.writeString(outputDirectory.resolve("pack.mcmeta"), metadata, StandardCharsets.UTF_8);
	}

	private void writeReadme(Path outputDirectory) throws IOException {
		String readme = """
			Ramps & Wedges Generated Resource Pack

			This pack was generated automatically by the
			Ramps & Wedges Paper plugin.

			It hides the configured vanilla stair carrier and creates
			namespaced item models for each configured ramp and wedge.

			Do not edit generated files manually.
			""";

		Files.writeString(outputDirectory.resolve("README.txt"), readme, StandardCharsets.UTF_8);
	}

	private void createZip(Path sourceDirectory, Path zipFile) throws IOException {
		Files.deleteIfExists(zipFile);

		try(ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile))) {
			try(var paths = Files.walk(sourceDirectory)) {
				paths.filter(Files::isRegularFile).forEach(path -> {
					try {
						String entryName = sourceDirectory.relativize(path).toString().replace('\\', '/');
						zip.putNextEntry(new ZipEntry(entryName));
						Files.copy(path, zip);
						zip.closeEntry();
					} catch(IOException e) {
						throw new ZipGenerationException(e);
					}
				});
			}
		} catch(ZipGenerationException e) {
			throw e.ioException();
		}
	}

	private void deleteDirectory(Path directory) throws IOException {
		if(!Files.exists(directory)) {
			return;
		}

		try(var paths = Files.walk(directory)) {
			paths.sorted(Comparator.reverseOrder()).forEach(path -> {
				try {
					Files.delete(path);
				} catch(IOException e) {
					throw new DirectoryDeleteException(e);
				}
			});
		} catch(DirectoryDeleteException e) {
			throw e.ioException();
		}
	}

	private static final class ZipGenerationException extends RuntimeException {
		private final IOException ioException;

		private ZipGenerationException(IOException ioException) {
			super(ioException);
			this.ioException = ioException;
		}

		private IOException ioException() {
			return ioException;
		}
	}

	private static final class DirectoryDeleteException extends RuntimeException {
		private final IOException ioException;

		private DirectoryDeleteException(IOException ioException) {
			super(ioException);
			this.ioException = ioException;
		}

		private IOException ioException() {
			return ioException;
		}
	}
}
