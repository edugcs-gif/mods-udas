package com.edugcs.caixademusica.mixin;

import com.edugcs.caixademusica.BoxHolder;
import com.edugcs.caixademusica.PlaybackState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Guarda a caixa embutida no baú do burro/barco na própria entidade, e salva com ela (veja {@link BoxHolder}). */
@Mixin(Entity.class)
public abstract class EntityMixin implements BoxHolder {
    @Unique
    private PlaybackState caixademusica$box;

    @Override
    public PlaybackState caixademusica$getBox() {
        return caixademusica$box;
    }

    @Override
    public void caixademusica$setBox(PlaybackState state) {
        caixademusica$box = state;
    }

    @Inject(method = "writeNbt", at = @At("HEAD"))
    private void caixademusica$writeBox(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (caixademusica$box != null) {
            NbtCompound box = new NbtCompound();
            caixademusica$box.writeNbt(box);
            nbt.put("CaixaDeMusicaBox", box);
        }
    }

    @Inject(method = "readNbt", at = @At("HEAD"))
    private void caixademusica$readBox(NbtCompound nbt, CallbackInfo ci) {
        caixademusica$box = nbt.contains("CaixaDeMusicaBox", NbtElement.COMPOUND_TYPE)
                ? PlaybackState.fromNbt(nbt.getCompound("CaixaDeMusicaBox"), PlaybackState.ENTITY_DEFAULT)
                : null;
    }
}
