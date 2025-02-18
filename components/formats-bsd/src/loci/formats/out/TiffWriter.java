/*
 * #%L
 * BSD implementations of Bio-Formats readers and writers
 * %%
 * Copyright (C) 2005 - 2020 Open Microscopy Environment:
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
 * #L%
 */

package loci.formats.out;

import java.io.IOException;
import loci.common.RandomAccessInputStream;
import loci.common.Region;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.FormatWriter;
import loci.formats.ImageTools;
import loci.formats.codec.CompressionType;
import loci.formats.gui.AWTImageTools;
import loci.formats.meta.MetadataRetrieve;
import loci.formats.tiff.IFD;
import loci.formats.tiff.TiffCompression;
import loci.formats.tiff.TiffConstants;
import loci.formats.tiff.TiffParser;
import loci.formats.tiff.TiffRational;
import loci.formats.tiff.TiffSaver;
import ome.units.quantity.Length;
import ome.units.UNITS;

/**
 * TiffWriter is the file format writer for TIFF files.
 */
public class TiffWriter extends FormatWriter {

  // -- Constants --

  public static final String COMPRESSION_UNCOMPRESSED =
    CompressionType.UNCOMPRESSED.getCompression();
  public static final String COMPRESSION_LZW =
    CompressionType.LZW.getCompression();
  public static final String COMPRESSION_J2K =
    CompressionType.J2K.getCompression();
  public static final String COMPRESSION_J2K_LOSSY =
    CompressionType.J2K_LOSSY.getCompression();
  public static final String COMPRESSION_JPEG =
    CompressionType.JPEG.getCompression();
  public static final String COMPRESSION_ZLIB =
    CompressionType.ZLIB.getCompression();

  private static final String[] BIG_TIFF_SUFFIXES = {"tf2", "tf8", "btf"};

  /** TIFF tiles must be of a height and width divisible by 16. */
  private static final int TILE_GRANULARITY = 16;

  // -- Fields --

  /** Whether or not the output file is a BigTIFF file. */
  protected boolean isBigTiff;

  /** Whether or not BigTIFF can be used automatically. */
  protected boolean canDetectBigTiff = true;

  /** The TiffSaver that will do most of the writing. */
  protected TiffSaver tiffSaver;

  /** Input stream to use when overwriting data. */
  protected RandomAccessInputStream in;

  /** Whether or not to check the parameters passed to saveBytes. */
  protected boolean checkParams = true;

  /** The tile width which will be used for writing. */
  protected int tileSizeX;

  /** The tile height which will be used for writing. */
  protected int tileSizeY;

  /**
   * Sets the compression code for the specified IFD.
   * 
   * @param ifd The IFD table to handle.
   */
  private void formatCompression(IFD ifd)
    throws FormatException
  {
    if (compression == null) compression = "";
    TiffCompression compressType = TiffCompression.UNCOMPRESSED;
    if (compression.equals(COMPRESSION_LZW)) {
      compressType = TiffCompression.LZW;
    }
    else if (compression.equals(COMPRESSION_J2K)) {
      compressType = TiffCompression.JPEG_2000;
    }
    else if (compression.equals(COMPRESSION_J2K_LOSSY)) {
      compressType = TiffCompression.JPEG_2000_LOSSY;
    }
    else if (compression.equals(COMPRESSION_JPEG)) {
      compressType = TiffCompression.JPEG;
    }
    else if (compression.equals(COMPRESSION_ZLIB)) {
      compressType = TiffCompression.DEFLATE;
    }
    Object v = ifd.get(IFD.COMPRESSION);
    if (v == null)
      ifd.put(IFD.COMPRESSION, compressType.getCode());
  }

  // -- Constructors --

  public TiffWriter() {
    this("Tagged Image File Format",
      new String[] {"tif", "tiff", "tf2", "tf8", "btf"});
  }

