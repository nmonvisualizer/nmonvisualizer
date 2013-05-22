package com.ibm.nmon.gui.file;

import java.util.List;
import java.util.Map;

import java.util.TimeZone;

import javax.swing.SwingUtilities;

import com.ibm.nmon.gui.main.NMONVisualizerGui;

import com.ibm.nmon.gui.util.ItemProgressDialog;

import com.ibm.nmon.util.ParserLog;

/**
 * Runnable responsible for actually parsing files. This keeps the parsing out of the Swing event
 * dispatching thread. Creates a dialog box with a progress bar that is updated as each file is
 * parsed.
 */
public final class ParserRunner implements Runnable {
    private final NMONVisualizerGui gui;
    private final List<String> toParse;
    private final TimeZone timeZone;

    private final ItemProgressDialog progress;

    private final Map<String, String> errors;

    public ParserRunner(NMONVisualizerGui gui, List<String> toParse, TimeZone timeZone) {
        this.gui = gui;
        this.toParse = toParse;
        this.timeZone = timeZone;

        progress = new ItemProgressDialog(gui, "Parsing Files...", toParse.size());

        // used linked map so errors are presented in the order parsed
        errors = new java.util.LinkedHashMap<String, String>();
    }

    public void run() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progress.setVisible(true);
            }
        });

        // assume that Swing.invokeLater calls are placed on an in-order queue
        // i.e. dispose call at the end of run will not be called until after all the progress bar
        // updates are completed

        ParserLog log = ParserLog.getInstance();

        for (int i = 0; i < toParse.size(); i++) {
            final String filename = toParse.get(i).replace('\\', '/' );
            
            log.setCurrentFilename(filename);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String name = filename;
                    int idx = name.lastIndexOf('/');
                    
                    if (idx != -1) {
                        name = name.substring(idx + 1); 
                    }
                    
                    progress.setCurrentItem(name);
                }
            });

            try {
                gui.parse(filename, timeZone);
            }
            catch (Exception e) {
                log.getLogger().error("could not parse " + filename, e);
            }

            if (log.hasData()) {
                errors.put(log.getCurrentFilename(), log.getMessages());
            }

            // update the progress bar on each file
            // wait here because parsing the very first file hits JIT, GC and object creation
            // keep the parser from getting ahead of the UI so the user does not see a 'flash' of
            // progress
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        progress.updateProgress();
                    }
                });
            }
            catch (Exception e) {
                // ignore
            }
        }

        // close progress when done
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (progress != null) {
                    progress.dispose();

                    if (!errors.isEmpty()) {
                        new ParserErrorDialog(gui, errors).setVisible(true);
                    }
                }
            }
        });
    }
}
