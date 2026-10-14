package world.embers.production;

import world.embers.Embers;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.*;

public final class Production {
    public static final Map<Tobacco,RegistryObject<Block>> CROPS=new EnumMap<>(Tobacco.class);
    public static final Map<Tobacco,RegistryObject<Item>> SEEDS=new EnumMap<>(Tobacco.class),FRESH=new EnumMap<>(Tobacco.class),DRY=new EnumMap<>(Tobacco.class);
    public static final RegistryObject<Block> RACK=Embers.BLOCKS.register("drying_rack",DryingRackBlock::new);
    public static final RegistryObject<Block> MACHINE=Embers.BLOCKS.register("processing_machine",ProcessingBlock::new);
    public static final RegistryObject<Item> RACK_ITEM=Embers.ITEMS.register("drying_rack",()->new BlockItem(RACK.get(),new Item.Properties()));
    public static final RegistryObject<Item> MACHINE_ITEM=Embers.ITEMS.register("processing_machine",()->new BlockItem(MACHINE.get(),new Item.Properties()));
    public static final RegistryObject<Item> WILD=Embers.ITEMS.register("wild_tobacco_seeds",()->new TobaccoSeed(null));
    public static final RegistryObject<BlockEntityType<DryingRackEntity>> RACK_ENTITY=Embers.BLOCK_ENTITIES.register("drying_rack",()->BlockEntityType.Builder.of(DryingRackEntity::new,RACK.get()).build(null));
    public static final RegistryObject<BlockEntityType<ProcessingEntity>> MACHINE_ENTITY=Embers.BLOCK_ENTITIES.register("processing_machine",()->BlockEntityType.Builder.of(ProcessingEntity::new,MACHINE.get()).build(null));
    public static final RegistryObject<MenuType<ProcessingMenu>> MENU=Embers.MENUS.register("processing",()->IForgeMenuType.create(ProcessingMenu::new));
    public static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(ForgeRegistries.RECIPE_TYPES,Embers.ID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,Embers.ID);
    public static final RegistryObject<RecipeType<ProcessingRecipe>> TYPE=TYPES.register("processing",()->new RecipeType<>(){});
    public static final RegistryObject<RecipeSerializer<ProcessingRecipe>> SERIALIZER=SERIALIZERS.register("processing",ProcessingRecipe.Serializer::new);
    static {for(Tobacco t:Tobacco.values()){
        CROPS.put(t,Embers.BLOCKS.register(t.id+"_tobacco_crop",()->new TobaccoCrop(t)));
        SEEDS.put(t,Embers.ITEMS.register(t.id+"_tobacco_seeds",()->new TobaccoSeed(t)));
        FRESH.put(t,Embers.ITEMS.register(t.id+"_tobacco",()->new Item(new Item.Properties())));
        DRY.put(t,Embers.ITEMS.register("dried_"+t.id+"_tobacco",()->new Item(new Item.Properties())));
    }}
    public static void register(IEventBus bus){TYPES.register(bus);SERIALIZERS.register(bus);}
    public static Tobacco fresh(ItemStack stack){for(Tobacco t:Tobacco.values())if(stack.is(FRESH.get(t).get()))return t;return null;}
    public static boolean dried(ItemStack stack){return DRY.values().stream().anyMatch(x->stack.is(x.get()));}
    public static void display(CreativeModeTab.Output out){out.accept(WILD.get());for(Tobacco t:Tobacco.values()){out.accept(SEEDS.get(t).get());out.accept(FRESH.get(t).get());out.accept(DRY.get(t).get());}out.accept(RACK_ITEM.get());out.accept(MACHINE_ITEM.get());}
}
