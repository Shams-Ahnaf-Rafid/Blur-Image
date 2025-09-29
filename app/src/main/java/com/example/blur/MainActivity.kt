package com.example.blur

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DrawingScreen()
                }
            }
        }
    }
}

@Composable
fun DrawingScreen() {
    val context = LocalContext.current
    val myGLSurfaceView = remember { MyGLSurfaceView(context) }
    var sliderPosition by remember { mutableStateOf(50f) }
    var toggleState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load original bitmap
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.background)

        // Optionally scale down for ML Kit to improve speed
        val scaledInput = Bitmap.createScaledBitmap(bitmap, 480, 640, true)

        myGLSurfaceView.renderer.segmentWithMLKit(scaledInput) { mask ->
            logMaskPixels(mask)
            val matrix = android.graphics.Matrix().apply { preScale(1f, -1f) }
            val flippedMask = Bitmap.createBitmap(mask, 0, 0, mask.width, mask.height, matrix, false)
            myGLSurfaceView.queueEvent {
                myGLSurfaceView.renderer.updateMaskTexture(flippedMask)
            }
        }
    }




    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { myGLSurfaceView },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Blur: ${sliderPosition.toInt()}")
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                    myGLSurfaceView.setBlurAmount(it)
                },
                valueRange = 0f..100f,
                modifier = Modifier
                    .padding(horizontal = 50.dp)
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                toggleState = !toggleState
                myGLSurfaceView.click()
            }) {
                Text(if (toggleState) "Unblur" else "Blur")
            }
        }
    }
}

fun logMaskPixels(mask: Bitmap) {
    val width = mask.width
    val height = mask.height

    for (y in 0 until minOf(1000, height)) {
        var row = ""
        for (x in 0 until minOf(10, width)) {
            val pixel = mask.getPixel(x, y)
            row += if (pixel != 0) "1 " else "0 "
        }
        android.util.Log.d("MLMask", row)
    }
}
