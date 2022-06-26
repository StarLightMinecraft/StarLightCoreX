package io.hikarilan.starlightcorex.utils

import kotlin.random.Random

class ScalarRandomHelperWithValues<T> {

    private val list: MutableList<RandomElement> = mutableListOf()

    fun addElement(element: RandomElement) {
        list.add(element)
    }

    fun invoke(): T {
        val mapped =
            (1 until list.size).associate {
                list.subList(0, it).map { v -> v.chance }.reduce { acc, d -> acc * d } to list[it].value
            }
        val iter = mapped.iterator()
        val random = Random.nextDouble(mapped.keys.sum())
        var now = 0.0
        while (iter.hasNext()) {
            iter.next().run {
                now += this.key
                if (now >= random) {
                    return this.value
                }
            }
        }
        throw IllegalStateException("No value present")
    }

    inner class RandomElement(val chance: Double, val value: T)
}

class RandomHelperWithValues<T> {

    private val list: MutableList<RandomElement> = mutableListOf()

    // 总概率
    private val maxChance by lazy { list.sumOf { it.chance } }

    fun addElement(element: RandomElement) {
        list.add(element)
    }

    fun invoke(): T {
        findElementFromRandom(Random.nextDouble(maxChance)).also { element ->
            return element?.value ?: throw IllegalStateException("no value present")
        }
    }

    private fun findElementFromRandom(random: Double): RandomElement? {
        val iter = list.iterator()
        var now = 0.0
        while (iter.hasNext()) {
            iter.next().run {
                now += this.chance
                if (now >= random) {
                    return this
                }
            }
        }
        return null
    }

    inner class RandomElement(val chance: Double, val value: T)
}