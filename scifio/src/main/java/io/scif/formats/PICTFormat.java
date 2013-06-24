/*
 * #%L
 * SCIFIO library for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2011 - 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
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
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package io.scif.formats;

import io.scif.AbstractFormat;
import io.scif.AbstractMetadata;
import io.scif.AbstractParser;
import io.scif.ByteArrayPlane;
import io.scif.ByteArrayReader;
import io.scif.FormatException;
import io.scif.HasColorTable;
import io.scif.ImageMetadata;
import io.scif.codec.CodecOptions;
import io.scif.codec.JPEGCodec;
import io.scif.codec.PackbitsCodec;
import io.scif.common.DataTools;
import io.scif.gui.AWTImageTools;
import io.scif.gui.LegacyQTTools;
import io.scif.io.ByteArrayHandle;
import io.scif.io.RandomAccessInputStream;
import io.scif.util.FormatTools;

import java.io.IOException;
import java.util.Vector;

import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable8;
import net.imglib2.meta.Axes;

import org.scijava.plugin.Plugin;

/**
 * PictReader is the file format reader for Apple PICT files.
 * Most of this code was adapted from the PICT readers in JIMI
 * (http://java.sun.com/products/jimi/index.html), ImageMagick
 * (http://www.imagemagick.org), and Java QuickDraw.
 *
 * <dl><dt><b>Source code:</b></dt>
 * <dd><a href="http://trac.openmicroscopy.org.uk/ome/browser/bioformats.git/components/bio-formats/src/loci/formats/in/PictReader.java">Trac</a>,
 * <a href="http://git.openmicroscopy.org/?p=bioformats.git;a=blob;f=components/bio-formats/src/loci/formats/in/PictReader.java;hb=HEAD">Gitweb</a></dd></dl>
 */
@Plugin(type = PICTFormat.class)
public class PICTFormat extends AbstractFormat {

  // -- Constants --

  // opcodes that we need
  private static final int PICT_CLIP_RGN = 1;
  private static final int PICT_BITSRECT = 0x90;
  private static final int PICT_BITSRGN = 0x91;
  private static final int PICT_PACKBITSRECT = 0x98;
  private static final int PICT_PACKBITSRGN = 0x99;
  private static final int PICT_9A = 0x9a;
  private static final int PICT_END = 0xff;
  private static final int PICT_LONGCOMMENT = 0xa1;
  private static final int PICT_JPEG = 0x18;
  private static final int PICT_TYPE_1 = 0xa9f;
  private static final int PICT_TYPE_2 = 0x9190;

  /** Table used in expanding pixels that use less than 8 bits. */
  private static final byte[] EXPANSION_TABLE = new byte[256 * 8];

  static {
    for (int i=0; i<256; i++) {
      for (int j=0; j<8; j++) {
        EXPANSION_TABLE[i*8 + j] =
          (byte) ((i & (int) Math.pow(2, 7 - j)) >> 7 - j);
      }
    }
  }
  
  // -- Fields --
  
  private boolean legacy;
  
  // -- PICTFormat API --
  
  public void setLegacy(boolean legacy) {
    this.legacy = legacy;
  }
  
  public boolean isLegacy() {
    return legacy;
  }
  
  // -- Format API Methods --
  /*
   * @see io.scif.Format#getFormatName()
   */
  public String getFormatName() {
    return "PICT";
  }

  /*
   * @see io.scif.Format#getSuffixes()
   */
  public String[] getSuffixes() {
    return new String[] {"pict", "pct"};
  }

