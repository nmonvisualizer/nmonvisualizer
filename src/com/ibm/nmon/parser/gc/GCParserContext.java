package com.ibm.nmon.parser.gc;

import java.util.Map;

import org.slf4j.Logger;

import com.ibm.nmon.data.BasicDataSet;

import com.ibm.nmon.data.DataRecord;

import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;

import com.ibm.nmon.parser.util.XMLParserHelper;

/**
 * Data holder for an in-progress GC parse session. Also contains various utility functions for
 * setting values on the current DataRecord and logging errors.
 */
public final class GCParserContext {
    private final Logger logger;

    private final BasicDataSet data;
    private DataRecord currentRecord;

    private int lineNumber;
    private Map<String, String> attributes;

    private boolean isGencon;

    // verbose GC does not log a count of compactions
    private int compactionCount;

    GCParserContext(BasicDataSet data, Logger logger) {
        this.data = data;
        this.logger = logger;

        reset();
    }

    public void reset() {
        currentRecord = null;
        attributes = null;
        lineNumber = 0;
        isGencon = false;
        compactionCount = 0;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public BasicDataSet getData() {
        return data;
    }

    public DataRecord getCurrentRecord() {
        return currentRecord;
    }

    public void setCurrentRecord(DataRecord currentRecord) {
        this.currentRecord = currentRecord;
    }

    public void saveRecord() {
        data.addRecord(currentRecord);
    }

    public void setGencon(boolean isGencon) {
        this.isGencon = isGencon;
    }

    public boolean isGencon() {
        return isGencon;
    }

    public int incrementCompactionCount() {
        return ++compactionCount;
    }

    public void resetCompactionCount() {
        compactionCount = 0;
    }

    public void setValue(String typeId, String field, String attribute) {
        String value = attributes.get(attribute);

        if (value == null) {
            logMissingAttribute(attribute);
            return;
        }

        currentRecord.setValue(getDataType(typeId), field, parseDouble(attribute));
    }

    public void setValue(String typeId, String field, double value) {
        currentRecord.setValue(getDataType(typeId), field, value);
    }

    public void setValueDiv1000(String typeId, String field, String name) {
        String value = attributes.get(name);

        if (value == null) {
            logMissingAttribute(name);
            return;
        }

        currentRecord.setValue(getDataType(typeId), field, parseDouble(name) / 1000);
    }

    public void parseAttributes(String unparsedAttributes) {
        this.attributes = XMLParserHelper.parseAttributes(unparsedAttributes);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public double parseDouble(String name) {
        String value = attributes.get(name);

        if (value == null) {
            logMissingAttribute(name);
            return Double.NaN;
        }

        double toReturn;

        try {
            toReturn = Double.parseDouble(value);
        }
        catch (NumberFormatException nfe) {
            logger.warn("attribute '{}' with value '{}', defined at line {}, is not a number", new Object[] { name,
                    value, getLineNumber(), });

            toReturn = Double.NaN;
        }

        return toReturn;
    }

    public void logMissingAttribute(String attribute) {
        logger.warn("no attribute named {} defined at line {}", attribute, getLineNumber());
    }

    public void logUnrecognizedElement(String elementName) {
        logger.warn("unrecogized element '{}' at line {}", elementName, getLineNumber());
    }

    public void logInvalidValue(String attribute, String value) {
        logger.warn("attribute '{}' with value '{}', defined at line {}, is not a valid value", new Object[] {
                attribute, value, getLineNumber() });
    }

    public DataType getDataType(String typeId) {
        String jvmName = data.getMetadata("jvm_name");
        SubDataType type = (SubDataType) data.getType(SubDataType.buildId(typeId, jvmName));

        if (type != null) {
            return type;
        }
        else if ("GCMEM".equals(typeId)) {
            type = new SubDataType("GCMEM", jvmName, "GC Memory Stats", "requested", "total_freed", "nursery_freed",
                    "tenured_freed", "flipped", "flipped_bytes", "tenured", "tenured_bytes", "moved", "moved_bytes");
        }
        else if ("GCSTAT".equals(typeId)) {
            type = new SubDataType("GCSTAT", jvmName, "GC Memory Stats", "finalizers", "soft", "weak", "phantom",
                    "tiltratio");
        }
        else if ("GCTIME".equals(typeId)) {
            type = new SubDataType("GCTIME", jvmName, "GC Times (ms)", "total_ms", "nursery_ms", "tenured_ms",
                    "mark_ms", "sweep_ms", "compact_ms", "exclusive_ms");
        }
        else if ("GCSINCE".equals(typeId)) {
            type = new SubDataType("GCSINCE", jvmName, "Time Since Last", "af_nursery", "af_tenured", "gc_scavenger",
                    "gc_global", "gc_system", "con_mark");
        }
        else if ("GCBEF".equals(typeId)) {
            type = new SubDataType("GCBEF", jvmName, "Sizes Before GC", "total", "free", "used", "total_nursery",
                    "free_nursery", "used_nursery", "total_tenured", "free_tenured", "used_tenured");
        }
        else if ("GCAFT".equals(typeId)) {
            type = new SubDataType("GCAFT", jvmName, "Sizes After GC", "total", "free", "used", "total_nursery",
                    "free_nursery", "used_nursery", "total_tenured", "free_tenured", "used_tenured");
        }
        else if ("GCCOUNT".equals(typeId)) {
            type = new SubDataType("GCCOUNT", jvmName, "GC Counts", "total_count", "nursery_count", "tenured_count",
                    "compaction_count", "system_count");
        }
        else {
            throw new IllegalArgumentException("invalid type " + typeId);
        }

        data.addType(type);

        return type;
    }
}
