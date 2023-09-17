package cam72cam.mod.item;

import cam72cam.mod.ModCore;
import cam72cam.mod.event.ClientEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Creates/Registers a creative tab for custom items */
public class CreativeTab {
    public CreativeModeTab internal;

    // TODO expose existing creative tabs as constants to be used by mods

    public List<CustomItem> inject = new ArrayList<>();

    /** */
    public CreativeTab(String label, Supplier<ItemStack> stack) {
        ClientEvents.CREATIVE_TAB.subscribe(event -> {
            internal = event.registerCreativeModeTab(new ResourceLocation(ModCore.MODID, label), builder -> {
                builder.title(Component.literal(label));
                builder.icon(() -> stack.get().internal);
                builder.displayItems((params, output) -> {
                    for (CustomItem customItem : inject) {
                        for (ItemStack itemVariant : customItem.getItemVariants(null)) {
                            output.accept(itemVariant.internal);
                        }
                    }
                });
            });
        });
    }

    /** Wraps minecraft's tabs, don't use directly */
    public CreativeTab(CreativeModeTab tab) {
        this.internal = tab;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CreativeTab && ((CreativeTab)o).internal == this.internal;
    }
}
