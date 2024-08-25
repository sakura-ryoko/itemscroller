package fi.dy.masa.itemscroller.recipes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.CraftingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.Constants;
import fi.dy.masa.itemscroller.util.InventoryUtils;
import net.minecraft.world.World;

public class RecipePattern
{
    private ItemStack result = InventoryUtils.EMPTY_STACK;
    private ItemStack[] recipe = new ItemStack[9];
    private RecipeEntry<?> vanillaRecipe;

    public RecipePattern()
    {
        this.ensureRecipeSizeAndClearRecipe(9);
    }

    public void ensureRecipeSize(int size)
    {
        if (this.getRecipeLength() != size)
        {
            this.recipe = new ItemStack[size];
        }
    }

    public void clearRecipe()
    {
        Arrays.fill(this.recipe, InventoryUtils.EMPTY_STACK);
        this.result = InventoryUtils.EMPTY_STACK;
        this.vanillaRecipe = null;
    }

    public void ensureRecipeSizeAndClearRecipe(int size)
    {
        this.ensureRecipeSize(size);
        this.clearRecipe();
    }

    @Nullable
    public <T extends RecipeInput> Recipe<T> lookupVanillaRecipe(World world) {
        //Assume all recipes here are of type CraftingRecipe
        this.vanillaRecipe = null;
        var mc = MinecraftClient.getInstance();
        int recipeSize;
        if (recipe.length == 4)
        {
            recipeSize = 2;
        }
        else if (recipe.length == 9)
        {
            recipeSize = 3;
        }
        else
        {
            return null;
        }

        for (RecipeEntry<CraftingRecipe> match : mc.world.getRecipeManager().getAllMatches(RecipeType.CRAFTING, CraftingRecipeInput.create(recipeSize, recipeSize, Arrays.asList(recipe)), world))
        {
            if (InventoryUtils.areStacksEqual(result, match.value().getResult(world.getRegistryManager())))
            {
                this.vanillaRecipe = match;
                return (Recipe<T>) match.value();
            }
        }
        return null;
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
                    this.recipe[i] = slotTmp.hasStack() ? slotTmp.getStack().copy() : InventoryUtils.EMPTY_STACK;
                }

                this.result = slot.getStack().copy();
                this.lookupVanillaRecipe(MinecraftClient.getInstance().world);
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
        ItemStack[] otherRecipe = other.getRecipeItems();

        this.ensureRecipeSizeAndClearRecipe(size);

        for (int i = 0; i < size; i++)
        {
            this.recipe[i] = InventoryUtils.isStackEmpty(otherRecipe[i]) == false ? otherRecipe[i].copy() : InventoryUtils.EMPTY_STACK;
        }

        this.result = InventoryUtils.isStackEmpty(other.getResult()) == false ? other.getResult().copy() : InventoryUtils.EMPTY_STACK;
        this.vanillaRecipe = other.vanillaRecipe;
    }

    public void readFromNBT(@Nonnull NbtCompound nbt, @Nonnull DynamicRegistryManager registryManager)
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
                    this.recipe[slot] = ItemStack.fromNbtOrEmpty(registryManager, tag);
                }
            }

            this.result = ItemStack.fromNbtOrEmpty(registryManager, nbt.getCompound("Result"));
        }
    }

    @Nonnull
    public NbtCompound writeToNBT(@Nonnull DynamicRegistryManager registryManager)
    {
        NbtCompound nbt = new NbtCompound();

        if (this.isValid())
        {
            NbtCompound tag = (NbtCompound) this.result.toNbt(registryManager);

            nbt.putInt("Length", this.recipe.length);
            nbt.put("Result", tag);

            NbtList tagIngredients = new NbtList();

            for (int i = 0; i < this.recipe.length; i++)
            {
                if (this.recipe[i].isEmpty() == false && InventoryUtils.isStackEmpty(this.recipe[i]) == false)
                {
                    tag = new NbtCompound();
                    tag.copyFrom((NbtCompound) this.recipe[i].toNbt(registryManager));

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
        if (this.result.isEmpty() == false)
        {
            return this.result;
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

    public ItemStack[] getRecipeItems()
    {
        return this.recipe;
    }

    public boolean isValid()
    {
        return InventoryUtils.isStackEmpty(this.getResult()) == false;
    }

    @Nullable
    public RecipeEntry<?> getVanillaRecipeEntry()
    {
        return vanillaRecipe;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends RecipeInput> Recipe<T> getVanillaRecipe()
    {
        if (recipe == null)
        {
            return null;
        }
        return (Recipe<T>) vanillaRecipe.value();
    }
}
