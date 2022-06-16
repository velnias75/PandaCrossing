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

import static net.minecraft.util.registry.Registry.BLOCK;

import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import com.google.zxing.common.BitMatrix;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.IPandaCrossingModEventListener;
import de.rangun.pandacrossing.PandaCrossingMod;
import de.rangun.pandacrossing.qr.QRConfigurator;
import de.rangun.pandacrossing.qr.QRGenerator;
import de.rangun.pandacrossing.qr.QRGenerator.IBlockTraverser;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public final class PCUndoCommand extends AbstractCommandBase
		implements Command<FabricClientCommandSource>, IPandaCrossingModEventListener {

	private volatile static Stack<Vector<Vector<UndoBlock>>> undoMatrixStack = new Stack<>();
	private boolean undoSuccess = false;

	private static class UndoBlock {

		final BlockPos pos;
		final BlockState state;

		private UndoBlock(BlockPos pos, BlockState state) {
			this.pos = pos;
			this.state = state;
		}
	}

	public PCUndoCommand(PandaCrossingMod mod, Map<ICommandAsyncNotifier, Boolean> commandRunningMap,
			final QRConfigurator conf) {
		super(mod, commandRunningMap, conf);
		mod.registerCleanUpListener(this);
	}

	public static void pushUndoMatrix(final ClientPlayerEntity player, final QRDirection dir, final Direction facing,
			final BlockPos curPos, final BitMatrix matrix, final int x_scale, final int y_scale)
			throws InterruptedException {

		undoMatrixStack.push(new Vector<Vector<UndoBlock>>(matrix.getHeight()));

		QRGenerator.traverseQRCode(new IBlockTraverser() {

			final World world = player.getEntityWorld();

			@Override
			public final void traverse(int x, int y, boolean b) {

				final BlockPos nextPos = nextPos(dir, facing, curPos, x, y);

				Vector<UndoBlock> row = new Vector<UndoBlock>(matrix.getWidth());
				undoMatrixStack.peek().add(row);

				row = undoMatrixStack.peek().get(y);
				row.add(new UndoBlock(nextPos, world.getBlockState(nextPos)));
			}

			@Override
			public final int getXScale() {
				return x_scale;
			}

			@Override
			public final int getYScale() {
				return y_scale;
			}

		}, matrix);
	}

	private boolean popUndoMatrix(final ClientPlayerEntity player) throws InterruptedException {

		if (!undoMatrixStack.isEmpty()) {

			final int delay = getDelay();

			for (final Vector<UndoBlock> v1 : undoMatrixStack.pop()) {

				for (final UndoBlock v2 : v1) {

					player.sendCommand("setblock " + v2.pos.getX() + " " + v2.pos.getY() + " " + v2.pos.getZ() + " "
							+ BLOCK.getId(v2.state.getBlock()) + deserializeBlockState(v2.state) + " replace");

					if (delay > 0) {
						TimeUnit.MILLISECONDS.sleep(delay);
					}
				}
			}

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
	public String commandName() {
		return "PCUndo";
	}

	@Override
	public Text feedbackText(final CommandContext<FabricClientCommandSource> ctx) {
		return undoSuccess ? Text.translatable("text.panda_crossing.undo.success").formatted(Formatting.BOLD)
				: Text.translatable("text.panda_crossing.undo.nothing").formatted(Formatting.DARK_RED)
						.formatted(Formatting.ITALIC);
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		setCommandContext(ctx);

		final Runnable task = () -> {

			if (runningMap.isEmpty()) {

				notifyListenersRunning();

				try {
					undoSuccess = popUndoMatrix(ctx.getSource().getPlayer());
				} catch (Exception e) {
					ctx.getSource().sendFeedback(Text.literal(e.getMessage()).formatted(Formatting.DARK_RED)
							.formatted(Formatting.BOLD).formatted(Formatting.ITALIC));
				}

				notifyListenersFinished();

			} else {
				ctx.getSource().sendFeedback(runningFeedback());
			}
		};

		new Thread(task).start();

		return Command.SINGLE_SUCCESS;
	}

	@Override
	public void cleanUp() {
		undoMatrixStack.clear();
	}

	@Override
	public void worldTickEnded() {
	}
}
