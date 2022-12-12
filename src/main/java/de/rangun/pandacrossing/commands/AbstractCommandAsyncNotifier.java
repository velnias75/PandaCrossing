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

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;

abstract class AbstractCommandAsyncNotifier implements ICommandAsyncNotifier {

	private final List<ICommandAsyncListener> listener = new ArrayList<>();
	private CommandContext<FabricClientCommandSource> ctx = null;

	protected AbstractCommandAsyncNotifier() {
	}

	protected AbstractCommandAsyncNotifier(final ICommandAsyncListener l) {
		listener.add(l);
	}

	protected void setCommandContext(final CommandContext<FabricClientCommandSource> ctx) {
		this.ctx = ctx;
	}

	protected void notifyListenersRunning() {
		for (final ICommandAsyncListener l : listener) {
			l.commandRunning(this);
		}
	}

	protected void notifyListenersFinished() {
		for (final ICommandAsyncListener l : listener) {
			l.commandFinished(this, ctx);
		}
	}
}
