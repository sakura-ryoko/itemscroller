package fi.dy.masa.itemscroller;

import net.minecraft.MinecraftVersion;
import fi.dy.masa.malilib.MaLiLib;

public class Reference
{
    public static final String MOD_ID = "itemscroller";
    public static final String MOD_NAME = "Item Scroller";
    public static final String MOD_VERSION = MaLiLib.getModVersionString(MOD_ID);
    public static final String MC_VERSION = MinecraftVersion.CURRENT.getName();
    public static final String MOD_TYPE = "fabric";
    public static final String MOD_STRING = MOD_ID+"-"+MOD_TYPE+"-"+MC_VERSION+"-"+MOD_VERSION;
}
