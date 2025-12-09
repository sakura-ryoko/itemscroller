package fi.dy.masa.itemscroller.mixin.recipe;

import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ResultSlot.class)
public interface IMixinCraftingResultSlot
{
    @Accessor("craftSlots")
    CraftingContainer itemscroller_getCraftingInventory();
}
