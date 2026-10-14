package world.embers.client;

import world.embers.production.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;

public final class ProcessingScreen extends AbstractContainerScreen<ProcessingMenu> {
    private Button start,stop;
    public ProcessingScreen(ProcessingMenu menu,Inventory inv,Component title){super(menu,inv,title);imageWidth=280;imageHeight=223;inventoryLabelX=58;inventoryLabelY=130;}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(Component.translatable("screen.embers.machine.start"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+16,topPos+107,85,20).build());stop=addRenderableWidget(Button.builder(Component.translatable("screen.embers.machine.stop"),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1)).bounds(leftPos+181,topPos+107,83,20).build());addRenderableWidget(Button.builder(Component.translatable("screen.embers.machine.recipes"),b->minecraft.setScreen(new Recipes(this))).bounds(leftPos+210,topPos+5,54,18).build());}
    @Override protected void containerTick(){super.containerTick();start.active=!menu.machine.running&&menu.machine.result.isEmpty();stop.active=menu.machine.running;}
    private void label(GuiGraphics g,String key,int x,int y){g.drawString(font,Component.translatable(key),leftPos+x,topPos+y,0x38352F,false);}
    @Override protected void renderBg(GuiGraphics g,float dt,int mx,int my){
        g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xFF272B2B);g.fill(leftPos+3,topPos+3,leftPos+imageWidth-3,topPos+imageHeight-3,0xFFC5C2B6);g.fill(leftPos+8,topPos+24,leftPos+272,topPos+103,0xFFA4A499);
        for(var s:menu.slots){g.fill(leftPos+s.x-1,topPos+s.y-1,leftPos+s.x+17,topPos+s.y+17,0xFF454943);g.fill(leftPos+s.x,topPos+s.y,leftPos+s.x+16,topPos+s.y+16,0xFF777B70);}
        label(g,"screen.embers.machine.tobacco",16,25);label(g,"screen.embers.machine.fuel",82,25);label(g,"screen.embers.machine.paper",154,25);label(g,"screen.embers.machine.additives",16,62);
        ProcessingEntity m=menu.machine;g.drawString(font,Component.translatable("screen.embers.machine.charges",m.fuel),leftPos+112,topPos+54,0x353A32,false);
        g.fill(leftPos+111,topPos+78,leftPos+217,topPos+84,0xFF53594E);g.fill(leftPos+112,topPos+79,leftPos+112+(int)(104*m.work/(float)Math.max(1,m.total)),topPos+83,0xFFB5BD91);
        g.drawString(font,Component.translatable("screen.embers.machine.state."+m.reason),leftPos+112,topPos+91,0x30372C,false);
        if(m.running){int seconds=Math.max(0,(m.total-m.work+19)/20);g.drawString(font,String.format("%d:%02d",seconds/60,seconds%60),leftPos+177,topPos+66,0x30372C,false);}
        ItemStack preview=preview();if(!preview.isEmpty()&&m.getItem(8).isEmpty()){g.renderFakeItem(preview,leftPos+234,topPos+75);g.fill(leftPos+234,topPos+75,leftPos+250,topPos+91,0x709AA194);}
    }
    private ItemStack preview(){if(!menu.machine.result.isEmpty())return menu.machine.result;ProcessingRecipe r=menu.machine.recipe();return r==null?ItemStack.EMPTY:r.output;}
    @Override public void render(GuiGraphics g,int mx,int my,float dt){renderBackground(g);super.render(g,mx,my,dt);if(menu.machine.running)for(int i=0;i<6;i++){var s=menu.slots.get(i);g.fill(leftPos+s.x,topPos+s.y,leftPos+s.x+16,topPos+s.y+16,0x38525851);}renderTooltip(g,mx,my);if(mx>=leftPos+232&&mx<=leftPos+252&&my>=topPos+73&&my<=topPos+93&&!preview().isEmpty())g.renderTooltip(font,preview(),mx,my);}
    private static final class Recipes extends Screen {
        final ProcessingScreen parent;final List<ProcessingRecipe> recipes;int page,selected=-1;boolean detail;int left,top;
        Recipes(ProcessingScreen parent){super(Component.translatable("screen.embers.machine.recipes"));this.parent=parent;recipes=parent.menu.machine.recipes();}
        @Override protected void init(){left=(width-280)/2;top=(height-223)/2;addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->{if(detail){detail=false;rebuildWidgets();}else onClose();}).bounds(left+10,top+195,65,20).build());
            if(!detail){for(int j=0;j<16;j++){int index=page*16+j;if(index>=recipes.size())break;int x=left+12+j%4*65,y=top+31+j/4*34;addRenderableWidget(Button.builder(Component.empty(),b->{selected=index;rebuildWidgets();}).bounds(x,y,60,30).build());}
                addRenderableWidget(Button.builder(Component.translatable("screen.embers.machine.query"),b->{if(selected>=0){detail=true;rebuildWidgets();}}).bounds(left+96,top+195,106,20).build());addRenderableWidget(Button.builder(Component.literal(">"),b->{page=(page+1)%Math.max(1,(recipes.size()+15)/16);rebuildWidgets();}).bounds(left+233,top+195,34,20).build());
            }else addRenderableWidget(Button.builder(Component.translatable("screen.embers.machine.select"),b->{minecraft.gameMode.handleInventoryButtonClick(parent.menu.containerId,100+selected);onClose();}).bounds(left+156,top+195,110,20).build());
        }
        @Override public void onClose(){minecraft.setScreen(parent);}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float dt){renderBackground(g);g.fill(left,top,left+280,top+223,0xFFBDBCAF);g.drawString(font,title,left+12,top+12,0x292D27,false);super.render(g,mx,my,dt);
            if(!detail){for(int j=0;j<16;j++){int idx=page*16+j;if(idx>=recipes.size())break;int x=left+12+j%4*65,y=top+31+j/4*34;g.renderItem(recipes.get(idx).output,x+22,y+7);if(idx==selected)g.fill(x+3,y+27,x+57,y+29,0xFFE0DDC4);if(mx>=x&&mx<x+60&&my>=y&&my<y+30)g.renderTooltip(font,recipes.get(idx).output,mx,my);}if(selected>=0)g.drawCenteredString(font,recipes.get(selected).output.getHoverName(),left+140,top+174,0x292D27);
            }else{ProcessingRecipe r=recipes.get(selected);g.renderItem(r.output,left+128,top+29);g.drawCenteredString(font,r.output.getHoverName(),left+140,top+51,0x292D27);int x=left+26,y=top+89;ItemStack[] t=r.tobacco.getItems();if(t.length>0)g.renderItem(t[0],x,y);ItemStack[] p=r.paper.getItems();if(p.length>0)g.renderItem(p[0],x+40,y);for(int i=0;i<3;i++){int dx=x+98+i*38;g.fill(dx-2,y-2,dx+18,y+18,0xFF626C5E);if(i<r.extras.size()){var part=r.extras.get(i);ItemStack[] all=part.ingredient().getItems();if(all.length>0){ItemStack item=all[0].copyWithCount(part.count());g.renderItem(item,dx,y);g.renderItemDecorations(font,item,dx,y);if(mx>=dx&&mx<dx+16&&my>=y&&my<y+16)g.renderTooltip(font,item,mx,my);}}}g.drawCenteredString(font,Component.translatable("screen.embers.machine.recipe_info",r.duration/20,r.fuel,r.output.getCount()),left+140,top+137,0x292D27);g.drawCenteredString(font,Component.translatable("screen.embers.machine.empty_allowed"),left+140,top+157,0x454B40);}
        }
    }
}
