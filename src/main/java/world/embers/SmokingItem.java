package world.embers;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.*;
import java.util.function.Consumer;

public class SmokingItem extends Item {
    public final Brand brand;public final boolean pack;
    public SmokingItem(Brand b,boolean pack){super(new Properties().stacksTo(pack?10:16));this.brand=b;this.pack=pack;}
    @Override public int getMaxStackSize(ItemStack stack){return pack?(PackHandling.sealed(stack)?10:1):16;}
    public static int remaining(ItemStack s){return s.hasTag()&&s.getTag().contains("remaining")?Math.max(0,Math.min(10,s.getTag().getInt("remaining"))):10;}
    public static boolean take(ItemStack stack){int n=remaining(stack);if(n==0||stack.getCount()!=1)return false;stack.getOrCreateTag().putInt("remaining",n-1);stack.getOrCreateTag().putBoolean("opened",true);return true;}
    public static int burnMax(ItemStack stack){var n=stack.getTag();if(n!=null&&n.contains("burnMax"))return Math.max(1,Math.min(28800,n.getInt("burnMax")));if(n!=null&&n.contains("burn"))return Math.max(n.getInt("burn"),Embers.BURN_SECONDS.get()*20);return stack.getItem() instanceof SmokingItem item?item.brand.burnTicks():Rules.BURN_TICKS;}
    public static float burnFraction(ItemStack stack){return stack.hasTag()&&stack.getTag().contains("burn")?Math.max(0,Math.min(1,stack.getTag().getInt("burn")/(float)burnMax(stack))):1;}
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){return InteractionResultHolder.pass(p.getItemInHand(h));}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> t,TooltipFlag f){
        if(pack)t.add(Component.translatable("tooltip.embers.remaining",remaining(s),10).withStyle(ChatFormatting.GRAY));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> c){c.accept(new world.embers.client.ItemExtension());}
}
