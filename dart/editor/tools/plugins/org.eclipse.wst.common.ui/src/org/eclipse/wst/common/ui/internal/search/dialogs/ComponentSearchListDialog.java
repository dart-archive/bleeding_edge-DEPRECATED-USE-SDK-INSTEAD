/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.wst.common.core.search.scope.SearchScope;
import org.eclipse.wst.common.ui.internal.Messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentSearchListDialog extends Dialog {
  private Display display = Display.getCurrent();
  private String dialogTitle;

  protected ComponentSearchListDialogConfiguration configuration;
  private List componentTableViewerInput;
  private List masterComponentList;

  // widgets
  protected Composite topComposite;
  protected Composite bottomComposite;
  private Text textFilter;
  protected TableViewer componentTableViewer;

  protected String fileLocationLabel = Messages._UI_LABEL_DECLARATION_LOCATION;
  protected ViewForm fileLocationView;
  protected CLabel locationLabel;

  // keep track of the item previously selected in the table
  private TableItem prevItem;
  private String prevItemText;

  protected Object componentSelection;
  protected Object qualifierTextSelection;

  protected ToolBar filterToolBar;
  protected ToolItem toolItem;
  protected MenuManager fMenuManager;

  protected HashMap TableDecoratorTrackingTool = new HashMap();
  private Button newButton;

  public ComponentSearchListDialog(Shell shell, String dialogTitle,
      ComponentSearchListDialogConfiguration configuration) {
    super(shell);
    setShellStyle(getShellStyle() | SWT.RESIZE);
    this.dialogTitle = dialogTitle;
    this.configuration = configuration;
    componentTableViewerInput = new ArrayList();
    masterComponentList = new ArrayList();
    configuration.init(this);
  }

  public void create() {
    super.create();
    getButton(IDialogConstants.OK_ID).setEnabled(false);
    setTextFilterFocus();
  }

  protected void setTextFilterFocus() {
    textFilter.setFocus();
  }

  protected Control createDialogArea(Composite parent) {
    getShell().setText(dialogTitle);

    Composite mainComposite = (Composite) super.createDialogArea(parent);
    GridData gData = (GridData) mainComposite.getLayoutData();
    gData.heightHint = 500;

    configuration.createWidgetAboveQualifierBox(mainComposite);
    // Subclasses may use this Composite to add desired widgets
    //topComposite = new Composite(mainComposite, SWT.NONE);
    //topComposite.setLayoutData(new GridData());
    //topComposite.setLayout(new GridLayout());

    // do we need to introduce a method here to contain this
    // so we can add different parent other than 'topComposite'
    Composite filterLabelAndText = new Composite(mainComposite, SWT.NONE);
    GridLayout layoutFilterLabelAndText = new GridLayout(2, false);
    layoutFilterLabelAndText.marginWidth = 0;
    layoutFilterLabelAndText.marginHeight = 0;
    filterLabelAndText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    filterLabelAndText.setLayout(layoutFilterLabelAndText);

    // Create Text textFilter

    Label filterLabel = new Label(filterLabelAndText, SWT.NONE);
    filterLabel.setText(configuration.getFilterLabelText());// + "(? = any character, * = any string):");
    GridData filterLabelData = new GridData();
    filterLabelData.horizontalSpan = 2;
    filterLabel.setLayoutData(filterLabelData);

    textFilter = new Text(filterLabelAndText, SWT.SINGLE | SWT.BORDER);
    textFilter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    textFilter.addModifyListener(new TextFilterModifyAdapter());
    GridData textFilterData = new GridData();
    textFilterData.horizontalAlignment = GridData.FILL;
    textFilterData.grabExcessHorizontalSpace = true;
    textFilter.setLayoutData(textFilterData);

    final INewComponentHandler handler = configuration.getNewComponentHandler();
    if (handler != null) {
      newButton = new Button(filterLabelAndText, SWT.NONE);
      newButton.setText(Messages._UI_LABEL_New);
      newButton.addSelectionListener(new SelectionListener() {

        public void widgetDefaultSelected(SelectionEvent e) {
          handler.openNewComponentDialog();
        }

        public void widgetSelected(SelectionEvent e) {
          handler.openNewComponentDialog();
        }
      });
    }

    // Create Component TableViewer
    createComponentTableViewer(mainComposite);

    configuration.createWidgetAboveQualifierBox(mainComposite);

    // Create Qualifier List widget
    Label qualifierLabel = new Label(mainComposite, SWT.NONE);
    qualifierLabel.setText(Messages._UI_LABEL_QUALIFIER);
    qualifierLabel.setText(fileLocationLabel);

    fileLocationView = new ViewForm(mainComposite, SWT.BORDER | SWT.FLAT);
    GridData data = new GridData();
    data.horizontalAlignment = GridData.FILL;
    data.grabExcessHorizontalSpace = true;
    data.heightHint = 22;
    fileLocationView.setLayoutData(data);

    locationLabel = new CLabel(fileLocationView, SWT.FLAT);
    fileLocationView.setContent(locationLabel);
    locationLabel.setFont(fileLocationView.getFont());

    configuration.createWidgetBelowQualifierBox(mainComposite);

    bottomComposite = new Composite(mainComposite, SWT.NONE);
    bottomComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    bottomComposite.setLayout(new GridLayout());

    // Populate the Component TableViewer via the provider
    // TODO: Is this the right way to set/get the ContentProvider?
    componentTableViewer.setContentProvider(new ComponentTableContentProvider());
    componentTableViewer.setLabelProvider(configuration.getDescriptionProvider().getLabelProvider());
    componentTableViewer.setSorter(new ViewerSorter());
    componentTableViewer.setInput(componentTableViewerInput);

    // TODO (cs) need to do some work to make the default search scope
    // more well defined, currently the default behaviour is to pass a null
    // argument in to populateMasterComponentList but we should provide
    // getters/setters to allow the default to be controlled
    populateMasterComponentList(null);
    refreshTableViewer("");

    return mainComposite;
  }

  /*
   * Creates the Component TableViewer.
   */
  private void createComponentTableViewer(Composite base) {
    componentTableViewer = createFilterMenuAndTableViewer(base);

    componentTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        //IStructuredSelection structuredSelection = (IStructuredSelection) event.getSelection();
        //List qualifiers = searchListProvider.getQualifiers(structuredSelection.getFirstElement());
        //updateQualifierList(qualifiers);
        updateCanFinish();
      }
    });

    componentTableViewer.getTable().addSelectionListener(new SelectionListener() {
      // Changing the text for the component selected and display its source
      // file in the box under the table viewer

      IComponentDescriptionProvider descriptionProvider = configuration.getDescriptionProvider();

      public void widgetSelected(SelectionEvent e) {
        run();
      }

      public void widgetDefaultSelected(SelectionEvent e) {
        // bug 144548 - unnecessary
        // run();
      }

      private void run() {
        // restores the text of previous item
        if (prevItem != null && !prevItem.isDisposed()) {
          prevItem.setText(prevItemText);
        }
        TableItem[] items = componentTableViewer.getTable().getSelection();
        Object component = items[0].getData();

        prevItem = items[0];
        prevItemText = items[0].getText();

        // add clarification for the first selected item
        items[0].setText(descriptionProvider.getName(component) + " - "
            + descriptionProvider.getQualifier(component));

        updateLocationView(component, descriptionProvider);
      }
    });

    componentTableViewer.addDoubleClickListener(new IDoubleClickListener() {

      public void doubleClick(DoubleClickEvent event) {
        okPressed();
      }

    });
  }

  protected TableViewer createFilterMenuAndTableViewer(Composite comp) {
    Composite labelAndFilter = new Composite(comp, SWT.NONE);
    labelAndFilter.setLayoutData(new GridData(GridData.FILL_BOTH));
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    labelAndFilter.setLayout(layout);

    Label tableLabel = new Label(labelAndFilter, SWT.NONE);
    tableLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    tableLabel.setText(configuration.getListLabelText());

    filterToolBar = new ToolBar(labelAndFilter, SWT.FLAT);
    configuration.createToolBarItems(filterToolBar);

    TableViewer tableViewer = new TableViewer(new Table(labelAndFilter, SWT.SINGLE | SWT.BORDER));
    Control TableWidget = tableViewer.getTable();
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 2;
    TableWidget.setLayoutData(gd);

    return tableViewer;
  }

  private void updateLocationView(Object component, IComponentDescriptionProvider lp) {
    IFile file = lp.getFile(component);

    if (file == null) {
      locationLabel.setText("");
      locationLabel.setImage(null);
      return;
    }
    String filePath = "";
    filePath = file.getFullPath().toString();
    //locationView.redraw();

    locationLabel.setText(TextProcessor.process(filePath));
    locationLabel.setImage(lp.getFileIcon(component));
  }

  /*
   * Returns the processed filter text for the Text field. Inserts a "." before each supported
   * meta-character.
   */
  protected String getProcessedFilterString() {
    return processFilterString(textFilter.getText());
  }

  /*
   * If supported metacharacters are used in the filter string, we need to insert a "." before each
   * metacharacter.
   */
  private String processFilterString(String inputString) {
    if (!(inputString.equals(""))) {
      inputString = insertString("*", ".", inputString);
      inputString = insertString("?", ".", inputString);
      inputString = inputString + ".*";
    } else {
      inputString = ".*";
    }

    return inputString.toLowerCase();
  }

  /*
   * Helper method to insert a "." before each metacharacter in the search/filter string.
   */
  private String insertString(String target, String newString, String string) {
    StringBuffer stringBuffer = new StringBuffer(string);

    int index = stringBuffer.indexOf(target);
    while (index != -1) {
      stringBuffer = stringBuffer.insert(index, newString);
      index = stringBuffer.indexOf(target, index + newString.length() + target.length());
    }

    return stringBuffer.toString();
  }

  /*
   * Listens to changes made in the text filter widget
   */
  private class TextFilterModifyAdapter implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      if (e.widget == textFilter) {
        if (delayedEvent != null) {
          delayedEvent.CANCEL = true;
        }

        delayedEvent = new DelayedEvent();
        Display.getCurrent().timerExec(400, delayedEvent);
      }
    }
  }

  //TODO... do we really need one instance?
  private DelayedEvent delayedEvent;

  /*
   * Update the component TableViewer when the text filter is modified. Use a DelayedEvent so we
   * don't update on every keystroke.
   */
  private class DelayedEvent implements Runnable {
    public boolean CANCEL = false;

    public void run() {
      if (!CANCEL) {
        refreshTableViewer(getProcessedFilterString());

        // Select first match
        if (componentTableViewer.getTable().getItemCount() > 0) {
          TableItem item = componentTableViewer.getTable().getItems()[0];
          TableItem items[] = new TableItem[1];
          items[0] = item;
          componentTableViewer.getTable().setSelection(items);
        }

        // Update qualifierList
        //IStructuredSelection structuredSelection = (IStructuredSelection) componentTableViewer.getSelection();
        // TODO ... manage qualifiers
        //List qualifiers = searchListProvider.getQualifiers(structuredSelection.getFirstElement());
        //updateQualifierList(qualifiers);

        updateCanFinish();
      }
    }
  }

  class ComponentList implements IComponentList {
    private Vector objectVector = new Vector();
    private long currentChangeCounter = 0;
    private long lastUpdateTime = 0;

    public void add(Object o) {
      objectVector.add(o);
      currentChangeCounter++;
      doViewerUpdate();
    }

    public void addAll(Collection collection) {
      objectVector.addAll(collection);
      currentChangeCounter += collection.size();
      doViewerUpdate();
    }

    private void doViewerUpdate() {
      // TODO: Investigate if we should also add a timer condition??
      //  if (currentChangeCounter >= 10) {
      //      currentChangeCounter = 0;
      //      fireUpdateList(this);
      //  }

      // cs: yep I think we really do need to use a time based approach
      //          
      long time = System.currentTimeMillis();
      if (time - lastUpdateTime > 300) {
        lastUpdateTime = time;
        fireUpdateList(ComponentList.this);
      }
    }

    public int size() {
      return objectVector.size();
    }

    public List subList(int startIndex, int endIndex) {
      return objectVector.subList(startIndex, endIndex);
    }

    public Iterator iterator() {
      return objectVector.iterator();
    }
  }

  // this method gets called from a non-ui thread so needs to call
  // asyncExec to ensure the UI updates happen on the UI thread
  //
  protected void fireUpdateList(final ComponentList list) {
    Runnable runnable = new Runnable() {
      public void run() {
        // add new objects
        int growingListSize = list.size();
        int currentSize = masterComponentList.size();
        if (growingListSize > currentSize) {
          masterComponentList.addAll(list.subList(currentSize, growingListSize));
        }

        refreshTableViewer(getProcessedFilterString());
      }
    };
    display.asyncExec(runnable);
  }

  public void updateForFilterChange() {
    populateMasterComponentList(null);
    refreshTableViewer(getProcessedFilterString());
  }

  /*
   * Populate the Component TreeViewer with items.
   */
  protected void populateMasterComponentList(final SearchScope searchScope) {
    masterComponentList.clear();

    final ComponentList componentList = new ComponentList();

    // TODO (cs) it doesn't seem to make sennse to do any of the work on the UI thread
    // I've change the behaviour here to do all of the work in the background
    //
    //searchListProvider._populateComponentListQuick(componentList, 0);        
    Job job = new Job("read components") {
      protected IStatus run(IProgressMonitor monitor) {
        try {
          // this stuff gets executed on a non-UI thread
          //
          configuration.getSearchListProvider().populateComponentList(componentList, searchScope,
              null);
          // Do a final update of our Input for the component tree viewer.
          fireUpdateList(componentList);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  protected void refreshTableViewer(String filterText) {
    componentTableViewerInput.clear();
    ILabelProvider labelProvider = configuration.getDescriptionProvider().getLabelProvider();
    Pattern regex = Pattern.compile(filterText);
    Iterator it = masterComponentList.iterator();
    while (it.hasNext()) {
      Object item = it.next();
      String itemString = labelProvider.getText(item);
      Matcher m = regex.matcher(itemString.toLowerCase());
      if (itemString.toLowerCase().startsWith(filterText) || m.matches()) {
        componentTableViewerInput.add(item);
      }
    }

    componentTableViewer.refresh();
    decorateTable();
  }

  /**
   * Looking at each item in the Table. If there are other items with same name , then add extra
   * info (namespace, file) into the the text label of all these duplicated items. - This should be
   * called everytime the Table viewer is refreshed..
   */
  protected void decorateTable() {
    TableDecoratorTrackingTool.clear();

    IComponentDescriptionProvider lp = configuration.getDescriptionProvider();

    // init the name-duplicates counter
    for (int i = 0; i < componentTableViewerInput.size(); i++) {
      Object currentItem = componentTableViewerInput.get(i);
      String name = lp.getName(currentItem);
      Integer count = (Integer) TableDecoratorTrackingTool.get(name);
      if (count == null) {
        TableDecoratorTrackingTool.put(name, new Integer(1));
      } else {
        TableDecoratorTrackingTool.put(name, new Integer(count.intValue() + 1));
      }
    }

    // Modify/decorate those items in the Table that have duplicated name
    TableItem[] items = componentTableViewer.getTable().getItems();
    for (int i = 0; i < items.length; i++) {
      Object currentItem = items[i].getData();
      Integer count = (Integer) TableDecoratorTrackingTool.get(lp.getName(currentItem));
      if (count != null && count.intValue() > 1) {
        items[i].setText(lp.getName(currentItem) + " - " + lp.getQualifier(currentItem));
      }
    }
  }

  /*
   * If there is a selection in the ComponentTreeViewer, enable OK
   */
  protected void updateCanFinish() {
    IStructuredSelection selection = (IStructuredSelection) componentTableViewer.getSelection();
    if (selection.getFirstElement() != null) {
      getButton(IDialogConstants.OK_ID).setEnabled(true);
    } else {
      getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
  }

  protected void okPressed() {
    IStructuredSelection selection = (IStructuredSelection) componentTableViewer.getSelection();
    componentSelection = selection.getFirstElement();

    super.okPressed();
  }

  private class ComponentTableContentProvider implements ITreeContentProvider {
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof List) {
        return ((List) parentElement).toArray();
      }
      return new Object[0];
    }

    public Object[] getElements(Object inputElement) {
      return getChildren(inputElement);
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      if (getChildren(element).length > 0) {
        return true;
      }
      return false;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    public void dispose() {
    }
  }

  public ComponentSpecification getSelectedComponent() {
    ComponentSpecification result = null;
    if (componentSelection != null) {
      result = new ComponentSpecification();
      IComponentDescriptionProvider componentDescriptionProvider = configuration.getDescriptionProvider();
      result.setName(componentDescriptionProvider.getName(componentSelection));
      result.setQualifier(componentDescriptionProvider.getQualifier(componentSelection));
      result.setFile(componentDescriptionProvider.getFile(componentSelection));
      result.setObject(componentSelection);
    }
    return result;
  }
}