  public TiffWriter(String format, String[] exts) {
    super(format, exts);
    compressionTypes = new String[] {
      COMPRESSION_UNCOMPRESSED,
      COMPRESSION_LZW,
      COMPRESSION_J2K,
      COMPRESSION_J2K_LOSSY,
      COMPRESSION_JPEG,
      COMPRESSION_ZLIB
    };
    isBigTiff = false;
  }

  // -- FormatWriter API methods --

  /* @see loci.formats.FormatWriter#setId(String) */
  @Override
  public void setId(String id) throws FormatException, IOException {
    super.setId(id);

    // if a BigTIFF extension is used, or we know that
    // more than 4GB of data will be written, then automatically
    // switch to BigTIFF
    if (!isBigTiff) {
      if (checkSuffix(id, BIG_TIFF_SUFFIXES)) {
        LOGGER.info("Switching to BigTIFF (by file extension)");
        isBigTiff = true;
      }
      else if (compression == null || compression.equals(COMPRESSION_UNCOMPRESSED)) {
        MetadataRetrieve retrieve = getMetadataRetrieve();
        long totalBytes = 0;
        for (int i=0; i<retrieve.getImageCount(); i++) {
          int sizeX = retrieve.getPixelsSizeX(i).getValue();
          int sizeY = retrieve.getPixelsSizeY(i).getValue();
          int sizeZ = retrieve.getPixelsSizeZ(i).getValue();
          int sizeC = retrieve.getPixelsSizeC(i).getValue();
          int sizeT = retrieve.getPixelsSizeT(i).getValue();
          int type = FormatTools.pixelTypeFromString(
            retrieve.getPixelsType(i).toString());
          long bpp = FormatTools.getBytesPerPixel(type);
          totalBytes += (long)sizeX * (long)sizeY * (long)sizeZ * (long)sizeC * (long)sizeT * bpp;
        }

        if (totalBytes >= TiffConstants.BIG_TIFF_CUTOFF) {
          if (canDetectBigTiff) {
            LOGGER.info("Switching to BigTIFF (by file size)");
            isBigTiff = true;
          }
          else {
            LOGGER.info("Automatic BigTIFF disabled but pixel byte count = {}",
              totalBytes);
          }
        }
      }
    }

    synchronized (this) {
      setupTiffSaver();
    }
  }

  // -- TiffWriter API methods --

  /**
   * Saves the given image to the specified (possibly already open) file.
   * The IFD hashtable allows specification of TIFF parameters such as bit
   * depth, compression and units. Use one IFD instance per plane.
   */
  public void saveBytes(int no, byte[] buf, IFD ifd)
    throws IOException, FormatException
  {
    int w = getSizeX();
    int h = getSizeY();
    saveBytes(no, buf, ifd, 0, 0, w, h);
  }

