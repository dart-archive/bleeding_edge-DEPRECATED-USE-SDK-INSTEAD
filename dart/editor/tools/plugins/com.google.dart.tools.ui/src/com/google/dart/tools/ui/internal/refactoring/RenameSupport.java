package com.google.dart.tools.ui.internal.refactoring;

import com.google.dart.core.ILocalVariable;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.internal.corext.refactoring.rename.DartRenameProcessor;
import com.google.dart.tools.internal.corext.refactoring.reorg.RenameSelectionState;
import com.google.dart.tools.internal.corext.refactoring.tagging.INameUpdating;
import com.google.dart.tools.internal.corext.refactoring.tagging.IReferenceUpdating;
import com.google.dart.tools.internal.corext.refactoring.tagging.ITextUpdating;
import com.google.dart.tools.ui.DartUIMessages;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.RenameProcessor;
import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.InvocationTargetException;

/**
 * Central access point to execute rename refactorings.
 * <p>
 * Note: this class is not intended to be subclassed or instantiated.
 * </p>
 * 
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RenameSupport {

  /**
   * Creates a new rename support for the given {@link CompilationUnit}.
   * 
   * @param unit the {@link CompilationUnit} to be renamed.
   * @param newName the compilation unit's new name. <code>null</code> is a valid value indicating
   *          that no new name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code>, and <code>UPDATE_TEXTUAL_MATCHES</code>, or their
   *          bitwise OR, or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
  public static RenameSupport create(CompilationUnit unit, String newName, int flags)
      throws CoreException {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    JavaRenameProcessor processor= new RenameCompilationUnitProcessor(unit);
//    return new RenameSupport(processor, newName, flags);
  }

  /**
   * Creates a new rename support for the given {@link DartProject}.
   * 
   * @param project the {@link DartProject} to be renamed.
   * @param newName the project's new name. <code>null</code> is a valid value indicating that no
   *          new name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code> or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
  public static RenameSupport create(DartProject project, String newName, int flags)
      throws CoreException {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    JavaRenameProcessor processor= new RenameJavaProjectProcessor(project);
//    return new RenameSupport(processor, newName, flags);
  }

  /**
   * Creates a new rename support for the given {@link ILocalVariable}.
   * 
   * @param variable the {@link ILocalVariable} to be renamed.
   * @param newName the variable's new name. <code>null</code> is a valid value indicating that no
   *          new name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code>, or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
  public static RenameSupport create(DartVariableDeclaration variable, String newName, int flags)
      throws CoreException {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    RenameLocalVariableProcessor processor= new RenameLocalVariableProcessor(variable);
//    processor.setUpdateReferences(updateReferences(flags));
//    return new RenameSupport(processor, newName, flags);
  }

  /**
   * Creates a new rename support for the given {@link Field}.
   * 
   * @param field the {@link Field} to be renamed.
   * @param newName the field's new name. <code>null</code> is a valid value indicating that no new
   *          name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code>, <code>UPDATE_TEXTUAL_MATCHES</code>,
   *          <code>UPDATE_GETTER_METHOD</code>, and <code>UPDATE_SETTER_METHOD</code>, or their
   *          bitwise OR, or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
  public static RenameSupport create(Field field, String newName, int flags) throws CoreException {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    if (JdtFlags.isEnum(field))
//    	return new RenameSupport(new RenameEnumConstProcessor(field), newName, flags);
//    else {
//    	final RenameFieldProcessor processor= new RenameFieldProcessor(field);
//    	processor.setRenameGetter(updateGetterMethod(flags));
//    	processor.setRenameSetter(updateSetterMethod(flags));
//    	return new RenameSupport(processor, newName, flags);
//    }
  }

  /**
   * Creates a new rename support for the given {@link Method}.
   * 
   * @param method the {@link Method} to be renamed.
   * @param newName the method's new name. <code>null</code> is a valid value indicating that no new
   *          name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code> or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
  public static RenameSupport create(Method method, String newName, int flags) throws CoreException {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    JavaRenameProcessor processor;
//    if (MethodChecks.isVirtual(method)) {
//    	processor= new RenameVirtualMethodProcessor(method);
//    } else {
//    	processor= new RenameNonVirtualMethodProcessor(method);
//    }
//    return new RenameSupport(processor, newName, flags);
  }

  /**
   * Creates a new rename support for the given {@link Type}.
   * 
   * @param type the {@link Type} to be renamed.
   * @param newName the type's new name. <code>null</code> is a valid value indicating that no new
   *          name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code>, and <code>UPDATE_TEXTUAL_MATCHES</code>, or their
   *          bitwise OR, or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
  public static RenameSupport create(Type type, String newName, int flags) throws CoreException {
    // TODO(scheglov) implement
    throw new RuntimeException("Not implemented");
//    JavaRenameProcessor processor= new RenameTypeProcessor(type);
//    return new RenameSupport(processor, newName, flags);
  }

  private static void initialize(DartRenameProcessor processor, String newName, int flags) {

    setNewName(processor, newName);
    if (processor instanceof IReferenceUpdating) {
      IReferenceUpdating reference = (IReferenceUpdating) processor;
      reference.setUpdateReferences(updateReferences(flags));
    }

    if (processor instanceof ITextUpdating) {
      ITextUpdating text = (ITextUpdating) processor;
      text.setUpdateTextualMatches(updateTextualMatches(flags));
    }
  }

  private static void setNewName(INameUpdating refactoring, String newName) {
    if (newName != null) {
      refactoring.setNewElementName(newName);
    }
  }

  private static boolean updateGetterMethod(int flags) {
    return (flags & UPDATE_GETTER_METHOD) != 0;
  }

  private static boolean updateReferences(int flags) {
    return (flags & UPDATE_REFERENCES) != 0;
  }

  private static boolean updateSetterMethod(int flags) {
    return (flags & UPDATE_SETTER_METHOD) != 0;
  }

  private static boolean updateTextualMatches(int flags) {
    int TEXT_UPDATES = UPDATE_TEXTUAL_MATCHES | UPDATE_REGULAR_COMMENTS | UPDATE_STRING_LITERALS;
    return (flags & TEXT_UPDATES) != 0;
  }

  private RenameRefactoring fRefactoring;

  private RefactoringStatus fPreCheckStatus;

//  private RenameSupport(RenameJavaElementDescriptor descriptor) throws CoreException {
//  	RefactoringStatus refactoringStatus= new RefactoringStatus();
//  	fRefactoring= (RenameRefactoring) descriptor.createRefactoring(refactoringStatus);
//  	if (refactoringStatus.hasFatalError()) {
//  		fPreCheckStatus= refactoringStatus;
//  	} else {
//  		preCheck();
//  		refactoringStatus.merge(fPreCheckStatus);
//  		fPreCheckStatus= refactoringStatus;
//  	}
//  }

  /** Flag indication that no additional update is to be performed. */
  public static final int NONE = 0;

  /** Flag indicating that references are to be updated as well. */
  public static final int UPDATE_REFERENCES = 1 << 0;

  /**
   * Flag indicating that Javadoc comments are to be updated as well.
   * 
   * @deprecated use UPDATE_REFERENCES or UPDATE_TEXTUAL_MATCHES or both.
   */
  @Deprecated
  public static final int UPDATE_JAVADOC_COMMENTS = 1 << 1;

  /**
   * Creates a new rename support for the given {@link IPackageFragmentRoot}.
   * 
   * @param root the {@link IPackageFragmentRoot} to be renamed.
   * @param newName the package fragment root's new name. <code>null</code> is a valid value
   *          indicating that no new name is provided.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
//  public static RenameSupport create(IPackageFragmentRoot root, String newName) throws CoreException {
//  	JavaRenameProcessor processor= new RenameSourceFolderProcessor(root);
//  	return new RenameSupport(processor, newName, 0);
//  }

  /**
   * Creates a new rename support for the given {@link IPackageFragment}.
   * 
   * @param fragment the {@link IPackageFragment} to be renamed.
   * @param newName the package fragment's new name. <code>null</code> is a valid value indicating
   *          that no new name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code>, and <code>UPDATE_TEXTUAL_MATCHES</code>, or their
   *          bitwise OR, or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
//  public static RenameSupport create(IPackageFragment fragment, String newName, int flags) throws CoreException {
//  	JavaRenameProcessor processor= new RenamePackageProcessor(fragment);
//  	return new RenameSupport(processor, newName, flags);
//  }

  /**
   * Flag indicating that regular comments are to be updated as well.
   * 
   * @deprecated use UPDATE_TEXTUAL_MATCHES
   */
  @Deprecated
  public static final int UPDATE_REGULAR_COMMENTS = 1 << 2;

  /**
   * Flag indicating that string literals are to be updated as well.
   * 
   * @deprecated use UPDATE_TEXTUAL_MATCHES
   */
  @Deprecated
  public static final int UPDATE_STRING_LITERALS = 1 << 3;

  /**
   * Flag indicating that textual matches in comments and in string literals are to be updated as
   * well.
   */
  public static final int UPDATE_TEXTUAL_MATCHES = 1 << 6;

  /** Flag indicating that the getter method is to be updated as well. */
  public static final int UPDATE_GETTER_METHOD = 1 << 4;

  /**
   * Creates a new rename support for the given {@link ITypeParameter}.
   * 
   * @param parameter the {@link ITypeParameter} to be renamed.
   * @param newName the parameter's new name. <code>null</code> is a valid value indicating that no
   *          new name is provided.
   * @param flags flags controlling additional parameters. Valid flags are
   *          <code>UPDATE_REFERENCES</code>, or <code>NONE</code>.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
//  public static RenameSupport create(ITypeParameter parameter, String newName, int flags) throws CoreException {
//  	RenameTypeParameterProcessor processor= new RenameTypeParameterProcessor(parameter);
//  	processor.setUpdateReferences(updateReferences(flags));
//  	return new RenameSupport(processor, newName, flags);
//  }

  /** Flag indicating that the setter method is to be updated as well. */
  public static final int UPDATE_SETTER_METHOD = 1 << 5;

  /**
   * Creates a new rename support for the given {@link RenameJavaElementDescriptor}.
   * 
   * @param descriptor the {@link RenameJavaElementDescriptor} to create a {@link RenameSupport}
   *          for. The caller is responsible for configuring the descriptor before it is passed.
   * @return the {@link RenameSupport}.
   * @throws CoreException if an unexpected error occurred while creating the {@link RenameSupport}.
   */
