/*
 * Copyright 2021-2022 by Heiko Schäfer <heiko@rangun.de>
 *
 * This file is part of PandaCrossing.
 *
 * PandaCrossing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * PandaCrossing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with PandaCrossing.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.rangun.pandacrossing;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import de.rangun.pandacrossing.commands.AbstractCommandBase.QRDirection;
import de.rangun.pandacrossing.commands.ICommandAsyncListener;
import de.rangun.pandacrossing.commands.ICommandAsyncNotifier;
import de.rangun.pandacrossing.commands.PCUndoCommand;
import de.rangun.pandacrossing.commands.QRCalcCommand;
import de.rangun.pandacrossing.commands.QRCommand;
import de.rangun.pandacrossing.commands.QRCommandUsage;
import de.rangun.pandacrossing.commands.QRPresetCommand;
import de.rangun.pandacrossing.commands.QRSettingsCommand;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class PandaCrossingMod implements ClientModInitializer, ICommandAsyncListener {

	private static KeyBinding keyBinding;

	private Map<ICommandAsyncNotifier, Boolean> commandRunningMap = new LinkedHashMap<>(10);
	private List<IPandaCrossingModEventListener> cleanUpListeners = new ArrayList<>();

	public static boolean hasClothConfig2() {

		if (FabricLoader.getInstance().isModLoaded("cloth-config2")) {

			final Version v = FabricLoader.getInstance().getModContainer("cloth-config2").get().getMetadata()
					.getVersion();

			try {
				return SemanticVersionPredicateParser.create(">=4.9.0")
						.test(new SemanticVersionImpl(v.getFriendlyString(), false));
			} catch (VersionParsingException e) {
				return false;
			}
		}

		return false;
	}

	@Override
	public void onInitializeClient() {

		ClothConfig2Utils ccu = null;

		if (hasClothConfig2()) {

			ccu = new ClothConfig2Utils();
			ccu.register();

			keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.panda_crossing.settings",
					InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_U, "category.panda_crossing.keys"));
		}

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {

			boolean showMsg = true;

			if (hasClothConfig2()) {
				showMsg = (new ClothConfig2Utils().getConfig()).show_welcome_message;
			}

			if (showMsg) {

				final MutableText welcomeMsg = Text.empty()
						.append(Text.literal("Welcome to PandaCrossing, ").formatted(Formatting.AQUA)
								.append(Text.literal(client.player.getDisplayName().getString())
										.formatted(Formatting.RED))
								.append(Text.literal(" :-)")))
						.formatted(Formatting.AQUA);

//			if (!hasPermission(client.player)) {
//				welcomeMsg.append("\n").append(
//						Text.literal("You don't seem to have the permission to use PandaCrossing on this server :-(")
//								.formatted(Formatting.DARK_RED));
//			}

				if (hasClothConfig2()) {
					welcomeMsg.append("\n")
							.append(Text.literal("Press \'").append(keyBinding.getBoundKeyLocalizedText())
									.append(Text.literal("\' to access the ").append(Text.literal("settings menu")
											.setStyle(Style.EMPTY.withClickEvent(
													new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/qrsettings"))))
											.append(Text.literal(".")))
									.formatted(Formatting.DARK_AQUA));
				} else {

					final String ccUrl_desc = "https://www.curseforge.com/minecraft/mc-mods/cloth-config";
					final String ccUrl_vers = ccUrl_desc + "/files/all?filter-game-version=1738749986%3a73407";

					welcomeMsg.append("\n").append("\n")
							.append(Text.literal("Cloth Config API")
									.setStyle(Style.EMPTY.withHoverEvent(
											new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(ccUrl_desc)))
											.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, ccUrl_vers)))
									.formatted(Formatting.GREEN, Formatting.ITALIC, Formatting.UNDERLINE))
							.append(Text.literal(" is ").formatted(Formatting.YELLOW))
							.append(Text.literal("strongly recommended").formatted(Formatting.BOLD, Formatting.YELLOW))
							.append(Text.literal(" to unlock all features!").formatted(Formatting.YELLOW));
				}

				client.inGameHud.getChatHud().addMessage(welcomeMsg);
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {

			for (IPandaCrossingModEventListener l : cleanUpListeners) {
				l.cleanUp();
			}

			commandRunningMap.clear();
		});

		ClientTickEvents.END_WORLD_TICK.register((client) -> {
			for (IPandaCrossingModEventListener l : cleanUpListeners) {
				try {
					l.worldTickEnded();
				} catch (InterruptedException e) {
				}
			}
		});

		final PCUndoCommand undo = new PCUndoCommand(this, commandRunningMap);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(undoCommandBuilder("pcundo", undo));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("qr").requires(source -> hasPermission(source.getClient().player))
					.then(argument("text", greedyString())
							.executes(new QRCommand(this, commandRunningMap, QRDirection.Horizontal)))
					.executes(new QRCommandUsage()));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(
					literal("qrcalc").then(argument("text", greedyString()).executes(new QRCalcCommand(this, false)))
							.executes(new QRCalcCommand(this, true)));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(undoCommandBuilder("qrundo", undo));
		});

		if (ccu != null) {

			ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
				dispatcher.register(literal("qrpreset").requires(source -> hasPermission(source.getClient().player))
						.executes(new QRPresetCommand(this, commandRunningMap, QRDirection.Horizontal)));
			});

			ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
				dispatcher.register(literal("qrsettings").executes(new QRSettingsCommand()));
			});
		}

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("vqrpreset").requires(source -> hasPermission(source.getClient().player))
					.executes(new QRPresetCommand(this, commandRunningMap, QRDirection.Vertical)));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("vqr").requires(source -> hasPermission(source.getClient().player))
					.then(argument("text", greedyString())
							.executes(new QRCommand(this, commandRunningMap, QRDirection.Vertical))));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("sqrpreset").requires(source -> hasPermission(source.getClient().player))
					.executes(new QRPresetCommand(this, commandRunningMap, QRDirection.Stairway)));
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("sqr").requires(source -> hasPermission(source.getClient().player))
					.then(argument("text", greedyString())
							.executes(new QRCommand(this, commandRunningMap, QRDirection.Stairway))));
		});

		if (ccu != null) {

			final ClothConfig2Utils ccu2 = ccu;

			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				while (keyBinding.wasPressed()) {
					client.setScreen(ccu2.getConfigScreen(null));
				}
			});
		}
	}

	private LiteralArgumentBuilder<FabricClientCommandSource> undoCommandBuilder(final String cmd,
			final PCUndoCommand cmdObject) {

		return literal(cmd).requires(source -> hasPermission(source.getClient().player)).executes(cmdObject);
	}

	private static boolean hasPermission(final ClientPlayerEntity player) {
		return player.hasPermissionLevel(2) || player.isCreative();
	}

	@Override
	public void commandRunning(final ICommandAsyncNotifier src) {
		commandRunningMap.put(src, true);
	}

	@Override
	public void commandFinished(final ICommandAsyncNotifier src, final CommandContext<FabricClientCommandSource> ctx) {

		if (ctx == null)
			throw new IllegalStateException("CommandContext has not been set in " + src.getClass().getName());

		ctx.getSource().sendFeedback(src.feedbackText(ctx));

		commandRunningMap.remove(src);
	}

	public void registerCleanUpListener(IPandaCrossingModEventListener l) {
		cleanUpListeners.add(l);
	}
}
