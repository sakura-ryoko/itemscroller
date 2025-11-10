package fi.dy.masa.itemscroller.villager;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

public class TradeType
{
    public final Item buyItem1;
    public final Item buyItem2;
    public final Item sellItem;

    public TradeType(Item buyItem1, Item buyItem2, Item sellItem)
    {
        this.buyItem1 = buyItem1;
        this.buyItem2 = buyItem2;
        this.sellItem = sellItem;
    }

    public boolean matchesTrade(MerchantOffer trade)
    {
        ItemStack stackBuyItem1 = trade.getBaseCostA();
        ItemStack stackBuyItem2 = trade.getCostB();
        ItemStack stackSellItem = trade.getResult();
        Item buyItem1 = stackBuyItem1.getItem();
        Item buyItem2 = stackBuyItem2.getItem();
        Item sellItem = stackSellItem.getItem();

        return this.buyItem1 == buyItem1 && this.buyItem2 == buyItem2 && this.sellItem == sellItem;
    }

    public CompoundTag toTag()
    {
        CompoundTag tag = new CompoundTag();

        tag.putString("Buy1", getNameForItem(this.buyItem1));
        tag.putString("Buy2", getNameForItem(this.buyItem2));
        tag.putString("Sell", getNameForItem(this.sellItem));

        return tag;
    }

    @Nullable
    public static TradeType fromTag(CompoundTag tag)
    {
        Item buy1 = getItemForName(tag.getStringOr("Buy1", ""));
        Item buy2 = getItemForName(tag.getStringOr("Buy2", ""));
        Item sell = getItemForName(tag.getStringOr("Sell", ""));

        if (buy1 != Items.AIR || buy2 != Items.AIR || sell != Items.AIR)
        {
            return new TradeType(buy1, buy2, sell);
        }

        return null;
    }

    public static Item getItemForName(String name)
    {
        try
        {
            ResourceLocation id = ResourceLocation.tryParse(name);

			if (id != null)
			{
				Optional<Holder.Reference<Item>> opt = BuiltInRegistries.ITEM.get(id);
				return opt.map(Holder.Reference::value).orElse(Items.AIR);
			}
        }
        catch (Exception ignored) { }

	    return Items.AIR;
    }

    public static String getNameForItem(Item item)
    {
        try
        {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        catch (Exception e)
        {
            return "?";
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        TradeType tradeType = (TradeType) o;

        if (!buyItem1.equals(tradeType.buyItem1)) { return false; }
        if (!buyItem2.equals(tradeType.buyItem2)) { return false; }
        return sellItem.equals(tradeType.sellItem);
    }

    @Override
    public int hashCode()
    {
        int result = buyItem1.hashCode();
        result = 31 * result + buyItem2.hashCode();
        result = 31 * result + sellItem.hashCode();
        return result;
    }

    public static TradeType of(MerchantOffer trade)
    {
        ItemStack stackBuyItem1 = trade.getBaseCostA();
        ItemStack stackBuyItem2 = trade.getCostB();
        ItemStack stackSellItem = trade.getResult();
        Item buyItem1 = stackBuyItem1.getItem();
        Item buyItem2 = stackBuyItem2.getItem();
        Item sellItem = stackSellItem.getItem();

        return new TradeType(buyItem1, buyItem2, sellItem);
    }
}
