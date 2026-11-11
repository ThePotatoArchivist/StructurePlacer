package archives.tater.structureplacer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;
import static java.util.stream.Collectors.groupingBy;
import static net.minecraft.commands.Commands.literal;

public class StructurePlacer implements ModInitializer {
	public static final String MOD_ID = "structureplacer";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final int MARGIN = 4;
	public static final StructurePlaceSettings SETTINGS = new StructurePlaceSettings();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
			dispatcher.register(literal("place_all_templates").executes(context ->
					placeAllTemplates(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()))
			));
		});
	}

	public static int placeAllTemplates(ServerLevel level, BlockPos origin) {
		try {
			var templateManager = level.getStructureTemplateManager();
			var byDir = templateManager.listTemplates()
					.collect(groupingBy(id -> id.withPath(path -> {
						var endIndex = path.lastIndexOf('/');
						return endIndex >= 0 ? path.substring(0, endIndex) : path;
                    })));

			var z = 0;
			var placed = 0;

			var sortedDirs = byDir.keySet().stream().sorted().toList();
			for (var dir : sortedDirs) {
				var templates = byDir.get(dir);

				var x = 0;
				var maxZ = 0;
				for (var templateId : templates) {
					var template = templateManager.get(templateId).orElse(null);
					if (template == null) continue;

					var pos = origin.offset(x, 0, z);
					var size = template.getSize();
					if (!template.placeInWorld(level, pos, pos, SETTINGS, level.getRandom(), 0)) continue;

					var label = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
					label.setPos(Vec3.atCenterOf(pos.offset(size.getX() / 2, size.getY(), size.getZ() / 2)));
					label.setText(Component.literal(templateId.toString()));
					label.setBillboardConstraints(Display.BillboardConstraints.CENTER);
					level.addFreshEntity(label);

					placed++;
					maxZ = max(maxZ, size.getZ());
					x += size.getX() + MARGIN;
				}
				z += maxZ + MARGIN;
			}
			return placed;
		} catch (Exception e) {
			return 0;
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
