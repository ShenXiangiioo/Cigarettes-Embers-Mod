package world.embers.production;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class TobaccoCrop extends CropBlock {
    public final Tobacco tobacco;
    public TobaccoCrop(Tobacco t){super(BlockBehaviour.Properties.copy(Blocks.WHEAT));tobacco=t;}
    @Override protected ItemLike getBaseSeedId(){return Production.SEEDS.get(tobacco).get();}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos){return super.mayPlaceOn(state,level,pos)||tobacco==Tobacco.YELLOW&&(state.is(Blocks.SAND)||state.is(Blocks.RED_SAND));}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return Block.box(2,0,2,14,3+getAge(state)*1.8,14);}
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(tobacco==Tobacco.YELLOW&&(level.getBlockState(pos.below()).is(Blocks.SAND)||level.getBlockState(pos.below()).is(Blocks.RED_SAND))){
            if(level.getRawBrightness(pos,0)>=9&&!isMaxAge(state)&&net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level,pos,state,random.nextInt(7)==0)){
                level.setBlock(pos,getStateForAge(getAge(state)+1),2);net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level,pos,state);
            }
        }else super.randomTick(state,level,pos,random);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.getItemInHand(hand).isEmpty()||!isMaxAge(state))return InteractionResult.PASS;
        if(!level.isClientSide){popResource(level,pos,new ItemStack(Production.FRESH.get(tobacco).get(),3));popResource(level,pos,new ItemStack(Production.SEEDS.get(tobacco).get()));level.setBlock(pos,getStateForAge(0),3);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
