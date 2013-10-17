package com.ibm.nmon.data.transform.name;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import java.io.IOException;
import java.io.File;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.ibm.nmon.data.matcher.ExactHostMatcher;
import com.ibm.nmon.data.matcher.HostMatcher;
import com.ibm.nmon.data.matcher.RegexHostMatcher;

public final class HostRenamerFactory {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HostRenamerFactory.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static HostRenamer loadFromFile(File file) throws IOException, IllegalArgumentException {
        List<Map<String, String>> data = MAPPER.readValue(file, new TypeReference<List<Map<String, String>>>() {});
        HostRenamer renamer = new HostRenamer();

        for (Map<String, String> map : data) {
            String alias = map.get("alias");

            NameTransformer transformer = null;
            HostMatcher matcher = null;

            if (alias != null) {
                if ("$LPAR".equals(alias)) {
                    transformer = new LPARNameTransformer(null);
                }
                else if ("$RUN".equals(alias)) {
                    transformer = new NMONRunNameTransformer(null);
                }
                else {
                    transformer = new SimpleNameTransformer(alias);
                }
            }
            else {
                alias = map.get("aliasRegex");

                if (alias != null) {
                    String group = map.get("aliasRegexGroup");

                    if (group == null) {
                        transformer = new RegexNameTransformer(alias);
                    }
                    else {
                        try {
                            transformer = new RegexNameTransformer(alias, Integer.parseInt(group));
                        }
                        catch (NumberFormatException nfe) {
                            LOGGER.warn("'aliasRegexGroup' must be a number");
                            transformer = new RegexNameTransformer(alias);
                        }
                    }
                }
                else {
                    LOGGER.warn("either 'alias' or 'aliasRegex'" + " must be defined for " + "each host");
                }
            }

            String name = map.get("name");

            if (name != null) {
                matcher = new ExactHostMatcher(name);
            }
            else {
                name = map.get("regex");

                if (name == null) {
                    matcher = HostMatcher.ALL;
                }
                else {
                    matcher = new RegexHostMatcher(name);
                }
            }

            LOGGER.debug("will rename '{}' to '{}'", matcher, transformer);

            renamer.addRenamer(matcher, transformer);
        }

        return renamer;
    }

    private HostRenamerFactory() {}
}
