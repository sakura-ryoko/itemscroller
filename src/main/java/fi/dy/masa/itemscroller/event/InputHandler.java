package fi.dy.masa.itemscroller.event;

import org.lwjgl.glfw.GLFW;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.*;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.KeyCodes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.*;
import fi.dy.masa.itemscroller.villager.VillagerDataStorage;

public class InputHandler implements IKeybindProvider, IKeyboardInputHandler, IMouseInputHandler
{
    private final KeybindCallbacks callbacks;

    public InputHandler()
    {
        this.callbacks = KeybindCallbacks.getInstance();
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(Reference.MOD_NAME, "itemscroller.hotkeys.category.hotkeys", Hotkeys.HOTKEY_LIST);
    }

    @Override
    public boolean onKeyInput(KeyEvent input, boolean eventKeyState)
    {
        if (InputUtils.isRecipeViewOpen() && eventKeyState)
        {
            int index = -1;
            RecipeStorage recipes = RecipeStorage.getInstance();
            int oldIndex = recipes.getSelection();
            int recipesPerPage = recipes.getRecipeCountPerPage();
//            int recipeIndexChange = GuiBase.isShiftDown() ? recipesPerPage : recipesPerPage / 2;
	        int recipeIndexChange = (input.hasShiftDown() || GuiBase.isShiftDown()) ? recipesPerPage : recipesPerPage / 2;

            if (input.key() >= KeyCodes.KEY_1 && input.key() <= KeyCodes.KEY_9)
            {
                index = Mth.clamp(input.key() - GLFW.GLFW_KEY_1, 0, 8);
            }
            else if (input.key() == KeyCodes.KEY_UP && oldIndex > 0)
            {
                index = oldIndex - 1;
            }
            else if (input.key() == KeyCodes.KEY_DOWN && oldIndex < (recipes.getTotalRecipeCount() - 1))
            {
                index = oldIndex + 1;
            }
            else if (input.key() == KeyCodes.KEY_LEFT && oldIndex >= recipeIndexChange)
            {
                index = oldIndex - recipeIndexChange;
            }
            else if (input.key() == KeyCodes.KEY_RIGHT && oldIndex < (recipes.getTotalRecipeCount() - recipeIndexChange))
            {
                index = oldIndex + recipeIndexChange;
            }

            if (index >= 0)
            {
                recipes.changeSelectedRecipe(index);
                return true;
            }
        }

        return this.handleInput(input.key(), eventKeyState, 0);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount)
    {
//        return this.handleInput(null, null, KeyCodes.KEY_NONE, false, amount);
	    return this.handleInput(KeyCodes.KEY_NONE, false, amount);
    }

    @Override
    public boolean onMouseClick(MouseButtonEvent click, boolean eventButtonState)
    {
//        return this.handleInput(click,null, click.getKeycode() - 100, eventButtonState, 0);
//	    return this.handleInput(new Click(click.x(), click.y(), new MouseInput(click.getKeycode() - 100, click.modifiers())), null, eventButtonState, 0);
	    return this.handleInput(click.input() - 100, eventButtonState, 0);
    }

