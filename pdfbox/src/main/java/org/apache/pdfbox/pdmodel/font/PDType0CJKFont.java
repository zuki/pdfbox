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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType0;
import org.apache.pdfbox.pdmodel.font.PDCIDSystemInfo;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;

/**
 * This is an implementation for the CJK Fonts bundled in
 *    Adobe Reader Font Packs - Asian and Extended Language Pack.
 *
 * @author Keiji Suzuki
 *
 */
public class PDType0CJKFont extends PDType0Font
{
    /**
     * Log instance.
     */
    private static final Log LOG = LogFactory.getLog(PDType0CJKFont.class);

    private static final Map<String, String> SUPPORTED_FONTS = new HashMap<String, String>();

    private static final String SUPPORTED_FONTS_PROPERTIES_PATH
        = "org/apache/pdfbox/resources/cjk/supported_fonts.properties";

    static
    {
        loadSupportedFonts();
    }

    public static final PDType0CJKFont JAPANESE_SANS_SERIF = PDType0CJKFont.create("KozGoPr6N-Medium");

    public static final PDType0CJKFont JAPANESE_SERIF = PDType0CJKFont.create("KozMinPr6N-Regular");

    public static final PDType0CJKFont SIMPLIFIED_CHINESE_SANS_SERIF = PDType0CJKFont.create("AdobeHeitiStd-Regular");

    public static final PDType0CJKFont SIMPLIFIED_CHINESE_SERIF = PDType0CJKFont.create("AdobeSongStd-Light");

    public static final PDType0CJKFont TRADITIONAL_CHINESE_SANS_SERIF = PDType0CJKFont.create("AdobeFanHeitiStd-Bold");

    public static final PDType0CJKFont TRADITIONAL_CHINESE_SERIF = PDType0CJKFont.create("AdobeMingStd-Light");

    public static final PDType0CJKFont KOREAN_SANS_SERIF = PDType0CJKFont.create("AdobeGothicStd-Bold");

    public static final PDType0CJKFont KOREAN_SERIF = PDType0CJKFont.create("AdobeMyungjoStd-Medium");

    /**
    * Constructor.
    *
    * @param fontName The PostscriptName of the font
    *
    * @throws IOException If an error occures while font parsing.       .
    */
    private PDType0CJKFont(String fontName) throws IOException
    {
        super();

        if (!SUPPORTED_FONTS.containsKey(fontName))
        {
            throw new IOException(fontName+" is not supported");
        }

        Properties fontProperties = getProperties(SUPPORTED_FONTS.get(fontName));

        String fontBBox = fontProperties.getProperty("FontBBox");
        String[] bb = fontBBox.substring(1, fontBBox.length() - 1).split(" ");
        COSArray bbox = new COSArray();
        bbox.add(new COSFloat(Float.valueOf(bb[0]).floatValue()));
        bbox.add(new COSFloat(Float.valueOf(bb[1]).floatValue()));
        bbox.add(new COSFloat(Float.valueOf(bb[2]).floatValue()));
        bbox.add(new COSFloat(Float.valueOf(bb[3]).floatValue()));

        String name = fontProperties.getProperty("Name");
        COSDictionary fd = new COSDictionary();
        fd.setName(COSName.TYPE, "FontDescriptor");
        fd.setName(COSName.FONT_NAME, name);
        fd.setInt(COSName.FLAGS, Integer.valueOf(fontProperties.getProperty("Flags")).intValue());
        fd.setItem(COSName.FONT_BBOX, bbox);
        fd.setFloat(COSName.ITALIC_ANGLE, Float.valueOf(fontProperties.getProperty("ItalicAngle")).floatValue());
        fd.setFloat(COSName.ASCENT, Float.valueOf(fontProperties.getProperty("Ascent")).floatValue());
        fd.setFloat(COSName.DESCENT, Float.valueOf(fontProperties.getProperty("Descent")).floatValue());
        fd.setFloat(COSName.CAP_HEIGHT, Float.valueOf(fontProperties.getProperty("CapHeight")).floatValue());
        fd.setFloat(COSName.STEM_V, Float.valueOf(fontProperties.getProperty("StemV")).floatValue());

        String systemInfo = fontProperties.getProperty("CIDSystemInfo");
        COSDictionary cidInfo = (new PDCIDSystemInfo(systemInfo)).getCIDSystemInfo();

        COSDictionary font = new COSDictionary();
        font.setItem(COSName.TYPE, COSName.FONT);
        font.setItem(COSName.SUBTYPE, COSName.CID_FONT_TYPE0);
        font.setName(COSName.BASE_FONT, name);
        font.setItem(COSName.CIDSYSTEMINFO, cidInfo);
        font.setLong(COSName.DW, 1000);
        font.setItem(COSName.W, getFontWidthsArray(fontProperties.getProperty("W")));
        font.setItem(COSName.FONT_DESC, fd);

        COSArray descFont = new COSArray();
        descFont.add(font);

        dict.setItem(COSName.SUBTYPE, COSName.TYPE0);
        dict.setName(COSName.BASE_FONT, name);
        dict.setName(COSName.ENCODING, fontProperties.getProperty("Encoding"));
        dict.setItem(COSName.DESCENDANT_FONTS, descFont);
        this.readEncoding();
        this.fetchCMapUCS2();

        PDCIDFont desFont = new PDCIDFontType0(font, this);
        setDescendantFont(desFont);

    }

    /**
    * Create a new PDType0CJKFont with the Postscript Name
    *
    * @param fontName The PostscriptName of the font
    *
    * @return a PDType0CJKFont instance or null if the error has occured
    */
    public static PDType0CJKFont create(String fontName)
    {
        try
        {
            return new PDType0CJKFont(fontName);
        }
        catch (IOException e)
        {
            LOG.warn(e.getMessage());
            return null;
        }
    }

    /**
    * Is there a font with the Postscript Name
    *
    * @param fontName The PostscriptName of the font
    *
    * @return true if exists, false if not exists
    */
    public static boolean isSupported(String fontName)
    {
        return SUPPORTED_FONTS.containsKey(fontName);
    }

    private static Properties getProperties(String fontPath) throws IOException
    {
        URL url = PDType0CJKFont.class.getClassLoader().getResource(fontPath);
        if (url == null)
        {
            throw new IOException("Specified properties not found at " + fontPath);
        }
        Properties properties = new Properties();
        properties.load(url.openStream());
        return properties;
    }

    private static void loadSupportedFonts()
    {
        try
        {
            Properties supportedProperties = getProperties(SUPPORTED_FONTS_PROPERTIES_PATH);
            Enumeration<?> names = supportedProperties.propertyNames();
            for (Object name : Collections.list(names))
            {
                String fontName = name.toString();
                String fontPath = supportedProperties.getProperty(fontName);
                SUPPORTED_FONTS.put(fontName, fontPath);
            }
        }
        catch(IOException e)
        {
            LOG.warn(e.getMessage());
        }
    }

}
