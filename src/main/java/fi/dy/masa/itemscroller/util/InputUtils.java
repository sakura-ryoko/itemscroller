package fi.dy.masa.itemscroller.util;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindMulti;
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
             if (key == Hotkeys.KEY_DRAG_FULL_STACKS.getKeybind())      { return MoveAction.MOVE_TO_OTHER_STACKS;       }
		else if (key == Hotkeys.KEY_DRAG_LEAVE_ONE.getKeybind())        { return MoveAction.MOVE_TO_OTHER_LEAVE_ONE;    }
        else if (key == Hotkeys.KEY_DRAG_MOVE_ONE.getKeybind())         { return MoveAction.MOVE_TO_OTHER_MOVE_ONE;     }
        else if (key == Hotkeys.KEY_DRAG_MATCHING.getKeybind())         { return MoveAction.MOVE_TO_OTHER_MATCHING;     }

        else if (key == Hotkeys.KEY_DRAG_DROP_STACKS.getKeybind())      { return MoveAction.DROP_STACKS;                }
        else if (key == Hotkeys.KEY_DRAG_DROP_LEAVE_ONE.getKeybind())   { return MoveAction.DROP_LEAVE_ONE;             }
        else if (key == Hotkeys.KEY_DRAG_DROP_SINGLE.getKeybind())      { return MoveAction.DROP_ONE;                   }

        else if (key == Hotkeys.KEY_WS_MOVE_UP_STACKS.getKeybind())     { return MoveAction.MOVE_UP_STACKS;             }
        else if (key == Hotkeys.KEY_WS_MOVE_UP_MATCHING.getKeybind())   { return MoveAction.MOVE_UP_MATCHING;           }
        else if (key == Hotkeys.KEY_WS_MOVE_UP_LEAVE_ONE.getKeybind())  { return MoveAction.MOVE_UP_LEAVE_ONE;          }
        else if (key == Hotkeys.KEY_WS_MOVE_UP_SINGLE.getKeybind())     { return MoveAction.MOVE_UP_MOVE_ONE;           }
        else if (key == Hotkeys.KEY_WS_MOVE_DOWN_STACKS.getKeybind())   { return MoveAction.MOVE_DOWN_STACKS;           }
        else if (key == Hotkeys.KEY_WS_MOVE_DOWN_MATCHING.getKeybind()) { return MoveAction.MOVE_DOWN_MATCHING;         }
        else if (key == Hotkeys.KEY_WS_MOVE_DOWN_LEAVE_ONE.getKeybind()){ return MoveAction.MOVE_DOWN_LEAVE_ONE;        }
        else if (key == Hotkeys.KEY_WS_MOVE_DOWN_SINGLE.getKeybind())   { return MoveAction.MOVE_DOWN_MOVE_ONE;         }

        return MoveAction.NONE;
    }

    public static boolean isActionKeyActive(MoveAction action)
    {
        switch (action)
        {
            case MOVE_TO_OTHER_STACKS:          return Hotkeys.KEY_DRAG_FULL_STACKS.getKeybind().isKeybindHeld();
            case MOVE_TO_OTHER_LEAVE_ONE:       return Hotkeys.KEY_DRAG_LEAVE_ONE.getKeybind().isKeybindHeld();
            case MOVE_TO_OTHER_MOVE_ONE:        return Hotkeys.KEY_DRAG_MOVE_ONE.getKeybind().isKeybindHeld();
            case MOVE_TO_OTHER_MATCHING:        return Hotkeys.KEY_DRAG_MATCHING.getKeybind().isKeybindHeld();
            case MOVE_TO_OTHER_EVERYTHING:      return Hotkeys.KEY_MOVE_EVERYTHING.getKeybind().isKeybindHeld();
            case DROP_STACKS:                   return Hotkeys.KEY_DRAG_DROP_STACKS.getKeybind().isKeybindHeld();
            case DROP_LEAVE_ONE:                return Hotkeys.KEY_DRAG_DROP_LEAVE_ONE.getKeybind().isKeybindHeld();
            case DROP_ONE:                      return Hotkeys.KEY_DRAG_DROP_SINGLE.getKeybind().isKeybindHeld();
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

	public static boolean isAttack(int keyCode, Minecraft mc)
	{
		return keyCode == KeybindMulti.getKeyCode(mc.options.keyAttack);
	}

	public static boolean isUse(int keyCode, Minecraft mc)
	{
		return keyCode == KeybindMulti.getKeyCode(mc.options.keyUse);
	}

	public static boolean isPickBlock(int keyCode, Minecraft mc)
	{
		return keyCode == KeybindMulti.getKeyCode(mc.options.keyPickItem);
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
