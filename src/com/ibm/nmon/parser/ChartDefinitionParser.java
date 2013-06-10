package com.ibm.nmon.parser;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;

import com.ibm.nmon.data.definition.*;
import com.ibm.nmon.data.matcher.*;
import com.ibm.nmon.data.transform.name.*;

import com.ibm.nmon.chart.definition.*;

import com.ibm.nmon.analysis.Statistic;

public final class ChartDefinitionParser extends BasicXMLParser {
    private final List<BaseChartDefinition> charts = new java.util.ArrayList<BaseChartDefinition>();
    private BaseChartDefinition currentChart;

    private HostMatcher hostMatcher;
    private NameTransformer hostTransformer;

    private TypeMatcher typeMatcher;
    private NameTransformer typeTransformer;

    // allow multiple field matchers; endDataElement() will handle as appropriate
    private final List<FieldMatcher> fieldMatchers = new java.util.ArrayList<FieldMatcher>(3);
    // but, allow only a single RegexNameTransformer for all fields
    private final Map<String, SimpleNameTransformer> fieldTransformers = new java.util.HashMap<String, SimpleNameTransformer>(
            3);
    private RegexNameTransformer regexFieldRegexTransformer;

    private boolean inData;

    private Statistic currentStat;

    private boolean useSecondaryYAxis;

    public ChartDefinitionParser() {
        reset();
    }

    public List<BaseChartDefinition> parseCharts(String filename) throws IOException {
        long start = System.nanoTime();

        try {
            parse(filename);

            if (logger.isDebugEnabled()) {
                logger.debug("parse complete for file '{}' in {}ms", filename, (System.nanoTime() - start) / 1000000.0d);
            }

            if (charts.isEmpty()) {
                throw new IOException("chart definition file '" + filename
                        + "' does not appear to have any data records");
            }

            return java.util.Collections.unmodifiableList(new java.util.ArrayList<BaseChartDefinition>(charts));
        }
        finally {
            reset();
        }
    }

    public List<BaseChartDefinition> parseCharts(InputStream in) throws IOException {
        long start = System.nanoTime();

        reset();

        parse(in);

        if (logger.isDebugEnabled()) {
            logger.debug("Parse complete for input stream '{}' in {}ms", in, (System.nanoTime() - start) / 1000000.0d);
        }

        if (charts.isEmpty()) {
            throw new IOException("chart definition input stream '" + in + "' does not appear to have any data records");
        }

        try {
            return java.util.Collections.unmodifiableList(new java.util.ArrayList<BaseChartDefinition>(charts));
        }
        finally {
            reset();
        }
    }

    @Override
    protected void startElement(String element, String unparsedAttributes) {
        if ("linechart".equals(element)) {
            createLineChart(parseAttributes(unparsedAttributes));
        }
        else if ("intervalchart".equals(element)) {
            createIntervalChart(parseAttributes(unparsedAttributes));
        }
        else if ("barchart".equals(element)) {
            createBarChart(parseAttributes(unparsedAttributes));
        }
        // TODO xAxis categoryAxis
        else if ("yAxis".equals(element)) {
            if (currentChart instanceof YAxisChartDefinition) {
                Map<String, String> attributes = parseAttributes(unparsedAttributes);

                ((YAxisChartDefinition) currentChart).setUsePercentYAxis(Boolean.valueOf(attributes.get("asPercent")));
                ((YAxisChartDefinition) currentChart).setYAxisLabel(attributes.get("label"));
            }
            else {
                logger.warn("ignoring " + "<yAxis>" + " element for chart "
                        + (currentChart == null ? currentChart : currentChart.getShortName()) + " without a Y axis"
                        + " at line {}", getLineNumber());
            }
        }
        else if ("yAxis2".equals(element)) {
            if (currentChart instanceof LineChartDefinition) {
                Map<String, String> attributes = parseAttributes(unparsedAttributes);

                if (attributes.get("asPercent") != null) {
                    logger.warn(
                            "ignoring " + " " + "asPercent" + " attribute for " + "<yAxis2>" + " element for chart "
                                    + (currentChart == null ? currentChart : currentChart.getShortName())
                                    + " at line {}" + "; secondary axes do not support percentages", getLineNumber());
                }

                if (currentChart instanceof BarChartDefinition && ((BarChartDefinition) currentChart).isStacked()) {
                    logger.warn("ignoring " + "<yAxis2>" + " element for chart "
                            + (currentChart == null ? currentChart : currentChart.getShortName()) + " at line {}"
                            + "; stacked bar charts do not support secondary axes", getLineNumber());
                }
                else {
                    ((YAxisChartDefinition) currentChart).setSecondaryYAxisLabel(attributes.get("label"));
                    ((YAxisChartDefinition) currentChart).setHasSecondaryYAxis(true);
                }
            }
            else {
                logger.warn("ignoring " + "<yAxis2>" + " element for chart "
                        + (currentChart == null ? currentChart : currentChart.getShortName()) + " without a Y axis"
                        + " at line {}", getLineNumber());
            }
        }
        else if ("data".equals(element)) {
            if (currentChart == null) {
                logger.warn("ignoring " + "<data>" + " element outside of a chart");

                inData = false;
                skip = true;
            }
            else {
                inData = true;
                skip = false;

                Map<String, String> attributes = parseAttributes(unparsedAttributes);
                String stat = attributes.get("stat");

                if (stat != null) {
                    currentStat = Statistic.valueOf(stat);
                }

                useSecondaryYAxis = Boolean.parseBoolean(attributes.get("useYAxis2"));
            }
        }
        else if ("host".equals(element)) {
            parseHost(parseAttributes(unparsedAttributes));
        }
        else if ("type".equals(element)) {
            parseType(parseAttributes(unparsedAttributes));
        }
        else if ("field".equals(element)) {
            parseField(parseAttributes(unparsedAttributes));
        }
        else if ("fieldAlias".equals(element)) {
            parseFieldAlias(parseAttributes(unparsedAttributes));
        }
    }

