package fi.dy.masa.itemscroller.mixin.recipe;

import net.minecraft.client.gui.screen.recipebook.AbstractCraftingRecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractCraftingRecipeBookWidget.class)
public class MixinAbstractCraftingRecipeBookWidget
{
    /*
    @Inject(method = "populateRecipes", at = @At("HEAD"), cancellable = true)
    private void itemscroller_populateRecipes(RecipeResultCollection recipeResultCollection, RecipeFinder recipeFinder, CallbackInfo ci)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0)
        {
            ci.cancel();
        }
    }
     */

    // Seems to be (intended) bug from Mojang
    /*
    @Inject(
            method = "showGhostRecipe",
            at = @At("HEAD"),
            cancellable = true
    )
    private void itemscroller_onShowGhostRecipe(GhostRecipe ghostRecipe, RecipeDisplay display, ContextParameterMap context, CallbackInfo ci)
    {
        if (((IMixinRecipeBookWidget) this).itemscroller_getGhostSlots() == ghostRecipe)
        {
            ci.cancel();
        }
    }
     */
}
