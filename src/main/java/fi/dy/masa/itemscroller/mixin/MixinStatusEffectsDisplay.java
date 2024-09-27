package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.itemscroller.util.InputUtils;

@Mixin(StatusEffectsDisplay.class)
public abstract class MixinStatusEffectsDisplay
{
    @Inject(method = "drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("HEAD"), cancellable = true)
    private void preventPotionEffectRendering(DrawContext context, int mouseX, int mouseY, CallbackInfo ci)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            ci.cancel();
        }
    }
}
