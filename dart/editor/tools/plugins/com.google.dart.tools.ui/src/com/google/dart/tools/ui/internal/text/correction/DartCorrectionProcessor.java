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

import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import java.util.Collection;

public class DartCorrectionProcessor implements
    org.eclipse.jface.text.quickassist.IQuickAssistProcessor {

//  private static class SafeAssistCollector extends SafeCorrectionProcessorAccess {
//    private final IInvocationContext fContext;
//    private final IProblemLocation[] fLocations;
//    private final Collection<IDartCompletionProposal> fProposals;
//
//    public SafeAssistCollector(IInvocationContext context, IProblemLocation[] locations,
//        Collection<DartCompletionProposal> proposals) {
//      fContext = context;
//      fLocations = locations;
//      fProposals = proposals;
//    }
//
//    @Override
//    public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
//      IQuickAssistProcessor curr = (IQuickAssistProcessor) desc.getProcessor(
//          fContext.getCompilationUnit(), IQuickAssistProcessor.class);
//      if (curr != null) {
//        DartCompletionProposal[] res = curr.getAssists(fContext, fLocations);
//        if (res != null) {
//          for (int k = 0; k < res.length; k++) {
//            fProposals.add(res[k]);
//          }
//        }
//      }
//    }
//  }
//  private static class SafeCorrectionCollector extends SafeCorrectionProcessorAccess {
//    private final IInvocationContext fContext;
//    private final Collection<DartCompletionProposal> fProposals;
//    private IProblemLocation[] fLocations;
//
//    public SafeCorrectionCollector(IInvocationContext context,
//        Collection<DartCompletionProposal> proposals) {
//      fContext = context;
//      fProposals = proposals;
//    }
//
//    @Override
//    public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
//      IQuickFixProcessor curr = (IQuickFixProcessor) desc.getProcessor(
//          fContext.getCompilationUnit(), IQuickFixProcessor.class);
//      if (curr != null) {
//        DartCompletionProposal[] res = curr.getCorrections(fContext, fLocations);
//        if (res != null) {
//          for (int k = 0; k < res.length; k++) {
//            fProposals.add(res[k]);
//          }
//        }
//      }
//    }
//
//    public void setProblemLocations(IProblemLocation[] locations) {
//      fLocations = locations;
//    }
//  }
//
//  private static abstract class SafeCorrectionProcessorAccess implements ISafeRunnable {
//    private MultiStatus fMulti = null;
//    private ContributedProcessorDescriptor fDescriptor;
//
//    public IStatus getStatus() {
//      if (fMulti == null) {
//        return Status.OK_STATUS;
//      }
//      return fMulti;
//    }
//
//    @Override
//    public void handleException(Throwable exception) {
//      if (fMulti == null) {
//        fMulti = new MultiStatus(DartUI.ID_PLUGIN, IStatus.OK,
//            CorrectionMessages.JavaCorrectionProcessor_error_status, null);
//      }
//      fMulti.merge(new Status(IStatus.ERROR, DartUI.ID_PLUGIN, IStatus.ERROR,
//          CorrectionMessages.JavaCorrectionProcessor_error_status, exception));
//    }
//
//    public void process(ContributedProcessorDescriptor desc) {
//      fDescriptor = desc;
//      SafeRunner.run(this);
//    }
//
//    public void process(ContributedProcessorDescriptor[] desc) {
//      for (int i = 0; i < desc.length; i++) {
//        fDescriptor = desc[i];
//        SafeRunner.run(this);
//      }
//    }
//
//    @Override
//    public void run() throws Exception {
//      safeRun(fDescriptor);
//    }
//
//    protected abstract void safeRun(ContributedProcessorDescriptor processor) throws Exception;
//
//  }
//  private static class SafeHasAssist extends SafeCorrectionProcessorAccess {
//    private final IInvocationContext fContext;
//    private boolean fHasAssists;
//
//    public SafeHasAssist(IInvocationContext context) {
//      fContext = context;
//      fHasAssists = false;
//    }
//
//    public boolean hasAssists() {
//      return fHasAssists;
//    }
//
//    @Override
//    public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
//      IQuickAssistProcessor processor = (IQuickAssistProcessor) desc.getProcessor(
//          fContext.getCompilationUnit(), IQuickAssistProcessor.class);
//      if (processor != null && processor.hasAssists(fContext)) {
//        fHasAssists = true;
//      }
//    }
//  }
//
//  private static class SafeHasCorrections extends SafeCorrectionProcessorAccess {
//    private final CompilationUnit fCu;
//    private final int fProblemId;
//    private boolean fHasCorrections;
//
//    public SafeHasCorrections(CompilationUnit cu, int problemId) {
//      fCu = cu;
//      fProblemId = problemId;
//      fHasCorrections = false;
//    }
//
//    public boolean hasCorrections() {
//      return fHasCorrections;
//    }
//
//    @Override
//    public void safeRun(ContributedProcessorDescriptor desc) throws Exception {
//      IQuickFixProcessor processor = (IQuickFixProcessor) desc.getProcessor(fCu,
//          IQuickFixProcessor.class);
//      if (processor != null && processor.hasCorrections(fCu, fProblemId)) {
//        fHasCorrections = true;
//      }
//    }
//  }
//
//  private static final String QUICKFIX_PROCESSOR_CONTRIBUTION_ID = "quickFixProcessors"; //$NON-NLS-1$
//
//  private static final String QUICKASSIST_PROCESSOR_CONTRIBUTION_ID = "quickAssistProcessors"; //$NON-NLS-1$
//
//  private static ContributedProcessorDescriptor[] fgContributedAssistProcessors = null;
//
//  private static ContributedProcessorDescriptor[] fgContributedCorrectionProcessors = null;
//
//  public static IStatus collectAssists(IInvocationContext context, IProblemLocation[] locations,
//      Collection<DartCompletionProposal> proposals) {
//    ContributedProcessorDescriptor[] processors = getAssistProcessors();
//    SafeAssistCollector collector = new SafeAssistCollector(context, locations, proposals);
//    collector.process(processors);
//
//    return collector.getStatus();
//  }
//
//  public static IStatus collectCorrections(IInvocationContext context,
//      IProblemLocation[] locations, Collection<DartCompletionProposal> proposals) {
//    ContributedProcessorDescriptor[] processors = getCorrectionProcessors();
//    SafeCorrectionCollector collector = new SafeCorrectionCollector(context, proposals);
//    for (int i = 0; i < processors.length; i++) {
//      ContributedProcessorDescriptor curr = processors[i];
//      IProblemLocation[] handled = getHandledProblems(locations, curr);
//      if (handled != null) {
//        collector.setProblemLocations(handled);
//        collector.process(curr);
//      }
//    }
//    return collector.getStatus();
//  }

  public static IStatus collectProposals(IInvocationContext context, IAnnotationModel model,
      Annotation[] annotations, boolean addQuickFixes, boolean addQuickAssists,
      Collection<DartCompletionProposal> proposals) {
    // TODO(scheglov) restore later
//    ArrayList<ProblemLocation> problems = new ArrayList<ProblemLocation>();
//
//    // collect problem locations and corrections from marker annotations
//    for (int i = 0; i < annotations.length; i++) {
//      Annotation curr = annotations[i];
//      ProblemLocation problemLocation = null;
//      if (curr instanceof IDartAnnotation) {
//        problemLocation = getProblemLocation((IDartAnnotation) curr, model);
//        if (problemLocation != null) {
//          problems.add(problemLocation);
//        }
//      }
//      if (problemLocation == null && addQuickFixes && curr instanceof SimpleMarkerAnnotation) {
//        collectMarkerProposals((SimpleMarkerAnnotation) curr, proposals);
//      }
//    }
//    MultiStatus resStatus = null;
//
//    IProblemLocation[] problemLocations = problems.toArray(new IProblemLocation[problems.size()]);
//    if (addQuickFixes) {
//      IStatus status = collectCorrections(context, problemLocations, proposals);
//      if (!status.isOK()) {
//        resStatus = new MultiStatus(DartUI.ID_PLUGIN, IStatus.ERROR,
//            CorrectionMessages.JavaCorrectionProcessor_error_quickfix_message, null);
//        resStatus.add(status);
//      }
//    }
//    if (addQuickAssists) {
//      IStatus status = collectAssists(context, problemLocations, proposals);
//      if (!status.isOK()) {
//        if (resStatus == null) {
//          resStatus = new MultiStatus(DartUI.ID_PLUGIN, IStatus.ERROR,
//              CorrectionMessages.JavaCorrectionProcessor_error_quickassist_message, null);
//        }
//        resStatus.add(status);
//      }
//    }
//    if (resStatus != null) {
//      return resStatus;
//    }
    return Status.OK_STATUS;
  }

//  public static boolean hasAssists(IInvocationContext context) {
//    ContributedProcessorDescriptor[] processors = getAssistProcessors();
//    SafeHasAssist collector = new SafeHasAssist(context);
//
//    for (int i = 0; i < processors.length; i++) {
//      collector.process(processors[i]);
//      if (collector.hasAssists()) {
//        return true;
//      }
//    }
//    return false;
//  }
//
//  public static boolean hasCorrections(Annotation annotation) {
//    if (annotation instanceof IDartAnnotation) {
//      IDartAnnotation dartAnnotation = (IDartAnnotation) annotation;
//      int problemId = dartAnnotation.getId();
//      if (problemId != -1) {
//        CompilationUnit cu = dartAnnotation.getCompilationUnit();
//        if (cu != null) {
//          return hasCorrections(cu, problemId, dartAnnotation.getMarkerType());
//        }
//      }
//    }
//    if (annotation instanceof SimpleMarkerAnnotation) {
//      return hasCorrections(((SimpleMarkerAnnotation) annotation).getMarker());
//    }
//    return false;
//  }
//
//  public static boolean hasCorrections(CompilationUnit cu, int problemId, String markerType) {
//    ContributedProcessorDescriptor[] processors = getCorrectionProcessors();
//    SafeHasCorrections collector = new SafeHasCorrections(cu, problemId);
//    for (int i = 0; i < processors.length; i++) {
//      if (processors[i].canHandleMarkerType(markerType)) {
//        collector.process(processors[i]);
//        if (collector.hasCorrections()) {
//          return true;
//        }
//      }
//    }
//    return false;
//  }
//
//  public static boolean isQuickFixableType(Annotation annotation) {
//    return (annotation instanceof IDartAnnotation || annotation instanceof SimpleMarkerAnnotation)
//        && !annotation.isMarkedDeleted();
//  }
//
//  private static void collectMarkerProposals(SimpleMarkerAnnotation annotation,
//      Collection<DartCompletionProposal> proposals) {
//    IMarker marker = annotation.getMarker();
//    IMarkerResolution[] res = IDE.getMarkerHelpRegistry().getResolutions(marker);
//    if (res.length > 0) {
//      for (int i = 0; i < res.length; i++) {
//        proposals.add(new MarkerResolutionProposal(res[i], marker));
//      }
//    }
//  }
//
//  private static ContributedProcessorDescriptor[] getAssistProcessors() {
//    if (fgContributedAssistProcessors == null) {
//      fgContributedAssistProcessors = getProcessorDescriptors(
//          QUICKASSIST_PROCESSOR_CONTRIBUTION_ID, false);
//    }
//    return fgContributedAssistProcessors;
//  }
//
//  private static ContributedProcessorDescriptor[] getCorrectionProcessors() {
//    if (fgContributedCorrectionProcessors == null) {
//      fgContributedCorrectionProcessors = getProcessorDescriptors(
//          QUICKFIX_PROCESSOR_CONTRIBUTION_ID, true);
//    }
//    return fgContributedCorrectionProcessors;
//  }
//
//  private static IProblemLocation[] getHandledProblems(IProblemLocation[] locations,
//      ContributedProcessorDescriptor processor) {
//    // implementation tries to avoid creating a new array
//    boolean allHandled = true;
//    ArrayList<IProblemLocation> res = null;
//    for (int i = 0; i < locations.length; i++) {
//      IProblemLocation curr = locations[i];
//      if (processor.canHandleMarkerType(curr.getMarkerType())) {
//        if (!allHandled) { // first handled problem
//          if (res == null) {
//            res = new ArrayList<IProblemLocation>(locations.length - i);
//          }
//          res.add(curr);
//        }
//      } else if (allHandled) {
//        if (i > 0) { // first non handled problem
//          res = new ArrayList<IProblemLocation>(locations.length - i);
//          for (int k = 0; k < i; k++) {
//            res.add(locations[k]);
//          }
//        }
//        allHandled = false;
//      }
//    }
//    if (allHandled) {
//      return locations;
//    }
//    if (res == null) {
//      return null;
//    }
//    return res.toArray(new IProblemLocation[res.size()]);
//  }
//
//  private static ProblemLocation getProblemLocation(IDartAnnotation dartAnnotation,
//      IAnnotationModel model) {
//    int problemId = dartAnnotation.getId();
//    if (problemId != -1) {
//      Position pos = model.getPosition((Annotation) dartAnnotation);
//      if (pos != null) {
//        return new ProblemLocation(pos.getOffset(), pos.getLength(), dartAnnotation); // Dart problems all handled by the quick assist processors
//      }
//    }
//    return null;
//  }
//
//  private static ContributedProcessorDescriptor[] getProcessorDescriptors(String contributionId,
//      boolean testMarkerTypes) {
//    IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(
//        DartUI.ID_PLUGIN, contributionId);
//    ArrayList<ContributedProcessorDescriptor> res = new ArrayList<ContributedProcessorDescriptor>(
//        elements.length);
//
//    for (int i = 0; i < elements.length; i++) {
//      ContributedProcessorDescriptor desc = new ContributedProcessorDescriptor(elements[i],
//          testMarkerTypes);
//      IStatus status = desc.checkSyntax();
//      if (status.isOK()) {
//        res.add(desc);
//      } else {
//        DartToolsPlugin.log(status);
//      }
//    }
//    return res.toArray(new ContributedProcessorDescriptor[res.size()]);
//  }
//
//  private static boolean hasCorrections(IMarker marker) {
//    if (marker == null || !marker.exists()) {
//      return false;
//    }
//
//    IMarkerHelpRegistry registry = IDE.getMarkerHelpRegistry();
//    return registry != null && registry.hasResolutions(marker);
//  }
//
  private DartCorrectionAssistant fAssistant;

  private String fErrorMessage;

  public DartCorrectionProcessor(DartCorrectionAssistant assistant) {
    fAssistant = assistant;
    fAssistant.addCompletionListener(new ICompletionListener() {

      @Override
      public void assistSessionEnded(ContentAssistEvent event) {
        fAssistant.setStatusLineVisible(false);
      }

      @Override
      public void assistSessionStarted(ContentAssistEvent event) {
        fAssistant.setStatusLineVisible(true);
        fAssistant.setStatusMessage(getJumpHintStatusLineMessage());
      }

      @Override
      public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
        // TODO(scheglov) restore this later
//        if (proposal instanceof IStatusLineProposal) {
//          IStatusLineProposal statusLineProposal = (IStatusLineProposal) proposal;
//          String message = statusLineProposal.getStatusMessage();
//          if (message != null) {
//            fAssistant.setStatusMessage(message);
//            return;
//          }
//        }
        fAssistant.setStatusMessage(getJumpHintStatusLineMessage());
      }

      private String getJumpHintStatusLineMessage() {
        if (fAssistant.isUpdatedOffset()) {
          String key = getQuickAssistBinding();
          if (key == null) {
            return CorrectionMessages.JavaCorrectionProcessor_go_to_original_using_menu;
          } else {
            return Messages.format(
                CorrectionMessages.JavaCorrectionProcessor_go_to_original_using_key, key);
          }
        } else if (fAssistant.isProblemLocationAvailable()) {
          String key = getQuickAssistBinding();
          if (key == null) {
            return CorrectionMessages.JavaCorrectionProcessor_go_to_closest_using_menu;
          } else {
            return Messages.format(
                CorrectionMessages.JavaCorrectionProcessor_go_to_closest_using_key, key);
          }
        } else {
          return ""; //$NON-NLS-1$
        }
      }

      private String getQuickAssistBinding() {
        final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(
            IBindingService.class);
        return bindingSvc.getBestActiveBindingFormattedFor(ITextEditorActionDefinitionIds.QUICK_ASSIST);
      }
    });
  }

  /*
   * @see org.eclipse.jface.text.quickassist.IQuickAssistProcessor#canAssist(org.eclipse.jface.text.
   * quickassist.IQuickAssistInvocationContext)
   * 
   * @since 3.2
   */
  @Override
  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    // TODO(scheglov) restore later
