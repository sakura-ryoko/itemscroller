package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(RecipeBookWidget.class)
public class MixinRecipeBookWidget
{
    //@Shadow @Final protected RecipeBookGhostSlots ghostSlots;

    /*
    @Inject(method = "method_62024", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(RecipeResultCollection par1, RecipeFinder par2, RecipeBook par3, CallbackInfo ci)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0)
        {
            ci.cancel();
        }
    }
     */

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void onUpdate(CallbackInfo ci)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0)
        {
            ci.cancel();
        }
    }

    // Seems to be (intended) bug from Mojang
    /*
    @Inject(
            method = "showGhostRecipe",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onShowGhostRecipe(GhostRecipe ghostRecipe, RecipeEntry<?> entry, CallbackInfo ci) {
        if (this.ghostSlots.getRecipe() == recipe) {
            ci.cancel();
        }
    }
     */
}
