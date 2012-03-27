package com.google.dart.tools.ui.internal.text.correction;

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;

/**
 * Handler to be used to run a quick fix or assist by keyboard shortcut
 * <p>
 * TODO(scheglov) restore this class, make it not abstract
 */
public class CorrectionCommandHandler extends AbstractHandler {

  public static String getShortCutString(String proposalId) {
    if (proposalId != null) {
      IBindingService bindingService = (IBindingService) PlatformUI.getWorkbench().getAdapter(
          IBindingService.class);
      if (bindingService != null) {
        TriggerSequence[] activeBindingsFor = bindingService.getActiveBindingsFor(proposalId);
        if (activeBindingsFor.length > 0) {
          return activeBindingsFor[0].format();
        }
      }
    }
    return null;
  }

  private final DartEditor fEditor;
  private final String fId;

  private final boolean fIsAssist;

  public CorrectionCommandHandler(DartEditor editor, String id, boolean isAssist) {
    fEditor = editor;
    fId = id;
    fIsAssist = isAssist;
  }

  /**
   * Try to execute the correction command.
   * 
   * @return <code>true</code> iff the correction could be started
   * @since 3.6
   */
  public boolean doExecute() {
    // TODO(scheglov) restore this
//    ISelection selection = fEditor.getSelectionProvider().getSelection();
//    CompilationUnit cu = DartUI.getWorkingCopyManager().getWorkingCopy(fEditor.getEditorInput());
//    IAnnotationModel model = DartUI.getDocumentProvider().getAnnotationModel(
//        fEditor.getEditorInput());
//    if (selection instanceof ITextSelection && cu != null && model != null) {
//      if (!ActionUtil.isEditable(fEditor)) {
//        return false;
//      }
//      ICompletionProposal proposal = findCorrection(fId, fIsAssist, (ITextSelection) selection, cu,
//          model);
//      if (proposal != null) {
//        invokeProposal(proposal, ((ITextSelection) selection).getOffset());
//        return true;
//      }
//    }
    return false;
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    doExecute();
    return null;
  }

  private ICompletionProposal findCorrection(String id, boolean isAssist, ITextSelection selection,
      CompilationUnit cu, IAnnotationModel model) {
    // TODO(scheglov) restore this
//    AssistContext context = new AssistContext(cu, fEditor.getViewer(), fEditor,
//        selection.getOffset(), selection.getLength());
//    Collection<DartCompletionProposal> proposals = new ArrayList<DartCompletionProposal>(10);
//    if (isAssist) {
//      if (id.equals(LinkedNamesAssistProposal.ASSIST_ID)) {
//        return getLocalRenameProposal(context); // shortcut for local rename
//      }
//      DartCorrectionProcessor.collectAssists(context, new ProblemLocation[0], proposals);
//    } else {
//      try {
//        boolean goToClosest = selection.getLength() == 0;
//        Annotation[] annotations = getAnnotations(selection.getOffset(), goToClosest);
//        DartCorrectionProcessor.collectProposals(context, model, annotations, true, false,
//            proposals);
//      } catch (BadLocationException e) {
//        return null;
//      }
//    }
//    for (Iterator<DartCompletionProposal> iter = proposals.iterator(); iter.hasNext();) {
//      Object curr = iter.next();
//      if (curr instanceof ICommandAccess) {
//        if (id.equals(((ICommandAccess) curr).getCommandId())) {
//          return (ICompletionProposal) curr;
//        }
//      }
//    }
    return null;
  }

//  private Annotation[] getAnnotations(int offset, boolean goToClosest) throws BadLocationException {
//    ArrayList<Annotation> resultingAnnotations = new ArrayList<Annotation>();
//    DartCorrectionAssistant.collectQuickFixableAnnotations(fEditor, offset, goToClosest,
//        resultingAnnotations);
//    return resultingAnnotations.toArray(new Annotation[resultingAnnotations.size()]);
//  }
//
//  private IDocument getDocument() {
//    return DartUI.getDocumentProvider().getDocument(fEditor.getEditorInput());
//  }
//
//  private ICompletionProposal getLocalRenameProposal(IInvocationContext context) {
//    DartNode node = context.getCoveringNode();
//    if (node instanceof DartIdentifier) {
//      return new LinkedNamesAssistProposal(context, (DartIdentifier) node);
//    }
//    return null;
//  }
//
//  private void invokeProposal(ICompletionProposal proposal, int offset) {
//    if (proposal instanceof ICompletionProposalExtension2) {
//      ITextViewer viewer = fEditor.getViewer();
//      if (viewer != null) {
//        ((ICompletionProposalExtension2) proposal).apply(viewer, (char) 0, 0, offset);
//        return;
//      }
//    } else if (proposal instanceof ICompletionProposalExtension) {
//      IDocument document = getDocument();
//      if (document != null) {
//        ((ICompletionProposalExtension) proposal).apply(document, (char) 0, offset);
//        return;
//      }
//    }
//    IDocument document = getDocument();
//    if (document != null) {
//      proposal.apply(document);
//    }
//  }

}
