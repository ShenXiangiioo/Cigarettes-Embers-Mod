package world.embers.client;
import world.embers.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import java.util.*;

/** Small, paginated administrator form. Every edit is confirmed by a server snapshot. */
public final class PressureScreen extends Screen {
    private final Screen parent;private final BlockPos station;private PressureSettings settings;private long price;private boolean loaded;
    private int mode,page,left,top,panelWidth;private final List<Component> labels=new ArrayList<>();private EditBox fee;private Component status=Component.empty();
    private PressureScreen(Screen parent,BlockPos station){super(Component.translatable(station==null?"screen.embers.admin.title":"screen.embers.admin.station"));this.parent=parent;this.station=station;}
    public static void open(Screen parent){Minecraft.getInstance().setScreen(new PressureScreen(parent,null));send(0,null,0,0,0,0,0);}
    private static void send(int op,BlockPos pos,int group,int index,int value,int cap,long fee){Network.CHANNEL.sendToServer(new AdminNetwork.Request(op,pos,group,index,value,cap,fee));}
    public static void receive(AdminNetwork.Snapshot snapshot){Minecraft m=Minecraft.getInstance();PressureScreen screen;
        if(m.screen instanceof PressureScreen current&&Objects.equals(current.station,snapshot.station()))screen=current;
        else if(snapshot.station()!=null){screen=new PressureScreen(null,snapshot.station());m.setScreen(screen);}else return;
        screen.loaded=true;screen.price=snapshot.fee();if(snapshot.station()==null)screen.settings=PressureSettings.load(snapshot.settings());screen.status=Component.empty();screen.rebuildWidgets();
    }
    private Button button(Component label,int x,int y,int w,Runnable run){return addRenderableWidget(Button.builder(label,b->run.run()).bounds(x,y,w,20).build());}
    private EditBox number(int x,int y,int w,String value,int max){EditBox b=new EditBox(font,x,y,w,18,Component.empty());b.setMaxLength(max);b.setFilter(v->v.isEmpty()||v.chars().allMatch(Character::isDigit));b.setValue(value);return addRenderableWidget(b);}
    @Override protected void init(){labels.clear();panelWidth=Math.min(420,width-16);left=(width-panelWidth)/2;top=Math.max(5,(height-278)/2);
        button(Component.translatable("gui.back"),left+12,top+248,88,()->{if(mode!=0){mode=page=0;rebuildWidgets();}else onClose();});
        if(!loaded)return;
        if(station!=null){fee=number(left+20,top+82,panelWidth-40,Long.toString(price),10);button(Component.translatable("gui.done"),left+20,top+115,panelWidth-40,()->{try{long value=Long.parseLong(fee.getValue());if(value>1_000_000_000L)throw new NumberFormatException();send(4,station,0,0,0,0,value);status=Component.translatable("screen.embers.admin.saving");}catch(NumberFormatException e){status=Component.translatable("screen.embers.admin.invalid_fee");}});return;}
        if(mode==0){button(Component.translatable("screen.embers.admin.increase"),left+20,top+51,panelWidth-40,()->{mode=1;page=0;rebuildWidgets();});button(Component.translatable("screen.embers.admin.decrease"),left+20,top+83,panelWidth-40,()->{mode=2;page=0;rebuildWidgets();});button(Component.translatable(settings.enabled?"screen.embers.admin.disable":"screen.embers.admin.enable"),left+20,top+115,panelWidth-40,()->send(2,null,0,0,settings.enabled?0:1,0,0));button(Component.translatable("screen.embers.admin.extras"),left+20,top+147,panelWidth-40,()->{mode=3;page=0;rebuildWidgets();});return;}
        if(mode==3){String[] features={"screen.embers.admin.effects","effect.embers.dragon_slayer","effect.embers.soaring_dragon","effect.embers.apple_lover","effect.embers.satiation","screen.embers.admin.addiction","screen.embers.admin.mob","screen.embers.admin.villager"};for(int i=page*5;i<Math.min(features.length,(page+1)*5);i++){int index=i;Component label=Component.translatable("screen.embers.admin.toggle",Component.translatable(features[i]),Component.translatable(settings.feature(i)?"options.on":"options.off"));button(label,left+20,top+48+(i-page*5)*35,panelWidth-40,()->send(5,null,0,index,settings.feature(index)?0:1,0,0));}Button prev=button(Component.literal("<"),left+panelWidth-90,top+248,34,()->{page--;rebuildWidgets();});prev.active=page>0;Button next=button(Component.literal(">"),left+panelWidth-48,top+248,34,()->{page++;rebuildWidgets();});next.active=page==0;return;}
        String[] keys=mode==1?PressureSettings.GAINS:PressureSettings.RELIEFS;int end=Math.min(keys.length,(page+1)*5);
        for(int i=page*5;i<end;i++){final int index=i;int y=top+57+(i-page*5)*35;Component label=Component.translatable("screen.embers.condition."+keys[i]);labels.add(label);int value=mode==1?settings.gain[i]:settings.relief[i],cap=mode==1?settings.caps[i]:i>=6?settings.reliefCaps[i==6?0:i==9?2:1]:0;
            EditBox amount=number(left+panelWidth-182,y,45,Integer.toString(value),3),limit=number(left+panelWidth-129,y,45,Integer.toString(cap),3);limit.visible=mode==1||i>=6;
            amount.setTooltip(Tooltip.create(Component.translatable("screen.embers.admin.amount")));limit.setTooltip(Tooltip.create(Component.translatable("screen.embers.admin.cap")));
            button(Component.translatable("gui.done"),left+panelWidth-76,y-1,64,()->{try{int a=Integer.parseInt(amount.getValue()),c=limit.visible?Integer.parseInt(limit.getValue()):0;if(a>100||c>100)throw new NumberFormatException();send(1,null,mode,index,a,c,0);status=Component.translatable("screen.embers.admin.saving");}catch(NumberFormatException e){status=Component.translatable("screen.embers.admin.invalid");}});
        }
        Button previous=button(Component.literal("<"),left+panelWidth-90,top+248,34,()->{page--;rebuildWidgets();});previous.active=page>0;
        Button next=button(Component.literal(">"),left+panelWidth-48,top+248,34,()->{page++;rebuildWidgets();});next.active=(page+1)*5<keys.length;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){renderBackground(g);g.fill(left,top,left+panelWidth,top+278,0xF01E2022);g.fill(left,top,left+panelWidth,top+28,0xFF35383B);g.drawString(font,title,left+12,top+10,0xEEEEEE,false);
        if(!loaded)g.drawString(font,Component.translatable("screen.embers.admin.loading"),left+20,top+62,0xCCCCCC,false);
        else if(station!=null)g.drawWordWrap(font,Component.translatable("screen.embers.admin.fee_help"),left+20,top+40,panelWidth-40,0xCCCCCC);
        else if(mode==0)g.drawWordWrap(font,Component.translatable("screen.embers.admin.off_help"),left+20,top+183,panelWidth-40,0xCCCCCC);
        else if(mode!=3){g.drawString(font,Component.translatable("screen.embers.admin.amount"),left+panelWidth-182,top+38,0xCCCCCC,false);g.drawString(font,Component.translatable("screen.embers.admin.cap"),left+panelWidth-129,top+38,0xCCCCCC,false);for(int i=0;i<labels.size();i++)g.drawWordWrap(font,labels.get(i),left+12,top+57+i*35,panelWidth-203,0xDDDDDD);}
        g.drawWordWrap(font,status,left+12,top+230,panelWidth-24,0xDFAB7E);super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){if(parent==null&&minecraft.player!=null)minecraft.player.closeContainer();minecraft.setScreen(parent);}
}
