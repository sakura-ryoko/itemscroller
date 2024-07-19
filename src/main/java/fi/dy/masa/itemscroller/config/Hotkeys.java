package fi.dy.masa.itemscroller.config;

import java.util.List;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeybindSettings;

public class Hotkeys
{
    private static final KeybindSettings GUI_RELAXED = KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, true, false, false, false);
    private static final KeybindSettings GUI_RELAXED_CANCEL = KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, true, false, false, true);
    private static final KeybindSettings GUI_NO_ORDER = KeybindSettings.create(KeybindSettings.Context.GUI, KeyAction.PRESS, false, false, false, true);

    public static final ConfigHotkey OPEN_CONFIG_GUI            = new ConfigHotkey("openConfigGui",         "I,C", "itemscroller.config.hotkeys.comment.openConfigGui").translatedName("itemscroller.config.hotkeys.name.openConfigGui");

    public static final ConfigHotkey CRAFT_EVERYTHING           = new ConfigHotkey("craftEverything",       "LEFT_CONTROL,C", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.craftEverything").translatedName("itemscroller.config.hotkeys.name.craftEverything");
    public static final ConfigHotkey DROP_ALL_MATCHING          = new ConfigHotkey("dropAllMatching",       "LEFT_CONTROL,LEFT_SHIFT,Q", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.dropAllMatching").translatedName("itemscroller.config.hotkeys.name.dropAllMatching");
    public static final ConfigHotkey MASS_CRAFT                 = new ConfigHotkey("massCraft",             "LEFT_CONTROL,LEFT_ALT,C", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.massCraft").translatedName("itemscroller.config.hotkeys.name.massCraft");
    public static final ConfigHotkey MOVE_CRAFT_RESULTS         = new ConfigHotkey("moveCraftResults",      "LEFT_CONTROL,M", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.moveCraftResults").translatedName("itemscroller.config.hotkeys.name.moveCraftResults");
    public static final ConfigHotkey RECIPE_VIEW                = new ConfigHotkey("recipeView",            "A", GUI_RELAXED, "itemscroller.config.hotkeys.comment.recipeView").translatedName("itemscroller.config.hotkeys.name.recipeView");
    public static final ConfigHotkey SLOT_DEBUG                 = new ConfigHotkey("slotDebug",             "LEFT_CONTROL,LEFT_ALT,LEFT_SHIFT,I", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.slotDebug").translatedName("itemscroller.config.hotkeys.name.slotDebug");
    public static final ConfigHotkey STORE_RECIPE               = new ConfigHotkey("storeRecipe",           "BUTTON_3", GUI_RELAXED_CANCEL, "itemscroller.config.hotkeys.comment.storeRecipe").translatedName("itemscroller.config.hotkeys.name.storeRecipe");
    public static final ConfigHotkey THROW_CRAFT_RESULTS        = new ConfigHotkey("throwCraftResults",     "LEFT_CONTROL,T", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.throwCraftResults").translatedName("itemscroller.config.hotkeys.name.throwCraftResults");
    public static final ConfigHotkey TOGGLE_MOD_ON_OFF          = new ConfigHotkey("toggleModOnOff",        "", KeybindSettings.GUI, "itemscroller.config.hotkeys.comment.toggleModOnOff").translatedName("itemscroller.config.hotkeys.name.toggleModOnOff");
    public static final ConfigHotkey VILLAGER_TRADE_FAVORITES   = new ConfigHotkey("villagerTradeFavorites","", KeybindSettings.GUI, "itemscroller.config.hotkeys.comment.villagerTradeFavorites").translatedName("itemscroller.config.hotkeys.name.villagerTradeFavorites");

    public static final ConfigHotkey KEY_DRAG_DROP_LEAVE_ONE    = new ConfigHotkey("keyDragDropLeaveOne",   "LEFT_SHIFT,Q,BUTTON_2", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragDropLeaveOne").translatedName("itemscroller.config.hotkeys.name.keyDragDropLeaveOne");
    public static final ConfigHotkey KEY_DRAG_DROP_SINGLE       = new ConfigHotkey("keyDragDropSingle",     "Q,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragDropSingle").translatedName("itemscroller.config.hotkeys.name.keyDragDropSingle");
    public static final ConfigHotkey KEY_DRAG_DROP_STACKS       = new ConfigHotkey("keyDragDropStacks",     "LEFT_SHIFT,Q,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragDropStacks").translatedName("itemscroller.config.hotkeys.name.keyDragDropStacks");

    public static final ConfigHotkey KEY_DRAG_LEAVE_ONE         = new ConfigHotkey("keyDragMoveLeaveOne",   "LEFT_SHIFT,BUTTON_2", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragMoveLeaveOne").translatedName("itemscroller.config.hotkeys.name.keyDragMoveLeaveOne");
    public static final ConfigHotkey KEY_DRAG_MATCHING          = new ConfigHotkey("keyDragMoveMatching",   "LEFT_ALT,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragMoveMatching").translatedName("itemscroller.config.hotkeys.name.keyDragMoveMatching");
    public static final ConfigHotkey KEY_DRAG_MOVE_ONE          = new ConfigHotkey("keyDragMoveOne",        "LEFT_CONTROL,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragMoveOne").translatedName("itemscroller.config.hotkeys.name.keyDragMoveOne");
    public static final ConfigHotkey KEY_DRAG_FULL_STACKS       = new ConfigHotkey("keyDragMoveStacks",     "LEFT_SHIFT,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyDragMoveStacks").translatedName("itemscroller.config.hotkeys.name.keyDragMoveStacks");

    public static final ConfigHotkey KEY_MOVE_EVERYTHING        = new ConfigHotkey("keyMoveEverything",     "LEFT_ALT,LEFT_SHIFT,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.keyMoveEverything").translatedName("itemscroller.config.hotkeys.name.keyMoveEverything");

    public static final ConfigHotkey KEY_WS_MOVE_DOWN_LEAVE_ONE = new ConfigHotkey("wsMoveDownLeaveOne",    "S,BUTTON_2", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveDownLeaveOne").translatedName("itemscroller.config.hotkeys.name.wsMoveDownLeaveOne");
    public static final ConfigHotkey KEY_WS_MOVE_DOWN_MATCHING  = new ConfigHotkey("wsMoveDownMatching",    "LEFT_ALT,S,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveDownMatching").translatedName("itemscroller.config.hotkeys.name.wsMoveDownMatching");
    public static final ConfigHotkey KEY_WS_MOVE_DOWN_SINGLE    = new ConfigHotkey("wsMoveDownSingle",      "S,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveDownSingle").translatedName("itemscroller.config.hotkeys.name.wsMoveDownSingle");
    public static final ConfigHotkey KEY_WS_MOVE_DOWN_STACKS    = new ConfigHotkey("wsMoveDownStacks",      "LEFT_SHIFT,S,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveDownStacks").translatedName("itemscroller.config.hotkeys.name.wsMoveDownStacks");
    public static final ConfigHotkey KEY_WS_MOVE_UP_LEAVE_ONE   = new ConfigHotkey("wsMoveUpLeaveOne",      "W,BUTTON_2", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveUpLeaveOne").translatedName("itemscroller.config.hotkeys.name.wsMoveUpLeaveOne");
    public static final ConfigHotkey KEY_WS_MOVE_UP_MATCHING    = new ConfigHotkey("wsMoveUpMatching",      "LEFT_ALT,W,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveUpMatching").translatedName("itemscroller.config.hotkeys.name.wsMoveUpMatching");
    public static final ConfigHotkey KEY_WS_MOVE_UP_SINGLE      = new ConfigHotkey("wsMoveUpSingle",        "W,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveUpSingle").translatedName("itemscroller.config.hotkeys.name.wsMoveUpSingle");
    public static final ConfigHotkey KEY_WS_MOVE_UP_STACKS      = new ConfigHotkey("wsMoveUpStacks",        "LEFT_SHIFT,W,BUTTON_1", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.wsMoveUpStacks").translatedName("itemscroller.config.hotkeys.name.wsMoveUpStacks");

    public static final ConfigHotkey MODIFIER_MOVE_EVERYTHING   = new ConfigHotkey("modifierMoveEverything", "LEFT_ALT,LEFT_SHIFT", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.modifierMoveEverything").translatedName("itemscroller.config.hotkeys.name.modifierMoveEverything");
    public static final ConfigHotkey MODIFIER_MOVE_MATCHING     = new ConfigHotkey("modifierMoveMatching",   "LEFT_ALT", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.modifierMoveMatching").translatedName("itemscroller.config.hotkeys.name.modifierMoveMatching");
    public static final ConfigHotkey MODIFIER_MOVE_STACK        = new ConfigHotkey("modifierMoveStack",      "LEFT_SHIFT", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.modifierMoveStack").translatedName("itemscroller.config.hotkeys.name.modifierMoveStack");
    public static final ConfigHotkey MODIFIER_TOGGLE_VILLAGER_GLOBAL_FAVORITE = new ConfigHotkey("modifierToggleVillagerGlobalFavorite", "LEFT_SHIFT", GUI_RELAXED, "itemscroller.config.hotkeys.comment.modifierToggleVillagerGlobalFavorite").translatedName("itemscroller.config.hotkeys.name.modifierToggleVillagerGlobalFavorite");

    public static final ConfigHotkey SORT_INVENTORY             = new ConfigHotkey("sortInventory",         "R", GUI_NO_ORDER, "itemscroller.config.hotkeys.comment.sortInventory").translatedName("itemscroller.config.hotkeys.name.sortInventory");

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_CONFIG_GUI,
            TOGGLE_MOD_ON_OFF,

            CRAFT_EVERYTHING,
            DROP_ALL_MATCHING,
            MASS_CRAFT,
            MOVE_CRAFT_RESULTS,
            RECIPE_VIEW,
            SLOT_DEBUG,
            STORE_RECIPE,
            THROW_CRAFT_RESULTS,
            VILLAGER_TRADE_FAVORITES,

            MODIFIER_MOVE_EVERYTHING,
            MODIFIER_MOVE_MATCHING,
            MODIFIER_MOVE_STACK,
            MODIFIER_TOGGLE_VILLAGER_GLOBAL_FAVORITE,

            KEY_DRAG_FULL_STACKS,
            KEY_DRAG_LEAVE_ONE,
            KEY_DRAG_MATCHING,
            KEY_DRAG_MOVE_ONE,

            KEY_DRAG_DROP_LEAVE_ONE,
            KEY_DRAG_DROP_SINGLE,
            KEY_DRAG_DROP_STACKS,

            KEY_MOVE_EVERYTHING,

            KEY_WS_MOVE_DOWN_LEAVE_ONE,
            KEY_WS_MOVE_DOWN_MATCHING,
            KEY_WS_MOVE_DOWN_SINGLE,
            KEY_WS_MOVE_DOWN_STACKS,
            KEY_WS_MOVE_UP_LEAVE_ONE,
            KEY_WS_MOVE_UP_MATCHING,
            KEY_WS_MOVE_UP_SINGLE,
            KEY_WS_MOVE_UP_STACKS,

            SORT_INVENTORY
    );
}
