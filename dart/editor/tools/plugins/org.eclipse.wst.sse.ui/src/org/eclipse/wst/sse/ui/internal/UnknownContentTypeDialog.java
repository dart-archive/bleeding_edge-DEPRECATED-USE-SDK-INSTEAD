/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Message dialog informing user that an editor was open on unsupported content type
 */
public class UnknownContentTypeDialog extends MessageDialogWithToggle {

  public UnknownContentTypeDialog(Shell parent, IPreferenceStore store, String key) {
    // set message to null in super so that message does not appear twice
    super(parent, SSEUIMessages.UnknownContentTypeDialog_0, null, null, INFORMATION,
        new String[] {IDialogConstants.OK_LABEL}, 0, SSEUIMessages.UnknownContentTypeDialog_1,
        false);
    setPrefStore(store);
    setPrefKey(key);
  }

  protected void buttonPressed(int buttonId) {
    super.buttonPressed(buttonId);

    // overwritten so that value stored is boolean, not string
    if (buttonId != IDialogConstants.CANCEL_ID && getToggleState() && getPrefStore() != null
        && getPrefKey() != null) {
      switch (buttonId) {
        case IDialogConstants.YES_ID:
        case IDialogConstants.YES_TO_ALL_ID:
        case IDialogConstants.PROCEED_ID:
        case IDialogConstants.OK_ID:
          getPrefStore().setValue(getPrefKey(), false);
          break;
        case IDialogConstants.NO_ID:
        case IDialogConstants.NO_TO_ALL_ID:
          getPrefStore().setValue(getPrefKey(), true);
          break;
      }
    }

  }

  protected Control createMessageArea(Composite composite) {
    super.createMessageArea(composite);
    Link messageLink = new Link(composite, SWT.NONE);
    messageLink.setText(SSEUIMessages.UnknownContentTypeDialog_2);
    messageLink.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        linkClicked();
      }
    });
    return composite;
  }

  private void linkClicked() {
    String pageId = "org.eclipse.ui.preferencePages.ContentTypes"; //$NON-NLS-1$
    PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getShell(), pageId,
        new String[] {pageId}, null);
    dialog.open();
  }
}
