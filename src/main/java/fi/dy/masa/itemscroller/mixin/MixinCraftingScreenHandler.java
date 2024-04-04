package fi.dy.masa.itemscroller.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.world.World;
import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(net.minecraft.screen.CraftingScreenHandler.class)
public abstract class MixinCraftingScreenHandler
{
    @Shadow @Final private RecipeInputInventory input;
    @Shadow @Final private net.minecraft.inventory.CraftingResultInventory result;
    @Shadow @Final private net.minecraft.entity.player.PlayerEntity player;

    @Inject(method = "onContentChanged", at = @At("RETURN"))
    private void itemscroller$onSlotChangedCraftingGrid(net.minecraft.inventory.Inventory inventory, CallbackInfo ci)
    {
        InventoryUtils.onSlotChangedCraftingGrid(this.player, this.input, this.result);
    }

    @Inject(method = "updateResult", at = @At("RETURN"))
    private static void itemscroller$onUpdateResult(
            ScreenHandler handler,
            World world,
            PlayerEntity player,
            RecipeInputInventory craftingInventory,
            CraftingResultInventory resultInv,
            CallbackInfo ci)
    {
        InventoryUtils.onSlotChangedCraftingGrid(player, craftingInventory, resultInv);
    }
}
