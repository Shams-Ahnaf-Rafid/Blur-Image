package com.example.blur
import android.opengl.GLES20.*
import android.util.Log

object ShaderHelper {

    fun compileVertexShader(shaderCode: String): Int =
        compileShader(GL_VERTEX_SHADER, shaderCode)

    fun compileFragmentShader(shaderCode: String): Int =
        compileShader(GL_FRAGMENT_SHADER, shaderCode)

    private fun compileShader(type: Int, shaderCode: String): Int {
        val shaderId = glCreateShader(type)
        if (shaderId == 0) {
            Log.w("ShaderHelper", "Could not create shader.")
            return 0
        }

        glShaderSource(shaderId, shaderCode)
        glCompileShader(shaderId)

        val compileStatus = IntArray(1)
        glGetShaderiv(
            shaderId,
            GL_COMPILE_STATUS,
            compileStatus,
            0
        )

        if (compileStatus[0] == 0) {
            glDeleteShader(shaderId)
            Log.w("ShaderHelper", "Shader compile failed.")
            return 0
        }

        return shaderId
    }

    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        val programId = glCreateProgram()
        if (programId == 0) return 0

        glAttachShader(programId, vertexShaderId)
        glAttachShader(programId, fragmentShaderId)
        glLinkProgram(programId)

        val linkStatus = IntArray(1)
        glGetProgramiv(
            programId,
            GL_LINK_STATUS,
            linkStatus,
            0
        )

        if (linkStatus[0] == 0) {
            glDeleteProgram(programId)
            Log.w("ShaderHelper", "Program link failed.")
            return 0
        }

        return programId
    }

    fun buildProgram(vertexShaderSource: String, fragmentShaderSource: String): Int {
        val vertexShader = compileVertexShader(vertexShaderSource)
        val fragmentShader = compileFragmentShader(fragmentShaderSource)
        val program = linkProgram(vertexShader, fragmentShader)
        return program
    }
}
