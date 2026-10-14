package world.embers.qa;
import world.embers.compat.CultivationPermits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("embers") @PrefixGameTestTemplate(false)
public final class CultivationTests {
    @GameTest(template="empty") public static void certificate_holder_and_expiry(GameTestHelper h) {
        UUID holder=UUID.randomUUID();CompoundTag tag=new CompoundTag();
        tag.putString("Holder","Owner");tag.putString("HolderUuid",holder.toString());tag.putLong("ExpiresAt",200);
        h.assertTrue(CultivationPermits.matches(tag,holder,199),"Valid holder before expiry");
        h.assertTrue(!CultivationPermits.matches(tag,holder,200),"Expires at exact boundary");
        h.assertTrue(!CultivationPermits.matches(tag,UUID.randomUUID(),100),"Another player cannot use the certificate");
        tag.putLong("ExpiresAt",0);h.assertTrue(CultivationPermits.matches(tag,holder,Long.MAX_VALUE),"Permanent certificate");
        tag.putBoolean("Revoked",true);h.assertTrue(!CultivationPermits.matches(tag,holder,100),"Revocation wins over permanent expiry");
        tag.putBoolean("Revoked",false);tag.putString("Holder","");h.assertTrue(!CultivationPermits.matches(tag,holder,100),"Unassigned certificate grants nothing");
        tag.putString("Holder","Owner");tag.putString("HolderUuid","");h.assertTrue(!CultivationPermits.matches(tag,holder,100),"Name alone cannot grant UUID privilege");h.succeed();
    }
    @GameTest(template="empty") public static void permit_index_roundtrip(GameTestHelper h) {
        var data=new CultivationPermits();data.grant("one");data.grant("one");data.grant("two");
        CompoundTag saved=data.save(new CompoundTag());h.assertTrue(saved.getList("Certificates",8).size()==2,"No duplicate certificate IDs");
        h.assertTrue(CultivationPermits.load(saved).save(new CompoundTag()).equals(saved),"Survives world save and restart");h.succeed();
    }
}
