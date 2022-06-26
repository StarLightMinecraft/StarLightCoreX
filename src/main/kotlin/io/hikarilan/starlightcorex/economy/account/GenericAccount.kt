package io.hikarilan.starlightcorex.economy.account

import io.hikarilan.starlightcorex.generic.ProxiedPermissible
import io.hikarilan.starlightcorex.person.Human

/**
 * 非玩家账户
 */
interface GenericAccount : Account, ProxiedPermissible<Human>