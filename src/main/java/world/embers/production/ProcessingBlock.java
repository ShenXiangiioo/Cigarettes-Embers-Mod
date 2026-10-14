package world.embers.production;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

public final class ProcessingBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,5);
    private static final Set<String> BREAKING=new HashSet<>();
    public ProcessingBlock(){super(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
    public static BlockPos part(BlockPos root,Direction facing,int i){return root.relative(facing.getClockWise(),i%3).above(i/3);}
    public static BlockPos root(BlockPos p,BlockState s){int i=s.getValue(PART);return p.relative(s.getValue(FACING).getCounterClockWise(),i%3).below(i/3);}
    public static ProcessingEntity entity(Level l,BlockPos p){BlockState s=l.getBlockState(p);return s.is(Production.MACHINE.get())&&l.getBlockEntity(root(p,s)) instanceof ProcessingEntity e?e:null;}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(PART)==0?new ProcessingEntity(p,s):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return !l.isClientSide&&s.getValue(PART)==0?createTickerHelper(type,Production.MACHINE_ENTITY.get(),ProcessingEntity::tick):null;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){Level l=c.getLevel();BlockPos p=c.getClickedPos();Direction f=c.getHorizontalDirection().getOpposite();for(int i=0;i<6;i++){BlockPos q=part(p,f,i);if(q.getY()>=l.getMaxBuildHeight()||!l.hasChunkAt(q)||!l.getBlockState(q).canBeReplaced(c)||i<3&&!l.getBlockState(q.below()).isFaceSturdy(l,q.below(),Direction.UP))return null;}return defaultBlockState().setValue(FACING,f);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){for(int i=1;i<6;i++)l.setBlock(part(p,s.getValue(FACING),i),s.setValue(PART,i),3);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand h,BlockHitResult hit){ProcessingEntity be=entity(l,p);if(!l.isClientSide&&player instanceof ServerPlayer sp&&be!=null)NetworkHooks.openScreen(sp,be,be.getBlockPos());return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){ProcessingEntity be=entity(l,p);if(be!=null)be.dropMachine=!player.isCreative()&&player.hasCorrectToolForDrops(s);super.playerWillDestroy(l,p,s,player);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){
        if(!s.is(n.getBlock())&&!l.isClientSide){BlockPos root=root(p,s);String key=l.dimension().location()+":"+root.asLong();if(BREAKING.add(key)){try{
            if(l.getBlockEntity(root) instanceof ProcessingEntity be){be.dropContents();if(be.dropMachine)popResource(l,root,new ItemStack(Production.MACHINE_ITEM.get()));}
            for(int i=0;i<6;i++){BlockPos q=part(root,s.getValue(FACING),i);if(!q.equals(p)&&l.getBlockState(q).is(this))l.removeBlock(q,false);}
        }finally{BREAKING.remove(key);}}}
        super.onRemove(s,l,p,n,moving);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState s){return net.minecraft.world.level.material.PushReaction.BLOCK;}
}
