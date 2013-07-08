package com.ibm.nmon.data.matcher;

import java.util.List;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.DataType;

/**
 * Matches a set of {@link DataType DataTypes} based on a regular expression.
 */
public final class RegexTypeMatcher implements TypeMatcher {
    private final Matcher matcher;

    public RegexTypeMatcher(String regex) {
        matcher = Pattern.compile(regex).matcher("");
    }

    @Override
    public List<DataType> getMatchingTypes(DataSet data) {
        if ((data == null) || (data.getTypeCount() == 0)) {
            return java.util.Collections.emptyList();
        }
        else {
            List<DataType> toReturn = new java.util.ArrayList<DataType>(data.getTypeCount());

            for (DataType type : data.getTypes()) {
                // note matching on toString, not typeId
                if (matcher.reset(type.toString()).matches()) {
                    toReturn.add(type);
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
        else if (obj instanceof RegexTypeMatcher) {
            RegexTypeMatcher matcher = (RegexTypeMatcher) obj;

            return this.matcher.pattern().pattern().equals(matcher.matcher.pattern().pattern());
        }
        else {
            return false;
        }
    }
}
