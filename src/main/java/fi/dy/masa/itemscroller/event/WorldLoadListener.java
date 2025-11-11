package fi.dy.masa.itemscroller.event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.DynamicRegistryManager;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.recipes.RecipeStorage;
import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import fi.dy.masa.itemscroller.villager.VillagerDataStorage;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadPre(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        // Quitting to main menu, save the settings before the integrated server gets shut down
        if (worldBefore != null && worldAfter == null)
        {
            this.writeData(worldBefore.getRegistryManager());
            VillagerDataStorage.getInstance().writeToDisk();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        RecipeStorage.getInstance().reset(worldAfter == null);

        // Logging in to a world, load the data
        if (worldBefore == null && worldAfter != null)
        {
            this.readStoredData(worldAfter.getRegistryManager());
            VillagerDataStorage.getInstance().readFromDisk();
        }

        // Logging out
        if (worldAfter == null)
        {
            ClickPacketBuffer.reset();
        }
    }

    private void writeData(@Nonnull DynamicRegistryManager registryManager)
    {
        if (Configs.Generic.SCROLL_CRAFT_STORE_RECIPES_TO_FILE.getBooleanValue())
        {
            RecipeStorage.getInstance().writeToDisk(registryManager);
        }
    }

    private void readStoredData(@Nonnull DynamicRegistryManager registryManager)
    {
        if (Configs.Generic.SCROLL_CRAFT_STORE_RECIPES_TO_FILE.getBooleanValue())
        {
            RecipeStorage.getInstance().readFromDisk(registryManager);
        }
    }
}
