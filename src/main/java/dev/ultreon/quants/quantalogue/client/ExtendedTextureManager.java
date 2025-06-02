package dev.ultreon.quants.quantalogue.client;

import dev.ultreon.quantum.client.texture.TextureManager;
import dev.ultreon.quantum.util.NamespaceID;

public class ExtendedTextureManager {
    public static void unloadTexture(TextureManager textureManager, NamespaceID id) {
        ((TextureManagerAccess) textureManager).quantalogue$unloadTexture(id);
    }
}
