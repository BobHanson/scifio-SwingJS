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

import java.io.File;
import java.io.IOException;

import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;

import ome.scifio.DatasetMetadata;
import ome.scifio.Format;
import ome.scifio.FormatException;
import ome.scifio.Metadata;
import ome.scifio.Plane;
import ome.scifio.Reader;
import ome.scifio.SCIFIO;
import ome.scifio.discovery.DiscoverableMetadataWrapper;
import ome.scifio.io.RandomAccessInputStream;

/**
 * Abstract superclass for all {@link ome.scifio.filters.Filter} that
 * delegate to {@link ome.scifio.Reader} instances.
 * <p>
 * NB: All concrete implementations of this interface should be annotated
 * as {@link ome.scifio.discovery.DiscoverableFilter} for discovery by {@code SezPoz}.
 * </p>
 * <p>
 * NB: This class attempts to locate a type-matching DatasetMetadataWrapper to protectively
 * wrap the wrapped {@code Reader}'s DatasetMetadata. If none is found, a reference to the
 * {@code Reader's} DatasetMetadata itself is used.
 * </p>
 * 
 * @author Mark Hiner
 *
 * @see ome.scifio.Reader
 * @see ome.scifio.filters.Filter
 * @see ome.scifio.discovery.DiscoverableFilter
 * @see ome.scifio.discovery.DiscoverableMetadataWrapper
 * @see ome.scifio.filters.AbstractDatasetMetadataWrapper
 */
public abstract class AbstractReaderFilter extends AbstractFilter<Reader> implements Reader {

  // -- Fields --
  
  private DatasetMetadata wrappedMeta = null;
  
  // -- Constructor --
  
  public AbstractReaderFilter() {
    super(Reader.class);
  }
  
  // -- AbstractReaderFilter API Methods --
  
  /**
   * Allows code to be executed regardless of which {@link #setSource()}
   * signature is called.
   * 
   * @param source - Lowest common denominator of arguments in the
   *                 {@code setSource} series.
   */
  protected void setSourceHelper(String source) {
    
  }
  
  /**
   * Allows code to be executed regardless of which {@link #openPlane()}
   * signature is called.
   */
  protected void openPlaneHelper() {
    
  }
  
  /**
   * Allows code to be executed regardless of which {@link #readPlane()}
   * signature is called.
   */
  protected void readPlaneHelper() {
  }
  
  /**
   * @return
   */
  public DatasetMetadata getParentMeta() {
    return getParent().getDatasetMetadata();
  }
  
  // -- Filter API Methods --
  
  /*
   * @see ome.scifio.filters.AbstractFilter#setParent(java.lang.Object)
   */
  @Override
  public void setParent(Object parent) {
    super.setParent(parent);
    
    Reader r = (Reader) parent;
    
    //TODO Maybe cache this result so we don't have to discover every time setparent is called
    // because it will be called frequently, given how MasterFilterHelper is implemented
    
    // look for a compatible MetadataWrapper class
    for (final IndexItem<DiscoverableMetadataWrapper, DatasetMetadataWrapper> item : 
      Index.load(DiscoverableMetadataWrapper.class, DatasetMetadataWrapper.class)) {
      try {
        if(getClass().isAssignableFrom(item.annotation().filterType())) {
          DatasetMetadataWrapper metaWrapper = item.instance();
          metaWrapper.wrap(r.getDatasetMetadata());
          wrappedMeta = metaWrapper;
          return;
        }
      } catch (final InstantiationException e) {
        // TODO
      }
    }
    
    // No Filter-specific wrapper found
    wrappedMeta = r.getDatasetMetadata();
  }
  
  /*
   * @see ome.scifio.filters.Filter#isCompatible(java.lang.Class)
   */
  public boolean isCompatible(Class<?> c) {
    return Reader.class.isAssignableFrom(c);
  }
  
  // -- Reader API Methods --

  /*
   * @see ome.scifio.Reader#openPlane(int, int)
   */
  public Plane openPlane(int imageIndex, int planeIndex)
      throws FormatException, IOException {
    openPlaneHelper();
    return getParent().openPlane(imageIndex, planeIndex);
  }

  /*
   * @see ome.scifio.Reader#openPlane(int, int, int, int, int, int)
   */
  public Plane openPlane(int imageIndex, int planeIndex, int x, int y, int w,
      int h) throws FormatException, IOException {
    openPlaneHelper();
    return getParent().openPlane(imageIndex, planeIndex, x, y, w, h);
  }

  /*
   * @see ome.scifio.Reader#openPlane(int, int, ome.scifio.Plane)
   */
  public Plane openPlane(int imageIndex, int planeIndex, Plane plane)
      throws FormatException, IOException {
    openPlaneHelper();
    return getParent().openPlane(imageIndex, planeIndex, plane);
  }

  /*
   * @see ome.scifio.Reader#openPlane(int, int, ome.scifio.Plane, int, int, int, int)
   */
  public Plane openPlane(int imageIndex, int planeIndex, Plane plane, int x,
      int y, int w, int h) throws FormatException, IOException {
    openPlaneHelper();
    return getParent().openPlane(imageIndex, planeIndex, plane, x, y, w, h);
  }

  /*
   * @see ome.scifio.Reader#openThumbPlane(int, int)
   */
  public Plane openThumbPlane(int imageIndex, int planeIndex)
      throws FormatException, IOException {
    return getParent().openThumbPlane(imageIndex, planeIndex);
  }

  /*
   * @see ome.scifio.Reader#setGroupFiles(boolean)
   */
  public void setGroupFiles(boolean group) {
    getParent().setGroupFiles(group);
  }

