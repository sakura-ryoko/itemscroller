package fi.dy.masa.itemscroller.mixin.screen;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fi.dy.masa.malilib.render.GuiContext;
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

    @Final @Shadow @Nullable protected Minecraft minecraft;

    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At(value = "TAIL"))
    private void itemscroller_onDrawScreenPost(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci)
    {
        RenderEventHandler.instance().onDrawScreenPost(GuiContext.fromGuiGraphics(graphics), this.minecraft, mouseX, mouseY);
    }
}
