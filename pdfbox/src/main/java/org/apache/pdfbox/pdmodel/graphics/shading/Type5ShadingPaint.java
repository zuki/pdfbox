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

import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.util.Matrix;

/**
 * This represents the Paint of a Type 5 shading.
 */
public class Type5ShadingPaint implements Paint
{
    private static final Log LOG = LogFactory.getLog(Type5ShadingPaint.class);

    private PDShadingType5 shading;
    private Matrix currentTransformationMatrix;
    private int pageHeight;

    /**
     * Constructor.
     *
     * @param shadingType5 the shading resources
     * @param ctm current transformation matrix
     * @param pageHeightValue
     */
    public Type5ShadingPaint(PDShadingType5 shadingType5, Matrix ctm, int pageHeightValue)
    {
        shading = shadingType5;
        currentTransformationMatrix = ctm;
        pageHeight = pageHeightValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTransparency()
    {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaintContext createContext(ColorModel cm, Rectangle deviceBounds,
            Rectangle2D userBounds, AffineTransform xform, RenderingHints hints)
    {
        try
        {
            return new Type5ShadingContext(shading, cm, xform, currentTransformationMatrix, pageHeight);
        }
        catch (IOException ex)
        {
            LOG.error(ex);
            return null;
        }
    }
}
