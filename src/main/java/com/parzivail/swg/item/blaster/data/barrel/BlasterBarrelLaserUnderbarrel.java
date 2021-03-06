package com.parzivail.swg.item.blaster.data.barrel;

public class BlasterBarrelLaserUnderbarrel extends BlasterBarrel
{
	public BlasterBarrelLaserUnderbarrel()
	{
		super("laser", 325);
	}

	@Override
	public float getHorizontalRecoilReduction()
	{
		return 0;
	}

	@Override
	public float getHorizontalSpreadReduction()
	{
		return 0.5f;
	}

	@Override
	public float getVerticalRecoilReduction()
	{
		return 0;
	}

	@Override
	public float getVerticalSpreadReduction()
	{
		return 0.5f;
	}

	@Override
	public float getNoiseReduction()
	{
		return 0;
	}

	@Override
	public float getRangeIncrease()
	{
		return 0;
	}
}
