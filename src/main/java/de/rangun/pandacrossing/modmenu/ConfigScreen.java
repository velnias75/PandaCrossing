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

package de.rangun.pandacrossing.modmenu;

import me.lambdaurora.spruceui.Position;
import me.lambdaurora.spruceui.SpruceTexts;
import me.lambdaurora.spruceui.option.SpruceIntegerInputOption;
import me.lambdaurora.spruceui.option.SpruceOption;
import me.lambdaurora.spruceui.screen.SpruceScreen;
import me.lambdaurora.spruceui.widget.SpruceButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;

public class ConfigScreen extends SpruceScreen {

	private final Screen parent;
	private static int delayVal = 0;

	public ConfigScreen(Screen parent) {
		super(new LiteralText("PandaCrossing Menu"));
		this.parent = parent;
	}

	@Override
	protected void init() {

		super.init();

		SpruceOption delay = new SpruceIntegerInputOption("Command delay:", () -> delayVal, value -> delayVal = value,
				new LiteralText("delay in seconds between sending commands"));

		SpruceButtonWidget button = new SpruceButtonWidget(Position.of(this, (width / 2) - 50, height - 30), 100, 20,
				SpruceTexts.GUI_DONE, btn -> client.openScreen(parent));

		addChild(delay.createWidget(Position.of(this, (width / 2) - 100, 30), 200));
		addChild(button);
	}

	@Override
	public void renderTitle(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 16777215);
	}

	public static int getCommandDelay() {
		return delayVal;
	}

}
