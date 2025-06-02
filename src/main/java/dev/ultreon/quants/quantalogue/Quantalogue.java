package dev.ultreon.quants.quantalogue;

import dev.ultreon.quants.quantalogue.client.Config;
import dev.ultreon.quants.quantalogue.client.screen.QuantalogueModListScreen;
import dev.ultreon.quants.quantalogue.client.screen.widget.QuantalogueIconButton;
import dev.ultreon.quants.quantalogue.client.screen.widget.QuantalogueIcon;
import dev.ultreon.quantum.client.QuantumClient;
import dev.ultreon.quantum.client.api.events.gui.ScreenEvents;
import dev.ultreon.quantum.client.gui.Screen;
import dev.ultreon.quantum.client.gui.screens.TitleScreen;
import dev.ultreon.quantum.client.gui.widget.Button;
import dev.ultreon.quantum.client.gui.widget.TitleButton;
import dev.ultreon.quantum.client.gui.widget.Widget;
import dev.ultreon.quantum.events.api.ValueEventResult;
import dev.ultreon.quantum.text.TranslationText;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

public class Quantalogue implements ClientModInitializer {
    private static Map<String, BiFunction<Screen, ModContainer, Screen>> providers;

    @Override
    public void onInitializeClient() {
        Config.load(FabricLoaderImpl.INSTANCE.getConfigDir());

        Quantalogue.providers = this.findConfigFactoryProviders();

        ScreenEvents.OPEN.subscribe(open ->
        {
            QuantumClient client = QuantumClient.get();
            if (Config.isTitleMenuVisible() && open instanceof TitleScreen) {
                Widget widget = this.findTitleTarget(open);
                Button<?> modButton = new QuantalogueIconButton(QuantalogueIcon.CATALOGUE, 21, 21, it -> client.showScreen(new QuantalogueModListScreen(open)));
//                modButton.setTooltip(Tooltip.create(TextObject.translation("Quantalogue.gui.mod_list")));
                open.add(modButton);
            }
            /*

                Widget widget = this.findPauseTarget(open);
                int x = widget != null ? widget.getX() : open.size.width / 2 - 124;
                int y = widget != null ? widget.getY() : open.size.height / 4 + 32 + 48;
                if (widget != null) x += Config.getPauseMenuAlign() == Config.Align.LEFT ? -24 : widget.getWidth() + 24;
                IconButton modButton = new CatalogueIconButton(QuantalogueIcon.CATALOGUE, x, y, it -> client.showScreen(new CatalogueModListScreen(open)));
//                modButton.setTooltip(Tooltip.create(TextObject.translation("Quantalogue.gui.mod_list")));
                open.add(modButton);
             */

            return ValueEventResult.pass();
        });
    }

    public static Map<String, BiFunction<Screen, ModContainer, Screen>> getConfigProviders() {
        return providers;
    }

    private Map<String, BiFunction<Screen, ModContainer, Screen>> findConfigFactoryProviders() {
        Map<String, BiFunction<Screen, ModContainer, Screen>> factories = new HashMap<>();
        Map<String, BiFunction<Screen, ModContainer, Screen>> providers = new HashMap<>();
        FabricLoader.getInstance().getAllMods().forEach(container -> {
            this.getConfigFactoryClass(container).ifPresent(className -> {
                Optional.ofNullable(createConfigFactoryProvider(className)).ifPresent(map -> {
                    map.forEach(providers::putIfAbsent); // Only adds provider if not provided already
                });
                this.createConfigFactory(className).ifPresent(function -> {
                    factories.put(container.getMetadata().getId(), function);
                });
            });
        });
        providers.putAll(factories);
        return Collections.unmodifiableMap(providers);
    }

