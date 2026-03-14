package fi.dy.masa.itemscroller.mixin.item;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(ItemInstance.class)
public interface IMixinItemInstance
{
	@Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
	private void itemscroller_getMaxCount(CallbackInfoReturnable<Integer> cir)
	{
		//System.out.printf("getMaxCount(): this item [%s] // Default Component [%d]\n", this.toString(), this.getComponents().getOrDefault(DataComponentTypes.MAX_STACK_SIZE, 1));

		// Client-side fx for empty shulker box stacking
		if (Minecraft.getInstance().isSameThread() &&
			Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue() &&
			Configs.Generic.SORT_INVENTORY_TOGGLE.getBooleanValue() &&
			Configs.Generic.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue() &&
			InventoryUtils.assumeEmptyShulkerStacking)
		{
			cir.setReturnValue(InventoryUtils.stackMaxSize((ItemStack) (Object) this, true));
		}
	}
}