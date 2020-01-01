/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2017 SCIFIO developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package io.scif.gui;

import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

/**
 * ColorModel that handles unsigned 32 bit data.
 */
public class UnsignedIntColorModel extends ColorModel {

	// -- Fields --

	private final int nChannels;

	private final ComponentColorModel helper;

	// -- Constructors --

	public UnsignedIntColorModel(final int pixelBits, final int dataType,
		final int nChannels)
	{
		super(pixelBits, makeBitArray(nChannels, pixelBits), AWTImageTools
			.makeColorSpace(nChannels), nChannels == 4, false,
			Transparency.TRANSLUCENT, dataType);

		helper = new ComponentColorModel(AWTImageTools.makeColorSpace(nChannels),
			nChannels == 4, false, Transparency.TRANSLUCENT, dataType);

		this.nChannels = nChannels;
	}

	// -- ColorModel API methods --

	@Override
	public synchronized Object getDataElements(final int rgb,
		final Object pixel)
	{
		return helper.getDataElements(rgb, pixel);
	}

	@Override
	public boolean isCompatibleRaster(final Raster raster) {
		return raster.getNumBands() == getNumComponents() && raster
			.getTransferType() == getTransferType();
	}

	@Override
	public WritableRaster createCompatibleWritableRaster(final int w,
		final int h)
	{
		final int[] bandOffsets = new int[nChannels];
		for (int i = 0; i < nChannels; i++)
			bandOffsets[i] = i;

		final SampleModel m = new ComponentSampleModel(DataBuffer.TYPE_INT, w, h,
			nChannels, w * nChannels, bandOffsets);
		final DataBuffer db = new DataBufferInt(w * h, nChannels);
		return Raster.createWritableRaster(m, db, null);
	}

	@Override
	public int getAlpha(final int pixel) {
		return Integer.MAX_VALUE;//(int) (Math.pow(2, 32) - 1);
	}

	@Override
	public int getBlue(final int pixel) {
		return getComponent(pixel);
	}

	@Override
	public int getGreen(final int pixel) {
		return getComponent(pixel);
	}

	@Override
	public int getRed(final int pixel) {
		return getComponent(pixel);
	}

	@Override
	public int getAlpha(final Object data) {
		final int max = Integer.MAX_VALUE;
		//BH OOPS! - missing parens final int max = (int) Math.pow(2, 32) - 1;
		if (data instanceof int[]) {
			final int[] i = (int[]) data;
			if (i.length == 1) return getAlpha(i[0]);
			return getAlpha(i.length == 4 ? i[0] : max);
		}
		return max;
	}

	@Override
	public int getRed(final Object data) {
		final int max = Integer.MAX_VALUE;
		//BH OOPS! - missing parens final int max = (int) Math.pow(2, 32) - 1;
		if (data instanceof int[]) {
			final int[] i = (int[]) data;
			if (i.length == 1) return getRed(i[0]);
			return getRed(i.length != 4 ? i[0] : i[1]);
		}
		return max;
	}

	@Override
	public int getGreen(final Object data) {
		final int max = Integer.MAX_VALUE;//(int) Math.pow(2, 32) - 1;
		if (data instanceof int[]) {
			final int[] i = (int[]) data;
			if (i.length == 1) return getGreen(i[0]);
			return getGreen(i.length != 4 ? i[1] : i[2]);
		}
		return max;
	}

	@Override
	public int getBlue(final Object data) {
		final int max = Integer.MAX_VALUE;//(int) Math.pow(2, 32) - 1;
		if (data instanceof int[]) {
			final int[] i = (int[]) data;
			if (i.length == 1) return getBlue(i[0]);
			return getBlue(i[i.length - 1]);
		}
		return max;
	}

	// -- Helper methods --

	// BH Notes: The Compiler does not optimize out Math.pow(2,32). 
	// from the class file:
//	
//	  public int getAlpha(int pixel);
//	     0  ldc2_w <Double 2.0> [101]
//	     3  ldc2_w <Double 32.0> [103]
//	     6  invokestatic java.lang.Math.pow(double, double) : double [105]

	    		 
	    		 
	private int getComponent(final int pixel) {
		final long v = uint(pixel);
		double f = v * 1.0 / 0xFFFFFFFFL; // not needed: (Math.pow(2, 32) - 1);
		f *= 255; 
		return (int) f;
	}

    static long[] uia = /** @j2sNative new Uint32Array(1) || */null;

    private static long uint(int i) {
    	/**
    	 * @j2sNative 
    	 * 
    	 * return (uia[0] = i, uia[0]);
    	 */
    	{
    		return 0xFFFFFFFFL & i;
    	}
	}

	private static int[] makeBitArray(final int nChannels, final int nBits) {
		final int[] bits = new int[nChannels];
		for (int i = 0; i < bits.length; i++) {
			bits[i] = nBits;
		}
		return bits;
	}

}
