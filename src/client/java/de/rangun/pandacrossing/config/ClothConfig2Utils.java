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

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.client.gui.screen.Screen;

public final class ClothConfig2Utils {

	public void register() {
		AutoConfig.register(PandaCrossingConfig.class, GsonConfigSerializer::new);
	}

	public Screen getConfigScreen(final Screen parent) {
		return AutoConfig.getConfigScreen(PandaCrossingConfig.class, parent).get();
	}

	public PandaCrossingConfig getConfig() {
		return AutoConfig.getConfigHolder(PandaCrossingConfig.class).getConfig();
	}

	public String getBlackMaterial() {
		return getConfig().material_black;
	}

	public String getWhiteMaterial() {
		return getConfig().material_white;
	}
}
