package com.google.dart.tools.internal.corext.refactoring.reorg;

import com.google.dart.tools.internal.corext.refactoring.participants.ResourceModifications;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.eclipse.ltk.core.refactoring.participants.ValidateEditChecker;

public abstract class RefactoringModifications {

  private ResourceModifications fResourceModifications;

  public RefactoringModifications() {
    fResourceModifications = new ResourceModifications();
  }

  public abstract void buildDelta(IResourceChangeDescriptionFactory builder);

  /**
   * Implementors add all resources that need a validate edit
   * 
   * @param checker the validate edit checker
   */
  public void buildValidateEdits(ValidateEditChecker checker) {
    // Default implementation does nothing.
  }

  public ResourceModifications getResourceModifications() {
    return fResourceModifications;
  }

  public abstract RefactoringParticipant[] loadParticipants(RefactoringStatus status,
      RefactoringProcessor owner,
      String[] natures,
      SharableParticipants shared);

  protected void createIncludingParents(IContainer container) {
    while (container != null
        && !(container.exists() || getResourceModifications().willExist(container))) {
      getResourceModifications().addCreate(container);
      container = container.getParent();
    }
  }

//  protected IResource[] collectResourcesOfInterest(IPackageFragment source) throws CoreException {
//  	DartElement[] children = source.getChildren();
//  	int childOfInterest = DartElement.COMPILATION_UNIT;
//  	if (source.getKind() == IPackageFragmentRoot.K_BINARY) {
//  		childOfInterest = DartElement.CLASS_FILE;
//  	}
//  	ArrayList<IResource> result = new ArrayList<IResource>(children.length);
//  	for (int i = 0; i < children.length; i++) {
//  		DartElement child = children[i];
//  		if (child.getElementType() == childOfInterest && child.getResource() != null) {
//  			result.add(child.getResource());
//  		}
//  	}
//  	// Gather non-java resources
//  	Object[] nonJavaResources = source.getNonJavaResources();
//  	for (int i= 0; i < nonJavaResources.length; i++) {
//  		Object element= nonJavaResources[i];
//  		if (element instanceof IResource) {
//  			result.add((IResource) element);
//  		}
//  	}
//  	return result.toArray(new IResource[result.size()]);
//  }
//
//  protected IFile getClasspathFile(IResource resource) {
//  	IProject project= resource.getProject();
//  	if (project == null)
//  		return null;
//  	IResource result= project.findMember(".classpath"); //$NON-NLS-1$
//  	if (result instanceof IFile)
//  		return (IFile)result;
//  	return null;
//  }
}
