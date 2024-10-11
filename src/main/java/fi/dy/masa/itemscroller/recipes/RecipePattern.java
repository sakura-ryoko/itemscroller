package fi.dy.masa.itemscroller.recipes;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.RecipeBookScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.client.recipebook.ClientRecipeBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.recipe.*;
import net.minecraft.recipe.display.SlotDisplayContexts;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.recipe.input.RecipeInput;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.context.ContextParameterMap;
import net.minecraft.world.World;

import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.mixin.IMixinClientRecipeBook;
import fi.dy.masa.itemscroller.mixin.IMixinRecipeBookScreen;
import fi.dy.masa.itemscroller.mixin.IMixinRecipeBookWidget;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.Constants;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RecipePattern
{
    private ItemStack result = InventoryUtils.EMPTY_STACK;
    private ItemStack[] recipe = new ItemStack[9];
    private RecipeEntry<?> vanillaRecipe;
    private NetworkRecipeId networkRecipeId;
    private RecipeDisplayEntry displayEntry;

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
        this.networkRecipeId = null;
        this.displayEntry = null;
    }

    public void ensureRecipeSizeAndClearRecipe(int size)
    {
        this.ensureRecipeSize(size);
        this.clearRecipe();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends RecipeInput> Recipe<T> lookupVanillaRecipe(World world)
    {
        //Assume all recipes here are of type CraftingRecipe
        this.vanillaRecipe = null;
        MinecraftClient mc = MinecraftClient.getInstance();
        int recipeSize;

        if (mc.world == null)
        {
            return null;
        }
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

        ServerWorld serverWorld = mc.getServer() != null ? mc.getServer().getWorld(mc.world.getRegistryKey()) : null;

        if (mc.isIntegratedServerRunning() && serverWorld != null)
        {
            CraftingRecipeInput input = CraftingRecipeInput.create(recipeSize, recipeSize, Arrays.asList(recipe));
            Optional<RecipeEntry<CraftingRecipe>> opt = serverWorld.getRecipeManager().getFirstMatch(RecipeType.CRAFTING, input, serverWorld);

            if (opt.isPresent())
            {
                RecipeEntry<CraftingRecipe> recipeEntry = opt.get();
                Recipe<CraftingRecipeInput> match = opt.get().value();
                ItemStack result = match.craft(input, serverWorld.getRegistryManager());

                if (result != null && !result.isEmpty())
                {
                    this.vanillaRecipe = recipeEntry;
                    this.storeIdFromClientRecipeBook(mc);
                    return (Recipe<T>) match;
                }
            }
        }
        else
        {
            this.storeIdFromClientRecipeBook(mc);
        }

        return null;
    }

    public void storeIdFromClientRecipeBook(MinecraftClient mc)
    {
        Pair<NetworkRecipeId, RecipeDisplayEntry> pair = this.matchClientRecipeBook(mc);

        if (pair == null || pair.getLeft() == null || pair.getRight() == null)
        {
            return;
        }

        this.storeNetworkRecipeId(pair.getLeft());
        this.storeRecipeDisplayEntry(pair.getRight());
    }

    public void storeNetworkRecipeId(NetworkRecipeId id)
    {
        this.networkRecipeId = id;
    }

    public void storeRecipeDisplayEntry(RecipeDisplayEntry entry)
    {
        this.displayEntry = entry;
    }

    public @Nullable NetworkRecipeId getNetworkRecipeId()
    {
        return this.networkRecipeId;
    }

    public @Nullable RecipeDisplayEntry getRecipeDisplayEntry()
    {
        return this.displayEntry;
    }

    public Pair<NetworkRecipeId, RecipeDisplayEntry> matchClientRecipeBook(MinecraftClient mc)
    {
        AtomicReference<Pair<NetworkRecipeId, RecipeDisplayEntry>> pair = new AtomicReference<>();

        if (mc.player == null || mc.world == null)
        {
            return null;
        }

        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        ContextParameterMap ctx = SlotDisplayContexts.createParameters(mc.world);
        Map<NetworkRecipeId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();
        List<ItemStack> recipeStacks = this.combineStacks(Arrays.stream(this.getRecipeItems()).toList(), 3);

        recipeMap.forEach((id, entry) ->
        {
            List<ItemStack> stacks = entry.getStacks(ctx);

            // Combine Stacks so that they can equal
            if (this.compareRecipeStacks(recipeStacks, this.combineStacks(stacks, 3)))
            {
                pair.set(Pair.of(id, entry));
            }
        });

        return pair.get();
    }

    private List<ItemStack> combineStacks(List<ItemStack> stacks, int iterations)
    {
        if (iterations > 3 || iterations < 1)
        {
            iterations = 3;
        }

        List<ItemStack> list = new ArrayList<>(stacks);
        int i = 0;

        while (i < iterations)
        {
            list = this.combineStacksEach(list);
            i++;
        }

        return list;
    }

    private List<ItemStack> combineStacksEach(List<ItemStack> stacks)
    {
        List<ItemStack> list = new ArrayList<>();
        ItemStack previous = ItemStack.EMPTY;
        ItemStack entry = ItemStack.EMPTY;

        for (int i = 0; i < stacks.size(); i++)
        {
            entry = stacks.get(i);

            if (!previous.isEmpty() && ItemStack.areItemsAndComponentsEqual(previous, entry))
            {
                int prevCount = previous.getCount();
                int maxCount = previous.getMaxCount();
                int entryCount = entry.getCount();

                if ((prevCount + entryCount) <= maxCount)
                {
                    previous.setCount(prevCount + entryCount);
                    entry = ItemStack.EMPTY;
                }
                else
                {
                    previous.setCount(maxCount);
                    entry.setCount((prevCount + entryCount) - maxCount);
                    list.add(previous.copy());
                    previous = entry.copy();
                    entry = ItemStack.EMPTY;
                }
            }
            else
            {
                if (!previous.isEmpty())
                {
                    list.add(previous.copy());
                }

                previous = entry.copy();
                entry = ItemStack.EMPTY;
            }
        }

        if (!previous.isEmpty())
        {
            list.add(previous.copy());
        }
        if (!entry.isEmpty())
        {
            list.add(entry.copy());
        }

        return list;
    }

    private boolean compareRecipeStacks(List<ItemStack> left, List<ItemStack> right)
    {
        if (left.size() != right.size())
        {
            return false;
        }

        for (int i = 0; i < left.size(); i++)
        {
            ItemStack l = left.get(i);
            ItemStack r = right.get(i);

            if (ItemStack.areItemsEqual(l, r) == false)
            {
                return false;
            }
            else if (l.getCount() != r.getCount())
            {
                return false;
            }
        }

        return true;
    }

    public boolean verifyClientRecipeBook(MinecraftClient mc, @Nullable NetworkRecipeId id)
    {
        if (id != null)
        {
            if (id.equals(this.getNetworkRecipeId()))
            {
                return true;
            }

            return false;
        }

        Pair<NetworkRecipeId, RecipeDisplayEntry> pair = this.matchClientRecipeBook(mc);

        if (pair == null || pair.getLeft() == null || pair.getRight() == null)
        {
            return false;
        }

        if (pair.getLeft() == this.getNetworkRecipeId())
        {
            return true;
        }

        return false;
    }

    public boolean matchClientRecipeBookEntry(RecipeDisplayEntry entry, MinecraftClient mc)
    {
        if (mc.world == null)
        {
            return false;
        }

        // Compact the stacks so that they equal
        List<ItemStack> recipeStacks = this.combineStacks(Arrays.stream(this.getRecipeItems()).toList(), 3);
        List<ItemStack> stacks = this.combineStacks(entry.getStacks(SlotDisplayContexts.createParameters(mc.world)), 3);

        if (this.compareRecipeStacks(recipeStacks, stacks))
        {
            return true;
        }

        return false;
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
                this.storeSelectedRecipeIdFromGui(gui);
            }
            else if (clearIfEmpty)
            {
                this.clearRecipe();
            }
        }
    }

    public void storeSelectedRecipeIdFromGui(HandledScreen<? extends ScreenHandler> gui)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (gui instanceof RecipeBookScreen rbs)
        {
            RecipeBookWidget<?> widget = ((IMixinRecipeBookScreen) rbs).itemscroller_getRecipeBookWidget();

            if (widget != null)
            {
                NetworkRecipeId id = ((IMixinRecipeBookWidget) widget).itemscroller_getSelectedRecipe();

                if (id != null)
                {
                    ClientRecipeBook recipeBook = mc.player.getRecipeBook();
                    Map<NetworkRecipeId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();

                    this.storeNetworkRecipeId(id);

                    if (recipeMap.containsKey(id))
                    {
                        this.storeRecipeDisplayEntry(recipeMap.get(id));
                    }

                    // Clear crafting grid
                    InventoryUtils.clearFirstCraftingGridOfAllItems(gui);
                }
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
        this.networkRecipeId = other.networkRecipeId;
        this.displayEntry = other.displayEntry;
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
        return this.vanillaRecipe;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends RecipeInput> Recipe<T> getVanillaRecipe()
    {
        if (recipe == null)
        {
            return null;
        }

        if (this.vanillaRecipe != null)
        {
            return (Recipe<T>) this.vanillaRecipe.value();
        }

        return null;
    }
}
