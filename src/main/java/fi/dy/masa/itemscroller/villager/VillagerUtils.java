package fi.dy.masa.itemscroller.villager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import fi.dy.masa.malilib.util.GuiUtils;

public class VillagerUtils
{
    public static boolean switchToTradeByVisibleIndex(int visibleIndex)
    {
        Screen screen = GuiUtils.getCurrentScreen();

        if (screen instanceof MerchantScreen merchantScreen)
        {
            MerchantMenu handler = merchantScreen.getMenu();

            int realIndex = getRealTradeIndexFor(visibleIndex, handler);

            if (realIndex >= 0)
            {
                // Use the real (server-side) index
                handler.setSelectionHint(realIndex);

                // Use the "visible index", since this will access the custom list
                handler.tryMoveItems(visibleIndex);

                // Use the real (server-side) index
                Minecraft.getInstance().getConnection().send(new ServerboundSelectTradePacket(realIndex));

                return true;
            }
        }

        return false;
    }

    public static int getRealTradeIndexFor(int visibleIndex, MerchantMenu handler)
    {
        if (handler instanceof IMerchantScreenHandler)
        {
            MerchantOffers originalList = ((IMerchantScreenHandler) handler).itemscroller$getOriginalList();
            MerchantOffers customList = handler.getOffers();

            if (originalList != null && customList != null &&
                visibleIndex >= 0 && visibleIndex < customList.size())
            {
                MerchantOffer trade = customList.get(visibleIndex);

                if (trade != null)
                {
                    int realIndex = originalList.indexOf(trade);

                    if (realIndex >= 0 && realIndex < originalList.size())
                    {
                        return realIndex;
                    }
                }
            }
        }

        return -1;
    }

    public static MerchantOffers buildCustomTradeList(MerchantOffers originalList)
    {
        FavoriteData data = VillagerDataStorage.getInstance().getFavoritesForCurrentVillager(originalList);
        IntArrayList favorites = data.favorites();

        //System.out.printf("build - fav: %s (%s), or: %d\n", favorites, data.isGlobal, originalList.size());

        // Some favorites defined
        if (favorites.isEmpty() == false)
        {
            MerchantOffers list = new MerchantOffers();
            int originalListSize = originalList.size();

            // First pick all the favorited recipes, in the order they are in the favorites list
            for (int index : favorites)
            {
                if (index >= 0 && index < originalListSize)
                {
                    list.add(originalList.get(index));
                }
            }

            // Then add the rest of the recipes in their original order
            for (int i = 0; i < originalListSize; ++i)
            {
                if (favorites.contains(i) == false)
                {
                    list.add(originalList.get(i));
                }
            }

            return list;
        }

        return originalList;
    }

    public static IntArrayList getGlobalFavoritesFor(MerchantOffers originalTrades, Collection<TradeType> globalFavorites)
    {
        IntArrayList favorites = new IntArrayList();
        Map<TradeType, Integer> trades = new HashMap<>();
        final int size = originalTrades.size();

        // Build a map from the trade types to the indices in the current villager's trade list
        for (int i = 0; i < size; ++i)
        {
            MerchantOffer trade = originalTrades.get(i);
            trades.put(TradeType.of(trade), i);
        }

        // Pick the trade list indices that are in the global favorites, in the order that they were global favorited
        for (TradeType type : globalFavorites)
        {
            Integer index = trades.get(type);

            if (index != null)
            {
                favorites.add(index.intValue());
            }
        }

        /* This is a version that is not sorted based on the order of the global favorites
        for (int i = 0; i < size; ++i)
        {
            TradeType type = TradeType.of(originalTrades.get(i));

            if (globalFavorites.contains(type))
            {
                favorites.add(i);
            }
        }
        */
        //System.out.printf("getGlobalFavoritesFor - list: %s - or: %d | global: %s\n", favorites, originalTrades.size(), globalFavorites);

        return favorites;
    }
}
