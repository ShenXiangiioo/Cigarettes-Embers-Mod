package world.embers;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;

/** Vanilla collision, water currents, pickup and despawn; high ground friction for light paper. */
public final class ButtEntity extends ItemEntity {
    public ButtEntity(EntityType<? extends ItemEntity> type,Level level){super(type,level);}
    @Override public void tick(){
        super.tick();
        if(onGround()&&!isInWater()){Vec3 v=getDeltaMovement();setDeltaMovement(Math.abs(v.x)<.006?0:v.x*.48,0,Math.abs(v.z)<.006?0:v.z*.48);}
    }
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
