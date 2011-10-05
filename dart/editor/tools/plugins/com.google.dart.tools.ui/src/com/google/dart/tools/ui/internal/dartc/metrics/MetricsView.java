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

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.part.ViewPart;

/**
 * MetricsView displays the build metrics for the DartCompiler
 */
public class MetricsView extends ViewPart implements ControlListener {

  /**
   * The view that contains the list of compilations units to query
   */
  public ListViewer listViewer;

  /**
   * The view that contains the Table of compiler metrics
   */
  public TableViewer tableViewer;

  /**
   * The manager that handles and organizes all the compilation units and their metrics
   */
  private final MetricsManager manager;

  /**
   * Column that contains the names of the given compiler metrics
   */
  private TableColumn col;

  /**
   * Column that contains the values of all the measured compiler metrics
   */
  private TableColumn col2;

  /**
   * Sash that allows the views to be resized
   */
  private SashForm sashForm;

  /**
   * Parent of the view
   */
  private Composite parent;

  public MetricsView() {
    super();
    this.manager = new MetricsManager();
    manager.setView(this);
  }

  @Override
  public void controlMoved(ControlEvent e) {
    //do nothing
  }

  /**
   * On resize, checks to see if it is Wider than it is Tall if so, it changes the layout to
   * Vertical.
   */
  @Override
  public void controlResized(ControlEvent e) {
    int orientation;
    if (parent.getSize().y > parent.getSize().x) {
      orientation = SWT.VERTICAL;
    } else {
      orientation = SWT.HORIZONTAL;
    }
    sashForm.setOrientation(orientation);
  }

  /**
   * Creates the view
   */
  @Override
  public void createPartControl(Composite parent) {
    this.parent = parent;
    int orientation;
    if (parent.getSize().y > parent.getSize().x) {
      orientation = SWT.VERTICAL;
    } else {
      orientation = SWT.HORIZONTAL;
    }
    sashForm = new SashForm(parent, orientation);

    listViewer = new ListViewer(sashForm, SWT.V_SCROLL);
    listViewer.add(manager.getDartLibraries());
    listViewer.getList().setSelection(0);
    listViewer.addSelectionChangedListener(manager);

    tableViewer = new TableViewer(sashForm, SWT.SINGLE);
    Table table = tableViewer.getTable();
    MetricsTableLabelProvider labelProvider = new MetricsTableLabelProvider();
    labelProvider.setManager(manager);
    tableViewer.setLabelProvider(labelProvider);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    col = new TableColumn(table, SWT.LEFT);
    col.setWidth(200);
    col.setText("No Compilation Unit Selected");
    col.setResizable(true);

    col2 = new TableColumn(table, SWT.LEFT);
    col2.setWidth(150);
    col2.setResizable(true);

    tableViewer.add(manager.metricTitles);
    sashForm.setWeights(new int[] {2, 3});
    parent.addControlListener(this);
  }

  @Override
  public void dispose() {
    super.dispose();
    manager.dispose();
  }

  @Override
  public void setFocus() {
  }

  /**
   * Sets the Titles of the two columns.
   * 
   * @param currentSelection-Titles the left Column {@link #col} whatever the selected element in
   *          the {@link #listViewer} is named
   * @param time-Sets the title of the right Column {@link #col2} the amount of time since the given
   *          compilation unit was last compiled
   */
  public void setSelectedElement(String currentSelection, Long time) {
    col.setText(currentSelection);
    if (time != null) {
      float elapsedTime = System.currentTimeMillis() - time;
      int days = (int) Math.floor(elapsedTime / (24 * 60 * 60 * 1000));
      elapsedTime = elapsedTime - (days * (24 * 60 * 60 * 1000));
      int hours = (int) Math.floor(elapsedTime / (60 * 60 * 1000));
      elapsedTime = elapsedTime - (hours * (60 * 60 * 1000));
      int minutes = (int) Math.floor(elapsedTime / (60 * 1000));
      elapsedTime = elapsedTime - (minutes * (60 * 1000));
      int seconds = (int) Math.floor(elapsedTime / 1000);
      String day = "";
      String hour = "";
      String minute = "";
      String second = "";
      if (days > 0) {
        day = days + "d ";
      }
      if (!day.equals("") || hours > 0) {
        hour = hours + "h ";
      }
      if (!hour.equals("") || minutes > 0) {
        minute = minutes + "m ";
      }
      second = seconds + "s ago";
      col2.setText(day + hour + minute + second);
    } else {
      col2.setText("No recent build to measure");
    }
  }

}
