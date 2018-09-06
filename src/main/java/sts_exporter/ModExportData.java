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
    public String name, modName;
    public URL url;
    public ArrayList<CardExportData> cards = new ArrayList<>();
    public ArrayList<RelicExportData> relics = new ArrayList<>();
    public ArrayList<CreatureExportData> creatures = new ArrayList<>();
    public ArrayList<PotionExportData> potions = new ArrayList<>();

    ModExportData(ModInfo info) {
        this.info = info;
        this.id = info.ID;
        this.name = info.Name;
        this.modName = info.Name;
        this.url = info.jarURL;
    }

    ModExportData() {
        this.info = null;
        this.id = "Slay the Spire";
        this.name = "Slay the Spire";
        this.modName = "";
        try {
            this.url = new File(Loader.STS_JAR).toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}