package fi.dy.masa.itemscroller.mixin;

import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.screen.AbstractCraftingScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractCraftingScreenHandler.class)
public interface IMixinAbstractCraftingScreenHandler
{
    @Accessor("craftingInventory")
    RecipeInputInventory itemscroller_getCraftingInventory();

    @Accessor("craftingResultInventory")
    CraftingResultInventory itemscroller_getCraftingResultInventory();
}