  /*
   * @see ome.scifio.Reader#isGroupFiles()
   */
  public boolean isGroupFiles() {
    return getParent().isGroupFiles();
  }

  /*
   * @see ome.scifio.Reader#fileGroupOption(java.lang.String)
   */
  public int fileGroupOption(String id) throws FormatException, IOException {
    return getParent().fileGroupOption(id);
  }

  /*
   * @see ome.scifio.Reader#getCurrentFile()
   */
  public String getCurrentFile() {
    return getParent().getCurrentFile();
  }

  /*
   * @see ome.scifio.Reader#getDomains()
   */
  public String[] getDomains() {
    return getParent().getDomains();
  }

  /*
   * @see ome.scifio.Reader#getStream()
   */
  public RandomAccessInputStream getStream() {
    return getParent().getStream();
  }

  /*
   * @see ome.scifio.Reader#getUnderlyingReaders()
   */
  public Reader[] getUnderlyingReaders() {
    return getParent().getUnderlyingReaders();
  }

  /*
   * @see ome.scifio.Reader#getOptimalTileWidth(int)
   */
  public int getOptimalTileWidth(int imageIndex) {
    return getParent().getOptimalTileWidth(imageIndex);
  }

  /*
   * @see ome.scifio.Reader#getOptimalTileHeight(int)
   */
  public int getOptimalTileHeight(int imageIndex) {
    return getParent().getOptimalTileHeight(imageIndex);
  }

  /*
   * @see ome.scifio.Reader#setMetadata(ome.scifio.Metadata)
   */
  public void setMetadata(Metadata meta) throws IOException {
    getParent().setMetadata(meta);
  }

  /*
   * @see ome.scifio.Reader#getMetadata()
   */
  public Metadata getMetadata() {
    return getParent().getMetadata();
  }

  /*
   * @see ome.scifio.Reader#getDatasetMetadata()
   */
  public DatasetMetadata getDatasetMetadata() {
    return wrappedMeta;
  }

  /*
   * @see ome.scifio.Reader#setNormalized(boolean)
   */
  public void setNormalized(boolean normalize) {
    getParent().setNormalized(normalize);
  }

  /*
   * @see ome.scifio.Reader#isNormalized()
   */
  public boolean isNormalized() {
    return getParent().isNormalized();
  }

  /*
   * @see ome.scifio.Reader#hasCompanionFiles()
   */
  public boolean hasCompanionFiles() {
    return getParent().hasCompanionFiles();
  }

  /*
   * @see ome.scifio.Reader#setSource(java.lang.String)
   */
  public void setSource(String fileName) throws IOException {
    setSourceHelper(fileName);
    getParent().setSource(fileName);
  }

  /*
   * @see ome.scifio.Reader#setSource(java.io.File)
   */
  public void setSource(File file) throws IOException {
    setSourceHelper(file.getAbsolutePath());
    getParent().setSource(file);
  }

  /*
   * @see ome.scifio.Reader#setSource(ome.scifio.io.RandomAccessInputStream)
   */
  public void setSource(RandomAccessInputStream stream) throws IOException {
    setSourceHelper(stream.getFileName());
    getParent().setSource(stream);
  }

  /*
   * @see ome.scifio.Reader#close(boolean)
   */
  public void close(boolean fileOnly) throws IOException {
    getParent().close(fileOnly);
  }

  /*
   * @see ome.scifio.Reader#close()
   */
  public void close() throws IOException {
    getParent().close();
  }

  /*
   * @see ome.scifio.Reader#readPlane(ome.scifio.io.RandomAccessInputStream, int, int, int, int, int, ome.scifio.Plane)
   */
  public Plane readPlane(RandomAccessInputStream s, int imageIndex, int x,
      int y, int w, int h, Plane plane) throws IOException {
    readPlaneHelper();
    return getParent().readPlane(s, imageIndex, x, y, w, h, plane);
  }

  /*
   * @see ome.scifio.Reader#readPlane(ome.scifio.io.RandomAccessInputStream, int, int, int, int, int, int, ome.scifio.Plane)
   */
  public Plane readPlane(RandomAccessInputStream s, int imageIndex, int x,
      int y, int w, int h, int scanlinePad, Plane plane) throws IOException {
    readPlaneHelper();
    return getParent().readPlane(s, imageIndex, x, y, w, h, scanlinePad, plane);
  }

  /*
   * @see ome.scifio.Reader#getPlaneCount(int)
   */
  public int getPlaneCount(int imageIndex) {
    return getParent().getPlaneCount(imageIndex);
  }

  /*
   * @see ome.scifio.Reader#getImageCount()
   */
  public int getImageCount() {
    return getParent().getImageCount();
  }

  /*
   * @see ome.scifio.Reader#createPlane(int, int, int, int)
   */
  public Plane createPlane(int xOffset, int yOffset, int xLength, int yLength) {
    return getParent().createPlane(xOffset, yOffset, xLength, yLength);
  }

  /*
   * @see ome.scifio.Reader#castToTypedPlane(ome.scifio.Plane)
   */
  public <P extends Plane> P castToTypedPlane(Plane plane) {
    return getParent().castToTypedPlane(plane);
  }
  
  // -- HasFormat API MEthods --

  /*
   * @see ome.scifio.HasFormat#getFormat()
   */
  public Format getFormat() {
    return getParent().getFormat();
  }
  
  // -- HasContext API Methods --
  
  /*
   * @see ome.scifio.HasContext#getContext()
   */
  public SCIFIO getContext() {
    return getParent().getContext();
  }
  
  /*
   * @see ome.scifio.HasContext#setContext(ome.scifio.SCIFIO)
   */
  public void setContext(SCIFIO ctx) {
    getParent().setContext(ctx);
  }
}