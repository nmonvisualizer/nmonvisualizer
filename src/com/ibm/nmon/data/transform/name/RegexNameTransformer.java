package com.ibm.nmon.data.transform.name;

import org.slf4j.Logger;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * <p>
 * Performs regular expression matching and replacement on a given string.
 * </p>
 * 
 * <p>
 * This class can be specified by either a matching group number or a full replacement string. If a group number is
 * given, {@link Matcher#group(int)} is returned by {@link #transform(String) transform()}. If a replacement string is
 * given, {@link Matcher#replaceAll(String)} is returned. By default the group number will be <code>1</code>.
 * </p>
 * 
 * @see Matcher#replaceAll(String)
 */
public final class RegexNameTransformer implements NameTransformer {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(RegexNameTransformer.class);

    private final Matcher matcher;
    private final int group;
    private final String replacement;

    /**
     * Creates a transformer that returns the first matching group in the regex, if it matches.
     * 
     * @param regex a regular expression with at least 1 matching group
     */
    public RegexNameTransformer(String regex) {
        this(regex, 1);
    }

    /**
     * Creates a transformer that returns the given matching group in the regex, if it matches.
     * 
     * @param regex a regular expression with at least as many matching groups as the given group
     * @param group the matching group to return
     */
    public RegexNameTransformer(String regex, int group) {
        matcher = Pattern.compile(regex).matcher("");

        if (group < 0) {
            throw new IllegalArgumentException("group cannot be less than 0");
        }

        this.group = group;
        this.replacement = null;
    }

    /**
     * Creates a transformer that returns the given replacement string, if the regex matches.
     * 
     * @param regex a regular expression to match
     * @param replacement the replacement string for regular expression substitution
     * 
     * @see Matcher#replaceAll(String)
     */

    public RegexNameTransformer(String regex, String replacement) {
        matcher = Pattern.compile(regex).matcher("");

        if ((replacement == null) || "".equals(replacement)) {
            throw new IllegalArgumentException("replacement string cannot be empty");
        }

        this.group = -1;
        this.replacement = replacement;
    }

    /**
     * @return either the matching group from the regex specified by the instance of this class or the given string
     *         after regex replacement. If the regex does not match, the original string will be returned.
     */
    @Override
    public String transform(String original) {
        if (!matcher.reset(original).matches()) {
            logger.debug("regex '{}' does not match '{}'", matcher.pattern().pattern(), original);
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
            if (replacement != null) {
                return matcher.replaceAll(replacement);
            }
            else {
                return matcher.group(group);
            }
        }
    }

    @Override
    public String toString() {
        if (group == -1) {
            return matcher.pattern().pattern() + ';' + replacement;
        }
        else {
            return matcher.pattern().pattern() + ';' + group;
        }
    }
}
