package com.ibm.nmon.parser.gc.state;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.parser.gc.GCParserContext;

abstract class JavaGCCycle implements GCState {

    protected boolean beforeGC;
    protected boolean error;

    public JavaGCCycle() {
        reset();
    }

    @Override
    public void reset() {
        this.error = false;
        this.beforeGC = true;
    }

    /**
     * Convert free and total bytes to KB then calculate used KB.
     */
    protected final void calculateSizes(GCParserContext context, String type, String freeAttribute,
            String totalAttribute) {
        String typeId = beforeGC ? "GCBEF" : "GCAFT";

        double free = context.parseDouble(freeAttribute) / 1024;
        double total = context.parseDouble(totalAttribute) / 1024;
        double used = total - free;

        context.setValue(typeId, "free_" + type, free);
        context.setValue(typeId, "used_" + type, used);
        context.setValue(typeId, "total_" + type, total);
    }

    /**
     * Calculate total values from the nursery and tenured values.
     */
    protected final void calculateTotalSizes(GCParserContext context) {
        DataType type = context.getDataType("GCBEF");
        DataRecord currentRecord = context.getCurrentRecord();

        double freeNurseryBefore = currentRecord.getData(type, "free_nursery");
        double usedNurseryBefore = currentRecord.getData(type, "used_nursery");
        double totalNurseryBefore = currentRecord.getData(type, "total_nursery");

        double freeTenuredBefore = currentRecord.getData(type, "free_tenured");
        double usedTenuredBefore = currentRecord.getData(type, "used_tenured");
        double totalTenuredBefore = currentRecord.getData(type, "total_tenured");

        // if GC is not gencon, then do not add as the nursery values will be NaN
        if (context.isGencon()) {
            currentRecord.setValue(type, "free", freeNurseryBefore + freeTenuredBefore);
            currentRecord.setValue(type, "used", usedNurseryBefore + usedTenuredBefore);
            currentRecord.setValue(type, "total", totalNurseryBefore + totalTenuredBefore);
        }
        else {
            currentRecord.setValue(type, "free", freeTenuredBefore);
            currentRecord.setValue(type, "used", usedTenuredBefore);
            currentRecord.setValue(type, "total", totalTenuredBefore);
        }

        type = context.getDataType("GCAFT");

        double freeNurseryAfter = currentRecord.getData(type, "free_nursery");
        double usedNurseryAfter = currentRecord.getData(type, "used_nursery");
        double totalNurseryAfter = currentRecord.getData(type, "total_nursery");

        double freeTenuredAfter = currentRecord.getData(type, "free_tenured");
        double usedTenuredAfter = currentRecord.getData(type, "used_tenured");
        double totalTenuredAfter = currentRecord.getData(type, "total_tenured");

        if (context.isGencon()) {
            currentRecord.setValue(type, "free", freeNurseryAfter + freeTenuredAfter);
            currentRecord.setValue(type, "used", usedNurseryAfter + usedTenuredAfter);
            currentRecord.setValue(type, "total", totalNurseryAfter + totalTenuredAfter);
        }
        else {
            currentRecord.setValue(type, "free", freeTenuredAfter);
            currentRecord.setValue(type, "used", usedTenuredAfter);
            currentRecord.setValue(type, "total", totalTenuredAfter);
        }

        type = context.getDataType("GCMEM");

        // add the requested size back if any
        // the after stats are after the request has been filled
        double requested = 0;

        if (currentRecord.hasData(type)) {
            requested = currentRecord.getData(type, "requested") / 1024;
        }

        double nurseryFreed = freeNurseryAfter - freeNurseryBefore + requested;
        double tenuredFreed = freeTenuredAfter - freeTenuredBefore + requested;

        currentRecord.setValue(type, "nursery_freed", nurseryFreed);
        currentRecord.setValue(type, "tenured_freed", tenuredFreed);

        if (context.isGencon()) {
            currentRecord.setValue(type, "total_freed", nurseryFreed + tenuredFreed);
        }
        else {
            currentRecord.setValue(type, "total_freed", tenuredFreed);
        }
    }
}