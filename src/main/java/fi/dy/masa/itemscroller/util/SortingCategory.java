package fi.dy.masa.itemscroller.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import com.google.common.collect.ImmutableList;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.itemscroller.Reference;
import fi.dy.masa.itemscroller.config.Configs;

public enum SortingCategory implements IConfigOptionListEntry
{
    BUILDING_BLOCKS     ("building_blocks",     "building_blocks"),
    COLORED_BLOCKS      ("colored_blocks",      "colored_blocks"),
    NATURAL             ("natural_blocks",      "natural_blocks"),
    FUNCTIONAL          ("functional_blocks",   "functional_blocks"),
    REDSTONE            ("redstone_blocks",     "redstone_blocks"),
    TOOLS               ("tools_and_utilities", "tools_and_utilities"),
    COMBAT              ("combat",              "combat"),
    FOOD_AND_DRINK      ("food_and_drinks",     "food_and_drinks"),
    INGREDIENTS         ("ingredients",         "ingredients"),
    SPAWN_EGGS          ("spawn_eggs",          "spawn_eggs"),
    OPERATOR            ("op_blocks",           "op_blocks"),

    INVENTORY           ("inventory",           "inventory"),
    HOTBAR              ("hotbar",              "hotbar"),
    SEARCH              ("search",              "search"),
    OTHER               ("other",               "other");

    public static final ImmutableList<SortingCategory> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;
    
    SortingCategory(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = Reference.MOD_ID+".gui.label.sorting_category."+translationKey;
    }

    public static ImmutableList<String> toImmutableList()
    {
        ImmutableList.Builder<String> list = ImmutableList.builder();

        VALUES.forEach((v) -> list.add(v.getDisplayName()));

        return list.build();
    }

    @Override
    public String getStringValue()
    {
        return this.configString;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.getTranslatedOrFallback(this.translationKey, this.configString);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int id = this.ordinal();

        if (forward)
        {
            if (++id >= values().length)
            {
                id = 0;
            }
        }
        else
        {
            if (--id < 0)
            {
                id = values().length - 1;
            }
        }

        return values()[id % values().length];
    }

    @Override
    public SortingCategory fromString(String value)
    {
        return fromStringStatic(value);
    }

    public static SortingCategory fromStringStatic(String name)
    {
        for (SortingCategory val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return SortingCategory.OTHER;
    }

    public static ItemGroup.DisplayContext buildDisplayContext(FeatureSet featureSet, DynamicRegistryManager registryManager)
    {
        ItemGroup.DisplayContext ctx = new ItemGroup.DisplayContext(featureSet, true, registryManager);

        Registries.ITEM_GROUP.stream().filter((group) ->
                group.getType() == ItemGroup.Type.CATEGORY).forEach((group) ->
                group.updateEntries(ctx));

        /*
        Registries.ITEM_GROUP.stream().filter((group) ->
                group.getType() != ItemGroup.Type.CATEGORY).forEach((group) ->
                group.updateEntries(ctx));
         */

        return ctx;
    }

    public static SortingCategory fromItemStack(ItemStack stack)
    {
        for (int i = 0; i < Registries.ITEM_GROUP.size(); i++)
        {
            ItemGroup test = Registries.ITEM_GROUP.get(i);

            if (test != null && test.getType().equals(ItemGroup.Type.CATEGORY))
            {
                Collection<ItemStack> stacks;
                Iterator<ItemStack> iter;

                if (test.hasStacks())
                {
                    stacks = test.getDisplayStacks();
                    iter = stacks.iterator();

                    while (iter.hasNext())
                    {
                        if (ItemStack.areItemsEqual(iter.next(), stack))
                        {
                            return fromItemGroup(test);
                        }
                    }

                }

                stacks = test.getSearchTabStacks();
                iter = stacks.iterator();

                while (iter.hasNext())
                {
                    if (ItemStack.areItemsEqual(iter.next(), stack))
                    {
                        return fromItemGroup(test);
                    }
                }

            }
        }

        return SortingCategory.OTHER;
    }

    public static SortingCategory fromItemGroup(ItemGroup group)
    {
        Identifier id = Registries.ITEM_GROUP.getId(group);

        if (id != null)
        {
            for (SortingCategory entry : VALUES)
            {
                if (id.getPath().equals(entry.getStringValue()))
                {
                    return entry;
                }
            }
        }

        return SortingCategory.OTHER;
    }

    public static int getConfigIndex(SortingCategory category)
    {
        List<String> config = Configs.Generic.SORT_CATEGORY_ORDER.getStrings();

        for (int i = 0; i < config.size(); i++)
        {
            if (config.get(i).equals(category.getStringValue()))
            {
                return i;
            }
        }

        return -1;
    }
}
