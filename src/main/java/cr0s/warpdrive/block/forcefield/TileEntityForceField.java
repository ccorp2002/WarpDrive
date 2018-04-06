package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBeamFrequency;
import cr0s.warpdrive.block.TileEntityAbstractBase;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.ForceFieldSetup;
import cr0s.warpdrive.data.VectorI;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class TileEntityForceField extends TileEntityAbstractBase {
	
	private VectorI vProjector;
	
	// cache parameters used for rendering, force projector check for others
	private int cache_beamFrequency;
	public Block cache_blockCamouflage;
	public int cache_metadataCamouflage;
	protected int cache_colorMultiplierCamouflage;
	protected int cache_lightCamouflage;
	
	// number of projectors check ignored before self-destruction
	private int gracePeriod_calls = 3;
	
	@Override
	public boolean canUpdate() {
		return false;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		if (tagCompound.hasKey("projector")) {
			vProjector = VectorI.createFromNBT(tagCompound.getCompoundTag("projector"));
			cache_beamFrequency = tagCompound.getInteger(IBeamFrequency.BEAM_FREQUENCY_TAG);
			if (tagCompound.hasKey("camouflageBlock")) {
				try {
					cache_blockCamouflage = Block.getBlockFromName(tagCompound.getString("camouflageBlock"));
					cache_metadataCamouflage = tagCompound.getByte("camouflageMeta");
					cache_colorMultiplierCamouflage = tagCompound.getInteger("camouflageColorMultiplier");
					cache_lightCamouflage = tagCompound.getByte("camouflageLight");
					if (Dictionary.BLOCKS_NOCAMOUFLAGE.contains(cache_blockCamouflage)) {
						cache_blockCamouflage = null;
						cache_metadataCamouflage = 0;
						cache_colorMultiplierCamouflage = 0;
						cache_lightCamouflage = 0;
					}
				} catch (Exception exception) {
					exception.printStackTrace();
				}
			} else {
				cache_blockCamouflage = null;
				cache_metadataCamouflage = 0;
				cache_colorMultiplierCamouflage = 0;
				cache_lightCamouflage = 0;
			}
		} else {
			vProjector = null;
			cache_beamFrequency = -1;
			cache_blockCamouflage = null;
			cache_metadataCamouflage = 0;
			cache_colorMultiplierCamouflage = 0;
			cache_lightCamouflage = 0;
		}
	}
	
	@Override
	public void writeToNBT(final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);
		if (vProjector != null) {
			tagCompound.setTag("projector", vProjector.writeToNBT(new NBTTagCompound()));
			tagCompound.setInteger(IBeamFrequency.BEAM_FREQUENCY_TAG, cache_beamFrequency);
			if (cache_blockCamouflage != null) {
				tagCompound.setString("camouflageBlock", Block.blockRegistry.getNameForObject(cache_blockCamouflage));
				tagCompound.setByte("camouflageMeta", (byte) cache_metadataCamouflage);
				tagCompound.setInteger("camouflageColorMultiplier", cache_colorMultiplierCamouflage);
				tagCompound.setByte("camouflageLight", (byte) cache_lightCamouflage);
			}
		}
	}
	
	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, -1, tagCompound);
	}
	
	@Override
	public void onDataPacket(final NetworkManager networkManager, final S35PacketUpdateTileEntity packet) {
		NBTTagCompound tagCompound = packet.func_148857_g();
		readFromNBT(tagCompound);
	}
	
	public void setProjector(final VectorI vectorI) {
		vProjector = vectorI;
		ForceFieldSetup forceFieldSetup = getForceFieldSetup();
		if (forceFieldSetup != null) {
			cache_beamFrequency = forceFieldSetup.beamFrequency;
			if (getBlockMetadata() == forceFieldSetup.getCamouflageMetadata()) {
				cache_blockCamouflage = forceFieldSetup.getCamouflageBlock();
				cache_metadataCamouflage = forceFieldSetup.getCamouflageMetadata();
				cache_colorMultiplierCamouflage = forceFieldSetup.getCamouflageColorMultiplier();
				cache_lightCamouflage = forceFieldSetup.getCamouflageLight();
			}
		}
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}
	
	public TileEntityForceFieldProjector getProjector() {
		if (vProjector != null) {
			TileEntity tileEntity = vProjector.getTileEntity(worldObj);
			if (tileEntity instanceof TileEntityForceFieldProjector) {
				TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector) tileEntity;
				if (worldObj.isRemote) {
					return tileEntityForceFieldProjector;
					
				} else if (tileEntityForceFieldProjector.isPartOfForceField(new VectorI(this))) {
					if (tileEntityForceFieldProjector.isOn()) {
						return tileEntityForceFieldProjector;
					} else {
						// projector is disabled or out of power
						worldObj.setBlockToAir(xCoord, yCoord, zCoord);
						if (WarpDriveConfig.LOGGING_FORCEFIELD) {
							WarpDrive.logger.info(String.format("Removed a force field from an offline projector @ %s (%d %d %d)", 
							                                    worldObj == null ? "~NULL~" : worldObj.provider.getDimensionName(),
							                                    xCoord, yCoord, zCoord));
						}
					}
				}
			}
		}
		
		if (!worldObj.isRemote) {
			gracePeriod_calls--;
			if (gracePeriod_calls < 0) {
				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
				if (WarpDriveConfig.LOGGING_FORCEFIELD) {
					WarpDrive.logger.info(String.format("Removed a force field with no projector defined @ %s (%d %d %d)",
					                                    worldObj == null ? "~NULL~" : worldObj.provider.getDimensionName(),
					                                    xCoord, yCoord, zCoord));
				}
			}
		}
		
		return null;
	}
	
	public ForceFieldSetup getForceFieldSetup() {
		final TileEntityForceFieldProjector tileEntityForceFieldProjector = getProjector();
		if (tileEntityForceFieldProjector == null) {
			return null;
		}
		return tileEntityForceFieldProjector.getForceFieldSetup();
	}
}
