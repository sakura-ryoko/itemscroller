package fi.dy.masa.itemscroller.mixin.screen;

import java.util.List;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.itemscroller.util.InventoryUtils;

@Mixin(HandledScreen.class)
public class MixinHandledScreen
{
	@Inject(method = "getTooltipFromItem(Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("HEAD"))
	private void itemscroller_ignore_bundleTooltipsForScrolling(ItemStack stack, CallbackInfoReturnable<List<Text>> cir)
	{
		InventoryUtils.setIgnoreScrollingInsideOfBundles(stack.getItem() instanceof BundleItem);
	}
}
