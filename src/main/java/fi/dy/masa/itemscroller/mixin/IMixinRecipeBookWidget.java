package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.screen.recipebook.GhostRecipe;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.recipe.NetworkRecipeId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookWidget.class)
public interface IMixinRecipeBookWidget
{
    @Accessor("ghostRecipe")
    GhostRecipe itemscroller_getGhostSlots();

    /*
    @Accessor("recipesArea")
    RecipeBookResults itemscroller_getRecipeArea();

    @Accessor("selectedRecipeResults")
    RecipeResultCollection itemscroller_getSelectedRecipeResults();
     */

    @Accessor("selectedRecipe")
    NetworkRecipeId itemscroller_getSelectedRecipe();

    /*
    @Invoker("select")
    boolean itemscroller_select(RecipeResultCollection results, NetworkRecipeId recipeId);
     */
}
