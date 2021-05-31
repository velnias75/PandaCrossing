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

import java.util.Vector;
import static net.minecraft.util.registry.Registry.BLOCK;
import com.google.zxing.common.BitMatrix;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.qr.QRGenerator;
import de.rangun.pandacrossing.qr.QRGenerator.IBlockTraverser;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

final class PCUndoCommand implements Command<FabricClientCommandSource> {

	private static Vector<Vector<UndoBlock>> undoMatrix = null;

	private static class UndoBlock {

		final BlockPos pos;
		final BlockState state;

		private UndoBlock(BlockPos pos, BlockState state) {
			this.pos = pos;
			this.state = state;
		}
	}

	static BlockPos nextPos(final Direction facing, final BlockPos curPos, final int x, final int y) {

		final BlockPos nextPos;

		switch (facing) {
		case WEST:
			nextPos = curPos.add(y * -1, 0, x * -1);
			break;
		case EAST:
			nextPos = curPos.add(y, 0, x);
			break;
		case NORTH:
			nextPos = curPos.add(x, 0, y * -1);
			break;
		default:
			nextPos = curPos.add(x * -1, 0, y);
			break;
		}

		return nextPos;
	}

	public static void generateUndoMatrix(final ClientPlayerEntity player, final Direction facing,
			final BlockPos curPos, final BitMatrix matrix) {

		undoMatrix = new Vector<Vector<UndoBlock>>(matrix.getHeight());

		QRGenerator.traverseQRCode(new IBlockTraverser() {

			final World world = player.getEntityWorld();

			@Override
			public void traverse(int x, int y, boolean b) {

				final BlockPos nextPos = nextPos(facing, curPos, x, y);

				Vector<UndoBlock> row = new Vector<UndoBlock>(matrix.getWidth());
				undoMatrix.add(row);

				row = undoMatrix.get(y);
				row.add(new UndoBlock(nextPos, world.getBlockState(nextPos)));
			}
		}, matrix);
	}

	private boolean applyUndoMatrix(final ClientPlayerEntity player) {

		if (undoMatrix != null) {

			for (final Vector<UndoBlock> v1 : undoMatrix) {
				for (final UndoBlock v2 : v1) {
					player.sendChatMessage("/setblock " + v2.pos.getX() + " " + v2.pos.getY() + " " + v2.pos.getZ()
							+ " " + BLOCK.getId(v2.state.getBlock()) + deserializeBlockState(v2.state) + " replace");
				}
			}

			undoMatrix = null;

			return true;
		}

		return false;
	}

	private static String deserializeBlockState(final BlockState state) {

		final StringBuilder sb = new StringBuilder("[");

		for (final Property<?> p : state.getProperties()) {
			sb.append(p.getName()).append('=').append(state.get(p).toString()).append(',');
		}

		sb.append(']');

		return sb.toString();
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		final Runnable task = () -> {
			ctx.getSource()
					.sendFeedback(applyUndoMatrix(ctx.getSource().getPlayer()) ? new LiteralText("undo successful")
							: new LiteralText("nothing to undo"));
		};

		new Thread(task).start();

		return Command.SINGLE_SUCCESS;
	}

}
