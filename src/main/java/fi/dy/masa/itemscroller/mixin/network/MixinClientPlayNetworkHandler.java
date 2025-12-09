package fi.dy.masa.itemscroller.mixin.network;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler
{
    @Inject(method = "handleAwardStats", at = @At("RETURN"), cancellable = true)
    private void onPong(ClientboundAwardStatsPacket packet, CallbackInfo ci)
    {
        if (InventoryUtils.onPong(packet))
        {
            ci.cancel();
        }
    }

//    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
//    private void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci)
//    {
//        KeybindCallbacks.getInstance().onPacket(packet);
//    }

    @Inject(
            method = "handleContainerContent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true)
    private void onInventory(ClientboundContainerSetContentPacket packet, CallbackInfo ci)
    {
        if (InventoryUtils.bufferInvUpdates)
        {
            InventoryUtils.invUpdatesBuffer.add(packet);
            ci.cancel();
        }
    }

    @Inject(
            method = "handleContainerSetSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onScreenHandlerSlotUpdateInvokeMainThread(ClientboundContainerSetSlotPacket packet, CallbackInfo ci)
    {
        if (InventoryUtils.bufferInvUpdates)
        {
            InventoryUtils.invUpdatesBuffer.add(packet);
            ci.cancel();
        }
    }
}
