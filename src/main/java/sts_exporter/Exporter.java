package sts_exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;
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
    private static final String[] templates = {"cardlist.html"};
    private static final String[] indexTemplates = {"index.html"};

    public Exporter() {
        BaseMod.subscribeToPostInitialize(this);
    }

    public static void initialize() {
        new Exporter();
    }

    public void receivePostInitialize() {
        // Run exporter
        exportAll("export");
    }

    private static void mkdir(String dir) {
        File f = new File(dir);
        f.mkdir();
    }

    public static void exportAll(String outdir) {
        logger.info("Exporting all cards to " + outdir);
        mkdir(outdir);
        ArrayList<String> colors = new ArrayList<>();
        for (AbstractCard.CardColor color : AbstractCard.CardColor.values()) {
            exportAll(color, outdir + "/" + color);
            colors.add(color.toString());
        }
        // write index file
        JtwigModel model = JtwigModel.newModel().with("colors",colors);
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
        logger.info("Done exporting cards.");
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
        ArrayList<CardData> cardData = new ArrayList<>();
        for (AbstractCard c : cards) {
            cardData.add(new CardData(c.makeCopy(), imagedir));
        }
        Collections.sort(cardData, (CardData a, CardData b) -> { return a.name.compareTo(b.name); });
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

    public static class CardData {
        public AbstractCard card;
        public CardData upgrade;
        public String name;
        public String color;
        public String rarity;
        public String type;
        public String image, relImage;
        public String cost, costAndUpgrade;
        public String text, textAndUpgrade;
        public int block, damage, magicNumber;

        public CardData(AbstractCard card, String imageDir) {
            card.initializeDescription();
            this.card = card;
            this.name = card.name;
            this.rarity = rarityName(card.rarity);
            this.color = colorName(card.color);
            this.type = typeString(card.type);
            if (!card.upgraded && card.canUpgrade()) {
                AbstractCard copy = card.makeCopy();
                copy.upgrade();
                copy.displayUpgrades();
                this.upgrade = new CardData(copy, imageDir);
            }
            // cost
            if (card.cost == -1) {
                this.cost = "X";
            } else if (card.cost == -2) {
                this.cost = ""; // unplayable
            } else {
                this.cost = String.valueOf(card.cost);
            }
            this.costAndUpgrade = combineUpgrade(cost, upgrade == null ? null : upgrade.cost);
            // description
            this.block = card.isBlockModified ? card.block : card.baseBlock;
            this.damage = card.isDamageModified ? card.damage : card.baseDamage;
            this.magicNumber = card.isMagicNumberModified ? card.magicNumber : card.baseMagicNumber;
            this.text = card.rawDescription
                            .replace("!B!", String.valueOf(block))
                            .replace("!D!", String.valueOf(damage))
                            .replace("!M!", String.valueOf(magicNumber))
                            .replace(" NL ", " ");
            if (upgrade == null) {
                this.textAndUpgrade = this.text;
            } else {
                this.textAndUpgrade = combineDescriptions(card.rawDescription, upgrade.card.rawDescription)
                                .replace("!B!", combineUpgrade(String.valueOf(block), String.valueOf(upgrade.block)))
                                .replace("!D!", combineUpgrade(String.valueOf(damage), String.valueOf(upgrade.damage)))
                                .replace("!M!", combineUpgrade(String.valueOf(magicNumber), String.valueOf(upgrade.magicNumber)))
                                .replace(" NL ", " ");
            }
            // image
            exportImageToDir(imageDir);
        }

        private void exportImageToDir(String imageDir) {
            String safename = this.name;
            safename = safename.replace(" ","");
            safename = safename.replace("/","");
            safename = safename.replace("+","Plus");
            this.image = imageDir + "/" + safename + ".png";
            this.relImage = "card-images/" + safename + ".png";
            exportImageToFile(this.image);
        }

        private void exportImageToFile(String imageFile) {
            logger.info("Rendering image to " + imageFile);
            // Scale and position of the card
            // IMG_WIDTH,IMG_HEIGHT are only for the card border, mana cost and rarity banner is outside that, so add some padding.
            card.drawScale = 1.0f;
            float width  = (AbstractCard.IMG_WIDTH + 24.0f) * card.drawScale;
            float height = (AbstractCard.IMG_HEIGHT + 24.0f) * card.drawScale;
            int iwidth = Math.round(width), iheight = Math.round(height);
            card.current_x = width/2;
            card.current_y = height/2;
            // create a frame buffer
            FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, iwidth, iheight, false);
            //make the FBO the current buffer
            fbo.begin();
            //... clear the FBO color with transparent black ...
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f); //transparent black
            Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT); //clear the color buffer
            // set up batch and projection matrix
            SpriteBatch sb = new SpriteBatch();
            Matrix4 matrix = new Matrix4();
            matrix.setToOrtho(0,iwidth, iheight,0, 0.f, 1.f); // note: flip the vertical direction, otherwise cards are upside down
            sb.setProjectionMatrix(matrix);
            // render the card
            sb.begin();
            card.render(sb, false);
            sb.end();
            sb.dispose();
            // write to png file
            Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0,0, iwidth, iheight);
            PixmapIO.writePNG(Gdx.files.local(imageFile), pixmap);
            pixmap.dispose();
            // done
            fbo.end();
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

    private static String combineUpgrade(String a, String b) {
        if (b == null || b.equals(a)) return a;
        return a + "(" + b + ")";
    }

    private static String combineDescriptions(String a, String b) {
        // Combine description with upgrade description
        if (a.equals(b)) return a;
        // prepare punctuation, so we count it as separate words
        a = a.replace("."," .").replace(","," ,").replace(" NL "," \n ");
        b = b.replace("."," .").replace(","," ,").replace(" NL "," \n ");
        // Split input into words
        ArrayList<String> awords = words(a);
        ArrayList<String> bwords = words(b);
        // Use the standard sequence alignment algorithm (Needleman–Wunsch)
        final int INSERT_A = 10;
        final int INSERT_B = 10;
        int[][] score = new int[awords.size()+1][bwords.size()+1];
        for (int ai=0 ; ai <= awords.size() ; ai++) {
            score[ai][0] = ai * INSERT_A;
        }
        for (int bi=0 ; bi <= bwords.size() ; bi++) {
            score[0][bi] = bi * INSERT_B;
        }
        for (int ai=1 ; ai <= awords.size() ; ai++) {
            for (int bi=1 ; bi <= bwords.size() ; bi++) {
                score[ai][bi] = Math.min(score[ai-1][bi] + INSERT_A,
                                Math.min(score[ai][bi-1] + INSERT_B,
                                         score[ai-1][bi-1] + wordCost(awords.get(ai-1),bwords.get(bi-1))));
            }
        }
        // Now return the optimal alignment, first in reverse order
        ArrayList<String>    words  = new ArrayList<>();
        ArrayList<Character> source = new ArrayList<>();
        int ai=awords.size(), bi=bwords.size();
        while (ai > 0 && bi > 0) {
            int acost       = score[ai-1][bi] + INSERT_A;
            int bcost       = score[ai][bi-1] + INSERT_B;
            int replacecost = score[ai-1][bi-1] + wordCost(awords.get(ai-1),bwords.get(bi-1));
            if (bcost <= acost && bcost <= replacecost) {
                words.add(bwords.get(bi-1));
                source.add('b');
                bi--;
                continue;
            } else if (acost <= replacecost) {
                words.add(awords.get(ai-1));
                source.add('a');
                ai--;
            } else {
                words.add(wordReplacement(awords.get(ai-1),bwords.get(bi-1)));
                source.add('c');
                ai--; bi--;
            }
        }
        while (bi > 0) {
            words.add(bwords.get(bi-1));
            source.add('b');
            bi--;
        }
        while (ai > 0) {
            words.add(awords.get(ai-1));
            source.add('a');
            ai--;
        }
        // Now reverse
        Collections.reverse(words);
        Collections.reverse(source);
        // Add parentheses to destinguish the sources
        // We keep track of which source we are taking words from ('a', 'b', or a combination 'c')
        char prev = 'c';
        int astart = 0;
        StringBuilder out = new StringBuilder();
        for (int i = 0 ; i < words.size() ; i++) {
            if (i > 0) out.append(" ");
            if (source.get(i) == 'a' && prev != 'a') astart = i;
            if (source.get(i) != 'b' && prev == 'b') out.append(") ");
            if (source.get(i) == 'b' && prev != 'b') out.append("(");
            if (source.get(i) == 'c' && prev == 'a') {
                // a deletion not followed by an insertion. For example "Exhaust. (not Exhaust.)".
                out.append("(not");
                for (int j = astart ; j < i ; j++) {
                    out.append(" ");
                    out.append(words.get(j));
                }
                out.append(")");
            }
            prev = source.get(i);
            out.append(words.get(i));
        }
        if (prev == 'b') out.append(")");
        if (prev == 'a') {
            out.append("(not");
            for (int j = astart ; j < words.size() ; j++) {
                out.append(" ");
                out.append(words.get(j));
            }
            out.append(")");
        }
        // Join and remove unnecesary spaces
        return out.toString().replace(" .",".").replace(" ,",",").replace(" )",")").replace("( ","(");
    }

    private static final int wordCost(String aw, String bw) {
        if (aw.equals(bw)) return 0;
        if (bw.equals(aw + "s")) return 10;
        return 21;
    }
    private static final String wordReplacement(String aw, String bw) {
        if (aw.equals(bw)) return aw;
        if (bw.equals(aw + "s")) return aw + "(s)";
        return aw + " (" + bw + ")";
    }

    private static final ArrayList<String> words(String str) {
        Scanner scanner = new Scanner(str);
        ArrayList<String> out = new ArrayList<>();
        while (scanner.hasNext()) {
            out.add(scanner.next());
        }
        scanner.close();
        return out;
    }
}
