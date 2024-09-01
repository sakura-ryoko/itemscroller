package fi.dy.masa.itemscroller.util;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.itemscroller.Reference;

public enum SortingMethod implements IConfigOptionListEntry
{
    ITEM_NAME       ("item_name",   "item_name"),
    ITEM_RARITY     ("item_rarity", "item_rarity"),
    ITEM_RAWID      ("item_rawid",  "item_rawid");

    public static final ImmutableList<SortingMethod> VALUES = ImmutableList.copyOf(values());

    private final String configString;
    private final String translationKey;

    SortingMethod(String configString, String translationKey)
    {
        this.configString = configString;
        this.translationKey = Reference.MOD_ID+".gui.label.sorting_method."+translationKey;
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
    public SortingMethod fromString(String value)
    {
        return fromStringStatic(value);
    }

    public static SortingMethod fromStringStatic(String name)
    {
        for (SortingMethod val : VALUES)
        {
            if (val.configString.equalsIgnoreCase(name))
            {
                return val;
            }
        }

        return SortingMethod.ITEM_RAWID;
    }
}
