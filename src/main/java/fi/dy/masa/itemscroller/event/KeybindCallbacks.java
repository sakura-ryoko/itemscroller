package fi.dy.masa.itemscroller.event;

import fi.dy.masa.itemscroller.mixin.IMixinCraftingResultSlot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IClientTickHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.gui.GuiConfigs;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.RecipePattern;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.*;

public class KeybindCallbacks implements IHotkeyCallback, IClientTickHandler
{
    private static final KeybindCallbacks INSTANCE = new KeybindCallbacks();

    protected int massCraftTicker;

    private boolean recipeBookClicks = false;

    public static KeybindCallbacks getInstance()
    {
        return INSTANCE;
    }

    private KeybindCallbacks()
    {
    }

    public void setCallbacks()
    {
        for (ConfigHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            hotkey.getKeybind().setCallback(this);
        }
    }

    public boolean functionalityEnabled()
    {
        return Configs.Generic.MOD_MAIN_TOGGLE.getBooleanValue();
    }

    @Override
    public boolean onKeyAction(KeyAction action, IKeybind key)
    {
        if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
        {
            ClickPacketBuffer.setShouldBufferClickPackets(true);
        }

        boolean cancel = this.onKeyActionImpl(action, key);

        ClickPacketBuffer.setShouldBufferClickPackets(false);

        return cancel;
    }

    private boolean onKeyActionImpl(KeyAction action, IKeybind key)
    {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player == null || mc.world == null)
        {
            return false;
        }

        if (key == Hotkeys.TOGGLE_MOD_ON_OFF.getKeybind())
        {
            Configs.Generic.MOD_MAIN_TOGGLE.toggleBooleanValue();
            String msg = this.functionalityEnabled() ? "itemscroller.message.toggled_mod_on" : "itemscroller.message.toggled_mod_off";
            InfoUtils.showGuiOrInGameMessage(Message.MessageType.INFO, msg);
            return true;
        }
        else if (key == Hotkeys.OPEN_CONFIG_GUI.getKeybind())
        {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }

