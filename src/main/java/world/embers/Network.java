package world.embers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.UUID;
import java.util.function.Supplier;

public final class Network {
    private static final String VERSION="12";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Embers.ID,"main"),()->VERSION,VERSION::equals,VERSION::equals);
    public static void init(){
        CHANNEL.messageBuilder(world.embers.compat.CultivationNetwork.Issue.class,6,NetworkDirection.PLAY_TO_SERVER).encoder(world.embers.compat.CultivationNetwork.Issue::encode).decoder(world.embers.compat.CultivationNetwork.Issue::decode).consumerMainThread(world.embers.compat.CultivationNetwork.Issue::handle).add();
        CHANNEL.messageBuilder(Input.class,0,NetworkDirection.PLAY_TO_SERVER).encoder(Input::encode).decoder(Input::decode).consumerMainThread(Input::handle).add();
        CHANNEL.messageBuilder(State.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(State::encode).decoder(State::decode).consumerMainThread(State::handle).add();
        CHANNEL.messageBuilder(Foley.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(Foley::encode).decoder(Foley::decode).consumerMainThread(Foley::handle).add();
        CHANNEL.messageBuilder(AdminNetwork.Request.class,3,NetworkDirection.PLAY_TO_SERVER).encoder(AdminNetwork.Request::encode).decoder(AdminNetwork.Request::decode).consumerMainThread(AdminNetwork.Request::handle).add();
        CHANNEL.messageBuilder(AdminNetwork.Snapshot.class,4,NetworkDirection.PLAY_TO_CLIENT).encoder(AdminNetwork.Snapshot::encode).decoder(AdminNetwork.Snapshot::decode).consumerMainThread(AdminNetwork.Snapshot::handle).add();
        CHANNEL.messageBuilder(MobState.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder(MobState::encode).decoder(MobState::decode).consumerMainThread(MobState::handle).add();
    }
    public record Input(int kind,boolean sneak,int target){
        void encode(FriendlyByteBuf b){b.writeVarInt(kind);b.writeBoolean(sneak);b.writeInt(target);}
        static Input decode(FriendlyByteBuf b){return new Input(b.readVarInt(),b.readBoolean(),b.readInt());}
        void handle(Supplier<NetworkEvent.Context> c){ServerPlayer p=c.get().getSender();if(p!=null&&p.isAlive()&&!p.isSpectator())ServerEvents.input(p,this);c.get().setPacketHandled(true);}
    }
    public record State(UUID player,CompoundTag data){
        void encode(FriendlyByteBuf b){b.writeUUID(player);b.writeNbt(data);}
        static State decode(FriendlyByteBuf b){return new State(b.readUUID(),b.readNbt());}
        void handle(Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->world.embers.client.ClientState.accept(player,data));c.get().setPacketHandled(true);}
    }
    public record Foley(UUID player,String sound,float volume,float pitch){
        void encode(FriendlyByteBuf b){b.writeUUID(player);b.writeUtf(sound,64);b.writeFloat(volume);b.writeFloat(pitch);}
        static Foley decode(FriendlyByteBuf b){return new Foley(b.readUUID(),b.readUtf(64),b.readFloat(),b.readFloat());}
        void handle(Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->world.embers.client.ClientFoley.accept(this));c.get().setPacketHandled(true);}
    }
    public static void foley(ServerPlayer p,String sound,float volume,float pitch){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),new Foley(p.getUUID(),sound,volume,pitch));}
    public record MobState(int entity,UUID uuid,CompoundTag data){
        void encode(FriendlyByteBuf b){b.writeInt(entity);b.writeUUID(uuid);b.writeNbt(data);}
        static MobState decode(FriendlyByteBuf b){return new MobState(b.readInt(),b.readUUID(),b.readNbt());}
        void handle(Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->world.embers.client.MobCigaretteLayer.accept(this));c.get().setPacketHandled(true);}
    }
    public static void stopFoley(ServerPlayer p){foley(p,"",0,1);}
    public static void sync(ServerPlayer p){CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),new State(p.getUUID(),ServerEvents.state(p).save()));}
    public static void sendTo(ServerPlayer receiver,ServerPlayer subject){CHANNEL.send(PacketDistributor.PLAYER.with(()->receiver),new State(subject.getUUID(),ServerEvents.state(subject).save()));}
}
