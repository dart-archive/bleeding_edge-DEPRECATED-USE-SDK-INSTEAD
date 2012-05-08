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
package com.google.dart.tools.search2.internal.ui;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.internal.ui.util.SWTUtil;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.ISearchResult;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.dialogs.SelectionDialog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dialog that shows a list of items with icon and label.
 */
public class SearchHistorySelectionDialog extends SelectionDialog {

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
    super.buttonPressed(buttonId);
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OPEN_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
  }

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

    applyDialogFont(ancestor);

    // set input & selections last, so all the widgets are created.
    fViewer.setInput(fInput);
    fViewer.getTable().setFocus();
    return ancestor;
  }

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
}
