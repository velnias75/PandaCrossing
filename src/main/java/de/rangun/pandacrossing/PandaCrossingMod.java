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

import com.mojang.brigadier.context.CommandContext;

import de.rangun.pandacrossing.QRCommand.IQRCommandAsyncListener;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.LiteralText;;

public final class PandaCrossingMod implements ClientModInitializer, IQRCommandAsyncListener {

	@Override
	public void onInitializeClient() {

		final PCUndoCommand pcUndoCommand = new PCUndoCommand();

		DISPATCHER.register(literal("qr")
				.requires(source -> source.getPlayer().isCreative() || source.hasPermissionLevel(4))
				.then(argument("text", greedyString()).executes(new QRCommand(this))).executes(new QRCommandUsage()));

		DISPATCHER.register(
				literal("pcundo").requires(source -> source.getPlayer().isCreative() || source.hasPermissionLevel(4))
						.executes(pcUndoCommand));
	}

	@Override
	public void IQRCommandFinished(final CommandContext<FabricClientCommandSource> ctx) {
		ctx.getSource().sendFeedback(new LiteralText("QR-Code processing finished."));
	}
}
