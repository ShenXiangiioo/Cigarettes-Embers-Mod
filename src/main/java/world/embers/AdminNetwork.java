package world.embers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;
import world.embers.compat.CommerceBridge;

public final class AdminNetwork {
    public record Request(int operation,BlockPos station,int group,int index,int value,int cap,long fee){
        public void encode(FriendlyByteBuf b){b.writeByte(operation);b.writeBoolean(station!=null);if(station!=null)b.writeBlockPos(station);b.writeInt(group);b.writeInt(index);b.writeInt(value);b.writeInt(cap);b.writeLong(fee);}
        public static Request decode(FriendlyByteBuf b){return new Request(b.readByte(),b.readBoolean()?b.readBlockPos():null,b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readLong());}
        public void handle(Supplier<NetworkEvent.Context> ctx){ServerPlayer p=ctx.get().getSender();if(p!=null)apply(p,this);ctx.get().setPacketHandled(true);}
    }
    public static void apply(ServerPlayer p,Request r){
        if(r.station==null){
            if(!CommerceBridge.tabletMenu(p))return;PressureSettings settings=PressureSettings.get(p.server);
            if(r.operation==1&&!settings.configure(r.group,r.index,r.value,r.cap))return;
            if(r.operation==2){if(r.value!=0&&r.value!=1)return;settings.enabled=r.value==1;settings.setDirty();for(ServerPlayer player:p.server.getPlayerList().getPlayers()){ServerEvents.applyPressureSwitch(player,settings.enabled);Network.sync(player);}}
            if(r.operation==5){if(r.value!=0&&r.value!=1||!settings.feature(r.index,r.value==1))return;}
            if((r.operation<0||r.operation>2)&&r.operation!=5)return;
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(settings.save(new CompoundTag()),null,0));
        }else {
            if(!CommerceBridge.administrator(p)||!p.isAlive()||p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(r.station))>64||!p.serverLevel().hasChunkAt(r.station)||!(p.serverLevel().getBlockEntity(r.station) instanceof StationEntity be))return;
            if(r.operation==4){if(r.fee<0||r.fee>1_000_000_000L)return;be.fee=r.fee;be.setChanged();}else if(r.operation!=3)return;
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(new CompoundTag(),r.station,be.fee));
        }
    }
    public record Snapshot(CompoundTag settings,BlockPos station,long fee){
        public void encode(FriendlyByteBuf b){b.writeNbt(settings);b.writeBoolean(station!=null);if(station!=null)b.writeBlockPos(station);b.writeLong(fee);}
        public static Snapshot decode(FriendlyByteBuf b){return new Snapshot(b.readNbt(),b.readBoolean()?b.readBlockPos():null,b.readLong());}
        public void handle(Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->world.embers.client.PressureScreen.receive(this));c.get().setPacketHandled(true);}
    }
}
