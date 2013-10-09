package com.ibm.nmon.data.transform.name;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.NMONDataSet;

/**
 * <p>
 * Replaces the given string with the name of the LPAR, as pulled from an NMON DataSet.
 * </p>
 * 
 * <p>
 * This class <em>does not</em> attempt to match the given string for validity. Callers are
 * responsible for ensuring a given string should be aliased; if it is not,
 * {@link #transform(String) transform()} should not be called.
 * </p>
 */
public final class LPARNameTransformer implements NameTransformer {
    private final NMONDataSet data;

    public LPARNameTransformer(DataSet data) {
        if ((data != null) && NMONDataSet.class.equals(data.getClass())) {
            this.data = (NMONDataSet) data;
        }
        else {
            this.data = null;
        }
    }

    @Override
    public String transform(String original) {
        if (data == null) {
            return original;
        }
        else {
            NMONDataSet nmonData = (NMONDataSet) data;

            if (nmonData.getMetadata("AIX") != null) {
                String lparstat = nmonData.getSystemInfo("lparstat -i");

                if (lparstat != null) {
                    int idx = lparstat.indexOf("Partition Name");

                    if (idx != -1) {
                        // some number of spaces before the colon
                        idx = lparstat.indexOf(": ", idx);

                        int end = lparstat.indexOf("\n", idx);

                        return lparstat.substring(idx + 2, end);
                    }
                }
            }

            return original;
        }
    }
}
