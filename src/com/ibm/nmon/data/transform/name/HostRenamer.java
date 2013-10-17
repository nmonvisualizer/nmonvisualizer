package com.ibm.nmon.data.transform.name;

import org.slf4j.Logger;

import java.util.List;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.matcher.HostMatcher;

public final class HostRenamer {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HostRenamer.class);

    private final List<HostMatcher> matchers;
    private final List<NameTransformer> transformers;

    public HostRenamer() {
        matchers = new java.util.LinkedList<HostMatcher>();
        transformers = new java.util.LinkedList<NameTransformer>();
    }

    private HostRenamer(HostMatcher matcher, NameTransformer renamer) {
        matchers = java.util.Collections.singletonList(matcher);
        transformers = java.util.Collections.singletonList(renamer);
    }

    public void addRenamer(HostMatcher matcher, NameTransformer renamer) {
        if ((matcher != null) && (renamer != null)) {
            matchers.add(matcher);
            transformers.add(renamer);
        }
    }

    public void rename(DataSet data) {
        int n = 0;

        for (HostMatcher matcher : matchers) {
            if (matcher.matchesHost(data)) {
                NameTransformer transformer = transformers.get(n);

                if (LPARNameTransformer.class.equals(transformer.getClass())) {
                    transformer = new LPARNameTransformer(data);
                }
                else if (NMONRunNameTransformer.class.equals(transformer.getClass())) {
                    transformer = new NMONRunNameTransformer(data);
                }

                String newHostname = transformer.transform(data.getHostname());

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("renaming '{}' to '{}' with '{}'", new Object[] { data.getHostname(), newHostname,
                            transformer });
                }

                data.setHostname(newHostname);

                break;
            }
            else {
                LOGGER.trace("'{}' does not match '{}'", matcher, data);
            }

            ++n;
        }
    }

    public static final HostRenamer BY_LPAR = new HostRenamer(HostMatcher.ALL, new LPARNameTransformer(null));
    public static final HostRenamer BY_RUN = new HostRenamer(HostMatcher.ALL, new NMONRunNameTransformer(null));
    public static final HostRenamer BY_HOST = new HostRenamer(HostMatcher.ALL, new NameTransformer() {
        @Override
        public String transform(String original) {
            return original;
        }

        public String toString() {
            return "hostname";
        };
    });
}
