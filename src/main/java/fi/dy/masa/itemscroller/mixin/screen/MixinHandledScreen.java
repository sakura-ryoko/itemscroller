package fi.dy.masa.itemscroller.mixin.screen;

import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(AbstractContainerScreen.class)
public class MixinHandledScreen
{
	@Inject(method = "getTooltipFromContainerItem(Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", at = @At("HEAD"))
	private void itemscroller_ignore_bundleTooltipsForScrolling(ItemStack stack, CallbackInfoReturnable<List<Component>> cir)
	{
		InventoryUtils.setIgnoreScrollingInsideOfBundles(stack.getItem() instanceof BundleItem);
	}
}
