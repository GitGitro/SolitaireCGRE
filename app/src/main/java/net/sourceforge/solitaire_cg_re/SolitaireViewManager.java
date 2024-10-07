package net.sourceforge.solitaire_cg_re;

public class SolitaireViewManager {
    private static SolitaireView instance;

    public static void setInstance(SolitaireView view) {
        instance = view;
    }

    public static SolitaireView getInstance() {
        return instance;
    }
}
