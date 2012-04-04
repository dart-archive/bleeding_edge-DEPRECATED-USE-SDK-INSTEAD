package com.google.dart.tools.internal.corext.refactoring;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartVariableDeclaration;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;
import com.google.dart.tools.ui.internal.refactoring.RenameSupport;
import com.google.dart.tools.ui.internal.refactoring.actions.RefactoringStarter;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ltk.ui.refactoring.resource.RenameResourceWizard;
import org.eclipse.swt.widgets.Shell;

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

  public static void startRenameRefactoring(final DartElement element, final Shell shell)
      throws CoreException {
    final RenameSupport support = createRenameSupport(element, null,
        RenameSupport.UPDATE_REFERENCES);
    if (support != null && support.preCheck().isOK()) {
      support.openDialog(shell);
    }
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
//
//  public static void startCleanupRefactoring(ICompilationUnit[] cus, ICleanUp[] cleanUps, boolean useOptionsFromProfile, Shell shell, boolean showWizard, String actionName) throws InvocationTargetException {
//  	final CleanUpRefactoring refactoring= new CleanUpRefactoring(actionName);
//  	for (int i= 0; i < cus.length; i++) {
//  		refactoring.addCompilationUnit(cus[i]);
//  	}
//
//  	if (!showWizard) {
//  		refactoring.setUseOptionsFromProfile(useOptionsFromProfile);
//  		for (int i= 0; i < cleanUps.length; i++) {
//  			refactoring.addCleanUp(cleanUps[i]);
//  		}
//
//  		IRunnableContext context;
//  		if (refactoring.getCleanUpTargetsSize() > 1) {
//  			context= new ProgressMonitorDialog(shell);
//  		} else {
//  			context= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//  		}
//
//  		RefactoringExecutionHelper helper= new RefactoringExecutionHelper(refactoring, IStatus.INFO, RefactoringSaveHelper.SAVE_REFACTORING, shell, context);
//  		try {
//  			helper.perform(true, true, true);
//  		} catch (InterruptedException e) {
//  		}
//  	} else {
//  		CleanUpRefactoringWizard refactoringWizard= new CleanUpRefactoringWizard(refactoring, RefactoringWizard.WIZARD_BASED_USER_INTERFACE);
//  		RefactoringStarter starter= new RefactoringStarter();
//  		starter.activate(refactoringWizard, shell, actionName, RefactoringSaveHelper.SAVE_REFACTORING);
//  	}
//  }
//
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
//
//  public static boolean startInlineMethodRefactoring(final ITypeRoot typeRoot, final CompilationUnit node, final int offset, final int length, final Shell shell) {
//  	final InlineMethodRefactoring refactoring= InlineMethodRefactoring.create(typeRoot, node, offset, length);
//  	if (refactoring != null) {
//  		new RefactoringStarter().activate(new InlineMethodWizard(refactoring), shell, RefactoringMessages.InlineMethodAction_dialog_title, RefactoringSaveHelper.SAVE_REFACTORING);
//  		return true;
//  	}
//  	return false;
//  }
//
//  public static boolean startInlineTempRefactoring(final ICompilationUnit unit, CompilationUnit node, final ITextSelection selection, final Shell shell) {
//  	final InlineTempRefactoring refactoring= new InlineTempRefactoring(unit, node, selection.getOffset(), selection.getLength());
//  	if (!refactoring.checkIfTempSelected().hasFatalError()) {
//  		new RefactoringStarter().activate(new InlineTempWizard(refactoring), shell, RefactoringMessages.InlineTempAction_inline_temp, RefactoringSaveHelper.SAVE_NOTHING);
//  		return true;
//  	}
//  	return false;
//  }
//
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

  public static void startRenameResourceRefactoring(final IResource resource, final Shell shell) {
    RenameResourceWizard wizard = new RenameResourceWizard(resource);
    new RefactoringStarter().activate(wizard, shell, wizard.getWindowTitle(),
        RefactoringSaveHelper.SAVE_ALL);
  }

  private static RenameSupport createRenameSupport(DartElement element, String newName, int flags)
      throws CoreException {
    switch (element.getElementType()) {
//      case DartElement.DART_PROJECT:
//        return RenameSupport.create((DartProject) element, newName, flags);
//        case IJavaElement.PACKAGE_FRAGMENT_ROOT:
//        	return RenameSupport.create((IPackageFragmentRoot) element, newName);
//        case IJavaElement.PACKAGE_FRAGMENT:
//        	return RenameSupport.create((IPackageFragment) element, newName, flags);
//      case DartElement.COMPILATION_UNIT:
//        return RenameSupport.create((CompilationUnit) element, newName, flags);
//      case DartElement.TYPE:
//        return RenameSupport.create((Type) element, newName, flags);
//      case DartElement.METHOD:
//        final Method method = (Method) element;
//        if (method.isConstructor()) {
//          return createRenameSupport(method.getDeclaringType(), newName, flags);
//        } else {
//          return RenameSupport.create((Method) element, newName, flags);
//        }
//      case DartElement.FIELD:
//        return RenameSupport.create((Field) element, newName, flags);
//        case IJavaElement.TYPE_PARAMETER:
//        	return RenameSupport.create((ITypeParameter) element, newName, flags);
      case DartElement.VARIABLE:
        return RenameSupport.create((DartVariableDeclaration) element, newName, flags);
    }
    return null;
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
