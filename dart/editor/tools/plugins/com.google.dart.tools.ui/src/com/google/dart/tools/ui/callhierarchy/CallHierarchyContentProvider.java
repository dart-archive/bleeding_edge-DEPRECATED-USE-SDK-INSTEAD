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
package com.google.dart.tools.ui.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.model.TypeMember;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.callhierarchy.CallerMethodWrapper;
import com.google.dart.tools.ui.internal.callhierarchy.MethodCall;
import com.google.dart.tools.ui.internal.callhierarchy.MethodWrapper;
import com.google.dart.tools.ui.internal.callhierarchy.RealCallers;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.internal.util.ExceptionHandler;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.DeferredTreeContentManager;

import java.lang.reflect.InvocationTargetException;

public class CallHierarchyContentProvider implements ITreeContentProvider {

  private class MethodWrapperRunnable implements IRunnableWithProgress {
    private MethodWrapper methodWrapper;
    private MethodWrapper[] calls = null;

    MethodWrapperRunnable(MethodWrapper methodWrapper) {
      this.methodWrapper = methodWrapper;
    }

    @Override
    public void run(IProgressMonitor pm) {
      calls = methodWrapper.getCalls(pm);
    }

    MethodWrapper[] getCalls() {
      if (calls != null) {
        return calls;
      }
      return new MethodWrapper[0];
    }
  }

  /**
   * A named preference that holds the types whose methods are by default expanded with constructors
   * in the Call Hierarchy.
   */
  public static final String OLD_PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS = "CallHierarchy.defaultExpandWithConstructors"; //$NON-NLS-1$

  private final static Object[] EMPTY_ARRAY = new Object[0];

  /**
   * Checks whether given caller method wrapper can be expanded with constructors.
   * 
   * @param wrapper the caller method wrapper
   * @return <code> true</code> if the wrapper can be expanded with constructors, <code>false</code>
   *         otherwise
   */
  static boolean canExpandWithConstructors(CallerMethodWrapper wrapper) {
    DartElement member = wrapper.getMember();
    if (!(member instanceof Method)) {
      return false;
    }
    Method method = (Method) member;
    if (method.isStatic() || method.isConstructor()) {
      return false;
    }
    return true;
  }

  /**
   * Sets the default "expand with constructors" mode for the method wrapper. Does nothing if the
   * mode has already been set.
   * 
   * @param wrapper the caller method wrapper
   */
  static void ensureDefaultExpandWithConstructors(CallerMethodWrapper wrapper) {

    if (!wrapper.isExpandWithConstructorsSet()) {
      if (CallHierarchyContentProvider.canExpandWithConstructors(wrapper)) {
        Method method = (Method) wrapper.getMember();
        Type type = method.getDeclaringType();
        boolean withConstructors = false;
        if (type != null) {
          if (isInTheDefaultExpandWithConstructorList(method)) {
            withConstructors = true;
          }
        }
        wrapper.setExpandWithConstructors(withConstructors);
      }
    }

  }

  /**
   * Returns whether the given element is an "Expand witch Constructors" node.
   * 
   * @param element a method wrapped
   * @return <code>true</code> iff the element is an "Expand witch Constructors" node
   */
  static boolean isExpandWithConstructors(MethodWrapper element) {
    return element instanceof CallerMethodWrapper
        && ((CallerMethodWrapper) element).getExpandWithConstructors();
  }

