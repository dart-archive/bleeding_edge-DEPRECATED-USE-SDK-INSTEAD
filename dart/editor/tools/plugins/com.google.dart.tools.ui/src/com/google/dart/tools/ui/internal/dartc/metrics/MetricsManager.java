/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.dartc.metrics;

import com.google.dart.compiler.CompilerConfiguration;
import com.google.dart.compiler.metrics.CompilerMetrics;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.builder.MetricsListener;
import com.google.dart.tools.core.internal.builder.MetricsMessenger;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * This class manages the Metrics used in {@link MetricsView} it tracks all the metrics for all the
 * currently built libraries and when the view asks for numbers from a new lib/app the manager give
 * the correct statistics back.
 */
public class MetricsManager implements MetricsListener, ISelectionChangedListener {

  /**
   * The instance of the MetricsView to be managed.
   */
  private MetricsView view;

  /**
   * Array of all the names of the compiler metrics to be queried.
   */
  public String[] metricTitles = {
      "total-compilation-time", "parse-time", "units-parsed", "src-chars-parsed",
      "src-lines-parsed", "non-comment-src-chars-parsed", "non-comment-src-lines-parsed",
      "js-output-char-size", "js-native-lib-output-char-size",
      "JS output characters consumed by nativelibraries", "Percent time spent parsing",
      "Time spent per compilation Unit", "lines per ms", "non-comment lines per ms",
      "output:input characters including comments", "output:input characters excluding comments"};

  /**
   * Map from the name of a compilation unit to a map of statistic names and their values.
   */
  public HashMap<String, HashMap<String, String>> compilationUnitToStats = new HashMap<String, HashMap<String, String>>();

  /**
   * Map from the name of a statistic to its value.
   */
  public HashMap<String, String> metricNameToStat = new HashMap<String, String>();

  /**
   * The name of the CompilationUnit that is currently selected in the ListView.
   */
  private String currentSelection;

  /**
   * Map from a compilation unit to its most recent build time for reference in the tableView.
   */
  private final HashMap<String, Long> compilerUnitToBuildTime = new HashMap<String, Long>();

  /**
   * Constructor
   */
  public MetricsManager() {
    MetricsMessenger.getSingleton().addListener(this);
    for (String s : getDartLibraries()) {
      compilationUnitToStats.put(s, newHashMap());
    }
  }

  public void dispose() {
    MetricsMessenger.getSingleton().removeListener(this);
  }

  /**
   * Used by the {@link MetricsTableLabelProvider} to fill in the correct stat given a query
   * 
   * @param statTitle Name of the statistic queried
   * @return the String representation of the answer to the query
   */
  public String getStat(String statTitle) {
    if (currentSelection != null) {
      return compilationUnitToStats.get(currentSelection).get(statTitle);
    }
    return "";
  }

  /**
   * Gather's the statistics and puts them into an easy to parse map.
   * 
   * @return HashMap from "name of metric" -> "value of metric"
   */
  public HashMap<String, String> initStatMap(CompilerMetrics metrics) {
    HashMap<String, String> statMap = new HashMap<String, String>();
    statMap.put(metricTitles[0], String.valueOf(metrics.getTotalCompilationTime()) + " ms");
    statMap.put(metricTitles[1], String.valueOf(metrics.getParseTime()) + " ms");
    statMap.put(metricTitles[2], String.valueOf(metrics.getNumUnitsParsed()));
    statMap.put(metricTitles[3], String.valueOf(metrics.getNumCharsParsed()));
    statMap.put(metricTitles[4], String.valueOf(metrics.getNumLinesParsed()));
    statMap.put(metricTitles[5], String.valueOf(metrics.getNumNonCommentChars()));
    statMap.put(metricTitles[6], String.valueOf(metrics.getNumNonCommentLines()));
    statMap.put(metricTitles[7], String.valueOf(metrics.getJSOutputCharSize()));
    if (metrics.getJSNativeLibCharSize() != -1) {
      statMap.put(metricTitles[8], String.valueOf(metrics.getJSNativeLibCharSize()));
    } else {
      statMap.put(metricTitles[8], "N/A");
    }
    statMap.put(metricTitles[9],
        Double.toString(roundTo(2, metrics.getPercentCharsConsumedByNativeLibraries())) + " %");
    statMap.put(metricTitles[10], Double.toString(roundTo(2, metrics.getPercentTimeParsing()))
        + "%");
    statMap.put(metricTitles[11], Double.toString(roundTo(2, metrics.getTimeSpentPerUnit()))
        + " ms");
    statMap.put(metricTitles[12], Double.toString(roundTo(2, metrics.getLinesPerMS()))
        + " lines/ms");
    statMap.put(metricTitles[13], Double.toString(roundTo(2, metrics.getNonCommentLinesPerMS()))
        + " lines/ms");
    statMap.put(metricTitles[14], Double.toString(roundTo(2, metrics.getRatioOutputToInput())));
    statMap.put(metricTitles[15],
        Double.toString(roundTo(2, metrics.getRatioOutputToInputExcludingComments())));
    return statMap;
  }

