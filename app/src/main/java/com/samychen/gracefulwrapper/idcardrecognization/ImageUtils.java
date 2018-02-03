package com.samychen.gracefulwrapper.idcardrecognization;

import android.graphics.Bitmap;

/**
 * Created by samychen on 2017/9/3 0003.
 * 我的github地址 https://github.com/samychen
 */

public class ImageUtils {
    static{
        System.loadLibrary("Imgprocess");
    }

    public static native Bitmap findIdNumber(Bitmap src, Bitmap out, Bitmap tpl, Bitmap.Config argb8888);
}
