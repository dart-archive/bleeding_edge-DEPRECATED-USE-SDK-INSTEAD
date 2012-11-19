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
package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.core.model.DartFunctionTypeAlias;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartTypeParameter;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.internal.corext.refactoring.code.ConvertGetterToMethodRefactoring;
import com.google.dart.tools.internal.corext.refactoring.code.ConvertMethodToGetterRefactoring;
import com.google.dart.tools.internal.corext.refactoring.code.ConvertOptionalParametersToNamedRefactoring;
import com.google.dart.tools.internal.corext.refactoring.code.InlineLocalRefactoring;
import com.google.dart.tools.internal.corext.refactoring.code.InlineMethodRefactoring;
import com.google.dart.tools.ui.cleanup.ICleanUp;
import com.google.dart.tools.ui.internal.cleanup.CleanUpRefactoring;
import com.google.dart.tools.ui.internal.cleanup.CleanUpRefactoringWizard;
import com.google.dart.tools.ui.internal.refactoring.ConvertGetterToMethodWizard;
import com.google.dart.tools.ui.internal.refactoring.ConvertMethodToGetterWizard;
import com.google.dart.tools.ui.internal.refactoring.ConvertOptionalParametersToNamedWizard;
import com.google.dart.tools.ui.internal.refactoring.InlineLocalWizard;
import com.google.dart.tools.ui.internal.refactoring.InlineMethodWizard;
import com.google.dart.tools.ui.internal.refactoring.RefactoringExecutionHelper;
import com.google.dart.tools.ui.internal.refactoring.RefactoringMessages;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizard;
import org.eclipse.ltk.ui.refactoring.resource.RenameResourceWizard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import java.lang.reflect.InvocationTargetException;

/**
 * Helper class to run refactorings from action code.
 * <p>
 * This class has been introduced to decouple actions from the refactoring code, in order not to
 * eagerly load refactoring classes during action initialization.
 * </p>
 * 
 * @coverage dart.editor.ui.refactoring.core
 */
public final class RefactoringExecutionStarter {

  public static RenameSupport createRenameSupport(DartElement element, String newName, int flags)
      throws CoreException {
    switch (element.getElementType()) {
      case DartElement.IMPORT:
        return RenameSupport.create((DartImport) element, newName);
      case DartElement.FUNCTION:
        return RenameSupport.create((DartFunction) element, newName);
      case DartElement.FUNCTION_TYPE_ALIAS:
        return RenameSupport.create((DartFunctionTypeAlias) element, newName);
      case DartElement.TYPE:
        return RenameSupport.create((Type) element, newName);
      case DartElement.TYPE_PARAMETER:
        return RenameSupport.create((DartTypeParameter) element, newName);
      case DartElement.FIELD:
        return RenameSupport.create((Field) element, newName);
      case DartElement.METHOD: {
        Method method = (Method) element;
        if (method.isConstructor() && !method.getElementName().contains(".")) {
          return createRenameSupport(method.getDeclaringType(), newName, flags);
        }
        return RenameSupport.create(method, newName);
      }
      case DartElement.VARIABLE:
        return RenameSupport.create((DartVariableDeclaration) element, newName);
    }
    return null;
  }

//  public static void startChangeSignatureRefactoring(final IMethod method, final SelectionDispatchAction action, final Shell shell) throws JavaModelException {
//  	if (!RefactoringAvailabilityTester.isChangeSignatureAvailable(method))
//  		return;
//  	try {
//  		ChangeSignatureProcessor processor= new ChangeSignatureProcessor(method);
//  		RefactoringStatus status= processor.checkInitialConditions(new NullProgressMonitor());
//  		if (status.hasFatalError()) {
//  			final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
//  			if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
//  				Object element= entry.getData();
//  				if (element != null) {
//  					String message= Messages.format(RefactoringMessages.RefactoringErrorDialogUtil_okToPerformQuestion, entry.getMessage());
//  					if (MessageDialog.openQuestion(shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, message)) {
//  						IStructuredSelection selection= new StructuredSelection(element);
//  						// TODO: should not hijack this
//  						// ModifiyParametersAction.
//  						// The action is set up on an editor, but we use it
//  						// as if it were set up on a ViewPart.
//  						boolean wasEnabled= action.isEnabled();
//  						action.selectionChanged(selection);
//  						if (action.isEnabled()) {
//  							action.run(selection);
//  						} else {
//  							MessageDialog.openInformation(shell, ActionMessages.ModifyParameterAction_problem_title, ActionMessages.ModifyParameterAction_problem_message);
//  						}
//  						action.setEnabled(wasEnabled);
//  					}
//  				}
//  				return;
//  			}
//  		}
//
//  		Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  		ChangeSignatureWizard wizard= new ChangeSignatureWizard(processor, refactoring);
//  		new RefactoringStarter().activate(wizard, shell, wizard.getDefaultPageTitle(), RefactoringSaveHelper.SAVE_REFACTORING);
//  	} catch (CoreException e) {
//  		ExceptionHandler.handle(e, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringMessages.RefactoringStarter_unexpected_exception);
//  	}
//  }
//
//  public static void startChangeTypeRefactoring(final ICompilationUnit unit, final Shell shell, final int offset, final int length) {
//  	final ChangeTypeRefactoring refactoring= new ChangeTypeRefactoring(unit, offset, length);
//  	new RefactoringStarter().activate(new ChangeTypeWizard(refactoring), shell, RefactoringMessages.ChangeTypeAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }

