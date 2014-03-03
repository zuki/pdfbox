package org.apache.pdfbox.pdmodel.font;

import java.io.IOException;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;

public class PDCIDSystemInfo
{
    private COSDictionary cidsysteminfo;

    public static final PDCIDSystemInfo ADOBE_JAPAN1_6 = 
        new PDCIDSystemInfo("Adobe", "Japan1", 6);

    public static final PDCIDSystemInfo ADOBE_IDENTITY_0 = 
        new PDCIDSystemInfo("Adobe", "Identity", 60);

    public PDCIDSystemInfo(String registry, String ordering, int supplement)
    {
        cidsysteminfo = new COSDictionary();
        cidsysteminfo.setString(COSName.REGISTRY, registry);
        cidsysteminfo.setString(COSName.ORDERING, ordering);
        cidsysteminfo.setInt(COSName.SUPPLEMENT, supplement);
    }

    public COSDictionary getCIDSystemInfo()
    {
        return cidsysteminfo;
    }
}
