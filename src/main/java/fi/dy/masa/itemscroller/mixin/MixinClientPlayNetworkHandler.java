package fi.dy.masa.itemscroller.mixin;

import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.client.network.ClientPlayNetworkHandler;
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
}
