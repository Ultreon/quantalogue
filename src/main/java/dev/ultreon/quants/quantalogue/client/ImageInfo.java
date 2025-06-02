package dev.ultreon.quants.quantalogue.client;

import dev.ultreon.quantum.util.NamespaceID;

/**
 * Author: MrCrayfish
 */
public record ImageInfo(NamespaceID resource, int width, int height, Runnable unregister) {
}
