package sts_exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.screens.SingleRelicViewPopup;

import basemod.BaseMod;
import basemod.ReflectionHacks;

class RelicExportData implements Comparable<RelicExportData> {
    public AbstractRelic relic;
    public ModExportData mod;
    public String tier;
    public String pool;
    public AbstractCard.CardColor poolColor;
    public ExportPath image;
    public ExportPath popupImage;
    public ExportPath smallPopupImage;
    public String name;
    public String description, descriptionHTML, descriptionPlain;
    public String flavorText, flavorTextHTML, flavorTextPlain;

    RelicExportData(ExportHelper export, AbstractRelic relic, AbstractCard.CardColor pool) {
        this.relic = relic;
        this.mod = export.findMod(relic.getClass());
        this.mod.relics.add(this);
        this.name = relic.name;
        this.description = relic.description;
        this.descriptionHTML = smartTextToHTML(relic.description,true,true);
        this.descriptionPlain = smartTextToPlain(relic.description,true,true);
        this.flavorText = relic.flavorText;
        this.flavorTextHTML = smartTextToHTML(relic.flavorText,false,true);;
        this.flavorTextPlain = smartTextToPlain(relic.flavorText,false,true);;
        this.tier = Exporter.tierName(relic.tier);
        this.poolColor = pool;
        this.pool = pool == null ? "" : Exporter.colorName(pool);
        this.image           = export.exportPath(this.mod, "relics", relic.relicId, ".png");
        this.popupImage      = export.exportPath(this.mod, "relics/popup", relic.relicId, ".png");
        this.smallPopupImage = export.exportPath(this.mod, "relics/small-popup", relic.relicId, ".png");
    }

    public void exportImages() {
        this.image.mkdir();
        this.popupImage.mkdir();
        this.smallPopupImage.mkdir();
        exportImageToFile();
        exportPopupImageToFile();
    }

