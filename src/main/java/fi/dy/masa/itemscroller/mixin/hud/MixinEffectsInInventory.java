package fi.dy.masa.itemscroller.mixin.hud;

import java.util.Collection;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.util.InputUtils;

@Mixin(EffectsInInventory.class)
public abstract class MixinEffectsInInventory
{
    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void itemscroller_preventPotionEffectRendering(GuiGraphicsExtractor graphics,
                                                           Collection<MobEffectInstance> activeEffects,
                                                           int x0, int yStep, int mouseX, int mouseY, int maxWidth,
                                                           CallbackInfo ci)
    {
        if (InputUtils.isRecipeViewOpen())
        {
            ci.cancel();
        }
    }
}
