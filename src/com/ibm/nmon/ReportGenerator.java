package com.ibm.nmon;

import java.io.IOException;

import java.io.File;

import java.io.FileFilter;

import com.ibm.nmon.file.HATJFileFilter;
import com.ibm.nmon.file.NMONFileFilter;

import java.io.InputStream;

import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.io.PrintStream;

import java.util.List;
import java.util.Map;

import com.ibm.nmon.util.ParserLog;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;

import com.ibm.nmon.interval.Interval;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.parser.ChartDefinitionParser;
import com.ibm.nmon.chart.definition.BaseChartDefinition;

import com.ibm.nmon.gui.chart.ChartFactory;

import com.ibm.nmon.util.FileHelper;
import com.ibm.nmon.util.GranularityHelper;
import com.ibm.nmon.util.TimeHelper;

public final class ReportGenerator extends NMONVisualizerApp {
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
                    NMONVisualizerCmdLine.class.getResourceAsStream("/cmdline.logging.properties"));
        }
        catch (IOException ioe) {
            System.err.println("cannot initialize logging, will output to System.out");
            ioe.printStackTrace();
        }

        List<String> paths = new java.util.ArrayList<String>();

        List<String> customDataCharts = new java.util.ArrayList<String>();
        List<String> customSummaryCharts = new java.util.ArrayList<String>();

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
                    case '-': {
                        if (j == 1) { // --param
                            String param = arg.substring(2);

                            if ("nodata".equals(param)) {
                                dataSetCharts = false;
                            }
                            else if ("nosummary".equals(param)) {
                                summaryCharts = false;
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
        generator.setBaseDirectory(pathToParse.isDirectory() ? pathToParse : pathToParse.getParentFile());
        generator.parse(filesToParse);
        generator.setInterval(startTime, endTime);

        System.out.println();
        System.out.println("Writing charts to " + generator.getChartDirectory());
        System.out.println("Charting from "
                + TimeHelper.TIMESTAMP_FORMAT_ISO.format(new java.util.Date(generator.getIntervalManager()
                        .getCurrentInterval().getStart()))
                + " to "
                + TimeHelper.TIMESTAMP_FORMAT_ISO.format(new java.util.Date(generator.getIntervalManager()
                        .getCurrentInterval().getEnd())));

        if (summaryCharts) {
            System.out.print("\tCreating summary charts ");
            System.out.flush();

            generator.createChartsAcrossDataSets(ReportGenerator.class
                    .getResourceAsStream("/com/ibm/nmon/gui/report/summary_single_interval.xml"));
        }

        if (dataSetCharts) {
            generator.createChartsForEachDataSet(ReportGenerator.class
                    .getResourceAsStream("/com/ibm/nmon/gui/report/dataset_report.xml"));
        }

        for (String file : customSummaryCharts) {
            System.out.print("\tCreating charts for " + file);
            System.out.flush();

            InputStream in = null;

            try {
                in = new FileInputStream(file);
            }
            catch (IOException ioe) {
                System.err.println("cannot load chart definition '" + file + "'");
                ioe.printStackTrace();
                continue;
            }

            generator.createChartsAcrossDataSets(in);
        }

        for (String file : customDataCharts) {
            System.out.print("\tCreating charts for " + file);
            System.out.flush();

            InputStream in = null;
            try {
                in = new FileInputStream(file);
            }
            catch (IOException ioe) {
                System.err.println("cannot load chart definition '" + file + "'");
                ioe.printStackTrace();
                continue;
            }

            generator.createChartsForEachDataSet(in);
        }

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

    private final ChartDefinitionParser parser = new ChartDefinitionParser();
    private final GranularityHelper granularityHelper;
    private final ChartFactory chartFactory;

    private File baseDirectory;
    private File chartDirectory;

    private ReportGenerator() {
        super();

        granularityHelper = new GranularityHelper(this);
        chartFactory = new ChartFactory(this);

        granularityHelper.setAutomatic(true);
        chartFactory.setGranularity(granularityHelper.getGranularity());
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public File getChartDirectory() {
        return chartDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        this.chartDirectory = new File(baseDirectory, "charts");
    }

    public void setInterval(long startTime, long endTime) {
        if (startTime == Interval.DEFAULT.getStart()) {
            startTime = getMinSystemTime();
        }

        if (endTime == Interval.DEFAULT.getEnd()) {
            endTime = getMaxSystemTime();
        }

        Interval toChart = null;

        try {
            toChart = new Interval(startTime, endTime);
        }
        catch (Exception e) {
            System.err.println("invalid start and end times: " + e.getMessage());
            System.err.println("The default interval will be used instead");
            toChart = Interval.DEFAULT;
        }

        getIntervalManager().addInterval(toChart);
        getIntervalManager().setCurrentInterval(toChart);
    }

    public void parse(List<String> filesToParse) {
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
            System.out.println("Errors written to " + errorFile);
        }

        // reset specifically so errors in ChartDefinitionParser _will_ go to the console
        java.util.logging.Logger.getLogger(ParserLog.getInstance().getLogger().getName()).setUseParentHandlers(true);

    }

    public void createChartsAcrossDataSets(InputStream chartDefinition) {
        List<BaseChartDefinition> chartDefinitions = parseChartDefinition(chartDefinition);

        if (chartDefinitions != null) {
            chartDirectory.mkdir();
            saveCharts(chartDefinitions, getDataSets(), chartDirectory);
        }
    }

    public void createChartsForEachDataSet(InputStream chartDefinition) {
        // for each dataset, create a subdirectory under 'charts' and output all the data set charts
        // there
        List<BaseChartDefinition> datasetChartDefinitions = parseChartDefinition(chartDefinition);

        if (chartDefinition != null) {
            chartDirectory.mkdir();

            for (DataSet data : getDataSets()) {
                System.out.print("\tCreating charts for '" + data.getHostname() + "' ");
                System.out.flush();

                File datasetChartsDir = new File(chartDirectory, data.getHostname());
                datasetChartsDir.mkdir();

                saveCharts(datasetChartDefinitions, java.util.Collections.singletonList(data), datasetChartsDir);
            }
        }
    }

    private List<BaseChartDefinition> parseChartDefinition(InputStream toParse) {
        try {
            return parser.parseCharts(toParse);
        }
        catch (IOException ioe) {
            System.err.println("cannot parse report definition xml");
            ioe.printStackTrace();

            return null;
        }
    }

    private void saveCharts(List<BaseChartDefinition> chartDefinitions, Iterable<? extends DataSet> data,
            File saveDirectory) {
        chartFactory.setInterval(getIntervalManager().getCurrentInterval());

        List<BaseChartDefinition> chartsToCreate = chartFactory.getChartsForData(chartDefinitions, data);

        for (BaseChartDefinition definition : chartsToCreate) {
            JFreeChart chart = chartFactory.createChart(definition, data);

            String filename = definition.getShortName().replace('\n', ' ') + ".png";
            File chartFile = new File(saveDirectory, filename);

            try {
                ChartUtilities.saveChartAsPNG(chartFile, chart, 1920 / 2, 1080 / 2);
            }
            catch (IOException ioe) {
                System.err.println("cannot create chart '" + chartFile.getName() + "'");
                continue;
            }

            System.out.print('.');
            System.out.flush();
        }

        System.out.println(" complete!");
    }

    @Override
    public void currentIntervalChanged(Interval interval) {
        super.currentIntervalChanged(interval);

        granularityHelper.recalculate();
        chartFactory.setGranularity(granularityHelper.getGranularity());
    }
}
