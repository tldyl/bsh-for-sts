package demoMod.bshForSts.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;

@SuppressWarnings("unused")
public class CardCrawlGamePatch {
    public static CardCrawlGame gameInstance;

    @SpirePatch(
            clz = CardCrawlGame.class,
            method = SpirePatch.CONSTRUCTOR
    )
    public static class PatchConstructor {
        public static void Postfix(CardCrawlGame game, String prefDir) {
            gameInstance = game;
        }
    }
}
