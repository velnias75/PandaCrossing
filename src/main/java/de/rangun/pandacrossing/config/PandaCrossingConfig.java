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

package de.rangun.pandacrossing.config;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "panda_crossing")
@Config.Gui.Background(Config.Gui.Background.TRANSPARENT)
public class PandaCrossingConfig implements ConfigData {

	public enum ECL {

		Low(ErrorCorrectionLevel.L), Medium(ErrorCorrectionLevel.M), Quartile(ErrorCorrectionLevel.Q),
		High(ErrorCorrectionLevel.H);

		public final ErrorCorrectionLevel level;

		ECL(ErrorCorrectionLevel level) {
			this.level = level;
		}
	};

	@ConfigEntry.BoundedDiscrete(min = 1, max = 256)
	public int dimension = 1;
//	@ConfigEntry.BoundedDiscrete(min = 0, max = 256)
//	public int margin = 1;
	public String preset = "PandaCrossing";
	@ConfigEntry.Gui.Tooltip(count = 4)
	public ECL error_correction_level = ECL.Quartile;
	public int command_delay = 0;
}
