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
package ome.scifio.filters;

import java.util.Hashtable;

import net.imglib2.meta.AxisType;
import ome.scifio.DatasetMetadata;
import ome.scifio.ImageMetadata;

/**
 * Wrapper for {@link ome.scifio.DatasetMetadata}. Used to create defensive copies of metadata for
 * manipulation by {@link ome.scifio.filters.ReaderFilter}s, while allowing for API modification
 * if needed.
 * <p>
 * If a Reader-based {@code Filter} requires special functionality from its Metadata,
 * a companion DatasetMetadataWrapper can be implemented. Concrete implementations
 * of this interface should always be annotated with {@code DiscoverableMetadataWrapper}
 * so they can be dynamically found when constructing new {@code Filters}.
 * </p>
 * 
 * @author Mark Hiner
 * 
 * @see ome.scifio.discovery.DiscoverableMetadataWrapper
 * @see ome.scifio.filters.AbstractReaderFilter
 */
public interface DatasetMetadataWrapper
  extends DatasetMetadata { 
  
  /**
   * Returns the {@code DatasetMetadata} used for delegation by this wrapper.
   * 
   * @return
   */
  DatasetMetadata unwrap();
  
  /**
   * Sets the {@code DatasetMetadata} this wrapper will delegate to.
   * Necessary for the sake of a zero-parameter constructor to allow
   * {@code SezPoz} discovery. 
   * 
   * @param meta
   */
  void wrap(DatasetMetadata meta);
  
  void addAxis(final int imageIndex, final AxisType type, boolean passUp);
  
  void addAxis(final int imageIndex, final AxisType type, final int value, boolean passUp);
  
  void putDatasetMeta(String key, Object value, boolean passUp);
  
  void putImageMeta(final int imageIndex, String key, Object value, boolean passUp);

  void setThumbSizeX(final int imageIndex, final int thumbX, boolean passUp);

  void setThumbSizeY(final int imageIndex, final int thumbY, boolean passUp);

  void setPixelType(final int imageIndex, final int type, boolean passUp);

  void setBitsPerPixel(final int imageIndex, final int bpp, boolean passUp);

  void setChannelDimLengths(final int imageIndex, final int[] cLengths, boolean passUp);

  void setChannelDimTypes(final int imageIndex, final String[] cTypes, boolean passUp);

  void setOrderCertain(final int imageIndex, final boolean orderCertain, boolean passUp);

  void setRGB(final int imageIndex, final boolean rgb, boolean passUp);

  void setLittleEndian(final int imageIndex, final boolean littleEndian, boolean passUp);

  void setInterleaved(final int imageIndex, final boolean interleaved, boolean passUp);

  void setIndexed(final int imageIndex, final boolean indexed, boolean passUp);

  void setFalseColor(final int imageIndex, final boolean falseC, boolean passUp);

  void setMetadataComplete(final int imageIndex, final boolean metadataComplete, boolean passUp);
  
  void add(final ImageMetadata meta, boolean passUp);

  void setImageMetadata(final int imageIndex, final Hashtable<String, Object> meta, boolean passUp);

  void setThumbnailImage(final int imageIndex, final boolean thumbnail, boolean passUp);

  void setAxisTypes(final int imageIndex, final AxisType[] axisTypes, boolean passUp);
  
  void setAxisType(final int imageIndex, final int axisIndex, final AxisType axis, boolean passUp);

  void setAxisLengths(final int imageIndex, final int[] axisLengths, boolean passUp);
  
  void setAxisLength(final int imageIndex, final AxisType axis, final int length, boolean passUp);
}