/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.cleanup.preference;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.IWorkingCopyManager;
import org.osgi.service.prefs.BackingStoreException;

/**
 *
 */
public class PreferencesAccess {

  private static class WorkingCopyPreferencesAccess extends PreferencesAccess {

    private final IWorkingCopyManager fWorkingCopyManager;

    private WorkingCopyPreferencesAccess(IWorkingCopyManager workingCopyManager) {
      fWorkingCopyManager = workingCopyManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jdt.internal.ui.preferences.PreferencesAccess#applyChanges()
     */
    @Override
    public void applyChanges() throws BackingStoreException {
      fWorkingCopyManager.applyChanges();
    }

    @Override
    public IScopeContext getDefaultScope() {
      return getWorkingCopyScopeContext(super.getDefaultScope());
    }

    @Override
    public IScopeContext getInstanceScope() {
      return getWorkingCopyScopeContext(super.getInstanceScope());
    }

    @Override
    public IScopeContext getProjectScope(IProject project) {
      return getWorkingCopyScopeContext(super.getProjectScope(project));
    }

    private final IScopeContext getWorkingCopyScopeContext(IScopeContext original) {
      return new WorkingCopyScopeContext(fWorkingCopyManager, original);
    }
  }
  private static class WorkingCopyScopeContext implements IScopeContext {

    private final IWorkingCopyManager fWorkingCopyManager;
    private final IScopeContext fOriginal;

    public WorkingCopyScopeContext(IWorkingCopyManager workingCopyManager, IScopeContext original) {
      fWorkingCopyManager = workingCopyManager;
      fOriginal = original;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.preferences.IScopeContext#getLocation()
     */
    @Override
    public IPath getLocation() {
      return fOriginal.getLocation();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.preferences.IScopeContext#getName()
     */
    @Override
    public String getName() {
      return fOriginal.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.core.runtime.preferences.IScopeContext#getNode(java.lang.String)
     */
    @Override
    public IEclipsePreferences getNode(String qualifier) {
      return fWorkingCopyManager.getWorkingCopy(fOriginal.getNode(qualifier));
    }
  }

  public static DefaultScope DEFAULT_SCOPE = new DefaultScope();

  public static InstanceScope INSTANCE_SCOPE = new InstanceScope();

  public static PreferencesAccess getOriginalPreferences() {
    return new PreferencesAccess();
  }

  public static PreferencesAccess getWorkingCopyPreferences(IWorkingCopyManager workingCopyManager) {
    return new WorkingCopyPreferencesAccess(workingCopyManager);
  }

  private PreferencesAccess() {
    // can only extends in this file
  }

  /**
   * Applies the changes
   * 
   * @throws BackingStoreException thrown when the changes could not be applied
   */
  public void applyChanges() throws BackingStoreException {
  }

  public IScopeContext getDefaultScope() {
    return DEFAULT_SCOPE;
  }

  public IScopeContext getInstanceScope() {
    return INSTANCE_SCOPE;
  }

  public IScopeContext getProjectScope(IProject project) {
    return new ProjectScope(project);
  }

}