  /**
   * Updates the TableView with the correct metrics when the Selection of the listView changes.
   */
  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    ISelection selection = event.getSelection();
    if (!selection.isEmpty()) {
      if (selection instanceof StructuredSelection) {
        String selected = (String) ((StructuredSelection) selection).getFirstElement();
        currentSelection = selected;
        updateTable();
        view.setSelectedElement(currentSelection, compilerUnitToBuildTime.get(currentSelection));
      }
    }
  }

  /**
   * Sets the MetricView that the MetricsManager is in charge of Managing.
   * 
   * @param view-the instance of the MetricsView
   */
  public void setView(MetricsView view) {
    this.view = view;
  }

  /**
   * When the DartBuilder compiles a CompilationUnit it fires of an update which is handled here.
   * This method takes in the new CompilerConfiguration and parses out the name. It then sends the
   * name and the metrics to {@link #handleStats(String, CompilerMetrics)}. After that it sets the
   * most recently compiled unit as the active unit updates the tableViewer.
   */
  @Override
  public void update(CompilerConfiguration config, String libName) {
    handleStats(libName, config.getCompilerMetrics());
    compilerUnitToBuildTime.put(libName, System.currentTimeMillis());
    updateList(libName);
    updateTable();
  }

  /**
   * Using {@link #getDartProjects()}, this method returns an array of all the {@link DartLibrary}s
   * in the workspace.
   * 
   * @return an array of {@link DartProject}s that are defined in the workspace
   */
  String[] getDartLibraries() {
    DartProject[] dartProjects = getDartProjects();
    Collection<String> dartLibraries = new ArrayList<String>(dartProjects.length * 2);
    for (int i = 0; i < dartProjects.length; i++) {
      DartLibrary[] libs;
      try {
        libs = dartProjects[i].getDartLibraries();
        for (int j = 0; j < libs.length; j++) {
          dartLibraries.add(new Path(libs[j].getElementName()).lastSegment());
        }
      } catch (DartModelException e) {
        DartToolsPlugin.log("Error trying to get the collection of all Dart libraries from "
            + dartProjects[i].getProject().getName(), e);
      }
    }
    return dartLibraries.toArray(new String[dartLibraries.size()]);
  }

  /**
   * Using the {@link ResourcesPlugin} and {@link DartProjectNature#hasDartNature(IProject)}, this
   * method returns an array of all the {@link DartProject}s that are open and have the Dart nature.
   * 
   * @return an array of open {@link DartProject}s in the workspace
   */
  private DartProject[] getDartProjects() {
    IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
    int count = projects.length;
    ArrayList<DartProject> dartProjects = new ArrayList<DartProject>(count);
    for (IProject project : projects) {
      if (project.isOpen() && DartProjectNature.hasDartNature(project)) {
        dartProjects.add(DartCore.create(project));
      }
    }
    return dartProjects.toArray(new DartProject[dartProjects.size()]);
  }

  /**
   * This method grabs all the metrics stored in CompilerMetrics and puts them into a map.
   * 
   * @param name
   * @param compilerMetrics
   */
  private void handleStats(String name, CompilerMetrics compilerMetrics) {
    currentSelection = name;
    HashMap<String, String> updatedMap = initStatMap(compilerMetrics);
    compilationUnitToStats.put(name, updatedMap);
  }

  /**
   * Generates a default HashMap to put the newly parsed statistics in.
   * 
   * @return HashMap from name of a statistic -> String representation of its value ("N/A") by
   *         default
   */
  private HashMap<String, String> newHashMap() {
    HashMap<String, String> newMap = new HashMap<String, String>();
    for (String s : metricTitles) {
      newMap.put(s, "N/A");
    }
    return newMap;
  }

  /**
   * Convenience method to help me round doubles to a specified number of decimal places.
   * 
   * @param places-number of decimal places desired
   * @param d-double to round
   * @return desired rounded double
   */
  private double roundTo(int places, double d) {
    int temp = (int) (d * Math.pow(10, places));
    return temp / Math.pow(10, places);
  }

  /**
   * Updates the listViewer of the MetricsView when a new CompilationUnit is added.
   */
  private void updateList(final String name) {
    final Control ctrl = view.listViewer.getControl();
    final List list = view.listViewer.getList();
    //Are we in the UIThread? If so spin it until we are done
    if (ctrl.getDisplay().getThread() == Thread.currentThread()) {
      view.listViewer.refresh();
      view.listViewer.add(getDartLibraries());
      for (int i = 0; i != getDartLibraries().length; ++i) {
        if (list.getItem(i).equals(name)) {
          view.listViewer.getList().setSelection(i);
          currentSelection = name;
          view.setSelectedElement(currentSelection, compilerUnitToBuildTime.get(currentSelection));
          break;
        }
      }
    } else {
      ctrl.getDisplay().asyncExec(new Runnable() {
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
          //Abort if this happens after disposes
          if (ctrl == null || ctrl.isDisposed()) {
            return;
          }
          view.listViewer.refresh();
          view.listViewer.add(getDartLibraries());
          for (int i = 0; i != getDartLibraries().length; ++i) {
            if (list.getItem(i).equals(name)) {
              view.listViewer.getList().setSelection(i);
              currentSelection = name;
              view.setSelectedElement(currentSelection,
                  compilerUnitToBuildTime.get(currentSelection));
              break;
            }
          }
        }
      });
    }
  }

  /**
   * Updates the tableViewer of the MetricsView when a CompilationUnit is selected or updated.
   */
  private void updateTable() {
    final Control ctrl = view.tableViewer.getControl();
    //Are we in the UIThread? If so spin it until we are done
    if (ctrl.getDisplay().getThread() == Thread.currentThread()) {
      view.tableViewer.refresh();
      view.tableViewer.add(metricTitles);
    } else {
      ctrl.getDisplay().asyncExec(new Runnable() {
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
          //Abort if this happens after disposes
          if (ctrl == null || ctrl.isDisposed()) {
            return;
          }
          view.tableViewer.refresh();
          view.tableViewer.add(metricTitles);
        }
      });
    }
  }
}
