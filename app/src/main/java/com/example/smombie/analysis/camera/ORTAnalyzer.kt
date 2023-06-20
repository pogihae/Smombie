package com.example.smombie.analysis.camera

import ai.onnxruntime.*
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.smombie.*
import com.example.smombie.util.*
import java.util.*
import kotlin.math.exp

data class AnalysisResult(
    val detectedLabel: String,
    val isSafe: Boolean
)

class ORTAnalyzer(
    private val ortSession: OrtSession?,
    private val updateUICallback: (AnalysisResult) -> Unit
) : ImageAnalysis.Analyzer {

    private fun softMax(modelResult: FloatArray): FloatArray {
        val values = modelResult.copyOf()
        val max = values.max()
        var sum = 0.0f

        for (i in values.indices) {
            values[i] = exp(values[i] - max)
            sum += values[i]
        }

        if (sum != 0.0f) {
            for (i in values.indices) {
                values[i] /= sum
            }
        }

        return values
    }

    override fun analyze(image: ImageProxy) {
        val imgBitmap = image.toBitmap()
        val rawBitmap =
            imgBitmap.let { Bitmap.createScaledBitmap(it, IMAGE_SIZE_X, IMAGE_SIZE_Y, false) }
        val bitmap = rawBitmap?.rotate(image.imageInfo.rotationDegrees.toFloat())

        var analysisResult: AnalysisResult? = null

        if (bitmap != null) {
            val imgData = preProcess(bitmap)
            val inputName = ortSession?.inputNames?.iterator()?.next()
            val shape = longArrayOf(1, 3, IMAGE_SIZE_X.toLong(), IMAGE_SIZE_Y.toLong())
            val env = OrtEnvironment.getEnvironment()
            env.use {
                val tensor = OnnxTensor.createTensor(env, imgData, shape)
                tensor.use {
                    val output = ortSession?.run(Collections.singletonMap(inputName, tensor))
                    output.use {
                        @Suppress("UNCHECKED_CAST") val rawOutput =
                            ((output?.get(0)?.value) as Array<FloatArray>)[0]
                        val result = softMax(rawOutput).withIndex().maxBy { it.value }
                        val label = LABELS[result.index]
                        val isSafe = label in SAFE_LABELS && result.value > THRESHOLD
                        Log.d("RES", "$label ${result.value}")
                        analysisResult = AnalysisResult(label, isSafe)
                    }
                }
            }
            analysisResult?.let(updateUICallback)
        }

        image.close()
    }

    protected fun finalize() {
        ortSession?.close()
    }

    companion object {
        val LABELS = listOf(
            "SIDEWALK", "ROAD", "CROSSWALK", "STAIR", "DOOR"
        )
        val SAFE_LABELS = listOf(
            "SIDEWALK"
        )
        private const val THRESHOLD = 0.85f
    }
}