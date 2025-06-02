package dev.ultreon.quants.quantalogue.platform.services;

import com.badlogic.gdx.graphics.Pixmap;
import dev.ultreon.quants.quantalogue.client.IModData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface IPlatformHelper {
    List<IModData> getAllModData();

    File getModDirectory();

    Path getConfigDirectory();

    Pixmap loadImageFromModResource(String modId, String resource) throws IOException;

    boolean isModLoaded(String modId);
}