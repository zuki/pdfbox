/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pdfbox.pdmodel.font;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.fontbox.ttf.*;
import org.apache.fontbox.cmap.CMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.util.StringUtil;

/**
 * This is an implementation for the TrueType Type 0 font
 *     supporting overall unicode and font embedding.
 *
 * @author Keiji Suzuki
 *
 */
public class PDType0UnicodeFont extends PDType0Font
{
    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDType0UnicodeFont.class);

    /** Document where this font is used */
    private PDDocument doc;

    /** Prefix of the embedded font */
    private String prefix;

    /** Font name of the embedded font*/
    private String bfname;

    /** Codes used in wrriten text */
    private TreeSet<Integer> usedCodes = new TreeSet<Integer>();

    /** Embedded font */
    private TTFSubsetter subFont;

    /** Cmap for unicode */
    private CmapSubtable unicodeCmap;

    /** font widths of original font */
    private int[] widths;

    /**
     * Loads a TTF to be embedded into a document.
     *
     * @param doc The PDF document that will hold the embedded font.
     * @param file a type0 ttf file.
     * @return a PDType0UnicodeFont instance.
     * @throws IOException If there is an error loading the data.
     */
    public static PDType0UnicodeFont load(PDDocument doc, String fontPath) throws IOException
    {
        return new PDType0UnicodeFont(doc, fontPath);
    }

    /**
     * Constructor.
     *
     * @param fontpath The absolute path of original font. When use ttc font,
     *          add 'comma' and the index of using font after the font path.
     * @param isSerif Does the font have serif
     *
     * @throws IOException If an error occures while font parsing.
     */
    private PDType0UnicodeFont(PDDocument doc, String fontPath) throws IOException
    {
        super();
        this.doc = doc;
        this.prefix = getPrefix();

        int fontIndex = -1;
        int ttcPos = fontPath.toLowerCase().indexOf("ttc");
        if (ttcPos > -1)
        {
            fontIndex = Integer.valueOf(fontPath.substring(ttcPos+4));
            fontPath = fontPath.substring(0, ttcPos+3);
        }

        RAFDataStream raf = new RAFDataStream(new File(fontPath), "r");
        TTFParser parser = new TTFParser(true, true);
        TrueTypeFont ttf = null;
        if (fontIndex > -1)
        {
            parser.parseTTC(raf);
            ttf = parser.parse(raf, fontIndex);
        }
        else
        {
            ttf = parser.parse(raf);
        }

        OS2WindowsMetricsTable os2 = ttf.getOS2Windows();
        if (!os2.permitEmbedding()) {
            throw new IOException("This font is not permitted embedding.");
        }
        subFont = new TTFSubsetter(ttf, prefix);

        PDFontDescriptor fd = new PDFontDescriptor();
        loadDescriptorDictionary(ttf, fd);

        COSDictionary font = new COSDictionary();
        font.setItem(COSName.TYPE, COSName.FONT);
        font.setItem(COSName.SUBTYPE, COSName.CID_FONT_TYPE2);
        font.setName(COSName.BASE_FONT, bfname);
        font.setItem(COSName.CIDSYSTEMINFO,
            PDCIDSystemInfo.ADOBE_IDENTITY_0.getCIDSystemInfo());
        font.setItem(COSName.FONT_DESC, fd.getCOSObject());

        COSArray descFont = new COSArray();
        descFont.add(font);

        dict.setItem(COSName.SUBTYPE, COSName.TYPE0);
        dict.setName(COSName.BASE_FONT, bfname);
        dict.setItem(COSName.ENCODING, COSName.IDENTITY_H);
        dict.setItem(COSName.DESCENDANT_FONTS, descFont);
        readEncoding();
        fetchCMapUCS2();

        PDCIDFont desFont = new PDCIDFontType2(font, this, ttf);
        setDescendantFont(desFont);
    }

    /**
     *  Reload embedded font with used codes. You must call this method
     *    before calling doc.save()
     *
     *  @throws IOException
     */
    public void reloadFont()  throws IOException
    {
        int[] used = getUsedCodes();
        for (int cp : used)
        {
            subFont.addCharCode(cp);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        subFont.writeToStream(bos);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        PDStream fontStream = new PDStream(doc, bis);
        fontStream.addCompression();
        fontStream.getStream().setInt(COSName.LENGTH1, fontStream.getByteArray().length);

        PDCIDFontType2 descendantFont = (PDCIDFontType2) getDescendantFont();
        PDFontDescriptor fd = (PDFontDescriptor) descendantFont.getFontDescriptor();
        fd.resetFontFile2(fontStream);


        InputStream stream = fontStream.createInputStream();
        TrueTypeFont ttf = null;
        try
        {
            TTFParser parser = new TTFParser(true, true);
            ttf = parser.parse(stream);

            CmapSubtable gidMap = getUnicodeCmap(ttf.getCmap());
            Map<Integer, Integer> unicode2cid = new LinkedHashMap<Integer, Integer>();
            Map<Integer, Integer> cid2gid = new HashMap<Integer, Integer>();
            Map<Integer, Integer> gid2cid = new HashMap<Integer, Integer>();
            TreeSet<Integer> gidset = new TreeSet<Integer>();
            int maxcid = Integer.MIN_VALUE;
            for (int i=0, len=used.length; i<len; i++)
            {
                int unicode = used[i];
                int cid = unicodeCmap.getGlyphId(unicode);
                int gid = gidMap.getGlyphId(unicode);
                unicode2cid.put(unicode, cid);
                cid2gid.put(cid, gid);
                gid2cid.put(gid, cid);
                gidset.add(gid);
                if (cid > maxcid)
                {
                    maxcid = cid;
                }
            }

            HeaderTable header = ttf.getHeader();
            float scaling = 1000f / header.getUnitsPerEm();

            HorizontalMetricsTable hmtx = ttf.getHorizontalMetrics();
            int[] subwidths = hmtx.getAdvanceWidth();
            StringBuilder sb = new StringBuilder();
            for (Integer gid : gidset)
            {
                // subwidths[0] is the width of charCode = 0
                int idx = (gid > subwidths.length - 2) ? (subwidths.length - 2) : gid;
                sb.append(" ").append(gid2cid.get(gid))
                  .append(" ").append(Math.round(subwidths[idx] * scaling));
            }
            COSArray wArray = getFontWidthsArray(sb.toString().substring(1));
            descendantFont.resetFontWidths(wArray);
            descendantFont.resetCID2GID(getCIDToGID(maxcid, cid2gid));

            resetToUnicode(getToUnicode(unicode2cid));
        }
        finally
        {
            if (ttf != null)
            {
                ttf.close();
            }
        }

    }

    private void loadDescriptorDictionary(TrueTypeFont ttf, PDFontDescriptor fd) throws IOException
    {
        NamingTable naming = ttf.getNaming();
        List<NameRecord> records = naming.getNameRecords();
        for (NameRecord nr : records)
        {
            if (nr.getNameId() == NameRecord.NAME_POSTSCRIPT_NAME)
            {
                bfname = prefix + nr.getString();
                fd.setFontName(bfname);
                break;
            }
        }

        OS2WindowsMetricsTable os2 = ttf.getOS2Windows();
        boolean isSymbolic = false;
        switch (os2.getFamilyClass())
        {
            case OS2WindowsMetricsTable.FAMILY_CLASS_SYMBOLIC:
                isSymbolic = true;
                break;
            case OS2WindowsMetricsTable.FAMILY_CLASS_SCRIPTS:
                fd.setScript(true);
                break;
            case OS2WindowsMetricsTable.FAMILY_CLASS_CLAREDON_SERIFS:
            case OS2WindowsMetricsTable.FAMILY_CLASS_FREEFORM_SERIFS:
            case OS2WindowsMetricsTable.FAMILY_CLASS_MODERN_SERIFS:
            case OS2WindowsMetricsTable.FAMILY_CLASS_OLDSTYLE_SERIFS:
            case OS2WindowsMetricsTable.FAMILY_CLASS_SLAB_SERIFS:
                fd.setSerif(true);
                break;
            default:
                // do nothing
        }
        switch (os2.getWidthClass())
        {
            case OS2WindowsMetricsTable.WIDTH_CLASS_ULTRA_CONDENSED:
                fd.setFontStretch("UltraCondensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_EXTRA_CONDENSED:
                fd.setFontStretch("ExtraCondensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_CONDENSED:
                fd.setFontStretch("Condensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_SEMI_CONDENSED:
                fd.setFontStretch("SemiCondensed");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_MEDIUM:
                fd.setFontStretch("Normal");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_SEMI_EXPANDED:
                fd.setFontStretch("SemiExpanded");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_EXPANDED:
                fd.setFontStretch("Expanded");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_EXTRA_EXPANDED:
                fd.setFontStretch("ExtraExpanded");
                break;
            case OS2WindowsMetricsTable.WIDTH_CLASS_ULTRA_EXPANDED:
                fd.setFontStretch("UltraExpanded");
                break;
            default:
                // do nothing
        }
        fd.setFontWeight(os2.getWeightClass());
        fd.setSymbolic(isSymbolic);
        fd.setNonSymbolic(!isSymbolic);
        HeaderTable header = ttf.getHeader();
        PDRectangle rect = new PDRectangle();
        float scaling = 1000f / header.getUnitsPerEm();
        rect.setLowerLeftX(Math.round(header.getXMin() * scaling));
        rect.setLowerLeftY(Math.round(header.getYMin() * scaling));
        rect.setUpperRightX(Math.round(header.getXMax() * scaling));
        rect.setUpperRightY(Math.round(header.getYMax() * scaling));
        fd.setFontBoundingBox(rect);

        HorizontalHeaderTable hHeader = ttf.getHorizontalHeader();
        fd.setAscent(Math.round(hHeader.getAscender() * scaling));
        fd.setDescent(Math.round(hHeader.getDescender() * scaling));


        PostScriptTable ps = ttf.getPostScript();
        fd.setFixedPitch(ps.getIsFixedPitch() > 0);
        fd.setItalicAngle(ps.getItalicAngle());

    /*
        GlyphTable glyphTable = ttf.getGlyph();
        GlyphData[] glyphs = glyphTable.getGlyphs();

        String[] names = ps.getGlyphNames();
        if (names != null)
        {
            for (int i = 0; i < names.length; i++)
            {
                // if we have a capital H then use that, otherwise use the
                // tallest letter
                if (names[i].equals("H"))
                {
                    fd.setCapHeight(Math.round(glyphs[i].getBoundingBox().getUpperRightY() / scaling));
                }
                if (names[i].equals("x"))
                {
                    fd.setXHeight(Math.round(glyphs[i].getBoundingBox().getUpperRightY() / scaling));
                }
            }
        }
    */

        // hmm there does not seem to be a clear definition for StemV,
        // this is close enough and I am told it doesn't usually get used.
        fd.setStemV(Math.round(fd.getFontBoundingBox().getWidth() * .13f));

        fd.setFlags(fd.getFlags());

        unicodeCmap = getUnicodeCmap(ttf.getCmap());
        if (unicodeCmap == null) {
            throw new IOException("Does not include unicode cmap");
        }

        HorizontalMetricsTable hmtx = ttf.getHorizontalMetrics();
        widths = hmtx.getAdvanceWidth();
        for (int i=0; i<widths.length; i++)
        {
            widths[i] =  Math.round(widths[i] * scaling);
        }
    }

    /**
     * Returns the cid (we use the original glyphId as cid) linked
     * with the given unicode.
     *
     * @param unicode the given character code to be mapped
     * @return glyphId the corresponding glyph id for the given character code
     */
    public int getCID(int unicode)
    {
        return unicodeCmap.getGlyphId(unicode);
    }

    /**
     * Hold the used codes with this font to reload subfont
     *
     * @param text the text wriiten with this font
     */
    public void setUsedCodes(String text)
    {
        for (int i = 0, len=text.length(), cp; i < len; i += Character.charCount(cp))
        {
            cp = text.codePointAt(i);
            usedCodes.add(cp);
        }
    }

    @Override
    public float getWidthFromFont(int code) throws IOException
    {
        int gid = getCID(code);
        if (gid > -1 && gid < widths.length)
        {
            return widths[gid];
        }
        else
        {
            return widths[0];
        }
    }

    private String getPrefix()
    {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<6; i++)
        {
            sb.append((char)('A'+rnd.nextInt(26)));
        }
        return sb.toString() + "+";
    }


    private int[] getUsedCodes()
    {
        int[] used = new int[usedCodes.size()];
        int j = 0;
        for (Integer code : usedCodes)
        {
            used[j++] = code.intValue();
        }

        return used;
    }

    private PDStream getToUnicode(Map<Integer, Integer> unicode2cid)
        throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("/CIDInit /ProcSet findresource begin\n")
            .append("12 dict begin\n")
            .append("begincmap\n")
            .append("/CIDSystemInfo\n")
            .append("<< /Registry (TT1+0)\n")
            .append("/Ordering (T42UV)\n")
            .append("/Supplement 0\n")
            .append(">> def\n")
            .append("/CMapName /TT1+0 def\n")
            .append("/CMapType 2 def\n")
            .append("1 begincodespacerange\n")
            .append("<0000> <FFFF>\n")
            .append("endcodespacerange\n");
        int size = 0, i = 0;
        for (Integer unicode : unicode2cid.keySet())
        {
            if (size == 0)
            {
                if (i != 0) {
                    sb.append("endbfchar\n");
                }
                size = Math.min(100, unicode2cid.size() - i);
                sb.append(size).append(" beginbfchar\n");
            }
            --size; ++i;
            sb.append(StringUtil.toHex(unicode2cid.get(unicode))).append(" ").append(StringUtil.toHex(unicode.intValue())).append('\n');
        }
        sb.append("endbfchar\n")
          .append("endcmap\n")
          .append("CMapName currentdict /CMap defineresource pop\n")
          .append("end end\n");

        ByteArrayInputStream bis = new ByteArrayInputStream(sb.toString().getBytes(Charset.forName("US-ASCII")));
        PDStream toUnicode = new PDStream(doc, bis);
        toUnicode.addCompression();
        return toUnicode;
    }

    private void resetToUnicode(PDStream toUnicode)
    {
        if (dict.getItem(COSName.TO_UNICODE) != null)
        {
            dict.removeItem(COSName.TO_UNICODE);
        }
        dict.setItem(COSName.TO_UNICODE, toUnicode);
    }

    private COSBase getCIDToGID(int maxcid, Map<Integer, Integer> cid2gid) throws IOException
    {
        byte[] bs = new byte[2*(maxcid+1)];
        for (int i=0; i<=maxcid; i++)
        {
            if (cid2gid.containsKey(i))
            {
                int gid = cid2gid.get(i);
                bs[i*2]   = (byte)((gid >> 8) & 0xFF);
                bs[i*2+1] = (byte)(gid & 0xFF);
            }
            else
            {
                bs[i*2]   = (byte)0;
                bs[i*2+1] = (byte)0;
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(bs);
        PDStream cidToGID = new PDStream(doc, bis);
        cidToGID.addCompression();
        return cidToGID.getCOSObject();
    }

    /**
     * Returns the Unicode cmap from the cmaptable.
     */
    private CmapSubtable getUnicodeCmap(CmapTable cmapTable)
    {
        CmapSubtable cmap = cmapTable.getSubtable(CmapTable.PLATFORM_UNICODE,
                                  CmapTable.ENCODING_UNICODE_2_0_FULL);
        if (cmap == null)
        {
            cmap = cmapTable.getSubtable(CmapTable.PLATFORM_WINDOWS,
                                         CmapTable.ENCODING_WIN_UCS4);
        }
        if (cmap == null)
        {
            cmap = cmapTable.getSubtable(CmapTable.PLATFORM_UNICODE,
                                         CmapTable.ENCODING_UNICODE_2_0_BMP);
        }
        if (cmap == null)
        {
            cmap = cmapTable.getSubtable(CmapTable.PLATFORM_WINDOWS,
                                         CmapTable.ENCODING_WIN_UNICODE);
        }
        return cmap;
    }

}
