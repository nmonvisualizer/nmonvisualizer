package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;

import java.text.ParseException;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import java.util.regex.Pattern;

import com.ibm.nmon.data.BasicDataSet;
import com.ibm.nmon.data.DataRecord;
import com.ibm.nmon.data.DataType;

import com.ibm.nmon.util.DataHelper;

public final class JMeterAggregateParser {
  private static final Logger LOGGER =
      org.slf4j.LoggerFactory.getLogger(JMeterAggregateParser.class);

  private static final Pattern DATA_SPLITTER = Pattern.compile(",");

  private static final Set<String> REQUIRED_FIELDS;
  private static final Set<String> IGNORED_FIELDS;

  // when aggregating fields, record max or sum instead of average
  private static final Set<String> MAX_FIELDS;
  private static final Set<String> SUM_FIELDS;

  private LineNumberReader in = null;

  private BasicDataSet dataSet = null;

  // indexes into the data that require special handling
  private int labelIndex = -1;
  private int timestampIndex = -1;
  private int successIndex = -1;
  private int hostnameIndex = -1;
  private int responseCodeIndex = -1;

  private final Set<String> transactionNames = new java.util.HashSet<String>();

  // map the average, max or sum aggregation action to the actual parsed fields
  private char[] fieldActions;

  public BasicDataSet parse(File file) throws IOException, ParseException {
    return parse(file.getAbsolutePath());
  }

  public BasicDataSet parse(String filename) throws IOException, ParseException {
    long start = System.nanoTime();

    dataSet = new BasicDataSet(filename);
    dataSet.setMetadata("hostname", "JMeter");

    try {
      in = new LineNumberReader(new FileReader(filename));

      String line = in.readLine();

      Map<String, Integer> fieldIndexes = parseHeader(DATA_SPLITTER.split(line));

      // for every millisecond, there will be one or more double[] for each transaction
      Map<Long, Map<String, List<double[]>>> dataByMilli = parseData(fieldIndexes, in);

      LOGGER.debug(
          "parsed {} lines into {} seconds of data", in.getLineNumber(), dataByMilli.size());

      convertData(dataByMilli, fieldIndexes);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Parse" + " complete for {} in {}ms",
            dataSet.getSourceFile(),
            (System.nanoTime() - start) / 1000000.0d);
      }

