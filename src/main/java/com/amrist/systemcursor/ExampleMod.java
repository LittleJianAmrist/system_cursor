package com.amrist.systemcursor;

import com.amrist.systemcursor.proxy.IProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, guiFactory = "com.amrist.systemcursor.SystemCursorGuiFactory")
public class ExampleMod {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    @SidedProxy(modId = Reference.MOD_ID, clientSide = "com.amrist.systemcursor.proxy.ClientProxy", serverSide = "com.amrist.systemcursor.proxy.CommonProxy")
    public static IProxy proxy;
    /**
     * <a href="https://cleanroommc.com/wiki/forge-mod-development/event#overview">
     *     Take a look at how many FMLStateEvents you can listen to via the @Mod.EventHandler annotation here
     * </a>
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SystemCursorConfig.initialize(event.getSuggestedConfigurationFile());
        LOGGER.info("Hello From {}!", Reference.MOD_NAME);
        LOGGER.info("Proxy is {}", proxy);
        LOGGER.info("Language: {}", Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage());
    }

}
