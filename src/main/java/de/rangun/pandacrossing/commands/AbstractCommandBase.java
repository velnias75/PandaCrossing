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

import com.google.zxing.WriterException;

import de.rangun.pandacrossing.PandaCrossingMod;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import de.rangun.pandacrossing.qr.QRGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

abstract class AbstractCommandBase extends AbstractCommandAsyncNotifier {

	AbstractCommandBase() {
	}

	AbstractCommandBase(ICommandAsyncListener l) {
		super(l);
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

	protected int getResultingDimension(final String text) {

		if (PandaCrossingMod.hasClothConfig2()) {
			try {
				return QRGenerator.createQRCodeBitMatrix(text == null ? "PandaCrossing" : text,
						(new ClothConfig2Utils().getConfig()).dimension).getWidth();
			} catch (WriterException e) {
			}
		}

		return 30;
	}

	protected long estimatedMilliseconds(final int dim) {

		if (PandaCrossingMod.hasClothConfig2()) {
			return (new ClothConfig2Utils().getConfig()).command_delay * (dim * dim);
		}

		return -1;
	}

	protected static BlockPos nextPos(final Direction facing, final BlockPos curPos, final int x, final int y) {

		final BlockPos nextPos;

		switch (facing) {
		case WEST:
			nextPos = curPos.add(y * -1, 0, x * -1);
			break;
		case EAST:
			nextPos = curPos.add(y, 0, x);
			break;
		case NORTH:
			nextPos = curPos.add(x, 0, y * -1);
			break;
		default:
			nextPos = curPos.add(x * -1, 0, y);
			break;
		}

		return nextPos;
	}

}