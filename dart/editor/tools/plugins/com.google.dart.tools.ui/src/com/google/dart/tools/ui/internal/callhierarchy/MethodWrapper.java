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
package com.google.dart.tools.ui.internal.callhierarchy;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.ui.callhierarchy.MethodWrapperWorkbenchAdapter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.ui.model.IWorkbenchAdapter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class represents the general parts of a method call (either to or from a method).
 */
public abstract class MethodWrapper extends PlatformObject {
  private Map<String, MethodCall> fElements = null;

  /*
   * A cache of previously found methods. This cache should be searched before adding a "new" method
   * object reference to the list of elements. This way previously found methods won't be searched
   * again.
   */
  private Map<String, Map<String, MethodCall>> fMethodCache;
  private final MethodCall fMethodCall;
  private final MethodWrapper fParent;
  private int fLevel;
  /**
   * One of {@link IJavaSearchConstants#REFERENCES}, {@link IJavaSearchConstants#READ_ACCESSES}, or
   * {@link IJavaSearchConstants#WRITE_ACCESSES}, or 0 if not set. Only used for root wrappers.
   */
  private int fFieldSearchMode;

  public MethodWrapper(MethodWrapper parent, MethodCall methodCall) {
    Assert.isNotNull(methodCall);

    if (parent == null) {
      setMethodCache(new HashMap<String, Map<String, MethodCall>>());
      fLevel = 1;
    } else {
      setMethodCache(parent.getMethodCache());
      fLevel = parent.getLevel() + 1;
    }

    this.fMethodCall = methodCall;
    this.fParent = parent;
  }

  /**
   * @return whether this member can have children
   */
  public abstract boolean canHaveChildren();

