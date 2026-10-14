package world.embers.client;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import world.embers.*;
import java.util.*;
/** Resource-pack-editable keyframes. Row format: seconds, x, y, z, pitch, yaw, roll. */
public class AnimationLibrary extends SimpleJsonResourceReloadListener {
    public static final AnimationLibrary INSTANCE=new AnimationLibrary();
    private Map<String,float[][]> clips=Map.of();
    // GeckoLib scans assets/*/animations globally; these keyframes use our own schema.
    private AnimationLibrary(){super(new Gson(),"embers_animations");}
    @Override protected void apply(Map<ResourceLocation,JsonElement> data,ResourceManager resources,ProfilerFiller profiler){Map<String,float[][]> next=new HashMap<>();for(var file:data.entrySet()){if(!file.getKey().getNamespace().equals(Embers.ID))continue;for(var clip:file.getValue().getAsJsonObject().entrySet()){JsonArray frames=clip.getValue().getAsJsonArray();float[][] rows=new float[frames.size()][7];for(int i=0;i<rows.length;i++){JsonArray row=frames.get(i).getAsJsonArray();if(row.size()!=7)throw new JsonParseException("Embers animation keyframe requires 7 values");for(int j=0;j<7;j++)rows[i][j]=row.get(j).getAsFloat();}next.put(clip.getKey(),rows);}}clips=Map.copyOf(next);}
    public void pose(PoseStack p,String name,float seconds){float[][] rows=clips.get(name);if(rows==null||rows.length<2)return;int i=0;while(i<rows.length-2&&seconds>rows[i+1][0])i++;float[] a=rows[i],b=rows[i+1];float f=Hands.ease((seconds-a[0])/Math.max(.001f,b[0]-a[0]));float[] v=new float[7];for(int j=1;j<7;j++)v[j]=a[j]+(b[j]-a[j])*f;p.translate(v[1],v[2],v[3]);p.mulPose(Axis.XP.rotationDegrees(v[4]));p.mulPose(Axis.YP.rotationDegrees(v[5]));p.mulPose(Axis.ZP.rotationDegrees(v[6]));}
}