    private boolean handleInput(int keyCode, boolean keyState, double dWheel)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null)
        {
            return false;
        }

        if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue() &&
            this.callbacks.functionalityEnabled())
        {
            ClickPacketBuffer.setShouldBufferClickPackets(true);
        }

        boolean cancel = this.handleInputImpl(keyCode, keyState, dWheel, mc);

        ClickPacketBuffer.setShouldBufferClickPackets(false);

        return cancel;
    }

    private boolean handleInputImpl(int keyCode, boolean keyState, double dWheel, Minecraft mc)
    {
        MoveAction action = InventoryUtils.getActiveMoveAction();

        if (action != MoveAction.NONE && InputUtils.isActionKeyActive(action) == false)
        {
            InventoryUtils.stopDragging();
        }

        boolean cancel = false;

        if (this.callbacks.functionalityEnabled() && mc.player != null)
        {
            final boolean isAttack = InputUtils.isAttack(keyCode, mc);
            final boolean isUse = InputUtils.isUse(keyCode, mc);
            final boolean isPickBlock = InputUtils.isPickBlock(keyCode, mc);
            final boolean isAttackUseOrPick = isAttack || isUse || isPickBlock;
            final int mouseX = fi.dy.masa.malilib.util.InputUtils.getMouseX();
            final int mouseY = fi.dy.masa.malilib.util.InputUtils.getMouseY();

            if (Configs.Toggles.VILLAGER_TRADE_FEATURES.getBooleanValue())
            {
                VillagerDataStorage storage = VillagerDataStorage.getInstance();

                if (mc.screen == null && mc.hitResult != null &&
                    mc.hitResult.getType() == HitResult.Type.ENTITY &&
                    ((EntityHitResult) mc.hitResult).getEntity() instanceof AbstractVillager)
                {
                    storage.setLastInteractedUUID(((EntityHitResult) mc.hitResult).getEntity().getUUID());
                }
            }

            if (mc.screen instanceof AbstractContainerScreen<?> gui &&
                (mc.screen instanceof CreativeModeInventoryScreen) == false &&
                Configs.GUI_BLACKLIST.contains(mc.screen.getClass().getName()) == false)
            {
	            RecipeStorage recipes = RecipeStorage.getInstance();

                if (dWheel != 0)
                {
                    // When scrolling while the recipe view is open, change the selection instead of moving items
                    if (InputUtils.isRecipeViewOpen())
                    {
                        recipes.scrollSelection(dWheel < 0);
                        cancel = true;
                    }
                    else if (!InventoryUtils.ignoreScrollingInsideOfBundles)
                    {
                        cancel = InventoryUtils.tryMoveItems(gui, recipes, dWheel > 0);
                    }
                }
                else
                {
                    Slot slot = AccessorUtils.getSlotUnderMouse(gui);
                    final boolean isShiftDown = GuiBase.isShiftDown();

                    if (keyState && isAttackUseOrPick)
                    {
                        int hoveredRecipeId = RenderEventHandler.instance().getHoveredRecipeId(mouseX, mouseY, recipes, gui);

                        // Hovering over an item in the recipe view
                        if (hoveredRecipeId >= 0)
                        {
                            InventoryUtils.handleRecipeClick(gui, mc, recipes, hoveredRecipeId, isAttack, isUse, isPickBlock, isShiftDown);
                            return true;
                        }
                        // Pick-blocking over a crafting output slot with the recipe view open, store the recipe
                        else if (isPickBlock && InputUtils.isRecipeViewOpen() && InventoryUtils.isCraftingSlot(gui, slot))
                        {
                            //System.out.print("handleInputImpl()\n");
                            recipes.storeCraftingRecipeToCurrentSelection(slot, gui, true, false, mc);
                            cancel = true;
                        }
                    }

                    InventoryUtils.checkForItemPickup(gui);

                    if (keyState && (isAttack || isUse))
                    {
                        InventoryUtils.storeSourceSlotCandidate(slot, gui);
                    }

                    if (Configs.Toggles.RIGHT_CLICK_CRAFT_STACK.getBooleanValue() &&
                        isUse && keyState &&
                        InventoryUtils.isCraftingSlot(gui, slot))
                    {
                        InventoryUtils.rightClickCraftOneStack(gui);
                    }
                    else if (Configs.Toggles.SHIFT_PLACE_ITEMS.getBooleanValue() &&
                             isAttack && isShiftDown &&
                             InventoryUtils.canShiftPlaceItems(gui) && slot != null)
                    {
                        cancel |= InventoryUtils.shiftPlaceItems(slot, gui);
                    }
                    else if (Configs.Toggles.SHIFT_DROP_ITEMS.getBooleanValue() &&
                             isAttack && isShiftDown &&
                             InputUtils.canShiftDropItems(gui, mc, mouseX, mouseY))
                    {
                        cancel |= InventoryUtils.shiftDropItems(gui);
                    }
                }
            }
        }

        return cancel;
    }

    @Override
    public void onMouseMove(double mouseX, double mouseY)
    {
        Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;

        if (this.callbacks.functionalityEnabled() &&
            mc.player != null &&
            GuiUtils.getCurrentScreen() instanceof AbstractContainerScreen<?> screen &&
            Configs.GUI_BLACKLIST.contains(screen.getClass().getName()) == false)
        {
            this.handleDragging(screen, mc, (int) mouseX, (int) mouseY, false);
        }
    }

    private boolean handleDragging(AbstractContainerScreen<?> gui, Minecraft mc, int mouseX, int mouseY, boolean isClick)
    {
        MoveAction action = InventoryUtils.getActiveMoveAction();
        boolean cancel = false;

        if (Configs.Generic.RATE_LIMIT_CLICK_PACKETS.getBooleanValue())
        {
            ClickPacketBuffer.setShouldBufferClickPackets(true);
        }

        if (InputUtils.isActionKeyActive(action))
        {
            cancel = InventoryUtils.dragMoveItems(gui, action, mouseX, mouseY, false);
        }
        else if (action != MoveAction.NONE)
        {
            InventoryUtils.stopDragging();
        }

        ClickPacketBuffer.setShouldBufferClickPackets(false);

        return cancel;
    }
}
