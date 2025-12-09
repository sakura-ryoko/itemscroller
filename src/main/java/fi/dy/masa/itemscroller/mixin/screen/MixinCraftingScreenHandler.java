package fi.dy.masa.itemscroller.mixin.screen;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

@Mixin(CraftingMenu.class)
public abstract class MixinCraftingScreenHandler
{
    @Shadow @Final private Player player;

    @Inject(method = "slotsChanged", at = @At("RETURN"))
    private void onSlotChangedCraftingGrid(net.minecraft.world.Container inventory, CallbackInfo ci)
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
            AbstractContainerMenu handler, ServerLevel serverWorld, Player player, CraftingContainer craftingInventory, ResultContainer resultInventory, RecipeHolder<CraftingRecipe> recipe, CallbackInfo ci)
    {
        if (Minecraft.getInstance().isSameThread() &&
            Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue())
        {
            InventoryUtils.onSlotChangedCraftingGrid(player, craftingInventory, resultInventory);
        }
    }
}
