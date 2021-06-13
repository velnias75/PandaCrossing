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

import org.lwjgl.glfw.GLFW;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;

import de.rangun.pandacrossing.commands.ICommandAsyncListener;
import de.rangun.pandacrossing.commands.ICommandAsyncNotifier;
import de.rangun.pandacrossing.commands.PCUndoCommand;
import de.rangun.pandacrossing.commands.QRCommand;
import de.rangun.pandacrossing.commands.QRCommandUsage;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class PandaCrossingMod implements ClientModInitializer, ICommandAsyncListener {

	private static KeyBinding keyBinding;

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

		final LiteralCommandNode<FabricClientCommandSource> undo = DISPATCHER.register(
				literal("pcundo").requires(source -> hasPermission(source)).executes(new PCUndoCommand(this)));

		DISPATCHER.register(literal("qr").requires(source -> hasPermission(source))
				.then(argument("text", greedyString()).executes(new QRCommand(this))).executes(new QRCommandUsage()));

		DISPATCHER.register(literal("qrundo").redirect(undo));

		if (ccu != null) {

			final ClothConfig2Utils ccu2 = ccu;

			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				while (keyBinding.wasPressed()) {
					client.openScreen(ccu2.getConfigScreen(null));
				}
			});
		}
	}

	private static boolean hasPermission(final FabricClientCommandSource src) {
		return src.hasPermissionLevel(2) || src.getPlayer().isCreative();
	}

	@Override
	public void commandFinished(final ICommandAsyncNotifier src, final CommandContext<FabricClientCommandSource> ctx) {

		if (ctx == null)
			throw new IllegalStateException("CommandContext has not been set in " + src.getClass().getName());

		ctx.getSource().sendFeedback(src.feedbackText(ctx));
	}
}
