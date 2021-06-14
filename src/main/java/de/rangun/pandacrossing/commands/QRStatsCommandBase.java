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

import java.util.concurrent.TimeUnit;

import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;

public abstract class QRStatsCommandBase extends AbstractCommandBase {

	public QRStatsCommandBase() {
		super();
	}

	public QRStatsCommandBase(ICommandAsyncListener l) {
		super(l);
	}

	protected MutableText timeText(final MutableText text, final long ms) {
		return text
				.append(new LiteralText("Placing or undoing the QR code will take around: ").formatted(Formatting.GRAY))
				.append(new LiteralText(String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ms),
						TimeUnit.MILLISECONDS.toMinutes(ms)
								- TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ms)),
						TimeUnit.MILLISECONDS.toSeconds(ms)
								- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms))))
										.formatted(Formatting.DARK_RED));
	}

	protected String dimension(final int dim) {
		return (new StringBuilder()).append(dim).append('x').append(dim).toString();
	}

}