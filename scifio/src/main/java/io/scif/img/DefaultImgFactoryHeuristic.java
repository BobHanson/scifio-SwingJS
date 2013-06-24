/*
 * #%L
 * OME SCIFIO package for reading and converting scientific file formats.
 * %%
 * Copyright (C) 2005 - 2013 Open Microscopy Environment:
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

package io.scif.img;

import io.scif.Metadata;
import io.scif.common.DataTools;
import io.scif.img.ImgOptions.ImgMode;
import io.scif.img.cell.SCIFIOCellImgFactory;
import io.scif.util.FormatTools;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.meta.Axes;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Default {@link ImgFactoryHeuristic} implementation. Uses the following
 * heuristic to select a factory:
 * <ul>
 *   <li>Check each ImgMode in order</li>
 *   <li>If ImgMode.ARRAY, select if dataset size < 2GB</li>
 *   <li>If ImgMode.PLANAR, select if plane size < 2GB and dataset fits memory.<li>
 *   <li>If ImgMode.CELL, return a SCIFIOCellImgFactory.</li>
 *   <li>If ImgMode.AUTO or none of the requested types could be selected,
 *   check as though the order were ARRAY -> PLANAR -> CELL.</li>
 * </ul>
 * 
 * <p>
 * NB: ImgMode.CELL is always satisfied. Thus to avoid a particular ImgMode,
 * provide a list excluding the undesired types that includes ImgMode.CELL
 * last.
 * </p>
 * 
 * @author Mark Hiner hinerm at gmail.com
 *
 */
public class DefaultImgFactoryHeuristic implements ImgFactoryHeuristic {

  // -- Constants --

  // % of available memory to trigger opening as a CellImg, if surpassed
  private static final double MEMORY_THRESHOLD = 0.75;
  
  // -- ImgFactoryHeuristic API Methods --
  
  /*
   * @see ImgFactoryHeuristic#createFactory(m, ImgMode[])
   */
  public <T extends RealType<T> & NativeType<T>> ImgFactory<T> createFactory(
      Metadata m, ImgMode[] imgModes) throws IncompatibleTypeException {
    ImgFactory<T> tmpFactory = null;

    T type = ImgIOUtils.makeType(m.getPixelType(0));
    
    // Max size of a plane of a PlanarImg, or total dataset for ArrayImg. 2GB.
    long maxSize = DataTools.safeMultiply64(2, 1024, 1024, 1024);
    
    long availableMem = (long) (Runtime.getRuntime().freeMemory() * MEMORY_THRESHOLD);
    long datasetSize = m.getDatasetSize();
    
    // check for overflow
    if (datasetSize <= 0) datasetSize = Long.MAX_VALUE;
    
    // divide by 1024 to compare to max_size and avoid overflow
    long planeSize = m.getAxisLength(0, Axes.X) * m.getAxisLength(0, Axes.Y) * FormatTools.getBytesPerPixel(m.getPixelType(0));

    boolean fitsInMemory = availableMem > datasetSize;

    boolean decided = false;
    int modeIndex = 0;
    
    // loop over ImgOptions in preferred order
    while (!decided) {
      
      // get the current mode, or AUTO if we've exhausted the list of modes
      ImgMode mode = modeIndex >= imgModes.length ? ImgMode.AUTO : imgModes[modeIndex++];
      
      if (mode.equals(ImgMode.AUTO)) {
        if (!fitsInMemory) tmpFactory = new SCIFIOCellImgFactory<T>();
        else if (datasetSize < maxSize) tmpFactory = new ArrayImgFactory<T>();
        else tmpFactory = new PlanarImgFactory<T>();

        // FIXME: no CellImgFactory right now.. isn't guaranteed to handle all images well (e.g. RGB)
//        else if (planeSize < maxSize) tmpFactory = new PlanarImgFactory<T>();
//        else tmpFactory = new CellImgFactory<T>();
        
        decided = true;
      }
      else if (mode.equals(ImgMode.ARRAY) && datasetSize < maxSize && fitsInMemory) {
        tmpFactory = new ArrayImgFactory<T>();
        decided = true;
      }
      else if (mode.equals(ImgMode.PLANAR) && planeSize < maxSize && fitsInMemory) {
        tmpFactory = new PlanarImgFactory<T>();
        decided = true;
      }
      else if (mode.equals(ImgMode.CELL)) {
        // FIXME: no CellImgFactory right now.. isn't guaranteed to handle all images well (e.g. RGB)
//        if (fitsInMemory) tmpFactory = new CellImgFactory<T>();
//        else tmpFactory = new SCIFIOCellImgFactory<T>();
        tmpFactory = new SCIFIOCellImgFactory<T>();
        
        decided = true;
      }
    }

    return tmpFactory.imgFactory(type);
  }
}