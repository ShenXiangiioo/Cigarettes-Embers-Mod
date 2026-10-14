package world.embers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
public class AshtrayBlock extends BaseEntityBlock {
    public final int style;
    public AshtrayBlock(){this(0);}
    public AshtrayBlock(int style){super(BlockBehaviour.Properties.of().strength(.8f).sound(SoundType.GLASS).noOcclusion());this.style=style;}
    @Override public RenderShape getRenderShape(BlockState s){return style==0?RenderShape.MODEL:RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Block.box(3,0,3,13,3,13);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new AshtrayEntity(p,s);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level l,BlockState s,net.minecraft.world.level.block.entity.BlockEntityType<T> type){return l.isClientSide?createTickerHelper(type,Embers.ASHTRAY_ENTITY.get(),AshtrayEntity::clientTick):null;}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){
        ItemStack held=p.getItemInHand(h);if(!held.isEmpty()&&!held.is(Embers.BUTT.get()))return InteractionResult.PASS;
        if(!l.isClientSide&&p instanceof ServerPlayer sp&&l.getBlockEntity(pos) instanceof AshtrayEntity be){
            if(held.is(Embers.BUTT.get())){if(be.deposit(false,ButtItem.brand(held))){held.shrink(1);ServerEvents.sound(p,"drop",.2f,1);}else ServerEvents.tip(p,"tray_full");}
            else if(p.isShiftKeyDown()){if(be.count>0){for(ItemStack butt:be.contents())ServerEvents.give(p,butt);be.empty();}}
            else if(be.count>=AshtrayEntity.CAPACITY)ServerEvents.tip(p,"tray_full");
            else if(!ServerEvents.state(sp).action.equals("idle")){ServerEvents.cancel(sp,ServerEvents.state(sp));}
            else ServerEvents.beginDisposal(sp,pos);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())){if(l.getBlockEntity(p) instanceof AshtrayEntity be&&be.count>0)for(ItemStack butt:be.contents())popResource(l,p,butt);super.onRemove(s,l,p,next,moving);}}
}
