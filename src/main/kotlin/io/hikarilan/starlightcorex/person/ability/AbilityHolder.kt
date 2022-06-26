package io.hikarilan.starlightcorex.person.ability

interface AbilityHolder {

    fun hasAbility(key: String): Boolean

    fun getAbility(key: String): Ability<*>

    fun getAbilities(): List<Ability<*>>

}

interface AbilityHolderDefaultImpl {

    val abilitiesList: List<Ability<*>>

    fun hasAbility(key: String): Boolean {
        return abilitiesList.any { it.key == key }
    }

    fun getAbility(key: String): Ability<*> {
        return abilitiesList.first { it.key == key }
    }

    fun getAbilities(): List<Ability<*>> {
        return abilitiesList
    }

}