package fi.dy.masa.itemscroller.util;

import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;

import fi.dy.masa.malilib.config.IConfigLockedListEntry;
import fi.dy.masa.malilib.config.IConfigLockedListType;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.itemscroller.Reference;

public class SortingCategory implements IConfigLockedListType
{
    public static final SortingCategory INSTANCE = new SortingCategory();
    public ImmutableList<@NotNull Entry> VALUES = ImmutableList.copyOf(Entry.values());
    //public static final Codec<SortingCategory> CODEC = Entry.CODEC.listOf().xmap(getDefault);

    @Nullable
    public CreativeModeTab.ItemDisplayParameters buildDisplayContext(Minecraft mc)
    {
        if (mc.level == null)
        {
            return null;
        }

        CreativeModeTab.ItemDisplayParameters ctx = new CreativeModeTab.ItemDisplayParameters(mc.level.enabledFeatures(), true, mc.level.registryAccess());

        BuiltInRegistries.CREATIVE_MODE_TAB.stream().filter((group) ->
                group.getType() == CreativeModeTab.Type.CATEGORY).forEach((group) ->
                group.buildContents(ctx));

        return ctx;
    }

    public Entry fromItemStack(ItemStack stack)
    {
        for (int i = 0; i < BuiltInRegistries.CREATIVE_MODE_TAB.size(); i++)
        {
            CreativeModeTab itemGroup = BuiltInRegistries.CREATIVE_MODE_TAB.byId(i);

            if (itemGroup != null && itemGroup.getType().equals(CreativeModeTab.Type.CATEGORY))
            {
                Collection<ItemStack> stacks;
                Iterator<ItemStack> iter;

                if (itemGroup.hasAnyItems())
                {
                    stacks = itemGroup.getDisplayItems();
                    iter = stacks.iterator();

                    while (iter.hasNext())
                    {
                        if (ItemStack.isSameItem(iter.next(), stack))
                        {
                            return fromItemGroup(itemGroup);
                        }
                    }

                }

                stacks = itemGroup.getSearchTabDisplayItems();
                iter = stacks.iterator();

                while (iter.hasNext())
                {
                    if (ItemStack.isSameItem(iter.next(), stack))
                    {
                        return fromItemGroup(itemGroup);
                    }
                }

            }
        }

        return Entry.OTHER;
    }

    @Nullable
    public Entry fromItemGroup(CreativeModeTab group)
    {
        Identifier id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(group);

        if (id != null)
        {
            Entry entry = Entry.fromString(id.getPath()); 
            if (entry != null)
            {
                return entry;
            }
        }

        return Entry.OTHER;
    }

    @Override
    public ImmutableList<@NotNull IConfigLockedListEntry> getDefaultEntries()
    {
        ImmutableList.Builder<@NotNull IConfigLockedListEntry> list = ImmutableList.builder();

        VALUES.forEach((list::add));

        return list.build();
    }

    @Override
    @Nullable
    public IConfigLockedListEntry fromString(String string)
    {
        return Entry.fromString(string);
    }

    public enum Entry implements IConfigLockedListEntry, StringRepresentable
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

        public static final StringRepresentable.EnumCodec<Entry> CODEC = StringRepresentable.fromEnum(Entry::values);
        public static final ImmutableList<@NotNull Entry> VALUES = ImmutableList.copyOf(values());

        private final String configKey;
        private final String translationKey;

        Entry(String configKey, String translationKey)
        {
            this.configKey = configKey;
            this.translationKey = Reference.MOD_ID+".gui.label.sorting_category."+translationKey;
        }

        @Override
        public @Nonnull String getSerializedName()
        {
            return this.configKey;
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
