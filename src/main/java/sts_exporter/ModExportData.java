package sts_exporter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;

class ModExportData {
    public ModInfo info;
    public String id;
    public String name;
    public String modName; // same as name, but empty for the base game
    public URL url;
    public ArrayList<CardExportData> cards = new ArrayList<>();
    public ArrayList<RelicExportData> relics = new ArrayList<>();
    public ArrayList<CreatureExportData> creatures = new ArrayList<>();
    public ArrayList<PotionExportData> potions = new ArrayList<>();
    public ArrayList<KeywordExportData> keywords = new ArrayList<>();
    public static final String BASE_GAME_ID = "slay-the-spire";

    ModExportData(ExportHelper export, ModInfo info) {
        this.info = info;
        this.id = info.ID;
        this.name = info.Name;
        this.modName = info.Name;
        this.url = info.jarURL;
    }

    ModExportData(ExportHelper export) {
        this.info = null;
        this.id = BASE_GAME_ID;
        this.name = "Slay the Spire";
        this.modName = "";
        try {
            this.url = new File(Loader.STS_JAR).toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void exportImages() {
        for (CardExportData x : this.cards) x.exportImages();
        for (RelicExportData x : this.relics) x.exportImages();
        for (CreatureExportData x : this.creatures) x.exportImages();
        for (PotionExportData x : this.potions) x.exportImages();
    }
}