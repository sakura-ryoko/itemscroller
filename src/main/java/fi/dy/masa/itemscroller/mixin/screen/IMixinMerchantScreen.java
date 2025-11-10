package fi.dy.masa.itemscroller.mixin.screen;

import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
public interface IMixinMerchantScreen
{
    @Accessor("shopItem")
    int itemscroller_getSelectedMerchantRecipe();
}
