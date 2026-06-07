package fi.dy.masa.itemscroller.mixin.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(CraftingMenu.class)
public abstract class MixinCraftingScreenHandler
{
    @Shadow @Final private Player player;

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void onSlotChangedCraftingGrid(Container container, CallbackInfo ci)
    {
        if (Minecraft.getInstance().isSameThread() &&
            Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue())
        {
            InventoryUtils.onSlotChangedCraftingGrid(this.player,
                    ((IMixinAbstractCraftingScreenHandler) this).itemscroller_getCraftingInventory(),
                    ((IMixinAbstractCraftingScreenHandler) this).itemscroller_getCraftingResultInventory());
        }
    }

    @Inject(method = "slotChangedCraftingGrid", at = @At("RETURN"))
    private static void onUpdateResult(
            AbstractContainerMenu menu, ServerLevel level, Player player, CraftingContainer container, ResultContainer resultSlots, RecipeHolder<CraftingRecipe> recipeHint, CallbackInfo ci)
    {
        if (Minecraft.getInstance().isSameThread() &&
            Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue())
        {
            InventoryUtils.onSlotChangedCraftingGrid(player, container, resultSlots);
        }
    }
}
