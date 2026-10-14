package world.embers.production;

import net.minecraft.core.*;
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
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class DryingRackBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty UPPER=BooleanProperty.create("upper"),LEFT=BooleanProperty.create("left"),RIGHT=BooleanProperty.create("right"),WATER=BlockStateProperties.WATERLOGGED;
    public DryingRackBlock(){super(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(UPPER,false).setValue(LEFT,false).setValue(RIGHT,false).setValue(WATER,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,UPPER,LEFT,RIGHT,WATER);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(UPPER)?null:new DryingRackEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&!s.getValue(UPPER)?createTickerHelper(t,Production.RACK_ENTITY.get(),DryingRackEntity::tick):null;}
    static boolean same(BlockState s,Direction d,boolean upper){return s.is(Production.RACK.get())&&s.getValue(FACING)==d&&s.getValue(UPPER)==upper;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        BlockPos p=c.getClickedPos();Level l=c.getLevel();Direction f=c.getHorizontalDirection().getOpposite();
        if(p.getY()>=l.getMaxBuildHeight()-1||!l.getBlockState(p.above()).canBeReplaced(c)||!l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP))return null;
        int count=1;for(Direction d:new Direction[]{f.getClockWise(),f.getCounterClockWise()})for(int i=1;i<=3&&same(l.getBlockState(p.relative(d,i)),f,false);i++)count++;
        if(count>3){if(c.getPlayer()!=null)c.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.embers.rack_limit"),true);return null;}
        return connected(l,p,defaultBlockState().setValue(FACING,f).setValue(WATER,l.getFluidState(p).getType()==Fluids.WATER));
    }
    private BlockState connected(LevelAccessor l,BlockPos p,BlockState s){Direction f=s.getValue(FACING);return s.setValue(LEFT,same(l.getBlockState(p.relative(f.getCounterClockWise())),f,s.getValue(UPPER))).setValue(RIGHT,same(l.getBlockState(p.relative(f.getClockWise())),f,s.getValue(UPPER)));}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,LivingEntity e,ItemStack stack){l.setBlock(p.above(),s.setValue(UPPER,true).setValue(WATER,l.getFluidState(p.above()).getType()==Fluids.WATER),3);}
    @Override public FluidState getFluidState(BlockState s){return s.getValue(WATER)?Fluids.WATER.getSource(false):super.getFluidState(s);}
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){
        if(s.getValue(WATER))l.scheduleTick(p,Fluids.WATER,Fluids.WATER.getTickDelay(l));
        boolean upper=s.getValue(UPPER);if(d==(upper?Direction.DOWN:Direction.UP)&&!same(n,s.getValue(FACING),!upper))return Blocks.AIR.defaultBlockState();
        if(!upper&&d==Direction.DOWN&&!n.isFaceSturdy(l,q,Direction.UP))return Blocks.AIR.defaultBlockState();
        return connected(l,p,s);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moving){if(!s.is(n.getBlock())&&!s.getValue(UPPER)&&l.getBlockEntity(p) instanceof DryingRackEntity rack){rack.dropContents();}super.onRemove(s,l,p,n,moving);}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext ctx){return s.getValue(FACING).getAxis()==Direction.Axis.Z?Block.box(0,0,5,16,s.getValue(UPPER)?10:16,11):Block.box(5,0,0,11,s.getValue(UPPER)?10:16,16);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        BlockPos root=s.getValue(UPPER)?p.below():p;if(!(l.getBlockEntity(root) instanceof DryingRackEntity rack))return InteractionResult.PASS;
        if(!l.isClientSide)rack.interact(player,hand,slot(s,hit));return InteractionResult.sidedSuccess(l.isClientSide);
    }
    public static int slot(BlockState s,BlockHitResult h){Direction f=s.getValue(FACING);Vec3 v=h.getLocation();double c=f.getAxis()==Direction.Axis.Z?v.x-Math.floor(v.x):v.z-Math.floor(v.z);if(f==Direction.SOUTH||f==Direction.WEST)c=1-c;return Math.max(0,Math.min(2,(int)(c*3)));}
}
