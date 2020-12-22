package com.ziyueqian.mymemory.utils

import android.graphics.Bitmap

object BitmapScaler {

    //maintain aspect ratio with given width
    fun scaleToFitWidth(b: Bitmap, width: Int) : Bitmap {
        val factor = width / b.width.toFloat()
        return Bitmap.createScaledBitmap(b, width, (b.height * factor).toInt(), true)
    }

    //maintain aspect ratio with given height
    fun scaleToFitHeight(b: Bitmap, height: Int) : Bitmap {
        val factor = height / b.height.toFloat()
        return Bitmap.createScaledBitmap(b, (b.width * factor).toInt(),  height,true)
    }
}
