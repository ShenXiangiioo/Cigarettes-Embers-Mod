package world.embers.compat;

import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Certificate IDs only: ownership, expiry and revocation remain authoritative in Commerce. */
public final class CultivationPermits extends SavedData {
    private final Set<String> certificates = new HashSet<>();
    public static CultivationPermits get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(CultivationPermits::load, CultivationPermits::new, "embers_cultivation_permits");
    }
    public static CultivationPermits load(CompoundTag tag) {
        CultivationPermits data = new CultivationPermits();
        for (Tag id : tag.getList("Certificates", Tag.TAG_STRING)) data.certificates.add(id.getAsString());
        return data;
    }
    public void grant(String certificateId) { if (certificates.add(certificateId)) setDirty(); }
    @Override public CompoundTag save(CompoundTag tag) {
        ListTag ids = new ListTag(); certificates.stream().sorted().forEach(id -> ids.add(StringTag.valueOf(id)));
        tag.put("Certificates", ids); return tag;
    }
    public boolean allows(ServerPlayer player) {
        if (!CommerceBridge.loaded() || certificates.isEmpty()) return false;
        try {
            Class<?> type = Class.forName("com.codex.commerce.data.QualificationCertificateSavedData");
            Object data = type.getMethod("get", ServerLevel.class).invoke(null, player.serverLevel());
            long now = System.currentTimeMillis();
            for (String id : certificates) {
                Object certificate = type.getMethod("find", String.class).invoke(data, id);
                if (certificate == null) continue;
                CompoundTag record = (CompoundTag) certificate.getClass().getMethod("toTag").invoke(certificate);
                if (matches(record, player.getUUID(), now)) return true;
            }
        } catch (ReflectiveOperationException e) { return false; }
        return false;
    }
    public static boolean matches(CompoundTag record, UUID player, long now) {
        return !record.getBoolean("Revoked") && !record.getString("Holder").isBlank()
                && record.getString("HolderUuid").equals(player.toString())
                && (record.getLong("ExpiresAt") == 0L || now < record.getLong("ExpiresAt"));
    }
}