    private Optional<String> getConfigFactoryClass(ModContainer container) {
        ModMetadata metadata = container.getMetadata();
        CustomValue value = metadata.getCustomValue("Quantalogue");
        if (value == null || value.getType() != CustomValue.CvType.OBJECT)
            return Optional.empty();

        CustomValue.CvObject catalogueObj = value.getAsObject();
        CustomValue configFactoryValue = catalogueObj.get("configFactory");
        if (configFactoryValue == null || configFactoryValue.getType() != CustomValue.CvType.STRING)
            return Optional.empty();

        return Optional.of(configFactoryValue.getAsString());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, BiFunction<Screen, ModContainer, Screen>> createConfigFactoryProvider(String className) {
        try {
            Class<?> configFactoryClass = Class.forName(className);
            Method createConfigProviderMethod = configFactoryClass.getDeclaredMethod("createConfigProvider");
            int mods = createConfigProviderMethod.getModifiers();
            if (!Modifier.isPublic(mods)) {
                Constants.LOG.error("createConfigProvider does not have public visibility in config provider: {}", className);
                return null;
            }
            if (!Modifier.isStatic(mods)) {
                Constants.LOG.error("createConfigProvider does not have static modifier in config provider: {}", className);
                return null;
            }
            if (createConfigProviderMethod.getReturnType() != Map.class) {
                Constants.LOG.error("createConfigProvider must return a Map<String, BiFunction<Screen, ModContainer, Screen>> in config provider: {}", className);
                return null;
            }
            return (Map<String, BiFunction<Screen, ModContainer, Screen>>) createConfigProviderMethod.invoke(null);
        } catch (ClassNotFoundException e) {
            Constants.LOG.error("Unable to locate config provider: " + className, e);
        } catch (InvocationTargetException | IllegalAccessException e) {
            Constants.LOG.error("Failed to load config provider: " + className, e);
        } catch (NoSuchMethodException e) {
            // Method is optional
        }
        return null;
    }

    private Optional<BiFunction<Screen, ModContainer, Screen>> createConfigFactory(String className) {
        try {
            Class<?> configFactoryClass = Class.forName(className);
            Method createConfigScreenMethod = configFactoryClass.getDeclaredMethod("createConfigScreen", Screen.class, ModContainer.class);
            int mods = createConfigScreenMethod.getModifiers();
            if (!Modifier.isPublic(mods)) {
                Constants.LOG.error("createConfigScreen does not have public visibility in config provider: {}", className);
                return Optional.empty();
            }
            if (!Modifier.isStatic(mods)) {
                Constants.LOG.error("createConfigScreen does not have static modifier in config provider: {}", className);
                return Optional.empty();
            }
            return Optional.of((currentScreen, container) ->
            {
                try {
                    return (Screen) createConfigScreenMethod.invoke(null, currentScreen, container);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to create config screen from provider: " + className, e);
                }
            });
        } catch (ClassNotFoundException e) {
            Constants.LOG.error("Unable to locate config provider: " + className, e);
        } catch (NoSuchMethodException e) {
            // Method is optional
        }
        return Optional.empty();
    }

    private Widget findTitleTarget(Screen screen) {
        String targetLang = this.getTitleTargetLang(Config.getTitleMenuTarget());
        return this.findTarget(screen, targetLang);
    }

    private Widget findPauseTarget(Screen screen) {
        String targetLang = this.getPauseTargetLang(Config.getPauseMenuTarget());
        return this.findTarget(screen, targetLang);
    }

    private Widget findTarget(Screen screen, String langTarget) {
        return screen.children().stream()
                .filter(listener -> listener instanceof Widget)
                .map(listener -> (Widget) listener)
                .filter(listener -> {
                    if (listener instanceof TitleButton btn) {
                        if (btn.text().get() instanceof TranslationText component) {
                            return component.serialize().getString("path", "").equals(langTarget);
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);
    }

    private String getTitleTargetLang(Config.TitleMenuTargets target) {
        return switch (target) {
            case SINGLE_PLAYER -> "menu.singleplayer";
            case MULTIPLAYER -> "menu.multiplayer";
            case REALMS -> "menu.online";
            case LANGUAGE -> "options.language";
            case OPTIONS -> "menu.options";
            case QUIT_GAME -> "menu.quit";
            case ACCESSIBILITY -> "options.accessibility";
        };
    }

    private String getPauseTargetLang(Config.PauseMenuTargets target) {
        return switch (target) {
            case RETURN_TO_GAME -> "menu.returnToGame";
            case ADVANCEMENTS -> "gui.advancements";
            case FEEDBACK -> "menu.sendFeedback";
            case OPTIONS -> "menu.options";
            case STATISTICS -> "gui.stats";
            case REPORT_BUGS -> "menu.reportBugs";
            case OPEN_TO_LAN -> "menu.shareToLan";
            case SAVE_AND_QUIT -> "menu.returnToMenu";
        };
    }
}
