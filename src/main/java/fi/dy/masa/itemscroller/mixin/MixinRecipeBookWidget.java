package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(RecipeBookWidget.class)
public class MixinRecipeBookWidget
{
    /*
    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void itemscroller_onUpdate(CallbackInfo ci)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0)
        {
            ci.cancel();
        }
    }
     */
}
