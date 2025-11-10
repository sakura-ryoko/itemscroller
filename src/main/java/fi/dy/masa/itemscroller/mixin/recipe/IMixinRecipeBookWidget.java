package fi.dy.masa.itemscroller.mixin.recipe;

import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface IMixinRecipeBookWidget
{
    @Accessor("lastRecipe")
    RecipeDisplayId itemscroller_getSelectedRecipe();
}
