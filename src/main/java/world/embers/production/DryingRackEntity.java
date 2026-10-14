package world.embers.production;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.FluidTags;
import world.embers.ServerEvents;

public final class DryingRackEntity extends BlockEntity {
    public final int[] tobacco={-1,-1,-1};public final double[] progress=new double[3];public boolean wet;
    public DryingRackEntity(BlockPos p,BlockState s){super(Production.RACK_ENTITY.get(),p,s);}
    public static void tick(Level l,BlockPos p,BlockState s,DryingRackEntity be){if(l.getGameTime()%20!=0)return;be.wet=be.isWet();boolean change=false;for(int i=0;i<3;i++)if(be.tobacco[i]>=0){double old=be.progress[i];be.progress[i]=Math.max(0,Math.min(1,old+(be.wet?-1d/600:Tobacco.at(l,p)==Tobacco.YELLOW?1d/900:1d/1200)));change|=old!=be.progress[i];}if(change)be.changed();}
    public boolean isWet(){
        if(level==null)return false;
        for(BlockPos p:new BlockPos[]{worldPosition,worldPosition.above()}){if(level.getFluidState(p).is(FluidTags.WATER))return true;for(Direction d:Direction.values())if(level.getFluidState(p.relative(d)).is(FluidTags.WATER))return true;}
        BlockPos top=worldPosition.above(2);return level.isRaining()&&level.canSeeSky(top)&&level.getBiome(top).value().getPrecipitationAt(top)!=Biome.Precipitation.NONE;
    }
    public void interact(Player p,InteractionHand h,int slot){ItemStack held=p.getItemInHand(h);if(tobacco[slot]<0){Tobacco t=Production.fresh(held);if(t==null)return;tobacco[slot]=t.ordinal();progress[slot]=storedProgress(held);if(!p.isCreative())held.shrink(1);}else if(held.isEmpty()){ServerEvents.give(p,take(slot));}else return;changed();}
    public ItemStack take(int i){if(tobacco[i]<0)return ItemStack.EMPTY;Tobacco t=Tobacco.values()[tobacco[i]];ItemStack out=new ItemStack((progress[i]>=1-1e-8?Production.DRY:Production.FRESH).get(t).get());if(progress[i]>0&&progress[i]<1-1e-8)out.getOrCreateTag().putDouble("embersDryingProgress",progress[i]*.5);tobacco[i]=-1;progress[i]=0;return out;}
    public static double storedProgress(ItemStack s){double p=s.hasTag()?s.getTag().getDouble("embersDryingProgress"):0;return Double.isFinite(p)?Math.max(0,Math.min(.999999,p)):0;}
    public void dropContents(){if(level==null||level.isClientSide)return;for(int i=0;i<3;i++){ItemStack s=take(i);if(!s.isEmpty())Block.popResource(level,worldPosition,s);}}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putIntArray("tobacco",tobacco);for(int i=0;i<3;i++)n.putDouble("progress"+i,progress[i]);n.putBoolean("wet",wet);}
    @Override public void load(CompoundTag n){super.load(n);int[] ts=n.getIntArray("tobacco");for(int i=0;i<3;i++){tobacco[i]=i<ts.length&&ts[i]>=0&&ts[i]<4?ts[i]:-1;progress[i]=Math.max(0,Math.min(1,n.getDouble("progress"+i)));}wet=n.getBoolean("wet");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
