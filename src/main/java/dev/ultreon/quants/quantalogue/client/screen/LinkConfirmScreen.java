package dev.ultreon.quants.quantalogue.client.screen;

import dev.ultreon.quantum.CommonConstants;
import dev.ultreon.quantum.GamePlatform;
import dev.ultreon.quantum.client.gui.*;
import dev.ultreon.quantum.client.gui.widget.Label;
import dev.ultreon.quantum.client.gui.widget.TextButton;
import dev.ultreon.quantum.client.text.UITranslations;
import dev.ultreon.quantum.text.TextObject;
import dev.ultreon.quantum.util.RgbColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class LinkConfirmScreen extends Screen {
    private final @NotNull URI uri;
    private Label titleLabel;
    private Label messageLabel;
    private TextButton proceedBtn;
    private TextButton cancelBtn;

    public LinkConfirmScreen(Screen back, @NotNull URI uri) {
        super(TextObject.translation("quantalogue.screen.link_confirm.title").setColor(RgbColor.rgb(0xff4040)), back);
        this.uri = uri;
    }

    @Override
    protected void init() {
        super.init();

        titleLabel = add(Label.of(this.title)
                .alignment(Alignment.CENTER)
                .textColor(RgbColor.RED)
                .scale(2));

        messageLabel = add(Label.of(TextObject.translation("quantalogue.screen.link_confirm.message"))
                .alignment(Alignment.CENTER));

        proceedBtn = add(TextButton.of(UITranslations.PROCEED, 95)
                .setCallback(this::open));

        cancelBtn = add(TextButton.of(UITranslations.CANCEL, 95)
                .setCallback(this::onBack));
    }

    @Override
    public void resized(int width, int height) {
        super.resized(width, height);

        titleLabel.setPos(this.getWidth() / 2, this.getHeight() / 2 - 30);
        messageLabel.setPos(this.getWidth() / 2, this.getHeight() / 2);
        proceedBtn.setPos(this.getWidth() / 2 - 100, this.getHeight() / 2 + 50);
        cancelBtn.setPos(this.getWidth() / 2 + 5, this.getHeight() / 2 + 50);
    }

    private void open(TextButton caller) {
        try {
            if (GamePlatform.get().isMacOSX()) {
                Runtime.getRuntime().exec(new String[]{"open", uri.toString()});
            } else {
                Desktop.getDesktop().browse(uri);
            }
            back();
        } catch (IOException e) {
            CommonConstants.LOGGER.error("Failed to open URI: " + uri, e);
            client.notifications.add(TextObject.translation("quantum.ui.error"), TextObject.translation("quantalogue.message.failed_open_url"));
        }
        this.back();
    }

    private void onBack(TextButton caller) {
        this.back();
    }

    public @NotNull URI getUri() {
        return uri;
    }
}
