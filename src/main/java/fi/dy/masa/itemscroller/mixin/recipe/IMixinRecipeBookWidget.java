package fi.dy.masa.itemscroller.mixin.recipe;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.recipe.NetworkRecipeId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookWidget.class)
public interface IMixinRecipeBookWidget
{
    @Accessor("selectedRecipe")
    NetworkRecipeId itemscroller_getSelectedRecipe();
}
