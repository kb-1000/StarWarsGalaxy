package com.parzivail.swg.block.light;

import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.tile.light.TileAngledWallLight;
import com.parzivail.util.block.HarvestLevel;
import com.parzivail.util.block.PBlockRotate;
import com.parzivail.util.block.TileRotatable;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAngledWallLight extends PBlockRotate
{
	public BlockAngledWallLight()
	{
		super("angledWallLight", Material.iron, 8);
		setCreativeTab(StarWarsGalaxy.tab);
		setHardness(50.0F);
		setLightLevel(1);
		this.setHarvestLevel("pickaxe", HarvestLevel.IRON);
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess p_149719_1_, int p_149719_2_, int p_149719_3_, int p_149719_4_)
	{
		if (p_149719_1_.getTileEntity(p_149719_2_, p_149719_3_, p_149719_4_) instanceof TileRotatable)
		{
			int meta = Math.abs((int)((TileRotatable)p_149719_1_.getTileEntity(p_149719_2_, p_149719_3_, p_149719_4_)).getFacing());
			switch (meta)
			{
				case 1:
					setBlockBounds(0.9f, 0.35f, 0.2f, 1f, 0.7f, 0.8f);
					break;
				case 2:
					setBlockBounds(0.2f, 0.35f, 0f, 0.8f, 0.7f, 0.1f);
					break;
				case 3:
					setBlockBounds(0f, 0.35f, 0.2f, 0.1f, 0.7f, 0.8f);
					break;
				case 4:
					setBlockBounds(0.2f, 0.35f, 0.9f, 0.8f, 0.7f, 1f);
					break;
			}
		}
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta)
	{
		return new TileAngledWallLight();
	}

	@Override
	public int getRenderType()
	{
		return -1;
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}
}