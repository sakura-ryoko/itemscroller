package fi.dy.masa.itemscroller.util;

import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.villager.VillagerDataStorage;

/**
 * New ItemScroller Data Manager
 * Manages both Villager and Recipe Data Storage components in a similar manner
 * to Masa's other mods.
 */
public class DataManager
{
    private static final DataManager INSTANCE = new DataManager();
    private DynamicRegistryManager registryManager;
    private RecipeManager recipeManager;
    public static DataManager getInstance() { return INSTANCE; }

    private DataManager() {}

    public void reset(boolean isLogout)
    {
        RecipeStorage.getInstance().reset(isLogout);
        VillagerDataStorage.getInstance().reset(isLogout);

        this.registryManager = DynamicRegistryManager.EMPTY;
        this.recipeManager = null;
    }

    public void onWorldJoinPre()
    {
        if (Configs.Generic.SCROLL_CRAFT_STORE_RECIPES_TO_FILE.getBooleanValue())
        {
            RecipeStorage.getInstance().writeToDisk();
        }

        VillagerDataStorage.getInstance().writeToDisk();
    }

    public void onWorldJoinPost()
    {
        if (Configs.Generic.SCROLL_CRAFT_STORE_RECIPES_TO_FILE.getBooleanValue())
        {
            RecipeStorage.getInstance().readFromDisk();
        }

        VillagerDataStorage.getInstance().readFromDisk();
    }

    /**
     * Store's the world registry manager for Dynamic Lookup
     * Set this at WorldLoadPost
     * @param manager (DynamicRegistryManager)
     */
    public void setWorldRegistryManager(DynamicRegistryManager manager)
    {
        if (manager != null && manager != DynamicRegistryManager.EMPTY)
        {
            this.registryManager = manager;
        }
        else
        {
            this.registryManager = DynamicRegistryManager.EMPTY;
        }
    }

    public DynamicRegistryManager getWorldRegistryManager()
    {
        if (this.registryManager != DynamicRegistryManager.EMPTY)
        {
            return this.registryManager;
        }
        else
        {
            return DynamicRegistryManager.EMPTY;
        }
    }

    /**
     * Store's the world recipe manager for Dynamic Lookup
     * Set this at WorldLoadPost
     * @param manager (RecipeManager)
     */
    public void setWorldRecipeManager(RecipeManager manager)
    {
        this.recipeManager = manager;
    }

    public RecipeManager getWorldRecipeManager()
    {
        return this.recipeManager;
    }
}
