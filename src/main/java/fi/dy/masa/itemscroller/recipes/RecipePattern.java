package fi.dy.masa.itemscroller.recipes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import net.minecraft.recipe.book.RecipeBookCategory;
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
    private RecipeBookCategory category;
    private long recipeSaveTime;

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
        this.category = null;
        this.recipeSaveTime = -1;
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
        this.storeRecipeCategory(pair.getRight().category());
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

    public void storeRecipeCategory(RecipeBookCategory category)
    {
        this.category = category;
    }

    public @Nullable NetworkRecipeId getNetworkRecipeId()
    {
        return this.networkRecipeId;
    }

    public @Nullable RecipeDisplayEntry getRecipeDisplayEntry()
    {
        return this.displayEntry;
    }

    public @Nullable RecipeBookCategory getRecipeCategory()
    {
        return this.category;
    }

    public boolean matchRecipeCategory(RecipeBookCategory category)
    {
        return this.getRecipeCategory() != null && this.getRecipeCategory().equals(category);
    }

    public @Nullable Pair<NetworkRecipeId, RecipeDisplayEntry> matchClientRecipeBook(MinecraftClient mc)
    {
        Pair<NetworkRecipeId, RecipeDisplayEntry> pair;

        if (mc.player == null || mc.world == null || this.isEmpty())
        {
            return null;
        }

        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        ContextParameterMap ctx = SlotDisplayContexts.createParameters(mc.world);
        Map<NetworkRecipeId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();

        if (recipeMap.size() < 1)
        {
            return null;
        }

        for (NetworkRecipeId id : recipeMap.keySet())
        {
            RecipeDisplayEntry entry = recipeMap.get(id);

            if (entry != null)
            {
                if (this.getRecipeCategory() != null && !this.matchRecipeCategory(entry.category()))
                {
                    continue;
                }

                List<ItemStack> stacks = entry.getStacks(ctx);

                if (RecipeUtils.areStacksEqual(this.getResult(), stacks.getFirst()))
                {
                    pair = Pair.of(id, entry);
                    return pair;
                }
            }
        }

        return null;
    }

    public boolean matchClientRecipeBookEntry(RecipeDisplayEntry entry, MinecraftClient mc)
    {
        if (mc.world == null || this.isEmpty())
        {
            return false;
        }

        if (this.getRecipeCategory() != null && !entry.category().equals(this.getRecipeCategory()))
        {
            return false;
        }
        List<ItemStack> recipeStacks = Arrays.stream(this.getRecipeItems()).toList();
        List<ItemStack> stacks = entry.getStacks(SlotDisplayContexts.createParameters(mc.world));

        //System.out.printf("matchClientRecipeBookEntry() --> [%s] vs [%s]\n", this.getResult().toString(), stacks.getFirst().toString());

        if (RecipeUtils.areStacksEqual(this.getResult(), stacks.getFirst()))
        {
            if (entry.craftingRequirements().isPresent())
            {
                return RecipeUtils.compareStacksAndIngredients(recipeStacks, entry.craftingRequirements().get(), this.countRecipeItems());
            }

            return true;
        }

        return false;
    }

    public void storeCraftingRecipe(Slot slot, HandledScreen<? extends ScreenHandler> gui, boolean clearIfEmpty, boolean fromKeybind, MinecraftClient mc)
    {
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

        if (range != null)
        {
            if (slot.hasStack())
            {
                int gridSize = range.getSlotCount();

                if (fromKeybind)
                {
                    // Slots are only populated from the Keybinds Callback
                    int numSlots = gui.getScreenHandler().slots.size();
                    this.ensureRecipeSizeAndClearRecipe(gridSize);

                    for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
                    {
                        Slot slotTmp = gui.getScreenHandler().getSlot(s);
                        this.recipe[i] = slotTmp.hasStack() ? slotTmp.getStack().copy() : InventoryUtils.EMPTY_STACK;
                    }
                    this.recipeSaveTime = System.currentTimeMillis();
                }
                // Stop the mod from overwriting the correctly saved recipe with a button or nugget from the Grid clear
                else if ((System.currentTimeMillis() - this.recipeSaveTime) < 4000L)
                {
                    //System.out.printf("storeCraftingRecipe() SKIPPING InputHandler input result [%s] versus [%s]\n", this.result.toString(), slot.getStack().toString());
                    this.recipeSaveTime = System.currentTimeMillis();
                    gui.getScreenHandler().setCursorStack(ItemStack.EMPTY);
                    InventoryUtils.clearFirstCraftingGridOfAllItems(gui);
                    return;
                }

                //System.out.printf("storeCraftingRecipe() old result [%s] new [%s]\n", this.result.toString(), slot.getStack().toString());
                this.result = slot.getStack().copy();
                this.lookupVanillaRecipe(mc.world);
                this.storeSelectedRecipeIdFromGui(gui);
                InventoryUtils.clearFirstCraftingGridOfAllItems(gui);
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

        if (mc.world == null || mc.player == null)
        {
            return;
        }
        if (gui instanceof RecipeBookScreen<?> rbs)
        {
            RecipeBookWidget<?> widget = ((IMixinRecipeBookScreen) rbs).itemscroller_getRecipeBookWidget();

            if (widget != null)
            {
                NetworkRecipeId id = ((IMixinRecipeBookWidget) widget).itemscroller_getSelectedRecipe();

                if (id != null)
                {
                    ClientRecipeBook recipeBook = mc.player.getRecipeBook();
                    Map<NetworkRecipeId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();

                    if (recipeMap.containsKey(id))
                    {
                        RecipeDisplayEntry entry = recipeMap.get(id);
                        ItemStack result = entry.getStacks(SlotDisplayContexts.createParameters(mc.world)).getFirst();

                        if (RecipeUtils.areStacksEqual(this.getResult(), result))
                        {
                            if (entry.craftingRequirements().isPresent())
                            {
                                if (RecipeUtils.compareStacksAndIngredients(Arrays.asList(this.getRecipeItems()), entry.craftingRequirements().get(), this.countRecipeItems()))
                                {
                                    ItemScroller.printDebug("storeSelectedRecipeIdFromGui(): Matched Ingredients for result stack [{}] networkId [{}]", this.getResult().toString(), id.index());
                                    this.storeNetworkRecipeId(id);
                                    this.storeRecipeCategory(entry.category());
                                    this.storeRecipeDisplayEntry(entry);
                                }
                                else
                                {
                                    ItemScroller.logger.warn("storeSelectedRecipeIdFromGui(): failed to match Ingredients for result stack [{}] networkId [{}]", this.getResult().toString(), id.index());
                                }
                            }
                            else
                            {
                                ItemScroller.printDebug("storeSelectedRecipeIdFromGui(): No craftingRequirements present, Saving Blindly for result stack [{}] networkId [{}]", this.getResult().toString(), id.index());
                                this.storeNetworkRecipeId(id);
                                this.storeRecipeCategory(entry.category());
                                this.storeRecipeDisplayEntry(entry);
                            }
                        }
                        else
                        {
                            // Go for broke, and iterate it.
                            Pair<NetworkRecipeId, RecipeDisplayEntry> pair = this.matchClientRecipeBook(mc);

                            if (pair != null)
                            {
                                ItemScroller.printDebug("storeSelectedRecipeIdFromGui(): matching pair for result stack [{}] networkId [{}]", this.getResult().toString(), pair.getLeft().index());
                                this.storeNetworkRecipeId(pair.getLeft());
                                this.storeRecipeCategory(pair.getRight().category());
                                this.storeRecipeDisplayEntry(pair.getRight());
                            }
                            else
                            {
                                // Sometimes the result gets de-sync to like an Iron Nugget, just copy it and try one last time (It should work)
                                this.result = result.copy();
                                pair = this.matchClientRecipeBook(mc);

                                if (pair != null)
                                {
                                    ItemScroller.printDebug("storeSelectedRecipeIdFromGui(): RE-matching pair results stack [{}] networkId [{}]", this.getResult().toString(), pair.getLeft().index());
                                    this.storeNetworkRecipeId(pair.getLeft());
                                    this.storeRecipeCategory(pair.getRight().category());
                                    this.storeRecipeDisplayEntry(pair.getRight());
                                }
                                else
                                {
                                    ItemScroller.logger.error("storeSelectedRecipeIdFromGui(): Final Exception matching results stack [{}] versus [{}] --> Clearing Recipe", this.getResult().toString(), result.toString());
                                    this.clearRecipe();
                                }
                            }
                        }
                    }
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
        this.category = other.category;
        this.recipeSaveTime = System.currentTimeMillis();
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

    public boolean isEmpty()
    {
        boolean empty = true;

        for (int i = 0; i < this.getRecipeLength(); i++)
        {
            if (!this.getRecipeItems()[i].isEmpty())
            {
                empty = false;
            }
        }

        return empty || this.getResult().isEmpty();
    }

    public int countRecipeItems()
    {
        int count = 0;

        for (ItemStack itemStack : this.recipe)
        {
            if (!itemStack.isEmpty())
            {
                count++;
            }
        }

        return count;
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