//    if (invocationContext instanceof IInvocationContext) {
//      return hasAssists((IInvocationContext) invocationContext);
//    }
    return false;
  }

  /*
   * @see
   * org.eclipse.jface.text.quickassist.IQuickAssistProcessor#canFix(org.eclipse.jface.text.source
   * .Annotation)
   * 
   * @since 3.2
   */
  @Override
  public boolean canFix(Annotation annotation) {
    // TODO(scheglov) restore later
    return false;
//    return hasCorrections(annotation);
  }

  /*
   * @see IContentAssistProcessor#computeCompletionProposals(ITextViewer, int)
   */
  @Override
  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext quickAssistContext) {
    // TODO(scheglov) restore later
    return new ICompletionProposal[0];
//    ISourceViewer viewer = quickAssistContext.getSourceViewer();
//    int documentOffset = quickAssistContext.getOffset();
//
//    IEditorPart part = fAssistant.getEditor();
//
//    CompilationUnit cu = DartUI.getWorkingCopyManager().getWorkingCopy(part.getEditorInput());
//    IAnnotationModel model = DartUI.getDocumentProvider().getAnnotationModel(part.getEditorInput());
//
//    AssistContext context = null;
//    if (cu != null) {
//      int length = viewer != null ? viewer.getSelectedRange().y : 0;
//      context = new AssistContext(cu, viewer, part, documentOffset, length);
//    }
//
//    Annotation[] annotations = fAssistant.getAnnotationsAtOffset();
//
//    fErrorMessage = null;
//
//    ICompletionProposal[] res = null;
//    if (model != null && context != null && annotations != null) {
//      ArrayList<DartCompletionProposal> proposals = new ArrayList<DartCompletionProposal>(10);
//      IStatus status = collectProposals(context, model, annotations, true,
//          !fAssistant.isUpdatedOffset(), proposals);
//      res = proposals.toArray(new ICompletionProposal[proposals.size()]);
//      if (!status.isOK()) {
//        fErrorMessage = status.getMessage();
//        DartToolsPlugin.log(status);
//      }
//    }
//
//    if (res == null || res.length == 0) {
//      return new ICompletionProposal[] {new ChangeCorrectionProposal(
//          CorrectionMessages.NoCorrectionProposal_description, new NullChange(""), 0, null)}; //$NON-NLS-1$
//    }
//    if (res.length > 1) {
//      Arrays.sort(res, new CompletionProposalComparator());
//    }
//    return res;
  }

  /*
   * @see IContentAssistProcessor#getErrorMessage()
   */
  @Override
  public String getErrorMessage() {
    return fErrorMessage;
  }

}
