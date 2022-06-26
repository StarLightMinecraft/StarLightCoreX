package io.hikarilan.starlightcorex.utils

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.utils.GeneralUtils.format
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import java.nio.file.Path
import java.text.DateFormat
import java.time.Instant
import kotlin.io.path.Path
import kotlin.io.path.appendText
import kotlin.io.path.createFile
import kotlin.io.path.exists

fun log(message: String) {
    GlobalLogger.plugin.logger.info(message)
    GlobalLogger.storage.appendText("[${Instant.now().format(DateFormat.MEDIUM)}] $message\n")
}

fun log(message: Component) {
    GlobalLogger.plugin.logger.info(GeneralUtils.plainComponentSerializer.serialize(message))
    GlobalLogger.storage.appendText(GeneralUtils.plainComponentSerializer.serialize(
        Component.text("[${Instant.now().format(DateFormat.MEDIUM)}] ").append(message).append(Component.newline())
    ))
}

object GlobalLogger : PluginInitializeModule {

    internal lateinit var storage: Path

    lateinit var plugin: StarLightCoreX

    override fun init(plugin: StarLightCoreX) {
        this.plugin = plugin
        storage = Path(plugin.dataFolder.path, "log.txt")
        if (!storage.exists()) storage.createFile()
    }

}