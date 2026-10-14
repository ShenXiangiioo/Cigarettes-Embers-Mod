package world.embers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public class StationMenu extends AbstractContainerMenu {
    public final Container station;public final BlockPos pos;
    private final int[] priceParts=new int[2];
    public long fee(){return station instanceof StationEntity be?be.fee:((priceParts[1]&65535L)<<16)|(priceParts[0]&65535L);}
    public StationMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,new SimpleContainer(3),buf.readBlockPos());}
    public StationMenu(int id,Inventory inv,Container container,BlockPos pos){super(Embers.STATION_MENU.get(),id);station=container;this.pos=pos;
        for(int part=0;part<2;part++){final int n=part;addDataSlot(new DataSlot(){public int get(){return station instanceof StationEntity be?(int)(be.fee>>(n*16))&65535:priceParts[n];}public void set(int value){priceParts[n]=value;}});}
        addSlot(new Slot(container,0,26,43){public boolean mayPlace(ItemStack s){return s.getItem() instanceof LighterItem;}public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(container,1,80,43){public boolean mayPlace(ItemStack s){return !paid()&&s.is(Items.LAVA_BUCKET);}public boolean isActive(){return !paid()||hasItem();}public int getMaxStackSize(){return 1;}});
        addSlot(new Slot(container,2,134,43){public boolean mayPlace(ItemStack s){return false;}public int getMaxStackSize(){return 1;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+y*9+9,8+x*18,106+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,164));
    }
    public boolean paid(){return fee()>0;}
    public boolean canRefill(){return station.getItem(0).getItem() instanceof LighterItem&&LighterItem.needsRefill(station.getItem(0))&&station.getItem(2).isEmpty()&&(paid()||station.getItem(1).is(Items.LAVA_BUCKET));}
    @Override public boolean clickMenuButton(Player p,int id){
        if(id!=0||!stillValid(p)||p.level().isClientSide||!canRefill())return false;
        // Prepare the result before payment; publish it only after the server confirms the debit.
        ItemStack filled=station.getItem(0).copyWithCount(1);LighterItem.refill(filled);
        long cost=fee();
        if(!(p instanceof net.minecraft.server.level.ServerPlayer sp)||!world.embers.compat.CommerceBridge.charge(sp,cost,p.level().dimension().location()+" "+pos.toShortString())){ServerEvents.tip(p,"refill_payment_failed");return false;}
        station.removeItem(0,1);if(cost==0)station.setItem(1,new ItemStack(Items.BUCKET));
        station.setItem(2,filled);station.setChanged();broadcastChanges();ServerEvents.sound(p,"refill",.4f,1);return true;
    }
    @Override public boolean stillValid(Player p){return station.stillValid(p)&&p.level().getBlockState(pos).is(Embers.STATION.get())&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public ItemStack quickMoveStack(Player p,int index){Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;ItemStack in=slot.getItem(),copy=in.copy();boolean moved;if(index<3)moved=moveItemStackTo(in,3,39,true);else if(in.getItem() instanceof LighterItem)moved=moveItemStackTo(in,0,1,false);else if(in.is(Items.LAVA_BUCKET)&&!paid())moved=moveItemStackTo(in,1,2,false);else if(index<30)moved=moveItemStackTo(in,30,39,false);else moved=moveItemStackTo(in,3,30,false);if(!moved)return ItemStack.EMPTY;if(in.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,in);return copy;}
}
