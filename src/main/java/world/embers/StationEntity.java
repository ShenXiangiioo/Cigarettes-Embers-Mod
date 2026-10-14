package world.embers;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class StationEntity extends BlockEntity implements Container,MenuProvider {
    private NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    public long fee;
    public StationEntity(BlockPos p,BlockState s){super(Embers.STATION_ENTITY.get(),p,s);}
    public int getContainerSize(){return 3;}public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
    public ItemStack getItem(int i){return items.get(i);}
    public ItemStack removeItem(int i,int n){ItemStack out=ContainerHelper.removeItem(items,i,n);if(!out.isEmpty())setChanged();return out;}
    public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
    public void setItem(int i,ItemStack s){items.set(i,s);s.setCount(Math.min(s.getCount(),getMaxStackSize()));setChanged();}
    public boolean stillValid(Player p){return level!=null&&level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    public void clearContent(){items.clear();setChanged();}
    @Override public void load(CompoundTag n){super.load(n);items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(n,items);fee=Math.max(0,Math.min(1_000_000_000L,n.getLong("fee")));}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);n.putLong("fee",fee);}
    public Component getDisplayName(){return Component.translatable("container.embers.refill");}
    public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new StationMenu(id,inv,this,worldPosition);}
}
