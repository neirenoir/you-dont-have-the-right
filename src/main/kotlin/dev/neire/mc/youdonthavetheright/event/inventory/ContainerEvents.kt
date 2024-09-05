package dev.neire.mc.youdonthavetheright.event.inventory

import net.minecraft.world.Container
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event

open class ContainerEvent(open val source: Container): Event() {
    open class SlotChange(
        override val source: Container,
        open val slot: Slot,
    ): ContainerEvent(source) {
        abstract class Moved (
            override val source: Container,
            open val target: Container,
            override val slot: Slot
        ): SlotChange(source, slot) {

            fun toPlayer(): Boolean {
                return target is Inventory
            }

            fun fromPlayer(): Boolean {
                return source is Inventory
            }

            fun getInvolvedPlayer(): Player? {
                return if (source is Inventory) {
                    (source as Inventory).player
                } else if (target is Inventory) {
                    (target as Inventory).player
                } else null
            }

            @Cancelable
            class Before(
                override val source: Container,
                override val target: Container,
                override val slot: Slot
            ): Moved(source, target, slot) {}

            class After(
                override val source: Container,
                override val target: Container,
                override val slot: Slot
            ): Moved(source, target, slot) {}
        }
    }
}