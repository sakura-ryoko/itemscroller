package fi.dy.masa.itemscroller.util;

import java.util.HashMap;
import java.util.Map;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * Wrapper class for ItemStack, which implements equals()
 * for the item, damage and NBT, but not stackSize.
 */
public class ItemType
{
    public static final ItemType EMPTY = new ItemType((ItemStack) null);
    private final ItemStack stack;
    private Identifier id;

    public ItemType(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            this.stack = InventoryUtils.EMPTY_STACK;
            this.id = null;
        }
        else
        {
            this.stack = InventoryUtils.copyStack(stack, false);
            this.id = Registries.ITEM.getId(this.stack.getItem());
        }

    }

    public ItemType(Slot slot)
    {
        if (slot != null && slot.hasStack())
        {
            this.stack = slot.getStack().copy();
            this.id = Registries.ITEM.getId(this.stack.getItem());
        }
        else
        {
            this.stack = InventoryUtils.EMPTY_STACK;
            this.id = null;
        }
    }

    public boolean isEmpty()
    {
        if (this.stack.equals(InventoryUtils.EMPTY_STACK))
        {
            return false;
        }
        else
        {
            return this.stack.isEmpty();
        }
    }

    public boolean isValid()
    {
        return this.hasId();
    }

    public boolean hasId()
    {
        return this.id != null;
    }

    public void setId()
    {
        if (!this.isEmpty())
        {
            this.id = Registries.ITEM.getId(this.stack.getItem());
        }
    }

    public Identifier getId()
    {
        return this.id;
    }

    public ItemStack getStack()
    {
        return this.stack;
    }

    @Override
    public String toString()
    {
        if (this.id != null && this.stack != null && !this.stack.isEmpty())
            return "ItemType id: "+ this.id +" // "+this.stack.getItem().toString();
        else if (this.id == null && this.stack != null && !this.stack.isEmpty())
            return "ItemType id: <null> // "+this.stack.getItem().toString();
        else if (this.id == null && this.stack != null)
            return "ItemType id: <null> // <empty>";
        else if (this.id != null)
            return "ItemType id: "+ this.id +" // <null>";
        else
            return "ItemType id: <null> // <null>";
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

        //return ItemStack.canCombine(this.stack, other.stack);
        return ItemStack.areItemsAndComponentsEqual(this.stack, other.stack);
    }

    /**
     * Returns a map that has a list of the indices for each different item in the input list
     * @param stacks
     * @return
     */
    public static Map<ItemType, IntArrayList> getSlotsPerItem(ItemType[] stacks)
    {
        Map<ItemType, IntArrayList> mapSlots = new HashMap<>();

        for (int i = 0; i < stacks.length; i++)
        {
            ItemType stack = stacks[i];

            if (stack.isValid())
            {
                IntArrayList slots = mapSlots.computeIfAbsent(stack, k -> new IntArrayList());

                slots.add(i);
            }
        }

        return mapSlots;
    }
}
