package fi.dy.masa.itemscroller.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.itemscroller.event.RenderEventHandler;

@Mixin(Screen.class)
public abstract class MixinScreen
{
    /*
    @Inject(method = "renderWithTooltip",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V",
                    shift = At.Shift.AFTER))
    private void itemscroller_inDrawScreenPre(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        RenderEventHandler.instance().onDrawCraftingScreenBackground(MinecraftClient.getInstance(), context, mouseX, mouseY);
    }
     */

    @Inject(method = "renderWithTooltip", at = @At(value = "TAIL"))
    private void itemscroller_onDrawScreenPost(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        RenderEventHandler.instance().onDrawScreenPost(MinecraftClient.getInstance(), context, mouseX, mouseY);
    }
}
