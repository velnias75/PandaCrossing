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

public final class QRGenerator {

	public interface IBlockTraverser {
		void traverse(final int x, final int y, final boolean b) throws InterruptedException;
	};

	public static BitMatrix createQRCodeBitMatrix(final String qrCodeData, final int dimension) throws WriterException {

		@SuppressWarnings("rawtypes")
		final Map<EncodeHintType, Comparable> hintMap = new HashMap<EncodeHintType, Comparable>();

		hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
		hintMap.put(EncodeHintType.MARGIN, 1);

		return new QRCodeWriter().encode(
				new String(qrCodeData.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8), BarcodeFormat.QR_CODE,
				Math.max(1, dimension - 1), Math.max(1, dimension - 1), hintMap);
	}

	public static void traverseQRCode(final IBlockTraverser traverser, final BitMatrix matrix)
			throws InterruptedException {

		for (int y = 0; y < matrix.getHeight(); ++y) {
			for (int x = 0; x < matrix.getWidth(); ++x) {
				traverser.traverse(x, y, matrix.get(x, y));
			}
		}
	}
}
