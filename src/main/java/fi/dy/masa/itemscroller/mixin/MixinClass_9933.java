package fi.dy.masa.itemscroller.mixin;

import net.minecraft.class_9933;
import net.minecraft.client.gui.screen.recipebook.RecipeResultCollection;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeFinder;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.screen.GhostRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(class_9933.class)
public class MixinClass_9933
{
    // TODO --> onSlotClicked()
    @Inject(method = "method_62024", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(RecipeResultCollection recipeResultCollection, RecipeFinder recipeFinder, RecipeBook recipeBook, CallbackInfo ci)
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
    private void onShowGhostRecipe(GhostRecipe ghostRecipe, RecipeEntry<?> recipe, CallbackInfo ci)
    {
        if (((IMixinRecipeBookWidget) this).itemscroller_getGhostSlots() == recipe) {
            ci.cancel();
        }
    }
}
