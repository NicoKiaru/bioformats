/*
 * #%L
 * OME Bio-Formats package for reading and converting biological file formats.
 * %%
 * Copyright (C) 2005 - 2017 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.formats.in;

import java.io.IOException;
import java.util.Hashtable;
import java.util.StringTokenizer;

import loci.common.Constants;
import loci.common.RandomAccessInputStream;

/**
 * SDTInfo encapsulates the header information for
 * Becker &amp; Hickl SPC-Image SDT files.
 *
 * @author Curtis Rueden ctrueden at wisc.edu
 */
public class SDTInfo {

  // -- Constants --

  public static final short BH_HEADER_CHKSUM = 0x55aa;
  public static final short BH_HEADER_NOT_VALID = 0x1111;
  public static final short BH_HEADER_VALID = 0x5555;

  /** For .set files (setup only). */
  public static final String SETUP_IDENTIFIER = "SPC Setup Script File";

  /** For normal .sdt files (setup + data). */
  public static final String DATA_IDENTIFIER = "SPC Setup & Data File";

  /**
   * For .sdt files created automatically in Continuous Flow mode measurement
   * (no setup, only data).
   */
  public static final String FLOW_DATA_IDENTIFIER = "SPC Flow Data File";

  /**
   * For .sdt files created using DLL function SPC_save_data_to_sdtfile
   * (no setup, only data).
   */
  public static final String DLL_DATA_IDENTIFIER = "SPC DLL Data File";

  /**
   * For .sdt files created in FIFO mode
   * (setup, data blocks = Decay, FCS, FIDA, FILDA &amp; MCS curves
   * for each used routing channel).
   */
  public static final String FCS_DATA_IDENTIFIER = "SPC FCS Data File";

  public static final String X_STRING = "#SP [SP_SCAN_X,I,";
  public static final String Y_STRING = "#SP [SP_SCAN_Y,I,";
  public static final String T_STRING = "#SP [SP_ADC_RE,I,";
  public static final String C_STRING = "#SP [SP_SCAN_RX,I,";
  
  public static final String X_IMG_STRING = "#SP [SP_IMG_X,I,";
  public static final String Y_IMG_STRING = "#SP [SP_IMG_Y,I,";

  public static final String BINARY_SETUP = "BIN_PARA_BEGIN:\0";

  // -- Fields --

  public int width, height, timeBins, channels, timepoints;
 
  // -- Fields - File header --

  /** Software revision number (lower 4 bits &gt;= 10(decimal)). */
  public short revision;

  /**
   * Offset of the info part which contains general text
   * information (Title, date, time, contents etc.).
   */
  public int infoOffs;

  /** Length of the info part. */
  public short infoLength;

  /**
   * Offset of the setup text data
   * (system parameters, display parameters, trace parameters etc.).
   */
  public int setupOffs;

  /** Length of the setup data. */
  public short setupLength;

  /** Offset of the first data block. */
  public int dataBlockOffs;

  /**
   * no_of_data_blocks valid only when in 0 .. 0x7ffe range,
   * if equal to 0x7fff  the  field 'reserved1' contains
   * valid no_of_data_blocks.
   */
  public short noOfDataBlocks;

  // length of the longest block in the file
  public int dataBlockLength;

  // offset to 1st. measurement description block
  // (system parameters connected to data blocks)
  public int measDescBlockOffs;

  // number of measurement description blocks
  public short noOfMeasDescBlocks;

  // length of the measurement description blocks
  public short measDescBlockLength;

  // valid: 0x5555, not valid: 0x1111
  public int headerValid;

  // reserved1 now contains noOfDataBlocks
  public long reserved1; // unsigned

  public int reserved2;

  // checksum of file header
  public int chksum;

  // -- Fields - File Info --

  public String info;

  // -- Fields -- Setup --

  public String setup;

  // -- Fields - MeasureInfo --

  public boolean hasMeasureInfo;

  /** Time of creation. */
  public String time;

  /** Date of creation. */
  public String date;

  /** Serial number of the module. */
  public String modSerNo;

  public short measMode;
  public float cfdLL;
  public float cfdLH;
  public float cfdZC;
  public float cfdHF;
  public float synZC;
  public short synFD;
  public float synHF;
  public float tacR;
  public short tacG;
  public float tacOF;
  public float tacLL;
  public float tacLH;
  public short adcRE;
  public short ealDE;
  public short ncx;
  public short ncy;
  public int page;
  public float colT;
  public float repT;
  public short stopt;
  public int overfl;
  public short useMotor;
  public int steps;
  public float offset;
  public short dither;
  public short incr;
  public short memBank;

