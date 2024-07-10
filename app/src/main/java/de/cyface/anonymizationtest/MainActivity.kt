package de.cyface.anonymizationtest

import SampleData
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.gpu.support.TfLiteGpu
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import de.cyface.anonymizationtest.ui.theme.AnonymizationTestTheme
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.gms.vision.TfLiteVision
import org.tensorflow.lite.task.gms.vision.detector.ObjectDetector
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            /*AnonymizationTestTheme {
                Conversation(messages = SampleData.conversationSample)
            }*/
            var isAnonymized by remember { mutableStateOf(false)}
            val originalBitmap = ImageBitmap.imageResource(id = R.drawable.license_plate)
            val anonymizedBitmap = remember { mutableStateOf(originalBitmap) }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    bitmap = if (isAnonymized) anonymizedBitmap.value else originalBitmap,
                    contentDescription = "People in traffic",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isAnonymized) {
                        // show the original image
                        anonymizedBitmap.value = originalBitmap
                    } else {

                        val image = anonymizedBitmap.value.asAndroidBitmap()

                        anonymizeLicensePlates(image) { anonymizedImage ->
                            anonymizedBitmap.value = anonymizedImage

                        }
                        /*anonymizeFaces(image) { anonymizedImage ->
                            anonymizedBitmap.value = anonymizedImage
                        }*/

                    }
                    isAnonymized = !isAnonymized
                }
            ) {
                Text(text = if (isAnonymized) "Original anzeigen" else "Anonymisieren")
            }
        }
    }

    private fun pixelate(original: Bitmap, pixelationArea: Rect, pixelationRadius: Int): Bitmap {
        val result = original.copy(original.config, true)
        val paint = Paint()

        for (i in pixelationArea.left until pixelationArea.right step pixelationRadius) {
            for (j in pixelationArea.top until pixelationArea.bottom step pixelationRadius) {
                val color = original.getPixel(i, j)
                paint.color = color
                val rect = Rect(i, j, i + pixelationRadius, j + pixelationRadius)
                Canvas(result).drawRect(rect, paint)
            }
        }

        return result
    }

    private fun anonymizeFaces(image: Bitmap, onSuccess: (ImageBitmap) -> Unit) {
// Anonymize Faces using MLKit
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()

        val detector = FaceDetection.getClient(options)

        // Load the image and detect faces
        val faceDetectionStart = System.currentTimeMillis()

        detector.process(image, 0)
            .addOnSuccessListener { faces ->

                var mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
                //val canvas = android.graphics.Canvas(mutableImage)

                for (face in faces) {
                    mutableImage = pixelate(mutableImage,face.boundingBox,10)
                }
                onSuccess(mutableImage.asImageBitmap())
                val faceDetectionStop = System.currentTimeMillis()
                Log.d("Test", "onCreate: Anonymized ${faces.size} Faces in Image in ${faceDetectionStop-faceDetectionStart} ms.")
            }
            .addOnFailureListener { e ->
                // Handle face detection failure
                e.printStackTrace()
            }
    }

    fun anonymizeLicensePlates(image: Bitmap, onSuccess: (ImageBitmap) -> Unit) {
        // Create a TfLiteInitializationOptions object and modify it to enable GPU support
        val context = applicationContext
        TfLiteGpu.isGpuDelegateAvailable(context).onSuccessTask { gpuAvailable ->
            val optionsBuilder =
                TfLiteInitializationOptions.builder()
            if (gpuAvailable) {
                optionsBuilder.setEnableGpuDelegateSupport(true)
            }
            // Use the TfLiteVision.initialize() method to enable use of the Play services runtime, and set a listener to verify that it loaded successfully
            TfLiteVision.initialize(context, optionsBuilder.build())
        }.addOnSuccessListener {
            onInitialized(image, context, onSuccess)
        }.addOnFailureListener {
            onError("TfLiteVision failed to initialize: "
                    + it.message)
        }
    }

    fun onInitialized(image: Bitmap, context: Context, onSuccess: (ImageBitmap) -> Unit) {
        // Set the options for model, such as the prediction threshold and results set size
        val threshold = 0.6f
        val maxResults = 10
        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(8)

        // Enable GPU acceleration with the options and allow the code to fail gracefully if acceleration is not supported on the device
        // TODO: Find out what happened to 'useGpu'. Obviously it moved in recent versions of the code.
        /*try {
            baseOptionsBuilder.useGpu()
        } catch(e: Exception) {
            onError("GPU is not supported on this device")
        }*/
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .setBaseOptions(baseOptionsBuilder.build())

        // Use the settings from this object to construct a TensorFlow Lite ObjectDetector object that contains the model
        val modelName = "1.tflite"
        val objectDetector =
            ObjectDetector.createFromFileAndOptions(
                context,
                modelName,
                optionsBuilder.build()
            )

        val normalizedHeight = 300
        val normalizedWidth = 300
        val imageProcessor = ImageProcessor
            .Builder()
            .add(ResizeOp(
                normalizedHeight,
                normalizedWidth,
                ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
            ))
            .build()
        val tensorizedImage = imageProcessor.process(TensorImage.fromBitmap(image))

        val results = objectDetector.detect(tensorizedImage)

        var mutableImage = image.copy(Bitmap.Config.ARGB_8888, true)
        //results.forEach { detection ->
            val boundingBoxInOriginalImageSize = results[3].boundingBox.convert(
                image.height,
                normalizedHeight,
                image.width,
                normalizedWidth
            )
            mutableImage = pixelate(mutableImage,boundingBoxInOriginalImageSize , 10)
        //}
        onSuccess(mutableImage.asImageBitmap())
    }



    fun onError(message: String) {
        Log.e("OBJ_DTCT", message)
    }

    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }

        context.assets.open(assetName).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while ((`is`.read(buffer).also { read = it }) != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }
}

fun RectF.convert(
    originalHeight: Int,
    normalizedHeight: Int,
    originalWidth: Int,
    normalizedWidth: Int): Rect {
    val ret = Rect()
    val widthScaleFactor = originalWidth.toFloat() / normalizedWidth.toFloat()
    val heightScaleFactor = originalHeight.toFloat() / normalizedHeight.toFloat()
    RectF(
        left * widthScaleFactor,
        top * heightScaleFactor,
        right * widthScaleFactor,
        bottom * heightScaleFactor,
    ).round(ret)

    return ret
}

data class Message(val author: String, val body: String)

@Composable
fun MessageCard(msg: Message) {
    Row(modifier = Modifier.padding(all = 8.dp)) {
        Image(
            painter = painterResource(R.drawable.people_in_traffic),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        var isExpanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                shape = MaterialTheme.shapes.medium, shadowElevation = 1.dp
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {
    MessageCard(Message("Geordi", "Hey Data! Could you please support me in loosening the EPS couplings!"))
}

@Composable
fun Conversation(messages: List<Message>) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(message)
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    AnonymizationTestTheme {
        Conversation(messages = SampleData.conversationSample)
    }
}