    @Override
    protected void endElement(String element) {
        if ("linechart".equals(element)) {
            charts.add(currentChart);
            currentChart = null;
        }
        else if ("intervalchart".equals(element)) {
            charts.add(currentChart);
            currentChart = null;
        }
        else if ("barchart".equals(element)) {
            charts.add(currentChart);
            currentChart = null;
        }
        else if ("data".equals(element)) {
            if (!skip) {
                endDataElement();
            }

            resetData();
        }
    }

    private void createLineChart(Map<String, String> attributes) {
        if (currentChart != null) {
            logger.warn("ignoring " + "<linechart>" + " element inside another chart definition" + " at line {}",
                    getLineNumber());
            skip = true;
            return;
        }

        String title = attributes.get("name");
        String shortName = attributes.get("shortName");
        boolean stacked = Boolean.valueOf(attributes.get("stacked"));

        currentChart = new LineChartDefinition(shortName == null ? title : shortName, title, stacked);

        if (attributes.get("linesNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("linesNamedBy"));
            ((LineChartDefinition) currentChart).setLineNamingMode(mode);
        }

        logger.debug("parsing line chart {}", currentChart.getShortName());
    }

    private void createIntervalChart(Map<String, String> attributes) {
        if (currentChart != null) {
            logger.warn("ignoring " + "<intervalchart>" + " element inside another chart definition" + " at line {}",
                    getLineNumber());
            skip = true;
        }

        String title = attributes.get("name");
        String shortName = attributes.get("shortName");

        currentChart = new IntervalChartDefinition(shortName == null ? title : shortName, title);

        if (attributes.get("linesNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("linesNamedBy"));
            ((LineChartDefinition) currentChart).setLineNamingMode(mode);
        }

        logger.debug("parsing interval chart {}", currentChart.getShortName());
    }

    private void createBarChart(Map<String, String> attributes) {
        if (currentChart != null) {
            logger.warn("ignoring " + "<barchart>" + " element inside another chart definition" + " at line {}",
                    getLineNumber());
            skip = true;
            return;
        }

        String title = attributes.get("name");
        String shortName = attributes.get("shortName");
        boolean stacked = Boolean.valueOf(attributes.get("stacked"));
        boolean subtractionNeeded = Boolean.valueOf(attributes.get("subtractionNeeded"));

        currentChart = new BarChartDefinition(shortName == null ? title : shortName, title, stacked, subtractionNeeded);

        if (attributes.get("barsNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("barsNamedBy"));
            ((BarChartDefinition) currentChart).setBarNamingMode(mode);
        }

        if (attributes.get("categoriesNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("categoriesNamedBy"));
            ((BarChartDefinition) currentChart).setCategoryNamingMode(mode);
        }

        logger.debug("parsing bar chart {}", currentChart.getShortName());
    }

    private void parseHost(Map<String, String> attributes) {
        if (!inData) {
            logger.warn("ignoring " + "<host>" + " element outside of <data>" + " at line {}"
                    + "; <data> will be skipped", getLineNumber());
            skip = true;
            return;
        }

        if (hostMatcher == null) {
            String name = attributes.get("name");

            if (name != null) {
                if (HostMatcher.ALL.toString().equals(name)) {
                    hostMatcher = HostMatcher.ALL;
                }
                else {
                    hostMatcher = new ExactHostMatcher(name);
                }
            }
            else {
                String regex = attributes.get("regex");

                if (regex != null) {
                    hostMatcher = new RegexHostMatcher(regex);
                }
                else {
                    String os = attributes.get("os");

                    if (os != null) {
                        hostMatcher = new OSMatcher(os);
                    }
                    else {
                        logger.error("either 'name', 'regex' or 'os'" + " must be defined for  " + "<host>"
                                + " at line {}" + "; <data> will be skipped", getLineNumber());
                        skip = true;
                    }
                }
            }

            hostTransformer = createTransformer(attributes, true, hostTransformer);
        }
        else {
            logger.warn("ignoring " + "extra " + "<host>" + " element" + " at line {}", getLineNumber());
        }
    }

    private void parseType(Map<String, String> attributes) {
        if (!inData) {
            logger.warn("ignoring " + "<type>" + " element outside of <data>" + " at line {}"
                    + "; <data> will be skipped", getLineNumber());
            skip = true;
            return;
        }

        if (typeMatcher == null) {
            String name = attributes.get("name");

            if (name != null) {
                if ("$PROCESSES".equals(name)) {
                    typeMatcher = ProcessMatcher.INSTANCE;
                }
                else if (TypeMatcher.ALL.toString().equals(name)) {
                    typeMatcher = TypeMatcher.ALL;
                }
                else {
                    typeMatcher = new ExactTypeMatcher(name);
                }
            }
            else {
                String regex = attributes.get("regex");

                if (regex != null) {
                    typeMatcher = new RegexTypeMatcher(regex);
                }
                else {
                    logger.error("either 'name' or 'regex'" + " must be defined for  " + "<type>" + " at line {}"
                            + "; <data> will be skipped", getLineNumber());
                    skip = true;
                }
            }

            typeTransformer = createTransformer(attributes, true, typeTransformer);
        }
        else {
            logger.warn("ignoring " + "extra " + "<type>" + " element" + " at line {}", getLineNumber());
        }
    }

    private void parseField(Map<String, String> attributes) {
        if (!inData) {
            logger.warn("ignoring " + "<field>" + " element outside of <data>" + " at line {}"
                    + "; <data> will be skipped", getLineNumber());
            skip = true;
            return;
        }

        String name = attributes.get("name");

        if (name != null) {
            if (FieldMatcher.ALL.toString().equals(name)) {
                fieldMatchers.add(FieldMatcher.ALL);
            }
            else {
                fieldMatchers.add(new ExactFieldMatcher(name));
            }

            NameTransformer transformer = createTransformer(attributes, true, null);

            if (transformer != null) {
                fieldTransformers.put(name, (SimpleNameTransformer) transformer);
            }
        }
        else {
            String regex = attributes.get("regex");

            if (regex != null) {
                fieldMatchers.add(new RegexFieldMatcher(regex));

                // if the field is being matched by regex, there can be only a single, global
                // regex field transform
                NameTransformer transformer = createTransformer(attributes, false, regexFieldRegexTransformer);

                if (transformer != null) {
                    regexFieldRegexTransformer = (RegexNameTransformer) transformer;
                }
            }
            else {
                logger.error("either 'name' or 'regex'" + " must be defined for " + "<field>" + " at line {}"
                        + "; <data> will be skipped", getLineNumber());
                skip = true;
            }
        }
    }

    private void parseFieldAlias(Map<String, String> attributes) {
        if (!inData) {
            logger.warn("ignoring " + "<fieldAlias>" + " element outside of <data>" + "at line {}"
                    + "; <data> will be skipped", getLineNumber());
            skip = true;
            return;
        }

        String name = attributes.get("name");

        if (name == null) {
            logger.warn("'name'" + " must be defined for " + "<fieldAlias>" + " at line {}", getLineNumber());
            return;
        }
        else {
            String value = attributes.get("value");

            if (value == null) {
                logger.warn("'value'" + " must be defined for " + "<fieldAlias>" + " at line {}", getLineNumber());
            }
            else {
                fieldTransformers.put(name, new SimpleNameTransformer(value));
            }
        }
    }

    private void endDataElement() {
        // as a convenience to users, allow multiple <field> elements
        // but, as an optimization, roll up all the ExactFieldMatchers into a SetFieldMatcher since
        // there has to be a DataDefinition per matcher
        List<FieldMatcher> consolidatedMatchers = new java.util.ArrayList<FieldMatcher>(fieldMatchers.size());
        List<String> names = new java.util.ArrayList<String>(fieldMatchers.size());

        for (FieldMatcher matcher : fieldMatchers) {
            if (matcher.getClass().equals(ExactFieldMatcher.class)) {
                String name = ((ExactFieldMatcher) matcher).getField();

                if ("$DISKS".equals(name)) {
                    consolidatedMatchers.add(DiskMatcher.INSTANCE);
                }
                else if ("$PARTITIONS".equals(name)) {
                    consolidatedMatchers.add(PartitionMatcher.INSTANCE);
                }
                else {
                    names.add(name);
                }
            }
            else {
                consolidatedMatchers.add(matcher);
            }
        }

        if ((names.size() == 0) && (consolidatedMatchers.size() == 0)) {
            consolidatedMatchers.add(FieldMatcher.ALL);
        }
        else if (names.size() == 1) {
            consolidatedMatchers.add(new ExactFieldMatcher(names.get(0)));
        }
        else if (names.size() > 1) {
            consolidatedMatchers.add(new SetFieldMatcher(names.toArray(new String[names.size()])));
        }

        // for each matcher create new DataDefinition
        for (FieldMatcher fieldMatcher : consolidatedMatchers) {
            DefaultDataDefinition definition = new DefaultDataDefinition(hostMatcher, typeMatcher, fieldMatcher,
                    currentStat, useSecondaryYAxis);

            for (String field : fieldTransformers.keySet()) {
                definition.addFieldTransformer(field, fieldTransformers.get(field));
            }

            if (hostTransformer != null) {
                if (hostMatcher.getClass().equals(ExactHostMatcher.class)) {
                    definition.addHostnameTransformer(((ExactHostMatcher) hostMatcher).getHostname(), hostTransformer);
                }
                else {
                    definition.addHostnameTransformer(DefaultDataDefinition.DEFAULT_NAME_TRANSFORMER_KEY,
                            hostTransformer);
                }
            }

            if (typeTransformer != null) {
                if (typeMatcher.getClass().equals(ExactTypeMatcher.class)) {
                    definition.addTypeTransformer(((ExactTypeMatcher) typeMatcher).getType(), typeTransformer);
                }
                else {
                    definition.addTypeTransformer(DefaultDataDefinition.DEFAULT_NAME_TRANSFORMER_KEY, typeTransformer);
                }
            }

            if (regexFieldRegexTransformer != null) {
                definition.addFieldTransformer(DefaultDataDefinition.DEFAULT_NAME_TRANSFORMER_KEY,
                        regexFieldRegexTransformer);
            }

            if (currentChart instanceof LineChartDefinition) {
                ((LineChartDefinition) currentChart).addLine(definition);
            }
            else if (currentChart.getClass().equals(BarChartDefinition.class)) {
                ((BarChartDefinition) currentChart).addCategory(definition);
            }

            logger.debug("added {} to chart {}", definition, currentChart.getShortName());
        }
    }

    private NameTransformer createTransformer(Map<String, String> attributes, boolean aliasAllowed,
            NameTransformer existing) {
        String alias = attributes.get("alias");

        if (alias != null) {
            if (aliasAllowed) {
                return new SimpleNameTransformer(alias);
            }
            else {
                logger.warn("ignoring invalid 'alias' attribute" + " at line {}", getLineNumber());
                return null;
            }
        }
        else {
            String regex = attributes.get("aliasRegex");

            if (regex != null) {
                if (existing != null) {
                    logger.warn("an existing aliasRegex has already been defined, ignoring 'aliasRegex'"
                            + " at line {}", getLineNumber());
                    return null;
                }
                else {
                    String temp = attributes.get("aliasRegexGroup");

                    if (temp == null) {
                        return new RegexNameTransformer(regex);
                    }
                    else {
                        try {
                            return new RegexNameTransformer(regex, Integer.parseInt(temp));
                        }
                        catch (NumberFormatException nfe) {
                            logger.warn("'aliasRegexGroup' must be a number" + " at line {}", getLineNumber());
                        }

                        return null;
                    }
                }
            }
            else {
                return null;
            }
        }
    }

    @Override
    protected void reset() {
        super.reset();

        charts.clear();
        currentChart = null;

        resetData();
    }

    private void resetData() {
        hostMatcher = null;
        hostTransformer = null;

        typeMatcher = null;
        typeTransformer = null;

        fieldMatchers.clear();
        fieldTransformers.clear();
        regexFieldRegexTransformer = null;

        inData = false;
        skip = false;

        currentStat = Statistic.AVERAGE;

        useSecondaryYAxis = false;
    }
}