      return dataSet;
    } finally {
      in.close();

      dataSet = null;

      labelIndex = -1;
      timestampIndex = -1;
      successIndex = -1;
      hostnameIndex = -1;
      responseCodeIndex = -1;

      transactionNames.clear();
      fieldActions = null;
    }
  }

  private Map<String, Integer> parseHeader(String[] headers) throws IOException {
    // used LinkedHashMap to maintain correspondence between array indexes of headers and values
    Map<String, Integer> fieldIndexes =
        new java.util.LinkedHashMap<String, Integer>(headers.length);

    for (int i = 0; i < headers.length; i++) {
      if (IGNORED_FIELDS.contains(headers[i])) {
        continue;
      }

      fieldIndexes.put(DataHelper.newString(headers[i]), i);
    }

    if (REQUIRED_FIELDS.containsAll(fieldIndexes.keySet())) {
      throw new IOException("header does not contain all required fields: " + REQUIRED_FIELDS);
    }

    // remove these so the rest of the map can be iterated to get the rest of the data
    labelIndex = fieldIndexes.remove("label");
    timestampIndex = fieldIndexes.remove("timeStamp");

    // optionally, gather all the load driver names and add as metadata
    Integer temp = fieldIndexes.remove("Hostname");

    if (temp != null) {
      hostnameIndex = temp;
    }

    // do not remove; will be converted to 0/1
    temp = fieldIndexes.get("success");

    if (temp != null) {
      successIndex = temp;
    }

    // if there is an exception, JMeter writes to the responseCode field
    // handle that by converting to a 500
    temp = fieldIndexes.get("responseCode");

    if (temp != null) {
      responseCodeIndex = temp;
    }

    fieldActions = new char[fieldIndexes.size()];
    int n = 0;

    for (String field : fieldIndexes.keySet()) {
      if (MAX_FIELDS.contains(field)) {
        fieldActions[n] = 'm';
      } else if (SUM_FIELDS.contains(field)) {
        fieldActions[n] = 's';
      } else {
        fieldActions[n] = 'a';
      }

      ++n;
    }

    return fieldIndexes;
  }

  private Map<Long, Map<String, List<double[]>>> parseData(
      Map<String, Integer> fieldIndexes, LineNumberReader in) throws IOException {
    int actualFields = fieldIndexes.size();
    int expectedFields =
        actualFields
            + 2
            + (successIndex == -1 ? 0 : 1)
            + (hostnameIndex == -1 ? 0 : 1)
            + (successIndex == -1 ? 0 : 1);

    Set<String> hostnames = new java.util.HashSet<String>();

    // since multiple threads can be running, it is possible to have multiple entries at the same
    // millisecond
    // for every timestamp, store that data for each transaction name and aggregate it later
    Map<Long, Map<String, List<double[]>>> dataBySecond =
        new java.util.HashMap<Long, Map<String, List<double[]>>>();
    String line = null;

    while ((line = in.readLine()) != null) {
      String[] data = DATA_SPLITTER.split(line);

      if (data.length < expectedFields) {
        LOGGER.warn(
            "skipping invalid data record '{}' at line {}; " + "expected at least {} fields",
            line,
            in.getLineNumber(),
            expectedFields);
        continue;
      }

      if (hostnameIndex > -1) {
        hostnames.add(data[hostnameIndex]);
      }

      long time = 0;
      double[] values = new double[actualFields];

      // round time to the nearest second
      try {
        time = Math.round(Long.parseLong(data[timestampIndex])/ 1000.0d) * 1000;
      } catch (NumberFormatException nfe) {
        LOGGER.warn(
            "skipping invalid data record '{}'  at line {}; " + "invalid timeStamp '{}'",
            line,
            in.getLineNumber(),
            data[timestampIndex]);
        continue;
      }

      String transactionName = data[labelIndex];
      transactionNames.add(transactionName);

      int n = 0;

      for (int idx : fieldIndexes.values()) {
        if (idx == successIndex) {
          values[n++] = Boolean.parseBoolean(data[idx]) ? 1 : 0;
        } else {
          try {
            values[n++] = Double.parseDouble(data[idx]);
          } catch (NumberFormatException nfe) {
            if (idx == responseCodeIndex) {
              values[n] = 500;
              continue;
            }
            LOGGER.warn(
                "skipping invalid data record '{}' at line {}; " + "invalid number '{}'",
                line,
                in.getLineNumber(),
                data[idx]);
            break;
          }
        }
      }

      Map<String, List<double[]>> timeData = dataBySecond.get(time);

      
      if (timeData == null) {
        timeData = new java.util.HashMap<String, List<double[]>>();

        List<double[]> l = new java.util.LinkedList<double[]>();

        l.add(values);
        timeData.put(transactionName, l);
        dataBySecond.put(time, timeData);
      } else {
        List<double[]> l = timeData.get(transactionName);

        if (l == null) {
          l = new java.util.LinkedList<double[]>();
        }

        l.add(values);
        timeData.put(transactionName, l);
      }
    }

    if (!hostnames.isEmpty()) {
      dataSet.setMetadata("loadDrivers", hostnames.toString());
    }

    return dataBySecond;
  }

  private void convertData(
      Map<Long, Map<String, List<double[]>>> dataByMilli, Map<String, Integer> fieldIndexes) {
    int actualFields = fieldIndexes.size();
    String[] transactions = transactionNames.toArray(new String[transactionNames.size()]);

    // deal with memory savings now that all names are known
    for (int i = 0; i < transactions.length; i++) {
      transactions[i] = DataHelper.newString(transactions[i]);
    }

    // create a data type for each field in the CSV
    // every type will have the transaction names as fields
    for (String field : fieldIndexes.keySet()) {
      field = DataHelper.newString(field);
      dataSet.addType(new DataType(field, field, transactions));
    }

    // for every millisecond of data, get each transaction
    // for every transaction, calculated the aggregated value
    for (Map.Entry<Long, Map<String, List<double[]>>> forSecond : dataByMilli.entrySet()) {
      long time = forSecond.getKey();
      // pivot data into a transaction sized array for each field / data type
      double[][] fieldsByTransaction = new double[actualFields][];

      for (int i = 0; i < fieldsByTransaction.length; i++) {
        fieldsByTransaction[i] = new double[transactions.length];
      }

      Map<String, List<double[]>> byTransaction = forSecond.getValue();

      // missing transactions at each time will happen so iterate over array instead of map
      for (int i = 0; i < transactions.length; i++) {
        List<double[]> forTransaction = byTransaction.get(transactions[i]);

        if (forTransaction == null) {
          continue;
        }

        // actually pivot the data; note j, then i
        for (double[] data : forTransaction) {
          for (int j = 0; j < actualFields; j++) {

            if (fieldActions[j] == 'm') {
              if (data[j] > fieldsByTransaction[j][i]) {
                fieldsByTransaction[j][i] = data[j];
              }
              // else ignore
            } else {
              fieldsByTransaction[j][i] += data[j];
            }
          }
        }

        if (forTransaction.size() > 1) {
          // calculate average from how many times the tx occurred
          for (int j = 0; j < actualFields; j++) {
            if (fieldActions[j] == 'a') {
              fieldsByTransaction[j][i] /= forTransaction.size();
            }
            // else leave sum and max as-is
          }
        }
      }

      DataRecord record = new DataRecord(time, Long.toString(time));

      int n = 0;

      for (String field : fieldIndexes.keySet()) {
        // iteration order == array index because fieldIndexes is a LinkedHashMap
        record.addData(dataSet.getType(field), fieldsByTransaction[n++]);
      }

      dataSet.addRecord(record);
    }
  }

  static {
    Set<String> temp = new java.util.HashSet<String>();

    temp.add("label");
    temp.add("timeStamp");

    REQUIRED_FIELDS = java.util.Collections.unmodifiableSet(temp);

    temp = new java.util.HashSet<String>();

    // in general, ignore text fields
    temp.add("responseMessage");
    temp.add("failureMessage");
    temp.add("threadName");
    temp.add("dataType");
    temp.add("URL");
    temp.add("Filename");
    temp.add("Encoding");

    IGNORED_FIELDS = java.util.Collections.unmodifiableSet(temp);

    temp = new java.util.HashSet<String>();

    temp.add("responseCode");
    temp.add("grpThreads");
    temp.add("allThreads");

    MAX_FIELDS = java.util.Collections.unmodifiableSet(temp);

    temp = new java.util.HashSet<String>();

    temp.add("success");
    temp.add("SampleCount");
    temp.add("ErrorCount");

    SUM_FIELDS = java.util.Collections.unmodifiableSet(temp);

    for (String field : SUM_FIELDS) {
      if (MAX_FIELDS.contains(field)) {
        throw new IllegalStateException("a SUM_FIELD cannot also be a MAX_FIELD");
      }
    }
  }
}
