package de.cyface.anonymizationtest3.model

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.ViewModel
import de.cyface.anonymizationtest3.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant

/**
 * This app follows the MVVM pattern.
 * This class realizes the view model of that pattern.
 *
 * @author Klemens Muthmann
 */
class Model(
    val imageResource: Int = R.drawable.license_plate,
): ViewModel(), YoloDetector.DetectorListener {
    /**
     * The current state of the application.
     * Alays us `_state.update` to set the models values, otherwise the view will not notice updates, and thus fail to redraw.
     */
    private val _state = MutableStateFlow(State())

    /**
     * A flow that notifies the view to redraw on update.
     */
    val state: StateFlow<State> = _state.asStateFlow()
    private var imageReadyForAnonymization: Bitmap? = null
    private var startTime = Instant.now()

    fun anonymize(image: Bitmap, detector: YoloDetector) {
        startTime = Instant.now()
        imageReadyForAnonymization = image
        detector.detect(image)
    }

    fun deanonymize(image: Bitmap) {
        _state.update {
            it.copy(anonymizedImage = image)
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

    override fun onEmptyDetect() {
        Log.d("CYFACE-DEBUG", "Empty Detection")
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        imageReadyForAnonymization?.let { image ->
            var pixelatedBitmap = image
            boundingBoxes.forEach { boundingBox ->

                val left = (boundingBox.x1 * image.width).toInt()
                val top = (boundingBox.y1 * image.height).toInt()
                val right = (boundingBox.x2 * image.width).toInt()
                val bottom = (boundingBox.y2 * image.height).toInt()

                pixelatedBitmap = pixelate(
                    pixelatedBitmap,
                    Rect(
                        left,
                        top,
                        right,
                        bottom
                    ),
                    20
                )
            }
            _state.update {
                it.anonymizedImage = pixelatedBitmap
                val now = Instant.now()
                it.copy(inferenceTime = now.minusMillis(startTime.toEpochMilli()).toEpochMilli().toString())
            }
        }
    }
}

/**
 * The models state, consists of the lastly measured inference time and an anonymized image.
 *
 * @author Klemens Muthmann
 */
data class State (
    var anonymizedImage: Bitmap? = null,
    var inferenceTime: String = ""
)
