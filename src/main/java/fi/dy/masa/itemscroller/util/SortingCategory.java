package fi.dy.masa.itemscroller.util;

import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.config.IConfigLockedListEntry;
import fi.dy.masa.malilib.config.IConfigLockedListType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.itemscroller.Reference;

public class SortingCategory implements IConfigLockedListType
{
    public static final SortingCategory INSTANCE = new SortingCategory();
    public ImmutableList<Entry> VALUES = ImmutableList.copyOf(Entry.values());

    @Nullable
    public ItemGroup.DisplayContext buildDisplayContext(MinecraftClient mc)
    {
        if (mc.world == null)
        {
            return null;
        }

        ItemGroup.DisplayContext ctx = new ItemGroup.DisplayContext(mc.world.getEnabledFeatures(), true, mc.world.getRegistryManager());

        Registries.ITEM_GROUP.stream().filter((group) ->
                group.getType() == ItemGroup.Type.CATEGORY).forEach((group) ->
                group.updateEntries(ctx));

        return ctx;
    }

    public Entry fromItemStack(ItemStack stack)
    {
        for (int i = 0; i < Registries.ITEM_GROUP.size(); i++)
        {
            ItemGroup itemGroup = Registries.ITEM_GROUP.get(i);

            if (itemGroup != null && itemGroup.getType().equals(ItemGroup.Type.CATEGORY))
            {
                Collection<ItemStack> stacks;
                Iterator<ItemStack> iter;

                if (itemGroup.hasStacks())
                {
                    stacks = itemGroup.getDisplayStacks();
                    iter = stacks.iterator();

                    while (iter.hasNext())
                    {
                        if (ItemStack.areItemsEqual(iter.next(), stack))
                        {
                            return fromItemGroup(itemGroup);
                        }
                    }

                }

                stacks = itemGroup.getSearchTabStacks();
                iter = stacks.iterator();

                while (iter.hasNext())
                {
                    if (ItemStack.areItemsEqual(iter.next(), stack))
                    {
                        return fromItemGroup(itemGroup);
                    }
                }

            }
        }

        return Entry.OTHER;
    }

    @Nullable
    public Entry fromItemGroup(ItemGroup group)
    {
        Identifier id = Registries.ITEM_GROUP.getId(group);

        if (id != null)
        {
            return Entry.fromString(id.getPath());
        }

        return Entry.OTHER;
    }

    @Override
    public ImmutableList<IConfigLockedListEntry> getDefaultEntries()
    {
        ImmutableList.Builder<IConfigLockedListEntry> list = ImmutableList.builder();

        VALUES.forEach((list::add));

        return list.build();
    }

    @Override
    @Nullable
    public IConfigLockedListEntry fromString(String string)
    {
        return Entry.fromString(string);
    }

    public enum Entry implements IConfigLockedListEntry
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
        OTHER               ("other",               "other");

        private final String configKey;
        private final String translationKey;

        Entry(String configKey, String translationKey)
        {
            this.configKey = configKey;
            this.translationKey = Reference.MOD_ID+".gui.label.sorting_category."+translationKey;
        }

        @Override
        public String getStringValue()
        {
            return this.configKey;
        }

        @Override
        public String getDisplayName()
        {
            return StringUtils.getTranslatedOrFallback(this.translationKey, this.configKey);
        }

        @Nullable
        public static Entry fromString(String key)
        {
            for (Entry entry : values())
            {
                if (entry.configKey.equalsIgnoreCase(key))
                {
                    return entry;
                }
                else if (entry.translationKey.equalsIgnoreCase(key))
                {
                    return entry;
                }
                else if (StringUtils.hasTranslation(entry.translationKey) && StringUtils.translate(entry.translationKey).equalsIgnoreCase(key))
                {
                    return entry;
                }
            }

            return null;
        }
    }
}
