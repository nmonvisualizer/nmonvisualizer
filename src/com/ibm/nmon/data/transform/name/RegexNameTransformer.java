package com.ibm.nmon.data.transform.name;

import org.slf4j.Logger;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * <p>
 * Performs regular expression substitution on a given string.
 * </p>
 * 
 * <p>
 * This class <em>requires</em> a regular expression with at least one matching group. This group
 * will be used to create the return value for {@link #transform(String) transform()}. By default
 * the group number will be <code>1</code>; if a different grouping number is required, it should be
 * specified in the constructor.
 * </p>
 */
public final class RegexNameTransformer implements NameTransformer {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RegexNameTransformer.class);

    private final Matcher matcher;
    private final int group;

    public RegexNameTransformer(String regex) {
        this(regex, 1);
    }

    public RegexNameTransformer(String regex, int group) {
        matcher = Pattern.compile(regex).matcher("");

        if (group < 1) {
            throw new IllegalArgumentException("group cannot be less than 1");
        }

        this.group = group;
    }

    /**
     * @return the matching group from the regex specified by the instance of this class. If the
     *         regex does not match or does not contain the correct group, the original string will
     *         be returned.
     */
    @Override
    public String transform(String original) {
        if (!matcher.reset(original).matches()) {
            logger.warn("regex '{}' does not match '{}'", matcher.pattern().pattern(), original);
            return original;
        }
        else if (matcher.groupCount() == 0) {
            logger.warn("regex '{}' does not contain any groups", matcher);
            return original;
        }
        else if (matcher.groupCount() < group) {
            logger.warn("regex '{}' only has {} groups, but {} was specified",
                    new Object[] { matcher, matcher.groupCount(), group });
            return original;
        }
        else {
            return matcher.group(group);
        }
    }

    @Override
    public String toString() {
        return matcher.pattern().pattern() + ';' + group;
    }
}
