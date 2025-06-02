package dev.ultreon.quants.quantalogue.platform.services;

import dev.ultreon.quantum.text.MutableText;
import dev.ultreon.quantum.text.TextObject;

/**
 * Author: MrCrayfish
 */
public interface IComponentHelper {
    MutableText createTitle();

    MutableText createVersion(String version);

    MutableText createFormatted(String formatKey, String value);

    String getCreditsKey();
}
