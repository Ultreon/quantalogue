package dev.ultreon.quants.quantalogue.platform;

import com.badlogic.gdx.graphics.Pixmap;
import dev.ultreon.quants.quantalogue.Constants;
import dev.ultreon.quants.quantalogue.client.FabricModData;
import dev.ultreon.quants.quantalogue.client.IModData;
import dev.ultreon.quants.quantalogue.exception.ModResourceNotFoundException;
import dev.ultreon.quants.quantalogue.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public List<IModData> getAllModData() {
        return FabricLoader.getInstance().getAllMods().stream().map(ModContainer::getMetadata).map(FabricModData::new).collect(Collectors.toList());
    }

    @Override
    public File getModDirectory() {
        return FabricLoaderImpl.INSTANCE.getModsDirectory();
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoaderImpl.INSTANCE.getConfigDir();
    }

    @Override
    public Pixmap loadImageFromModResource(String modId, String resource) throws ModResourceNotFoundException {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .flatMap(c -> c.findPath(resource))
                .map(path -> {
                    try (InputStream is = Files.newInputStream(path)) {
                        byte[] bytes = is.readAllBytes();
                        return new Pixmap(bytes, 0, bytes.length);
                    } catch (IOException e) {
                        Constants.LOG.error("Failed to load image resource '{}' from mod {}", resource, modId, e);
                        return null;
                    }
                }).orElseThrow(ModResourceNotFoundException::new);
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

}
