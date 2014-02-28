/*
 * Copyright (c) 2012, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.eclipse.wizards;

import com.google.dart.tools.core.generator.AbstractSample;
import com.google.dart.tools.ui.DartToolsPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import java.util.List;

class SamplesComposite extends Composite {
  static final String NEW_APPPLICATION_SETTINGS = "newApplicationWizard.settings"; //$NON-NLS-1$
  private static final String CONTENT_GENERATION_DISABLED = "contentGenerationDisabled"; //$NON-NLS-1$

  private Button addSampleContentCheckbox;
  private ListViewer samplesListViewer;

  public SamplesComposite(Composite parent, int style) {
    super(parent, style);

    initialize();
  }

  protected AbstractSample getCurrentSample() {
    if (addSampleContentCheckbox.getSelection()) {
      IStructuredSelection selection = (IStructuredSelection) samplesListViewer.getSelection();

      if (selection.isEmpty()) {
        return null;
      } else {
        return (AbstractSample) selection.getFirstElement();
      }
    } else {
      return null;
    }
  }

  private void createSampleGroup() {

    Group contentGroup = new Group(this, SWT.NONE);
    contentGroup.setText("Sample content");
    GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(contentGroup);
    GridLayoutFactory.fillDefaults().margins(8, 8).applyTo(contentGroup);

    addSampleContentCheckbox = new Button(contentGroup, SWT.CHECK);
    addSampleContentCheckbox.setText("Create sample content");
    addSampleContentCheckbox.setSelection(getGenerateContentPreference());
    addSampleContentCheckbox.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
            NEW_APPPLICATION_SETTINGS);
        settings.put(CONTENT_GENERATION_DISABLED, !addSampleContentCheckbox.getSelection());

        updateMessageAndEnablement();
      }
    });

    Label spacer = new Label(contentGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
    GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(spacer);

    samplesListViewer = new ListViewer(contentGroup);
    samplesListViewer.setLabelProvider(new LabelProvider());
    samplesListViewer.setContentProvider(new ArrayContentProvider());
    List<AbstractSample> samples = AbstractSample.getAllSamples();
    samplesListViewer.setInput(samples);
    GridDataFactory.fillDefaults().hint(-1, 60).grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(
        samplesListViewer.getControl());
    samplesListViewer.setSelection(new StructuredSelection(getDefaultSample(samples)));
    samplesListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateMessageAndEnablement();
      }
    });

    samplesListViewer.getList().setEnabled(addSampleContentCheckbox.getSelection());
  }

  private AbstractSample getDefaultSample(List<AbstractSample> samples) {
    for (AbstractSample sample : samples) {
      if (sample.shouldBeDefault()) {
        return sample;
      }
    }

    return samples.get(0);
  }

  private boolean getGenerateContentPreference() {
    IDialogSettings settings = DartToolsPlugin.getDefault().getDialogSettingsSection(
        NEW_APPPLICATION_SETTINGS);
    return !settings.getBoolean(CONTENT_GENERATION_DISABLED);
  }

  private void initialize() {
    GridData gridData1 = new GridData();
    gridData1.heightHint = -1;

    GridData gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.horizontalAlignment = GridData.FILL;
    gridData.verticalAlignment = GridData.CENTER;
    gridData.grabExcessHorizontalSpace = true;

    GridLayout gridLayout = new GridLayout();
    gridLayout.numColumns = 3;
    gridLayout.horizontalSpacing = 5;
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    gridLayout.verticalSpacing = 10;

    setLayout(gridLayout);

    createSampleGroup();

    setSize(new Point(449, 311));
  }

  private void updateMessageAndEnablement() {

    samplesListViewer.getList().setEnabled(addSampleContentCheckbox.getSelection());
  }

}
