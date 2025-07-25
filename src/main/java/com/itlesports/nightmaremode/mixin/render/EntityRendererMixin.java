package com.itlesports.nightmaremode.mixin.render;

import btw.community.nightmaremode.NightmareMode;
import com.itlesports.nightmaremode.NightmareUtils;
import com.itlesports.nightmaremode.mixin.EntityAccessor;
import net.minecraft.src.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Arrays;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin implements EntityAccessor {
    @Shadow private Minecraft mc;
    @Mutable
    @Shadow float fogColorRed;
    @Shadow float fogColorBlue;
    @Shadow float fogColorGreen;

    // MEA CODE. credit to Pot_tx
    @ModifyArgs(method = "updateCameraAndRender(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityClientPlayerMP;setAngles(FF)V", ordinal = 0))
    private void slowSmoothCameraInWeb(Args args) {
        if (((EntityAccessor)this.mc.thePlayer).getIsInWeb()) {
            args.set(0, (float) args.get(0) * 0.25F);
            args.set(1, (float) args.get(1) * 0.25F);
        }
    }

    @ModifyArgs(method = "updateCameraAndRender(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityClientPlayerMP;setAngles(FF)V", ordinal = 1))
    private void slowCameraInWeb(Args args) {
        if (((EntityAccessor)this.mc.thePlayer).getIsInWeb()) {
            args.set(0, (float) args.get(0) * 0.25F);
            args.set(1, (float) args.get(1) * 0.25F);
        }
    }
    @ModifyArg(method = "modUpdateLightmapOverworld", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/TextureUtil;uploadTexture(I[III)V"),index = 1)
    private int[] manageNightvisionColor(int[] par1ArrayOfInteger){
        if(NightmareUtils.getIsBloodMoon() && NightmareMode.bloodmoonColors) {
            return bloodmoonForcedBrightness();
        } else if (this.mc.thePlayer.isPotionActive(Potion.nightVision)) {
            if(this.mc.thePlayer.dimension == 1){
                return nightvisionEnd();
            } else {
                return nightvisionFullbright();
            }
        }
        return par1ArrayOfInteger;
    }



    @Inject(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClearColor(FFFF)V"))
    private void manageEndFogWithNightVision(float par1, CallbackInfo ci){
        if (this.mc.thePlayer.dimension == 1) {
            this.fogColorRed = 0;
            this.fogColorBlue = 0;
            this.fogColorGreen= 0;
        }
        // if anaglyph:
        // r = 0.178771923f
        // g = 0.178771923f
        // b = 0.195104557f
    }
    @Redirect(method = "updateFogColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/src/EntityLivingBase;isPotionActive(Lnet/minecraft/src/Potion;)Z"))
    private boolean noNightvisionRedFog(EntityLivingBase instance, Potion par1Potion){
        return false;
    }
    @Redirect(method = "updateLightmap", at = @At(value = "FIELD", target = "Lnet/minecraft/src/GameSettings;gammaSetting:F"))
    private float activateFullbright(GameSettings instance){
        return NightmareMode.fullBright ? 16f : instance.gammaSetting;
    }
    @Redirect(method = "modUpdateLightmapOverworld", at = @At(value = "FIELD", target = "Lnet/minecraft/src/GameSettings;gammaSetting:F"))
    private float activateFullbright0(GameSettings instance){
        return NightmareMode.fullBright ? 16f : instance.gammaSetting;
    }

    @Unique
    private static int[] nightvisionEnd(){
        int[] numbers = new int[256];
        Arrays.fill(numbers,-10197916);
        return numbers;
    }

    @Unique
    private static int[] nightvisionFullbright(){
        int[] numbers = new int[256];
        Arrays.fill(numbers,-1);
        return numbers;
    }
    @Unique
    private static int[] bloodmoonForcedBrightness(){
        int[] numbers = new int[256];
        Arrays.fill(numbers,-12829636); // slightly brighter bloodmoon
        // 255 << 24 | 60 << 16 | 60 << 8 | 60

//        Arrays.fill(numbers,-14145496);
        // 255 << 24 | 40 << 16 | 40 << 8 | 40
        return numbers;
    }
}
