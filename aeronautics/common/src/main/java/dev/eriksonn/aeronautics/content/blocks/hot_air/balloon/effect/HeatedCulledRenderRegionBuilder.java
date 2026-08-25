package dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.effect;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.render.region.SimpleCulledRenderRegionBuilder;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class HeatedCulledRenderRegionBuilder extends SimpleCulledRenderRegionBuilder {
    private final BlockPos worldOrigin;
    private final LevelAccelerator accelerator;

    public HeatedCulledRenderRegionBuilder(final BlockPos worldOrigin, final LevelAccelerator accelerator, final int gridSize) {
        super(gridSize);
        this.worldOrigin = worldOrigin;
        this.accelerator = accelerator;
    }

    /**
     * Renders all cubes into the specified consumer.
     *
     * @param consumer The consumer to draw cubes into
     */
    public void render(@NotNull final Matrix4f matrix4f, @NotNull final VertexConsumer consumer) {
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (final Cube cube : this.getCubes()) {
            final int x0 = cube.x();
            final int y0 = cube.y();
            final int z0 = cube.z();
            final int x1 = cube.x() + cube.sizeX();
            final int y1 = cube.y() + cube.sizeY();
            final int z1 = cube.z() + cube.sizeZ();

            if (this.shouldFaceRender(cube, Direction.NORTH)) {
                final Direction dir = Direction.NORTH;
                this.vertex(matrix4f, consumer, x0, y0, z0, 0, 0, dir);
                this.vertex(matrix4f, consumer, x0, y1, z0, 0, 1, dir);
                this.vertex(matrix4f, consumer, x1, y1, z0, 1, 1, dir);
                this.vertex(matrix4f, consumer, x1, y0, z0, 1, 0, dir);
            }

            if (this.shouldFaceRender(cube, Direction.EAST)) {
                final Direction dir = Direction.EAST;
                this.vertex(matrix4f, consumer, x1, y0, z0, 0, 0, dir);
                this.vertex(matrix4f, consumer, x1, y1, z0, 0, 1, dir);
                this.vertex(matrix4f, consumer, x1, y1, z1, 1, 1, dir);
                this.vertex(matrix4f, consumer, x1, y0, z1, 1, 0, dir);
            }

            if (this.shouldFaceRender(cube, Direction.SOUTH)) {
                final Direction dir = Direction.SOUTH;
                this.vertex(matrix4f, consumer, x1, y0, z1, 1, 0, dir);
                this.vertex(matrix4f, consumer, x1, y1, z1, 1, 1, dir);
                this.vertex(matrix4f, consumer, x0, y1, z1, 0, 1, dir);
                this.vertex(matrix4f, consumer, x0, y0, z1, 0, 0, dir);
            }

            if (this.shouldFaceRender(cube, Direction.WEST)) {
                final Direction dir = Direction.WEST;
                this.vertex(matrix4f, consumer, x0, y0, z1, 1, 0, dir);
                this.vertex(matrix4f, consumer, x0, y1, z1, 1, 1, dir);
                this.vertex(matrix4f, consumer, x0, y1, z0, 0, 1, dir);
                this.vertex(matrix4f, consumer, x0, y0, z0, 0, 0, dir);
            }

            if (this.shouldFaceRender(cube, Direction.DOWN)) {
                final Direction dir = Direction.DOWN;
                if (this.accelerator.getBlockState(pos.set(cube.x(), cube.y() - 1, cube.z()).offset(this.worldOrigin)).is(AeroTags.BlockTags.AIRTIGHT)) {
                    this.vertex(matrix4f, consumer, x0, y0, z0, 0, 1, dir);
                    this.vertex(matrix4f, consumer, x1, y0, z0, 1, 1, dir);
                    this.vertex(matrix4f, consumer, x1, y0, z1, 1, 0, dir);
                    this.vertex(matrix4f, consumer, x0, y0, z1, 0, 0, dir);
                }
            }

            if (this.shouldFaceRender(cube, Direction.UP)) {
                final Direction dir = Direction.UP;
                this.vertex(matrix4f, consumer, x0, y1, z1, 0, 1, dir);
                this.vertex(matrix4f, consumer, x1, y1, z1, 1, 1, dir);
                this.vertex(matrix4f, consumer, x1, y1, z0, 1, 0, dir);
                this.vertex(matrix4f, consumer, x0, y1, z0, 0, 0, dir);
            }
        }
    }

    private void vertex(@NotNull final Matrix4f matrix4f, @NotNull final VertexConsumer consumer,
                        final int x, final int y, final int z, final float u, final float v,
                        @NotNull final Direction direction) {
        consumer.vertex(matrix4f, x, y, z)
                .uv(u, v)
                .color(this.getColor(x, y, z))
                .normal(direction.getStepX(), direction.getStepY(), direction.getStepZ())
                .endVertex();
    }

    private int getColor(final int x, final int y, final int z) {
//        if (y == 0) {
//            return 0x77ffffff;
//        }
        return 0xffffffff;
    }
}
