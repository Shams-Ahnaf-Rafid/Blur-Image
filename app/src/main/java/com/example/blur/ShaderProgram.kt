package com.example.blur

import android.content.Context
import android.opengl.GLES20.*

open class ShaderProgram(
    context: Context,
    vertexShaderResourceId: Int,
    fragmentShaderResourceId: Int
) {
    val program: Int = ShaderHelper.buildProgram(
        TextResourceReader.readTextFileFromRawResource(context, vertexShaderResourceId),
        TextResourceReader.readTextFileFromRawResource(context, fragmentShaderResourceId)
    )

    fun useProgram() {
        glUseProgram(program)
    }
}