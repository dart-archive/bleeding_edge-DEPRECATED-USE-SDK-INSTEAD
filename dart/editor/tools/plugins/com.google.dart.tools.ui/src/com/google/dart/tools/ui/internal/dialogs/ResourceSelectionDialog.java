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
package com.google.dart.tools.ui.internal.dialogs;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * ***Copied from org.eclipse.ui.dialogs.ResourceSelectionDialog*** *Added Two fields:
 * desiredExtensions, and isDerivedDesired and their setter methods. Also changed the ITreeProvider
 * implementation to reflect changes in those two variables A standard resource selection dialog
 * which solicits a list of resources from the user. The <code>getResult</code> method returns the
 * selected resources.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 * <p>
 * Example:
 * 
 * <pre>
 *  ResourceSelectionDialog dialog =
 *    new ResourceSelectionDialog(getShell(), rootResource, msg);
 *  dialog.setInitialSelections(selectedResources);
 *  dialog.open();
 *  return dialog.getResult();
 * </pre>
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 */
@SuppressWarnings("restriction")
public class ResourceSelectionDialog extends SelectionDialog {
  // the root element to populate the viewer with
  private final IAdaptable root;

  // the visual selection widget group
  private CheckboxTreeAndListGroup selectionGroup;

  private List<String> desiredExtensions;

  private boolean isDerivedIsDesired;

  // constants
  private final static int SIZING_SELECTION_WIDGET_WIDTH = 400;

  private final static int SIZING_SELECTION_WIDGET_HEIGHT = 300;

  protected static final Object DESIRED_FILE_EXTENSION = "dart";

  /**
   * Creates a resource selection dialog rooted at the given element.
   * 
   * @param parentShell the parent shell
   * @param rootElement the root element to populate this dialog with
   * @param message the message to be displayed at the top of this dialog, or <code>null</code> to
   *          display a default message
   */
  public ResourceSelectionDialog(Shell parentShell, IAdaptable rootElement, String message) {
    super(parentShell);
    setTitle("Resource Selection");
    root = rootElement;
    if (message != null) {
      setMessage(message);
    } else {
      setMessage("Select the resources:");
    }
    setShellStyle(getShellStyle() | SWT.SHEET);
  }

  /**
   * @param event the event
   */
  public void checkStateChanged(CheckStateChangedEvent event) {
    getOkButton().setEnabled(selectionGroup.getCheckedElementCount() > 0);
  }

  @Override
  public void create() {
    super.create();
    initializeDialog();
  }

  public void setDerivedIsDesired(boolean isDerivedIsDesired) {
    this.isDerivedIsDesired = isDerivedIsDesired;
  }

  public void setDesiredExtensions(String[] extensions) {
    LinkedList<String> desiredExtensions = new LinkedList<String>();
    for (String s : extensions) {
      desiredExtensions.add(s);
    }
    this.desiredExtensions = desiredExtensions;
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(shell,
        IIDEHelpContextIds.RESOURCE_SELECTION_DIALOG);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    // page group
    Composite composite = (Composite) super.createDialogArea(parent);

    //create the input element, which has the root resource
    //as its only child
    ArrayList<IAdaptable> input = new ArrayList<IAdaptable>();
    input.add(root);

    createMessageArea(composite);
    selectionGroup = new CheckboxTreeAndListGroup(composite, input,
        getResourceProvider(IResource.FOLDER | IResource.PROJECT | IResource.ROOT),
        WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(),
        getResourceProvider(IResource.FILE),
        WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider(), SWT.NONE,
        // since this page has no other significantly-sized
        // widgets we need to hardcode the combined widget's
        // size, otherwise it will open too small
        SIZING_SELECTION_WIDGET_WIDTH, SIZING_SELECTION_WIDGET_HEIGHT);

    composite.addControlListener(new ControlListener() {
      @Override
      public void controlMoved(ControlEvent e) {
      }

      @Override
      public void controlResized(ControlEvent e) {
        //Also try and reset the size of the columns as appropriate
        TableColumn[] columns = selectionGroup.getListTable().getColumns();
        for (int i = 0; i < columns.length; i++) {
          columns[i].pack();
        }
      }
    });

    return composite;
  }

  /**
   * The <code>ResourceSelectionDialog</code> implementation of this <code>Dialog</code> method
   * builds a list of the selected resources for later retrieval by the client and closes this
   * dialog.
   */
  @Override
  protected void okPressed() {
    Iterator<?> resultEnum = selectionGroup.getAllCheckedListItems();
    ArrayList<Object> list = new ArrayList<Object>();
    while (resultEnum.hasNext()) {
      list.add(resultEnum.next());
    }
    setResult(list);
    super.okPressed();
  }

  /**
   * Visually checks the previously-specified elements in the container (left) portion of this
   * dialog's resource selection viewer.
   */
  private void checkInitialSelections() {
    Iterator<?> itemsToCheck = getInitialElementSelections().iterator();

    while (itemsToCheck.hasNext()) {
      IResource currentElement = (IResource) itemsToCheck.next();

      if (currentElement.getType() == IResource.FILE) {
        selectionGroup.initialCheckListItem(currentElement);
      } else {
        selectionGroup.initialCheckTreeItem(currentElement);
      }
    }
  }

  /**
   * Returns a content provider for <code>IResource</code>s that returns only children of the given
   * resource type.
   */
  private ITreeContentProvider getResourceProvider(final int resourceType) {
    return new WorkbenchContentProvider() {

      @Override
      public Object[] getChildren(Object o) {
        if (o instanceof IContainer) {
          IResource[] members = null;
          try {
            members = ((IContainer) o).members();
          } catch (CoreException e) {
            //just return an empty set of children
            return new Object[0];
          }

          //filter out the desired resource types
          ArrayList<IResource> results = new ArrayList<IResource>();
          for (int i = 0; i < members.length; i++) {
            //And the test bits with the resource types to see if they are what we want
            if ((members[i].getType() & resourceType) > 0) {
              if (members[i].isDerived() == isDerivedIsDesired) {
                if (desiredExtensions.size() == 0) {
                  results.add(members[i]);
                } else if (members[i].getType() != IResource.FILE
                    || desiredExtensions.contains(members[i].getFileExtension())) {
                  results.add(members[i]);
                }
              }
            }
          }
          return results.toArray();
        }
        //input element case
        if (o instanceof ArrayList) {
          return ((ArrayList<?>) o).toArray();
        }
        return new Object[0];
      }
    };
  }

  /**
   * Initializes this dialog's controls.
   */
  private void initializeDialog() {
    selectionGroup.addCheckStateListener(new ICheckStateListener() {
      @Override
      public void checkStateChanged(CheckStateChangedEvent event) {
        getOkButton().setEnabled(selectionGroup.getCheckedElementCount() > 0);
      }
    });

    if (getInitialElementSelections().isEmpty()) {
      getOkButton().setEnabled(false);
    } else {
      checkInitialSelections();
    }
  }
}
