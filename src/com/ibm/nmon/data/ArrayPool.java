package com.ibm.nmon.data;

import java.util.Map;

/**
 * Simple cache for double arrays. Currently only caches arrays that are all zero for a minor memory
 * savings (~1MB over 20 test NMON files).
 */
final class ArrayPool {
    private static final Map<Integer, double[]> POOL = new java.util.HashMap<Integer, double[]>();

    static double[] getArray(double[] toPool) {
        // only pool arrays that are all 0
        for (int i = 0; i < toPool.length; i++) {
            if (toPool[i] != 0) {
                return toPool;
            }
        }

        double[] pooled = POOL.get(toPool.length);

        if (pooled == null) {
            // assume new arrays default to 0 filled
            pooled = new double[toPool.length];
        }

        return pooled;
    }
}
