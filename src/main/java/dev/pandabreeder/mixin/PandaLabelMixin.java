package dev.pandabreeder.mixin;

import dev.pandabreeder.PandaBreederClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Alters only the frame's render state, never entity names or synchronized metadata. */
@Mixin(EntityRenderer.class)
public abstract class PandaLabelMixin {
    @Inject(method="updateRenderState",at=@At("TAIL"))
    private void pandaBreederLabel(Entity entity,EntityRenderState state,float tickProgress,CallbackInfo ci) {
        var mod=PandaBreederClient.INSTANCE;
        if(mod==null||!mod.data.overlays||!(entity instanceof PandaEntity)||state.invisible||state.squaredDistanceToCamera>1024||MinecraftClient.getInstance().options.hudHidden)return;
        var p=mod.data.find(entity.getUuid());if(p==null)return;
        boolean best=mod.recommended(p);
        String genes=p.genotype()==null?"?":p.main.name().substring(0,2)+"/"+p.hidden.name().substring(0,2)+" "+p.genotype().brownCopies()+"B";
        var label=Text.literal((best?"★ ":"")+p.id+" "+genes).formatted(best?Formatting.GREEN:Formatting.WHITE);
        if(entity.hasCustomName()) label.append(Text.literal(" | "+entity.getName().getString()).formatted(Formatting.GRAY));
        state.displayName=label;state.nameLabelPos=new Vec3d(0,entity.getHeight()+0.5,0);
    }
}
