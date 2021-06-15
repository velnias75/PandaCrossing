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

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.config.ConfigException;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class QRCalcCommand extends AbstractQRStatsCommandBase implements Command<FabricClientCommandSource> {

	private final boolean usePreset;
	private int dim;
	private long ms;

	public QRCalcCommand(final ICommandAsyncListener l, boolean usePreset) {
		super(l);
		this.usePreset = usePreset;
	}

	private String getQRText(CommandContext<FabricClientCommandSource> context) {

		if (usePreset) {
			return getPreset();
		}

		return getString(context, "text");
	}

	@Override
	public int run(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {

		setCommandContext(context);

		final Runnable task = () -> {

			try {
				this.dim = getResultingDimension(getQRText(context));
				this.ms = estimatedMilliseconds(this.dim);
			} catch (ConfigException e) {
				exceptionFeedback(context, e);
			} finally {
				notifyListenersFinished();
			}
		};

		new Thread(task).start();

		return Command.SINGLE_SUCCESS;
	}

	@Override
	public String commandName() {
		return "QRCalc";
	}

	@Override
	public Text feedbackText(CommandContext<FabricClientCommandSource> ctx) {

		MutableText usage = new LiteralText("The created QR code of ").formatted(Formatting.GRAY)
				.append(new LiteralText("\"" + getQRText(ctx) + "\"").formatted(Formatting.ITALIC)).append(" will be ")
				.append(new LiteralText(dimension(dim)).formatted(Formatting.ITALIC)).append(" blocks.");

		if (ms > 0) {
			usage = timeText(usage.append("\n"), ms);
		}

		return usage;
	}

}
