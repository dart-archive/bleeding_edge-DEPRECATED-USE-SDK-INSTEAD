/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.nsedit;

import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.eclipse.wst.xml.ui.internal.XMLUIMessages;

/*
 * This class is an extension of CommonEditNamespacesDialog. This class adds the target namespaces
 * dialog field.
 */
public class CommonEditNamespacesTargetFieldDialog extends CommonEditNamespacesDialog {

  class TargetNamespaceModifyListener implements ModifyListener {
    public void modifyText(ModifyEvent e) {
      String oldTargetNamespace = targetNamespace;
      targetNamespace = targetNamespaceField.getText();
      updateTargetNamespaceAndNamespaceInfo(oldTargetNamespace, targetNamespace);
    }
  }

  protected String targetNamespace;
  protected Text targetNamespaceField;

  public CommonEditNamespacesTargetFieldDialog(Composite parent, IPath resourceLocation1) {
    super(parent, resourceLocation1, XMLUIMessages._UI_NAMESPACE_DECLARATIONS);

    Composite targetComp = getTopComposite();
    targetComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    GridData gd = new GridData(GridData.FILL_BOTH);
    Label targetNamespaceLabel = new Label(targetComp, SWT.NONE);
    targetNamespaceLabel.setLayoutData(gd);
    targetNamespaceLabel.setText(XMLUIMessages._UI_TARGET_NAMESPACE);

    targetNamespaceField = new Text(targetComp, SWT.BORDER);
    targetNamespaceField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    targetNamespaceField.addModifyListener(new TargetNamespaceModifyListener());

    // createControlArea();
  }

  public String getTargetNamespace() {
    return targetNamespace;
  }

  public void performEdit() {
    Object selection = getSelection(tableViewer.getSelection());
    if (selection != null) {
      boolean editTargetNamespace = false;
      NamespaceInfo nsInfo = (NamespaceInfo) selection;
      if (getTargetNamespace().equals(nsInfo.uri)) {
        editTargetNamespace = true;
      }

      invokeDialog(XMLUIMessages._UI_LABEL_NEW_NAMESPACE_INFORMATION, nsInfo);
      updateErrorMessage(namespaceInfoList);
      performDelayedUpdate();

      if (editTargetNamespace) {
        targetNamespaceField.setText(nsInfo.uri);
      }
    }
  }

  public void setTargetNamespace(String theTargetNamespace) {
    targetNamespace = theTargetNamespace != null ? theTargetNamespace : ""; //$NON-NLS-1$
    targetNamespaceField.setText(targetNamespace);
    // updateTargetNamespaceAndNamespaceInfo(targetNamespace);
  }

  void updateTargetNamespaceAndNamespaceInfo(String oldTargetNamespace, String newTargetNamespace) {
    NamespaceInfo info = getNamespaceInfo(newTargetNamespace);
    if (info == null) {
      info = getNamespaceInfo(oldTargetNamespace);
      if (info == null) {
        info = new NamespaceInfo(newTargetNamespace, "tns", null); //$NON-NLS-1$
        namespaceInfoList.add(info);
      } else {
        info.uri = targetNamespace;
      }
    }
    tableViewer.refresh();
  }
}
