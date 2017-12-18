package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import java.util.List;
import java.util.Set;
import java.util.Map;

import java.util.TimeZone;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.io.IOException;

import java.io.File;
import java.io.LineNumberReader;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;
import com.ibm.nmon.data.SubDataType;
import com.ibm.nmon.util.DataHelper;

import com.ibm.nmon.util.TimeHelper;
import static com.ibm.nmon.util.TimeHelper.TIMESTAMP_FORMAT_ISO;

public final class IOStatParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(IOStatParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT_AIX = new SimpleDateFormat("HH:mm:ss");

    private static final Matcher ISO_PATTERN = Pattern
            .compile("(Time: )?\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}([\\-+](\\d{4}?|\\d{2}:\\d{2}|\\d{2})|Z)")
            .matcher("");
    private static final Matcher INFO = Pattern.compile(
            "(.+)\\s(.+)\\s\\((.+)\\)\\s+(\\d{2,4}[\\/-]\\d{2}[\\/-]\\d{2,4})(\\s+_(.+)_)?(\\s+\\((.+)\\sCPU\\))?")
            .matcher("");
    private static final Pattern DATA_SPLITTER = Pattern.compile(":?\\s+");

    public static final String DEFAULT_HOSTNAME = "iostat";

    private static final Set<String> VALID_TYPES;

    static {
        Set<String> tmp = new java.util.HashSet<String>();

        tmp.add("System");
        tmp.add("tty");
        tmp.add("avg-cpu");
        tmp.add("Disks");
        tmp.add("Device");
        tmp.add("FS");
        tmp.add("Adapter");
        tmp.add("Vadapter");
        tmp.add("Paths/Disks");

        VALID_TYPES = java.util.Collections.unmodifiableSet(tmp);
    }

    private LineNumberReader in = null;

    private SimpleDateFormat dateFormat = null;
    // iostat output is localized so use NumberFormat instead of Double.parseDouble()
    // this means that the program _must_ run with the same local as the system that ran iostat
    // may need to set -Duser.language and -Duser.region when starting the JVM
    private final NumberFormat numberFormat = NumberFormat.getNumberInstance();

    // this is a full datetime rounded to midnight
    private long dateOffset = 0;

    private BasicDataSet data = null;
    private DataRecord currentRecord = null;

    // latch used to ensure the first record of summary data is not saved
    private boolean firstRecord = true;

    // cache data type field arrays since there will be a subtype created for each disk / device
    private Map<String, String[]> typeFieldsCache = new java.util.HashMap<String, String[]>();

    // private String[] disk_metrics;

    public BasicDataSet parse(File file, TimeZone timeZone) throws IOException, ParseException {
        return parse(file.getAbsolutePath(), timeZone);
    }

    public BasicDataSet parse(String filename, TimeZone timeZone) throws IOException, ParseException {
        long start = System.nanoTime();

        data = new BasicDataSet(filename);
        data.setHostname(DEFAULT_HOSTNAME);

        try {
            in = new LineNumberReader(new java.io.FileReader(filename));

            String line = null;

            boolean isAIX = false;

            String type = null;
            String[] typeFields = null;

            boolean parseTime = false;
            boolean extendedDiskStats = false;

            line = in.readLine();

            while (line != null) {
                if ("".equals(line)) {
                    type = null;
                    extendedDiskStats = false;

                    line = in.readLine();
                    continue;
                }

                String[] values = DATA_SPLITTER.split(line);
                String first = values[0];

                if (type == null) {
                    if (VALID_TYPES.contains(first)) {
                        if (values.length < 2) {
                            LOGGER.warn("'{}' at line {} has too few columns; skipping",
                                    new Object[] { first, in.getLineNumber() });
                        }
                        else if ("configuration".equals(values[1])) { // line starts with 'System configuration'
                            isAIX = true;
                            parseAIXConfig(line, timeZone);
                        }
                        else {
                            type = first;
                            typeFields = values;
                            parseTime = isAIX; // parse time for each new AIX data type to see if it changed

                            LOGGER.trace("type set to '{}' at line {}", new Object[] { type, in.getLineNumber() });
                        }
                    }
                    else if ("Linux".equals(first)) {
                        parseLinuxConfig(line, timeZone);
                    }
                    else if (ISO_PATTERN.reset(line).matches()) {
                        createCurrentRecord(line);
                    }
                    else {
                        // will continue to output this for data after an invalid type
                        LOGGER.warn("invalid type '{}' at line {}", new Object[] { first, in.getLineNumber() });
                    }
                }
                else { // type != null => handle data
                       // skip format line for AIX extended disk stats
                    if (line.charAt(0) == '-') {
                        extendedDiskStats = true;

                        line = in.readLine();
                        continue;
                    }

                    if (parseTime) {
                        // last column is time in AIX
                        String time = values[values.length - 1];

                        // serv and qfull are headers for extended disk stats
                        if (!"time".equals(time) && !"serv".equals(time) && !"qfull".equals(time)) {
                            createCurrentRecord(time); // create new record on time change
                            parseTime = false; // reset on next data stanza
                        }
                    }

                    // special handling for data types with fixed (or no) subtypes
                    // these start with space before the values
                    if ("".equals(first)) {
                        if ("tty".equals(type)) {
                            parseAIXTTYAndCPU(type, typeFields, values);
                        }
                        else if ("System".equals(type)) {
                            // first line after System are the field names; parse data on next line
                            if ("Kbps".equals(values[1])) {
                                // AIX hostname is after System; set here to avoid setting on every data stanza
                                data.setHostname(typeFields[1]);

                                typeFields = values;
                            }
                            else {
                                parseAIXSystem(type, typeFields, values);
                            }
                        }
                        else if ("avg-cpu".equals(type)) {
                            parseLinuxCPU(type, typeFields, values);
                        }
                        else if ("Disks".equals(type)) {
                            // will only be here when reading header rows; otherwise first column will be disk name, not
                            // empty string
                            if (!extendedDiskStats) {
                                throw new IOException("line " + in.getLineNumber()
                                        + ": expected additional disk header rows after line of '-' for extended disk stats");
                            }
                            else {
                                if (!typeFieldsCache.containsKey(type)) {
                                    // discard line with 'xfers read write' and use this line as the first line
                                    if ("xfers".equals(typeFields[1])) {
                                        typeFields = values;
                                    }
                                    else {
                                        if ((typeFields.length - 8) != values.length) {
                                            throw new IOException(
                                                    "third row of extended disk stat names should have 8 less values than the second");
                                        }

                                        typeFields = parseAIXExtendedDiskHeaders(typeFields, values);
                                    }
                                } // else already built the type fields, ignore subsequent ones
                            }
                        }
                    }
                    else { // first column != ""
                        parseData(type, typeFields, values, isAIX, extendedDiskStats);
                    }
                }

                line = in.readLine();
            }

            return data;
        }
        finally

        {
            if (in != null) {
                try {
                    in.close();
                }
                catch (

                Exception e) {
                    // ignore
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                            (System.nanoTime() - start) / 1000000.0d);
                }

                in = null;

                dateFormat = null;
                dateOffset = 0;

                data = null;
                currentRecord = null;

                firstRecord = true;

                typeFieldsCache.clear();
            }
        }
    }

    private void parseAIXConfig(String line, TimeZone timeZone) {
        data.setMetadata("OS", "AIX");
        data.setMetadata("AIX", "iostat"); // mimic NMON so OSMatcher works as expected

        dateFormat = TIMESTAMP_FORMAT_AIX;
        dateFormat.setTimeZone(timeZone);

        // AIX has no date, use the default
        // shift offset to timezone local, not UTC
        dateOffset = getDefaultDate();
        dateOffset += timeZone.getOffset(dateOffset);

        line = line.substring("System configuration: ".length());
        String[] config = line.split("[ =]");

        for (int i = 0; i < config.length; i++) {
            data.setMetadata(DataHelper.newString(config[i]), DataHelper.newString(config[++i]));
        }
    }

    // tty line in AIX contains terminal and CPU utilization data
    // split that into two DataTypes
    private void parseAIXTTYAndCPUHeader(String[] rawFields) {
        List<String> fields = new java.util.ArrayList<String>();

        // i = 0 => tty:
        for (int i = 1; i < rawFields.length; i++) {
            if ("avg-cpu".equals(rawFields[i])) {
                // TTY data complete
                DataType tty = new DataType("IOStat" + " TTY", "IOStat" + " Terminal", fields.toArray(new String[0]));
                data.addType(tty);
                fields.clear();
            }
            else if ("time".equals(rawFields[i])) {
                continue;
            }
            else {
                if ("%".equals(rawFields[i])) {
                    // ignore idle
                    if ("idle".equals(rawFields[i + 1])) {
                        ++i;
                    }
                    else {
                        // handle spaces after % in AIX
                        fields.add(DataHelper.newString(rawFields[i++] + rawFields[i]));
                    }
                }
                else {
                    fields.add(DataHelper.newString(rawFields[i]));
                }
            }
        }

        DataType cpu = new DataType("IOStat" + " CPU", "IOStat" + " CPU" + " Utilization", fields.toArray(new String[0]));
        data.addType(cpu);
    }

    // parse tty and CPU utilization data into different data types
    private void parseAIXTTYAndCPU(String type, String[] typeFields, String[] values)
            throws IOException, ParseException {
        if (currentRecord == null) {
            throw new IOException("cannot adddata without a current record; does the file have timestamps?");
        }

        DataType dataType = data.getType("IOStat" + " CPU");

        if (dataType == null) {
            parseAIXTTYAndCPUHeader(typeFields);
        }

        // ignore blank first value
        int n = 1;

        DataType tty = data.getType("IOStat" + " TTY");
        double[] ttyData = new double[tty.getFieldCount()];

        for (int i = 0; i < ttyData.length; i++) {
            ttyData[i] = numberFormat.parse(values[n++]).doubleValue();
        }

        currentRecord.addData(tty, ttyData);

        DataType cpu = data.getType("IOStat" +" CPU");
        double[] cpuData = new double[cpu.getFieldCount()];

        for (int i = 0; i < cpuData.length;) {
            if (n == 5) {
                n++; // skip idle
            }
            else {
                cpuData[i++] = numberFormat.parse(values[n++]).doubleValue();
            }
        }

        currentRecord.addData(cpu, cpuData);
    }

    // parse System data
    // for Physical / Logical lines, create a single type for each, if present
    private void parseAIXSystem(String type, String[] typeFields, String[] values) throws IOException, ParseException {
        if (currentRecord == null) {
            throw new IOException("cannot add data without a current record; does the file have timestamps?");
        }

        DataType dataType = data.getType("IOStat" + " "+ values[1]);

        if (dataType == null) {
            // less 1 on the length to skip time
            String[] fields = new String[values.length - 2];

            for (int i = 1; i < values.length - 1; i++) {
                fields[i - 1] = DataHelper.newString(typeFields[i]);
            }

            dataType = new DataType("IOStat"+ " " + values[1], values[1], fields);

            data.addType(dataType);
        }

        double[] data = new double[dataType.getFieldCount()];

        for (int i = 1; i < data.length; i++) {
            data[i] = numberFormat.parse(values[i + 1]).doubleValue();
        }

        currentRecord.addData(dataType, data);
    }

    // concatenate the two rows of extended disk headers
    // this code is brittle in the face of any formatting changes
    private String[] parseAIXExtendedDiskHeaders(String[] row1, String[] row2) {
        // add 1 for dummy time column so getDataType uses all the values
        String[] tmp = new String[row1.length + 1];

        // 0, 1 => preserve initial blank in typeFields; do not use blank from values
        for (int i = 0, j = 1; i < row1.length; i++) {
            if (i == 1) {
                tmp[i] = "%tm_act";
                ++j;
            }
            else if (((i > 6) && (i < 11)) || ((i > 12) && (i < 17)) || (i > 17)) {
                tmp[i] = row1[i] + row2[j++];
            }
            else {
                tmp[i] = row1[i];
            }

            if ((i > 6) && (i < 12)) {
                tmp[i] = "r_" + tmp[i];
            }

            if ((i > 12) && (i < 18)) {
                tmp[i] = "w_" + tmp[i];
            }
        }

        tmp[tmp.length - 1] = "time";

        return tmp;
    }

    private void parseLinuxConfig(String line, TimeZone timeZone) throws IOException {
        Matcher matcher = INFO.reset(line);

        if (matcher.matches()) {
            data.setHostname(DataHelper.newString(matcher.group(3)));
            data.setMetadata("OS", DataHelper.newString(matcher.group(1) + ' ' + matcher.group(2)));

            String arch = matcher.group(6);
            String cpuCount = matcher.group(8);

            // Linux must export S_TIME_FORMAT=ISO before running iostat
            dateFormat = TIMESTAMP_FORMAT_ISO;
            dateFormat.setTimeZone(timeZone);

            // ISO includes date time, so offset is 0
            dateOffset = 0;

            if (arch != null) {
                data.setMetadata("ARCH", DataHelper.newString(arch));
            }

            if (cpuCount != null) {
                data.setMetadata("CPU_COUNT", DataHelper.newString(cpuCount));
            }
        }
        else {
            throw new IOException("file does not start with a recognized Linux iostat header");
        }
    }

    private void parseLinuxCPU(String type, String[] typeFields, String[] values) throws IOException, ParseException {
        if (currentRecord == null) {
            throw new IOException("cannot add data without a current record; does the file have timestamps?");
        }

        DataType cpu = data.getType("IOStat" + " CPU");

        if (cpu == null) {
            // create CPU data type
            // subtract 2 since avg-cpu (first column) is not a field
            // also ignore %idle (the last column)
            String[] fields = new String[typeFields.length - 2];

            for (int i = 0; i < fields.length; i++) {
                fields[i] = DataHelper.newString(typeFields[i + 1]);
            }

            cpu = new DataType("IOStat" +" CPU", "IOStat" + " CPU" + " Utilization", fields);
            data.addType(cpu);
        }

        double[] cpuData = new double[values.length - 2];

        for (int i = 0; i < cpuData.length; i++) {
            cpuData[i] = numberFormat.parse(values[i + 1]).doubleValue();
        }

        currentRecord.addData(cpu, cpuData);
    }

    // parse a line of data from strings into numbers
    private void parseData(String type, String[] typeFields, String[] values, boolean isAIX, boolean extendedDiskStats)
            throws IOException, ParseException {
        if (currentRecord == null) {
            throw new IOException("cannot add data without a current record; does the file have timestamps?");
        }

        DataType dataType = getDataType(type, values[0], typeFields, isAIX);

        // first field is the subtype; ignore AIX time data
        int dataLength = values.length - (isAIX ? 2 : 1);
        int fieldCount = dataType.getFieldCount();

        if (dataLength > fieldCount) {
            LOGGER.warn("'{}' at line {} has {} extra columns; they will be ignored",
                    new Object[] { dataType.getId(), in.getLineNumber(), dataLength - fieldCount });
            dataLength = dataType.getFieldCount();
        }
        else if (dataLength < fieldCount) {
            if (!extendedDiskStats || dataType.getId().startsWith("hdisk")) {
                LOGGER.warn("'{}' at line {} has too few columns; NaN will set for missing data",
                        new Object[] { dataType.getId(), in.getLineNumber() });
            } // else do not warn for missing extended disk stats on non-hdisks
        }

        double[] data = new double[fieldCount];

        ++dataLength;
        int n = 0;

        for (int i = 1; i < dataLength; i++) {
            String tmp = values[i];

            if ("-".equals(tmp)) {
                data[n] = Double.NaN;
            }
            else {
                data[n] = numberFormat.parse(tmp).doubleValue();

                // handle suffixes for extended disk stats; values with unite default to bytes or milliseconds
                // note the number format ignores the suffix and parses the digits normally
                if (extendedDiskStats) {
                    switch (tmp.charAt(tmp.length() - 1)) {
                    case 'K': // KB
                        data[n] *= 1000;
                        break;
                    case 'M': {
                        String field = dataType.getField(n);
                        if (field.endsWith("time") || field.endsWith("serv")) { // minutes
                            data[n] *= 60 * 1000;
                        }
                        else { // MB
                            data[n] *= 1000000;
                        }
                        break;
                    }
                    case 'G': // GB
                        data[n] *= 1000000000;
                    case 'T': // TB
                        data[n] *= 1000000000000L;
                    case 'S': // seconds
                        data[n] *= 1000;
                        break;
                    case 'H': // hours
                        data[n] *= 3600 * 1000;
                        break;
                    // default, leave the number as-is
                    }
                }
            }

            ++n;
        }

        if (dataLength < fieldCount) {
            for (int i = dataLength; i < fieldCount; i++) {
                data[i] = Double.NaN;
            }
        }

        currentRecord.addData(dataType, data);
    }

    // create a data subtype for each disk, adapter, etc
    private DataType getDataType(String type, String subtype, String[] typeFields, boolean isAIX) {
        DataType dataType = data.getType(SubDataType.buildId("IOStat " + type, subtype));

        if (dataType != null) {
            return dataType;
        }

        String[] fieldsArray = typeFieldsCache.get(type);

        if (fieldsArray == null) {
            // less 1 on the length in AIX to skip the time column
            int end = typeFields.length - (isAIX ? 1 : 0);
            int offset = 1;

            // AIX 'FS Name' type; skip name
            if ("FS".equals(type)) {
                ++offset;
            }

            List<String> fields = new java.util.ArrayList<String>(end - 1);

            // start at 1 to skip type name
            for (int i = offset; i < end; i++) {
                if ("%".equals(typeFields[i])) {
                    // handle spaces after % in AIX
                    fields.add(DataHelper.newString(typeFields[i] + typeFields[++i]));
                }
                else {
                    fields.add(DataHelper.newString(typeFields[i]));
                }
            }

            fieldsArray = fields.toArray(new String[fields.size()]);
            typeFieldsCache.put(type, fieldsArray);
        }

        dataType = new SubDataType("IOStat " + type, subtype, subtype, true, fieldsArray);

        data.addType(dataType);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.debug("created data type '{}' with {} fields",
                    new Object[] { dataType.getId(), dataType.getFieldCount() });
        }

        return dataType;
    }

    private void createCurrentRecord(String timeToParse) throws ParseException {
        long time = dateFormat.parse(timeToParse).getTime() + dateOffset;

        if (currentRecord != null) {
            // only create a new record if the time actually changes
            if (currentRecord.getTime() == time) {
                return;
            }

            if (firstRecord) {
                firstRecord = false;
            }
            else {
                data.addRecord(currentRecord);
            }

            currentRecord = null;
        }

        currentRecord = new DataRecord(time, timeToParse);
    }

    public static long getDefaultDate() {
        return TimeHelper.today();
    }
}
