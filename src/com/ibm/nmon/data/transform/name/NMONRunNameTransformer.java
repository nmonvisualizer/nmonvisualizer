package com.ibm.nmon.data.transform.name;

import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.NMONDataSet;

public final class NMONRunNameTransformer implements NameTransformer {

    private final NMONDataSet data;

    public NMONRunNameTransformer(DataSet data) {
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
            String runname = data.getMetadata("runname");

            if (runname != null) {
                return runname;
            }
            else {
                return original;
            }
        }
    }
}
