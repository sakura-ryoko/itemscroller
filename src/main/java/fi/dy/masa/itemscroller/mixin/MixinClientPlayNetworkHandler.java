package fi.dy.masa.itemscroller.mixin;

import fi.dy.masa.itemscroller.event.KeybindCallbacks;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CraftFailedResponseS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler
{
    @Inject(method = "onStatistics", at = @At("RETURN"), cancellable = true)
    private void onPong(StatisticsS2CPacket packet, CallbackInfo ci)
    {
        if (InventoryUtils.onPong(packet))
        {
            ci.cancel();
        }
    }

    @Inject(method = "onScreenHandlerSlotUpdate", at = @At("RETURN"))
    private void onScreenHandlerSlotUpdate(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci)
    {
        KeybindCallbacks.getInstance().onPacket(packet);
    }

    @Inject(
            method = "onCraftFailedResponse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onCraftFailedResponse(CraftFailedResponseS2CPacket packet, CallbackInfo ci)
    {
        // todo
    }

    @Inject(
            method = "onInventory",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true)
    private void onInventory(InventoryS2CPacket packet, CallbackInfo ci)
    {
        if (InventoryUtils.bufferInvUpdates)
        {
            InventoryUtils.invUpdatesBuffer.add(packet);
            ci.cancel();
        }
    }

    @Inject(
            method = "onScreenHandlerSlotUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void onScreenHandlerSlotUpdateInvokeMainThread(ScreenHandlerSlotUpdateS2CPacket packet, CallbackInfo ci)
    {
        if (InventoryUtils.bufferInvUpdates)
        {
            InventoryUtils.invUpdatesBuffer.add(packet);
            ci.cancel();
        }
    }
}