  /**
   * Saves the given image to the specified series in the current file.
   * The IFD hashtable allows specification of TIFF parameters such as bit
   * depth, compression and units. Use one IFD instance per plane.
   */
  public void saveBytes(int no, byte[] buf, IFD ifd, int x, int y, int w, int h)
    throws IOException, FormatException
  {
    if (checkParams) checkParams(no, buf, x, y, w, h);
    if (ifd == null) ifd = new IFD();
    MetadataRetrieve retrieve = getMetadataRetrieve();
    int type = FormatTools.pixelTypeFromString(
        retrieve.getPixelsType(series).toString());
    int index = no;
    int currentTileSizeX = getTileSizeX();
    int currentTileSizeY = getTileSizeY();
    boolean usingTiling = currentTileSizeX > 0 && currentTileSizeY > 0;
    if (usingTiling) {
      ifd.put(IFD.TILE_WIDTH, Long.valueOf(currentTileSizeX));
      ifd.put(IFD.TILE_LENGTH, Long.valueOf(currentTileSizeY));
    }
    if (usingTiling && (currentTileSizeX < w || currentTileSizeY < h)) {
      int numTilesX = (w + (x % currentTileSizeX) + currentTileSizeX - 1) / currentTileSizeX;
      int numTilesY = (h + (y % currentTileSizeY) + currentTileSizeY - 1) / currentTileSizeY;
      for (int yTileIndex = 0; yTileIndex < numTilesY; yTileIndex++) {
        for (int xTileIndex = 0; xTileIndex < numTilesX; xTileIndex++) {
          Region tileParams = new Region();
          tileParams.width = xTileIndex < numTilesX - 1 ? currentTileSizeX - (x % currentTileSizeX) : w - (currentTileSizeX * xTileIndex);
          tileParams.height = yTileIndex < numTilesY - 1 ? currentTileSizeY - (y % currentTileSizeY) : h - (currentTileSizeY * yTileIndex);
          tileParams.x = x + (xTileIndex * currentTileSizeX) - (xTileIndex > 0 ? (x % currentTileSizeX) : 0);
          tileParams.y = y + (yTileIndex * currentTileSizeY) - (yTileIndex > 0 ? (y % currentTileSizeY) : 0);
          byte [] tileBuf = getTile(buf, tileParams, new Region(x, y, w, h));

          // This operation is synchronized
          synchronized (this) {
            // This operation is synchronized against the TIFF saver.
            synchronized (tiffSaver) {
              index = prepareToWriteImage(no, tileBuf, ifd, tileParams.x, tileParams.y, tileParams.width, tileParams.height);
              if (index == -1) {
                return;
              }
            }
          }

          boolean lastPlane = no == getPlaneCount() - 1;
          boolean lastSeries = getSeries() == retrieve.getImageCount() - 1;
          boolean lastResolution = getResolution() == getResolutionCount() - 1;
          tiffSaver.writeImage(tileBuf, ifd, index, type, tileParams.x, tileParams.y, tileParams.width, tileParams.height,
            lastPlane && lastSeries && lastResolution);
        }
      }
    }
    else {
      // This operation is synchronized
      synchronized (this) {
        // This operation is synchronized against the TIFF saver.
        synchronized (tiffSaver) {
          index = prepareToWriteImage(no, buf, ifd, x, y, w, h);
          if (index == -1) {
            return;
          }
        }
      }

      boolean lastPlane = no == getPlaneCount() - 1;
      boolean lastSeries = getSeries() == retrieve.getImageCount() - 1;
      boolean lastResolution = getResolution() == getResolutionCount() - 1;
      tiffSaver.writeImage(buf, ifd, index, type, x, y, w, h,
        lastPlane && lastSeries && lastResolution);
    }
  }

