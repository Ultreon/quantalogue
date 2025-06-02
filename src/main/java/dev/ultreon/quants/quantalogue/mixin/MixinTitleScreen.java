package dev.ultreon.quants.quantalogue.mixin;

import com.badlogic.gdx.graphics.Texture;
import dev.ultreon.quants.quantalogue.client.TextureManagerAccess;
import dev.ultreon.quants.quantalogue.client.screen.QuantalogueModListScreen;
import dev.ultreon.quantum.client.gui.Screen;
import dev.ultreon.quantum.client.gui.screens.ModListScreen;
import dev.ultreon.quantum.client.gui.screens.TitleScreen;
import dev.ultreon.quantum.client.gui.widget.TitleButton;
import dev.ultreon.quantum.client.texture.TextureManager;
import dev.ultreon.quantum.util.NamespaceID;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {
    protected MixinTitleScreen(@Nullable String title) {
        super(title);
    }

    @Inject(method = "showModList", at = @At("HEAD"), cancellable = true)
    public void showModList(TitleButton caller, CallbackInfo ci) {
        ci.cancel();

        this.client.showScreen(new QuantalogueModListScreen(this));
    }
}
