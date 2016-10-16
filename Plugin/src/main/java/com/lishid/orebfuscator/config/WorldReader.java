/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.utils.Globals;

public class WorldReader {
	private static enum WorldType { Default, Normal, TheEnd, Nether }
	
    private boolean[] transparentBlocks;
    
    private WorldConfig defaultWorld;
    private WorldConfig normalWorld;
    private WorldConfig endWorld;
    private WorldConfig netherWorld;
    private Map<String, WorldConfig> worlds;
    
    private JavaPlugin plugin;
    private Logger logger;
    private OrebfuscatorConfig orebfuscatorConfig;
    private MaterialReader materialReader;
    
    public WorldReader(
    		JavaPlugin plugin,
    		Logger logger,
    		OrebfuscatorConfig orebfuscatorConfig,
    		MaterialReader materialReader
    		)
    {
    	this.plugin = plugin;
		this.logger = logger;
		this.orebfuscatorConfig = orebfuscatorConfig;
		this.materialReader = materialReader;
    }

	public void load() {
        this.transparentBlocks = this.orebfuscatorConfig.getTransparentBlocks();
        
    	ConfigurationSection section = getConfig().getConfigurationSection("Worlds");
    	Set<String> keys = section != null ? section.getKeys(false): new HashSet<String>();
    	
    	this.defaultWorld = readWorldByType(keys, WorldType.Default, null);
    	this.normalWorld = readWorldByType(keys, WorldType.Normal, this.defaultWorld);
    	this.endWorld = readWorldByType(keys, WorldType.TheEnd, this.defaultWorld);
    	this.netherWorld = readWorldByType(keys, WorldType.Nether, this.defaultWorld);
    	
    	this.worlds = new WeakHashMap<String, WorldConfig>();
    	
    	for(String key : keys) {
    		readWorldsByName("Worlds." + key);
    	}
        
        this.orebfuscatorConfig.setDefaultWorld(this.defaultWorld);
        this.orebfuscatorConfig.setNormalWorld(this.normalWorld);
        this.orebfuscatorConfig.setEndWorld(this.endWorld);
        this.orebfuscatorConfig.setNetherWorld(this.netherWorld);
        this.orebfuscatorConfig.setWorlds(this.worlds);
	}
	
    private FileConfiguration getConfig() {
    	return this.plugin.getConfig();
    }
    
    public static String createPath(String path, String key, FileConfiguration config) {
    	int counter = 1;
    	String newPath = path + "." + key;
    	
    	while(config.get(newPath) != null) {
    		newPath = path + "." + key + (counter++);
    	}
    	
    	return newPath;
    }
    
    private WorldConfig readWorldByType(Set<String> keys, WorldType worldType, WorldConfig baseWorld) {
    	WorldConfig world = null;
    	
    	for(String key : keys) {
    		String worldPath = "Worlds." + key;
    		List<String> types = getStringList(worldPath + ".Types", null, false);
    		
    		if(types != null && types.size() > 0 && parseWorldTypes(types).contains(worldType)) {
    			if(worldType == WorldType.Default) {
    				world = new WorldConfig();
    				world.setDefaults();
    			}
    			
    			world = readWorld(worldPath, world, worldType == WorldType.Default);
    			break;
    		}
    	}
    	
    	if(world == null) {
	    	switch(worldType) {
	    	case Default:
	    		world = createDefaultWorld(createPath("Worlds", "Default", getConfig()));
	    		break;
	    	case Normal:
	    		world = createNormalWorld(createPath("Worlds", "Normal", getConfig()));
	    		break;
	    	case TheEnd:
	    		world = createEndWorld(createPath("Worlds", "TheEnd", getConfig()));
	    		break;
	    	case Nether:
	    		world = createNetherWorld(createPath("Worlds", "Nether", getConfig()));
	    		break;
	    	}
    	}
    	
    	world.init(baseWorld);
    	
    	return world;
    }
    
    private void readWorldsByName(String worldPath) {
    	List<String> names = getStringList(worldPath + ".Names", null, false);
    	
    	if(names == null || names.size() == 0) {
    		return;
    	}
    	
		for(String name : names) {
			String key = name.toLowerCase();
			
			if(!this.worlds.containsKey(key)) {
				this.worlds.put(key, readWorld(worldPath, null, false));
			}
		}
    }
    
    private List<WorldType> parseWorldTypes(List<String> types) {
    	List<WorldType> parsedTypes = new ArrayList<WorldType>();
    	
    	for(String type : types) {
    		WorldType worldType;
    		
    		if(type.equalsIgnoreCase("DEFAULT")) {
    			worldType = WorldType.Default;
    		} else if(type.equalsIgnoreCase("NORMAL")) {
    			worldType = WorldType.Normal;
    		} else if(type.equalsIgnoreCase("THE_END")) {
    			worldType = WorldType.TheEnd;
    		} else if(type.equalsIgnoreCase("NETHER")) {
    			worldType = WorldType.Nether;
    		} else {
    			this.logger.log(Level.WARNING, Globals.LogPrefix + "World type '" + type + "' is not supported.");
    			continue;
    		}
    		
    		parsedTypes.add(worldType);
    	}
    	
    	return parsedTypes;
    }
	
