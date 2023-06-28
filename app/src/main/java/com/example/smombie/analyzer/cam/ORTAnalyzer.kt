package com.example.smombie.analyzer.cam

import ai.onnxruntime.*
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.smombie.*
import com.example.smombie.util.*
import java.util.*
import kotlin.math.exp


class ORTAnalyzer(
    private val ortSession: OrtSession?,
    private val onAnalyzeFinish: (String, Float) -> Unit
) : ImageAnalysis.Analyzer {

    private val allLabels = listOf(
        "SIDEWALK", "ROAD", "CROSSWALK", "STAIR", "DOOR", "ENTRY"
    )

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

                        @Suppress("UNCHECKED_CAST")
                        val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]
                        val result = softMax(rawOutput).withIndex().maxBy { it.value }
                        val label = allLabels[result.index]

                        onAnalyzeFinish(label, result.value)
                    }
                }
            }
        }

        image.close()
    }

    protected fun finalize() {
        ortSession?.close()
    }
}