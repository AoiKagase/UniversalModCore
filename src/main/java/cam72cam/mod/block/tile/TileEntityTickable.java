package cam72cam.mod.block.tile;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntityTickable;
import cam72cam.mod.resource.Identifier;

public class TileEntityTickable extends TileEntity {
    public TileEntityTickable() {
        super();
    }

    public TileEntityTickable(Identifier id) {
        super(id);
    }

    private BlockEntityTickable tickable;
    @Override
    public void updateEntity() {
        if (tickable == null) {
            tickable = (BlockEntityTickable) instance();
            if (tickable == null) {
                ModCore.debug("uhhhhh, null tickable?");
                return;
            }
        }
        tickable.update();
    }

    @Override
    public Identifier getName() {
        return new Identifier(ModCore.MODID, "hack_tickable");
    }
}
