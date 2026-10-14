package world.embers.production;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;

public final class TobaccoSeed extends BlockItem {
    private final Tobacco fixed;
    public TobaccoSeed(Tobacco fixed){super(Production.CROPS.get(fixed==null?Tobacco.RED:fixed).get(),new Item.Properties());this.fixed=fixed;}
    @Override public String getDescriptionId(){return "item.embers."+(fixed==null?"wild":fixed.id)+"_tobacco_seeds";}
    @Override protected BlockState getPlacementState(BlockPlaceContext context){
        // Client prediction cannot grant a crop: the server always checks the live certificate.
        boolean permit=context.getLevel().isClientSide || context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player
                && world.embers.compat.CultivationPermits.get(player.serverLevel()).allows(player);
        Tobacco t=fixed==null?Tobacco.at(context.getLevel(),context.getClickedPos()):(permit||context.getLevel().getBiome(context.getClickedPos()).is(fixed.biomes))?fixed:null;
        if(t==null||fixed!=null&&fixed!=t){if(context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.translatable("message.embers.seed_biome",Component.translatable("region.embers."+(fixed==null?"wild":fixed.id))),true);return null;}
        BlockState state=Production.CROPS.get(t).get().defaultBlockState();
        if(canPlace(context,state))return state;
        if(context.getPlayer()!=null)context.getPlayer().displayClientMessage(Component.translatable(t==Tobacco.YELLOW?"message.embers.seed_soil_sand":"message.embers.seed_soil"),true);
        return null;
    }
}
