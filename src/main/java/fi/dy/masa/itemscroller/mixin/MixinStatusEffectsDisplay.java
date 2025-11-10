package fi.dy.masa.itemscroller.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.itemscroller.util.InputUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;

@Mixin(EffectsInInventory.class)
public abstract class MixinStatusEffectsDisplay
{
    @Inject(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At("HEAD"), cancellable = true)
    private void preventPotionEffectRendering(GuiGraphics context, int mouseX, int mouseY, CallbackInfo ci)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            ci.cancel();
        }
    }
}