  /**
   * Checks if the method or its declaring type matches the pre-defined array of methods and types
   * for default expand with constructors.
   * 
   * @param method the wrapped method
   * @return <code>true</code> if method or type matches the pre-defined list, <code>false</code>
   *         otherwise
   */
  static boolean isInTheDefaultExpandWithConstructorList(Method method) {
    String serializedMembers = PreferenceConstants.getPreferenceStore().getString(
        PreferenceConstants.PREF_DEFAULT_EXPAND_WITH_CONSTRUCTORS_MEMBERS);
    if (serializedMembers.length() == 0) {
      return false;
    }

    String[] defaultMemberPatterns = serializedMembers.split(";"); //$NON-NLS-1$

    String methodName = method.getElementName();
    Type declaringType = method.getDeclaringType();
    String declaringTypeName = declaringType.getElementName();
    String superClassName;
    String[] superInterfaceNames;
    try {
      superClassName = declaringType.getSuperclassName();
      if (superClassName != null) {
        superClassName = stripTypeArguments(superClassName);
      }
      superInterfaceNames = declaringType.getSuperInterfaceNames();
      for (int i = 0; i < superInterfaceNames.length; i++) {
        superInterfaceNames[i] = stripTypeArguments(superInterfaceNames[i]);
      }
    } catch (DartModelException e) {
      return false;
    }

    for (int i = 0; i < defaultMemberPatterns.length; i++) {
      String defaultMemberPattern = defaultMemberPatterns[i];
      int pos = defaultMemberPattern.lastIndexOf('.');
      String defaultTypeName = defaultMemberPattern.substring(0, pos);
      String defaultMethodName = defaultMemberPattern.substring(pos + 1);

      if ("*".equals(defaultMethodName)) { //$NON-NLS-1$
        if (declaringTypeName.equals(defaultTypeName)) {
          return true;
        }
      } else {
        if (!methodName.equals(defaultMethodName)) {
          continue;
        }
        if (declaringTypeName.equals(defaultTypeName)) {
          return true;
        }
      }
      if (superClassName != null && typeNameMatches(superClassName, defaultTypeName)) {
        return true;
      }
      for (int j = 0; j < superInterfaceNames.length; j++) {
        String superInterfaceName = superInterfaceNames[j];
        if (typeNameMatches(superInterfaceName, defaultTypeName)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Strips type arguments from the given type name and returns only erased type name.
   * 
   * @param typeName the type name
   * @return the erased type name
   */
  private static String stripTypeArguments(String typeName) {
    int pos = typeName.indexOf('<');
    if (pos != -1) {
      return typeName.substring(0, pos);
    }
    return typeName;
  }

  /**
   * Checks whether the two type names match. They match if they are equal, or if could be the same
   * type but one is missing the package.
   * 
   * @param nameA type name (can be qualified)
   * @param nameB type name (can be qualified)
   * @return <code>true</code> iff the given type names match
   */
  private static boolean typeNameMatches(String nameA, String nameB) {
    if (nameA.equals(nameB)) {
      return true;
    }
    if (nameB.endsWith(nameA) && nameB.lastIndexOf('.') == nameB.length() - nameA.length() - 1) {
      return true;
    }
    if (nameA.endsWith(nameB) && nameA.lastIndexOf('.') == nameA.length() - nameB.length() - 1) {
      return true;
    }
    return false;
  }

  private DeferredTreeContentManager treeContentManager;

  private CallHierarchyViewPart chvPart;

  public CallHierarchyContentProvider(CallHierarchyViewPart part) {
    super();
    chvPart = part;
  }

  @Override
  public void dispose() {
    // Nothing to dispose
  }

  public void doneFetching() {
    if (chvPart != null) {
      chvPart.setCancelEnabled(false);
    }
  }

  @Override
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof TreeRoot) {
      TreeRoot dummyRoot = (TreeRoot) parentElement;
      return dummyRoot.getRoots();

    } else if (parentElement instanceof RealCallers) {
      MethodWrapper parentWrapper = ((RealCallers) parentElement).getParent();
      RealCallers element = ((RealCallers) parentElement);
      if (treeContentManager != null) {
        Object[] children = treeContentManager.getChildren(new DeferredMethodWrapper(this, element));
        if (children != null) {
          return children;
        }
      }
      return fetchChildren(parentWrapper);

    } else if (parentElement instanceof MethodWrapper) {
      MethodWrapper methodWrapper = ((MethodWrapper) parentElement);

      if (shouldStopTraversion(methodWrapper)) {
        return EMPTY_ARRAY;
      } else {
        if (parentElement instanceof CallerMethodWrapper) {
          CallerMethodWrapper caller = (CallerMethodWrapper) parentElement;
          ensureDefaultExpandWithConstructors(caller);
          if (caller.getExpandWithConstructors()) {
            Type type = ((TypeMember) caller.getMember()).getDeclaringType();
            try {
              TypeMember[] constructors = DartModelUtil.getConstructorsOfType(type);
              Object children[] = new Object[constructors.length + 1];
              for (int j = 0; j < constructors.length; j++) {
                MethodCall constructor = new MethodCall(constructors[j]);
                CallerMethodWrapper constructorWrapper = (CallerMethodWrapper) caller.createMethodWrapper(constructor);
                children[j] = constructorWrapper;
              }
              children[constructors.length] = new RealCallers(methodWrapper, caller.getMethodCall());
              return children;
            } catch (DartModelException e) {
              DartToolsPlugin.log(e);
              return null;
            }

          }
        }
        if (treeContentManager != null) {
          Object[] children = treeContentManager.getChildren(new DeferredMethodWrapper(
              this,
              methodWrapper));
          if (children != null) {
            return children;
          }
        }
        return fetchChildren(methodWrapper);
      }
    }
    return EMPTY_ARRAY;
  }

  @Override
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof MethodWrapper) {
      return ((MethodWrapper) element).getParent();
    }
    return null;
  }

