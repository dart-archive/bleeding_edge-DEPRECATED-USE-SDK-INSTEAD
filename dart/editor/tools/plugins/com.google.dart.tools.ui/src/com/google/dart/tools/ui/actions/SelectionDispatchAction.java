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
package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;

/**
 * Action that dispatches the <code>IAction#run()</code> and the
 * <code>ISelectionChangedListener#selectionChanged</code> according to the type of the selection.
 * <ul>
 * <li>if selection is of type <code>ITextSelection</code> then <code>run(ITextSelection)</code> and
 * <code>selectionChanged(ITextSelection)</code> is called.</li>
 * <li>if selection is of type <code>IStructuredSelection</code> then
 * <code>run(IStructuredSelection)</code> and <code>
 * 	selectionChanged(IStructuredSelection)</code> is called.</li>
 * <li>default is to call <code>run(ISelection)</code> and <code>
 * 	selectionChanged(ISelection)</code>.</li>
 * </ul>
 * <p>
 * Note: This class is not intended to be subclassed outside the JDT UI plug-in.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public abstract class SelectionDispatchAction extends AbstractInstrumentedAction implements
    ISelectionChangedListener {

  private final IWorkbenchSite fSite;
  private ISelectionProvider fSpecialSelectionProvider;

  /**
   * Creates a new action with no text and no image.
   * <p>
   * Configure the action later using the set methods.
   * </p>
   * 
   * @param site the site this action is working on
   */
  protected SelectionDispatchAction(IWorkbenchSite site) {
    Assert.isNotNull(site);
    fSite = site;
  }

  /**
   * Returns the selection provided by the site owning this action.
   * 
   * @return the site's selection
   */
  public ISelection getSelection() {
    ISelectionProvider selectionProvider = getSelectionProvider();
    if (selectionProvider != null) {
      return selectionProvider.getSelection();
    } else {
      return null;
    }
  }

  /**
   * Returns the selection provider managed by the site owning this action or the selection provider
   * explicitly set in {@link #setSpecialSelectionProvider(ISelectionProvider)}.
   * 
   * @return the site's selection provider
   */
  public ISelectionProvider getSelectionProvider() {
    if (fSpecialSelectionProvider != null) {
      return fSpecialSelectionProvider;
    }
    return fSite.getSelectionProvider();
  }

  /**
   * Returns the shell provided by the site owning this action.
   * 
   * @return the site's shell
   */
  public Shell getShell() {
    return fSite.getShell();
  }

  /**
   * Returns the site owning this action.
   * 
   * @return the site owning this action
   */
  public IWorkbenchSite getSite() {
    return fSite;
  }

  @Override
  public void run() {
    EmitInstrumentationCommand();
    dispatchRun(getSelection());
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param selection the selection
   */
  public void run(DartTextSelection selection) {
    EmitInstrumentationCommand();
    run((ITextSelection) selection);
  }

  /**
   * Executes this actions with the given selection. This default implementation does nothing.
   * 
   * @param selection the selection
   */
  public void run(ISelection selection) {
  }

  /**
   * Executes this actions with the given structured selection. This default implementation calls
   * <code>run(ISelection selection)</code>.
   * 
   * @param selection the selection
   */
  public void run(IStructuredSelection selection) {
    EmitInstrumentationCommand();
    run((ISelection) selection);
  }

  /**
   * Executes this actions with the given text selection. This default implementation calls
   * <code>run(ISelection selection)</code>.
   * 
   * @param selection the selection
   */
  public void run(ITextSelection selection) {
    EmitInstrumentationCommand();
    run((ISelection) selection);
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param selection the selection
   */
  public void selectionChanged(DartTextSelection selection) {
    selectionChanged((ITextSelection) selection);
  }

  /**
   * Notifies this action that the given selection has changed. This default implementation sets the
   * action's enablement state to <code>false</code>.
   * 
   * @param selection the new selection
   */
  public void selectionChanged(ISelection selection) {
    setEnabled(false);
  }

  /**
   * Notifies this action that the given structured selection has changed. This default
   * implementation calls <code>selectionChanged(ISelection selection)</code>.
   * 
   * @param selection the new selection
   */
  public void selectionChanged(IStructuredSelection selection) {
    selectionChanged((ISelection) selection);
  }

  /**
   * Notifies this action that the given text selection has changed. This default implementation
   * calls <code>selectionChanged(ISelection selection)</code>.
   * 
   * @param selection the new selection
   */
  public void selectionChanged(ITextSelection selection) {
    selectionChanged((ISelection) selection);
  }

  @Override
  public void selectionChanged(SelectionChangedEvent event) {
    dispatchSelectionChanged(event.getSelection());
  }

  /**
   * Sets a special selection provider which will be used instead of the site's selection provider.
   * This method should be used directly after constructing the action and before the action is
   * registered as a selection listener. The invocation will not a perform a selection change
   * notification.
   * 
   * @param provider a special selection provider which is used instead of the site's selection
   *          provider or <code>null</code> to use the site's selection provider. Clients can for
   *          example use a {@link ConvertingSelectionProvider} to first convert a selection before
   *          passing it to the action.
   */
  public void setSpecialSelectionProvider(ISelectionProvider provider) {
    fSpecialSelectionProvider = provider;
  }

  /**
   * Updates the action's enablement state according to the given selection. This default
   * implementation calls one of the <code>selectionChanged</code> methods depending on the type of
   * the passed selection.
   * 
   * @param selection the selection this action is working on
   */
  public void update(ISelection selection) {
    dispatchSelectionChanged(selection);
  }

  private void dispatchRun(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      run((IStructuredSelection) selection);
    } else if (selection instanceof DartTextSelection) {
      run((DartTextSelection) selection);
    } else if (selection instanceof ITextSelection) {
      run((ITextSelection) selection);
    } else {
      run(selection);
    }
  }

  private void dispatchSelectionChanged(ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      selectionChanged((IStructuredSelection) selection);
    } else if (selection instanceof DartTextSelection) {
      selectionChanged((DartTextSelection) selection);
    } else if (selection instanceof ITextSelection) {
      selectionChanged((ITextSelection) selection);
    } else {
      selectionChanged(selection);
    }
  }
}
