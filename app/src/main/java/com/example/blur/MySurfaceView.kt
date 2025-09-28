package com.example.blur

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.MotionEvent

class MyGLSurfaceView(context: Context) : GLSurfaceView(context) {

    private val renderer: MyGLRenderer

    var lastX: Float = 0.0f;
    var lastY: Float = 0.0f;

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        val normX = (x / width) * 2f - 1f
        val normY = -((y / height) * 2f - 1f)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = normX
                lastY = normY
                renderer.setPoints(normX, normY, normX, normY)
                requestRender()
            }
            MotionEvent.ACTION_MOVE -> {
                renderer.setPoints(lastX, lastY, normX, normY)
                lastX = normX
                lastY = normY
                requestRender()
            }
        }
        return true
    }

    fun click() {
        queueEvent {
            renderer.click()
        }
        requestRender()
    }

    fun setBlurAmount(x: Float) {
        queueEvent {
            renderer.setBlurAmount(x)
        }
        requestRender()
    }

}
