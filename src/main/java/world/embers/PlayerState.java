package world.embers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class PlayerState {
    public String cigaretteId=java.util.UUID.randomUUID().toString();public boolean cigaretteCounted,benefitSpent;public int addictionStrikes;
    public int stress, smoked, idleRelief, fishRelief, bedRelief;
    public boolean stressEnabled=true;
    public int[] gains=new int[7];
    public long day=-1;
    public int ores,kills,animalKills,dimensionTicks,rainTicks,idleTicks,bedTicks;
    public int mouth=-1,burn=Rules.BURN_TICKS,burnMax=Rules.BURN_TICKS,inhaleTotal,inhaleTicks,exhaleTicks;
    public float remaining(){return Math.max(0,Math.min(1,burn/(float)Math.max(1,burnMax)));}
    public boolean lit,sealed=true;
    public float exhaleStrength;
    // Client-only thermal interpolation, not persisted or used for gameplay.
    public float emberHeat,emberHeatOld;
    public net.minecraft.core.BlockPos trayTarget;
    public boolean disposal(){return action.equals("tray_out")||action.equals("toss");}
    public boolean mouthVisible(){return mouth>=0&&(!disposal()||actionTicks<6);}
    public String action="idle";
    public int actionTicks,actionDuration,variant,lighter=-1,lighterSlot=-1,sourceSlot=-1;
    public boolean retry,fuelSpent;
    public int reverseTicks,reverseCooldown=100,blindTimer=1800,nauseaTimer=1800,partner=-1;
    public long lastRequest=-100,lastPuff=-100;
    public double lastX,lastZ;
    public ItemStack actionItem=ItemStack.EMPTY;
    public boolean supportsCigarette(){return action.equals("inhale")&&variant==1;}
    public boolean busy(){return !action.equals("idle")&&!action.equals("offer");}
    public void start(String name,int duration){action=name;actionTicks=0;actionDuration=duration;}
    public void idle(){action="idle";actionTicks=0;actionDuration=0;actionItem=ItemStack.EMPTY;fuelSpent=false;trayTarget=null;}
    public void cancelAction(){idle();exhaleTicks=0;inhaleTicks=0;}
    public void freshCigarette(){cigaretteId=java.util.UUID.randomUUID().toString();cigaretteCounted=benefitSpent=false;addictionStrikes=0;}
    public void writeCigarette(CompoundTag n){n.putString("cigaretteId",cigaretteId);n.putBoolean("cigaretteCounted",cigaretteCounted);n.putBoolean("benefitSpent",benefitSpent);n.putInt("addictionStrikes",addictionStrikes);}
    public void readCigarette(CompoundTag n){freshCigarette();if(n==null)return;if(n.contains("cigaretteId"))cigaretteId=n.getString("cigaretteId");cigaretteCounted=n.getBoolean("cigaretteCounted");benefitSpent=n.getBoolean("benefitSpent");addictionStrikes=n.getInt("addictionStrikes");}
    public void clearMouth(){freshCigarette();mouth=-1;lit=false;burn=burnMax=Rules.BURN_TICKS;inhaleTotal=0;inhaleTicks=0;exhaleTicks=0;}
    public void resetDay(long id){day=id;animalKills=0;smoked=idleRelief=fishRelief=bedRelief=0;gains=new int[7];}
    public void addStress(int source,int amount){addStress(source,amount,Rules.SOURCE_LIMITS[source]);}
    public void addStress(int source,int amount,int cap){if(!stressEnabled)return;int a=Rules.dailyGrant(gains[source],amount,cap);gains[source]+=a;stress=Rules.clampStress(stress+a);}
    public CompoundTag save(){
        CompoundTag n=new CompoundTag();writeCigarette(n);n.putBoolean("stressEnabled",stressEnabled);n.putInt("stress",stress);n.putInt("smoked",smoked);n.putLong("day",day);n.putIntArray("gains",gains);
        n.putInt("idleRelief",idleRelief);n.putInt("fishRelief",fishRelief);n.putInt("bedRelief",bedRelief);
        n.putInt("animalKills",animalKills);n.putFloat("exhaleStrength",exhaleStrength);if(trayTarget!=null)n.putLong("trayTarget",trayTarget.asLong());n.putInt("ores",ores);n.putInt("kills",kills);n.putInt("dimensionTicks",dimensionTicks);n.putInt("rainTicks",rainTicks);n.putInt("idleTicks",idleTicks);
        n.putInt("mouth",mouth);n.putBoolean("lit",lit);n.putInt("burn",burn);n.putInt("burnMax",burnMax);n.putInt("inhaleTotal",inhaleTotal);n.putInt("inhaleTicks",inhaleTicks);n.putInt("exhaleTicks",exhaleTicks);
        n.putString("action",action);n.putInt("actionTicks",actionTicks);n.putInt("actionDuration",actionDuration);n.putInt("variant",variant);n.putInt("lighter",lighter);
        n.putBoolean("retry",retry);n.putBoolean("sealed",sealed);n.putInt("reverseTicks",reverseTicks);n.put("actionItem",actionItem.save(new CompoundTag()));return n;
    }
    public static PlayerState load(CompoundTag n){
        PlayerState s=new PlayerState();if(n.isEmpty())return s;s.readCigarette(n);s.stressEnabled=!n.contains("stressEnabled")||n.getBoolean("stressEnabled");
        s.stress=Rules.clampStress(n.getInt("stress"));s.smoked=n.getInt("smoked");s.day=n.getLong("day");int[] g=n.getIntArray("gains");System.arraycopy(g,0,s.gains,0,Math.min(g.length,s.gains.length));s.animalKills=Math.max(0,n.getInt("animalKills"));s.exhaleStrength=n.getFloat("exhaleStrength");if(n.contains("trayTarget"))s.trayTarget=net.minecraft.core.BlockPos.of(n.getLong("trayTarget"));
        s.idleRelief=n.getInt("idleRelief");s.fishRelief=n.getInt("fishRelief");s.bedRelief=n.getInt("bedRelief");s.ores=n.getInt("ores");s.kills=n.getInt("kills");s.dimensionTicks=n.getInt("dimensionTicks");s.rainTicks=n.getInt("rainTicks");s.idleTicks=n.getInt("idleTicks");
        s.mouth=n.getInt("mouth");if(s.mouth>=Brand.values().length||s.mouth< -1)s.mouth=-1;s.lit=n.getBoolean("lit");s.burn=n.getInt("burn");s.burnMax=n.contains("burnMax")?Math.max(1,Math.min(28800,n.getInt("burnMax"))):Math.max(s.burn,Embers.BURN_SECONDS.get()*20);s.inhaleTotal=n.getInt("inhaleTotal");s.inhaleTicks=n.getInt("inhaleTicks");s.exhaleTicks=n.getInt("exhaleTicks");
        s.action=n.getString("action");if(s.action.isEmpty())s.action="idle";s.actionTicks=n.getInt("actionTicks");s.actionDuration=n.getInt("actionDuration");s.variant=n.getInt("variant");s.lighter=n.getInt("lighter");s.retry=n.getBoolean("retry");s.sealed=n.getBoolean("sealed");s.reverseTicks=n.getInt("reverseTicks");s.actionItem=ItemStack.of(n.getCompound("actionItem"));return s;
    }
}
