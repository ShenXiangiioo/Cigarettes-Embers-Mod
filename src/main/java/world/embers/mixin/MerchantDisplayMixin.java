package world.embers.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import world.embers.MobSmoking;

@Mixin(ServerPlayer.class)
public abstract class MerchantDisplayMixin {
    @ModifyVariable(method="sendMerchantOffers",at=@At("HEAD"),argsOnly=true)
    private MerchantOffers embers$snapshotDisplayPrices(MerchantOffers offers){return MobSmoking.displayOffers(offers);}
}
