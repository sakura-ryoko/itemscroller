package fi.dy.masa.itemscroller.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ScreenHandler.class)
public interface IMixinScreenHandler
{
    @Invoker("offerOrDropStack")
    void itemscroller_offerOrDropStack(PlayerEntity player, ItemStack stack);
}
