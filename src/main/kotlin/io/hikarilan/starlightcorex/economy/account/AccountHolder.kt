package io.hikarilan.starlightcorex.economy.account

import io.hikarilan.starlightcorex.generic.Nameable
import io.hikarilan.starlightcorex.generic.Unique
import io.hikarilan.starlightcorex.utils.MailReceiver

interface AccountHolder : Nameable, Unique, MailReceiver {

    val account: Account

}