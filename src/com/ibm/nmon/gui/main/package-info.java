/**
 * <p>
 * ChartPanels fire events when a chart changes, annotations are added and lines / bars are
 * highlighted.
 * </p>
 * <p>
 * A ReportPanel listens to all ChartPanel events and forwards them to its listeners. ReportPanels
 * current listeners are DataSetView, SummaryView and ReportSplitPane. Since these are all
 * subclasses of ChartSplitPane, ChartSplitPane then forwards PropertyChange events to its
 * listeners. The specific subclasses of ChartSplitPane are responsible however for actually
 * connecting the event handling to their respective ReportPanels.
 * </p>
 * <p>
 * DataTypeView is a special case since it does not have a ReportPanel. Instead, it uses a
 * ChartPanel and directly listens to events from the ChartPanel.
 * </p>
 * <p>
 * Finally, ViewManager listens for all the events from SummaryView, DataSetView and DataTypeView.
 * Annotation events are used to store annotations in the AnnotationCache. Chart events are used to
 * setup the annotations when charts change.
 * <p>
 */
package com.ibm.nmon.gui.main;