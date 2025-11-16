package fi.dy.masa.itemscroller.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.itemscroller.util.InputUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.StatusEffectsDisplay;
import net.minecraft.entity.effect.StatusEffectInstance;

@Mixin(StatusEffectsDisplay.class)
public abstract class MixinStatusEffectsDisplay
{
    @Inject(method = "drawStatusEffects(Lnet/minecraft/client/gui/DrawContext;Ljava/util/Collection;IIIII)V",
            at = @At("HEAD"), cancellable = true)
    private void itemscroller_preventPotionEffectRendering(DrawContext context, Collection<StatusEffectInstance> effects, int x, int height, int mouseX, int mouseY, int width, CallbackInfo ci)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            ci.cancel();
        }
    }
}
