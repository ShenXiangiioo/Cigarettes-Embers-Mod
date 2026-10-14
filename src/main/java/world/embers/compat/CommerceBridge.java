package world.embers.compat;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraftforge.fml.ModList;

/** Optional adapter for Commerce 1.6.30, resolved before any monetary mutation. */
public final class CommerceBridge {
    public static boolean loaded(){return ModList.get().isLoaded("commerce");}
    public static boolean tablet(ItemStack item){return BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals("commerce:administrator_tablet");}
    public static boolean administrator(ServerPlayer p){if(!loaded())return false;try{return (boolean)Class.forName("com.codex.commerce.util.AdministratorAccess").getMethod("hasTabletAuthority",Player.class).invoke(null,p);}catch(ReflectiveOperationException e){return false;}}
    public static boolean tabletMenu(ServerPlayer p){return administrator(p)&&p.containerMenu.getClass().getName().equals("com.codex.commerce.menu.LoginSecurityAdminMenu")&&p.containerMenu.stillValid(p);}
    public static boolean charge(ServerPlayer p,long fee,String location){
        if(fee==0)return true;if(fee<0||!loaded())return false;
        try {
            Class<?> bankClass=Class.forName("com.codex.commerce.data.BankAccountSavedData"),historyClass=Class.forName("com.codex.commerce.data.CommerceTransactionSavedData");
            Object bank=bankClass.getMethod("get",ServerLevel.class).invoke(null,p.serverLevel()),history=historyClass.getMethod("get",ServerLevel.class).invoke(null,p.serverLevel());
            Method debit=bankClass.getMethod("withdraw",UUID.class,long.class),credit=bankClass.getMethod("creditTreasury",String.class,long.class,String.class,String.class,String.class),record=historyClass.getMethod("record",ServerLevel.class,UUID.class,String.class,long.class,String.class);
            String treasury=(String)bankClass.getField("DEFAULT_BANK_ID").get(null);
            long treasuryBalance=(long)bankClass.getMethod("getTreasuryBalance",String.class).invoke(bank,treasury);
            if(treasuryBalance>Long.MAX_VALUE-fee)return false;
            if(!(boolean)debit.invoke(bank,p.getUUID(),fee))return false;
            // These local SavedData operations run together on the server thread, like Commerce's own service fees.
            credit.invoke(bank,treasury,fee,"embers.refill_fee",p.getGameProfile().getName(),location);
            record.invoke(history,p.serverLevel(),p.getUUID(),"cigarette_refill",fee,location);
            return true;
        }catch(ReflectiveOperationException e){com.mojang.logging.LogUtils.getLogger().error("Commerce refill integration failed",e);return false;}
    }
}
