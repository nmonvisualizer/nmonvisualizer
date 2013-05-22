package com.ibm.nmon;

import java.io.IOException;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;

import java.io.BufferedReader;

import java.text.SimpleDateFormat;

import java.util.List;
import java.util.Set;

import com.ibm.nmon.data.DataSetListener;
import com.ibm.nmon.data.DataSet;
import com.ibm.nmon.data.ProcessDataSet;

import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.data.Process;

import com.ibm.nmon.file.CombinedFileFilter;

import com.ibm.nmon.util.FileHelper;

public final class NMONVisualizerCmdLine extends NMONVisualizerApp implements DataSetListener {
    private static final SimpleDateFormat OUTPUT_FORMAT = new SimpleDateFormat("MM/dd/yy,HH:mm:ss,");

    public static void main(String[] args) throws Exception {
        // initialize logging from the classpath properties file
        java.util.logging.LogManager.getLogManager().readConfiguration(
                NMONVisualizerCmdLine.class.getResourceAsStream("/cmdline.logging.properties"));

        NMONVisualizerCmdLine app = new NMONVisualizerCmdLine();
        app.addDataSetListener(app);

        File pathToParse = getPathToParse(args);
        List<String> toParse = new java.util.ArrayList<String>();

        FileHelper.recurseDirectories(java.util.Collections.singletonList(pathToParse),
                CombinedFileFilter.getInstance(false), toParse);

        if (toParse.size() == 0) {
            if (pathToParse.isDirectory()) {
                System.err.println("no files to parse in " + pathToParse);
            }
            else {
                System.err.println(pathToParse + " is not a recognized file");
            }
            return;
        }

        for (String file : toParse) {
            System.out.print("Parsing file " + file + "... ");
            System.out.flush();

            try {
                app.parse(file, java.util.TimeZone.getDefault());

                System.out.println("Complete");
            }
            catch (Exception e) {
                System.out.println("Failed parsing " + file);
                e.printStackTrace();
            }
        }

        System.out.println();

        String outputDir = "";
        String baseName = "";

        if (pathToParse.isDirectory()) {
            outputDir = pathToParse.getAbsolutePath();
            baseName = "consolidated";
        }
        else {
            outputDir = pathToParse.getParentFile().getAbsolutePath();
            baseName = pathToParse.getName();

            int idx = baseName.lastIndexOf('.');

            if (idx != -1) {
                baseName = baseName.substring(0, idx);
            }
        }

        createMasterDetailFile(outputDir, baseName, app);

        // if any data set has process data, output the process data files
        for (DataSet data : app.getDataSets()) {
            if ((data instanceof ProcessDataSet) && (((ProcessDataSet) data).getProcessCount() > 0)) {
                createMasterDetailProcessFile(outputDir, baseName, app);
                createCommandsFile(outputDir, baseName, app);
                break;
            }
        }
    }

    private static File getPathToParse(String[] args) throws IOException {
        String pathname;

        if (args.length < 1) {
            System.out.print("What is the path and/or base name you would like to analyze?: ");
            System.out.flush();

            BufferedReader sysin = new BufferedReader(new java.io.InputStreamReader(System.in));
            pathname = sysin.readLine();
            sysin.close();
        }
        else {
            pathname = args[0];
        }

        pathname = pathname.replace("\\", "/");

        return new File(pathname);
    }

