package world.embers.production;

import world.embers.Embers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Defer the bonus until vanilla and protection mods have completed the manual break. */
@Mod.EventBusSubscriber(modid=Embers.ID)
public final class WildSeeds {
    public static final TagKey<Block> SOURCES=TagKey.create(Registries.BLOCK,new ResourceLocation("embers","wild_seed_sources"));
    private static final List<BlockEvent.BreakEvent> PENDING=new ArrayList<>();
    public static Tobacco weighted(int roll){return roll<4?Tobacco.RED:roll<7?Tobacco.YELLOW:roll<9?Tobacco.GREEN:Tobacco.PURPLE;}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void broken(BlockEvent.BreakEvent e){if(e.getPlayer() instanceof ServerPlayer p&&!p.isCreative()&&!p.isSpectator()&&e.getState().is(SOURCES)&&!(p.getMainHandItem().getItem() instanceof ShearsItem))PENDING.add(e);}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;
        for(var broken:PENDING){ServerPlayer p=(ServerPlayer)broken.getPlayer();if(!broken.isCanceled()&&p.serverLevel()==broken.getLevel()&&!broken.getLevel().getBlockState(broken.getPos()).is(broken.getState().getBlock())&&p.getRandom().nextFloat()<.1f)Block.popResource(p.serverLevel(),broken.getPos(),new ItemStack(Production.SEEDS.get(weighted(p.getRandom().nextInt(10))).get()));}PENDING.clear();
    }
}
