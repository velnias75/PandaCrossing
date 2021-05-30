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
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PandaCrossingMod implements ModInitializer {

	private interface IBlockTraverser {
		void traverse(int x, int y, boolean b);
	};

	private static BitMatrix createQRCodeBitMatrix(String qrCodeData) throws WriterException {

		@SuppressWarnings("rawtypes")
		final Map<EncodeHintType, Comparable> hintMap = new HashMap<EncodeHintType, Comparable>();

		hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
		hintMap.put(EncodeHintType.MARGIN, 1);

		return new QRCodeWriter().encode(
				new String(qrCodeData.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), BarcodeFormat.QR_CODE,
				1, 1, hintMap);
	}

	private static void traverseQRCode(IBlockTraverser traverser, BitMatrix matrix)
			throws WriterException, UnsupportedEncodingException {

		for (int y = 0; y < matrix.getHeight(); ++y) {
			for (int x = 0; x < matrix.getWidth(); ++x) {
				traverser.traverse(x, y, matrix.get(x, y));
			}
		}
	}

	private int removeMaterial(int concrete_left, final ItemStack stack, Item item) {

		if (concrete_left > 0 && stack.getItem().equals(item)) {

			final int dec = Math.min(concrete_left, stack.getMaxCount());

			stack.decrement(dec);
			concrete_left -= dec;
		}
		return concrete_left;
	}

	@Override
	public void onInitialize() {

		final class BlockChecker implements IBlockTraverser {

			private int white_concrete_invcount;
			private int black_concrete_invcount;

			private int white_concrete_count;
			private int black_concrete_count;

			private BlockChecker(PlayerEntity player) {

				white_concrete_count = 0;
				black_concrete_count = 0;

				white_concrete_invcount = player.inventory.count(Items.WHITE_CONCRETE);
				black_concrete_invcount = player.inventory.count(Items.BLACK_CONCRETE);
			}

			private int getWhiteConcreteTotalCount() {
				return white_concrete_count;
			}

			private int getBlackConcreteTotalCount() {
				return black_concrete_count;
			}

			private int getWhiteConcreteNeededCount() {
				return white_concrete_invcount - getWhiteConcreteTotalCount();
			}

			private int getBlackConcreteNeededCount() {
				return black_concrete_invcount - getBlackConcreteTotalCount();
			}

			private boolean isValidInventory() {
				return getWhiteConcreteNeededCount() >= 0 && getBlackConcreteNeededCount() >= 0;
			}

			@Override
			public void traverse(int x, int y, boolean b) {

				if (b) {
					this.black_concrete_count++;
				} else {
					this.white_concrete_count++;
				}
			}
		}

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(literal("qr").then(argument("text", greedyString()).executes(ctx -> {

				final String txt = getString(ctx, "text");

				final ServerPlayerEntity player = ctx.getSource().getPlayer();
				final World world = player.getServerWorld();
				final Vec3d playerPos = player.getPos();
				final BlockPos curPos = new BlockPos(playerPos.getX(), playerPos.getY() - 1.0d, playerPos.getZ());

				try {

					final BitMatrix matrix = createQRCodeBitMatrix(txt);

					// determine material counts
					final BlockChecker materialCount = new BlockChecker(player);

					traverseQRCode(materialCount, matrix);

					if (!player.isCreative() && !materialCount.isValidInventory()) {

						final int white_concrete_needed = materialCount.getWhiteConcreteNeededCount();
						final int black_concrete_needed = materialCount.getBlackConcreteNeededCount();

						ctx.getSource()
								.sendFeedback(new LiteralText("You'll need "
										+ (white_concrete_needed < 0
												? Math.abs(white_concrete_needed) + " "
														+ Items.WHITE_CONCRETE.getName().getString() + ", "
												: "")
										+ (black_concrete_needed < 0
												? Math.abs(black_concrete_needed) + "" + " "
														+ Items.BLACK_CONCRETE.getName().getString() + "."
												: "")),
										false);
					} else {

						if (!player.isCreative()) {

							// remove the material from player's inventory
							int black_concrete_left = materialCount.getBlackConcreteTotalCount();
							int white_concrete_left = materialCount.getWhiteConcreteTotalCount();

							for (int slot = 0; slot < player.inventory.size(); ++slot) {

								final ItemStack stack = player.inventory.getStack(slot);

								black_concrete_left = removeMaterial(black_concrete_left, stack, Items.BLACK_CONCRETE);
								white_concrete_left = removeMaterial(white_concrete_left, stack, Items.WHITE_CONCRETE);
							}
						}

						final Direction facing = player.getHorizontalFacing();

						// place the QR Code
						traverseQRCode(new IBlockTraverser() {

							@Override
							public void traverse(int x, int y, boolean b) {

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

								world.setBlockState(nextPos, b ? Blocks.BLACK_CONCRETE.getDefaultState()
										: Blocks.WHITE_CONCRETE.getDefaultState());
							}

						}, matrix);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				return 1;
			})).executes(ctx -> {
				ctx.getSource().sendFeedback(new LiteralText("Some usage"), false);

				return 1;
			}));
		});
	}
}
