package world.embers.client;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
public class SmokeParticle extends TextureSheetParticle {
    private float density=1;private float size;private final float phase,turn;private boolean breath;
    protected SmokeParticle(ClientLevel l,double x,double y,double z,double vx,double vy,double vz,SpriteSet sprites){super(l,x,y,z,vx,vy,vz);pickSprite(sprites);xd=vx;yd=vy;zd=vz;friction=.978f;gravity=-.009f;hasPhysics=true;lifetime=30+random.nextInt(22);quadSize=.012f+random.nextFloat()*.009f;quadSize*=1.16f;size=quadSize;rCol=.83f;gCol=.84f;bCol=.84f;alpha=0;phase=random.nextFloat()*6.28f;turn=(random.nextFloat()-.5f)*.012f;roll=random.nextFloat()*6.28f;oRoll=roll;}
    public void style(world.embers.Brand brand){float scale=brand.classic()?1.22f:brand.horizontalCase()?1.08f:1;size*=scale;quadSize*=scale;density=brand.classic()?1.40f:1.10f;}
    public void mob(world.embers.Brand brand){style(brand);size*=1.65f;quadSize=size;density*=1.12f;}
    public void machine(boolean large){size=quadSize=large?.060f+random.nextFloat()*.035f:.035f+random.nextFloat()*.025f;density=large?1.3f:1.1f;lifetime=65+random.nextInt(35);friction=.989f;gravity=-.006f;}
    @Override public void tick(){super.tick();if(level.isRainingAt(net.minecraft.core.BlockPos.containing(x,y,z)))age++;float f=Math.min(1,age/(float)lifetime);alpha=Math.min(1,(breath?.72f:.56f)*density)*Math.min(1,age/4f)*(1-f)*(1-f);quadSize=size*(1+f*4.0f);oRoll=roll;roll+=turn;xd+=Math.sin(age*.13+phase)*.00014;zd+=Math.cos(age*.11+phase)*.0001;}
    @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType>{public Particle createParticle(SimpleParticleType type,ClientLevel l,double x,double y,double z,double vx,double vy,double vz){SmokeParticle p=new SmokeParticle(l,x,y,z,vx,vy,vz,sprites);if(type==world.embers.Embers.EXHALE.get()){p.breath=true;p.size=p.quadSize=1.16f*(.035f+(float)Math.sqrt(vx*vx+vy*vy+vz*vz)*1.4f);p.lifetime=45;p.friction=.95f;}return p;}}
}
