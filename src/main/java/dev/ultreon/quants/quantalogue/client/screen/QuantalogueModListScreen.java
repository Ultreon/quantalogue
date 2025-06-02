package dev.ultreon.quants.quantalogue.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.github.tommyettinger.textra.Layout;
import com.google.common.collect.ImmutableMap;
import dev.ultreon.quants.quantalogue.Constants;
import dev.ultreon.quants.quantalogue.Utils;
import dev.ultreon.quants.quantalogue.client.Branding;
import dev.ultreon.quants.quantalogue.client.ClientHelper;
import dev.ultreon.quants.quantalogue.client.IModData;
import dev.ultreon.quants.quantalogue.client.ImageInfo;
import dev.ultreon.quants.quantalogue.client.screen.widget.QuantalogueIconButton;
import dev.ultreon.quants.quantalogue.client.screen.widget.DropdownMenu;
import dev.ultreon.quants.quantalogue.client.screen.widget.QuantalogueIcon;
import dev.ultreon.quants.quantalogue.platform.ClientServices;
import dev.ultreon.quantum.CommonConstants;
import dev.ultreon.quantum.GamePlatform;
import dev.ultreon.quantum.client.QuantumClient;
import dev.ultreon.quantum.client.gui.Bounds;
import dev.ultreon.quantum.client.gui.Position;
import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.Screen;
import dev.ultreon.quantum.client.gui.widget.*;
import dev.ultreon.quantum.client.gui.widget.Button;
import dev.ultreon.quantum.client.gui.widget.Label;
import dev.ultreon.quantum.item.Item;
import dev.ultreon.quantum.item.ItemStack;
import dev.ultreon.quantum.item.Items;
import dev.ultreon.quantum.registry.Registries;
import dev.ultreon.quantum.sound.event.SoundEvents;
import dev.ultreon.quantum.text.ColorCode;
import dev.ultreon.quantum.text.MutableText;
import dev.ultreon.quantum.text.TextObject;
import dev.ultreon.quantum.util.NamespaceID;

