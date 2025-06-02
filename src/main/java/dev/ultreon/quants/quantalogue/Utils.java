package dev.ultreon.quants.quantalogue;

import dev.ultreon.quantum.util.NamespaceID;

/**
 * Author: MrCrayfish
 */
public class Utils {
    public static NamespaceID resource(String name) {
        return new NamespaceID(Constants.MOD_ID, name);
    }
}
