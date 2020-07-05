package cam72cam.mod.fluid;

import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import cam72cam.mod.serialization.TagCompound;

import java.util.List;
import java.util.function.Supplier;

public class FluidTank implements ITank {
    // TODO clean up capacity crap.  Probably just want to implement my own fluid handler from scratch TBH
    public final net.minecraftforge.fluids.capability.templates.FluidTank internal;

    private Supplier<List<Fluid>> filter;
    private Runnable onChange = () -> {};

    private FluidTank() {
        // Serialization
        this(null, 0);
    }

    public FluidTank(FluidStack fluidStack, int capacity) {
        internal = new net.minecraftforge.fluids.capability.templates.FluidTank(capacity) {
            public void onContentsChanged() {
                FluidTank.this.onChange.run();
            }
        };
        if (fluidStack != null) {
            internal.setFluid(fluidStack.internal);
        }
    }

    public void onChanged(Runnable onChange) {
        this.onChange = onChange;
    }

    @Override
    public FluidStack getContents() {
        return new FluidStack(internal.getFluid());
    }

    @Override
    public int getCapacity() {
        return internal.getCapacity();
    }

    public void setCapacity(int milliBuckets) {
        if (internal.getFluidAmount() > milliBuckets) {
            internal.drain(internal.getFluidAmount() - milliBuckets, FluidAction.EXECUTE);
        }
        internal.setCapacity(milliBuckets);
    }

    /**
     * null == all
     * [] == none
     */
    public void setFilter(Supplier<List<Fluid>> filter) {
        this.filter = filter;
    }

    @Override
    public boolean allows(Fluid fluid) {
        return (filter == null || filter.get() == null || filter.get().contains(fluid)) &&
                internal.isFluidValid(new net.minecraftforge.fluids.FluidStack(fluid.internal, 1));
    }

    @Override
    public int fill(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return 0;
        }
        return internal.fill(fluidStack.internal, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }

    @Override
    public FluidStack drain(FluidStack fluidStack, boolean simulate) {
        if (!allows(fluidStack.getFluid())) {
            return null;
        }
        return new FluidStack(internal.drain(fluidStack.internal, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE));
    }

    public TagCompound write(TagCompound tag) {
        return new TagCompound(internal.writeToNBT(tag.internal));
    }

    public void read(TagCompound tag) {
        internal.readFromNBT(tag.internal);
    }

    public boolean tryDrain(ITank inputTank, int max, boolean simulate) {
        int maxTransfer = this.fill(inputTank.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or limit too small
            return false;
        }

        FluidStack attemptedDrain = inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), true);

        if (attemptedDrain == null || attemptedDrain.getAmount() != maxTransfer) {
            // Can't transfer the full amount
            return false;
        }

        // Either attempt or do fill
        boolean ok = this.fill(attemptedDrain, simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            inputTank.drain(new FluidStack(inputTank.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }

    public boolean tryFill(ITank inputTank, int max, boolean simulate) {
        int maxTransfer = inputTank.fill(this.getContents(), true);
        maxTransfer = Math.min(maxTransfer, max);

        if (maxTransfer == 0) {
            // Out of room or limit too small
            return false;
        }

        FluidStack attemptedDrain = this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), true);

        if (attemptedDrain == null || attemptedDrain.getAmount() != maxTransfer) {
            // Can't transfer the full amount
            return false;
        }

        // Either attempt or do fill
        boolean ok = inputTank.fill(attemptedDrain, simulate) == attemptedDrain.getAmount();

        if (!simulate) {
            // Drain input tank
            this.drain(new FluidStack(this.getContents().getFluid(), maxTransfer), false);
        }
        return ok;
    }
}
