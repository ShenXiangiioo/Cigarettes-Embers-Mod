package world.embers.compat;

import world.embers.Network;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.*;
import java.util.*;
import java.util.function.Supplier;

public final class CultivationNetwork {
    public record Issue(BlockPos pos, int menuId, String name, String holder, String number, String validity, List<CompoundTag> exemptions) {
        public void encode(FriendlyByteBuf b) {
            b.writeBlockPos(pos); b.writeVarInt(menuId); b.writeUtf(name,40); b.writeUtf(holder,32); b.writeUtf(number,32); b.writeUtf(validity,12);
            b.writeVarInt(exemptions.size()); exemptions.forEach(b::writeNbt);
        }
        public static Issue decode(FriendlyByteBuf b) {
            BlockPos pos=b.readBlockPos(); int menu=b.readVarInt(); String name=b.readUtf(40), holder=b.readUtf(32), number=b.readUtf(32), validity=b.readUtf(12);
            int count=b.readVarInt(); if(count<0||count>2048) throw new IllegalArgumentException("Invalid exemption count");
            List<CompoundTag> exemptions=new ArrayList<>(); for(int i=0;i<count;i++) exemptions.add(b.readNbt());
            return new Issue(pos,menu,name,holder,number,validity,exemptions);
        }
        public void handle(Supplier<NetworkEvent.Context> context) {
            ServerPlayer player=context.get().getSender();
            if(player!=null) issue(player,this);
            context.get().setPacketHandled(true);
        }
    }
    public static boolean issue(ServerPlayer player, Issue request) {
        if(!CommerceBridge.administrator(player) || player.containerMenu.containerId!=request.menuId()
                || !player.containerMenu.getClass().getName().equals("com.codex.commerce.menu.QualificationDispenserMenu")
                || !player.containerMenu.stillValid(player) || !player.serverLevel().hasChunkAt(request.pos())) return false;
        try {
            Class<?> menu=player.containerMenu.getClass();
            if(!request.pos().equals(menu.getMethod("pos").invoke(player.containerMenu))) return false;
            Class<?> dataType=Class.forName("com.codex.commerce.data.QualificationCertificateSavedData"), exemptionType=Class.forName(dataType.getName()+"$Exemption");
            var open=menu.getMethod("open",ServerPlayer.class,BlockPos.class,int.class);
            List<Object> exemptions=new ArrayList<>();
            for(CompoundTag tag:request.exemptions()) {
                if(tag==null) return false;
                Object exemption=exemptionType.getMethod("fromTag",CompoundTag.class).invoke(null,tag);
                if(exemption==null) return false; exemptions.add(exemption);
            }
            Object data=dataType.getMethod("get",ServerLevel.class).invoke(null,player.serverLevel());
            Object certificate=dataType.getMethod("issue",ServerPlayer.class,String.class,String.class,String.class,String.class,List.class)
                    .invoke(data,player,request.name(),request.holder(),request.number(),request.validity(),exemptions);
            if(certificate==null) { player.displayClientMessage(Component.translatable("message.embers.permit_failed"),true); return false; }
            CompoundTag record=(CompoundTag)certificate.getClass().getMethod("toTag").invoke(certificate);
            CultivationPermits.get(player.serverLevel()).grant(record.getString("Id"));
            ItemStack item=new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("commerce","qualification_certificate")));
            item.getOrCreateTag().put("QualificationCertificate",record);
            if(!player.getInventory().add(item)) player.drop(item,false);
            player.displayClientMessage(Component.translatable("message.embers.permit_issued"),true);
            open.invoke(null,player,request.pos(),0);
            return true;
        } catch(ReflectiveOperationException e) {
            com.mojang.logging.LogUtils.getLogger().error("Commerce cultivation certificate integration failed",e);
            player.displayClientMessage(Component.translatable("message.embers.permit_failed"),true); return false;
        }
    }
}
