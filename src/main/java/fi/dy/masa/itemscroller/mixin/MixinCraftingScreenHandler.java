package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(net.minecraft.screen.CraftingScreenHandler.class)
public abstract class MixinCraftingScreenHandler
{
    //@Shadow @Final private RecipeInputInventory craftingInventory;
    //@Shadow @Final private net.minecraft.inventory.CraftingResultInventory craftingResultInventory;
    @Shadow @Final private net.minecraft.entity.player.PlayerEntity player;

    @Inject(method = "onContentChanged", at = @At("RETURN"))
    private void onSlotChangedCraftingGrid(net.minecraft.inventory.Inventory inventory, CallbackInfo ci)
    {
        if (MinecraftClient.getInstance().isOnThread())
        {
            InventoryUtils.onSlotChangedCraftingGrid(this.player,
                    ((IMixinAbstractCraftingScreenHandler) this).itemscroller_getCraftingInventory(),
                    ((IMixinAbstractCraftingScreenHandler) this).itemscroller_getCraftingResultInventory());
        }
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private static void onUpdateResult(
            ScreenHandler handler,
            World world,
            PlayerEntity player,
            RecipeInputInventory craftingInventory,
            CraftingResultInventory resultInv,
            RecipeEntry<CraftingRecipe> recipeEntry, CallbackInfo ci)
    {
        if (MinecraftClient.getInstance().isOnThread())
        {
            InventoryUtils.onSlotChangedCraftingGrid(player, craftingInventory, resultInv);
        }
    }
}
