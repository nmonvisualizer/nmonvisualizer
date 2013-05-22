package com.ibm.nmon.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;

/**
 * Loads the <code>version.properties</code> file packaged with the build so that the current
 * version number can be read and displayed in the UI.
 */
public final class VersionInfo {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(VersionInfo.class);

    private static final String version = loadVersion();

    public static String getVersion() {
        return version;
    }

    private static String loadVersion() {
        InputStream in = VersionInfo.class.getResourceAsStream("/com/ibm/nmon/" + "version.properties");

        if (in != null) {
            Properties properties = new Properties();

            try {
                properties.load(in);
                return properties.getProperty("version");
            }
            catch (IOException ioe) {
                logger.warn("Error loading " + "version.properties" + " file", ioe);
                return "UNKNOWN";
            }
            finally {
                try {
                    in.close();
                }
                catch (IOException ioe) {
                    logger.warn("Cannot close " + "version.properties");
                }
            }
        }
        else {
            logger.debug("could not load " + "version.properties" + "; defaulting to '" + "development" + '\'');

            return "development";
        }
    }
}
