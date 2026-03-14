package fi.dy.masa.itemscroller.util;

import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

import fi.dy.masa.itemscroller.mixin.item.IMixinSlot;
import fi.dy.masa.itemscroller.mixin.screen.IMixinMerchantScreen;
import fi.dy.masa.itemscroller.mixin.screen.IMixinScreenWithHandler;

public class AccessorUtils
{
    @Nullable
    public static Slot getSlotUnderMouse(AbstractContainerScreen<?> gui)
    {
        return ((IMixinScreenWithHandler) gui).itemscroller_getHoveredSlot();
    }

    @Nullable
    public static Slot getSlotAtPosition(AbstractContainerScreen<?> gui, double x, double y)
    {
        return ((IMixinScreenWithHandler) gui).itemscroller_getSlotAtPositionInvoker(x, y);
    }

    public static void handleMouseClick(AbstractContainerScreen<?> gui, Slot slotIn, int slotId, int mouseButton, ContainerInput type)
    {
        ((IMixinScreenWithHandler) gui).itemscroller_handleMouseClickInvoker(slotIn, slotId, mouseButton, type);
    }

    public static int getGuiLeft(AbstractContainerScreen<?> gui)
    {
        return ((IMixinScreenWithHandler) gui).itemscroller_getGuiLeft();
    }

    public static int getGuiTop(AbstractContainerScreen<?> gui)
    {
        return ((IMixinScreenWithHandler) gui).itemscroller_getGuiTop();
    }

    public static int getGuiXSize(AbstractContainerScreen<?> gui)
    {
        return ((IMixinScreenWithHandler) gui).itemscroller_getBackgroundWidth();
    }

    public static int getGuiYSize(AbstractContainerScreen<?> gui)
    {
        return ((IMixinScreenWithHandler) gui).itemscroller_getBackgroundHeight();
    }

    public static int getSelectedMerchantRecipe(MerchantScreen gui)
    {
        return ((IMixinMerchantScreen) gui).itemscroller_getSelectedMerchantRecipe();
    }

    public static int getSlotIndex(Slot slot)
    {
        return ((IMixinSlot) slot).itemscroller_getSlotIndex();
    }
}
