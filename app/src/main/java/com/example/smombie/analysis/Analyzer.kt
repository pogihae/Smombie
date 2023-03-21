package com.example.smombie.analysis

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

abstract class Analyzer(parentLifecycleOwner: LifecycleOwner) : LifecycleOwner,
    DefaultLifecycleObserver {

    private val lifecycleRegistry = LifecycleRegistry(this)

    init {
        parentLifecycleOwner.lifecycle.addObserver(this)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    open fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    open fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        stop()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }
}