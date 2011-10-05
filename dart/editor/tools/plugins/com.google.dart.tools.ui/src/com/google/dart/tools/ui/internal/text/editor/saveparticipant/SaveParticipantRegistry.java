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
package com.google.dart.tools.ui.internal.text.editor.saveparticipant;

import com.google.dart.tools.ui.internal.cleanup.CleanUpPostSaveListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.IScopeContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A registry for save participants. This registry manages {@link SaveParticipantDescriptor}s and
 * keeps track of enabled save participants.
 * <p>
 * Save participants can be enabled and disabled on the Java &gt; Editor &gt; Save Participants
 * preference page. Enabled save participants are notified through a call to
 * {@link IPostSaveListener#saved(org.eclipse.CompilationUnit.jsdt.core.IJavaScriptUnit, org.eclipse.core.runtime.IProgressMonitor)}
 * whenever the {@link org.eclipse.wst.jsdt.internal.ui.javaeditor.CompilationUnitDocumentProvider}
 * saves a compilation unit that is in the workspace.
 * </p>
 * <p>
 * An instance of this registry can be received through a call to
 * {@link org.eclipse.DartToolsPlugin.jsdt.internal.ui.JavaScriptPlugin#getSaveParticipantRegistry()}
 * .
 * </p>
 */
public final class SaveParticipantRegistry {

  private static final IPostSaveListener[] EMPTY_ARRAY = new IPostSaveListener[0];

  /** The map of descriptors, indexed by their identifiers. */
  private Map<String, SaveParticipantDescriptor> fDescriptors;

  /**
   * Creates a new instance.
   */
  public SaveParticipantRegistry() {
  }

  public void dispose() {
  }

  public IPostSaveListener[] getEnabledPostSaveListeners(IProject project) {
    return getEnabledPostSaveListeners(new ProjectScope(project));
  }

  /**
   * Returns an array of <code>IPostSaveListener</code> which are enabled in the given context.
   *
   * @param context the context from which to retrive the settings from, not null
   * @return the current enabled post save listeners according to the preferences
   */
  public synchronized IPostSaveListener[] getEnabledPostSaveListeners(IScopeContext context) {
    ensureRegistered();

    List<IPostSaveListener> result = null;
    for (Iterator<SaveParticipantDescriptor> iterator = fDescriptors.values().iterator(); iterator.hasNext();) {
      SaveParticipantDescriptor descriptor = iterator.next();
      if (descriptor.getPreferenceConfiguration().isEnabled(context)) {
        if (result == null) {
          result = new ArrayList<IPostSaveListener>();
        }
        result.add(descriptor.getPostSaveListener());
      }
    }

    if (result == null) {
      return EMPTY_ARRAY;
    } else {
      return result.toArray(new IPostSaveListener[result.size()]);
    }
  }

  /**
   * Returns the save participant descriptor for the given <code>id</code> or <code>null</code> if
   * no such listener is registered.
   *
   * @param id the identifier of the requested save participant
   * @return the corresponding descriptor, or <code>null</code> if none can be found
   */
  public synchronized SaveParticipantDescriptor getSaveParticipantDescriptor(String id) {
    ensureRegistered();
    return fDescriptors.get(id);
  }

  /**
   * Returns an array of <code>SaveParticipantDescriptor</code> describing all registered save
   * participants.
   *
   * @return the array of registered save participant descriptors
   */
  public synchronized SaveParticipantDescriptor[] getSaveParticipantDescriptors() {
    ensureRegistered();
    return fDescriptors.values().toArray(
        new SaveParticipantDescriptor[fDescriptors.size()]);
  }

  /**
   * Checks weather there are enabled or disabled post save listener in the given context.
   *
   * @param context to context to check, not null
   * @return true if there are settings in context
   */
  public synchronized boolean hasSettingsInScope(IScopeContext context) {
    ensureRegistered();

    for (Iterator<SaveParticipantDescriptor> iterator = fDescriptors.values().iterator(); iterator.hasNext();) {
      SaveParticipantDescriptor descriptor = iterator.next();
      if (descriptor.getPreferenceConfiguration().hasSettingsInScope(context)) {
        return true;
      }
    }

    return false;
  }

  /**
   * Ensures that all descriptors are created and stored in <code>fDescriptors</code>.
   */
  private void ensureRegistered() {
    if (fDescriptors == null) {
      reloadDescriptors();
    }
  }

  /**
   * Loads the save participants.
   * <p>
   * This method can be called more than once in order to reload from a changed extension registry.
   * </p>
   */
  private void reloadDescriptors() {
    Map<String, SaveParticipantDescriptor> map = new HashMap<String, SaveParticipantDescriptor>();
    SaveParticipantDescriptor desc = new SaveParticipantDescriptor(new CleanUpPostSaveListener()) {
      /**
       * {@inheritDoc}
       */
      @Override
      public ISaveParticipantPreferenceConfiguration createPreferenceConfiguration() {
        return new CleanUpSaveParticipantPreferenceConfiguration();
      }
    };
    map.put(desc.getId(), desc);

    fDescriptors = map;
  }

}
