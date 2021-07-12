/*
 * Copyright 2021 by Heiko Schäfer <heiko@rangun.de>
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
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.DISPATCHER;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import de.rangun.pandacrossing.commands.AbstractCommandBase.QRDirection;
import de.rangun.pandacrossing.commands.ICommandAsyncListener;
import de.rangun.pandacrossing.commands.ICommandAsyncNotifier;
import de.rangun.pandacrossing.commands.PCUndoCommand;
import de.rangun.pandacrossing.commands.QRCalcCommand;
import de.rangun.pandacrossing.commands.QRCommand;
import de.rangun.pandacrossing.commands.QRCommandUsage;
import de.rangun.pandacrossing.commands.QRPresetCommand;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
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
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

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

			final MutableText welcomeMsg = new LiteralText("Welcome to PandaCrossing, ").formatted(Formatting.AQUA)
					.append(new LiteralText(client.player.getDisplayName().asString()).formatted(Formatting.RED)
							.append(new LiteralText(" :-)").formatted(Formatting.AQUA)));

//			if (!hasPermission(client.player)) {
//				welcomeMsg.append("\n").append(
//						new LiteralText("You don't seem to have the permission to use PandaCrossing on this server :-(")
//								.formatted(Formatting.DARK_RED));
//			}

			if (hasClothConfig2()) {
				welcomeMsg.append("\n").append(new LiteralText("Press \'"))
						.append(keyBinding.getBoundKeyLocalizedText())
						.append(new LiteralText("\' to access the settings menu."));
			}

			client.inGameHud.addChatMessage(MessageType.SYSTEM, welcomeMsg, Util.NIL_UUID);
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

		final LiteralCommandNode<FabricClientCommandSource> undo = DISPATCHER
				.register(literal("pcundo").requires(source -> hasPermission(source.getClient().player))
						.executes(new PCUndoCommand(this, commandRunningMap)));

		DISPATCHER.register(literal("qr").requires(source -> hasPermission(source.getClient().player))
				.then(argument("text", greedyString())
						.executes(new QRCommand(this, commandRunningMap, QRDirection.Horizontal)))
				.executes(new QRCommandUsage()));

		DISPATCHER.register(
				literal("qrcalc").then(argument("text", greedyString()).executes(new QRCalcCommand(this, false)))
						.executes(new QRCalcCommand(this, true)));

		DISPATCHER.register(literal("qrundo").redirect(undo));

		if (ccu != null) {
			DISPATCHER.register(literal("qrpreset").requires(source -> hasPermission(source.getClient().player))
					.executes(new QRPresetCommand(this, commandRunningMap, QRDirection.Horizontal)));
		}

		DISPATCHER.register(literal("vqrpreset").requires(source -> hasPermission(source.getClient().player))
				.executes(new QRPresetCommand(this, commandRunningMap, QRDirection.Vertical)));

		DISPATCHER.register(literal("vqr").requires(source -> hasPermission(source.getClient().player))
				.then(argument("text", greedyString())
						.executes(new QRCommand(this, commandRunningMap, QRDirection.Vertical))));

		DISPATCHER.register(literal("sqrpreset").requires(source -> hasPermission(source.getClient().player))
				.executes(new QRPresetCommand(this, commandRunningMap, QRDirection.Stairway)));

		DISPATCHER.register(literal("sqr").requires(source -> hasPermission(source.getClient().player))
				.then(argument("text", greedyString())
						.executes(new QRCommand(this, commandRunningMap, QRDirection.Stairway))));

		if (ccu != null) {

			final ClothConfig2Utils ccu2 = ccu;

			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				while (keyBinding.wasPressed()) {
					client.setScreen(ccu2.getConfigScreen(null));
				}
			});
		}
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
