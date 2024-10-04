package fi.dy.masa.itemscroller.data;

import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.network.ServuxScrollerHandler;
import fi.dy.masa.itemscroller.network.ServuxScrollerPacket;
import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collection;

public class DataManager
{
    private static final DataManager INSTANCE = new DataManager();

    private final static ServuxScrollerHandler<ServuxScrollerPacket.Payload> HANDLER = ServuxScrollerHandler.getInstance();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean servuxServer;
    private boolean hasInValidServux;
    private String servuxVersion;
    private DynamicRegistryManager registryManager;

    private PreparedRecipes preparedRecipes = PreparedRecipes.EMPTY;

    public DataManager()
    {
        this.servuxServer = false;
        this.hasInValidServux = false;
        this.servuxVersion = "";
        this.registryManager = DynamicRegistryManager.EMPTY;
    }

    public static DataManager getInstance() { return INSTANCE; }

    public void onGameInit()
    {
        ClientPlayHandler.getInstance().registerClientPlayHandler(HANDLER);
        HANDLER.registerPlayPayload(ServuxScrollerPacket.Payload.ID, ServuxScrollerPacket.Payload.CODEC, IPluginClientPlayHandler.BOTH_CLIENT);
    }

    public Identifier getNetworkChannel() { return ServuxScrollerHandler.CHANNEL_ID; }

    public IPluginClientPlayHandler<ServuxScrollerPacket.Payload> getNetworkHandler() { return HANDLER; }

    public void reset(boolean isLogout)
    {
        if (isLogout)
        {
            ItemScroller.printDebug("DataManager#reset() - log-out");
            HANDLER.reset(this.getNetworkChannel());
            HANDLER.resetFailures(this.getNetworkChannel());

            this.servuxServer = false;
            this.hasInValidServux = false;
            this.registryManager = DynamicRegistryManager.EMPTY;
            this.preparedRecipes = PreparedRecipes.EMPTY;
        }
    }

    public void onWorldLoadPre()
    {
        if (!mc.isIntegratedServerRunning())
        {
            HANDLER.registerPlayReceiver(ServuxScrollerPacket.Payload.ID, HANDLER::receivePlayPayload);
        }
    }

    public void onWorldJoin()
    {
        ItemScroller.printDebug("DataManager#onWorldJoin()");
    }

    public void onPacketFailure()
    {
        // Define how to handle multiple sendPayload failures
        this.servuxServer = false;
        this.hasInValidServux = true;
    }

    public void setIsServuxServer()
    {
        this.servuxServer = true;
        if (this.hasInValidServux)
        {
            this.hasInValidServux = false;
        }
    }

    public void setServuxVersion(String ver)
    {
        if (ver != null && !ver.isEmpty())
        {
            this.servuxVersion = ver;
        }
        else
        {
            this.servuxVersion = "unknown";
        }
    }

    public String getServuxVersion()
    {
        if (this.hasServuxServer())
        {
            return this.servuxVersion;
        }

        return "not_connected";
    }

    public boolean hasServuxServer() { return this.servuxServer; }

    /**
     * Store's the world registry manager for Dynamic Lookup for various data
     * Set this at WorldLoadPost
     * @param manager ()
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

    public boolean hasRecipes()
    {
        return !this.preparedRecipes.equals(PreparedRecipes.EMPTY);
    }

    public PreparedRecipes getPreparedRecipes()
    {
        return this.preparedRecipes;
    }

    public boolean receiveMetadata(NbtCompound data)
    {
        if (!this.servuxServer && !mc.isIntegratedServerRunning())
        {
            ItemScroller.printDebug("DataManager#receiveMetadata(): received METADATA from Servux");

            if (data.getInt("version") != ServuxScrollerPacket.PROTOCOL_VERSION)
            {
                ItemScroller.logger.warn("scrollerDataChannel: Mis-matched protocol version!");
            }

            this.setServuxVersion(data.getString("servux"));
            this.setIsServuxServer();
            return true;
        }

        return false;
    }

    public void receiveRecipeManager(NbtCompound data)
    {
        if (!mc.isIntegratedServerRunning() && data.contains("RecipeManager"))
        {
            Collection<RecipeEntry<?>> recipes = new ArrayList<>();
            NbtList list = data.getList("RecipeManager", NbtElement.BYTE_ARRAY_TYPE);
            ItemScroller.printDebug("DataManager#receiveRecipeManager(): from Servux");

            for (int i = 0; i < list.size(); i++)
            {
                try
                {
                    NbtByteArray byteArray = (NbtByteArray) list.get(i);
                    RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), this.getWorldRegistryManager());
                    buf.writeByteArray(byteArray.getByteArray());
                    RecipeEntry<?> entry = RecipeEntry.PACKET_CODEC.decode(buf);
                    recipes.add(entry);
                }
                catch (Exception e)
                {
                    ItemScroller.logger.error("receiveRecipeManager: index [{}], Exception reading packet, {}", i, e.getMessage());
                }
            }

            if (!recipes.isEmpty())
            {
                this.preparedRecipes = PreparedRecipes.of(recipes);
                ItemScroller.printDebug("DataManager#receiveRecipeManager(): finished loading Recipe Manager -> Prepared Recipes from Servux");
            }
            else
            {
                ItemScroller.logger.warn("receiveRecipeManager: failed to read Recipe Manager from Servux (Collection was empty!)");
            }
        }
    }

    public void requestMassCraft()
    {
        // Do Something
    }

    public void receiveMassCraftResponse(NbtCompound data)
    {
        // Do something
    }
}
