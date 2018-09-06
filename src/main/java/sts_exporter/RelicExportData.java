package sts_exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
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
    public String image, absImage, relImage;
    public String popupImage, absPopupImage, relPopupImage;
    public String smallPopupImage, absSmallPopupImage, relSmallPopupImage;
    public String name, description, descriptionHTML, flavorText;

    RelicExportData(AbstractRelic relic, AbstractCard.CardColor pool, String imageDir) {
        this.relic = relic;
        this.mod = Exporter.findMod(relic.getClass());
        this.mod.relics.add(this);
        this.name = relic.name;
        this.description = relic.description;
        this.descriptionHTML = smartTextToHTML(relic.description);
        this.flavorText = relic.flavorText;
        this.tier = Exporter.tierName(relic.tier);
        this.pool = pool == null ? "" : Exporter.colorName(pool);
        // Render image
        exportImageToDir(imageDir);
    }

    private void exportImageToDir(String imageDir) {
        String safename = relic.relicId;
        safename = safename.replace(" ","");
        safename = safename.replace("/","");
        this.image = safename + ".png";
        this.absImage = imageDir + "/" + this.image;
        this.relImage = "relics/" + this.image;
        exportImageToFile(this.absImage);
        this.popupImage = safename + "-popup.png";
        this.absPopupImage = imageDir + "/" + this.popupImage;
        this.relPopupImage = "relics/" + this.popupImage;
        this.smallPopupImage = safename + "-small-popup.png";
        this.absSmallPopupImage = imageDir + "/" + this.smallPopupImage;
        this.relSmallPopupImage = "relics/" + this.smallPopupImage;
        exportPopupImageToFile(this.absPopupImage, this.absSmallPopupImage);
    }

    private void exportImageToFile(String imageFile) {
        Exporter.logger.info("Rendering relic image to " + imageFile);
        // Render to a png
        CardExportData.renderSpriteBatchToPNG(0.f, 0.f, 256.f, 256.f, 1.0f, imageFile, (SpriteBatch sb) -> {
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

    private void exportPopupImageToFile(String imageFile, String smallImageFile) {
        Exporter.logger.info("Rendering relic popup image to " + imageFile);
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
        CardExportData.renderSpriteBatchToPixmap(x-xpadding, y-ypadding, width+2*xpadding, height+2*ypadding, 1.0f, (SpriteBatch sb) -> {
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
            PixmapIO.writePNG(Gdx.files.local(imageFile), pixmap);
            Pixmap smallPixmap = CardExportData.resizePixmap(pixmap, Math.round(width/2), Math.round(height/2));
            PixmapIO.writePNG(Gdx.files.local(smallImageFile), smallPixmap);
            smallPixmap.dispose();
        });
    }

    // Note: We can't use SingleRelicViewPopup, because that plays a sound.
    private void exportImageToFileWithAnoyingSound(String imageFile) {
        Exporter.logger.info("Rendering relic image to " + imageFile);
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
        CardExportData.renderSpriteBatchToPNG(x-xpadding, y-ypadding, width+2*xpadding, height+2*ypadding, 1.0f, imageFile, (SpriteBatch sb) -> {
            popup.render(sb);
        });
        popup.close();
    }

    public static String smartTextToHTML(String string) {
        if (string == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        Scanner s = new Scanner(string);
        while (s.hasNext()) {
            String word = s.next();
            if (word.equals("NL")) {
                out.append("<br>\n");
            } else if (word.equals("TAB")) {
                out.append("\t");
                continue;
            } else if (word.charAt(0) == '#') {
                out.append("<span class=\"color-");
                out.append(word.charAt(1));
                out.append("\">");
                out.append(word.substring(2, word.length()));
                out.append("</span> ");
            } else {
                // TODO: do something with orbs?
                out.append(word);
                out.append(" ");
            }
        }
        s.close();
        return out.toString();
    }


    public static ArrayList<RelicExportData> exportAllRelics(String imagedir) {
        Exporter.mkdir(imagedir);
        ArrayList<RelicExportData> relics = new ArrayList<>();
        HashMap<String,AbstractRelic> sharedRelics = (HashMap<String,AbstractRelic>)ReflectionHacks.getPrivateStatic(RelicLibrary.class, "sharedRelics");
        for (AbstractRelic relic : sharedRelics.values()) {
            relics.add(new RelicExportData(relic, null, imagedir));
        }
        for (AbstractRelic relic : RelicLibrary.redList) {
            relics.add(new RelicExportData(relic, AbstractCard.CardColor.RED, imagedir));
        }
        for (AbstractRelic relic : RelicLibrary.greenList) {
            relics.add(new RelicExportData(relic, AbstractCard.CardColor.GREEN, imagedir));
        }
        for (AbstractRelic relic : RelicLibrary.blueList) {
            relics.add(new RelicExportData(relic, AbstractCard.CardColor.BLUE, imagedir));
        }
        for (HashMap.Entry<AbstractCard.CardColor,HashMap<String,AbstractRelic>> entry : BaseMod.getAllCustomRelics().entrySet()) {
            for (AbstractRelic relic : entry.getValue().values()) {
                relics.add(new RelicExportData(relic, entry.getKey(), imagedir));
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