package com.ibm.nmon.data.matcher;

import java.util.Collection;
import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.ibm.nmon.data.DataSet;

/**
 * Matches a set of {@link DataSet} hostnames based on a regular expression.
 */
public final class RegexHostMatcher implements HostMatcher {
    private final Matcher matcher;

    public RegexHostMatcher(String regex) {
        matcher = Pattern.compile(regex).matcher("");
    }

    @Override
    public boolean matchesHost(DataSet data) {
        return matcher.reset(data.getHostname()).matches();
    }

    @Override
    public List<DataSet> getMatchingHosts(Collection<DataSet> toMatch) {
        if ((toMatch == null) || toMatch.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        else {
            List<DataSet> toReturn = new java.util.ArrayList<DataSet>(toMatch.size());

            for (DataSet data : toMatch) {
                if (matcher.reset(data.getHostname()).matches()) {
                    toReturn.add(data);
                }
            }

            return toReturn;
        }
    }

    @Override
    public String toString() {
        return matcher.pattern().pattern();
    }

    @Override
    public int hashCode() {
        return matcher.pattern().pattern().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        else if (obj instanceof RegexHostMatcher) {
            RegexHostMatcher matcher = (RegexHostMatcher) obj;

            return this.matcher.pattern().pattern().equals(matcher.matcher.pattern().pattern());
        }
        else {
            return false;
        }
    }
}
