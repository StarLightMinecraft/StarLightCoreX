package io.hikarilan.starlightcorex.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.person.ability.Ability
import io.hikarilan.starlightcorex.utils.serialization.AbilitySerializer
import io.hikarilan.starlightcorex.utils.serialization.CurrencySerializer
import io.hikarilan.starlightcorex.utils.serialization.ItemStackSerializer
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.HeightMap
import org.bukkit.Location
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.DecimalFormat
import java.time.Instant
import java.util.*
import kotlin.math.atan2
import kotlin.random.Random

val gson: Gson = GsonComponentSerializer.gson()
    .serializer()
    .newBuilder()
    .serializeNulls()
    .registerTypeAdapter(ItemStack::class.java, ItemStackSerializer)
    .registerTypeHierarchyAdapter(Currency::class.java, CurrencySerializer)
    .registerTypeHierarchyAdapter(Ability::class.java, AbilitySerializer)
    .setPrettyPrinting()
    .create()

val currencyFormatter = DecimalFormat("#,###,###,###,##0.00")

val scheduler = Bukkit.getScheduler()

const val MAX_VOLUME = 99999.0f

const val DEFAULT_PITCH = 1.0f

object GeneralUtils {

    val legacyComponentSerializer = LegacyComponentSerializer.legacySection()

    val plainComponentSerializer = PlainTextComponentSerializer.plainText()

    fun Listener.registerListener(plugin: Plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    fun Instant.format(
        format: Int = DateFormat.MEDIUM,
        locale: Locale = Locale.getDefault(Locale.Category.FORMAT)
    ): String {
        return DateFormat.getDateTimeInstance(format, format, locale).format(Date.from(this))
    }

    fun Random.nextSign(): Int = if (nextBoolean()) 1 else -1

    fun randomTeleport(): Location {
        return Bukkit.getWorlds()[0].let {
            it.worldBorder.center.clone().add(
                Random.nextSign() * Random.nextDouble(it.worldBorder.size / 2),
                0.0,
                Random.nextSign() * Random.nextDouble(it.worldBorder.size / 2)
            )
        }
    }

    fun randomTeleportSafe(): Location {
        return randomTeleport().toHighestLocation(HeightMap.MOTION_BLOCKING).add(0.0, 1.0, 0.0)
    }

    val <T> T.gsonType: Type
        get() = object : TypeToken<T>() {}.type

    inline fun <reified T> Any.toNonNullTyped(): T = this as T
    inline fun <reified T> Any?.toNullableTyped(): T? = this as T?

    fun Float.distanceToVolume(): Float {
        return this / 16.0f
    }

    fun getDirectionBetween(loc1: Location, loc2: Location): Double {
        val x = loc2.x - loc1.x
        val z = loc2.z - loc1.z
        return Math.toDegrees(atan2(z, x))
    }

}