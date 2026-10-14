package world.embers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class AshtrayEntity extends BlockEntity {
    public static final int CAPACITY=20;
    public int count;
    private long[] warmUntil=new long[CAPACITY];
    private int[] brands=defaultBrands();
    private static int[] defaultBrands(){int[] ids=new int[CAPACITY];java.util.Arrays.fill(ids,Brand.CHAPMAN.ordinal());return ids;}
    public Brand brand(int index){return index>=0&&index<count?Brand.byId(brands[index]):Brand.CHAPMAN;}
    public java.util.List<net.minecraft.world.item.ItemStack> contents(){var items=new java.util.ArrayList<net.minecraft.world.item.ItemStack>();for(int i=0;i<count;i++)items.add(ButtItem.of(brand(i)));return items;}
    public AshtrayEntity(BlockPos p,BlockState s){super(Embers.ASHTRAY_ENTITY.get(),p,s);}
    @Override public void load(CompoundTag n){super.load(n);count=Math.max(0,Math.min(CAPACITY,n.getInt("count")));brands=defaultBrands();int[] ids=n.getIntArray("brands");for(int i=0;i<Math.min(count,ids.length);i++)brands[i]=Brand.byId(ids[i]).ordinal();warmUntil=new long[CAPACITY];long[] saved=n.getLongArray("warmUntil");System.arraycopy(saved,0,warmUntil,0,Math.min(saved.length,count));}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putInt("count",count);n.putIntArray("brands",brands);n.putLongArray("warmUntil",warmUntil);}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public void update(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public boolean deposit(boolean freshlyExtinguished){return deposit(freshlyExtinguished,Brand.CHAPMAN);}
    public boolean deposit(boolean freshlyExtinguished,Brand brand){if(count>=CAPACITY)return false;brands[count]=brand.ordinal();warmUntil[count]=freshlyExtinguished&&level!=null?level.getGameTime()+40:0;count++;update();return true;}
    public void empty(){count=0;java.util.Arrays.fill(warmUntil,0);brands=defaultBrands();update();}
    public int warmth(int index,long time){if(index<0||index>=count)return 0;long remaining=warmUntil[index]-time;return remaining>0&&remaining<=40?(int)remaining:0;}
    public record ButtPose(double x,double y,double z,float yaw,float tilt,float scale){
        public net.minecraft.world.phys.Vec3 ash(){double a=Math.toRadians(yaw),t=Math.toRadians(tilt),dx=.0013*scale,dy=.013*scale;return new net.minecraft.world.phys.Vec3(x+dx*Math.cos(a)+dy*Math.sin(t)*Math.sin(a),y+dy*Math.cos(t)+.007,z-dx*Math.sin(a)+dy*Math.sin(t)*Math.cos(a));}
    }
    /** One deterministic layout shared by the model and its residual smoke emitters. */
    public ButtPose buttPose(int index){java.util.Random r=new java.util.Random(worldPosition.asLong());int style=((AshtrayBlock)getBlockState().getBlock()).style;ButtPose pose=null;for(int i=0;i<=index;i++)pose=new ButtPose(.35+(i%4)*.095+(r.nextFloat()-.5)*.022,(style==0?.094:.083)+(i%3)*.002,.32+(i/4)*.086+(r.nextFloat()-.5)*.024,r.nextFloat()*360,86+r.nextFloat()*6,3+r.nextFloat()*.35f);return pose;}
    public static void clientTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,AshtrayEntity tray){
        long time=level.getGameTime();for(int i=0;i<tray.count;i++){int remaining=tray.warmth(i,time);if(remaining==0||time%(remaining>24?2:remaining>10?4:8)!=0)continue;var at=tray.buttPose(i).ash().add(pos.getX(),pos.getY(),pos.getZ());level.addParticle(Embers.SMOKE.get(),at.x,at.y,at.z,(level.random.nextDouble()-.5)*.002,.007,(level.random.nextDouble()-.5)*.002);}
    }
}
