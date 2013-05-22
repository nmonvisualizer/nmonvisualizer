package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

public final class IOStatParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(IOStatParser.class);

    private static final SimpleDateFormat TIMESTAMP_FORMAT_ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZ");
    private static final SimpleDateFormat TIMESTAMP_FORMAT_US = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
    private static final SimpleDateFormat TIMESTAMP_FORMAT_OLD = new SimpleDateFormat("'Time: 'hh:mm:ss a");
    private static final SimpleDateFormat TIMESTAMP_FORMAT_AIX = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat DATE_FORMAT_US = new SimpleDateFormat("MM/dd/yyyy");
    private static final SimpleDateFormat DATE_FORMAT_ISO = new SimpleDateFormat("yyyy-MM-dd");

    private static final Pattern INFO = Pattern
            .compile("(.+)\\s(.+)\\s\\((.+)\\)\\s+(\\d{2}\\/\\d{2}\\/\\d{2,4})(\\s+_(.+)_)?(\\s+\\((.+)\\sCPU\\))?");
    private static final Pattern DATA_SPLITTER = Pattern.compile(":?\\s+");

    private static final Set<Integer> NO_IGNORED_FIELDS = java.util.Collections.emptySet();

    public static final String DEFAULT_HOSTNAME = "iostat";

    private LineNumberReader in = null;

    private SimpleDateFormat format = null;

    private boolean isAIX = false;
    // private boolean isExtendedDiskStats = false;

    // this is a full datetime rounded to midnight
    private long dateOffset = 0;

    private BasicDataSet data = null;
    private DataRecord currentRecord = null;

    private Map<String, List<String>> subDataTypesByType = new java.util.HashMap<String, List<String>>();
    private Map<String, Set<Integer>> fieldsToIgnoreByType = new java.util.HashMap<String, Set<Integer>>();

    private String lastTypeInRecord = null;

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

            if (isAIX) {
                determineTimestampFormat(timeZone);
                line = in.readLine();

                if (line.startsWith("tty")) {
                    parseAIXTTYAndCPUHeader(line);
                    line = "";
                }
            }
            else {
                determineTimestampFormat(timeZone);

                line = in.readLine(); // possible CPU header or Device header with type names

                if (line.startsWith("avg-cpu:")) {
                    parseLinuxCPUHeader(line);
                    line = "";
                }
            }

            line = parseDataTypes(line);

            // line should now contain a header row or a time stamp (Linux)
            // read until file is complete
            while (line != null) {
                if (!isAIX && (currentRecord == null)) {
                    createCurrentRecord(line);
                    line = in.readLine();
                    continue;
                }
                // else AIX will create record during TTY parsing

                String[] temp = DATA_SPLITTER.split(line);
                String type = temp[0];

                if ("tty".equals(type)) {
                    parseAIXTTYAndCPU();
                }
                else if ("avg-cpu".equals(type)) {
                    parseLinuxCPU();
                }
                else {
                    parseData(type);
                }

                if (type.equals(lastTypeInRecord)) {
                    data.addRecord(currentRecord);
                    currentRecord = null;
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

                subDataTypesByType.clear();
                fieldsToIgnoreByType.clear();

                lastTypeInRecord = null;
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
            Matcher matcher = INFO.matcher(line);

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
                }

                dateFormat.setTimeZone(timeZone);
                dateOffset = DataHelper.dayFromDatetime(dateFormat.parse(date).getTime());

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
                format = TIMESTAMP_FORMAT_OLD;
                format.setTimeZone(timeZone);
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
        // and we are ignoring %idle (the last column)
        String[] fields = new String[temp.length - 2];

        for (int i = 0; i < fields.length; i++) {
            fields[i] = DataHelper.newString(temp[i + 1]);
        }

        DataType cpu = new DataType("IOStat CPU", "IOStat Average CPU", fields);
        data.addType(cpu);

        fieldsToIgnoreByType.put("avg-cpu", NO_IGNORED_FIELDS);

        in.readLine(); // summary CPU data
        in.readLine(); // blank line after CPU data
    }

    private void parseAIXTTYAndCPUHeader(String line) throws IOException {
        String[] temp = DATA_SPLITTER.split(line);
        List<String> fields = new java.util.ArrayList<String>();

        Set<Integer> fieldsToIgnore = new java.util.HashSet<Integer>();

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
                    if ("idle".equals(temp[i + 1])) {
                        ++i;
                        fieldsToIgnore.add(fields.size());
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

        fieldsToIgnoreByType.put("avg-cpu", fieldsToIgnore);

        DataType cpu = new DataType("IOStat CPU", "IOStat Average CPU", fields.toArray(new String[0]));
        data.addType(cpu);

        // hack to get parseDataTypes to stop when tty is encountered
        subDataTypesByType.put("tty", java.util.Arrays.asList("CPU", "TTY"));

        in.readLine(); // summary tty and CPU data
        in.readLine(); // blank line after tty;
    }

    private String parseDataTypes(String line) throws IOException {
        // The first set of data is summary data. Use it to build the DataTypes.
        // Each row is a field; each column is a new DataType.

        if ("".equals(line)) {
            line = in.readLine();
        }
        // line should now be the header row

        while (line != null) {
            String[] temp = DATA_SPLITTER.split(line);
            String type = temp[0];

            List<String> subDataTypes = subDataTypesByType.get(type);

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
                    // extended disk stats require special handling
                    if ("Disks".equals(type) && "xfers".equals(temp[1])) {
                        // in.readLine(); // formatted line full of dashes (----)
                        // temp = DATA_SPLITTER.split(in.readLine()); // actual type names, 1st row
                        // String temp2[] = DATA_SPLITTER.split(in.readLine()); // actual type
                        // names, 1st row
                        // isExtendedDiskStats = true;
                        throw new IOException("AIX extended disk statistics (-D) are not currently supported");
                    }

                    if ("System".equals(type)) {
                        // AIX System (-s) has the hostname in the first line
                        // headers are on the next line
                        data.setHostname(DataHelper.newString(temp[1]));
                        temp = DATA_SPLITTER.split(in.readLine());
                    }
                }
                else {
                    // on Linux, the headers are complete when the next time stamp is hit
                    try {
                        format.parse(line);
                        return line;
                    }
                    catch (ParseException pe) {
                        // ignore
                    }
                }

                lastTypeInRecord = temp[0];
                subDataTypes = new java.util.ArrayList<String>(temp.length - 1);

                for (int i = 1; i < temp.length; i++) {
                    if ("%".equals(temp[i])) {
                        // handle spaces after % in AIX
                        subDataTypes.add(DataHelper.newString(temp[i] + temp[++i]));
                    }
                    else if ("time".equals(temp[i])) {
                        // skip AIX time fields
                        continue;
                    }
                    else {
                        subDataTypes.add(DataHelper.newString(temp[i]));
                    }
                }

                line = in.readLine(); // first field row

                List<String> fields = new java.util.ArrayList<String>();

                // read fields until a blank is encountered
                while (!line.equals("")) {
                    temp = DATA_SPLITTER.split(line.trim());
                    fields.add(DataHelper.newString(temp[0]));

                    line = in.readLine();
                }

                if (fields.size() == 0) {
                    LOGGER.warn("no fields defined for {}; data will be ignored", type);
                }
                else {
                    for (String subType : subDataTypes) {
                        String name = type + ' ' + subType;
                        String[] fieldsArray = fields.toArray(new String[fields.size()]);
                        data.addType(new SubDataType("IOStat " + type, subType, name, false, fieldsArray));
                    }

                    subDataTypesByType.put(type, subDataTypes);

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("{} section contains {} DataTypes: {}", new Object[] { type, subDataTypes.size(),
                                subDataTypes });
                        LOGGER.trace("{} section has {} {}: {}", new Object[] { type, fields.size(),
                                fields.size() > 1 ? "ies" : "y", fields });
                    }
                }

                fieldsToIgnoreByType.put(type, NO_IGNORED_FIELDS);
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

    private void parseAIXTTYAndCPU() throws IOException, ParseException {
        // tty header already read
        String[] temp = DATA_SPLITTER.split(in.readLine());

        long time = format.parse(temp[temp.length - 1]).getTime() + dateOffset;
        currentRecord = new DataRecord(time, String.format("%08x", data.getRecordCount()));

        // DATA_SPLITTER adds a null first element to temp; ignore it
        int n = 1;

        DataType tty = data.getType("TTY");
        double[] ttyData = new double[tty.getFieldCount()];

        for (int i = 0; i < ttyData.length; i++) {
            ttyData[i] = Double.parseDouble(temp[n++]);
        }

        currentRecord.addData(tty, ttyData);

        DataType cpu = data.getType("IOStat CPU");
        double[] cpuData = new double[cpu.getFieldCount()];
        Set<Integer> typesToIgnore = fieldsToIgnoreByType.get("avg-cpu");
        int cpuStartIdx = n;

        for (int i = 0; i < cpuData.length;) {
            if (typesToIgnore.contains(n - cpuStartIdx)) {
                n++;
                continue;
            }
            else {
                cpuData[i++] = Double.parseDouble(temp[n++]);
            }
        }

        currentRecord.addData(cpu, cpuData);

        in.readLine(); // blank line after tty and CPU data
    }

    private void parseData(String type) throws IOException, ParseException {
        List<String> subTypes = subDataTypesByType.get(type);

        String line = "";

        if (subTypes == null) {
            // ignore data block

            do {
                line = in.readLine();
            }
            while (!"".equals(line));

            return;
        }

        if ("System".equals(type)) {
            // AIX System data (-s) has and additional header row
            in.readLine();
            // first data row starts with spaces; trim to ensure field is temp[0]
            line = in.readLine().trim();
        }
        else {
            line = in.readLine(); // first data row
        }

        int subTypeCount = subTypes.size();
        Set<Integer> fieldsToIgnore = fieldsToIgnoreByType.get(type);

        Map<DataType, double[]> dataToAdd = new java.util.HashMap<DataType, double[]>();

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
                LOGGER.warn("{} '{}' at line {} has {} extra columns; they will be ignored", new Object[] { type,
                        field, in.getLineNumber(), dataLength - subTypeCount });
                dataLength = subTypeCount;
            }
            else if (dataLength < subTypeCount) {
                LOGGER.warn("{} '{}' at line {} has too few columns; zero will be assumed for missing data",
                        new Object[] { type, field, in.getLineNumber() });
            }

            for (int i = 1, n = 0; i <= dataLength; i++) {
                if (fieldsToIgnore.contains(i)) {
                    continue;
                }

                String subType = subTypes.get(n++);
                DataType dataType = data.getType(SubDataType.buildId("IOStat " + type, subType));
                double[] subTypeData = dataToAdd.get(dataType);

                if (subTypeData == null) {
                    subTypeData = new double[dataType.getFieldCount()];
                    dataToAdd.put(dataType, subTypeData);
                }

                // for each field (column in the file), look up the field index ...
                int subTypeIdx = dataType.getFieldIndex(field);

                // and add the current data to the type typeData[diskIdx] =
                subTypeData[subTypeIdx] = Double.parseDouble(temp[i]);
            }

            line = in.readLine(); // read next data row, if any
        }

        for (DataType dataType : dataToAdd.keySet()) {
            double[] toAdd = dataToAdd.get(dataType);
            currentRecord.addData(dataType, toAdd);
        }
    }

    private void createCurrentRecord(String timeToParse) throws ParseException {
        long time = format.parse(timeToParse).getTime() + dateOffset;
        currentRecord = new DataRecord(time, timeToParse);
    }

    public static long getDefaultDate() {
        return DataHelper.today();
    }
}
