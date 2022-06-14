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

import java.util.Map;

import com.mojang.brigadier.context.CommandContext;

import de.rangun.pandacrossing.PandaCrossingMod;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import de.rangun.pandacrossing.config.ConfigException;
import de.rangun.pandacrossing.qr.QRGenerator;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public abstract class AbstractCommandBase extends AbstractCommandAsyncNotifier {

	protected final Map<ICommandAsyncNotifier, Boolean> runningMap;

	public static enum QRDirection {
		Horizontal, Vertical, Stairway
	};

	protected AbstractCommandBase() {
		super();
		runningMap = null;
	}

	protected AbstractCommandBase(ICommandAsyncListener l) {
		super(l);
		runningMap = null;
	}

	protected AbstractCommandBase(PandaCrossingMod mod, final Map<ICommandAsyncNotifier, Boolean> map) {
		super(mod);
		runningMap = map;
	}

	protected int getDelay() {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).command_delay;
		}

		return 0;
	}

	protected int getDimension() {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).dimension;
		}

		return 1;
	}

	protected int getXScale() {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).scale;
		}

		return 1;
	}

	protected int getYScale() {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).scale;
		}

		return 1;
	}

	protected String getPreset() {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).preset;
		}

		return "PandaCrossing";
	}

	protected int getResultingDimension(final String text) throws ConfigException {

		try {
			return QRGenerator.createQRCodeBitMatrix(text == null ? getPreset() : text, getDimension()).getWidth()
					* getXScale();
		} catch (Exception e) {
			return 27 * getXScale();
		}
	}

	protected long estimatedMilliseconds(final int dim) {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).command_delay * (dim * dim);
		}

		return -1;
	}

	protected void exceptionFeedback(final CommandContext<FabricClientCommandSource> context, Throwable e) {
		context.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.DARK_RED)
				.formatted(Formatting.BOLD).formatted(Formatting.ITALIC));
	}

	protected Text runningFeedback() {
		return Text.literal("Please wait until execution of last command is finished …").formatted(Formatting.DARK_RED)
				.formatted(Formatting.ITALIC);
	}

	protected static BlockPos nextPos(final QRDirection dir, final Direction facing, final BlockPos curPos, final int x,
			final int y) {

		final BlockPos nextPos;

		switch (facing) {
		case WEST:
			nextPos = curPos.add((dir == QRDirection.Horizontal || dir == QRDirection.Stairway) ? y * -1 : 0,
					(dir == QRDirection.Horizontal) ? 0 : y, x * -1);
			break;
		case EAST:
			nextPos = curPos.add((dir == QRDirection.Horizontal || dir == QRDirection.Stairway) ? y : 0,
					(dir == QRDirection.Horizontal) ? 0 : y, x);
			break;
		case NORTH:
			nextPos = curPos.add(x, dir == QRDirection.Horizontal ? 0 : y,
					(dir == QRDirection.Horizontal || dir == QRDirection.Stairway) ? y * -1 : 0);
			break;
		default:
			nextPos = curPos.add(x * -1, dir == QRDirection.Horizontal ? 0 : y,
					(dir == QRDirection.Horizontal || dir == QRDirection.Stairway) ? y : 0);
			break;
		}

		return nextPos;
	}

}