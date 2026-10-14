package world.embers.production;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ProcessingMenu extends AbstractContainerMenu {
    public final ProcessingEntity machine;
    public ProcessingMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,(ProcessingEntity)inv.player.level().getBlockEntity(b.readBlockPos()));}
    public ProcessingMenu(int id,Inventory inv,ProcessingEntity be){super(Production.MENU.get(),id);machine=be;
        int[][] xy={{24,35},{92,35},{160,35},{24,75},{50,75},{76,75},{112,109},{138,109},{234,75}};
        for(int i=0;i<9;i++){final int n=i;addSlot(new Slot(be,i,xy[i][0],xy[i][1]){@Override public boolean mayPlace(ItemStack s){return be.canPlaceItem(n,s);}@Override public boolean mayPickup(Player p){return n>=6||!be.running;}@Override public int getMaxStackSize(){return n==8?10:64;}});}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+y*9+9,58+x*18,141+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,58+x*18,199));
    }
    @Override public boolean stillValid(Player p){return machine.stillValid(p);}
    @Override public boolean clickMenuButton(Player p,int id){if(p.level().isClientSide||!stillValid(p))return false;if(id==0)return machine.start();if(id==1){machine.stop();return true;}if(id>=100)return machine.choose(id-100);return false;}
    @Override public ItemStack quickMoveStack(Player p,int i){if(i<0||i>=slots.size())return ItemStack.EMPTY;Slot s=slots.get(i);if(!s.hasItem()||!s.mayPickup(p))return ItemStack.EMPTY;ItemStack stack=s.getItem(),copy=stack.copy();if(i<9){if(!moveItemStackTo(stack,9,slots.size(),true))return ItemStack.EMPTY;}else {if(machine.running||!moveItemStackTo(stack,0,6,false))return ItemStack.EMPTY;}if(stack.isEmpty())s.set(ItemStack.EMPTY);else s.setChanged();s.onTake(p,stack);return copy;}
}
