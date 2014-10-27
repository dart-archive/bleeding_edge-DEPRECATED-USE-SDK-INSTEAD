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
import com.google.dart.tools.ui.internal.projects.SamplesLabelProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

class SamplesComposite extends Composite {
  private static final String NEW_APPPLICATION_SETTINGS = "newApplicationWizard.settings"; //$NON-NLS-1$
  private static final String CONTENT_GENERATION_DISABLED = "contentGenerationDisabled"; //$NON-NLS-1$

  private WizardPage page;
  private Button addSampleContentCheckbox;
  private TableViewer samplesViewer;

  public SamplesComposite(WizardPage page, Composite parent, int style) {
    super(parent, style);

    this.page = page;

    initialize();
  }

  protected AbstractSample getCurrentSample() {
    if (addSampleContentCheckbox.getSelection()) {
      IStructuredSelection selection = (IStructuredSelection) samplesViewer.getSelection();

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

    samplesViewer = new TableViewer(contentGroup, SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER
        | SWT.FULL_SELECTION);
    samplesViewer.setLabelProvider(new SamplesLabelProvider());
    samplesViewer.setContentProvider(new ArrayContentProvider());
    samplesViewer.setInput(new ArrayList<AbstractSample>());
    GridDataFactory.fillDefaults().hint(300, 90).grab(true, false).align(SWT.FILL, SWT.CENTER).applyTo(
        samplesViewer.getControl());
    samplesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        updateMessageAndEnablement();
      }
    });

    samplesViewer.getTable().setEnabled(addSampleContentCheckbox.getSelection());

    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        populateSamplesList();
      }
    });
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

  private void populateSamplesList() {
    try {
      page.getWizard().getContainer().run(true, false, new IRunnableWithProgress() {
        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException,
            InterruptedException {
          monitor.beginTask("", IProgressMonitor.UNKNOWN);
          final List<AbstractSample> samples = AbstractSample.getAllSamples();
          Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
              samplesViewer.setInput(samples);
              samplesViewer.setSelection(new StructuredSelection(getDefaultSample(samples)));
            }
          });
          monitor.done();
        }
      });
    } catch (InvocationTargetException e) {
      DartToolsPlugin.log(e);
    } catch (InterruptedException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void updateMessageAndEnablement() {
    samplesViewer.getTable().setEnabled(addSampleContentCheckbox.getSelection());
  }
}
