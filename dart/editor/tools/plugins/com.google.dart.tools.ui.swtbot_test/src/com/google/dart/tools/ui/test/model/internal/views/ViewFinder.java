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
package com.google.dart.tools.ui.test.model.internal.views;

import com.google.dart.tools.ui.test.model.internal.util.StringComparator;
import com.google.dart.tools.ui.test.model.internal.workbench.WorkbenchFinder;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * A service for finding {@link IViewReference}s and corresponding SWT widgets.
 */
@SuppressWarnings("restriction")
public class ViewFinder extends WorkbenchFinder {

  //TODO(pquitslund): update all UI execs to use UIThreadRunnable

  public static interface IViewMatcher {
    String getLabel();

    boolean matches(IViewDescriptor view);

    boolean matches(IViewPart part);

    boolean matches(IViewReference view);
  }

  private static final class IdMatcher implements IViewMatcher {
    private final String id;

    IdMatcher(String name) {
      this.id = name;
    }

    @Override
    public String getLabel() {
      return id;
    }

    @Override
    public boolean matches(IViewDescriptor view) {
      return StringComparator.matches(view.getId(), id);
    }

    @Override
    public boolean matches(IViewPart part) {
      IViewSite viewSite = part.getViewSite();
      if (viewSite == null) {
        return false;
      }
      return StringComparator.matches(viewSite.getId(), id);
    }

    @Override
    public boolean matches(IViewReference view) {
      return StringComparator.matches(view.getId(), id);
    }
  }

  private static final class NameMatcher implements IViewMatcher {
    private final String name;

    NameMatcher(String name) {
      this.name = name;
    }

    @Override
    public String getLabel() {
      return name;
    }

    @Override
    public boolean matches(IViewDescriptor view) {
      return StringComparator.matches(view.getLabel(), name);
    }

    @Override
    public boolean matches(IViewPart part) {
      return StringComparator.matches(part.getTitle(), name);
    }

    @Override
    public boolean matches(IViewReference view) {
      return StringComparator.matches(view.getPartName(), name);
    }

  }

  //note: finds first match
  public static IViewReference findMatch(IViewMatcher matcher) {
    IWorkbenchPage page = getActivePage();
    if (page == null) {
      return null;
    }
    IViewReference[] open = page.getViewReferences();
    for (int i = 0; i < open.length; i++) {
      if (matcher.matches(open[i])) {
        return open[i];
      }
    }
    return null;
  }

  public static IViewReference findNamed(String viewName) {
    return findMatch(new NameMatcher(viewName));
  }

  public static IViewReference findWithId(String viewId) {
    return findMatch(new IdMatcher(viewId));
  }

  ///////////////////////////////////////////////////////////////////////////////////
  //
  // Public utility methods.
  //
  ///////////////////////////////////////////////////////////////////////////////////

  /**
   * Get the current active viewpart without retrying.
   */
  public static IViewPart getActiveViewPartNoRetries() {
    if (!Platform.isRunning()) {
      return null;
    }
    final IWorkbenchPart[] part = new IViewPart[1];
    try {
      final Display display = PlatformUI.getWorkbench().getDisplay();
      display.syncExec(new Runnable() {
        @Override
        public void run() {
          part[0] = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
        }
      });
    } catch (Exception e) {
      /*
       * trap and ignore --- this might mean that the workbench is not up yet and we want to wait and try again
       */
    }
    if (part[0] instanceof IViewPart) {
      return (IViewPart) part[0];
    }
    return null;
  }

  /**
   * Get the control associated with the view registered with the platform at the given id.
   * 
   * @param viewId the platform-registered view identifier
   * @return the associated <code>Control</code> or <code>null</code> if there is either no
   *         associated view or there is no control associated with the view.
   */
  public static Control getViewControl(String viewId) {
    IViewPart viewPart = getViewPart(viewId);
    return getControl(viewPart);
  }

  public static Control getViewControlForName(String viewName) {
    IViewReference ref = findNamed(viewName);
    if (ref == null) {
      return null;
    }
    IViewPart part = (IViewPart) ref.getPart(false);
    return getControl(part);
  }

  /**
   * Get the viewpart registered to the platform with the given id.
   * 
   * @param id the id of the viewpart
   * @return the associated viewpart or null if there is none
   */
  public static IViewPart getViewPart(final String id) {
    final IViewPart[] part = new IViewPart[1];
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        //be safe here since the workbench might be disposed (or not active)
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
          return;
        }
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null) {
          return;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
          return;
        }

        IViewReference[] viewReferences = page.getViewReferences();
        for (int i = 0; i < viewReferences.length; i++) {
          IViewReference ref = viewReferences[i];
          if (ref.getId().equals(id)) {
            part[0] = ref.getView(false); //don't attempt to restore -- OR?
            return;
          }
        }
      }
    });
    return part[0];
  }

  public static IViewReference getViewRef(final IViewDescriptor descriptor) {
    final IViewReference[] refs = new IViewReference[1];
    final Display display = PlatformUI.getWorkbench().getDisplay();
    display.syncExec(new Runnable() {
      @Override
      public void run() {
        //be safe here since the workbench might be disposed (or not active)
        IWorkbench workbench = PlatformUI.getWorkbench();
        if (workbench == null) {
          return;
        }
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        if (window == null) {
          return;
        }
        IWorkbenchPage page = window.getActivePage();
        if (page == null) {
          return;
        }

        IViewReference[] viewReferences = page.getViewReferences();
        for (int i = 0; i < viewReferences.length; i++) {
          IViewReference ref = viewReferences[i];
          if (ref.getId().equals(descriptor.getId())) {
            refs[0] = ref;
            return;
          }
        }
      }
    });
    return refs[0];
  }

  public static IViewMatcher idMatcher(String id) {
    return new IdMatcher(id);
  }

  public static boolean isViewWithIdActive(String viewId) {
    IViewPart part = getActiveViewPartNoRetries();
    if (part == null) {
      return false;
    }
    IViewSite viewSite = part.getViewSite();
    if (viewSite == null) {
      return false;
    }
    String id = viewSite.getId();
    //System.out.println("active view: " + id);
    if (id == null) {
      return false;
    }
    return id.equals(viewId);
  }

  public static boolean isViewWithIdDirty(String viewId) {
    IViewReference view = findWithId(viewId);
    if (view == null) {
      return false;
    }
    return view.isDirty();
  }

  public static IViewMatcher nameMatcher(String name) {
    return new NameMatcher(name);
  }

  private static Control getControl(IViewPart viewPart) {
    if (viewPart == null) {
      return null;
    }
    IViewSite viewSite = viewPart.getViewSite();
    if (viewSite instanceof PartSite) {
      PartSite partSite = (PartSite) viewSite;
      return partSite.getPane().getControl();
    }
    return null;
  }

}
