package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.screen.recipebook.AbstractCraftingRecipeBookWidget;
import net.minecraft.client.gui.screen.recipebook.GhostRecipe;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.book.RecipeBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(AbstractCraftingRecipeBookWidget.class)
public class MixinAbstractCraftingRecipeBookWidget
{
    @Inject(method = "populateRecipes", at = @At("HEAD"), cancellable = true)
    private void itemscroller_populateRecipes(RecipeResultCollection recipeResultCollection, RecipeFinder recipeFinder, RecipeBook recipeBook, CallbackInfo ci)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0)
        {
            ci.cancel();
        }
    }

    // Seems to be (intended) bug from Mojang
    @Inject(
            method = "showGhostRecipe",
            at = @At("HEAD"),
            cancellable = true
    )
    private void itemscroller_nShowGhostRecipe(GhostRecipe ghostRecipe, RecipeEntry<?> recipe, CallbackInfo ci)
    {
        if (((IMixinRecipeBookWidget) this).itemscroller_getGhostSlots() == recipe) {
            ci.cancel();
        }
    }
}
