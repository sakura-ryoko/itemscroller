package fi.dy.masa.itemscroller.mixin;

import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class MixinClientPlayerInteractionManager
{
    @Inject(method = "clickSlot", at = @At("HEAD"), cancellable = true)
    private void cancelWindowClicksWhileReplayingBufferedPackets(CallbackInfo ci)
    {
        if (ClickPacketBuffer.shouldCancelWindowClicks())
        {
            ci.cancel();
        }
    }

    @Redirect(method = "clickSlot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendPacket(Lnet/minecraft/network/packet/Packet;)V"))
    private void bufferClickPacketsAndCancel(ClientPlayNetworkHandler netHandler, Packet<?> packet)
    {
        /*
        if (packet instanceof ClickSlotC2SPacket clickPacket)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            System.out.printf("clickPacket: type: %s button: %d, slot: %d, (after) cursor item: %s\n", clickPacket.getActionType(), clickPacket.getButton(), clickPacket.getSlot(), clickPacket.getStack());
            clickPacket.getModifiedStacks().forEach((integer, stack) -> System.out.printf("%d = %s, ", integer, stack));
            System.out.println();
        }
         */
        if (ClickPacketBuffer.shouldBufferClickPackets())
        {
            ClickPacketBuffer.bufferPacket(packet);
            return;
        }

        netHandler.sendPacket(packet);
    }
}
