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

package de.rangun.pandacrossing.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class QRCommandUsage extends AbstractCommandBase implements Command<FabricClientCommandSource> {

	public QRCommandUsage() {
		super();
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {
		ctx.getSource().sendFeedback(new LiteralText("Usage: ").formatted(Formatting.DARK_RED)
				.append(new LiteralText("/qr [text]").formatted(Formatting.YELLOW).formatted(Formatting.ITALIC))
				.append("\n")
				.append(new LiteralText(
						" creates a 23x23 block horizontal concrete QR code with the bottom left corner below the player\'s feet, representing ")
								.formatted(Formatting.GRAY)
								.append(new LiteralText("text").formatted(Formatting.ITALIC)
										.append(new LiteralText(".").append("\n")
												.append(new LiteralText(" /qrundo").formatted(Formatting.YELLOW)
														.formatted(Formatting.ITALIC)
														.append(new LiteralText(" will undo the last creation.")
																.formatted(Formatting.GRAY)))))));
		return Command.SINGLE_SUCCESS;
	}

	@Override
	public Text feedbackText(CommandContext<FabricClientCommandSource> ctx) {
		return null;
	}

}
