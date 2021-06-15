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

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.block.Blocks.BLACK_CONCRETE;
import static net.minecraft.block.Blocks.WHITE_CONCRETE;
import static net.minecraft.util.registry.Registry.BLOCK;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.zxing.common.BitMatrix;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.qr.QRGenerator;
import de.rangun.pandacrossing.qr.QRGenerator.IBlockTraverser;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class QRCommand extends AbstractCommandBase implements Command<FabricClientCommandSource> {

	private final static Identifier BLACK_CONCRETE_ID = BLOCK.getId(BLACK_CONCRETE);
	private final static Identifier WHITE_CONCRETE_ID = BLOCK.getId(WHITE_CONCRETE);

	public QRCommand(final ICommandAsyncListener l, Map<ICommandAsyncNotifier, Boolean> commandRunningMap) {
		super(l, commandRunningMap);
	}

	@Override
	public String commandName() {
		return "QRCommand";
	}

	@Override
	public Text feedbackText(final CommandContext<FabricClientCommandSource> ctx) {
		return new TranslatableText("text.panda_crossing.qrcommand.finished");
	}

	protected String getText(final CommandContext<FabricClientCommandSource> ctx) {
		return getString(ctx, "text");
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		final String txt = getText(ctx);

		final int delay = getDelay();

		final ClientPlayerEntity player = ctx.getSource().getPlayer();
		final Vec3d playerPos = player.getPos();
		final BlockPos curPos = new BlockPos(playerPos.getX(), playerPos.getY() - 1.0d, playerPos.getZ());

		setCommandContext(ctx);

		final Runnable task = () -> {

			if (runningMap.isEmpty()) {

				notifyListenersRunning();

				try {

					final BitMatrix matrix = QRGenerator.createQRCodeBitMatrix(txt, getDimension());
					final Direction facing = player.getHorizontalFacing();

					PCUndoCommand.generateUndoMatrix(player, facing, curPos, matrix);
					QRGenerator.traverseQRCode(new IBlockTraverser() {

						@Override
						public void traverse(int x, int y, boolean b) throws InterruptedException {

							final BlockPos nextPos = nextPos(facing, curPos, x, y);

							player.sendChatMessage("/setblock " + nextPos.getX() + " " + nextPos.getY() + " "
									+ nextPos.getZ() + " "
									+ (b ? BLACK_CONCRETE_ID.toString() : WHITE_CONCRETE_ID.toString()) + " replace");

							if (delay > 0) {
								TimeUnit.MILLISECONDS.sleep(delay);
							}
						}

					}, matrix);

				} catch (Exception e) {
					exceptionFeedback(ctx, e);
				} finally {
					notifyListenersFinished();
				}

			} else {
				ctx.getSource().sendFeedback(runningFeedback());
			}
		};

		new Thread(task).start();

		return Command.SINGLE_SUCCESS;
	}
}
