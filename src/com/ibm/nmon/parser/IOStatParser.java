package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.List;
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
import static com.ibm.nmon.util.TimeHelper.DATE_FORMAT_ISO;

public final class IOStatParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(IOStatParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT_US = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    private static final SimpleDateFormat TIMESTAMP_FORMAT_OLD = new SimpleDateFormat("'Time: 'hh:mm:ss a");
    private static final SimpleDateFormat TIMESTAMP_FORMAT_AIX = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT_US = new SimpleDateFormat("MM/dd/yyyy");

    private static final Matcher ISO_PATTERN = Pattern.compile(
            "(Time: )?\\d{4}\\-\\d{2}\\-\\d{2}T\\d{2}:\\d{2}:\\d{2}([\\-+](\\d{4}?|\\d{2}:\\d{2}|\\d{2})|Z)").matcher(
            "");
    private static final Matcher INFO = Pattern.compile(
            "(.+)\\s(.+)\\s\\((.+)\\)\\s+(\\d{2,4}[\\/-]\\d{2}[\\/-]\\d{2,4})(\\s+_(.+)_)?(\\s+\\((.+)\\sCPU\\))?")
            .matcher("");
    private static final Pattern DATA_SPLITTER = Pattern.compile(":?\\s+");

    public static final String DEFAULT_HOSTNAME = "iostat";

    private LineNumberReader in = null;

    private SimpleDateFormat format = null;

    private boolean isAIX = false;
    // private boolean isExtendedDiskStats = false;

    // this is a full datetime rounded to midnight
    private long dateOffset = 0;

    private BasicDataSet data = null;
    private DataRecord currentRecord = null;

    private Map<String, List<String>> typesByHeader = new java.util.HashMap<String, List<String>>();

    // private String[] disk_metrics;

    public BasicDataSet parse(File file, TimeZone timeZone) throws IOException, ParseException {
        return parse(file.getAbsolutePath(), timeZone);
    }

    public BasicDataSet parse(String filename, TimeZone timeZone) throws IOException, ParseException {
        long start = System.nanoTime();

        data = new BasicDataSet(filename);
        data.setHostname(DEFAULT_HOSTNAME);

        String line = null;

        try {
            in = new LineNumberReader(new java.io.FileReader(filename));

            parseHeader(timeZone);
            determineTimestampFormat(timeZone);

            line = in.readLine();

            if (isAIX) {
                if (line.startsWith("tty")) {
                    parseAIXTTYAndCPUHeader(line);

                    line = in.readLine();

                    if (line.startsWith("System")) {
                        data.setHostname(line.substring(("System" + ": ").length()));
                        line = in.readLine();
                    }
                }
            }
            else {
                if (line.startsWith("avg-cpu:")) {
                    parseLinuxCPUHeader(line);
                }

                line = in.readLine();
            }

            // line should now be the header row of the first disk / device block
            line = parseDataTypes(line);

            // line should now contain a header row or a time stamp (Linux)
            // read until file is complete
            while (line != null) {
                if (isAIX) {
                    if (line.startsWith("System")) {
                        // skip repeated System rows
                        line = in.readLine();

                        // blank line after 'System configuration'
                        if ("".equals(line)) {
                            line = in.readLine();
                        }

                        continue;
                    }
                }
                else {
                    if (line.startsWith("Time:") || ISO_PATTERN.reset(line).matches()) {
                        createCurrentRecord(line);
                        line = in.readLine();
                        continue;
                    }
                }
                // AIX will create record during TTY parsing

                String[] temp = DATA_SPLITTER.split(line);
                String typeName = temp[0];

                if ("FS".equals(typeName)) {
                    typeName = temp[0] + ' ' + temp[1];
                }

                if ("tty".equals(typeName)) {
                    parseAIXTTYAndCPU();
                }
                else if ("avg-cpu".equals(typeName)) {
                    parseLinuxCPU();
                }
                else if ("".equals(typeName)) {
                    if (line.equals("")) {
                        // if the whole line is empty, skip it
                        line = in.readLine();
                        continue;
                    }
                    else { // otherwise assume leading spaces => AIX summary data
                        parseAIXSummaryData();
                    }
                }
                else {
                    parseData(typeName);
                }

                line = in.readLine(); // next header row
            }

            if (currentRecord != null) {
                data.addRecord(currentRecord);
            }

            return data;
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {
                    // ignore
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Parse complete for {} in {}ms", data.getSourceFile(),
                            (System.nanoTime() - start) / 1000000.0d);
                }

                in = null;
                format = null;
                isAIX = false;
                // isExtendedDiskStats = false;
                dateOffset = 0;
                data = null;
                currentRecord = null;

                typesByHeader.clear();
            }
        }
    }

    private void parseHeader(TimeZone timeZone) throws IOException, ParseException {
        String line = in.readLine(); // header line

        // handle AIX initial blank line and other possible bad formatting
        while ("".equals(line)) {
            line = in.readLine();
        }

        if (line.startsWith("System configuration: ")) { // AIX
            isAIX = true;
            data.setMetadata("OS", "AIX");

            line = line.substring("System configuration: ".length());
            String[] config = line.split("[ =]");

            for (int i = 0; i < config.length; i++) {
                data.setMetadata(DataHelper.newString(config[i]), DataHelper.newString(config[++i]));
            }

            // AIX has no date, use the default
            dateOffset = getDefaultDate();
        }
        else { // Linux
            Matcher matcher = INFO.reset(line);

            if (matcher.matches()) {
                data.setHostname(DataHelper.newString(matcher.group(3)));
                data.setMetadata("OS", DataHelper.newString(matcher.group(1) + ' ' + matcher.group(2)));

                String date = matcher.group(4);
                String arch = matcher.group(6);
                String cpuCount = matcher.group(8);

                SimpleDateFormat dateFormat = DATE_FORMAT_ISO;

                if (date.indexOf('/') != -1) {
                    // handle 2 digit years; note possible year 2100 issue if this code is still in
                    // use!
                    if (date.length() == (DATE_FORMAT_US.toPattern().length() - 2)) {
                        date = date.substring(0, 6) + "20" + date.substring(6);
                    }

                    dateFormat = DATE_FORMAT_US;
                    dateOffset = TimeHelper.dayFromDatetime(dateFormat.parse(date).getTime());
                }
                // else ISO includes date time, so offset can stay 0

                dateFormat.setTimeZone(timeZone);

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

        // shift offset to timezone local, not UTC
        dateOffset += timeZone.getOffset(dateOffset);

        in.readLine(); // blank line after header
    }

    private void determineTimestampFormat(TimeZone timeZone) throws IOException {
        if (isAIX) {
            format = TIMESTAMP_FORMAT_AIX;
            format.setTimeZone(timeZone);
        }
        else {
            String line = in.readLine(); // first timestamp line

            if (line.startsWith("Time: ")) {
                if (ISO_PATTERN.reset(line).matches()) {
                    // some versions of IOStat output Time: _and_ an ISO datetime
                    // create a new format here rather than parsing out Time: manually
                    format = new SimpleDateFormat('\'' + "Time: " + '\'' + TIMESTAMP_FORMAT_ISO.toPattern());
                }
                else {
                    format = TIMESTAMP_FORMAT_OLD;
                    format.setTimeZone(timeZone);
                }
            }
            else {
                try {
                    TIMESTAMP_FORMAT_ISO.parse(line);
                    // ISO format includes a timezone, ignore the one passed in

                    format = TIMESTAMP_FORMAT_ISO;
                }
                catch (ParseException pe) {
                    try {
                        TIMESTAMP_FORMAT_US.parse(line);

                        format = TIMESTAMP_FORMAT_US;
                        format.setTimeZone(timeZone);
                    }
                    catch (ParseException pe2) {
                        throw new IOException("unknown timestamp format");
                    }
                }
            }
        }
    }

    private void parseLinuxCPUHeader(String line) throws IOException {
        // create CPU data type
        String[] temp = DATA_SPLITTER.split(line);
        // subtract 2 since avg-cpu (first column) is not a field
        // also ignore %idle (the last column)
        String[] fields = new String[temp.length - 2];

        for (int i = 0; i < fields.length; i++) {
            fields[i] = DataHelper.newString(temp[i + 1]);
        }

        DataType cpu = new DataType("IOStat CPU", "IOStat Average CPU", fields);
        data.addType(cpu);

        in.readLine(); // summary CPU data
        in.readLine(); // blank line after CPU data
    }

    // tty line in AIX contains terminal and CPU utilization data
    // split that into two DataTypes
    private void parseAIXTTYAndCPUHeader(String line) throws IOException {
        String[] temp = DATA_SPLITTER.split(line);
        List<String> fields = new java.util.ArrayList<String>();

        // i = 0 => tty:
        for (int i = 1; i < temp.length; i++) {
            if ("avg-cpu".equals(temp[i])) {
                // TTY data complete
                DataType tty = new DataType("IOStat TTY", "IOStat terminal", fields.toArray(new String[0]));
                data.addType(tty);
                fields.clear();
            }
            else if ("time".equals(temp[i])) {
                continue;
            }
            else {
                if ("%".equals(temp[i])) {
                    // put '% user', etc back together; ignore idle
                    if ("idle".equals(temp[i + 1])) {
                        ++i;
                    }
                    else {
                        fields.add(DataHelper.newString(temp[i++] + temp[i]));
                    }
                }
                else {
                    fields.add(DataHelper.newString(temp[i]));
                }
            }
        }

        DataType cpu = new DataType("IOStat" + " CPU", "IOStat" + " Average CPU", fields.toArray(new String[0]));
        data.addType(cpu);

        // hack to get parseDataTypes to stop when tty is encountered
        typesByHeader.put("tty", java.util.Arrays.asList("CPU", "TTY"));

        in.readLine(); // summary tty and CPU data
        in.readLine(); // blank line after tty;
    }

    private String parseDataTypes(String line) throws IOException {
        // The first set of data is summary data. Use it to build the DataTypes.

        while (line != null) {
            if (!isAIX && (line.startsWith("Time:") || ISO_PATTERN.reset(line).matches())) {
                // on Linux, headers are complete with the next timestamp
                return line;
            }

            String[] temp = DATA_SPLITTER.split(line);
            String type = temp[0];
            int offset = 1;

            List<String> subDataTypes = typesByHeader.get(type);

            if (subDataTypes != null) {
                if (isAIX) {
                    // assume seeing the same header / subtype again => ready to start parsing data
                    return line;
                }
                else {
                    throw new IOException("duplicate header for " + type + " at line " + in.getLineNumber());
                }
            }
            else {
                if (isAIX) {
                    if ("".equals(type)) {
                        // for Physical / Logical lines, create a single type for each
                        // less 1 on the length to skip time
                        String[] fields = new String[temp.length - (offset + 1)];

                        for (int i = offset; i < temp.length - 1; i++) {
                            fields[i - 1] = DataHelper.newString(temp[i]);
                        }

                        line = in.readLine().trim(); // trim for leading spaces

                        while (!"".equals(line)) {
                            type = DataHelper.newString(DATA_SPLITTER.split(line)[0]);
                            data.addType(new DataType("IOStat " + type, type, fields));

                            line = in.readLine().trim();
                        }

                        line = in.readLine();
                        continue;
                    }
                    // extended disk stats is not supported
                    else if ("Disks".equals(type) && "xfers".equals(temp[1])) {
                        throw new IOException("AIX extended disk statistics (-D) are not currently supported");
                    }
                    else if ("System".equals(type)) {
                        // skip 'System configuration' lines; assume this ends type definitions
                        // skip next blank and return next header row
                        in.readLine();
                        return in.readLine();
                    }
                    else if ("FS".equals(type)) {
                        type = temp[0] + ' ' + temp[1];
                        ++offset;
                    }
                }

                // Each row is a field; each column is a new DataType. This results in a
                // sub-datatype for
                // each metric with a field for each disk.

                // less 1 on the length in AIX to skip the time column
                subDataTypes = new java.util.ArrayList<String>(temp.length - (offset + 1 + (isAIX ? 1 : 0)));
                int end = temp.length - (isAIX ? 1 : 0);

                for (int i = offset; i < end; i++) {
                    if ("%".equals(temp[i])) {
                        // handle spaces after % in AIX
                        subDataTypes.add(DataHelper.newString(temp[i] + temp[++i]));
                    }
                    else {
                        subDataTypes.add(DataHelper.newString(temp[i]));
                    }
                }

                line = in.readLine(); // first field row

                List<String> fields = new java.util.ArrayList<String>();

                // read fields until a blank is encountered
                while (!line.equals("")) {
                    temp = DATA_SPLITTER.split(line);
                    fields.add(DataHelper.newString(temp[0]));

                    line = in.readLine();
                }

                if (fields.size() == 0) {
                    LOGGER.warn("no fields defined for {}; data will be ignored", type);
                }
                else {
                    for (String subType : subDataTypes) {
                        String[] fieldsArray = fields.toArray(new String[fields.size()]);
                        String name = type + ' ' + subType;
                        data.addType(new SubDataType("IOStat " + type, subType, name, false, fieldsArray));
                    }

                    typesByHeader.put(type, subDataTypes);

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("{} section contains {} DataTypes: {}", new Object[] { type, subDataTypes.size(),
                                subDataTypes });
                        LOGGER.trace("{} section has {} {}: {}", new Object[] { type, fields.size(),
                                fields.size() > 1 ? "ies" : "y", fields });
                    }
                }

                line = in.readLine(); // next header line;
            }
        }

        // should only get here on error; i.e. the file ended before header parsing completed
        return line;
    }

    private void parseLinuxCPU() throws IOException {
        String[] temp = DATA_SPLITTER.split(in.readLine());
        // DATA_SPLITTER adds a null first element to temp; ignore it
        // also ignore %idle, the last column
        double[] cpuData = new double[temp.length - 2];

        for (int i = 0; i < cpuData.length; i++) {
            cpuData[i] = Double.parseDouble(temp[i + 1]);
        }

        currentRecord.addData(data.getType("IOStat CPU"), cpuData);

        in.readLine(); // blank line after CPU data
    }

    // parse tty and CPU utilization into different data types
    // also create the current DataRecord and set the time here, if available
    private void parseAIXTTYAndCPU() throws IOException, ParseException {
        // tty header already read
        String[] temp = DATA_SPLITTER.split(in.readLine());

        createCurrentRecord(temp[temp.length - 1]);

        // DATA_SPLITTER adds a null first element to temp; ignore it
        int n = 1;

        DataType tty = data.getType("IOStat TTY");
        double[] ttyData = new double[tty.getFieldCount()];

        for (int i = 0; i < ttyData.length; i++) {
            ttyData[i] = Double.parseDouble(temp[n++]);
        }

        currentRecord.addData(tty, ttyData);

        DataType cpu = data.getType("IOStat CPU");
        double[] cpuData = new double[cpu.getFieldCount()];

        for (int i = 0; i < cpuData.length;) {
            cpuData[i++] = Double.parseDouble(temp[n++]);
        }

        currentRecord.addData(cpu, cpuData);

        in.readLine(); // blank line after tty and CPU data
    }

    // handle AIX Physical / Logical data
    private void parseAIXSummaryData() throws IOException {
        String line = in.readLine();

        while (!"".equals(line)) {
            String[] temp = DATA_SPLITTER.split(line.trim());

            DataType type = data.getType("IOStat " + temp[0]);
            double[] data = new double[type.getFieldCount()];

            for (int i = 0; i < data.length; i++) {
                data[i] = Double.parseDouble(temp[i + 1]);
            }

            currentRecord.addData(type, data);

            line = in.readLine();
        }
    }

    // parse a data 'stanza'; i.e. one type's worth of data
    // contrast this with parseDataTypes which parses all the DataTypes and sub-types before
    // stopping
    private void parseData(String type) throws IOException, ParseException {
        List<String> subTypes = typesByHeader.get(type);

        if (subTypes == null) {
            // type has no fields, ignore
            // no attempt is made to skip any new fields adding during IOStat capture
            return;
        }

        Map<DataType, double[]> dataToAdd = new java.util.HashMap<DataType, double[]>();

        // create data arrays for all subtypes
        for (int i = 0; i < subTypes.size(); i++) {
            String subType = subTypes.get(i);
            DataType dataType = data.getType(SubDataType.buildId("IOStat " + type, subType));
            dataToAdd.put(dataType, new double[dataType.getFieldCount()]);
        }

        int subTypeCount = subTypes.size();

        String line = in.readLine();

        while ((line != null) && !"".equals(line)) {
            String[] temp = DATA_SPLITTER.split(line);
            String field = temp[0];

            // ignore AIX time data for each disk
            int dataLength = temp.length - (isAIX ? 2 : 1);

            if (isAIX && (currentRecord == null)) {
                // no tty data to get a timestamp from
                // use the data record instead; assume time is the last column
                createCurrentRecord(temp[temp.length - 1]);
            }

            if (dataLength > subTypeCount) {
                LOGGER.warn("'{}' at line {} has {} extra columns; they will be ignored",
                        new Object[] { field, in.getLineNumber(), dataLength - subTypeCount });
                dataLength = subTypeCount;
            }
            else if (dataLength < subTypeCount) {
                LOGGER.warn("'{}' at line {} has too few columns; zero will be assumed for missing data", new Object[] {
                        field, in.getLineNumber() });
            }

            for (int i = 0; i < subTypes.size(); i++) {
                String subType = subTypes.get(i);

                DataType dataType = data.getType(SubDataType.buildId("IOStat " + type, subType));
                double[] subTypeData = dataToAdd.get(dataType);

                if (subTypeData == null) {
                    subTypeData = new double[dataType.getFieldCount()];
                    dataToAdd.put(dataType, subTypeData);
                }

                // for each field (column in the file), look up the field index ...
                // and add the current data to the type
                int subTypeIdx = dataType.getFieldIndex(field);
                String data = temp[i + 1];

                if ("-".equals(data)) {
                    subTypeData[subTypeIdx] = Double.NaN;
                }
                else {
                    subTypeData[subTypeIdx] = Double.parseDouble(data);
                }
            }

            line = in.readLine(); // read next data row, if any
        }

        for (DataType dataType : dataToAdd.keySet()) {
            double[] toAdd = dataToAdd.get(dataType);
            currentRecord.addData(dataType, toAdd);
        }
    }

    private void createCurrentRecord(String timeToParse) throws ParseException {
        if (currentRecord != null) {
            data.addRecord(currentRecord);
            currentRecord = null;
        }

        long time = format.parse(timeToParse).getTime() + dateOffset;
        currentRecord = new DataRecord(time, timeToParse);
    }

    public static long getDefaultDate() {
        return TimeHelper.today();
    }
}