  /**
   * Performs the preparation for work prior to the usage of the TIFF saver.
   * This method is factored out from <code>saveBytes()</code> in an attempt to
   * ensure thread safety.
   */
  protected int prepareToWriteImage(
      int no, byte[] buf, IFD ifd, int x, int y, int w, int h)
  throws IOException, FormatException {
    MetadataRetrieve retrieve = getMetadataRetrieve();
    boolean littleEndian = false;
    if (retrieve.getPixelsBigEndian(series) != null) {
      littleEndian = !retrieve.getPixelsBigEndian(series).booleanValue();
    }
    else if (retrieve.getPixelsBinDataCount(series) == 0) {
      littleEndian = !retrieve.getPixelsBinDataBigEndian(series, 0).booleanValue();
    }

    // Ensure that no more than one thread manipulated the initialized array
    // at one time.
    synchronized (this) {
      if (!initialized[series][no]) {
        initialized[series][no] = true;

        try (RandomAccessInputStream tmp = createInputStream()) {
          tmp.order(littleEndian);
          if (tmp.length() == 0) {
            synchronized (this) {
              // write TIFF header
              tiffSaver.writeHeader();
            }
          }
        }
      }
    }

    int c = getSamplesPerPixel();
    int type = FormatTools.pixelTypeFromString(
      retrieve.getPixelsType(series).toString());
    int bytesPerPixel = FormatTools.getBytesPerPixel(type);

    int blockSize = w * h * c * bytesPerPixel;
    if (blockSize > buf.length) {
      c = buf.length / (w * h * bytesPerPixel);
    }

    formatCompression(ifd);
    byte[][] lut = AWTImageTools.get8BitLookupTable(cm);
    if (lut != null) {
      int[] colorMap = new int[lut.length * lut[0].length];
      for (int i=0; i<lut.length; i++) {
        for (int j=0; j<lut[0].length; j++) {
          colorMap[i * lut[0].length + j] = (int) ((lut[i][j] & 0xff) << 8);
        }
      }
      ifd.putIFDValue(IFD.COLOR_MAP, colorMap);
    }
    else {
      short[][] lut16 = AWTImageTools.getLookupTable(cm);
      if (lut16 != null) {
        int[] colorMap = new int[lut16.length * lut16[0].length];
        for (int i=0; i<lut16.length; i++) {
          for (int j=0; j<lut16[0].length; j++) {
            colorMap[i * lut16[0].length + j] = (int) (lut16[i][j] & 0xffff);
          }
        }
        ifd.putIFDValue(IFD.COLOR_MAP, colorMap);
      }
    }

    int width = getSizeX();
    int height = getSizeY();
    ifd.put(IFD.IMAGE_WIDTH, Long.valueOf(width));
    ifd.put(IFD.IMAGE_LENGTH, Long.valueOf(height));

    Length px = retrieve.getPixelsPhysicalSizeX(series);
    Double physicalSizeX = px == null || px.value(UNITS.MICROMETER) == null ? null : px.value(UNITS.MICROMETER).doubleValue();
    if (physicalSizeX == null || physicalSizeX.doubleValue() == 0) {
      physicalSizeX = 0d;
    }
    else physicalSizeX = 1d / physicalSizeX;

    Length py = retrieve.getPixelsPhysicalSizeY(series);
    Double physicalSizeY = py == null || py.value(UNITS.MICROMETER) == null ? null : py.value(UNITS.MICROMETER).doubleValue();
    if (physicalSizeY == null || physicalSizeY.doubleValue() == 0) {
      physicalSizeY = 0d;
    }
    else physicalSizeY = 1d / physicalSizeY;

    ifd.put(IFD.RESOLUTION_UNIT, 3);
    ifd.put(IFD.X_RESOLUTION,
      new TiffRational((long) (physicalSizeX * 1000 * 10000), 1000));
    ifd.put(IFD.Y_RESOLUTION,
      new TiffRational((long) (physicalSizeY * 1000 * 10000), 1000));

    if (!isBigTiff) {
      isBigTiff = (out.length() + 2
          * (width * height * c * bytesPerPixel)) >= 4294967296L;
      if (isBigTiff) {
        throw new FormatException("File is too large; call setBigTiff(true)");
      }
    }

    // write the image
    ifd.put(IFD.LITTLE_ENDIAN, Boolean.valueOf(littleEndian));
    if (!ifd.containsKey(IFD.REUSE)) {
      ifd.put(IFD.REUSE, out.length());
      out.seek(out.length());
    }
    else {
      out.seek((Long) ifd.get(IFD.REUSE));
    }

    ifd.putIFDValue(IFD.PLANAR_CONFIGURATION,
      interleaved || getSamplesPerPixel() == 1 ? 1 : 2);

    int sampleFormat = 1;
    if (FormatTools.isSigned(type)) sampleFormat = 2;
    if (FormatTools.isFloatingPoint(type)) sampleFormat = 3;
    ifd.putIFDValue(IFD.SAMPLE_FORMAT, sampleFormat);

    int channels = retrieve.getPixelsSizeC(series).getValue().intValue();
    int z = retrieve.getPixelsSizeZ(series).getValue().intValue();
    int t = retrieve.getPixelsSizeT(series).getValue().intValue();
    ifd.putIFDValue(IFD.IMAGE_DESCRIPTION,
      "ImageJ=\nhyperstack=true\nimages=" + (channels * z * t) + "\nchannels=" +
      channels + "\nslices=" + z + "\nframes=" + t);

    int index = (no * getResolutionCount()) + getResolution();
    int currentSeries = getSeries();
    int currentResolution = getResolution();
    for (int i=0; i<currentSeries; i++) {
      setSeries(i);
      index += (getPlaneCount() * getResolutionCount());
    }
    setSeries(currentSeries);
    setResolution(currentResolution);
    return index;
  }

