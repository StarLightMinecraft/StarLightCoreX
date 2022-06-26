package io.hikarilan.starlightcorex.storage

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.economy.account.Account
import io.hikarilan.starlightcorex.politics.country.account.CountryAccount
import io.hikarilan.starlightcorex.economy.currency.Currency
import io.hikarilan.starlightcorex.person.account.HumanCashAccount
import io.hikarilan.starlightcorex.generic.RootSerializable
import io.hikarilan.starlightcorex.person.Human
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.politics.country.Country
import io.hikarilan.starlightcorex.storage.GenericStorage.addToStorage
import io.hikarilan.starlightcorex.utils.GeneralUtils.toNonNullTyped
import io.hikarilan.starlightcorex.utils.GeneralUtils.toNullableTyped
import io.hikarilan.starlightcorex.utils.PluginInitializeModule
import io.hikarilan.starlightcorex.utils.gson
import io.hikarilan.starlightcorex.utils.scheduler
import org.bukkit.scheduler.BukkitTask
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.reflect.typeOf

inline fun <reified T> getStorageFor(uniqueId: UUID): T? {
    return when (typeOf<T>()) {
        typeOf<TechnicalPlayer>() -> GenericStorage.technicalPlayers[uniqueId].toNullableTyped()
        typeOf<Human>() -> getAllStorageFor<Human>().find { it.uniqueId == uniqueId }.toNullableTyped()
        typeOf<HumanCashAccount>() -> getAllStorageFor<HumanCashAccount>().find { it.uniqueId == uniqueId }
            .toNullableTyped()
        typeOf<Country>() -> GenericStorage.countries[uniqueId].toNullableTyped()
        typeOf<CountryAccount>() -> getAllStorageFor<CountryAccount>().find { it.uniqueId == uniqueId }
            .toNullableTyped()
        typeOf<Currency>() -> getAllStorageFor<T>().find { (it as Currency).uniqueId == uniqueId }.toNullableTyped()
        typeOf<Account>() -> getAllStorageFor<Account>().find { it.uniqueId == uniqueId }.toNullableTyped()
        else -> null
    }
}

inline fun <reified T> getAllStorageFor(): Collection<T> {
    return when (typeOf<T>()) {
        typeOf<TechnicalPlayer>() -> GenericStorage.technicalPlayers.values.toNonNullTyped()
        typeOf<Human>() -> GenericStorage.technicalPlayers.flatMap { it.value.humans }.toNonNullTyped()
        typeOf<HumanCashAccount>() -> GenericStorage.technicalPlayers.flatMap { it.value.humans }.map { it.account }
            .toNonNullTyped()
        typeOf<Country>() -> GenericStorage.countries.values.toNonNullTyped()
        typeOf<CountryAccount>() -> GenericStorage.countries.values.map { it.account }.toNonNullTyped()
        typeOf<Currency>() -> GenericStorage.countries.mapNotNull { it.value.currency }.filterIsInstance<T>()
            .toNonNullTyped()
        typeOf<Account>() -> (
                GenericStorage.technicalPlayers.flatMap { it.value.humans }.map { it.account }
                        + GenericStorage.countries.values.map { it.account }
                )
            .toCollection(mutableListOf())
            .toNonNullTyped()
        else -> emptyList()
    }
}

inline fun <reified T : RootSerializable> getOrCreateStorageFor(uniqueId: UUID, func: () -> T): T {
    return getStorageFor<T>(uniqueId) ?: func().also { it.addToStorage(uniqueId) }
}

object GenericStorage : PluginInitializeModule {

    val technicalPlayers = mutableMapOf<UUID, TechnicalPlayer>()
    val countries = mutableMapOf<UUID, Country>()

    lateinit var task: BukkitTask

    inline fun <reified T : RootSerializable> T.addToStorage(uniqueId: UUID) {
        when (typeOf<T>()) {
            typeOf<TechnicalPlayer>() -> technicalPlayers[uniqueId] = this as TechnicalPlayer
            typeOf<Country>() -> countries[uniqueId] = this as Country
            else -> throw IllegalArgumentException("Storage for ${typeOf<T>()} is not supported yet")
        }
    }

    override fun init(plugin: StarLightCoreX) {
        loadPlayers(plugin)
        loadCountries(plugin)
        task = scheduler.runTaskTimerAsynchronously(plugin, Runnable {
            destroy(plugin)
        }, 5 * 60 * 20L, 5 * 60 * 20L)
    }

    override fun destroy(plugin: StarLightCoreX) {
        savePlayers(plugin)
        saveCountries(plugin)
    }

    private fun loadPlayers(plugin: StarLightCoreX) {
        val players = plugin.dataFolder.toPath().resolve(Path.of("players")).createDirectories()
        players.forEachDirectoryEntry(glob = "*.json") { file ->
            if (file.isRegularFile()) {
                gson.fromJson(file.reader(), TechnicalPlayer::class.java).let {
                    this.technicalPlayers[it.uniqueId] = it
                }
            }
        }
    }

    private fun loadCountries(plugin: StarLightCoreX) {
        val countries = plugin.dataFolder.toPath().resolve(Path.of("countries")).createDirectories()
        countries.forEachDirectoryEntry(glob = "*.json") { file ->
            if (file.isRegularFile()) {
                gson.fromJson(file.reader(), Country::class.java).let {
                    this.countries[it.uniqueId] = it
                }
            }
        }
    }

    private fun savePlayers(plugin: StarLightCoreX) {
        plugin.logger.info("Saving players...")
        val players = plugin.dataFolder.toPath().resolve(Path.of("players"))
        this.technicalPlayers.forEach { (t, u) ->
            players.resolve(Path.of("$t.json")).also {
                if (!it.exists()) it.createFile()
            }.writeText(gson.toJson(u))
        }
    }

    private fun saveCountries(plugin: StarLightCoreX) {
        plugin.logger.info("Saving countries...")
        val countries = plugin.dataFolder.toPath().resolve(Path.of("countries"))
        this.countries.forEach { (t, u) ->
            countries.resolve(Path.of("$t.json")).also {
                if (!it.exists()) it.createFile()
            }.writeText(gson.toJson(u))
        }
    }


}