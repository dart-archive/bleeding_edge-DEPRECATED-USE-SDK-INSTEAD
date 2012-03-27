package com.google.dart.tools.core.refactoring.descriptors;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.Map;

/**
 * Partial implementation of a Dart refactoring contribution.
 * <p>
 * Note: this class is not intended to be extended outside the refactoring framework.
 * </p>
 */
public abstract class DartRefactoringContribution extends RefactoringContribution {

  /**
   * Creates the a new refactoring instance.
   * 
   * @param descriptor the refactoring descriptor
   * @param status the status used for the resulting status
   * @return the refactoring, or <code>null</code>
   * @throws CoreException if an error occurs while creating the refactoring
   */
  public abstract Refactoring createRefactoring(DartRefactoringDescriptor descriptor,
      RefactoringStatus status) throws CoreException;

  @Override
  @SuppressWarnings("unchecked")
  public final Map<String, String> retrieveArgumentMap(final RefactoringDescriptor descriptor) {
    Assert.isNotNull(descriptor);
    if (descriptor instanceof DartRefactoringDescriptor) {
      return ((DartRefactoringDescriptor) descriptor).getArguments();
    }
    return super.retrieveArgumentMap(descriptor);
  }
}
