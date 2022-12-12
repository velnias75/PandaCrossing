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
import de.rangun.pandacrossing.qr.QRConfigurator;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;

public final class QRPresetCommand extends QRCommand {

	public QRPresetCommand(PandaCrossingMod mod, Map<ICommandAsyncNotifier, Boolean> commandRunningMap, QRDirection dir,
			final QRConfigurator conf) {
		super(mod, commandRunningMap, dir, conf);
	}

	@Override
	public String commandName() {
		return "QRPresetCommand";
	}

	@Override
	protected String getText(CommandContext<FabricClientCommandSource> ctx) {
		return getPreset();
	}

}
