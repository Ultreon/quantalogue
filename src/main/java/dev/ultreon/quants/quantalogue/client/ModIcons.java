package dev.ultreon.quants.quantalogue.client;

import dev.ultreon.quants.quantalogue.Constants;
import dev.ultreon.quantum.client.gui.icon.Icon;
import dev.ultreon.quantum.util.NamespaceID;

public enum ModIcons implements Icon {
    ICON;

    @Override
    public NamespaceID id() {
        return new NamespaceID(Constants.MOD_ID, "icon.png");
    }

    @Override
    public int width() {
        return 160;
    }

    @Override
    public int height() {
        return 160;
    }

    @Override
    public int u() {
        return 0;
    }

    @Override
    public int v() {
        return 0;
    }

    @Override
    public int texWidth() {
        return 160;
    }

    @Override
    public int texHeight() {
        return 160;
    }
}
