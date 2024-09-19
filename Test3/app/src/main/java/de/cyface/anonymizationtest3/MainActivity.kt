package de.cyface.anonymizationtest3

import android.content.res.AssetManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import de.cyface.anonymizationtest3.model.BoundingBox
import de.cyface.anonymizationtest3.model.Model
import de.cyface.anonymizationtest3.model.YoloDetector
import de.cyface.anonymizationtest3.ui.theme.AnonymizationTest3Theme
import java.nio.ByteBuffer
import java.nio.channels.Channels

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnonymizationTest3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnonymizationApp(Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun AnonymizationApp(modifier: Modifier) {
        MainScreen(modifier)
    }

    @Composable
    fun MainScreen(modifier: Modifier, model: Model = viewModel()) {
        val state by model.state.collectAsState()
        val image = ImageBitmap.imageResource(id = model.imageResource)
        val detector = YoloDetector(
            this,
            "yolov8n_640px_float32.tflite",
            null,
            model) { message ->
            Log.d("TEST", message)
            /*runOnUiThread {
                Toast.makeText(baseContext, message, Toast.LENGTH_LONG).show()
            }*/
        }

        Surface(color = MaterialTheme.colorScheme.primary, modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    if (state.anonymizedImage == null) image else state.anonymizedImage!!.asImageBitmap(),
                    contentDescription = null
                )
                ElevatedButton(onClick = {
                    model.anonymize(image.asAndroidBitmap(), detector)
                }) {
                    Text(text = "Anonymize")
                }
                ElevatedButton(onClick = {
                    model.deanonymize(image.asAndroidBitmap())
                }) {
                    Text(text = "Deanonymize")
                }
                Text(text = "Inference Time: ${state.inferenceTime}")
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun Preview() {
        AnonymizationTest3Theme {
            AnonymizationApp(Modifier.padding())
        }
    }
}
