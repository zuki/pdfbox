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
package org.apache.pdfbox.pdmodel.graphics.shading;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

/**
 * This represents resources for an axial shading.
 *
 * @version $Revision: 1.0 $
 */
public class PDShadingType2 extends PDShading
{
    private COSArray coords = null;
    private COSArray domain = null;
    private COSArray extend = null;

    /**
     * Constructor using the given shading dictionary.
     *
     * @param shadingDictionary The dictionary for this shading.
     */
    public PDShadingType2(COSDictionary shadingDictionary)
    {
        super(shadingDictionary);
    }

    /**
     * {@inheritDoc}
     */
    public int getShadingType()
    {
        return PDShading.SHADING_TYPE2;
    }

    /**
     * This will get the optional Extend values for this shading.
     *
     * @return the extend values
     */
    public COSArray getExtend()
    {
        if (extend == null)
        {
            extend = (COSArray) getCOSDictionary().getDictionaryObject(COSName.EXTEND);
        }
        return extend;
    }

    /**
     * Sets the optional Extend entry for this shading.
     *
     * @param newExtend the extend array
     */
    public void setExtend(COSArray newExtend)
    {
        extend = newExtend;
        if (newExtend == null)
        {
            getCOSDictionary().removeItem(COSName.EXTEND);
        }
        else
        {
            getCOSDictionary().setItem(COSName.EXTEND, newExtend);
        }
    }

    /**
     * This will get the optional Domain values for this shading.
     *
     * @return the domain values
     */
    public COSArray getDomain()
    {
        if (domain == null)
        {
            domain = (COSArray) getCOSDictionary().getDictionaryObject(COSName.DOMAIN);
        }
        return domain;
    }

    /**
     * Sets the optional Domain entry for this shading.
     *
     * @param newDomain the domain array
     */
    public void setDomain(COSArray newDomain)
    {
        domain = newDomain;
        if (newDomain == null)
        {
            getCOSDictionary().removeItem(COSName.DOMAIN);
        }
        else
        {
            getCOSDictionary().setItem(COSName.DOMAIN, newDomain);
        }
    }

    /**
     * This will get the Coords values for this shading.
     *
     * @return the coords values
     */
    public COSArray getCoords()
    {
        if (coords == null)
        {
            coords = (COSArray) getCOSDictionary().getDictionaryObject(COSName.COORDS);
        }
        return coords;
    }

    /**
     * Sets the Coords entry for this shading.
     *
     * @param newCoords the coords array
     */
    public void setCoords(COSArray newCoords)
    {
        coords = newCoords;
        if (newCoords == null)
        {
            getCOSDictionary().removeItem(COSName.COORDS);
        }
        else
        {
            getCOSDictionary().setItem(COSName.COORDS, newCoords);
        }
    }
}
