package com.ibm.nmon;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtilities;

import com.ibm.nmon.file.NMONFileFilter;

import com.ibm.nmon.parser.ChartDefinitionParser;
import com.ibm.nmon.chart.definition.BaseChartDefinition;

import com.ibm.nmon.data.DataSet;

import com.ibm.nmon.gui.chart.ChartFactory;

import com.ibm.nmon.util.FileHelper;
import com.ibm.nmon.util.GranularityHelper;

import com.ibm.nmon.util.ParserLog;

public final class ReportGenerator extends NMONVisualizerApp {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");

        if (args.length == 0) {
            System.err.println("a file path must be specified");
            return;
        }

        ReportGenerator generator = new ReportGenerator();

        // find all NMON files
        File pathToParse = new File(args[args.length - 1]);
        List<String> filesToParse = new java.util.ArrayList<String>();

        FileHelper.recurseDirectories(java.util.Collections.singletonList(pathToParse), new NMONFileFilter(),
                filesToParse);

        if (filesToParse.isEmpty()) {
            System.err.println('\'' + pathToParse.toString() + "' contains no parsable files");
            return;
        }

        // all subsequent output goes into the directory given by the user
        generator.setBaseDirectory(pathToParse.isDirectory() ? pathToParse : pathToParse.getParentFile());

        // parse the files
        generator.parse(filesToParse);

        System.out.println();

        // create summary charts in a 'charts' subdirectory
        File summaryChartsDir = new File(generator.getBaseDirectory(), "charts");

        summaryChartsDir.mkdir();

        System.out.print("creating summary charts ");
        System.out.flush();

        List<BaseChartDefinition> summaryChartDefinitions = generator.parseChartDefinition(ReportGenerator.class
                .getResourceAsStream("/com/ibm/nmon/gui/report/summary_single_interval.xml"));

        generator.saveCharts(summaryChartDefinitions, generator.getDataSets(), summaryChartsDir);

        System.out.println();

        // for each dataset, create a subdirectory under 'charts' and output all the data set charts
        // there
        List<BaseChartDefinition> datasetChartDefinitions = generator.parseChartDefinition(ReportGenerator.class
                .getResourceAsStream("/com/ibm/nmon/gui/report/dataset_report.xml"));

        for (DataSet data : generator.getDataSets()) {
            System.out.print("creating charts for " + data.getHostname() + ' ');
            System.out.flush();

            File datasetChartsDir = new File(summaryChartsDir, data.getHostname());
            datasetChartsDir.mkdir();

            generator.saveCharts(datasetChartDefinitions, java.util.Collections.singletonList(data), datasetChartsDir);
        }
    }

    // TODO -s and -d options along with -c options for other reports
    private final ChartDefinitionParser parser = new ChartDefinitionParser();
    private final GranularityHelper granularityHelper;
    private final ChartFactory chartFactory;

    private File baseDirectory;

    private ReportGenerator() throws Exception {
        super();

        // avoid logging parsing errors to console
        // remove all existing handlers and add one that logs to the ParserLog
        Logger root = Logger.getLogger("");

        for (Handler handler : root.getHandlers()) {
            root.removeHandler(handler);
        }

        root.addHandler(ParserLog.getInstance());

        granularityHelper = new GranularityHelper(this);
        chartFactory = new ChartFactory(this);

        granularityHelper.setAutomatic(true);
        chartFactory.setGranularity(granularityHelper.getGranularity());
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(File baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    private void parse(List<String> filesToParse) throws IOException {
        ParserLog log = ParserLog.getInstance();
        Map<String, String> errors = new java.util.LinkedHashMap<String, String>();

        System.out.println("parsing NMON files...");
        for (String fileToParse : filesToParse) {
            System.out.print(fileToParse + "... ");
            System.out.flush();

            log.setCurrentFilename(fileToParse);

            try {
                parse(fileToParse, getDisplayTimeZone());

                if (log.hasData()) {
                    System.out.println("complete with errors!");
                }
                else {
                    System.out.println("complete");
                }
            }
            catch (Exception e) {
                log.getLogger().error("could not parse " + fileToParse, e);
                System.out.println("complete with errors!");
                // continue parsing other files
            }

            if (log.hasData()) {
                errors.put(log.getCurrentFilename(), log.getMessages());
            }
        }

        if (!errors.isEmpty()) {
            File errorFile = new File(baseDirectory, "ReportGenerator_"
                    + new SimpleDateFormat("yyyyMMdd_HHmmss").format(System.currentTimeMillis()) + ".log");
            PrintStream out = new PrintStream(new FileOutputStream(errorFile));

            for (String filename : errors.keySet()) {
                out.print(filename);
                out.println(':');
                out.println(errors.get(filename));
            }

            out.close();
            System.out.println("parsing complete!" + " Errors written to " + errorFile);
        }
        else {
            System.out.println("parsing complete!");
        }

        granularityHelper.recalculate();
        chartFactory.setGranularity(granularityHelper.getGranularity());
    }

    private List<BaseChartDefinition> parseChartDefinition(InputStream toParse) {
        try {
            return parser.parseCharts(toParse);
        }
        catch (Exception e) {
            System.err.println("cannot parse report definition xml");
            e.printStackTrace();

            return null;
        }
    }

    private void saveCharts(List<BaseChartDefinition> chartDefinitions, Iterable<? extends DataSet> data,
            File saveDirectory) throws IOException {
        List<BaseChartDefinition> chartsToCreate = chartFactory.getChartsForData(chartDefinitions, data);

        for (BaseChartDefinition definition : chartsToCreate) {
            JFreeChart chart = chartFactory.createChart(definition, data);

            String filename = chart.getTitle().getText().replace('\n', ' ') + ".png";
            File chartFile = new File(saveDirectory, filename);

            ChartUtilities.saveChartAsPNG(chartFile, chart, 1920 / 2, 1080 / 2);

            System.out.print('.');
            System.out.flush();
        }

        System.out.println(" complete!");
    }

    @Override
    protected String[] getDataForGCParse(String fileToParse) {
        // TODO Auto-generated method stub
        return super.getDataForGCParse(fileToParse);
    }

    @Override
    protected Object[] getDataForIOStatParse(String fileToParse, String hostname) {
        // TODO Auto-generated method stub
        return super.getDataForIOStatParse(fileToParse, hostname);
    }
}
