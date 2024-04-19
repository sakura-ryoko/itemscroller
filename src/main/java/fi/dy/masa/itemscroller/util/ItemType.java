package fi.dy.masa.itemscroller.util;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.item.ItemStack;

/**
 * Wrapper class for ItemStack, which implements equals()
 * for the item, damage and NBT, but not stackSize.
 */
public class ItemType
{
    private final ItemStack stack;

    public ItemType(@Nonnull ItemStack stack)
    {
        this.stack = stack.isEmpty() ? InventoryUtils.EMPTY_STACK : InventoryUtils.copyStack(stack, false);
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        //result = prime * result + ((stack == null) ? 0 : stack.hashCode());
        result = prime * result + this.stack.getItem().hashCode();
        result = prime * result + (this.stack.getComponents() != null ? this.stack.getComponents().hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (this.getClass() != obj.getClass())
            return false;

        ItemType other = (ItemType) obj;

        return ItemStack.areItemsAndComponentsEqual(this.stack, other.stack);
    }

    /**
     * Returns a map that has a list of the indices for each different item in the input list
     * @param stacks
     * @return
     */
    public static Map<ItemType, IntArrayList> getSlotsPerItem(ItemStack[] stacks)
    {
        Map<ItemType, IntArrayList> mapSlots = new HashMap<>();

        for (int i = 0; i < stacks.length; i++)
        {
            ItemStack stack = stacks[i];

            if (InventoryUtils.isStackEmpty(stack) == false)
            {
                ItemType item = new ItemType(stack);
                IntArrayList slots = mapSlots.computeIfAbsent(item, k -> new IntArrayList());

                slots.add(i);
            }
        }

        return mapSlots;
    }
}
