package world.embers.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.HashMap;

/** Fine print and paper wraps need linear sampling; the standard entity type forces nearest sampling. */
final class PropRenderType extends RenderType {
    private PropRenderType(){super("embers_print",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
    private static final Map<ResourceLocation,RenderType> TYPES=new HashMap<>();
    static RenderType cutout(ResourceLocation texture){return TYPES.computeIfAbsent(texture,t->create("embers_print",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,true,false,
        CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER).setTextureState(new TextureStateShard(t,true,false)).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setCullState(NO_CULL).createCompositeState(true)));}
}
