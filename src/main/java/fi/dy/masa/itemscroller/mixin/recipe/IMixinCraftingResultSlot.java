package fi.dy.masa.itemscroller.mixin.recipe;

import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.screen.slot.CraftingResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CraftingResultSlot.class)
public interface IMixinCraftingResultSlot
{
    @Accessor("input")
    RecipeInputInventory itemscroller_getCraftingInventory();
}
