package com.ibm.nmon.chart.definition;

import java.io.IOException;
import java.io.InputStream;

import java.util.List;
import java.util.Map;
import java.util.Set;

import java.text.SimpleDateFormat;

import com.ibm.nmon.data.definition.*;
import com.ibm.nmon.data.matcher.*;
import com.ibm.nmon.data.transform.name.*;
import com.ibm.nmon.parser.BasicXMLParser;

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

    private SimpleDateFormat dateFormat;

    private boolean inData;

    private Statistic currentStat;
    private final Set<Statistic> markers = new java.util.HashSet<Statistic>(3);

    private boolean useSecondaryYAxis;

    public ChartDefinitionParser() {
        reset();
    }

    public List<BaseChartDefinition> parseCharts(String filename) throws IOException {
        long start = System.nanoTime();

        try {
            parse(filename);

            if (logger.isDebugEnabled()) {
                logger.debug("parse complete for file '{}' in {}ms", filename,
                        (System.nanoTime() - start) / 1000000.0d);
            }

            if (charts.isEmpty()) {
                throw new IOException(
                        "chart definition file '" + filename + "' does not appear to have any charts defined");
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
            throw new IOException(
                    "chart definition input stream '" + in + "' does not appear to have any charts defined");
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
        else if ("histogram".equals(element)) {
            createHistogram(parseAttributes(unparsedAttributes));
        }
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
            if (currentChart instanceof YAxisChartDefinition) {
                Map<String, String> attributes = parseAttributes(unparsedAttributes);

                if (attributes.get("asPercent") != null) {
                    logger.warn("ignoring " + "asPercent" + " attribute for " + "<yAxis2>" + " element for chart '"
                            + (currentChart == null ? currentChart : currentChart.getShortName()) + '\'' + " at line {}"
                            + "; secondary axes do not support percentages", getLineNumber());
                }

                if (currentChart instanceof BarChartDefinition && ((BarChartDefinition) currentChart).isStacked()) {
                    logger.warn(
                            "ignoring " + "<yAxis2>" + " element for chart "
                                    + (currentChart == null ? currentChart : currentChart.getShortName())
                                    + " at line {}" + "; stacked bar charts do not support secondary axes",
                            getLineNumber());
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
        else if ("xAxis".equals(element)) {
            if (currentChart instanceof LineChartDefinition) {
                Map<String, String> attributes = parseAttributes(unparsedAttributes);

                ((LineChartDefinition) currentChart).setXAxisLabel(attributes.get("label"));
            }
            else if (currentChart instanceof BarChartDefinition) {
                Map<String, String> attributes = parseAttributes(unparsedAttributes);

                ((BarChartDefinition) currentChart).setCategoryAxisLabel(attributes.get("label"));
            }
            else if (currentChart instanceof HistogramChartDefinition) {
                Map<String, String> attributes = parseAttributes(unparsedAttributes);

                ((HistogramChartDefinition) currentChart).setXAxisLabel(attributes.get("label"));

                String minString = attributes.get("min");
                String maxString = attributes.get("max");

                if (((minString != null) && (maxString == null)) || ((maxString != null) && (minString == null))) {
                    logger.warn("ignoring " + "<histogram>" + " attributes 'min' and 'max' " + "must both be specified "
                            + " at line {}" + ", if a defined range is desired", getLineNumber());
                }
                else {
                    try {
                        ((HistogramChartDefinition) currentChart).setXAxisRange(
                                new org.jfree.data.Range(Integer.parseInt(minString), Integer.parseInt(maxString)));
                    }
                    catch (NumberFormatException nfe) {
                        logger.warn(
                                "ignoring " + "<histogram>" + " attributes 'min' and 'max' "
                                        + "with values '{}' and '{}'" + " at line {}" + ", are not a valid numbers",
                                minString, maxString, getLineNumber());
                    }
                    catch (IllegalArgumentException iae) {
                        logger.warn(
                                "ignoring " + "<histogram>" + " attributes 'min' and 'max' "
                                        + "with values '{}' and '{}'" + " at line {}" + ", invalid " + "range",
                                minString, maxString, getLineNumber());
                    }
                }
            }
            else {
                logger.warn("ignoring " + "<xAxis>" + " element for chart "
                        + (currentChart == null ? currentChart : currentChart.getShortName()) + " without an X axis"
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
                    try {
                        currentStat = Statistic.valueOf(stat);
                    }
                    catch (IllegalArgumentException iae) {
                        logger.warn("ignoring " + "invalid " + "'stat'" + " attribute {} at line {}", stat,
                                getLineNumber());
                    }
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
        else if ("marker".equals(element)) {
            Map<String, String> attributes = parseAttributes(unparsedAttributes);
            String stat = attributes.get("stat");

            if (stat != null) {
                try {
                    markers.add(Statistic.valueOf(stat));
                }
                catch (IllegalArgumentException iae) {
                    logger.warn("ignoring " + "invalid " + "'marker'" + " attribute {} at line {}", stat,
                            getLineNumber());
                }
            }
        }
        else if ("charts".equals(element)) {
            // do nothing but also do not log a spurious warning
        }
        else {
            logger.warn("unknown element {} at line {}", element, getLineNumber());
        }
    }

    @Override
    protected void endElement(String element) {
        if (currentChart == null) {
            if (!"charts".equals(element)) {
                logger.warn("ignoring" + " element </{}> at line {}; current chart is not defined", element,
                        getLineNumber());
            }
            // else no chart should be defined when ending the root </charts> element

            return;
        }

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
        else if ("histogram".equals(element)) {
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

        if ((title == null) || "".equals(title)) {
            logger.warn("ignoring " + "<linechart>" + " element with no name" + " at line {}", getLineNumber());
            skip = true;
            return;
        }

        String shortName = attributes.get("shortName");
        boolean stacked = Boolean.valueOf(attributes.get("stacked"));

        currentChart = new LineChartDefinition(shortName == null ? title : shortName, title, stacked);

        if (attributes.get("subtitledBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("subtitledBy"));
            currentChart.setSubtitleNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        if (attributes.get("linesNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("linesNamedBy"));
            ((LineChartDefinition) currentChart).setLineNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        String showDataPoints = attributes.get("showDataPoints");

        if (showDataPoints != null) {
            ((LineChartDefinition) currentChart).setShowDataPoints(Boolean.valueOf(showDataPoints));
        }

        parseSize("<linechart>", attributes);

        logger.debug("parsed line chart {}", currentChart.getShortName());
    }

    private void createIntervalChart(Map<String, String> attributes) {
        if (currentChart != null) {
            logger.warn("ignoring " + "<intervalchart>" + " element inside another chart definition" + " at line {}",
                    getLineNumber());
            skip = true;
        }

        String title = attributes.get("name");

        if ((title == null) || "".equals(title)) {
            logger.warn("ignoring " + "<intervalchart>" + " element with no name" + " at line {}", getLineNumber());
            skip = true;
            return;
        }

        String shortName = attributes.get("shortName");

        currentChart = new IntervalChartDefinition(shortName == null ? title : shortName, title);

        if (attributes.get("subtitledBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("subtitledBy"));
            currentChart.setSubtitleNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        if (attributes.get("linesNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("linesNamedBy"));
            ((LineChartDefinition) currentChart).setLineNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        String showDataPoints = attributes.get("showDataPoints");

        if (showDataPoints != null) {
            ((LineChartDefinition) currentChart).setShowDataPoints(Boolean.valueOf(showDataPoints));
        }

        parseSize("<intervalchart>", attributes);

        logger.debug("parsed interval chart {}", currentChart.getShortName());
    }

    private void createBarChart(Map<String, String> attributes) {
        if (currentChart != null) {
            logger.warn("ignoring " + "<barchart>" + " element inside another chart definition" + " at line {}",
                    getLineNumber());
            skip = true;
            return;
        }

        String title = attributes.get("name");

        if ((title == null) || "".equals(title)) {
            logger.warn("ignoring " + "<barchart>" + " element with no name" + " at line {}", getLineNumber());
            skip = true;
            return;
        }

        String shortName = attributes.get("shortName");
        boolean stacked = Boolean.valueOf(attributes.get("stacked"));
        boolean subtractionNeeded = Boolean.valueOf(attributes.get("subtractionNeeded"));

        currentChart = new BarChartDefinition(shortName == null ? title : shortName, title, stacked, subtractionNeeded);

        if (attributes.get("subtitledBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("subtitledBy"));
            currentChart.setSubtitleNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        if (attributes.get("barsNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("barsNamedBy"));
            ((BarChartDefinition) currentChart).setBarNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        if (attributes.get("categoriesNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("categoriesNamedBy"));
            ((BarChartDefinition) currentChart).setCategoryNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        parseSize("<barchart>", attributes);

        logger.debug("parsed bar chart {}", currentChart.getShortName());
    }

    private void createHistogram(Map<String, String> attributes) {
        if (currentChart != null) {
            logger.warn("ignoring " + "<histogram>" + " element inside another chart definition" + " at line {}",
                    getLineNumber());
            skip = true;
            return;
        }

        String title = attributes.get("name");

        if ((title == null) || "".equals(title)) {
            logger.warn("ignoring " + "<histogram>" + " element with no name" + " at line {}", getLineNumber());
            skip = true;
            return;
        }

        String shortName = attributes.get("shortName");

        currentChart = new HistogramChartDefinition(shortName == null ? title : shortName, title);

        if (attributes.get("barsNamedBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("barsNamedBy"));
            ((HistogramChartDefinition) currentChart).setHistogramNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        if (attributes.get("subtitledBy") != null) {
            NamingMode mode = NamingMode.valueOf(attributes.get("subtitledBy"));
            currentChart.setSubtitleNamingMode(mode);
            parseDateFormat(mode, attributes);
        }

        String temp = attributes.get("bins");

        if (temp != null) {
            try {
                ((HistogramChartDefinition) currentChart).setBins(Integer.parseInt(temp));
            }
            catch (NumberFormatException nfe) {
                logger.warn("ignoring " + "<histogram>" + " attribute 'bins' with value {}" + " at line {}"
                        + ", it is not a valid number", temp, getLineNumber());
            }
        }

        temp = attributes.get("showMarkers");

        // null => default, which is to show markers
        if ((temp != null) && !Boolean.valueOf(attributes.get("showMarkers"))) {
            ((HistogramChartDefinition) currentChart).setMarkers(new Statistic[0]);
        }

        parseSize("<histogram>", attributes);

        logger.debug("parsed histogram chart {}", currentChart.getShortName());
    }

    private void parseHost(Map<String, String> attributes) {
        if (!inData) {
            logger.warn(
                    "ignoring " + "<host>" + " element outside of <data>" + " at line {}" + "; <data> will be skipped",
                    getLineNumber());
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
            logger.warn(
                    "ignoring " + "<type>" + " element outside of <data>" + " at line {}" + "; <data> will be skipped",
                    getLineNumber());
            skip = true;
            return;
        }

        if (typeMatcher == null) {
            String name = attributes.get("name");

            if (name != null) {
                if ("$PROCESSES".equals(name)) {
                    typeMatcher = ProcessMatcher.INSTANCE;
                }
                else if ("$TOP_PROCESSES_BY_CPU".equals(name)) {
                    typeMatcher = TopProcessMatcher.BY_CPU;
                }
                else if ("$TOP_PROCESSES_BY_MEMORY".equals(name)) {
                    typeMatcher = TopProcessMatcher.BY_MEMORY;
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
            logger.warn(
                    "ignoring " + "<field>" + " element outside of <data>" + " at line {}" + "; <data> will be skipped",
                    getLineNumber());
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
            logger.warn("ignoring " + "<fieldAlias>" + " element outside of <data>" + " at line {}"
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

    private void parseSize(String elementName, Map<String, String> attributes) {
        if (attributes.get("width") != null) {
            String temp = attributes.get("width");

            try {
                currentChart.setWidth(Integer.parseInt(temp));
            }
            catch (NumberFormatException nfe) {
                logger.warn("ignoring " + elementName + " attribute " + "'width'" + " with value '{}'" + " at line {}"
                        + ", is not a valid number", temp, getLineNumber());
            }
        }

        if (attributes.get("height") != null) {
            String temp = attributes.get("height");

            try {
                currentChart.setHeight(Integer.parseInt(temp));
            }
            catch (NumberFormatException nfe) {
                logger.warn("ignoring " + elementName + " attribute " + "'height'" + " with value '{}'" + " at line {}"
                        + ", is not a valid number", temp, getLineNumber());
            }
        }
    }

    private void parseDateFormat(NamingMode mode, Map<String, String> attributes) {
        // only parse once
        if (dateFormat != null) {
            return;
        }

        // if mode != NamingMode.DATE then dateFormat will just be ignored by all other NamingModes
        String format = attributes.get("dateFormat");

        if (format == null) {
            dateFormat = null;
        }
        else {
            try {
                dateFormat = new SimpleDateFormat(format);
            }
            catch (Exception e) {
                logger.warn("ignoring " + "invalid '" + "dateFormat" + "' attribute" + " '{}'" + " at line {}", format,
                        getLineNumber());
                dateFormat = null;
            }
        }
    }

    private void endDataElement() {
        if (typeMatcher == null) {
            logger.warn("ignoring " + "<data>" + " element without a <type>" + " at line {}" + "; <type> is required",
                    getLineNumber());
            return;
        }

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
                // else if ("$ALL".equals(name)) covered by having the names list be empty
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
            definition.setDateFormat(dateFormat);

            for (String field : fieldTransformers.keySet()) {
                definition.addFieldTransformer(field, fieldTransformers.get(field));
            }

            if (hostTransformer != null) {
                // no <host> element => hostMatcher and hostTransformer will be null, no need for
                // hostMatcher null check here
                if (hostMatcher.getClass().equals(ExactHostMatcher.class)) {
                    definition.addHostnameTransformer(((ExactHostMatcher) hostMatcher).getHostname(), hostTransformer);
                }
                else {
                    definition.addHostnameTransformer(DefaultDataDefinition.DEFAULT_NAME_TRANSFORMER_KEY,
                            hostTransformer);
                }
            }

            if (typeTransformer != null) {
                // no <type> element => typeMatcher and typeTransformer will be null, no need for
                // typeMatcher null check here
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

            currentChart.addData(definition);

            if (currentChart instanceof HistogramChartDefinition) {
                HistogramChartDefinition histogramChart = ((HistogramChartDefinition) currentChart);

                // count will be zero if 'showMarkers' is set to false
                // ignore parsed markers if that is the case
                if ((histogramChart.getMarkerCount() != 0) && (markers.size() > 0)) {
                    histogramChart.setMarkers(markers.toArray(new Statistic[markers.size()]));
                }
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
                logger.warn("ignoring " + "invalid '" + "alias" + "' attribute" + " at line {}", getLineNumber());
                return existing;
            }
        }
        else {
            String regex = attributes.get("aliasRegex");

            if (regex == null) {
                // try to reuse existing regex
                regex = attributes.get("regex");
            }

            if (regex != null) {
                if (existing != null) {
                    logger.warn(
                            "an existing regex substitution has already been defined, ignoring additional substitutions"
                                    + " at line {}",
                            getLineNumber());
                    return existing;
                }
                else {
                    String group = attributes.get("aliasByGroup");

                    if (group == null) {
                        String replacement = attributes.get("aliasByReplacement");

                        if ((replacement != null) && !"".equals(replacement)) {
                            return new RegexNameTransformer(regex, replacement);
                        }
                        else {
                            return new RegexNameTransformer(regex);
                        }
                    }
                    else {
                        try {
                            return new RegexNameTransformer(regex, Integer.parseInt(group));
                        }
                        catch (NumberFormatException nfe) {
                            logger.warn("'" + "aliasByGroup" + "' must be a number" + " at line {}", getLineNumber());
                        }

                        return existing;
                    }
                }
            }
            else {
                return existing;
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

        dateFormat = null;

        inData = false;
        skip = false;

        currentStat = Statistic.AVERAGE;

        useSecondaryYAxis = false;
    }
}
