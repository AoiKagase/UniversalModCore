package cam72cam.mod.render;

import cam72cam.mod.ModCore;
import cam72cam.mod.block.BlockEntity;
import cam72cam.mod.block.BlockType;
import cam72cam.mod.block.BlockTypeEntity;
import cam72cam.mod.block.tile.TileEntity;
import cam72cam.mod.event.ClientEvents;
import cam72cam.mod.resource.Identifier;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.GrassColors;
import net.minecraft.world.biome.BiomeColors;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry for block rendering (and internal implementation)
 *
 * Currently only supports TE's, not standard blocks
 */
public class BlockRender {
    // Don't need to return a *new* array list for no result
    private static final List<BakedQuad> EMPTY = Collections.emptyList();
    // Block coloring (grass) hooks
    private static final List<Runnable> colors = new ArrayList<>();
    // BlockEntity type -> BlockEntity Renderer
    private static final Map<Identifier, Function<BlockEntity, StandardModel>> renderers = new HashMap<>();
    // Internal hack for globally rendered TE's
    private static List<net.minecraft.tileentity.TileEntity> prev = new ArrayList<>();

    static {
        ClientEvents.TICK.subscribe(() -> {
            if (Minecraft.getInstance().world == null) {
                return;
            }
            /*
            Find all UMC TEs
            Create new array to prevent CME's with poorly behaving mods
            TODO: Opt out of renderGlobal!
             */
            List<net.minecraft.tileentity.TileEntity> tes = new ArrayList<>(Minecraft.getInstance().world.loadedTileEntityList).stream()
                    .filter(x -> x instanceof TileEntity && ((TileEntity) x).isLoaded())
                    .collect(Collectors.toList());
            Minecraft.getInstance().worldRenderer.updateTileEntities(prev, tes);
            prev = tes;
        });
    }

    /** Internal, do not use.  Is fired by UMC directly */
    public static void onPostColorSetup() {
        colors.forEach(Runnable::run);

        renderers.forEach((type, render) -> {
            ClientRegistry.bindTileEntityRenderer(TileEntity.getType(type), (ted) -> new TileEntityRenderer<TileEntity>(ted) {
                @Override
                public void render(TileEntity te, float partialTicks, MatrixStack var3, IRenderTypeBuffer var4, int combinedLightIn, int var6) {
                    if (ModCore.isInReload()) {
                        return;
                    }

                    BlockEntity instance = te.instance();
                    if (instance == null) {
                        return;
                    }
                    Class<? extends BlockEntity> cls = instance.getClass();
                    StandardModel model = render.apply(instance);
                    if (model == null) {
                        return;
                    }

                    if (!model.hasCustom()) {
                        return;
                    }

                    RenderType.getSolid().setupRenderState();

                    RenderHelper.enableStandardItemLighting();

                    Minecraft.getInstance().gameRenderer.getLightTexture().enableLightmap();

                    int j = combinedLightIn % 65536;
                    int k = combinedLightIn / 65536;
                    GL13.glMultiTexCoord2f(33986, (float)j, (float)k);

                    try (OpenGL.With matrix = OpenGL.matrix()) {
                        //TODO 1.15 lerp xyz
                        RenderSystem.multMatrix(var3.getLast().getMatrix());
                        model.renderCustom(partialTicks);
                    }

                    RenderType.getSolid().clearRenderState();
                }

                public boolean isGlobalRenderer(TileEntity te) {
                    return true;
                }
            });
        });

    }

    // TODO version for non TE blocks

    private interface IBakedThingy extends IForgeBakedModel, IBakedModel {

    }

    public static <T extends BlockEntity> void register(BlockType block, Function<T, StandardModel> model, Class<T> cls) {
        renderers.put(((BlockTypeEntity)block).id, (te) -> model.apply(cls.cast(te)));

        colors.add(() -> {
            BlockColors blockColors = Minecraft.getInstance().getBlockColors();
            blockColors.register((state, worldIn, pos, tintIndex) -> worldIn != null && pos != null ? BiomeColors.getGrassColor(worldIn, pos) : GrassColors.get(0.5D, 1.0D), block.internal);
        });

        ClientEvents.MODEL_BAKE.subscribe(event -> {
            event.getModelRegistry().put(new ModelResourceLocation(block.internal.getRegistryName(), ""), new IBakedThingy() {
                @Override
                public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand, IModelData properties) {
                    if (block instanceof BlockTypeEntity) {
                        TileEntity data = properties.getData(TileEntity.TE_PROPERTY);
                        if (data == null || !cls.isInstance(data.instance())) {
                            System.out.println(data);
                            return EMPTY;
                        }
                        StandardModel out = model.apply(cls.cast(data.instance()));
                        if (out == null) {
                            return EMPTY;
                        }
                        return out.getQuads(side, rand);
                    } else {
                        // TODO
                        return EMPTY;
                    }
                }

                @Override
                public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) {
                    return EMPTY;
                }

                @Override
                public boolean isAmbientOcclusion() {
                    return true;
                }

                @Override
                public boolean isGui3d() {
                    return true;
                }

                @Override
                public boolean isSideLit() {
                    return false;
                }

                @Override
                public boolean isBuiltInRenderer() {
                    return false;
                }

                @Override
                public TextureAtlasSprite getParticleTexture() {
                    if (block.internal.getMaterialColor() == Material.IRON.getColor()) {
                        return Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(Blocks.IRON_BLOCK.getDefaultState()).getParticleTexture();
                    }
                    return Minecraft.getInstance().getBlockRendererDispatcher().getBlockModelShapes().getModel(Blocks.STONE.getDefaultState()).getParticleTexture();
                }

                @Override
                public ItemOverrideList getOverrides() {
                    return null;
                }
            });
        });
    }
}
