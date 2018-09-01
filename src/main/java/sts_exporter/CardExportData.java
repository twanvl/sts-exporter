package sts_exporter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.GameDictionary;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;

import basemod.ReflectionHacks;
import basemod.abstracts.CustomCard;

public class CardExportData {
    public AbstractCard card;
    public CardExportData upgrade;
    public String name;
    public String color;
    public String rarity;
    public String type;
    public String image, relImage, absImage;
    public String cost, costAndUpgrade;
    public String text, textAndUpgrade, textWikiData, textWikiFormat;
    public int block, damage, magicNumber;

    public CardExportData(AbstractCard card, String imageDir) {
        this(card, imageDir, true);
    }

    public CardExportData(AbstractCard card, String imageDir, boolean exportUpgrade) {
        card.initializeDescription();
        this.card = card;
        this.name = card.name;
        this.rarity = Exporter.rarityName(card.rarity);
        this.color = Exporter.colorName(card.color);
        this.type = Exporter.typeString(card.type);
        if (exportUpgrade && !card.upgraded && card.canUpgrade()) {
            AbstractCard copy = card.makeCopy();
            copy.upgrade();
            copy.displayUpgrades();
            this.upgrade = new CardExportData(copy, imageDir, false);
        }
        // cost
        if (card.cost == -1) {
            this.cost = "X";
        } else if (card.cost == -2) {
            this.cost = ""; // unplayable
        } else {
            this.cost = String.valueOf(card.cost);
        }
        this.costAndUpgrade = combineUpgrade(cost, upgrade == null ? null : upgrade.cost, TextMode.NORMAL_MODE);
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
            this.textAndUpgrade = combineDescriptions(card.rawDescription, upgrade.card.rawDescription, TextMode.NORMAL_MODE)
                            .replace("!B!", combineUpgrade(String.valueOf(block), String.valueOf(upgrade.block), TextMode.NORMAL_MODE))
                            .replace("!D!", combineUpgrade(String.valueOf(damage), String.valueOf(upgrade.damage), TextMode.NORMAL_MODE))
                            .replace("!M!", combineUpgrade(String.valueOf(magicNumber), String.valueOf(upgrade.magicNumber), TextMode.NORMAL_MODE))
                            .replace(" NL ", "\n");
            this.textWikiData = combineDescriptions(card.rawDescription, upgrade.card.rawDescription, TextMode.WIKI_DATA)
                            .replace("!B!", combineUpgrade(String.valueOf(block), String.valueOf(upgrade.block), TextMode.WIKI_DATA))
                            .replace("!D!", combineUpgrade(String.valueOf(damage), String.valueOf(upgrade.damage), TextMode.WIKI_DATA))
                            .replace("!M!", combineUpgrade(String.valueOf(magicNumber), String.valueOf(upgrade.magicNumber), TextMode.WIKI_DATA))
                            .replace(" NL ", "\n");
            this.textWikiFormat = combineDescriptions(card.rawDescription, upgrade.card.rawDescription, TextMode.WIKI_FORMAT)
                            .replace("!B!", combineUpgrade(String.valueOf(block), String.valueOf(upgrade.block), TextMode.WIKI_FORMAT))
                            .replace("!D!", combineUpgrade(String.valueOf(damage), String.valueOf(upgrade.damage), TextMode.WIKI_FORMAT))
                            .replace("!M!", combineUpgrade(String.valueOf(magicNumber), String.valueOf(upgrade.magicNumber), TextMode.WIKI_FORMAT))
                            .replace(" NL ", "\n");
        }
        // image
        exportImageToDir(imageDir);
    }

    private void exportImageToDir(String imageDir) {
        String safename = this.name;
        safename = safename.replace(" ","");
        safename = safename.replace("/","");
        safename = safename.replace("+","Plus");
        this.image = safename + ".png";
        this.absImage = imageDir + "/" + safename + ".png";
        this.relImage = "card-images/" + safename + ".png";
        exportImageToFile(this.absImage);
    }

    private void exportImageToFile(String imageFile) {
        Exporter.logger.info("Rendering card image to " + imageFile);
        if (false) {
            // This is the in-game rendering path
            // Scale and position of the card
            // IMG_WIDTH,IMG_HEIGHT are only for the card border, mana cost and rarity banner is outside that, so add some padding.
            card.drawScale = 1.0f;
            float width  = (AbstractCard.IMG_WIDTH + 24.0f) * card.drawScale;
            float height = (AbstractCard.IMG_HEIGHT + 24.0f) * card.drawScale;
            int iwidth = Math.round(width), iheight = Math.round(height);
            card.current_x = width/2;
            card.current_y = height/2;
            // Render card to png file
            renderSpriteBatchToPNG(0,0, width,height, iwidth,iheight, imageFile, (SpriteBatch sb) -> {
                card.render(sb,false);
            });
        } else {
            // Use SingleCardViewPopup, to get better image and better fonts.
            card.isLocked = false;
            card.isSeen = true;
            SingleCardViewPopup scv = CardCrawlGame.cardPopup;
            scv.open(card);
            fixModdedImage(scv, card);
            SingleCardViewPopup.isViewingUpgrade = card.upgraded;
            SingleCardViewPopup.enableUpgradeToggle = false;
            // get hitbox
            Hitbox cardHb = (Hitbox)ReflectionHacks.getPrivate(CardCrawlGame.cardPopup, SingleCardViewPopup.class, "cardHb");
            float lpadding = 64.0f * Settings.scale;
            float rpadding = 64.0f * Settings.scale;
            float tpadding = 64.0f * Settings.scale;
            float bpadding = 40.0f * Settings.scale;
            // Note: We would like to use a scale=1/Settings.scale, but that doesn't actually work, since fonts are initialized at startup using Settings.scale
            // Note2: y is up instead of down, so use y-bpadding
            renderSpriteBatchToPNG(cardHb.x-lpadding, cardHb.y-bpadding, cardHb.width+lpadding+rpadding, cardHb.height+tpadding+bpadding, 1.0f, imageFile, (SpriteBatch sb) -> {
                // We can't just call
                //CardCrawlGame.cardPopup.render(sb);
                // because that also draws a UI
                callPrivate(scv, SingleCardViewPopup.class, "renderCardBack", SpriteBatch.class, sb);
                callPrivate(scv, SingleCardViewPopup.class, "renderPortrait", SpriteBatch.class, sb);
                callPrivate(scv, SingleCardViewPopup.class, "renderFrame", SpriteBatch.class, sb);
                callPrivate(scv, SingleCardViewPopup.class, "renderCardBanner", SpriteBatch.class, sb);
                callPrivate(scv, SingleCardViewPopup.class, "renderCardTypeText", SpriteBatch.class, sb);
                if (Settings.lineBreakViaCharacter) {
                    callPrivate(scv, SingleCardViewPopup.class, "renderDescriptionCN", SpriteBatch.class, sb);
                } else {
                    callPrivate(scv, SingleCardViewPopup.class, "renderDescription", SpriteBatch.class, sb);
                }
                callPrivate(scv, SingleCardViewPopup.class, "renderTitle", SpriteBatch.class, sb);
                callPrivate(scv, SingleCardViewPopup.class, "renderCost", SpriteBatch.class, sb);
            });
            SingleCardViewPopup.enableUpgradeToggle = true;
            scv.close();
        }
    }

    private void fixModdedImage(SingleCardViewPopup popup, AbstractCard card) {
        // See basemod.patches.com.megacrit.cardcrawl.screens.SingleCardViewPopup.OpenFix
        // In short: BaseMod loads the large card image only when opening the SingleCardViewPopup with 2 arguments
        Field portraitImageField;
        try {
            portraitImageField = popup.getClass().getDeclaredField("portraitImg");
            portraitImageField.setAccessible(true);
            if (portraitImageField.get(popup) == null && card instanceof CustomCard) {
                portraitImageField.set(popup, CustomCard.getPortraitImage((CustomCard) card));
            }
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static Object callPrivate(Object obj, Class<?> objClass, String methodName, Class<?> paramType, Object arg) {
        try {
           Method method = objClass.getDeclaredMethod(methodName, paramType);
           method.setAccessible(true);
           return method.invoke(obj, arg);
        } catch (Exception ex) {
           Exporter.logger.error("Exception occured when calling private method " + methodName + " of " + objClass.getName(), ex);
           return null;
        }
    }

    public static void renderSpriteBatchToPNG(float x, float y, float width, float height, float scale, String imageFile, Consumer<SpriteBatch> render) {
        renderSpriteBatchToPNG(x,y,width,height, Math.round(scale*width), Math.round(scale*height), imageFile, render);
    }

    public static void renderSpriteBatchToPNG(float x, float y, float width, float height, int iwidth, int iheight, String imageFile, Consumer<SpriteBatch> render) {
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
        matrix.setToOrtho(x, x+width, y+height,y, 0.f, 1.f); // note: flip the vertical direction, otherwise cards are upside down
        sb.setProjectionMatrix(matrix);
        // render the thing
        sb.begin();
        render.accept(sb);
        sb.end();
        sb.dispose();
        // write to png file
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0,0, iwidth, iheight);
        PixmapIO.writePNG(Gdx.files.local(imageFile), pixmap);
        pixmap.dispose();
        // done
        fbo.end();
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

    enum TextMode {
        NORMAL_MODE,
        WIKI_DATA,
        WIKI_FORMAT,
    }

    public static String rarityName(AbstractCard.CardRarity rarity) {
        return toTitleCase(rarity.toString()); // TODO: localize
    }

    public static String colorName(AbstractCard.CardColor color) {
        return toTitleCase(color.toString()); // TODO: localize
    }

    private static String combineUpgrade(String a, String b, TextMode mode) {
        if (b == null || b.equals(a)) return a;
        switch(mode) {
            default:        return a + "(" + b + ")";
            case WIKI_DATA: return "[" + a + "|" + b + "]";
        }
    }

    private static String combineDescriptions(String a, String b, TextMode mode) {
        // Combine description with upgrade description
        if (a.equals(b) && mode == TextMode.NORMAL_MODE) return a;
        // prepare punctuation, so we count it as separate words
        a = preprocessText(a, mode);
        b = preprocessText(b, mode);
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
                                        score[ai-1][bi-1] + wordCost(awords.get(ai-1),bwords.get(bi-1),mode)));
            }
        }
        // Now return the optimal alignment, first in reverse order
        ArrayList<String>    words  = new ArrayList<>();
        ArrayList<Character> source = new ArrayList<>();
        int ai=awords.size(), bi=bwords.size();
        while (ai > 0 && bi > 0) {
            int acost       = score[ai-1][bi] + INSERT_A;
            int bcost       = score[ai][bi-1] + INSERT_B;
            int replacecost = score[ai-1][bi-1] + wordCost(awords.get(ai-1),bwords.get(bi-1),mode);
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
                words.add(wordReplacement(awords.get(ai-1),bwords.get(bi-1),mode));
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
        if (mode == TextMode.NORMAL_MODE || mode == TextMode.WIKI_FORMAT) {
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
                // is this a keyword?
                out.append(formatKeyword(words.get(i), mode));
            }
            if (prev == 'b') out.append(")");
            if (prev == 'a') {
                out.append(" (not");
                for (int j = astart ; j < words.size() ; j++) {
                    out.append(" ");
                    out.append(words.get(j));
                }
                out.append(")");
            }
        } else {
            for (int i = 0 ; i < words.size() ; i++) {
                if      (source.get(i) == 'c' && prev == 'a') out.append("|] ");
                else if (source.get(i) == 'c' && prev == 'b') out.append("] ");
                else if (source.get(i) == 'b' && prev == 'a') out.append("|");
                else if (source.get(i) == 'b' && prev == 'c') { if (i > 0) out.append("| "); else out.append("|"); }
                else if (source.get(i) == 'a' && prev == 'c') { if (i > 0) out.append(" ["); else out.append("["); }
                else if (source.get(i) == 'a' && prev == 'b') out.append("] [");
                else if (i > 0) out.append(" ");
                prev = source.get(i);
                // is this a keyword?
                out.append(formatKeyword(words.get(i), mode));
            }
            if (prev == 'b') out.append("]");
            if (prev == 'a') out.append("|");
        }
        // Join and remove unnecesary spaces
        if (mode == TextMode.WIKI_DATA) {
            return out.toString().replace(" .",".").replace(" ,",",").replace(" ]","]").replace("[ ","[").replace(" NL]"," NL ]").replace("[NL ","[ NL ").replace(" NL|"," NL |").replace("|NL ","| NL ");
        } else {
            return out.toString().replace(" .",".").replace(" ,",",").replace(" )",")").replace("( ","(").replace(" NL)",") NL ").replace("(NL "," NL (");
        }
    }

    private static String preprocessText(String a, TextMode mode) {
        a = a.replace("."," .").replace(","," ,");
        if (mode == TextMode.WIKI_DATA) {
            a = a.replace("[R]","<R>").replace("[G]","<G>").replace("[B]","<B>");
        } else if(mode == TextMode.WIKI_FORMAT) {
            final String[] energySymbols = {"[R] [R] [R]","[R] [R]","[R]", "[G] [G] [G]","[G] [G]","[G]", "[B] [B] [B]","[B] [B]","[B]"};
            for (String e : energySymbols) {
                a = a.replace(e, (e.length()+1)/4 + " Energy");
            }
        }
        return a;
    }

    private static final int wordCost(String aw, String bw, TextMode mode) {
        if (aw.equals(bw)) return 0;
        if (mode != TextMode.WIKI_DATA && bw.equals(aw + "s")) return 10;
        return 21;
    }
    private static final String wordReplacement(String aw, String bw, TextMode mode) {
        if (aw.equals(bw)) return aw;
        if (mode != TextMode.WIKI_DATA && bw.equals(aw + "s")) return aw + "(s)";
        return aw + " (" + bw + ")";
    }

    private static final String formatKeyword(String w, TextMode mode) {
        if (GameDictionary.keywords.containsKey(w.toLowerCase()) || w.toLowerCase().equals("energy")) {
            // keyword
            if (mode == TextMode.WIKI_FORMAT) {
                return "[[" + w + "]]";
            } else if (mode == TextMode.WIKI_DATA) {
                return "#" + w;
            }
        }
        return w;
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
