package cam72cam.mod;

import cam72cam.mod.config.ConfigFile;
import cam72cam.mod.entity.ModdedEntity;
import cam72cam.mod.entity.sync.EntitySync;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.event.CommonEvents;
import cam72cam.mod.gui.GuiRegistry;
import cam72cam.mod.input.Keyboard;
import cam72cam.mod.input.Mouse;
import cam72cam.mod.net.Packet;
import cam72cam.mod.net.PacketDirection;
import cam72cam.mod.render.BlockRender;
import cam72cam.mod.text.Command;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cam72cam.mod.util.ModCoreCommand;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@cpw.mods.fml.common.Mod(modid = ModCore.MODID, name = ModCore.NAME, version = ModCore.VERSION, acceptedMinecraftVersions = "[1.7.10,1.8)")
public class ModCore {
    public static final String MODID = "universalmodcore";
    public static final String NAME = "UniversalModCore";
    public static final String VERSION = "0.1.0";
    public static ModCore instance;
    static List<Supplier<Mod>> modCtrs = new ArrayList<>();

    private List<Mod> mods;
    private Logger logger;

    public static void register(Supplier<Mod> ctr) {
        modCtrs.add(ctr);
    }

    public ModCore() {
        System.out.println("Welcome to UniversalModCore!");
        instance = this;
        mods = modCtrs.stream().map(Supplier::get).collect(Collectors.toList());
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        proxy.event(ModEvent.INITIALIZE);
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.event(ModEvent.SETUP);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.event(ModEvent.FINALIZE);
    }

    @EventHandler
    public void serverStarting(FMLServerStartedEvent event) {
        proxy.event(ModEvent.START);
    }

    public static abstract class Mod {
        public abstract String modID();

        public abstract void commonEvent(ModEvent event);
        public abstract void clientEvent(ModEvent event);
        public abstract void serverEvent(ModEvent event);

        public final Path getConfig(String fname) {
            return Paths.get(Loader.instance().getConfigDir().toString(), fname);
        }

        public static void debug(String msg, Object...params) {
            ModCore.debug(msg, params);
        }
        public static void info(String msg, Object...params) {
            ModCore.info(msg, params);
        }
        public static void warn(String msg, Object...params) {
            ModCore.warn(msg, params);
        }
        public static void error(String msg, Object...params) {
            ModCore.error(msg, params);
        }
        public static void catching(Throwable ex) {
            ModCore.catching(ex);
        }
    }

    @SidedProxy(serverSide = "cam72cam.mod.ModCore$ServerProxy", clientSide = "cam72cam.mod.ModCore$ClientProxy", modId = ModCore.MODID)
    private static Proxy proxy;
    public static class Proxy {
        public Proxy() {
            event(ModEvent.CONSTRUCT);
        }

        public void event(ModEvent event) {
            instance.mods.forEach(m -> m.commonEvent(event));
        }
    }

    public static class ClientProxy extends Proxy {
        public void event(ModEvent event) {
            super.event(event);
            instance.mods.forEach(m -> m.clientEvent(event));
        }
    }

    public static class ServerProxy extends Proxy {
        public void event(ModEvent event) {
            super.event(event);
            instance.mods.forEach(m -> m.serverEvent(event));
        }
    }


    static {
        ModCore.register(Internal::new);
    }

    private static void addHandler(Object handler) {
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
    }

    public static class Internal extends Mod {
        public int skipN = 2;

        @Override
        public String modID() {
            return "universalmodcoreinternal";
        }

        @Override
        public void commonEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:


                    Packet.register(EntitySync.EntitySyncPacket::new, PacketDirection.ServerToClient);
                    Packet.register(Keyboard.MovementPacket::new, PacketDirection.ClientToServer);
                    Packet.register(ModdedEntity.PassengerPositionsPacket::new, PacketDirection.ServerToClient);
                    Packet.register(Mouse.MousePressPacket::new, PacketDirection.ClientToServer);
                    Command.register(new ModCoreCommand());
                    ConfigFile.sync(Config.class);
                    break;
                case INITIALIZE:
                    addHandler(new CommonEvents.EventBus());
                    break;
                case SETUP:
                    World.MAX_ENTITY_RADIUS = Math.max(World.MAX_ENTITY_RADIUS, 32);

                    GuiRegistry.registration();
                    break;
                case START:
                    Command.registration();
                    break;
            }
        }

        @Override
        @SideOnly(Side.CLIENT)
        public void clientEvent(ModEvent event) {
            switch (event) {
                case CONSTRUCT:
                    break;
                case INITIALIZE:
                    addHandler(new ClientEvents.ClientEventBus());
                    break;
                case SETUP:
                    if (Minecraft.getMinecraft().getResourceManager() instanceof SimpleReloadableResourceManager) {
                        // Lambda does not work here!  Don't simplify!
                        ((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(new IResourceManagerReloadListener() {
                            @Override
                            public void onResourceManagerReload(IResourceManager p_110549_1_) {

                                if (skipN > 0) {
                                    skipN--;
                                    return;
                                }
                                ModCore.instance.mods.forEach(mod -> mod.clientEvent(ModEvent.RELOAD));
                            }
                        });
                    } else {
                        error("BAD RESOURCE MANAGER TYPE " + Minecraft.getMinecraft().getResourceManager());
                    }
                    BlockRender.onPostColorSetup();
                    ClientEvents.fireReload();
                    break;
            }

        }

        @Override
        public void serverEvent(ModEvent event) {
        }
    }

    public static void debug(String msg, Object... params) {
        if (Config.DebugLogging) {
            if (instance == null || instance.logger == null) {
                System.out.println("DEBUG: " + String.format(msg, params));
                return;
            }

            instance.logger.info(String.format(msg, params));
        }
    }

    public static void info(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("INFO: " + String.format(msg, params));
            return;
        }

        instance.logger.info(String.format(msg, params));
    }

    public static void warn(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("WARN: " + String.format(msg, params));
            return;
        }

        instance.logger.warn(String.format(msg, params));
    }

    public static void error(String msg, Object... params) {
        if (instance == null || instance.logger == null) {
            System.out.println("ERROR: " + String.format(msg, params));
            return;
        }

        instance.logger.error(String.format(msg, params));
    }

    public static void catching(Throwable ex) {
        if (instance == null || instance.logger == null) {
            ex.printStackTrace();
            return;
        }

        instance.logger.catching(ex);
    }
}
