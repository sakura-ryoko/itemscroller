package fi.dy.masa.itemscroller.recipes;

import java.util.Arrays;
import javax.annotation.Nonnull;
import fi.dy.masa.itemscroller.util.ItemType;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.Constants;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RecipePattern
{
    private static final int MAX_SLOTS = 9;
    private ItemType result = ItemType.EMPTY;
    private ItemType[] recipe = new ItemType[MAX_SLOTS];

    public RecipePattern()
    {
        this.ensureRecipeSizeAndClearRecipe(MAX_SLOTS);
    }

    public void ensureRecipeSize(int size)
    {
        if (this.getRecipeLength() != size)
        {
            this.recipe = new ItemType[size];
        }
    }

    public void clearRecipe()
    {
        Arrays.fill(this.recipe, ItemType.EMPTY);
        this.result = ItemType.EMPTY;
    }

    public void ensureRecipeSizeAndClearRecipe(int size)
    {
        this.ensureRecipeSize(size);
        this.clearRecipe();
    }

    public void storeCraftingRecipe(Slot slot, HandledScreen<? extends ScreenHandler> gui, boolean clearIfEmpty)
    {
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

        if (range != null)
        {
            if (slot.hasStack())
            {
                int gridSize = range.getSlotCount();
                int numSlots = gui.getScreenHandler().slots.size();

                this.ensureRecipeSizeAndClearRecipe(gridSize);

                for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
                {
                    Slot slotTmp = gui.getScreenHandler().getSlot(s);
                    this.recipe[i] = new ItemType(slotTmp);
                }

                this.result = new ItemType(slot);
            }
            else if (clearIfEmpty)
            {
                this.clearRecipe();
            }
        }
    }

    public void copyRecipeFrom(RecipePattern other)
    {
        int size = other.getRecipeLength();
        ItemType[] otherRecipe = other.getRecipeItems();

        this.ensureRecipeSizeAndClearRecipe(size);

        for (int i = 0; i < size; i++)
        {
            this.recipe[i] = new ItemType(otherRecipe[i].getStack());
        }

        this.result = InventoryUtils.isStackEmpty(other.getResult()) == false ? new ItemType(other.getResult()) : ItemType.EMPTY;
    }

    public void readFromNBT(@Nonnull NbtCompound nbt)
    {
        if (nbt.contains("Result", Constants.NBT.TAG_COMPOUND) && nbt.contains("Ingredients", Constants.NBT.TAG_LIST))
        {
            NbtList tagIngredients = nbt.getList("Ingredients", Constants.NBT.TAG_COMPOUND);
            int count = tagIngredients.size();
            int length = nbt.getInt("Length");

            if (length > 0)
            {
                this.ensureRecipeSizeAndClearRecipe(length);
            }

            for (int i = 0; i < count; i++)
            {
                NbtCompound tag = tagIngredients.getCompound(i);
                int slot = tag.getInt("Slot");

                if (slot >= 0 && slot < this.recipe.length)
                {
                    this.recipe[slot] = InventoryUtils.recipeSlotReadNbt(tag);
                }
            }

            // this.result = ItemStack.fromNbt(nbt.getCompound("Result"));
            this.result = InventoryUtils.recipeResultReadNbt(nbt.getCompound("Result"));
        }
    }

    @Nonnull
    public NbtCompound writeToNBT()
    {
        NbtCompound nbt = new NbtCompound();

        if (this.result.isValid())
        {
            NbtCompound tag = InventoryUtils.recipeResultWriteNbt(this.result);

            nbt.putInt("Length", this.recipe.length);
            nbt.put("Result", tag);

            NbtList tagIngredients = new NbtList();

            for (int i = 0; i < this.recipe.length; i++)
            {
                if (this.recipe[i].isEmpty() == false && this.recipe[i].hasId())
                {
                    tag = new NbtCompound();
                    tag.copyFrom(InventoryUtils.recipeSlotWriteNbt(this.recipe[i]));

                    tag.putInt("Slot", i);
                    tagIngredients.add(tag);
                }
            }

            nbt.put("Ingredients", tagIngredients);
        }

        return nbt;
    }

    public ItemStack getResult()
    {
        if (this.result.isValid())
        {
            return this.result.getStack();
        }
        else
        {
            return InventoryUtils.EMPTY_STACK;
        }
    }

    public int getRecipeLength()
    {
        return this.recipe.length;
    }

    public ItemType[] getRecipeItems()
    {
        return this.recipe;
    }

    public boolean isValid()
    {
        if (this.result.isEmpty() == false)
        {
            if (this.result.hasId())
            {
                return this.result.isValid();
            }
        }

        return false;
    }
}
