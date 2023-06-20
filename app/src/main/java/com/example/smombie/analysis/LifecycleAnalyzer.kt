package com.example.smombie.analysis

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

abstract class LifecycleAnalyzer(parentLifecycleOwner: LifecycleOwner) : LifecycleOwner,
    DefaultLifecycleObserver {

    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        parentLifecycleOwner.lifecycle.addObserver(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        onStart()
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        onStop()
    }

    abstract fun onStart()
    abstract fun onStop()

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stop()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}