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