        if (this.functionalityEnabled() == false ||
            (GuiUtils.getCurrentScreen() instanceof HandledScreen) == false ||
            Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName()))
        {
            return false;
        }

        HandledScreen<?> gui = (HandledScreen<?>) GuiUtils.getCurrentScreen();
        Slot slot = AccessorUtils.getSlotUnderMouse(gui);
        RecipeStorage recipes = RecipeStorage.getInstance();
        MoveAction moveAction = InputUtils.getDragMoveAction(key);

        if (slot != null)
        {
            if (moveAction != MoveAction.NONE)
            {
                final int mouseX = fi.dy.masa.malilib.util.InputUtils.getMouseX();
                final int mouseY = fi.dy.masa.malilib.util.InputUtils.getMouseY();
                return InventoryUtils.dragMoveItems(gui, moveAction, mouseX, mouseY, true);
            }
            else if (key == Hotkeys.KEY_MOVE_EVERYTHING.getKeybind())
            {
                InventoryUtils.tryMoveStacks(slot, gui, false, true, false);
                return true;
            }
            else if (key == Hotkeys.DROP_ALL_MATCHING.getKeybind())
            {
                if (Configs.Toggles.DROP_MATCHING.getBooleanValue() &&
                    Configs.GUI_BLACKLIST.contains(gui.getClass().getName()) == false &&
                    slot.hasStack())
                {
                    InventoryUtils.dropStacks(gui, slot.getStack(), slot, true);
                    return true;
                }
            }
        }

        if (key == Hotkeys.CRAFT_EVERYTHING.getKeybind())
        {
            InventoryUtils.craftEverythingPossibleWithCurrentRecipe(recipes.getSelectedRecipe(), gui);
            return true;
        }
        else if (key == Hotkeys.THROW_CRAFT_RESULTS.getKeybind())
        {
            InventoryUtils.throwAllCraftingResultsToGround(recipes.getSelectedRecipe(), gui);
            return true;
        }
        else if (key == Hotkeys.MOVE_CRAFT_RESULTS.getKeybind())
        {
            InventoryUtils.moveAllCraftingResultsToOtherInventory(recipes.getSelectedRecipe(), gui);
            return true;
        }
        else if (key == Hotkeys.STORE_RECIPE.getKeybind())
        {
            if (InputUtils.isRecipeViewOpen() && InventoryUtils.isCraftingSlot(gui, slot))
            {
                recipes.storeCraftingRecipeToCurrentSelection(slot, gui, true);
                return true;
            }
        }
        else if (key == Hotkeys.VILLAGER_TRADE_FAVORITES.getKeybind())
        {
            return InventoryUtils.villagerTradeEverythingPossibleWithAllFavoritedTrades();
        }
        else if (key == Hotkeys.SLOT_DEBUG.getKeybind())
        {
            if (slot != null)
            {
                InventoryUtils.debugPrintSlotInfo(gui, slot);
            }
            else
            {
                ItemScroller.logger.info("GUI class: {}", gui.getClass().getName());
            }

            return true;
        }
        else if (key == Hotkeys.SORT_INVENTORY.getKeybind())
        {
            if (Configs.Generic.SORT_INVENTORY_TOGGLE.getBooleanValue())
            {
                InventoryUtils.sortInventory(gui);
                return true;
            }
        }

        return false;
    }

    private static void debugPrintInv(RecipeInputInventory inv)
    {
        for (int i = 0; i < inv.getHeight(); i++)
        {
            for (int j = 0; j < inv.getWidth(); j++)
            {
                System.out.print(inv.getStack(i * inv.getWidth() + j) + " ");
            }
            System.out.println();
        }
    }

    @Override
    public void onClientTick(MinecraftClient mc)
    {
        if (InventoryUtils.dontUpdateRecipeBook > 0) {
            --InventoryUtils.dontUpdateRecipeBook;
        }
        if (this.functionalityEnabled() == false || mc.player == null)
        {
            return;
        }

        ClickPacketBuffer.sendBufferedPackets(Configs.Generic.PACKET_RATE_LIMIT.getIntegerValue());

        if (ClickPacketBuffer.shouldCancelWindowClicks())
        {
            return;
        }

        if (GuiUtils.getCurrentScreen() instanceof HandledScreen<?> gui &&
            (GuiUtils.getCurrentScreen() instanceof CreativeInventoryScreen) == false &&
            Configs.GUI_BLACKLIST.contains(GuiUtils.getCurrentScreen().getClass().getName()) == false &&
            Hotkeys.MASS_CRAFT.getKeybind().isKeybindHeld())
        {
            if (++this.massCraftTicker < Configs.Generic.MASS_CRAFT_INTERVAL.getIntegerValue())
            {
                return;
            }

            InventoryUtils.bufferInvUpdates = true;
            Slot outputSlot = CraftingHandler.getFirstCraftingOutputSlotForGui(gui);

            if (outputSlot != null)
            {
                if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
                {
                    ClickPacketBuffer.setShouldBufferClickPackets(true);
                }

                RecipePattern recipe = RecipeStorage.getInstance().getSelectedRecipe();

                int limit = Configs.Generic.MASS_CRAFT_ITERATIONS.getIntegerValue();

                if (Configs.Generic.MASS_CRAFT_RECIPE_BOOK.getBooleanValue() && recipe.lookupVanillaRecipe(mc.world) != null)
                {
                    InventoryUtils.dontUpdateRecipeBook = 2;
                    for (int i = 0; i < limit; ++i)
                    {
                        // todo
//                        InventoryUtils.tryClearCursor(gui);
//                        InventoryUtils.setInhibitCraftingOutputUpdate(true);
                        InventoryUtils.throwAllCraftingResultsToGround(recipe, gui);

                        RecipeInputInventory craftingInv = ((IMixinCraftingResultSlot) outputSlot).itemscroller_getCraftingInventory();
                        if (!recipe.getVanillaRecipe().matches(craftingInv.createRecipeInput(), mc.world))
                        {
                            CraftingHandler.SlotRange range = CraftingHandler.getCraftingGridSlots(gui, outputSlot);
                            final int invSlots = gui.getScreenHandler().slots.size();
                            final int rangeSlots = range.getSlotCount();

                            for (int j = 0, slotNum = range.getFirst(); j < rangeSlots && slotNum < invSlots; j++, slotNum++)
                            {
                                InventoryUtils.shiftClickSlot(gui, slotNum);

                                Slot slotTmp = gui.getScreenHandler().getSlot(slotNum);
                                ItemStack stack = slotTmp.getStack();
                                if (!stack.isEmpty())
                                {
                                    InventoryUtils.dropStack(gui, slotNum);
                                }
                            }
                        }

                        mc.interactionManager.clickRecipe(gui.getScreenHandler().syncId, recipe.getVanillaRecipeEntry(), true);
//                        InventoryUtils.setInhibitCraftingOutputUpdate(false);
//                        InventoryUtils.updateCraftingOutputSlot(outputSlot);

                        craftingInv = ((IMixinCraftingResultSlot) outputSlot).itemscroller_getCraftingInventory();
                        if (recipe.getVanillaRecipe().matches(craftingInv.createRecipeInput(), mc.world))
                        {
                            break;
                        }

                        InventoryUtils.shiftClickSlot(gui, outputSlot.id);
                        recipeBookClicks = true;
                    }
                }
                else if (Configs.Generic.MASS_CRAFT_SWAPS.getBooleanValue())
                {
                    for (int i = 0; i < limit; ++i)
                    {
                        InventoryUtils.tryClearCursor(gui);
                        InventoryUtils.setInhibitCraftingOutputUpdate(true);
                        InventoryUtils.throwAllCraftingResultsToGround(recipe, gui);
                        InventoryUtils.throwAllNonRecipeItemsToGround(recipe, gui);
                        RecipeInputInventory inv = ((IMixinCraftingResultSlot) (outputSlot)).itemscroller_getCraftingInventory();
                        //System.out.println("Before:");
                        //debugPrintInv(inv);
                        try
                        {
                            Thread.sleep(0);
                        } catch (InterruptedException e)
                        {
                        }
                        InventoryUtils.setCraftingGridContentsUsingSwaps(gui, mc.player.getInventory(), recipe, outputSlot);
                        //System.out.println("After:");
                        //debugPrintInv(inv);
                        InventoryUtils.setInhibitCraftingOutputUpdate(false);
                        InventoryUtils.updateCraftingOutputSlot(outputSlot);

                        //System.out.printf("Output slot: %s\n", outputSlot.getStack());

                        if (InventoryUtils.areStacksEqual(outputSlot.getStack(), recipe.getResult()) == false)
                        {
                            break;
                        }

                        InventoryUtils.shiftClickSlot(gui, outputSlot.id);
                        //System.out.println("Shift clicked");
                        //debugPrintInv(inv);
                    }
                }
                else
                {
                    int failsafe = 0;

                    while (++failsafe < limit)
                    {
                        InventoryUtils.tryClearCursor(gui);
                        InventoryUtils.setInhibitCraftingOutputUpdate(true);
                        InventoryUtils.throwAllCraftingResultsToGround(recipe, gui);
                        InventoryUtils.throwAllNonRecipeItemsToGround(recipe, gui);
                        InventoryUtils.tryMoveItemsToFirstCraftingGrid(recipe, gui, true);
                        InventoryUtils.setInhibitCraftingOutputUpdate(false);
                        InventoryUtils.updateCraftingOutputSlot(outputSlot);

                        if (InventoryUtils.areStacksEqual(outputSlot.getStack(), recipe.getResult()) == false)
                        {
                            break;
                        }

                        if (Configs.Generic.CARPET_CTRL_Q_CRAFTING.getBooleanValue())
                        {
                            InventoryUtils.dropStack(gui, outputSlot.id);
                        }
                        else
                        {
                            InventoryUtils.dropStacksWhileHasItem(gui, outputSlot.id, recipe.getResult());
                        }
                    }
                }

                ClickPacketBuffer.setShouldBufferClickPackets(false);
            }

            this.massCraftTicker = 0;
            InventoryUtils.bufferInvUpdates = false;
            InventoryUtils.invUpdatesBuffer.removeIf(packet -> {
                packet.apply(mc.getNetworkHandler());
                return true;
            });
        }
    }

    public void onPacket(ScreenHandlerSlotUpdateS2CPacket packet)
    {
        var mc = MinecraftClient.getInstance();
    }
}
