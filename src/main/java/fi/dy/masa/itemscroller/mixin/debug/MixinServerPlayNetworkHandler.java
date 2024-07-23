package fi.dy.masa.itemscroller.mixin.debug;

import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class MixinServerPlayNetworkHandler
{
    @Shadow public ServerPlayerEntity player;

    @Inject(method = "onClickSlot", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 2))
    private void beforeSendUpdates(ClickSlotC2SPacket packet, CallbackInfo ci) {
        for (Slot slot : this.player.currentScreenHandler.slots)
        {
            ItemStack clientStack = this.player.currentScreenHandler.trackedStacks.get(slot.id);
            if (!ItemStack.areItemsEqual(slot.getStack(), clientStack))
            {
                System.err.printf("Slot %d desync! Server: %s, Client: %s\n", slot.id, slot.getStack(), clientStack);
            }
        }
    }
}
