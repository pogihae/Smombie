package com.example.smombie.analyzer

interface Analyzer {
    fun startAnalyze()
    fun stopAnalyze()

    enum class State {
        SAFE, WARNING, HAZARD
    }
}