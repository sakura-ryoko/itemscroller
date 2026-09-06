package fi.dy.masa.itemscroller.villager;

import java.util.UUID;
import javax.annotation.Nullable;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import fi.dy.masa.malilib.util.data.Constants;
import fi.dy.masa.malilib.util.data.tag.CompoundData;
import fi.dy.masa.malilib.util.data.tag.IntData;
import fi.dy.masa.malilib.util.data.tag.ListData;

public class VillagerData
{
    public static final String VILLAGER_DATA_UUID_M = "UUIDM";
    public static final String VILLAGER_DATA_UUID_L = "UUIDL";
    public static final String VILLAGER_DATA_LIST_POSITION = "ListPosition";
    public static final String VILLAGER_DATA_FAVORITES = "Favorites";

    private final UUID uuid;
    private final IntArrayList favorites = new IntArrayList();
    private int tradeListPosition;

    VillagerData(UUID uuid)
    {
        this.uuid = uuid;
    }

    public UUID getUUID()
    {
        return this.uuid;
    }

    public int getTradeListPosition()
    {
        return this.tradeListPosition;
    }

    void setTradeListPosition(int position)
    {
        this.tradeListPosition = position;
    }

    void toggleFavorite(int tradeIndex)
    {
        if (this.favorites.contains(tradeIndex))
        {
            this.favorites.rem(tradeIndex);
        }
        else
        {
            this.favorites.add(tradeIndex);
        }
    }

    IntArrayList getFavorites()
    {
        return this.favorites;
    }

    protected boolean isEmpty()
    {
        return this.favorites.isEmpty();
    }

    public CompoundData toNBT()
    {
	    CompoundData data = new CompoundData();

        if (this.isEmpty())
        {
            return data;
        }

	    data.putLong(VILLAGER_DATA_UUID_M, this.uuid.getMostSignificantBits());
	    data.putLong(VILLAGER_DATA_UUID_L, this.uuid.getLeastSignificantBits());
	    data.putInt(VILLAGER_DATA_LIST_POSITION, this.tradeListPosition);

        ListData tagList = new ListData();

        for (Integer val : this.favorites)
        {
            tagList.add(new IntData(val));
        }

	    data.put(VILLAGER_DATA_FAVORITES, tagList);

        return data;
    }

    @Nullable
    public static VillagerData fromNBT(CompoundData tag)
    {
        if (tag.contains(VILLAGER_DATA_UUID_M, Constants.NBT.TAG_LONG) && tag.contains(VILLAGER_DATA_UUID_L, Constants.NBT.TAG_LONG))
        {
            VillagerData data = new VillagerData(new UUID(tag.getLong(VILLAGER_DATA_UUID_M), tag.getLong(VILLAGER_DATA_UUID_L)));
            ListData tagList = tag.getList(VILLAGER_DATA_FAVORITES);
            final int count = tagList.size();

            data.favorites.clear();
            data.tradeListPosition = tag.getInt(VILLAGER_DATA_LIST_POSITION);

            for (int i = 0; i < count; ++i)
            {
                data.favorites.add(tagList.getIntAt(i));
            }

            return data;
        }

        return null;
    }
}
