package fi.dy.masa.itemscroller.villager;

import javax.annotation.Nullable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtList;
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

    public NbtCompound toNBT()
    {
        NbtCompound tag = new NbtCompound();

        tag.putLong("UUIDM", this.uuid.getMostSignificantBits());
        tag.putLong("UUIDL", this.uuid.getLeastSignificantBits());
        tag.putInt("ListPosition", this.tradeListPosition);

        NbtList tagList = new NbtList();

        for (Integer val : this.favorites)
        {
            tagList.add(NbtInt.of(val));
        }

        tag.put("Favorites", tagList);

        return tag;
    }

    @Nullable
    public static VillagerData fromNBT(NbtCompound tag)
    {
        if (tag.contains("UUIDM") && tag.contains("UUIDL"))
        {
            VillagerData data = new VillagerData(new UUID(tag.getLong("UUIDM", 0L), tag.getLong("UUIDL", 0L)));
            NbtList tagList = tag.getListOrEmpty("Favorites");
            final int count = tagList.size();

            data.favorites.clear();
            data.tradeListPosition = tag.getInt("ListPosition", -1);

            for (int i = 0; i < count; ++i)
            {
                data.favorites.add(tagList.getInt(i, -1));
            }

            return data;
        }

        return null;
    }
}
