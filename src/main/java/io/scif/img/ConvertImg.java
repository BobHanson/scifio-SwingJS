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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.scijava.Context;
import org.scijava.io.location.BytesLocation;
import org.scijava.io.location.FileLocation;

import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import net.imagej.ImgPlus;

/**
 * A simple manual test opening and saving {@link ImgPlus}es.
 *
 * @author Mark Hiner
 */
public class ConvertImg {

	static boolean j2sHeadless = true;

	
	public static void checkBytes() {
		try {
			byte[] bytes = Files.readAllBytes(new File("data/out_benchmark_v1_2018_x64y64z5c2s1t1.ids.tif").toPath());
			System.err.println("bytes " + bytes.length);
			ImgPlus<?> o = readImage(bytes);
			System.err.println(o.getImg());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * @j2sAlias readImage
	 * @param jsBytes
	 * @return
	 */
	public static ImgPlus<?> readImage(byte[] jsBytes) {
		BytesLocation loc = new BytesLocation(jsBytes);
		final Context c = new Context(Context.INIT_SERVICES | Context.INIT_PLUGINS | Context.INIT_NOT_STRICT | Context.INIT_NOT_DEFERRED);
		final SCIFIOConfig config = new SCIFIOConfig().imgOpenerSetImgModes(
			ImgMode.ARRAY);
		final ImgPlus<?> img = new ImgOpener(c).openImgs(loc, config).get(0);
		return img;
	}
	
	public static void convertImg(final File file) throws Exception {
		final Context c = new Context(Context.INIT_SERVICES | Context.INIT_PLUGINS | Context.INIT_NOT_STRICT | Context.INIT_NOT_DEFERRED);
		final SCIFIOConfig config = new SCIFIOConfig().imgOpenerSetImgModes(
			ImgMode.ARRAY);
		System.err.println("reading " + file);
		final ImgPlus<?> img = new ImgOpener(c)
				.openImgs(new FileLocation(file), config).get(0);
		
		String name = img.getName() + ".tif";
		final String outPath = file.getParent() + "out_" + name;
		System.err.println("saving " + outPath);
		FileLocation loc = new FileLocation(outPath);
		loc.getFile().delete();
		new ImgSaver(c).saveImg(loc, img);
		System.err.println("saving complete " + outPath);
		c.dispose();
		System.err.println("context disposed");
	}

	public static void main(final String[] args) throws Exception {
		
		checkBytes();
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
