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

package com.google.dart.tools.ui.web.pubspec;

import com.google.dart.tools.core.pub.DependencyObject;
import com.google.dart.tools.core.pub.IModelListener;
import com.google.dart.tools.core.pub.PubPackageManager;
import com.google.dart.tools.core.pub.PubspecModel;
import com.google.dart.tools.ui.web.DartWebPlugin;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.DetailsPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.MasterDetailsBlock;
import org.eclipse.ui.forms.SectionPart;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * The Master block for the dependencies - lists all the dependencies
 */
public class DependenciesMasterBlock extends MasterDetailsBlock implements IModelListener {

  class MasterContentProvider implements IStructuredContentProvider {
    @Override
    public void dispose() {
    }

    @Override
    public Object[] getElements(Object inputElement) {
      if (model != null && model.getDependecies() != null) {
        return model.getDependecies();
      }
      return new Object[0];
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

  }

  class MasterLabelProvider extends LabelProvider implements ITableLabelProvider {
    @Override
    public Image getColumnImage(Object obj, int index) {
      if (obj instanceof DependencyObject) {
        return DartWebPlugin.getImage("pubspec.png");
      }
      return null;
    }

    @Override
    public String getColumnText(Object obj, int index) {
      return obj.toString();
    }
  }

  private class AddPackageDialog extends InputDialog {

    public AddPackageDialog(Shell parentShell, String dialogTitle, String dialogMessage,
        String initialValue, IInputValidator validator) {
      super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite composite = (Composite) super.createDialogArea(parent);
      Label label = new Label(composite, SWT.NONE);
      label.setText("For example, unittest, polymer, ...");
      label.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
      Label imageLabel = new Label(composite, SWT.NONE);
      imageLabel.setImage(DartWebPlugin.getImage("pub_package_ex.png"));
      imageLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
      return composite;
    }

    @Override
    protected void validateInput() {
      if (getText().getText().isEmpty()) {
        setErrorMessage("Enter name of package");
      } else {
        setErrorMessage(null);
      }
    }

  }

  private FormPage page;
  private PubspecModel model;

  private TableViewer viewer;

  private SectionPart sectionPart;

  private boolean editable;

  public DependenciesMasterBlock(FormPage page, boolean editable) {
    this.page = page;
    model = ((PubspecEditor) page.getEditor()).getModel();
    this.editable = editable;
  }

  @Override
  public void createContent(IManagedForm managedForm, Composite parent) {
    super.createContent(managedForm, parent);

    sashForm.setWeights(new int[] {50, 50});
  }

  public TableViewer getViewer() {
    return viewer;
  }

  @Override
  public void modelChanged(Object[] objects, String type) {
    viewer.refresh();
    if (type.equals(IModelListener.ADDED)) {
      viewer.setSelection(new StructuredSelection(objects[0]));
    }
    if (type.equals(IModelListener.REMOVED) && model.getDependecies().length > 0) {
      viewer.setSelection(new StructuredSelection(model.getDependecies()[0]));
    }
  }

  @Override
  protected void applyLayout(final Composite parent) {
    GridLayout layout = new GridLayout();
    layout.marginLeft = 5;
    layout.marginBottom = 5;
    parent.setLayout(layout);
  }

  @Override
  protected void createMasterPart(final IManagedForm managedForm, Composite parent) {
    FormToolkit toolkit = managedForm.getToolkit();
    Section section = toolkit.createSection(parent, Section.DESCRIPTION | Section.TITLE_BAR);
    section.setText("Dependencies"); //$NON-NLS-1$
    section.setDescription("Specify all the packages required by this package"); //$NON-NLS-1$
    section.marginHeight = 5;
    Composite client = toolkit.createComposite(section, SWT.WRAP);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 2;
    layout.marginHeight = 2;
    client.setLayout(layout);
    Table t = toolkit.createTable(client, SWT.NULL);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = 20;
    gd.widthHint = 100;
    t.setLayoutData(gd);
    toolkit.paintBordersFor(client);
    Composite buttonGroup = toolkit.createComposite(client);
    gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
    buttonGroup.setLayoutData(gd);
    buttonGroup.setLayout(new GridLayout());

    Button addButton = toolkit.createButton(buttonGroup, "Add...", SWT.PUSH);
    addButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    addButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        handleAddDependency();

      }
    });
    PixelConverter converter = new PixelConverter(addButton);
    int widthHint = converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(addButton);
    Button b = toolkit.createButton(buttonGroup, "Remove", SWT.PUSH);
    b.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    b.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        handleRemoveDependency();

      }
    });

    GridDataFactory.swtDefaults().hint(widthHint, -1).applyTo(b);
    section.setClient(client);
    section.setEnabled(editable);
    sectionPart = new SectionPart(section);
    managedForm.addPart(sectionPart);
    viewer = new TableViewer(t);
    viewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        managedForm.fireSelectionChanged(sectionPart, event.getSelection());
      }
    });
    viewer.setContentProvider(new MasterContentProvider());
    viewer.setLabelProvider(new MasterLabelProvider());
    viewer.setComparator(new ViewerComparator());
    viewer.setInput(page.getEditor().getEditorInput());
    model.addModelListener(this);
  }

  @Override
  protected void createToolBarActions(IManagedForm managedForm) {
  }

  @Override
  protected void registerPages(DetailsPart detailsPart) {
    detailsPart.registerPage(DependencyObject.class, new DependencyDetailsPage(page));
  }

  private void handleAddDependency() {
    String[] list = PubPackageManager.getInstance().getPackageListArray();
    int result = Window.OK;
    String name = null;
    if (list.length > 0) {
      PackageSelectionDialog inputDialog = new PackageSelectionDialog(
          PlatformUI.getWorkbench().getDisplay().getActiveShell(),
          list);
      inputDialog.setTitle("Add dependency");
      inputDialog.setMessage("Enter name of package:");
      result = inputDialog.open();
      if (result == Window.OK) {
        name = (String) inputDialog.getFirstResult();
      }

    } else {
      AddPackageDialog inputDialog = new AddPackageDialog(
          page.getSite().getShell(),
          "Add Dependency",
          "Enter the name of package:",
          "",
          null);

      result = inputDialog.open();
      if (result == Window.OK) {
        name = inputDialog.getValue();
      }
    }
    if (name != null) {
      model.add(new DependencyObject[] {new DependencyObject(name)}, IModelListener.ADDED);
      sectionPart.markDirty();
    }
  }

  private void handleRemoveDependency() {
    ISelection selection = viewer.getSelection();
    if (!selection.isEmpty() && selection instanceof StructuredSelection) {
      DependencyObject o = (DependencyObject) ((StructuredSelection) selection).getFirstElement();
      model.remove(new DependencyObject[] {o}, true);
      sectionPart.markDirty();
    }
  }

}
