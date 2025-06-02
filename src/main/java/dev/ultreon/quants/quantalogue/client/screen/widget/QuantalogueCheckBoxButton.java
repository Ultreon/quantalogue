package dev.ultreon.quants.quantalogue.client.screen.widget;

import dev.ultreon.quants.quantalogue.Constants;
import dev.ultreon.quantum.client.gui.Bounds;
import dev.ultreon.quantum.client.gui.Position;
import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.widget.Button;
import dev.ultreon.quantum.util.NamespaceID;

import java.util.function.Supplier;

/**
 * Author: MrCrayfish
 */
public class QuantalogueCheckBoxButton extends Button<QuantalogueCheckBoxButton> {
    private static final NamespaceID TEXTURE = new NamespaceID(Constants.MOD_ID, "textures/gui/checkbox.png");

    private final OnPress onPress;
    private boolean selected;
    public int alpha;

    public QuantalogueCheckBoxButton(int x, int y, OnPress onPress) {
        super(14, 14);
        setPos(x, y);
        this.onPress = onPress;
    }

    public boolean isSelected() {
        return this.selected;
    }

    @Override
    public boolean click() {
        this.selected = !this.selected;
        this.onPress.onPress(this);

        return true;
    }

    @Override
    public void renderWidget(Renderer graphics, float partialTicks) {
        graphics.setColor(1, 1, 1, this.alpha);
        graphics.blit(TEXTURE, this.getX(), this.getY(), 14, 14, this.isHovered | this.isFocused ? 14 : 0, this.isSelected() ? 14 : 0, 14, 14, 64, 64);
    }

    @Override
    public QuantalogueCheckBoxButton position(Supplier<Position> position) {
        onRevalidate(it -> setPos(position.get()));
        return this;
    }

    @Override
    public QuantalogueCheckBoxButton bounds(Supplier<Bounds> position) {
        onRevalidate(it -> setBounds(position.get()));
        return this;
    }

    public interface OnPress {
        void onPress(QuantalogueCheckBoxButton button);
    }
}
