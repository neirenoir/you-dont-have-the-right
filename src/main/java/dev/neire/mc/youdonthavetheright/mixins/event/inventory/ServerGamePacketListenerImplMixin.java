package dev.neire.mc.youdonthavetheright.mixins.event.inventory;

import dev.neire.mc.youdonthavetheright.event.inventory.ContainerEvents;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow public ServerPlayer player;

    @Shadow @Final private MinecraftServer server;

    @Inject(
        method = "handleContainerClick",
        at =
            @At(
                value = "INVOKE",
                target =
                    "Lnet/minecraft/world/inventory/AbstractContainerMenu;" +
                    "clicked(" +
                        "II" +
                        "Lnet/minecraft/world/inventory/ClickType;" +
                        "Lnet/minecraft/world/entity/player/Player;" +
                    ")V"
            ),
        cancellable = true
    )
    public void handleContainerClickBefore(ServerboundContainerClickPacket p, CallbackInfo ci) {
        if (0 > p.getSlotNum()) {
            // NOTE: Happens on throw. Consider firing a different event?
            return;
        }



        var slot = player.containerMenu.getSlot(p.getSlotNum());
        Container source;
        Container target;

        // A note on getting the zeroth slot: the Minecraft menu protocol definition
        // seems to define the first slots (starting at 0) as belonging to the "foreign
        // container", which can be the player's inventory itself if the event has been
        // triggered by a player shuffling around items inside their own inventory screen.
        // In that case: source == target
        Container playerContainer = player.getInventory();
        Container slotContainer = player.containerMenu.getSlot(0).container;

        boolean isPlayerSlot = p.getSlotNum() > slotContainer.getContainerSize() - 1;
        // Pickup`fires AFTER picking or dropping
        if (p.getChangedSlots().size() ==  1) {
            // Player just picked/dropped the carried item
            boolean dropped = p.getCarriedItem() == ItemStack.EMPTY;
            source = dropped || isPlayerSlot ? playerContainer : slotContainer;
            target = !dropped || isPlayerSlot ? playerContainer : slotContainer;
        } else {
            source = isPlayerSlot ? playerContainer : slotContainer;
            target = isPlayerSlot ? slotContainer : playerContainer;
        }

        var e = new ContainerEvents.SlotChange.Moved.Before(source, target, slot);
        MinecraftForge.EVENT_BUS.post(e);

        if (e.isCanceled()) {
            player.containerMenu.resumeRemoteUpdates();
            if (p.getStateId() != this.player.containerMenu.getStateId()) {
                player.containerMenu.broadcastFullState();
            } else {
                player.containerMenu.broadcastChanges();
            }

            ci.cancel();
        }
    }

    @Inject(
        method = "handleContainerClick",
        at =
        @At(
            value = "INVOKE",
            target =
                "Lnet/minecraft/world/inventory/AbstractContainerMenu;" +
                    "clicked(" +
                        "II" +
                        "Lnet/minecraft/world/inventory/ClickType;" +
                        "Lnet/minecraft/world/entity/player/Player;" +
                    ")V",
            shift = At.Shift.AFTER
        )
    )
    public void handleContainerClickAfter(ServerboundContainerClickPacket p, CallbackInfo ci) {
        if (0 > p.getSlotNum()) {
            // NOTE: Happens on dropping items. Consider firing a different event?
            return;
        }

        var slot = player.containerMenu.getSlot(p.getSlotNum());
        Container source;
        Container target;

        Container playerContainer = player.getInventory();
        Container slotContainer = player.containerMenu.getSlot(0).container;

        boolean isPlayerSlot = p.getSlotNum() > slotContainer.getContainerSize() - 1;
        // Pickup`fires AFTER picking or dropping
        if (p.getChangedSlots().size() ==  1) {
            // Player just picked/dropped the carried item
            boolean dropped = p.getCarriedItem() == ItemStack.EMPTY;
            source = dropped || isPlayerSlot ? playerContainer : slotContainer;
            target = !dropped || isPlayerSlot ? playerContainer : slotContainer;
        } else {
            source = isPlayerSlot ? playerContainer : slotContainer;
            target = isPlayerSlot ? slotContainer : playerContainer;
        }

        var e = new ContainerEvents.SlotChange.Moved.After(source, target, slot);
        MinecraftForge.EVENT_BUS.post(e);
    }

}

