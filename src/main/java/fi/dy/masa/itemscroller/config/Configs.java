package fi.dy.masa.itemscroller.config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.screen.ingame.CraftingScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.screen.slot.CraftingResultSlot;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.config.options.*;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.recipes.CraftingHandler;
import fi.dy.masa.itemscroller.recipes.CraftingHandler.SlotRange;
import fi.dy.masa.itemscroller.util.SortingCategory;
import fi.dy.masa.itemscroller.util.SortingMethod;

public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    private static final ImmutableList<String> DEFAULT_TOP_SORTING = ImmutableList.of("minecraft:diamond_sword","minecraft:diamond_pickaxe","minecraft:diamond_axe","minecraft:diamond_shovel","minecraft:diamond_hoe","minecraft:netherite_sword","minecraft:netherite_pickaxe","minecraft:netherite_axe","minecraft:netherite_shovel","minecraft:netherite_hoe");
    private static final ImmutableList<String> DEFAULT_BOTTOM_SORTING = ImmutableList.of();

    public static class Generic
    {
        public static final ConfigBoolean CARPET_CTRL_Q_CRAFTING                = new ConfigBoolean("carpetCtrlQCraftingEnabledOnServer",   false, "itemscroller.config.generic.comment.carpetCtrlQCraftingEnabledOnServer").translatedName("itemscroller.config.generic.name.carpetCtrlQCraftingEnabledOnServer");
        public static final ConfigBoolean CLIENT_CRAFTING_FIX                   = new ConfigBoolean("clientCraftingFixOn1.12",              true, "itemscroller.config.generic.comment.clientCraftingFixOn1_12").translatedName("itemscroller.config.generic.name.clientCraftingFixOn1_12");
        public static final ConfigBoolean CRAFTING_RENDER_RECIPE_ITEMS          = new ConfigBoolean("craftingRenderRecipeItems",            true, "itemscroller.config.generic.comment.craftingRenderRecipeItems").translatedName("itemscroller.config.generic.name.craftingRenderRecipeItems");
        //public static final ConfigBoolean DEBUG_MESSAGES                        = new ConfigBoolean("debugMessages",                        false, "itemscroller.config.generic.comment.debugMessages").translatedName("itemscroller.config.generic.name.debugMessages");
        public static final ConfigBoolean MOD_MAIN_TOGGLE                       = new ConfigBoolean("modMainToggle",                        true, "itemscroller.config.generic.comment.modMainToggle").translatedName("itemscroller.config.generic.name.modMainToggle");
        public static final ConfigBoolean MASS_CRAFT_INHIBIT_MID_UPDATES        = new ConfigBoolean("massCraftInhibitMidUpdates",           true, "itemscroller.config.generic.comment.massCraftInhibitMidUpdates").translatedName("itemscroller.config.generic.name.massCraftInhibitMidUpdates");
        public static final ConfigInteger MASS_CRAFT_INTERVAL                   = new ConfigInteger("massCraftInterval",                    2, 1, 60, "itemscroller.config.generic.comment.massCraftInterval").translatedName("itemscroller.config.generic.name.massCraftInterval");
        public static final ConfigInteger MASS_CRAFT_ITERATIONS                 = new ConfigInteger("massCraftIterations",                  36, 1, 256, "itemscroller.config.generic.comment.massCraftIterations").translatedName("itemscroller.config.generic.name.massCraftIterations");
        public static final ConfigBoolean MASS_CRAFT_SWAPS                      = new ConfigBoolean("massCraftSwapsOnly",                   false, "itemscroller.config.generic.comment.massCraftSwapsOnly").translatedName("itemscroller.config.generic.name.massCraftSwapsOnly");
        public static final ConfigBoolean MASS_CRAFT_RECIPE_BOOK                = new ConfigBoolean("massCraftUseRecipeBook", false, "itemscroller.config.generic.comment.massCraftUseRecipeBook").translatedName("itemscroller.config.generic.name.massCraftUseRecipeBook");
        public static final ConfigInteger PACKET_RATE_LIMIT                     = new ConfigInteger("packetRateLimit",                      4, 1, 1024, "itemscroller.config.generic.comment.packetRateLimit").translatedName("itemscroller.config.generic.name.packetRateLimit");
        public static final ConfigBoolean SCROLL_CRAFT_STORE_RECIPES_TO_FILE    = new ConfigBoolean("craftingRecipesSaveToFile",            true, "itemscroller.config.generic.comment.craftingRecipesSaveToFile").translatedName("itemscroller.config.generic.name.craftingRecipesSaveToFile");
        public static final ConfigBoolean SCROLL_CRAFT_RECIPE_FILE_GLOBAL       = new ConfigBoolean("craftingRecipesSaveFileIsGlobal",      false, "itemscroller.config.generic.comment.craftingRecipesSaveFileIsGlobal").translatedName("itemscroller.config.generic.name.craftingRecipesSaveFileIsGlobal");
        public static final ConfigBoolean RATE_LIMIT_CLICK_PACKETS              = new ConfigBoolean("rateLimitClickPackets",                false, "itemscroller.config.generic.comment.rateLimitClickPackets").translatedName("itemscroller.config.generic.name.rateLimitClickPackets");
        public static final ConfigBoolean REVERSE_SCROLL_DIRECTION_SINGLE       = new ConfigBoolean("reverseScrollDirectionSingle",         false, "itemscroller.config.generic.comment.reverseScrollDirectionSingle").translatedName("itemscroller.config.generic.name.reverseScrollDirectionSingle");
        public static final ConfigBoolean REVERSE_SCROLL_DIRECTION_STACKS       = new ConfigBoolean("reverseScrollDirectionStacks",         false, "itemscroller.config.generic.comment.reverseScrollDirectionStacks").translatedName("itemscroller.config.generic.name.reverseScrollDirectionStacks");
        public static final ConfigBoolean USE_RECIPE_CACHING                    = new ConfigBoolean("useRecipeCaching",                     true, "itemscroller.config.generic.comment.useRecipeCaching").translatedName("itemscroller.config.generic.name.useRecipeCaching");
        public static final ConfigBoolean SLOT_POSITION_AWARE_SCROLL_DIRECTION  = new ConfigBoolean("useSlotPositionAwareScrollDirection",  false, "itemscroller.config.generic.comment.useSlotPositionAwareScrollDirection").translatedName("itemscroller.config.generic.name.useSlotPositionAwareScrollDirection");
        public static final ConfigBoolean VILLAGER_TRADE_USE_GLOBAL_FAVORITES   = new ConfigBoolean("villagerTradeUseGlobalFavorites",      true, "itemscroller.config.generic.comment.villagerTradeUseGlobalFavorites").translatedName("itemscroller.config.generic.name.villagerTradeUseGlobalFavorites");
        public static final ConfigBoolean VILLAGER_TRADE_LIST_REMEMBER_SCROLL   = new ConfigBoolean("villagerTradeListRememberScrollPosition", true, "itemscroller.config.generic.comment.villagerTradeListRememberScrollPosition").translatedName("itemscroller.config.generic.name.villagerTradeListRememberScrollPosition");

        public static final ConfigBoolean SORT_INVENTORY_TOGGLE                 = new ConfigBoolean("sortInventoryToggle",                  false, "itemscroller.config.generic.comment.sortInventoryToggle").translatedName("itemscroller.config.generic.name.sortInventoryToggle");
        public static final ConfigBoolean SORT_ASSUME_EMPTY_BOX_STACKS          = new ConfigBoolean("sortAssumeEmptyBoxStacks",             true, "itemscroller.config.generic.comment.sortAssumeEmptyBoxStacks").translatedName("itemscroller.config.generic.name.sortAssumeEmptyBoxStacks");
        public static final ConfigBoolean SORT_SHULKER_BOXES_AT_END             = new ConfigBoolean("sortShulkerBoxesAtEnd",                true, "itemscroller.config.generic.comment.sortShulkerBoxesAtEnd").translatedName("itemscroller.config.generic.name.sortShulkerBoxesAtEnd");
        public static final ConfigBoolean SORT_SHULKER_BOXES_INVERTED           = new ConfigBoolean("sortShulkerBoxesInverted",             false, "itemscroller.config.generic.comment.sortShulkerBoxesInverted").translatedName("itemscroller.config.generic.name.sortShulkerBoxesInverted");
        public static final ConfigBoolean SORT_BUNDLES_AT_END                   = new ConfigBoolean("sortBundlesAtEnd",                     true, "itemscroller.config.generic.comment.sortBundlesAtEnd").translatedName("itemscroller.config.generic.name.sortBundlesAtEnd");
        public static final ConfigBoolean SORT_BUNDLES_INVERTED                 = new ConfigBoolean("sortBundlesInverted",                  false, "itemscroller.config.generic.comment.sortBundlesInverted").translatedName("itemscroller.config.generic.name.sortBundlesInverted");
        public static final ConfigStringList SORT_TOP_PRIORITY_INVENTORY        = new ConfigStringList("sortTopPriorityInventory",           DEFAULT_TOP_SORTING, "itemscroller.config.generic.comment.sortTopPriorityInventory").translatedName("itemscroller.config.generic.name.sortTopPriorityInventory");
        public static final ConfigStringList SORT_BOTTOM_PRIORITY_INVENTORY     = new ConfigStringList("sortBottomPriorityInventory",        DEFAULT_BOTTOM_SORTING, "itemscroller.config.generic.comment.sortBottomPriorityInventory").translatedName("itemscroller.config.generic.name.sortBottomPriorityInventory");
        public static final ConfigOptionList SORT_METHOD_DEFAULT                = new ConfigOptionList("sortMethodDefault",                  SortingMethod.CATEGORY_NAME, "itemscroller.config.generic.comment.sortMethodDefault").translatedName("itemscroller.config.generic.name.sortMethodDefault");
        public static final ConfigLockedList SORT_CATEGORY_ORDER                = new ConfigLockedList("sortCategoryOrder",                  SortingCategory.INSTANCE, "itemscroller.config.generic.comment.sortCategoryOrder").translatedName("itemscroller.config.generic.name.sortCategoryOrder");

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                CARPET_CTRL_Q_CRAFTING,
                CLIENT_CRAFTING_FIX,
                CRAFTING_RENDER_RECIPE_ITEMS,
                //DEBUG_MESSAGES,
                MASS_CRAFT_INHIBIT_MID_UPDATES,
                MASS_CRAFT_INTERVAL,
                MASS_CRAFT_ITERATIONS,
                MASS_CRAFT_SWAPS,
                MASS_CRAFT_RECIPE_BOOK,
                MOD_MAIN_TOGGLE,
                PACKET_RATE_LIMIT,
                RATE_LIMIT_CLICK_PACKETS,
                SCROLL_CRAFT_STORE_RECIPES_TO_FILE,
                SCROLL_CRAFT_RECIPE_FILE_GLOBAL,
                REVERSE_SCROLL_DIRECTION_SINGLE,
                REVERSE_SCROLL_DIRECTION_STACKS,
                SLOT_POSITION_AWARE_SCROLL_DIRECTION,
                USE_RECIPE_CACHING,
                VILLAGER_TRADE_USE_GLOBAL_FAVORITES,
                VILLAGER_TRADE_LIST_REMEMBER_SCROLL,

                SORT_INVENTORY_TOGGLE,
                SORT_ASSUME_EMPTY_BOX_STACKS,
                SORT_SHULKER_BOXES_AT_END,
                SORT_SHULKER_BOXES_INVERTED,
                SORT_BUNDLES_AT_END,
                SORT_BUNDLES_INVERTED,
                SORT_TOP_PRIORITY_INVENTORY,
                SORT_BOTTOM_PRIORITY_INVENTORY,
                SORT_METHOD_DEFAULT,
                SORT_CATEGORY_ORDER
        );
    }

    public static class Toggles
    {
        public static final ConfigBoolean CRAFTING_FEATURES         = new ConfigBoolean("enableCraftingFeatures",           true, "itemscroller.config.toggles.comment.enableCraftingFeatures").translatedName("itemscroller.config.toggles.name.enableCraftingFeatures");
        public static final ConfigBoolean DROP_MATCHING             = new ConfigBoolean("enableDropkeyDropMatching",        true, "itemscroller.config.toggles.comment.enableDropkeyDropMatching").translatedName("itemscroller.config.toggles.name.enableDropkeyDropMatching");
        public static final ConfigBoolean RIGHT_CLICK_CRAFT_STACK   = new ConfigBoolean("enableRightClickCraftingOneStack", true, "itemscroller.config.toggles.comment.enableRightClickCraftingOneStack").translatedName("itemscroller.config.toggles.name.enableRightClickCraftingOneStack");
        public static final ConfigBoolean SCROLL_EVERYTHING         = new ConfigBoolean("enableScrollingEverything",        true, "itemscroller.config.toggles.comment.enableScrollingEverything").translatedName("itemscroller.config.toggles.name.enableScrollingEverything");
        public static final ConfigBoolean SCROLL_MATCHING           = new ConfigBoolean("enableScrollingMatchingStacks",    true, "itemscroller.config.toggles.comment.enableScrollingMatchingStacks").translatedName("itemscroller.config.toggles.name.enableScrollingMatchingStacks");
        public static final ConfigBoolean SCROLL_SINGLE             = new ConfigBoolean("enableScrollingSingle",            true, "itemscroller.config.toggles.comment.enableScrollingSingle").translatedName("itemscroller.config.toggles.name.enableScrollingSingle");
        public static final ConfigBoolean SCROLL_STACKS             = new ConfigBoolean("enableScrollingStacks",            true, "itemscroller.config.toggles.comment.enableScrollingStacks").translatedName("itemscroller.config.toggles.name.enableScrollingStacks");
        public static final ConfigBoolean SCROLL_STACKS_FALLBACK    = new ConfigBoolean("enableScrollingStacksFallback",    true, "itemscroller.config.toggles.comment.enableScrollingStacksFallback").translatedName("itemscroller.config.toggles.name.enableScrollingStacksFallback");
        public static final ConfigBoolean SCROLL_VILLAGER           = new ConfigBoolean("enableScrollingVillager",          true, "itemscroller.config.toggles.comment.enableScrollingVillager").translatedName("itemscroller.config.toggles.name.enableScrollingVillager");
        public static final ConfigBoolean SHIFT_DROP_ITEMS          = new ConfigBoolean("enableShiftDropItems",             true, "itemscroller.config.toggles.comment.enableShiftDropItems").translatedName("itemscroller.config.toggles.name.enableShiftDropItems");
        public static final ConfigBoolean SHIFT_PLACE_ITEMS         = new ConfigBoolean("enableShiftPlaceItems",            true, "itemscroller.config.toggles.comment.enableShiftPlaceItems").translatedName("itemscroller.config.toggles.name.enableShiftPlaceItems");
        public static final ConfigBoolean VILLAGER_TRADE_FEATURES   = new ConfigBoolean("enableVillagerTradeFeatures",      true, "itemscroller.config.toggles.comment.enableVillagerTradeFeatures").translatedName("itemscroller.config.toggles.name.enableVillagerTradeFeatures");

        public static final ImmutableList<IConfigValue> OPTIONS = ImmutableList.of(
                CRAFTING_FEATURES,
                DROP_MATCHING,
                RIGHT_CLICK_CRAFT_STACK,
                SCROLL_EVERYTHING,
                SCROLL_MATCHING,
                SCROLL_SINGLE,
                SCROLL_STACKS,
                SCROLL_STACKS_FALLBACK,
                SCROLL_VILLAGER,
                SHIFT_DROP_ITEMS,
                SHIFT_PLACE_ITEMS,
                VILLAGER_TRADE_FEATURES
        );
    }

    public static final Set<String> GUI_BLACKLIST = new HashSet<>();
    public static final Set<String> SLOT_BLACKLIST = new HashSet<>();

    public static void loadFromFile()
    {
        File configFile = new File(FileUtils.getConfigDirectory(), CONFIG_FILE_NAME);

        if (configFile.exists() && configFile.isFile() && configFile.canRead())
        {
            JsonElement element = JsonUtils.parseJsonFile(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Generic", Generic.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
                ConfigUtils.readConfigBase(root, "Toggles", Toggles.OPTIONS);

                getStrings(root, GUI_BLACKLIST, "guiBlacklist");
                getStrings(root, SLOT_BLACKLIST, "slotBlacklist");
            }
        }

        CraftingHandler.clearDefinitions();

        // "net.minecraft.client.gui.inventory.GuiCrafting,net.minecraft.inventory.SlotCrafting,0,1-9", // vanilla Crafting Table
        CraftingHandler.addCraftingGridDefinition(CraftingScreen.class.getName(), CraftingResultSlot.class.getName(), 0, new SlotRange(1, 9));
        //"net.minecraft.client.gui.inventory.PlayerInventoryScreen,net.minecraft.inventory.SlotCrafting,0,1-4", // vanilla player inventory crafting grid
        CraftingHandler.addCraftingGridDefinition(InventoryScreen.class.getName(), CraftingResultSlot.class.getName(), 0, new SlotRange(1, 4));
    }

    public static void saveToFile()
    {
        File dir = FileUtils.getConfigDirectory();

        if ((dir.exists() && dir.isDirectory()) || dir.mkdirs())
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Generic", Generic.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            ConfigUtils.writeConfigBase(root, "Toggles", Toggles.OPTIONS);

            writeStrings(root, GUI_BLACKLIST, "guiBlacklist");
            writeStrings(root, SLOT_BLACKLIST, "slotBlacklist");

            JsonUtils.writeJsonToFile(root, new File(dir, CONFIG_FILE_NAME));
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }

    private static void getStrings(JsonObject obj, Set<String> outputSet, String arrayName)
    {
        outputSet.clear();

        if (JsonUtils.hasArray(obj, arrayName))
        {
            JsonArray arr = obj.getAsJsonArray(arrayName);
            final int size = arr.size();

            for (int i = 0; i < size; i++)
            {
                outputSet.add(arr.get(i).getAsString());
            }
        }
    }

    private static void writeStrings(JsonObject obj, Set<String> inputSet, String arrayName)
    {
        if (inputSet.isEmpty() == false)
        {
            JsonArray arr = new JsonArray();

            for (String str : inputSet)
            {
                arr.add(str);
            }

            obj.add(arrayName, arr);
        }
    }
}
