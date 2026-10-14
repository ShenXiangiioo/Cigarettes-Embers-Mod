package world.embers.production;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;
import net.minecraftforge.items.wrapper.InvWrapper;
import java.util.*;

/** Reservations and inventory are committed only on the server thread. */
public final class ProcessingEntity extends BlockEntity implements Container,MenuProvider {
    private NonNullList<ItemStack> items=NonNullList.withSize(9,ItemStack.EMPTY),reserved=NonNullList.withSize(6,ItemStack.EMPTY);
    public ItemStack result=ItemStack.EMPTY;public int work,total=6000,fuel;public boolean running,dropMachine=true;
    public String selected="",reason="ready";
    private LazyOptional<IItemHandler> capability=LazyOptional.of(()->new InvWrapper(this){
        @Override public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return slot>=6||running?stack:super.insertItem(slot,stack,simulate);}
        @Override public ItemStack extractItem(int slot,int amount,boolean simulate){return running&&slot<6?ItemStack.EMPTY:super.extractItem(slot,amount,simulate);}
    });
    public ProcessingEntity(BlockPos p,BlockState s){super(Production.MACHINE_ENTITY.get(),p,s);}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){if(cap==ForgeCapabilities.ITEM_HANDLER)return capability.cast();return super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();capability.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();capability=LazyOptional.of(()->new InvWrapper(this){@Override public ItemStack insertItem(int s,ItemStack i,boolean sim){return s>=6||running?i:super.insertItem(s,i,sim);}@Override public ItemStack extractItem(int s,int n,boolean sim){return running&&s<6?ItemStack.EMPTY:super.extractItem(s,n,sim);}});}
    public List<ProcessingRecipe> recipes(){return level==null?List.of():level.getRecipeManager().getAllRecipesFor(Production.TYPE.get()).stream().sorted(Comparator.comparing(r->r.getId().toString())).toList();}
    public ProcessingRecipe recipe(){List<ProcessingRecipe> list=recipes();if(!selected.isEmpty()){ProcessingRecipe preferred=list.stream().filter(r->r.getId().toString().equals(selected)&&r.matches(this,level)).findFirst().orElse(null);if(preferred!=null)return preferred;}List<ProcessingRecipe> match=list.stream().filter(r->r.matches(this,level)).toList();return match.size()==1?match.get(0):null;}
    public boolean choose(int index){if(running||!result.isEmpty())return false;List<ProcessingRecipe> list=recipes();if(index<0||index>=list.size())return false;selected=list.get(index).getId().toString();changed();return true;}
    private boolean fits(int slot,ItemStack stack){ItemStack old=items.get(slot);return old.isEmpty()?stack.getCount()<=Math.min(slot==8?10:64,stack.getMaxStackSize()):ItemStack.isSameItemSameTags(old,stack)&&old.getCount()+stack.getCount()<=Math.min(slot==8?10:64,old.getMaxStackSize());}
    private boolean returnsFit(List<ItemStack> returns){NonNullList<ItemStack> simulated=NonNullList.withSize(2,ItemStack.EMPTY);simulated.set(0,items.get(6).copy());simulated.set(1,items.get(7).copy());for(ItemStack s:returns)if(!putReturn(simulated,s.copy()))return false;return true;}
    private static boolean putReturn(List<ItemStack> into,ItemStack stack){for(int i=0;i<into.size();i++){ItemStack old=into.get(i);if(old.isEmpty()){into.set(i,stack);return true;}if(ItemStack.isSameItemSameTags(old,stack)&&old.getCount()+stack.getCount()<=old.getMaxStackSize()){old.grow(stack.getCount());return true;}}return false;}
    private static List<ItemStack> remainders(ItemStack stack){List<ItemStack> r=new ArrayList<>();if(stack.hasCraftingRemainingItem()){for(int i=0;i<stack.getCount();i++)r.add(stack.getCraftingRemainingItem());}return r;}
    public boolean start(){if(level==null||level.isClientSide||running||!result.isEmpty())return false;running=true;if(!reserve()){running=false;changed();return false;}changed();return true;}
    private boolean reserve(){ProcessingRecipe r=recipe();if(r==null){reason="ingredients";return false;}if(!fits(8,r.output)){reason="output";return false;}int[] use=r.consumption(this);boolean load=fuel<r.fuel;if(load&&(fuel!=0||!items.get(1).is(Items.LAVA_BUCKET))){reason="fuel";return false;}
        NonNullList<ItemStack> next=NonNullList.withSize(6,ItemStack.EMPTY);next.set(2,items.get(2).copyWithCount(1));for(int i=0;i<4;i++){int slot=ProcessingRecipe.INPUT_SLOTS[i];if(use[i]>0)next.set(slot,items.get(slot).copyWithCount(use[i]));}
        List<ItemStack> returned=new ArrayList<>();for(ItemStack s:next)if(!s.isEmpty())returned.addAll(remainders(s));if(load)returned.add(new ItemStack(Items.BUCKET));if(!returnsFit(returned)){reason="containers";return false;}
        if(load){items.get(1).shrink(1);fuel=4;addReturns(List.of(new ItemStack(Items.BUCKET)));}fuel-=r.fuel;
        for(int i=0;i<6;i++)if(!next.get(i).isEmpty())items.get(i).shrink(next.get(i).getCount());reserved=next;result=r.output.copy();work=0;total=r.duration;reason="working";return true;
    }
    private void addReturns(List<ItemStack> returned){List<ItemStack> dst=new ArrayList<>(List.of(items.get(6),items.get(7)));for(ItemStack r:returned){if(!putReturn(dst,r.copy()))throw new IllegalStateException("Reserved container space disappeared");}items.set(6,dst.get(0));items.set(7,dst.get(1));}
    public void stop(){if(level==null||level.isClientSide)return;running=false;if(!result.isEmpty()&&work<total){for(int i=0;i<6;i++)if(!reserved.get(i).isEmpty()){ItemStack s=reserved.get(i);if(items.get(i).isEmpty())items.set(i,s);else if(ItemStack.isSameItemSameTags(items.get(i),s))items.get(i).grow(s.getCount());else Block.popResource(level,worldPosition,s);}reserved=NonNullList.withSize(6,ItemStack.EMPTY);result=ItemStack.EMPTY;work=0;}reason=result.isEmpty()?"stopped":"output";changed();}
    private boolean finish(){if(result.isEmpty()||work<total)return false;List<ItemStack> ret=new ArrayList<>();for(ItemStack s:reserved)ret.addAll(remainders(s));if(!fits(8,result)||!returnsFit(ret)){reason=!fits(8,result)?"output":"containers";return false;}
        if(items.get(8).isEmpty())items.set(8,result);else items.get(8).grow(result.getCount());addReturns(ret);result=ItemStack.EMPTY;reserved=NonNullList.withSize(6,ItemStack.EMPTY);work=0;return true;
    }
    public static void tick(Level l,BlockPos p,BlockState s,ProcessingEntity e){
        for(int i=0;i<6;i++)if(!l.hasChunkAt(ProcessingBlock.part(p,s.getValue(ProcessingBlock.FACING),i)))return;
        if(e.running&&!e.result.isEmpty()&&e.work<e.total){e.work++;e.reason="working";e.setChanged();}
        if(!e.result.isEmpty()&&e.work>=e.total&&e.finish()){if(e.running&&!e.reserve())e.running=false;e.changed();}
        if(l.getGameTime()%20==0&&e.running)e.changed();
    }
    public void dropContents(){if(level==null||level.isClientSide)return;running=false;for(ItemStack s:items)if(!s.isEmpty())Block.popResource(level,worldPosition,s);if(!result.isEmpty()){if(work>=total){Block.popResource(level,worldPosition,result);for(ItemStack s:reserved)for(ItemStack r:remainders(s))Block.popResource(level,worldPosition,r);}else for(ItemStack s:reserved)if(!s.isEmpty())Block.popResource(level,worldPosition,s);}items=NonNullList.withSize(9,ItemStack.EMPTY);reserved=NonNullList.withSize(6,ItemStack.EMPTY);result=ItemStack.EMPTY;}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override public int getContainerSize(){return 9;}@Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    @Override public ItemStack getItem(int i){return items.get(i);}
    @Override public ItemStack removeItem(int i,int n){if(running&&i<6)return ItemStack.EMPTY;ItemStack s=ContainerHelper.removeItem(items,i,n);changed();return s;}
    @Override public ItemStack removeItemNoUpdate(int i){if(running&&i<6)return ItemStack.EMPTY;return ContainerHelper.takeItem(items,i);}
    @Override public void setItem(int i,ItemStack s){if(running&&i<6)return;items.set(i,s);s.setCount(Math.min(s.getCount(),Math.min(i==8?10:64,s.getMaxStackSize())));changed();}
    @Override public boolean canPlaceItem(int i,ItemStack s){if(running||i>=6)return false;if(i==1)return s.is(Items.LAVA_BUCKET);if(i==2)return recipes().stream().anyMatch(r->r.paper.test(s));return recipes().stream().anyMatch(r->r.ingredients().stream().anyMatch(p->p.ingredient().test(s)));}
    @Override public void clearContent(){if(!running){items=NonNullList.withSize(9,ItemStack.EMPTY);changed();}}
    @Override public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(Vec3Center.x(worldPosition),Vec3Center.y(worldPosition),Vec3Center.z(worldPosition))<=64;}
    private static class Vec3Center{static double x(BlockPos p){return p.getX()+.5;}static double y(BlockPos p){return p.getY()+.5;}static double z(BlockPos p){return p.getZ()+.5;}}
    @Override public Component getDisplayName(){return Component.translatable("container.embers.processing");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new ProcessingMenu(id,inv,this);}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);CompoundTag job=new CompoundTag();ContainerHelper.saveAllItems(job,reserved);n.put("reserved",job);n.put("result",result.save(new CompoundTag()));n.putInt("work",work);n.putInt("total",total);n.putInt("fuel",fuel);n.putBoolean("running",running);n.putString("selected",selected);n.putString("reason",reason);}
    @Override public void load(CompoundTag n){super.load(n);items=NonNullList.withSize(9,ItemStack.EMPTY);reserved=NonNullList.withSize(6,ItemStack.EMPTY);ContainerHelper.loadAllItems(n,items);ContainerHelper.loadAllItems(n.getCompound("reserved"),reserved);result=ItemStack.of(n.getCompound("result"));total=Math.max(20,Math.min(72000,n.getInt("total")));work=Math.max(0,Math.min(total,n.getInt("work")));fuel=Math.max(0,Math.min(4,n.getInt("fuel")));running=n.getBoolean("running")&&!result.isEmpty();selected=n.getString("selected");reason=n.getString("reason");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}@Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
