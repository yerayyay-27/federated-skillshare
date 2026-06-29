package it.unibo;

/**
 * A navigation target for a "Back" button: a label naming the destination plus
 * the action that returns there. Lets a screen show "Back to X" with the real
 * place it came from. Pure client-side navigation; never crosses RPC.
 */
public class BackTarget {

    private final String label;
    private final Runnable action;

    public BackTarget(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public String getLabel() {
        return label;
    }

    public void go() {
        if (action != null) {
            action.run();
        }
    }
}