	private WorldConfig readWorld(String worldPath, WorldConfig cfg, boolean withSave) {
		if(cfg == null) {
			cfg = new WorldConfig();
		}
		
		Boolean enabled = getBoolean(worldPath + ".Enabled", cfg.isEnabled(), withSave);
	    Boolean antiTexturePackAndFreecam = getBoolean(worldPath + ".AntiTexturePackAndFreecam", cfg.isAntiTexturePackAndFreecam(), withSave);
	    Integer airGeneratorMaxChance = getInt(worldPath + ".AirGeneratorMaxChance", cfg.getAirGeneratorMaxChance(), 40, 100, withSave);
	    Boolean darknessHideBlocks = getBoolean(worldPath + ".DarknessHideBlocks", cfg.isDarknessHideBlocks(), withSave);
	    boolean[] darknessBlocks = readBlockMatrix(cfg.getDarknessBlocks(), cfg.getDarknessBlockIds(), worldPath + ".DarknessBlocks", withSave);	    
	    Integer mode1Block = this.materialReader.getMaterialIdByPath(worldPath + ".Mode1Block", cfg.getMode1BlockId(), withSave);
		Integer[] randomBlocks = this.materialReader.getMaterialIdsByPath(worldPath + ".RandomBlocks", cfg.getRandomBlocks(), withSave);
		boolean[] obfuscateBlocks = readBlockMatrix(cfg.getObfuscateBlocks(), cfg.getObfuscateBlockIds(), worldPath + ".ObfuscateBlocks", withSave);

		readProximityHider(worldPath, cfg, withSave);

	    cfg.setEnabled(enabled);
	    cfg.setAntiTexturePackAndFreecam(antiTexturePackAndFreecam);
	    cfg.setAirGeneratorMaxChance(airGeneratorMaxChance);
	    cfg.setDarknessHideBlocks(darknessHideBlocks);
	    cfg.setDarknessBlocks(darknessBlocks);
	    cfg.setMode1BlockId(mode1Block);
		cfg.setRandomBlocks(randomBlocks);
		cfg.setObfuscateBlocks(obfuscateBlocks);
		
	    return cfg;
	}
	
	private void readProximityHider(String worldPath, WorldConfig worldConfig, boolean withSave) {
    	ProximityHiderConfig cfg = worldConfig.getProximityHiderConfig();
    	
	    String sectionPath = worldPath + ".ProximityHider";
	    Boolean enabled = getBoolean(sectionPath + ".Enabled", cfg.isEnabled(), withSave);
	    Integer distance = getInt(sectionPath + ".Distance", cfg.getDistance(), 2, 64, withSave);
	    Integer specialBlockID = this.materialReader.getMaterialIdByPath(sectionPath + ".SpecialBlock", cfg.getSpecialBlockID(), withSave);
	    Integer endY = getInt(sectionPath + ".EndY", cfg.getEndY(), 0, 255, withSave);
	    Boolean useSpecialBlock = getBoolean(sectionPath + ".UseSpecialBlock", cfg.isUseSpecialBlock(), withSave);
	    Boolean useYLocationProximity = getBoolean(sectionPath + ".UseEndY", cfg.isUseYLocationProximity(), withSave);	    
	    boolean[] proximityHiderBlocks = readBlockMatrix(cfg.getProximityHiderBlocks(), cfg.getProximityHiderBlockIds(), sectionPath + ".ProximityHiderBlocks", withSave);
	    
	    cfg.setEnabled(enabled);
	    cfg.setDistance(distance);
	    cfg.setSpecialBlockID(specialBlockID);
	    cfg.setEndY(endY);
	    cfg.setUseSpecialBlock(useSpecialBlock);
	    cfg.setUseYLocationProximity(useYLocationProximity); 
	    cfg.setProximityHiderBlocks(proximityHiderBlocks);
	}
	
	private boolean[] readBlockMatrix(boolean[] original, Integer[] defaultBlockIds, String configKey, boolean withSave) {
	    boolean[] blocks = original;
	    Integer[] blockIds = this.materialReader.getMaterialIdsByPath(configKey, defaultBlockIds, withSave);
	    
	    if(blockIds != null) {
	    	if(blocks == null) {
	    		blocks = new boolean[256];
	    	}
	    	
	    	setBlockValues(blocks, blockIds);
	    }
	    
	    return blocks;
	}
	
	private WorldConfig createDefaultWorld(String worldPath) {
        getConfig().set(worldPath + ".Types", new String[] { "DEFAULT" });
        
        WorldConfig world = new WorldConfig();
        world.setDefaults();
        
	    return readWorld(worldPath, world, true);
	}
	
