package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartElementLabels;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class DartRefactoringDescriptorUtil {

  /**
   * Predefined argument called <code>element&lt;Number&gt;</code>.
   * <p>
   * This argument should be used to describe the elements being refactored. The value of this
   * argument does not necessarily have to uniquely identify the elements. However, it must be
   * possible to uniquely identify the elements using the value of this argument in conjunction with
   * the values of the other user-defined attributes.
   * </p>
   * <p>
   * The element arguments are simply distinguished by appending a number to the argument name, e.g.
   * element1. The indices of this argument are non zero-based.
   * </p>
   */
  public static final String ATTRIBUTE_ELEMENT = "element"; //$NON-NLS-1$

  /**
   * Predefined argument called <code>input</code>.
   * <p>
   * This argument should be used to describe the element being refactored. The value of this
   * argument does not necessarily have to uniquely identify the input element. However, it must be
   * possible to uniquely identify the input element using the value of this argument in conjunction
   * with the values of the other user-defined attributes.
   * </p>
   */
  public static final String ATTRIBUTE_INPUT = "input"; //$NON-NLS-1$

  /**
   * Predefined argument called <code>name</code>.
   * <p>
   * This argument should be used to name the element being refactored. The value of this argument
   * may be shown in the user interface.
   * </p>
   */
  public static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

  /**
   * Predefined argument called <code>references</code>.
   * <p>
   * This argument should be used to describe whether references to the elements being refactored
   * should be updated as well.
   * </p>
   */
  public static final String ATTRIBUTE_REFERENCES = "references"; //$NON-NLS-1$

  /**
   * Predefined argument called <code>selection</code>.
   * <p>
   * This argument should be used to describe user input selections within a text file. The value of
   * this argument has the format "offset length".
   * </p>
   */
  public static final String ATTRIBUTE_SELECTION = "selection"; //$NON-NLS-1$

  /**
   * Creates a fatal error status telling that the input element does not exist.
   * 
   * @param element the input element, or <code>null</code>
   * @param name the name of the refactoring
   * @param id the id of the refactoring
   * @return the refactoring status
   */
  public static RefactoringStatus createInputFatalStatus(final Object element, final String name,
      final String id) {
    Assert.isNotNull(name);
    Assert.isNotNull(id);
    if (element != null) {
      return RefactoringStatus.createFatalErrorStatus(Messages.format(
          RefactoringCoreMessages.InitializableRefactoring_input_not_exists, new String[] {
              DartElementLabels.getTextLabel(element, DartElementLabels.ALL_FULLY_QUALIFIED), name,
              id}));
    } else {
      return RefactoringStatus.createFatalErrorStatus(Messages.format(
          RefactoringCoreMessages.InitializableRefactoring_inputs_do_not_exist, new String[] {
              name, id}));
    }
  }

  /**
   * Creates a warning status telling that the input element does not exist.
   * 
   * @param element the input element, or <code>null</code>
   * @param name the name of the refactoring
   * @param id the id of the refactoring
   * @return the refactoring status
   */
  public static RefactoringStatus createInputWarningStatus(final Object element, final String name,
      final String id) {
    Assert.isNotNull(name);
    Assert.isNotNull(id);
    if (element != null) {
      return RefactoringStatus.createWarningStatus(Messages.format(
          RefactoringCoreMessages.InitializableRefactoring_input_not_exists, new String[] {
              DartElementLabels.getTextLabel(element, DartElementLabels.ALL_FULLY_QUALIFIED), name,
              id}));
    } else {
      return RefactoringStatus.createWarningStatus(Messages.format(
          RefactoringCoreMessages.InitializableRefactoring_inputs_do_not_exist, new String[] {
              name, id}));
    }
  }

  /**
   * Converts the specified element to an input handle.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param element the element
   * @return a corresponding input handle Note: if the given project is not the element's project,
   *         then the full handle is returned
   */
  public static String elementToHandle(final String project, final DartElement element) {
    final String handle = element.getHandleIdentifier();
    if (project != null && !(element instanceof DartProject)) {
      DartProject dartProject = element.getDartProject();
      if (project.equals(dartProject.getElementName())) {
        final String id = dartProject.getHandleIdentifier();
        return handle.substring(id.length());
      }
    }
    return handle;
  }

  /**
   * Converts an input handle back to the corresponding Dart element.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @return the corresponding Dart element, or <code>null</code> if no such element exists
   */
  public static DartElement handleToElement(final String project, final String handle) {
    return handleToElement(project, handle, true);
  }

  /**
   * Converts an input handle back to the corresponding Dart element.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @param check <code>true</code> to check for existence of the element, <code>false</code>
   *          otherwise
   * @return the corresponding Dart element, or <code>null</code> if no such element exists
   */
  public static DartElement handleToElement(final String project, final String handle,
      final boolean check) {
    return handleToElement(null, project, handle, check);
  }

  /**
   * Converts an input handle back to the corresponding Dart element.
   * 
   * @param owner the working copy owner
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @param check <code>true</code> to check for existence of the element, <code>false</code>
   *          otherwise
   * @return the corresponding Dart element, or <code>null</code> if no such element exists
   */
  public static DartElement handleToElement(final WorkingCopyOwner owner, final String project,
      final String handle, final boolean check) {
    DartElement element = null;
    if (owner != null) {
      element = DartCore.create(handle, owner);
    } else {
      element = DartCore.create(handle);
    }
    if (element == null && project != null) {
      final DartProject dartProject = DartCore.create(ResourcesPlugin.getWorkspace().getRoot()).getDartProject(
          project);
      final String identifier = dartProject.getHandleIdentifier();
      if (owner != null) {
        element = DartCore.create(identifier + handle, owner);
      } else {
        element = DartCore.create(identifier + handle);
      }
    }
    if (check && element instanceof Method) {
      /*
       * Resolve the method based on simple names of parameter types (to accommodate for different
       * qualifications when refactoring is e.g. recorded in source but applied on binary method):
       */
      final Method method = (Method) element;
      final Method[] methods = method.getDeclaringType().findMethods(method);
      if (methods != null && methods.length > 0) {
        element = methods[0];
      }
    }
    if (element != null && (!check || element.exists())) {
      return element;
    }
    return null;
  }

  /**
   * Converts an input handle with the given prefix back to the corresponding resource.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param handle the input handle
   * @return the corresponding resource, or <code>null</code> if no such resource exists. Note: if
   *         the given handle is absolute, the project is not used to resolve.
   */
  public static IResource handleToResource(final String project, final String handle) {
    final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    if (handle.isEmpty()) {
      return null;
    }
    final IPath path = Path.fromPortableString(handle);
    if (path == null) {
      return null;
    }
    if (project != null && !project.isEmpty() && !path.isAbsolute()) {
      return root.getProject(project).findMember(path);
    }
    return root.findMember(path);
  }

  /**
   * Converts the specified resource to an input handle.
   * 
   * @param project the project, or <code>null</code> for the workspace
   * @param resource the resource
   * @return the input handle. Note: if the given project is not the resource's project, then the
   *         full handle is returned.
   */
  public static String resourceToHandle(final String project, final IResource resource) {
    if (project != null && !project.isEmpty() && project.equals(resource.getProject().getName())) {
      return resource.getProjectRelativePath().toPortableString();
    }
    return resource.getFullPath().toPortableString();
  }

  private DartRefactoringDescriptorUtil() {
  }

}
