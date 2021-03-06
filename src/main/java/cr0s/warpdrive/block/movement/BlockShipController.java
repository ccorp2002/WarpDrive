package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.block.BlockAbstractContainer;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockShipController extends BlockAbstractContainer {
	
	@SideOnly(Side.CLIENT)
	private IIcon[] iconBuffer;
	
	private static final int ICON_INACTIVE_SIDE = 0;
	private static final int ICON_BOTTOM = 1;
	private static final int ICON_TOP = 2;
	private static final int ICON_SIDE_ACTIVATED = 3;
	
	public BlockShipController() {
		super(Material.iron);
		setBlockName("warpdrive.movement.ship_controller");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void registerBlockIcons(final IIconRegister iconRegister) {
		iconBuffer = new IIcon[11];
		// Solid textures
		iconBuffer[ICON_INACTIVE_SIDE] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_inactive");
		iconBuffer[ICON_BOTTOM       ] = iconRegister.registerIcon("warpdrive:movement/ship_controller-bottom");
		iconBuffer[ICON_TOP          ] = iconRegister.registerIcon("warpdrive:movement/ship_controller-top");
		// Animated textures
		iconBuffer[ICON_SIDE_ACTIVATED    ] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active0");
		iconBuffer[ICON_SIDE_ACTIVATED + 1] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active1");
		iconBuffer[ICON_SIDE_ACTIVATED + 2] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active2");
		iconBuffer[ICON_SIDE_ACTIVATED + 3] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active3");
		iconBuffer[ICON_SIDE_ACTIVATED + 4] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active4");
		iconBuffer[ICON_SIDE_ACTIVATED + 5] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active5");
		iconBuffer[ICON_SIDE_ACTIVATED + 6] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active6");
		iconBuffer[ICON_SIDE_ACTIVATED + 7] = iconRegister.registerIcon("warpdrive:movement/ship_controller-side_active7");
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(final IBlockAccess blockAccess, final int x, final int y, final int z, final int side) {
		final int metadata  = blockAccess.getBlockMetadata(x, y, z);
		if (side == 0) {
			return iconBuffer[ICON_BOTTOM];
		} else if (side == 1) {
			return iconBuffer[ICON_TOP];
		}
		
		if (metadata == 0) { // Inactive state
			return iconBuffer[ICON_INACTIVE_SIDE];
		} else if (metadata > 0) { // Activated, in metadata stored mode number
			if (ICON_SIDE_ACTIVATED + metadata < iconBuffer.length) {
				return iconBuffer[ICON_SIDE_ACTIVATED + metadata];
			}
		}
		
		return null;
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public IIcon getIcon(final int side, final int metadata) {
		if (side == 0) {
			return iconBuffer[ICON_BOTTOM];
		} else if (side == 1) {
			return iconBuffer[ICON_TOP];
		}
		
		return iconBuffer[ICON_SIDE_ACTIVATED + 5];
	}
	
	@Override
	public TileEntity createNewTileEntity(final World world, final int metadata) {
		return new TileEntityShipController();
	}
	
	@Override
	public boolean onBlockActivated(final World world, final int x, final int y, final int z,
	                                final EntityPlayer entityPlayer,
	                                final int side, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return false;
		}
		
		if (entityPlayer.getHeldItem() == null) {
			final TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileEntityShipController) {
				if (entityPlayer.isSneaking()) {
					Commons.addChatMessage(entityPlayer, ((TileEntityShipController) tileEntity).getStatus());
				} else {
					Commons.addChatMessage(entityPlayer, ((TileEntityShipController) tileEntity).attachPlayer(entityPlayer));
				}
				return true;
			}
		}
		
		return false;
	}
}