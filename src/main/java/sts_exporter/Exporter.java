package sts_exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.helpers.CardLibrary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

@SpireInitializer
public class Exporter implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(Exporter.class.getName());
    private static final String[] colorTemplates = {"cardlist.html","cardlist.wiki","wiki-card-data.txt"};
    private static final String[] indexTemplates = {"index.html","creatures.html","potions.html","relics.html","style.css"};

    private static ArrayList<ModExportData> mods = new ArrayList<>();

    public Exporter() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new Exporter();
    }

    @Override
    public void receivePostInitialize() {
        // set scale to 1.0
        //float oldscale = Settings.scale;
        //Settings.scale = 1.0f;
        // Run exporter
        try {
            initModList();
            exportAll("export");
        } catch (Exception e) {
            logger.error("Error during export", e);
        }
        //Settings.scale = oldscale;
    }

    static void mkdir(String dir) {
        File f = new File(dir);
        f.mkdir();
    }

    public static void exportAll(String outdir) {
        logger.info("Exporting all cards to " + outdir);
        mkdir(outdir);
        // colors and cards
        ArrayList<String> colors = new ArrayList<>();
        for (AbstractCard.CardColor color : AbstractCard.CardColor.values()) {
            exportAllCards(color, outdir + "/" + color);
            colors.add(color.toString());
        }

        // monsters
        logger.info("Exporting creatures");
        ArrayList<CreatureExportData> creatures = CreatureExportData.exportAllCreatures(outdir + "/creatures");

        // relics
        logger.info("Exporting relics");
        ArrayList<RelicExportData> relics = RelicExportData.exportAllRelics(outdir + "/relics");

        // potions
        logger.info("Exporting potions");
        ArrayList<PotionExportData> potions = PotionExportData.exportAllPotions(outdir + "/potions");

        // mods
        exportAllMods(outdir + "/mods");

        // write index files
        JtwigModel model = JtwigModel.newModel();
        model.with("colors",colors);
        model.with("relics",relics);
        model.with("creatures",creatures);
        model.with("potions",potions);
        model.with("mods",mods);
        writeTwigTemplates(model, indexTemplates, outdir);
        logger.info("Done exporting.");
    }

    public static void exportAllCards(AbstractCard.CardColor color, String outdir) {
        AbstractList<AbstractCard> cards = CardLibrary.getCardList(CardLibrary.LibraryType.valueOf(color.name()));
        exportAllCards(color, cards, outdir);
    }
    public static void exportAllCards(AbstractCard.CardColor color, AbstractList<AbstractCard> cards, String outdir) {
        logger.info("Exporting " + color + " to " + outdir);
        mkdir(outdir);
        String imagedir = outdir + "/card-images";
        String smallImagedir = outdir + "/small-card-images";
        mkdir(imagedir);
        mkdir(smallImagedir);
        // Create a data structure for all the cards.
        // At the same time, export all images
        ArrayList<CardExportData> cardData = new ArrayList<>();
        for (AbstractCard c : cards) {
            cardData.add(new CardExportData(c.makeCopy(), imagedir, smallImagedir));
        }
        Collections.sort(cardData, (CardExportData a, CardExportData b) -> { return a.name.compareTo(b.name); });
        // Export HTML file with all cards
        JtwigModel model = JtwigModel.newModel();
        model.with("selection", colorName(color));
        model.with("cards", cardData);
        writeTwigTemplates(model, colorTemplates, outdir);
    }

    static void exportAllMods(String modsdir) {
        mkdir(modsdir);
        for (ModExportData mod : mods) {
            JtwigModel model = JtwigModel.newModel();
            model.with("mod", mod);
            model.with("cards", mod.cards);
            model.with("relics", mod.relics);
            model.with("creatures", mod.creatures);
            model.with("potions", mod.potions);
            model.with("selection", mod.name);
            writeTwigTemplate(model, "templates/mod.html.twig", modsdir + "/" + mod.id + ".html");
        }
    }

    private static void writeTwigTemplates(JtwigModel model, String[] templateNames, String outdir) {
        for (String templateName : templateNames) {
            writeTwigTemplate(model, "templates/" + templateName + ".twig", outdir + "/" + templateName);
        }
    }

    private static void writeTwigTemplate(JtwigModel model, String templateFile, String outFile) {
        try {
            logger.info("Writing " + outFile);
            FileOutputStream stream = new FileOutputStream(outFile);
            JtwigTemplate template = JtwigTemplate.classpathTemplate(templateFile);
            template.render(model, stream);
            stream.close();
        } catch (IOException e) {
            logger.error(e);
        }
    }

    private static void initModList() {
        mods.add(new ModExportData());
        for (ModInfo modInfo : Loader.MODINFOS) {
            mods.add(new ModExportData(modInfo));
        }
    }

    public static ModExportData findMod(Class<?> cls) {
        // Inspired by BaseMod.patches.whatmod.WhatMod
        URL locationURL = cls.getProtectionDomain().getCodeSource().getLocation();

        if (locationURL == null) {
            try {
                ClassPool pool = Loader.getClassPool();
                CtClass ctCls = pool.get(cls.getName());
                String url = ctCls.getURL().getFile();
                int i = url.lastIndexOf('!');
                url = url.substring(0, i);
                locationURL = new URL(url);
            } catch (NotFoundException | MalformedURLException e) {
                e.printStackTrace();
            }
        }
        if (locationURL == null) {
            return mods.get(0);
        }
        for (ModExportData mod : mods) {
            if (locationURL.equals(mod.url)) {
                return mod;
            }
        }
        return mods.get(0);
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
        return toTitleCase(rarity.toString()); // TODO: localize
    }

    public static String colorName(AbstractCard.CardColor color) {
        return toTitleCase(color.toString()); // TODO: localize
    }

}