  /**
   * Returns the call hierarchy view part.
   * 
   * @return the call hierarchy view part
   */
  public CallHierarchyViewPart getViewPart() {
    return chvPart;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element == TreeRoot.EMPTY_ROOT || element == TreeTermination.SEARCH_CANCELED) {
      return false;
    }

    // Only certain members can have subelements, so there's no need to fool the
    // user into believing that there is more
    if (element instanceof MethodWrapper) {
      MethodWrapper methodWrapper = (MethodWrapper) element;
      if (!methodWrapper.canHaveChildren()) {
        return false;
      }
      if (shouldStopTraversion(methodWrapper)) {
        return false;
      }
      return true;
    } else if (element instanceof TreeRoot) {
      return true;
    } else if (element instanceof DeferredMethodWrapper) {
      // Err on the safe side by returning true even though
      // we don't know for sure that there are children.
      return true;
    }

    return false; // the "Update ..." placeholder has no children
  }

  @Override
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (oldInput instanceof TreeRoot) {
      MethodWrapper[] roots = ((TreeRoot) oldInput).getRoots();
      cancelJobs(roots);
    }
    if (viewer instanceof AbstractTreeViewer) {
      treeContentManager = new DeferredTreeContentManager(
          (AbstractTreeViewer) viewer,
          chvPart.getSite());
    }
  }

  public void startFetching() {
    if (chvPart != null) {
      chvPart.setCancelEnabled(true);
    }
  }

  /**
   * Collapses and refreshes the given element when search has been canceled.
   * 
   * @param element the element on which search has been canceled and which has to be collapsed
   */
  protected void collapseAndRefresh(MethodWrapper element) {
    CallHierarchyViewer viewer = chvPart.getViewer();

    /*
     * Problem case: The user expands the RealCallers node and then unchecks
     * "Expand with Constructors" while the search for the real callers is still in progress.
     * 
     * In this scenario, the RealCallers is not even part of the current tree any more, since the
     * ExpandWithConstructorsAction already toggled the flag and refreshed the tree.
     * 
     * But since setExpandedState(element, false) walks up the getParent() chain of the given
     * element, this causes the parent's children to be created, which would wrongly start a
     * deferred search.
     * 
     * The fix is to do nothing when the RealCaller's parent is expandWithConstructors.
     */
    boolean elementStays = true;
    if (element instanceof RealCallers) {
      elementStays = isExpandWithConstructors(element.getParent());
    }
    if (elementStays) {
      viewer.setExpandedState(element, false);
    }

    viewer.refresh(element);
  }

  protected Object[] fetchChildren(final MethodWrapper methodWrapper) {
    IRunnableContext context = DartToolsPlugin.getActiveWorkbenchWindow();
    MethodWrapperRunnable runnable = new MethodWrapperRunnable(methodWrapper);
    try {
      context.run(true, true, runnable);
    } catch (InvocationTargetException e) {
      ExceptionHandler.handle(
          e,
          CallHierarchyMessages.CallHierarchyContentProvider_searchError_title,
          CallHierarchyMessages.CallHierarchyContentProvider_searchError_message);
      return EMPTY_ARRAY;
    } catch (InterruptedException e) {
      final CallerMethodWrapper element = (CallerMethodWrapper) methodWrapper;
      if (!isExpandWithConstructors(element)) {
        Display.getDefault().asyncExec(new Runnable() {
          @Override
          public void run() {
            collapseAndRefresh(element);
          }
        });
      }
    }

    return runnable.getCalls();
  }

  /**
   * Cancel all current jobs.
   * 
   * @param wrappers the parents to cancel jobs for
   */
  void cancelJobs(MethodWrapper[] wrappers) {
    if (treeContentManager != null && wrappers != null) {
      for (int i = 0; i < wrappers.length; i++) {
        MethodWrapper wrapper = wrappers[i];
        treeContentManager.cancel(wrapper);
      }
      if (chvPart != null) {
        chvPart.setCancelEnabled(false);
      }
    }
  }

  private boolean shouldStopTraversion(MethodWrapper methodWrapper) {
    return (methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth())
        || methodWrapper.isRecursive();
  }
}
