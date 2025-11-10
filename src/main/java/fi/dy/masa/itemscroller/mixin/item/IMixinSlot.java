package fi.dy.masa.itemscroller.mixin.item;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface IMixinSlot
{
    @Accessor("slot")
    int itemscroller_getSlotIndex();
}
