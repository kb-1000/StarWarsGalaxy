package com.parzivail.swg.dimension.endor;

import com.parzivail.swg.dimension.endor.terrain.TerrainSwissHills;
import com.parzivail.swg.registry.BlockRegister;
import com.parzivail.swg.registry.StructureRegister;
import com.parzivail.util.world.*;
import com.parzivail.util.world.TerrainLayer.Function;
import com.parzivail.util.world.TerrainLayer.Method;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.feature.WorldGenLakes;

import java.util.List;
import java.util.Random;

/**
 * Created by colby on 9/10/2017.
 */
public class ChunkProviderEndor implements IChunkProvider
{
	private final World worldObj;
	private final int waterLevel = 100;

	private final WorldGenLakes waterLakeGenerator;

	public ITerrainHeightmap terrain;

	public ChunkProviderEndor(World worldObj, long seed)
	{
		this.worldObj = worldObj;
		waterLakeGenerator = new WorldGenLakes(Blocks.water);
		terrain = new MultiCompositeTerrain(seed, 800,
		                                    // Naboo Mountains
		                                    new TerrainSwissHills(seed),
		                                    // Naboo Hills
		                                    //new CompositeTerrain(new TerrainLayer(seed, TerrainLayer.Function.MidWave, TerrainLayer.Method.Add, 100, 80), new TerrainLayer(seed + 1, TerrainLayer.Function.Midpoint, TerrainLayer.Method.Add, 50, 20)),
		                                    // Naboo Plains
		                                    new CompositeTerrain(new TerrainLayer(seed, Function.MidWave, Method.Add, 100, 10), new TerrainLayer(seed + 1, Function.Constant, Method.Add, 200, 40)));
	}

	/**
	 * loads or generates the chunk at the chunk location specified
	 */
	public Chunk loadChunk(int x, int z)
	{
		return provideChunk(x, z);
	}

	/**
	 * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
	 * specified chunk from the map seed and chunk seed
	 */
	public Chunk provideChunk(int cx, int cz)
	{
		Chunk chunk = new Chunk(worldObj, cx, cz);
		for (short x = 0; x < 16; x++)
		{
			for (short z = 0; z < 16; z++)
			{
				double height = terrain.getHeightAt((cx * 16 + x), (cz * 16 + z)) + 60;
				int finalHeight = (int)height;

				if (chunk.getBlockStorageArray()[0] == null)
					chunk.getBlockStorageArray()[0] = new ExtendedBlockStorage(0, !worldObj.provider.hasNoSky);
				chunk.getBlockStorageArray()[0].setExtBlockID(x, 0, z, Blocks.bedrock);

				for (short y = 1; y <= finalHeight; y++)
				{
					int l = y >> 4;
					ExtendedBlockStorage extendedblockstorage = chunk.getBlockStorageArray()[l];

					if (extendedblockstorage == null)
					{
						extendedblockstorage = new ExtendedBlockStorage(l << 4, !worldObj.provider.hasNoSky);
						chunk.getBlockStorageArray()[l] = extendedblockstorage;
					}

					if (y <= waterLevel + 1 && y == finalHeight)
						extendedblockstorage.setExtBlockID(x, y & 15, z, Blocks.sand);
					else if (y == finalHeight)
						extendedblockstorage.setExtBlockID(x, y & 15, z, BlockRegister.fastGrass);
					else if (y <= finalHeight)
					{
						double sandThreshold = (int)(height * 0.9);

						if (y < sandThreshold)
							extendedblockstorage.setExtBlockID(x, y & 15, z, Blocks.dirt);
						else
							extendedblockstorage.setExtBlockID(x, y & 15, z, Blocks.stone);
					}
					else if (y <= waterLevel)
						extendedblockstorage.setExtBlockID(x, y & 15, z, Blocks.water);
				}
			}
		}

		StructureRegister.structureEngine.genStructure(chunk);

		BiomeGenBase[] abiomegenbase = worldObj.getWorldChunkManager().loadBlockGeneratorData(null, cx * 16, cz * 16, 16, 16);
		byte[] abyte = chunk.getBiomeArray();

		for (int l = 0; l < abyte.length; ++l)
		{
			abyte[l] = (byte)abiomegenbase[l].biomeID;
		}

		chunk.generateSkylightMap();
		return chunk;
	}

	/**
	 * Checks to see if a chunk exists at x, y
	 */
	public boolean chunkExists(int p_73149_1_, int p_73149_2_)
	{
		return true;
	}

	/**
	 * Populates chunk with ores etc etc
	 */
	public void populate(IChunkProvider chunk, int chunkX, int chunkZ)
	{
		int k = chunkX * 16;
		int l = chunkZ * 16;

		Random random = new Random((k * 31) ^ l);

		if (random.nextInt(4) == 0)
		{
			int l1 = k + random.nextInt(16) + 8;
			int i2 = random.nextInt(256);
			int j2 = l + random.nextInt(16) + 8;
			waterLakeGenerator.generate(worldObj, random, l1, i2, j2);
		}

		BiomeGenBase b = worldObj.getBiomeGenForCoords(k + 16, l + 16);
		if (!(b instanceof PBiomeGenBase))
			return;
		PBiomeGenBase biomegenbase = (PBiomeGenBase)b;
		biomegenbase.decorate(this, worldObj, random, k, l);
	}

	/**
	 * Two modes of operation: if passed true, save all Chunks in one go.  If passed false, save up to two chunks.
	 * Return true if all chunks have been saved.
	 */
	public boolean saveChunks(boolean p_73151_1_, IProgressUpdate p_73151_2_)
	{
		return true;
	}

	/**
	 * Save extra data not associated with any Chunk.  Not saved during autosave, only during world unload.  Currently
	 * unimplemented.
	 */
	public void saveExtraData()
	{
	}

	/**
	 * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
	 */
	public boolean unloadQueuedChunks()
	{
		return false;
	}

	/**
	 * Returns if the IChunkProvider supports saving.
	 */
	public boolean canSave()
	{
		return true;
	}

	/**
	 * Converts the instance data to a readable string.
	 */
	public String makeString()
	{
		return "EndorLevelSource";
	}

	/**
	 * Returns a list of creatures of the specified type that can spawn at the given location.
	 */
	public List getPossibleCreatures(EnumCreatureType p_73155_1_, int p_73155_2_, int p_73155_3_, int p_73155_4_)
	{
		BiomeGenBase biomegenbase = worldObj.getBiomeGenForCoords(p_73155_2_, p_73155_4_);
		return biomegenbase.getSpawnableList(p_73155_1_);
	}

	public ChunkPosition findClosestStructure(World p_147416_1_, String p_147416_2_, int p_147416_3_, int p_147416_4_, int p_147416_5_)
	{
		return null;
	}

	public int getLoadedChunkCount()
	{
		return 0;
	}

	public void recreateStructures(int p_82695_1_, int p_82695_2_)
	{
	}
}
