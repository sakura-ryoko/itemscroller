package fi.dy.masa.itemscroller.recipes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.llamalad7.mixinextras.lib.apache.commons.tuple.Pair;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.game.RecipeBookUtils;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.mixin.recipe.IMixinClientRecipeBook;
import fi.dy.masa.itemscroller.mixin.recipe.IMixinRecipeBookWidget;
import fi.dy.masa.itemscroller.mixin.screen.IMixinRecipeBookScreen;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RecipePattern
{
    private ItemStack result = InventoryUtils.EMPTY_STACK;
    private ItemStack[] recipe = new ItemStack[9];
    private RecipeHolder<?> vanillaRecipe;
    private RecipeDisplayId networkRecipeId;
    private RecipeDisplayEntry displayEntry;
    private RecipeBookCategory category;
    private RecipeBookUtils.Type recipeType;
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
        this.recipeType = null;
        this.recipeSaveTime = -1;
    }

    public void ensureRecipeSizeAndClearRecipe(int size)
    {
        this.ensureRecipeSize(size);
        this.clearRecipe();
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends RecipeInput> Recipe<T> lookupVanillaRecipe(Level world)
    {
        //Assume all recipes here are of type CraftingRecipe
        this.vanillaRecipe = null;
        Minecraft mc = Minecraft.getInstance();
        int recipeSize;

        if (mc.level == null)
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

        ServerLevel serverWorld = mc.getSingleplayerServer() != null ? mc.getSingleplayerServer().getLevel(mc.level.dimension()) : null;

        if (mc.hasSingleplayerServer() && serverWorld != null)
        {
            CraftingInput input = CraftingInput.of(recipeSize, recipeSize, Arrays.asList(recipe));
            Optional<RecipeHolder<CraftingRecipe>> opt = serverWorld.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, serverWorld);

            if (opt.isPresent())
            {
                RecipeHolder<CraftingRecipe> recipeEntry = opt.get();
                Recipe<CraftingInput> match = opt.get().value();
                ItemStack result = match.assemble(input, serverWorld.registryAccess());

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

    public void storeIdFromClientRecipeBook(Minecraft mc)
    {
        Pair<RecipeDisplayId, RecipeDisplayEntry> pair = this.matchClientRecipeBook(mc);

        if (pair == null || pair.getLeft() == null || pair.getRight() == null)
        {
            return;
        }

        this.storeNetworkRecipeId(pair.getLeft());
        this.storeRecipeCategory(pair.getRight().category());
        this.storeRecipeDisplayEntry(pair.getRight());
        this.storeRecipeType(RecipeBookUtils.Type.fromRecipeDisplay(pair.getRight().display()));
    }

    public void storeNetworkRecipeId(RecipeDisplayId id)
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

    public void storeRecipeType(RecipeBookUtils.Type type)
    {
        this.recipeType = type;
    }

    public @Nullable RecipeDisplayId getNetworkRecipeId()
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

    public @Nullable RecipeBookUtils.Type getRecipeType()
    {
        return this.recipeType;
    }

    public boolean matchRecipeCategory(RecipeBookCategory category)
    {
        return this.getRecipeCategory() != null && this.getRecipeCategory().equals(category);
    }

    public boolean matchRecipeType(RecipeDisplayEntry entry)
    {
        return RecipeBookUtils.Type.fromRecipeDisplay(entry.display()) == this.recipeType;
    }

    public @Nullable Pair<RecipeDisplayId, RecipeDisplayEntry> matchClientRecipeBook(Minecraft mc)
    {
        Pair<RecipeDisplayId, RecipeDisplayEntry> pair;

        if (mc.player == null || mc.level == null || this.isEmpty())
        {
            return null;
        }

        ClientRecipeBook recipeBook = mc.player.getRecipeBook();
        ContextMap map = RecipeBookUtils.getMap(mc);
        Map<RecipeDisplayId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();

        if (recipeMap.size() < 1 || map == null)
        {
            return null;
        }

        for (RecipeDisplayId id : recipeMap.keySet())
        {
            RecipeDisplayEntry entry = recipeMap.get(id);

            if (entry != null)
            {
                if (this.getRecipeCategory() != null && !this.matchRecipeCategory(entry.category()))
                {
                    continue;
                }

                if (this.getRecipeType() != null && !this.matchRecipeType(entry))
                {
                    ItemScroller.LOGGER.warn("matchClientRecipeBook(): Type mismatch: [{} != {}]", this.getRecipeType().name(), RecipeBookUtils.Type.fromRecipeDisplay(entry.display()).name());
                    continue;
                }

                List<ItemStack> stacks = entry.resultItems(map);

                if (stacks.isEmpty())
                {
                    // And why would that be? *cries without essential data*
                    ItemScroller.LOGGER.warn("matchClientRecipeBook(): Failed receiving crafting stacks for NetworkRecipeId: [{}] -- is it even a valid recipe?", id.index());
                    continue;
                }

                if (RecipeBookUtils.areStacksEqual(this.getResult(), stacks.getFirst()))
                {
                    pair = Pair.of(id, entry);
                    return pair;
                }
            }
        }

        return null;
    }

//    @Deprecated(forRemoval = true)
//    public boolean matchClientRecipeBookEntry(RecipeDisplayEntry entry, MinecraftClient mc)
//    {
//        if (mc.world == null || this.isEmpty())
//        {
//            return false;
//        }
//
//        // Mojang breaks their own player recipe book.  Verifying the Category here can cause problems.
//        /*
//        if (this.getRecipeCategory() != null && !entry.category().equals(this.getRecipeCategory()))
//        {
//            return false;
//        }
//         */
//        List<ItemStack> recipeStacks = Arrays.stream(this.getRecipeItems()).toList();
//        List<ItemStack> stacks = entry.getStacks(SlotDisplayContexts.createParameters(mc.world));
//
//        //System.out.printf("matchClientRecipeBookEntry() --> [%s] vs [%s]\n", this.getResult().toString(), stacks.getFirst().toString());
//
//        if (stacks.isEmpty())
//        {
//            // And why would that be? *cries without essential data*
//            ItemScroller.LOGGER.warn("matchClientRecipeBookEntry(): Failed receiving crafting stacks for NetworkRecipeId: [{}] -- is it even a valid recipe?", entry.id().index());
//            return false;
//        }
//
//        if (RecipeBookUtils.areStacksEqual(this.getResult(), stacks.getFirst()))
//        {
//            if (entry.craftingRequirements().isPresent())
//            {
//                return RecipeUtils.compareStacksAndIngredients(recipeStacks, entry.craftingRequirements().get(), this.countRecipeItems(), RecipeUtils.Type.fromRecipeDisplay(entry.display()));
//            }
//
//            return true;
//        }
//
//        return false;
//    }

    public void storeCraftingRecipe(Slot slot, AbstractContainerScreen<? extends AbstractContainerMenu> gui, boolean clearIfEmpty, boolean fromKeybind, Minecraft mc)
    {
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

        if (range != null)
        {
            if (slot.hasItem())
            {
                int gridSize = range.getSlotCount();

                if (fromKeybind)
                {
                    // Slots are only populated from the Keybinds Callback
                    int numSlots = gui.getMenu().slots.size();
                    this.ensureRecipeSizeAndClearRecipe(gridSize);

                    for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
                    {
                        Slot slotTmp = gui.getMenu().getSlot(s);
                        this.recipe[i] = slotTmp.hasItem() ? slotTmp.getItem().copy() : InventoryUtils.EMPTY_STACK;
                    }
                    this.recipeSaveTime = System.currentTimeMillis();
                }
                // Stop the mod from overwriting the correctly saved recipe with a button or nugget from the Grid clear
                else if ((System.currentTimeMillis() - this.recipeSaveTime) < 4000L)
                {
                    //System.out.printf("storeCraftingRecipe() SKIPPING InputHandler input result [%s] versus [%s]\n", this.result.toString(), slot.getStack().toString());
                    this.recipeSaveTime = System.currentTimeMillis();
                    gui.getMenu().setCarried(ItemStack.EMPTY);
                    InventoryUtils.clearFirstCraftingGridOfAllItems(gui);
                    return;
                }

                //System.out.printf("storeCraftingRecipe() old result [%s] new [%s]\n", this.result.toString(), slot.getStack().toString());
                this.result = slot.getItem().copy();
                this.lookupVanillaRecipe(mc.level);
                this.storeSelectedRecipeIdFromGui(gui);
                InventoryUtils.clearFirstCraftingGridOfAllItems(gui);
            }
            else if (clearIfEmpty)
            {
                this.clearRecipe();
            }
        }
    }

    public void storeSelectedRecipeIdFromGui(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null || mc.player == null)
        {
            return;
        }

        List<RecipeBookUtils.Type> types;

        if (gui instanceof StonecutterScreen)
        {
            types = List.of(RecipeBookUtils.Type.STONECUTTER);
        }
        else
        {
            types = List.of(RecipeBookUtils.Type.SHAPED, RecipeBookUtils.Type.SHAPELESS);
        }

        // DEBUG
//        RecipeBookUtils.toggleDebugLog(true);
//        RecipeBookUtils.toggleAnsiColorLog(true);

        if (gui instanceof AbstractRecipeBookScreen<?> rbs)
        {
            RecipeBookComponent<?> widget = ((IMixinRecipeBookScreen) rbs).itemscroller_getRecipeBookWidget();

            if (widget != null)
            {
                RecipeDisplayId id = ((IMixinRecipeBookWidget) widget).itemscroller_getSelectedRecipe();

                if (id != null)
                {
                    ClientRecipeBook recipeBook = mc.player.getRecipeBook();
                    Map<RecipeDisplayId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();
                    ContextMap map = RecipeBookUtils.getMap(mc);

					if (map == null) return;
                    if (recipeMap.containsKey(id))
                    {
                        RecipeDisplayEntry entry = recipeMap.get(id);
                        List<ItemStack> stacks = entry.resultItems(map);

                        if (stacks.isEmpty())
                        {
                            // And why would that be? *cries without essential data*
                            ItemScroller.LOGGER.error("storeSelectedRecipeIdFromGui(): Failed reading crafting stacks for NetworkRecipeId: [{}] -- is it even a valid recipe?", entry.id().index());
                            return;
                        }

                        ItemStack result = stacks.getFirst();

                        if (RecipeBookUtils.areStacksEqual(this.getResult(), result))
                        {
                            if (entry.craftingRequirements().isPresent())
                            {
                                if (RecipeBookUtils.compareStacksAndIngredients(Arrays.asList(this.getRecipeItems()), entry.craftingRequirements().get(), RecipeBookUtils.Type.fromRecipeDisplay(entry.display()), types))
                                {
                                    ItemScroller.debugLog("storeSelectedRecipeIdFromGui(): Matched Ingredients for result stack [{}] networkId [{}]", this.getResult().toString(), id.index());
                                    this.storeNetworkRecipeId(id);
                                    this.storeRecipeCategory(entry.category());
                                    this.storeRecipeDisplayEntry(entry);
                                    this.storeRecipeType(RecipeBookUtils.Type.fromRecipeDisplay(entry.display()));
                                }
                                else
                                {
                                    ItemScroller.LOGGER.warn("storeSelectedRecipeIdFromGui(): failed to match Ingredients for result stack [{}] networkId [{}]", this.getResult().toString(), id.index());
                                }
                            }
                            else
                            {
                                ItemScroller.debugLog("storeSelectedRecipeIdFromGui(): No craftingRequirements present, Saving Blindly for result stack [{}] networkId [{}]", this.getResult().toString(), id.index());
                                this.storeNetworkRecipeId(id);
                                this.storeRecipeCategory(entry.category());
                                this.storeRecipeDisplayEntry(entry);
                                this.storeRecipeType(RecipeBookUtils.Type.fromRecipeDisplay(entry.display()));
                            }
                        }
                        else
                        {
                            // Go for broke, and iterate it.
                            Pair<RecipeDisplayId, RecipeDisplayEntry> pair = this.matchClientRecipeBook(mc);

                            if (pair != null)
                            {
                                ItemScroller.debugLog("storeSelectedRecipeIdFromGui(): matching pair for result stack [{}] networkId [{}]", this.getResult().toString(), pair.getLeft().index());
                                this.storeNetworkRecipeId(pair.getLeft());
                                this.storeRecipeCategory(pair.getRight().category());
                                this.storeRecipeDisplayEntry(pair.getRight());
                                this.storeRecipeType(RecipeBookUtils.Type.fromRecipeDisplay(entry.display()));
                            }
                            else
                            {
                                // Sometimes the result gets de-sync to like an Iron Nugget, just copy it and try one last time (It should work)
                                this.result = result.copy();
                                pair = this.matchClientRecipeBook(mc);

                                if (pair != null)
                                {
                                    ItemScroller.debugLog("storeSelectedRecipeIdFromGui(): RE-matching pair results stack [{}] networkId [{}]", this.getResult().toString(), pair.getLeft().index());
                                    this.storeNetworkRecipeId(pair.getLeft());
                                    this.storeRecipeCategory(pair.getRight().category());
                                    this.storeRecipeDisplayEntry(pair.getRight());
                                    this.storeRecipeType(RecipeBookUtils.Type.fromRecipeDisplay(entry.display()));
                                }
                                else
                                {
                                    ItemScroller.LOGGER.error("storeSelectedRecipeIdFromGui(): Final Exception matching results stack [{}] versus [{}] --> Clearing Recipe", this.getResult().toString(), result.toString());
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
        this.recipeType = other.recipeType;
        this.recipeSaveTime = System.currentTimeMillis();
    }

    public void readFromData(@Nonnull CompoundData data, @Nonnull RegistryAccess registry)
    {
        if (data.contains("Result", Constants.NBT.TAG_COMPOUND) && data.contains("Ingredients", Constants.NBT.TAG_LIST))
        {
            ListData tagIngredients = data.getList("Ingredients");
            int count = tagIngredients.size();
            int length = data.getInt("Length");

            if (length > 0)
            {
                this.ensureRecipeSizeAndClearRecipe(length);
            }

            for (int i = 0; i < count; i++)
            {
                CompoundData tag = tagIngredients.getCompoundAt(i);
                int slot = tag.getInt("Slot");

                if (slot >= 0 && slot < this.recipe.length)
                {
                    this.recipe[slot] = fi.dy.masa.malilib.util.InventoryUtils.fromDataOrEmpty(registry, tag);
                }
            }

            this.result = fi.dy.masa.malilib.util.InventoryUtils.fromDataOrEmpty(registry, data.getCompound("Result"));
        }
    }

    @Nonnull
    public CompoundData writeToData(@Nonnull RegistryAccess registry)
    {
	    CompoundData data = new CompoundData();

        if (this.isValid())
        {
	        CompoundData tag = fi.dy.masa.malilib.util.InventoryUtils.toDataOrEmpty(this.result, registry);

	        data.putInt("Length", this.recipe.length);
	        data.put("Result", tag);

            ListData tagIngredients = new ListData();

            for (int i = 0; i < this.recipe.length; i++)
            {
                if (this.recipe[i].isEmpty() == false && InventoryUtils.isStackEmpty(this.recipe[i]) == false)
                {
	                tag = fi.dy.masa.malilib.util.InventoryUtils.toDataOrEmpty(this.recipe[i], registry);
                    tag.putInt("Slot", i);
                    tagIngredients.add(tag);
                }
            }

	        data.put("Ingredients", tagIngredients);
        }

        return data;
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
    public RecipeHolder<?> getVanillaRecipeEntry()
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