  /** Module type. */
  public String modType;

  public float synTH;
  public short deadTimeComp;

  /** 2 = disabled line markers. */
  public short polarityL;

  public short polarityF;
  public short polarityP;

  /** Line predivider = 2 ** (linediv). */
  public short linediv;

  public short accumulate;
  public int flbckY;
  public int flbckX;
  public int bordU;
  public int bordL;
  public float pixTime;
  public short pixClk;
  public short trigger;
  public int scanX;
  public int scanY;
  public int scanRX;
  public int scanRY;
  public short fifoTyp;
  public int epxDiv;
  public int modTypeCode;

  /** New in v.8.4. */
  public int modFpgaVer;

  public float overflowCorrFactor;
  public int adcZoom;

  /** Cycles (accumulation cycles in FLOW mode). */
  public int cycles;

  // -- Fields - MeasStopInfo --

  public boolean hasMeasStopInfo;

  /** Last SPC_test_state return value (status). */
  public int status;

  /** Scan clocks bits 2-0 (frame, line, pixel), rates_read - bit 15. */
  public int flags;

  /**
   * Time from start to  - disarm (simple measurement)
   * - or to the end of the cycle (for complex measurement).
   */
  public float stopTime;

  /** Current step (if multi-step measurement). */
  public int curStep;

  /**
   * Current cycle (accumulation cycle in FLOW mode) -
   * (if multi-cycle measurement).
   */
  public int curCycle;

  /** Current measured page. */
  public int curPage;

  /** Minimum rates during the measurement. */
  public float minSyncRate;

  /** (-1.0 - not set). */
  public float minCfdRate;

  public float minTacRate;
  public float minAdcRate;

  /** Maximum rates during the measurement. */
  public float maxSyncRate;

  /** (-1.0 - not set). */
  public float maxCfdRate;

  public float maxTacRate;
  public float maxAdcRate;
  public int mReserved1;
  public float mReserved2;

  // -- Fields - MeasFCSInfo --

  public boolean hasMeasFCSInfo;

  /** Routing channel number. */
  public int chan;

  /**
   * Bit 0 = 1 - decay curve calculated.
   * Bit 1 = 1 - fcs   curve calculated.
   * Bit 2 = 1 - FIDA  curve calculated.
   * Bit 3 = 1 - FILDA curve calculated.
   * Bit 4 = 1 - MCS curve calculated.
   * Bit 5 = 1 - 3D Image calculated.
   */
  public int fcsDecayCalc;

  /** Macro time clock in 0.1 ns units. */
  public long mtResol; // unsigned

  /** Correlation time [ms]. */
  public float cortime;

  /** No of photons. */
  public long calcPhotons; // unsigned

  /** No of FCS values. */
  public int fcsPoints;

  /** Macro time of the last photon. */
  public float endTime;

  /**
   * No of Fifo overruns
   * when &gt; 0  fcs curve &amp; endTime are not valid.
   */
  public int overruns;

  /**
   * 0 - linear FCS with log binning (100 bins/log)
   * when bit 15 = 1 (0x8000) - Multi-Tau FCS
   * where bits 14-0 = ktau parameter.
   */
  public int fcsType;

  /**
   * Cross FCS routing channel number
   * when chan = crossChan and mod == crossMod - Auto FCS
   * otherwise - Cross FCS.
   */
  public int crossChan;

  /** Module number. */
  public int mod;

  /** Cross FCS module number. */
  public int crossMod;

  /** Macro time clock of cross FCS module in 0.1 ns units. */
  public long crossMtResol; // unsigned

  // -- Fields - extended MeasureInfo -

  public boolean hasExtendedMeasureInfo;

  /**
   * 4 subsequent fields valid only for Camera mode
   * or FIFO_IMAGE mode.
   */
  public int imageX;
  public int imageY;
  public int imageRX;
  public int imageRY;

  /** Gain for XY ADCs (SPC930). */
  public short xyGain;

  /** Use or not  Master Clock (SPC140 multi-module). */
  public short masterClock;

  /** ADC sample delay (SPC-930). */
  public short adcDE;

  /** Detector type (SPC-930 in camera mode). */
  public short detType;

  /** X axis representation (SPC-930). */
  public short xAxis;

