package world.embers.production;

import com.google.gson.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import java.util.*;

public final class ProcessingRecipe implements Recipe<Container> {
    public record Part(Ingredient ingredient,int count){}
    private final ResourceLocation id;public final Ingredient tobacco,paper;public final List<Part> extras;public final ItemStack output;public final int duration,fuel;
    public ProcessingRecipe(ResourceLocation id,Ingredient tobacco,Ingredient paper,List<Part> extras,ItemStack output,int duration,int fuel){this.id=id;this.tobacco=tobacco;this.paper=paper;this.extras=List.copyOf(extras);this.output=output.copy();this.duration=duration;this.fuel=fuel;}
    public static final int[] INPUT_SLOTS={0,3,4,5};
    public List<Part> ingredients(){List<Part> all=new ArrayList<>();all.add(new Part(tobacco,1));all.addAll(extras);return all;}
    public int[] consumption(Container c){
        if(!paper.test(c.getItem(2)))return null;
        List<Part> parts=ingredients();int[] use=new int[4];
        for(int slot:INPUT_SLOTS)if(!c.getItem(slot).isEmpty()&&parts.stream().noneMatch(p->p.ingredient.test(c.getItem(slot))))return null;
        return assign(c,parts,0,use)?use:null;
    }
    private boolean assign(Container c,List<Part> parts,int index,int[] use){return index==parts.size()||assignPart(c,parts,index,0,parts.get(index).count,use);}
    private boolean assignPart(Container c,List<Part> parts,int index,int slot,int left,int[] use){
        if(left==0)return assign(c,parts,index+1,use);if(slot==4)return false;
        ItemStack item=c.getItem(INPUT_SLOTS[slot]);Part part=parts.get(index);
        int available=part.ingredient.test(item)?Math.min(left,item.getCount()-use[slot]):0;
        for(int take=available;take>=0;take--){use[slot]+=take;if(assignPart(c,parts,index,slot+1,left-take,use))return true;use[slot]-=take;}return false;
    }
    @Override public boolean matches(Container c,Level l){return consumption(c)!=null;}
    @Override public ItemStack assemble(Container c,RegistryAccess r){return output.copy();}
    @Override public boolean canCraftInDimensions(int w,int h){return true;}
    @Override public ItemStack getResultItem(RegistryAccess r){return output;}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return Production.SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return Production.TYPE.get();}
    @Override public boolean isSpecial(){return true;}
    public static final class Serializer implements RecipeSerializer<ProcessingRecipe>{
        @Override public ProcessingRecipe fromJson(ResourceLocation id,JsonObject j){List<Part> parts=new ArrayList<>();for(JsonElement e:GsonHelper.getAsJsonArray(j,"additives",new JsonArray())){JsonObject p=e.getAsJsonObject();int n=GsonHelper.getAsInt(p,"count",1);if(n<1||n>64)throw new JsonSyntaxException("additive count 1..64");parts.add(new Part(Ingredient.fromJson(p.get("ingredient")),n));}int time=GsonHelper.getAsInt(j,"duration",6000),fuel=GsonHelper.getAsInt(j,"fuel",1);ItemStack out=ShapedRecipe.itemStackFromJson(j.getAsJsonObject("result"));if(parts.size()>3||time<20||time>72000||fuel<1||fuel>4||out.isEmpty()||out.getCount()>10||out.getCount()>out.getMaxStackSize())throw new JsonSyntaxException("Invalid processing recipe");return new ProcessingRecipe(id,Ingredient.fromJson(j.get("tobacco")),j.has("paper")?Ingredient.fromJson(j.get("paper")):Ingredient.of(Items.PAPER),parts,out,time,fuel);}
        @Override public ProcessingRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){Ingredient t=Ingredient.fromNetwork(b),p=Ingredient.fromNetwork(b);int size=b.readVarInt();if(size<0||size>3)throw new IllegalArgumentException("extra size");List<Part> parts=new ArrayList<>();for(int i=0;i<size;i++)parts.add(new Part(Ingredient.fromNetwork(b),b.readVarInt()));return new ProcessingRecipe(id,t,p,parts,b.readItem(),b.readVarInt(),b.readVarInt());}
        @Override public void toNetwork(FriendlyByteBuf b,ProcessingRecipe r){r.tobacco.toNetwork(b);r.paper.toNetwork(b);b.writeVarInt(r.extras.size());for(Part p:r.extras){p.ingredient.toNetwork(b);b.writeVarInt(p.count);}b.writeItem(r.output);b.writeVarInt(r.duration);b.writeVarInt(r.fuel);}
    }
}
