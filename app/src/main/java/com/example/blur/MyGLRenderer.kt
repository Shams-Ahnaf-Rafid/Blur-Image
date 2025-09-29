package com.example.blur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.opengl.GLSurfaceView
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
//
class MyGLRenderer(private val context: Context) : GLSurfaceView.Renderer {

    private var fboId = IntArray(3)
    private var fboTextureId = IntArray(3)
    private var screenWidth = 1
    private var screenHeight = 1
    private var count = 0
    private var first = true
    private var touch = false
    private var remove = 1

    private lateinit var Shader: ShaderProgram

    private var fgTextureId = 0
    private var maskTextureId = 0
    private var imageWidth = 1
    private var imageHeight = 1

    private var pointA = floatArrayOf(-2f, -2f)
    private var pointB = floatArrayOf(-2f, -2f)

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
    private var aspectRatioHandle = 0
    private var imageAspectRatioHandle = 0
    private var displayVertexHandle = 0
    private var intensityHandle = 0
    private var mediapipeMaskHandle = 0

    // ---- ML Kit Segmenter ----
    val segmenter by lazy {
        val options = SelfieSegmenterOptions.Builder()
            .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE) // or STREAM_MODE for camera
            .build()
        Segmentation.getClient(options)
    }

    fun segmentWithMLKit(bitmap: Bitmap, onMaskReady: (Bitmap) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)

        segmenter.process(image)
            .addOnSuccessListener { mask ->
                val buffer = mask.buffer
                val maskWidth = mask.width
                val maskHeight = mask.height

                buffer.rewind() // make sure we start at beginning

                // Copy buffer values first
                val floatBuffer = buffer.asFloatBuffer() // view buffer as floats
                val confidenceValues = FloatArray(floatBuffer.remaining())
                floatBuffer.get(confidenceValues)


                // Log max confidence
                val maxConfidence = confidenceValues.maxOrNull() ?: 0f
                Log.d("MLKit", "Max mask confidence: $maxConfidence")

                // Create pixels array
                val pixels = IntArray(maskWidth * maskHeight)
                for (i in confidenceValues.indices) {
                    val confidence = confidenceValues[i]
                    pixels[i] = if (confidence > 0.5f) 0xFFFFFFFF.toInt() else 0x00000000
                }

                val maskBitmap = Bitmap.createBitmap(pixels, maskWidth, maskHeight, Bitmap.Config.ARGB_8888)

                // Scale to original image size
                val scaledMask = Bitmap.createScaledBitmap(maskBitmap, bitmap.width, bitmap.height, true)
                //maskBitmap.recycle() // recycle small mask

                onMaskReady(scaledMask) // safely send mask to GL thread
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Log.e("MLKit", "Segmentation failed", e)
            }
    }




    fun updateMaskTexture(maskBitmap: Bitmap) {
        if (maskTextureId == 0) {
            val textures = IntArray(1)
            glGenTextures(1, textures, 0)
            maskTextureId = textures[0]
        }

        glBindTexture(GL_TEXTURE_2D, maskTextureId)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, maskBitmap, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun adjustTouchPoint(x: Float, y: Float): FloatArray {
        val screenAspect = screenWidth.toFloat() / screenHeight
        val imageAspect = imageWidth.toFloat() / imageHeight

        var adjustedX = x / 0.9f
        var adjustedY = y / 0.9f

        if (screenAspect > imageAspect) {
            adjustedX *= screenAspect / imageAspect
        } else {
            adjustedY *= imageAspect / screenAspect
        }

        return floatArrayOf(adjustedX, adjustedY)
    }

    fun setPoints(x0: Float, y0: Float, x1: Float, y1: Float) {
        val adjustedA = adjustTouchPoint(x0, y0)
        val adjustedB = adjustTouchPoint(x1, y1)

        pointA[0] = adjustedA[0]; pointA[1] = adjustedA[1]
        pointB[0] = adjustedB[0]; pointB[1] = adjustedB[1]
        touch = true
    }

    fun click() {
        remove = (remove + 1) % 2
    }

    fun setBlurAmount(x: Float) {
        drawToFBO(fboId[2], fgTextureId, horizontal = 1, display = 2, x)
        drawToFBO(fboId[1], fboTextureId[2], horizontal = 0, display = 2, x)
    }

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

    override fun onSurfaceCreated(unused: GL10?, config: EGLConfig?) {
        Shader = ShaderProgram(context, R.raw.vertex_shader, R.raw.fragment_shader)
        Shader.useProgram()

        brushPosHandle = glGetAttribLocation(Shader.program, "a_Position")
        brushPointsHandle = glGetUniformLocation(Shader.program, "u_Points")
        brushThicknessHandle = glGetUniformLocation(Shader.program, "u_Thickness")
        brushResolutionHandle = glGetUniformLocation(Shader.program, "u_resolution")
        fgMaskHandle = glGetUniformLocation(Shader.program, "u_Mask")
        fgTexHandle = glGetUniformLocation(Shader.program, "u_Forest")
        blurHandle = glGetUniformLocation(Shader.program, "u_Blur")
        horizontalHandle = glGetUniformLocation(Shader.program, "u_Horizontal")
        displayHandle = glGetUniformLocation(Shader.program, "u_Display")
        removeHandle = glGetUniformLocation(Shader.program, "u_Remove")
        aspectRatioHandle = glGetUniformLocation(Shader.program, "u_AspectRatio")
        imageAspectRatioHandle = glGetUniformLocation(Shader.program, "u_ImageAspectRatio")
        displayVertexHandle = glGetUniformLocation(Shader.program, "u_display")
        intensityHandle = glGetUniformLocation(Shader.program, "u_Intensity")
        mediapipeMaskHandle = glGetUniformLocation(Shader.program, "u_MLMask")
        Log.d("Check", "u_MLMask handle=$mediapipeMaskHandle")
        fgTextureId = loadTextureFromRes(R.drawable.background)
    }

    override fun onSurfaceChanged(unused: GL10?, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        glViewport(0, 0, width, height)

        if (count == 0) {
            setupFBO(width, height)
            count++
        }
    }

    override fun onDrawFrame(unused: GL10?) {
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)

        if (first) {
            drawToFBO(fboId[2], fgTextureId, horizontal = 1, display = 2, 50f)
            drawToFBO(fboId[1], fboTextureId[2], horizontal = 0, display = 2, 50f)
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
            passAspectRatios(0)

            glActiveTexture(GL_TEXTURE0)
            glBindTexture(GL_TEXTURE_2D, fboTextureId[0])
            glUniform1i(fgMaskHandle, 0)

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
        passAspectRatios(1)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, fboTextureId[0])
        glUniform1i(fgMaskHandle, 0)

        glActiveTexture(GL_TEXTURE1)
        glBindTexture(GL_TEXTURE_2D, fgTextureId)
        glUniform1i(fgTexHandle, 1)

        glActiveTexture(GL_TEXTURE2)
        glBindTexture(GL_TEXTURE_2D, fboTextureId[1])
        glUniform1i(blurHandle, 2)

        // Bind ML Kit mask if available
        if (maskTextureId != 0) {
            Log.d("Check", "$maskTextureId")
            glActiveTexture(GL_TEXTURE3)
            glBindTexture(GL_TEXTURE_2D, maskTextureId)
            glUniform1i(mediapipeMaskHandle, 3)
        }

        glUniform1i(displayHandle, 1)
        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)

        glDisableVertexAttribArray(brushPosHandle)
        glDisable(GL_BLEND)
    }

    private fun drawToFBO(fbo: Int, textureId: Int, horizontal: Int, display: Int, intensity: Float) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo)
        glViewport(0, 0, screenWidth, screenHeight)
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_COLOR_BUFFER_BIT)

        Shader.useProgram()
        vertexBuffer.position(0)
        glEnableVertexAttribArray(brushPosHandle)
        glVertexAttribPointer(brushPosHandle, 2, GL_FLOAT, false, 8, vertexBuffer)

        glUniform1f(brushThicknessHandle, 100f)
        glUniform1f(intensityHandle, intensity)
        glUniform2f(brushResolutionHandle, screenWidth.toFloat(), screenHeight.toFloat())
        passAspectRatios(display)

        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, textureId)

        if (horizontal == 1) glUniform1i(fgTexHandle, 0)
        else glUniform1i(blurHandle, 0)

        glUniform1i(horizontalHandle, horizontal)
        glUniform1i(displayHandle, display)

        glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        glDisableVertexAttribArray(brushPosHandle)
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    private fun passAspectRatios(display: Int) {
        val screenAspect = screenWidth.toFloat() / screenHeight
        val imageAspect = imageWidth.toFloat() / imageHeight
        glUniform1f(aspectRatioHandle, screenAspect)
        glUniform1f(imageAspectRatioHandle, imageAspect)
        glUniform1i(displayVertexHandle, display)
    }

    private fun setupFBO(width: Int, height: Int) {
        glGenFramebuffers(3, fboId, 0)
        glGenTextures(3, fboTextureId, 0)

        for (i in 0 until 3) {
            glBindTexture(GL_TEXTURE_2D, fboTextureId[i])
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, null)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)

            glBindFramebuffer(GL_FRAMEBUFFER, fboId[i])
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, fboTextureId[i], 0)
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
        glBindTexture(GL_TEXTURE_2D, 0)
    }

    private fun loadTextureFromRes(resId: Int): Int {
        val textures = IntArray(1)
        glGenTextures(1, textures, 0)

        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)

        imageWidth = bitmap.width
        imageHeight = bitmap.height

        val matrix = Matrix().apply { preScale(1f, -1f) }
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

    fun cleanup() {
        // ML Kit client does not need manual close
    }
}
