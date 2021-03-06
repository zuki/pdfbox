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
package org.apache.pdfbox.util.operator.pagedrawer;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.image.PDInlineImage;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.PDFOperator;
import org.apache.pdfbox.util.operator.OperatorProcessor;

/**
 * BI Begins an inline image.
 * @author Ben Litchfield
 */
public final class BeginInlineImage extends OperatorProcessor
{
    /**
     * @throws IOException If there is an error displaying the inline image.
     */
    public void process(PDFOperator operator, List<COSBase> operands) throws IOException
    {
        PageDrawer drawer = (PageDrawer)context;

        PDInlineImage image = new PDInlineImage(operator.getImageParameters(),
                                                operator.getImageData(),
                                                context.getResources().getColorSpaces());
        BufferedImage awtImage;
        if (image.isStencil())
        {
            PDColorSpace colorSpace = drawer.getGraphicsState().getNonStrokingColorSpace();
            PDColor color = drawer.getGraphicsState().getNonStrokingColor();
            awtImage = image.getStencilImage(colorSpace.toPaint(color)); // <--- TODO: pass page height?
        }
        else
        {
            awtImage = image.getImage();
        }
        Matrix ctm = drawer.getGraphicsState().getCurrentTransformationMatrix();
        drawer.drawImage(awtImage, ctm.createAffineTransform());
    }
}