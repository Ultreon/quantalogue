package dev.ultreon.quants.quantalogue.platform;

import dev.ultreon.quants.quantalogue.platform.services.IComponentHelper;
import dev.ultreon.quantum.text.MutableText;
import dev.ultreon.quantum.text.TextObject;

/**
 * Author: MrCrayfish
 */
public class FabricComponentHelper implements IComponentHelper {
    @Override
    public MutableText createTitle() {
        return TextObject.translation("quantalogue.gui.mod_list");
    }

    @Override
    public MutableText createVersion(String version) {
        return TextObject.translation("quantalogue.gui.version", version);
    }

    @Override
    public MutableText createFormatted(String formatKey, String value) {
        return TextObject.translation(formatKey, value);
    }

    @Override
    public String getCreditsKey() {
        return "quantalogue.gui.contributors";
    }
}
