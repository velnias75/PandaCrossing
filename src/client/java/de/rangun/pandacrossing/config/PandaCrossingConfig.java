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

package de.rangun.pandacrossing.config;

import static net.minecraft.block.Blocks.BLACK_CONCRETE;
import static net.minecraft.block.Blocks.WHITE_CONCRETE;
import static net.minecraft.util.registry.Registry.BLOCK;

import java.util.Set;

import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.util.Identifier;

@Config(name = "panda_crossing")
@Config.Gui.Background(Config.Gui.Background.TRANSPARENT)
@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
public final class PandaCrossingConfig implements ConfigData {

	public enum ECL {

		Low(ErrorCorrectionLevel.L), Medium(ErrorCorrectionLevel.M), Quartile(ErrorCorrectionLevel.Q),
		High(ErrorCorrectionLevel.H);

		public final ErrorCorrectionLevel level;

		ECL(ErrorCorrectionLevel level) {
			this.level = level;
		}
	};

	@ConfigEntry.BoundedDiscrete(min = 1, max = 256)
	public int dimension = 27;

	@ConfigEntry.BoundedDiscrete(min = 1, max = 256)
	public int scale = 1;

//	@ConfigEntry.BoundedDiscrete(min = 0, max = 256)
//	public int margin = 1;
	public String preset = "PandaCrossing";

	public String material_black = BLOCK.getId(BLACK_CONCRETE).toString();
	public String material_white = BLOCK.getId(WHITE_CONCRETE).toString();

	@ConfigEntry.Gui.Tooltip(count = 4)
	public ECL error_correction_level = ECL.Quartile;

	public int command_delay = 0;

	public boolean animation_mode = false;

	public boolean show_welcome_message = true;

	@Override
	public void validatePostLoad() throws ValidationException {

		if (preset.isEmpty() || "".equals(preset))
			throw new ValidationException("preset cannot be empty");

		if (!isValidMaterial(material_black))
			throw new ValidationException("no such black material: " + material_black);

		if (!isValidMaterial(material_white))
			throw new ValidationException("no such white material: " + material_white);
	}

	public boolean isValidMaterial(String material) {
		final Identifier id = Identifier.tryParse(material);
		return (id != null && BLOCK.containsId(id));
	}

	private Set<Integer> dims() {
		return null;
	}
}
