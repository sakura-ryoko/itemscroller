package fi.dy.masa.itemscroller.mixin;

import java.util.Collection;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.itemscroller.util.InputUtils;

@Mixin(EffectsInInventory.class)
public abstract class MixinStatusEffectsDisplay
{
    @Inject(method = "renderEffects(Lnet/minecraft/client/gui/GuiGraphics;Ljava/util/Collection;IIIII)V",
            at = @At("HEAD"), cancellable = true)
    private void itemscroller_preventPotionEffectRendering(GuiGraphics context, Collection<MobEffectInstance> effects, int x, int height, int mouseX, int mouseY, int width, CallbackInfo ci)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            ci.cancel();
        }
    }
}
