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

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static net.minecraft.block.Blocks.BLACK_CONCRETE;
import static net.minecraft.block.Blocks.WHITE_CONCRETE;
import static net.minecraft.util.registry.Registry.BLOCK;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.google.zxing.common.BitMatrix;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.pandacrossing.IPandaCrossingModEventListener;
import de.rangun.pandacrossing.PandaCrossingMod;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import de.rangun.pandacrossing.qr.ConfigException;
import de.rangun.pandacrossing.qr.QRConfigurator;
import de.rangun.pandacrossing.qr.QRGenerator;
import de.rangun.pandacrossing.qr.QRGenerator.IBlockTraverser;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class QRCommand extends AbstractCommandBase
		implements Command<FabricClientCommandSource>, IPandaCrossingModEventListener {

	private final static Identifier BLACK_CONCRETE_ID = BLOCK.getId(BLACK_CONCRETE);
	private final static Identifier WHITE_CONCRETE_ID = BLOCK.getId(WHITE_CONCRETE);

	private final Lock lock = new ReentrantLock();
	private final Condition notTicking = lock.newCondition();

	private final QRDirection dir;

	public QRCommand(final PandaCrossingMod mod, Map<ICommandAsyncNotifier, Boolean> commandRunningMap,
			final QRDirection dir, final QRConfigurator conf) {
		super(mod, commandRunningMap, conf);
		this.dir = dir;

		mod.registerCleanUpListener(this);
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

	private BlockPos curPos(final QRDirection dir, final ClientPlayerEntity player) {
		final Vec3d playerPos = player.getPos();

		if (dir == QRDirection.Vertical) {

			switch (player.getHorizontalFacing()) {
			case WEST:
				return new BlockPos(playerPos.getX() - 1.0d, playerPos.getY(), playerPos.getZ());
			case EAST:
				return new BlockPos(playerPos.getX() + 1.0d, playerPos.getY(), playerPos.getZ());
			case NORTH:
				return new BlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ() - 1.0d);
			default:
				return new BlockPos(playerPos.getX(), playerPos.getY(), playerPos.getZ() + 1.0d);
			}

		}

		return new BlockPos(playerPos.getX(), playerPos.getY() - 1.0d, playerPos.getZ());
	}

	private StringBuilder blockState(final String blockid, final Direction facing) {

		final Block block = BLOCK.get(Identifier.tryParse(blockid));

		if (block != null && block.getDefaultState().contains(
				DirectionProperty.of("facing", Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST))) {
			return (new StringBuilder("[facing=")).append(facing.asString()).append(']');
		}

		return new StringBuilder();
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		final String txt = getText(ctx);
		final int delay = getDelay();

		final ClientPlayerEntity player = ctx.getSource().getPlayer();
		final BlockPos curPos = curPos(dir, player);

		final boolean animationMode = PandaCrossingMod.hasClothConfig2()
				? (new ClothConfig2Utils()).getConfig().animation_mode
				: false;

		setCommandContext(ctx);

		final Runnable task = () -> {

			if (runningMap.isEmpty()) {

				notifyListenersRunning();

				try {

					final BitMatrix matrix = QRGenerator.createQRCodeBitMatrix(txt, getDimension(), conf);
					final Direction facing = player.getHorizontalFacing();

					final ClothConfig2Utils ccu = PandaCrossingMod.hasClothConfig2() ? (new ClothConfig2Utils()) : null;
					final String black_material = ccu != null ? ccu.getBlackMaterial() : BLACK_CONCRETE_ID.toString();
					final String white_material = ccu != null ? ccu.getWhiteMaterial() : WHITE_CONCRETE_ID.toString();

					if (ccu != null) {

						if (!ccu.getConfig().isValidMaterial(black_material)) {
							throw new ConfigException("no such black material: " + black_material);
						}

						if (!ccu.getConfig().isValidMaterial(white_material)) {
							throw new ConfigException("no such white material: " + white_material);
						}
					}

					PCUndoCommand.pushUndoMatrix(player, dir, facing, curPos, matrix, getXScale(), getYScale());
					QRGenerator.traverseQRCode(new IBlockTraverser() {

						@Override
						public final void traverse(int x, int y, boolean b) throws InterruptedException {

							final BlockPos nextPos = nextPos(dir, facing, curPos, x, y);

							if (animationMode) {
								player.setPos(nextPos.getX(), nextPos.getY() + 1, nextPos.getZ());
							}

							player.sendChatMessage((new StringBuilder("/setblock ")).append(nextPos.getX()).append(' ')
									.append(nextPos.getY()).append(' ').append(nextPos.getZ()).append(' ')
									.append((b ? black_material : white_material))
									.append(blockState(b ? black_material : white_material, facing)).append(" replace")
									.toString());

							if (delay > 0) {
								TimeUnit.MILLISECONDS.sleep(delay);
							}

							if (animationMode) {
								lock.lock();

								boolean ticking = true;

								try {
									while (ticking) {
										notTicking.await();
										ticking = false;
									}
								} finally {
									lock.unlock();
								}
							}
						}

						@Override
						public final int getXScale() {
							return QRCommand.super.getXScale();
						}

						@Override
						public final int getYScale() {
							return QRCommand.super.getYScale();
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

	@Override
	public void cleanUp() {
	}

	@Override
	public void worldTickEnded() throws InterruptedException {

		lock.lock();

		try {
			notTicking.signal();
		} finally {
			lock.unlock();
		}
	}

}
