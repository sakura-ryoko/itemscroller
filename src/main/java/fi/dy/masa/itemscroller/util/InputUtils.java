package fi.dy.masa.itemscroller.util;

import javax.annotation.Nullable;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.sdl.SDLKeycode;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
import fi.dy.masa.malilib.mixin.input.IMixinKeyBinding;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.itemscroller.config.Hotkeys;
import fi.dy.masa.itemscroller.event.KeybindCallbacks;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;

public class InputUtils
{
    public static boolean isRecipeViewOpen()
    {
        return GuiUtils.getCurrentScreen() != null &&
               KeybindCallbacks.getInstance().functionalityEnabled() &&
	           Hotkeys.RECIPE_VIEW.getKeybind().isKeybindHeld() &&
               CraftingHandler.isCraftingGui(GuiUtils.getCurrentScreen());
    }

    public static boolean canShiftDropItems(AbstractContainerScreen<?> gui, Minecraft mc, int mouseX, int mouseY)
    {
        if (InventoryUtils.isStackEmpty(gui.getMenu().getCarried()) == false)
        {
            int left = AccessorUtils.getGuiLeft(gui);
            int top = AccessorUtils.getGuiTop(gui);
            int xSize = AccessorUtils.getGuiXSize(gui);
            int ySize = AccessorUtils.getGuiYSize(gui);
	        boolean isOutsideGui = mouseX < left || mouseY < top || mouseX >= left + xSize || mouseY >= top + ySize;

            return isOutsideGui && AccessorUtils.getSlotAtPosition(gui, mouseX - left, mouseY - top) == null;
        }

        return false;
    }

    public static MoveAction getDragMoveAction(IKeybind key)
    {
        if (key == Hotkeys.KEY_DRAG_FULL_STACKS.getKeybind())
        {
            return MoveAction.MOVE_TO_OTHER_STACKS;
        }
        else if (key == Hotkeys.KEY_DRAG_MOVE_ONE.getKeybind())
        {
            return MoveAction.MOVE_TO_OTHER_MOVE_ONE;
        }
        else if (key == Hotkeys.KEY_DRAG_LEAVE_ONE.getKeybind())
        {
            return MoveAction.MOVE_TO_OTHER_LEAVE_ONE;
        }
        else if (key == Hotkeys.KEY_DRAG_MATCHING.getKeybind())
        {
            return MoveAction.MOVE_TO_OTHER_MATCHING;
        }
        else if (key == Hotkeys.KEY_DRAG_DROP_STACKS.getKeybind())
        {
            return MoveAction.DROP_STACKS;
        }
        else if (key == Hotkeys.KEY_DRAG_DROP_LEAVE_ONE.getKeybind())
        {
            return MoveAction.DROP_LEAVE_ONE;
        }
        else if (key == Hotkeys.KEY_DRAG_DROP_SINGLE.getKeybind())
        {
            return MoveAction.DROP_ONE;
        }

        return MoveAction.NONE;
    }

    public static boolean hasShiftDown()
    {
        try
        {
            if ((SDLKeyboard.SDL_GetModState() & SDLKeycode.SDL_KMOD_SHIFT) != 0)
            {
                return true;
            }
        }
        catch (Throwable ignored)
        {
        }
        return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || (InputConstants.KEY_RSHIFT > 0 && InputConstants.isKeyDown(InputConstants.KEY_RSHIFT));
    }

