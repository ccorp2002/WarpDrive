package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.TileEntityAbstractEnergy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.event.ChunkHandler;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;

import net.minecraftforge.fml.common.Optional;

public class TileEntityAirGeneratorTiered extends TileEntityAbstractEnergy {
	
	// persistent properties
	protected byte tier = -1;
	private boolean isEnabled = true;
	
	// computed properties
	private int maxEnergyStored = 0;
	private int cooldownTicks = 0;
	
	public TileEntityAirGeneratorTiered() {
		super();
		
		peripheralName = "warpdriveAirGenerator";
		addMethods(new String[] {
				"enable"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		Block block = getBlockType();
		if (block instanceof BlockAirGeneratorTiered) {
			tier = ((BlockAirGeneratorTiered) block).tier;
			maxEnergyStored = WarpDriveConfig.BREATHING_MAX_ENERGY_STORED[tier - 1];
		} else {
			WarpDrive.logger.error("Missing block for " + this + " at " + worldObj + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
		}
	}
	
	@Override
	public void update() {
		super.update();
		
		if (worldObj.isRemote) {
			return;
		}
		
		if (isInvalid()) {
			return;
		}
		
		// Air generator works only in space & hyperspace
		final int metadata = getBlockMetadata();
		if (WarpDrive.starMap.hasAtmosphere(worldObj, pos.getX(), pos.getZ())) {
			IBlockState blockState = worldObj.getBlockState(pos);
			if (blockState.getValue(BlockProperties.ACTIVE)) {
				worldObj.setBlockState(pos, blockState.withProperty(BlockProperties.ACTIVE, false)); // set disabled texture
			}
			return;
		}
		
		cooldownTicks++;
		if (cooldownTicks > WarpDriveConfig.BREATHING_AIR_GENERATION_TICKS) {
			IBlockState blockState = worldObj.getBlockState(pos);
			if (isEnabled && energy_consume(WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK[tier - 1], true)) {
				if (!blockState.getValue(BlockProperties.ACTIVE)) {
					worldObj.setBlockState(pos, blockState.withProperty(BlockProperties.ACTIVE, true)); // set enabled texture
				}
			} else {
				if (blockState.getValue(BlockProperties.ACTIVE)) {
					worldObj.setBlockState(pos, blockState.withProperty(BlockProperties.ACTIVE, false)); // set disabled texture
				}
			}
			releaseAir(blockState.getValue(BlockProperties.FACING));
			
			cooldownTicks = 0;
		}
	}
	
	private void releaseAir(final EnumFacing direction) {
		final int x = pos.getX() + direction.getFrontOffsetX();
		final int y = pos.getY() + direction.getFrontOffsetY();
		final int z = pos.getZ() + direction.getFrontOffsetZ();
		
		StateAir stateAir = ChunkHandler.getStateAir(worldObj, x, y, z);
		stateAir.updateBlockCache(worldObj);
		if (stateAir.isAir()) {// can be air
			final short range = (short) (WarpDriveConfig.BREATHING_AIR_GENERATION_RANGE_BLOCKS[tier - 1] - 1);
			final int energy_cost = !stateAir.isAirSource() ? WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK[tier - 1] : WarpDriveConfig.BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK[tier - 1];
			if (isEnabled && energy_consume(energy_cost, true)) {// enough energy and enabled
				if (stateAir.setAirSource(worldObj, direction, range)) {
					// (needs to renew air or was not maxed out)
					energy_consume(WarpDriveConfig.BREATHING_ENERGY_PER_NEW_AIR_BLOCK[tier - 1], false);
				} else {
					// (just maintaining)
					energy_consume(WarpDriveConfig.BREATHING_ENERGY_PER_EXISTING_AIR_BLOCK[tier - 1], false);
				}
				
			} else {// low energy => remove air block
				if (stateAir.concentration > 4) {
					stateAir.setConcentration(worldObj, (byte) (stateAir.concentration - 4));
				} else if (stateAir.concentration > 1) {
					stateAir.setConcentration(worldObj, (byte) 1);
				}
			}
		}
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		isEnabled = tag.getBoolean("isEnabled");
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		tag.setBoolean("isEnabled", isEnabled);
		return super.writeToNBT(tag);
	}
	
	@Override
	public int energy_getMaxStorage() {
		return maxEnergyStored;
	}
	
	@Override
	public boolean energy_canInput(EnumFacing from) {
		return true;
	}
	
	@Override
	public String toString() {
		return String.format("%s @ \'%s\' (%d %d %d)",
				getClass().getSimpleName(),
				worldObj == null ? "~NULL~" : worldObj.getWorldInfo().getWorldName(),
				pos.getX(), pos.getY(), pos.getZ());
	}
	
	public Object[] enable(Object[] arguments) {
		if (arguments.length == 1) {
			isEnabled = Commons.toBool(arguments[0]);
		}
		return new Object[] { isEnabled };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "OpenComputers")
	public Object[] enable(Context context, Arguments arguments) {
			return enable(argumentsOCtoCC(arguments));
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public Object[] callMethod(IComputerAccess computer, ILuaContext context, int method, Object[] arguments) {
		String methodName = getMethodName(method);
		
		switch (methodName) {
			case "enable": 
				return enable(arguments);		
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}