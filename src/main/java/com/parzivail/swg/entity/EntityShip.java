package com.parzivail.swg.entity;

import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.network.client.MessageSetShipInput;
import com.parzivail.util.math.RotatedAxes;
import com.parzivail.util.math.lwjgl.Vector3f;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.util.EnumHand;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Rotations;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class EntityShip extends Entity
{
	private static final DataParameter<Rotations> ROTATION = EntityDataManager.createKey(EntityShip.class, DataSerializers.ROTATIONS);
	private static final DataParameter<Float> THROTTLE = EntityDataManager.createKey(EntityShip.class, DataSerializers.FLOAT);

	private boolean leftInputDown;
	private boolean rightInputDown;
	private boolean forwardInputDown;
	private boolean backInputDown;

	public EntityShip(World worldIn)
	{
		super(worldIn);
		this.setSize(1, 1);
	}

	public EntityShip(World worldIn, double x, double y, double z)
	{
		this(worldIn);
		this.setPosition(x, y, z);
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.prevPosX = x;
		this.prevPosY = y;
		this.prevPosZ = z;
	}

	@Override
	protected void entityInit()
	{
		this.dataManager.register(THROTTLE, 0f);
		this.dataManager.register(ROTATION, new Rotations(0, 0, 0));
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound)
	{
		setThrottle(compound.getFloat("throttle"));
		dataManager.set(ROTATION, new Rotations(compound.getTagList("rotation", 5)));
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound)
	{
		compound.setTag("rotation", dataManager.get(ROTATION).writeToNBT());
		compound.setFloat("throttle", getThrottle());
	}

	@Nullable
	@Override
	public AxisAlignedBB getCollisionBox(Entity entityIn)
	{
		return entityIn.getEntityBoundingBox();
	}

	@Nullable
	@Override
	public AxisAlignedBB getCollisionBoundingBox()
	{
		return this.getEntityBoundingBox();
	}

	@Override
	public boolean canBeCollidedWith()
	{
		return !this.isDead;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount)
	{
		if (!this.world.isRemote && !this.isDead)
		{
			if (source instanceof EntityDamageSourceIndirect && source.getTrueSource() != null && this.isPassenger(source.getTrueSource()))
				return false;
			else
			{
				this.setDead();
				return true;
			}
		}
		else
			return true;
	}

	@Nullable
	@Override
	public Entity getControllingPassenger()
	{
		List<Entity> list = this.getPassengers();
		return list.isEmpty() ? null : list.get(0);
	}

	@Override
	public void onUpdate()
	{
		this.prevPosX = this.posX;
		this.prevPosY = this.posY;
		this.prevPosZ = this.posZ;
		super.onUpdate();

		if (this.canPassengerSteer())
		{
			Entity controllingPassenger = getControllingPassenger();
			if (controllingPassenger instanceof EntityPlayerSP && this.world.isRemote)
			{
				MovementInput input = ((EntityPlayerSP)controllingPassenger).movementInput;
				StarWarsGalaxy.NETWORK.sendToServer(new MessageSetShipInput(this, input.forwardKeyDown, input.backKeyDown, input.leftKeyDown, input.rightKeyDown));
			}

			this.updateMotion();
		}
		else
		{
			this.motionX = 0.0D;
			this.motionY = 0.0D;
			this.motionZ = 0.0D;
		}

		if (!world.isRemote)
			this.control();

		this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

		//this.doBlockCollisions();
	}

	public void setInputs(boolean forwardInputDown, boolean backInputDown, boolean leftInputDown, boolean rightInputDown)
	{
		this.forwardInputDown = forwardInputDown;
		this.backInputDown = backInputDown;
		this.leftInputDown = leftInputDown;
		this.rightInputDown = rightInputDown;
	}

	private void control()
	{
		if (this.isBeingRidden())
		{
			float f = 0.0F;

			if (this.leftInputDown)
			{
				rotateYaw(3);
			}

			if (this.rightInputDown)
			{
				rotateYaw(-3);
			}

			if (this.forwardInputDown)
			{
				//				rotatePitch(3);
				float throttle = getThrottle();
				throttle = MathHelper.clamp(throttle + 0.1f, 0, 1);
				setThrottle(throttle);
			}

			if (this.backInputDown)
			{
				//				rotatePitch(-3);
				float throttle = getThrottle();
				throttle = MathHelper.clamp(throttle - 0.1f, 0, 1);
				setThrottle(throttle);
			}

			//			Entity controllingPassenger = getControllingPassenger();
			//			if (controllingPassenger instanceof EntityPlayer)
			//			{
			//				EntityPlayer player = (EntityPlayer)controllingPassenger;
			//				dataManager.set(ROTATION, new Rotations(player.rotationYaw, 0, 0));
			//			}
		}
	}

	private void rotateYaw(float amount)
	{
		changeOrientation(axes -> axes.rotateGlobalYaw(amount));
	}

	private void rotatePitch(float amount)
	{
		changeOrientation(axes -> axes.rotateLocalPitch(amount));
	}

	private void changeOrientation(Consumer<RotatedAxes> func)
	{
		Rotations angles = dataManager.get(ROTATION);
		RotatedAxes axes = new RotatedAxes(angles.getY(), angles.getX(), angles.getZ());
		func.accept(axes);
		dataManager.set(ROTATION, new Rotations(axes.getPitch(), axes.getYaw(), axes.getRoll()));
	}

	public RotatedAxes getRotation()
	{
		Rotations angles = dataManager.get(ROTATION);
		return new RotatedAxes(angles.getY(), angles.getX(), angles.getZ());
	}

	private void setThrottle(float throttle)
	{
		dataManager.set(THROTTLE, throttle);
	}

	private float getThrottle()
	{
		return dataManager.get(THROTTLE);
	}

	private void updateMotion()
	{
		double d1 = this.hasNoGravity() ? 0.0D : -0.03999999910593033D;

		//this.motionY += d1;

		float throttle = getThrottle();
		if (throttle > 0)
		{
			RotatedAxes rotatedAxes = getRotation();
			Vector3f forward = rotatedAxes.findLocalVectorGlobally(new Vector3f(1, 0, 0));

			this.motionX = forward.x * throttle * 4;
			this.motionY = forward.y * throttle * 4;
			this.motionZ = forward.z * throttle * 4;
		}
		else
		{
			this.motionX = 0;
			this.motionY = 0;
			this.motionZ = 0;
		}
	}

	@Override
	public void updatePassenger(Entity passenger)
	{
		if (this.isPassenger(passenger))
		{
			passenger.setPosition(this.posX, this.posY, this.posZ);
			//this.applyYawToEntity(passenger);
		}
	}

	protected void applyYawToEntity(Entity entityToUpdate)
	{
		entityToUpdate.setRenderYawOffset(this.rotationYaw);
		float f = MathHelper.wrapDegrees(entityToUpdate.rotationYaw - this.rotationYaw);
		float f1 = MathHelper.clamp(f, -105.0F, 105.0F);
		entityToUpdate.prevRotationYaw += f1 - f;
		entityToUpdate.rotationYaw += f1 - f;
		entityToUpdate.setRotationYawHead(entityToUpdate.rotationYaw);
	}

	/**
	 * Applies this entity's orientation (pitch/yaw) to another entity. Used to update passenger orientation.
	 */
	@SideOnly(Side.CLIENT)
	@Override
	public void applyOrientationToEntity(Entity entityToUpdate)
	{
		//this.applyYawToEntity(entityToUpdate);
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand)
	{
		if (player.isSneaking())
			return false;
		else
		{
			if (!this.world.isRemote)
				player.startRiding(this);

			return true;
		}
	}
}