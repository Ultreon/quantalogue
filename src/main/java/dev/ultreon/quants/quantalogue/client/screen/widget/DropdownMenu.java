package dev.ultreon.quants.quantalogue.client.screen.widget;

import com.badlogic.gdx.graphics.Color;
import com.github.tommyettinger.textra.Layout;
import dev.ultreon.quants.quantalogue.Constants;
import dev.ultreon.quants.quantalogue.client.screen.DropdownMenuHandler;
import dev.ultreon.quantum.client.QuantumClient;
import dev.ultreon.quantum.client.gui.Bounds;
import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.widget.UIContainer;
import dev.ultreon.quantum.client.gui.widget.Widget;
import dev.ultreon.quantum.text.TextObject;
import dev.ultreon.quantum.util.NamespaceID;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Author: MrCrayfish
 */
public class DropdownMenu extends UIContainer<DropdownMenu> {
    private static final Color DARK = new Color(0x00000050);
    private static final Color DARKER = new Color(0x000000AA);
    private final DropdownMenuHandler handler;
    private final List<Widget> items = new ArrayList<>();
    private Alignment alignment = Alignment.BELOW_LEFT;
    private @Nullable DropdownMenu parent;
    private @Nullable DropdownMenu subMenu;

    private DropdownMenu(DropdownMenuHandler handler) {
        super(0, 0);
        this.handler = handler;
        this.isVisible = false;
    }

    private void setAlignment(Alignment alignment) {
        this.alignment = alignment;
    }

    public void toggle(int mouseX, int mouseY) {
        this.toggle(new Bounds(mouseX, mouseY, 0, 0));
    }

    public void toggle(Widget widget) {
        this.toggle(widget.getBounds());
    }

    public void toggle(Bounds rect) {
        if (!this.isVisible) {
            this.show(rect);
        } else {
            this.hide();
        }
    }

    private void show(Bounds rect) {
        this.updatePosition(rect);
        this.items.forEach(child -> {
            child.isVisible = true;
        });
        this.isVisible = true;
        if (this.parent == null) {
            this.handler.setMenu(this);
        }
    }

    public void hide() {
        this.items.forEach(child -> {
            child.isVisible = false;
            if (child instanceof DropdownItem menu) {
                menu.subMenu.hide();
            }
        });
        this.subMenu = null;
        this.isVisible = false;
    }

    private void updatePosition(Bounds rect) {
        this.alignment.aligner.accept(this, rect);
    }

    public void addItem(MenuItem item) {
        this.add(item);
        this.items.add(item);
        item.isVisible = false;
    }

    private void deepClose() {
        this.handler.setMenu(null);
    }

