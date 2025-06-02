package dev.ultreon.quants.quantalogue.client;

import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.Screen;
import dev.ultreon.quantum.text.ColorCode;
import dev.ultreon.quantum.util.NamespaceID;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Author: MrCrayfish
 */
public interface IModData {
    Type getType();

    String getModId();

    String getDisplayName();

    String getVersion();

    String getDescription();

    @Nullable
    String getItemIcon();

    @Nullable
    String getImageIcon();

    String getLicense();

    @Nullable
    String getCredits();

    @Nullable
    String getAuthors();

    @Nullable
    String getHomepage();

    @Nullable
    String getIssueTracker();

    @Nullable
    String getBanner();

    @Nullable
    String getBackground();

    @Nullable
    Update getUpdate();

    Set<String> getDependencies(); //TODO lazily

    boolean hasConfig();

    boolean isLogoSmooth();

    boolean isLibrary();

    void openConfigScreen(Screen parent);

    void drawUpdateIcon(Renderer renderer, Update update, int x, int y);

    record Update(boolean animated, String url, int texOffset, NamespaceID textures) {
    }

    enum Type {
        DEFAULT(ColorCode.RESET),
        LIBRARY(ColorCode.DARK_GRAY),
        GENERATED(ColorCode.AQUA);

        private final ColorCode style;

        Type(ColorCode style) {
            this.style = style;
        }

        public ColorCode getStyle() {
            return this.style;
        }
    }
}