  // -- Fields - MeasHISTInfo --

  public boolean hasMeasHISTInfo;

  /** Interval time [ms] for FIDA histogram. */
  public float fidaTime;

  /** Interval time [ms] for FILDA histogram. */
  public float fildaTime;

  /** No of FIDA values. */
  public int fidaPoints;

  /** No of FILDA values. */
  public int fildaPoints;

  /** Interval time [ms] for MCS histogram. */
  public float mcsTime;

  /** No of MCS values. */
  public int mcsPoints;

  // -- Fields - extended binary header --

  /** Number of MCS_TA points. */
  public int mcstaPoints;

  // -- Fields - BHFileBlockHeader --

  /**
   * Number of the block in the file.
   * Valid only when in 0..0x7ffe range, otherwise use lblock_no field
   * obsolete now, lblock_no contains full block no information.
   */
  public short blockNo;

  /** Offset of the data block from the beginning of the file. */
  public int dataOffs;

  /** Offset to the data block header of the next data block. */
  public int nextBlockOffs;

  public long[] allBlockOffsets;
  public long[] allBlockLengths;

  /** See blockType defines below. */
  public int blockType;

  /**
   * Number of the measurement description block
   * corresponding to this data block.
   */
  public short measDescBlockNo;

  /** Long blockNo - see remarks below. */
  public long lblockNo; // unsigned

  /** reserved2 now contains block (set) length. */
  public long blockLength; // unsigned
  
  // -- Constructor --

