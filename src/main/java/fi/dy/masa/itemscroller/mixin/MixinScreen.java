package fi.dy.masa.itemscroller.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import fi.dy.masa.itemscroller.event.RenderEventHandler;

@Mixin(Screen.class)
public abstract class MixinScreen
{
    @Inject(method = "renderWithTooltip", at = @At(value = "RETURN"))
    private void itemscroller$onDrawScreenPost(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci)
    {
        RenderEventHandler.instance().onDrawScreenPost(MinecraftClient.getInstance(), context);
    }
}
