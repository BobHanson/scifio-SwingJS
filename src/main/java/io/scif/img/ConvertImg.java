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

package io.scif.img;

import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.gui.ImageViewer;

import java.io.File;

import javax.swing.JFileChooser;

import net.imagej.ImgPlus;

import org.scijava.Context;
import org.scijava.io.location.FileLocation;

/**
 * A simple manual test opening and saving {@link ImgPlus}es.
 *
 * @author Mark Hiner
 */
public class ConvertImg {

	static boolean j2sHeadless = true;

	private static void convertImg(final File file) throws Exception {
		final Context c = new Context(Context.INIT_SERVICES | Context.INIT_PLUGINS | Context.INIT_NOT_STRICT | Context.INIT_NOT_DEFERRED);
		final SCIFIOConfig config = new SCIFIOConfig().imgOpenerSetImgModes(
			ImgMode.ARRAY);
		System.out.println("reading " + file);
		Class<?> cl = ImgOpener.class;
		final ImgPlus<?> img = new ImgOpener(c)
				.openImgs(new FileLocation(file
			.getAbsolutePath()), config).get(0);
		
		String name = img.getName() + ".tif";
		final String outPath = file.getParent() + "out_" + name;
		System.out.println("saving " + outPath);
		FileLocation loc = new FileLocation(outPath);
		loc.getFile().delete();
		new ImgSaver(c).saveImg(loc, img);
		System.out.println("saving complete " + outPath);
		c.dispose();
		System.out.println("context disposed");
	}

	public static void main(final String[] args) throws Exception {
		
		File file = new File("data/benchmark_v1_2018_x64y64z5c2s1t1.ics");
		System.out.println("reading " + file.getAbsolutePath());
		convertImg(file);
//		
//		final JFileChooser opener = new JFileChooser(System.getProperty(
//			"user.home"));
//		final int result = opener.showOpenDialog(null);
//		if (result == JFileChooser.APPROVE_OPTION) {
//			convertImg(opener.getSelectedFile());
//		}
	}

}
