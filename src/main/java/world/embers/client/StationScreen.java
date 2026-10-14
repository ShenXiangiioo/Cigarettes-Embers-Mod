package world.embers.client;
import world.embers.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
public class StationScreen extends AbstractContainerScreen<StationMenu> {
    private Button refill;
    public StationScreen(StationMenu m,Inventory i,Component title){super(m,i,title);imageWidth=176;imageHeight=188;inventoryLabelY=94;titleLabelY=11;}
    @Override protected void init(){super.init();refill=addRenderableWidget(Button.builder(Component.translatable("button.embers.refill"),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);}).bounds(leftPos+48,topPos+70,80,18).build());}
    @Override protected void containerTick(){super.containerTick();refill.active=menu.canRefill();refill.setMessage(Component.translatable(menu.paid()?"button.embers.pay_refill":"button.embers.refill"));}
    @Override protected void renderBg(GuiGraphics g,float p,int mx,int my){
        int x=leftPos,y=topPos;g.fill(x-2,y-2,x+178,y+190,0xFF151B1E);g.fill(x,y,x+176,y+188,0xFF303A3B);g.fill(x+2,y+2,x+174,y+30,0xFF222B2C);g.fill(x+8,y+30,x+168,y+31,0xFFA59164);
        g.drawCenteredString(font,Component.translatable("screen.embers.refill.price",menu.fee()),x+88,y+22,0xDDDDDD);
        for(var slot:menu.slots){if(!slot.isActive())continue;int sx=x+slot.x,sy=y+slot.y;g.fill(sx-1,sy-1,sx+17,sy+17,0xFF171F20);g.fill(sx,sy,sx+16,sy+16,0xFF49514C);}
        g.drawString(font,"→",x+112,y+46,0xDCC694,false);
        g.drawCenteredString(font,Component.translatable("label.embers.lighter"),x+34,y+33,0xC8C9B6);if(menu.slots.get(1).isActive())g.drawCenteredString(font,Component.translatable(menu.paid()?"label.embers.stored_material":"label.embers.lava"),x+88,y+33,0xC8C9B6);
        g.drawCenteredString(font,Component.translatable("label.embers.refill_output"),x+142,y+33,0xC8C9B6);
        int fuel=menu.station.getItem(0).getItem() instanceof LighterItem?LighterItem.fuel(menu.station.getItem(0)):0;
        g.fill(x+24,y+63,x+46,y+65,0xFF171F20);g.fill(x+24,y+63,x+24+22*fuel/LighterItem.MAX_FUEL,y+65,0xFFCEB57B);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,titleLabelX,titleLabelY,0xE7DBBD,false);g.drawString(font,playerInventoryTitle,inventoryLabelX,inventoryLabelY,0xC9C6B3,false);}
    @Override public void render(GuiGraphics g,int x,int y,float p){renderBackground(g);super.render(g,x,y,p);renderTooltip(g,x,y);}
}
