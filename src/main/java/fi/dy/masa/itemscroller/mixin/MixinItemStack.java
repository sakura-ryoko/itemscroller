package fi.dy.masa.itemscroller.mixin;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack
{
    @Inject(method = "capCount", at = @At("HEAD"), cancellable = true)
    private void dontCap(int maxCount, CallbackInfo ci)
    {
        // Client-side fx for empty shulker box stacking
        if (MinecraftClient.getInstance().isOnThread() &&
            Configs.Generic.SORT_INVENTORY_TOGGLE.getBooleanValue() &&
            Configs.Generic.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue())
        {
            ci.cancel();
        }
    }

    @Inject(method = "getMaxCount", at = @At("HEAD"), cancellable = true)
    private void getMaxCount(CallbackInfoReturnable<Integer> cir)
    {
        // Client-side fx for empty shulker box stacking
        if (MinecraftClient.getInstance().isOnThread() &&
            Configs.Generic.SORT_INVENTORY_TOGGLE.getBooleanValue() &&
            Configs.Generic.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue() &&
            InventoryUtils.assumeEmptyShulkerStacking)
        {
            cir.setReturnValue(InventoryUtils.stackMaxSize((ItemStack) (Object) this, true));
        }
    }
}