    public static boolean hasControlDown()
    {
        try
        {
            if ((SDLKeyboard.SDL_GetModState() & SDLKeycode.SDL_KMOD_CTRL) != 0)
            {
                return true;
            }
        }
        catch (Throwable ignored)
        {
        }
        return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL) || (InputConstants.KEY_RCONTROL > 0 && InputConstants.isKeyDown(InputConstants.KEY_RCONTROL));
    }

    public static boolean hasAltDown()
    {
        try
        {
            if ((SDLKeyboard.SDL_GetModState() & SDLKeycode.SDL_KMOD_ALT) != 0)
            {
                return true;
            }
        }
        catch (Throwable ignored)
        {
        }
        return InputConstants.isKeyDown(InputConstants.KEY_LALT) || (InputConstants.KEY_RALT > 0 && InputConstants.isKeyDown(InputConstants.KEY_RALT));
    }

    public static boolean isLeftMouseButtonDown()
    {
        try
        {
            return (SDLMouse.nSDL_GetMouseState(0L, 0L) & SDLMouse.SDL_BUTTON_LMASK) != 0;
        }
        catch (Throwable ignored)
        {
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler != null && mc.mouseHandler.isLeftPressed();
    }

    public static boolean isRightMouseButtonDown()
    {
        try
        {
            return (SDLMouse.nSDL_GetMouseState(0L, 0L) & SDLMouse.SDL_BUTTON_RMASK) != 0;
        }
        catch (Throwable ignored)
        {
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.mouseHandler != null && mc.mouseHandler.isRightPressed();
    }

    public static boolean isDropKeyDown()
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && mc.options.keyDrop != null)
        {
            if (mc.options.keyDrop.isDown())
            {
                return true;
            }
            try
            {
                InputConstants.Key boundKey = ((IMixinKeyBinding) mc.options.keyDrop).malilib$getBoundKey();
                if (boundKey != null && boundKey.getValue() > 0)
                {
                    return InputConstants.isKeyDown(boundKey.getValue());
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return false;
    }

    public static boolean isActionKeyActive(MoveAction action)
    {
        switch (action)
        {
            case MOVE_TO_OTHER_STACKS:
                return Hotkeys.KEY_DRAG_FULL_STACKS.getKeybind().isKeybindHeld() ||
                       (hasShiftDown() && isLeftMouseButtonDown());

            case MOVE_TO_OTHER_LEAVE_ONE:
                return Hotkeys.KEY_DRAG_LEAVE_ONE.getKeybind().isKeybindHeld() ||
                       (hasShiftDown() && isRightMouseButtonDown());

            case MOVE_TO_OTHER_MOVE_ONE:
                return Hotkeys.KEY_DRAG_MOVE_ONE.getKeybind().isKeybindHeld() ||
                       (hasControlDown() && isLeftMouseButtonDown());

            case MOVE_TO_OTHER_MATCHING:
                return Hotkeys.KEY_DRAG_MATCHING.getKeybind().isKeybindHeld() ||
                       (hasAltDown() && isLeftMouseButtonDown());

            case MOVE_TO_OTHER_EVERYTHING:
                return Hotkeys.KEY_MOVE_EVERYTHING.getKeybind().isKeybindHeld();

            case DROP_STACKS:
                return Hotkeys.KEY_DRAG_DROP_STACKS.getKeybind().isKeybindHeld() ||
                       (hasShiftDown() && isLeftMouseButtonDown() && isDropKeyDown());

            case DROP_LEAVE_ONE:
                return Hotkeys.KEY_DRAG_DROP_LEAVE_ONE.getKeybind().isKeybindHeld() ||
                       (hasShiftDown() && isRightMouseButtonDown() && isDropKeyDown());

            case DROP_ONE:
                return Hotkeys.KEY_DRAG_DROP_SINGLE.getKeybind().isKeybindHeld() ||
                       (isLeftMouseButtonDown() && isDropKeyDown());

            case MOVE_UP_STACKS:                return Hotkeys.KEY_WS_MOVE_UP_STACKS.getKeybind().isKeybindHeld();
            case MOVE_UP_MATCHING:              return Hotkeys.KEY_WS_MOVE_UP_MATCHING.getKeybind().isKeybindHeld();
            case MOVE_UP_LEAVE_ONE:             return Hotkeys.KEY_WS_MOVE_UP_LEAVE_ONE.getKeybind().isKeybindHeld();
            case MOVE_UP_MOVE_ONE:              return Hotkeys.KEY_WS_MOVE_UP_SINGLE.getKeybind().isKeybindHeld();
            case MOVE_DOWN_STACKS:              return Hotkeys.KEY_WS_MOVE_DOWN_STACKS.getKeybind().isKeybindHeld();
            case MOVE_DOWN_MATCHING:            return Hotkeys.KEY_WS_MOVE_DOWN_MATCHING.getKeybind().isKeybindHeld();
            case MOVE_DOWN_LEAVE_ONE:           return Hotkeys.KEY_WS_MOVE_DOWN_LEAVE_ONE.getKeybind().isKeybindHeld();
            case MOVE_DOWN_MOVE_ONE:            return Hotkeys.KEY_WS_MOVE_DOWN_SINGLE.getKeybind().isKeybindHeld();
            default:
        }

        return false;
    }

    public static MoveAmount getMoveAmount(MoveAction action)
    {
        switch (action)
        {
            case SCROLL_TO_OTHER_MOVE_ONE:
            case MOVE_TO_OTHER_MOVE_ONE:
            case DROP_ONE:
            case MOVE_DOWN_MOVE_ONE:
            case MOVE_UP_MOVE_ONE:
                return MoveAmount.MOVE_ONE;

            case MOVE_TO_OTHER_LEAVE_ONE:
            case DROP_LEAVE_ONE:
            case MOVE_DOWN_LEAVE_ONE:
            case MOVE_UP_LEAVE_ONE:
                return MoveAmount.LEAVE_ONE;

            case SCROLL_TO_OTHER_STACKS:
            case MOVE_TO_OTHER_STACKS:
            case DROP_STACKS:
            case MOVE_DOWN_STACKS:
            case MOVE_UP_STACKS:
                return MoveAmount.FULL_STACKS;

            case SCROLL_TO_OTHER_MATCHING:
            case MOVE_TO_OTHER_MATCHING:
            case DROP_ALL_MATCHING:
            case MOVE_UP_MATCHING:
            case MOVE_DOWN_MATCHING:
                return MoveAmount.ALL_MATCHING;

            case MOVE_TO_OTHER_EVERYTHING:
            case SCROLL_TO_OTHER_EVERYTHING:
                return MoveAmount.EVERYTHING;

            default:
        }

        return MoveAmount.NONE;
    }

	public static boolean isAttack(int scanCode, Minecraft mc)
	{
		return scanCode == KeybindMulti.getKeyCode(mc.options.keyAttack);
	}

	public static boolean isUse(int scanCode, Minecraft mc)
	{
		return scanCode == KeybindMulti.getKeyCode(mc.options.keyUse);
	}

	public static boolean isPickBlock(int scanCode, Minecraft mc)
	{
		return scanCode == KeybindMulti.getKeyCode(mc.options.keyPickItem);
	}

	public static boolean isAttack(@Nullable MouseButtonEvent click, @Nullable KeyEvent input, Minecraft mc)
    {
	    if (click != null && mc.options.keyAttack.matchesMouse(click))
	    {
			return true;
	    }
		else return input != null && mc.options.keyAttack.matches(input);
    }

    public static boolean isUse(@Nullable MouseButtonEvent click, @Nullable KeyEvent input, Minecraft mc)
    {
	    if (click != null && mc.options.keyUse.matchesMouse(click))
	    {
		    return true;
	    }
	    else return input != null && mc.options.keyUse.matches(input);
    }

    public static boolean isPickBlock(@Nullable MouseButtonEvent click, @Nullable KeyEvent input, Minecraft mc)
    {
	    if (click != null && mc.options.keyPickItem.matchesMouse(click))
	    {
		    return true;
	    }
	    else return input != null && mc.options.keyPickItem.matches(input);
    }
}
