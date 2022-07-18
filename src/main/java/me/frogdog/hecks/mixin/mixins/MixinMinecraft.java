package me.frogdog.hecks.mixin.mixins;

import me.frogdog.hecks.Hecks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V",ordinal = 2, shift = At.Shift.BEFORE))
    private void initHook2(CallbackInfo ci) {
        new Hecks();
    }

}
