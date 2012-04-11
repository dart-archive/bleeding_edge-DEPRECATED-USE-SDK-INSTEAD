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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartElementLabelProvider;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.dialogs.fields.DialogField;
import com.google.dart.tools.ui.internal.dialogs.fields.IListAdapter;
import com.google.dart.tools.ui.internal.dialogs.fields.LayoutUtil;
import com.google.dart.tools.ui.internal.dialogs.fields.ListDialogField;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.viewsupport.ColoringLabelProvider;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HistoryListAction extends Action {

  private class HistoryListDialog extends StatusDialog {

    private ListDialogField historyList;
    private IStatus historyStatus;
    private DartElement[] result;

    private HistoryListDialog(Shell shell, DartElement[][] elements) {
      super(shell);
      setTitle(CallHierarchyMessages.HistoryListDialog_title);

      String[] buttonLabels = new String[] {CallHierarchyMessages.HistoryListDialog_remove_button,};

      IListAdapter adapter = new IListAdapter() {
        @Override
        public void customButtonPressed(ListDialogField field, int index) {
          doCustomButtonPressed();
        }

        @Override
        public void doubleClicked(ListDialogField field) {
          doDoubleClicked();
        }

        @Override
        public void selectionChanged(ListDialogField field) {
          doSelectionChanged();
        }
      };

      DartElementLabelProvider labelProvider = new DartElementLabelProvider(
          DartElementLabelProvider.SHOW_QUALIFIED | DartElementLabelProvider.SHOW_ROOT) {

        @Override
        public Image getImage(Object element) {
          DartElement[] members = (DartElement[]) element;
          return super.getImage(members[0]);
        }

        @Override
        public StyledString getStyledText(Object element) {
          DartElement[] members = (DartElement[]) element;
          return new StyledString(HistoryAction.getElementLabel(members));
        }

        @Override
        public String getText(Object element) {
          DartElement[] members = (DartElement[]) element;
          return HistoryAction.getElementLabel(members);
        }
      };

      historyList = new ListDialogField(adapter, buttonLabels, new ColoringLabelProvider(
          labelProvider));
      historyList.setLabelText(CallHierarchyMessages.HistoryListDialog_label);
      Collection<Object> col = new ArrayList<Object>();
      for (Object each : elements) {
        col.add(each);
      }
      historyList.setElements(col);

      ISelection sel;
      if (elements.length > 0) {
        sel = new StructuredSelection(elements[0]);
      } else {
        sel = new StructuredSelection();
      }

      historyList.selectElements(sel);
    }

    public DartElement[][] getRemaining() {
      List<Object> elems = historyList.getElements();
      return elems.toArray(new DartElement[elems.size()][]);
    }

    public DartElement[] getResult() {
      return result;
    }

    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      PlatformUI.getWorkbench().getHelpSystem().setHelp(newShell,
          DartHelpContextIds.HISTORY_LIST_DIALOG);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      initializeDialogUnits(parent);

      Composite composite = (Composite) super.createDialogArea(parent);

      Composite inner = new Composite(composite, SWT.NONE);
      inner.setLayoutData(new GridData(GridData.FILL_BOTH));
      inner.setFont(composite.getFont());

      LayoutUtil.doDefaultLayout(inner, new DialogField[] {historyList}, true, 0, 0);
      LayoutUtil.setHeightHint(historyList.getListControl(null), convertHeightInCharsToPixels(12));
      LayoutUtil.setHorizontalGrabbing(historyList.getListControl(null));

      applyDialogFont(composite);
      return composite;
    }

    @Override
    protected boolean isResizable() {
      return true;
    }

    /**
     * Method doCustomButtonPressed.
     */
    private void doCustomButtonPressed() {
      historyList.removeElements(historyList.getSelectedElements());
    }

    private void doDoubleClicked() {
      if (historyStatus.isOK()) {
        okPressed();
      }
    }

    private void doSelectionChanged() {
      StatusInfo status = new StatusInfo();
      List<Object> selected = historyList.getSelectedElements();
      if (selected.size() != 1) {
        status.setError(""); //$NON-NLS-1$
        result = null;
      } else {
        result = (DartElement[]) selected.get(0);
      }
      historyList.enableButton(0, historyList.getSize() > selected.size() && selected.size() != 0);
      historyStatus = status;
      updateStatus(status);
    }

  }

  private CallHierarchyViewPart fView;

  public HistoryListAction(CallHierarchyViewPart view) {
    fView = view;
    setText(CallHierarchyMessages.HistoryListAction_label);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this, DartHelpContextIds.HISTORY_LIST_ACTION);
  }

  @Override
  public void run() {
    DartElement[][] historyEntries = fView.getHistoryEntries();
    HistoryListDialog dialog = new HistoryListDialog(DartToolsPlugin.getActiveWorkbenchShell(),
        historyEntries);
    if (dialog.open() == Window.OK) {
      fView.setHistoryEntries(dialog.getRemaining());
      fView.setInputElements(dialog.getResult());
    }
  }

}
