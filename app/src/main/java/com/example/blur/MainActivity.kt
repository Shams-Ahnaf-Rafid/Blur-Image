package com.example.blur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color


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
    var txt = remember { mutableStateOf(0)}
    var sliderPosition = remember { mutableStateOf(50f) }

    val myGLSurfaceView = remember {
        MyGLSurfaceView(context)
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
//                .background(Color(0xFF5E5454))
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(text = "Value: ${sliderPosition.value.toInt()}")

            Slider(
                value = sliderPosition.value,
                onValueChange = {
                    sliderPosition.value = it
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
                .background(Color(0xFF110E0E))
                .padding(16.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                txt.value++
                myGLSurfaceView.click()
            }) {
                if (txt.value % 2 == 1) Text("Blur")
                else Text("Unblur")
            }
        }
    }
}