  @Override
  public boolean equals(Object oth) {
    if (this == oth) {
      return true;
    }

    if (oth == null) {
      return false;
    }

    if (oth instanceof MethodWrapperWorkbenchAdapter) {
      //Note: A MethodWrapper is equal to a referring MethodWrapperWorkbenchAdapter and vice versa (bug 101677).
      oth = ((MethodWrapperWorkbenchAdapter) oth).getMethodWrapper();
    }

    if (oth.getClass() != getClass()) {
      return false;
    }

    MethodWrapper other = (MethodWrapper) oth;

    if (this.fParent == null) {
      if (other.fParent != null) {
        return false;
      }
    } else {
      if (!this.fParent.equals(other.fParent)) {
        return false;
      }
    }

    if (this.getMethodCall() == null) {
      if (other.getMethodCall() != null) {
        return false;
      }
    } else {
      if (!this.getMethodCall().equals(other.getMethodCall())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    if (adapter == DartElement.class) {
      return getMember();
    } else if (adapter == IWorkbenchAdapter.class) {
      return new MethodWrapperWorkbenchAdapter(this);
    } else {
      return null;
    }
  }

  public MethodWrapper[] getCalls(IProgressMonitor progressMonitor) {
    if (fElements == null) {
      doFindChildren(progressMonitor);
    }

    MethodWrapper[] result = new MethodWrapper[fElements.size()];
    int i = 0;

    for (Iterator<String> iter = fElements.keySet().iterator(); iter.hasNext();) {
      MethodCall methodCall = getMethodCallFromMap(fElements, iter.next());
      result[i++] = createMethodWrapper(methodCall);
    }

    return result;
  }

  public int getFieldSearchMode() {
    if (fFieldSearchMode != 0) {
      return fFieldSearchMode;
    }
    MethodWrapper parent = getParent();
    while (parent != null) {
      if (parent.fFieldSearchMode != 0) {
        return parent.fFieldSearchMode;
      } else {
        parent = parent.getParent();
      }
    }
    return 2; //TODO IJavaSearchConstants.REFERENCES;
  }

  public int getLevel() {
    return fLevel;
  }

  public DartElement getMember() {
    return getMethodCall().getMember();
  }

  public MethodCall getMethodCall() {
    return fMethodCall;
  }

  public String getName() {
    if (getMethodCall() != null) {
      return getMethodCall().getMember().getElementName();
    } else {
      return ""; //$NON-NLS-1$
    }
  }

  public MethodWrapper getParent() {
    return fParent;
  }

  @Override
  public int hashCode() {
    final int PRIME = 1000003;
    int result = 0;

    if (fParent != null) {
      result = (PRIME * result) + fParent.hashCode();
    }

    if (getMethodCall() != null) {
      result = (PRIME * result) + getMethodCall().getMember().hashCode();
    }

    return result;
  }

  /**
   * Determines if the method represents a recursion call (i.e. whether the method call is already
   * in the cache.)
   * 
   * @return True if the call is part of a recursion
   */
  public boolean isRecursive() {
    if (fParent instanceof RealCallers) {
      return false;
    }
    MethodWrapper current = getParent();

    while (current != null) {
      if (getMember().getHandleIdentifier().equals(current.getMember().getHandleIdentifier())) {
        return true;
      }

      current = current.getParent();
    }

    return false;
  }

  /**
   * Removes the given method call from the cache.
   */
  public void removeFromCache() {
    fElements = null;
    fMethodCache.remove(getMethodCall().getKey());
  }

  public void setFieldSearchMode(int fieldSearchMode) {
    fFieldSearchMode = fieldSearchMode;
  }

  /**
   * Checks with the progress monitor to see whether the creation of the type hierarchy should be
   * canceled. Should be regularly called so that the user can cancel.
   * 
   * @param progressMonitor the progress monitor
   * @exception OperationCanceledException if cancelling the operation has been requested
   * @see IProgressMonitor#isCanceled
   */
  protected void checkCanceled(IProgressMonitor progressMonitor) {
    if (progressMonitor != null && progressMonitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Creates a method wrapper for the child of the receiver.
   * 
   * @param methodCall the method call
   * @return the method wrapper
   */
  protected abstract MethodWrapper createMethodWrapper(MethodCall methodCall);

  /**
   * This method finds the children of the current IMember (either callers or callees, depending on
   * the concrete subclass).
   * 
   * @param progressMonitor a progress monitor
   * @return a map from handle identifier ({@link String}) to {@link MethodCall}
   */
  protected abstract Map<String, MethodCall> findChildren(IProgressMonitor progressMonitor);

  protected abstract String getTaskName();

  private void addCallToCache(MethodCall methodCall) {
    Map<String, MethodCall> cachedCalls = lookupMethod(this.getMethodCall());
    cachedCalls.put(methodCall.getKey(), methodCall);
  }

  private void doFindChildren(IProgressMonitor progressMonitor) {
    Map<String, MethodCall> existingResults = lookupMethod(getMethodCall());

    if (existingResults != null && !existingResults.isEmpty()) {
      fElements = new HashMap<String, MethodCall>();
      fElements.putAll(existingResults);
    } else {
      initCalls();

      if (progressMonitor != null) {
        progressMonitor.beginTask(getTaskName(), 100);
      }

      try {
        performSearch(progressMonitor);
      } catch (OperationCanceledException e) {
        fElements = null;
        throw e;
      } finally {
        if (progressMonitor != null) {
          progressMonitor.done();
        }
      }
    }
  }

  private Map<String, Map<String, MethodCall>> getMethodCache() {
    return fMethodCache;
  }

  private MethodCall getMethodCallFromMap(Map<String, MethodCall> elements, String key) {
    return elements.get(key);
  }

  private void initCacheForMethod() {
    Map<String, MethodCall> cachedCalls = new HashMap<String, MethodCall>();
    getMethodCache().put(this.getMethodCall().getKey(), cachedCalls);
  }

  private void initCalls() {
    this.fElements = new HashMap<String, MethodCall>();

    initCacheForMethod();
  }

  /**
   * Looks up a previously created search result in the "global" cache.
   * 
   * @param methodCall the method call
   * @return the List of previously found search results
   */
  private Map<String, MethodCall> lookupMethod(MethodCall methodCall) {
    return getMethodCache().get(methodCall.getKey());
  }

  private void performSearch(IProgressMonitor progressMonitor) {
    fElements = findChildren(progressMonitor);

    for (Iterator<String> iter = fElements.keySet().iterator(); iter.hasNext();) {
      checkCanceled(progressMonitor);

      MethodCall methodCall = getMethodCallFromMap(fElements, iter.next());
      addCallToCache(methodCall);
    }
  }

  private void setMethodCache(Map<String, Map<String, MethodCall>> methodCache) {
    fMethodCache = methodCache;
  }
}
