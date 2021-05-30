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
import com.mojang.brigadier.Command;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

final class QRCommand implements Command<FabricClientCommandSource> {

	private final static String BLACK_CONCRETE_ID = Registry.BLOCK.getId(Blocks.BLACK_CONCRETE).toString();
	private final static String WHITE_CONCRETE_ID = Registry.BLOCK.getId(Blocks.WHITE_CONCRETE).toString();

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

	@Override
	public int run(CommandContext<FabricClientCommandSource> ctx) throws CommandSyntaxException {

		final String txt = getString(ctx, "text");

		final ClientPlayerEntity player = ctx.getSource().getPlayer();
		final Vec3d playerPos = player.getPos();
		final BlockPos curPos = new BlockPos(playerPos.getX(), playerPos.getY() - 1.0d, playerPos.getZ());

		try {

			final BitMatrix matrix = createQRCodeBitMatrix(txt);
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

					player.sendChatMessage("/setblock " + nextPos.getX() + " " + nextPos.getY() + " " + nextPos.getZ()
							+ " " + (b ? BLACK_CONCRETE_ID : WHITE_CONCRETE_ID) + " replace");
				}

			}, matrix);
//			}

		} catch (Exception e) {
			throw new SimpleCommandExceptionType(new Message() {

				@Override
				public String getString() {
					return e.getMessage();
				}
			}).create();
		}

		return Command.SINGLE_SUCCESS;
	}
}
