package com.ibm.nmon;

import java.lang.reflect.Method;

/**
 * Pseudo-main class needed so that there can be multiple main classes in the NMONVisualizer jar but
 * Jar-in-Jar-Loader can still setup the classpath. This class defaults to NMONVisualizer if no
 * arguments are given. Otherwise, the first argument is assumed to be the main class.
 */
public final class DelegatingMain {
    public static void main(String[] args) throws Throwable {
        if (args.length == 0) {
            com.ibm.nmon.gui.main.NMONVisualizerGui.main(null);
        }
        else {
            String[] newArgs = new String[args.length - 1];
            System.arraycopy(args, 1, newArgs, 0, newArgs.length);

            Method main = Class.forName(args[0]).getMethod("main", String[].class);
            main.invoke(DelegatingMain.class, (Object) newArgs);
        }
    }
}
