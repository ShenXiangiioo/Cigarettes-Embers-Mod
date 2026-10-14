package world.embers.qa;
import mezz.jei.api.*;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
@JeiPlugin
public final class JeiQa implements IModPlugin {
    static IJeiRuntime runtime;
    public ResourceLocation getPluginUid(){return new ResourceLocation("embers","qa");}
    @Override public void onRuntimeAvailable(IJeiRuntime r){runtime=r;}
    public static void show(String item){var stack=new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(item)));runtime.getRecipesGui().show(runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack));}
}