  public static void startCleanupRefactoring(CompilationUnit[] cus, ICleanUp[] cleanUps,
      boolean useOptionsFromProfile, Shell shell, boolean showWizard, String actionName)
      throws InvocationTargetException {
    final CleanUpRefactoring refactoring = new CleanUpRefactoring(actionName);
    for (int i = 0; i < cus.length; i++) {
      refactoring.addCompilationUnit(cus[i]);
    }

    if (!showWizard) {
      refactoring.setUseOptionsFromProfile(useOptionsFromProfile);
      for (int i = 0; i < cleanUps.length; i++) {
        refactoring.addCleanUp(cleanUps[i]);
      }

      IRunnableContext context;
      if (refactoring.getCleanUpTargetsSize() > 1) {
        context = new ProgressMonitorDialog(shell);
      } else {
        context = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      }

      RefactoringExecutionHelper helper = new RefactoringExecutionHelper(
          refactoring,
          IStatus.INFO,
          RefactoringSaveHelper.SAVE_ALL,
          shell,
          context);
      try {
        helper.perform(true, true, true);
      } catch (InterruptedException e) {
      }
    } else {
      CleanUpRefactoringWizard refactoringWizard = new CleanUpRefactoringWizard(
          refactoring,
          RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
      RefactoringStarter starter = new RefactoringStarter();
      starter.activate(refactoringWizard, shell, actionName, RefactoringSaveHelper.SAVE_ALL);
    }
  }

//  public static void startConvertAnonymousRefactoring(final ICompilationUnit unit, final int offset, final int length, final Shell shell) {
//  	final ConvertAnonymousToNestedRefactoring refactoring= new ConvertAnonymousToNestedRefactoring(unit, offset, length);
//  	new RefactoringStarter().activate(new ConvertAnonymousToNestedWizard(refactoring), shell, RefactoringMessages.ConvertAnonymousToNestedAction_dialog_title,
//  			RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startCopyRefactoring(IResource[] resources, IJavaElement[] javaElements, Shell shell) throws JavaModelException {
//  	ICopyPolicy copyPolicy= ReorgPolicyFactory.createCopyPolicy(resources, javaElements);
//  	if (copyPolicy.canEnable()) {
//  		JavaCopyProcessor processor= new JavaCopyProcessor(copyPolicy);
//  		Refactoring refactoring= new CopyRefactoring(processor);
//  		RefactoringWizard wizard= new ReorgCopyWizard(processor, refactoring);
//  		processor.setNewNameQueries(new NewNameQueries(wizard));
//  		processor.setReorgQueries(new ReorgQueries(wizard));
//  		new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, processor.getSaveMode());
//  	}
//  }
//
//  public static void startCutRefactoring(final Object[] elements, final Shell shell) throws InterruptedException, InvocationTargetException {
//  	JavaDeleteProcessor processor= new JavaDeleteProcessor(elements);
//  	processor.setSuggestGetterSetterDeletion(false);
//  	processor.setQueries(new ReorgQueries(shell));
//  	Refactoring refactoring= new DeleteRefactoring(processor);
//  	int stopSeverity= RefactoringCore.getConditionCheckingFailedSeverity();
//  	new RefactoringExecutionHelper(refactoring, stopSeverity, RefactoringSaveHelper.SAVE_NOTHING, shell, new ProgressMonitorDialog(shell)).perform(false, false);
//  }
//
//  public static void startDeleteRefactoring(final Object[] elements, final Shell shell) throws CoreException {
//  	Refactoring refactoring= new DeleteRefactoring(new JavaDeleteProcessor(elements));
//  	DeleteUserInterfaceManager.getDefault().getStarter(refactoring).activate(refactoring, shell, RefactoringSaveHelper.SAVE_NOTHING);
//  }
//
//  public static void startExtractInterfaceRefactoring(final IType type, final Shell shell) {
//  	ExtractInterfaceProcessor processor= new ExtractInterfaceProcessor(type, JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject()));
//  	Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  	new RefactoringStarter().activate(new ExtractInterfaceWizard(processor, refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring,
//  			RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startExtractSupertypeRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
//  	if (!RefactoringAvailabilityTester.isExtractSupertypeAvailable(members))
//  		return;
//  	IJavaProject project= null;
//  	if (members != null && members.length > 0)
//  		project= members[0].getJavaProject();
//  	ExtractSupertypeProcessor processor= new ExtractSupertypeProcessor(members, JavaPreferencesSettings.getCodeGenerationSettings(project));
//  	Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  	ExtractSupertypeWizard wizard= new ExtractSupertypeWizard(processor, refactoring);
//  	new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startInferTypeArgumentsRefactoring(final IJavaElement[] elements, final Shell shell) {
//  	try {
//  		if (!RefactoringAvailabilityTester.isInferTypeArgumentsAvailable(elements))
//  			return;
//  		final InferTypeArgumentsRefactoring refactoring= new InferTypeArgumentsRefactoring(elements);
//  		new RefactoringStarter()
//  				.activate(new InferTypeArgumentsWizard(refactoring), shell, RefactoringMessages.InferTypeArgumentsAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  	} catch (CoreException e) {
//  		ExceptionHandler.handle(e, RefactoringMessages.InferTypeArgumentsAction_dialog_title, RefactoringMessages.OpenRefactoringWizardAction_exception);
//  	}
//  }
//
//  public static boolean startInlineConstantRefactoring(final ICompilationUnit unit, final CompilationUnit node, final int offset, final int length, final Shell shell) {
//  	final InlineConstantRefactoring refactoring= new InlineConstantRefactoring(unit, node, offset, length);
//  	if (! refactoring.checkStaticFinalConstantNameSelected().hasFatalError()) {
//  		new RefactoringStarter().activate(new InlineConstantWizard(refactoring), shell, RefactoringMessages.InlineConstantAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  		return true;
//  	}
//  	return false;
//  }

  public static boolean startConvertGetterToMethodRefactoring(DartFunction function, Shell shell) {
    Refactoring refactoring = new ConvertGetterToMethodRefactoring(function);
    new RefactoringStarter().activate(
        new ConvertGetterToMethodWizard(refactoring),
        shell,
        RefactoringMessages.ConvertGetterToMethodAction_dialog_title,
        RefactoringSaveHelper.SAVE_ALL);
    return true;
  }

  public static boolean startConvertMethodToGetterRefactoring(DartFunction function, Shell shell) {
    try {
      if (function.getParameterNames().length != 0) {
        MessageDialog.openInformation(
            shell,
            RefactoringMessages.ConvertMethodToGetterAction_dialog_title,
            RefactoringMessages.ConvertMethodToGetterAction_only_without_arguments);
        return true;
      }
      Refactoring refactoring = new ConvertMethodToGetterRefactoring(function);
      new RefactoringStarter().activate(
          new ConvertMethodToGetterWizard(refactoring),
          shell,
          RefactoringMessages.ConvertMethodToGetterAction_dialog_title,
          RefactoringSaveHelper.SAVE_ALL);
      return true;
    } catch (DartModelException e) {
    }
    return false;
  }

  public static boolean startConvertOptionalParametersToNamedRefactoring(DartFunction function,
      Shell shell) {
    try {
      if (function != null) {
        if (!RefactoringAvailabilityTester.isConvertOptionalParametersToNamedAvailable(function)) {
          MessageDialog.openInformation(
              shell,
              RefactoringMessages.ConvertOptionalParametersToNamedAction_dialog_title,
              RefactoringMessages.ConvertOptionalParametersToNamedAction_noOptionalPositional);
          return true;
        }
        Refactoring refactoring = new ConvertOptionalParametersToNamedRefactoring(function);
        new RefactoringStarter().activate(
            new ConvertOptionalParametersToNamedWizard(refactoring),
            shell,
            RefactoringMessages.ConvertOptionalParametersToNamedAction_dialog_title,
            RefactoringSaveHelper.SAVE_ALL);
        return true;
      }
    } catch (DartModelException e) {
    }
    return false;
  }

  public static boolean startInlineMethodRefactoring(CompilationUnit unit, int offset, int length,
      Shell shell) {
    try {
      DartElement[] elements = unit.codeSelect(offset, length);
      if (elements.length == 1 && elements[0] instanceof DartFunction) {
        DartFunction method = (DartFunction) elements[0];
        InlineMethodRefactoring refactoring = new InlineMethodRefactoring(method, unit, offset);
        if (refactoring != null) {
          new RefactoringStarter().activate(
              new InlineMethodWizard(refactoring),
              shell,
              RefactoringMessages.InlineMethodAction_dialog_title,
              RefactoringSaveHelper.SAVE_ALL);
          return true;
        }
      }
    } catch (DartModelException e) {
    }
    return false;
  }

  public static boolean startInlineTempRefactoring(final CompilationUnit unit, DartUnit node,
      final ITextSelection selection, final Shell shell) {
    final InlineLocalRefactoring refactoring = new InlineLocalRefactoring(
        unit,
        selection.getOffset(),
        selection.getLength());
    if (!refactoring.checkIfTempSelected().hasFatalError()) {
      new RefactoringStarter().activate(
          new InlineLocalWizard(refactoring),
          shell,
          RefactoringMessages.InlineLocalAction_dialog_title,
          RefactoringSaveHelper.SAVE_NOTHING);
      return true;
    }
    return false;
  }

//  public static void startIntroduceFactoryRefactoring(final ICompilationUnit unit, final ITextSelection selection, final Shell shell) {
//  	final IntroduceFactoryRefactoring refactoring= new IntroduceFactoryRefactoring(unit, selection.getOffset(), selection.getLength());
//  	new RefactoringStarter().activate(new IntroduceFactoryWizard(refactoring, RefactoringMessages.IntroduceFactoryAction_use_factory), shell,
//  			RefactoringMessages.IntroduceFactoryAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startIntroduceIndirectionRefactoring(final IClassFile file, final int offset, final int length, final Shell shell) {
//  	final IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring(file, offset, length);
//  	new RefactoringStarter().activate(new IntroduceIndirectionWizard(refactoring, RefactoringMessages.IntroduceIndirectionAction_dialog_title), shell,
//  			RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startIntroduceIndirectionRefactoring(final ICompilationUnit unit, final int offset, final int length, final Shell shell) {
//  	final IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring(unit, offset, length);
//  	new RefactoringStarter().activate(new IntroduceIndirectionWizard(refactoring, RefactoringMessages.IntroduceIndirectionAction_dialog_title), shell,
//  			RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startIntroduceIndirectionRefactoring(final IMethod method, final Shell shell) {
//  	final IntroduceIndirectionRefactoring refactoring= new IntroduceIndirectionRefactoring(method);
//  	new RefactoringStarter().activate(new IntroduceIndirectionWizard(refactoring, RefactoringMessages.IntroduceIndirectionAction_dialog_title), shell,
//  			RefactoringMessages.IntroduceIndirectionAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startIntroduceParameter(ICompilationUnit unit, int offset, int length, Shell shell) {
//  	final IntroduceParameterRefactoring refactoring= new IntroduceParameterRefactoring(unit, offset, length);
//  	new RefactoringStarter().activate(new IntroduceParameterWizard(refactoring), shell, RefactoringMessages.IntroduceParameterAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startMoveInnerRefactoring(final IType type, final Shell shell) throws JavaModelException {
//  	if (!RefactoringAvailabilityTester.isMoveInnerAvailable(type))
//  		return;
//  	final MoveInnerToTopRefactoring refactoring= new MoveInnerToTopRefactoring(type, JavaPreferencesSettings.getCodeGenerationSettings(type.getJavaProject()));
//  	new RefactoringStarter().activate(new MoveInnerToTopWizard(refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startMoveMethodRefactoring(final IMethod method, final Shell shell) {
//  	MoveInstanceMethodProcessor processor= new MoveInstanceMethodProcessor(method, JavaPreferencesSettings.getCodeGenerationSettings(method.getJavaProject()));
//  	Refactoring refactoring= new MoveRefactoring(processor);
//  	MoveInstanceMethodWizard wizard= new MoveInstanceMethodWizard(processor, refactoring);
//  	new RefactoringStarter().activate(wizard, shell, RefactoringMessages.MoveInstanceMethodAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startMoveRefactoring(final IResource[] resources, final IJavaElement[] elements, final Shell shell) throws JavaModelException {
//  	IMovePolicy policy= ReorgPolicyFactory.createMovePolicy(resources, elements);
//  	if (policy.canEnable()) {
//  		JavaMoveProcessor processor= new JavaMoveProcessor(policy);
//  		Refactoring refactoring= new MoveRefactoring(processor);
//  		RefactoringWizard wizard= new ReorgMoveWizard(processor, refactoring);
//  		processor.setCreateTargetQueries(new CreateTargetQueries(wizard));
//  		processor.setReorgQueries(new ReorgQueries(wizard));
//  		new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, processor.getSaveMode());
//  	}
//  }
//
//  public static void startMoveStaticMembersRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
//  	if (!RefactoringAvailabilityTester.isMoveStaticAvailable(members))
//  		return;
//  	final Set<IMember> set= new HashSet<IMember>();
//  	set.addAll(Arrays.asList(members));
//  	final IMember[] elements= set.toArray(new IMember[set.size()]);
//  	IJavaProject project= null;
//  	if (elements.length > 0)
//  		project= elements[0].getJavaProject();
//  	MoveStaticMembersProcessor processor= new MoveStaticMembersProcessor(elements, JavaPreferencesSettings.getCodeGenerationSettings(project));
//  	Refactoring refactoring= new MoveRefactoring(processor);
//  	MoveMembersWizard wizard= new MoveMembersWizard(processor, refactoring);
//  	new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startPullUpRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
//  	if (!RefactoringAvailabilityTester.isPullUpAvailable(members))
//  		return;
//  	IJavaProject project= null;
//  	if (members != null && members.length > 0)
//  		project= members[0].getJavaProject();
//  	PullUpRefactoringProcessor processor= new PullUpRefactoringProcessor(members, JavaPreferencesSettings.getCodeGenerationSettings(project));
//  	Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  	new RefactoringStarter().activate(new PullUpWizard(processor, refactoring), shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startPushDownRefactoring(final IMember[] members, final Shell shell) throws JavaModelException {
//  	if (!RefactoringAvailabilityTester.isPushDownAvailable(members))
//  		return;
//  	PushDownRefactoringProcessor processor= new PushDownRefactoringProcessor(members);
//  	Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  	PushDownWizard wizard= new PushDownWizard(processor, refactoring);
//  	new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }

  public static void startRenameRefactoring(final DartElement element, final Shell shell)
      throws CoreException {
    final RenameSupport support = createRenameSupport(
        element,
        null,
        RenameSupport.UPDATE_REFERENCES);
    if (support != null && support.preCheck().isOK()) {
      support.openDialog(shell);
    }
  }

  public static void startRenameResourceRefactoring(final IResource resource, final Shell shell) {
    RenameResourceWizard wizard = new RenameResourceWizard(resource);
    new RefactoringStarter().activate(
        wizard,
        shell,
        wizard.getWindowTitle(),
        RefactoringSaveHelper.SAVE_ALL);
  }

//  public static void startReplaceInvocationsRefactoring(final ITypeRoot typeRoot, final int offset, final int length, final Shell shell) {
//  	final ReplaceInvocationsRefactoring refactoring= new ReplaceInvocationsRefactoring(typeRoot, offset, length);
//  	new RefactoringStarter().activate(new ReplaceInvocationsWizard(refactoring), shell, RefactoringMessages.ReplaceInvocationsAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startReplaceInvocationsRefactoring(final IMethod method, final Shell shell) {
//  	final ReplaceInvocationsRefactoring refactoring= new ReplaceInvocationsRefactoring(method);
//  	new RefactoringStarter().activate(new ReplaceInvocationsWizard(refactoring), shell, RefactoringMessages.ReplaceInvocationsAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  public static void startSelfEncapsulateRefactoring(final IField field, final Shell shell) {
//  	try {
//  		if (!RefactoringAvailabilityTester.isSelfEncapsulateAvailable(field))
//  			return;
//  		final SelfEncapsulateFieldRefactoring refactoring= new SelfEncapsulateFieldRefactoring(field);
//  		new RefactoringStarter().activate(new SelfEncapsulateFieldWizard(refactoring), shell, "", RefactoringSaveHelper.SAVE_REFACTORING); //$NON-NLS-1$
//  	} catch (JavaModelException e) {
//  		ExceptionHandler.handle(e, ActionMessages.SelfEncapsulateFieldAction_dialog_title, ActionMessages.SelfEncapsulateFieldAction_dialog_cannot_perform);
//  	}
//  }
//
//  public static void startUseSupertypeRefactoring(final IType type, final Shell shell) {
//  	UseSuperTypeProcessor processor= new UseSuperTypeProcessor(type);
//  	Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  	UseSupertypeWizard wizard= new UseSupertypeWizard(processor, refactoring);
//  	new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }
//
//  private RefactoringExecutionStarter() {
//  	// Not for instantiation
//  }
//
//  public static void startIntroduceParameterObject(ICompilationUnit unit, int offset, Shell shell) throws CoreException {
//  	IJavaElement javaElement= unit.getElementAt(offset);
//  	if (javaElement instanceof IMethod) {
//  		IMethod method= (IMethod) javaElement;
//  		startIntroduceParameterObject(method, shell);
//  	}
//  }
//
//  public static void startIntroduceParameterObject(IMethod method, Shell shell) throws CoreException {
//  	RefactoringStatus availability= Checks.checkAvailability(method);
//  	if (availability.hasError()){
//  		MessageDialog.openError(shell, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_title, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_description);
//  		return;
//  	}
//  	IntroduceParameterObjectDescriptor ipod= RefactoringSignatureDescriptorFactory.createIntroduceParameterObjectDescriptor();
//  	ipod.setMethod(method);
//
//  	IntroduceParameterObjectProcessor processor= new IntroduceParameterObjectProcessor(ipod);
//
//  	final RefactoringStatus status= processor.checkInitialConditions(new NullProgressMonitor());
//  	if (status.hasFatalError()) {
//  		final RefactoringStatusEntry entry= status.getEntryMatchingSeverity(RefactoringStatus.FATAL);
//  		if (entry.getCode() == RefactoringStatusCodes.OVERRIDES_ANOTHER_METHOD || entry.getCode() == RefactoringStatusCodes.METHOD_DECLARED_IN_INTERFACE) {
//  			final Object element= entry.getData();
//  			IMethod superMethod= (IMethod) element;
//  			availability= Checks.checkAvailability(superMethod);
//  			if (availability.hasError()){
//  				MessageDialog.openError(shell, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_title, RefactoringMessages.RefactoringExecutionStarter_IntroduceParameterObject_problem_description);
//  				return;
//  			}
//  			String message= Messages.format(RefactoringMessages.RefactoringErrorDialogUtil_okToPerformQuestion, entry.getMessage());
//  			if (element != null && MessageDialog.openQuestion(shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, message)) {
//  				ipod= RefactoringSignatureDescriptorFactory.createIntroduceParameterObjectDescriptor();
//  				ipod.setMethod(superMethod);
//  				processor= new IntroduceParameterObjectProcessor(ipod);
//  			}
//  			else processor=null;
//  		}
//  	}
//  	if (processor != null) {
//  		Refactoring refactoring= new ProcessorBasedRefactoring(processor);
//  		IntroduceParameterObjectWizard wizard= new IntroduceParameterObjectWizard(processor, refactoring);
//  		new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  	}
//  }
//
//  public static void startExtractClassRefactoring(IType type, Shell shell) {
//  	ExtractClassDescriptor descriptor= RefactoringSignatureDescriptorFactory.createExtractClassDescriptor();
//  	descriptor.setType(type);
//  	ExtractClassRefactoring refactoring= new ExtractClassRefactoring(descriptor);
//  	ExtractClassWizard wizard= new ExtractClassWizard(descriptor, refactoring);
//  	new RefactoringStarter().activate(wizard, shell, RefactoringMessages.OpenRefactoringWizardAction_refactoring, RefactoringSaveHelper.SAVE_REFACTORING);
//  }

}
