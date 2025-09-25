package com.example.blur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.opengl.GLSurfaceView
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var fboId = IntArray(3)
    private var fboTextureId = IntArray(3)
    private var screenWidth = 1
    private var screenHeight = 1
    var count = 0;
    private lateinit var Shader: ShaderProgram
    private var touch = false
    private var remove = 1
    private var first = true

    private val quadVertices = floatArrayOf(
        1f, 1f,
        1f, -1f,
        -1f, 1f,
        -1f, -1f
    )

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(quadVertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .put(quadVertices)
        .apply { position(0) }

    private var brushPosHandle = 0
    private var brushPointsHandle = 0
    private var brushThicknessHandle = 0
    private var brushResolutionHandle = 0
    private var fgMaskHandle = 0
    private var blurHandle = 0
    private var horizontalHandle = 0
    private var displayHandle = 0
    private var removeHandle = 0
    private var fgTexHandle = 0
    private var fgTextureId = 0

    private var pointA = floatArrayOf(-2f, -2f)
    private var pointB = floatArrayOf(-2f, -2f)

    fun setPoints(x0: Float, y0: Float, x1: Float, y1: Float) {
        pointA[0] = x0; pointA[1] = y0
        pointB[0] = x1; pointB[1] = y1
        touch = true
    }

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        glClearColor(1f, 1f, 1f, 1f)

        Shader = ShaderProgram(context, R.raw.vertex_shader, R.raw.fragment_shader)

        Shader.useProgram()
        brushPosHandle = glGetAttribLocation(Shader.program, "a_Position")
        brushPointsHandle = glGetUniformLocation(Shader.program, "u_Points")
        brushThicknessHandle = glGetUniformLocation(Shader.program, "u_Thickness")
        brushResolutionHandle = glGetUniformLocation(Shader.program, "u_resolution")
        fgMaskHandle = glGetUniformLocation(Shader.program, "u_Mask")
        fgTexHandle = glGetUniformLocation(Shader.program, "u_Forest")
        blurHandle = glGetUniformLocation(Shader.program, "u_Blur")
        displayHandle = glGetUniformLocation(Shader.program, "u_Display")
        horizontalHandle = glGetUniformLocation(Shader.program, "u_Horizontal")
        removeHandle = glGetUniformLocation(Shader.program, "u_Remove")

        fgTextureId = loadTextureFromRes(R.drawable.background)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        Log.d("Check", "$width $height")
        glViewport(0, 0, width, height)
        if (count == 0) {
            setupFBO(width, height)
            count++;
        }
    }

    private fun setupFBO(width: Int, height: Int) {
        glGenFramebuffers(3, fboId, 0)
        glGenTextures(3, fboTextureId, 0)

        for (i in 0 until 3) {
            glBindTexture(GL_TEXTURE_2D, fboTextureId[i])
            glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                null
            )

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            glBindFramebuffer(GL_FRAMEBUFFER, fboId[i])
            glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_COLOR_ATTACHMENT0,
                GL_TEXTURE_2D,
                fboTextureId[i],
                0
            )

        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }



    override fun onDrawFrame(unused: GL10?) {


        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)

        if (first) {

            glBindFramebuffer(GL_FRAMEBUFFER, fboId[2])
            glViewport(0, 0, screenWidth, screenHeight)
            glClearColor(0f,0f,0f,0f)
            glClear(GL_COLOR_BUFFER_BIT)

            Shader.useProgram()
            vertexBuffer.position(0)
            glEnableVertexAttribArray(brushPosHandle)
            glVertexAttribPointer(brushPosHandle, 2, GL_FLOAT, false, 8, vertexBuffer)

            glUniform1f(brushThicknessHandle, 100f)
            glUniform2f(brushResolutionHandle, screenWidth.toFloat(), screenHeight.toFloat())
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, fgTextureId)
            glUniform1i(fgTexHandle, 0)
            glUniform1i(horizontalHandle, 1)
            glUniform1i(displayHandle, 2)
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
            glDisableVertexAttribArray(brushPosHandle)
            glBindFramebuffer(GL_FRAMEBUFFER, 0)

            glBindFramebuffer(GL_FRAMEBUFFER, fboId[1])
            glViewport(0, 0, screenWidth, screenHeight)
            glClearColor(0f,0f,0f,0f)
            glClear(GL_COLOR_BUFFER_BIT)

            Shader.useProgram()
            vertexBuffer.position(0)
            glEnableVertexAttribArray(brushPosHandle)
            glVertexAttribPointer(brushPosHandle, 2, GL_FLOAT, false, 8, vertexBuffer)

            glUniform1f(brushThicknessHandle, 100f)
            glUniform2f(brushResolutionHandle, screenWidth.toFloat(), screenHeight.toFloat())
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, fboTextureId[2])
            glUniform1i(blurHandle, 0)
            glUniform1i(horizontalHandle, 0)
            glUniform1i(displayHandle, 2)
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
            glDisableVertexAttribArray(brushPosHandle)
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
            first = false
        }

        glBindFramebuffer(GL_FRAMEBUFFER, fboId[0])
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        if (touch) {

            Shader.useProgram()
            vertexBuffer.position(0)
            glEnableVertexAttribArray(brushPosHandle)
            glVertexAttribPointer(brushPosHandle, 2, GL_FLOAT, false, 8, vertexBuffer)
            glUniform2fv(brushPointsHandle, 2, floatArrayOf(pointA[0], pointA[1], pointB[0], pointB[1]), 0)
            glUniform1f(brushThicknessHandle, 100f)
            glUniform2f(brushResolutionHandle, screenWidth.toFloat(), screenHeight.toFloat())
            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, fgTextureId)
            glUniform1i(fgTexHandle, 0)
            glActiveTexture(GL_TEXTURE1)
            glBindTexture(GL_TEXTURE_2D, fboTextureId[0])
            glUniform1i(fgMaskHandle, 1)
            glUniform1i(removeHandle, remove)
            glUniform1i(displayHandle, 0)
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
            glDisableVertexAttribArray(brushPosHandle)

            touch = false
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0)

        Shader.useProgram()
        vertexBuffer.position(0)
        glEnableVertexAttribArray(brushPosHandle)
        glVertexAttribPointer(brushPosHandle, 2, GL_FLOAT, false, 8, vertexBuffer)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, fboTextureId[0])
        glUniform1i(fgMaskHandle, 0)
        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, fgTextureId)
        glUniform1i(fgTexHandle, 1)
        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, fboTextureId[1])
        glUniform1i(blurHandle, 2)
        glUniform1i(displayHandle, 1)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glDisableVertexAttribArray(brushPosHandle)

        glDisable(GL_BLEND)
    }

    private fun loadTextureFromRes(resId: Int): Int {
        val textures = IntArray(1)
        glGenTextures(1, textures, 0)
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
        val matrix = android.graphics.Matrix().apply { preScale(1f, -1f) }
        val flipped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false)
        glBindTexture(GL_TEXTURE_2D, textures[0])
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, flipped, 0)
        bitmap.recycle()
        flipped.recycle()
        return textures[0]
    }

    fun click() {
        remove = (remove + 1) % 2
    }

}