package fi.dy.masa.itemscroller.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.screen.slot.Slot;

@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)
public interface IMixinScreenWithHandler
{
    @Invoker("getSlotAt")
    Slot itemscroller$getSlotAtPositionInvoker(double x, double y);

    @Invoker("onMouseClick")
    void itemscroller$handleMouseClickInvoker(Slot slotIn, int slotId, int mouseButton, net.minecraft.screen.slot.SlotActionType type);

    @Accessor("focusedSlot")
    Slot itemscroller$getHoveredSlot();

    @Accessor("x")
    int itemscroller$getGuiLeft();

    @Accessor("y")
    int itemscroller$getGuiTop();

    @Accessor("backgroundWidth")
    int itemscroller$getBackgroundWidth();

    @Accessor("backgroundHeight")
    int itemscroller$getBackgroundHeight();
}
