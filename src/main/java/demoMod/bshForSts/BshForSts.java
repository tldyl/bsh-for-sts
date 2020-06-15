package demoMod.bshForSts;

import basemod.BaseMod;
import basemod.interfaces.EditStringsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.PostUpdateSubscriber;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.UIStrings;
import demoMod.bshForSts.patches.CardCrawlGamePatch;
import demoMod.bshForSts.ui.CommandWindow;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Vector;

@SuppressWarnings("unused")
@SpireInitializer
public class BshForSts implements PostInitializeSubscriber,
                                  EditStringsSubscriber,
                                  PostUpdateSubscriber {
    public static CommandWindow commandWindow;
    public static String lastLoadedFilePath = "";
    public static final List<AbstractGameAction> actionList = new Vector<>();

    public static void main(String args[]) {}

    public static void initialize() {
        new BshForSts();
    }

    public BshForSts() {
        BaseMod.subscribe(this);
    }

    @Override
    public void receivePostInitialize() {
        loadSettings();
        SpriteBatch sb = null;
        try {
            Field field = CardCrawlGame.class.getDeclaredField("sb");
            field.setAccessible(true);
            sb = (SpriteBatch) field.get(CardCrawlGamePatch.gameInstance);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        CommandWindow commandWindow = new CommandWindow();
        commandWindow.setSb(sb);
        commandWindow.setVisible(true);
    }

    public static void loadSettings() {
        try {
            SpireConfig config = new SpireConfig("BshForSts", "settings");
            config.load();
            if (config.has(makeID("LastLoadedFilePath"))) {
                lastLoadedFilePath = config.getString(makeID("LastLoadedFilePath"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveSettings() {
        try {
            SpireConfig config = new SpireConfig("BshForSts", "settings");
            if (lastLoadedFilePath != null) config.setString(makeID("LastLoadedFilePath"), lastLoadedFilePath);
            config.save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String makeID(String name) {
        return "BshForSts:" + name;
    }

    @Override
    public void receiveEditStrings() {
        String language;
        switch (Settings.language) {
            case ZHS:
                language = "zhs";
                break;
            default:
                language = "eng";
                break;
        }
        String uiStrings;
        if (!language.equals("eng")) {
            uiStrings = Gdx.files.internal("localization/" + language + "/BshForSts-UIStrings.json").readString(String.valueOf(StandardCharsets.UTF_8));
            BaseMod.loadCustomStrings(UIStrings.class, uiStrings);
        }
    }

    @Override
    public void receivePostUpdate() {
        if (actionList.size() > 0) {
            AbstractGameAction action = actionList.get(0);
            if (!action.isDone) {
                action.update();
            } else {
                actionList.remove(0);
            }
        }
    }
}
