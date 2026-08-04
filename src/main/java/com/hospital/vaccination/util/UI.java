package com.hospital.vaccination.util;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class UI {

    private UI() { }

    public static final Color PRIMARY        = new Color(15,  76, 129);
    public static final Color PRIMARY_HOVER  = new Color(10,  60, 107);
    public static final Color PRIMARY_LIGHT  = new Color(232, 240, 249);

    public static final Color SIDEBAR_TOP    = new Color(15,  30,  61);
    public static final Color SIDEBAR_BOTTOM = new Color(30,  58,  95);
    public static final Color SIDEBAR_HOVER  = new Color(255, 255, 255, 22);
    public static final Color SIDEBAR_ACTIVE = new Color(255, 255, 255, 38);

    public static final Color ACCENT_GOLD    = new Color(244, 185,  66);

    public static final Color BG_APP         = new Color(244, 246, 249);
    public static final Color CARD_BG        = Color.WHITE;
    public static final Color BORDER         = new Color(223, 228, 234);
    public static final Color BORDER_FOCUS   = new Color(15,  76, 129, 120);

    public static final Color TEXT_PRIMARY   = new Color( 30,  38,  53);
    public static final Color TEXT_SECONDARY = new Color(104, 116, 134);
    public static final Color TEXT_MUTED     = new Color(148, 158, 172);
    public static final Color TEXT_ON_DARK   = Color.WHITE;
    public static final Color TEXT_ON_DARK_M = new Color(190, 205, 225);

    public static final Color SUCCESS        = new Color(  0, 168, 107);
    public static final Color SUCCESS_BG     = new Color(220, 246, 234);
    public static final Color WARN           = new Color(255, 168,  39);
    public static final Color WARN_BG        = new Color(254, 243, 220);
    public static final Color DANGER         = new Color(230,  57,  70);
    public static final Color DANGER_BG      = new Color(253, 226, 229);
    public static final Color INFO_BG        = new Color(224, 236, 247);

    public static final Color ROW_ALT        = new Color(249, 251, 253);
    public static final Color SHADOW         = new Color(15,  30,  61, 22);

    private static Font BASE_FONT;

    static {
        String[] candidates = { "Segoe UI", "SF Pro Text", "Inter", "Arial" };
        for (String name : candidates) {
            Font f = new Font(name, Font.PLAIN, 13);
            if (f.getFamily().equalsIgnoreCase(name)) { BASE_FONT = f; break; }
        }
        if (BASE_FONT == null) BASE_FONT = UIManager.getFont("Label.font");
    }

    public static Font base(float size)         { return BASE_FONT.deriveFont(size); }
    public static Font base(int style, float s) { return BASE_FONT.deriveFont(style, s); }
    public static Font bold(float size)         { return base(Font.BOLD, size); }

    public static Font icon(float size) {
        return base(Font.PLAIN, size);
    }

    public static Font h1()          { return bold(24f);  }
    public static Font h2()          { return bold(20f);  }
    public static Font h3()          { return bold(15f);  }
    public static Font body()        { return base(13f);  }
    public static Font bodyBold()    { return bold(13f);  }
    public static Font small()       { return base(11.5f);}
    public static Font caps()        { return bold(10.5f);}
    public static Font statNumber()  { return bold(28f);  }
    public static Font nav()         { return base(13.5f);}

    public static Border padding(int all)                    { return new EmptyBorder(all, all, all, all); }
    public static Border padding(int v, int h)               { return new EmptyBorder(v, h, v, h); }
    public static Border padding(int t, int r, int b, int l) { return new EmptyBorder(t, r, b, l); }

    public static void install() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        UIManager.put("Table.gridColor",              BORDER);
        UIManager.put("Table.selectionBackground",    PRIMARY_LIGHT);
        UIManager.put("Table.selectionForeground",    TEXT_PRIMARY);
        UIManager.put("Table.showHorizontalLines",    Boolean.TRUE);
        UIManager.put("Table.showVerticalLines",      Boolean.FALSE);
        UIManager.put("Table.intercellSpacing",       new Dimension(0, 0));

        UIManager.put("Panel.background",             BG_APP);
        UIManager.put("OptionPane.background",        CARD_BG);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

        UIManager.put("MenuBar.background",           CARD_BG);
        UIManager.put("MenuBar.border",               BorderFactory.createMatteBorder(0,0,1,0,BORDER));
        UIManager.put("Menu.background",              CARD_BG);
        UIManager.put("MenuItem.background",          CARD_BG);
        UIManager.put("MenuItem.selectionBackground", PRIMARY_LIGHT);
        UIManager.put("MenuItem.selectionForeground", TEXT_PRIMARY);

        UIManager.put("ToolTip.background",           new Color(30, 38, 53));
        UIManager.put("ToolTip.foreground",           Color.WHITE);
        UIManager.put("ToolTip.border",               new EmptyBorder(6, 10, 6, 10));

        UIManager.put("ScrollBar.thumb",              new Color(180, 190, 205));
        UIManager.put("ScrollBar.thumbHighlight",     new Color(160, 170, 190));
        UIManager.put("ScrollBar.thumbShadow",        new Color(160, 170, 190));
        UIManager.put("ScrollBar.track",              CARD_BG);
        UIManager.put("ScrollBar.width",              10);
    }
}