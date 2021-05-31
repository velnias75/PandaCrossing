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

package de.rangun.pandacrossing;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.block.Blocks.BLACK_CONCRETE;
import static net.minecraft.block.Blocks.WHITE_CONCRETE;
import static net.minecraft.util.registry.Registry.BLOCK;

import java.util.ArrayList;
import java.util.List;

import com.google.zxing.common.BitMatrix;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.qr.QRGenerator;
import de.rangun.pandacrossing.qr.QRGenerator.IBlockTraverser;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

final class QRCommand implements Command<FabricClientCommandSource> {

	private final static String BLACK_CONCRETE_ID = BLOCK.getId(BLACK_CONCRETE).toString();
	private final static String WHITE_CONCRETE_ID = BLOCK.getId(WHITE_CONCRETE).toString();

	public interface IQRCommandAsyncListener {
		public void IQRCommandFinished(final CommandContext<FabricClientCommandSource> ctx);
	}

	private final List<IQRCommandAsyncListener> listener = new ArrayList<>();
	
	QRCommand(final IQRCommandAsyncListener l) {
		listener.add(l);
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		final String txt = getString(ctx, "text");

		final ClientPlayerEntity player = ctx.getSource().getPlayer();
		final Vec3d playerPos = player.getPos();
		final BlockPos curPos = new BlockPos(playerPos.getX(), playerPos.getY() - 1.0d, playerPos.getZ());

		final Runnable task = () -> {

			try {

				final BitMatrix matrix = QRGenerator.createQRCodeBitMatrix(txt);
				final Direction facing = player.getHorizontalFacing();
				
				PCUndoCommand.generateUndoMatrix(player, facing, curPos, matrix);
				QRGenerator.traverseQRCode(new IBlockTraverser() {

					@Override
					public void traverse(int x, int y, boolean b) {

						final BlockPos nextPos = PCUndoCommand.nextPos(facing, curPos, x, y);

						player.sendChatMessage("/setblock " + nextPos.getX() + " " + nextPos.getY() + " "
								+ nextPos.getZ() + " " + (b ? BLACK_CONCRETE_ID : WHITE_CONCRETE_ID) + " replace");
					}

				}, matrix);

			} catch (Exception e) {
				ctx.getSource().sendFeedback(new LiteralText(e.getMessage()));
			}

			for (final IQRCommandAsyncListener l : listener) {
				l.IQRCommandFinished(ctx);
			}
		};

		new Thread(task).start();

		return Command.SINGLE_SUCCESS;
	}
}
