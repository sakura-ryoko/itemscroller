package fi.dy.masa.itemscroller.villager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.ListData;
import fi.dy.masa.malilib.util.data.tag.util.DataFileUtils;
import fi.dy.masa.itemscroller.ItemScroller;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.config.Configs;

public class VillagerDataStorage
{
    private static final VillagerDataStorage INSTANCE = new VillagerDataStorage();

    private final Map<UUID, VillagerData> data = new HashMap<>();
    private final List<TradeType> globalFavorites = new ArrayList<>();
    private UUID lastInteractedUUID;
    private boolean dirty;

    public static VillagerDataStorage getInstance()
    {
        return INSTANCE;
    }

    public void setLastInteractedUUID(UUID uuid)
    {
        this.lastInteractedUUID = uuid;
    }

    @Nullable
    public VillagerData getDataForLastInteractionTarget()
    {
        return this.getDataFor(this.lastInteractedUUID, true);
    }

    public VillagerData getDataFor(@Nullable UUID uuid, boolean create)
    {
        VillagerData data = uuid != null ? this.data.get(uuid) : null;

        if (data == null && uuid != null && create)
        {
            this.setLastInteractedUUID(uuid);
            data = new VillagerData(uuid);
            this.data.put(uuid, data);
            this.dirty = true;
        }

        return data;
    }

    public void setTradeListPosition(int position)
    {
        VillagerData data = this.getDataFor(this.lastInteractedUUID, true);

        if (data != null)
        {
            data.setTradeListPosition(position);
            this.dirty = true;
        }
    }

    public void toggleFavorite(int tradeIndex)
    {
        VillagerData data = this.getDataFor(this.lastInteractedUUID, true);

        if (data != null)
        {
            data.toggleFavorite(tradeIndex);
            this.dirty = true;
        }
    }

    public void toggleGlobalFavorite(TradeOffer trade)
    {
        TradeType type = TradeType.of(trade);

        if (this.globalFavorites.contains(type))
        {
            this.globalFavorites.remove(type);
        }
        else
        {
            this.globalFavorites.add(type);
        }

        this.dirty = true;
    }

    public FavoriteData getFavoritesForCurrentVillager(MerchantScreenHandler handler)
    {
        return this.getFavoritesForCurrentVillager(((IMerchantScreenHandler) handler).itemscroller$getOriginalList());
    }

    public FavoriteData getFavoritesForCurrentVillager(TradeOfferList originalTrades)
    {
        VillagerData data = this.getDataFor(this.lastInteractedUUID, false);
        IntArrayList favorites = data != null ? data.getFavorites() : null;

        if (favorites != null && favorites.isEmpty() == false)
        {
            return new FavoriteData(favorites, false);
        }

        if (Configs.Generic.VILLAGER_TRADE_USE_GLOBAL_FAVORITES.getBooleanValue() && this.lastInteractedUUID != null)
        {
            return new FavoriteData(VillagerUtils.getGlobalFavoritesFor(originalTrades, this.globalFavorites), true);
        }

        return new FavoriteData(IntArrayList.of(), favorites == null);
    }

    private void readFromNBT(CompoundData tags)
    {
        if (tags == null || tags.contains("VillagerData", Constants.NBT.TAG_LIST) == false)
        {
            return;
        }

        ListData tagList = tags.getList("VillagerData");
        int count = tagList.size();

        for (int i = 0; i < count; i++)
        {
	        CompoundData tag = tagList.getCompoundAt(i);
            VillagerData data = VillagerData.fromNBT(tag);

            if (data != null)
            {
                this.data.put(data.getUUID(), data);
            }
        }

        tagList = tags.getList("GlobalFavorites");
        count = tagList.size();

        for (int i = 0; i < count; i++)
        {
	        CompoundData tag = tagList.getCompoundAt(i);
            TradeType type = TradeType.fromTag(tag);

            if (type != null)
            {
                this.globalFavorites.add(type);
            }
        }
    }

    private CompoundData writeToNBT()
    {
	    CompoundData tags = new CompoundData();
        ListData favoriteListData = new ListData();
	    ListData globalFavoriteData = new ListData();

        for (VillagerData data : this.data.values())
        {
            favoriteListData.add(data.toNBT());
        }

        for (TradeType type : this.globalFavorites)
        {
            globalFavoriteData.add(type.toTag());
        }

	    tags.put("VillagerData", favoriteListData);
	    tags.put("GlobalFavorites", globalFavoriteData);

        this.dirty = false;

        return tags;
    }

    private String getFileName()
    {
        String worldName = StringUtils.getWorldOrServerName();

        if (worldName != null)
        {
            return "villager_data_" + worldName + ".nbt";
        }

        return "villager_data.nbt";
    }

    private Path getSaveDirPath()
    {
        return FileUtils.getMinecraftDirectoryAsPath().resolve(Reference.MOD_ID);
    }

    public void readFromDisk()
    {
        this.data.clear();
        this.globalFavorites.clear();

        try
        {
            Path saveDir = this.getSaveDirPath();

            if (Files.isDirectory(saveDir))
            {
                Path file = saveDir.resolve(this.getFileName());

                if (Files.exists(file))
                {
//                    NbtCompound nbtIn = NbtUtils.readNbtFromFileAsPath(file, NbtSizeTracker.ofUnlimitedBytes());
	                CompoundData data = DataFileUtils.readCompoundDataFromNbtFile(file);

                    if (data != null && !data.isEmpty())
                    {
                        this.readFromNBT(data);
                        //ItemScroller.debugLog("readFromDisk(): Successfully loaded villager's from file '{}'", file.toAbsolutePath());
                    }
                    else
                    {
                        ItemScroller.LOGGER.warn("readFromDisk(): Error reading villager data from file '{}'", file.toAbsolutePath());
                    }
                }
                // File does not exist
            }
            else
            {
                ItemScroller.LOGGER.warn("readFromDisk(): Error reading villager data from dir '{}'", saveDir.toAbsolutePath());
            }
        }
        catch (Exception e)
        {
            ItemScroller.LOGGER.warn("Failed to read villager data from file", e);
        }
    }

    public void writeToDisk()
    {
        if (this.dirty)
        {
            try
            {
                Path saveDir = this.getSaveDirPath();

                if (!Files.exists(saveDir))
                {
                    FileUtils.createDirectoriesIfMissing(saveDir);
                    //ItemScroller.debugLog("writeToDisk(): Creating directory '{}'.", saveDir.toAbsolutePath());
                }

                if (Files.isDirectory(saveDir))
                {
                    Path fileTmp = saveDir.resolve(this.getFileName() + ".tmp");
                    Path fileReal = saveDir.resolve(this.getFileName());

//                    NbtUtils.writeCompressed(this.writeToNBT(), fileTmp);
	                CompoundData data = this.writeToNBT();
					DataFileUtils.writeCompoundDataToCompressedNbtFile(fileTmp, data);

                    if (Files.exists(fileReal))
                    {
                        Files.delete(fileReal);
                    }

                    Files.move(fileTmp, fileReal);

                    //ItemScroller.debugLog("writeToDisk(): Successfully saved recipes file '{}'", fileReal.toAbsolutePath());
                    this.dirty = false;
                }
            }
            catch (Exception e)
            {
                ItemScroller.LOGGER.warn("Failed to write villager data to file!", e);
            }
        }
    }
}
