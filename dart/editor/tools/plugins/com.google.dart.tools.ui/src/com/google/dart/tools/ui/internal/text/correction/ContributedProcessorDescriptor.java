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
package com.google.dart.tools.ui.internal.text.correction;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.dialogs.StatusInfo;
import com.google.dart.tools.ui.internal.util.DartModelUtil;
import com.google.dart.tools.ui.text.editor.tmp.JavaScriptCore;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ContributedProcessorDescriptor {

  private final IConfigurationElement fConfigurationElement;
  private Object fProcessorInstance;
  private Boolean fStatus;
  private final String fRequiredSourceLevel;
  private final Set<String> fHandledMarkerTypes;

  private static final String ID = "id"; //$NON-NLS-1$
  private static final String CLASS = "class"; //$NON-NLS-1$

  private static final String REQUIRED_SOURCE_LEVEL = "requiredSourceLevel"; //$NON-NLS-1$

  private static final String HANDLED_MARKER_TYPES = "handledMarkerTypes"; //$NON-NLS-1$
  private static final String MARKER_TYPE = "markerType"; //$NON-NLS-1$

  public ContributedProcessorDescriptor(IConfigurationElement element, boolean testMarkerTypes) {
    fConfigurationElement = element;
    fProcessorInstance = null;
    fStatus = null; // undefined
    if (fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT).length == 0) {
      fStatus = Boolean.TRUE;
    }
    fRequiredSourceLevel = element.getAttribute(REQUIRED_SOURCE_LEVEL);
    fHandledMarkerTypes = testMarkerTypes ? getHandledMarkerTypes(element) : null;
  }

  public boolean canHandleMarkerType(String markerType) {
    return fHandledMarkerTypes == null || fHandledMarkerTypes.contains(markerType);
  }

  public IStatus checkSyntax() {
    IConfigurationElement[] children = fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
    if (children.length > 1) {
      String id = fConfigurationElement.getAttribute(ID);
      return new StatusInfo(
          IStatus.ERROR,
          "Only one < enablement > element allowed. Disabling " + id); //$NON-NLS-1$
    }
    return new StatusInfo(IStatus.OK, "Syntactically correct quick assist/fix processor"); //$NON-NLS-1$
  }

  public Object getProcessor(CompilationUnit cunit, Class<?> expectedType) {
    if (matches(cunit)) {
      if (fProcessorInstance == null) {
        try {
          Object extension = fConfigurationElement.createExecutableExtension(CLASS);
          if (expectedType.isInstance(extension)) {
            fProcessorInstance = extension;
          } else {
            String message = "Invalid extension to " + fConfigurationElement.getName() //$NON-NLS-1$
                + ". Must extends '" + expectedType.getName() + "'." + fConfigurationElement.getContributor().getName(); //$NON-NLS-1$ //$NON-NLS-2$
            DartToolsPlugin.log(new Status(IStatus.ERROR, DartToolsPlugin.PLUGIN_ID, message));
            fStatus = Boolean.FALSE;
            return null;
          }
        } catch (CoreException e) {
          DartToolsPlugin.log(e);
          fStatus = Boolean.FALSE;
          return null;
        }
      }
      return fProcessorInstance;
    }
    return null;
  }

  private Set<String> getHandledMarkerTypes(IConfigurationElement element) {
    HashSet<String> map = new HashSet<String>(7);
    IConfigurationElement[] children = element.getChildren(HANDLED_MARKER_TYPES);
    for (int i = 0; i < children.length; i++) {
      IConfigurationElement[] types = children[i].getChildren(MARKER_TYPE);
      for (int k = 0; k < types.length; k++) {
        String attribute = types[k].getAttribute(ID);
        if (attribute != null) {
          map.add(attribute);
        }
      }
    }
//    if (map.isEmpty()) {
//      map.add(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
//      map.add(IJavaModelMarker.BUILDPATH_PROBLEM_MARKER);
//      map.add(IJavaModelMarker.TASK_MARKER);
//    }
    return map;
  }

  private boolean matches(CompilationUnit cunit) {
    if (fRequiredSourceLevel != null) {
      String current = cunit.getDartProject().getOption(JavaScriptCore.COMPILER_SOURCE, true);
      if (DartModelUtil.isVersionLessThan(current, fRequiredSourceLevel)) {
        return false;
      }
    }

    if (fStatus != null) {
      return fStatus.booleanValue();
    }

    IConfigurationElement[] children = fConfigurationElement.getChildren(ExpressionTagNames.ENABLEMENT);
    if (children.length == 1) {
      try {
        ExpressionConverter parser = ExpressionConverter.getDefault();
        Expression expression = parser.perform(children[0]);
        EvaluationContext evalContext = new EvaluationContext(null, cunit);
        evalContext.addVariable("compilationUnit", cunit); //$NON-NLS-1$
        DartProject javaProject = cunit.getDartProject();
        String[] natures = javaProject.getProject().getDescription().getNatureIds();
        evalContext.addVariable("projectNatures", Arrays.asList(natures)); //$NON-NLS-1$
        evalContext.addVariable(
            "sourceLevel", javaProject.getOption(JavaScriptCore.COMPILER_SOURCE, true)); //$NON-NLS-1$
        return expression.evaluate(evalContext) == EvaluationResult.TRUE;
      } catch (CoreException e) {
        DartToolsPlugin.log(e);
      }
      return false;
    }
    fStatus = Boolean.FALSE;
    return false;
  }

}
