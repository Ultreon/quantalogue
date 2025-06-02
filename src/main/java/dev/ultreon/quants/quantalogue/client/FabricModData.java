package dev.ultreon.quants.quantalogue.client;

import dev.ultreon.quants.quantalogue.Quantalogue;
import dev.ultreon.quantum.client.QuantumClient;
import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.Screen;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static dev.ultreon.quants.quantalogue.client.IModData.Type.*;

/**
 * Author: MrCrayfish
 */
public class FabricModData implements IModData {
    private final ModMetadata metadata;
    private final Type type;
    private final Set<String> dependencies;
    private final String imageIcon;
    private final String imageBanner;
    private final String imageBackground;
    private final String itemIcon;

    public FabricModData(ModMetadata metadata) {
        this.metadata = metadata;
        this.type = analyzeType(metadata);
        this.dependencies = analyzeDependencies(metadata);
        String imageIcon = metadata.getIconPath(64).orElse(null);
        String imageBanner = null;
        String imageBackground = null;
        String itemIcon = null;
        CustomValue value = metadata.getCustomValue("Quantalogue");
        if (value != null && value.getType() == CustomValue.CvType.OBJECT) {
            CustomValue.CvObject catalogueObj = value.getAsObject();
            CustomValue iconValue = catalogueObj.get("icon");
            if (iconValue != null && iconValue.getType() == CustomValue.CvType.OBJECT) {
                CustomValue.CvObject iconObj = iconValue.getAsObject();
                CustomValue imageValue = iconObj.get("image");
                if (imageValue != null && imageValue.getType() == CustomValue.CvType.STRING) {
                    imageIcon = imageValue.getAsString();
                }
                CustomValue itemValue = iconObj.get("item");
                if (itemValue != null && itemValue.getType() == CustomValue.CvType.STRING) {
                    itemIcon = itemValue.getAsString();
                }
            }
            CustomValue bannerValue = catalogueObj.get("banner");
            if (bannerValue != null && bannerValue.getType() == CustomValue.CvType.STRING) {
                imageBanner = bannerValue.getAsString();
            }
            CustomValue backgroundValue = catalogueObj.get("background");
            if (backgroundValue != null && backgroundValue.getType() == CustomValue.CvType.STRING) {
                imageBackground = backgroundValue.getAsString();
            }
        }
        this.imageIcon = imageIcon;
        this.itemIcon = itemIcon;
        this.imageBanner = imageBanner;
        this.imageBackground = imageBackground;
    }

    @Override
    public Type getType() {
        return this.type;
    }

    @Override
    public String getModId() {
        return this.metadata.getId();
    }

    @Override
    public String getDisplayName() {
        return this.metadata.getName();
    }

    @Override
    public String getVersion() {
        return this.metadata.getVersion().getFriendlyString();
    }

    @Override
    public String getDescription() {
        return this.metadata.getDescription();
    }

    @Nullable
    @Override
    public String getItemIcon() {
        return this.itemIcon;
    }

    @Nullable
    @Override
    public String getImageIcon() {
        return this.imageIcon;
    }

    @Override
    public String getLicense() {
        return StringUtils.join(this.metadata.getLicense(), ", ");
    }

    @Nullable
    @Override
    public String getCredits() {
        return StringUtils.join(this.metadata.getContributors().stream().map(Person::getName).collect(Collectors.toList()), ", ");
    }

    @Nullable
    @Override
    public String getAuthors() {
        return StringUtils.join(this.metadata.getAuthors().stream().map(Person::getName).collect(Collectors.toList()), ", ");
    }

    @Nullable
    @Override
    public String getHomepage() {
        return this.metadata.getContact().get("homepage").orElse(null);
    }

    @Nullable
    @Override
    public String getIssueTracker() {
        return this.metadata.getContact().get("issues").orElse(null);
    }

    @Nullable
    @Override
    public String getBanner() {
        return this.imageBanner;
    }

    @Nullable
    @Override
    public String getBackground() {
        return this.imageBackground;
    }

    @Override
    public Update getUpdate() {
        return null;
    }

    @Override
    public Set<String> getDependencies() {
        return this.dependencies;
    }

    @Override
    public boolean hasConfig() {
        return Quantalogue.getConfigProviders().containsKey(this.metadata.getId());
    }

    @Override
    public boolean isLogoSmooth() {
        return false;
    }

    @Override
    public boolean isLibrary() {
        return this.type == LIBRARY || this.type == GENERATED;
    }

    @Override
    public void openConfigScreen(Screen parent) {
        BiFunction<Screen, ModContainer, Screen> configFactory = Quantalogue.getConfigProviders().get(this.metadata.getId());
        if (configFactory != null) {
            FabricLoader.getInstance().getModContainer(this.metadata.getId()).ifPresent(container ->
            {
                Screen configScreen = configFactory.apply(parent, container);
                if (configScreen != null) {
                    QuantumClient.get().showScreen(configScreen);
                }
            });
        }
    }

    @Override
    public void drawUpdateIcon(Renderer renderer, Update update, int x, int y) {
    }

    private static Type analyzeType(ModMetadata metadata) {
        CustomValue lifecycle = metadata.getCustomValue("fabric-api:module-lifecycle");
        if (lifecycle != null) {
            return LIBRARY;
        }

        String modId = metadata.getId();
        if (modId.startsWith("fabric-") || modId.equals("quantum") || modId.equals("java") || modId.equals("fabricloader") || modId.equals("mixinextras")) {
            return LIBRARY;
        }

        CustomValue generated = metadata.getCustomValue("fabric-loom:generated");
        if (generated != null && generated.getType() == CustomValue.CvType.BOOLEAN && generated.getAsBoolean()) {
            return GENERATED;
        }

        // Use ModMenu custom metadata to determine if a library
        CustomValue modMenuData = metadata.getCustomValue("modmenu");
        if (modMenuData != null && modMenuData.getType() == CustomValue.CvType.OBJECT) {
            CustomValue.CvObject object = modMenuData.getAsObject();
            CustomValue badges = object.get("badges");
            if (badges != null && badges.getType() == CustomValue.CvType.ARRAY) {
                CustomValue.CvArray array = badges.getAsArray();
                for (CustomValue badge : array) {
                    if (badge.getType() != CustomValue.CvType.STRING)
                        continue;

                    String type = badge.getAsString();
                    if (type.equals("library")) {
                        return LIBRARY;
                    }
                }
            }
        }

        return DEFAULT;
    }

    private static Set<String> analyzeDependencies(ModMetadata source) {
        return source.getDependencies().stream().filter(dependency -> {
            if (dependency.getKind() != ModDependency.Kind.DEPENDS) {
                return false;
            }
            String modId = dependency.getModId();
            return !modId.equals("fabricloader") && !modId.equals("fabric") && !modId.equals("quantum") && !modId.equals("java");
        }).flatMap(dependency -> {
            return FabricLoader.getInstance().getModContainer(dependency.getModId()).stream();
        }).map(container -> {
            ModMetadata metadata = container.getMetadata();
            return metadata.getId();
        }).collect(Collectors.toUnmodifiableSet());
    }
}
