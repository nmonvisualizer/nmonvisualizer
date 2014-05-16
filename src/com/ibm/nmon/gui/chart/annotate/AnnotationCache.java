package com.ibm.nmon.gui.chart.annotate;

import java.util.List;

import org.jfree.chart.annotations.Annotation;
import org.jfree.chart.plot.Marker;

public final class AnnotationCache {
    private static final List<Annotation> annotations = new java.util.ArrayList<Annotation>();
    private static final List<Marker> markers = new java.util.ArrayList<Marker>();

    private static final List<AnnotationListener> listeners = new java.util.ArrayList<AnnotationListener>();

    public static void add(Object o) {
        if (o instanceof Marker) {
            markers.add((Marker) o);
        }
        else if (o instanceof Annotation) {
            annotations.add((Annotation) o);
        }

        for (AnnotationListener listener : listeners) {
            listener.annotationAdded();
        }
    }

    public static void addMarker(Marker marker) {
        markers.add(marker);

        for (AnnotationListener listener : listeners) {
            listener.annotationAdded();
        }
    }

    public static void addAnnotation(Annotation annotation) {
        annotations.add(annotation);

        for (AnnotationListener listener : listeners) {
            listener.annotationAdded();
        }
    }

    public static List<Marker> getMarkers() {
        return java.util.Collections.unmodifiableList(markers);
    }

    public static List<Annotation> getAnnotations() {
        return java.util.Collections.unmodifiableList(annotations);
    }

    public static boolean hasAnnotations() {
        return !markers.isEmpty() || !annotations.isEmpty();
    }

    public static void clear() {
        markers.clear();
        annotations.clear();

        for (AnnotationListener listener : listeners) {
            listener.annotationsCleared();
        }
    }

    public static void removeLastMarker() {
        if (!markers.isEmpty()) {
            markers.remove(markers.size() - 1);

            for (AnnotationListener listener : listeners) {
                listener.annotationRemoved();
            }
        }
    }

    public static void removeLastAnnotation() {
        if (!annotations.isEmpty()) {
            annotations.remove(annotations.size() - 1);

            for (AnnotationListener listener : listeners) {
                listener.annotationRemoved();
            }
        }
    }

    public static void addAnnotationListener(AnnotationListener listener) {
        listeners.add(listener);
    }

    public static void removeAnnoationListener(AnnotationListener listener) {
        listeners.remove(listener);
    }

    private AnnotationCache() {}
}
