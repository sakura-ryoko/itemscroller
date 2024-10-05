package fi.dy.masa.itemscroller.data;

import java.util.ArrayList;
import java.util.Collection;
import javax.annotation.Nullable;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.recipe.PreparedRecipes;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.network.ClientPlayHandler;
import fi.dy.masa.malilib.network.IPluginClientPlayHandler;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.mixin.IMixinServerRecipeManager;
import fi.dy.masa.itemscroller.network.ServuxScrollerHandler;
import fi.dy.masa.itemscroller.network.ServuxScrollerPacket;

public class DataManager
{
    private static final DataManager INSTANCE = new DataManager();

    private final static ServuxScrollerHandler<ServuxScrollerPacket.Payload> HANDLER = ServuxScrollerHandler.getInstance();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean servuxServer;
    private boolean hasInValidServux;
    private String servuxVersion;
    private DynamicRegistryManager registryManager;

    private PreparedRecipes preparedRecipes;
    private int recipeCount;

    public DataManager()
    {
        this.servuxServer = false;
        this.hasInValidServux = false;
        this.servuxVersion = "";
        this.registryManager = DynamicRegistryManager.EMPTY;
        this.preparedRecipes = PreparedRecipes.EMPTY;
        this.recipeCount = 0;
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
            this.servuxVersion = "";
            this.registryManager = DynamicRegistryManager.EMPTY;
            this.preparedRecipes = PreparedRecipes.EMPTY;
            this.recipeCount = 0;
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

    public @Nullable PreparedRecipes getPreparedRecipes()
    {
        if (mc.isIntegratedServerRunning() && this.getRecipeManager() != null)
        {
            return ((IMixinServerRecipeManager) this.getRecipeManager()).itemscroller_getPreparedRecipes();
        }
        else if (this.hasRecipes())
        {
            return this.preparedRecipes;
        }

        return null;
    }

    public int getRecipeCount()
    {
        return this.recipeCount;
    }

    public @Nullable RecipeManager getRecipeManager()
    {
        if (mc.isIntegratedServerRunning() && mc.getServer() != null)
        {
            return mc.getServer().getRecipeManager();
        }
        else if (mc.world != null)
        {
            return mc.world.getRecipeManager();
        }

        return null;
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
            this.requestRecipeManager();

            return true;
        }

        return false;
    }

    public void requestRecipeManager()
    {
        if (!mc.isIntegratedServerRunning() && this.hasServuxServer())
        {
            NbtCompound nbt = new NbtCompound();
            nbt.putString("version", Reference.MOD_STRING);

            HANDLER.encodeClientData(ServuxScrollerPacket.RecipeManagerRequest(nbt));
        }
    }

    public void receiveRecipeManager(NbtCompound data)
    {
        if (!mc.isIntegratedServerRunning() && data.contains("RecipeManager"))
        {
            Collection<RecipeEntry<?>> recipes = new ArrayList<>();
            NbtList list = data.getList("RecipeManager", Constants.NBT.TAG_COMPOUND);
            int count = 0;

            this.preparedRecipes = PreparedRecipes.EMPTY;
            this.recipeCount = 0;

            for (int i = 0; i < list.size(); i++)
            {
                NbtCompound item = list.getCompound(i);
                Identifier idReg = Identifier.tryParse(item.getString("id_reg"));
                Identifier idValue = Identifier.tryParse(item.getString("id_value"));

                if (idReg == null || idValue == null)
                {
                    continue;
                }

                try
                {
                    RegistryKey<Recipe<?>> key = RegistryKey.of(RegistryKey.ofRegistry(idReg), idValue);
                    Pair<Recipe<?>, NbtElement> pair = Recipe.CODEC.decode(this.getWorldRegistryManager().getOps(NbtOps.INSTANCE), item.getCompound("recipe")).getOrThrow();
                    RecipeEntry<?> entry = new RecipeEntry<>(key, pair.getFirst());
                    recipes.add(entry);
                    count++;
                }
                catch (Exception e)
                {
                    ItemScroller.logger.error("receiveRecipeManager: index [{}], Exception reading packet, {}", i, e.getMessage());
                }
            }

            if (!recipes.isEmpty())
            {
                this.preparedRecipes = PreparedRecipes.of(recipes);
                this.recipeCount = count;
                ItemScroller.printDebug("DataManager#receiveRecipeManager(): finished loading Recipe Manager: Read [{}] Recipes from Servux", count);
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
