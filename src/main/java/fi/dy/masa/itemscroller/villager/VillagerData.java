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

	    data.putLong("UUIDM", this.uuid.getMostSignificantBits());
	    data.putLong("UUIDL", this.uuid.getLeastSignificantBits());
	    data.putInt("ListPosition", this.tradeListPosition);

        ListData tagList = new ListData();

        for (Integer val : this.favorites)
        {
            tagList.add(new IntData(val));
        }

	    data.put("Favorites", tagList);

        return data;
    }

    @Nullable
    public static VillagerData fromNBT(CompoundData tag)
    {
        if (tag.contains("UUIDM", Constants.NBT.TAG_LONG) && tag.contains("UUIDL", Constants.NBT.TAG_LONG))
        {
            VillagerData data = new VillagerData(new UUID(tag.getLong("UUIDM"), tag.getLong("UUIDL")));
            ListData tagList = tag.getList("Favorites");
            final int count = tagList.size();

            data.favorites.clear();
            data.tradeListPosition = tag.getInt("ListPosition");

            for (int i = 0; i < count; ++i)
            {
                data.favorites.add(tagList.getIntAt(i));
            }

            return data;
        }

        return null;
    }
}
