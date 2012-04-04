package com.google.dart.tools.ui.internal.refactoring.reorg;

import com.google.dart.tools.internal.corext.refactoring.reorg.RenameSelectionState;
import com.google.dart.tools.internal.corext.refactoring.tagging.INameUpdating;
import com.google.dart.tools.ui.internal.refactoring.UserInterfaceStarter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.swt.widgets.Shell;

/**
 * @coverage dart.editor.ui.refactoring.ui
 */
public class RenameUserInterfaceStarter extends UserInterfaceStarter {

  @Override
  public boolean activate(Refactoring refactoring, Shell parent, int saveMode) throws CoreException {
    RenameProcessor processor = (RenameProcessor) refactoring.getAdapter(RenameProcessor.class);
    Object[] elements = processor.getElements();
    RenameSelectionState state = elements.length == 1 ? new RenameSelectionState(elements[0])
        : null;
    boolean executed = super.activate(refactoring, parent, saveMode);
    INameUpdating nameUpdating = (INameUpdating) refactoring.getAdapter(INameUpdating.class);
    if (executed && nameUpdating != null && state != null) {
      Object newElement = nameUpdating.getNewElement();
      if (newElement != null) {
        state.restore(newElement);
      }
    }
    return executed;
  }
}
