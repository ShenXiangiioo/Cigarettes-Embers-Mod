package world.embers.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public final class CultivationScreen extends Screen {
    private final Screen parent;
    private final Consumer<Boolean> apply;
    private boolean selected;
    private int left,top,panelWidth;
    public CultivationScreen(Screen parent, boolean selected, Consumer<Boolean> apply) {
        super(Component.translatable("screen.embers.cultivation.title")); this.parent=parent;this.selected=selected;this.apply=apply;
    }
    @Override protected void init() {
        panelWidth=Math.min(410,width-16); left=(width-panelWidth)/2;top=Math.max(4,(height-190)/2);
        var check=addRenderableWidget(new Checkbox(left+16,top+49,20,20,Component.empty(),selected) { @Override public void onPress(){super.onPress();CultivationScreen.this.selected=selected();} });
        addRenderableWidget(Button.builder(Component.translatable("gui.done"),b->{selected=check.selected();onClose();}).bounds(left+16,top+155,panelWidth-32,20).build());
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial) {
        renderBackground(g);g.fill(left,top,left+panelWidth,top+190,0xF0222628);
        g.drawString(font,title,left+16,top+16,0xEEEEEE,false);
        g.drawWordWrap(font,Component.translatable("screen.embers.cultivation.option"),left+44,top+51,panelWidth-60,0xEEEEEE);
        g.drawWordWrap(font,Component.translatable("screen.embers.cultivation.help"),left+16,top+91,panelWidth-32,0xBEC3C4);
        super.render(g,mx,my,partial);
    }
    @Override public boolean isPauseScreen(){return false;}
    @Override public void onClose(){apply.accept(selected);minecraft.setScreen(parent);}
}
