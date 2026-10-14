package world.embers.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Vector3f;

/** Lathed cut-glass bowl and low octagonal bronze bowl; shared by block and inventory. */
public final class AshtrayVisuals {
    public static void render(PoseStack p,MultiBufferSource b,int style,int light){
        boolean glass=style==1;int n=glass?32:16;
        var out=b.getBuffer(glass?RenderType.entityTranslucent(Visuals.WHITE):RenderType.entityCutoutNoCull(Visuals.WHITE));
        // Closed cross-section: underside, bevelled base, outer wall, rim, inner wall, floor.
        float[][] profile=glass?new float[][]{{0,.012f},{.265f,.012f},{.31f,.032f},{.315f,.145f},{.292f,.171f},{.262f,.158f},{.24f,.063f},{0,.063f}}
            :new float[][]{{0,.012f},{.29f,.012f},{.322f,.037f},{.307f,.127f},{.277f,.142f},{.252f,.13f},{.239f,.065f},{0,.065f}};
        for(int r=0;r<profile.length-1;r++)for(int j=0;j<n;j++){
            double a=j*Math.PI*2/n,aa=(j+1)*Math.PI*2/n;
            float notch=(float)Math.pow(Math.max(0,Math.cos(a*4)),10)*(glass?.019f:.013f);
            float nextNotch=(float)Math.pow(Math.max(0,Math.cos(aa*4)),10)*(glass?.019f:.013f);
            int alpha=glass?(r==3?175:r==0||r==6?62:92):255;
            float glint=(float)(.5+.5*Math.sin(a*3+.4));
            int color=glass?(j%4==0?0xE7FCF8:0xA1C5C4):r==3?0xB49151:r==5||r==6?0x444340:(j%2==0?0x796445:0x93805B);
            float r1=profile[r][0],r2=profile[r+1][0],y1=profile[r][1]-(r>=3&&r<=5?notch:0),y2=profile[r+1][1]-(r+1>=3&&r+1<=5?notch:0);
            float Y1=profile[r][1]-(r>=3&&r<=5?nextNotch:0),Y2=profile[r+1][1]-(r+1>=3&&r+1<=5?nextNotch:0);
            Vector3f A=new Vector3f(r1*(float)Math.sin(a),y1,r1*(float)Math.cos(a)),B=new Vector3f(r1*(float)Math.sin(aa),Y1,r1*(float)Math.cos(aa)),C=new Vector3f(r2*(float)Math.sin(aa),Y2,r2*(float)Math.cos(aa)),D=new Vector3f(r2*(float)Math.sin(a),y2,r2*(float)Math.cos(a));
            quad(p,out,A,B,C,D,color,alpha,light);
            if(glass&&r==3){
                // A thin cut edge stays readable even when a shader fades clear glass.
                var edge=b.getBuffer(RenderType.entityCutoutNoCull(Visuals.WHITE));
                Vector3f c=new Vector3f(C).add(0,.0005f,0),d=new Vector3f(D).add(0,.0005f,0);
                quad(p,edge,d,c,new Vector3f(c).mul(.994f,1,.994f),new Vector3f(d).mul(.994f,1,.994f),j%4==0?0xD6E5E2:0x94ADAB,255,light);
            }
            if(glass&&r==2&&j%4==0){
                var edge=b.getBuffer(RenderType.entityCutoutNoCull(Visuals.WHITE));
                Vector3f a1=new Vector3f(A).mul(1.002f,1,1.002f),d1=new Vector3f(D).mul(1.002f,1,1.002f);
                Vector3f tangent=new Vector3f(-a1.z,0,a1.x).normalize().mul(.00055f);
                quad(p,edge,new Vector3f(a1).sub(tangent),new Vector3f(a1).add(tangent),new Vector3f(d1).add(tangent),new Vector3f(d1).sub(tangent),0xA8BDBA,255,light);
            }
        }
        // The profile already has a floor at .065. The old extra cylinder ended
        // at exactly the same height, causing depth fighting, especially with shaders.
    }
    private static void quad(PoseStack p,VertexConsumer v,Vector3f a,Vector3f b,Vector3f c,Vector3f d,int color,int alpha,int light){
        Vector3f normal=new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a));
        if(normal.lengthSquared()<1e-10f)normal=new Vector3f(c).sub(b).cross(new Vector3f(d).sub(b));
        if(normal.lengthSquared()<1e-10f)return;normal.normalize();
        for(Vector3f point:new Vector3f[]{a,b,c,d})v.vertex(p.last().pose(),point.x,point.y,point.z).color(color>>16&255,color>>8&255,color&255,alpha).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),normal.x,normal.y,normal.z).endVertex();
    }
}
