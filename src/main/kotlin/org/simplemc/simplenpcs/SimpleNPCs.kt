package org.simplemc.simplenpcs

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitTask
import java.time.Duration

/**
 * SimpleNPCs plugin
 */
class SimpleNPCs : JavaPlugin(), Listener {
    private val bindPersistenceKey = NamespacedKey(this, "npcbind")
    private val awaitingBinds: MutableMap<CommandSender, Pair<BukkitTask, String?>> = mutableMapOf()
    private var bindTimeout: Long = 0

    override fun onEnable() {
        // ensure config file exists
        saveDefaultConfig()

        server.pluginManager.registerEvents(this, this)

        checkNotNull(getCommand("bindnpc")).setExecutor(::bindCommand)
        checkNotNull(getCommand("unbindnpc")).setExecutor(::unbindCommand)
        checkNotNull(getCommand("cancelnpc")).setExecutor(::cancelCommand)

        bindTimeout = Duration.parse(config.getString("bindtimeout", "PT10S"))
            .let {
                check(!it.isNegative) { "Bind timeout cannot be negative (Got $it)!" }
                it.seconds.secondsToTicks()
            }

        logger.info("${description.name} version ${description.version} enabled!")
    }

    /**
     * Bind a command to an entity for execution on player interaction
     */
    fun bind(entity: Entity, command: String) = entity.setBind(command)

    /**
     * Remove currently bound command from an entity
     */
    fun unbind(entity: Entity) = entity.removeBind()

    private fun bindCommand(sender: CommandSender, command: Command, label: String, vararg args: String): Boolean {
        if (args.isNotEmpty()) {
            awaitBind(sender, args.joinToString(separator = " "))
            return true
        }
        return false
    }

    private fun unbindCommand(sender: CommandSender, command: Command, label: String, vararg args: String): Boolean {
        awaitBind(sender, null)
        return true
    }

    private fun cancelCommand(sender: CommandSender, command: Command, label: String, vararg args: String): Boolean {
        awaitingBinds.remove(sender)
        return true
    }

    @EventHandler(ignoreCancelled = true)
    private fun npcInteract(event: PlayerInteractEntityEvent) {
        val entity = event.rightClicked
        val player = event.player
        logger.info("interact")

        // only fire once per interaction, so pick a hand
        if (event.hand == EquipmentSlot.HAND) {
            awaitingBinds.remove(player)
                // if player is awaiting a bind action, perform that
                ?.let { (timeout, command) ->
                    timeout.cancel()
                    event.isCancelled = true
                    command?.let {
                        logger.info("bind")
                        bind(entity, command)
                        player.sendMessage("NPC ${entity.name} bound command `$command`!")
                    } ?: let {
                        logger.info("unbind")
                        unbind(entity)
                        player.sendMessage("NPC ${entity.name} bind removed!")
                    }
                }
                // otherwise, perform NPC action (if applicable)
                ?: entity.getBind()?.let { command ->
                    if (player.hasPermission("simplenpc.interact")) {
                        logger.info("npc")
                        player.performCommand(command)
                        event.isCancelled = true
                    }
                }
        }
    }

    override fun onDisable() {
        logger.info("${description.name} disabled.")
    }

    private fun awaitBind(sender: CommandSender, bind: String?) {
        // specify type to workaround "Overload resolution ambiguity" issue with the bukkit scheduler and kotlin lambdas
        val task: () -> Unit = {
            awaitingBinds.remove(sender)?.let { (_, command) ->
                sender.sendMessage(
                    command?.let { "NPC binding of command `$it` timed out" }
                        ?: "NPC unbinding timed out"
                )
            }
        }

        awaitingBinds[sender]?.first?.cancel() // cancel/overwrite existing timeout
        awaitingBinds[sender] = Bukkit.getScheduler().runTaskLater(this, task, bindTimeout) to bind
    }

    private fun Long.secondsToTicks() = this * 20
    private fun Entity.setBind(command: String) = persistentDataContainer.set(bindPersistenceKey, PersistentDataType.STRING, command)
    private fun Entity.removeBind() = persistentDataContainer.remove(bindPersistenceKey)
    private fun Entity.getBind() = persistentDataContainer.get(bindPersistenceKey, PersistentDataType.STRING)
}
