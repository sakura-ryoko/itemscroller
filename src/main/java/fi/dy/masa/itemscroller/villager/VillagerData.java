package fi.dy.masa.itemscroller.villager;

import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import java.util.UUID;

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

    public CompoundTag toNBT()
    {
        CompoundTag tag = new CompoundTag();

        tag.putLong("UUIDM", this.uuid.getMostSignificantBits());
        tag.putLong("UUIDL", this.uuid.getLeastSignificantBits());
        tag.putInt("ListPosition", this.tradeListPosition);

        ListTag tagList = new ListTag();

        for (Integer val : this.favorites)
        {
            tagList.add(IntTag.valueOf(val));
        }

        tag.put("Favorites", tagList);

        return tag;
    }

    @Nullable
    public static VillagerData fromNBT(CompoundTag tag)
    {
        if (tag.contains("UUIDM") && tag.contains("UUIDL"))
        {
            VillagerData data = new VillagerData(new UUID(tag.getLongOr("UUIDM", 0L), tag.getLongOr("UUIDL", 0L)));
            ListTag tagList = tag.getListOrEmpty("Favorites");
            final int count = tagList.size();

            data.favorites.clear();
            data.tradeListPosition = tag.getIntOr("ListPosition", -1);

            for (int i = 0; i < count; ++i)
            {
                data.favorites.add(tagList.getIntOr(i, -1));
            }

            return data;
        }

        return null;
    }
}
