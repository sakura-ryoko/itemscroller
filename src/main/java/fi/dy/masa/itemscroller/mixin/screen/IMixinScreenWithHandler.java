package fi.dy.masa.itemscroller.mixin.screen;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class)
public interface IMixinScreenWithHandler
{
    @Invoker("getHoveredSlot")
    Slot itemscroller_getSlotAtPositionInvoker(double x, double y);

    @Invoker("slotClicked")
    void itemscroller_handleMouseClickInvoker(Slot slotIn, int slotId, int mouseButton, net.minecraft.world.inventory.ClickType type);

    @Accessor("hoveredSlot")
    Slot itemscroller_getHoveredSlot();

    @Accessor("leftPos")
    int itemscroller_getGuiLeft();

    @Accessor("topPos")
    int itemscroller_getGuiTop();

    @Accessor("imageWidth")
    int itemscroller_getBackgroundWidth();

    @Accessor("imageHeight")
    int itemscroller_getBackgroundHeight();
}
