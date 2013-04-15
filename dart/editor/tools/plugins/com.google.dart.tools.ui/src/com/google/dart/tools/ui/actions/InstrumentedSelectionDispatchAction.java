package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.text.editor.DartTextSelection;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Action that dispatches the <code>IAction#run()</code> and the
 * <code>ISelectionChangedListener#selectionChanged</code> according to the type of the selection.
 * <ul>
 * <li>if selection is of type <code>ITextSelection</code> then <code>run(ITextSelection)</code> and
 * <code>selectionChanged(ITextSelection)</code> is called.</li>
 * <li>if selection is of type <code>IStructuredSelection</code> then
 * <code>run(IStructuredSelection)</code> and <code>
 *  selectionChanged(IStructuredSelection)</code> is called.</li>
 * <li>default is to call <code>run(ISelection)</code> and <code>
 *  selectionChanged(ISelection)</code>.</li>
 * </ul>
 * <p>
 * Note: This class is not intended to be subclassed outside the JDT UI plug-in.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public abstract class InstrumentedSelectionDispatchAction extends InstrumentedAction implements
    ISelectionChangedListener {

  private IWorkbenchSite fSite;
  private ISelectionProvider fSpecialSelectionProvider;
  private IWorkbenchWindow window;

  /**
   * Creates a new action with no text and no image.
   * <p>
   * Configure the action later using the set methods.
   * </p>
   * 
   * @param site the site this action is working on
   */
  protected InstrumentedSelectionDispatchAction(IWorkbenchSite site) {
    Assert.isNotNull(site);
    fSite = site;
  }

  protected InstrumentedSelectionDispatchAction(IWorkbenchWindow window) {
    Assert.isNotNull(window);
    this.window = window;
  }

  /**
   * Executes this actions with the given selection. This default implementation does nothing.
   * 
   * @param selection the selection
   */
  public void doRun(ISelection selection, Event event, UIInstrumentationBuilder instrumentation) {
    if (selection instanceof DartSelection) {
      instrumentation.record((DartSelection) selection);
      doRun((DartSelection) selection, event, instrumentation);
    } else if (selection instanceof DartTextSelection) {
      instrumentation.record((DartTextSelection) selection);
      doRun((DartTextSelection) selection, event, instrumentation);
    } else if (selection instanceof IStructuredSelection) {
      instrumentation.record((IStructuredSelection) selection);
      doRun((IStructuredSelection) selection, event, instrumentation);
    } else if (selection instanceof ITextSelection) {
      instrumentation.record((ITextSelection) selection);
      doRun((ITextSelection) selection, event, instrumentation);
    } else {
      instrumentation.record(selection);
      doRun(selection, event, instrumentation);
    }
  }

  /**
   * Returns the selection provided by the site owning this action.
   * 
   * @return the site's selection
   */
  @Override
  public ISelection getSelection() {
    ISelectionProvider selectionProvider = getSelectionProvider();
    if (selectionProvider != null) {
      ISelection selection = selectionProvider.getSelection();
      if (selection.isEmpty()) {
        IEditorInput input = getSite().getPage().getActiveEditor().getEditorInput();
        if (input instanceof IFileEditorInput) {
          return new StructuredSelection(((IFileEditorInput) input).getFile());
        }
      }
      return selection;
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
    return getSite().getSelectionProvider();
  }

  /**
   * Returns the shell provided by the site owning this action.
   * 
   * @return the site's shell
   */
  public Shell getShell() {
    return getSite().getShell();
  }

  /**
   * Returns the site owning this action.
   * 
   * @return the site owning this action
   */
  public IWorkbenchSite getSite() {
    if (fSite == null) {
      fSite = window.getActivePage().getActivePart().getSite();
    }
    return fSite;
  }

  public void run(DartTextSelection selection) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
    try {

      if (selection != null) {
        instrumentation.record(selection);
      }

      doRun(selection, null, instrumentation);
      instrumentation.metric("Run", "Completed");

    } catch (RuntimeException e) {
      instrumentation.record(e);
      throw e;
    }

    finally {
      instrumentation.log();
    }
  }

  public void run(IStructuredSelection selection) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
    try {

      if (selection != null) {
        instrumentation.record(selection);
      }

      doRun(selection, null, instrumentation);
      instrumentation.metric("Run", "Completed");

    } catch (RuntimeException e) {
      instrumentation.metric("Exception", e.getClass().toString());
      instrumentation.data("Exception", e.toString());
      throw e;
    }

    finally {
      instrumentation.log();
    }
  }

  /**
   * Note: This method is for internal use only. Clients should not call this method.
   * 
   * @param selection the selection
   */
  public void selectionChanged(DartSelection selection) {
    selectionChanged((ITextSelection) selection);
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
    if (event != null) {
      dispatchSelectionChanged(event.getSelection());
    }
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

  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    doRun((ITextSelection) selection, event, instrumentation);
  }

  protected void doRun(DartTextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    doRun((ITextSelection) selection, event, instrumentation);
  }

  @Override
  protected void doRun(Event event, UIInstrumentationBuilder instrumentation) {
    ISelection selection = getSelection();
    doRun(selection, event, instrumentation);
  }

  /**
   * Executes this actions with the given structured selection. This default implementation calls
   * <code>run(ISelection selection)</code>.
   * 
   * @param selection the selection
   */
  protected void doRun(IStructuredSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    doRun((ISelection) selection, event, instrumentation);
  }

  /**
   * Executes this actions with the given text selection. This default implementation calls
   * <code>run(ISelection selection)</code>.
   * 
   * @param selection the selection
   */
  protected void doRun(ITextSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    doRun((ISelection) selection, event, instrumentation);
  }

  private void dispatchSelectionChanged(ISelection selection) {
    if (selection instanceof DartSelection) {
      selectionChanged((DartSelection) selection);
    } else if (selection instanceof DartTextSelection) {
      selectionChanged((DartTextSelection) selection);
    } else if (selection instanceof IStructuredSelection) {
      selectionChanged((IStructuredSelection) selection);
    } else if (selection instanceof ITextSelection) {
      selectionChanged((ITextSelection) selection);
    } else {
      selectionChanged(selection);
    }
  }

}
