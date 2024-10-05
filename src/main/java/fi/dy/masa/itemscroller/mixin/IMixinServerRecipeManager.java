package fi.dy.masa.itemscroller.mixin;

import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.ServerRecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerRecipeManager.class)
public interface IMixinServerRecipeManager
{
    @Accessor("preparedRecipes")
    PreparedRecipes itemscroller_getPreparedRecipes();
}
