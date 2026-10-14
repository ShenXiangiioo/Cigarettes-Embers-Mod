package world.embers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** World-wide authoritative rules; setting an amount to zero disables that condition. */
public final class PressureSettings extends SavedData {
    public static final String[] GAINS={"ore","hostile","dimension","damage","death","rain","animal"};
    public static final String[] RELIEFS={"premium_first","premium_second","premium_later","ordinary_first","ordinary_second","ordinary_later","fish","wake","rest","idle"};
    public boolean effectsEnabled=true,addictionEnabled=true,mobEnabled=true,villagerEnabled=true;public boolean[] brandEffects={true,true,true,true};
    public boolean enabled=Embers.STRESS_ENABLED.get();
    public int[] gain={1,1,1,2,5,1,5},caps=Rules.SOURCE_LIMITS.clone(),relief={11,8,5,7,5,3,2,20,2,1},reliefCaps={20,20,10};
    public static PressureSettings get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(PressureSettings::load,PressureSettings::new,"embers_pressure_settings");}
    public static PressureSettings load(CompoundTag n){PressureSettings s=new PressureSettings();if(n.contains("enabled"))s.enabled=n.getBoolean("enabled");if(n.contains("effectsEnabled"))s.effectsEnabled=n.getBoolean("effectsEnabled");if(n.contains("addictionEnabled"))s.addictionEnabled=n.getBoolean("addictionEnabled");if(n.contains("mobEnabled"))s.mobEnabled=n.getBoolean("mobEnabled");if(n.contains("villagerEnabled"))s.villagerEnabled=n.getBoolean("villagerEnabled");for(int i=0;i<4;i++)if(n.contains("brandEffect"+i))s.brandEffects[i]=n.getBoolean("brandEffect"+i);copy(n,"gain",s.gain);copy(n,"caps",s.caps);copy(n,"relief",s.relief);copy(n,"reliefCaps",s.reliefCaps);return s;}
    private static void copy(CompoundTag n,String key,int[] into){int[] a=n.getIntArray(key);for(int i=0;i<Math.min(a.length,into.length);i++)into[i]=Math.max(0,Math.min(100,a[i]));}
    public boolean configure(int group,int index,int value,int cap){if(value<0||value>100||cap<0||cap>100)return false;if(group==1&&index>=0&&index<gain.length){gain[index]=value;caps[index]=cap;}else if(group==2&&index>=0&&index<relief.length){relief[index]=value;if(index>=6)reliefCaps[index==6?0:index==9?2:1]=cap;}else return false;setDirty();return true;}
    @Override public CompoundTag save(CompoundTag n){n.putBoolean("effectsEnabled",effectsEnabled);n.putBoolean("addictionEnabled",addictionEnabled);n.putBoolean("mobEnabled",mobEnabled);n.putBoolean("villagerEnabled",villagerEnabled);for(int i=0;i<4;i++)n.putBoolean("brandEffect"+i,brandEffects[i]);n.putBoolean("enabled",enabled);n.putIntArray("gain",gain);n.putIntArray("caps",caps);n.putIntArray("relief",relief);n.putIntArray("reliefCaps",reliefCaps);return n;}
    public int smokingRelief(boolean premium,int ordinal){return relief[(premium?0:3)+Math.min(2,Math.max(0,ordinal-1))];}
    public boolean feature(int index){return switch(index){case 0->effectsEnabled;case 1,2,3,4->brandEffects[index-1];case 5->addictionEnabled;case 6->mobEnabled;case 7->villagerEnabled;default->false;};}
    public boolean feature(int index,boolean value){switch(index){case 0->effectsEnabled=value;case 1,2,3,4->brandEffects[index-1]=value;case 5->addictionEnabled=value;case 6->mobEnabled=value;case 7->villagerEnabled=value;default->{return false;}}setDirty();return true;}
}
