package sts_exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.megacrit.cardcrawl.helpers.GameDictionary;

import sts_exporter.patches.BaseModPatches;

class KeywordExportData implements Comparable<KeywordExportData> {
    public String description, descriptionHTML, descriptionPlain;
    public String name;
    public ArrayList<String> names = new ArrayList<>();
    public ModExportData mod;

    KeywordExportData(ExportHelper export, String name, String description) {
        this.name = name;
        this.description = description;
        this.descriptionHTML = RelicExportData.smartTextToHTML(description,true,true);
        this.descriptionPlain = RelicExportData.smartTextToPlain(description,true,true);
        this.mod = export.findMod(BaseModPatches.keywordClasses.get(name));
        this.mod.keywords.add(this);
    }

    public static ArrayList<KeywordExportData> exportAllKeywords(ExportHelper export) {
        ArrayList<KeywordExportData> keywords = new ArrayList<>();
        HashMap<String,KeywordExportData> keywordLookup = new HashMap<>();
        for (Map.Entry<String,String> kw : GameDictionary.keywords.entrySet()) {
            String parent = GameDictionary.parentWord.get(kw.getKey());
            if (parent == null || parent.equals(kw.getKey())) {
                KeywordExportData keyword = new KeywordExportData(export, kw.getKey(), kw.getValue());
                keywords.add(keyword);
                keywordLookup.put(kw.getKey(),keyword);
            }
        }
        for (Map.Entry<String,String> kw : GameDictionary.parentWord.entrySet()) {
            String parent = kw.getValue();
            keywordLookup.get(parent).names.add(kw.getKey());
        }
        Collections.sort(keywords);
        return keywords;
    }

    @Override
    public int compareTo(KeywordExportData that) {
        return name.compareTo(that.name);
    }
}