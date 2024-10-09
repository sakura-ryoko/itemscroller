package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.server.world.ServerWorld;
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

@Mixin(CraftingScreenHandler.class)
public abstract class MixinCraftingScreenHandler
{
    @Shadow @Final private PlayerEntity player;

    @Inject(method = "onContentChanged", at = @At("RETURN"))
    private void onSlotChangedCraftingGrid(net.minecraft.inventory.Inventory inventory, CallbackInfo ci)
    {
        // FIXME
        if (MinecraftClient.getInstance().isOnThread())
        {
            InventoryUtils.onSlotChangedCraftingGrid(this.player,
                    ((IMixinAbstractCraftingScreenHandler) this).itemscroller_getCraftingInventory(),
                    ((IMixinAbstractCraftingScreenHandler) this).itemscroller_getCraftingResultInventory());
        }
    }

    // FIXME
    @Inject(method = "updateResult", at = @At("RETURN"))
    private static void onUpdateResult(
            ScreenHandler handler, ServerWorld serverWorld, PlayerEntity player, RecipeInputInventory craftingInventory, CraftingResultInventory resultInventory, RecipeEntry<CraftingRecipe> recipe, CallbackInfo ci)
    {
        // FIXME
        if (MinecraftClient.getInstance().isOnThread())
        {
            InventoryUtils.onSlotChangedCraftingGrid(player, craftingInventory, resultInventory);
        }
    }
}
