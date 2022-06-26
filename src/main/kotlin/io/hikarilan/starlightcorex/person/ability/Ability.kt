package io.hikarilan.starlightcorex.person.ability

interface Ability<T> {

    val key: String

    val value: T

    var ambiguousName: String
}

data class DoubleAbility(override val key: String, override val value: Double, override var ambiguousName: String="") : Ability<Double>

data class BooleanAbility(override val key: String, override var ambiguousName: String="") : Ability<Boolean> {
    override val value: Boolean = true
}