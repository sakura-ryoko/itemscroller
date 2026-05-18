package fi.dy.masa.itemscroller;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.TickHandler;
import fi.dy.masa.malilib.event.WorldLoadHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import fi.dy.masa.malilib.util.i18n.i18nMode;
import fi.dy.masa.itemscroller.config.Configs;
import fi.dy.masa.itemscroller.event.InputHandler;
import fi.dy.masa.itemscroller.event.KeybindCallbacks;
import fi.dy.masa.itemscroller.event.WorldLoadListener;
import fi.dy.masa.itemscroller.gui.GuiConfigs;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new)
        );
	    Configs.LANG.ifPresent(
                i18nManager ->
                        Registry.TRANSLATION_OVERRIDE_MANAGER.registerTranslationManager(Reference.MOD_ID, i18nManager,
                                                                                         (i18nMode) Configs.Generic.TRANSLATION_MODE.getOptionListValue())
        );
        Configs.Generic.TRANSLATION_MODE.setValueChangeCallback(
                cfg ->
                        Registry.TRANSLATION_OVERRIDE_MANAGER.registerLanguageMode(Reference.MOD_ID, (i18nMode) cfg.getOptionListValue())
        );

        InputHandler handler = new InputHandler();
        InputEventHandler.getKeybindManager().registerKeybindProvider(handler);
        InputEventHandler.getInputManager().registerKeyboardInputHandler(handler);
        InputEventHandler.getInputManager().registerMouseInputHandler(handler);

        WorldLoadListener listener = new WorldLoadListener();
        WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
        WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

        TickHandler.getInstance().registerClientTickHandler(KeybindCallbacks.getInstance());

        KeybindCallbacks.getInstance().setCallbacks();
    }
}
