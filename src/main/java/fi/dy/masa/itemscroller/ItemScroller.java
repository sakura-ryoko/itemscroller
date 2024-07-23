package fi.dy.masa.itemscroller;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.itemscroller.config.Configs;

public class ItemScroller implements ModInitializer
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    @Override
    public void onInitialize()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    /*
    public static void printDebug(String key, Object... args)
    {
        if (Configs.Generic.DEBUG_MESSAGES.getBooleanValue())
        {
            logger.info(key, args);
        }
    }
     */
}
