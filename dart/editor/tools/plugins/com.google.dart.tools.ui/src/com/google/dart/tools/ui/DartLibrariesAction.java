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
package com.google.dart.tools.ui;

import com.google.dart.tools.core.model.DartProject;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.dialogs.PreferencesUtil;

import java.util.Hashtable;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class DartLibrariesAction implements IObjectActionDelegate {

  private IWorkbenchSite fSite;
  protected DartProject project;
  protected static final Hashtable<String, String> PROPS_TO_IDS = new Hashtable<String, String>();

  {
    DartX.todo();
//    PROPS_TO_IDS.put(
//        "org.eclipse.wst.jsdt.internal.ui.configure.scope", BuildPathsPropertyPage.PROP_ID); //$NON-NLS-1$
//    PROPS_TO_IDS.put(
//        "org.eclipse.wst.jsdt.internal.ui.configure.javascript.properties", CodeStylePreferencePage.PROP_ID); //$NON-NLS-1$
//    PROPS_TO_IDS.put(
//        "org.eclipse.wst.jsdt.internal.ui.configure.source.folders", BuildPathsPropertyPage.PROP_ID); //$NON-NLS-1$
    // PROPS_TO_IDS.put("", BuildPathsPropertyPage.PROP_ID);
    // PROPS_TO_IDS.put("", BuildPathsPropertyPage.PROP_ID);

  }

  @Override
  public void run(IAction arg0) {
    Object data = null;
    String ID = arg0.getId();
    String propertyPage = PROPS_TO_IDS.get(ID);

    PreferencesUtil.createPropertyDialogOn(getShell(), project, propertyPage, null, data).open();

  }

  @Override
  public void selectionChanged(IAction arg0, ISelection arg1) {
    DartX.todo();
//    if (arg1 instanceof IStructuredSelection) {
//      IStructuredSelection selection = (IStructuredSelection) arg1;
//      Object item = selection.getFirstElement();
//      if (item instanceof NamespaceGroup) {
//        item = ((NamespaceGroup) item).getPackageFragmentRoot();
//      }
//      if (item instanceof ProjectLibraryRoot) {
//        ProjectLibraryRoot root = ((ProjectLibraryRoot) item);
//        project = root.getProject();
//      }
//    }
  }

  @Override
  public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
    fSite = arg1.getSite();

  }

  protected Shell getShell() {
    if (fSite == null) {
      return DartToolsPlugin.getActiveWorkbenchShell();
    }

    return fSite.getShell() != null ? fSite.getShell() : DartToolsPlugin.getActiveWorkbenchShell();
  }

}
