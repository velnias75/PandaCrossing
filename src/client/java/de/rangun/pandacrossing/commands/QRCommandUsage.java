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

package de.rangun.pandacrossing.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.qr.ConfigException;
import de.rangun.pandacrossing.qr.QRConfigurator;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class QRCommandUsage extends AbstractQRStatsCommandBase implements Command<FabricClientCommandSource> {

	public QRCommandUsage(final QRConfigurator conf) {
		super(conf);
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		try {
			final int rd = getResultingDimension(null);

			final MutableText usage = Text.literal("Usage: ").formatted(Formatting.DARK_RED)
					.append(Text.literal("/qr [text]").formatted(Formatting.YELLOW).formatted(Formatting.ITALIC))
					.append("\n")
					.append(Text.literal(" creates a ca. " + dimension(rd)
							+ " (depending on [text]) blocks horizontal concrete QR code with the bottom left corner below the player\'s feet, representing ")
							.formatted(Formatting.GRAY)
							.append(Text.literal("text").formatted(Formatting.ITALIC)
									.append(Text.literal(".").append("\n")
											.append(Text.literal(" /qrundo").formatted(Formatting.YELLOW)
													.formatted(Formatting.ITALIC)
													.append(Text.literal(" will undo the last creation.")
															.formatted(Formatting.RESET)
															.formatted(Formatting.GRAY))))));

			final long ms = estimatedMilliseconds(rd);

			if (ms > 0) {
				timeText(usage.append("\n\n "), ms);
			}

			ctx.getSource().sendFeedback(usage);

		} catch (ConfigException e) {
			exceptionFeedback(ctx, e);
		}

		return Command.SINGLE_SUCCESS;
	}

	@Override
	public String commandName() {
		return "QRCommandUsage";
	}

	@Override
	public Text feedbackText(CommandContext<FabricClientCommandSource> ctx) {
		return null;
	}

}