    @Override
    public void renderWidget(Renderer graphics, float deltaTick) {
        graphics.pushMatrix();
        graphics.translate(0, 0, 50);
        QuantumClient client = QuantumClient.get();
        graphics.fill(0, 0, client.getWidth(), client.getHeight(), DARK);
        graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), DARKER);
        this.items.forEach(widget -> {
            widget.render(graphics, deltaTick);
        });
        if (this.subMenu != null) {
            this.subMenu.render(graphics, deltaTick);
        }
        graphics.popMatrix();
    }

    @Override
    public boolean mouseClick(int mouseX, int mouseY, int button, int clicks) {
        if (!this.enabled || !this.isVisible)
            return false;

        AtomicBoolean clicked = new AtomicBoolean();
        this.widgets.forEach(widget -> {
            if (widget.mouseClick(mouseX, mouseY, button, clicks)) {
                clicked.set(true);
            }
        });
        return clicked.get();
    }

    public static class MenuItem extends Widget {
        protected static final NamespaceID NORMAL = new NamespaceID(Constants.MOD_ID, "dropdown/item");
        protected static final NamespaceID HIGHLIGHTED = new NamespaceID(Constants.MOD_ID, "dropdown/item_highlighted");
        protected final DropdownMenu parent;
        private final Runnable onClick;
        protected final TextObject label;
        protected final Layout layout = new Layout();

        public MenuItem(DropdownMenu menu, TextObject label, Runnable onClick) {
            super(100, 20);
            this.setName(label.getText());
            this.label = label;
            this.parent = menu;
            this.onClick = onClick;
        }

        protected boolean selected() {
            return false;
        }

        @Override
        public void renderWidget(Renderer graphics, float deltaTick) {
            graphics.draw9Slice(this.isHovered() || this.selected() ? HIGHLIGHTED : NORMAL, this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0, 0, 12, 12, 2, 12, 12);

            int offset = (int) ((this.getHeight() - font.lineHeight) / 2 + 1);
            graphics.textLeft(this.label, this.getX() + offset, this.getY() + offset, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClick(int mouseX, int mouseY, int button, int clicks) {
            this.onClick.run();
            this.parent.deepClose();
            return true;
        }

        protected int calculateWidth() {
            layout.clear();
            font.markup(label.getText(), layout);
            int labelOffset = (int) ((this.getHeight() - font.lineHeight) / 2 + 1);
            int labelWidth = (int) layout.getWidth();
            return labelOffset + labelWidth + labelOffset;
        }
    }

    private static class CheckboxMenuItem extends MenuItem {
        private static final NamespaceID TEXTURE = new NamespaceID(Constants.MOD_ID, "textures/gui/checkbox.png");

        private final MutableBoolean holder;
        private final Function<Boolean, Boolean> callback;

        public CheckboxMenuItem(DropdownMenu menu, TextObject label, MutableBoolean holder, Function<Boolean, Boolean> callback) {
            super(menu, label, () -> {
            });
            this.holder = holder;
            this.callback = callback;
        }

        @Override
        public void renderWidget(Renderer graphics, float deltaTick) {
            super.renderWidget(graphics, deltaTick);
            int offset = (this.getHeight() - 14) / 2;
            graphics.blit(TEXTURE, this.getX() + this.getWidth() - 14 - offset, this.getY() + offset, 14, 14, isHovered || isFocused ? 14 : 0, this.holder.getValue() ? 14 : 0, 14, 14, 64, 64);
        }

        @Override
        public boolean mouseClick(int mouseX, int mouseY, int button, int clicks) {
            boolean newValue = !this.holder.getValue();
            this.holder.setValue(newValue);
            if (this.callback.apply(newValue)) {
                this.parent.deepClose();
            }

            return true;
        }

        @Override
        protected int calculateWidth() {
            int labelOffset = (int) ((this.getHeight() - font.lineHeight) / 2 + 1);
            layout.clear();
            font.markup(label.getText(), layout);
            int labelWidth = (int) layout.getWidth();
            int checkboxOffset = (this.getHeight() - 14) / 2;
            return labelOffset + labelWidth + labelOffset + 14 + checkboxOffset;
        }
    }

    private static class DropdownItem extends MenuItem {
        private final DropdownMenu subMenu;

        public DropdownItem(DropdownMenu menu, DropdownMenu subMenu, TextObject label) {
            super(menu, label, () -> {
            });
            this.subMenu = subMenu;
        }

        @Override
        public void renderWidget(Renderer graphics, float deltaTick) {
            graphics.pushMatrix();
            if (this.selected()) {
                graphics.translate(0, 0, 51);
            }
            super.renderWidget(graphics, deltaTick);
            int top = (int) (this.getY() + (this.getHeight() - font.lineHeight) / 2 + 1);
            graphics.textLeft(">", this.getX() + this.getWidth() - 10, top, 0xFFFFFFFF);
            graphics.popMatrix();
        }

        @Override
        public boolean mouseClick(int mouseX, int mouseY, int button, int clicks) {
            if (this.parent.subMenu != null) {
                this.parent.subMenu.hide();
                if (this.parent.subMenu == this.subMenu) {
                    this.parent.subMenu = null;
                    return true;
                }
            }
            this.parent.subMenu = this.subMenu;
            this.subMenu.show(this.getBounds());

            return true;
        }

        @Override
        protected boolean selected() {
            return this.parent.subMenu == this.subMenu;
        }

        @Override
        protected int calculateWidth() {
            int labelOffset = (int) ((this.getHeight() - font.lineHeight) / 2 + 1);
            layout.clear();
            font.markup(this.label.getText(), layout);
            int labelWidth = (int) layout.getWidth();
            layout.clear();
            font.markup(">", layout);
            int arrowWidth = (int) layout.getWidth();
            return labelOffset + labelWidth + labelOffset + arrowWidth + labelOffset;
        }
    }

    private interface MenuAligner {
        void accept(DropdownMenu menu, Bounds rectangle);
    }

    public enum Alignment {
        ABOVE_LEFT((menu, rectangle) -> {
            menu.setX(rectangle.getX());
            menu.setY(rectangle.getY() - menu.getHeight());
        }),
        ABOVE_RIGHT((menu, rectangle) -> {
            menu.setX(rectangle.getX() + rectangle.getWidth() - menu.getWidth());
            menu.setY(rectangle.getY() - menu.getHeight());
        }),
        BELOW_LEFT((menu, rectangle) -> {
            menu.setX(rectangle.getX() - 1);
            menu.setY(rectangle.getY() + rectangle.getHeight());
        }),
        BELOW_RIGHT((menu, rectangle) -> {
            menu.setX(rectangle.getX() + rectangle.getWidth() - menu.getWidth() + 1);
            menu.setY(rectangle.getY() + rectangle.getHeight());
        }),
        END_TOP((menu, rectangle) -> {
            menu.setX(rectangle.getX() + rectangle.getWidth());
            menu.setY(rectangle.getY() - 1);
        }),
        END_BOTTOM((menu, rectangle) -> {
            menu.setX(rectangle.getX() + rectangle.getWidth());
            menu.setY(rectangle.getY() + rectangle.getHeight() - menu.getHeight() + 1);
        });

        private final MenuAligner aligner;

        Alignment(MenuAligner positioner) {
            this.aligner = positioner;
        }

    }

    public static Builder builder(DropdownMenuHandler handler) {
        return new Builder(handler);
    }

    public static class Builder {
        private final DropdownMenuHandler handler;
        private final DropdownMenu base;
        private final List<MenuItem> items = new ArrayList<>();
        private int minItemWidth = 0;
        private int minItemHeight = 20;

        private Builder(DropdownMenuHandler handler) {
            this.handler = handler;
            this.base = new DropdownMenu(handler);
        }

        public Builder setMinItemSize(int width, int height) {
            this.minItemWidth = width;
            this.minItemHeight = height;
            return this;
        }

        public Builder setAlignment(Alignment alignment) {
            this.base.setAlignment(alignment);
            return this;
        }

        public Builder addItem(TextObject label, Runnable onClick) {
            this.items.add(new MenuItem(this.base, label, onClick));
            return this;
        }

        public Builder addCheckbox(TextObject label, MutableBoolean holder, Function<Boolean, Boolean> callback) {
            this.items.add(new CheckboxMenuItem(this.base, label, holder, callback));
            return this;
        }

        public Builder addMenu(TextObject label, Builder builder) {
            DropdownMenu menu = builder.build();
            menu.parent = this.base;
            this.items.add(new DropdownItem(this.base, menu, label));
            return this;
        }

        public DropdownMenu build() {
            int maxWidth = this.items.stream().mapToInt(MenuItem::calculateWidth).max().orElse(100);
            this.items.forEach(widget -> {
                widget.setSize(Math.max(maxWidth, this.minItemWidth), this.minItemHeight);
                this.base.addItem(widget);
            });
            return this.base;
        }
    }
}
