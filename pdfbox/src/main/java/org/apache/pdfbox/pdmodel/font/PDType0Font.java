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
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fontbox.cmap.CMap;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.font.encoding.StandardEncoding;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

/**
 * A Composite (Type 0) font.
 *
 * @author Ben Litchfield
 */
public class PDType0Font extends PDFont
{
    private static final Log LOG = LogFactory.getLog(PDType0Font.class);

    private PDCIDFont descendantFont;
    private CMap cMap, cMapUCS2;
    private boolean isCMapPredefined;

    /**
     * Constructor.
     * 
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    public PDType0Font() throws IOException
    {
        super();
        descendantFont = null;
        cMap = null;
        cMapUCS2 = null;
        isCMapPredefined = true;
    }

    /**
     * Constructor.
     *
     * @param fontDictionary The font dictionary according to the PDF specification.
     */
    public PDType0Font(COSDictionary fontDictionary) throws IOException
    {
        super(fontDictionary);
        COSArray descendantFonts = (COSArray)dict.getDictionaryObject(COSName.DESCENDANT_FONTS);
        COSDictionary descendantFontDictionary = (COSDictionary) descendantFonts.getObject(0);

        if (descendantFontDictionary == null)
        {
            throw new IOException("Missing descendant font dictionary");
        }

        readEncoding();
        fetchCMapUCS2();
        descendantFont = PDFontFactory.createDescendantFont(descendantFontDictionary, this);

        // warn if there may be text extraction issues
        if (!isSymbolic())
        {
            LOG.warn("Nonsymbolic Type 0 font: " + getName());
        }
    }

    /**
     * Reads the font's Encoding entry, which should be a CMap name/stream.
     */
    private void readEncoding() throws IOException
    {
        COSBase encoding = dict.getDictionaryObject(COSName.ENCODING);
        if (encoding != null)
        {
            if (encoding instanceof COSName)
            {
                // predefined CMap
                COSName encodingName = (COSName)encoding;
                cMap = CMapManager.getPredefinedCMap(encodingName.getName());
                if (cMap != null)
                {
                    isCMapPredefined = true;
                    return;
                }
                else
                {
                    throw new IOException("Missing required CMap");
                }
            }
            else
            {
                cMap = readCMap(encoding);
                if (cMap == null)
                {
                    throw new IOException("Missing required CMap");
                }
                else if (!cMap.hasCIDMappings())
                {
                    LOG.warn("Invalid Encoding CMap in font " + getName());
                }
            }
        }
    }

    /**
     * Fetches the corresponding UCS2 CMap if the font's CMap is predefined.
     */
    private void fetchCMapUCS2() throws IOException
    {
        // if the font is composite and uses a predefined cmap (excluding Identity-H/V) then
        // or if its decendant font uses Adobe-GB1/CNS1/Japan1/Korea1
        if (isCMapPredefined)
        {
            // a) Map the character code to a CID using the font's CMap
            // b) Obtain the ROS from the font's CIDSystemInfo
            // c) Construct a second CMap name by concatenating the ROS in the format "R-O-UCS2"
            // d) Obtain the CMap with the constructed name
            // e) Map the CID according to the CMap from step d), producing a Unicode value

            String cMapName = null;

            // get the encoding CMap
            COSBase encoding = dict.getDictionaryObject(COSName.ENCODING);
            if (encoding != null && encoding instanceof COSName)
            {
                cMapName = ((COSName)encoding).getName();
            }

            // try to find the corresponding Unicode (UC2) CMap
            if (cMapName != null && !cMapName.equals("Identity-H") &&
                                    !cMapName.equals("Identity-V"))
            {
                CMap cMap = CMapManager.getPredefinedCMap(cMapName);
                if (cMap != null)
                {
                    String ucs2Name = cMap.getRegistry() + "-" + cMap.getOrdering() + "-UCS2";
                    CMap ucs2CMap = CMapManager.getPredefinedCMap(ucs2Name);
                    if (ucs2CMap != null)
                    {
                        cMapUCS2 = ucs2CMap;
                    }
                }
            }
        }
    }

