package com.google.dart.tools.ui.actions;

import com.google.dart.tools.ui.instrumentation.UIInstrumentation;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;

/**
 * Superclass to support adding instrumentation to actions. All actions should subclass this and
 * override doRun with the functionality of the action and making appropriate calls to the
 * instrumentation argument. getSelection should be override if the action depends on a context
 * 
 * @see InstrumentedSelectionDispatchAction for alternative signatures that include a selection
 */
public abstract class InstrumentedAction extends Action {

  public InstrumentedAction() {
  }

  public InstrumentedAction(String text) {
    super(text);
  }

  public InstrumentedAction(String text, ImageDescriptor image) {
    super(text, image);
  }

  public InstrumentedAction(String text, int style) {
    super(text, style);
  }

  @Override
  public final void run() {
    runWithEvent(null);
  }

  @Override
  public final void runWithEvent(Event event) {
    UIInstrumentationBuilder instrumentation = UIInstrumentation.builder(this.getClass());
    try {
      instrumentation.record(getSelection());

      doRun(event, instrumentation);

      instrumentation.metric("Run", "Completed");
    } catch (RuntimeException e) {
      instrumentation.record(e);
      throw e;
    } finally {
      instrumentation.log();
    }
  }

  /**
   * Implementing classes should override this method to perform the action for the class
   * 
   * @param event The event passed with the event, may be null
   * @param instrumentation The instrumentation logger, will not be null
   */
  protected abstract void doRun(Event event, UIInstrumentationBuilder instrumentation);

  /**
   * Get the current selection. If there isn't one, returns null. The default implementation will
   * return null. Actions that are performed on a selection should subclass this.
   */
  protected ISelection getSelection() {
    return null;
  }

}
