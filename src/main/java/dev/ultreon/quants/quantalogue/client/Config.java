package dev.ultreon.quants.quantalogue.client;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * Author: MrCrayfish
 */
public class Config {
    private static final String DEFAULT_CONFIG = """
            # ----------- Quantalogue CONFIG -----------
            # If properties are missing, delete this file
            # and load the game to regenerate the config.
            
            # [Title Menu Button Visibility]
            # The visibility of the Quantalogue button on the title menu
            # Possible values: true, false
            title_menu_visible=true
            
            # [Title Menu Button Alignment]
            # The alignment of the Quantalogue button relative to the target widget on the title menu
            # Possible values: left, right
            title_menu_align=left
            
            # [Title Menu Target]
            # The widget on the title screen to target the placement of the Quantalogue button
            # Possible values: single_player, multiplayer, realms, language, options, quit_game, accessibility
            title_menu_target=realms
            
            # [Pause Menu Button Visibility]
            # The visibility of the Quantalogue button on the pause menu
            # Possible values: true, false
            pause_menu_visible=true
            
            # [Pause Menu Button Alignment]
            # The alignment of the Quantalogue button relative to the target widget on the pause menu
            # Possible values: left, right
            pause_menu_align=left
            
            # [Pause Menu Target]
            # The widget on the pause screen to target the placement of the Quantalogue button
            # Possible values: single_player, multiplayer, realms, language, options, quit_game, accessibility
            pause_menu_target=realms
            """;

    private static Boolean titleMenuVisible = true;
    private static Align titleMenuAlign = Align.LEFT;
    private static TitleMenuTargets titleMenuTarget = TitleMenuTargets.REALMS;
    private static Boolean pauseMenuVisible = true;
    private static Align pauseMenuAlign = Align.LEFT;
    private static PauseMenuTargets pauseMenuTarget = PauseMenuTargets.OPTIONS;

    public static Boolean isTitleMenuVisible() {
        return titleMenuVisible;
    }

    public static Align getTitleMenuAlign() {
        return titleMenuAlign;
    }

    public static TitleMenuTargets getTitleMenuTarget() {
        return titleMenuTarget;
    }

    public static Boolean isPauseMenuVisible() {
        return pauseMenuVisible;
    }

    public static Align getPauseMenuAlign() {
        return pauseMenuAlign;
    }

    public static PauseMenuTargets getPauseMenuTarget() {
        return pauseMenuTarget;
    }

    public static void load(Path path) {
        File file = getConfigFile(path);
        if (file != null) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(file));
                titleMenuVisible = Boolean.valueOf(properties.getProperty("title_menu_visible"));
                titleMenuAlign = Align.get(properties.getProperty("title_menu_align"));
                titleMenuTarget = TitleMenuTargets.get(properties.getProperty("title_menu_target"));
                pauseMenuVisible = Boolean.valueOf(properties.getProperty("pause_menu_visible"));
                pauseMenuAlign = Align.get(properties.getProperty("pause_menu_align"));
                pauseMenuTarget = PauseMenuTargets.get(properties.getProperty("pause_menu_target"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Nullable
    private static File getConfigFile(Path path) {
        Path file = path.resolve("quantalogue.properties");
        if (!Files.exists(file)) {
            try {
                Files.writeString(file, DEFAULT_CONFIG, StandardOpenOption.CREATE);
            } catch (IOException e) {
                return null;
            }
        }
        return file.toFile();
    }

    public enum Align {
        LEFT("left"),
        RIGHT("right");

        private final String name;

        Align(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return this.name;
        }

        public static Align get(String name) {
            for (var v : values()) {
                if (v.getSerializedName().equals(name)) {
                    return v;
                }
            }

            return null;
        }

    }

    public enum TitleMenuTargets {
        SINGLE_PLAYER("single_player"),
        MULTIPLAYER("multiplayer"),
        REALMS("realms"),
        LANGUAGE("language"),
        OPTIONS("options"),
        QUIT_GAME("quit_game"),
        ACCESSIBILITY("accessibility");

        private final String name;

        TitleMenuTargets(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return this.name;
        }

        public static TitleMenuTargets get(String name) {
            for (var v : values()) {
                if (v.getSerializedName().equals(name)) {
                    return v;
                }
            }

            return null;
        }
    }

    public enum PauseMenuTargets {
        RETURN_TO_GAME("return_to_game"),
        ADVANCEMENTS("advancements"),
        FEEDBACK("feedback"),
        OPTIONS("options"),
        STATISTICS("statistics"),
        REPORT_BUGS("report_bugs"),
        OPEN_TO_LAN("open_to_lan"),
        SAVE_AND_QUIT("save_and_quit");

        private final String name;

        PauseMenuTargets(String name) {
            this.name = name;
        }

        public String getSerializedName() {
            return this.name;
        }

        public static PauseMenuTargets get(String name) {
            for (var v : values()) {
                if (v.getSerializedName().equals(name)) {
                    return v;
                }
            }

            return null;
        }
    }
}
