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
import de.rangun.pandacrossing.config.PandaCrossingConfig;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class PandaCrossingMod implements ClientModInitializer, ICommandAsyncListener {

	private static KeyBinding keyBinding;

	private static boolean hasPermission(final FabricClientCommandSource src) {
		return src.hasPermissionLevel(2) || src.getPlayer().isCreative();
	}

	@Override
	public void onInitializeClient() {

		AutoConfig.register(PandaCrossingConfig.class, GsonConfigSerializer::new);

		keyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.examplemod.spook", InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_U, "category.examplemod.test"));

		final LiteralCommandNode<FabricClientCommandSource> undo = DISPATCHER.register(
				literal("pcundo").requires(source -> hasPermission(source)).executes(new PCUndoCommand(this)));

		DISPATCHER.register(literal("qr").requires(source -> hasPermission(source))
				.then(argument("text", greedyString()).executes(new QRCommand(this))).executes(new QRCommandUsage()));

		DISPATCHER.register(literal("qrundo").redirect(undo));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (keyBinding.wasPressed()) {
				client.openScreen(AutoConfig.getConfigScreen(PandaCrossingConfig.class, null).get());
			}
		});

	}

	@Override
	public void commandFinished(final ICommandAsyncNotifier src, final CommandContext<FabricClientCommandSource> ctx) {

		if (ctx == null)
			throw new IllegalStateException("CommandContext has not been set in " + src.getClass().getName());

		ctx.getSource().sendFeedback(src.feedbackText(ctx));
	}
}
