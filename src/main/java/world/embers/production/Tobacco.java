package world.embers.production;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public enum Tobacco {
    RED("red",0x914C4C),YELLOW("yellow",0xB99945),GREEN("green",0x577F4B),PURPLE("purple",0x785479);
    public final String id; public final int color;
    public final TagKey<Biome> biomes;
    Tobacco(String id,int color){this.id=id;this.color=color;biomes=TagKey.create(Registries.BIOME,new ResourceLocation("embers", "tobacco_"+id));}
    public static Tobacco at(Level level,BlockPos pos){for(Tobacco t:values())if(level.getBiome(pos).is(t.biomes))return t;return null;}
}
