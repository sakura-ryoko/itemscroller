package fi.dy.masa.itemscroller.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.util.Identifier;

import fi.dy.masa.malilib.config.IConfigLockedListEntry;
import fi.dy.masa.malilib.config.IConfigLockedListType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.itemscroller.Reference;

public class SortingCategory implements IConfigLockedListType
{
    public static final SortingCategory INSTANCE = new SortingCategory();
    public ImmutableList<SortingCategory.Entry> VALUES = ImmutableList.copyOf(SortingCategory.Entry.values());
    
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

    public static Entry fromItemStack(ItemStack stack)
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
    public static Entry fromItemGroup(ItemGroup group)
    {
        Identifier id = Registries.ITEM_GROUP.getId(group);

        if (id != null)
        {
            return Entry.fromString(id.getPath());
        }

        return Entry.OTHER;
    }

    @Override
    public @Nullable IConfigLockedListEntry getEmpty()
    {
        return null;
    }

    @Override
    public @Nullable IConfigLockedListEntry getEntry(String key)
    {
        return Entry.fromString(key);
    }

    @Override
    public ImmutableList<IConfigLockedListEntry> getDefaultEntries()
    {
        ImmutableList.Builder<IConfigLockedListEntry> list = ImmutableList.builder();

        VALUES.forEach((list::add));

        return list.build();
    }

    @Override
    public List<String> getConfigKeys(List<IConfigLockedListEntry> values)
    {
        List<String> list = new ArrayList<>();

        for (IConfigLockedListEntry entry : values)
        {
            list.add(entry.getDisplayName());
        }

        return list;
    }

    @Override
    public List<IConfigLockedListEntry> setEntries(List<IConfigLockedListEntry> entries)
    {
        List<IConfigLockedListEntry> defList = new ArrayList<>(this.getDefaultEntries().stream().toList());
        List<IConfigLockedListEntry> list = new ArrayList<>();

        entries.forEach((v) ->
        {
            Entry entry = Entry.fromString(v.getStringValue());

            if (entry != null)
            {
                list.add(entry);
                defList.remove(entry);
            }
        });

        // Default entries are missing
        if (defList.isEmpty() == false)
        {
            list.addAll(defList);
        }

        return list;
    }

    @Override
    public int getEntryIndex(List<IConfigLockedListEntry> entries, IConfigLockedListEntry entry)
    {
        for (int i = 0; i < entries.size(); i++)
        {
            if (entries.get(i).equals(entry))
            {
                return i;
            }
        }

        return -1;
    }

    @Override
    public List<IConfigLockedListEntry> fromJsonArray(JsonArray array)
    {
        List<IConfigLockedListEntry> defList = new ArrayList<>(this.getDefaultEntries().stream().toList());
        List<IConfigLockedListEntry> list = new ArrayList<>();

        // Only add matches ONCE & compare with Defaults.
        for (int i = 0; i < array.size(); i++)
        {
            Entry entry = Entry.fromString(array.get(i).getAsString());

            if (entry != null && list.contains(entry) == false)
            {
                list.add(entry);
                defList.remove(entry);
            }
        }

        // Default entries are missing
        if (defList.isEmpty() == false)
        {
            list.addAll(defList);
        }

        return list;
    }

    @Override
    public void toJsonArray(List<IConfigLockedListEntry> values, JsonArray array)
    {
        List<IConfigLockedListEntry> list = new ArrayList<>(this.getDefaultEntries().stream().toList());

        // Should only save 1 instance of each config
        for (IConfigLockedListEntry val : values)
        {
            if (list.contains(val))
            {
                array.add(new JsonPrimitive(val.getStringValue()));
                list.remove(val);
            }
        }

        // Default settings are missing
        if (list.isEmpty() == false)
        {
            for (IConfigLockedListEntry entry : list)
            {
                array.add(new JsonPrimitive(entry.getStringValue()));
            }
        }
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

        // Not technically Item Categories
        //INVENTORY           ("inventory",           "inventory"),
        //HOTBAR              ("hotbar",              "hotbar"),
        //SEARCH              ("search",              "search"),

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
