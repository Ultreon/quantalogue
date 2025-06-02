package dev.ultreon.quants.quantalogue.client.screen.widget;

import com.badlogic.gdx.graphics.Texture;
import dev.ultreon.quantum.client.gui.Callback;
import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.widget.IconButton;
import dev.ultreon.quantum.text.TextObject;
import dev.ultreon.quantum.util.RgbColor;

import static dev.ultreon.quantum.client.QuantumClient.id;

/**
 * Author: MrCrayfish
 */
public class QuantalogueIconButton extends IconButton {
    private TextObject text = TextObject.empty();
    private final QuantalogueIcon icon;

    public QuantalogueIconButton(QuantalogueIcon icon, int x, int y, Callback<IconButton> callback) {
        super(icon);
        this.icon = icon;
        setSize(21, 21);
        setPos(x, y);
        setCallback(callback);
    }

    public QuantalogueIconButton(QuantalogueIcon icon, int x, int y, int width, int height, Callback<IconButton> callback) {
        super(icon);
        this.icon = icon;
        setSize(width, height);
        setPos(x, y);
        setCallback(callback);
    }

    public QuantalogueIconButton(QuantalogueIcon icon, int x, int y, TextObject text, Callback<IconButton> callback) {
        super(icon);
        this.icon = icon;
        this.text = text;
        setPos(x, y);
        setSize(100, 21);
        setCallback(callback);
    }

    public QuantalogueIconButton(QuantalogueIcon icon, int x, int y, int width, TextObject text, Callback<IconButton> callback) {
        super(icon);
        this.icon = icon;
        this.text = text;
        setPos(x, y);
        setSize(width, 21);
        setCallback(callback);
    }

    public QuantalogueIconButton(QuantalogueIcon icon, int x, int y, int width, int height, TextObject text, Callback<IconButton> callback) {
        super(icon);
        this.icon = icon;
        this.text = text;
        setPos(x, y);
        setSize(width, height);
        setCallback(callback);
    }

    @Override
    public void renderWidget(Renderer renderer, float deltaTime) {
        Texture texture = this.client.getTextureManager().getTexture(id("textures/gui/widgets.png"));

        int x = this.pos.x;
        int y = this.pos.y;

        this.renderButton(renderer, texture, x, y);

        if (this.isPressed()) y += 2;
        renderer.blitColor(RgbColor.WHITE.darker().darker());
        this.icon.render(renderer, x + 3, y - yOffset + 4, icon.width(), icon.height(), deltaTime);
        renderer.blitColor(RgbColor.WHITE);
        this.icon.render(renderer, x + 3, y - yOffset + 3, icon.width(), icon.height(), deltaTime);

        renderer.textCenter(text, 21 + ((getWidth() - 21) >> 1), (getHeight() >> 1) - font.lineHeight / 2);
    }
}
