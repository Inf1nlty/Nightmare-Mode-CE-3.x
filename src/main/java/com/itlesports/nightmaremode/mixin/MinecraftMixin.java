package com.itlesports.nightmaremode.mixin;

import btw.community.nightmaremode.NightmareMode;
import btw.world.util.WorldUtils;
import net.minecraft.src.*;
import net.minecraft.src.I18n;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow public GameSettings gameSettings;
    @Shadow public GuiScreen currentScreen;

    @Shadow public EntityClientPlayerMP thePlayer;
    private boolean wasZooming = false;
    private float originalFov = 0.0f;

    @Inject(method = "startGame", at = @At("TAIL"))
    private void addNightmareSpecificKeybinds(CallbackInfo ci){
        NightmareMode.getInstance().initKeybind();
    }

    @Inject(method = "screenshotListener", at = @At(value = "HEAD"))
    private void manageKeybinds(CallbackInfo ci) {
        if (Keyboard.isKeyDown(NightmareMode.nightmareZoom.keyCode) && this.currentScreen == null) {
            if (!this.wasZooming) {
                this.originalFov = this.gameSettings.fovSetting;
                this.wasZooming = true;
            }
            this.gameSettings.fovSetting = -1.2f;
        } else if (this.wasZooming) {
            this.gameSettings.fovSetting = originalFov;
            this.wasZooming = false;
        }


        if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) && Keyboard.isKeyDown(Keyboard.KEY_F4) && NightmareMode.getInstance() != null && !NightmareMode.getInstance().getCanLeaveGame()) {
            if (NightmareMode.worldState == 0) {
                ChatMessageComponent text2 = new ChatMessageComponent();
                text2.addText("<???> " + I18n.getString("nightmare.hardmode_begun"));
                text2.setColor(EnumChatFormatting.DARK_RED);
                this.thePlayer.sendChatToPlayer(text2);
                this.thePlayer.playSound("mob.wither.death",1.0f,0.905f);
                WorldUtils.gameProgressSetNetherBeenAccessedServerOnly();
                NightmareMode.worldState = 1;
            }
        }
    }
}