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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.core.internal.model.ExternalCompilationUnitImpl;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.ProblemsLabelDecorator;
import com.google.dart.tools.ui.internal.viewsupport.DartElementImageProvider;
import com.google.dart.tools.ui.internal.viewsupport.DartUILabelProvider;
import com.google.dart.tools.ui.internal.viewsupport.IProblemChangedListener;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;

/**
 * The <code>DartEditorErrorTickUpdater</code> will register as a IProblemChangedListener to listen
 * on problem changes of the editor's input. It updates the title images when the annotation model
 * changed.
 */
public class DartEditorErrorTickUpdater implements IProblemChangedListener {

  private DartEditor fJavaEditor;
  private DartUILabelProvider fLabelProvider;

  public DartEditorErrorTickUpdater(DartEditor editor) {
    Assert.isNotNull(editor);
    fJavaEditor = editor;
    fLabelProvider = new DartUILabelProvider(0, DartElementImageProvider.SMALL_ICONS);
    fLabelProvider.addLabelDecorator(new ProblemsLabelDecorator(null));
    DartToolsPlugin.getDefault().getProblemMarkerManager().addListener(this);
  }

  public void dispose() {
    fLabelProvider.dispose();
    DartToolsPlugin.getDefault().getProblemMarkerManager().removeListener(this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see IProblemChangedListener#problemsChanged(IResource[], boolean)
   */
  @Override
  public void problemsChanged(IResource[] changedResources, boolean isMarkerChange) {
    if (!isMarkerChange) {
      return;
    }

    IEditorInput input = fJavaEditor.getEditorInput();
    if (input != null) { // might run async, tests needed
      DartElement jelement = (DartElement) input.getAdapter(DartElement.class);
      if (jelement != null) {
        IResource resource = jelement.getResource();
        for (int i = 0; i < changedResources.length; i++) {
          if (changedResources[i].equals(resource)) {
            updateEditorImage(jelement);
          }
        }
      }
    }
  }

  public void updateEditorImage(Object jelement) {
    Image titleImage = fJavaEditor.getTitleImage();
    if (titleImage == null) {
      return;
    }
    Image newImage;
    DartX.todo();
    if (jelement instanceof CompilationUnit /*
                                             * && !jelement.getDartProject().
                                             * isOnIncludepath(jelement)
                                             */) {
      if (jelement instanceof ExternalCompilationUnitImpl) {
        newImage = fLabelProvider.getImage(jelement);
      } else {
        newImage = fLabelProvider.getImage(((CompilationUnit) jelement).getResource());
      }
    } else {
      newImage = fLabelProvider.getImage(jelement);
    }
    if (titleImage != newImage) {
      postImageChange(newImage);
    }
  }

  private void postImageChange(final Image newImage) {
    Shell shell = fJavaEditor.getEditorSite().getShell();
    if (shell != null && !shell.isDisposed()) {
      shell.getDisplay().syncExec(new Runnable() {
        @Override
        public void run() {
          fJavaEditor.updatedTitleImage(newImage);
        }
      });
    }
  }

}
