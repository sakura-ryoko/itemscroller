package fi.dy.masa.itemscroller.recipes;

import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;

import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import fi.dy.masa.itemscroller.mixin.screen.IMixinAbstractRecipeBookScreen;
import fi.dy.masa.itemscroller.mixin.screen.IMixinAnvilScreen;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.AccessorUtils;
import fi.dy.masa.itemscroller.util.InventoryUtils;

public class RecipePattern
{
    public static final String RECIPE_RESULT = "Result";
    public static final String RECIPE_INGREDIENTS = "Ingredients";
    public static final String RECIPE_LENGTH = "Length";
    public static final String RECIPE_SLOT = "Slot";

    public enum RecipeType
    {
        CRAFTING("crafting"),
        STONECUTTER("stonecutter"),
        ANVIL("anvil"),
        GRINDSTONE("grindstone"),
        LOOM("loom"),
        SMITHING("smithing"),
        ENCHANTMENT("enchantment");

        private final String id;

        RecipeType(String id) { this.id = id; }
        public String getId() { return this.id; }

        @Nullable
        public static RecipeType fromId(String id)
        {
            for (RecipeType t : values())
            {
                if (t.id.equals(id)) return t;
            }
            return null;
        }
    }

    private ItemStack result = InventoryUtils.EMPTY_STACK;
    private ItemStack[] recipe = new ItemStack[9];
    private RecipeHolder<?> vanillaRecipe;
    private RecipeDisplayId networkRecipeId;
    private RecipeDisplayEntry displayEntry;
    private RecipeBookCategory category;
    private RecipeBookUtils.Type recipeType;
    private long recipeSaveTime;

    // Processing GUI support (stonecutter / anvil / grindstone / loom / smithing / enchantment)
    private RecipeType processingType = RecipeType.CRAFTING;
    private ItemStack[] processingInputs = new ItemStack[0];
    private int selectedRecipe = -1;      // stonecutter / loom selected recipe index
    private int enchantmentOption = -1;   // enchantment table option (0-2)
    private String renameText = "";       // anvil rename text
    private boolean hasRename = false;    // anvil rename enabled
    private boolean enchantClickPending = false; // enchantment table: waiting for server sync after clicking an option

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
        this.processingType = RecipeType.CRAFTING;
        this.processingInputs = new ItemStack[0];
        this.selectedRecipe = -1;
        this.enchantmentOption = -1;
        this.renameText = "";
        this.hasRename = false;
        this.enchantClickPending = false;
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
            CraftingInput input = CraftingInput.of(recipeSize, recipeSize, Arrays.asList(this.recipe));
            Optional<RecipeHolder<CraftingRecipe>> opt = serverWorld.recipeAccess().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, serverWorld);

            if (opt.isPresent())
            {
                RecipeHolder<CraftingRecipe> recipeEntry = opt.get();
                Recipe<CraftingInput> match = opt.get().value();
                ItemStack result = match.assemble(input);

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

                if (CraftingHandler.isProcessingGui(gui))
                {
                    this.storeProcessingRecipe(slot, gui, range);
                }
                else if (fromKeybind || slot instanceof ResultSlot rs || CraftingHandler.isCraftingGui(gui))
                {
                    // Double-trigger protection: the middle mouse button fires both the InputHandler
                    // pick-block path and the STORE_RECIPE hotkey. The first call clears the grid,
                    // so the second call would read empty inputs and overwrite the stored recipe.
                    // Check BEFORE clearing, because clearRecipe() resets recipeSaveTime and result.
                    if (this.isValid() && (System.currentTimeMillis() - this.recipeSaveTime) < 4000L)
                    {
                        boolean gridEmpty = true;
                        int numSlotsCheck = gui.getMenu().slots.size();

                        for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlotsCheck; i++, s++)
                        {
                            if (gui.getMenu().getSlot(s).hasItem())
                            {
                                gridEmpty = false;
                                break;
                            }
                        }

                        if (gridEmpty)
                        {
                            ItemScroller.debugLog("storeCraftingRecipe(): Skipping empty-input overwrite (double trigger), keeping existing recipe");
                            return;
                        }
                    }

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
//                    System.out.printf("storeCraftingRecipe() SKIPPING InputHandler input result [%s] versus [%s]\n", this.result.toString(), slot.getItem().toString());
                    this.recipeSaveTime = System.currentTimeMillis();
                    gui.getMenu().setCarried(ItemStack.EMPTY);
                    return;
                }

