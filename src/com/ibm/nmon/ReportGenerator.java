package com.ibm.nmon;

import java.io.IOException;

import java.io.File;
import java.io.FileFilter;

import com.ibm.nmon.file.HATJFileFilter;
import com.ibm.nmon.file.NMONFileFilter;

import java.io.FileOutputStream;
import java.io.PrintStream;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import java.util.List;
import java.util.Map;

import com.ibm.nmon.util.ParserLog;

import com.ibm.nmon.interval.Interval;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.report.ReportFactory;
import com.ibm.nmon.chart.definition.BaseChartDefinition;

import com.ibm.nmon.util.FileHelper;

import com.ibm.nmon.util.TimeFormatCache;
import com.ibm.nmon.util.TimeHelper;

public final class ReportGenerator extends NMONVisualizerApp {
    private static final SimpleDateFormat FILE_TIME_FORMAT = new SimpleDateFormat("HHmmss");

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("no path(s) to parse specified");
            return;
        }

        // ensure the Swing GUI does not pop up or cause XWindows errors
        System.setProperty("java.awt.headless", "true");

        try {
            // initialize logging from the classpath properties file
            java.util.logging.LogManager.getLogManager().readConfiguration(
                    ReportGenerator.class.getResourceAsStream("/cmdline.logging.properties"));
        }
        catch (IOException ioe) {
            System.err.println("cannot initialize logging, will output to System.out");
            ioe.printStackTrace();
        }

        List<String> paths = new java.util.ArrayList<String>();

        List<String> customDataCharts = new java.util.ArrayList<String>();
        List<String> customSummaryCharts = new java.util.ArrayList<String>();
        List<String> multiplexedFieldCharts = new java.util.ArrayList<String>();
        List<String> multiplexedTypeCharts = new java.util.ArrayList<String>();

        String intervalsFile = "";

        boolean summaryCharts = true;
        boolean dataSetCharts = true;

        long startTime = Interval.DEFAULT.getStart();
        long endTime = Interval.DEFAULT.getEnd();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            char c = arg.charAt(0);

            if (c == '-') {
                nextarg: for (int j = 1; j < arg.length(); j++) {
                    c = arg.charAt(j);

                    switch (c) {
                    case 's':
                        try {
                            startTime = parseTime(args, ++i, 's');
                            break nextarg;

                        }
                        catch (IllegalArgumentException iae) {
                            System.err.println(iae.getMessage());
                            return;
                        }
                    case 'e':
                        try {
                            endTime = parseTime(args, ++i, 'e');
                            break nextarg;
                        }
                        catch (IllegalArgumentException iae) {
                            System.err.println(iae.getMessage());
                            return;
                        }
                    case 'd': {
                        ++i;

                        if (i > args.length) {
                            System.err.println("file must be specified for " + '-' + 'd');
                            return;
                        }

                        customDataCharts.add(args[i]);
                        break nextarg;
                    }
                    case 'a': {
                        ++i;

                        if (i > args.length) {
                            System.err.println("file must be specified for " + '-' + 's');
                            return;
                        }

                        customSummaryCharts.add(args[i]);
                        break nextarg;

                    }
                    case 'i': {
                        ++i;

                        if (i > args.length) {
                            System.err.println("file must be specified for " + '-' + 's');
                            return;
                        }

                        intervalsFile = args[i];
                        break nextarg;
                    }
                    case '-': {
                        if (j == 1) { // --param
                            String param = arg.substring(2);

                            if ("nodata".equals(param)) {
                                dataSetCharts = false;
                            }
                            else if ("nosummary".equals(param)) {
                                summaryCharts = false;
                            }
                            else if ("mf".equals(param)) {
                                ++i;

                                if (i > args.length) {
                                    System.err.println("file must be specified for " + '-' + '-' + "mf");
                                    return;
                                }

                                multiplexedFieldCharts.add(args[i]);
                            }
                            else if ("mt".equals(param)) {
                                ++i;

                                if (i > args.length) {
                                    System.err.println("file must be specified for " + '-' + '-' + "mt");
                                    return;
                                }

                                multiplexedTypeCharts.add(args[i]);
                            }
                            else {
                                System.err.println("ignoring " + "unknown parameter " + '-' + '-' + param);
                            }

                            break nextarg;
                        }
                        else {
                            System.err.println("ignoring " + "misplaced dash in " + arg);
                            break;
                        }
                    }
                    default:
                        System.err.println("ignoring " + "unknown parameter " + '-' + c);
                    }
                }
            }
            else {
                // arg does not start with '-', assume file / directory
                paths.add(arg);
            }
        }

        if (!summaryCharts && !dataSetCharts && customDataCharts.isEmpty() && customSummaryCharts.isEmpty()) {
            System.err.println("--" + "nodata" + " and " + "--" + "nosummary"
                    + " were specifed but no custom chart definitions (-d or -a) were given");
            return;
        }

        if (paths.isEmpty()) {
            System.err.println("no path(s) to parse specified");
            return;
        }

        List<String> filesToParse = new java.util.ArrayList<String>();

        // find all NMON files
        for (String path : paths) {
            File pathToParse = new File(path);

            FileHelper.recurseDirectories(java.util.Collections.singletonList(pathToParse), reportFileFilter,
                    filesToParse);

            if (filesToParse.isEmpty()) {
                System.err.println('\'' + pathToParse.toString() + "' contains no parsable files");
                return;
            }
        }

        File pathToParse = null;

        if (paths.size() == 1) {
            // all subsequent output goes into the directory given by the user
            pathToParse = new File(paths.get(0));
        }
        else {
            // otherwise, use the current working directory
            pathToParse = new File(System.getProperty("user.dir"));
        }

        ReportGenerator generator = new ReportGenerator();

        File baseDirectory = pathToParse.isDirectory() ? pathToParse : pathToParse.getParentFile();

        // parse intervals
        if (!"".equals(intervalsFile)) {
            try {
                generator.getIntervalManager().loadFromFile(new File(intervalsFile), 0);
            }
            catch (IOException ioe) {
                System.err.println("cannot load intervals from '" + intervalsFile + "'");
                ioe.printStackTrace();
            }
        }

        // parse files
        generator.parse(filesToParse, baseDirectory);

        // set interval after parse so min and max system times are set
        createIntervalIfNecessary(startTime, endTime, generator);

        System.out.println();

        // load chart definitions
        for (String file : customSummaryCharts) {
            generator.parseChartDefinition(file);
        }

        for (String file : customDataCharts) {
            generator.parseChartDefinition(file);
        }

        for (String file : multiplexedFieldCharts) {
            generator.parseChartDefinition(file);
        }

        for (String file : multiplexedTypeCharts) {
            generator.parseChartDefinition(file);
        }

        if (generator.getIntervalManager().getIntervalCount() != 0) {
            // create charts for all intervals
            for (Interval interval : generator.getIntervalManager().getIntervals()) {
                generator.createReport(interval, baseDirectory, summaryCharts, dataSetCharts, customSummaryCharts,
                        customDataCharts, multiplexedFieldCharts, multiplexedTypeCharts);
            }
        }
        else {
            generator.createReport(Interval.DEFAULT, baseDirectory, summaryCharts, dataSetCharts, customSummaryCharts,
                    customDataCharts, multiplexedFieldCharts, multiplexedTypeCharts);
        }

        System.out.println();
        System.out.println("Charts complete!");
    }

    private static final FileFilter reportFileFilter = new FileFilter() {
        private final FileFilter nmonFilter = new NMONFileFilter();
        private final FileFilter hatjFilter = new HATJFileFilter();

        public boolean accept(File pathname) {
            return nmonFilter.accept(pathname) || hatjFilter.accept(pathname);
        };
    };

    private static long parseTime(String[] args, int index, char param) {
        if (index > args.length) {
            throw new IllegalArgumentException("time must be specified for " + '-' + param);
        }

        try {
            return TimeHelper.TIMESTAMP_FORMAT_ISO.parse(args[index]).getTime();
        }
        catch (ParseException pe) {
            throw new IllegalArgumentException("time specified for " + '-' + param + " (" + args[index]
                    + ") is not valid");
        }
    }

    private static void createIntervalIfNecessary(long startTime, long endTime, ReportGenerator generator) {
        if (startTime == Interval.DEFAULT.getStart()) {
            startTime = generator.getMinSystemTime();
        }

        if (endTime == Interval.DEFAULT.getEnd()) {
            endTime = generator.getMaxSystemTime();
        }

        Interval toChart = null;

        if ((startTime == generator.getMinSystemTime() && (endTime == generator.getMaxSystemTime()))) {
            toChart = Interval.DEFAULT;
        }
        else {
            try {
                toChart = new Interval(startTime, endTime);
            }
            catch (Exception e) {
                System.err.println("invalid start and end times: " + e.getMessage());
                System.err.println("The default interval will be used instead");
                toChart = Interval.DEFAULT;
            }
        }

        generator.getIntervalManager().addInterval(toChart);
    }

    private final ReportFactory factory;
    private final ReportFactory.ReportFactoryCallback callback;

    private ReportGenerator() {
        factory = new ReportFactory(this);

        callback = new ReportFactory.ReportFactoryCallback() {
            @Override
            public void onChartDefinitionAdded(String key, String definitionPath) {
                System.out.println("Loaded chart definitions from '" + definitionPath + '\'');
            }

            @Override
            public void onChartDefinitionFailure(String key, String definitionPath, IOException ioe) {
                System.err.println("Could not load chart definitions from '" + definitionPath + '\'');
                ioe.printStackTrace();
            }

            @Override
            public void beforeCreateCharts(String chartDefinitionKey, List<DataSet> data, String savePath) {
                if (ReportFactory.DEFAULT_SUMMARY_CHARTS_KEY.equals(chartDefinitionKey)) {
                    System.out.print("\tCreating summary charts ");
                }
                else if (ReportFactory.DEFAULT_DATASET_CHARTS_KEY.equals(chartDefinitionKey)) {
                    System.out.print("\tCreating charts for " + data.get(0).getHostname() + " ");
                }
                else {
                    System.out.print("\tCreating charts for " + chartDefinitionKey);
                }
            }

            @Override
            public void onCreateChart(BaseChartDefinition definition, String savePath) {
                System.out.print('.');
                System.out.flush();
            }

            @Override
            public void afterCreateCharts(String chartDefinitionKey, List<DataSet> data, String savePath) {
                System.out.println(" Complete");
            }
        };
    }

    private void parse(List<String> filesToParse, File baseDirectory) {
        // avoid logging parsing errors to console
        java.util.logging.Logger.getLogger(ParserLog.getInstance().getLogger().getName()).setUseParentHandlers(false);

        ParserLog log = ParserLog.getInstance();
        Map<String, String> errors = new java.util.LinkedHashMap<String, String>();

        System.out.println("Parsing NMON files...");

        for (String fileToParse : filesToParse) {
            System.out.print("\t" + fileToParse + "... ");
            System.out.flush();

            log.setCurrentFilename(fileToParse);

            try {
                parse(fileToParse, getDisplayTimeZone());

                if (log.hasData()) {
                    System.out.println("Complete with errors!");
                }
                else {
                    System.out.println("Complete");
                }
            }
            catch (Exception e) {
                log.getLogger().error("could not parse " + fileToParse, e);
                System.out.println("Complete with errors!");
                // continue parsing other files
            }

            if (log.hasData()) {
                errors.put(log.getCurrentFilename(), log.getMessages());
            }
        }

        System.out.println("Parsing complete!");

        if (!errors.isEmpty()) {
            File errorFile = new File(baseDirectory, "ReportGenerator_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis()) + ".log");

            PrintStream out = null;
            try {
                out = new PrintStream(new FileOutputStream(errorFile));
            }
            catch (IOException io) {
                System.err.println("could not create error log file '" + errorFile.getAbsolutePath()
                        + "'; no output will be logged");
                return;
            }
            for (String filename : errors.keySet()) {
                out.print(filename);
                out.println(':');
                out.println(errors.get(filename));
            }

            out.close();

            System.out.println();
            System.out.println("Errors written to " + errorFile);
        }

        // reset specifically so errors in ChartDefinitionParser _will_ go to the console
        java.util.logging.Logger.getLogger(ParserLog.getInstance().getLogger().getName()).setUseParentHandlers(true);
    }

    private void parseChartDefinition(String definitionFile) {
        // use the file name as the key
        factory.addChartDefinition(definitionFile, definitionFile, callback);
    }

    private void createReport(Interval interval, File baseDirectory, boolean summaryCharts, boolean dataSetCharts,
            List<String> customSummaryCharts, List<String> customDataCharts, List<String> multiplexedFieldCharts,
            List<String> multiplexedTypeCharts) {
        System.out.println();

        getIntervalManager().setCurrentInterval(interval);

        System.out.println("Charting interval " + TimeFormatCache.formatInterval(interval));

        File chartDirectory = null;

        // put in base charts directory if the default interval or there is only a single interval
        if (getIntervalManager().getIntervalCount() <= 1) {
            chartDirectory = new File(baseDirectory, "charts");
        }
        else {
            // use the interval name if possible
            if ("".equals(interval.getName())) {
                chartDirectory = new File(baseDirectory, "charts" + '/'
                        + FILE_TIME_FORMAT.format(new Date(interval.getStart())) + '-'
                        + FILE_TIME_FORMAT.format(new Date(interval.getEnd())));
            }
            else {
                chartDirectory = new File(baseDirectory, "charts" + '/' + interval.getName());
            }
        }

        System.out.println("Writing charts to " + chartDirectory);

        if (summaryCharts) {
            factory.createChartsAcrossDataSets("summary", chartDirectory, callback);
        }

        if (dataSetCharts) {
            factory.createChartsForEachDataSet("dataset", chartDirectory, callback);
        }

        for (String file : customSummaryCharts) {
            factory.createChartsAcrossDataSets(file, chartDirectory, callback);
        }

        for (String file : customDataCharts) {
            factory.createChartsForEachDataSet(file, chartDirectory, callback);
        }

        for (String file : multiplexedFieldCharts) {
            factory.multiplexChartsAcrossFields(file, chartDirectory, callback);
        }

        for (String file : multiplexedTypeCharts) {
            factory.multiplexChartsAcrossTypes(file, chartDirectory, callback);
        }
    }
}
