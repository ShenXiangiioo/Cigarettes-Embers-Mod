package world.embers.compat;

import mezz.jei.api.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import world.embers.production.*;
import world.embers.client.ProcessingScreen;
import java.util.*;

/** Optional JEI entry point. No common-side class references JEI types. */
@JeiPlugin
public final class EmbersJeiPlugin implements IModPlugin {
    public static final RecipeType<ProcessingRecipe> PROCESSING=RecipeType.create("embers","processing",ProcessingRecipe.class);
    public static final RecipeType<Tobacco> DRYING=RecipeType.create("embers","drying",Tobacco.class);
    @Override public ResourceLocation getPluginUid(){return new ResourceLocation("embers","production");}
    @Override public void registerCategories(IRecipeCategoryRegistration r){var h=r.getJeiHelpers().getGuiHelper();r.addRecipeCategories(new Processor(h),new Drying(h));}
    @Override public void registerRecipes(IRecipeRegistration r){var level=Minecraft.getInstance().level;if(level!=null)r.addRecipes(PROCESSING,level.getRecipeManager().getAllRecipesFor(Production.TYPE.get()));r.addRecipes(DRYING,List.of(Tobacco.values()));}
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){r.addRecipeCatalyst(new ItemStack(Production.MACHINE_ITEM.get()),PROCESSING);r.addRecipeCatalyst(new ItemStack(Production.RACK_ITEM.get()),DRYING);}
    @Override public void registerGuiHandlers(IGuiHandlerRegistration r){r.addRecipeClickArea(ProcessingScreen.class,111,78,106,6,PROCESSING);}
    private static void text(GuiGraphics g,String key,int y,Object...args){g.drawString(Minecraft.getInstance().font,Component.translatable(key,args),4,y,0xFF353535,false);}
    private static final class Processor implements IRecipeCategory<ProcessingRecipe>{
        final IDrawable background,icon,slot;
        Processor(IGuiHelper h){background=h.createBlankDrawable(176,97);icon=h.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(Production.MACHINE_ITEM.get()));slot=h.getSlotDrawable();}
        public RecipeType<ProcessingRecipe> getRecipeType(){return PROCESSING;}
        public Component getTitle(){return Component.translatable("container.embers.processing");}
        public IDrawable getBackground(){return background;}public IDrawable getIcon(){return icon;}
        public void setRecipe(IRecipeLayoutBuilder b,ProcessingRecipe r,IFocusGroup focus){
            var ingredients=r.ingredients();
            for(int i=0;i<4;i++){var s=b.addSlot(RecipeIngredientRole.INPUT,5+i*21,19).setBackground(slot,-1,-1);if(i<ingredients.size()){var part=ingredients.get(i);s.addItemStacks(Arrays.stream(part.ingredient().getItems()).map(x->x.copyWithCount(part.count())).toList());}}
            b.addSlot(RecipeIngredientRole.INPUT,110,19).setBackground(slot,-1,-1).addIngredients(r.paper);
            b.addSlot(RecipeIngredientRole.OUTPUT,153,19).setBackground(slot,-1,-1).addItemStack(r.output);
            b.addSlot(RecipeIngredientRole.CATALYST,5,49).setBackground(slot,-1,-1).addItemStack(new ItemStack(Items.LAVA_BUCKET));b.setShapeless();
        }
        public ResourceLocation getRegistryName(ProcessingRecipe r){return r.getId();}
        public void draw(ProcessingRecipe r,IRecipeSlotsView slots,GuiGraphics g,double x,double y){text(g,"jei.embers.shared",2);g.drawString(Minecraft.getInstance().font,Component.translatable("jei.embers.lava"),28,54,0xFF353535,false);text(g,"jei.embers.processing_time",74,r.duration/20);g.drawString(Minecraft.getInstance().font,Component.translatable("screen.embers.machine.paper"),109,2,0xFF353535,false);g.drawString(Minecraft.getInstance().font,"→",135,23,0xFF353535,false);}
    }
    private static final class Drying implements IRecipeCategory<Tobacco>{
        final IDrawable background,icon,slot;
        Drying(IGuiHelper h){background=h.createBlankDrawable(176,78);icon=h.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(Production.RACK_ITEM.get()));slot=h.getSlotDrawable();}
        public RecipeType<Tobacco> getRecipeType(){return DRYING;}public Component getTitle(){return Component.translatable("block.embers.drying_rack");}
        public IDrawable getBackground(){return background;}public IDrawable getIcon(){return icon;}
        public void setRecipe(IRecipeLayoutBuilder b,Tobacco t,IFocusGroup focus){b.addSlot(RecipeIngredientRole.INPUT,36,8).setBackground(slot,-1,-1).addItemStack(new ItemStack(Production.FRESH.get(t).get()));b.addSlot(RecipeIngredientRole.OUTPUT,121,8).setBackground(slot,-1,-1).addItemStack(new ItemStack(Production.DRY.get(t).get()));}
        public void draw(Tobacco t,IRecipeSlotsView slots,GuiGraphics g,double x,double y){g.drawString(Minecraft.getInstance().font,"→",80,12,0xFF353535,false);text(g,"jei.embers.drying_time",38);text(g,"jei.embers.drying_return",55);}
    }
}