//                System.out.printf("storeCraftingRecipe() old result [%s] new [%s]\n", this.result.toString(), slot.getItem().toString());
                this.result = slot.getItem().copy();

                if (CraftingHandler.isProcessingGui(gui) == false)
                {
                    this.lookupVanillaRecipe(mc.level);

                    if (this.vanillaRecipe == null)
                    {
                        this.storeSelectedRecipeIdFromGui(gui);
                    }
                }
            }
            else if (clearIfEmpty)
            {
                // Double-trigger protection: the middle mouse button fires both the InputHandler
                // pick-block path and the STORE_RECIPE hotkey. The first call stores the recipe,
                // the second call sees the (now empty) output slot and would clear the recipe.
                if (this.isValid() && (System.currentTimeMillis() - this.recipeSaveTime) < 4000L)
                {
                    ItemScroller.debugLog("storeCraftingRecipe(): Skipping clearRecipe (double trigger), keeping existing recipe");
                    return;
                }
                this.clearRecipe();
            }
        }
    }

    /** Store a recipe from a processing GUI (stonecutter / anvil / grindstone / loom / smithing / enchantment) */
    private void storeProcessingRecipe(Slot slot, AbstractContainerScreen<? extends AbstractContainerMenu> gui, SlotRange range)
    {
        int numSlots = gui.getMenu().slots.size();
        int gridSize = range.getSlotCount();

        // Double-trigger protection (same as the crafting table): the middle mouse button fires
        // both the InputHandler pick-block path and the STORE_RECIPE hotkey. The first call clears
        // the input slots, so the second call would read empty inputs and overwrite the stored recipe.
        if (this.isValid() && (System.currentTimeMillis() - this.recipeSaveTime) < 4000L)
        {
            boolean inputsEmpty = true;

            for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
            {
                if (gui.getMenu().getSlot(s).hasItem())
                {
                    inputsEmpty = false;
                    break;
                }
            }

            if (inputsEmpty)
            {
                ItemScroller.debugLog("storeProcessingRecipe(): Skipping empty-input overwrite (double trigger), keeping existing recipe");
                return;
            }
        }

        ItemStack[] inputs = new ItemStack[gridSize];
        Arrays.fill(inputs, InventoryUtils.EMPTY_STACK);

        for (int i = 0, s = range.getFirst(); i < gridSize && s < numSlots; i++, s++)
        {
            Slot slotTmp = gui.getMenu().getSlot(s);
            inputs[i] = slotTmp.hasItem() ? slotTmp.getItem().copy() : InventoryUtils.EMPTY_STACK;
        }

        this.processingInputs = inputs;
        this.result = slot.getItem().copy();
        this.recipeSaveTime = System.currentTimeMillis();

        if (gui instanceof StonecutterScreen && gui.getMenu() instanceof StonecutterMenu menu)
        {
            this.processingType = RecipeType.STONECUTTER;
            this.selectedRecipe = menu.getSelectedRecipeIndex();
        }
        else if (gui instanceof AnvilScreen && gui.getMenu() instanceof AnvilMenu menu)
        {
            this.processingType = RecipeType.ANVIL;
            EditBox nameField = ((IMixinAnvilScreen) gui).itemscroller_getNameField();
            this.renameText = nameField != null ? nameField.getValue() : "";
            this.hasRename = !this.renameText.isEmpty();
        }
        else if (gui instanceof GrindstoneScreen)
        {
            this.processingType = RecipeType.GRINDSTONE;
        }
        else if (gui instanceof LoomScreen && gui.getMenu() instanceof LoomMenu menu)
        {
            this.processingType = RecipeType.LOOM;
            this.selectedRecipe = menu.getSelectedBannerPatternIndex();
        }
        else if (gui instanceof SmithingScreen)
        {
            this.processingType = RecipeType.SMITHING;
        }
        else if (gui instanceof EnchantmentScreen)
        {
            this.processingType = RecipeType.ENCHANTMENT;
            this.enchantmentOption = 0;
        }
    }

    public boolean isProcessingRecipe()
    {
        return this.processingType != RecipeType.CRAFTING;
    }

    public RecipeType getProcessingType()
    {
        return this.processingType;
    }

    public void setProcessingType(RecipeType type)
    {
        this.processingType = type;
    }

    public int getSelectedRecipe()
    {
        return this.selectedRecipe;
    }

    public int getEnchantmentOption()
    {
        return this.enchantmentOption;
    }

    public void setEnchantmentOption(int option)
    {
        this.enchantmentOption = option;
    }

    public String getRenameText()
    {
        return this.renameText;
    }

    public boolean hasRename()
    {
        return this.hasRename;
    }

    /** Extra display text shown under the recipe inputs in the recipe view (enchantment level / anvil rename) */
    @Nullable
    public String getDisplayText()
    {
        if (this.processingType == RecipeType.ENCHANTMENT && this.enchantmentOption >= 0)
        {
            return "Lv." + (this.enchantmentOption + 1);
        }
        if (this.processingType == RecipeType.ANVIL && this.hasRename)
        {
            return this.renameText;
        }
        return null;
    }

    /** Fill the processing GUI input slots from the player inventory, then select recipe / option / rename */
    public void fillProcessingInputs(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        if (!this.isValid() || !CraftingHandler.isProcessingGui(gui)) return;

        CraftingHandler.ProcessingGuiDef def = CraftingHandler.getProcessingGuiDef(gui);
        if (def == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        for (int i = 0; i < this.processingInputs.length && i < def.inputCount(); i++)
        {
            int slotNum = def.inputFirst() + i;
            if (slotNum >= gui.getMenu().slots.size()) break;

            ItemStack template = this.processingInputs[i];
            if (template.isEmpty()) continue;

            Slot slot = gui.getMenu().getSlot(slotNum);

            // Remove any non-matching item from the slot first, so the correct item can be moved in
            if (slot.hasItem() && InventoryUtils.areStacksEqual(slot.getItem(), template) == false)
            {
                InventoryUtils.shiftClickSlot(gui, slotNum);
            }

            if (slot.hasItem() == false || InventoryUtils.areStacksEqual(slot.getItem(), template) == false)
            {
                InventoryUtils.tryClearCursor(gui);
                InventoryUtils.moveItemsFromInventory(gui, slotNum, mc.player.getInventory(), template, true);
            }
        }

        this.selectProcessingOptions(gui);
    }

    /** Clear the processing GUI input slots */
    public void clearProcessingInputs(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        CraftingHandler.ProcessingGuiDef def = CraftingHandler.getProcessingGuiDef(gui);
        if (def == null) return;

        for (int i = 0; i < def.inputCount(); i++)
        {
            int slotNum = def.inputFirst() + i;
            if (slotNum < gui.getMenu().slots.size())
            {
                Slot slot = gui.getMenu().getSlot(slotNum);
                if (slot.hasItem())
                {
                    InventoryUtils.shiftClickSlot(gui, slotNum);
                }
            }
        }
    }

    /** Select the stored recipe / enchantment option / rename text in the processing GUI */
    private void selectProcessingOptions(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Minecraft mc = Minecraft.getInstance();

        switch (this.processingType)
        {
            case STONECUTTER ->
            {
                // Always click the button: the client-side selected index may not be synced yet,
                // so comparing it can skip the click and leave the server on the wrong recipe,
                // which makes the output slot never match the stored result.
                if (gui.getMenu() instanceof StonecutterMenu menu && this.selectedRecipe >= 0)
                {
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, this.selectedRecipe);
                }
            }
            case LOOM ->
            {
                // Always click the button (same reason as STONECUTTER)
                if (gui.getMenu() instanceof LoomMenu menu && this.selectedRecipe >= 0)
                {
                    mc.gameMode.handleInventoryButtonClick(menu.containerId, this.selectedRecipe);
                }
            }
            case ENCHANTMENT ->
            {
                // The enchantment option click is handled by craftEnchantment() with sync-wait
                // protection. Clicking here too would double-click (double XP cost) when
                // fillProcessingInputs() is called from within craftEnchantment().
            }
            case ANVIL ->
            {
                if (gui instanceof AnvilScreen anvilScreen && this.hasRename)
                {
                    Slot leftSlot = gui.getMenu().getSlot(0);
                    Slot rightSlot = gui.getMenu().getSlot(1);

                    if (leftSlot.hasItem() == false) return;
                    if (this.processingInputs.length > 1 && this.processingInputs[1].isEmpty() == false && rightSlot.hasItem() == false) return;

                    EditBox nameField = ((IMixinAnvilScreen) anvilScreen).itemscroller_getNameField();
                    if (nameField != null)
                    {
                        String currentName = nameField.getValue();
                        if (!this.renameText.equals(currentName))
                        {
                            nameField.setValue(this.renameText);
                        }
                        nameField.setFocused(false);
                    }
                }
            }
            default -> { }
        }
    }

    /** Hook called after the generic crafting grid fill logic has filled the input slots */
    public void onGridFilled(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        if (this.isProcessingRecipe())
        {
            this.selectProcessingOptions(gui);
        }
    }

    /** Mass craft in a processing GUI, shift-clicking the output into the player inventory.
     *  Single craft per tick: the output slot update is server-synced with a delay (the
     *  stonecutter has no client-side prediction), so a loop here would see an empty output
     *  slot after the first shift-click and exit. The onClientTick handler calls this every
     *  tick while the key is held. */
    public void craftProcessingAsMany(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        if (!this.isValid() || !CraftingHandler.isProcessingGui(gui)) return;

        Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);
        if (outputSlot == null) return;

        if (this.processingType == RecipeType.ENCHANTMENT)
        {
            this.craftEnchantment(gui, outputSlot, false);
            return;
        }

        // Make sure the input slots are filled (refill consumed items)
        this.fillProcessingInputs(gui);

        ItemStack resultStack = this.getResult();

        if (outputSlot.hasItem() && InventoryUtils.areStacksEqual(outputSlot.getItem(), resultStack))
        {
            InventoryUtils.shiftClickSlot(gui, outputSlot.index);
        }
    }

    /** Mass craft in a processing GUI, dropping the output (keeps items in the player inventory).
     *  Single craft per tick (see craftProcessingAsMany). */
    public void craftProcessingAsManyAndKeep(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        if (!this.isValid() || !CraftingHandler.isProcessingGui(gui)) return;

        Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);
        if (outputSlot == null) return;

        if (this.processingType == RecipeType.ENCHANTMENT)
        {
            this.craftEnchantment(gui, outputSlot, true);
            return;
        }

        // Make sure the input slots are filled (refill consumed items)
        this.fillProcessingInputs(gui);

        ItemStack resultStack = this.getResult();

        if (outputSlot.hasItem() && InventoryUtils.areStacksEqual(outputSlot.getItem(), resultStack))
        {
            InventoryUtils.dropStacksWhileHasItem(gui, outputSlot.index, resultStack);
        }
    }

    /** Fill the inputs and mass craft in a processing GUI.
     *  Note: no clearProcessingInputs() here - clearing and refilling in the same tick makes the
     *  output slot disappear (server sync delay) and the craft loop sees an empty output slot.
     *  fillProcessingInputs() already removes non-matching items from the input slots. */
    public void craftProcessingEverything(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        if (!this.isValid() || !CraftingHandler.isProcessingGui(gui)) return;

        this.fillProcessingInputs(gui);
        this.craftProcessingAsMany(gui);
    }

    /** Enchantment table special handling: fill -> click option -> move enchanted item out -> repeat */
    private void craftEnchantment(AbstractContainerScreen<? extends AbstractContainerMenu> gui, Slot outputSlot, boolean keep)
    {
        if (!(gui.getMenu() instanceof EnchantmentMenu menu)) return;

        Slot inputSlot = menu.getSlot(0);
        Slot lapisSlot = menu.getSlot(1);
        Minecraft mc = Minecraft.getInstance();

        if (!inputSlot.hasItem())
        {
            this.enchantClickPending = false;
            // Refill the input slot with the next item so the mass enchanting can continue
            this.fillProcessingInputs(gui);
            return;
        }

        // Enchanted: move the item out and clear the pending flag
        if (inputSlot.getItem().isEnchanted())
        {
            this.enchantClickPending = false;

            if (keep)
            {
                InventoryUtils.dropStacksWhileHasItem(gui, inputSlot.index, inputSlot.getItem());
            }
            else
            {
                InventoryUtils.shiftClickSlot(gui, inputSlot.index);
            }

            // Refill the input slot with the next item so the mass enchanting can continue
            this.fillProcessingInputs(gui);
            return;
        }

        // After clicking an enchantment option, wait for the server to sync the result
        // (the item becomes enchanted). Do NOT click again while waiting, otherwise the
        // repeated clicks get rejected / double-charge and the GUI flickers without enchanting.
        if (this.enchantClickPending)
        {
            return;
        }

        if (this.processingInputs.length > 0 &&
            inputSlot.getItem().is(this.processingInputs[0].getItem()) &&
            lapisSlot.hasItem() && lapisSlot.getItem().is(Items.LAPIS_LAZULI) &&
            this.enchantmentOption >= 0 && this.enchantmentOption <= 2)
        {
            int[] powers = menu.costs;
            if (this.enchantmentOption < powers.length && powers[this.enchantmentOption] > 0)
            {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, this.enchantmentOption);
                this.enchantClickPending = true;
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
            RecipeBookComponent<?> widget = ((IMixinAbstractRecipeBookScreen) rbs).itemscroller_getRecipeBookWidget();
            List<Pair<RecipeDisplayId, RecipeDisplayEntry>> idList = new ArrayList<>();

            if (widget != null)
            {
                if (((IMixinRecipeBookWidget) widget).itemscroller_getLastRecipe() == null)
                {
                    // 26.3+ Seems to have the Widget Empty while the Recipe Book is not open / clicked on.
                    Slot hoveredSlot = AccessorUtils.getSlotUnderMouse(rbs);
                    ItemStack hoveredItem = hoveredSlot != null ? hoveredSlot.getItem() : ItemStack.EMPTY;
                    List<Pair<RecipeDisplayId, RecipeDisplayEntry>> pair = RecipeBookUtils.getDisplayEntryFromRecipeBook(hoveredItem, types);

                    if (pair != null && !pair.isEmpty())
                    {
                        idList.addAll(pair);
                    }
                }
                else
                {
                    RecipeDisplayId widgetId = ((IMixinRecipeBookWidget) widget).itemscroller_getLastRecipe();
                    RecipeDisplayEntry widgetEntry;
                    ClientRecipeBook recipeBook = mc.player.getRecipeBook();
                    Map<RecipeDisplayId, RecipeDisplayEntry> recipeMap = ((IMixinClientRecipeBook) recipeBook).itemscroller_getRecipeMap();

                    if (widgetId != null && recipeMap.containsKey(widgetId))
                    {
                        widgetEntry = recipeMap.get(widgetId);
                        idList.add(Pair.of(widgetId, widgetEntry));
                    }
                }

                if (!idList.isEmpty())
                {
                    ContextMap map = RecipeBookUtils.getMap(mc);

                    if (map == null) { return; }
	                for (Pair<RecipeDisplayId, RecipeDisplayEntry> pair : idList)
	                {
		                if (pair != null)
		                {
			                RecipeDisplayId id = pair.getLeft();
			                RecipeDisplayEntry entry = pair.getRight();
			                List<ItemStack> stacks = entry.resultItems(map);

			                if (stacks.isEmpty())
			                {
				                // And why would that be? *cries without essential data*
				                ItemScroller.LOGGER.error("storeSelectedRecipeIdFromGui(): Failed reading crafting stacks for NetworkRecipeId: [{}] -- is it even a valid recipe?", entry.id().index());
				                continue;
			                }

			                for (ItemStack resultStack : stacks)
			                {
				                if (RecipeBookUtils.areStacksEqual(this.getResult(), resultStack))
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
        if (data.contains(RECIPE_RESULT, Constants.NBT.TAG_COMPOUND) && data.contains(RECIPE_INGREDIENTS, Constants.NBT.TAG_LIST))
        {
            ListData tagIngredients = data.getList(RECIPE_INGREDIENTS);
            int count = tagIngredients.size();
            int length = data.getInt(RECIPE_LENGTH);

            if (length > 0)
            {
                this.ensureRecipeSizeAndClearRecipe(length);
            }

            for (int i = 0; i < count; i++)
            {
                CompoundData tag = tagIngredients.getCompoundAt(i);
                int slot = tag.getInt(RECIPE_SLOT);

                if (slot >= 0 && slot < this.recipe.length)
                {
                    this.recipe[slot] = fi.dy.masa.malilib.util.InventoryUtils.fromDataOrEmpty(registry, tag);
                }
            }

            this.result = fi.dy.masa.malilib.util.InventoryUtils.fromDataOrEmpty(registry, data.getCompound(RECIPE_RESULT));
        }

        // Processing GUI recipe data
        if (data.contains("ProcessingType", Constants.NBT.TAG_STRING))
        {
            RecipeType type = RecipeType.fromId(data.getString("ProcessingType"));

            if (type != null && type != RecipeType.CRAFTING)
            {
                this.processingType = type;
                this.processingInputs = new ItemStack[0];
                this.selectedRecipe = -1;
                this.enchantmentOption = -1;
                this.renameText = "";
                this.hasRename = false;

                if (data.contains("ProcessingInputs", Constants.NBT.TAG_LIST))
                {
                    ListData tagInputs = data.getList("ProcessingInputs");
                    int count = tagInputs.size();
                    ItemStack[] inputs = new ItemStack[count];

                    for (int i = 0; i < count; i++)
                    {
                        inputs[i] = fi.dy.masa.malilib.util.InventoryUtils.fromDataOrEmpty(registry, tagInputs.getCompoundAt(i));
                    }

                    this.processingInputs = inputs;
                }
                if (data.contains("SelectedRecipe", Constants.NBT.TAG_INT))
                {
                    this.selectedRecipe = data.getInt("SelectedRecipe");
                }
                if (data.contains("EnchantmentOption", Constants.NBT.TAG_INT))
                {
                    this.enchantmentOption = data.getInt("EnchantmentOption");
                }
                if (data.contains("RenameText", Constants.NBT.TAG_STRING))
                {
                    this.renameText = data.getString("RenameText");
                    this.hasRename = !this.renameText.isEmpty();
                }
            }
        }
    }

    @Nonnull
    public CompoundData writeToData(@Nonnull RegistryAccess registry)
    {
	    CompoundData data = new CompoundData();

        if (this.isValid())
        {
	        CompoundData tag = fi.dy.masa.malilib.util.InventoryUtils.toDataOrEmpty(this.result, registry);

	        data.putInt(RECIPE_LENGTH, this.recipe.length);
	        data.put(RECIPE_RESULT, tag);

            if (this.isProcessingRecipe())
            {
                data.putString("ProcessingType", this.processingType.getId());

                ListData tagInputs = new ListData();

                for (ItemStack stack : this.processingInputs)
                {
                    tagInputs.add(fi.dy.masa.malilib.util.InventoryUtils.toDataOrEmpty(stack, registry));
                }

                data.put("ProcessingInputs", tagInputs);

                if (this.selectedRecipe >= 0)
                {
                    data.putInt("SelectedRecipe", this.selectedRecipe);
                }
                if (this.enchantmentOption >= 0)
                {
                    data.putInt("EnchantmentOption", this.enchantmentOption);
                }
                if (this.hasRename)
                {
                    data.putString("RenameText", this.renameText);
                }
            }
            else
            {
                ListData tagIngredients = new ListData();

                for (int i = 0; i < this.recipe.length; i++)
                {
                    if (this.recipe[i].isEmpty() == false && InventoryUtils.isStackEmpty(this.recipe[i]) == false)
                    {
                        tag = fi.dy.masa.malilib.util.InventoryUtils.toDataOrEmpty(this.recipe[i], registry);
                        tag.putInt(RECIPE_SLOT, i);
                        tagIngredients.add(tag);
                    }
                }

                data.put(RECIPE_INGREDIENTS, tagIngredients);
            }
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
        return this.isProcessingRecipe() ? this.processingInputs.length : this.recipe.length;
    }

    public ItemStack[] getRecipeItems()
    {
        return this.isProcessingRecipe() ? this.processingInputs : this.recipe;
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

        for (ItemStack itemStack : this.getRecipeItems())
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
