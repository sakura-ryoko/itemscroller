package fi.dy.masa.itemscroller.util;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntIntMutablePair;
import org.apache.commons.lang3.math.Fraction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.gamerules.GameRules;

import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.game.wrap.GameWrap;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.mixin.recipe.IMixinCraftingResultSlot;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.villager.VillagerDataStorage;
import fi.dy.masa.itemscroller.villager.VillagerUtils;

public class InventoryUtils
{
    private static final Set<Integer> DRAGGED_SLOTS = new HashSet<>();
    private static final int SERVER_SYNC_MAGIC = 45510;
    public static int dontUpdateRecipeBook;

    private static WeakReference<Slot> sourceSlotCandidate = null;
    private static WeakReference<Slot> sourceSlot = null;
    private static ItemStack stackInCursorLast = ItemStack.EMPTY;
    @Nullable protected static CraftingRecipe lastRecipe;
    private static MoveAction activeMoveAction = MoveAction.NONE;
    private static int lastPosX;
    private static int lastPosY;
    private static int slotNumberLast;
    private static boolean inhibitCraftResultUpdate;
    private static Runnable selectedSlotUpdateTask;
    public static boolean assumeEmptyShulkerStacking = false;
    private static List<String> topSortingPriorityList = Configs.Generic.SORT_TOP_PRIORITY_INVENTORY.getStrings();
    private static List<String> bottomSortingPriorityList = Configs.Generic.SORT_BOTTOM_PRIORITY_INVENTORY.getStrings();
    public static boolean bufferInvUpdates = false;
    public static List<Packet<ClientGamePacketListener>> invUpdatesBuffer = new ArrayList<>();
    private static CreativeModeTab.ItemDisplayParameters displayContext;
    public static boolean ignoreScrollingInsideOfBundles = false;

    /*
    private static Pair<Integer, Integer> lastSwapTry = Pair.of(-1, -1);
    private static int repeatedSwaps = 0;
    private static int MAX_REPEATED = 5;
    private static List<Pair<Integer, Integer>> hotbarSwaps = new ArrayList<>();
     */

    public static void setInhibitCraftingOutputUpdate(boolean inhibitUpdate)
    {
        inhibitCraftResultUpdate = inhibitUpdate;
    }

    public static void setIgnoreScrollingInsideOfBundles(boolean toggle)
    {
        ignoreScrollingInsideOfBundles = toggle;
    }

    public static void onSlotChangedCraftingGrid(Player player,
                                                 CraftingContainer craftMatrix,
                                                 ResultContainer inventoryCraftResult)
    {
//        if (inhibitCraftResultUpdate && Configs.Generic.MASS_CRAFT_INHIBIT_MID_UPDATES.getBooleanValue())
//        {
//            return;
//        }

        if (Configs.Generic.CLIENT_CRAFTING_FIX.getBooleanValue())
        {
            updateCraftingOutputSlot(player, craftMatrix, inventoryCraftResult, true);
        }
    }

    public static void updateCraftingOutputSlot(Slot outputSlot)
    {
        Player player = Minecraft.getInstance().player;

        if (player != null &&
            outputSlot instanceof ResultSlot resultSlot &&
            resultSlot.container instanceof ResultContainer resultInv)
        {
            CraftingContainer craftingInv = ((IMixinCraftingResultSlot) outputSlot).itemscroller_getCraftingInventory();
            updateCraftingOutputSlot(player, craftingInv, resultInv, true);
        }
    }

    public static void updateCraftingOutputSlot(Player player,
                                                CraftingContainer craftMatrix,
                                                ResultContainer inventoryCraftResult,
                                                boolean setEmptyStack)
    {
        Minecraft mc = Minecraft.getInstance();
        ServerLevel serverWorld = mc.getSingleplayerServer() != null ? mc.getSingleplayerServer().getLevel(mc.level.dimension()) : null;
        Level world = player.level();

        if ((world instanceof ClientLevel) && player instanceof LocalPlayer)
        {
            ItemStack stack = ItemStack.EMPTY;
            CraftingRecipe recipe = Configs.Generic.USE_RECIPE_CACHING.getBooleanValue() ? lastRecipe : null;
            RecipeHolder<?> recipeEntry = null;
            CraftingInput recipeInput = craftMatrix.asCraftInput();

            if ((recipe == null || recipe.matches(recipeInput, world) == false) &&
                (serverWorld != null))
            {
                Optional<RecipeHolder<CraftingRecipe>> opt = serverWorld.recipeAccess().getRecipeFor(RecipeType.CRAFTING, recipeInput, serverWorld);
                recipe = opt.map(RecipeHolder::value).orElse(null);
                recipeEntry = opt.orElse(null);
            }

            if (recipe != null)
            {
                GameRules rules = new GameRules(((LocalPlayer) player).connection.enabledFeatures());

                if ((recipe.isSpecial() ||
                    rules.get(GameRules.LIMITED_CRAFTING) == false))
                {
                    inventoryCraftResult.setRecipeUsed(recipeEntry);
                    stack = recipe.assemble(recipeInput, world.registryAccess());
                }

                if (setEmptyStack || stack.isEmpty() == false)
                {
                    inventoryCraftResult.setItem(0, stack);
                }
            }

            lastRecipe = recipe;
        }
    }

    public static String getStackString(ItemStack stack)
    {
        if (isStackEmpty(stack) == false)
        {
            Identifier rl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String idStr = rl != null ? rl.toString() : "null";
            String displayName = stack.getHoverName().getString();
            String nbtStr = stack.getComponents() != null ? stack.getComponents().toString() : "<no NBT>";

            return String.format("[%s - display: %s - NBT: %s] (%s)", idStr, displayName, nbtStr, stack);
        }

        return "<empty>";
    }

    public static void debugPrintSlotInfo(AbstractContainerScreen<? extends AbstractContainerMenu> gui, Slot slot)
    {
        if (slot == null)
        {
            ItemScroller.LOGGER.info("slot was null");
            return;
        }

        boolean hasSlot = gui.getMenu().slots.contains(slot);
        Object inv = slot.container;
        String stackStr = InventoryUtils.getStackString(slot.getItem());

        ItemScroller.LOGGER.info(String.format("slot: slotNumber: %d, getSlotIndex(): %d, getHasStack(): %s, " +
                "slot class: %s, inv class: %s, Container's slot list has slot: %s, stack: %s, numSlots: %d",
                                               slot.index, AccessorUtils.getSlotIndex(slot), slot.hasItem(), slot.getClass().getName(),
                inv != null ? inv.getClass().getName() : "<null>", hasSlot ? " true" : "false", stackStr,
                                               gui.getMenu().slots.size()));
    }

    private static boolean isValidSlot(Slot slot, AbstractContainerScreen<? extends AbstractContainerMenu> gui, boolean requireItems)
    {
        AbstractContainerMenu container = gui.getMenu();

        return container != null && container.slots != null &&
                slot != null && container.slots.contains(slot) &&
                (requireItems == false || slot.hasItem()) &&
                Configs.SLOT_BLACKLIST.contains(slot.getClass().getName()) == false;
    }

