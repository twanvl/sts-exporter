package sts_exporter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.characters.Ironclad;
import com.megacrit.cardcrawl.characters.TheSilent;
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
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.monsters.exordium.Lagavulin;

import basemod.BaseMod;
import basemod.ReflectionHacks;

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
        float scale = 1.0f;
        float xpadding = 100.0f;
        float ypadding = 50.0f;
        int width  = Math.round((creature.hb.width  + 2*xpadding) * scale);
        int height = Math.round((creature.hb.height + 2*ypadding) * scale);
        // Render to a png
        CardExportData.renderSpriteBatchToPNG(creature.hb.x-xpadding,creature.hb.y-ypadding, creature.hb.width+2*xpadding,creature.hb.height+2*ypadding, width,height, imageFile, (SpriteBatch sb) -> {
            // Note: the normal render code uses a PolygonSpriteBatch, which resets the projection matrix from our SpriteBatch
            // Therefore we copy the essential rendering code here.
            // This also saves us from initializing AbstractDungeon fields that are accessed, and doing work to disable health bars etc.
            // Get the creature's skeleton
            Skeleton skeleton = (Skeleton)ReflectionHacks.getPrivate(creature, AbstractCreature.class, "skeleton");
            // Directly use the creature's skeleton
            if (skeleton != null) {
                if (creature.state != null) {
                    creature.state.update(Gdx.graphics.getDeltaTime());
                    creature.state.apply(skeleton);
                }
                skeleton.updateWorldTransform();
                skeleton.setPosition(creature.drawX, creature.drawY);
                skeleton.setColor(creature.tint.color);
                PolygonSpriteBatch psb = new PolygonSpriteBatch();
                psb.setProjectionMatrix(sb.getProjectionMatrix());
                psb.begin();
                AbstractCreature.sr.draw(psb, skeleton);
                psb.end();
                psb.dispose();
            } else if (creature instanceof AbstractMonster) {
                // draw image
                AbstractMonster monster = (AbstractMonster)creature;
                Texture img = (Texture)ReflectionHacks.getPrivate(monster, AbstractMonster.class, "img");
                if (img != null) {
                    sb.setColor(creature.tint.color);
                    sb.draw(img, creature.drawX - (float)img.getWidth() * Settings.scale / 2.0f, creature.drawY, (float)img.getWidth() * Settings.scale, (float)img.getHeight() * Settings.scale, 0, 0, img.getWidth(), img.getHeight(), false, false);
                }
            }
        });
    }

    public static ArrayList<AbstractCreature> getAllCreatures() {
        // We need to initialize DailyMods before creating AbstractPlayers
        DailyMods.setModsFalse();
        // We need to initialize the random seeds before creating AbstractMonsters (for AbstractDungeon.monsterHpRng among others)
        Settings.seed = new Long(12345);
        AbstractDungeon.generateSeeds();

        // Get all player characters
        ArrayList<AbstractCreature> creatures = new ArrayList<>();
        try {
            Method createCharacter = CardCrawlGame.class.getDeclaredMethod("createCharacter");
            createCharacter.setAccessible(true);
            for (AbstractPlayer.PlayerClass playerClass : AbstractPlayer.PlayerClass.values()) {
                AbstractPlayer p = (AbstractPlayer)createCharacter.invoke(null, "PlayerName", playerClass);
                p.name = p.title;
                creatures.add(p);
            }
        } catch (Exception e) {
            Exporter.logger.error("Exception occured when getting createCharacter method", e);
            // use basemod and hardcoded list instead.
            creatures.add(new Ironclad("Ironclad", AbstractPlayer.PlayerClass.IRONCLAD));
            creatures.add(new TheSilent("The Silent", AbstractPlayer.PlayerClass.THE_SILENT));
            try {
                for (AbstractPlayer.PlayerClass playerClass : AbstractPlayer.PlayerClass.values()) {
                    AbstractPlayer p = BaseMod.createCharacter(playerClass.toString(), "PlayerName");
                    p.name = p.title;
                    creatures.add(p);
                }
            } catch (Exception e2) {
                Exporter.logger.error("Backup strategy of using BaseMod createCharacter also failed", e2);
            }
        }

        // Now get all monsters
        // There is unfortunately no list of all monsters in the game. The best we can do use use MonsterHelper.getMonster
        for (String name : getAllMonsterNames()) {
            creatures.add(MonsterHelper.getMonster(name));
        }
        // or MonsterHelper.getEncounter
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
        creatures.add(lagavulin);

        return creatures;
    }

    public static ArrayList<String> getAllMonsterNames() {
        // these don't appear in encounters
        ArrayList<String> out = new ArrayList<>();
        out.add("BronzeOrb");
        out.add("TorchHead");
        return out;
    }

    public static Collection<String> getAllEncounterNames() {
        // copied from MonsterHelper.uploadEnemyData
        ArrayList<EnemyData> data = new ArrayList<EnemyData>();
        data.add(new EnemyData("Blue Slaver", 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Cultist", 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Jaw Worm", 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("2 Louse", 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Small Slimes", 1, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Gremlin Gang", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Large Slime", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Looter", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Lots of Slimes", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Exordium Thugs", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Exordium Wildlife", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Red Slaver", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("3 Louse", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("2 Fungi Beasts", 1, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Gremlin Nob", 1, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Lagavulin", 1, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("3 Sentries", 1, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Lagavulin Event", 1, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData("Mushrooms", 1, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData("The Guardian", 1, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Hexaghost", 1, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Slime Boss", 1, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Chosen", 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Shell Parasite", 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Spheric Guardian", 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("3 Byrds", 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("2 Thieves", 2, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Chosen and Byrds", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Sentry and Sphere", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Slaver and Parasite", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Snake Plant", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Snecko", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Centurion and Healer", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Cultist and Chosen", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("3 Cultists", 2, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Gremlin Leader", 2, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Slavers", 2, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Book of Stabbing", 2, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Masked Bandits", 2, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData("Automaton", 2, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Champ", 2, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Collector", 2, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Flame Bruiser 1 Orb", 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Orb Walker", 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("3 Darklings", 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("3 Shapes", 3, EnemyData.MonsterType.WEAK));
        data.add(new EnemyData("Flame Bruiser 2 Orb", 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("4 Shapes", 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Maw", 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Puppeteer", 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Sphere and 2 Shapes", 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Spire Growth", 3, EnemyData.MonsterType.STRONG));
        data.add(new EnemyData("Giant Head", 3, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Nemesis", 3, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("2 Orb Walkers", 3, EnemyData.MonsterType.ELITE));
        data.add(new EnemyData("Mysterious Sphere", 3, EnemyData.MonsterType.EVENT));
        data.add(new EnemyData("Awakened One", 3, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Donu and Deca", 3, EnemyData.MonsterType.BOSS));
        data.add(new EnemyData("Time Eater", 3, EnemyData.MonsterType.BOSS));
        ArrayList<String> encounters = new ArrayList<String>();
        for (EnemyData d : data) {
            encounters.add(d.name);
        }
        return encounters;
    }

    public static Collection<String> getAllEncounterNamesFromDungeons() {
        // Hacky way to get all monster names
        // This unfortunately breaks the game somewhat
        HashSet<String> encounterNames = new HashSet<>();
        AbstractPlayer p = new Ironclad("Exporter", AbstractPlayer.PlayerClass.IRONCLAD);
        ArrayList<String> emptyList = new ArrayList<String>();
        getAllEncounterNames(encounterNames, new Exordium(p, emptyList));
        getAllEncounterNames(encounterNames, new TheCity(p, emptyList));
        getAllEncounterNames(encounterNames, new TheBeyond(p, emptyList));
        // Better to hard-code encounter lists
        encounterNames.add("Spheric Guardian");
        encounterNames.add("Chosen");
        encounterNames.add("Shell Parasite");
        encounterNames.add("3 Byrds");
        encounterNames.add("2 Thieves");
        encounterNames.add("Chosen and Byrds");
        encounterNames.add("Sentry and Sphere");
        encounterNames.add("Slaver and Parasite");
        encounterNames.add("Snake Plant");
        encounterNames.add("Snecko");
        encounterNames.add("Centurion and Healer");
        encounterNames.add("Cultist and Chosen");
        encounterNames.add("3 Cultists");
        encounterNames.add("Gremlin Leader");
        encounterNames.add("Slavers");
        encounterNames.add("Book of Stabbing");
        // bosses might need unlocks
        encounterNames.add("The Guardian");
        encounterNames.add("Hexaghost");
        encounterNames.add("Slime Boss");
        encounterNames.add("Awakened One");
        encounterNames.add("Time Eater");
        encounterNames.add("Donu and Deca");
        encounterNames.add("Automaton");
        encounterNames.add("Collector");
        encounterNames.add("Champ");
        // clean up a bit
        return encounterNames;
    }
    private static void getAllEncounterNames(HashSet<String> encounterNames, AbstractDungeon dungeon) {
        // there is some randomness in the encounter generation, so just do a couple of loops to get them all
        for (int i = 0 ; i < 10 ; i++) {
            try {
                Method method = AbstractDungeon.class.getDeclaredMethod("generateMonsters");
                method.setAccessible(true);
                method.invoke(dungeon);
            } catch (Exception e) {
                Exporter.logger.error("Exception occured when calling generateMonsters", e);
            }
            for (String e : AbstractDungeon.bossList) encounterNames.add(e);
            for (String e : AbstractDungeon.monsterList) encounterNames.add(e);
            for (String e : AbstractDungeon.eliteMonsterList) encounterNames.add(e);
        }
    }
}