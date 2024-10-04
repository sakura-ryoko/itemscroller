package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.recipe.NetworkRecipeId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookWidget.class)
public interface IMixinRecipeBookWidget
{
    // FIXME
    @Accessor("field_54830")
    NetworkRecipeId itemscroller_getGhostSlots();
}
