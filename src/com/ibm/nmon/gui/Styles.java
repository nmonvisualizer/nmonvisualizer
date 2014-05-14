package com.ibm.nmon.gui;

import java.util.Map;

import java.awt.Color;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.awt.BasicStroke;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import java.text.DecimalFormat;

/**
 * Holder object for various styles and icons used throughout the GUI.
 */
@SuppressWarnings("unchecked")
public final class Styles {
    // not storing SimpleDateFormats / NumberFormats here to avoid threading issues
    // each class using these strings should create its own format instance
    public static final String DATE_FORMAT_STRING = "HH:mm:ss MMM dd";
    public static final String DATE_FORMAT_STRING_SHORT = "HH:mm:ss";
    public static final String DATE_FORMAT_STRING_WITH_YEAR = "HH:mm:ss MMM dd yyyy";

    public static final String NUMBER_FORMAT_STRING = "#,##0.000";
    public static final DecimalFormat NUMBER_FORMAT = new DecimalFormat(Styles.NUMBER_FORMAT_STRING);

    // public static final Font HEADING;
    public static final Font BOLD;
    public static final Font BOLD_ITALIC;
    public static final Font STRIKETHROUGH;

    public static final Font LABEL;
    public static final Font LABEL_ERROR;

    public static final Font TITLE;

    public static final Font ANNOTATION_FONT;

    public static final Border CONTENT_BORDER = BorderFactory.createEmptyBorder(0, 5, 2, 2);
    public static final Border TITLE_BORDER = BorderFactory.createEmptyBorder(5, 2, 5, 2);
    public static final Border LOWER_LINE_BORDER = BorderFactory.createMatteBorder(0, 0, 1, 0,
            java.awt.Color.LIGHT_GRAY);
    public static final Border DOUBLE_LINE_BORDER = BorderFactory.createMatteBorder(1, 0, 1, 0,
            java.awt.Color.LIGHT_GRAY);

    public static final Color ERROR_COLOR = Color.RED;
    public static final Color DEFAULT_COLOR = Color.BLACK;

    public static final Color ANNOTATION_COLOR = new Color(0x222266);

    public static final BasicStroke ANNOTATION_STROKE = new BasicStroke(.6f, 0, 0, 1.0f, new float[] { 1, 2, 5, 2 }, 5);

    public static final ImageIcon IBM_ICON = buildIcon("ibmicon.png");

    public static final ImageIcon ADD_ICON = buildIcon("add.png");
    public static final ImageIcon COPY_ICON = buildIcon("page_copy.png");
    public static final ImageIcon CLEAR_ICON = buildIcon("delete.png");

    public static final ImageIcon SAVE_ICON = buildIcon("disk.png");

    public static final ImageIcon INTERVAL_ICON = buildIcon("clock.png");

    public static final ImageIcon REPORT_ICON = buildIcon("report_picture.png");
    public static final ImageIcon COMPUTER_ICON = buildIcon("computer.png");

    static {
        Font defaultFont = UIManager.getDefaults().getFont("TextField.font");

        LABEL = defaultFont.deriveFont(Font.BOLD, defaultFont.getSize() * 1.1f);
        LABEL_ERROR = LABEL.deriveFont(Font.BOLD | Font.ITALIC);

        TITLE = defaultFont.deriveFont(Font.BOLD, defaultFont.getSize() * 1.25f);

        BOLD = defaultFont.deriveFont(Font.BOLD);
        BOLD_ITALIC = defaultFont.deriveFont(Font.BOLD | Font.ITALIC);

        @SuppressWarnings("rawtypes")
        Map attributes = defaultFont.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        STRIKETHROUGH = defaultFont.deriveFont(attributes);

        ANNOTATION_FONT = defaultFont;
    }

    /**
     * Create an {@link ImageIcon} from an image stored in the <code>com.ibm.nmon.gui.icons</code>
     * package.
     */
    public static ImageIcon buildIcon(String image) {
        return new ImageIcon(Styles.class.getResource("/com/ibm/nmon/gui/icons/" + image));
    }

    public static Border createTopLineBorder(JComponent component) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, java.awt.Color.LIGHT_GRAY),
                BorderFactory.createMatteBorder(1, 0, 0, 0, component.getBackground()));
    }

    public static Border createBottomLineBorder(JComponent component) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.LIGHT_GRAY),
                BorderFactory.createMatteBorder(0, 0, 1, 0, component.getBackground()));
    }

    private Styles() {}
}
