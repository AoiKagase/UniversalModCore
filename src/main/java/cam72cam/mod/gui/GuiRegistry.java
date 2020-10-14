package cam72cam.mod.gui;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.entity.Entity;
import cam72cam.mod.entity.Player;
import cam72cam.mod.gui.container.ClientContainerBuilder;
import cam72cam.mod.gui.container.IContainer;
import cam72cam.mod.gui.container.ServerContainerBuilder;
import cam72cam.mod.gui.screen.IScreen;
import cam72cam.mod.gui.screen.ScreenBuilder;
import cam72cam.mod.math.Vec3i;
import cam72cam.mod.resource.Identifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.CRC32;


public class GuiRegistry {
    private static Map<Integer, Function<CreateEvent, Object>> registry = new HashMap<>();

    public GuiRegistry() {
    }

    public static void registration() {
        NetworkRegistry.INSTANCE.registerGuiHandler(ModCore.instance, new IGuiHandler() {
            @Nullable
            @Override
            public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
                return registry.get(ID).apply(new CreateEvent(true, new Player(player), x, y, z));
            }

            @Nullable
            @Override
            public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
                return registry.get(ID).apply(new CreateEvent(false, new Player(player), x, y, z));
            }
        });
    }

    @FunctionalInterface
    public interface GUI {
        void open(Player player);
    }

    @FunctionalInterface
    public interface EntityGUI {
        void open(Player player, Entity entity);
    }
    @FunctionalInterface
    public interface BlockGUI {
        void open(Player player, Vec3i pos);
    }

    private static int intFromName(String s) {
        CRC32 hasher = new CRC32();
        hasher.update(s.length());
        hasher.update(s.getBytes());
        return (int) hasher.getValue();
    }

    public static GUI register(Identifier name, Supplier<IScreen> ctr) {
        int id = intFromName(name.toString());
        registry.put(id, event -> {
            if (event.isServer) {
                return null;
            }
            return new ScreenBuilder(ctr.get());
        });
        return (player) -> player.internal.openGui(ModCore.instance, id, player.getWorld().internal, 0, 0, 0);
    }

    public static <T extends BlockEntity> BlockGUI registerBlock(Class<T> cls, Function<T, IScreen> ctr) {
        int id = intFromName(cls.toString());
        registry.put(id, event -> {
            if (event.isServer) {
                return null;
            }
            T entity = event.player.getWorld().getBlockEntity(new Vec3i(event.entityIDorX, event.y, event.z), cls);
            if (entity == null) {
                return null;
            }
            IScreen screen = ctr.apply(entity);
            if (screen == null) {
                return null;
            }

            return new ScreenBuilder(screen);
        });
        return (player, pos) -> player.internal.openGui(ModCore.instance, id, player.getWorld().internal, pos.x, pos.y, pos.z);
    }

    public static <T extends Entity> EntityGUI registerEntityContainer(Class<T> cls, Function<T, IContainer> ctr) {
        int id = intFromName(("container" + cls.toString()));
        registry.put(id, event -> {
            T entity = event.player.getWorld().getEntity(event.entityIDorX, cls);
            if (entity == null) {
                return null;
            }
            ServerContainerBuilder server = new ServerContainerBuilder(event.player.internal.inventory, ctr.apply(entity));
            if (event.isServer) {
                return server;
            }
            return new ClientContainerBuilder(server);
        });
        return (player, ent) -> player.internal.openGui(ModCore.instance, id, player.getWorld().internal, ent.internal.getEntityId(), 0, 0);
    }

    public static <T extends BlockEntity> BlockGUI registerBlockContainer(Class<T> cls, Function<T, IContainer> ctr) {
        int id = intFromName(("container" + cls.toString()));
        registry.put(id, event -> {
            T entity = event.player.getWorld().getBlockEntity(new Vec3i(event.entityIDorX, event.y, event.z), cls);
            if (entity == null) {
                return null;
            }
            ServerContainerBuilder server = new ServerContainerBuilder(event.player.internal.inventory, ctr.apply(entity));
            if (event.isServer) {
                return server;
            }
            return new ClientContainerBuilder(server);
        });
        return (player, pos) -> player.internal.openGui(ModCore.instance, id, player.getWorld().internal, pos.x, pos.y, pos.z);
    }

    private static class CreateEvent {
        final boolean isServer;
        final Player player;
        final int entityIDorX;
        final int y;
        final int z;

        private CreateEvent(boolean isServer, Player player, int entityIDorX, int y, int z) {
            this.isServer = isServer;
            this.player = player;
            this.entityIDorX = entityIDorX;
            this.y = y;
            this.z = z;
        }
    }
}
