package fi.dy.masa.itemscroller.mixin.screen;

import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MerchantScreen.class)
public interface IMixinMerchantScreen
{
    @Accessor("selectedIndex")
    int itemscroller_getSelectedMerchantRecipe();
}