  // -- FormatWriter API methods --

  /* (non-Javadoc)
   * @see loci.formats.FormatWriter#close()
   */
  @Override
  public void close() throws IOException {
    super.close();
    if (in != null) {
      in.close();
    }
    if (tiffSaver != null) {
      tiffSaver.close();
    }
  }

  /* @see loci.formats.FormatWriter#getPlaneCount() */
  @Override
  public int getPlaneCount() {
    return getPlaneCount(series);
  }

  // -- IFormatWriter API methods --

  /**
   * @see loci.formats.IFormatWriter#saveBytes(int, byte[], int, int, int, int)
   */
  @Override
  public void saveBytes(int no, byte[] buf, int x, int y, int w, int h)
    throws FormatException, IOException
  {
    IFD ifd = new IFD();
    if (!sequential) {
      try (RandomAccessInputStream stream = new RandomAccessInputStream(currentId)) {
        TiffParser parser = new TiffParser(stream);
        long[] ifdOffsets = parser.getIFDOffsets();
        if (no < ifdOffsets.length) {
          ifd = parser.getIFD(ifdOffsets[no]);
        }
        saveBytes(no, buf, ifd, x, y, w, h);
      }
    }
    else {
      saveBytes(no, buf, ifd, x, y, w, h);
    }
  }

  /* @see loci.formats.IFormatWriter#canDoStacks(String) */
  @Override
  public boolean canDoStacks() { return true; }

  /* @see loci.formats.IFormatWriter#getPixelTypes(String) */
  @Override
  public int[] getPixelTypes(String codec) {
    if (codec != null && codec.equals(COMPRESSION_JPEG)) {
      return new int[] {FormatTools.INT8, FormatTools.UINT8,
        FormatTools.INT16, FormatTools.UINT16};
    }
    else if (codec != null && codec.equals(COMPRESSION_J2K)) {
      return new int[] {FormatTools.INT8, FormatTools.UINT8,
        FormatTools.INT16, FormatTools.UINT16, FormatTools.INT32,
        FormatTools.UINT32, FormatTools.FLOAT};
    }
    return new int[] {FormatTools.INT8, FormatTools.UINT8, FormatTools.INT16,
      FormatTools.UINT16, FormatTools.INT32, FormatTools.UINT32,
      FormatTools.FLOAT, FormatTools.DOUBLE};
  }

  // -- TiffWriter API methods --

  /**
   * Sets whether or not BigTIFF files should be written.
   * This flag is not reset when close() is called.
   */
  public void setBigTiff(boolean bigTiff) {
    FormatTools.assertId(currentId, false, 1);
    isBigTiff = bigTiff;
  }

  /**
   * Sets whether or not BigTIFF can be used automatically
   * based upon the input data size (true by default).
   * This flag is not reset when close() is called.
   */
  public void setCanDetectBigTiff(boolean detect) {
    FormatTools.assertId(currentId, false, 1);
    canDetectBigTiff = detect;
  }

  // -- Helper methods --

