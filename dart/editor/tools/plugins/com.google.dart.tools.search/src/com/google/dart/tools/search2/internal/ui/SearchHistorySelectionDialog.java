/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.SearchPreferencePage;
import com.google.dart.tools.search.internal.ui.util.SWTUtil;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dialog that shows a list of items with icon and label.
 */
public class SearchHistorySelectionDialog extends SelectionDialog {

  private static class HistoryConfigurationDialog extends StatusDialog {

    private static final int DEFAULT_ID = 100;

    private int fHistorySize;
    private Text fHistorySizeTextField;
    private final List<ISearchResult> fCurrentList;
    private final List<Object> fCurrentRemoves;

    public HistoryConfigurationDialog(Shell parent, List<ISearchResult> currentList,
        List<Object> removedEntries) {
      super(parent);
      fCurrentList = currentList;
      fCurrentRemoves = removedEntries;
      setTitle(SearchMessages.SearchHistorySelectionDialog_history_size_title);
      fHistorySize = SearchPreferencePage.getHistoryLimit();
      setHelpAvailable(false);
    }

    @Override
    protected void buttonPressed(int buttonId) {
      if (buttonId == DEFAULT_ID) {
        IPreferenceStore store = SearchPlugin.getDefault().getPreferenceStore();
        fHistorySizeTextField.setText(store.getDefaultString(SearchPreferencePage.LIMIT_HISTORY));
        validateDialogState();
      }
      super.buttonPressed(buttonId);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
      createButton(parent, DEFAULT_ID,
          SearchMessages.SearchHistorySelectionDialog_restore_default_button, false);
      super.createButtonsForButtonBar(parent);
    }

    /*
     * Overrides method from Dialog
     */
    @Override
    protected Control createDialogArea(Composite container) {
      Composite ancestor = (Composite) super.createDialogArea(container);
      GridLayout layout = (GridLayout) ancestor.getLayout();
      layout.numColumns = 2;
      ancestor.setLayout(layout);

      Label limitText = new Label(ancestor, SWT.NONE);
      limitText.setText(SearchMessages.SearchHistorySelectionDialog_history_size_description);
      limitText.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));

