package com.google.dart.tools.internal.corext.refactoring.reorg;

import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.internal.corext.refactoring.DartRefactoringArguments;
import com.google.dart.tools.ui.internal.refactoring.RefactoringSaveHelper;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ltk.core.refactoring.ChangeDescriptor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;

public interface IReorgPolicy extends IReorgDestinationValidator {

//  public static interface ICopyPolicy extends IReorgPolicy {
//    public Change createChange(IProgressMonitor monitor, INewNameQueries queries)
//        throws DartModelException;
//
//    public ReorgExecutionLog getReorgExecutionLog();
//  }
//
//  public static interface IMovePolicy extends IReferenceUpdating, IQualifiedNameUpdating,
//      IReorgPolicy {
//    /**
//     * Checks if <b>Java</b> references to the selected element(s) can be updated if moved to the
//     * selected destination. Even if <code>false</code>, participants could still update non-Java
//     * references.
//     * 
//     * @return <code>true</code> iff <b>Java</b> references to the moved element can be updated
//     */
//    public boolean canUpdateJavaReferences();
//
//    public boolean canUpdateQualifiedNames();
//
//    public Change createChange(IProgressMonitor monitor) throws DartModelException;
//
//    public CreateTargetExecutionLog getCreateTargetExecutionLog();
//
//    public ICreateTargetQuery getCreateTargetQuery(ICreateTargetQueries createQueries);
//
//    public boolean hasAllInputSet();
//
//    public boolean isTextualMove();
//
//    public Change postCreateChange(Change[] participantChanges, IProgressMonitor monitor)
//        throws CoreException;
//
//    public void setDestinationCheck(boolean check);
//  }

  /**
   * @return true if this policy can handle the source elements
   * @throws DartModelException in unexpected cases
   */
  public boolean canEnable() throws DartModelException;

  public RefactoringStatus checkFinalConditions(IProgressMonitor monitor,
      CheckConditionsContext context, IReorgQueries queries) throws CoreException;

  /**
   * @return the destination of this reorg or null if not a Dart element
   */
  public DartElement getDartElementDestination();

  /**
   * @return the source Dart elements to reorg
   */
  public DartElement[] getDartElements();

  /**
   * @return a descriptor describing a reorg from source to target
   */
  public ChangeDescriptor getDescriptor();

  /**
   * @return the unique id of this policy
   */
  public String getPolicyId();

  /**
   * @return the destination of this reorg or null if not a resource
   */
  public IResource getResourceDestination();

  /**
   * @return the source resources to reorg
   */
  public IResource[] getResources();

  /**
   * @return the save mode required for this reorg policy
   * @see RefactoringSaveHelper
   */
  public int getSaveMode();

  /**
   * Initializes the reorg policy with arguments from a script.
   * 
   * @param arguments the arguments
   * @return an object describing the status of the initialization. If the status has severity
   *         <code>FATAL_ERROR</code>, the refactoring will not be executed.
   */
  public RefactoringStatus initialize(DartRefactoringArguments arguments);

  public RefactoringParticipant[] loadParticipants(RefactoringStatus status,
      RefactoringProcessor processor, String[] natures, SharableParticipants shared)
      throws CoreException;

  /**
   * @param destination the destination for this reorg
   */
  public void setDestination(IReorgDestination destination);

  /**
   * Can destination be a target for the given source elements?
   * 
   * @param destination the destination to verify
   * @return OK status if valid destination
   * @throws DartModelException in unexpected cases
   */
  public RefactoringStatus verifyDestination(IReorgDestination destination)
      throws DartModelException;
}
