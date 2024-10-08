package de.cyface.anonymizationtest3

import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import de.cyface.anonymizationtest3.model.Model
import de.cyface.anonymizationtest3.model.YoloDetector
import de.cyface.anonymizationtest3.ui.theme.AnonymizationTest3Theme

/**
 * This is the central an only activity used in this app. It shows a static image for anonymization and two buttons.
 * One button allows to anonymize the image, while the other allows to rollback to process.
 *
 * @author Klemens Muthmann
 */
class MainActivity : ComponentActivity() {

    /**
     * The implementation here is pretty much taken over from the example app generated by
     * Android Studio.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnonymizationTest3Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }

    /**
     * The main screen holds the applications state and builds the view tree.
     * The layout uses material design, thus the buttons are two elevated buttons.
     * The image is either the anonymized image from the state if that exists or the standard image
     * otherwise.
     * The view tree is a column with an image and two buttons below that.
     */
    @Composable
    fun MainScreen(modifier: Modifier, model: Model = viewModel()) {
        val state by model.state.collectAsState()
        val image = ImageBitmap.imageResource(id = model.imageResource)
        val detector = YoloDetector(
            this,
            "yolov8n_2048px_float32.tflite",
            null,
            model) { message ->
            Log.d("TEST", message)
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

    /**
     * Show the preview during development.
     */
    @Preview(showBackground = true)
    @Composable
    fun Preview() {
        AnonymizationTest3Theme {
            MainScreen(Modifier.padding())
        }
    }
}
