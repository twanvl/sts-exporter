package sts_exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.cards.AbstractCard.CardColor;

import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

class ExportHelper {
    ExportHelper(SpireConfig config) {
        this.dir = config.getString(Exporter.CONFIG_EXPORT_DIR);
        this.include_basegame = config.getBool(Exporter.CONFIG_INCLUDE_BASE_GAME);
    }

    // ----------------------------------------------------------------------------
    // Exporting
    // ----------------------------------------------------------------------------

    // Target directory
    String dir;
    boolean include_basegame;

    // Collect all items
    void collectAll() {
        initModList();
        // collect items
        Exporter.logger.info("Collecting items");
        CardExportData.exportAllCards(this);
        RelicExportData.exportAllRelics(this);
        CreatureExportData.exportAllCreatures(this);
        PotionExportData.exportAllPotions(this);
        this.colors = ColorExportData.exportAllColors(this);
        // collect only from included mods
        for (ModExportData mod : this.mods) {
            if (modIncludedInExport(mod)) {
                cards.addAll(mod.cards);
                relics.addAll(mod.relics);
                creatures.addAll(mod.creatures);
                potions.addAll(mod.potions);
            }
        }
        Collections.sort(this.cards);
        Collections.sort(this.relics);
        Collections.sort(this.creatures);
        Collections.sort(this.potions);
        // per color items
        for (CardExportData c : this.cards) {
            findColor(c.card.color).cards.add(c);
        }
        for (RelicExportData r : this.relics) {
            if (r.poolColor != null) findColor(r.poolColor).relics.add(r);
        }
    }

    // Export all collected items
    void exportAll() {
        exportAllTemplates();
        exportAllImages();
    }

    void exportAllImages() {
        for (ModExportData mod : mods) {
            if (modIncludedInExport(mod)) {
                mod.exportImages();
            }
        }
    }

    private static final String[] colorTemplates = {"cards.html","cards.md","cards.wiki","wiki-card-data.txt","style.css"};
    private static final String[] indexTemplates = {"index.html","wiki-card-data.txt"};
    private static final String[] commonTemplates = {"creatures.html","potions.html","relics.html","cards.html","creatures.md","potions.md","relics.md","cards.md","style.css"};
    private static final String[] modTemplates = {"index.html","index.md"};

    void exportAllTemplates() {
        for (ModExportData mod : mods) {
            if (modIncludedInExport(mod)) {
                JtwigModel model = getTwigModel(mod);
                writeTwigTemplates(model, "templates/mods", exportDir(mod), modTemplates);
                writeTwigTemplates(model, "templates", exportDir(mod), commonTemplates);
            }
        }
        for (ColorExportData color : colors) {
            JtwigModel model = getTwigModel(color);
            writeTwigTemplates(model, "templates", exportDir(color), colorTemplates);
        }
        JtwigModel model = getTwigModelAll();
        writeTwigTemplates(model, "templates", dir, indexTemplates);
        writeTwigTemplates(model, "templates", dir, commonTemplates);
    }

    String exportDir(ModExportData mod) {
        return dir + "/" + mod.id;
    }
    String exportDir(ColorExportData color) {
        return dir + "/colors/" + color.id;
    }

    ExportPath exportPath(ModExportData mod, String dir, String id, String suffix) {
        if (id.startsWith(mod.id+":")) {
            id = id.substring(mod.id.length() + 1); // strip mod ids
        }
        String file = makeFilename(id) + suffix;
        return new ExportPath(this.dir, mod.id, dir, file);
    }

    private static String makeFilename(String id) {
        id = id.replaceAll(":","-");
        id = id.replace("+","Plus");
        id = id.replace("*","Star");
        return id.replaceAll("[\\s\\\\/:*?\"\'<>|+%]", "");
    }

    // ----------------------------------------------------------------------------
    // Per mod items
    // ----------------------------------------------------------------------------

    // Per mod items
    private ArrayList<ModExportData> mods = new ArrayList<>();
    // Combined items
    public ArrayList<CardExportData> cards = new ArrayList<>();
    public ArrayList<RelicExportData> relics = new ArrayList<>();
    public ArrayList<CreatureExportData> creatures = new ArrayList<>();
    public ArrayList<PotionExportData> potions = new ArrayList<>();
    public ArrayList<ColorExportData> colors = new ArrayList<>();

