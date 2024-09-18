package com.itlesports.nightmaremode.mixin;

import net.minecraft.src.*;
import net.minecraft.src.BiomeEndDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import paulscode.sound.SoundSystem;

@Mixin(BiomeEndDecorator.class)

public abstract class BiomeEndDecoratorMixin extends BiomeDecorator{
    public BiomeEndDecoratorMixin(BiomeGenBase par1BiomeGenBase) {
        super(par1BiomeGenBase);
    }

    @Inject(method="decorate", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/src/World;spawnEntityInWorld(Lnet/minecraft/src/Entity;)Z",
            shift = At.Shift.AFTER))
    private void spawnDragon(CallbackInfo ci){
        EntityDragon var4 = new EntityDragon(this.currentWorld);
        var4.setLocationAndAngles(0.0, 64.0, 0.0, this.randomGenerator.nextFloat() * 360.0F, 0.0F);
        this.currentWorld.spawnEntityInWorld(var4);
//        System.out.println(this.getSoundSystem()); crashes
//        getting the soundsystem using the accessor causes a crash. will postpone custom soundtrack until later release
    }
}