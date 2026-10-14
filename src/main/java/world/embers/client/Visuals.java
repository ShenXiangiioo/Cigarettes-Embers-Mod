package world.embers.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import world.embers.*;

/** Shared small-scale prop geometry. Rounded metal, cylindrical paper and independent flame gravity. */
public final class Visuals {
    public static final ResourceLocation WHITE=texture("misc/white");
    private static final ResourceLocation PAPER=texture("material/paper"),FILTER=texture("material/filter"),ASH=texture("material/ash"),METAL=texture("material/metal");
    private static final int FULL=LightTexture.FULL_BRIGHT;
    static ResourceLocation texture(String path){return new ResourceLocation(Embers.ID,"textures/"+path+".png");}
    private static ResourceLocation packTexture(String path){return texture((net.minecraft.client.Minecraft.getInstance().options.languageCode.startsWith("zh")?"pack/":"pack_en/")+path);}
    private static int shade(int c,float factor){return (Math.min(255,(int)((c>>16&255)*factor))<<16)|(Math.min(255,(int)((c>>8&255)*factor))<<8)|Math.min(255,(int)((c&255)*factor));}
    private static void vertex(PoseStack p,VertexConsumer v,float x,float y,float z,float u,float vv,float nx,float ny,float nz,int color,int alpha,int light){
        v.vertex(p.last().pose(),x,y,z).color(color>>16&255,color>>8&255,color&255,alpha).uv(u,vv).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.last().normal(),nx,ny,nz).endVertex();
    }
    private static void face(PoseStack p,VertexConsumer v,float[][] a,float nx,float ny,float nz,int color,int light){
        for(int i=0;i<4;i++)vertex(p,v,a[i][0],a[i][1],a[i][2],i==0||i==3?0:1,i<2?1:0,nx,ny,nz,color,255,light);
    }
    public static void box(PoseStack p,MultiBufferSource b,float x,float y,float z,float w,float h,float d,int color,int light){texturedBox(p,b,WHITE,x,y,z,w,h,d,color,light);}
    private static void texturedBox(PoseStack p,MultiBufferSource b,ResourceLocation tex,float x,float y,float z,float w,float h,float d,int color,int light){
        VertexConsumer v=b.getBuffer(PropRenderType.cutout(tex));float X=x+w,Y=y+h,Z=z+d;
        face(p,v,new float[][]{{x,y,Z},{X,y,Z},{X,Y,Z},{x,Y,Z}},0,0,1,color,light);
        face(p,v,new float[][]{{X,y,z},{x,y,z},{x,Y,z},{X,Y,z}},0,0,-1,color,light);
        face(p,v,new float[][]{{X,y,Z},{X,y,z},{X,Y,z},{X,Y,Z}},1,0,0,color,light);
        face(p,v,new float[][]{{x,y,z},{x,y,Z},{x,Y,Z},{x,Y,z}},-1,0,0,color,light);
        face(p,v,new float[][]{{x,Y,Z},{X,Y,Z},{X,Y,z},{x,Y,z}},0,1,0,color,light);
        face(p,v,new float[][]{{x,y,z},{X,y,z},{X,y,Z},{x,y,Z}},0,-1,0,color,light);
    }
    private static void sheet(PoseStack p,MultiBufferSource b,ResourceLocation tex,float x,float y,float z,float w,float h,int light,float v0,float v1){
        var v=b.getBuffer(PropRenderType.cutout(tex));
        vertex(p,v,x,y,z,0,v1,0,0,1,0xFFFFFF,255,light);vertex(p,v,x+w,y,z,1,v1,0,0,1,0xFFFFFF,255,light);
        vertex(p,v,x+w,y+h,z,1,v0,0,0,1,0xFFFFFF,255,light);vertex(p,v,x,y+h,z,0,v0,0,0,1,0xFFFFFF,255,light);
    }
    public static void picture(PoseStack p,MultiBufferSource b,ResourceLocation t,float x,float y,float z,float w,float h,int light){sheet(p,b,t,x,y,z,w,h,light,0,1);}
    private static void rounded(PoseStack p,MultiBufferSource b,float w,float y,float h,float d,float radius,int color,boolean metal,int light){
        int n=24;float[][] ring=new float[n][2];
        for(int j=0;j<n;j++){double a=j*Math.PI*2/n;float sx=(float)Math.sin(a),cz=(float)Math.cos(a);ring[j][0]=Math.signum(sx)*(w/2-radius)+radius*sx;ring[j][1]=Math.signum(cz)*(d/2-radius)+radius*cz;}
        var v=b.getBuffer(PropRenderType.cutout(metal?METAL:PAPER));
        for(int j=0;j<n;j++){
            int k=(j+1)%n;float dx=ring[k][0]-ring[j][0],dz=ring[k][1]-ring[j][1],len=(float)Math.sqrt(dx*dx+dz*dz);float nx=-dz/len,nz=dx/len;
            Vector3f normal=new Vector3f(nx,0,nz);p.last().normal().transform(normal);normal.normalize();
            float gleam=metal?.80f+.48f*(float)Math.pow(Math.max(0,normal.z*.87f+normal.y*.4f),14):1;
            int col=shade(color,gleam);
            face(p,v,new float[][]{{ring[j][0],y,ring[j][1]},{ring[k][0],y,ring[k][1]},{ring[k][0],y+h,ring[k][1]},{ring[j][0],y+h,ring[j][1]}},nx,0,nz,col,light);
            face(p,v,new float[][]{{0,y+h,0},{ring[j][0],y+h,ring[j][1]},{ring[k][0],y+h,ring[k][1]},{0,y+h,0}},0,1,0,col,light);
            face(p,v,new float[][]{{0,y,0},{ring[k][0],y,ring[k][1]},{ring[j][0],y,ring[j][1]},{0,y,0}},0,-1,0,shade(col,.85f),light);
        }
    }
    public static void pack(PoseStack p,MultiBufferSource b,Brand brand,int count,float open,int light){
        if(brand.horizontalCase()){casePack(p,b,brand,count,open,light);return;}
        float w=brand.slim?.092f:.109f,h=.168f,d=brand.slim?.036f:.052f,hinge=.126f;
        var wrapper=packTexture(brand.id);
        rounded(p,b,w,0,hinge,d,brand.premium?.0025f:.0014f,brand.color,false,light);
        packSides(p,b,brand,w,hinge,d,light);
        sheet(p,b,wrapper,-w/2,.0001f,d/2+.0002f,w,hinge,light,.25f,1);
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180));picture(p,b,packTexture(brand.id+"_back"),-w/2,.001f,d/2+.0002f,w,hinge,light);p.popPose();
        // Thin folded collar and a recessed interior, never a solid slab across the cigarettes.
        texturedBox(p,b,PAPER,-w/2+.002f,hinge-.014f,-d/2+.001f,w-.004f,.026f,.002f,0xD6CDB7,light);
        box(p,b,-w/2+.001f,hinge-.015f,-d/2+.001f,.002f,.029f,d-.002f,0xD8D0BB,light);
        box(p,b,w/2-.003f,hinge-.015f,-d/2+.001f,.002f,.029f,d-.002f,0xD8D0BB,light);
        box(p,b,-w/2+.002f,hinge-.004f,d/2-.002f,w-.004f,.015f,.002f,0xE3DDCB,light);
        // Recessed foil lip, a folded collar and individual top ends remain visible when open.
        texturedBox(p,b,METAL,-w/2+.004f,hinge-.010f,d/2-.005f,w-.008f,.007f,.001f,brand.premium?0xBAA16C:0xB7B8B1,light);
        for(int i=0;i<count;i++){
            p.pushPose();p.translate(-w/2+.014+(4-i%5)*(w-.025)/5,hinge-.006,-.011+(i/5)*.021);
            cylinder(p,b,texture("cigarette/"+brand.id+"_filter"),brand.cigaretteRadius(),.026f,0xFFFFFF,light,24);p.translate(0,.026,0);cylinder(p,b,PAPER,brand.cigaretteRadius(),.0007f,0xF3EBDA,light,16);p.popPose();
        }
        p.pushPose();p.translate(0,hinge,-d/2);p.mulPose(Axis.XP.rotationDegrees(-open*112));
        // Hollow flip-top: back, top and front lip pivot together around the rear seam.
        box(p,b,-w/2,0,0,w,h-hinge,.0016f,brand.color,light);
        box(p,b,-w/2,h-hinge-.002f,0,w,.002f,d,brand.color,light);
        box(p,b,-w/2,0,d-.002f,w,h-hinge,.002f,brand.color,light);
        box(p,b,-w/2,0,0,.0015f,h-hinge,d,brand.color,light);box(p,b,w/2-.0015f,0,0,.0015f,h-hinge,d,brand.color,light);
        sheet(p,b,wrapper,-w/2,0,d+.0002f,w,h-hinge,light,0,.25f);p.popPose();
    }
    private static void packSides(PoseStack p,MultiBufferSource b,Brand brand,float w,float h,float d,int light){
        // Side folds, bottom tuck flap and a narrow foil impression are geometry,
        // not printed lines on the front cover. No real-world brand is introduced.
        int fold=shade(brand.color,.72f),edge=shade(brand.color,1.10f);
        for(int sign:new int[]{-1,1}){
            float x=sign*(w/2+.00015f);
            box(p,b,x-.00018f,.003f,-d*.22f,.00036f,h-.007f,.0007f,fold,light);
            box(p,b,x-.0002f,h-.0021f,-d/2+.002f,.0004f,.0011f,d-.004f,fold,light);
            box(p,b,x-.0002f,.012f,-d*.22f,.0004f,.0006f,d*.42f,brand.accent,light);
            box(p,b,x-.0002f,.015f,-d*.22f,.0004f,.0006f,d*.42f,shade(brand.accent,.75f),light);
        }
        box(p,b,-w/2+.002f,.001f,-d/2-.00015f,w-.004f,.004f,.0003f,edge,light);
        box(p,b,-w/2+.0015f,h-.0008f,d/2+.00025f,w-.003f,.0012f,.0004f,fold,light);
        // Bottom folded envelope seam, visible as the box tilts during extraction.
        var out=b.getBuffer(PropRenderType.cutout(PAPER));
        face(p,out,new float[][]{{-w/2+.002f,-.00015f,-d/2+.002f},{0,-.00015f,d*.12f},{w/2-.002f,-.00015f,-d/2+.002f},{-w/2+.002f,-.00015f,-d/2+.002f}},0,-1,0,shade(brand.color,.84f),light);
    }
    private static void casePack(PoseStack p,MultiBufferSource b,Brand brand,int count,float open,int light){
        float w=.181f,h=.101f,d=.029f,wall=.0024f;
        // A wide case hinged along its upper long edge. Ten full cylinders lie side by side, pointing across the case.
        texturedBox(p,b,PAPER,-w/2,0,-d/2,w,h,wall,brand.color,light);
        box(p,b,-w/2,0,-d/2,w,wall,d,brand.color,light);box(p,b,-w/2,h-wall,-d/2,w,wall,d,brand.color,light);
        box(p,b,-w/2,0,-d/2,wall,h,d,brand.color,light);box(p,b,w/2-wall,0,-d/2,wall,h,d,brand.color,light);
        picture(p,b,packTexture(brand.id+"_inside"),-w/2+wall,wall,-d/2+wall+.0002f,w-2*wall,h-2*wall,light);
        for(int i=0;i<count;i++){
            p.pushPose();p.translate(-.077,.0085+i*.0092,0);p.mulPose(Axis.ZP.rotationDegrees(-90));p.scale(.90f,.99f,.90f);cigarette(p,b,brand,1,false,false,light);p.popPose();
        }
        // Three short hinge barrels, fine raised rails and a recessed latch distinguish
        // this shallow hinged case from a book and from a cardboard flip-top pack.
        for(int i=-1;i<=1;i++){p.pushPose();p.translate(i*.056,h-.001,d/2-.001);p.mulPose(Axis.ZP.rotationDegrees(90));cylinder(p,b,METAL,.0028f,.024f,shade(brand.accent,.65f),light,16);p.popPose();}
        box(p,b,-w/2+.003f,.002f,d/2-.003f,w-.006f,.0011f,.0011f,shade(brand.accent,.8f),light);
        box(p,b,-.010f,-.0006f,d/2-.001f,.020f,.0032f,.003f,shade(brand.accent,.8f),light);
        // Swing outward before folding above the long edge, never through the cigarettes.
        p.pushPose();p.translate(-w/2,h,d/2);p.mulPose(Axis.XP.rotationDegrees(-open*145));
        texturedBox(p,b,PAPER,0,-h,-wall,w,h,wall,brand.color,light);
        picture(p,b,packTexture(brand.id),0,-h,.0002f,w,h,light);
        p.pushPose();p.translate(w,0,-wall-.0002f);p.mulPose(Axis.YP.rotationDegrees(180));picture(p,b,packTexture(brand.id+"_inside"),0,-h,0,w,h,light);p.popPose();p.popPose();
    }
    public static void cylinder(PoseStack p,MultiBufferSource b,ResourceLocation tex,float radius,float height,int color,int light,int segments){
        var v=b.getBuffer(PropRenderType.cutout(tex));
        for(int j=0;j<segments;j++){
            double a=j*Math.PI*2/segments,aa=(j+1)*Math.PI*2/segments;float x=(float)Math.cos(a),z=(float)Math.sin(a),X=(float)Math.cos(aa),Z=(float)Math.sin(aa),u=j/(float)segments,U=(j+1)/(float)segments;
            vertex(p,v,x*radius,0,z*radius,u,1,x,0,z,color,255,light);vertex(p,v,X*radius,0,Z*radius,U,1,X,0,Z,color,255,light);
            vertex(p,v,X*radius,height,Z*radius,U,0,X,0,Z,color,255,light);vertex(p,v,x*radius,height,z*radius,u,0,x,0,z,color,255,light);
            face(p,v,new float[][]{{0,height,0},{x*radius,height,z*radius},{X*radius,height,Z*radius},{0,height,0}},0,1,0,color,light);
            face(p,v,new float[][]{{0,0,0},{X*radius,0,Z*radius},{x*radius,0,z*radius},{0,0,0}},0,-1,0,color,light);
        }
    }
    public static void cigarette(PoseStack p,MultiBufferSource b,Brand brand,float remaining,boolean lit,boolean inhale,int light){cigarette(p,b,brand,remaining,lit,inhale?1f:0f,light);}
    public static void cigarette(PoseStack p,MultiBufferSource b,Brand brand,float remaining,boolean lit,float heat,int light){
        float r=brand.cigaretteRadius(),len=brand.cigaretteLength(remaining),filterLength=brand.filterLength();
        ResourceLocation filter=texture("cigarette/"+brand.id+"_filter"),paper=texture("cigarette/"+brand.id+"_paper");
        cylinder(p,b,filter,r,filterLength-.001f,0xFFFFFF,light,32);
        p.pushPose();p.translate(0,filterLength-.003f,0);cylinder(p,b,WHITE,r*1.004f,.0012f,brand.accent,light,20);p.popPose();
        p.pushPose();p.mulPose(Axis.YP.rotationDegrees(32+brand.ordinal()*17));
        box(p,b,r*.93f,filterLength+.001f,-.00022f,.00012f,Math.max(.001f,len-filterLength-.009f),.00045f,brand.horizontalCase()?brand.color:0xBEBBAF,light);p.popPose();
        if(lit||remaining<.98f)burningEnd(p,b,brand,r,len,remaining,lit,heat,light);
        else {
            p.pushPose();p.translate(0,filterLength,0);cylinder(p,b,paper,r,Math.max(.001f,len-filterLength),0xFFFFFF,light,32);
            p.translate(0,len-filterLength,0);cylinder(p,b,FILTER,r,.0008f,0x715134,light,32);p.popPose();
        }
    }
    private static float grain(int index,int seed){int x=index*374761393+seed*668265263;x=(x^(x>>>13))*1274126177;return ((x^(x>>>16))&65535)/65535f;}
    private static float burnGrain(int index,int seed,float burn){int step=(int)burn;float f=burn-step;f=f*f*(3-2*f);return grain(index,seed+step)*(1-f)+grain(index,seed+step+1)*f;}
    private static int blend(int a,int b,float f){int out=0;for(int shift:new int[]{16,8,0})out|=((int)((a>>shift&255)*(1-f)+(b>>shift&255)*f))<<shift;return out;}
    private static void burningEnd(PoseStack p,MultiBufferSource b,Brand brand,float radius,float length,float remaining,boolean lit,float heat,int light){
        int n=40,seed=brand.ordinal()*73+19;float consumed=1-remaining,burn=consumed*37;
        float cycle=consumed*9,phase=cycle-(int)cycle;
        float growth=phase<.88f?phase/.88f:1-Hands.ease((phase-.88f)/.12f);
        // Compact ash with a blunt, mildly chipped face. The coal is at the root,
        // behind the ash, not a glowing pointed cap. Geometry evolves with consumption.
        float ashLength=(.002f+.010f*growth)*Math.min(1,consumed*38);
        float root=Math.max(brand.filterLength()+.0005f,length-.0038f);
        float[][][] ring=new float[7][n][3];
        for(int j=0;j<n;j++){
            double a=j*Math.PI*2/n;float c=(float)Math.cos(a),sn=(float)Math.sin(a);
            float grain=burnGrain(j,seed,burn),fine=burnGrain(j,seed+121,burn);
            float edge=root+.00065f*(float)Math.sin(a*3+seed)+.00045f*(grain-.5f);
            float top=root+.0028f+ashLength+.0007f*(fine-.5f)+.0004f*(float)Math.sin(a*4+seed);
            float[] y={brand.filterLength(),edge,edge+.00065f,edge+.0023f,top-.0013f,top,top+.00015f*(grain-.5f)};
            float[] r={radius,radius,radius*(.96f+.035f*fine),radius*(.95f+.05f*grain),radius*(.93f+.07f*fine),radius*(.87f+.09f*grain),radius*.48f};
            for(int k=0;k<7;k++)ring[k][j]=new float[]{c*r[k],y[k],sn*r[k]};
        }
        for(int k=0;k<6;k++)for(int j=0;j<n;j++){
            int next=(j+1)%n;float g=burnGrain(j,seed+77,burn),a=j*(float)Math.PI*2/n;
            int col=k==0?0xFFFFFF:k==1?shade(0x352C27,.70f+g*.3f):k==2?shade(0x44332A,.65f+g*.35f):shade(g>.87f?0x77716A:0xCCC9C1,.82f+g*.18f);
            var out=b.getBuffer(PropRenderType.cutout(k==0?texture("cigarette/"+brand.id+"_paper"):ASH));
            float[][] points={ring[k][j],ring[k][next],ring[k+1][next],ring[k+1][j]};
            for(int q=0;q<4;q++)vertex(p,out,points[q][0],points[q][1],points[q][2],q==0||q==3?j/(float)n:next/(float)n,q<2?1:0,(float)Math.cos(a),.12f,(float)Math.sin(a),col,255,light);
            if(lit&&(k==2||k==1&&g>.6f)){
                // Visible red coal at rest, warming to amber across the inhalation.
                int hot=blend(0xAE3510,0xFFD48C,heat*(.78f+.22f*g));
                int alpha=(int)((k==2?100:35)+(k==2?145:105)*heat);
                var glow=b.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
                for(int q=0;q<4;q++)vertex(p,glow,points[q][0]*1.005f,points[q][1],points[q][2]*1.005f,0,0,(float)Math.cos(a),.1f,(float)Math.sin(a),hot,alpha,FULL);
            }
        }
        var ash=b.getBuffer(PropRenderType.cutout(ASH));
        for(int j=0;j<n;j++){
            int next=(j+1)%n;float g=burnGrain(j,seed+251,burn);
            float[] center={radius*.05f,root+.0028f+ashLength-.00025f,-radius*.04f};
            face(p,ash,new float[][]{center,ring[6][j],ring[6][next],center},0,1,0,shade(0xBDBAB2,.84f+g*.16f),light);
        }
    }

    public static void lighter(PoseStack p,MultiBufferSource b,LighterKind k,float lid,boolean flame,float time,int light){lighter(p,b,k,lid,flame,time,light,new Vector3f(0,1,0));}
    public static void lighter(PoseStack p,MultiBufferSource b,LighterKind k,float lid,boolean flame,float time,int light,Vector3f up){
        float w=k.lid?.050f:k.jet?.039f:.033f,h=k.lid?.063f:.076f,d=k.lid?.017f:.023f;
        rounded(p,b,w,0,h,d,.0032f,k.color,k.lid||k.jet,light);
        PropFinish.lighter(p,b,k,w,h,d,light);
        // Narrow rolled base and separate brushed insert make the shell read as manufactured metal.
        rounded(p,b,w-.001f,.001f,.0013f,d-.001f,.003f,k.lid?0xC5B485:0x646760,true,light);
        if(k.lid){
            rounded(p,b,w-.006f,h-.002f,.018f,d-.003f,.0017f,0xB5B4AC,true,light);
            for(int j=0;j<4;j++)for(int row=0;row<2;row++)box(p,b,-.017f+j*.009f,h+.003f+row*.007f,d/2-.0012f,.0035f,.0033f,.0002f,0x292824,light);
            p.pushPose();p.translate(-w/2,h,0);p.mulPose(Axis.ZP.rotationDegrees(lid*155));p.translate(w/2,0,0);rounded(p,b,w,0,.024f,d,.0032f,k.color,true,light);p.popPose();
            p.pushPose();p.translate(.014,h+.016,0);p.mulPose(Axis.XP.rotationDegrees(90));cylinder(p,b,METAL,.006f,.010f,0x9E9C94,light,20);p.popPose();
            if(flame)flame(p,b,-.008f,h+.019f,0,.043f,k,time,up);
        }else if(k.jet){
            rounded(p,b,w,h,.009f,d,.002f,0x24272A,false,light);
            int jets=k==LighterKind.JET_SILVER?3:k==LighterKind.JET_COPPER?2:1;
            for(int j=0;j<jets;j++){
                float x=(j-(jets-1)/2f)*.010f;p.pushPose();p.translate(x,h+.008f,0);cylinder(p,b,METAL,.0046f,.004f,0xB9C0C0,light,16);cylinder(p,b,WHITE,.0027f,.0045f,0x27272A,light,16);p.popPose();
                if(flame)flame(p,b,x,h+.012f,0,.030f,k,time+j,up);
            }
            box(p,b,w/2-.001f,h-.013f,-.006f,.0024f,.013f,.012f,0x41464B,light);
        }else {
            rounded(p,b,w,h-.002f,.015f,d,.003f,0xC0C1B9,true,light);
            for(int j=0;j<3;j++)box(p,b,-.012f+j*.007f,h+.003f,d/2+.0001f,.003f,.004f,.0002f,0x343632,light);
            if(k==LighterKind.WHEEL){p.pushPose();p.translate(.010,h+.011,-.005);p.mulPose(Axis.XP.rotationDegrees(90));cylinder(p,b,METAL,.006f,.011f,0x787B76,light,24);p.popPose();}
            else box(p,b,.007f,h+.003f,-.008f,.010f,.012f,.016f,0x3B3F39,light);
            if(flame)flame(p,b,-.007f,h+.014f,0,.038f,k,time,up);
        }
    }
    private static void flame(PoseStack p,MultiBufferSource b,float x,float y,float z,float h,LighterKind kind,float t,Vector3f up){
        boolean jet=kind.jet;
        p.pushPose();p.translate(x,y,z);
        if(!jet){Vector3f localUp=new Matrix3f(p.last().normal()).invert().transform(new Vector3f(up)).normalize();p.mulPose(new Quaternionf().rotationTo(new Vector3f(0,1,0),localUp));}
        float flicker=1+(noise(t*.65f,11)-.5f)*(jet?.12f:.28f);
        int outer=switch(kind){case JET_BLACK->0x3279FF;case JET_COPPER->0x22C9B0;case JET_SILVER->0x8F62ED;case OIL_BLACK->0x6C51E7;case OIL_CHROME->0xB1A1DB;default->0xE77823;};
        int tip=switch(kind){case JET_COPPER->0x7DEBDA;case JET_SILVER->0xC9BCFF;case OIL_BLACK->0xADC8FF;default->jet?0x9DDEFF:0xFAD591;};
        fireVolume(p,b,h*flicker,jet?.0030f:.0058f,outer,tip,jet?100:78,t,jet,17);
        fireVolume(p,b,h*(jet?.64f:.71f)*flicker,jet?.00145f:.0028f,jet?0x709ADF:0xE78329,jet?0xC3E7FF:0xFFE5A0,jet?160:115,t,jet,53);
        // A small blue root and a separate off-center tongue merge into the main flame.
        fireVolume(p,b,h*.20f,jet?.0019f:.0035f,0x326DA9,0x829BB7,90,t,true,89);
        if(!jet){p.translate(.0012*(noise(t*.21f,7)-.5),0,.0014);fireVolume(p,b,h*(.62f+.20f*noise(t*.22f,33)),.0024f,outer,tip,48,t+5,false,101);}
        p.popPose();
    }
    private static float noise(float x,int seed){int n=(int)Math.floor(x);float f=x-n;f=f*f*(3-2*f);return grain(n,seed)*(1-f)+grain(n+1,seed)*f;}
    private static void fireVolume(PoseStack p,MultiBufferSource b,float h,float radius,int bottom,int top,int opacity,float t,boolean jet,int seed){
        var v=b.getBuffer(RenderType.entityTranslucentEmissive(WHITE));int sides=20,rings=18;
        for(int r=0;r<rings;r++)for(int j=0;j<sides;j++){
            float[][] corners={{r/(float)rings,j/(float)sides},{r/(float)rings,(j+1)/(float)sides},{(r+1)/(float)rings,(j+1)/(float)sides},{(r+1)/(float)rings,j/(float)sides}};
            for(float[] q:corners){float f=q[0];double a=q[1]*Math.PI*2;
                float envelope=jet?(1-f)*(1-f*.35f):(float)Math.pow(Math.max(0,Math.sin(Math.PI*(.07+f*.93))),.72);
                float eddy=noise(t*.32f-f*2.7f,seed),twist=noise(t*.23f-f*3.3f,seed+29);
                float width=radius*envelope*(1+(eddy-.5f)*(jet?.16f:.52f));
                width*=1+(jet?.04f:.16f)*(float)Math.sin(a*3+twist*8+f*6);
                float sx=(noise(t*.18f-f*1.7f,seed+5)-.5f)*f*f*h*(jet?.04f:.46f);
                float sz=(noise(t*.22f-f*1.9f,seed+41)-.5f)*f*f*h*(jet?.04f:.34f);
                float height=f*h*(1+(float)Math.sin(a*2+eddy*7)*f*(1-f)*(jet?.015f:.12f));
                float fade=(float)Math.pow(1-f, .65)*Math.min(1,.30f+f*9);
                int alpha=(int)(opacity*fade*(.8f+.2f*eddy));
                vertex(p,v,(float)Math.cos(a)*width+sx,height,(float)Math.sin(a)*width+sz,0,0,(float)Math.cos(a),.15f,(float)Math.sin(a),blend(bottom,top,Math.min(1,f*1.7f)),alpha,FULL);
            }
        }
    }
    public static void butt(PoseStack p,MultiBufferSource b,int light){butt(p,b,Brand.CHAPMAN,light);}
    public static void butt(PoseStack p,MultiBufferSource b,Brand brand,int light){
        p.pushPose();float girth=brand.cigaretteRadius()/.0058f;p.scale(girth,brand.horizontalCase()?1.35f:1,girth);
        // Separate used cork, wrinkled paper and a crushed, uneven ash end.
        cylinder(p,b,texture("cigarette/"+brand.id+"_filter"),.0045f,.019f,0xC4BBA9,light,24);
        int n=24;var paper=b.getBuffer(PropRenderType.cutout(texture("cigarette/"+brand.id+"_paper")));
        for(int j=0;j<n;j++){
            double a=j*Math.PI*2/n,aa=(j+1)*Math.PI*2/n;
            float r=.0045f*(.86f+.10f*(float)Math.sin(a*5)),rr=.0045f*(.86f+.10f*(float)Math.sin(aa*5));
            float end=.027f+.0018f*(float)Math.sin(a*3+.7),endNext=.027f+.0018f*(float)Math.sin(aa*3+.7);
            face(p,paper,new float[][]{{(float)Math.cos(a)*.0045f,.019f,(float)Math.sin(a)*.0045f},{(float)Math.cos(aa)*.0045f,.019f,(float)Math.sin(aa)*.0045f},{(float)Math.cos(aa)*rr+.0013f,endNext,(float)Math.sin(aa)*rr*.72f},{(float)Math.cos(a)*r+.0013f,end,(float)Math.sin(a)*r*.72f}},(float)Math.cos(a),0,(float)Math.sin(a),j%4==0?0xB8AA95:0xE6DECA,light);
        }
        p.pushPose();p.translate(.0013,.026,0);p.scale(.85f,1,.67f);cylinder(p,b,ASH,.0045f,.0032f,0x514E49,light,24);
        for(int j=0;j<11;j++){double a=j*2.399;float r=(j%3+1)*.001f;box(p,b,(float)Math.cos(a)*r-.00065f,.0025f+(j%3)*.0003f,(float)Math.sin(a)*r-.00065f,.0013f,.0007f,.0013f,j%3==0?0xB4AFA4:0x807A6F,light);}
        p.popPose();p.popPose();
    }
}
