package dev.ultreon.quants.quantalogue.platform;

import dev.ultreon.quants.quantalogue.platform.services.IComponentHelper;
import dev.ultreon.quants.quantalogue.platform.services.IPlatformHelper;

public class ClientServices {
    public static final IPlatformHelper PLATFORM = new FabricPlatformHelper();
    public static final IComponentHelper COMPONENT = new FabricComponentHelper();
}