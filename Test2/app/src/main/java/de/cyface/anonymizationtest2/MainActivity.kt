package de.cyface.anonymizationtest2

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import de.cyface.anonymizationtest2.ml.AutoModel2
import de.cyface.anonymizationtest2.ui.theme.AnonymizationTest2Theme
import org.tensorflow.lite.support.image.TensorImage

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnonymizationTest2Theme {
                Scaffold(modifier = Modifier.fillMaxSize(), floatingActionButton = {
                    button(applicationContext)
                }) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun button(context: Context) {
    Button(onClick = {
        val start = System.currentTimeMillis()
        val model = AutoModel2.newInstance(context)

        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.streetscene)
        val image = TensorImage.fromBitmap(bitmap)

        // Runs model inference and gets result.
        val outputs = model.process(image)

        // Gets result from DetectionResult.
        for(result in outputs.detectionResultList) {
            // This location is scaled to the normalized image
            val location = result.locationAsRectF;
            val category = result.categoryAsString;
            val score = result.scoreAsFloat;

            Log.d("test", "Detected a ${category} at $location with confidence $score")
        }
        Log.d("test", "${outputs.detectionResultList.size}")

        // Releases model resources if no longer used.
        model.close()
        Log.d("test", "Took ${System.currentTimeMillis()-start} ms")
    }) {
        Text("Test")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AnonymizationTest2Theme {
        Greeting("Android")
    }
}