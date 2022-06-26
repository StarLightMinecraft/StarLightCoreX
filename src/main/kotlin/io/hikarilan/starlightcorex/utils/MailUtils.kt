package io.hikarilan.starlightcorex.utils

import io.hikarilan.starlightcorex.StarLightCoreX
import io.hikarilan.starlightcorex.person.TechnicalPlayer
import io.hikarilan.starlightcorex.storage.getStorageFor
import io.hikarilan.starlightcorex.utils.GeneralUtils.format
import io.hikarilan.starlightcorex.utils.GeneralUtils.registerListener
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.time.Instant

object MailUtils : Listener, PluginInitializeModule {

    override fun init(plugin: StarLightCoreX) {
        registerListener(plugin)
    }

    @EventHandler
    private fun onJoin(e: PlayerJoinEvent) {
        getStorageFor<TechnicalPlayer>(e.player.uniqueId)?.currentHuman?.let { human ->
            human.receivedMails.forEach {
                human.sendMailImmediately(
                    it.copy(
                        message = Component.text("[离线邮件]").color(primaryColor)
                            .append(Component.text("[${Instant.ofEpochMilli(it.sendTime).format(locale = e.player.locale())}]").color(secondaryColorVariant))
                            .append(it.message)
                    )
                )
            }
            human.receivedMails.clear()
        }
    }

}

data class Mail(
    val message: Component,
    val sendTime: Long = System.currentTimeMillis(),
    val expiredWhen: Long = System.currentTimeMillis() + 1
)

interface MailReceiver {

    fun sendMail(mail: Mail)

    fun saveMail(mail: Mail)

}

interface ProxiedMailReceiver : MailReceiver {

    val receivers: List<DirectMailReceiver>

    override fun sendMail(mail: Mail) {
        receivers.forEach { it.sendMail(mail) }
    }

    override fun saveMail(mail: Mail) {
        receivers.forEach { it.receivedMails.add(mail) }
    }

}

interface DirectMailReceiver : MailReceiver {

    val receivedMails: MutableList<Mail>

    fun sendMessage(message: Component)

    fun isOnline(): Boolean

    override fun sendMail(mail: Mail) {
        if (isOnline()) {
            sendMailImmediately(mail)
            return
        }
        saveMail(mail)
    }

    fun sendMailImmediately(mail: Mail) {
        if (mail.expiredWhen < System.currentTimeMillis()) return
        sendMessage(mail.message)
    }

    override fun saveMail(mail: Mail) {
        receivedMails.add(mail)
    }

}