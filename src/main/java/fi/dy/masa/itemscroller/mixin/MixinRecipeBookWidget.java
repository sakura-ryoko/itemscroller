package fi.dy.masa.itemscroller.mixin;

import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.client.gui.screen.recipebook.RecipeBookGhostSlots;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(RecipeBookWidget.class)
public class MixinRecipeBookWidget
{
    @Shadow @Final protected RecipeBookGhostSlots ghostSlots;

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void onSlotClicked(Slot slot, CallbackInfo ci)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0)
        {
            ci.cancel();
        }
    }

    @Inject(method = "update", at = @At("HEAD"), cancellable = true)
    private void onUpdate(CallbackInfo ci)
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
    private void onShowGhostRecipe(RecipeEntry<?> recipe, List<Slot> slots, CallbackInfo ci) {
        if (this.ghostSlots.getRecipe() == recipe) {
            ci.cancel();
        }
    }
}
