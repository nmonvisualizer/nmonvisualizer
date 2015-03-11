package com.ibm.nmon;

import java.lang.reflect.Method;

/**
 * Pseudo-main class needed so that there can be multiple main classes in the NMONVisualizer jar but
 * Jar-in-Jar-Loader can still setup the classpath. This class defaults to NMONVisualizer if no
 * arguments are given. Otherwise, the first argument is assumed to be the main class.
 */
public final class DelegatingMain {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            com.ibm.nmon.gui.main.NMONVisualizerGui.main(new String[0]);
        }
        else {
            String className = args[0];
            Class clazz = null;

            String[] newArgs = null;

            try {
                clazz = Class.forName(className);

                // remove the class name from the arguments
                newArgs = new String[args.length - 1];
                System.arraycopy(args, 1, newArgs, 0, newArgs.length);
            }
            catch (ClassNotFoundException e) {
                // assume passing arguments to NMONVisualizerGui
                clazz = com.ibm.nmon.gui.main.NMONVisualizerGui.class;

                newArgs = args;
            }

            Method main = clazz.getMethod("main", String[].class);
            main.invoke(DelegatingMain.class, (Object) newArgs);
        }
    }
}
