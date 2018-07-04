package com.parzivail.swg.ship;

import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.handler.KeyHandler;
import com.parzivail.swg.network.MessageFlightModelUpdate;
import com.parzivail.util.common.Lumberjack;
import com.parzivail.util.entity.EntityUtils;
import com.parzivail.util.math.RotatedAxes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Vector3f;

import java.util.UUID;

public class MultipartFlightModel extends Entity
{
	private EntitySeat[] seats;
	private UUID[] searchingSeats;

	public RotatedAxes orientation;
	public RotatedAxes previousOrientation;
	public Vector3f angularMomentum;
	public float throttle;

	/**
	 * Distance from the bottom of the model to the "center" of it, from which it will rotate in a roll
	 */
	public float verticalCenteringOffset;
	/**
	 * Distance we need to translate the model up to make sure the bottom is in line with the entity bottom
	 */
	public float verticalGroundingOffset;

	public ShipData data;

	public MultipartFlightModel(World world)
	{
		super(world);
		setSize(1, 2);

		preventEntitySpawning = true;
		ignoreFrustumCheck = true;
		renderDistanceWeight = 200D;

		orientation = previousOrientation = new RotatedAxes();
		angularMomentum = new Vector3f();

		createData();
	}

	protected void setPivots(float verticalCenteringOffset, float verticalGroundingOffset)
	{
		this.verticalCenteringOffset = verticalCenteringOffset;
		this.verticalGroundingOffset = verticalGroundingOffset;
	}

	protected void createData()
	{
		data = new ShipData();
	}

	public boolean canBeCollidedWith()
	{
		return true;
	}

	public boolean attackEntityFrom(DamageSource source, float amount)
	{
		setDead();
		return true;
	}

	private EntitySeat[] createSeats()
	{
		return new EntitySeat[] {
				new EntitySeat(this, "Basic Seat", SeatRole.Driver, new Vector3f(0, 0, 2))
		};
	}

	@Override
	public void onUpdate()
	{
		if (posY < -64.0D)
			setDead();

		partWatchdog();

		prevPosX = lastTickPosX = posX;
		prevPosY = lastTickPosY = posY;
		prevPosZ = lastTickPosZ = posZ;

		rotationYaw = 180 - orientation.getYaw();
		rotationPitch = orientation.getPitch();
		prevRotationYaw = 180 - previousOrientation.getYaw();
		prevRotationPitch = previousOrientation.getPitch();

		previousOrientation = orientation.clone();

		if (worldObj.isRemote && EntityUtils.isClientControlled(this))
			KeyHandler.handleVehicleMovement();

		if (riddenByEntity instanceof EntityLivingBase)
		{
			Vector3f forward = orientation.findLocalVectorGlobally(new Vector3f(0, 0, -1));
			//Lumberjack.log(this.throttle);
			moveEntity(motionX + forward.x * throttle, motionY + forward.y * throttle, motionZ + forward.z * throttle);
		}
		else
			moveEntity(motionX, motionY, motionZ);
		orientation.rotateLocalPitch(angularMomentum.x);
		orientation.rotateLocalYaw(angularMomentum.y);
		orientation.rotateLocalRoll(angularMomentum.z);

		angularMomentum.x *= 0.7;
		angularMomentum.y *= 0.7;
		angularMomentum.z *= 0.7;

		if (Math.abs(angularMomentum.x) < 0.001f)
			angularMomentum.x = 0;
		if (Math.abs(angularMomentum.y) < 0.001f)
			angularMomentum.y = 0;
		if (Math.abs(angularMomentum.z) < 0.001f)
			angularMomentum.z = 0;

		if (!worldObj.isRemote)
		{
			if (riddenByEntity != null && riddenByEntity.isDead)
				riddenByEntity = null;
		}
	}

	public void acceptInput(ShipInput input)
	{
		switch (input)
		{
			case RollLeft:
				angularMomentum.z -= 4;
				break;
			case RollRight:
				angularMomentum.z += 4;
				break;
			case PitchUp:
				angularMomentum.x += 2;
				break;
			case PitchDown:
				angularMomentum.x -= 2;
				break;
			case YawLeft:
				break;
			case YawRight:
				break;
			case ThrottleUp:
				throttle += data.maxThrottle * data.acceleration;
				throttle = MathHelper.clamp_float(throttle, 0, data.maxThrottle);
				break;
			case ThrottleDown:
				throttle -= data.maxThrottle * data.acceleration;
				throttle = MathHelper.clamp_float(throttle, 0, data.maxThrottle);
				break;
			case BlasterFire:
				break;
			case SpecialAesthetic:
				break;
			case SpecialWeapon:
				break;
		}
		StarWarsGalaxy.network.sendToServer(new MessageFlightModelUpdate(this));
	}

	private void partWatchdog()
	{
		if (!worldObj.isRemote)
		{
			if (seats == null)
				seats = createSeats();

			for (int i = 0; i < seats.length; i++)
			{
				EntitySeat part = seats[i];
				if (part == null)
				{
					setSeat(searchingSeats[i], i);
					continue;
				}

				part.setLocation();

				if (worldObj.getEntityByID(part.getEntityId()) == null)
					worldObj.spawnEntityInWorld(part);
			}
		}
	}

	@Override
	public void setDead()
	{
		super.setDead();

		if (seats != null)
			for (EntitySeat part : seats)
				part.setDead();
	}

	@Override
	protected void entityInit()
	{

	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompound)
	{
		String seatsString = tagCompound.getString("seats");
		String[] seatsPairs = seatsString.split(";");

		seats = new EntitySeat[seatsPairs.length];
		searchingSeats = new UUID[seatsPairs.length];
		for (int i = 0; i < seatsPairs.length; i++)
		{
			String[] pair = seatsPairs[i].split("\\|");
			long lsb = Long.parseLong(pair[0]);
			long msb = Long.parseLong(pair[1]);
			UUID uuid = new UUID(msb, lsb);
			searchingSeats[i] = uuid;
		}
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound)
	{
		if (seats == null)
		{
			tagCompound.setString("seats", "");
			return;
		}

		StringBuilder sb = new StringBuilder();

		for (EntitySeat part : seats)
			sb.append(part.getUniqueID().getLeastSignificantBits()).append("|").append(part.getUniqueID().getMostSignificantBits()).append(";");

		tagCompound.setString("seats", sb.toString());
	}

	public void setSeat(UUID seatId, int seatIdx)
	{
		Entity entity = EntityUtils.getEntityByUuid(worldObj, seatId);

		if (entity == null)
		{
			Lumberjack.warn("Parent failed to locate part with UUID " + seatId);
			//setDead();
		}
		else
		{
			if (!(entity instanceof EntitySeat))
				return;

			seats[seatIdx] = (EntitySeat)entity;
			seats[seatIdx].setParent(getEntityId());
			Lumberjack.warn("Parent located part");
		}
	}

	public boolean isControlling(Entity thePlayer)
	{
		if (seats != null)
			for (EntitySeat seat : seats)
				if (seat.role == SeatRole.Driver && seat.riddenByEntity == thePlayer)
					return true;
		return false;
	}
}