    private void exportImageToFile() {
        Exporter.logger.info("Rendering relic image to " + this.image.absolute);
        // Render to a png
        ExportHelper.renderSpriteBatchToPNG(0.f, 0.f, 256.f, 256.f, 1.0f, this.image.absolute, (SpriteBatch sb) -> {
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.33f));
            sb.draw(this.relic.outlineImg, 64.0f, 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, 2.0f, 2.0f, 0.0f, 0, 0, 128, 128, false, false);
            sb.setColor(Color.WHITE);
            if (relic.largeImg == null || relic.largeImg.getWidth() < 256) {
                sb.draw(relic.img,      64.0f, 64.0f, 64.0f, 64.0f, 128.0f, 128.0f, 2.0f, 2.0f, 0.0f, 0, 0, 128, 128, false, false);
            } else {
                sb.draw(relic.largeImg, 0.0f, 0.0f, 128.0f, 128.0f, 256.0f, 256.0f, 1.0f, 1.0f, 0.0f, 0, 0, 256, 256, false, false);
            }
        });
    }

    private void exportPopupImageToFile() {
        Exporter.logger.info("Rendering relic popup image to " + this.popupImage.absolute);
        // See SingleRelicViewPopup.generateRarityLabel and generateFrameImg and renderRarity
        final float DESC_LINE_SPACING = 30.0f * Settings.scale;
        final float DESC_LINE_WIDTH = 418.0f * Settings.scale;
        final float RELIC_OFFSET_Y = 76.0f * Settings.scale;
        String rarityLabel;
        Texture relicFrameImg;
        Color tmpColor;
        switch (relic.tier) {
            case BOSS: {
                rarityLabel = SingleRelicViewPopup.TEXT[0];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameBoss.png");
                tmpColor = Settings.RED_TEXT_COLOR;
                break;
            }
            case COMMON: {
                rarityLabel = SingleRelicViewPopup.TEXT[1];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
                tmpColor = Settings.GOLD_COLOR;
                break;
            }
            case DEPRECATED: {
                rarityLabel = SingleRelicViewPopup.TEXT[2];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
                tmpColor = Settings.BLUE_TEXT_COLOR;
                break;
            }
            case RARE: {
                rarityLabel = SingleRelicViewPopup.TEXT[3];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameRare.png");
                tmpColor = Settings.CREAM_COLOR;
                break;
            }
            case SHOP: {
                rarityLabel = SingleRelicViewPopup.TEXT[4];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameRare.png");
                tmpColor = Settings.CREAM_COLOR;
                break;
            }
            case SPECIAL: {
                rarityLabel = SingleRelicViewPopup.TEXT[5];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameRare.png");
                tmpColor = Settings.GOLD_COLOR;
                break;
            }
            case STARTER: {
                rarityLabel = SingleRelicViewPopup.TEXT[6];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameCommon.png");
                tmpColor = Settings.GOLD_COLOR;
                break;
            }
            case UNCOMMON: {
                rarityLabel = SingleRelicViewPopup.TEXT[7];
                relicFrameImg = ImageMaster.loadImage("images/ui/relicFrameUncommon.png");
                tmpColor = Settings.CREAM_COLOR;
                break;
            }
            default: {
                return;
            }
        }
        // Render to a png
        float width = 640.0f, height = 810.0f;
        float x = (Settings.WIDTH - width) * 0.5f;
        float y = (Settings.HEIGHT - height) * 0.5f - 20.0f;
        float xpadding = 0.0f;
        float ypadding = 0.0f;
        ExportHelper.renderSpriteBatchToPixmap(x-xpadding, y-ypadding, width+2*xpadding, height+2*ypadding, 1.0f, (SpriteBatch sb) -> {
            // renderPopupBg
            sb.setColor(Color.WHITE);
            sb.draw(ImageMaster.RELIC_POPUP, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 540.0f, 960.0f, 540.0f, 1920.0f, 1080.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1080, false, false);
            // renderFrame
            sb.draw(relicFrameImg, (float)Settings.WIDTH / 2.0f - 960.0f, (float)Settings.HEIGHT / 2.0f - 540.0f, 960.0f, 540.0f, 1920.0f, 1080.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 1920, 1080, false, false);
            // renderRelicImage
            sb.setColor(new Color(0.0f, 0.0f, 0.0f, 0.5f));
            sb.draw(this.relic.outlineImg, (float)Settings.WIDTH / 2.0f - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f + RELIC_OFFSET_Y, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 128, 128, false, false);
            sb.setColor(Color.WHITE);
            if (relic.largeImg == null || relic.largeImg.getWidth() < 256) {
                sb.draw(this.relic.img, (float)Settings.WIDTH / 2.0f - 64.0f, (float)Settings.HEIGHT / 2.0f - 64.0f + RELIC_OFFSET_Y, 64.0f, 64.0f, 128.0f, 128.0f, Settings.scale * 2.0f, Settings.scale * 2.0f, 0.0f, 0, 0, 128, 128, false, false);
            } else {
                sb.draw(relic.largeImg, (float)Settings.WIDTH / 2.0f - 128.0f, (float)Settings.HEIGHT / 2.0f - 128.0f + RELIC_OFFSET_Y, 128.0f, 128.0f, 256.0f, 256.0f, Settings.scale, Settings.scale, 0.0f, 0, 0, 256, 256, false, false);
            }
            // renderName
            FontHelper.renderWrappedText(sb, FontHelper.SCP_cardDescFont, this.relic.name, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 280.0f * Settings.scale, 9999.0f, Settings.CREAM_COLOR, 1.0f);
            // renderRarity
            FontHelper.renderWrappedText(sb, FontHelper.cardDescFont_N, rarityLabel + SingleRelicViewPopup.TEXT[10], (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f + 240.0f * Settings.scale, 9999.0f, tmpColor, 1.0f);
            // renderDescription
            FontHelper.renderSmartText(sb, FontHelper.cardDescFont_N, this.relic.description, (float)Settings.WIDTH / 2.0f - 200.0f * Settings.scale, (float)Settings.HEIGHT / 2.0f - 140.0f * Settings.scale - FontHelper.getSmartHeight(FontHelper.cardDescFont_N, this.relic.description, DESC_LINE_WIDTH, DESC_LINE_SPACING) / 2.0f, DESC_LINE_WIDTH, DESC_LINE_SPACING, Settings.CREAM_COLOR);
            // renderQuote
            FontHelper.renderWrappedText(sb, FontHelper.SRV_quoteFont, this.relic.flavorText, (float)Settings.WIDTH / 2.0f, (float)Settings.HEIGHT / 2.0f - 310.0f * Settings.scale, DESC_LINE_WIDTH, Settings.CREAM_COLOR, 1.0f);
        }, (Pixmap pixmap) -> {
            PixmapIO.writePNG(Gdx.files.local(this.popupImage.absolute), pixmap);
            Pixmap smallPixmap = ExportHelper.resizePixmap(pixmap, Math.round(width/2), Math.round(height/2));
            PixmapIO.writePNG(Gdx.files.local(this.smallPopupImage.absolute), smallPixmap);
            smallPixmap.dispose();
        });
    }

    // Note: We can't use SingleRelicViewPopup, because that plays a sound.
    private void exportImageToFileWithAnoyingSound() {
        Exporter.logger.info("Rendering relic image to " + this.image.absolute);
        // Make relic seen
        SingleRelicViewPopup popup = CardCrawlGame.relicPopup;
        popup.open(relic);
        relic.isSeen = true;
        // Render to a png
        float width = 590.0f, height = 768.0f;
        float x = (Settings.WIDTH - width) * 0.5f;
        float y = (Settings.HEIGHT - height) * 0.5f;
        float xpadding = 0.0f;
        float ypadding = 0.0f;
        ExportHelper.renderSpriteBatchToPNG(x-xpadding, y-ypadding, width+2*xpadding, height+2*ypadding, 1.0f, this.image.absolute, (SpriteBatch sb) -> {
            popup.render(sb);
        });
        popup.close();
    }

    public static String smartTextToHTML(String string, boolean smart, boolean markup) {
        return parseSmartText(string,smart,markup,true);
    }

    public static String smartTextToPlain(String string, boolean smart, boolean markup) {
        return parseSmartText(string,smart,markup,false);
    }

    // Parse "smart text" from FontHelper into plain text or html
    // @param smart FontHelper.renderSmartText escapes
    // @param markup GlyphLayout color markup
    // @param html HTML or plain text output
    public static String parseSmartText(String string, boolean smart, boolean markup, boolean html) {
        if (string == null) return "";
        StringBuilder out = new StringBuilder();
        boolean space = false; // should we insert a space?
        boolean wordStart = true;
        int wordTags = 0; // number of tags that close at the end of the word
        int openTags = 0;
        for (int pos = 0; pos < string.length();) {
            char c = string.charAt(pos);
            if (c == ' ') {
                while (wordTags > 0) {
                    wordTags--; openTags--;
                    if (html) out.append("</span>");
                }
                pos++;
                wordStart = true;
                space = true;
            } else if (c == '\n' || c == '\t') {
                while (wordTags > 0) {
                    wordTags--; openTags--;
                    if (html) out.append("</span>");
                }
                pos++;
                wordStart = true;
                space = false;
            } else if (smart && wordStart && string.startsWith("NL ",pos)) {
                out.append('\n');
                pos += 3;
                space = false;
            } else if (smart && wordStart && string.startsWith("TAB ",pos)) {
                out.append('\t');
                pos += 4;
                space = false;
            } else if (smart && wordStart && c == '#' && pos+1 < string.length()) {
                if (space) {
                    out.append(' ');
                    space = false;
                }
                if (html) {
                    out.append("<span class=\"color-");
                    out.append(string.charAt(pos+1));
                    out.append("\">");
                }
                pos += 2;
                openTags++;
                wordTags++;
            } else if (markup && c == '[' && pos+1 < string.length() && string.charAt(pos+1) == '[') {
                // escaped [
                if (space) {
                    out.append(' ');
                    space = false;
                }
                wordStart = false;
                out.append(c);
                pos += 2;
            } else if (markup && c == '[' && pos + 1 < string.length()) {
                if (space) {
                    out.append(' ');
                    space = false;
                }
                int end = string.indexOf(']',pos);
                if (end == -1 || (end == pos+1 && openTags == 0) || (end == pos+2)) {
                    // no closing bracket, or an energy orb like [R]
                    wordStart = false;
                    out.append(c);
                    pos++;
                } else if (end == pos+1) {
                    if (openTags > 0) {
                        if (wordTags > 0) wordTags--;
                        openTags--;
                        if (html) out.append("</span>");
                    }
                    pos = end + 1;
                } else {
                    String colorName = string.substring(pos+1,end);
                    if (colorName.charAt(0) != '#' && Colors.get(colorName) == null) {
                        // not a valid color, ignore
                        wordStart = false;
                        out.append(c);
                        pos++;
                    } else {
                        if (html) {
                            out.append("<span style=\"color:");
                            out.append(colorName);
                            out.append("\">");
                        }
                        openTags++;
                        if (smart || wordTags > 0) {
                            // note: FontHelper uses separate calls to GlyphLayout.setText, so each word is rendered independently, as a result all tags end at word boundaries
                            // note2: If we are already using wordTags, then also close this tag at the end of the word
                            wordTags++;
                        }
                        pos = end + 1;
                    }
                }
            } else {
                if (space) {
                    out.append(' ');
                    space = false;
                }
                wordStart = false;
                if (html && c == '<') {
                    out.append("&lt;");
                } else if (html && c == '>') {
                    out.append("&gt;");
                } else if (html && c == '&') {
                    out.append("&amp;");
                } else {
                    out.append(c);
                }
                pos++;
            }
        }
        while (openTags > 0) {
            openTags--;
            if (html) out.append("</span>");
        }
        return out.toString();
    }


    public static ArrayList<RelicExportData> exportAllRelics(ExportHelper export) {
        ArrayList<RelicExportData> relics = new ArrayList<>();
        @SuppressWarnings("unchecked")
        HashMap<String,AbstractRelic> sharedRelics = (HashMap<String,AbstractRelic>)ReflectionHacks.getPrivateStatic(RelicLibrary.class, "sharedRelics");
        for (AbstractRelic relic : sharedRelics.values()) {
            relics.add(new RelicExportData(export, relic, null));
        }
        for (AbstractRelic relic : RelicLibrary.redList) {
            relics.add(new RelicExportData(export, relic, AbstractCard.CardColor.RED));
        }
        for (AbstractRelic relic : RelicLibrary.greenList) {
            relics.add(new RelicExportData(export, relic, AbstractCard.CardColor.GREEN));
        }
        for (AbstractRelic relic : RelicLibrary.blueList) {
            relics.add(new RelicExportData(export, relic, AbstractCard.CardColor.BLUE));
        }
        for (AbstractRelic relic : RelicLibrary.whiteList) {
            relics.add(new RelicExportData(export, relic, AbstractCard.CardColor.PURPLE));
        }
        for (HashMap.Entry<AbstractCard.CardColor,HashMap<String,AbstractRelic>> entry : BaseMod.getAllCustomRelics().entrySet()) {
            for (AbstractRelic relic : entry.getValue().values()) {
                relics.add(new RelicExportData(export, relic, entry.getKey()));
            }
        }
        Collections.sort(relics);
        return relics;
    }

    @Override
    public int compareTo(RelicExportData that) {
        if (relic.tier != that.relic.tier) return relic.tier.compareTo(that.relic.tier);
        return name.compareTo(that.name);
    }
}