import dev.ultreon.quantum.util.RgbColor;
import dev.ultreon.quantum.util.Suppliers;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class QuantalogueModListScreen extends Screen implements DropdownMenuHandler {
    private static final Favourites FAVOURITES = new Favourites();
    private static final Comparator<ModListEntry> SORT_ALPHABETICALLY = Comparator.comparing(o -> o.getData().getDisplayName());
    private static final Comparator<ModListEntry> SORT_ALPHABETICALLY_REVERSED = SORT_ALPHABETICALLY.reversed();
    private static final Comparator<ModListEntry> SORT_FAVOURITES_FIRST = Comparator.comparing(ModListEntry::getData, Comparator.comparing(data -> FAVOURITES.has(data.getModId()))).reversed().thenComparing(SORT_ALPHABETICALLY);
    private static final MutableObject<String> OPTION_QUERY = new MutableObject<>("");
    private static final MutableBoolean OPTION_HIDE_LIBRARIES = new MutableBoolean(true);
    private static final MutableBoolean OPTION_CONFIGS_ONLY = new MutableBoolean(false);
    private static final MutableBoolean OPTION_UPDATES_ONLY = new MutableBoolean(false);
    private static final MutableBoolean OPTION_FAVOURITES_ONLY = new MutableBoolean(false);
    private static final MutableObject<Comparator<ModListEntry>> OPTION_SORT = new MutableObject<>(SORT_ALPHABETICALLY);
    private static final NamespaceID MISSING_BANNER = Utils.resource("textures/gui/missing_banner.png");
    private static final NamespaceID MISSING_BACKGROUND = Utils.resource("textures/gui/missing_background.png");
    private static final ImageInfo MISSING_BANNER_INFO = new ImageInfo(MISSING_BANNER, 120, 120, () -> {
    });
    private static final Map<String, ImageInfo> BANNER_CACHE = new HashMap<>();
    private static final Map<String, ImageInfo> IMAGE_ICON_CACHE = new HashMap<>();
    private static final Map<String, Item> ITEM_ICON_CACHE = new HashMap<>();
    private static final Map<String, IModData> CACHED_MODS = new HashMap<>();
    private static final Pattern MOD_ID_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{1,63}$");
    private static final Supplier<Pair<Integer, Integer>> COUNTS = Suppliers.memoize(() -> {
        int[] counts = new int[2];
        CACHED_MODS.forEach((it, data) -> counts[data.isLibrary() ? 1 : 0]++);
        return Pair.of(counts[0], counts[1]);
    });
    private static final Map<String, SearchFilter> SEARCH_FILTERS = ImmutableMap.<String, SearchFilter>builder()
            .put("dependencies", new SearchFilter((query, data) -> {
                IModData target = CACHED_MODS.get(query.toLowerCase(Locale.ENGLISH));
                return target != null && target.getDependencies().contains(data.getModId());
            }))
            .put("dependents", new SearchFilter((query, data) -> data.getDependencies().contains(query.toLowerCase(Locale.ENGLISH)))).build();
    private static final Color LINE_COLOR = RgbColor.argb(0xFF707070).toGdx();
    private static final Color DARK_COLOR = RgbColor.argb(0x66000000).toGdx();
    // TODO: Quantum Voxel doesnt' support text entry formatting yet 💀
//    private static final Style SEARCH_FILTER_KEY = Style.EMPTY.withColor(ColorCode.GOLD);
//    private static final Style SEARCH_FILTER_VALUE = Style.EMPTY.withColor(ColorCode.WHITE);
    private static ImageInfo cachedBackground;
    private static boolean loaded = false;

    private final Screen parentScreen;
    private Button optionsButton;
    private TextEntry searchTextField;
    private ModList modList;
    private IModData selectedModData;
    private Button<?> modFolderButton;
    private Button<?> configButton;
    private Button<?> websiteButton;
    private Button<?> issueButton;
    private Label descriptionLabel;
    private int tooltipYOffset;
    //    private List<? extends FormattedCharSequence> activeTooltip;
    private @Nullable DropdownMenu menu;
    private Layout textLayout = new Layout();
    private MutableText activeTooltip;

    public QuantalogueModListScreen(Screen parent) {
        super(TextObject.empty());
        this.parentScreen = parent;
        if (!loaded) {
            ClientServices.PLATFORM.getAllModData().forEach(data -> CACHED_MODS.put(data.getModId(), data));
            CACHED_MODS.put("quantum", new QuantumVoxelModData()); // Override quantum
            BANNER_CACHE.put("quantum", new ImageInfo(NamespaceID.of("textures/gui/quantum_voxel.png"), 1024, 256, () -> {
            }));
            FAVOURITES.load();
            loaded = true;
        }
    }

    @Override
    public void setMenu(@Nullable DropdownMenu menu) {
        if (this.menu != null && this.menu != menu) {
            this.menu.hide();
        }
        this.menu = menu;
        if (menu != null) {
            menu.setRoot(this);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.searchTextField = new TextEntry() {
//            @Override
//            public int getInnerWidth() {
//                if(this.getValue().startsWith("@")) {
//                    return super.getInnerWidth() - 16;
//                }
//                return super.getInnerWidth();
//            }
        };
        searchTextField.setBounds(new Bounds(10, 25, 150, 20));
        //this.font, 10, 25, 150, 20, TextObject.empty())
        this.searchTextField.hint().set(TextObject.translation("quantalogue.misc.search"));
//        this.searchTextField.setFormatter(this::formatQuery);
//        this.searchTextField.setMaxLength(128);
        this.searchTextField.setValue(OPTION_QUERY.getValue());
        this.searchTextField.callback(entry -> {
            String s = entry.getValue();
            if (s.length() > 128) {
                entry.setValue(s = s.substring(0, 128));
            }
            if (!OPTION_QUERY.getValue().equals(s)) {
                OPTION_QUERY.setValue(s);
                this.updateSearchFieldSuggestion(s);
                this.modList.filterAndUpdateList();
                this.updateSelectedModList();
            }
        });
        this.add(this.searchTextField);
        this.modList = new ModList();
        this.modList.setX(10);
        this.add(this.modList);
        TextButton backBtn = TextButton.of(TextObject.translation("quantum.ui.back")).setCallback(it -> this.client.showScreen(this.parentScreen));
        backBtn.setBounds(new Bounds(10, this.modList.getY() + this.modList.getHeight() + 8, 127, 20));
        this.add(backBtn);
        this.modFolderButton = this.add(new QuantalogueIconButton(QuantalogueIcon.FOLDER, 140, this.modList.getY() + this.modList.getHeight() + 8, it -> openFile(ClientServices.PLATFORM.getModDirectory())));
        int padding = 10;
        int contentLeft = this.modList.getRight() + 12 + padding;
        int contentWidth = this.getWidth() - contentLeft - padding;
        int buttonWidth = (contentWidth - padding) / 3;
        this.configButton = this.add(new QuantalogueIconButton(QuantalogueIcon.WRENCH, contentLeft, 105, TextObject.translation("quantalogue.gui.config"), it -> {
            if (this.selectedModData != null) {
                this.selectedModData.openConfigScreen(this);
            }
        }));
        this.configButton.isVisible = false;
        this.websiteButton = this.add(new QuantalogueIconButton(QuantalogueIcon.GLOBE, contentLeft + buttonWidth + 5, 105, buttonWidth, TextObject.literal("Website"), it -> this.openLink(this.selectedModData.getHomepage())));
        this.websiteButton.isVisible = false;
        this.issueButton = this.add(new QuantalogueIconButton(QuantalogueIcon.CATALOGUE, contentLeft + buttonWidth + buttonWidth + 10, 105, buttonWidth, TextObject.literal("Submit Bug"), it -> this.openLink(this.selectedModData.getIssueTracker())));
        this.issueButton.isVisible = false;
        this.descriptionLabel = new Label();
        this.descriptionLabel.setBounds(new Bounds(contentWidth + padding * 2, 50, contentLeft - padding, 130));
        this.descriptionLabel.isVisible = false;
        //this.descriptionList.setRenderBackground(false); // TODO what appened
        this.add(this.descriptionLabel);

        DropdownMenu menu = DropdownMenu.builder(this)
                .setMinItemSize(100, 16)
                .setAlignment(DropdownMenu.Alignment.BELOW_RIGHT)
                .addMenu(TextObject.translation("quantalogue.gui.filters"), DropdownMenu.builder(this)
                        .setMinItemSize(60, 16)
                        .setAlignment(DropdownMenu.Alignment.END_TOP)
                        .addCheckbox(TextObject.translation("quantalogue.gui.filters.configs_only"), OPTION_CONFIGS_ONLY, it -> {
                            this.modList.filterAndUpdateList();
                            return false;
                        })
                        .addCheckbox(TextObject.translation("quantalogue.gui.filters.updates_only"), OPTION_UPDATES_ONLY, it -> {
                            this.modList.filterAndUpdateList();
                            return false;
                        })
                        .addCheckbox(TextObject.translation("quantalogue.gui.filters.favourites"), OPTION_FAVOURITES_ONLY, it -> {
                            this.modList.filterAndUpdateList();
                            return false;
                        }))
                .addMenu(TextObject.translation("quantalogue.gui.sort"), DropdownMenu.builder(this)
                        .setMinItemSize(60, 16)
                        .setAlignment(DropdownMenu.Alignment.END_TOP)
                        .addItem(TextObject.translation("quantalogue.gui.sort.alphabetically"), () -> {
                            OPTION_SORT.setValue(SORT_ALPHABETICALLY);
                            this.modList.revalidate();
                        })
                        .addItem(TextObject.translation("quantalogue.gui.sort.alphabetically_reverse"), () -> {
                            OPTION_SORT.setValue(SORT_ALPHABETICALLY_REVERSED);
                            this.modList.revalidate();
                        })
                        .addItem(TextObject.translation("quantalogue.gui.sort.favourites_first"), () -> {
                            OPTION_SORT.setValue(SORT_FAVOURITES_FIRST);
                            this.modList.revalidate();
                        }))
                .addCheckbox(TextObject.translation("quantalogue.gui.hide_libraries"), OPTION_HIDE_LIBRARIES, it -> {
                    this.modList.revalidate();
                    return false;
                }).build();

        this.optionsButton = this.add(new QuantalogueIconButton(QuantalogueIcon.MENU, this.modList.getRight() - 16, 6, 16, 16, btn -> menu.toggle(btn.getBounds())));

        // Filter the mod list
        this.modList.filterAndUpdateList();

        // Resizing window causes all widgets to be recreated, therefore need to update selected info
        if (this.selectedModData != null) {
            this.setSelectedModData(this.selectedModData);
            this.updateSelectedModList();
            ModListEntry entry = this.modList.getEntryFromInfo(this.selectedModData);
            if (entry != null) {
                // TODO
//                this.modList.centerScrollOn(entry);
            }
        }
        this.updateSearchFieldSuggestion(this.searchTextField.getValue());
    }

    @Override
    public boolean mousePress(int mouseX, int mouseY, int button) {
        if (menu != null && menu.isVisible) {
            return menu.mousePress(mouseX, mouseY, button);
        }
        return super.mousePress(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseRelease(int mouseX, int mouseY, int button) {
        if (menu != null && menu.isVisible) {
            return menu.mouseRelease(mouseX, mouseY, button);
        }
        return super.mouseRelease(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDrag(int mouseX, int mouseY, int deltaX, int deltaY, int pointer) {
        if (menu != null && menu.isVisible) {
            return menu.mouseDrag(mouseX, mouseY, deltaX, deltaY, pointer);
        }
        return super.mouseDrag(mouseX, mouseY, deltaX, deltaY, pointer);
    }

    @Override
    public boolean mouseWheel(int mouseX, int mouseY, double rotation) {
        if (menu != null && menu.isVisible) {
            return menu.mouseWheel(mouseX, mouseY, rotation);
        }
        return super.mouseWheel(mouseX, mouseY, rotation);
    }

    @Override
    public void resized(int width, int height) {
        this.widgets.clear();
        this.size.width = width;
        this.size.height = height;

        this.init();
    }

    private int getRight() {
        return getX() + getWidth();
    }

    private void openFile(File file) {
        if (GamePlatform.get().isMacOSX()) {
            try {
                // Prevent LWJGL3 freeze
                Runtime.getRuntime().exec(new String[]{"open", file.getAbsolutePath()});
            } catch (IOException e) {
                Constants.LOG.warn("Failed to open directory: {}", file, e);
            }
        } else {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                Constants.LOG.warn("Failed to open directory: {}", file, e);
            }
        }
    }

    private void openLink(@Nullable String url) {
        if (url != null) {
            try {
                new LinkConfirmScreen(this, new URI(url));
            } catch (URISyntaxException e) {
                CommonConstants.LOGGER.error("Malformed link: " + e);
            }
        }
    }

    @Override
    public void renderBackground(Renderer graphics) {
        super.renderBackground(graphics);
        this.drawModList(graphics);
        this.drawModInfo(graphics);
    }

    @Override
    public void renderWidget(Renderer graphics, float partialTicks) {
        this.activeTooltip = null;

        boolean inMenu = this.menu != null;
        super.renderWidget(graphics, partialTicks);

        if (OPTION_QUERY.getValue().startsWith("@")) {
            int iconX = this.searchTextField.getX() + this.searchTextField.getWidth() - 15;
            int iconY = this.searchTextField.getY() + (this.searchTextField.getHeight() - 10) / 2;
            QuantalogueIcon.FAV.render(graphics, iconX, iconY, partialTicks);

            if (this.menu == null && ClientHelper.isMouseWithin(iconX, iconY, 10, 10, mousePos.x, mousePos.y)) {
                this.setActiveTooltip(TextObject.translation("quantalogue.gui.advanced_search.info"));
            }
        }

        Optional<IModData> optional = Optional.ofNullable(CACHED_MODS.get(Constants.MOD_ID));
        optional.ifPresent(this::loadAndCacheLogo);
        ImageInfo bannerInfo = BANNER_CACHE.get(Constants.MOD_ID);
        if (bannerInfo != null) {
            graphics.setColor(Color.WHITE);
            graphics.blit(bannerInfo.resource(), 10, 9, 0, 0, 10, 10, bannerInfo.width(), bannerInfo.height(), bannerInfo.width(), bannerInfo.height());
        }

        if (this.menu != null) {
            this.menu.render(graphics, partialTicks);
        } else {
            if (ClientHelper.isMouseWithin(10, 9, 10, 10, mousePos.x, mousePos.y)) {
                this.setActiveTooltip(TextObject.translation("quantalogue.gui.info"));
                this.tooltipYOffset = 10;
            }

            if (this.optionsButton.isWithin(mousePos.x, mousePos.y)) {
                this.setActiveTooltip(TextObject.translation("quantalogue.gui.options"));
                this.tooltipYOffset = 10;
            }

            if (this.modFolderButton.isWithin(mousePos.x, mousePos.y)) {
                this.setActiveTooltip(TextObject.translation("quantalogue.gui.open_mods_folder"));
            }
        }

        if (this.activeTooltip != null) {
            MutableText activeTooltip1 = activeTooltip;
            graphics.renderTooltip(mousePos.x, mousePos.y, TextObject.nullToEmpty(activeTooltip1.getText().lines().findFirst().orElse("")), activeTooltip1.getText().lines().skip(1).collect(Collectors.joining("\n")), null);
        }
    }

    @Override
    public void onClosed() {
        FAVOURITES.save();
    }

    private void updateSelectedModList() {
        ModListEntry selectedEntry = this.modList.getEntryFromInfo(this.selectedModData);
        if (selectedEntry != null) {
//            this.modList.setSelected(selectedEntry);
        }
    }

    private void updateSearchFieldSuggestion(String value) {
        if (value.isEmpty()) {
//            this.searchTextField.setSuggestion(TextObject.translation("quantalogue.gui.search").append(TextObject.literal("...")).getString());
        } else if (value.startsWith("@")) {
            // Mark as special search
            int end = value.indexOf(":");
            if (end != -1) {
                String type = value.substring(1, end);
                Optional<String> optional = SEARCH_FILTERS.keySet().stream().filter(filter -> filter.startsWith(type.toLowerCase(Locale.ENGLISH))).min(Comparator.comparing(String::length));
                if (optional.isPresent()) {
                    int length = type.length();
//                    this.searchTextField.setSuggestion(optional.get().substring(length));
                } else {
//                    this.searchTextField.setSuggestion("");
                }
            } else {
//                this.searchTextField.setSuggestion("");
            }
        } else {
            Optional<IModData> optional = CACHED_MODS.values().stream().filter(data -> data.getDisplayName().toLowerCase(Locale.ENGLISH).startsWith(value.toLowerCase(Locale.ENGLISH))).min(Comparator.comparing(IModData::getDisplayName));
            if (optional.isPresent()) {
                int length = value.length();
                String displayName = optional.get().getDisplayName();
//                this.searchTextField.setSuggestion(displayName.substring(length));
            } else {
//                this.searchTextField.setSuggestion("");
            }
        }
    }

    /**
     * Draws everything considered left of the screen; title, search bar and mod list.
     *
     * @param renderer the current Renderer instance
     */
    private void drawModList(Renderer renderer) {
        this.modList.render(renderer, client.partialTick);
        this.searchTextField.render(renderer, client.partialTick);

        TextObject modsLabel = ClientServices.COMPONENT.createTitle().setBold(true).setColor(ColorCode.WHITE);
        TextObject countLabel = TextObject.literal("(" + CACHED_MODS.size() + ")").setColor(ColorCode.GRAY);
        TextObject title = TextObject.empty().copy().append(modsLabel).append(" ").append(countLabel);
        int titleWidth = renderer.textWidth(title);
        int titleLeft = this.modList.getX() + (this.modList.getWidth() - titleWidth) / 2;
        renderer.textLeft(title, titleLeft, 10, RgbColor.WHITE);

        int countLabelWidth = renderer.textWidth(countLabel);
        if (ClientHelper.isMouseWithin(titleLeft + titleWidth - countLabelWidth, 10, countLabelWidth, (int) this.font.lineHeight, mousePos.x, mousePos.y)) {
            Pair<Integer, Integer> counts = COUNTS.get();
            List<TextObject> lines = List.of(
                    TextObject.translation("quantalogue.gui.mod_count", counts.getLeft()),
                    TextObject.translation("quantalogue.gui.library_count", counts.getRight())
            );
            this.setActiveTooltip(lines);
            this.tooltipYOffset = 10;
        }
    }

    /**
     * Draws everything considered right of the screen; logo, mod title, description and more.
     *
     * @param graphics the current Renderer instance
     */
    private void drawModInfo(Renderer graphics) {
        int listRight = this.modList.getRight();
        graphics.line(listRight + 11, -1, listRight + 11, this.getHeight(), LINE_COLOR);
        graphics.fill(listRight + 12, 0, this.getWidth(), this.getHeight(), DARK_COLOR);
        this.descriptionLabel.render(graphics, client.partialTick);

        int contentLeft = listRight + 12 + 10;
        int contentWidth = this.size.width - contentLeft - 10;

        if (this.selectedModData != null) {
            this.drawBackground(graphics, this.size.width - contentLeft + 10, listRight + 12, 0);

            // Draw mod logo
            this.drawBanner(graphics, contentWidth, contentLeft, 10, this.size.width - (listRight + 12 + 10) - 10, 50);

            // Draw mod name
            graphics.pushMatrix();
            graphics.translate(contentLeft, 70, 0);
            graphics.scale(2.0, 2.0);
            graphics.textLeft(this.selectedModData.getDisplayName(), 0, 0, 0xFFFFFF);
            graphics.popMatrix();

            // Draw version
            TextObject modId = TextObject.literal("Mod ID: " + this.selectedModData.getModId()).setColor(ColorCode.DARK_GRAY);
            int modIdWidth = graphics.textWidth(modId);
            graphics.textLeft(modId, contentLeft + contentWidth - modIdWidth, 92, 0xFFFFFF);

            // Draw version
            this.drawStringWithLabel(graphics, "quantalogue.gui.version", this.selectedModData.getVersion().toString(), contentLeft, 92, contentWidth, mousePos.x, mousePos.y, ColorCode.GRAY, ColorCode.WHITE);

            // Draws an icon if there is an update for the mod
            IModData.Update update = this.selectedModData.getUpdate();
            if (update != null && update.url() != null && !update.url().isBlank()) {
                TextObject version = ClientServices.COMPONENT.createVersion(this.selectedModData.getVersion());
                int versionWidth = graphics.textWidth(version);
                this.selectedModData.drawUpdateIcon(graphics, update, contentLeft + versionWidth + 5, 92);
                if (ClientHelper.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, mousePos.x, mousePos.y)) {
                    TextObject message = ClientServices.COMPONENT.createFormatted("quantalogue.gui.update_available", update.url());
                    this.setActiveTooltip(message);
                }
            }

            // Draw fade from the bottom
            // TODO Add gradient
//            graphics.fillGradient(listRight + 12, this.height - 50, this.width, this.height, 0x00000000, 0x66000000);

            int labelOffset = this.getHeight() - 18;

            // Draw license
            String license = this.selectedModData.getLicense();
            if (!license.isBlank()) {
                this.drawStringWithLabel(graphics, "quantalogue.gui.licenses", license, contentLeft, labelOffset, contentWidth, mousePos.x, mousePos.y, ColorCode.GRAY, ColorCode.WHITE);
                labelOffset -= 15;
            }

            // Draw credits
            String credits = this.selectedModData.getCredits();
            if (credits != null && !credits.isBlank()) {
                this.drawStringWithLabel(graphics, ClientServices.COMPONENT.getCreditsKey(), credits, contentLeft, labelOffset, contentWidth, mousePos.x, mousePos.y, ColorCode.GRAY, ColorCode.WHITE);
                labelOffset -= 15;
            }

            // Draw authors
            String authors = this.selectedModData.getAuthors();
            if (authors != null && !authors.isBlank()) {
                this.drawStringWithLabel(graphics, "quantalogue.gui.authors", authors, contentLeft, labelOffset, contentWidth, mousePos.x, mousePos.y, ColorCode.GRAY, ColorCode.WHITE);
            }
        } else {
            TextObject message = TextObject.translation("quantalogue.gui.no_selection").setColor(ColorCode.GRAY);
            graphics.textCenter(message, contentLeft + contentWidth / 2, this.getHeight() / 2 - 5, 0xFFFFFF);
        }
    }

    /**
     * Draws a string and prepends a label. If the formed string and label is longer than the
     * specified max width, it will automatically be trimmed and allows the user to hover the
     * string with their mouse to read the full contents.
     *
     * @param graphics the current matrix stack
     * @param format   a string to prepend to the content
     * @param text     the string to render
     * @param x        the x position
     * @param y        the y position
     * @param maxWidth the maximum width the string can render
     * @param mouseX   the current mouse x position
     * @param mouseY   the current mouse u position
     */
    private void drawStringWithLabel(Renderer graphics, String format, String text, int x, int y, int maxWidth, int mouseX, int mouseY, ColorCode labelColor, ColorCode contentColor) {
        TextObject formatted = ClientServices.COMPONENT.createFormatted(format, text);
        String rawString = formatted.getText();
        String label = rawString.substring(0, rawString.indexOf(":") + 1);
        String content = rawString.substring(rawString.indexOf(":") + 1);
        if (graphics.textWidth(formatted) > maxWidth) {
            MutableText credits = TextObject.literal(label).setColor(labelColor);
            credits.append(TextObject.literal(content).setColor(contentColor));
            graphics.textLeft(credits.getText(), x, y, RgbColor.WHITE, maxWidth, true, "...");
            if (ClientHelper.isMouseWithin(x, y, maxWidth, 9, mouseX, mouseY)) // Sets the active tool tip if string is too long so users can still read it
            {
                this.setActiveTooltip(TextObject.literal(text));
            }
        } else {
            graphics.textLeft(TextObject.literal(label).setColor(labelColor).append(TextObject.literal(content).setColor(contentColor)), x, y, RgbColor.WHITE);
        }
    }

    @Override
    public boolean mouseClick(int mouseX, int mouseY, int button, int clicks) {
        if (this.menu != null) {
            if (!this.menu.mouseClick(mouseX, mouseY, button, clicks)) {
                this.setMenu(null);
            }
            return true;
        }
        if (ClientHelper.isMouseWithin(10, 9, 10, 10, mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_1) {
            this.openLink("https://www.curseforge.com/minecraft/mc-mods/Quantalogue");
            return true;
        }
        if (this.selectedModData != null) {
            int contentLeft = this.modList.getRight() + 12 + 10;
            TextObject version = ClientServices.COMPONENT.createVersion(this.selectedModData.getVersion());
            textLayout.clear();
            font.markup(version.getText(), textLayout);
            int versionWidth = (int) textLayout.getWidth();
            if (ClientHelper.isMouseWithin(contentLeft + versionWidth + 5, 92, 8, 8, mouseX, mouseY)) {
                IModData.Update update = this.selectedModData.getUpdate();
                if (update != null && update.url() != null && !update.url().isBlank()) {
                    this.openLink(update.url());
                }
            }
        }
        return super.mouseClick(mouseX, mouseY, button, clicks);
    }

    @Override
    public boolean keyPress(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_F && (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT))) {
            if (!this.searchTextField.isFocused()) {
                this.focused = this.searchTextField;
                searchTextField.onFocusGained();
                this.searchTextField.setCursorIdx(searchTextField.getValue().length());
                this.searchTextField.select(0, searchTextField.getValue().length());
            }
            return true;
        }
        return super.keyPress(keyCode);
    }

    private void setActiveTooltip(TextObject content) {
        this.activeTooltip = content.copy();
        this.tooltipYOffset = 0;
    }

    private void setActiveTooltip(List<? extends TextObject> activeTooltip) {
        if (activeTooltip.isEmpty()) {
            this.activeTooltip = TextObject.empty().copy();
        } else {
            this.activeTooltip = activeTooltip.getFirst().copy();

            for (int i = 1; i < activeTooltip.size(); i++) {
                this.activeTooltip.append(activeTooltip.get(i));
            }
        }
        this.tooltipYOffset = 0;
    }

    private void setSelectedModData(IModData data) {
        this.selectedModData = data;
        this.loadAndCacheLogo(data);
        this.reloadBackground(data);
        this.configButton.isVisible = true;
        this.websiteButton.isVisible = true;
        this.issueButton.isVisible = true;
        this.configButton.enabled = data.hasConfig();
        this.websiteButton.enabled = data.getHomepage() != null;
        this.issueButton.enabled = data.getIssueTracker() != null;
        int contentLeft = this.modList.getRight() + 12 + 10;
        int contentWidth = this.size.width - contentLeft - 10;
        int labelCount = this.getLabelCount(data);
        this.descriptionLabel.size.width = contentWidth;
        this.descriptionLabel.size.height = this.size.height - 135 - labelCount * 15 - 9;
        this.descriptionLabel.setX(contentLeft);
        // TODO
//        this.descriptionLabel.setTextFromInfo(data);
//        this.descriptionLabel.setScrollAmount(0);
    }

    private int getLabelCount(IModData selectedModData) {
        int count = 1; //1 by default since license property will always exist
        if (selectedModData.getCredits() != null && !selectedModData.getCredits().isBlank()) count++;
        if (selectedModData.getAuthors() != null && !selectedModData.getAuthors().isBlank()) count++;
        return count;
    }

    private void drawBackground(Renderer graphics, int contentWidth, int x, int y) {
        if (this.selectedModData == null)
            return;

        NamespaceID textureRef = cachedBackground != null ? cachedBackground.resource() : MISSING_BACKGROUND;
        Texture texture = QuantumClient.get().getTextureManager().getTexture(textureRef);
        graphics.blit(texture, x, y, contentWidth, 128, 0, 0, texture.getWidth(), texture.getHeight(), texture.getWidth(), texture.getHeight());
//        RenderSystem.setShaderTexture(0, texture);
//        Matrix4f matrix = graphics.pose().last().pose();
//        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
//        builder.addVertex(matrix, x, y, 0).setUv(0, 0).setColor(1.0F, 1.0F, 1.0F, 1.0F);
//        builder.addVertex(matrix, x, y + 128, 0).setUv(0, 1).setColor(0.0F, 0.0F, 0.0F, 0.0F);
//        builder.addVertex(matrix, x + contentWidth, y + 128, 0).setUv(1, 1).setColor(0.0F, 0.0F, 0.0F, 0.0F);
//        builder.addVertex(matrix, x + contentWidth, y, 0).setUv(1, 0).setColor(1.0F, 1.0F, 1.0F, 1.0F);
//        try (MeshData data = builder.buildOrThrow()) {
//            RenderType.guiTextured(textureRef).draw(data);
//        }
    }

    private void drawBanner(Renderer graphics, int contentWidth, int x, int y, int maxWidth, int maxHeight) {
        if (this.selectedModData != null) {
            ImageInfo info = this.getBanner(this.selectedModData.getModId());
            int displayWidth = info.width();
            int displayHeight = info.height();
            if (info.width() > maxWidth) {
                displayWidth = maxWidth;
                displayHeight = (displayWidth * info.height()) / info.width();
            }
            if (displayHeight > maxHeight) {
                displayHeight = maxHeight;
                displayWidth = (displayHeight * info.width()) / info.height();
            }

            x += (contentWidth - displayWidth) / 2;
            y += (maxHeight - displayHeight) / 2;

            graphics.blit(info.resource(), x, y, displayWidth, displayHeight, 0, 0, info.width(), info.height(), info.width(), info.height());
        }
    }

    private ImageInfo getBanner(String modId) {
        // Try getting the banner for the mod
        ImageInfo bannerInfo = BANNER_CACHE.get(modId);
        if (bannerInfo != null)
            return bannerInfo;

        // Try using the icon image for the banner
        ImageInfo iconInfo = IMAGE_ICON_CACHE.get(modId);
        if (iconInfo != null) {
            // Hack to make icon fill max banner height
            int expandedWidth = iconInfo.width() * 10;
            int expandedHeight = iconInfo.height() * 10;
            return new ImageInfo(iconInfo.resource(), expandedWidth, expandedHeight, iconInfo.unregister());
        }

        // Fallback and just use missing banner
        return MISSING_BANNER_INFO;
    }

    private void loadAndCacheLogo(IModData data) {
        if (BANNER_CACHE.containsKey(data.getModId()))
            return;

        // Fills an empty logo as logo may not be present
        BANNER_CACHE.put(data.getModId(), null);

        // Load the banner resource if present
        Branding.BANNER.loadResource(data).ifPresent(info -> BANNER_CACHE.put(data.getModId(), info));
    }

    private void loadAndCacheIcon(IModData data) {
        if (IMAGE_ICON_CACHE.containsKey(data.getModId()))
            return;

        // Fills an empty icon as icon may not be present
        IMAGE_ICON_CACHE.put(data.getModId(), null);

        // Load the icon branding
        Branding.ICON.loadResource(data).ifPresentOrElse(info -> IMAGE_ICON_CACHE.put(data.getModId(), info), () -> {
            // If no icon, try and use the loaded banner if a square
            ImageInfo bannerInfo = BANNER_CACHE.get(data.getModId());
            if (bannerInfo != null) {
                if (bannerInfo.width() == bannerInfo.height()) {
                    IMAGE_ICON_CACHE.put(data.getModId(), bannerInfo);
                }
            } else {
                // Otherwise temporarily load the banner, use if square, otherwise free the resource
                Branding.BANNER.loadResource(data).ifPresent(info -> {
                    if (info.width() == info.height()) {
                        IMAGE_ICON_CACHE.put(data.getModId(), info);
                        BANNER_CACHE.put(data.getModId(), info); // Saves loading later
                    } else {
                        info.unregister().run();
                    }
                });
            }
        });
    }

    private void reloadBackground(IModData data) {
        Branding.BACKGROUND.loadResource(data).ifPresentOrElse(info -> cachedBackground = info, () -> {
            if (cachedBackground != null) {
                cachedBackground.unregister().run();
                cachedBackground = null;
            }
        });
    }

    private class ModList extends SelectionList<ModListEntry> {
        private static final Predicate<IModData> SEARCH_PREDICATE = data -> {
            String query = OPTION_QUERY.getValue();
            if (query.startsWith("@")) {
                return performSearchFilter(query, data);
            }
            return data.getDisplayName()
                    .toLowerCase(Locale.ENGLISH)
                    .contains(query.toLowerCase(Locale.ENGLISH));
        };
        private static final Predicate<IModData> FILTER_PREDICATE = data -> {
            // We ignore filters when using special query
            String query = OPTION_QUERY.getValue();
            if (query.startsWith("@")) {
                return true;
            }
            if (OPTION_CONFIGS_ONLY.booleanValue() && !data.hasConfig()) {
                return false;
            }
            if (OPTION_UPDATES_ONLY.booleanValue() && data.getUpdate() == null) {
                return false;
            }
            if (OPTION_HIDE_LIBRARIES.booleanValue() && data.isLibrary()) {
                return false;
            }
            if (OPTION_FAVOURITES_ONLY.booleanValue() && !FAVOURITES.has(data.getModId())) {
                return false;
            }
            return true;
        };
        private boolean hideFavourites;
        private Color outlineColour = Color.WHITE;
        private Color backgroundColour = new Color(1, 1, 1, .5f);
        private boolean pressed;

        public ModList() {
            super();
            setY(45);
            setSize(150, QuantalogueModListScreen.this.size.height - 35 - 45);
            //this.setRenderB

            this.itemRenderer((renderer, value, y, selected, deltaTime) -> {
                value.render(renderer, 0, y, getX(), getWidth(), getItemHeight(), QuantalogueModListScreen.this.mousePos.x, QuantalogueModListScreen.this.mousePos.y, selected, deltaTime);
            });
            this.selectable(true);
            this.callback(caller -> {
                caller.mouseClick(QuantalogueModListScreen.this.mousePos.x, QuantalogueModListScreen.this.mousePos.y, Input.Buttons.LEFT, 1);
            });
        }

        public void filterAndUpdateList() {
            this.entries.clear();
            List<ModListEntry> entries = CACHED_MODS.values().stream()
                    .filter(SEARCH_PREDICATE)
                    .filter(FILTER_PREDICATE)
                    .map(info -> new ModListEntry(info, this))
                    .sorted(OPTION_SORT.getValue())
                    .collect(Collectors.toList());
            this.entries(entries);
//            this.refreshScrollAmount(); // FIXME: Unable to port this to QV
        }

        @Override
        public @Nullable Widget getWidgetAt(int x, int y) {
            if (x > this.getX() + this.getWidth())
                return null;
            return super.getWidgetAt(x, y);
        }

        @Nullable
        public ModListEntry getEntryFromInfo(IModData data) {
            Entry<ModListEntry> modListEntryEntry = this.children().stream().filter(entry -> entry.getValue().data == data).findFirst().orElse(null);
            if (modListEntryEntry == null) return null;
            return modListEntryEntry.getValue();
        }

        @Override
        public void renderWidget(@NotNull Renderer renderer, float partialTicks) {
            super.renderWidget(renderer, partialTicks);

            if (this.children().isEmpty()) {
                int left = this.getX() + this.getWidth() / 2;
                int top = (int) (this.getY() + (this.getHeight() - QuantalogueModListScreen.this.font.lineHeight) / 2);
                renderer.textCenter(TextObject.translation("quantalogue.gui.no_mods"), left, top, RgbColor.WHITE);
            }
        }

        @Override
        public void renderChild(@NotNull Renderer renderer, float deltaTime, Widget widget) {
            super.renderChild(renderer, deltaTime, widget);
            renderer.fill(this.getX(), pos.y - 2, this.getRowRight(), pos.y + getRowBottom() + 2, outlineColour);
            renderer.fill(this.getX() + 1, pos.y - 1, this.getRowRight() - 1, pos.y + getRowBottom() + 1, backgroundColour);
        }

        private int getRowBottom() {
            return getY() + getHeight();
        }

        private float getRowRight() {
            return getX() + getWidth();
        }

        @Override
        public boolean keyPress(int key) {
            if (key == GLFW.GLFW_KEY_ENTER && this.getSelected() != null) {
                QuantalogueModListScreen.this.setSelectedModData(this.getSelected().data);
                this.client.playSound(SoundEvents.BUTTON_PRESS, 1f);
                this.pressed = true;
                return true;
            }
            return super.keyPress(key);
        }

        @Override
        public void onFocusLost() {
            super.onFocusLost();

            if (pressed) {
                this.pressed = false;
                this.client.playSound(SoundEvents.BUTTON_RELEASE, 1f);
            }
        }

        @Override
        public boolean keyRelease(int key) {
            if (pressed) {
                pressed = false;
                this.client.playSound(SoundEvents.BUTTON_RELEASE, 1f);
                return true;
            }
            return super.keyPress(key);
        }

        @Override
        public boolean mouseRelease(int x, int y, int button) {
            this.hideFavourites = false;
            return false;
        }

//        @Override
//        public boolean updateScrolling(double mouseX, double mouseY, int button) {
//            boolean scrolling = super.updateScrolling(mouseX, mouseY, button);
//            this.hideFavourites = scrolling;
//            return scrolling;
//        }

        public boolean shouldHideFavourites() {
            return this.hideFavourites;
        }

        public int getRight() {
            return getX() + getWidth();
        }
    }

    private static boolean performSearchFilter(String query, IModData data) {
        if (!query.startsWith("@"))
            return false;

        int end = query.indexOf(":");
        if (end == -1)
            return false;

        String type = query.substring(1, end).toLowerCase(Locale.ENGLISH);
        if (!SEARCH_FILTERS.containsKey(type))
            return false;

        String value = query.substring(end + 1);
        return SEARCH_FILTERS.get(type).predicate().test(value, data);
    }

//    private FormattedCharSequence formatQuery(String partial, int displayPos) {
//        String query = OPTION_QUERY.getValue();
//        if (!query.startsWith("@"))
//            return FormattedCharSequence.forward(partial, Style.EMPTY);
//
//        int split = query.indexOf(":");
//        if (split == -1)
//            return FormattedCharSequence.forward(partial, SEARCH_FILTER_KEY);
//
//        if (displayPos > split)
//            return FormattedCharSequence.forward(partial, SEARCH_FILTER_VALUE);
//
//        if (displayPos + partial.length() < split)
//            return FormattedCharSequence.forward(partial, SEARCH_FILTER_KEY);
//
//        split = partial.indexOf(":");
//        if (split == -1)
//            return FormattedCharSequence.forward(partial, SEARCH_FILTER_KEY);
//
//        return FormattedCharSequence.composite(
//                FormattedCharSequence.forward(partial.substring(0, split + 1), SEARCH_FILTER_KEY),
//                FormattedCharSequence.forward(partial.substring(split + 1), SEARCH_FILTER_VALUE)
//        );

    private class ModListEntry {
        private final IModData data;
        private final ModList list;
        private final PinnedButton button;
        private ItemStack icon;

        public ModListEntry(IModData data, ModList list) {
            this.data = data;
            this.list = list;
            this.button = new PinnedButton(data.getModId());
            this.icon = new ItemStack(this.getItemIcon());
        }

        public void render(Renderer graphics, int ignoredIndex, int top, int left, int rowWidth, int rowHeight, int mouseX, int mouseY, boolean ignoredHovered, float partialTicks) {
            // Draws mod name and version
            boolean inOptionsMenu = QuantalogueModListScreen.this.menu != null;
            boolean drawFavouriteIcon = !inOptionsMenu && !this.list.shouldHideFavourites() && ClientHelper.isMouseWithin(left + rowWidth - rowHeight - 4, top, rowHeight + 4, rowHeight, mouseX, mouseY) || FAVOURITES.has(this.data.getModId());
            {
                String name = this.data.getDisplayName();
                int paddingEnd = 4;
                int trimWidth = this.list.getWidth() - 24 - paddingEnd;
                IModData.Update update = this.data.getUpdate();
                if (update != null) {
                    trimWidth -= 12;
                }
                if (drawFavouriteIcon) {
                    trimWidth -= 18;
                }
                textLayout.clear();
                MutableText title = TextObject.literal(name);
                if (this.data.isLibrary()) {
                    title.setColor(ColorCode.DARK_GRAY);
                }

                graphics.textLeft(title.getText(), left + 24, top + 2, RgbColor.WHITE, trimWidth, "...");
                graphics.textLeft(TextObject.literal(this.data.getVersion()).setColor(ColorCode.GRAY), left + 24, top + 12, 0xFFFFFF);
            }

            // Draw image icon or fallback to item icon
            this.drawIcon(graphics, top, left);

            // Draws an icon if there is an update for the mod
            IModData.Update update = this.data.getUpdate();
            if (update != null) {
                int iconLeft = left + rowWidth - 8 - 9 + (drawFavouriteIcon ? -14 : 0);
                this.data.drawUpdateIcon(graphics, update, iconLeft, top + 7);
            }

            if (drawFavouriteIcon) {
                this.button.setX(left + rowWidth - this.button.getWidth() - 8);
                this.button.setY(top + (rowHeight - this.button.getHeight()) / 2);
                this.button.render(graphics, partialTicks);
                if (!inOptionsMenu && this.button.isWithin(mouseX, mouseY)) {
                    TextObject label = !FAVOURITES.has(this.data.getModId()) ?
                            TextObject.translation("quantalogue.gui.favourite") :
                            TextObject.translation("quantalogue.gui.remove_favourite");
                    QuantalogueModListScreen.this.setActiveTooltip(label);
                }
            }
        }

        private void drawIcon(Renderer renderer, int top, int left) {
            QuantalogueModListScreen.this.loadAndCacheIcon(this.data);

            ImageInfo iconInfo = IMAGE_ICON_CACHE.get(this.data.getModId());
            if (iconInfo != null) {
                renderer.blit(iconInfo.resource(), left + 4, top + 3, 0, 0, 16, 16, iconInfo.width(), iconInfo.height(), iconInfo.width(), iconInfo.height());
                return;
            }

            try {
                client.itemRenderer.render(this.icon.getItem(), renderer, left, top + 7);
            } catch (Exception e) {
                // Attempt to catch exceptions when rendering item. Sometime level instance isn't checked for null
                Constants.LOG.debug("Failed to draw icon for mod '{}'", this.data.getModId());
                ITEM_ICON_CACHE.put(this.data.getModId(), Items.GRASS_BLOCK);
                this.icon = new ItemStack(Items.GRASS_BLOCK);
            }
        }

        private Item getItemIcon() {
            if (ITEM_ICON_CACHE.containsKey(this.data.getModId())) {
                return ITEM_ICON_CACHE.get(this.data.getModId());
            }

            // Put grass as default item icon
            ITEM_ICON_CACHE.put(this.data.getModId(), Items.GRASS_BLOCK);

            String itemIcon = this.data.getItemIcon();
            if (itemIcon != null && !itemIcon.isEmpty()) {
                NamespaceID resource = NamespaceID.tryParse(itemIcon);
                if (resource != null) {
                    Item item = Registries.ITEM.get(resource);
                    if (item != Items.AIR) {
                        ITEM_ICON_CACHE.put(this.data.getModId(), item);
                        return item;
                    }
                }
            }

            // If the mod doesn't specify an item to use, Catalogue will attempt to get an item from the mod
            Optional<Item> optional = Optional.empty();
            for (Item item1 : Registries.ITEM.values().toArray().toArray(Item.class)) {
                if (item1.getId().getDomain().equals(this.data.getModId())) {
                    optional = Optional.of(item1);
                    break;
                }
            }
            if (optional.isPresent()) {
                Item item = optional.get();
                if (item != Items.AIR) {
                    ITEM_ICON_CACHE.put(this.data.getModId(), item);
                    return item;
                }
            }

            return Items.GRASS_BLOCK;
        }

        private TextObject getFormattedModName(boolean favouriteIconVisible) {
            return title;
        }

        public boolean mouseClick(int mouseX, int mouseY, int button, int clicks) {
            if (this.button.mouseClick(mouseX, mouseY, button, clicks))
                return false;

            if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                DropdownMenu menu = DropdownMenu.builder(QuantalogueModListScreen.this)
                        .setMinItemSize(0, 16)
                        .setAlignment(DropdownMenu.Alignment.BELOW_LEFT)
                        .addItem(TextObject.translation("quantalogue.gui.show_dependencies"), () -> {
                            String filter = "@dependencies:" + this.data.getModId();
                            QuantalogueModListScreen.this.searchTextField.setValue(filter);
                        })
                        .addItem(TextObject.translation("quantalogue.gui.show_dependents"), () -> {
                            String filter = "@dependents:" + this.data.getModId();
                            QuantalogueModListScreen.this.searchTextField.setValue(filter);
                        }).build();
                menu.toggle(mouseX, mouseY);
                return false;
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                QuantalogueModListScreen.this.setSelectedModData(this.data);
//                this.list.setSelected(this); // TODO
                return true;
            }
            return false;
        }

        public IModData getData() {
            return this.data;
        }

        private class PinnedButton extends Button<PinnedButton> {
            private static final NamespaceID TEXTURE = new NamespaceID(Constants.MOD_ID, "textures/gui/icons.png");

            private final String modId;

            public PinnedButton(String modId) {
                super(10, 10);
                setPos(0, 0);
                this.modId = modId;
            }

            @Override
            public void renderWidget(Renderer graphics, float ignoredPartialTick) {
                int textureU = FAVOURITES.has(this.modId) ? 10 : 0;
                graphics.blit(TEXTURE, this.getX(), this.getY(), textureU, 10, 10, 10, 64, 64);
            }

            @Override
            public boolean click() {
                FAVOURITES.toggle(this.modId);
                ModListEntry.this.list.filterAndUpdateList();
                return true;
            }

            @Override
            public PinnedButton position(Supplier<Position> position) {
                onRevalidate(it -> setPos(position.get()));
                return this;
            }

            @Override
            public Button<PinnedButton> bounds(Supplier<Bounds> position) {
                onRevalidate(it -> setBounds(position.get()));
                return this;
            }
        }
    }
//    }

    private record SearchFilter(BiPredicate<String, IModData> predicate) {
    }

    private static class Favourites {
        private final Set<String> mods = new HashSet<>();
        private boolean needsSave;
        private Path file;

        public void toggle(String modId) {
            if (!this.mods.remove(modId)) {
                this.mods.add(modId);
            }
            this.needsSave = true;
        }

        public boolean has(String modId) {
            return this.mods.contains(modId);
        }

        private void init() {
            try {
                Path configDir = ClientServices.PLATFORM.getConfigDirectory();
                Path file = configDir.resolve("Quantalogue_favourites.txt");
                if (!Files.exists(file)) {
                    Files.createFile(file);
                }
                this.file = file;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void load() {
            try {
                this.init();
                this.mods.clear();
                Predicate<String> modIdRegex = MOD_ID_PATTERN.asMatchPredicate();
                Files.readAllLines(file).forEach(s -> {
                    if (modIdRegex.test(s) && ClientServices.PLATFORM.isModLoaded(s)) {
                        this.mods.add(s);
                    }
                });
                // Save immediately to remove invalid lines
                this.needsSave = true;
                this.save();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void save() {
            if (!this.needsSave)
                return;

            try {
                this.needsSave = false;
                this.init();
                Files.write(this.file, this.mods, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
