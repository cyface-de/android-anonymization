package de.cyface.anonymizationtest3

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Task
/*import com.google.android.gms.tflite.java.TfLite*/
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.nio.FloatBuffer

class ObjectDetector(
    private val context: Context,
    private val modelFileName: String
)
{
    private lateinit var interpreter: InterpreterApi

    fun initialize(onSuccess: () -> Unit, onFailure: () -> Int) {
        /*val initializeTask: Task<Void> by lazy {
            TfLite.initialize(context)
        }
        initializeTask.addOnSuccessListener {
            val interpreterOption = InterpreterApi.Options().setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            // Finding this was really hard. It is not documented anywhere. You have to dig deep into example code.
            val model = FileUtil.loadMappedFile(context, modelFileName)
            interpreter = InterpreterApi.create(model, interpreterOption)
            onSuccess()
        }.addOnFailureListener { e ->
            Log.e("Interpreter", "Cannot initialize interpreter", e)
            onFailure()
        }*/
    }

    fun detect(image: Bitmap): FloatBuffer {
        Log.d("CYFACE", "Inputs: ${interpreter.inputTensorCount}")
        Log.d("CYFACE", "Outputs: ${interpreter.outputTensorCount}")

        // TODO: Eventuell Rotation und Resize hinzufügen.
        val tensorImage = imageProcessor().process(TensorImage.fromBitmap(image))
        val outputShape = interpreter.getOutputTensor(0).shape()
        val outputBuffer = FloatBuffer.allocate(outputShape[1] * outputShape[2])

        // TODO: Eventuel auch mit: runForMultipleInputsOutputs
        interpreter.run(tensorImage.tensorBuffer.buffer, outputBuffer)
        // Step 2: Initialize the detector object
        /*val options = org.tensorflow.lite.task.vision.detector.ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()
        val detector = org.tensorflow.lite.task.vision.detector.ObjectDetector.createFromFileAndOptions(
            context, // the application context
            "yolov8n_640px_float32.tflite", // must be same as the filename in assets folder
            options
        )
        return detector.detect(tensorImage)*/

        return outputBuffer
    }

    private fun imageProcessor(): ImageProcessor {
        // TODO: What does Normalize Operation do? Why is it successful if I add `NormalizeOp(1.0f, 1.0f)`?
        return ImageProcessor.Builder().add(ResizeOp(640,640, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR)).add(NormalizeOp(1.0f,1.0f)).build()
    }
}