  /**
   * Constructs a new SDT header by reading values from the given input source,
   * populating the given metadata table.
   */
  public SDTInfo(RandomAccessInputStream in, Hashtable meta)
    throws IOException
  {
    // read bhfileHeader
    revision = in.readShort();
    infoOffs = in.readInt();
    infoLength = in.readShort();
    setupOffs = in.readInt();
    setupLength = in.readShort();
    dataBlockOffs = in.readInt();
    noOfDataBlocks = in.readShort();
    dataBlockLength = in.readInt();
    measDescBlockOffs = in.readInt();
    noOfMeasDescBlocks = in.readShort();
    measDescBlockLength = in.readShort();
    headerValid = in.readUnsignedShort();
    reserved1 = (0xffffffffL & in.readInt()); // unsigned
    reserved2 = in.readUnsignedShort();
    chksum = in.readUnsignedShort();

    // save bhfileHeader to metadata table
    if (meta != null) {
      final String bhfileHeader = "bhfileHeader.";
      meta.put(bhfileHeader + "revision", Short.valueOf(revision));
      meta.put(bhfileHeader + "infoOffs", Integer.valueOf(infoOffs));
      meta.put(bhfileHeader + "infoLength", Short.valueOf(infoLength));
      meta.put(bhfileHeader + "setupOffs", Integer.valueOf(setupOffs));
      meta.put(bhfileHeader + "dataBlockOffs", Integer.valueOf(dataBlockOffs));
      meta.put(bhfileHeader + "noOfDataBlocks", Short.valueOf(noOfDataBlocks));
      meta.put(bhfileHeader + "dataBlockLength",
        Integer.valueOf(dataBlockLength));
      meta.put(bhfileHeader + "measDescBlockOffs",
        Integer.valueOf(measDescBlockOffs));
      meta.put(bhfileHeader + "noOfMeasDescBlocks",
        Short.valueOf(noOfMeasDescBlocks));
      meta.put(bhfileHeader + "measDescBlockLength",
        Integer.valueOf(measDescBlockLength));
      meta.put(bhfileHeader + "headerValid", Integer.valueOf(headerValid));
      meta.put(bhfileHeader + "reserved1", Long.valueOf(reserved1));
      meta.put(bhfileHeader + "reserved2", Integer.valueOf(reserved2));
      meta.put(bhfileHeader + "chksum", Integer.valueOf(chksum));
    }

    // read file info
    in.seek(infoOffs);
    byte[] infoBytes = new byte[infoLength];
    in.readFully(infoBytes);
    info = new String(infoBytes, Constants.ENCODING);

    StringTokenizer st = new StringTokenizer(info, "\n");
    int count = st.countTokens();
    st.nextToken();
    String key = null, value = null;
    for (int i=1; i<count-1; i++) {
      String token = st.nextToken().trim();
      if (token.indexOf(':') == -1) continue;
      key = token.substring(0, token.indexOf(':')).trim();
      value = token.substring(token.indexOf(':') + 1).trim();
      meta.put(key, value);
    }

    // read setup
    in.seek(setupOffs);
    byte[] setupBytes = new byte[setupLength];
    in.readFully(setupBytes);
    setup = new String(setupBytes, Constants.ENCODING);

    int textEnd = setup.indexOf(BINARY_SETUP);
    if (textEnd > 0) {
      setup = setup.substring(0, textEnd);
      textEnd += BINARY_SETUP.length();
      in.seek(setupOffs + textEnd);
    }

    // variables to hold height & width read from header string for measMode 13
    int mode13width = 0;
    int mode13height = 0;

    st = new StringTokenizer(setup, "\n");
    while (st.hasMoreTokens()) {
      String token = st.nextToken().trim();

      if (token.startsWith("#SP") || token.startsWith("#DI") ||
        token.startsWith("#PR") || token.startsWith("#MP"))
      {
        int open = token.indexOf('[');
        key = token.substring(open + 1, token.indexOf(",", open));
        value = token.substring(token.lastIndexOf(",") + 1, token.length() - 1);
      }
      else if (token.startsWith("#TR") || token.startsWith("#WI")) {
        key = token.substring(0, token.indexOf('[')).trim();
        value = token.substring(token.indexOf('[') + 1, token.indexOf(']'));
      }

      if (key != null && value != null) meta.put(key, value);

      if (token.indexOf(X_STRING) != -1) {
        int ndx = token.indexOf(X_STRING) + X_STRING.length();
        int end = token.indexOf("]", ndx);
        width = Integer.parseInt(token.substring(ndx, end));
      }
      else if (token.indexOf(Y_STRING) != -1) {
        int ndx = token.indexOf(Y_STRING) + Y_STRING.length();
        int end = token.indexOf("]", ndx);
        height = Integer.parseInt(token.substring(ndx, end));
      }
      else if (token.indexOf(T_STRING) != -1) {
        int ndx = token.indexOf(T_STRING) + T_STRING.length();
        int end = token.indexOf("]", ndx);
        timeBins = Integer.parseInt(token.substring(ndx, end));
      }
      else if (token.indexOf(C_STRING) != -1) {
        int ndx = token.indexOf(C_STRING) + C_STRING.length();
        int end = token.indexOf("]", ndx);
        channels = Integer.parseInt(token.substring(ndx, end));
      }
      
      else if (token.indexOf(X_IMG_STRING) != -1) {
        int ndx = token.indexOf(X_IMG_STRING) + X_IMG_STRING.length();
        int end = token.indexOf("]", ndx);
        mode13width = Integer.parseInt(token.substring(ndx, end));
      }
      
      else if (token.indexOf(Y_IMG_STRING) != -1) {
        int ndx = token.indexOf(Y_IMG_STRING) + Y_IMG_STRING.length();
        int end = token.indexOf("]", ndx);
        mode13height = Integer.parseInt(token.substring(ndx, end));
      }
        
      
    }

    if (in.getFilePointer() < setupOffs + setupLength) {
      // BHBinHdr

      in.skipBytes(4);
      long baseOffset = in.getFilePointer();
      long softwareRevision = readUnsignedLong(in);
      long paramLength = readUnsignedLong(in);
      long reserved1 = readUnsignedLong(in);
      int reserved2 = in.readShort() & 0xffff;

      // SPCBinHdr

      long fcsOldOffset = readUnsignedLong(in);
      long fcsOldSize = readUnsignedLong(in);
      long gr1Offset = readUnsignedLong(in);
      long gr1Size = readUnsignedLong(in);
      long fcsOffset = readUnsignedLong(in);
      long fcsSize = readUnsignedLong(in);
      long fidaOffset = readUnsignedLong(in);
      long fidaSize = readUnsignedLong(in);
      long fildaOffset = readUnsignedLong(in);
      long fildaSize = readUnsignedLong(in);
      long gr2Offset = readUnsignedLong(in);
      int grNo = in.readShort() & 0xffff;
      int hstNo = in.readShort() & 0xffff;
      long hstOffset = readUnsignedLong(in);
      long gvdOffset = readUnsignedLong(in);
      int gvdSize = in.readShort() & 0xffff;
      int fitOffset = in.readShort() & 0xffff;
      int fitSize = in.readShort() & 0xffff;
      int extdevOffset = in.readShort() & 0xffff;
      int extdevSize = in.readShort() & 0xffff;
      long binhdrextOffset = readUnsignedLong(in);
      int binhdrextSize = in.readShort() & 0xffff;

      if (binhdrextOffset != 0) {
        in.seek(baseOffset + binhdrextOffset);
        long mcsImgOffset = readUnsignedLong(in);
        long mcsImgSize = readUnsignedLong(in);
        int momNo = in.readShort() & 0xffff;
        int momSize = in.readShort() & 0xffff;
        long momOffset = readUnsignedLong(in);
        long sysparExtOffset = readUnsignedLong(in);
        long sysparExtSize = readUnsignedLong(in);
        long mosaicOffset = readUnsignedLong(in);
        long mosaicSize = readUnsignedLong(in);
        // 52 longs reserved

        if (mcsImgOffset != 0) {
          in.seek(baseOffset + mcsImgOffset);

          int mcsActive = in.readInt();
          in.skipBytes(4); // Window
          mcstaPoints = in.readShort() & 0xffff;
          int mcstaFlags = in.readShort() & 0xffff;
          int mcstaTimePerPoint = in.readShort() & 0xffff;
          float mcsOffset = in.readFloat();
          float mcsTpp = in.readFloat();

          if (meta != null) {
            meta.put("MCS_TA.active", mcsActive);
            meta.put("MCS_TA.points", mcstaPoints);
            meta.put("MCS_TA.flags", mcstaFlags);
            meta.put("MCS_TA.time per point", mcstaTimePerPoint);
          }
        }
      }
    }

    // read measurement data
    if (noOfMeasDescBlocks > 0) {
      in.seek(measDescBlockOffs);

      hasMeasureInfo = measDescBlockLength >= 211;
      hasMeasStopInfo = measDescBlockLength >= 211 + 60;
      hasMeasFCSInfo = measDescBlockLength >= 211 + 60 + 38;
      hasExtendedMeasureInfo = measDescBlockLength >= 211 + 60 + 38 + 26;
      hasMeasHISTInfo = measDescBlockLength >= 211 + 60 + 38 + 26 + 24;

      if (hasMeasureInfo) {
        time = in.readString(9).trim();
        date = in.readString(11).trim();
        modSerNo = in.readString(16).trim();

        measMode = in.readShort();
        cfdLL = in.readFloat();
        cfdLH = in.readFloat();
        cfdZC = in.readFloat();
        cfdHF = in.readFloat();
        synZC = in.readFloat();
        synFD = in.readShort();
        synHF = in.readFloat();
        tacR = in.readFloat();
        tacG = in.readShort();
        tacOF = in.readFloat();
        tacLL = in.readFloat();
        tacLH = in.readFloat();
        adcRE = in.readShort();
        ealDE = in.readShort();
        ncx = in.readShort();
        ncy = in.readShort();
        page = in.readUnsignedShort();
        colT = in.readFloat();
        repT = in.readFloat();
        stopt = in.readShort();
        overfl = in.readUnsignedByte();
        useMotor = in.readShort();
        steps = in.readUnsignedShort();
        offset = in.readFloat();
        dither = in.readShort();
        incr = in.readShort();
        memBank = in.readShort();

        modType = in.readString(16).trim();

        synTH = in.readFloat();
        deadTimeComp = in.readShort();
        polarityL = in.readShort();
        polarityF = in.readShort();
        polarityP = in.readShort();
        linediv = in.readShort();
        accumulate = in.readShort();
        flbckY = in.readInt();
        flbckX = in.readInt();
        bordU = in.readInt();
        bordL = in.readInt();
        pixTime = in.readFloat();
        pixClk = in.readShort();
        trigger = in.readShort();
        scanX = in.readInt();
        scanY = in.readInt();
        scanRX = in.readInt();
        scanRY = in.readInt();
        fifoTyp = in.readShort();
        epxDiv = in.readInt();
        modTypeCode = in.readUnsignedShort();
        modFpgaVer = in.readUnsignedShort();
        overflowCorrFactor = in.readFloat();
        adcZoom = in.readInt();
        cycles = in.readInt();

        timepoints = stopt;

        // save MeasureInfo to metadata table
        if (meta != null) {
          final String measureInfo = "MeasureInfo.";
          meta.put(measureInfo + "time", time);
          meta.put(measureInfo + "date", date);
          meta.put(measureInfo + "modSerNo", modSerNo);
          meta.put(measureInfo + "measMode", Short.valueOf(measMode));
          meta.put(measureInfo + "cfdLL", Float.valueOf(cfdLL));
          meta.put(measureInfo + "cfdLH", Float.valueOf(cfdLH));
          meta.put(measureInfo + "cfdZC", Float.valueOf(cfdZC));
          meta.put(measureInfo + "cfdHF", Float.valueOf(cfdHF));
          meta.put(measureInfo + "synZC", Float.valueOf(synZC));
          meta.put(measureInfo + "synFD", Short.valueOf(synFD));
          meta.put(measureInfo + "synHF", Float.valueOf(synHF));
          meta.put(measureInfo + "tacR", Float.valueOf(tacR));
          meta.put(measureInfo + "tacG", Short.valueOf(tacG));
          meta.put(measureInfo + "tacOF", Float.valueOf(tacOF));
          meta.put(measureInfo + "tacLL", Float.valueOf(tacLL));
          meta.put(measureInfo + "tacLH", Float.valueOf(tacLH));
          meta.put(measureInfo + "adcRE", Short.valueOf(adcRE));
          meta.put(measureInfo + "ealDE", Short.valueOf(ealDE));
          meta.put(measureInfo + "ncx", Short.valueOf(ncx));
          meta.put(measureInfo + "ncy", Short.valueOf(ncy));
          meta.put(measureInfo + "page", Integer.valueOf(page));
          meta.put(measureInfo + "colT", Float.valueOf(colT));
          meta.put(measureInfo + "repT", Float.valueOf(repT));
          meta.put(measureInfo + "stopt", Short.valueOf(stopt));
          meta.put(measureInfo + "overfl", Integer.valueOf(overfl));
          meta.put(measureInfo + "useMotor", Short.valueOf(useMotor));
          meta.put(measureInfo + "steps", Integer.valueOf(steps));
          meta.put(measureInfo + "offset", Float.valueOf(offset));
          meta.put(measureInfo + "dither", Short.valueOf(dither));
          meta.put(measureInfo + "incr", Short.valueOf(incr));
          meta.put(measureInfo + "memBank", Short.valueOf(memBank));
          meta.put(measureInfo + "modType", modType);
          meta.put(measureInfo + "synTH", Float.valueOf(synTH));
          meta.put(measureInfo + "deadTimeComp", Short.valueOf(deadTimeComp));
          meta.put(measureInfo + "polarityL", Short.valueOf(polarityL));
          meta.put(measureInfo + "polarityF", Short.valueOf(polarityF));
          meta.put(measureInfo + "polarityP", Short.valueOf(polarityP));
          meta.put(measureInfo + "linediv", Short.valueOf(linediv));
          meta.put(measureInfo + "accumulate", Short.valueOf(accumulate));
          meta.put(measureInfo + "flbckY", Integer.valueOf(flbckY));
          meta.put(measureInfo + "flbckX", Integer.valueOf(flbckX));
          meta.put(measureInfo + "bordU", Integer.valueOf(bordU));
          meta.put(measureInfo + "bordL", Integer.valueOf(bordL));
          meta.put(measureInfo + "pixTime", Float.valueOf(pixTime));
          meta.put(measureInfo + "pixClk", Short.valueOf(pixClk));
          meta.put(measureInfo + "trigger", Short.valueOf(trigger));
          meta.put(measureInfo + "scanX", Integer.valueOf(scanX));
          meta.put(measureInfo + "scanY", Integer.valueOf(scanY));
          meta.put(measureInfo + "scanRX", Integer.valueOf(scanRX));
          meta.put(measureInfo + "scanRY", Integer.valueOf(scanRY));
          meta.put(measureInfo + "fifoTyp", Short.valueOf(fifoTyp));
          meta.put(measureInfo + "epxDiv", Integer.valueOf(epxDiv));
          meta.put(measureInfo + "modTypeCode", Integer.valueOf(modTypeCode));
          meta.put(measureInfo + "modFpgaVer", Integer.valueOf(modFpgaVer));
          meta.put(measureInfo + "overflowCorrFactor",
            Float.valueOf(overflowCorrFactor));
          meta.put(measureInfo + "adcZoom", Integer.valueOf(adcZoom));
          meta.put(measureInfo + "cycles", Integer.valueOf(cycles));
        }

        // extract dimensional parameters from measure info
        if (scanX > 0) width = scanX;
        if (scanY > 0) height = scanY;
        if (adcRE > 0) timeBins = adcRE;
        if (scanRX > 0) channels = scanRX;
        
        // measurement mode 0 and 1 are both single-point data
        if (measMode == 0 || measMode == 1)  {
          width = 1;
          height = 1;
        }  
        
        // for measurement_mode 13 one channel is stored in each block 
        // & width & height are not in scanX & scanY
        if (measMode == 13)  {
          width = mode13width;
          height = mode13height;
          channels = noOfMeasDescBlocks;  
        }
        
      }

      if (hasMeasStopInfo) {
        // MeasStopInfo - information collected when measurement is finished
        status = in.readUnsignedShort();
        flags = in.readUnsignedShort();
        stopTime = in.readFloat();
        curStep = in.readInt();
        curCycle = in.readInt();
        curPage = in.readInt();
        minSyncRate = in.readFloat();
        minCfdRate = in.readFloat();
        minTacRate = in.readFloat();
        minAdcRate = in.readFloat();
        maxSyncRate = in.readFloat();
        maxCfdRate = in.readFloat();
        maxTacRate = in.readFloat();
        maxAdcRate = in.readFloat();
        mReserved1 = in.readInt();
        mReserved2 = in.readFloat();

        // save MeasStopInfo to metadata table
        if (meta != null) {
          final String measStopInfo = "MeasStopInfo.";
          meta.put(measStopInfo + "status", Integer.valueOf(status));
          meta.put(measStopInfo + "flags", Integer.valueOf(flags));
          meta.put(measStopInfo + "stopTime", Float.valueOf(stopTime));
          meta.put(measStopInfo + "curStep", Integer.valueOf(curStep));
          meta.put(measStopInfo + "curCycle", Integer.valueOf(curCycle));
          meta.put(measStopInfo + "curPage", Integer.valueOf(curPage));
          meta.put(measStopInfo + "minSyncRate", Float.valueOf(minSyncRate));
          meta.put(measStopInfo + "minCfdRate", Float.valueOf(minCfdRate));
          meta.put(measStopInfo + "minTacRate", Float.valueOf(minTacRate));
          meta.put(measStopInfo + "minAdcRate", Float.valueOf(minAdcRate));
          meta.put(measStopInfo + "maxSyncRate", Float.valueOf(maxSyncRate));
          meta.put(measStopInfo + "maxCfdRate", Float.valueOf(maxCfdRate));
          meta.put(measStopInfo + "maxTacRate", Float.valueOf(maxTacRate));
          meta.put(measStopInfo + "maxAdcRate", Float.valueOf(maxAdcRate));
          meta.put(measStopInfo + "reserved1", Integer.valueOf(mReserved1));
          meta.put(measStopInfo + "reserved2", Float.valueOf(mReserved2));
        }
      }

      if (hasMeasFCSInfo) {
        // MeasFCSInfo - information collected when FIFO measurement is finished
        chan = in.readUnsignedShort();
        fcsDecayCalc = in.readUnsignedShort();
        mtResol = (0xffffffffL & in.readInt()); // unsigned
        cortime = in.readFloat();
        calcPhotons = (0xffffffffL & in.readInt()); // unsigned
        fcsPoints = in.readInt();
        endTime = in.readFloat();
        overruns = in.readUnsignedShort();
        fcsType = in.readUnsignedShort();
        crossChan = in.readUnsignedShort();
        mod = in.readUnsignedShort();
        crossMod = in.readUnsignedShort();
        crossMtResol = (0xffffffffL & in.readInt()); // unsigned

        // save MeasFCSInfo to metadata table
        if (meta != null) {
          final String measFCSInfo = "MeasFCSInfo.";
          meta.put(measFCSInfo + "chan", Integer.valueOf(chan));
          meta.put(measFCSInfo + "fcsDecayCalc", Integer.valueOf(fcsDecayCalc));
          meta.put(measFCSInfo + "mtResol", Long.valueOf(mtResol));
          meta.put(measFCSInfo + "cortime", Float.valueOf(cortime));
          meta.put(measFCSInfo + "calcPhotons", Long.valueOf(calcPhotons));
          meta.put(measFCSInfo + "fcsPoints", Integer.valueOf(fcsPoints));
          meta.put(measFCSInfo + "endTime", Float.valueOf(endTime));
          meta.put(measFCSInfo + "overruns", Integer.valueOf(overruns));
          meta.put(measFCSInfo + "fcsType", Integer.valueOf(fcsType));
          meta.put(measFCSInfo + "crossChan", Integer.valueOf(crossChan));
          meta.put(measFCSInfo + "mod", Integer.valueOf(mod));
          meta.put(measFCSInfo + "crossMod", Integer.valueOf(crossMod));
          meta.put(measFCSInfo + "crossMtResol", Float.valueOf(crossMtResol));
        }
      }

      if (hasExtendedMeasureInfo) {
        imageX = in.readInt();
        imageY = in.readInt();
        imageRX = in.readInt();
        imageRY = in.readInt();
        xyGain = in.readShort();
        masterClock = in.readShort();
        adcDE = in.readShort();
        detType = in.readShort();
        xAxis = in.readShort();

        // save extra MeasureInfo to metadata table
        if (meta != null) {
          final String measureInfo = "MeasureInfo.";
          meta.put(measureInfo + "imageX", Integer.valueOf(imageX));
          meta.put(measureInfo + "imageY", Integer.valueOf(imageY));
          meta.put(measureInfo + "imageRX", Integer.valueOf(imageRX));
          meta.put(measureInfo + "imageRY", Integer.valueOf(imageRY));
          meta.put(measureInfo + "xyGain", Short.valueOf(xyGain));
          meta.put(measureInfo + "masterClock", Short.valueOf(masterClock));
          meta.put(measureInfo + "adcDE", Short.valueOf(adcDE));
          meta.put(measureInfo + "detType", Short.valueOf(detType));
          meta.put(measureInfo + "xAxis", Short.valueOf(xAxis));
        }
      }

      if (hasMeasHISTInfo) {
        // MeasHISTInfo - extension of FCSInfo, valid only for FIFO meas
        // extension of MeasFCSInfo for other histograms (FIDA, FILDA, MCS)
        fidaTime = in.readFloat();
        fildaTime = in.readFloat();
        fidaPoints = in.readInt();
        fildaPoints = in.readInt();
        mcsTime = in.readFloat();
        mcsPoints = in.readInt();

        // save MeasHISTInfo to metadata table
        if (meta != null) {
          final String measHISTInfo = "MeasHISTInfo.";
          meta.put(measHISTInfo + "fidaTime", Float.valueOf(fidaTime));
          meta.put(measHISTInfo + "fildaTime", Float.valueOf(fildaTime));
          meta.put(measHISTInfo + "fidaPoints", Integer.valueOf(fidaPoints));
          meta.put(measHISTInfo + "fildaPoints", Integer.valueOf(fildaPoints));
          meta.put(measHISTInfo + "mcsTime", Float.valueOf(mcsTime));
          meta.put(measHISTInfo + "mcsPoints", Integer.valueOf(mcsPoints));
        }
      }
    }

    in.seek(dataBlockOffs);

    allBlockOffsets = new long[noOfDataBlocks];
    allBlockLengths = new long[noOfDataBlocks];

    for (int i=0; i<noOfDataBlocks; i++) {
      // read BHFileBlockHeader
      blockNo = in.readShort();
      dataOffs = in.readInt();
      nextBlockOffs = in.readInt();
      blockType = in.readUnsignedShort();
      measDescBlockNo = in.readShort();
      lblockNo = (0xffffffffL & in.readInt()); // unsigned
      int len = in.readInt();
      blockLength = (0xffffffffL & len); // unsigned

      allBlockOffsets[i] = in.getFilePointer();
      allBlockLengths[i] = blockLength;

      // save BHFileBlockHeader to metadata table
      if (meta != null) {
        final String bhFileBlockHeader = "BHFileBlockHeader.";
        meta.put(bhFileBlockHeader + "blockNo", Short.valueOf(blockNo));
        meta.put(bhFileBlockHeader + "dataOffs", Integer.valueOf(dataOffs));
        meta.put(bhFileBlockHeader + "nextBlockOffs",
          Integer.valueOf(nextBlockOffs));
        meta.put(bhFileBlockHeader + "blockType", Integer.valueOf(blockType));
        meta.put(bhFileBlockHeader + "measDescBlockNo",
          Short.valueOf(measDescBlockNo));
        meta.put(bhFileBlockHeader + "lblockNo", Long.valueOf(lblockNo));
        meta.put(bhFileBlockHeader + "blockLength", Long.valueOf(blockLength));
      }

      in.seek(nextBlockOffs);
    }
  }

  private long readUnsignedLong(RandomAccessInputStream in) throws IOException {
    return in.readInt() & 0xffffffffL;
  }

}
