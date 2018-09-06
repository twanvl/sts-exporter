package sts_exporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.helpers.PotionHelper;
import com.megacrit.cardcrawl.potions.AbstractPotion;

class PotionExportData implements Comparable<PotionExportData> {
    public AbstractPotion potion;
    public ModExportData mod;
    public String image, absImage, relImage;
    public String id, name, rarity, description, descriptionHTML;
    public String playerClass;

    PotionExportData(AbstractPotion potion, AbstractPlayer.PlayerClass cls, String imageDir) {
        this.potion = potion;
        this.mod = Exporter.findMod(potion.getClass());
        this.mod.potions.add(this);
        this.id = potion.ID;
        this.name = potion.name;
        this.description = potion.description;
        this.descriptionHTML = RelicExportData.smartTextToHTML(potion.description);
        this.rarity = Exporter.rarityName(potion.rarity);
        this.playerClass = playerClass == null ? "" : playerClass.toString();
        // Render image
        exportImageToDir(imageDir);
    }

    private void exportImageToDir(String imageDir) {
        String safename = id;
        safename = safename.replace(" ","");
        safename = safename.replace("/","");
        this.image = safename + ".png";
        this.absImage = imageDir + "/" + safename + ".png";
        this.relImage = "potions/" + safename + ".png";
        exportImageToFile(this.absImage);
    }

    // Note: We can't use SingleRelicViewPopup, because that plays a sound.
    private void exportImageToFile(String imageFile) {
        Exporter.logger.info("Rendering potion image to " + imageFile);
        // Render to a png
        potion.move(32.0f,32.0f);
        float width = 64.0f, height = 64.0f;
        float x = 0;
        float y = 0;
        float xpadding = 0.0f;
        float ypadding = 0.0f;
        CardExportData.renderSpriteBatchToPNG(x-xpadding, y-ypadding, width+2*xpadding, height+2*ypadding, 1.0f, imageFile, (SpriteBatch sb) -> {
            potion.render(sb);
        });
    }

    public static ArrayList<PotionExportData> exportAllPotions(String outdir) {
        Exporter.mkdir(outdir);
        ArrayList<PotionExportData> potions = new ArrayList<>();
        for (HashMap.Entry<String,AbstractPlayer.PlayerClass> potionID : getAllPotionIds().entrySet()) {
            potions.add(new PotionExportData(PotionHelper.getPotion(potionID.getKey()), potionID.getValue(), outdir));
        }
        Collections.sort(potions);
        return potions;
    }

    public static HashMap<String,AbstractPlayer.PlayerClass> getAllPotionIds() {
        HashMap<String,AbstractPlayer.PlayerClass> potions = new HashMap<>();
        for (AbstractPlayer.PlayerClass playerClass : AbstractPlayer.PlayerClass.values()) {
            PotionHelper.initialize(playerClass);
            for (String potionID : PotionHelper.potions) {
                if (potions.containsKey(potionID)) {
                    potions.put(potionID, null);
                } else {
                    potions.put(potionID, playerClass);
                }
            }
        }
        return potions;
    }

    @Override
    public int compareTo(PotionExportData that) {
        if (potion.rarity != that.potion.rarity) return potion.rarity.compareTo(that.potion.rarity);
        return name.compareTo(that.name);
    }
}