    private void initModList() {
        mods.add(new ModExportData(this));
        for (ModInfo modInfo : Loader.MODINFOS) {
            mods.add(new ModExportData(this, modInfo));
        }
    }

    public ModExportData findMod(Class<?> cls) {
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

    public boolean modIncludedInExport(ModExportData mod) {
        if (mod.id.equals(ModExportData.BASE_GAME_ID)) {
            return include_basegame;
        } else {
            return true;
        }
    }

    public ColorExportData findColor(CardColor color) {
        for (ColorExportData c : this.colors) {
            if (c.color == color) return c;
        }
        return null;
    }

    // ----------------------------------------------------------------------------
    // Image exporting
    // ----------------------------------------------------------------------------

    public static void renderSpriteBatchToPNG(float x, float y, float width, float height, float scale, String imageFile, Consumer<SpriteBatch> render) {
        renderSpriteBatchToPNG(x,y,width,height, Math.round(scale*width), Math.round(scale*height), imageFile, render);
    }

    public static void renderSpriteBatchToPNG(float x, float y, float width, float height, int iwidth, int iheight, String imageFile, Consumer<SpriteBatch> render) {
        renderSpriteBatchToPixmap(x,y,width,height,iwidth,iheight,render,(Pixmap pixmap) -> PixmapIO.writePNG(Gdx.files.local(imageFile), pixmap));
    }

    public static void renderSpriteBatchToPixmap(float x, float y, float width, float height, float scale, Consumer<SpriteBatch> render, Consumer<Pixmap> write) {
        renderSpriteBatchToPixmap(x,y,width,height, Math.round(scale*width), Math.round(scale*height), render, write);
    }

    public static void renderSpriteBatchToPixmap(float x, float y, float width, float height, int iwidth, int iheight, Consumer<SpriteBatch> render, Consumer<Pixmap> write) {
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
        write.accept(pixmap);
        pixmap.dispose();
        // done
        fbo.end();
        fbo.dispose();
    }

    public static Pixmap resizePixmap(Pixmap pixmap, int width, int height) {
        Pixmap resized = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        resized.setFilter(Pixmap.Filter.BiLinear);
        resized.drawPixmap(pixmap, 0,0,pixmap.getWidth(),pixmap.getHeight(), 0,0,width,height);
        return resized;
    }

    // ----------------------------------------------------------------------------
    // Template exporting
    // ----------------------------------------------------------------------------

    private static void writeTwigTemplates(JtwigModel model, String indir, String outdir, String[] templateNames) {
        new File(outdir).mkdirs();
        for (String templateName : templateNames) {
            writeTwigTemplate(model, indir + "/" + templateName + ".twig", outdir + "/" + templateName);
        }
    }

    private static void writeTwigTemplate(JtwigModel model, String templateFile, String outFile) {
        try {
            Exporter.logger.info("Writing " + outFile);
            FileOutputStream stream = new FileOutputStream(outFile);
            JtwigTemplate template = JtwigTemplate.classpathTemplate(templateFile);
            template.render(model, stream);
            stream.close();
        } catch (IOException e) {
            Exporter.logger.error(e);
        }
    }

    private JtwigModel getTwigModel(ModExportData mod) {
        JtwigModel model = JtwigModel.newModel();
        model.with("dir",exportDir(mod));
        model.with("root","../");
        model.with("listmod",false);
        model.with("name", mod.name);
        model.with("mod", mod);
        model.with("relics",mod.relics);
        model.with("creatures",mod.creatures);
        model.with("potions",mod.potions);
        model.with("cards",mod.cards);
        return model;
    }

    private JtwigModel getTwigModel(ColorExportData color) {
        JtwigModel model = JtwigModel.newModel();
        model.with("dir",exportDir(color));
        model.with("root","../");
        model.with("listmod",true);
        model.with("name", color.name);
        model.with("color",color);
        model.with("relics",color.relics);
        model.with("cards",color.cards);
        return model;
    }

    private JtwigModel getTwigModelAll() {
        JtwigModel model = JtwigModel.newModel();
        model.with("dir",this.dir);
        model.with("root","");
        model.with("listmod",true);
        model.with("name","All");
        model.with("colors",this.colors);
        model.with("relics",this.relics);
        model.with("creatures",this.creatures);
        model.with("potions",this.potions);
        model.with("cards",this.cards);
        model.with("mods",this.mods);
        return model;
    }
}