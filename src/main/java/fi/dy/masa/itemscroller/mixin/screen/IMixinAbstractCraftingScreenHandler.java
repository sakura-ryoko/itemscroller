package fi.dy.masa.itemscroller.mixin.screen;

import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCraftingMenu.class)
public interface IMixinAbstractCraftingScreenHandler
{
    @Accessor("craftSlots")
    CraftingContainer itemscroller_getCraftingInventory();

    @Accessor("resultSlots")
    ResultContainer itemscroller_getCraftingResultInventory();
}
