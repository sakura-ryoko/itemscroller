package fi.dy.masa.itemscroller.mixin.screen;

import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractRecipeBookScreen.class)
public interface IMixinRecipeBookScreen
{
    @Accessor("recipeBookComponent")
    RecipeBookComponent<?> itemscroller_getRecipeBookWidget();
}