      fHistorySizeTextField = new Text(ancestor, SWT.BORDER | SWT.RIGHT);
      fHistorySizeTextField.setTextLimit(2);
      fHistorySizeTextField.setText(String.valueOf(fHistorySize));
      fHistorySizeTextField.addModifyListener(new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
          validateDialogState();
        }
      });

      GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1);
      gridData.widthHint = convertWidthInCharsToPixels(6);
      fHistorySizeTextField.setLayoutData(gridData);
      fHistorySizeTextField.setSelection(0, fHistorySizeTextField.getText().length());
      applyDialogFont(ancestor);

      return ancestor;
    }

    @Override
    protected boolean isResizable() {
      return true;
    }

    @Override
    protected void okPressed() {
      IPreferenceStore store = SearchPlugin.getDefault().getPreferenceStore();
      store.setValue(SearchPreferencePage.LIMIT_HISTORY, fHistorySize);

      // establish history size
      for (int i = fCurrentList.size() - 1; i >= fHistorySize; i--) {
        fCurrentRemoves.add(fCurrentList.get(i));
        fCurrentList.remove(i);
      }
      super.okPressed();
    }

    protected final boolean validateDialogState() {
      IStatus status = null;
      try {
        String historySize = fHistorySizeTextField.getText();
        int size = Integer.parseInt(historySize);
        if (size < 1 || size >= 100) {
          status = new Status(IStatus.ERROR, SearchPlugin.getID(), IStatus.ERROR,
              SearchMessages.SearchHistorySelectionDialog_history_size_error, null);
        } else {
          fHistorySize = size;
        }
      } catch (NumberFormatException e) {
        status = new Status(IStatus.ERROR, SearchPlugin.getID(), IStatus.ERROR,
            SearchMessages.SearchHistorySelectionDialog_history_size_error, null);
      }
      if (status == null) {
        status = Status.OK_STATUS;
      }
      updateStatus(status);
      return !status.matches(IStatus.ERROR);
    }

  }
  private static final class SearchesLabelProvider extends LabelProvider {

    private ArrayList<Image> fImages = new ArrayList<Image>();

    @Override
    public void dispose() {
      Iterator<Image> iter = fImages.iterator();
      while (iter.hasNext()) {
        iter.next().dispose();
      }

      fImages = null;
    }

    @Override
    public Image getImage(Object element) {

      ImageDescriptor imageDescriptor = ((ISearchResult) element).getImageDescriptor();
      if (imageDescriptor == null) {
        return null;
      }

      Image image = imageDescriptor.createImage();
      fImages.add(image);

      return image;
    }

    @Override
    public String getText(Object element) {
      return ((ISearchResult) element).getLabel();
    }
  }

  private static final int REMOVE_ID = IDialogConstants.CLIENT_ID + 1;
  private static final int WIDTH_IN_CHARACTERS = 55;

  private List<ISearchResult> fInput;
  private final List<Object> fRemovedEntries;

  private TableViewer fViewer;
  private Button fRemoveButton;

  private boolean fIsOpenInNewView;

  private Link fLink;

  public SearchHistorySelectionDialog(Shell parent, List<ISearchResult> input) {
    super(parent);
    setTitle(SearchMessages.SearchesDialog_title);
    setMessage(SearchMessages.SearchesDialog_message);
    fInput = input;
    fRemovedEntries = new ArrayList<Object>();
    setHelpAvailable(false);
  }

  @Override
  public void create() {
    super.create();

    List<?> initialSelection = getInitialElementSelections();
    if (initialSelection != null) {
      fViewer.setSelection(new StructuredSelection(initialSelection));
    }

    validateDialogState();
  }

  /**
   * @return the isOpenInNewView
   */
  public boolean isOpenInNewView() {
    return fIsOpenInNewView;
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == REMOVE_ID) {
      IStructuredSelection selection = (IStructuredSelection) fViewer.getSelection();
      Iterator<?> searchResults = selection.iterator();
      while (searchResults.hasNext()) {
        Object curr = searchResults.next();
        fRemovedEntries.add(curr);
        fInput.remove(curr);
        fViewer.remove(curr);
      }
      if (fViewer.getSelection().isEmpty() && !fInput.isEmpty()) {
        fViewer.setSelection(new StructuredSelection(fInput.get(0)));
      }
      return;
    }
    if (buttonId == IDialogConstants.OPEN_ID) {
      fIsOpenInNewView = true;
      buttonId = IDialogConstants.OK_ID;
    }
    super.buttonPressed(buttonId);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OPEN_LABEL, true);
    createButton(parent, IDialogConstants.OPEN_ID,
        SearchMessages.SearchHistorySelectionDialog_open_in_new_button, false);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

  /*
   * Overrides method from Dialog
   */
  @Override
  protected Control createDialogArea(Composite container) {
    Composite ancestor = (Composite) super.createDialogArea(container);

    createMessageArea(ancestor);

    Composite parent = new Composite(ancestor, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    parent.setLayout(layout);
    parent.setLayoutData(new GridData(GridData.FILL_BOTH));

    fViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER
        | SWT.FULL_SELECTION);
    fViewer.setContentProvider(new ArrayContentProvider());

    final Table table = fViewer.getTable();
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        okPressed();
      }
    });
    fViewer.setLabelProvider(new SearchesLabelProvider());
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = convertHeightInCharsToPixels(15);
    gd.widthHint = convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);
    table.setLayoutData(gd);

    fRemoveButton = new Button(parent, SWT.PUSH);
    fRemoveButton.setText(SearchMessages.SearchesDialog_remove_label);
    fRemoveButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        buttonPressed(REMOVE_ID);
      }
    });
    fRemoveButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false));
    SWTUtil.setButtonDimensionHint(fRemoveButton);

    fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        validateDialogState();
      }
    });

    fLink = new Link(parent, SWT.NONE);
    configureHistoryLink();
    fLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        HistoryConfigurationDialog dialog = new HistoryConfigurationDialog(getShell(), fInput,
            fRemovedEntries);
        if (dialog.open() == Window.OK) {
          fViewer.refresh();
          configureHistoryLink();
        }
      }
    });
    fLink.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));

    applyDialogFont(ancestor);

    // set input & selections last, so all the widgets are created.
    fViewer.setInput(fInput);
    fViewer.getTable().setFocus();
    return ancestor;
  }

  /*
   * Overrides method from Dialog
   */
  @Override
  protected Label createMessageArea(Composite composite) {
    Composite parent = new Composite(composite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    parent.setLayout(layout);
    parent.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Label label = new Label(parent, SWT.WRAP);
    label.setText(getMessage());
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    //gd.widthHint= convertWidthInCharsToPixels(WIDTH_IN_CHARACTERS);
    label.setLayoutData(gd);

    applyDialogFont(label);
    return label;
  }

  @Override
  protected IDialogSettings getDialogBoundsSettings() {
    return SearchPlugin.getDefault().getDialogSettingsSection(
        "DialogBounds_SearchHistorySelectionDialog"); //$NON-NLS-1$
  }

  @Override
  protected int getDialogBoundsStrategy() {
    return DIALOG_PERSISTSIZE;
  }

  /*
   * Overrides method from Dialog
   */
  @Override
  protected void okPressed() {
    // Build a list of selected children.
    ISelection selection = fViewer.getSelection();
    if (selection instanceof IStructuredSelection) {
      setResult(((IStructuredSelection) fViewer.getSelection()).toList());
    }

    // remove queries
    for (Iterator<Object> iter = fRemovedEntries.iterator(); iter.hasNext();) {
      ISearchResult result = (ISearchResult) iter.next();
      ISearchQuery query = result.getQuery();
      if (query != null) { // must not be null: invalid implementation of a search query
        InternalSearchUI.getInstance().removeQuery(query);
      }
    }
    super.okPressed();
  }

  protected final void validateDialogState() {
    IStructuredSelection sel = (IStructuredSelection) fViewer.getSelection();
    int elementsSelected = sel.toList().size();

    fRemoveButton.setEnabled(elementsSelected > 0);
    Button okButton = getOkButton();
    if (okButton != null) {
      okButton.setEnabled(elementsSelected == 1);
    }
    Button openInNewButton = getButton(IDialogConstants.OPEN_ID);
    if (openInNewButton != null) {
      openInNewButton.setEnabled(elementsSelected == 1);
    }
  }

  private void configureHistoryLink() {
    int historyLimit = SearchPreferencePage.getHistoryLimit();
    fLink.setText(Messages.format(SearchMessages.SearchHistorySelectionDialog_configure_link_label,
        new Integer(historyLimit)));
  }
}
