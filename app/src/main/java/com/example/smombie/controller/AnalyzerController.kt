package com.example.smombie.controller

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.smombie.analyzer.Analyzer
import com.example.smombie.analyzer.cam.CameraAnalyzer
import com.example.smombie.analyzer.gyro.GyroAnalyzer
import com.example.smombie.analyzer.mic.MicAnalyzer
import com.example.smombie.ui.AlertPreviewView
import com.example.smombie.ui.AlertTextView



// 구현 시 주의
// 1. 다른 분석 방법이 추가 되어도 코드 변경을 최소화 할 수 있는지 - 추상화
// 2. 경고 단계가 변화하며 필요한 리소스들이 할당 및 해제가 되는지 - 카메라 외


/**
 *
 * 각 단계별 사용할 분석 방법을 할당 및 해제해줌,
 * 단계에 맞는 뷰를 표시 및 숨겨줌
 *
 * @param context
 * @param usedAnalyzers 사용하는 분석 방법, 정수 (or 비트 연산으로 표시)
 *
 * @author 박민석 (2022.12)
 * */
class AnalyzerController(context: Context, usedAnalyzers: Int) {

    private val analyzers: Map<Int, Analyzer>
    private val registeredAnalyzers = mutableListOf<Analyzer>()

    private val alertPreviewView = AlertPreviewView(context)
    private val alertTextView = AlertTextView(context)
    private val uiHandler = Handler(Looper.getMainLooper())

    init {
        val mAnalyzers = mutableMapOf<Int, Analyzer>()
        val analyzerIds = listOf(ANALYZER_CAMERA, ANALYZER_MIC, ANALYZER_GYRO)

        for (id in analyzerIds) {
            if ((id or usedAnalyzers) != 0) {
                mAnalyzers[id] = createAnalyzer(context, id)
            }
        }

        analyzers = mAnalyzers
    }

    /**
     * 등록된 분석 방법들로 분석을 시작함.
     * 사전에 시작 분석 방법 등록해야함
     * */
    fun run() {
        analyzers.forEach { it.value.stopAnalyze() }
        registeredAnalyzers.forEach { it.startAnalyze() }
    }

    fun terminate() {
        analyzers.forEach { it.value.stopAnalyze() }
    }

    /**
     * 초기 분석 방법 등록을 위한 메소드
     * */
    fun registerAnalyzer(id: Int) {
        val analyzer = analyzers[id] ?: throw IllegalArgumentException()
        registeredAnalyzers.add(analyzer)
    }

    private fun createAnalyzer(context: Context, id: Int): Analyzer {
        return when (id) {
            ANALYZER_CAMERA -> CameraAnalyzer(context) { _, state ->
                updateUI(state)
                changeAnalyzer(state)
            }
            ANALYZER_MIC -> MicAnalyzer(context) { _, state ->
                updateUI(state)
                changeAnalyzer(state)
            }
            else -> GyroAnalyzer(context) { _, state ->
                updateUI(state)
                changeAnalyzer(state)
            }
        }
    }

    private fun changeAnalyzer(state: Analyzer.State) {
        when (state) {
            Analyzer.State.SAFE -> {
                analyzers[ANALYZER_CAMERA]?.let {
                    registeredAnalyzers.clear()
                    registeredAnalyzers.add(it)
                }
            }

            Analyzer.State.WARNING -> {
                if (analyzers.size > 1) {
                    registeredAnalyzers.clear()
                    analyzers.forEach {
                        if (it.key != ANALYZER_CAMERA) {
                            registeredAnalyzers.add(it.value)
                        }
                    }
                }
            }

            Analyzer.State.HAZARD -> {
                analyzers[ANALYZER_CAMERA]?.let {
                    registeredAnalyzers.clear()
                    registeredAnalyzers.add(it)
                }
            }
        }
    }

    private fun updateUI(state: Analyzer.State) {
        uiHandler.post {
            when (state) {
                Analyzer.State.SAFE -> {
                    alertPreviewView.hide()
                    alertTextView.startAlert()
                }

                Analyzer.State.WARNING -> {
                    alertPreviewView.hide()
                    alertTextView.startAlert()
                }

                Analyzer.State.HAZARD -> {
                    alertTextView.hide()
                    alertPreviewView.show()
                    alertPreviewView.startAlert()
                }
            }
        }
    }

    companion object {
        const val ANALYZER_CAMERA = 1
        const val ANALYZER_MIC = 2
        const val ANALYZER_GYRO = 4
    }
}
