package world.embers.mixin;
import world.embers.MobSmoking;
import net.minecraft.world.entity.npc.*;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractVillager.class)
public abstract class VillagerOffersMixin {
    @Inject(method="getOffers",at=@At("RETURN"))
    private void embers$owners(CallbackInfoReturnable<MerchantOffers> ci){if((Object)this instanceof Villager v)for(var offer:ci.getReturnValue())if(offer instanceof MobSmoking.OfferOwner owner)owner.embers$owner(v);}
}