    // Output a CSV file with 1 row for each timestamp / hostname combination. Each row will have a
    // column for each metric being output. This allows LoadRunner to read in a single file with all
    // hosts in it, at the expense of being able to sort by a specific metric since the metric name
    // will include the hostname. An alternative, which requires no code change, is to parse each
    // file individually them import each CSV separately into LoadRunner.
    private static void createMasterDetailFile(String outputDir, String baseName, NMONVisualizerCmdLine app)
            throws IOException {
        String outputFile = outputDir + '/' + baseName + "_nmon.csv";

        List<DataType> typesToOutput = getTypesToOutput(app);

        System.out.print("Writing NMON data to master-detail CSV file to " + outputFile + "... ");
        System.out.flush();
        PrintWriter out = new PrintWriter(new FileWriter(outputFile));

        StringBuilder builder = new StringBuilder(512);

        // output master-detail header
        // with 1 column for each output field
        builder.append("Date,Time,System,");

        for (DataType type : typesToOutput) {
            for (String field : type.getFields()) {
                builder.append(type.getId());
                builder.append('-');
                builder.append(field);
                builder.append(',');
            }
        }

        // remove trailing comma
        builder.deleteCharAt(builder.length() - 1);

        out.println(builder);

        // for each time monitored, output the data for each file, field by field
        for (long time : app.timesMonitored) {
            String dateTime = OUTPUT_FORMAT.format(new java.util.Date(time));

            for (DataSet data : app.getDataSets()) {
                DataRecord record = data.getRecord(time);

                // no record for the given time, continue to the next host
                if (record == null) {
                    continue;
                }

                builder.setLength(0);

                builder.append(dateTime);
                builder.append(data.getHostname());
                builder.append(',');

                for (DataType type : typesToOutput) {
                    DataType hostType = data.getType(type.getId());

                    for (String field : type.getFields()) {
                        if (hostType != null) {
                            // note lookup by host type here
                            // since this host may not have all the values being searched
                            if (hostType.hasField(field)) {
                                builder.append(record.getData(hostType, field));
                            }
                        }

                        // note if host does not even have the DataType, still output the correct
                        // number of commas
                        builder.append(',');
                    }
                }

                // remove trailing comma
                builder.deleteCharAt(builder.length() - 1);

                out.println(builder);
            }
        }

        out.close();

        System.out.println("Complete");
    }

    // build a hardcoded set of data to output
    private static List<DataType> getTypesToOutput(NMONVisualizerCmdLine app) {
        Set<String> networks = new java.util.HashSet<String>();
        Set<String> disks = new java.util.HashSet<String>();

        for (DataSet data : app.getDataSets()) {
            // all network data for ethernet devices
            DataType parsedNetwork = data.getType("NET");

            for (int i = 0; i < parsedNetwork.getFieldCount(); i++) {
                String field = parsedNetwork.getField(i);

                if (field.startsWith("eth") || field.startsWith("en")) {
                    networks.add(field);
                }
            }

            // add all the disks from each NMON file
            // adding for each metric is probably unnecessary since the disks reported on _should_
            // be the same for each, but it does not hurt here to be sure
            disks.addAll(data.getType("DISKBUSY").getFields());
            disks.addAll(data.getType("DISKREAD").getFields());
            disks.addAll(data.getType("DISKWRITE").getFields());
            disks.addAll(data.getType("DISKXFER").getFields());
        }

        // add generated total value for all disk metrics
        disks.add("Total");

        List<DataType> types = new java.util.ArrayList<DataType>(5);

        types.add(new DataType("CPU_ALL", "CPU Total", "User%", "Sys%", "Wait%", "CPU%"));
        types.add(new DataType("NET", "Network I/O", networks.toArray(new String[0])));

        String[] diskNames = disks.toArray(new String[0]);
        types.add(new DataType("DISKBUSY", "Disk %Busy", diskNames));
        types.add(new DataType("DISKREAD", "Disk Read KB/s", diskNames));
        types.add(new DataType("DISKWRITE", "Disk Write KB/s", diskNames));
        types.add(new DataType("DISKXFER", "Disk transfers per second", diskNames));

        // Linux
        types.add(new DataType("VM", "Paging and Virtual Memory", "pgpgin", "pgpgout", "pswpin", "pswpout"));
        // AIX
        types.add(new DataType("PAGE", "Paging and Virtual Memory", "pgin", "pgout", "pgsin", "pgsout"));

        types.add(new DataType("MEM", "Memory MB", "memfree", "Real free(MB)"));

        types.add(new DataType("PROC", "Processes", "Runnable", "Swap-in", "pswitch", "syscall"));

        return types;
    }

