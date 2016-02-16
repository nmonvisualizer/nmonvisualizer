package com.ibm.nmon.parser;

import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

import java.io.IOException;

import java.io.File;

import java.io.BufferedReader;
import java.io.FileReader;

import java.io.StringReader;
import java.io.StringWriter;

import java.util.TimeZone;

import com.ibm.nmon.data.NMONDataSet;

/**
 * A parser for <code>topas -a</code> output. This serves as a bridge between Topas and NMON. This class takes the
 * output and converts it to a string that can be parsed by {@link NMONParser}.
 */
public final class TopasOutParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TopasOutParser.class);

    private final NMONParser nmonParser;

    public TopasOutParser(NMONParser nmonParser) {
        this.nmonParser = nmonParser;
    }

    // topas -a outputs sorted data; unsort it by looking for the header records and outputting them first
    // once the ZZZZ records are parsed output them then all the corresponding data records for that timestamp
    public NMONDataSet parse(String filename, TimeZone timeZone, boolean scaleProcessesByCPU) throws IOException {
        File file = new File(filename);

        BufferedReader in = null;

        StringWriter writer = null;

        try {
            in = new BufferedReader(new FileReader(file));
            writer = new StringWriter((int) file.length());

            // LinkedHashMap so insertion order is maintained; removes need to reparse headers later
            Map<String, List<String>> headersToLines = new java.util.LinkedHashMap<String, List<String>>(32);

            List<String> headers = new java.util.ArrayList<String>(32);
            List<String> timestamps = new java.util.ArrayList<String>(128);

            String line = null;

            while ((line = in.readLine()) != null) {
                int idx = line.indexOf(",");

                String header = line.substring(0, idx);

                if ("AAA".equals(header)) {
                    // put all the AAA records at the top by writing them out now
                    writer.write(line);
                    writer.write('\n');
                }
                else if ("ZZZZ".equals(header)) {
                    timestamps.add(line);
                }
                else {
                    // save all the data by header name
                    List<String> lines = headersToLines.get(header);

                    if (lines == null) {
                        LOGGER.trace("found {} " + "header", header);
                        lines = new java.util.ArrayList<String>(218);
                        headersToLines.put(header, lines);
                    }

                    if (line.charAt(idx + 1) == 'T') {
                        lines.add(line);
                    }
                    else {
                        if ("LPAR".equals(header)) {
                            // fix capitalization of LPAR data to match NMON
                            line = "LPAR,LPAR Stats,PhysicalCPU,virtualCPUs,logicalCPUs,poolCPUs,entitled,weight,PoolIdle,usedAllCPU%,usedPoolCPU%,SharedCPU";
                        }

                        headers.add(line);
                    }
                }
            }

            LOGGER.debug("found {} " + "data types", headers.size());
            LOGGER.debug("found {} " + "ZZZZ timestamps", timestamps.size());
            LOGGER.debug("found {} " + "data types", headersToLines.size());

            for (String header : headers) {
                writer.write(header);
                writer.write('\n');
            }
            
            // output each timestamp, then all the data for that time
            // assume all arrays are the same size, i.e. there is data at each timestamp for all values
            for (int i = 0; i < timestamps.size(); i++) {
                writer.write(timestamps.get(i));
                writer.write('\n');

                for (List<String> lines : headersToLines.values()) {
                    writer.write(lines.get(i));
                    writer.write('\n');
                }
            }

            return nmonParser.parse(filename, new StringReader(writer.toString()), timeZone, scaleProcessesByCPU);
        }
        finally {
            if (in != null) {
                in.close();
            }

            if (writer != null) {
                writer.close();
            }
        }

    }
}