  protected void setupTiffSaver() throws IOException {
    out.close();
    out = createOutputStream();
    tiffSaver = createTiffSaver();

    MetadataRetrieve retrieve = getMetadataRetrieve();
    boolean littleEndian = false;
    if (retrieve.getPixelsBigEndian(series) != null) {
      littleEndian = !retrieve.getPixelsBigEndian(series).booleanValue();
    }
    else if (retrieve.getPixelsBinDataCount(series) == 0) {
      littleEndian = !retrieve.getPixelsBinDataBigEndian(series, 0).booleanValue();
    }

    tiffSaver.setWritingSequentially(sequential);
    tiffSaver.setLittleEndian(littleEndian);
    tiffSaver.setBigTiff(isBigTiff);
    tiffSaver.setCodecOptions(options);
  }

  @Override
  public int getTileSizeX() throws FormatException {
    if (tileSizeX == 0) {
      return super.getTileSizeX();
    }
    return tileSizeX;
  }

  @Override
  public int setTileSizeX(int tileSize) throws FormatException {
    tileSizeX = super.setTileSizeX(tileSize);
    if (tileSize == 0) {
      tileSizeX = 0;
    }
    else if (tileSize < TILE_GRANULARITY) {
      tileSizeX = TILE_GRANULARITY;
    }
    else {
      tileSizeX = Math.round((float)tileSize/TILE_GRANULARITY) * TILE_GRANULARITY;
    }
    return tileSizeX;
  }

  @Override
  public int getTileSizeY() throws FormatException {
    if (tileSizeY == 0) {
      return super.getTileSizeY();
    }
    return tileSizeY;
  }

  @Override
  public int setTileSizeY(int tileSize) throws FormatException {
    tileSizeY = super.setTileSizeY(tileSize);
    if (tileSize == 0) {
      tileSizeY = 0;
    }
    else if (tileSize < TILE_GRANULARITY) {
      tileSizeY = TILE_GRANULARITY;
    }
    else {
      tileSizeY = Math.round((float)tileSize/TILE_GRANULARITY) * TILE_GRANULARITY;
    }
    return tileSizeY;
  }

  private byte[] getTile(byte[] buf, Region tileParams, Region srcParams) {
    MetadataRetrieve retrieve = getMetadataRetrieve();
    int type = FormatTools.pixelTypeFromString(retrieve.getPixelsType(series).toString());
    int channel_count = getSamplesPerPixel();
    int bytesPerPixel = FormatTools.getBytesPerPixel(type);
    int tileSize = tileParams.width * tileParams.height * bytesPerPixel * channel_count;
    byte [] returnBuf = new byte[tileSize];

    for (int row = tileParams.y; row != tileParams.y + tileParams.height; row++) {
      for (int sampleoffset = 0; sampleoffset < (tileParams.width * channel_count); sampleoffset++) {
        int channel_index = sampleoffset / tileParams.width;
        int channel_offset = (sampleoffset - (tileParams.width * channel_index)) * bytesPerPixel;
        int full_row_width = srcParams.width * bytesPerPixel;
        int full_plane_size = full_row_width * srcParams.height;
        int xoffset = (tileParams.x - srcParams.x) * bytesPerPixel;
        int yoffset = (row - srcParams.y) * full_row_width;
        int row_offset = (row - tileParams.y) * tileParams.width * bytesPerPixel;
        int src_index = yoffset + xoffset + channel_offset + (channel_index * full_plane_size);
        int dest_index = (tileParams.height * tileParams.width * channel_index * bytesPerPixel) + row_offset;
        for (int pixelByte = 0; pixelByte < bytesPerPixel; pixelByte++) {
          returnBuf[dest_index + channel_offset + pixelByte] = buf[src_index + pixelByte];
        }
      }
    }
    return returnBuf;
  }
  
  protected RandomAccessInputStream createInputStream() throws IOException {
    return new RandomAccessInputStream(currentId);
  }
  
  protected TiffSaver createTiffSaver() {
    return new TiffSaver(out, currentId);
  }

}