    // Output a CSV file with 1 row for each timestamp / hostname combination. Each row will have a
    // column with the value 'host-command-pid' followed by a column with the CPU for that command
    private static void createMasterDetailProcessFile(String outputDir, String baseName, NMONVisualizerCmdLine app)
            throws IOException {

        List<ProcessDataSet> dataSets = new java.util.ArrayList<ProcessDataSet>(app.getDataSetCount());

        for (DataSet dataSet : app.getDataSets()) {
            if (dataSet instanceof ProcessDataSet) {
                dataSets.add((ProcessDataSet) dataSet);
            }
        }

        String outputFile = outputDir + '/' + baseName + "_top.csv";

        System.out.print("Writing TOP data to master-detail CSV file to " + outputFile + "... ");
        System.out.flush();
        PrintWriter out = new PrintWriter(new FileWriter(outputFile));

        StringBuilder builder = new StringBuilder(512);

        builder.append("Date,Time,Command,CPU%");
        out.println(builder);

        for (long time : app.timesMonitored) {
            for (DataSet data : dataSets) {
                DataRecord record = data.getRecord(time);

                if (record == null) {
                    continue;
                }

                for (Process p : ((ProcessDataSet) data).getProcesses()) {
                    DataType type = ((ProcessDataSet) data).getType(p);

                    if (!record.hasData(type)) {
                        continue;
                    }

                    builder.setLength(0);

                    builder.append(OUTPUT_FORMAT.format(new java.util.Date(time)));
                    builder.append(data.getHostname());
                    builder.append('-');
                    builder.append(p.getName());
                    builder.append('-');
                    if (p.getId() == -1) {
                        builder.append("ALL");
                    }
                    else {
                        builder.append(Integer.toString(p.getId()));
                    }
                    builder.append(',');
                    builder.append(record.getData(type, "%CPU"));

                    out.println(builder);
                }
            }
        }

        out.close();

        System.out.println("Complete");
    }

    // Output a CSV file with 1 row for each timestamp / hostname combination. Each row will have a
    // column with the value 'host-command-pid' followed by a column with the name of the command
    // then final column with the full command line
    private static void createCommandsFile(String outputDir, String baseName, NMONVisualizerCmdLine app)
            throws IOException {

        List<ProcessDataSet> dataSets = new java.util.ArrayList<ProcessDataSet>(app.getDataSetCount());

        for (DataSet dataSet : app.getDataSets()) {
            if (dataSet instanceof ProcessDataSet) {
                dataSets.add((ProcessDataSet) dataSet);
            }
        }

        String outputFile = outputDir + '/' + baseName + "_commands.csv";

        System.out.print("Writing command data to CSV file to " + outputFile + "... ");
        System.out.flush();
        PrintWriter out = new PrintWriter(new FileWriter(outputFile));

        StringBuilder builder = new StringBuilder(512);

        builder.append("Host,PID,Command,Command Line");
        out.println(builder);

        for (ProcessDataSet data : dataSets) {
            for (Process p : data.getProcesses()) {
                if (p.getId() == -1) {
                    continue;
                }

                builder.setLength(0);

                builder.append(data.getHostname());
                builder.append(',');
                builder.append(p.getId());
                builder.append(',');
                builder.append(p.getName());
                builder.append(',');
                builder.append(p.getCommandLine());

                out.println(builder);
            }
        }

        out.close();

        System.out.println("Complete");
    }

    private final Set<Long> timesMonitored = new java.util.TreeSet<Long>();

    public NMONVisualizerCmdLine() throws Exception {
        super();
    }

    @Override
    public void dataAdded(DataSet data) {
        timesMonitored.addAll(data.getTimes());
    }

    @Override
    public void dataRemoved(DataSet data) {}

    @Override
    public void dataChanged(DataSet data) {}

    @Override
    public void dataCleared() {}
}