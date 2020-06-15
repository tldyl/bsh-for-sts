package demoMod.bshForSts.actions;

import com.megacrit.cardcrawl.actions.AbstractGameAction;

import java.util.function.Consumer;

public class ExecuteCommandAction extends AbstractGameAction {
    private Consumer<Void> consumer;

    public ExecuteCommandAction(Consumer<Void> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void update() {
        consumer.accept(null);
        this.isDone = true;
    }
}