  // -- Nested classes --
  
  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Metadata extends AbstractMetadata implements HasColorTable {

    // -- Fields --

    /** Number of bytes in a row of pixel data (variable). */
    protected int rowBytes;

    /** Vector of byte arrays representing individual rows. */
    protected Vector strips;

    /** Whether or not the file is PICT v1. */
    protected boolean versionOne;

    /** Color lookup table for palette color images. */
    protected byte[][] lookup;

    /** Helper reader in case this one fails. */
    protected LegacyQTTools qtTools = new LegacyQTTools();

    private boolean legacy = false;
    private Vector<Long> jpegOffsets = new Vector<Long>();
    
    // -- PICTFormat Metadata getters and setters --

    public int getRowBytes() {
      return rowBytes;
    }

    public void setRowBytes(int rowBytes) {
      this.rowBytes = rowBytes;
    }

    public Vector getStrips() {
      return strips;
    }

    public void setStrips(Vector strips) {
      this.strips = strips;
    }

    public boolean isVersionOne() {
      return versionOne;
    }

    public void setVersionOne(boolean versionOne) {
      this.versionOne = versionOne;
    }

    public byte[][] getLookup() {
      return lookup;
    }

    public void setLookup(byte[][] lookup) {
      this.lookup = lookup;
    }

    public LegacyQTTools getQtTools() {
      return qtTools;
    }

    public void setQtTools(LegacyQTTools qtTools) {
      this.qtTools = qtTools;
    }

    public Vector<Long> getJpegOffsets() {
      return jpegOffsets;
    }

    public void setJpegOffsets(Vector<Long> jpegOffsets) {
      this.jpegOffsets = jpegOffsets;
    }
    
    // -- Metadata API Methods --
    
    public void populateImageMetadata() {
      ImageMetadata iMeta = get(0);
      
      if (iMeta.getAxisIndex(Axes.CHANNEL) == -1)
        iMeta.setAxisLength(Axes.CHANNEL, 1);
      
      iMeta.setAxisLength(Axes.Z, 1);
      iMeta.setAxisLength(Axes.TIME, 1);
      
      iMeta.setLittleEndian(false);
      iMeta.setPlaneCount(1);
      iMeta.setFalseColor(false);
      iMeta.setMetadataComplete(true);
      iMeta.setInterleaved(false);
      iMeta.setPixelType(FormatTools.UINT8);
      iMeta.setBitsPerPixel(8);
      iMeta.setRGB(iMeta.getAxisLength(Axes.CHANNEL) > 1);
      
      iMeta.setIndexed(!iMeta.isRGB() && lookup != null);
    }

    @Override
    public void close(boolean fileOnly) throws IOException {
      super.close(fileOnly);
      if (!fileOnly) {
        rowBytes = 0;
        strips = null;
        versionOne = false;
        lookup = null;
        legacy = false;
        if (jpegOffsets != null) jpegOffsets.clear();
        else jpegOffsets = new Vector<Long>();
      }
    }
    
    // -- HasColorTable API Methods --

    public ColorTable getColorTable(int imageIndex, int planeIndex) {
      return lookup == null ? null : new ColorTable8(lookup);
    }

  }
  
  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Parser extends AbstractParser<Metadata> {

    // -- Parser API methods --
    
    @Override
    protected void typedParse(RandomAccessInputStream stream, Metadata meta)
      throws IOException, FormatException
    {
      meta.createImageMetadata(1);
      ImageMetadata iMeta = meta.get(0);

      stream.seek(518);
      short sizeY = stream.readShort();
      short sizeX = stream.readShort();
      
      iMeta.setAxisLength(Axes.X, sizeX);
      iMeta.setAxisLength(Axes.Y, sizeY);
      
      Vector strips = new Vector();
      byte[][] lookup = null;
      boolean versionOne = false;
      meta.setStrips(strips);
      meta.setLookup(lookup);
      meta.setRowBytes(0);

      int opcode;

      int verOpcode = stream.read();
      int verNumber = stream.read();

      if (verOpcode == 0x11 && verNumber == 0x01) versionOne = true;
      else if (verOpcode == 0x00 && verNumber == 0x11) {
        versionOne = false;
        int verNumber2 = stream.readShort();

        if (verNumber2 != 0x02ff) {
          throw new FormatException("Invalid PICT file : " + verNumber2);
        }

        // skip over v2 header -- don't need it here
        //stream.skipBytes(26);
        stream.skipBytes(6);
        int pixelsPerInchX = stream.readInt();
        int pixelsPerInchY = stream.readInt();
        stream.skipBytes(4);
        int y = stream.readShort();
        int x = stream.readShort();
        if (x > 0) iMeta.setAxisLength(Axes.X, x);
        if (y > 0) iMeta.setAxisLength(Axes.Y, y);
        stream.skipBytes(4);
      }
      else throw new FormatException("Invalid PICT file");

      addGlobalMeta("Version", versionOne ? 1 : 2);
      meta.setVersionOne(versionOne);

      do {
        if (versionOne) opcode = stream.read();
        else {
          // if at odd boundary skip a byte for opcode in PICT v2

          if ((stream.getFilePointer() & 0x1L) != 0) {
            stream.skipBytes(1);
          }
          if (stream.getFilePointer() + 2 >= stream.length()) {
            break;
          }
          opcode = stream.readShort() & 0xffff;
        }
      }
      while (drivePictDecoder(meta, opcode));
    }
    
    // -- Helper methods --

    /** Handles the opcodes in the PICT file. */
    private boolean drivePictDecoder(Metadata meta, int opcode)
      throws FormatException, IOException
    {
      LOGGER.debug("drivePictDecoder({}) @ {}", opcode, in.getFilePointer());

      switch (opcode) {
        case PICT_BITSRGN:  // rowBytes must be < 8
        case PICT_PACKBITSRGN: // rowBytes must be < 8
        case PICT_BITSRECT: // rowBytes must be < 8
        case PICT_PACKBITSRECT:
          meta.setRowBytes(in.readShort());
          if (meta.isVersionOne() || (meta.getRowBytes() & 0x8000) == 0) handleBitmap(meta, opcode);
          else handlePixmap(meta, opcode);
          break;
        case PICT_9A:
          handlePixmap(meta, opcode);
          break;
        case PICT_CLIP_RGN:
          int x = in.readShort();
          in.skipBytes(x - 2);
          break;
        case PICT_LONGCOMMENT:
          in.skipBytes(2);
          x = in.readShort();
          in.skipBytes(x);
          break;
        case PICT_END: // end of PICT
          return false;
        case PICT_TYPE_1:
        case PICT_TYPE_2:
          x = in.read();
          in.skipBytes(x);
          break;
        case PICT_JPEG:
          meta.getJpegOffsets().add(in.getFilePointer() + 2);
          meta.setAxisLength(0, Axes.CHANNEL, 3);
          meta.setRGB(0, true);
          while ((in.readShort() & 0xffff) != 0xffd9 &&
            in.getFilePointer() < in.length());
          while (in.getFilePointer() < in.length()) {
            while ((in.readShort() & 0xffff) != 0xffd8 &&
              in.getFilePointer() < in.length());
            if (in.getFilePointer() < in.length()) {
              meta.getJpegOffsets().add(in.getFilePointer() - 2);
            }
          }
          meta.setInterleaved(0, true);
          break;
        default:
          if (opcode < 0) {
            //throw new FormatException("Invalid opcode: " + opcode);
            LOGGER.warn("Invalid opcode: {}", opcode);
          }
      }

      return in.getFilePointer() < in.length();
    }
    

    /** Extract the image data in a PICT bitmap structure. */
    private void handleBitmap(Metadata meta, int opcode) throws FormatException, IOException {
      readImageHeader(meta, opcode);
      handlePixmap(meta, 1, 1);
    }

    /** Extracts the image data in a PICT pixmap structure. */
    private void handlePixmap(Metadata meta, int opcode) throws FormatException, IOException {
      readImageHeader(meta, opcode);
      LOGGER.debug("handlePixmap({})", opcode);

      int pixelSize = in.readShort();
      int compCount = in.readShort();
      in.skipBytes(14);

      if (opcode == PICT_9A) {
        // rowBytes doesn't exist, so set it to its logical value
        switch (pixelSize) {
          case 32:
            meta.setRowBytes(meta.getAxisLength(0, Axes.X) * compCount);
            break;
          case 16:
            meta.setRowBytes(meta.getAxisLength(0, Axes.X) * 2);
            break;
          default:
            throw new FormatException("Sorry, vector data not supported.");
        }
      }
      else {
        // read the lookup table

        in.skipBytes(4);
        int flags = in.readShort();
        int count = in.readShort();

        count++;
        byte[][] lookup = new byte[3][count];

        for (int i=0; i<count; i++) {
          in.skipBytes(2);
          lookup[0][i] = in.readByte();
          in.skipBytes(1);
          lookup[1][i] = in.readByte();
          in.skipBytes(1);
          lookup[2][i] = in.readByte();
          in.skipBytes(1);
        }
        meta.setLookup(lookup);
      }

      // skip over two rectangles
      in.skipBytes(18);

      if (opcode == PICT_BITSRGN || opcode == PICT_PACKBITSRGN) in.skipBytes(2);

      handlePixmap(meta, pixelSize, compCount);
    }

    /** Handles the unpacking of the image data. */
    private void handlePixmap(Metadata meta, int pixelSize, int compCount)
      throws FormatException, IOException
    {
      LOGGER.debug("handlePixmap({}, {}, {})",
        new Object[] {meta.getRowBytes(), pixelSize, compCount});
      int rawLen;
      byte[] buf;  // row raw bytes
      byte[] uBuf = null;  // row uncompressed data
      int[] uBufI = null;  // row uncompressed data - 16+ bit pixels
      int bufSize = meta.getRowBytes();
      int outBufSize = meta.getAxisLength(0, Axes.X);
      byte[] outBuf = null;  // used to expand pixel data

      boolean compressed = (meta.getRowBytes() >= 8) || (pixelSize == 32);

      // allocate buffers

      switch (pixelSize) {
        case 32:
          if (!compressed) uBufI = new int[meta.getAxisLength(0, Axes.X)];
          else uBuf = new byte[bufSize];
          break;
        case 16:
          uBufI = new int[meta.getAxisLength(0, Axes.X)];
          break;
        case 8:
          uBuf = new byte[bufSize];
          break;
        default:
          outBuf = new byte[outBufSize];
          uBuf = new byte[bufSize];
          break;
      }

      if (!compressed) {
        LOGGER.debug("Pixel data is uncompressed (pixelSize={}).", pixelSize);
        buf = new byte[bufSize];
        for (int row=0; row<meta.getAxisLength(0, Axes.X); row++) {
          in.read(buf, 0, meta.getRowBytes());

          switch (pixelSize) {
            case 16:
              for (int i=0; i<meta.getAxisLength(0, Axes.X); i++) {
                uBufI[i] = DataTools.bytesToShort(buf, i*2, 2, false);
              }
              meta.getStrips().add(uBufI);
              buf = null;
              meta.setAxisLength(0, Axes.CHANNEL, 3);
              break;
            case 8:
              meta.getStrips().add(buf);
              break;
            default: // pixel size < 8
              expandPixels(pixelSize, buf, outBuf, outBuf.length);
              meta.getStrips().add(outBuf);
              buf = null;
          }
        }
      }
      else {
        LOGGER.debug("Pixel data is compressed (pixelSize={}; compCount={}).",
          pixelSize, compCount);
        buf = new byte[bufSize + 1 + bufSize / 128];
        for (int row=0; row<meta.getAxisLength(0, Axes.Y); row++) {
          if (meta.getRowBytes() > 250) rawLen = in.readShort();
          else rawLen = in.read();

          if (rawLen > buf.length) rawLen = buf.length;

          if ((in.length() - in.getFilePointer()) <= rawLen) {
            rawLen = (int) (in.length() - in.getFilePointer() - 1);
          }

          if (rawLen < 0) {
            rawLen = 0;
            in.seek(in.length() - 1);
          }

          in.read(buf, 0, rawLen);

          if (pixelSize == 16) {
            uBufI = new int[meta.getAxisLength(0, Axes.X)];
            unpackBits(buf, uBufI);
            meta.getStrips().add(uBufI);
            meta.setAxisLength(0, Axes.CHANNEL, 3);
          }
          else {
            PackbitsCodec c = new PackbitsCodec();
            c.setContext(getContext());
            CodecOptions options = new CodecOptions();
            options.maxBytes = meta.getAxisLength(0, Axes.X) * 4;
            uBuf = c.decompress(buf, options);
          }

          if (pixelSize < 8) {
            expandPixels(pixelSize, uBuf, outBuf, outBuf.length);
            meta.getStrips().add(outBuf);
          }
          else if (pixelSize == 8) {
            meta.getStrips().add(uBuf);
          }
          else if (pixelSize == 24 || pixelSize == 32) {
            byte[] newBuf = null;

            for (int q=0; q<compCount; q++) {
              int offset = q * meta.getAxisLength(0, Axes.X);
              int len = (int) Math.min(meta.getAxisLength(0, Axes.X), uBuf.length - offset);
              newBuf = new byte[meta.getAxisLength(0, Axes.X)];
              if (offset < uBuf.length) {
                System.arraycopy(uBuf, offset, newBuf, 0, len);
              }
              meta.getStrips().add(newBuf);
            }
            meta.setAxisLength(0, Axes.CHANNEL, 3);
          }
        }
      }
    }

    private void readImageHeader(Metadata meta, int opcode) throws IOException {
      int rowBytes = meta.getRowBytes();
      if (opcode == PICT_9A) in.skipBytes(6);
      else rowBytes &= 0x3fff;
      meta.setRowBytes(rowBytes);

      int tlY = in.readShort();
      int tlX = in.readShort();
      int brY = in.readShort();
      int brX = in.readShort();

      if (brX - tlX > 0) meta.setAxisLength(0, Axes.X, brX - tlX);
      if (brY - tlY > 0) meta.setAxisLength(0, Axes.Y, brY - tlY);

      in.skipBytes(18);
    }

    /** Expand an array of bytes. */
    private void expandPixels(int bitSize, byte[] ib, byte[] ob, int outLen)
      throws FormatException
    {
      LOGGER.debug("expandPixels({}, {}, {}, {})",
        new Object[] {bitSize, ib.length, ob.length, outLen});
      if (bitSize == 1) {
        int remainder = outLen % 8;
        int max = outLen / 8;
        for (int i=0; i<max; i++) {
          if (i < ib.length) {
            int look = (ib[i] & 0xff) * 8;
            System.arraycopy(EXPANSION_TABLE, look, ob, i*8, 8);
          }
          else i = max;
        }

        if (remainder != 0) {
          if (max < ib.length) {
            System.arraycopy(EXPANSION_TABLE, (ib[max] & 0xff) * 8, ob,
              max*8, remainder);
          }
        }

        return;
      }

      byte v;
      int count = 8 / bitSize; // number of pixels in a byte
      int maskshift = bitSize; // num bits to shift mask
      int pixelshift = 8 - bitSize; // num bits to shift pixel
      int tpixelshift = 0;
      int pixelshiftdelta = bitSize;
      int tmask; // temp mask

      if (bitSize != 1 && bitSize != 2 && bitSize != 4) {
        throw new FormatException("Can only expand 1, 2, and 4 bit values");
      }

      int mask = ((int) Math.pow(2, bitSize) - 1) << (8 - bitSize);

      int i = 0;
      for (int o = 0; o < ob.length; i++) {
        tmask = mask;
        tpixelshift = pixelshift;
        v = ib[i];
        for (int t = 0; t < count && o < ob.length; t++, o++) {
          ob[o] = (byte) (((v & tmask) >>> tpixelshift) & 0xff);
          tmask = (byte) ((tmask & 0xff) >>> maskshift);
          tpixelshift -= pixelshiftdelta;
        }
      }
    }

    /** PackBits variant that outputs an int array. */
    private void unpackBits(byte[] ib, int[] ob) {
      LOGGER.debug("unpackBits(...)");
      int i = 0;
      int b;
      int rep;
      int end;

      for (int o=0; o<ob.length;) {
        if (i+1 < ib.length) {
          b = ib[i++];
          if (b >= 0) {
            end = o + b + 1;
            while (o < end && o < ob.length && (i + 1) < ib.length) {
              ob[o++] = DataTools.bytesToShort(ib, i, 2, false);
              i += 2;
            }
          }
          else if (b != -128) {
            rep = DataTools.bytesToShort(ib, i, 2, false);
            i += 2;
            end = o - b + 1;
            while (o < end && o < ob.length) {
              ob[o++] = rep;
            }
          }
        }
        else o = ob.length;
      }
    }
  }
  
