package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookScreen.class)
public interface IMixinRecipeBookScreen
{
    @Accessor("recipeBook")
    RecipeBookWidget<?> itemscroller_getRecipeBookWidget();
}
