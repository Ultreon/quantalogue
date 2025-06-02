package dev.ultreon.quants.quantalogue.client.screen.widget;

import dev.ultreon.quants.quantalogue.Constants;
import dev.ultreon.quantum.client.gui.icon.Icon;
import dev.ultreon.quantum.util.NamespaceID;
import org.jetbrains.annotations.NotNull;

public enum QuantalogueIcon implements Icon {
    FOLDER(0, 0), WRENCH(1, 0), GLOBE(2, 0), CATALOGUE(3, 0), MENU(4, 0),

    FAV_BLANK(0, 1), FAV(1, 1), INFO(2, 1);

    private final int x;
    private final int y;

    QuantalogueIcon(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static final @NotNull NamespaceID ID = new NamespaceID(Constants.MOD_ID, "textures/gui/icons.png");

    @Override
    public NamespaceID id() {
        return ID;
    }

    @Override
    public int width() {
        return 10;
    }

    @Override
    public int height() {
        return 10;
    }

    @Override
    public int u() {
        return 10 * x;
    }

    @Override
    public int v() {
        return 10 * y;
    }

    @Override
    public int texWidth() {
        return 64;
    }

    @Override
    public int texHeight() {
        return 64;
    }
}
