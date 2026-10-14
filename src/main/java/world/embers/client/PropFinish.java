package world.embers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import world.embers.LighterKind;

/** Shallow machined recesses and raised inlay. All marks remain subject to scene lighting. */
final class PropFinish {
    private static void stroke(PoseStack p,MultiBufferSource b,float x,float y,float X,float Y,float z,float width,int color,int light){
        float dx=X-x,dy=Y-y,len=(float)Math.sqrt(dx*dx+dy*dy);
        p.pushPose();p.translate(x,y,z);p.mulPose(Axis.ZP.rotation((float)-Math.atan2(dx,dy)));
        Visuals.box(p,b,-width/2,0,0,width,len,.00025f,color,light);p.popPose();
    }
    private static void ring(PoseStack p,MultiBufferSource b,float x,float y,float r,float z,int petals,int color,int light){
        for(int j=0;j<96;j++){
            double a=j*Math.PI*2/96,c=(j+1)*Math.PI*2/96;
            float ra=r*(.82f+.18f*(float)Math.cos(a*petals)),rc=r*(.82f+.18f*(float)Math.cos(c*petals));
            stroke(p,b,x+(float)Math.cos(a)*ra,y+(float)Math.sin(a)*ra,x+(float)Math.cos(c)*rc,y+(float)Math.sin(c)*rc,z,.00032f,color,light);
        }
    }
    static void lighter(PoseStack p,MultiBufferSource b,LighterKind k,float w,float h,float d,int light){
        if(!k.jet&&!k.lid){
            // Everyday plastic stays unadorned: molded seam, transparent-looking fuel sight and a base plug.
            Visuals.box(p,b,-w*.33f,.008f,d/2+.0001f,.0025f,h*.55f,.00025f,0x39464A,light);
            Visuals.box(p,b,-w*.315f,.01f,d/2+.0004f,.0006f,h*.50f,.0002f,0x87928B,light);
            stroke(p,b,w*.40f,.004f,w*.40f,h-.002f,d/2,.00025f,0x77766C,light);return;
        }
        int trim=switch(k){case JET_BLACK->0x75879B;case JET_COPPER->0xE2B185;case JET_SILVER->0xD6DEE4;case OIL_BRASS->0xE0C28C;case OIL_CHROME->0x677F8C;default->0xCCAC65;};
        int shadow=k==LighterKind.OIL_CHROME?0x424B53:0x3C3329;
        for(int side:new int[]{1,-1}){
            p.pushPose();if(side<0)p.mulPose(Axis.YP.rotationDegrees(180));float z=d/2+.00015f;
            for(int e:new int[]{-1,1}){
                stroke(p,b,e*w*.41f,.006f,e*w*.41f,h-.005f,z,.0007f,shadow,light);
                stroke(p,b,e*w*.39f,.006f,e*w*.39f,h-.005f,z+.00028f,.0004f,trim,light);
            }
            switch(k){
                case JET_BLACK->{for(int j=0;j<6;j++){float yy=.013f+j*.008f;stroke(p,b,-w*.30f,yy,w*.28f,yy+.010f,z,.001f,shadow,light);stroke(p,b,-w*.28f,yy+.001f,w*.26f,yy+.011f,z+.0003f,.0003f,trim,light);}}
                case JET_COPPER->{for(int j=0;j<7;j++){float yy=.013f+j*.007f;stroke(p,b,0,yy-.006f,w*.29f,yy,z,.00045f,trim,light);stroke(p,b,w*.29f,yy,0,yy+.006f,z,.00045f,trim,light);stroke(p,b,0,yy+.006f,-w*.29f,yy,z,.00045f,trim,light);stroke(p,b,-w*.29f,yy,0,yy-.006f,z,.00045f,trim,light);}}
                case JET_SILVER->{for(int j=-4;j<=4;j++)stroke(p,b,j*w*.065f,.009f,j*w*.065f,h-.01f,z,.00045f,shadow,light);ring(p,b,0,h*.47f,w*.24f,z+.0004f,6,trim,light);}
                case OIL_BRASS->{ring(p,b,0,h*.52f,.016f,z,12,shadow,light);ring(p,b,0,h*.52f,.0138f,z+.0003f,8,trim,light);ring(p,b,0,h*.52f,.006f,z+.0005f,6,trim,light);for(int j=0;j<4;j++){float yy=.010f+j*.014f;ring(p,b,-w*.33f,yy,.002f,z,4,trim,light);ring(p,b,w*.33f,yy,.002f,z,4,trim,light);}}
                case OIL_CHROME->{ring(p,b,.003f,h*.59f,.010f,z,0,shadow,light);ring(p,b,.006f,h*.62f,.008f,z,0,trim,light);for(int j=0;j<7;j++){float yy=.009f+j*.003f;for(int x=0;x<18;x++){float a=-w*.31f+x*w*.62f/18,c=a+w*.62f/18;stroke(p,b,a,yy+(float)Math.sin(a*150+j*.4)*.0015f,c,yy+(float)Math.sin(c*150+j*.4)*.0015f,z,.00025f,shadow,light);}}}
                case OIL_BLACK->{for(int j=0;j<7;j++){float xx=(j-3)*w*.084f;stroke(p,b,0,h*.26f,xx,h*.81f,z,.0006f,shadow,light);stroke(p,b,.0003f,h*.26f,xx+.0003f,h*.81f,z+.0003f,.00028f,trim,light);}for(float yy:new float[]{.009f,.016f,h-.009f})stroke(p,b,-w*.32f,yy,w*.32f,yy,z,.0005f,trim,light);}
                default->{}
            }
            p.popPose();
        }
    }
}
