package fi.dy.masa.itemscroller.mixin.recipe;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;

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