    public static boolean isCraftingSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, @Nullable Slot slot)
    {
        return slot != null && CraftingHandler.getCraftingGridSlots(gui, slot) != null;
    }

    /**
     * Checks if there are slots belonging to another inventory on screen above the given slot
     */
    private static boolean inventoryExistsAbove(Slot slot, AbstractContainerMenu container)
    {
        for (Slot slotTmp : container.slots)
        {
            if (slotTmp.y < slot.y && areSlotsInSameInventory(slot, slotTmp) == false)
            {
                return true;
            }
        }

        return false;
    }

    public static boolean canShiftPlaceItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);
        ItemStack stackCursor = gui.getMenu().getCarried();

        // The target slot needs to be an empty, valid slot, and there needs to be items in the cursor
        return slot != null && isStackEmpty(stackCursor) == false && isValidSlot(slot, gui, false) &&
               slot.hasItem() == false && slot.mayPlace(stackCursor);
    }

    public static boolean tryMoveItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                       RecipeStorage recipes,
                                       boolean scrollingUp)
    {
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);

        // We require an empty cursor
        if (slot == null || isStackEmpty(gui.getMenu().getCarried()) == false || ignoreScrollingInsideOfBundles)
        {
            return false;
        }

        // Villager handling only happens when scrolling over the trade output slot
        boolean villagerHandling = Configs.Toggles.SCROLL_VILLAGER.getBooleanValue() && gui instanceof MerchantScreen && slot instanceof MerchantResultSlot;
        boolean craftingHandling = Configs.Toggles.CRAFTING_FEATURES.getBooleanValue() && isCraftingSlot(gui, slot);
        boolean keyActiveMoveEverything = Hotkeys.MODIFIER_MOVE_EVERYTHING.getKeybind().isKeybindHeld();
        boolean keyActiveMoveMatching = Hotkeys.MODIFIER_MOVE_MATCHING.getKeybind().isKeybindHeld();
        boolean keyActiveMoveStacks = Hotkeys.MODIFIER_MOVE_STACK.getKeybind().isKeybindHeld();
        boolean nonSingleMove = keyActiveMoveEverything || keyActiveMoveMatching || keyActiveMoveStacks;
        boolean moveToOtherInventory = scrollingUp;

        if (Configs.Generic.SLOT_POSITION_AWARE_SCROLL_DIRECTION.getBooleanValue())
        {
            boolean above = inventoryExistsAbove(slot, gui.getMenu());
            // so basically: (above && scrollingUp) || (above == false && scrollingUp == false)
            moveToOtherInventory = (above == scrollingUp);
        }

        if ((Configs.Generic.REVERSE_SCROLL_DIRECTION_SINGLE.getBooleanValue() && nonSingleMove == false) ||
            (Configs.Generic.REVERSE_SCROLL_DIRECTION_STACKS.getBooleanValue() && nonSingleMove))
        {
            moveToOtherInventory = ! moveToOtherInventory;
        }

        // Check that the slot is valid, (don't require items in case of the villager output slot or a crafting slot)
        if (isValidSlot(slot, gui, villagerHandling == false && craftingHandling == false) == false)
        {
            return false;
        }

        if (craftingHandling)
        {
            return tryMoveItemsCrafting(recipes, slot, gui, moveToOtherInventory, keyActiveMoveStacks, keyActiveMoveEverything);
        }

        if (villagerHandling)
        {
            return tryMoveItemsVillager((MerchantScreen) gui, slot, moveToOtherInventory, keyActiveMoveStacks);
        }

        if ((Configs.Toggles.SCROLL_SINGLE.getBooleanValue() == false && nonSingleMove == false) ||
            (Configs.Toggles.SCROLL_STACKS.getBooleanValue() == false && keyActiveMoveStacks) ||
            (Configs.Toggles.SCROLL_MATCHING.getBooleanValue() == false && keyActiveMoveMatching) ||
            (Configs.Toggles.SCROLL_EVERYTHING.getBooleanValue() == false && keyActiveMoveEverything))
        {
            return false;
        }

        // Move everything
        if (keyActiveMoveEverything)
        {
            tryMoveStacks(slot, gui, false, moveToOtherInventory, false);
        }
        // Move all matching items
        else if (keyActiveMoveMatching)
        {
            tryMoveStacks(slot, gui, true, moveToOtherInventory, false);
            return true;
        }
        // Move one matching stack
        else if (keyActiveMoveStacks)
        {
            tryMoveStacks(slot, gui, true, moveToOtherInventory, true);
        }
        else
        {
            ItemStack stack = slot.getItem();

            // Scrolling items from this slot/inventory into the other inventory
            if (moveToOtherInventory)
            {
                tryMoveSingleItemToOtherInventory(slot, gui);
            }
            // Scrolling items from the other inventory into this slot/inventory
            else if (getStackSize(stack) < slot.getMaxStackSize(stack))
            {
                tryMoveSingleItemToThisInventory(slot, gui);
            }
        }

        return false;
    }

    public static boolean dragMoveItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                        MoveAction action,
                                        int mouseX, int mouseY, boolean isClick)
    {
        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            // Updating these here is part of the fix to preventing a drag after shift + place
            lastPosX = mouseX;
            lastPosY = mouseY;
            stopDragging();

            return false;
        }

        boolean cancel = false;

        if (isClick && action != MoveAction.NONE)
        {
            // Reset this or the method call won't do anything...
            slotNumberLast = -1;
            lastPosX = mouseX;
            lastPosY = mouseY;
            activeMoveAction = action;
            cancel = dragMoveFromSlotAtPosition(gui, mouseX, mouseY, action);
        }
        else
        {
            action = activeMoveAction;
        }

        if (activeMoveAction != MoveAction.NONE && cancel == false)
        {
	        int distX = (int) (mouseX - lastPosX);
	        int distY = (int) (mouseY - lastPosY);
	        int absX = Math.abs(distX);
	        int absY = Math.abs(distY);

            if (absX > absY)
            {
                int inc = distX > 0 ? 1 : -1;

                for (int x = lastPosX; ; x += inc)
                {
	                int y = absX != 0 ? lastPosY + ((x - lastPosX) * distY / absX) : mouseY;
                    dragMoveFromSlotAtPosition(gui, x, y, action);

                    if (x == mouseX)
                    {
                        break;
                    }
                }
            }
            else
            {
                int inc = distY > 0 ? 1 : -1;

                for (int y = lastPosY; ; y += inc)
                {
	                int x = absY != 0 ? lastPosX + ((y - lastPosY) * distX / absY) : mouseX;
                    dragMoveFromSlotAtPosition(gui, x, y, action);

                    if (y == mouseY)
                    {
                        break;
                    }
                }
            }
        }

        lastPosX = mouseX;
        lastPosY = mouseY;

        // Always update the slot under the mouse.
        // This should prevent a "double click/move" when shift + left clicking on slots that have more
        // than one stack of items. (the regular slotClick() + a "drag move" from the slot that is under the mouse
        // when the left mouse button is pressed down and this code runs).
        Slot slot = AccessorUtils.getSlotAtPosition(gui, mouseX, mouseY);

        if (slot != null)
        {
            if (gui instanceof CreativeModeInventoryScreen)
            {
                boolean isPlayerInv = ((CreativeModeInventoryScreen) gui).isInventoryOpen(); // TODO 1.19.3+
                int slotNumber = isPlayerInv ? AccessorUtils.getSlotIndex(slot) : slot.index;
                slotNumberLast = slotNumber;
            }
            else
            {
                slotNumberLast = slot.index;
            }
        }
        else
        {
            slotNumberLast = -1;
        }

        return cancel;
    }

    public static void stopDragging()
    {
        activeMoveAction = MoveAction.NONE;
        DRAGGED_SLOTS.clear();
    }

    private static boolean dragMoveFromSlotAtPosition(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                      int x, int y, MoveAction action)
    {
        if (gui instanceof CreativeModeInventoryScreen)
        {
            return dragMoveFromSlotAtPositionCreative(gui, x, y, action);
        }

        Slot slot = AccessorUtils.getSlotAtPosition(gui, x, y);
        Minecraft mc = Minecraft.getInstance();
        MoveAmount amount = InputUtils.getMoveAmount(action);
        boolean flag = slot != null && isValidSlot(slot, gui, true) && slot.mayPickup(mc.player);
        //boolean cancel = flag && (amount == MoveAmount.LEAVE_ONE || amount == MoveAmount.MOVE_ONE);

        if (flag && slot.index != slotNumberLast &&
            (amount != MoveAmount.MOVE_ONE || DRAGGED_SLOTS.contains(slot.index) == false))
        {
            switch (action)
            {
                case MOVE_TO_OTHER_MOVE_ONE:
                    tryMoveSingleItemToOtherInventory(slot, gui);
                    break;

                case MOVE_TO_OTHER_LEAVE_ONE:
                    tryMoveAllButOneItemToOtherInventory(slot, gui);
                    break;

                case MOVE_TO_OTHER_STACKS:
                    shiftClickSlot(gui, slot.index);
                    break;

                case MOVE_TO_OTHER_MATCHING:
                    tryMoveStacks(slot, gui, true, true, false);
                    break;

                case DROP_ONE:
                    clickSlot(gui, slot.index, 0, ClickType.THROW);
                    break;

                case DROP_LEAVE_ONE:
                    leftClickSlot(gui, slot.index);
                    rightClickSlot(gui, slot.index);
                    dropItemsFromCursor(gui);
                    break;

                case DROP_STACKS:
                    clickSlot(gui, slot.index, 1, ClickType.THROW);
                    break;

                case MOVE_DOWN_MOVE_ONE:
                case MOVE_DOWN_LEAVE_ONE:
                case MOVE_DOWN_STACKS:
                case MOVE_DOWN_MATCHING:
                    tryMoveItemsVertically(gui, slot, false, amount);
                    break;

                case MOVE_UP_MOVE_ONE:
                case MOVE_UP_LEAVE_ONE:
                case MOVE_UP_STACKS:
                case MOVE_UP_MATCHING:
                    tryMoveItemsVertically(gui, slot, true, amount);
                    break;

                default:
            }

            DRAGGED_SLOTS.add(slot.index);
        }

        return true;
    }

    private static boolean dragMoveFromSlotAtPositionCreative(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                              int x, int y, MoveAction action)
    {
        CreativeModeInventoryScreen guiCreative = (CreativeModeInventoryScreen) gui;
        Slot slot = AccessorUtils.getSlotAtPosition(gui, (double) x, (double) y);
        boolean isPlayerInv = guiCreative.isInventoryOpen(); // TODO 1.19.3+

        // Only allow dragging from the hotbar slots
        if (slot == null || (slot.getClass() != Slot.class && isPlayerInv == false))
        {
            return false;
        }

        Minecraft mc = Minecraft.getInstance();
        MoveAmount amount = InputUtils.getMoveAmount(action);
        boolean flag = slot != null && isValidSlot(slot, gui, true) && slot.mayPickup(mc.player);
        boolean cancel = flag && (amount == MoveAmount.LEAVE_ONE || amount == MoveAmount.MOVE_ONE);
        // The player inventory tab of the creative inventory uses stupid wrapped
        // slots that all have slotNumber = 0 on the outer instance ;_;
        // However in that case we can use the slotIndex which is easy enough to get.
        int slotNumber = isPlayerInv ? AccessorUtils.getSlotIndex(slot) : slot.index;

        if (flag && slotNumber != slotNumberLast && DRAGGED_SLOTS.contains(slotNumber) == false)
        {
            switch (action)
            {
                case SCROLL_TO_OTHER_MOVE_ONE:
                case MOVE_TO_OTHER_MOVE_ONE:
                    leftClickSlot(guiCreative, slot, slotNumber);
                    rightClickSlot(guiCreative, slot, slotNumber);
                    shiftClickSlot(guiCreative, slot, slotNumber);
                    leftClickSlot(guiCreative, slot, slotNumber);

                    cancel = true;
                    break;

                case MOVE_TO_OTHER_LEAVE_ONE:
                    // Too lazy to try to duplicate the proper code for the weird creative inventory...
                    if (isPlayerInv == false)
                    {
                        leftClickSlot(guiCreative, slot, slotNumber);
                        rightClickSlot(guiCreative, slot, slotNumber);

                        // Delete the rest of the stack by placing it in the first creative "source slot"
                        Slot slotFirst = gui.getMenu().slots.get(0);
                        leftClickSlot(guiCreative, slotFirst, slotFirst.index);
                    }

                    cancel = true;
                    break;

                case SCROLL_TO_OTHER_STACKS:
                case MOVE_TO_OTHER_STACKS:
                    shiftClickSlot(gui, slot, slotNumber);
                    cancel = true;
                    break;

                case DROP_ONE:
                    clickSlot(gui, slot.index, 0, ClickType.THROW);
                    break;

                case DROP_LEAVE_ONE:
                    leftClickSlot(gui, slot.index);
                    rightClickSlot(gui, slot.index);
                    dropItemsFromCursor(gui);
                    break;

                case DROP_STACKS:
                    clickSlot(gui, slot.index, 1, ClickType.THROW);
                    cancel = true;
                    break;

                case MOVE_DOWN_MOVE_ONE:
                case MOVE_DOWN_LEAVE_ONE:
                case MOVE_DOWN_STACKS:
                    tryMoveItemsVertically(gui, slot, false, amount);
                    cancel = true;
                    break;

                case MOVE_UP_MOVE_ONE:
                case MOVE_UP_LEAVE_ONE:
                case MOVE_UP_STACKS:
                    tryMoveItemsVertically(gui, slot, true, amount);
                    cancel = true;
                    break;

                default:
            }

            DRAGGED_SLOTS.add(slotNumber);
        }

        return cancel;
    }

    public static void dropStacks(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                  ItemStack stackReference,
                                  Slot slotReference,
                                  boolean sameInventory)
    {
        if (slotReference != null && isStackEmpty(stackReference) == false)
        {
            AbstractContainerMenu container = gui.getMenu();
            stackReference = stackReference.copy();

            for (Slot slot : container.slots)
            {
                // If this slot is in the same inventory that the items were picked up to the cursor from
                // and the stack is identical to the one in the cursor, then this stack will get dropped.
                if (areSlotsInSameInventory(slot, slotReference) == sameInventory &&
                    areStacksEqual(slot.getItem(), stackReference))
                {
                    // Drop the stack
                    dropStack(gui, slot.index);
                }
            }
        }
    }

    public static void dropAllMatchingStacks(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                             ItemStack stackReference)
    {
        if (isStackEmpty(stackReference) == false)
        {
            AbstractContainerMenu container = gui.getMenu();
            stackReference = stackReference.copy();

            for (Slot slot : container.slots)
            {
                if (areStacksEqual(slot.getItem(), stackReference))
                {
                    // Drop the stack
                    dropStack(gui, slot.index);
                }
            }
        }
    }

    public static boolean shiftDropItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        ItemStack stackReference = gui.getMenu().getCarried();

        if (isStackEmpty(stackReference) == false && sourceSlot != null)
        {
            stackReference = stackReference.copy();

            // First drop the existing stack from the cursor
            dropItemsFromCursor(gui);

            dropStacks(gui, stackReference, sourceSlot.get(), true);
            return true;
        }

        return false;
    }

    public static boolean shiftPlaceItems(Slot slot, AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        // Left click to place the items from the cursor to the slot
        leftClickSlot(gui, slot.index);

        // Ugly fix to prevent accidentally drag-moving the stack from the slot that it was just placed into...
        DRAGGED_SLOTS.add(slot.index);

        tryMoveStacks(slot, gui, true, false, false);

        return true;
    }

    /**
     * Store a reference to the slot when a slot is left or right clicked on.
     * The slot is then later used to determine which inventory an ItemStack was
     * picked up from, if the stack from the cursor is dropped while holding shift.
     */
    public static void storeSourceSlotCandidate(Slot slot, AbstractContainerScreen<?> gui)
    {
        // Left or right mouse button was pressed
        if (slot != null)
        {
            ItemStack stackCursor = gui.getMenu().getCarried();
            ItemStack stack = EMPTY_STACK;

            if (isStackEmpty(stackCursor) == false)
            {
                // Do a cheap copy without NBT data
                stack = new ItemStack(stackCursor.getItem(), getStackSize(stackCursor));
            }

            // Store the candidate
            // NOTE: This method is called BEFORE the stack has been picked up to the cursor!
            // Thus we can't check that there is an item already in the cursor, and that's why this is just a "candidate"
            sourceSlotCandidate = new WeakReference<>(slot);
            stackInCursorLast = stack;
        }
    }

    /**
     * Check if the (previous) mouse event resulted in picking up a new ItemStack to the cursor
     */
    public static void checkForItemPickup(AbstractContainerScreen<?> gui)
    {
        ItemStack stackCursor = gui.getMenu().getCarried();

        // Picked up or swapped items to the cursor, grab a reference to the slot that the items came from
        // Note that we are only checking the item here!
        if (isStackEmpty(stackCursor) == false && ItemStack.isSameItem(stackCursor, stackInCursorLast) == false && sourceSlotCandidate != null)
        {
            sourceSlot = new WeakReference<>(sourceSlotCandidate.get());
        }
    }

    private static boolean tryMoveItemsVillager(MerchantScreen gui,
                                                Slot slot,
                                                boolean moveToOtherInventory,
                                                boolean fullStacks)
    {
        if (fullStacks)
        {
            // Try to fill the merchant's buy slots from the player inventory
            if (moveToOtherInventory == false)
            {
                tryMoveItemsToMerchantBuySlots(gui, true);
            }
            // Move items from sell slot to player inventory
            else if (slot.hasItem())
            {
                tryMoveStacks(slot, gui, true, true, true);
            }
            // Scrolling over an empty output slot, clear the buy slots
            else
            {
                tryMoveStacks(slot, gui, false, true, false);
            }
        }
        else
        {
            // Scrolling items from player inventory into merchant buy slots
            if (moveToOtherInventory == false)
            {
                tryMoveItemsToMerchantBuySlots(gui, false);
            }
            // Scrolling items from this slot/inventory into the other inventory
            else if (slot.hasItem())
            {
                moveOneSetOfItemsFromSlotToPlayerInventory(gui, slot);
            }
        }

        return false;
    }

    public static void villagerClearTradeInputSlots()
    {
        if (GuiUtils.getCurrentScreen() instanceof MerchantScreen merchantGui)
        {
            Slot slot = merchantGui.getMenu().getSlot(0);

            if (slot.hasItem())
            {
                shiftClickSlot(merchantGui, slot.index);
            }

            slot = merchantGui.getMenu().getSlot(1);

            if (slot.hasItem())
            {
                shiftClickSlot(merchantGui, slot.index);
            }
        }
    }

    public static void villagerTradeEverythingPossibleWithTrade(int visibleIndex)
    {
        if (GuiUtils.getCurrentScreen() instanceof MerchantScreen merchantGui)
        {
            MerchantMenu handler = merchantGui.getMenu();

            try
            {
                if (handler.getOffers().isEmpty()) return;
            }
            catch (Exception ignored)
            {
                return;
            }

            Slot slot = handler.getSlot(2);
            ItemStack sellItem = handler.getOffers().get(visibleIndex).getResult().copy();

            while (true)
            {
                VillagerUtils.switchToTradeByVisibleIndex(visibleIndex);
                //tryMoveItemsToMerchantBuySlots(merchantGui, true);

                // Not a valid recipe
                //if (slot.hasStack() == false)
                if (areStacksEqual(sellItem, slot.getItem()) == false)
                {
                    break;
                }

                shiftClickSlot(merchantGui, slot.index);

                // No room in player inventory
                if (slot.hasItem())
                {
                    break;
                }
            }

            villagerClearTradeInputSlots();
        }
    }

    public static boolean villagerTradeEverythingPossibleWithAllFavoritedTrades()
    {
        Screen screen = GuiUtils.getCurrentScreen();

        if (screen instanceof MerchantScreen)
        {
            MerchantMenu handler = ((MerchantScreen) screen).getMenu();
            IntArrayList favorites = VillagerDataStorage.getInstance().getFavoritesForCurrentVillager(handler).favorites();

            for (int index = 0; index < favorites.size(); ++index)
            {
                VillagerUtils.switchToTradeByVisibleIndex(index);
                villagerTradeEverythingPossibleWithTrade(index);
            }

            villagerClearTradeInputSlots();

            return true;
        }

        return false;
    }

    private static boolean tryMoveSingleItemToOtherInventory(Slot slot,
                                                             AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        ItemStack stackOrig = slot.getItem();
        AbstractContainerMenu container = gui.getMenu();
        Minecraft mc = Minecraft.getInstance();

        if (isStackEmpty(gui.getMenu().getCarried()) == false || slot.mayPickup(mc.player) == false ||
            (getStackSize(stackOrig) > 1 && slot.mayPlace(stackOrig) == false))
        {
            return false;
        }

        // Can take all the items to the cursor at once, use a shift-click method to move one item from the slot
        if (getStackSize(stackOrig) <= stackOrig.getMaxStackSize())
        {
            return clickSlotsToMoveSingleItemByShiftClick(gui, slot.index);
        }

        ItemStack stack = stackOrig.copy();
        setStackSize(stack, 1);

        ItemStack[] originalStacks = getOriginalStacks(container);

        // Try to move the temporary single-item stack via the shift-click handler method
        slot.set(stack);
        container.quickMoveStack(mc.player, slot.index);

        // Successfully moved the item somewhere, now we want to check where it went
        if (slot.hasItem() == false)
        {
            int targetSlot = getTargetSlot(container, originalStacks);

            // Found where the item went
            if (targetSlot >= 0)
            {
                // Remove the dummy item from the target slot (on the client side)
                container.slots.get(targetSlot).remove(1);

                // Restore the original stack to the slot under the cursor (on the client side)
                restoreOriginalStacks(container, originalStacks);

                // Do the slot clicks to actually move the items (on the server side)
                return clickSlotsToMoveSingleItem(gui, slot.index, targetSlot);
            }
        }

        // Restore the original stack to the slot under the cursor (on the client side)
        slot.set(stackOrig);

        return false;
    }

    private static boolean tryMoveAllButOneItemToOtherInventory(Slot slot,
                                                                AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        ItemStack stackOrig = slot.getItem().copy();

        if (getStackSize(stackOrig) == 1 || getStackSize(stackOrig) > stackOrig.getMaxStackSize() ||
            slot.mayPickup(player) == false || slot.mayPlace(stackOrig) == false)
        {
            return true;
        }

        // Take half of the items from the original slot to the cursor
        rightClickSlot(gui, slot.index);

        ItemStack stackInCursor = gui.getMenu().getCarried();
        if (isStackEmpty(stackInCursor))
        {
            return false;
        }

        int stackInCursorSizeOrig = getStackSize(stackInCursor);
        int tempSlotNum = -1;

        // Find some other slot where to store one of the items temporarily
        for (Slot slotTmp : gui.getMenu().slots)
        {
            if (slotTmp.index != slot.index &&
                areSlotsInSameInventory(slotTmp, slot, true) &&
                slotTmp.mayPlace(stackInCursor) &&
                slotTmp.mayPickup(player))
            {
                ItemStack stackInSlot = slotTmp.getItem();

                if (isStackEmpty(stackInSlot) || areStacksEqual(stackInSlot, stackInCursor))
                {
                    // Try to put one item into the temporary slot
                    rightClickSlot(gui, slotTmp.index);

                    stackInCursor = gui.getMenu().getCarried();

                    // Successfully stored one item
                    if (isStackEmpty(stackInCursor) || getStackSize(stackInCursor) < stackInCursorSizeOrig)
                    {
                        tempSlotNum = slotTmp.index;
                        break;
                    }
                }
            }
        }

        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            // Return the rest of the items into the original slot
            leftClickSlot(gui, slot.index);
        }

        // Successfully stored one item in a temporary slot
        if (tempSlotNum != -1)
        {
            // Shift click the stack from the original slot
            shiftClickSlot(gui, slot.index);

            // Take half a stack from the temporary slot
            rightClickSlot(gui, tempSlotNum);

            // Return one item into the original slot
            rightClickSlot(gui, slot.index);

            // Return the rest of the items to the temporary slot, if any
            if (isStackEmpty(gui.getMenu().getCarried()) == false)
            {
                leftClickSlot(gui, tempSlotNum);
            }

            return true;
        }
        // No temporary slot found, try to move the stack manually
        else
        {
            boolean treatHotbarAsDifferent = gui.getClass() == InventoryScreen.class;
            IntArrayList slots = getSlotNumbersOfEmptySlots(gui.getMenu(), slot, false, treatHotbarAsDifferent, false);

            if (slots.isEmpty())
            {
                slots = getSlotNumbersOfMatchingStacks(gui.getMenu(), slot, false, slot.getItem(), true, treatHotbarAsDifferent, false);
            }

            if (slots.isEmpty() == false)
            {
                // Take the stack
                leftClickSlot(gui, slot.index);

                // Return one item
                rightClickSlot(gui, slot.index);

                // Try to place the stack in the cursor to any valid empty or matching slots in a different inventory
                for (int slotNum : slots)
                {
                    Slot slotTmp = gui.getMenu().getSlot(slotNum);
                    stackInCursor = gui.getMenu().getCarried();

                    if (isStackEmpty(stackInCursor))
                    {
                        return true;
                    }

                    if (slotTmp.mayPlace(stackInCursor))
                    {
                        leftClickSlot(gui, slotNum);
                    }
                }

                // Items left, return them
                if (isStackEmpty(stackInCursor) == false)
                {
                    leftClickSlot(gui, slot.index);
                }
            }
        }

        return false;
    }

    private static boolean tryMoveSingleItemToThisInventory(Slot slot,
                                                            AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        AbstractContainerMenu container = gui.getMenu();
        ItemStack stackOrig = slot.getItem();
        Minecraft mc = Minecraft.getInstance();

        if (slot.mayPlace(stackOrig) == false)
        {
            return false;
        }

        for (int slotNum = container.slots.size() - 1; slotNum >= 0; slotNum--)
        {
            Slot slotTmp = container.slots.get(slotNum);
            ItemStack stackTmp = slotTmp.getItem();

            if (areSlotsInSameInventory(slotTmp, slot) == false &&
                isStackEmpty(stackTmp) == false && slotTmp.mayPickup(mc.player) &&
                (getStackSize(stackTmp) == 1 || slotTmp.mayPlace(stackTmp)))
            {
                if (areStacksEqual(stackTmp, stackOrig))
                {
                    return clickSlotsToMoveSingleItem(gui, slotTmp.index, slot.index);
                }
            }
        }

        // If we weren't able to move any items from another inventory, then try to move items
        // within the same inventory (mostly between the hotbar and the player inventory)
        /*
        for (Slot slotTmp : container.slots)
        {
            ItemStack stackTmp = slotTmp.getStack();

            if (slotTmp.id != slot.id &&
                isStackEmpty(stackTmp) == false && slotTmp.canTakeItems(gui.mc.player) &&
                (getStackSize(stackTmp) == 1 || slotTmp.canInsert(stackTmp)))
            {
                if (areStacksEqual(stackTmp, stackOrig))
                {
                    return this.clickSlotsToMoveSingleItem(gui, slotTmp.id, slot.id);
                }
            }
        }
        */

        return false;
    }

    public static void tryMoveStacks(Slot slot,
                                     AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                     boolean matchingOnly,
                                     boolean toOtherInventory,
                                     boolean firstOnly)
    {
        tryMoveStacks(slot.getItem(), slot, gui, matchingOnly, toOtherInventory, firstOnly);
    }

    private static void tryMoveStacks(ItemStack stackReference,
                                      Slot slot,
                                      AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                      boolean matchingOnly,
                                      boolean toOtherInventory,
                                      boolean firstOnly)
    {
        AbstractContainerMenu container = gui.getMenu();
        final int maxSlot = container.slots.size() - 1;

        for (int i = maxSlot; i >= 0; i--)
        {
            Slot slotTmp = container.slots.get(i);

            if (slotTmp.index != slot.index &&
                areSlotsInSameInventory(slotTmp, slot) == toOtherInventory && slotTmp.hasItem() &&
                (matchingOnly == false || areStacksEqual(stackReference, slotTmp.getItem())))
            {
                boolean success = shiftClickSlotWithCheck(gui, slotTmp.index);

                // Failed to shift-click items, try a manual method
                if (success == false && Configs.Toggles.ITEM_MOVING_FALLBACK.getBooleanValue())
                {
                    clickSlotsToMoveItemsFromSlot(slotTmp, gui, toOtherInventory);
                }

                if (firstOnly)
                {
                    return;
                }
            }
        }

        // If moving to the other inventory, then move the hovered slot's stack last
        if (toOtherInventory &&
            shiftClickSlotWithCheck(gui, slot.index) == false &&
            Configs.Toggles.ITEM_MOVING_FALLBACK.getBooleanValue())
        {
            clickSlotsToMoveItemsFromSlot(slot, gui, toOtherInventory);
        }
    }

    private static void tryMoveItemsToMerchantBuySlots(MerchantScreen gui,
                                                       boolean fillStacks)
    {
        MerchantOffers list = gui.getMenu().getOffers();
        int index = AccessorUtils.getSelectedMerchantRecipe(gui);

        if (list == null || list.size() <= index)
        {
            return;
        }

        MerchantOffer recipe = list.get(index);

        if (recipe == null)
        {
            return;
        }

        ItemStack buy1 = recipe.getCostA();
        ItemStack buy2 = recipe.getCostB();

        if (isStackEmpty(buy1) == false)
        {
            fillBuySlot(gui, 0, buy1, fillStacks);
        }

        if (isStackEmpty(buy2) == false)
        {
            fillBuySlot(gui, 1, buy2, fillStacks);
        }
    }

    private static void fillBuySlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                    int slotNum,
                                    ItemStack buyStack,
                                    boolean fillStacks)
    {
        Slot slot = gui.getMenu().getSlot(slotNum);
        ItemStack existingStack = slot.getItem();
        Minecraft mc = Minecraft.getInstance();

        // If there are items not matching the merchant recipe, move them out first
        if (isStackEmpty(existingStack) == false && areStacksEqual(buyStack, existingStack) == false)
        {
            shiftClickSlot(gui, slotNum);
        }

        existingStack = slot.getItem();

        if (isStackEmpty(existingStack) || areStacksEqual(buyStack, existingStack))
        {
            moveItemsFromInventory(gui, slotNum, mc.player.getInventory(), buyStack, fillStacks);
        }
    }

    public static void handleRecipeClick(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                         Minecraft mc,
                                         RecipeStorage recipes,
                                         int hoveredRecipeId,
                                         boolean isLeftClick,
                                         boolean isRightClick,
                                         boolean isPickBlock,
                                         boolean isShiftDown)
    {
        if (isLeftClick || isRightClick)
        {
            boolean changed = recipes.getSelection() != hoveredRecipeId;
            recipes.changeSelectedRecipe(hoveredRecipeId);

            if (changed)
            {
                InventoryUtils.clearFirstCraftingGridOfItems(recipes.getSelectedRecipe(), gui, false);
            }
            else
            {
                InventoryUtils.tryMoveItemsToFirstCraftingGrid(recipes.getRecipe(hoveredRecipeId), gui, isShiftDown);
            }

            // Right click: Also craft the items
            if (isRightClick)
            {
                Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);
                boolean dropKeyDown = mc.options.keyDrop.isDown(); // FIXME 1.14

                if (outputSlot != null)
                {
                    if (dropKeyDown)
                    {
                        if (isShiftDown)
                        {
                            if (Configs.Generic.CARPET_CTRL_Q_CRAFTING.getBooleanValue())
                            {
                                InventoryUtils.dropStack(gui, outputSlot.index);
                            }
                            else
                            {
                                InventoryUtils.dropStacksUntilEmpty(gui, outputSlot.index);
                            }
                        }
                        else
                        {
                            InventoryUtils.dropItem(gui, outputSlot.index);
                        }
                    }
                    else
                    {
                        if (isShiftDown)
                        {
                            InventoryUtils.shiftClickSlot(gui, outputSlot.index);
                        }
                        else
                        {
                            InventoryUtils.moveOneSetOfItemsFromSlotToPlayerInventory(gui, outputSlot);
                        }
                    }
                }
            }
        }
        else if (isPickBlock)
        {
            InventoryUtils.clearFirstCraftingGridOfAllItems(gui);
        }
    }

    public static void tryMoveItemsToFirstCraftingGrid(RecipePattern recipe,
                                                       AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                       boolean fillStacks)
    {
        Slot craftingOutputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

        if (craftingOutputSlot != null)
        {
            tryMoveItemsToCraftingGridSlots(recipe, craftingOutputSlot, gui, fillStacks);
        }
    }

    public static void loadRecipeItemsToGridForOutputSlotUnderMouse(RecipePattern recipe,
                                                                    AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);
        loadRecipeItemsToGridForOutputSlot(recipe, gui, slot);
    }

    private static void loadRecipeItemsToGridForOutputSlot(RecipePattern recipe,
                                                           AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                           Slot outputSlot)
    {
        if (isCraftingSlot(gui, outputSlot) && isStackEmpty(recipe.getResult()) == false)
        {
            tryMoveItemsToCraftingGridSlots(recipe, outputSlot, gui, false);
        }
    }

    private static boolean tryMoveItemsCrafting(RecipeStorage recipes,
                                                Slot slot,
                                                AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                boolean moveToOtherInventory,
                                                boolean moveStacks,
                                                boolean moveEverything)
    {
        RecipePattern recipe = recipes.getSelectedRecipe();
        ItemStack stackRecipeOutput = recipe.getResult();

        // Try to craft items
        if (moveToOtherInventory)
        {
            // Items in the output slot
            if (slot.hasItem())
            {
                // The output item matches the current recipe
                if (areStacksEqual(slot.getItem(), stackRecipeOutput))
                {
                    if (moveEverything)
                    {
                        craftAsManyItemsAsPossible(recipe, slot, gui);
                    }
                    else if (moveStacks)
                    {
                        shiftClickSlot(gui, slot.index);
                    }
                    else
                    {
                        moveOneSetOfItemsFromSlotToPlayerInventory(gui, slot);
                    }
                }
            }
            // Scrolling over an empty output slot, clear the grid
            else
            {
                clearCraftingGridOfAllItems(gui, CraftingHandler.getCraftingGridSlots(gui, slot));
            }
        }
        // Try to move items to the grid
        else if (moveToOtherInventory == false && isStackEmpty(stackRecipeOutput) == false)
        {
            tryMoveItemsToCraftingGridSlots(recipe, slot, gui, moveStacks);
        }

        return false;
    }

    private static void craftAsManyItemsAsPossible(RecipePattern recipe,
                                                   Slot slot,
                                                   AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        ItemStack result = recipe.getResult();
        int failSafe = 1024;

        while (failSafe > 0 && slot.hasItem() && areStacksEqual(slot.getItem(), result))
        {
            shiftClickSlot(gui, slot.index);

            // Ran out of some or all ingredients for the recipe
            if (slot.hasItem() == false || areStacksEqual(slot.getItem(), result) == false)
            {
                tryMoveItemsToCraftingGridSlots(recipe, slot, gui, true);
            }
            // No change in the result slot after shift clicking, let's assume the craft failed and stop here
            else
            {
                break;
            }

            failSafe--;
        }
    }

    public static void clearFirstCraftingGridOfItems(RecipePattern recipe,
                                                     AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                     boolean clearNonMatchingOnly)
    {
        Slot craftingOutputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

        if (craftingOutputSlot != null)
        {
            SlotRange range = CraftingHandler.getCraftingGridSlots(gui, craftingOutputSlot);
            clearCraftingGridOfItems(recipe, gui, range, clearNonMatchingOnly);
        }
    }

    public static void clearFirstCraftingGridOfAllItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot craftingOutputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

        if (craftingOutputSlot != null)
        {
            SlotRange range = CraftingHandler.getCraftingGridSlots(gui, craftingOutputSlot);
            clearCraftingGridOfAllItems(gui, range);
        }
    }

    private static boolean clearCraftingGridOfItems(RecipePattern recipe,
                                                    AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                    SlotRange range,
                                                    boolean clearNonMatchingOnly)
    {
        final int invSlots = gui.getMenu().slots.size();
        final int rangeSlots = range.getSlotCount();
        final int recipeSize = recipe.getRecipeLength();
        final int slotCount = Math.min(rangeSlots, recipeSize);

        for (int i = 0, slotNum = range.getFirst(); i < slotCount && slotNum < invSlots; i++, slotNum++)
        {
            Slot slotTmp = gui.getMenu().getSlot(slotNum);

            if (slotTmp != null && slotTmp.hasItem() &&
                (clearNonMatchingOnly == false || areStacksEqual(recipe.getRecipeItems()[i], slotTmp.getItem()) == false))
            {
                shiftClickSlot(gui, slotNum);

                // Failed to clear the slot
                if (slotTmp.hasItem())
                {
                    dropStack(gui, slotNum);
                }
            }
        }

        return true;
    }

    private static boolean clearCraftingGridOfAllItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui, SlotRange range)
    {
        final int invSlots = gui.getMenu().slots.size();
        final int rangeSlots = range.getSlotCount();
        boolean clearedAll = true;

        for (int i = 0, slotNum = range.getFirst(); i < rangeSlots && slotNum < invSlots; i++, slotNum++)
        {
            Slot slotTmp = gui.getMenu().getSlot(slotNum);

            if (slotTmp != null && slotTmp.hasItem())
            {
                shiftClickSlot(gui, slotNum);

                // Failed to clear the slot
                if (slotTmp.hasItem())
                {
                    clearedAll = false;
                }
            }
        }

        return clearedAll;
    }

    private static boolean tryMoveItemsToCraftingGridSlots(RecipePattern recipe,
                                                           Slot slot,
                                                           AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                           boolean fillStacks)
    {
        AbstractContainerMenu container = gui.getMenu();
        int numSlots = container.slots.size();
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

        // Check that the slot range is valid and that the recipe can fit into this type of crafting grid
        if (range != null && range.getLast() < numSlots && recipe.getRecipeLength() <= range.getSlotCount())
        {
            // Clear non-matching items from the grid first
            if (clearCraftingGridOfItems(recipe, gui, range, true) == false)
            {
                return false;
            }

            // This slot is used to check that we get items from a DIFFERENT inventory than where this slot is in
            Slot slotGridFirst = container.getSlot(range.getFirst());
            Map<ItemType, IntArrayList> ingredientSlots = ItemType.getSlotsPerItem(recipe.getRecipeItems());

            for (Map.Entry<ItemType, IntArrayList> entry : ingredientSlots.entrySet())
            {
                ItemStack ingredientReference = entry.getKey().stack();
                IntArrayList recipeSlots = entry.getValue();
                IntArrayList targetSlots = new IntArrayList();

                // Get the actual target slot numbers based on the grid's start and the relative positions inside the grid
                for (int s : recipeSlots)
                {
                    targetSlots.add(s + range.getFirst());
                }

                if (fillStacks)
                {
                    fillCraftingGrid(gui, slotGridFirst, ingredientReference, targetSlots);
                }
                else
                {
                    moveOneRecipeItemIntoCraftingGrid(gui, slotGridFirst, ingredientReference, targetSlots);
                }
            }
        }

        return false;
    }

    private static void fillCraftingGrid(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                         Slot slotGridFirst,
                                         ItemStack ingredientReference,
                                         IntArrayList targetSlots)
    {
        AbstractContainerMenu container = gui.getMenu();
        int slotNum;
        int slotReturn = -1;
        int sizeOrig;

        if (isStackEmpty(ingredientReference))
        {
            return;
        }

        while (true)
        {
            slotNum = getSlotNumberOfLargestMatchingStackFromDifferentInventory(container, slotGridFirst, ingredientReference);

            // Didn't find ingredient items
            if (slotNum < 0)
            {
                break;
            }

            if (slotReturn == -1)
            {
                slotReturn = slotNum;
            }

            // Pick up the ingredient stack from the found slot
            leftClickSlot(gui, slotNum);

            ItemStack stackCursor = gui.getMenu().getCarried();

            // Successfully picked up ingredient items
            if (areStacksEqual(ingredientReference, stackCursor))
            {
                sizeOrig = getStackSize(stackCursor);
                dragSplitItemsIntoSlots(gui, targetSlots);
                stackCursor = gui.getMenu().getCarried();

                // Items left in cursor
                if (isStackEmpty(stackCursor) == false)
                {
                    // Didn't manage to move any items anymore
                    if (getStackSize(stackCursor) >= sizeOrig)
                    {
                        break;
                    }

                    // Collect all the remaining items into the first found slot, as long as possible
                    leftClickSlot(gui, slotReturn);

                    // All of them didn't fit into the first slot anymore, switch into the current source slot
                    if (isStackEmpty(gui.getMenu().getCarried()) == false)
                    {
                        slotReturn = slotNum;
                        leftClickSlot(gui, slotReturn);
                    }
                }
            }
            // Failed to pick up the stack, break to avoid infinite loops
            // TODO: we could also "blacklist" this slot and try to continue...?
            else
            {
                break;
            }

            // Somehow items were left in the cursor, break here
            if (isStackEmpty(gui.getMenu().getCarried()) == false)
            {
                break;
            }
        }

        // Return the rest of the items to the original slot
        if (slotNum >= 0 && isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            leftClickSlot(gui, slotNum);
        }
    }

    public static void rightClickCraftOneStack(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);
        ItemStack stackCursor = gui.getMenu().getCarried();

        if (slot == null || slot.hasItem() == false ||
            (isStackEmpty(stackCursor) == false) && areStacksEqual(slot.getItem(), stackCursor) == false)
        {
            return;
        }

        int sizeLast = 0;

        while (true)
        {
            rightClickSlot(gui, slot.index);
            stackCursor = gui.getMenu().getCarried();

            // Failed to craft items, or the stack became full, or ran out of ingredients
            if (isStackEmpty(stackCursor) || getStackSize(stackCursor) <= sizeLast ||
                getStackSize(stackCursor) >= stackCursor.getMaxStackSize() ||
                areStacksEqual(slot.getItem(), stackCursor) == false)
            {
                break;
            }

            sizeLast = getStackSize(stackCursor);
        }
    }

    public static void craftEverythingPossibleWithCurrentRecipe(RecipePattern recipe,
                                                                AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot slot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

        if (slot != null && isStackEmpty(recipe.getResult()) == false)
        {
            SlotRange range = CraftingHandler.getCraftingGridSlots(gui, slot);

            if (range != null)
            {
                // Clear all items from the grid first, to avoid unbalanced stacks
                if (clearCraftingGridOfItems(recipe, gui, range, false) == false)
                {
                    return;
                }

                tryMoveItemsToCraftingGridSlots(recipe, slot, gui, true);

                if (slot.hasItem())
                {
                    craftAsManyItemsAsPossible(recipe, slot, gui);
                }
            }
        }
    }

    public static void moveAllCraftingResultsToOtherInventory(RecipePattern recipe,
                                                              AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        if (isStackEmpty(recipe.getResult()) == false)
        {
            Slot slot = null;
            ItemStack stackResult = recipe.getResult().copy();

            for (Slot slotTmp : gui.getMenu().slots)
            {
                // This slot is likely in the player inventory, as there is another inventory above
                if (areStacksEqual(slotTmp.getItem(), stackResult) &&
                    inventoryExistsAbove(slotTmp, gui.getMenu()))
                {
                    slot = slotTmp;
                    break;
                }
            }

            if (slot != null)
            {
                // Get a list of slots with matching items, which are in the same inventory
                // as the slot that is assumed to be in the player inventory.
                IntArrayList slots = getSlotNumbersOfMatchingStacks(gui.getMenu(), slot, true, stackResult, false, false, false);

                for (int slotNum : slots)
                {
                    shiftClickSlot(gui, slotNum);
                }
            }
        }
    }

    public static void throwAllCraftingResultsToGround(RecipePattern recipe,
                                                       AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot slot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

        if (slot != null && isStackEmpty(recipe.getResult()) == false)
        {
            dropStacks(gui, recipe.getResult(), slot, false);
        }
    }

    public static void throwAllNonRecipeItemsToGround(RecipePattern recipe,
                                                      AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

        if (outputSlot != null && isStackEmpty(recipe.getResult()) == false)
        {
            SlotRange range = CraftingHandler.getCraftingGridSlots(gui, outputSlot);
            ItemStack[] recipeItems = recipe.getRecipeItems();
            final int invSlots = gui.getMenu().slots.size();
            final int rangeSlots = Math.min(range.getSlotCount(), recipeItems.length);

            for (int i = 0, slotNum = range.getFirst(); i < rangeSlots && slotNum < invSlots; i++, slotNum++)
            {
                Slot slotTmp = gui.getMenu().getSlot(slotNum);
                ItemStack stack = slotTmp.getItem();

                if (stack.isEmpty() == false && areStacksEqual(stack, recipeItems[i]) == false)
                {
                    dropAllMatchingStacks(gui, stack);
                }
            }
        }
    }

    public static void setCraftingGridContentsUsingSwaps(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                         Inventory inv,
                                                         RecipePattern recipe,
                                                         Slot outputSlot)
    {
        SlotRange range = CraftingHandler.getCraftingGridSlots(gui, outputSlot);

        if (range != null && isStackEmpty(recipe.getResult()) == false)
        {
            ItemStack[] recipeItems = recipe.getRecipeItems();
            final int invSlots = gui.getMenu().slots.size();
            final int rangeSlots = Math.min(range.getSlotCount(), recipeItems.length);
            IntArrayList toRemove = new IntArrayList();
            boolean movedSomething = false;

            setInhibitCraftingOutputUpdate(true);

            for (int i = 0, slotNum = range.getFirst(); i < rangeSlots && slotNum < invSlots; i++, slotNum++)
            {
                Slot craftingTableSlot = gui.getMenu().getSlot(slotNum);
                ItemStack recipeStack = recipeItems[i];
                ItemStack slotStack = craftingTableSlot.getItem();

                if (areStacksEqual(recipeStack, slotStack) == false)
                {
                    if (recipeStack.isEmpty())
                    {
                        toRemove.add(slotNum);
                    }
                    else
                    {
                        int index = getSlotNumberOfLargestMatchingStackFromDifferentInventory(gui.getMenu(), craftingTableSlot, recipeStack);

                        if (index >= 0)
                        {
                            Slot ingredientSlot = gui.getMenu().getSlot(index);

                            if (ingredientSlot.container instanceof Inventory && ingredientSlot.getContainerSlot() < 9)
                            {
                                // hotbar
                                clickSlot(gui, slotNum, ingredientSlot.getContainerSlot(), ClickType.SWAP);
                            }
                            else
                            {
                                swapSlots(gui, slotNum, index);
                            }
                            movedSomething = true;
                        }
                    }
                }
            }

            movedSomething |= !toRemove.isEmpty();

            for (int slotNum : toRemove)
            {
                shiftClickSlot(gui, slotNum);

                if (isStackEmpty(gui.getMenu().getSlot(slotNum).getItem()) == false)
                {
                    dropStack(gui, slotNum);
                }
            }

            setInhibitCraftingOutputUpdate(false);

            if (movedSomething)
            {
                updateCraftingOutputSlot(outputSlot);
            }
        }
    }

    private static int putSingleItemIntoSlots(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                              IntArrayList targetSlots,
                                              int startIndex)
    {
        ItemStack stackInCursor = gui.getMenu().getCarried();

        if (isStackEmpty(stackInCursor))
        {
            return 0;
        }

        int numSlots = gui.getMenu().slots.size();
        int numItems = getStackSize(stackInCursor);
        int loops = Math.min(numItems, targetSlots.size() - startIndex);
        int count = 0;

        for (int i = 0; i < loops; i++)
        {
            int slotNum = targetSlots.getInt(startIndex + i);

            if (slotNum >= numSlots)
            {
                break;
            }

            rightClickSlot(gui, slotNum);
            count++;
        }

        return count;
    }

    public static void moveOneSetOfItemsFromSlotToPlayerInventory(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                                  Slot slot)
    {
        leftClickSlot(gui, slot.index);

        ItemStack stackCursor = gui.getMenu().getCarried();

        if (isStackEmpty(stackCursor) == false)
        {
            IntArrayList slots = getSlotNumbersOfMatchingStacks(gui.getMenu(), slot, false, stackCursor, true, true, false);

            if (moveItemFromCursorToSlots(gui, slots) == false)
            {
                slots = getSlotNumbersOfEmptySlotsInPlayerInventory(gui.getMenu(), false);
                moveItemFromCursorToSlots(gui, slots);
            }
        }
    }

    private static void moveOneRecipeItemIntoCraftingGrid(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                          Slot slotGridFirst,
                                                          ItemStack ingredientReference,
                                                          IntArrayList targetSlots)
    {
        AbstractContainerMenu container = gui.getMenu();
        int index = 0;
        int slotNum = -1;
        int slotCount = targetSlots.size();

        while (index < slotCount)
        {
            slotNum = getSlotNumberOfSmallestStackFromDifferentInventory(container, slotGridFirst, ingredientReference, slotCount);

            // Didn't find ingredient items
            if (slotNum < 0)
            {
                break;
            }

            // Pick up the ingredient stack from the found slot
            leftClickSlot(gui, slotNum);

            // Successfully picked up ingredient items
            if (areStacksEqual(ingredientReference, gui.getMenu().getCarried()))
            {
                int filled = putSingleItemIntoSlots(gui, targetSlots, index);
                index += filled;

                if (filled < 1)
                {
                    break;
                }
            }
            // Failed to pick up the stack, break to avoid infinite loops
            // TODO: we could also "blacklist" this slot and try to continue...?
            else
            {
                break;
            }
        }

        // Return the rest of the items to the original slot
        if (slotNum >= 0 && isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            leftClickSlot(gui, slotNum);
        }
    }

    private static boolean moveItemFromCursorToSlots(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                     IntArrayList slotNumbers)
    {
        for (int slotNum : slotNumbers)
        {
            leftClickSlot(gui, slotNum);

            if (isStackEmpty(gui.getMenu().getCarried()))
            {
                return true;
            }
        }

        return false;
    }

    private static void moveItemsFromInventory(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                               int slotTo,
                                               Container invSrc,
                                               ItemStack stackTemplate,
                                               boolean fillStacks)
    {
        AbstractContainerMenu container = gui.getMenu();

        for (Slot slot : container.slots)
        {
            if (slot == null)
            {
                continue;
            }

            if (slot.container == invSrc && areStacksEqual(stackTemplate, slot.getItem()))
            {
                if (fillStacks)
                {
                    if (clickSlotsToMoveItems(gui, slot.index, slotTo) == false)
                    {
                        break;
                    }
                }
                else
                {
                    clickSlotsToMoveSingleItem(gui, slot.index, slotTo);
                    break;
                }
            }
        }
    }

    private static int getSlotNumberOfLargestMatchingStackFromDifferentInventory(AbstractContainerMenu container,
                                                                                 Slot slotReference,
                                                                                 ItemStack stackReference)
    {
        int slotNum = -1;
        int largest = 0;

        for (Slot slot : container.slots)
        {
            if (areSlotsInSameInventory(slot, slotReference) == false && slot.hasItem() &&
                areStacksEqual(stackReference, slot.getItem()))
            {
                int stackSize = getStackSize(slot.getItem());

                if (stackSize > largest)
                {
                    slotNum = slot.index;
                    largest = stackSize;
                }
            }
        }

        return slotNum;
    }

    /**
     * Returns the slot number of the slot that has the smallest stackSize that is still equal to or larger
     * than idealSize. The slot must also NOT be in the same inventory as slotReference.
     * If an adequately large stack is not found, then the largest one is selected.
     */
    private static int getSlotNumberOfSmallestStackFromDifferentInventory(AbstractContainerMenu container,
                                                                          Slot slotReference,
                                                                          ItemStack stackReference,
                                                                          int idealSize)
    {
        int slotNumSmallest = -1;
        int slotNumLargest = -1;
        int smallest = Integer.MAX_VALUE;
        int largest = 0;

        for (Slot slot : container.slots)
        {
            if (areSlotsInSameInventory(slot, slotReference) == false && slot.hasItem() &&
                areStacksEqual(stackReference, slot.getItem()))
            {
                int stackSize = getStackSize(slot.getItem());

                if (stackSize < smallest && stackSize >= idealSize)
                {
                    slotNumSmallest = slot.index;
                    smallest = stackSize;
                }

                if (stackSize > largest)
                {
                    slotNumLargest = slot.index;
                    largest = stackSize;
                }
            }
        }

        return slotNumSmallest != -1 ? slotNumSmallest : slotNumLargest;
    }

    /**
     * Return the slot numbers of slots that have items identical to stackReference.
     * If preferPartial is true, then stacks with a stackSize less that getMaxStackSize() are
     * at the beginning of the list (not ordered though) and full stacks are at the end, otherwise the reverse is true.
     * @param container
     * @param slotReference
     * @param sameInventory if true, then the returned slots are from the same inventory, if false, then from a different inventory
     * @param stackReference
     * @param preferPartial
     * @param treatHotbarAsDifferent
     * @param reverse if true, returns the slots starting from the end of the inventory
     * @return
     */
    @SuppressWarnings("SameParameterValue")
    private static IntArrayList getSlotNumbersOfMatchingStacks(AbstractContainerMenu container,
                                                               Slot slotReference,
                                                               boolean sameInventory,
                                                               ItemStack stackReference,
                                                               boolean preferPartial,
                                                               boolean treatHotbarAsDifferent,
                                                               boolean reverse)
    {
        IntArrayList slots = new IntArrayList(64);
        final int maxSlot = container.slots.size() - 1;
        final int increment = reverse ? -1 : 1;

        for (int i = reverse ? maxSlot : 0; i >= 0 && i <= maxSlot; i += increment)
        {
            Slot slot = container.getSlot(i);

            if (slot != null && slot.hasItem() &&
                areSlotsInSameInventory(slot, slotReference, treatHotbarAsDifferent) == sameInventory &&
                areStacksEqual(slot.getItem(), stackReference))
            {
                if ((getStackSize(slot.getItem()) < stackReference.getMaxStackSize()) == preferPartial)
                {
                    slots.add(0, slot.index);
                }
                else
                {
                    slots.add(slot.index);
                }
            }
        }

        return slots;
    }

    @SuppressWarnings("SameParameterValue")
    private static IntArrayList getSlotNumbersOfMatchingStacks(AbstractContainerMenu container,
                                                               ItemStack stackReference,
                                                               boolean preferPartial)
    {
        IntArrayList slots = new IntArrayList(64);
        final int maxSlot = container.slots.size() - 1;

        for (int i = 0; i <= maxSlot; ++i)
        {
            Slot slot = container.getSlot(i);

            if (slot != null && slot.hasItem() && areStacksEqual(slot.getItem(), stackReference))
            {
                if ((getStackSize(slot.getItem()) < stackReference.getMaxStackSize()) == preferPartial)
                {
                    slots.add(0, slot.index);
                }
                else
                {
                    slots.add(slot.index);
                }
            }
        }

        return slots;
    }

    public static int getPlayerInventoryIndexWithItem(ItemStack stackReference, Inventory inv)
    {
        final int size = inv.getNonEquipmentItems().size();

        for (int index = 0; index < size; ++index)
        {
            ItemStack stack = inv.getNonEquipmentItems().get(index);

            if (areStacksEqual(stack, stackReference))
            {
                return index;
            }
        }

        return -1;
    }

    @SuppressWarnings("SameParameterValue")
    private static IntArrayList getSlotNumbersOfEmptySlots(AbstractContainerMenu container,
                                                           Slot slotReference,
                                                           boolean sameInventory,
                                                           boolean treatHotbarAsDifferent,
                                                           boolean reverse)
    {
        IntArrayList slots = new IntArrayList(64);
        final int maxSlot = container.slots.size() - 1;
        final int increment = reverse ? -1 : 1;

        for (int i = reverse ? maxSlot : 0; i >= 0 && i <= maxSlot; i += increment)
        {
            Slot slot = container.getSlot(i);

            if (slot != null && slot.hasItem() == false &&
                areSlotsInSameInventory(slot, slotReference, treatHotbarAsDifferent) == sameInventory)
            {
                slots.add(slot.index);
            }
        }

        return slots;
    }

    @SuppressWarnings("SameParameterValue")
    private static IntArrayList getSlotNumbersOfEmptySlotsInPlayerInventory(AbstractContainerMenu container,
                                                                            boolean reverse)
    {
        IntArrayList slots = new IntArrayList(64);
        final int maxSlot = container.slots.size() - 1;
        final int increment = reverse ? -1 : 1;

        for (int i = reverse ? maxSlot : 0; i >= 0 && i <= maxSlot; i += increment)
        {
            Slot slot = container.getSlot(i);

            if (slot != null && (slot.container instanceof Inventory) && slot.hasItem() == false)
            {
                slots.add(slot.index);
            }
        }

        return slots;
    }

    public static boolean areStacksEqual(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.isSameItemSameComponents(stack1, stack2);
    }

    private static boolean areSlotsInSameInventory(Slot slot1, Slot slot2)
    {
        return areSlotsInSameInventory(slot1, slot2, false);
    }

    private static boolean areSlotsInSameInventory(Slot slot1, Slot slot2, boolean treatHotbarAsDifferent)
    {
        if (slot1.container == slot2.container)
        {
            if (treatHotbarAsDifferent && slot1.container instanceof Inventory)
            {
                int index1 = AccessorUtils.getSlotIndex(slot1);
                int index2 = AccessorUtils.getSlotIndex(slot2);
                // Don't ever treat the offhand slot as a different inventory
                return index1 == 40 || index2 == 40 || (index1 < 9) == (index2 < 9);
            }

            return true;
        }

        return false;
    }

    private static ItemStack[] getOriginalStacks(AbstractContainerMenu container)
    {
        ItemStack[] originalStacks = new ItemStack[container.slots.size()];

        for (int i = 0; i < originalStacks.length; i++)
        {
            originalStacks[i] = container.slots.get(i).getItem().copy();
        }

        return originalStacks;
    }

    private static void restoreOriginalStacks(AbstractContainerMenu container, ItemStack[] originalStacks)
    {
        for (int i = 0; i < originalStacks.length; i++)
        {
            ItemStack stackSlot = container.getSlot(i).getItem();

            if (areStacksEqual(stackSlot, originalStacks[i]) == false ||
                (isStackEmpty(stackSlot) == false && getStackSize(stackSlot) != getStackSize(originalStacks[i])))
            {
                container.getSlot(i).set(originalStacks[i]);
            }
        }
    }

    private static int getTargetSlot(AbstractContainerMenu container, ItemStack[] originalStacks)
    {
        List<Slot> slots = container.slots;

        for (int i = 0; i < originalStacks.length; i++)
        {
            ItemStack stackOrig = originalStacks[i];
            ItemStack stackNew = slots.get(i).getItem();

            if ((isStackEmpty(stackOrig) && isStackEmpty(stackNew) == false) ||
               (isStackEmpty(stackOrig) == false && isStackEmpty(stackNew) == false &&
               getStackSize(stackNew) == (getStackSize(stackOrig) + 1)))
            {
                return i;
            }
        }

        return -1;
    }

    /*
    private void clickSlotsToMoveItems(Slot slot, ContainerScreen<? extends Container> gui, boolean matchingOnly, boolean toOtherInventory)
    {
        for (Slot slotTmp : gui.getContainer().slots)
        {
            if (slotTmp.id != slot.id && areSlotsInSameInventory(slotTmp, slot) == toOtherInventory &&
                slotTmp.hasStack() && (matchingOnly == false || areStacksEqual(slot.getStack(), slotTmp.getStack())))
            {
                this.clickSlotsToMoveItemsFromSlot(slotTmp, gui, toOtherInventory);
                return;
            }
        }

        // Move the hovered-over slot's stack last
        if (toOtherInventory)
        {
            this.clickSlotsToMoveItemsFromSlot(slot, gui, toOtherInventory);
        }
    }
    */

    private static void clickSlotsToMoveItemsFromSlot(Slot slotFrom,
                                                      AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                      boolean toOtherInventory)
    {
        // Left click to pick up the found source stack
        leftClickSlot(gui, slotFrom.index);

        if (isStackEmpty(gui.getMenu().getCarried()))
        {
            return;
        }

        for (Slot slotDst : gui.getMenu().slots)
        {
            ItemStack stackDst = slotDst.getItem();

            if (areSlotsInSameInventory(slotDst, slotFrom) != toOtherInventory &&
                (isStackEmpty(stackDst) || areStacksEqual(stackDst, gui.getMenu().getCarried())))
            {
                // Left click to (try and) place items to the slot
                leftClickSlot(gui, slotDst.index);
            }

            if (isStackEmpty(gui.getMenu().getCarried()))
            {
                return;
            }
        }

        // Couldn't fit the entire stack to the target inventory, return the rest of the items
        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            leftClickSlot(gui, slotFrom.index);
        }
    }

    private static boolean clickSlotsToMoveSingleItem(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                      int slotFrom,
                                                      int slotTo)
    {
        //System.out.println("clickSlotsToMoveSingleItem(from: " + slotFrom + ", to: " + slotTo + ")");
        ItemStack stack = gui.getMenu().slots.get(slotFrom).getItem();

        if (isStackEmpty(stack))
        {
            return false;
        }

        // Click on the from-slot to take items to the cursor - if there is more than one item in the from-slot,
        // right click on it, otherwise left click.
        if (getStackSize(stack) > 1)
        {
            rightClickSlot(gui, slotFrom);
        }
        else
        {
            leftClickSlot(gui, slotFrom);
        }

        // Right click on the target slot to put one item to it
        rightClickSlot(gui, slotTo);

        // If there are items left in the cursor, then return them back to the original slot
        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            // Left click again on the from-slot to return the rest of the items to it
            leftClickSlot(gui, slotFrom);
        }

        return true;
    }

    private static boolean clickSlotsToMoveSingleItemByShiftClick(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                                  int slotFrom)
    {
        Slot slot = gui.getMenu().slots.get(slotFrom);
        ItemStack stack = slot.getItem();

        if (isStackEmpty(stack))
        {
            return false;
        }

        if (getStackSize(stack) > 1)
        {
            // Left click on the from-slot to take all the items to the cursor
            leftClickSlot(gui, slotFrom);

            // Still items left in the slot, put the stack back and abort
            if (slot.hasItem())
            {
                leftClickSlot(gui, slotFrom);
                return false;
            }
            else
            {
                // Right click one item back to the slot
                rightClickSlot(gui, slotFrom);
            }
        }

        // ... and then shift-click on the slot
        shiftClickSlot(gui, slotFrom);

        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            // ... and then return the rest of the items
            leftClickSlot(gui, slotFrom);
        }

        return true;
    }

    /**
     * Try move items from slotFrom to slotTo
     * @return true if at least some items were moved
     */
    private static boolean clickSlotsToMoveItems(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                 int slotFrom,
                                                 int slotTo)
    {
        //System.out.println("clickSlotsToMoveItems(from: " + slotFrom + ", to: " + slotTo + ")");

        // Left click to take items
        leftClickSlot(gui, slotFrom);

        // Couldn't take the items, bail out now
        if (isStackEmpty(gui.getMenu().getCarried()))
        {
            return false;
        }

        boolean ret = true;
        int size = getStackSize(gui.getMenu().getCarried());

        // Left click on the target slot to put the items to it
        leftClickSlot(gui, slotTo);

        // If there are items left in the cursor, then return them back to the original slot
        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            ret = getStackSize(gui.getMenu().getCarried()) != size;

            // Left click again on the from-slot to return the rest of the items to it
            leftClickSlot(gui, slotFrom);
        }

        return ret;
    }

    public static void dropStacksUntilEmpty(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                            int slotNum)
    {
        if (slotNum >= 0 && slotNum < gui.getMenu().slots.size())
        {
            Slot slot = gui.getMenu().getSlot(slotNum);
            int failsafe = 64;

            while (failsafe-- > 0 && slot.hasItem())
            {
                dropStack(gui, slotNum);
            }
        }
    }

    public static void dropStacksWhileHasItem(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                              int slotNum,
                                              ItemStack stackReference)
    {
        if (slotNum >= 0 && slotNum < gui.getMenu().slots.size())
        {
            Slot slot = gui.getMenu().getSlot(slotNum);
            int failsafe = 256;

            while (failsafe-- > 0 && areStacksEqual(slot.getItem(), stackReference))
            {
                dropStack(gui, slotNum);
            }
        }
    }

    private static boolean shiftClickSlotWithCheck(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                   int slotNum)
    {
        Slot slot = gui.getMenu().getSlot(slotNum);

        if (slot == null || slot.hasItem() == false)
        {
            return false;
        }

        int sizeOrig = getStackSize(slot.getItem());
        shiftClickSlot(gui, slotNum);

        return slot.hasItem() == false || getStackSize(slot.getItem()) != sizeOrig;
    }

    public static boolean tryMoveItemsVertically(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                 Slot slot,
                                                 boolean moveUp,
                                                 MoveAmount amount)
    {
        // We require an empty cursor
        if (slot == null || isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            return false;
        }

        IntArrayList slots = getVerticallyFurthestSuitableSlotsForStackInSlot(gui.getMenu(), slot, moveUp);

        if (slots.isEmpty())
        {
            return false;
        }

        if (amount == MoveAmount.FULL_STACKS)
        {
            moveStackToSlots(gui, slot, slots, false);
        }
        else if (amount == MoveAmount.MOVE_ONE)
        {
            moveOneItemToFirstValidSlot(gui, slot, slots);
        }
        else if (amount == MoveAmount.LEAVE_ONE)
        {
            moveStackToSlots(gui, slot, slots, true);
        }
        else if (amount == MoveAmount.ALL_MATCHING)
        {
            moveMatchingStacksToSlots(gui, slot, moveUp);
        }

        return true;
    }

    private static void moveMatchingStacksToSlots(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                  Slot slot,
                                                  boolean moveUp)
    {
        IntArrayList matchingSlots = getSlotNumbersOfMatchingStacks(gui.getMenu(), slot, true, slot.getItem(), true, true, false);
        IntArrayList targetSlots = getSlotNumbersOfEmptySlots(gui.getMenu(), slot, false, true, false);
        targetSlots.addAll(getSlotNumbersOfEmptySlots(gui.getMenu(), slot, true, true, false));
        targetSlots.addAll(matchingSlots);

        matchingSlots.sort(new SlotVerticalSorterSlotNumbers(gui.getMenu(), !moveUp));
        targetSlots.sort(new SlotVerticalSorterSlotNumbers(gui.getMenu(), moveUp));

        for (int matchingSlot : matchingSlots)
        {
            int srcSlotNum = matchingSlot;
            Slot srcSlot = gui.getMenu().getSlot(srcSlotNum);
            Slot lastSlot = moveStackToSlots(gui, srcSlot, targetSlots, false);

            if (lastSlot == null || (lastSlot.index == srcSlot.index || (lastSlot.y > srcSlot.y) == moveUp))
            {
                return;
            }
        }
    }

    private static Slot moveStackToSlots(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                         Slot slotFrom,
                                         IntArrayList slotsTo,
                                         boolean leaveOne)
    {
        Slot lastSlot = null;

        // Empty slot, nothing to do
        if (slotFrom.hasItem() == false)
        {
            return null;
        }

        // Pick up the stack
        leftClickSlot(gui, slotFrom.index);

        if (leaveOne)
        {
            rightClickSlot(gui, slotFrom.index);
        }

        for (int slotNum : slotsTo)
        {
            // Empty cursor, all done here
            if (isStackEmpty(gui.getMenu().getCarried()))
            {
                break;
            }

            Slot dstSlot = gui.getMenu().getSlot(slotNum);

            if (dstSlot.mayPlace(gui.getMenu().getCarried()) &&
                (dstSlot.hasItem() == false || areStacksEqual(dstSlot.getItem(), gui.getMenu().getCarried())))
            {
                leftClickSlot(gui, slotNum);
                lastSlot = dstSlot;
            }
        }

        // Return the rest of the items, if any
        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            leftClickSlot(gui, slotFrom.index);
        }

        return lastSlot;
    }

    private static void moveOneItemToFirstValidSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                    Slot slotFrom,
                                                    IntArrayList slotsTo)
    {
        // Pick up half of the the stack
        rightClickSlot(gui, slotFrom.index);

        if (isStackEmpty(gui.getMenu().getCarried()))
        {
            return;
        }

        int sizeOrig = getStackSize(gui.getMenu().getCarried());

        for (int slotNum : slotsTo)
        {
            rightClickSlot(gui, slotNum);
            ItemStack stackCursor = gui.getMenu().getCarried();

            if (isStackEmpty(stackCursor) || getStackSize(stackCursor) != sizeOrig)
            {
                break;
            }
        }

        // Return the rest of the items, if any
        if (isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            leftClickSlot(gui, slotFrom.index);
        }
    }

    private static IntArrayList getVerticallyFurthestSuitableSlotsForStackInSlot(AbstractContainerMenu container,
                                                                                  Slot slotIn,
                                                                                  boolean above)
    {
        if (slotIn == null || slotIn.hasItem() == false)
        {
            return IntArrayList.of();
        }

        IntArrayList slotNumbers = new IntArrayList();
        ItemStack stackSlot = slotIn.getItem();

        for (Slot slotTmp : container.slots)
        {
            if (slotTmp.index != slotIn.index && slotTmp.y != slotIn.y)
            {
                if (above == slotTmp.y < slotIn.y)
                {
                    ItemStack stackTmp = slotTmp.getItem();

                    if ((isStackEmpty(stackTmp) && slotTmp.mayPlace(stackSlot)) ||
                        (areStacksEqual(stackTmp, stackSlot)) && slotTmp.getMaxStackSize(stackTmp) > getStackSize(stackTmp))
                    {
                        slotNumbers.add(slotTmp.index);
                    }
                }
            }
        }

        slotNumbers.sort(new SlotVerticalSorterSlotNumbers(container, above));

        return slotNumbers;
    }

    public static void tryClearCursor(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        ItemStack stackCursor = gui.getMenu().getCarried();

        if (isStackEmpty(stackCursor) == false)
        {
            IntArrayList emptySlots = getSlotNumbersOfEmptySlotsInPlayerInventory(gui.getMenu(), false);

            if (emptySlots.isEmpty() == false)
            {
                leftClickSlot(gui, emptySlots.getInt(0));
            }
            else
            {
                IntArrayList matchingSlots = getSlotNumbersOfMatchingStacks(gui.getMenu(), stackCursor, true);

                if (matchingSlots.isEmpty() == false)
                {
                    for (int slotNum : matchingSlots)
                    {
                        Slot slot = gui.getMenu().getSlot(slotNum);
                        ItemStack stackSlot = slot.getItem();

                        if (slot == null || areStacksEqual(stackSlot, stackCursor) == false ||
                            getStackSize(stackSlot) >= stackCursor.getMaxStackSize())
                        {
                            break;
                        }

                        if (slot.container instanceof Inventory)
                        {
                            leftClickSlot(gui, slotNum);
                            stackCursor = gui.getMenu().getCarried();
                        }
                    }
                }
            }

            if (isStackEmpty(gui.getMenu().getCarried()) == false)
            {
                dropItemsFromCursor(gui);
            }
        }
    }

    public static void resetLastSlotNumber()
    {
        slotNumberLast = -1;
    }

    public static MoveAction getActiveMoveAction()
    {
        return activeMoveAction;
    }

    public static void sortInventory(AbstractContainerScreen<?> gui)
    {
        Pair<Integer, Integer> range = new IntIntMutablePair(Integer.MAX_VALUE, 0);
        Slot focusedSlot = AccessorUtils.getSlotUnderMouse(gui);
        Minecraft mc = GameWrap.getClient();
        boolean shulkerBoxFix;

        if (focusedSlot == null)
	        //|| focusedSlot.hasStack() == false)
        {
            return;
        }

        //System.out.printf("sort - focusedSlot[%d]: %s\n", focusedSlot.id, focusedSlot.hasStack() ? focusedSlot.getStack().getName().getString() : "<EMPTY>");
        AbstractContainerMenu container = gui.getMenu();
        int limit = container.slots.size();
        int focusedIndex = -1;

        if (gui instanceof CreativeModeInventoryScreen creative && !creative.isInventoryOpen())
        {
            return;
        }
        if (gui instanceof InventoryScreen && (focusedSlot.index < 9 || focusedSlot.index > 44))
        {
            return;
        }

        // Do not try to sort shulkers inside a shulker
        shulkerBoxFix = gui instanceof ShulkerBoxScreen && focusedSlot.index < 27;

        for (int i = 0; i < limit; i++)
        {
            Slot slot = container.slots.get(i);

            //System.out.printf("sort - slot[%d]: %s\n", i, slot.hasStack() ? slot.getStack().getName().getString() : "<EMPTY>");
            if (slot == focusedSlot)
            {
                focusedIndex = i;
            }
            if (slot.container == focusedSlot.container)
            {
                if (i < range.first())
                {
                    range.first(i);
                }
                if (i >= range.second())
                {
                    range.second(i + 1);
                }
            }
        }

        if (focusedIndex == -1)
        {
            return;
        }

        if (focusedSlot.container instanceof Inventory)
        {
            if (range.left() == 5 && range.right() == 46)
            {
                // Creative, PlayerScreenHandler
                if (focusedIndex >= 9 && focusedIndex < 36)
                {
                    range.left(9).right(36);
                }
                else if (focusedIndex >= 36 && focusedIndex < 45)
                {
                    range.left(36).right(45);
                }
            }
            else if (range.right() - range.left() == 36)
            {
                // Normal containers
                if (focusedIndex < range.left() + 27)
                {
                    range.right(range.left() + 27);
                }
                else
                {
                    range.left(range.right() - 9);
                }
            }
        }

        // try to find usable hotbar slot
        int hotbarSlot = 8;
        if ( shulkerBoxFix )
        {
            var playerInventory = Minecraft.getInstance().player.getInventory();
            while ( hotbarSlot >= 0 )
            {
                int slot_ix = container.findSlot(playerInventory, hotbarSlot).orElse(-1);
                if ( slot_ix != -1 && !isShulkerBox(container.getSlot(slot_ix).getItem()) )
                {
                    break;
                }

                --hotbarSlot;
            }

            if ( hotbarSlot < 0 )
            {
                ItemScroller.LOGGER.warn("sortInventory(): no usable hotbar slot to sort shulkerbox");
                return;
            }
        }

        final int swapSlot = hotbarSlot;

        //System.out.printf("Sorting [%d, %d] (first, second)\n", range.first(), range.second());
        //System.out.printf("Sorting [%d, %d] (left, right)\n", range.left(), range.right());
        tryClearCursor(gui);
        tryMergeItems(gui, range.left(), range.right() - 1);

        if (Configs.Generic.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue())
        {
            ServerboundClientCommandPacket packet = new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS);

            mc.getConnection().send(packet);
            selectedSlotUpdateTask = () -> trySort(gui, range.first(), range.second(), shulkerBoxFix, swapSlot);
        }
        else
        {
            trySort(gui, range.first(), range.second(), shulkerBoxFix, swapSlot);
        }
    }

    private static void trySort(AbstractContainerScreen<?> gui, int start, int end, boolean shulkerBoxFix, int swapSlot)
    {
        try
        {
            quickSort(gui, start, end, shulkerBoxFix, swapSlot);
        }
        catch (Exception err)
        {
            ItemScroller.LOGGER.error("trySort(): failed to sort items", err);
        }
    }

    private static void quickSort(AbstractContainerScreen<?> gui, int start, int end, boolean shulkerBoxFix, int swapSlot)
    {
        var ct = end - start;
        var handler = gui.getMenu();

        // make snapshot of contents; give each item a temporary ID.
        // this ID also happens to be its slot index, relative to `start`.
        var snapshot = new ArrayList<>
        (
                IntStream.range(0, end - start)
                    .mapToObj(ix -> Pair.of(ix, handler.getSlot(start + ix).getItem().copy()))
                    .filter(pair -> !(shulkerBoxFix && isShulkerBox(pair.value())))
                    .toList()
        );
        ct = snapshot.size();

        // because the array might have unsortable holes, build an index from array index to slot index
        int[] slotindex_by_arrayindex = snapshot.stream().mapToInt(pair -> start + pair.key()).toArray();

        // sort pairs
        List<Pair<Integer, ItemStack>> sorted_pairs =
        (
            snapshot.stream()
                .sorted(
                    (left, right) ->
                        compareStacks(left.value(), right.value())
                    )
                .toList()
        );

        ItemScroller.LOGGER.debug(String.format
        (
            "======\nsort\n%s\n\n",
            IntStream.range(0, ct)
                .mapToObj(
                    ix -> String.format(
                        "%2d: %2d/%-20s  %2d/%-20s",
                        ix,
                        snapshot.get(ix).key(), snapshot.get(ix).value().getHoverName().getString(),
                        sorted_pairs.get(ix).key(),sorted_pairs.get(ix).value().getHoverName().getString()
                    )
                )
                .collect(Collectors.joining("\n"))
        ));

        // build index of an item's final position by its fake ID
        Map<Integer, Integer> finalpos_by_id =
        (
            IntStream.range(0, ct).boxed()
                .collect(Collectors.toMap(
                    ix -> sorted_pairs.get(ix).key(),
                    ix -> ix
                ))
        );

        // sort
        int limit = 0, max_limit = 200;
        Pair<Integer,ItemStack> dst, hold = null;

        for (int src_ix = 0; src_ix < ct; ++src_ix)
        {
            // check if item is in correct position
            Pair<Integer,ItemStack> src = snapshot.get(src_ix);
            int src_id = src.key();
            int dst_ix = finalpos_by_id.get(src_id);

            dst = snapshot.get(dst_ix);

            if (src_ix == dst_ix)
            {
                ItemScroller.LOGGER.debug("quickSort(): {} ok", src_ix);
                continue;
            }

            // pick up and hold "src"
            snapshot.set(src_ix, hold);
            hold = src;
            ItemScroller.LOGGER.debug("quickSort(): pick up {}; holding {}", src_ix, hold);
            clickSlot(gui, slotindex_by_arrayindex[src_ix], swapSlot, ClickType.SWAP);

            // continually place the held item into its correct place, following the chain to its end
            // todo: we could skip swapping empty slots, but for some reason, this is not reliable. it seems to swap
            //       in an item from the player's hotbar into the container.
            for (limit = 0; limit < max_limit; ++limit)
            {
                snapshot.set(dst_ix, hold);
                hold = dst;
                clickSlot(gui, slotindex_by_arrayindex[dst_ix], swapSlot, ClickType.SWAP);

                ItemScroller.LOGGER.debug("quickSort(): ... swap {} {}; holding {}", dst_ix, dst != null ? dst.value() : "null", hold);
                if (hold == null)
                {
                    break;
                }

                dst_ix = finalpos_by_id.get(hold.key());
                dst = snapshot.get(dst_ix);
            }

            if (limit == max_limit)
            {
                ItemScroller.LOGGER.warn("quickSort(): took too long to follow swap chain ??");
            }

        }
        if (hold != null)
        {
            ItemScroller.LOGGER.warn("quickSort(): sorting complete, but still holding {} ??", hold);
        }
    }

    private static int compareStacks(ItemStack stack1, ItemStack stack2)
    {
        Minecraft mc = GameWrap.getClient();

        stack1 = stack1 != null ? stack1 : ItemStack.EMPTY;
        stack2 = stack2 != null ? stack2 : ItemStack.EMPTY;

        // boxes towards the end of the list
        boolean stack1IsBox = isShulkerBox(stack1);
        boolean stack2IsBox = isShulkerBox(stack2);

        if (Configs.Generic.SORT_SHULKER_BOXES_AT_END.getBooleanValue() && stack1IsBox != stack2IsBox)
        {
            return Boolean.compare(stack1IsBox, stack2IsBox);
        }

        // bundles towards the end of the list
        boolean stack1IsBundle = isBundle(stack1);
        boolean stack2IsBundle = isBundle(stack2);

        if (Configs.Generic.SORT_BUNDLES_AT_END.getBooleanValue() && stack1IsBundle != stack2IsBundle)
        {
            return Boolean.compare(stack1IsBundle, stack2IsBundle);
        }

        // order items according to user-defined top/bottom priority
        // a priority of -1 means that no priority was specified
        int priority1 = getCustomPriority(stack1);
        int priority2 = getCustomPriority(stack2);

        if (priority1 != -1 || priority2 != -1)
        {
            return Integer.compare(priority1, priority2);
        }

        // empty slots last
        boolean stack1IsEmpty = stack1.isEmpty();
        boolean stack2IsEmpty = stack2.isEmpty();

        if (stack1IsEmpty != stack2IsEmpty)
        {
            return Boolean.compare(stack1IsEmpty, stack2IsEmpty);
        }

        if (stack1IsEmpty)
        {
            // both stacks are empty
            return 0;
        }

        // sort by shulker box contents
        if (stack1IsBox && stack2IsBox)
        {
            List<ItemStack> contents1 = stack1.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList();
            List<ItemStack> contents2 = stack2.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList();
            int flip = (Configs.Generic.SORT_SHULKER_BOXES_INVERTED.getBooleanValue() ? -1 : 1);

            return Integer.compare(contents1.size(), contents2.size()) * flip;
        }

        // sort by bundle contents
        if (stack1IsBundle && stack2IsBundle)
        {
            BundleContents bundle1 = stack1.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            BundleContents bundle2 = stack2.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
            int flip = (Configs.Generic.SORT_BUNDLES_INVERTED.getBooleanValue() ? -1 : 1);
            Fraction occupancy1 = bundle1.weight();
            Fraction occupancy2 = bundle2.weight();

            return occupancy1.compareTo(occupancy2) * flip;
        }

        SortingMethod method = (SortingMethod) Configs.Generic.SORT_METHOD_DEFAULT.getOptionListValue();

        if (method.equals(SortingMethod.CATEGORY_NAME) ||
            method.equals(SortingMethod.CATEGORY_COUNT) ||
            method.equals(SortingMethod.CATEGORY_RARITY) ||
            method.equals(SortingMethod.CATEGORY_RAWID) &&
            mc.level != null)
        {
            // Sort by category
            if (displayContext == null)
            {
                displayContext = SortingCategory.INSTANCE.buildDisplayContext(mc);
                // This isn't used here, but it is required to build the list of items,
                // as if we are opening the Creative Inventory Screen.
            }

            SortingCategory.Entry cat1 = SortingCategory.INSTANCE.fromItemStack(stack1);
            SortingCategory.Entry cat2 = SortingCategory.INSTANCE.fromItemStack(stack2);

            if (!cat1.getStringValue().equals(cat2.getStringValue()))
            {
                int index1 = Configs.Generic.SORT_CATEGORY_ORDER.getEntryIndex(cat1);
                int index2 = Configs.Generic.SORT_CATEGORY_ORDER.getEntryIndex(cat2);
                boolean stack1UnspecifiedCategoryPriority = index1 == -1;
                boolean stack2UnspecifiedCategoryPriority = index2 == -1;

                if ( stack1UnspecifiedCategoryPriority != stack2UnspecifiedCategoryPriority)
                {
                    return Boolean.compare(stack1UnspecifiedCategoryPriority, stack2UnspecifiedCategoryPriority);
                }

                return Integer.compare(index1, index2);
            }
        }

        if (stack1.getItem() != stack2.getItem())
        {
            if (method.equals(SortingMethod.CATEGORY_NAME) || method.equals(SortingMethod.ITEM_NAME))
            {
                // Sort by Item Name
                return stack1.getHoverName().getString().compareTo(stack2.getHoverName().getString());
            }
            else if (method.equals(SortingMethod.CATEGORY_COUNT) || method.equals(SortingMethod.ITEM_COUNT))
            {
                // Sort by Item Count
                int result = Integer.compare(stack2.getCount(), stack1.getCount());
                if ( result != 0 )
                {
                    return result;
                }

                return Integer.compare(BuiltInRegistries.ITEM.getId(stack1.getItem()), BuiltInRegistries.ITEM.getId(stack2.getItem()));
            }
            else if (method.equals(SortingMethod.CATEGORY_RARITY) || method.equals(SortingMethod.ITEM_RARITY))
            {
                // Sort by Item Rarity
                int result = stack1.getRarity().compareTo(stack2.getRarity());
                if ( result != 0 )
                {
                    return result;
                }

                return Integer.compare(BuiltInRegistries.ITEM.getId(stack1.getItem()), BuiltInRegistries.ITEM.getId(stack2.getItem()));
            }
            else
            {
                // Sort by Item RawID
                return Integer.compare(BuiltInRegistries.ITEM.getId(stack1.getItem()), BuiltInRegistries.ITEM.getId(stack2.getItem()));
            }
        }
        if (areStacksEqual(stack1, stack2) == false)
        {
            // Sort's Data Components by Hash Code
            return Integer.compare(stack1.getComponents().hashCode(), stack2.getComponents().hashCode());
        }

        return Integer.compare(stack2.getCount(), stack1.getCount());
    }

    private static int getCustomPriority(ItemStack stack)
    {
        if (stack == null || stack.isEmpty())
        {
            // No priority for empty stacks
            return -1;
        }

        // Get item ID and name to check against custom priority lists
        String itemID = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        String itemName = stack.getHoverName().getString();

        if (itemID.equals(itemName))
        {
            itemName = null;
        }

        // Top priority check
        int idTopPriority = topSortingPriorityList.indexOf(itemID);
        int nameTopPriority = itemName != null ? topSortingPriorityList.indexOf(itemName) : -1;

        // Bottom priority check
        int idBottomPriority = bottomSortingPriorityList.indexOf(itemID);
        int nameBottomPriority = itemName != null ? bottomSortingPriorityList.indexOf(itemName) : -1;

        // Sort at the top: Prefer name priority if it exists
        if (nameTopPriority != -1)
        {
            return -topSortingPriorityList.size() + nameTopPriority - 2;
        }
        if (idTopPriority != -1)
        {
            return -topSortingPriorityList.size() + idTopPriority - 2;
        }

        // Sort at the bottom: Prefer name priority if it exists
        if (nameBottomPriority != -1)
        {
            return bottomSortingPriorityList.size() + nameBottomPriority;
        }
        if (idBottomPriority != -1)
        {
            return bottomSortingPriorityList.size() + idBottomPriority;
        }

        // Default: no specific priority found
        return -1;
    }

    public static boolean onPong(ClientboundAwardStatsPacket packet)
    {
        if (selectedSlotUpdateTask != null)
        {
            selectedSlotUpdateTask.run();
            selectedSlotUpdateTask = null;
            return true;
        }
        return false;
    }

    public static boolean isShulkerBox(ItemStack stack)
    {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean isEmptyShulkerBox(ItemStack stack)
    {
        return isShulkerBox(stack) && stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().findAny().isEmpty();
    }

    private static boolean isBundle(ItemStack stack)
    {
        return stack.is(Items.BUNDLE) || stack.getComponents().has(DataComponents.BUNDLE_CONTENTS);
    }

    private static boolean isEmptyBundle(ItemStack stack)
    {
        return isBundle(stack) && fi.dy.masa.malilib.util.InventoryUtils.bundleCountItems(stack) < 1;
    }

    public static int stackMaxSize(ItemStack stack, boolean assumeShulkerStacking)
    {
        if (stack.isEmpty())
        {
            return 64;
        }

        if (assumeShulkerStacking && Configs.Generic.SORT_ASSUME_EMPTY_BOX_STACKS.getBooleanValue())
        {
            if (isEmptyShulkerBox(stack))
            {
                return 64;
            }
        }

        return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    /**
     * @return are there still items left in the original slot?
     */
    private static boolean addStackTo(AbstractContainerScreen<? extends AbstractContainerMenu> gui, Slot slot, Slot target)
    {
        if (slot == null || target == null)
        {
            return false;
        }

        ItemStack stack = slot.getItem();
        ItemStack targetStack = target.getItem();

        if (stack.isEmpty() || !ItemStack.isSameItem(stack, targetStack))
        {
            return !stack.isEmpty();
        }

        if (targetStack.isEmpty())
        {
            clickSlot(gui, slot, slot.index, 0, ClickType.PICKUP);
            clickSlot(gui, target, target.index, 0, ClickType.PICKUP);
            //System.out.printf("Moved stack from slot %d to slot %d\n", slot.id, target.id);
            //ItemScroller.printDebug("Moved stack from slot {} to slot {}", slot.id, target.id);
            return false;
        }

        int stackSize = stack.getCount();
        int targetSize = targetStack.getCount();
        assumeEmptyShulkerStacking = true;
        int maxSize = stackMaxSize(stack, true);
        //System.out.printf("Merging %s into %s, maxSize: %d\n", stack, targetStack, maxSize);
        //ItemScroller.printDebug("Merging {} into {}, maxSize: {}", stack, targetStack, maxSize);

        if (targetSize >= maxSize)
        {
            return true;
        }

        clickSlot(gui, slot, slot.index, 0, ClickType.PICKUP);
        clickSlot(gui, target, target.index, 0, ClickType.PICKUP);
        clickSlot(gui, slot, slot.index, 0, ClickType.PICKUP);
        assumeEmptyShulkerStacking = false;
        int amount = stackSize + targetSize - maxSize;

        return amount > 0;
    }

    private static void tryMergeItems(AbstractContainerScreen<?> gui, int left, int right)
    {
        Map<ItemType, Integer> nonFullStacks = new HashMap<>();

        for (int i = left; i <= right; i++)
        {
            Slot slot = gui.getMenu().getSlot(i);

            if (slot.hasItem())
            {
                ItemStack stack = slot.getItem();

                if (stack.getCount() >= stackMaxSize(stack, true)) {
                    // ignore overstacking items.
                    continue;
                }

                ItemType key = new ItemType(stack);
                int slotNum = nonFullStacks.getOrDefault(key, -1);

                if (slotNum == -1)
                {
                    nonFullStacks.put(key, i);
                }
                else
                {
                    if (addStackTo(gui, slot, gui.getMenu().getSlot(slotNum)))
                    {
                        nonFullStacks.put(key, i);
                    }
                }
            }
        }
    }

    /*
    private static class SlotVerticalSorterSlots implements Comparator<Slot>
    {
        private final boolean topToBottom;

        public SlotVerticalSorterSlots(boolean topToBottom)
        {
            this.topToBottom = topToBottom;
        }

        @Override
        public int compare(Slot slot1, Slot slot2)
        {
            if (slot1.yPos == slot2.yPos)
            {
                return (slot1.id < slot2.id) == this.topToBottom ? -1 : 1;
            }

            return (slot1.yPos < slot2.yPos) == this.topToBottom ? -1 : 1;
        }
    }
    */

    private static class SlotVerticalSorterSlotNumbers implements IntComparator
    {
        private final AbstractContainerMenu container;
        private final boolean topToBottom;

        public SlotVerticalSorterSlotNumbers(AbstractContainerMenu container, boolean topToBottom)
        {
            this.container = container;
            this.topToBottom = topToBottom;
        }

        @Override
        public int compare(int slotNum1, int slotNum2)
        {
            if (Objects.equals(slotNum1, slotNum2))
            {
                return 0;
            }

            Slot slot1 = this.container.getSlot(slotNum1);
            Slot slot2 = this.container.getSlot(slotNum2);

            if (slot1.y == slot2.y)
            {
                return (slot1.index < slot2.index) == this.topToBottom ? -1 : 1;
            }

            return (slot1.y < slot2.y) == this.topToBottom ? -1 : 1;
        }
    }

    public static void clickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                 int slotNum,
                                 int mouseButton,
                                 ClickType type)
    {
        if (slotNum >= 0 && slotNum < gui.getMenu().slots.size())
        {
            Slot slot = gui.getMenu().getSlot(slotNum);
            clickSlot(gui, slot, slotNum, mouseButton, type);
        }
        else
        {
            try
            {
                Minecraft mc = Minecraft.getInstance();
                mc.gameMode.handleInventoryMouseClick(gui.getMenu().containerId, slotNum, mouseButton, type, mc.player);
            }
            catch (Exception e)
            {
                ItemScroller.LOGGER.warn("Exception while emulating a slot click: gui: '{}', slotNum: {}, mouseButton; {}, SlotActionType: {}",
                                         gui.getClass().getName(), slotNum, mouseButton, type, e);
            }
        }
    }

    public static void clickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                 Slot slot,
                                 int slotNum,
                                 int mouseButton,
                                 ClickType type)
    {
        try
        {
            AccessorUtils.handleMouseClick(gui, slot, slotNum, mouseButton, type);
        }
        catch (Exception e)
        {
            ItemScroller.LOGGER.warn("Exception while emulating a slot click: gui: '{}', slotNum: {}, mouseButton; {}, SlotActionType: {}",
                                     gui.getClass().getName(), slotNum, mouseButton, type, e);
        }
    }

    public static void leftClickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, Slot slot, int slotNumber)
    {
        clickSlot(gui, slot, slotNumber, 0, ClickType.PICKUP);
    }

    public static void rightClickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, Slot slot, int slotNumber)
    {
        clickSlot(gui, slot, slotNumber, 1, ClickType.PICKUP);
    }

    public static void shiftClickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, Slot slot, int slotNumber)
    {
        clickSlot(gui, slot, slotNumber, 0, ClickType.QUICK_MOVE);
    }

    public static void leftClickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, int slotNum)
    {
        clickSlot(gui, slotNum, 0, ClickType.PICKUP);
    }

    public static void rightClickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, int slotNum)
    {
        clickSlot(gui, slotNum, 1, ClickType.PICKUP);
    }

    public static void shiftClickSlot(AbstractContainerScreen<? extends AbstractContainerMenu> gui, int slotNum)
    {
        clickSlot(gui, slotNum, 0, ClickType.QUICK_MOVE);
    }

    public static void dropItemsFromCursor(AbstractContainerScreen<? extends AbstractContainerMenu> gui)
    {
        clickSlot(gui, -999, 0, ClickType.PICKUP);
    }

    public static void dropItem(AbstractContainerScreen<? extends AbstractContainerMenu> gui, int slotNum)
    {
        clickSlot(gui, slotNum, 0, ClickType.THROW);
    }

    public static void dropStack(AbstractContainerScreen<? extends AbstractContainerMenu> gui, int slotNum)
    {
        clickSlot(gui, slotNum, 1, ClickType.THROW);
    }

    public static void swapSlots(AbstractContainerScreen<? extends AbstractContainerMenu> gui, int slotNum, int otherSlot)
    {
        //System.out.printf("swapSlots: [%d -> %d]\n", slotNum, otherSlot);

        clickSlot(gui, slotNum, 8, ClickType.SWAP);
        clickSlot(gui, otherSlot, 8, ClickType.SWAP);
        clickSlot(gui, slotNum, 8, ClickType.SWAP);
    }

    private static void dragSplitItemsIntoSlots(AbstractContainerScreen<? extends AbstractContainerMenu> gui,
                                                IntArrayList targetSlots)
    {
        ItemStack stackInCursor = gui.getMenu().getCarried();

        if (isStackEmpty(stackInCursor))
        {
            return;
        }

        if (targetSlots.size() == 1)
        {
            leftClickSlot(gui, targetSlots.getInt(0));
            return;
        }

        int numSlots = gui.getMenu().slots.size();

        // Start the drag
        clickSlot(gui, -999, 0, ClickType.QUICK_CRAFT);

        for (int slotNum : targetSlots)
        {
            if (slotNum >= numSlots)
            {
                break;
            }

            clickSlot(gui, slotNum, 1, ClickType.QUICK_CRAFT);
        }

        // End the drag
        clickSlot(gui, -999, 2, ClickType.QUICK_CRAFT);
    }

    /**************************************************************
     * Compatibility code for pre-1.11 vs. 1.11+
     * Well kind of, as in make the differences minimal,
     * only requires changing these things for the ItemStack
     * related changes.
     *************************************************************/

    public static final ItemStack EMPTY_STACK = ItemStack.EMPTY;

    public static boolean isStackEmpty(ItemStack stack)
    {
        return stack.isEmpty();
    }

    public static int getStackSize(ItemStack stack)
    {
        return stack.getCount();
    }

    public static void setStackSize(ItemStack stack, int size)
    {
        stack.setCount(size);
    }

    public static ItemStack copyStack(ItemStack stack, boolean empty)
    {
        if (empty)
            return stack.copyAndClear();
        else
            return stack.copy();
    }
}
