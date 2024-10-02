package fi.dy.masa.itemscroller.mixin;

import net.minecraft.class_10298;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.recipe.RecipeEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookWidget.class)
public interface IMixinRecipeBookWidget
{
    // FIXME
    @Accessor("field_54830")
    class_10298 itemscroller_getGhostSlots();
}
