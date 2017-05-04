package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockAirFlow extends BlockAbstractAir {
	
	public BlockAirFlow(final String registryName) {
		super(registryName);
	}
	
	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, @Nonnull World world, @Nonnull BlockPos blockPos) {
		if (!world.isRemote) {
			StateAir stateAir = ChunkHandler.getStateAir(world, blockPos.getX(), blockPos.getY(), blockPos.getZ());
			if (!stateAir.isAirSource() || stateAir.concentration == 0) {
				world.setBlockToAir(blockPos);
			}
		}
		return super.getCollisionBoundingBox(blockState, world, blockPos);
	}
}