package world.embers.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;

/** Uses the ordinary world-particle shader, never an emissive hand/entity material. */
final class SmokeRenderType extends RenderType {
    private SmokeRenderType(){super("embers_smoke",DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
    static final RenderType CLOUD=smoke(Visuals.texture("particle/smoke_atlas"));
    private static RenderType smoke(net.minecraft.resources.ResourceLocation texture){return create("embers_smoke",DefaultVertexFormat.PARTICLE,VertexFormat.Mode.QUADS,65536,false,true,
        CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getParticleShader)).setTextureState(new TextureStateShard(texture,true,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setLightmapState(LIGHTMAP).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
}
