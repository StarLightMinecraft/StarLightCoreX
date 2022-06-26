package io.hikarilan.starlightcorex.region

import io.hikarilan.starlightcorex.person.Human
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location

data class Region(
    val world: String,
    val x: Int,
    val z: Int
) {

    constructor(chunk: Chunk) : this(chunk.world.name, chunk.x, chunk.z)

    fun checkIn(location:Location):Boolean{
        return location.chunk.let { it.world.name == world && it.x == x && it.z == z }
    }

    fun checkIn(human: Human): Boolean {
        return checkIn(human.location)
    }

}
