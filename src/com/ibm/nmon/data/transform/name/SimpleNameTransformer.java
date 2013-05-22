package com.ibm.nmon.data.transform.name;

/**
 * <p>
 * Performs simple alias substitution on a given string.
 * </p>
 * 
 * <p>
 * This class <em>does not</em> attempt to match the given string for validity. Callers are
 * responsible for ensuring a given string should be aliased; if it is not,
 * {@link #transform(String) transform()} should not be called.
 * </p>
 */
public final class SimpleNameTransformer implements NameTransformer {
    private final String alias;

    public SimpleNameTransformer(String alias) {
        if ((alias == null) || "".equals(alias)) {
            throw new IllegalArgumentException("alias" + " cannot be null");
        }

        this.alias = alias;

    }

    /**
     * @return the <code>alias</code> specified by the instance of this class
     */
    @Override
    public String transform(String original) {
        return alias;
    }

    @Override
    public String toString() {
        return alias;
    }
}