//  public static RenameSupport create(RenameJavaElementDescriptor descriptor) throws CoreException {
//  	return new RenameSupport(descriptor);
//  }

  private RenameSupport(DartRenameProcessor processor, String newName, int flags) {
    fRefactoring = new RenameRefactoring(processor);
    initialize(processor, newName, flags);
  }

  /**
   * Opens the refactoring dialog for this rename support.
   * 
   * @param parent a shell used as a parent for the refactoring dialog.
   * @throws CoreException if an unexpected exception occurs while opening the dialog.
   * 
   * @see #openDialog(Shell, boolean)
   */
  public void openDialog(Shell parent) throws CoreException {
    openDialog(parent, false);
  }

  /**
   * Opens the refactoring dialog for this rename support.
   * 
   * <p>
   * This method has to be called from within the UI thread.
   * </p>
   * 
   * @param parent a shell used as a parent for the refactoring, preview, or error dialog
   * @param showPreviewOnly if <code>true</code>, the dialog skips all user input pages and directly
   *          shows the preview or error page. Otherwise, shows all pages.
   * @return <code>true</code> if the refactoring has been executed successfully, <code>false</code>
   *         if it has been canceled or if an error has happened during initial conditions checking.
   * 
   * @throws CoreException if an error occurred while executing the operation.
   * 
   * @see #openDialog(Shell)
   */
  public boolean openDialog(Shell parent, boolean showPreviewOnly) throws CoreException {
    // TODO(scheglov) implement
    return false;
//    ensureChecked();
//    if (fPreCheckStatus.hasFatalError()) {
//    	showInformation(parent, fPreCheckStatus);
//    	return false;
//    }
//
//    UserInterfaceStarter starter;
//    if (! showPreviewOnly) {
//    	starter= RenameUserInterfaceManager.getDefault().getStarter(fRefactoring);
//    } else {
//    	starter= new RenameUserInterfaceStarter();
//    	RenameRefactoringWizard wizard= new RenameRefactoringWizard(fRefactoring, fRefactoring.getName(), null, null, null) {
//    		@Override
//    		protected void addUserInputPages() {
//    			// nothing to add
//    		}
//    	};
//    	wizard.setForcePreviewReview(showPreviewOnly);
//    	starter.initialize(wizard);
//    }
//    return starter.activate(fRefactoring, parent, getJavaRenameProcessor().getSaveMode());
  }

  /**
   * Executes the rename refactoring without showing a dialog to gather additional user input (for
   * example the new name of the <tt>IJavaElement</tt>). Only an error dialog is shown (if
   * necessary) to present the result of the refactoring's full precondition checking.
   * <p>
   * The method has to be called from within the UI thread.
   * </p>
   * 
   * @param parent a shell used as a parent for the error dialog.
   * @param context a {@link IRunnableContext} to execute the operation.
   * 
   * @throws InterruptedException if the operation has been canceled by the user.
   * @throws InvocationTargetException if an error occurred while executing the operation.
   * 
   * @see #openDialog(Shell)
   * @see IRunnableContext#run(boolean, boolean, org.eclipse.jface.operation.IRunnableWithProgress)
   */
  public void perform(Shell parent, IRunnableContext context) throws InterruptedException,
      InvocationTargetException {
    // TODO(scheglov) implement
//    try {
//    	ensureChecked();
//    	if (fPreCheckStatus.hasFatalError()) {
//    		showInformation(parent, fPreCheckStatus);
//    		return;
//    	}
//
//    	RenameSelectionState state= createSelectionState();
//
//    	RefactoringExecutionHelper helper= new RefactoringExecutionHelper(fRefactoring,
//    			RefactoringCore.getConditionCheckingFailedSeverity(),
//    			getJavaRenameProcessor().getSaveMode(),
//    			parent,
//    			context);
//    	helper.perform(true, true);
//
//    	restoreSelectionState(state);
//    } catch (CoreException e) {
//    	throw new InvocationTargetException(e);
//    }
  }

  /**
   * Executes some light weight precondition checking. If the returned status is an error then the
   * refactoring can't be executed at all. However, returning an OK status doesn't guarantee that
   * the refactoring can be executed. It may still fail while performing the exhaustive precondition
   * checking done inside the methods <code>openDialog</code> or <code>perform</code>.
   * 
   * The method is mainly used to determine enable/disablement of actions.
   * 
   * @return the result of the light weight precondition checking.
   * 
   * @throws CoreException if an unexpected exception occurs while performing the checking.
   * 
   * @see #openDialog(Shell)
   * @see #perform(Shell, IRunnableContext)
   */
  public IStatus preCheck() throws CoreException {
    ensureChecked();
    if (fPreCheckStatus.hasFatalError()) {
      return fPreCheckStatus.getEntryMatchingSeverity(RefactoringStatus.FATAL).toStatus();
    } else {
      return Status.OK_STATUS;
    }
  }

  private RenameSelectionState createSelectionState() {
    RenameProcessor processor = (RenameProcessor) fRefactoring.getProcessor();
    Object[] elements = processor.getElements();
    RenameSelectionState state =
        elements.length == 1 ? new RenameSelectionState(elements[0]) : null;
    return state;
  }

  private void ensureChecked() throws CoreException {
    if (fPreCheckStatus == null) {
      if (!fRefactoring.isApplicable()) {
        fPreCheckStatus =
            RefactoringStatus.createFatalErrorStatus(DartUIMessages.RenameSupport_not_available);
      } else {
        fPreCheckStatus = new RefactoringStatus();
      }
    }
  }

  private DartRenameProcessor getDartRenameProcessor() {
    return (DartRenameProcessor) fRefactoring.getProcessor();
  }

  private void restoreSelectionState(RenameSelectionState state) throws CoreException {
    INameUpdating nameUpdating = (INameUpdating) fRefactoring.getAdapter(INameUpdating.class);
    if (nameUpdating != null && state != null) {
      Object newElement = nameUpdating.getNewElement();
      if (newElement != null) {
        state.restore(newElement);
      }
    }
  }

  private void showInformation(Shell parent, RefactoringStatus status) {
    String message = status.getMessageMatchingSeverity(RefactoringStatus.FATAL);
    MessageDialog.openInformation(parent, DartUIMessages.RenameSupport_dialog_title, message);
  }
}
