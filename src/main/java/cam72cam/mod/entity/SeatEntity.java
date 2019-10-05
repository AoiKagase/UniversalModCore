package cam72cam.mod.entity;

import cam72cam.mod.ModCore;
import cam72cam.mod.util.TagCompound;
import cam72cam.mod.world.World;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

public class SeatEntity extends Entity implements IEntityAdditionalSpawnData {
    public static EntityType<SeatEntity> TYPE;
    static final Identifier ID = new Identifier(ModCore.MODID, "seat");
    private UUID parent;
    private int ticksUnsure = 0;
    boolean shouldSit = true;

    public SeatEntity(EntityType<SeatEntity> type, net.minecraft.world.World worldIn) {
        super(type, worldIn);
    }

    @Override
    protected void entityInit() {

    }

    @Override
    protected void readEntityFromNBT(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        parent = data.getUUID("parent");
        shouldSit = data.getBoolean("shouldSit");
    }

    @Override
    protected void writeEntityToNBT(CompoundTag compound) {
        TagCompound data = new TagCompound(compound);
        data.setUUID("parent", parent);
        data.setBoolean("shouldSit", shouldSit);
    }

    @Override
    public void onUpdate() {
        if (parent == null) {
            System.out.println("No parent, goodbye");
            this.setDead();
            return;
        }
        if (getPassengers().isEmpty()) {
            System.out.println("No passengers, goodbye");
            this.setDead();
            return;
        }
        if (ticksUnsure > 10) {
            System.out.println("Parent not loaded, goodbye");
            this.setDead();
            return;
        }

        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ticksUnsure = 0;
        } else {
            ticksUnsure++;
        }
    }

    public void setParent(ModdedEntity moddedEntity) {
        this.parent = moddedEntity.getUniqueID();
    }

    public cam72cam.mod.entity.Entity getParent() {
        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            return linked;
        }
        return null;
    }

    @Override
    public double getMountedYOffset() {
        return 0;
    }

    @Override
    public final void updatePassenger(net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).updateSeat(this);
        }
    }

    @Override
    public boolean shouldRiderSit() {
        return shouldSit;
    }

    @Override
    public final void removePassenger(net.minecraft.entity.Entity passenger) {
        cam72cam.mod.entity.Entity linked = World.get(world).getEntity(parent, cam72cam.mod.entity.Entity.class);
        if (linked != null && linked.internal instanceof ModdedEntity) {
            ((ModdedEntity) linked.internal).removeSeat(this);
        }
        super.removePassenger(passenger);
    }

    public cam72cam.mod.entity.Entity getEntityPassenger() {
        if (this.isDead) {
            return null;
        }
        if (this.getPassengers().size() == 0) {
            return null;
        }
        return World.get(world).getEntity(getPassengers().get(0));
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        TagCompound data = new TagCompound();
        data.setUUID("parent", parent);
        ByteBufUtils.writeTag(buffer, data.internal);
    }

    @Override
    public void readSpawnData(ByteBuf additionalData) {
        TagCompound data = new TagCompound(ByteBufUtils.readTag(additionalData));
        parent = data.getUUID("parent");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return false;
    }
}
