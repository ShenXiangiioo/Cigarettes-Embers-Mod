package world.embers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.network.NetworkHooks;
public class StationBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public StationBlock(){super(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.METAL).noOcclusion());registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(1,0,1,15,14,15);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new StationEntity(p,s);}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(!l.isClientSide&&p instanceof ServerPlayer sp&&l.getBlockEntity(pos) instanceof StationEntity be){if(world.embers.compat.CommerceBridge.tablet(p.getItemInHand(h)))AdminNetwork.apply(sp,new AdminNetwork.Request(3,pos,0,0,0,0,0));else NetworkHooks.openScreen(sp,be,pos);}return InteractionResult.sidedSuccess(l.isClientSide);}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())){if(l.getBlockEntity(p) instanceof StationEntity be)Containers.dropContents(l,p,be);super.onRemove(s,l,p,next,moving);}}
}
