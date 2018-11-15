package sts_exporter;

import java.io.IOException;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.relics.AbstractRelic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basemod.BaseMod;
import basemod.ModButton;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.interfaces.PostInitializeSubscriber;

@SpireInitializer
public class Exporter implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(Exporter.class.getName());

    SpireConfig config;
    public static final String CONFIG_EXPORT_AT_START = "export_at_startup";
    public static final String CONFIG_INCLUDE_BASE_GAME = "include_base_game";
    public static final String CONFIG_EXPORT_DIR = "export_dir";

    public Exporter() {
        // config
        try {
            config = new SpireConfig("Exporter", "config");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!config.has(CONFIG_EXPORT_AT_START)) config.setBool(CONFIG_EXPORT_AT_START, false);
        if (!config.has(CONFIG_INCLUDE_BASE_GAME)) config.setBool(CONFIG_INCLUDE_BASE_GAME, false);
        if (!config.has(CONFIG_EXPORT_DIR)) config.setString(CONFIG_EXPORT_DIR, "export");
        // initialize
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new Exporter();
    }

    @Override
    public void receivePostInitialize() {
        ModPanel settingsPanel = new ModPanel();
        settingsPanel.addUIElement(new ModLabeledToggleButton("Run exporter at startup", 360, 700, Settings.CREAM_COLOR, FontHelper.charDescFont, config.getBool(CONFIG_EXPORT_AT_START), settingsPanel, l -> {}, button -> {
            config.setBool(CONFIG_EXPORT_AT_START, button.enabled);
            saveConfig();
        }));
        settingsPanel.addUIElement(new ModLabeledToggleButton("Export items from the base game", 360, 650, Settings.CREAM_COLOR, FontHelper.charDescFont, config.getBool(CONFIG_INCLUDE_BASE_GAME), settingsPanel, l -> {}, button -> {
            config.setBool(CONFIG_INCLUDE_BASE_GAME, button.enabled);
            saveConfig();
        }));
        settingsPanel.addUIElement(new ModButton(350, 200, settingsPanel, button -> {
            exportAll();
        }));
        settingsPanel.addUIElement(new ModLabel("Export now", 350+125, 200+50, settingsPanel, l -> {}));
        BaseMod.registerModBadge(ImageMaster.loadImage("img/ExporterBadge.png"), "Spire Exporter", "twanvl", "", settingsPanel);

        if (config.getBool(CONFIG_EXPORT_AT_START)) {
            exportAll();
        }
    }

    void saveConfig() {
        try {
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void exportAll() {
        // set scale to 1.0
        float oldScale = Settings.scale;
        int oldWidth = Settings.WIDTH, oldHeight = Settings.HEIGHT;
        Settings.scale = 1.0f;
        Settings.WIDTH = 1920;
        Settings.HEIGHT = 1080;
        if (oldScale != 1.0f) {
            FontHelper.initialize(); // Re initialize fonts, because that depends on Settings.scale
        }
        // Run exporter
        try {
            ExportHelper export = new ExportHelper(config);
            export.collectAll();
            export.exportAll();
        } catch (Exception e) {
            logger.error("Error during export", e);
        }
        // Restore scale
        Settings.scale = oldScale;
        Settings.WIDTH = oldWidth;
        Settings.HEIGHT = oldHeight;
        if (oldScale != 1.0f) {
            FontHelper.initialize();
        }
    }

    public static String typeString(AbstractCard.CardType type) {
        switch (type) {
            case ATTACK: {
                return AbstractCard.TEXT[0];
            }
            case SKILL: {
                return AbstractCard.TEXT[1];
            }
            case POWER: {
                return AbstractCard.TEXT[2];
            }
            case STATUS: {
                return AbstractCard.TEXT[7];
            }
            case CURSE: {
                return AbstractCard.TEXT[3];
            }
            default: {
                return AbstractCard.TEXT[5];
            }
        }
    }

    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;
        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            } else {
                c = Character.toLowerCase(c);
            }
            titleCase.append(c);
        }
        return titleCase.toString();
    }

    public static String rarityName(AbstractCard.CardRarity rarity) {
        return toTitleCase(rarity.toString()); // TODO: localize?
    }

    public static String rarityName(AbstractPotion.PotionRarity rarity) {
        return toTitleCase(rarity.toString()); // TODO: localize?
    }

    public static String tierName(AbstractRelic.RelicTier tier) {
        return toTitleCase(tier.toString()); // TODO: localize?
    }

    public static String colorName(AbstractCard.CardColor color) {
        return toTitleCase(color.toString()); // TODO: localize?
    }
}
