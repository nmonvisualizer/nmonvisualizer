package com.ibm.nmon.parser;

import org.slf4j.Logger;

import com.ibm.nmon.parser.util.XMLParserHelper;
import com.ibm.nmon.util.DataHelper;

import java.io.IOException;

import java.io.LineNumberReader;
import java.io.InputStream;

import java.util.Map;

/**
 * Simple non-validating XML parser.
 */
public abstract class BasicXMLParser {
    protected final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    private LineNumberReader in;

    protected boolean skip = false;

    protected final void parse(String filename) throws IOException {
        in = new LineNumberReader(new java.io.FileReader(filename));
        parse();
    }

    protected final void parse(InputStream stream) throws IOException {
        in = new LineNumberReader(new java.io.InputStreamReader(stream));
        parse();
    }

    private void parse() throws IOException {
        try {
            String line = null;

            // attempt to search for <?xml> first - abort if not present
            // only search the first few lines
            boolean valid = false;

            for (int i = 0; i < 10; i++) {
                line = in.readLine();

                if (line == null) {
                    throw new IllegalArgumentException("file ended before " + "<?xml> tag");
                }
                else if (line.contains("<?xml")) {
                    valid = true;
                    break;
                }
            }

            if (!valid) {
                throw new IllegalArgumentException("file contains no " + "<?xml> tag" + " in the first 10 lines ");
            }

            line = in.readLine();
            int searchStart = 0;

            while (line != null) {
                line= line.trim();
                int elementStart = line.indexOf('<', searchStart) + 1;

                // no xml element => try next line
                if (elementStart == 0) {
                    line = in.readLine();
                    searchStart = 0;
                    continue;
                }

                char c = line.charAt(elementStart);

                if (c == '!') {
                    // comment or other xml directive
                    // lastIndexOf here because the comment can contain other xml elements
                    // look for the very last > to find the end of the comment
                    int elementEnd = line.lastIndexOf('>');

                    // no '>' => append next line and try again
                    if (elementEnd == -1) {
                        line += in.readLine();
                    }
                    else if (line.charAt(elementEnd - 1) == '-') {
                        // comment complete
                        line = in.readLine();
                        searchStart = 0;
                    }
                    else {
                        line += in.readLine();
                    }
                }
                else {
                    // no '>' => append next line and try again
                    int elementEnd = line.indexOf('>', elementStart);

                    if (elementEnd == -1) {
                        line += in.readLine();
                        continue;
                    }

                    boolean start = true;
                    boolean end = false;

                    int nameStart = elementStart;

                    // </element...
                    if (c == '/') {
                        start = false;
                        end = true;
                        ++nameStart;
                    }

                    if (nameStart == elementEnd) {
                        logger.warn("ignoring unnamed element at line {}", getLineNumber());
                        searchStart = elementEnd + 1;
                        continue;
                    }

                    int nameEnd = line.indexOf(' ', nameStart);

                    // <element... /
                    if (line.charAt(elementEnd - 1) == '/') {
                        end = true;

                        if (nameEnd == -1) {
                            // <element/> => drop '/' at end of name
                            nameEnd = elementEnd - 1;
                        }
                    }
                    else {
                        if (nameEnd == -1) {
                            nameEnd = elementEnd;
                        }
                    }

                    String name = DataHelper.newString(line.substring(nameStart, nameEnd));

                    if (start) {
                        String unparsedAttributes = line.substring(nameEnd, elementEnd);
                        logger.trace("start element '{}' with attributes '{}'", name, unparsedAttributes);
                        startElement(name, unparsedAttributes);
                    }

                    if (end) {
                        logger.trace("end element '{}'", name);
                        endElement(name);
                    }

                    // more data on the current line?
                    if (elementEnd == (line.length() - 1)) {
                        line = in.readLine();
                        searchStart = 0;
                    }
                    else {
                        searchStart = elementEnd + 1;
                    }
                }
            }
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {
                    // ignore
                }

                in = null;
            }
        }
    }

    protected void reset() {
        skip = false;
    }

    protected abstract void startElement(String element, String unparsedAttributes);

    protected abstract void endElement(String element);

    /**
     * Attributes are not parsed unless needed. Subclasses should call this method to turn the
     * unparsed attributes string into a Map of strings to values.
     */
    protected final Map<String, String> parseAttributes(String unparsedAttributes) {
        return XMLParserHelper.parseAttributes(unparsedAttributes);
    }

    /**
     * Only valid during parsing.
     * 
     * @throws NullPointerException if a parse is not currently in progress
     */
    protected final int getLineNumber() {
        return in.getLineNumber();
    }
}