    /**
     * Returns the PostScript name of the font.
     */
    public String getBaseFont()
    {
        return dict.getNameAsString(COSName.BASE_FONT);
    }

    /**
     * Returns the descendant font.
     */
    public PDCIDFont getDescendantFont()
    {
        return descendantFont;
    }

    /**
     * Set the descendant font.
     */
    public void setDescendantFont(PDCIDFont descendantFont)
    {
        this.descendantFont = descendantFont;
    }

    /**
     * Returns the font's CMap.
     */
    public CMap getCMap()
    {
        return cMap;
    }

    @Override
    public PDFontDescriptor getFontDescriptor()
    {
        return descendantFont.getFontDescriptor();
    }

    @Override
    public Matrix getFontMatrix()
    {
        return descendantFont.getFontMatrix();
    }

    @Override
    public boolean isVertical()
    {
        return cMap.getWMode() == 1;
    }

    @Override
    public float getHeight(int code) throws IOException
    {
        return descendantFont.getHeight(code);
    }

    @Override
    public float getAverageFontWidth()
    {
        return descendantFont.getAverageFontWidth();
    }

    @Override
    public Vector getPositionVector(int code)
    {
        // units are always 1/1000 text space, font matrix is not used, see FOP-2252
        return descendantFont.getPositionVector(code).scale(-1 / 1000f);
    }

    @Override
    public Vector getDisplacement(int code) throws IOException
    {
        if (isVertical())
        {
            return new Vector(0, descendantFont.getVerticalDisplacementVectorY(code) / 1000f);
        }
        else
        {
            return super.getDisplacement(code);
        }
    }

    @Override
    public float getWidth(int code) throws IOException
    {
        return descendantFont.getWidth(code);
    }

    @Override
    public float getWidthFromFont(int code) throws IOException
    {
        return descendantFont.getWidthFromFont(code);
    }

    @Override
    public boolean isEmbedded()
    {
        return descendantFont.isEmbedded();
    }

    @Override
    public String toUnicode(int code) throws IOException
    {
        // try to use a ToUnicode CMap
        String unicode = super.toUnicode(code);
        if (unicode != null)
        {
            return unicode;
        }

        if (!isSymbolic())
        {
            // this nonsymbolic behaviour isn't well documented, test with PDFBOX-1422,
            // also see PDCIDFontType2#cidToGID()
            String name = StandardEncoding.INSTANCE.getName(code);
            return GlyphList.getAdobeGlyphList().toUnicode(name);
        }
        else if (isCMapPredefined && cMapUCS2 != null)
        {
            // if the font is composite and uses a predefined cmap (excluding Identity-H/V) then
            // or if its decendant font uses Adobe-GB1/CNS1/Japan1/Korea1

            // a) Map the character code to a character identifier (CID) according to the font?s CMap
            int cid = codeToCID(code);

            // e) Map the CID according to the CMap from step d), producing a Unicode value
            return cMapUCS2.toUnicode(cid);
        }
        else
        {
            // if no value has been produced, there is no way to obtain Unicode for the character.
            return null;
        }
    }

    @Override
    public String getName()
    {
        return getBaseFont();
    }

    @Override
    public BoundingBox getBoundingBox() throws IOException
    {
        return descendantFont.getBoundingBox();
    }

    @Override
    public int readCode(InputStream in) throws IOException
    {
        return cMap.readCode(in);
    }

    /**
     * Returns the CID for the given character code. If not found then CID 0 is returned.
     *
     * @param code character code
     * @return CID
     */
    public int codeToCID(int code)
    {
        return descendantFont.codeToCID(code);
    }

    /**
     * Returns the GID for the given character code.
     *
     * @param code character code
     * @return GID
     */
    public int codeToGID(int code) throws IOException
    {
        return descendantFont.codeToGID(code);
    }

    @Override
    public boolean isStandard14()
    {
        return false;
    }

    @Override
    public boolean isDamaged()
    {
        return descendantFont.isDamaged();
    }

    @Override
    public String toString()
    {
        String descendant = null;
        if (getDescendantFont() != null)
        {
            descendant = getDescendantFont().getClass().getSimpleName();
        }
        return getClass().getSimpleName() + "/" + descendant + " " + getBaseFont();
    }

}
