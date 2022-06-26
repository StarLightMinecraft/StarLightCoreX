package io.hikarilan.starlightcorex.utils

import io.hikarilan.starlightcorex.StarLightCoreX

interface PluginInitializeModule {

    fun init(plugin: StarLightCoreX)

    fun destroy(plugin: StarLightCoreX) {}

    fun reload(plugin: StarLightCoreX) {
        destroy(plugin)
        init(plugin)
    }

    fun tick(plugin: StarLightCoreX, currentTick: Long) {
    }

}