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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PandaCrossingMod implements ModInitializer {

	private interface IBlockPlacer {
		void placeBlock(int x, int y, boolean b);
	};

	private static void createQRCode(IBlockPlacer bp, String qrCodeData, int qrCodeheight, int qrCodewidth)
			throws WriterException, UnsupportedEncodingException {

		@SuppressWarnings("rawtypes")
		final Map<EncodeHintType, Comparable> hintMap = new HashMap<EncodeHintType, Comparable>();

		hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
		hintMap.put(EncodeHintType.MARGIN, 1);

		final BitMatrix matrix = new QRCodeWriter().encode(
				new String(qrCodeData.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), BarcodeFormat.QR_CODE,
				qrCodewidth, qrCodeheight, hintMap);

		for (int y = 0; y < matrix.getHeight(); ++y) {
			for (int x = 0; x < matrix.getWidth(); ++x) {
				bp.placeBlock(x, y, matrix.get(x, y));
			}
		}
	}

	@Override
	public void onInitialize() {

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(literal("qr").then(argument("text", greedyString()).executes(ctx -> {

				final String txt = getString(ctx, "text");
				final ServerPlayerEntity player = ctx.getSource().getPlayer();
				final World world = player.getServerWorld();
				final Vec3d playerPos = player.getPos();
				final BlockPos curPos = new BlockPos(playerPos.getX(), playerPos.getY() - 1.0d, playerPos.getZ());

				try {

					createQRCode(new IBlockPlacer() {

						@Override
						public void placeBlock(int x, int y, boolean b) {

							world.setBlockState(curPos.add(x, 0, y), b ? Blocks.BLACK_CONCRETE.getDefaultState()
									: Blocks.WHITE_CONCRETE.getDefaultState());
						}

					}, txt, 21, 21);

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
