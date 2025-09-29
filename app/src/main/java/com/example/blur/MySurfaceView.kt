package com.example.blur

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer: MyGLRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateMask(maskBitmap: Bitmap) {
        val safeMask = maskBitmap.copy(Bitmap.Config.ARGB_8888, false)
        queueEvent {
            renderer.updateMaskTexture(safeMask)
        }
    }

    fun setBlurAmount(amount: Float) {
        queueEvent { renderer.setBlurAmount(amount) }
    }

    fun click() {
        queueEvent { renderer.click() }
    }
}
