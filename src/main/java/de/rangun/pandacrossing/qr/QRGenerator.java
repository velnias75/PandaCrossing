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

package de.rangun.pandacrossing.qr;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import de.rangun.pandacrossing.PandaCrossingMod;
import de.rangun.pandacrossing.config.ClothConfig2Utils;
import de.rangun.pandacrossing.config.ConfigException;
import de.rangun.pandacrossing.config.PandaCrossingConfig;

public final class QRGenerator {

	public interface IBlockTraverser {

		int getXScale();

		int getYScale();

		void traverse(final int x, final int y, final boolean b) throws InterruptedException;
	};

	public static BitMatrix createQRCodeBitMatrix(final String qrCodeData, final int dimension)
			throws WriterException, ConfigException {

		@SuppressWarnings("rawtypes")
		final Map<EncodeHintType, Comparable> hintMap = new HashMap<>(2);

		if (PandaCrossingMod.hasClothConfig2()) {

			final PandaCrossingConfig ccu = (new ClothConfig2Utils()).getConfig();

			hintMap.put(EncodeHintType.ERROR_CORRECTION, ccu.error_correction_level.level);

//			if (ccu.margin > 0) {
//				hintMap.put(EncodeHintType.MARGIN, ccu.margin);
//			}
			hintMap.put(EncodeHintType.MARGIN, 1);

		} else {
			hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
			hintMap.put(EncodeHintType.MARGIN, 1);
		}

		if (qrCodeData.isEmpty() || "".equals(qrCodeData))
			throw new ConfigException("Text cannot be empty");

		return new QRCodeWriter().encode(
				new String(qrCodeData.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), BarcodeFormat.QR_CODE,
				Math.max(1, dimension - 1), Math.max(1, dimension - 1), hintMap);
	}

	public static void traverseQRCode(final IBlockTraverser traverser, final BitMatrix matrix)
			throws InterruptedException {

		final int m_height = matrix.getHeight();
		final int m_width = matrix.getWidth();

		final int x_scale = traverser.getXScale();
		final int y_scale = traverser.getYScale();

		int new_x = 0;
		int new_y = 0;

		for (int y = 0; y < m_height; ++y) {

			for (int ys = 0; ys < y_scale; ++ys) {

				new_x = 0;

				for (int x = 0; x < m_width; ++x) {

					for (int xs = 0; xs < x_scale; ++xs) {
						traverser.traverse(new_x++, new_y, matrix.get(x, y));
					}
				}

				new_y++;
			}
		}
	}
}
