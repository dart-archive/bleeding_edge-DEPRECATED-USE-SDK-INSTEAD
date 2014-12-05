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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.builder.BuildParticipant;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.core.runtime.Status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Each {@link BuildParticipantDeclaration} represents one type of build participant and is used to
 * instantiate that type of build participant.
 * 
 * @coverage dart.tools.core.builder
 */
public class BuildParticipantDeclaration {

  private static final String PARTICIPANT_EXTENSION_POINT = "buildParticipants"; //$NON-NLS-1$
  private static final String PARTICIPANT_CONTRIBUTION = "buildParticipant"; //$NON-NLS-1$
  private static final String PARTICIPANT_CLASS_ATTR = "class"; //$NON-NLS-1$
  private static final String PARTICIPANT_PRIORITY_ATTR = "priority"; //$NON-NLS-1$
  private static final int DEFAULT_PRIORITY = 50;

  private static final Object lock = new Object();

  /**
   * The {@link BuildParticipantDeclaration}s or {@code null} if not yet initialized. Synchronize
   * against {@link #lock} before accessing this field.
   */
  private static ArrayList<BuildParticipantDeclaration> declarations;

  /**
   * Answer build participants for the specified project in the order in which they should be
   * called. The result is a mixture of both {@link BuildParticipant} and
   * {@link DartBuildParticipant}. Instances of {@link DartBuildParticipant} are shared across all
   * builders while instances of {@link BuildParticipant} are created one per project and wrapped in
   * new instances of {@link DartBuildParticipant}. It is the responsibility of the caller to cache
   * the result as this method returns new instances of {@link BuildParticipant} each time it is
   * called.
   * 
   * @param project the project for which participants are to be created (not {@code null})
   * @return an array of participants (not {@code null} , contains no {@code null}s)
   */
  public static BuildParticipant[] participantsFor(IProject project) {
    synchronized (lock) {
      if (declarations == null) {

        // Get the extensions declaring build participants
        IExtensionRegistry registry = RegistryFactory.getRegistry();
        IExtensionPoint extensionPoint = registry.getExtensionPoint(
            DartCore.PLUGIN_ID,
            PARTICIPANT_EXTENSION_POINT);

        // Create build participant declarations
        declarations = new ArrayList<BuildParticipantDeclaration>();
        for (IExtension extension : extensionPoint.getExtensions()) {
          for (IConfigurationElement element : extension.getConfigurationElements()) {
            if (element.getName().equals(BuildParticipantDeclaration.PARTICIPANT_CONTRIBUTION)) {
              try {
                if (DartCoreDebug.ENABLE_ANALYSIS_SERVER
                    && !element.getAttribute("id").equals(
                        "com.google.dart.tools.core.buildParticipant.analysis.engine")) {

                  declarations.add(new BuildParticipantDeclaration(element));
                }
              } catch (CoreException e1) {
                DartCore.logError(
                    "Exception creating build participant declaration\n  from plugin "
                        + element.getNamespaceIdentifier(),
                    e1);
              }
            }
          }
        }

        // Sort the build declarations by priority
        Collections.sort(declarations, new Comparator<BuildParticipantDeclaration>() {
          @Override
          public int compare(BuildParticipantDeclaration d1, BuildParticipantDeclaration d2) {
            return d1.getPriority() - d2.getPriority();
          }
        });
      }

      // Construct one new participant for each declaration
      ArrayList<BuildParticipant> participants = new ArrayList<BuildParticipant>();
      Iterator<BuildParticipantDeclaration> iter = declarations.iterator();
      while (iter.hasNext()) {
        BuildParticipantDeclaration declaration = iter.next();
        try {
          participants.add(declaration.newParticipant(project));
        } catch (CoreException e) {
          DartCore.logError("Exception instantiating build participant\n  from plugin "
              + declaration.getPluginId(), e);
          iter.remove();
        }
      }
      return participants.toArray(new BuildParticipant[participants.size()]);
    }
  }

  private final IConfigurationElement configElement;

  private final int priority;

  /**
   * Construct a new instance representing the specified type of build participant.
   * 
   * @param element the element (not {@code null})
   */
  private BuildParticipantDeclaration(IConfigurationElement element) throws CoreException {

    // Extract the priority from the element if defined
    String priorityAttr = element.getAttribute(BuildParticipantDeclaration.PARTICIPANT_PRIORITY_ATTR);
    int priorityInt;
    if (priorityAttr != null) {
      try {
        priorityInt = Integer.parseInt(priorityAttr);
      } catch (NumberFormatException e) {
        throw new CoreException(new Status(
            IStatus.ERROR,
            DartCore.PLUGIN_ID,
            "Expected priority to be an integer, but found" + priorityAttr));
      }
    } else {
      priorityInt = BuildParticipantDeclaration.DEFAULT_PRIORITY;
    }

    this.configElement = element;
    this.priority = priorityInt;
  }

  /**
   * Answer the identifer of the plugin declaring this participant.
   */
  private String getPluginId() {
    return configElement.getNamespaceIdentifier();
  }

  /**
   * Answer the priority for build participants created using this declaration.
   * 
   * @return the priority where a lower number indicates a higher priority
   */
  private int getPriority() {
    return priority;
  }

  /**
   * Answer a new build participant for the specified project. It is the responsibility of the
   * caller to cache the result as this method may return a new instance each time it is called.
   * 
   * @param project the project associated with the new participant (not {@code null})
   */
  private BuildParticipant newParticipant(IProject project) throws CoreException {

    Object object = configElement.createExecutableExtension(PARTICIPANT_CLASS_ATTR);
    if (object instanceof BuildParticipant) {
      return (BuildParticipant) object;
    }
    throw new CoreException(new Status(
        IStatus.ERROR,
        DartCore.PLUGIN_ID,
        "Expected build participant to be an instance of\n  " + BuildParticipant.class.getName()
            + "\n  but was " + (object != null ? object.getClass().getName() : "null")));
  }

}