  /**
   * @author Mark Hiner hinerm at gmail.com
   *
   */
  public static class Reader extends ByteArrayReader<Metadata> {
    
    // -- Constructor --
    
    public Reader() {
      domains = new String[] {FormatTools.GRAPHICS_DOMAIN};
    }

    // -- Reader API Methods --
    
    public ByteArrayPlane openPlane(int imageIndex, int planeIndex,
      ByteArrayPlane plane, int x, int y, int w, int h)
      throws FormatException, IOException
    {
      Metadata meta = getMetadata();
      byte[] buf = plane.getBytes();

      if (meta.getJpegOffsets().size() > 0) {
        ByteArrayHandle v = new ByteArrayHandle();
        getStream().seek(meta.getJpegOffsets().get(0));
        byte[] b = new byte[(int) (getStream().length() - getStream().getFilePointer())];
        getStream().read(b);
        RandomAccessInputStream s = new RandomAccessInputStream(getContext(), b);
        for (long jpegOffset : meta.getJpegOffsets()) {
          s.seek(jpegOffset - meta.getJpegOffsets().get(0));

          CodecOptions options = new CodecOptions();
          options.interleaved = meta.isInterleaved(0);
          options.littleEndian = meta.isLittleEndian(0);

          v.write(new JPEGCodec().decompress(s, options));
        }

        s = new RandomAccessInputStream(getContext(), v);
        s.seek(0);
        readPlane(s, imageIndex, x, y, w, h, plane);
        s.close();

        return plane;
      }

      if (((PICTFormat)getFormat()).isLegacy() || meta.getStrips().size() == 0) {
        getStream().seek(512);
        byte[] pix = new byte[(int) (getStream().length() - getStream().getFilePointer())];
        getStream().read(pix);
        byte[][] b = AWTImageTools.getBytes(
          AWTImageTools.makeBuffered(meta.getQtTools().pictToImage(pix)));
        pix = null;
        for (int i=0; i<b.length; i++) {
          System.arraycopy(b[i], 0, buf, i*b[i].length, b[i].length);
        }
        b = null;
        return plane;
      }

      // combine everything in the strips Vector

      if ((meta.getAxisLength(0, Axes.Y)*4 < meta.getStrips().size()) && (((meta.getStrips().size() / 3) %
          meta.getAxisLength(0, Axes.Y)) != 0))
      {
        meta.setAxisLength(0, Axes.Y, meta.getStrips().size());
      }

      int planeSize = w * h;

      if (meta.getLookup() != null) {
        // 8 bit data

        byte[] row;

        for (int i=y; i<y+h; i++) {
          row = (byte[]) meta.getStrips().get(i);
          int len = (int) Math.min(row.length, w);
          System.arraycopy(row, x, buf, (i - y) * w, len);
        }
      }
      else if (meta.getAxisLength(0, Axes.Y)*3 == meta.getStrips().size() || meta.getAxisLength(0, Axes.Y)*4 == meta.getStrips().size()) {
        // 24 or 32 bit data

        int nc = meta.getStrips().size() / meta.getAxisLength(0, Axes.Y);

        byte[] c0 = null;
        byte[] c1 = null;
        byte[] c2 = null;

        for (int i=y; i<h + y; i++) {
          c0 = (byte[]) meta.getStrips().get(i * nc + nc - 3);
          c1 = (byte[]) meta.getStrips().get(i * nc + nc - 2);
          c2 = (byte[]) meta.getStrips().get(i * nc + nc - 1);
          int baseOffset = (i - y) * w;
          System.arraycopy(c0, x, buf, baseOffset, w);
          System.arraycopy(c1, x, buf, planeSize + baseOffset, w);
          System.arraycopy(c2, x, buf, 2*planeSize + baseOffset, w);
        }
      }
      else {
        // RGB value is packed into a single short: xRRR RRGG GGGB BBBB
        int[] row = null;
        for (int i=y; i<h + y; i++) {
          row = (int[]) meta.getStrips().get(i);

          for (int j=x; j<w + x; j++) {
            int base = (i - y) * w + (j - x);
            buf[base] = (byte) ((row[j] & 0x7c00) >> 10);
            buf[planeSize + base] = (byte) ((row[j] & 0x3e0) >> 5);
            buf[2 * planeSize + base] = (byte) (row[j] & 0x1f);
          }
        }
      }
      return plane;
    }
    
  }
}