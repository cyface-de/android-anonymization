package de.cyface.anonymizationtest3

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Picture
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.cyface.anonymizationtest3.model.BoundingBox
import de.cyface.anonymizationtest3.model.YoloDetector

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.util.concurrent.CountDownLatch

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("de.cyface.anonymizationtest3", appContext.packageName)
    }

    @Test
    fun objectDetectionHappyPath() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val objectDetector = ObjectDetector(context, "yolov8n_640px_float32.tflite")
        val latch = CountDownLatch(1)

        val image = context.getDrawable(R.drawable.license_plate)?.toBitmap()

        if(image!=null) {
            objectDetector.initialize(
                onSuccess = {
                    Log.d("TEST", "initialized")
                    val result = objectDetector.detect(image)
                    for(index in 0..100) {
                        Log.d("TEST", "${result.get(index)}")
                    }
                    latch.countDown()
                }, onFailure = {
                    Log.d("Test", "initializion failed")
                    fail()
                    latch.countDown()
                    1
                }
            )
        }
        latch.await()

    }

    @Test
    fun yoloObjectDetectionHappyPath() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val detector = YoloDetector(
            context,
            "yolov8n_640px_float32.tflite",
            null,
            object : YoloDetector.DetectorListener {
                override fun onEmptyDetect() {
                    Log.d("TEST", "onEmptyDetect")
                }

                override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
                    boundingBoxes.forEach { box -> Log.d("TEST", "onDetect $box") }
                }

            }
        ) { message ->
            Log.d("TEST", message)
        }

        val image = context.getDrawable(R.drawable.license_plate)?.toBitmap()
        if (image != null) {
            detector.detect(image)
        }
    }
}