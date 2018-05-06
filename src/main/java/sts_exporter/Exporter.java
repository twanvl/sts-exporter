package sts_exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.CardLibrary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;

@SpireInitializer
public class Exporter implements PostInitializeSubscriber {
    public static final Logger logger = LogManager.getLogger(Exporter.class.getName());
    private static final String[] templates = {"cardlist.html","cardlist.wiki","wiki-card-data.txt"};
    private static final String[] indexTemplates = {"index.html","creatures.html"};

    public Exporter() {
        BaseMod.subscribeToPostInitialize(this);
    }

    public static void initialize() {
        new Exporter();
    }

    public void receivePostInitialize() {
        // set scale to 1.0
        //float oldscale = Settings.scale;
        //Settings.scale = 1.0f;
        // Run exporter
        try {
            exportAll("export");
        } catch (Exception e) {
            logger.error("Error during export", e);
        }
        //Settings.scale = oldscale;
    }

    private static void mkdir(String dir) {
        File f = new File(dir);
        f.mkdir();
    }

    public static void exportAll(String outdir) {
        logger.info("Exporting all cards to " + outdir);
        mkdir(outdir);
        // colors and cards
        ArrayList<String> colors = new ArrayList<>();
        for (AbstractCard.CardColor color : AbstractCard.CardColor.values()) {
            exportAll(color, outdir + "/" + color);
            colors.add(color.toString());
        }
        // monsters
        logger.info("Exporting creatures");
        String monsterdir = outdir + "/creatures";
        ArrayList<CreatureExportData> creatures = new ArrayList<>();
        for (AbstractCreature m : CreatureExportData.getAllCreatures()) {
            creatures.add(new CreatureExportData(m, monsterdir));
        }
        // write index file
        JtwigModel model = JtwigModel.newModel().with("colors",colors).with("creatures",creatures);
        for (String templateName : indexTemplates) {
            try {
                logger.info("Writing " + outdir + "/" + templateName);
                FileOutputStream stream = new FileOutputStream(outdir + "/" + templateName);
                JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/" + templateName + ".twig");
                template.render(model, stream);
                stream.close();
            } catch (IOException e) {
                logger.error(e);
            }
        }
        logger.info("Done exporting.");
    }

    public static void exportAll(AbstractCard.CardColor color, String outdir) {
        AbstractList<AbstractCard> cards = CardLibrary.getCardList(CardLibrary.LibraryType.valueOf(color.name()));
        exportAll(color, cards, outdir);
    }
    public static void exportAll(AbstractCard.CardColor color, AbstractList<AbstractCard> cards, String outdir) {
        logger.info("Exporting " + color + " to " + outdir);
        mkdir(outdir);
        String imagedir = outdir + "/card-images";
        mkdir(imagedir);
        // Create a data structure for all the cards.
        // At the same time, export all images
        ArrayList<CardExportData> cardData = new ArrayList<>();
        for (AbstractCard c : cards) {
            cardData.add(new CardExportData(c.makeCopy(), imagedir));
        }
        Collections.sort(cardData, (CardExportData a, CardExportData b) -> { return a.name.compareTo(b.name); });
        // Export HTML file with all cards
        JtwigModel model = JtwigModel.newModel();
        model.with("color", colorName(color));
        model.with("cards", cardData);
        for (String templateName : templates) {
            try {
                logger.info("Writing " + outdir + "/" + templateName);
                FileOutputStream stream = new FileOutputStream(outdir + "/" + templateName);
                JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/" + templateName + ".twig");
                template.render(model, stream);
                stream.close();
            } catch (IOException e) {
                logger.error(e);
            }
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
        return toTitleCase(rarity.toString()); // TODO: localize
    }

    public static String colorName(AbstractCard.CardColor color) {
        return toTitleCase(color.toString()); // TODO: localize
    }

}
