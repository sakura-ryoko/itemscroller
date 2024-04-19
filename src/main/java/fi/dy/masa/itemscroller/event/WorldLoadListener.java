package fi.dy.masa.itemscroller.event;

import javax.annotation.Nullable;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import fi.dy.masa.malilib.interfaces.IWorldLoadListener;
import fi.dy.masa.itemscroller.util.ClickPacketBuffer;
import fi.dy.masa.itemscroller.util.DataManager;

public class WorldLoadListener implements IWorldLoadListener
{
    @Override
    public void onWorldLoadPre(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        // Quitting to main menu, save the settings before the integrated server gets shut down
        if (worldBefore != null && worldAfter == null)
        {
            DataManager.getInstance().onWorldJoinPre();
        }
    }

    @Override
    public void onWorldLoadPost(@Nullable ClientWorld worldBefore, @Nullable ClientWorld worldAfter, MinecraftClient mc)
    {
        DataManager.getInstance().reset(worldAfter == null);

        // Logging in to a world, load the data
        if (worldBefore == null && worldAfter != null)
        {
            DataManager.getInstance().setWorldRegistryManager(worldAfter.getRegistryManager());
            DataManager.getInstance().setWorldRecipeManager(worldAfter.getRecipeManager());
            DataManager.getInstance().onWorldJoinPost();
        }

        // Logging out
        if (worldAfter == null)
        {
            ClickPacketBuffer.reset();
        }
    }
}
