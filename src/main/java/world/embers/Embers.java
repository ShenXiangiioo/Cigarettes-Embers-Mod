package world.embers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.effect.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.*;

@Mod(Embers.ID)
public final class Embers {
    public static final String ID="embers";
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,ID);
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,ID);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,ID);
    public static final DeferredRegister<SoundEvent> SOUNDS=DeferredRegister.create(ForgeRegistries.SOUND_EVENTS,ID);
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,ID);
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES=DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES,ID);
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> SMOKE=PARTICLES.register("smoke",()->new net.minecraft.core.particles.SimpleParticleType(false));
    public static final RegistryObject<net.minecraft.core.particles.SimpleParticleType> EXHALE=PARTICLES.register("exhale",()->new net.minecraft.core.particles.SimpleParticleType(false));
    public static final Map<Brand,RegistryObject<Item>> PACKS=new EnumMap<>(Brand.class),CIGARETTES=new EnumMap<>(Brand.class);
    public static final Map<LighterKind,RegistryObject<Item>> LIGHTERS=new EnumMap<>(LighterKind.class);
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,ID);
    public static final RegistryObject<net.minecraft.world.entity.EntityType<ButtEntity>> BUTT_ENTITY=ENTITIES.register("cigarette_butt",()->net.minecraft.world.entity.EntityType.Builder.<ButtEntity>of(ButtEntity::new,net.minecraft.world.entity.MobCategory.MISC).sized(.14f,.035f).clientTrackingRange(6).updateInterval(2).build("embers:cigarette_butt"));
    public static final RegistryObject<Item> BUTT=ITEMS.register("cigarette_butt",ButtItem::new);
    public static final RegistryObject<Block> STATION=BLOCKS.register("refill_station",StationBlock::new);
    public static final RegistryObject<Block> ASHTRAY=BLOCKS.register("ashtray",AshtrayBlock::new);
    public static final RegistryObject<Block> GLASS_ASHTRAY=BLOCKS.register("glass_ashtray",()->new AshtrayBlock(1)),BRASS_ASHTRAY=BLOCKS.register("brass_ashtray",()->new AshtrayBlock(2));
    public static final RegistryObject<BlockEntityType<StationEntity>> STATION_ENTITY=BLOCK_ENTITIES.register("refill_station",()->BlockEntityType.Builder.of(StationEntity::new,STATION.get()).build(null));
    public static final RegistryObject<BlockEntityType<AshtrayEntity>> ASHTRAY_ENTITY=BLOCK_ENTITIES.register("ashtray",()->BlockEntityType.Builder.of(AshtrayEntity::new,ASHTRAY.get(),GLASS_ASHTRAY.get(),BRASS_ASHTRAY.get()).build(null));
    public static final RegistryObject<MenuType<StationMenu>> STATION_MENU=MENUS.register("refill_station",()->IForgeMenuType.create(StationMenu::new));
    public static final RegistryObject<MobEffect> COLLAPSE=EFFECTS.register("collapse",()->new EmbersEffect(MobEffectCategory.HARMFUL,0xAD4F40));
    public static final RegistryObject<MobEffect> UNDEAD_FOCUS=EFFECTS.register("undead_focus",()->new EmbersEffect(MobEffectCategory.BENEFICIAL,0xC8B479));
    public static final Map<String,RegistryObject<SoundEvent>> AUDIO=new HashMap<>();
    public static final ForgeConfigSpec.IntValue BURN_SECONDS;
    public static final ForgeConfigSpec.BooleanValue STRESS_ENABLED;
    public static final ForgeConfigSpec CONFIG;
    static {
        for(Brand b:Brand.values()){
            PACKS.put(b,ITEMS.register(b.id+"_pack",()->new SmokingItem(b,true)));
            CIGARETTES.put(b,ITEMS.register(b.id+"_cigarette",()->new SmokingItem(b,false)));
        }
        for(LighterKind k:LighterKind.values())LIGHTERS.put(k,ITEMS.register(k.id+"_lighter",()->new LighterItem(k)));
        ITEMS.register("refill_station",()->new BlockItem(STATION.get(),new Item.Properties()));
        ITEMS.register("ashtray",()->new BlockItem(ASHTRAY.get(),new Item.Properties()));
        ITEMS.register("glass_ashtray",()->new AshtrayItem(GLASS_ASHTRAY.get()));ITEMS.register("brass_ashtray",()->new AshtrayItem(BRASS_ASHTRAY.get()));
        // Keep the retired 0.0.8 sound ID registered so upgrading old worlds needs no missing-ID migration.
        for(String s:new String[]{"cigarette_burn","exhale_medium","exhale_long","pack_return","pack_open","pack_take","pack_wrap","pack_close","inhale_short","inhale_deep","exhale","cough","sigh","extinguish","water_out","burn_hurt","lighter_pop","heartbeat","breath","refill","lighter_open","lighter_close","flint","piezo","jet","oil","drop"})
            AUDIO.put(s,SOUNDS.register(s,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ID,s))));
        for(LighterKind k:LighterKind.values())for(String phase:new String[]{"open","close","strike","burn","burn_loop"}){String s=k.id+"_"+phase;AUDIO.put(s,SOUNDS.register(s,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ID,s))));}
        for(int i=0;i<5;i++){String s="sigh_"+i;AUDIO.put(s,SOUNDS.register(s,()->SoundEvent.createVariableRangeEvent(new ResourceLocation(ID,s))));}
        TABS.register("embers",()->CreativeModeTab.builder().title(net.minecraft.network.chat.Component.translatable("itemGroup.embers")).icon(()->new ItemStack(PACKS.get(Brand.CABALLERO_BLACK).get())).displayItems((p,out)->{
            for(Brand b:Brand.values()){out.accept(PACKS.get(b).get());out.accept(CIGARETTES.get(b).get());}
            for(LighterKind k:LighterKind.values())out.accept(LIGHTERS.get(k).get());out.accept(STATION.get());out.accept(ASHTRAY.get());out.accept(GLASS_ASHTRAY.get());out.accept(BRASS_ASHTRAY.get());out.accept(BUTT.get());world.embers.production.Production.display(out);
        }).build());
        ForgeConfigSpec.Builder c=new ForgeConfigSpec.Builder();
        BURN_SECONDS=c.comment("Natural burn duration for slim and horizontal-case cigarettes. Classic cigarettes last 20% longer. Inhaling accelerates combustion.").defineInRange("burnSeconds",120,30,1200);
        STRESS_ENABLED=c.comment("Enable survival stress rules.").define("stressEnabled",true);CONFIG=c.build();
    }
    public Embers(){
        IEventBus bus=FMLJavaModLoadingContext.get().getModEventBus();
        world.embers.production.Production.register(bus);SmokingBenefits.register();
        ITEMS.register(bus);BLOCKS.register(bus);BLOCK_ENTITIES.register(bus);MENUS.register(bus);SOUNDS.register(bus);EFFECTS.register(bus);TABS.register(bus);PARTICLES.register(bus);ENTITIES.register(bus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,CONFIG);Network.init();MinecraftForge.EVENT_BUS.register(new ServerEvents());
    }
    public static SoundEvent sound(String id){return AUDIO.get(id).get();}
    static class EmbersEffect extends MobEffect {EmbersEffect(MobEffectCategory c,int color){super(c,color);}}
}
