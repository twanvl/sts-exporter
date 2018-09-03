package sts_exporter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.daily.DailyMods;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.dungeons.Exordium;
import com.megacrit.cardcrawl.dungeons.TheBeyond;
import com.megacrit.cardcrawl.dungeons.TheCity;
import com.megacrit.cardcrawl.helpers.EnemyData;
import com.megacrit.cardcrawl.helpers.MonsterHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;
import com.megacrit.cardcrawl.rooms.EmptyRoom;

import basemod.BaseMod;

public class CreatureExportData {
    public AbstractCreature creature;
    public String image, relImage;
    public String name;

    public CreatureExportData(AbstractCreature creature, String imageDir) {
        this.creature = creature;
        this.name = creature.name;
        exportImageToDir(imageDir);
    }

    private void exportImageToDir(String imageDir) {
        String safename = creature.getClass().getSimpleName();
        if (creature.id != null) {
            safename = creature.id;
        }
        safename = safename.replace(" ","");
        safename = safename.replace("/","");
        this.image = imageDir + "/" + safename + ".png";
        this.relImage = "creatures/" + safename + ".png";
        exportImageToFile(this.image);
    }

    private void exportImageToFile(String imageFile) {
        Exporter.logger.info("Rendering creature image to " + imageFile);
        // Get size of the creature.
        // We could use the hitbox, but that is not guaranteed to actually contain the whole image.
        // For now, just add a lot of padding.
        float scale = 1.0f / Settings.scale;
        float xpadding = 90.0f;
        float ypadding = 40.0f;
        // Render to a png
        CardExportData.renderSpriteBatchToPNG(creature.hb.x-xpadding,creature.hb.y-ypadding, creature.hb.width+2*xpadding,creature.hb.height+2*ypadding, scale, imageFile, (SpriteBatch sb) -> {
            // use AbstractCreature.render()
            // Note: the normal render code uses a PolygonSpriteBatch CardCrawlGame.psb, so make sure the projection is the same
            Matrix4 oldProjection = CardCrawlGame.psb.getProjectionMatrix();
            CardCrawlGame.psb.setProjectionMatrix(sb.getProjectionMatrix());
            boolean oldHideCombatElements = Settings.hideCombatElements;
            Settings.hideCombatElements = true; // don't render monster intent
            if (creature instanceof AbstractPlayer) {
                ((AbstractPlayer)creature).renderPlayerImage(sb);
            } else {
                creature.render(sb);
            }
            // cleanup
            CardCrawlGame.psb.setProjectionMatrix(oldProjection);
            Settings.hideCombatElements = oldHideCombatElements;
        });
    }

    public static ArrayList<AbstractCreature> getAllCreatures() {
        // We need to initialize DailyMods before creating AbstractPlayers
        DailyMods.setModsFalse();
        // We need to initialize the random seeds before creating AbstractMonsters (for AbstractDungeon.monsterHpRng among others)
        Settings.seed = new Long(12345);
        AbstractDungeon.generateSeeds();

        // For rendering monsters we need:
        AbstractDungeon.player = new Ironclad("Ironclad", AbstractPlayer.PlayerClass.IRONCLAD);
        AbstractDungeon.player.isDead = true; // don't render monster health bars
        AbstractDungeon.currMapNode = new MapRoomNode(0, -1);
        AbstractDungeon.currMapNode.room = new EmptyRoom();
        AbstractDungeon.currMapNode.room.monsters = new MonsterGroup(new AbstractMonster[0]); // needed to render monsters

        // Get all player characters
        ArrayList<AbstractCreature> creatures = new ArrayList<>();
        try {
            Method createCharacter = CardCrawlGame.class.getDeclaredMethod("createCharacter", AbstractPlayer.PlayerClass.class);
            createCharacter.setAccessible(true);
            for (AbstractPlayer.PlayerClass playerClass : AbstractPlayer.PlayerClass.values()) {
                if (playerClass.toString() == "CROWBOT") continue; // old version of the game
                AbstractPlayer p = (AbstractPlayer)createCharacter.invoke(null, playerClass);
                p.name = p.title;
                creatures.add(p);
            }
        } catch (Exception e) {
            Exporter.logger.error("Exception occured when getting createCharacter method", e);
        }

        // Now get all monsters
        // There is unfortunately no list of all monsters in the game. The best we can do is to use MonsterHelper.getEncounter
        HashSet<String> seenMonsters = new HashSet<>();
        for (String encounter : getAllEncounterNames()) {
            Exporter.logger.info("Getting monsters for encounter " + encounter);
            MonsterGroup monsters = MonsterHelper.getEncounter(encounter);
            for (AbstractMonster monster : monsters.monsters) {
                String id = monster.getClass().getName();
                if (seenMonsters.contains(id)) continue;
                creatures.add(monster);
                seenMonsters.add(id);
            }
        }
        // Awake lagavulin looks different
        Lagavulin lagavulin = new Lagavulin(false);
        lagavulin.id = lagavulin.id + "Awake";
        lagavulin.name = lagavulin.name + " (Awake)";
        creatures.add(lagavulin);

        return creatures;
    }

    public static Collection<String> getAllEncounterNames() {
        return BaseMod.encounterList;
    }

}