package com.ibm.nmon.data.transform.name;

/**
 * An interface for classes that need to rename and/or otherwise transform Strings. This interface is
 * used by {@link DefaultDataDefinition} to specify how data sets, data types and fields can be
 * renamed.
 */
public interface NameTransformer {
    public String transform(String original);
}
