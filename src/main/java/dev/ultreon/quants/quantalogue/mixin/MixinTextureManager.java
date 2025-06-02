package dev.ultreon.quants.quantalogue.mixin;

import com.badlogic.gdx.graphics.Texture;
import dev.ultreon.quants.quantalogue.client.TextureManagerAccess;
import dev.ultreon.quantum.client.texture.TextureManager;
import dev.ultreon.quantum.util.NamespaceID;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(TextureManager.class)
public abstract class MixinTextureManager implements TextureManagerAccess {
    @Shadow @Final private Map<NamespaceID, Texture> textures;

    @Override
    public void quantalogue$unloadTexture(NamespaceID id) {
        Texture remove = textures.remove(id);
        if (remove == null) return;

        remove.dispose();
    }
}
