package world.embers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
public final class PackHandling {
    public static boolean sealed(ItemStack s){return s.getItem() instanceof SmokingItem i&&i.pack&&SmokingItem.remaining(s)==10&&(!s.hasTag()||!s.getTag().getBoolean("opened"));}
    public static boolean isolate(ServerPlayer p){ItemStack stack=p.getMainHandItem();if(!(stack.getItem() instanceof SmokingItem i)||!i.pack||stack.getCount()<=1)return true;int free=p.getInventory().getFreeSlot();if(free<0){ServerEvents.tip(p,"pack_space");return false;}ItemStack one=stack.copyWithCount(1),rest=stack.copyWithCount(stack.getCount()-1);p.getInventory().setItem(p.getInventory().selected,one);p.getInventory().setItem(free,rest);return true;}
}
