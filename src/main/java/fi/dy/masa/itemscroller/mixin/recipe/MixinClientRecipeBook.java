package fi.dy.masa.itemscroller.mixin.recipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;

@Mixin(ClientRecipeBook.class)
public class MixinClientRecipeBook
{
    @Inject(method = "add", at = @At("RETURN"))
    private void itemscroller_addToRecipeBook(RecipeDisplayEntry entry, CallbackInfo ci)
    {
        RecipeStorage.getInstance().onAddToRecipeBook(entry);
    }
}
