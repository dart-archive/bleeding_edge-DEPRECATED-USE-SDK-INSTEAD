package com.google.dart.tools.internal.corext.refactoring.participants;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.TypeMember;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.internal.core.refactoring.resource.ResourceProcessors;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to deal with Dart element processors.
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public class DartProcessors {

  public static String[] computeAffectedNatures(DartElement element) throws CoreException {
    if (element instanceof TypeMember) {
      TypeMember member = (TypeMember) element;
      if (member.isPrivate()) {
        return element.getDartProject().getProject().getDescription().getNatureIds();
      }
    }
    DartProject project = element.getDartProject();
    return ResourceProcessors.computeAffectedNatures(project.getProject());
  }

  public static String[] computeAffectedNaturs(DartElement[] elements) throws CoreException {
    Set<String> result = new HashSet<String>();
    for (int i = 0; i < elements.length; i++) {
      String[] natures = computeAffectedNatures(elements[i]);
      for (int j = 0; j < natures.length; j++) {
        result.add(natures[j]);
      }
    }
    return result.toArray(new String[result.size()]);
  }
}
