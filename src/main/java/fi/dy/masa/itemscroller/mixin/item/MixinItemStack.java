package fi.dy.masa.itemscroller.mixin.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.config.Configs;

@Mixin(ItemStack.class)
public abstract class MixinItemStack
{
    @Inject(method = "limitSize", at = @At("HEAD"), cancellable = true)
    private void itemscroller_dontCap(int maxCount, CallbackInfo ci)
    {
        // Client-side fx for empty shulker box stacking
        if (Minecraft.getInstance().isSameThread() &&
            Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue() &&
            Configs.Generic.SORT_INVENTORY_TOGGLE.getBooleanValue() &&
            Configs.Generic.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue())
        {
            ci.cancel();
        }
    }
}
