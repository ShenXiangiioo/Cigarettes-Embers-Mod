package world.embers.mixin;
import world.embers.MobSmoking;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class DiscountOfferMixin implements MobSmoking.OfferOwner {
    @Unique private Villager embers$villager;
    @Override public void embers$owner(Villager v){embers$villager=v;}
    @Inject(method="getCostA",at=@At("RETURN"),cancellable=true)
    private void embers$price(CallbackInfoReturnable<ItemStack> ci){int discount=MobSmoking.discount(embers$villager);if(discount>0){ItemStack item=ci.getReturnValue().copy();item.setCount(Math.max(1,item.getCount()-discount));ci.setReturnValue(item);}}
    @Inject(method="getSpecialPriceDiff",at=@At("RETURN"),cancellable=true)
    private void embers$networkPrice(CallbackInfoReturnable<Integer> ci){if(MobSmoking.discount(embers$villager)>0){MerchantOffer offer=(MerchantOffer)(Object)this;int base=offer.getBaseCostA().getCount();ci.setReturnValue(offer.getCostA().getCount()-base-Math.max(0,(int)Math.floor(base*offer.getDemand()*offer.getPriceMultiplier())));}}
}
