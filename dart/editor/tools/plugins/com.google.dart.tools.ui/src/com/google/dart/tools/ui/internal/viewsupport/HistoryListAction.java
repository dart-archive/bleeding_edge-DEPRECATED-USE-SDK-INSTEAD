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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.dialogs.fields.DialogField;
import com.google.dart.tools.ui.internal.dialogs.fields.IDialogFieldListener;
import com.google.dart.tools.ui.internal.dialogs.fields.IListAdapter;
import com.google.dart.tools.ui.internal.dialogs.fields.LayoutUtil;
import com.google.dart.tools.ui.internal.dialogs.fields.ListDialogField;
import com.google.dart.tools.ui.internal.dialogs.fields.Separator;
import com.google.dart.tools.ui.internal.dialogs.fields.StringDialogField;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/* package */class HistoryListAction extends Action {

  private class HistoryListDialog extends StatusDialog {
    private static final int MAX_MAX_ENTRIES = 100;
    private ListDialogField fHistoryList;
    private StringDialogField fMaxEntriesField;
    private int fMaxEntries;

    private Object fResult;

    private HistoryListDialog() {
      super(fHistory.getShell());
      setTitle(fHistory.getHistoryListDialogTitle());

      createHistoryList();
      createMaxEntriesField();
      setHelpAvailable(false);
    }

    /*
     * @see org.eclipse.jface.dialogs.StatusDialog#create()
     */
    @Override
    public void create() {
      setShellStyle(getShellStyle() | SWT.RESIZE);
      super.create();
    }

    public int getMaxEntries() {
      return fMaxEntries;
    }

    public List<Object> getRemaining() {
      return fHistoryList.getElements();
    }

    public Object getResult() {
      return fResult;
    }

    /*
     * @see Dialog#createDialogArea(Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
      initializeDialogUnits(parent);

      Composite composite = (Composite) super.createDialogArea(parent);

      Composite inner = new Composite(composite, SWT.NONE);
      inner.setLayoutData(new GridData(GridData.FILL_BOTH));
      inner.setFont(composite.getFont());

      LayoutUtil.doDefaultLayout(inner, new DialogField[] {fHistoryList, new Separator()}, true);
      LayoutUtil.setHeightHint(fHistoryList.getListControl(null), convertHeightInCharsToPixels(12));
      LayoutUtil.setHorizontalGrabbing(fHistoryList.getListControl(null));

      Composite additionalControls = new Composite(inner, SWT.NONE);
      additionalControls.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
      LayoutUtil.doDefaultLayout(additionalControls, new DialogField[] {fMaxEntriesField}, false);
      LayoutUtil.setHorizontalGrabbing(fMaxEntriesField.getTextControl(null));

      applyDialogFont(composite);
      return composite;
    }

    private void createHistoryList() {
      IListAdapter adapter = new IListAdapter() {
        @Override
        public void customButtonPressed(ListDialogField field, int index) {
          doCustomButtonPressed(index);
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
      String[] buttonLabels = new String[] {
          DartUIMessages.HistoryListAction_remove, DartUIMessages.HistoryListAction_remove_all};
      LabelProvider labelProvider = new TestRunLabelProvider();
      fHistoryList = new ListDialogField(adapter, buttonLabels, labelProvider);
      fHistoryList.setLabelText(fHistory.getHistoryListDialogMessage());

      List<Object> historyEntries = fHistory.getHistoryEntries();
      fHistoryList.setElements(historyEntries);

      Object currentEntry = fHistory.getCurrentEntry();
      ISelection sel;
      if (currentEntry != null) {
        sel = new StructuredSelection(currentEntry);
      } else {
        sel = new StructuredSelection();
      }
      fHistoryList.selectElements(sel);
    }

    private void createMaxEntriesField() {
      fMaxEntriesField = new StringDialogField();
      fMaxEntriesField.setLabelText(fHistory.getMaxEntriesMessage());
      fMaxEntriesField.setDialogFieldListener(new IDialogFieldListener() {
        @Override
        public void dialogFieldChanged(DialogField field) {
          String maxString = fMaxEntriesField.getText();
          boolean valid;
          try {
            fMaxEntries = Integer.parseInt(maxString);
            valid = fMaxEntries > 0 && fMaxEntries < MAX_MAX_ENTRIES;
          } catch (NumberFormatException e) {
            valid = false;
          }
          if (valid) {
            updateStatus(StatusInfo.OK_STATUS);
          } else {
            updateStatus(new StatusInfo(IStatus.ERROR, Messages.format(
                DartUIMessages.HistoryListAction_max_entries_constraint,
                Integer.toString(MAX_MAX_ENTRIES))));
          }
        }
      });
      fMaxEntriesField.setText(Integer.toString(fHistory.getMaxEntries()));
    }

    private void doCustomButtonPressed(int index) {
      switch (index) {
        case 0: // remove
          fHistoryList.removeElements(fHistoryList.getSelectedElements());
          fHistoryList.selectFirstElement();
          break;

        case 1: // remove all
          fHistoryList.removeAllElements();

          //$FALL-THROUGH$
        default:
          break;
      }
    }

    private void doDoubleClicked() {
      okPressed();
    }

    private void doSelectionChanged() {
      List<Object> selected = fHistoryList.getSelectedElements();
      if (selected.size() >= 1) {
        fResult = selected.get(0);
      } else {
        fResult = null;
      }
      fHistoryList.enableButton(0, selected.size() != 0);
    }

  }

  private final class TestRunLabelProvider extends LabelProvider {
    private final HashMap<ImageDescriptor, Image> fImages = new HashMap<ImageDescriptor, Image>();

    @Override
    public void dispose() {
      for (Iterator<Image> iter = fImages.values().iterator(); iter.hasNext();) {
        Image image = iter.next();
        image.dispose();
      }
      fImages.clear();
    }

    @Override
    public Image getImage(Object element) {
      ImageDescriptor imageDescriptor = fHistory.getImageDescriptor(element);
      return getCachedImage(imageDescriptor);
    }

    @Override
    public String getText(Object element) {
      return fHistory.getText(element);
    }

    private Image getCachedImage(ImageDescriptor imageDescriptor) {
      Object cached = fImages.get(imageDescriptor);
      if (cached != null) {
        return (Image) cached;
      }
      Image image = imageDescriptor.createImage(fHistory.getShell().getDisplay());
      fImages.put(imageDescriptor, image);
      return image;
    }
  }

  private final ViewHistory fHistory;

  public HistoryListAction(ViewHistory history) {
    super(null, IAction.AS_RADIO_BUTTON);
    fHistory = history;
    fHistory.configureHistoryListAction(this);
  }

  /*
   * @see IAction#run()
   */
  @Override
  public void run() {
    HistoryListDialog dialog = new HistoryListDialog();
    if (dialog.open() == Window.OK) {
      fHistory.setHistoryEntries(dialog.getRemaining(), dialog.getResult());
      fHistory.setMaxEntries(dialog.getMaxEntries());
    }
  }

}