	private WorldConfig createNormalWorld(String worldPath) {
		Integer[] randomBlocks = new Integer[]{ 1, 4, 5, 14, 15, 16, 21, 46, 48, 49, 56, 73, 82, 129 };
		Integer[] obfuscateBlockIds = new Integer[] { 14, 15, 16, 21, 54, 56, 73, 74, 129, 130 };

		getConfig().set(worldPath + ".Types", new String[] { "NORMAL" });
		
		this.materialReader.getMaterialIdByPath(worldPath + ".Mode1Block", 1, true);
		this.materialReader.getMaterialIdsByPath(worldPath + ".RandomBlocks", randomBlocks, true);
		this.materialReader.getMaterialIdsByPath(worldPath + ".ObfuscateBlocks", obfuscateBlockIds, true);
		
		boolean[] obfuscateBlocks = new boolean[256];
		
		setBlockValues(obfuscateBlocks, obfuscateBlockIds, false);
		
		obfuscateBlocks[1] = true;
		
		WorldConfig cfg = new WorldConfig();
		cfg.setObfuscateBlocks(obfuscateBlocks);
		cfg.setRandomBlocks(randomBlocks);
		cfg.setMode1BlockId(1);
		
		return cfg;
	}
	
	private WorldConfig createEndWorld(String worldPath) {
		Integer[] randomBlocks = new Integer[]{ 7, 49, 121, 201, 206 };
		Integer[] obfuscateBlockIds = new Integer[] { 121 };

		getConfig().set(worldPath + ".Types", new String[] { "THE_END" });
		
		this.materialReader.getMaterialIdByPath(worldPath + ".Mode1Block", 121, true);
		this.materialReader.getMaterialIdsByPath(worldPath + ".RandomBlocks", randomBlocks, true);
		this.materialReader.getMaterialIdsByPath(worldPath + ".ObfuscateBlocks", obfuscateBlockIds, true);
		
		boolean[] obfuscateBlocks = new boolean[256];
		
		setBlockValues(obfuscateBlocks, obfuscateBlockIds, false);
		
		obfuscateBlocks[121] = true;
		
		WorldConfig cfg = new WorldConfig();
		cfg.setRandomBlocks(randomBlocks);
		cfg.setObfuscateBlocks(obfuscateBlocks);
		cfg.setMode1BlockId(121);
		
		return cfg;
	}

	private WorldConfig createNetherWorld(String worldPath) {
		Integer[] randomBlocks = new Integer[]{ 13, 87, 88, 112, 153 };
		Integer[] obfuscateBlockIds = new Integer[]{ 87, 153 };
		
		getConfig().set(worldPath + ".Types", new String[] { "NETHER" });
		
		this.materialReader.getMaterialIdByPath(worldPath + ".Mode1Block", 87, true);
		this.materialReader.getMaterialIdsByPath(worldPath + ".RandomBlocks", randomBlocks, true);
		this.materialReader.getMaterialIdsByPath(worldPath + ".ObfuscateBlocks", obfuscateBlockIds, true);
		
		boolean[] obfuscateBlocks = new boolean[256];
		
		setBlockValues(obfuscateBlocks, obfuscateBlockIds, false);
		
		obfuscateBlocks[87] = true;
		
		WorldConfig cfg = new WorldConfig();
		cfg.setRandomBlocks(randomBlocks);
		cfg.setObfuscateBlocks(obfuscateBlocks);
		cfg.setMode1BlockId(87);
		
		return cfg;
	}

    private static void setBlockValues(boolean[] boolArray, Integer[] blocks) {
    	List<Integer> blockList = Arrays.asList(blocks);
    	
        for (int i = 0; i < boolArray.length; i++) {
            boolArray[i] = blockList.contains(i);
        }
    }
    
    private void setBlockValues(boolean[] boolArray, Integer[] blocks, boolean transparent) {
    	List<Integer> blockList = Arrays.asList(blocks);
    	
        for (int i = 0; i < boolArray.length; i++) {
            boolArray[i] = blockList.contains(i);

            // If block is transparent while we don't want them to, or the other way around
            if (transparent != isBlockTransparent((short) i)) {
                // Remove it
                boolArray[i] = false;
            }
        }
    }
        
    private Integer getInt(String path, Integer defaultData, int min, int max, boolean withSave) {
        if (getConfig().get(path) == null && withSave) {
      		getConfig().set(path, defaultData);
        }
        
        Integer value = getConfig().get(path) != null ? (Integer)getConfig().getInt(path): defaultData;
        
        if(value != null) {
	        if(value < min) { 
	        	value = min;
	        }
	        else if(value > max) {
	        	value = max;
	        }
        }
        
        return value;
    }
    
    private Boolean getBoolean(String path, Boolean defaultData, boolean withSave) {
        if (getConfig().get(path) == null) {
        	if(!withSave) {
        		return defaultData;
        	}
        	
        	getConfig().set(path, defaultData);
        }
        
        return getConfig().get(path) != null ? (Boolean)getConfig().getBoolean(path): defaultData;
    }

    private List<String> getStringList(String path, List<String> defaultData, boolean withSave) {
        if (getConfig().get(path) == null) {
        	if(!withSave) {
        		return defaultData;
        	}

        	getConfig().set(path, defaultData);
        }
        
        return getConfig().getStringList(path);
    }
    
    private boolean isBlockTransparent(int id) {
        if (id < 0)
            id += 256;

        if (id >= 256) {
            return false;
        }

        return this.transparentBlocks[id];
    }
}
