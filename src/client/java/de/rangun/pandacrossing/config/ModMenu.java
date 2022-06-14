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

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import de.rangun.pandacrossing.PandaCrossingMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class ModMenu implements ModMenuApi {

	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (PandaCrossingMod.hasClothConfig2()) {
			return parent -> (new ClothConfig2Utils()).getConfigScreen(parent);
		}

		return parent -> null;
	}
}
