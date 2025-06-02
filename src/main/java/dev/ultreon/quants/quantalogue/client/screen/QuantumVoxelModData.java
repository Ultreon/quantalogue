package dev.ultreon.quants.quantalogue.client.screen;

import dev.ultreon.quants.quantalogue.client.IModData;
import dev.ultreon.quantum.client.QuantumClient;
import dev.ultreon.quantum.client.gui.Renderer;
import dev.ultreon.quantum.client.gui.Screen;
import dev.ultreon.quantum.client.gui.screens.settings.SettingsScreen;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Author: MrCrayfish
 */
public class QuantumVoxelModData implements IModData {
    @Override
    public Type getType() {
        return Type.DEFAULT;
    }

    @Override
    public String getModId() {
        return "quantum";
    }

    @Override
    public String getDisplayName() {
        return "Quantum Voxel";
    }

    @Override
    public String getVersion() {
        return QuantumClient.getGameVersion();
    }

    @Override
    public String getDescription() {
        return FabricLoader.getInstance().getModContainer(getModId()).orElseThrow().getMetadata().getDescription();
    }

    @Nullable
    @Override
    public String getItemIcon() {
        return null;
    }

    @Nullable
    @Override
    public String getImageIcon() {
        return null;
    }

    @Override
    public String getLicense() {
        return "Apache 2.0";
    }

    @Nullable
    @Override
    public String getCredits() {
        return null;
    }

    @Nullable
    @Override
    public String getAuthors() {
        return "Ultreon Studios";
    }

    @Nullable
    @Override
    public String getHomepage() {
        return "https://ultreon.dev";
    }

    @Nullable
    @Override
    public String getIssueTracker() {
        return "https://github.com/QuantumVoxel/quantum-voxel/issues";
    }

    @Nullable
    @Override
    public String getBanner() {
        return null;
    }

    @Nullable
    @Override
    public String getBackground() {
        return null;
    }

    @Override
    public Update getUpdate() {
        return null;
    }

    @Override
    public Set<String> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean hasConfig() {
        return true;
    }

    @Override
    public boolean isLogoSmooth() {
        return true;
    }

    @Override
    public boolean isLibrary() {
        return true;
    }

    @Override
    public void openConfigScreen(Screen parent) {
        QuantumClient client = QuantumClient.get();
        client.showScreen(new SettingsScreen());
    }

    @Override
    public void drawUpdateIcon(Renderer renderer, Update update, int x, int y) {

    }
}
