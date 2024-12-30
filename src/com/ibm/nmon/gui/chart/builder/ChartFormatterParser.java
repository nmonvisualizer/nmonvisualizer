package com.ibm.nmon.gui.chart.builder;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.List;
import java.util.Properties;

import java.util.regex.Pattern;

import java.awt.Color;
import org.jfree.chart.ChartColor;

public class ChartFormatterParser {
    private static final Pattern SPLITTER = Pattern.compile(",");
    private static final Class<ChartColor> COLOR_CLASS = ChartColor.class;

    private final Properties properties = new Properties();

    public ChartFormatter loadFromFile(String file) throws IllegalArgumentException, IOException {
        return loadFromFile(new File(file));
    }

    /**
     * <p>
     * Load a {@link ChartFormatter} from a properties file. Values not specified in the file will be set to the default
     * values.
     * </p>
     * <p>
     * The values specified in the properties file can be:
     * <ul>
     * <li>Hex values like <code>0xa0a0a0</code> or <code>#A0A0A0</code>. Lower or uppercase is accepted.</li>
     * <li>An array of RGB values like <code>[ 128, 128, 128 ]</code></li>
     * <li>A color name like <code>RED</code>. Names are defined in {@link Color} and {@link ChartColor}. Values can be
     * lower or uppercase.
     * </ul>
     * </p>
     * 
     * <p>
     * For <code>series</code> (see {@link ChartFormatter#setSeriesColors(Color[])}, a comma separated list of colors
     * can be accepted. The list can contain any of value types specified above, in any order.
     * </p>
     */
    public ChartFormatter loadFromFile(File file) throws IllegalArgumentException, IOException {
        if (!file.exists()) {
            throw new IllegalArgumentException("'" + file.getName() + "' does not exist");
        }

        FileReader in = null;

        try {
            in = new FileReader(file);
            properties.load(in);
        }
        finally {
            if (in != null) {
                in.close();
            }
        }

        // assume null property sets formatter to default value
        ChartFormatter formatter = new ChartFormatter();
        formatter.setBackground(parseProperty("background"));
        formatter.setPlotBackground(parseProperty("plotBackground"));
        formatter.setTextColor(parseProperty("text"));
        formatter.setElementColor(parseProperty("elements"));
        formatter.setGridLineColor(parseProperty("gridLines"));
        formatter.setAnnotationColor(parseProperty("annotations"));

        // parse the series value as a comma separated list of colors in any format
        String temp = properties.getProperty("series");

        if (temp != null) {
            List<Color> series = new java.util.ArrayList<Color>();

            String[] colors = SPLITTER.split(temp);

            for (int i = 0; i < colors.length; i++) {
                String data = colors[i];

                // reconstruct array values by re-concatenating the next two elements
                // not the most efficient, but easier than searching beforehand or using a more complex splitting regex
                int idx = data.indexOf('[');

                if (idx != -1) {
                    if (colors.length <= (i + 2)) {
                        throw new IllegalArgumentException("cannot parse '" + colors[i] + "' as a color.\n"
                                + "Arrays must have 3 elements [r, g, b]");
                    }

                    data += ',';
                    data += colors[i + 1];
                    data += ',';
                    data += colors[i + 2];
                    i += 2;

                    idx = data.indexOf(']');

                    // technically not checking that ] is at the end of the string
                    // parseColor will handle missing commas in the next element like '] RED'
                    if (idx == -1) {
                        throw new IllegalArgumentException(
                                "cannot parse '" + data + "' as a color.\n" + "Arrays must end with ']'");
                    }
                }

                series.add(parseColor("series", data));
            }

            formatter.setSeriesColors(series.toArray(new Color[series.size()]));
        }

        return formatter;
    }

    private Color parseProperty(String name) {
        String data = properties.getProperty(name);

        if (data == null) {
            return null;
        }

        return parseColor(name, data);
    }

    private Color parseColor(String name, String data) {
        data = data.trim();

        if ("".equals(data)) {
            return null;
        }

        if (data.startsWith("#") || data.startsWith("0x")) {
            try {
                // accepts upper and lowercase hex digits
                return Color.decode(data);
            }
            catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("cannot parse '" + data + "' as a color for " + name + ".");
            }
        }
        else if (data.startsWith("[")) {
            // parse as a 3 element array of rgb colors
            String[] rgb = SPLITTER.split(data);

            if (rgb.length != 3) {
                throw new IllegalArgumentException("cannot parse '" + data + "' as a color for " + name + "."
                        + "\nArrays must have 3 elements [r, g, b].");
            }

            rgb[0] = rgb[0].trim().substring(1).trim(); // remove [
            rgb[1] = rgb[1].trim();

            int idx = rgb[2].indexOf("]");

            // technically not checking that ] is at the end of the string
            // parseInt will throw exceptions for missing commas in the next element like '] RED'
            if (idx == -1) {
                throw new IllegalArgumentException(
                        "cannot parse '" + data + "' as a color.\n" + "Arrays must end with ']'");
            }

            rgb[2] = rgb[2].substring(0, idx).trim();

            int r, g, b;

            try {
                r = Integer.parseInt(rgb[0]);
            }
            catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "cannot parse '" + rgb[0] + "' from '" + data + "' as a color for " + name + ".");
            }

            try {
                g = Integer.parseInt(rgb[1]);
            }
            catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "cannot parse '" + rgb[1] + "' from '" + data + "' as a color for " + name + ".");
            }

            try {
                b = Integer.parseInt(rgb[2]);
            }
            catch (NumberFormatException nfe) {
                throw new IllegalArgumentException(
                        "cannot parse '" + rgb[2] + "' from '" + data + "' as a color for " + name + ".");
            }

            try {
                return new Color(r, g, b);
            }
            catch (IllegalArgumentException iae) {
                // handle illegal values (e.g. -1 or 257)
                throw new IllegalArgumentException(
                        "cannot parse '" + data + "' as a color for " + name + ".", iae);
            }
        }
        else {
            try {
                // try to find a field named by the value; uppercase since all ChartColor values are uppercase.
                // getField throws exception if not found, so no need for null check
                return (Color) COLOR_CLASS.getField(data.toUpperCase()).get(null);
            }
            catch (Exception e) {
                throw new IllegalArgumentException("cannot parse '" + data + "' as a color for " + name + ".");
            }
        }
    }
}
