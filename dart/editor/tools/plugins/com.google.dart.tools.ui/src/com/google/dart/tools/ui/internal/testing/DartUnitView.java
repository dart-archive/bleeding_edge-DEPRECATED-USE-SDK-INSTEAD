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

package com.google.dart.tools.ui.internal.testing;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A view to display and run dart:unittest tests.
 */
public class DartUnitView extends ViewPart {
  private DartUnitViewPage defaultPage;
  private DartUnitViewPage visiblePage;

  private List<DartUnitViewPage> pages = new ArrayList<DartUnitViewPage>();

  private Composite parentComposite;

  IPartListener partListener = new IPartListener() {
    @Override
    public void partActivated(IWorkbenchPart part) {
      handleActivation(part);
    }

    @Override
    public void partBroughtToTop(IWorkbenchPart part) {
      handleActivation(part);
    }

    @Override
    public void partClosed(IWorkbenchPart part) {
      handleActivation(part.getSite().getPage().getActiveEditor());
    }

    @Override
    public void partDeactivated(IWorkbenchPart part) {

    }

    @Override
    public void partOpened(IWorkbenchPart part) {

    }
  };

  public DartUnitView() {

  }

  @Override
  public void createPartControl(Composite parent) {
    this.parentComposite = parent;

    parent.setLayout(new StackLayout());

    defaultPage = createDefaultPage();
    defaultPage.createControl(parent);

    showPage(defaultPage);
  }

  @Override
  public void dispose() {
    getSite().getPage().removePartListener(partListener);

    super.dispose();
  }

  @Override
  public void init(IViewSite site) throws PartInitException {
    super.init(site);

    site.getPage().addPartListener(partListener);
  }

  @Override
  public void setFocus() {
    if (visiblePage != null) {
      defaultPage.setFocus();
    }
  }

  protected DartUnitViewPage createDefaultPage() {
    return new DartUnitViewPage();
  }

  @Override
  protected void setContentDescription(String description) {
    super.setContentDescription(description);
  }

  private DartUnitViewPage createPageFor(IProject project) {
    DartUnitViewPage page = new DartUnitViewPage(project);

    page.createControl(parentComposite);

    pages.add(page);

    return page;
  }

  private void deletePage(DartUnitViewPage page) {
    if (page == visiblePage) {
      showPage(defaultPage);
    }

    pages.remove(page);
    page.dispose();
  }

  /**
   * Collect any pages that reference projects with no open editors.
   */
  private void gcPages() {
    Set<IProject> referencedProjects = new HashSet<IProject>();

    for (IEditorReference ref : getSite().getPage().getEditorReferences()) {
      IEditorPart editor = ref.getEditor(false);

      if (editor != null) {
        if (editor.getEditorInput() instanceof IFileEditorInput) {
          IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();

          referencedProjects.add(input.getFile().getProject());
        }
      }
    }

    // collect unreferenced pages
    for (DartUnitViewPage page : new ArrayList<DartUnitViewPage>(pages)) {
      if (page == visiblePage) {
        continue;
      }

      if (!referencedProjects.contains(page.getProject())) {
        deletePage(page);
      }
    }
  }

  private DartUnitViewPage getPageFor(IProject project) {
    for (DartUnitViewPage page : pages) {
      if (project.equals(page.getProject())) {
        return page;
      }
    }

    return null;
  }

  private void handleActivation(IWorkbenchPart part) {
    if (part instanceof IEditorPart) {
      IEditorPart editor = (IEditorPart) part;

      if (editor.getEditorInput() instanceof IFileEditorInput) {
        IFileEditorInput input = (IFileEditorInput) editor.getEditorInput();

        refocusContent(editor, input.getFile());
      }
    } else if (part == null) {
      showPage(defaultPage);
    }
  }

  private void refocusContent(IEditorPart editor, IFile file) {
    IProject project = file.getProject();

    swapPages(project);
  }

  private void showPage(DartUnitViewPage page) {
    if (visiblePage == page) {
      return;
    }

    if (page != defaultPage) {
      // Move the page to the end of the pages list.
      pages.remove(page);
      pages.add(page);
    }

    visiblePage = page;

    StackLayout layout = (StackLayout) parentComposite.getLayout();
    layout.topControl = visiblePage.getControl();

    parentComposite.layout();

    page.activated(this);

    gcPages();
  }

  private void swapPages(IProject project) {
    if (project == null) {
      showPage(defaultPage);
    } else if (getPageFor(project) != null) {
      showPage(getPageFor(project));
    } else {
      DartUnitViewPage page = createPageFor(project);

      showPage(page);
    }
  }

}
