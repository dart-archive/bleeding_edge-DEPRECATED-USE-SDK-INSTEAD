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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.engine.utilities.instrumentation.Instrumentation;
import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.dialogs.OptionalMessageDialog;
import com.google.dart.tools.ui.text.DartPartitions;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.LegacyActionTools;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * A content assist processor that aggregates the proposals of the
 * {@link com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer}s contributed via the
 * <code>com.google.dart.tools.ui.javaCompletionProposalComputer</code> extension point.
 * <p>
 * Subclasses may extend:
 * <ul>
 * <li><code>createContext</code> to provide the context object passed to the computers</li>
 * <li><code>createProgressMonitor</code> to change the way progress is reported</li>
 * <li><code>filterAndSort</code> to add sorting and filtering</li>
 * <li><code>getContextInformationValidator</code> to add context validation (needed if any contexts
 * are provided)</li>
 * <li><code>getErrorMessage</code> to change error reporting</li>
 * </ul>
 * </p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ContentAssistProcessor implements IContentAssistProcessor {

  private static final boolean DEBUG = DartCoreDebug.ENABLE_CONTENT_ASSIST_TIMING;

  /**
   * Dialog settings key for the "all categories are disabled" warning dialog. See
   * {@link OptionalMessageDialog}.
   */
  private static final String PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY = "EmptyDefaultAssistCategory"; //$NON-NLS-1$

  private static final Comparator ORDER_COMPARATOR = new Comparator() {

    @Override
    public int compare(Object o1, Object o2) {
      CompletionProposalCategory d1 = (CompletionProposalCategory) o1;
      CompletionProposalCategory d2 = (CompletionProposalCategory) o2;

      return d1.getSortOrder() - d2.getSortOrder();
    }

  };

  private final List fCategories;
  private final String fPartition;
  private final ContentAssistant fAssistant;

  private char[] fCompletionAutoActivationCharacters;

  /* cycling stuff */
  private int fRepetition = -1;
  private List<List<CompletionProposalCategory>> fCategoryIteration = null;
  private String fIterationGesture = null;
  private int fNumberOfComputedResults = 0;
  private String fErrorMessage;

  public ContentAssistProcessor(ContentAssistant assistant, String partition) {
    Assert.isNotNull(partition);
    Assert.isNotNull(assistant);
    fPartition = partition;
    fCategories = CompletionProposalComputerRegistry.getDefault().getProposalCategories();
    fAssistant = assistant;
    fAssistant.addCompletionListener(new ICompletionListener() {

      /*
       * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionEnded
       * (org.eclipse.jface.text.contentassist.ContentAssistEvent)
       */
      @Override
      public void assistSessionEnded(ContentAssistEvent event) {
        if (event.processor != ContentAssistProcessor.this) {
          return;
        }

        for (Iterator it = fCategories.iterator(); it.hasNext();) {
          CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
          cat.sessionEnded();
        }

        fCategoryIteration = null;
        fRepetition = -1;
        fIterationGesture = null;
        if (event.assistant instanceof IContentAssistantExtension2) {
          IContentAssistantExtension2 extension = (IContentAssistantExtension2) event.assistant;
          extension.setShowEmptyList(false);
          extension.setRepeatedInvocationMode(false);
          extension.setStatusLineVisible(false);
          if (extension instanceof IContentAssistantExtension3) {
            IContentAssistantExtension3 ext3 = (IContentAssistantExtension3) extension;
            ((ContentAssistant) ext3).setRepeatedInvocationTrigger(null);
          }
        }
      }

      /*
       * @see org.eclipse.jface.text.contentassist.ICompletionListener# assistSessionStarted
       * (org.eclipse.jface.text.contentassist.ContentAssistEvent)
       */
      @Override
      public void assistSessionStarted(ContentAssistEvent event) {
        if (event.processor != ContentAssistProcessor.this) {
          return;
        }

        fIterationGesture = getIterationGesture();
        KeySequence binding = getIterationBinding();

        // this may show the warning dialog if all categories are disabled
        fCategoryIteration = getCategoryIteration();
        for (Iterator it = fCategories.iterator(); it.hasNext();) {
          CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
          cat.sessionStarted();
        }

        fRepetition = 0;
        if (event.assistant instanceof IContentAssistantExtension2) {
          IContentAssistantExtension2 extension = (IContentAssistantExtension2) event.assistant;

          if (fCategoryIteration.size() == 1) {
            extension.setRepeatedInvocationMode(false);
            extension.setShowEmptyList(false);
          } else {
            extension.setRepeatedInvocationMode(true);
            extension.setStatusLineVisible(true);
            extension.setStatusMessage(createIterationMessage());
            extension.setShowEmptyList(true);
            if (extension instanceof IContentAssistantExtension3) {
              IContentAssistantExtension3 ext3 = (IContentAssistantExtension3) extension;
              ((ContentAssistant) ext3).setRepeatedInvocationTrigger(binding);
            }
          }

        }
      }

      /*
       * @see org.eclipse.jface.text.contentassist.ICompletionListener#selectionChanged
       * (org.eclipse.jface.text.contentassist.ICompletionProposal, boolean)
       */
      @Override
      public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
      }

    });
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
   */
  @Override
  public final ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    long start = DEBUG ? System.currentTimeMillis() : 0;

    clearState();

    IProgressMonitor monitor = createProgressMonitor();
    monitor.beginTask(
        DartTextMessages.ContentAssistProcessor_computing_proposals,
        fCategories.size() + 1);

    ContentAssistInvocationContext context = createContext(viewer, offset);
    long setup = DEBUG ? System.currentTimeMillis() : 0;

    monitor.subTask(DartTextMessages.ContentAssistProcessor_collecting_proposals);
    List proposals = collectProposals(viewer, offset, monitor, context);
    long collect = DEBUG ? System.currentTimeMillis() : 0;

    monitor.subTask(DartTextMessages.ContentAssistProcessor_sorting_proposals);
    List filtered = filterAndSortProposals(proposals, monitor, context);
    fNumberOfComputedResults = filtered.size();
    long filter = DEBUG ? System.currentTimeMillis() : 0;

    ICompletionProposal[] result = (ICompletionProposal[]) filtered.toArray(new ICompletionProposal[filtered.size()]);
    monitor.done();

    if (DEBUG) {
      // TODO Present perf stats somewhere more accessible
      // than <workspace>/.metadata/.log
      System.err.println("Code Assist Stats (" + result.length + " proposals)"); //$NON-NLS-1$ //$NON-NLS-2$
      System.err.println("Code Assist (setup):\t" + (setup - start)); //$NON-NLS-1$
      System.err.println("Code Assist (collect):\t" + (collect - setup)); //$NON-NLS-1$
      System.err.println("Code Assist (sort):\t" + (filter - collect)); //$NON-NLS-1$
//      Logger.log(Logger.INFO, "Code Assist Stats (" + result.length + " proposals)");
//      Logger.log(Logger.INFO, "Code Assist (collect):\t" + (collect - setup) + "ms");
    }

    return result;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
   */
  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    clearState();

    IProgressMonitor monitor = createProgressMonitor();
    monitor.beginTask(
        DartTextMessages.ContentAssistProcessor_computing_contexts,
        fCategories.size() + 1);

    monitor.subTask(DartTextMessages.ContentAssistProcessor_collecting_contexts);
    List proposals = collectContextInformation(viewer, offset, monitor);

    monitor.subTask(DartTextMessages.ContentAssistProcessor_sorting_contexts);
    List filtered = filterAndSortContextInformation(proposals, monitor);
    fNumberOfComputedResults = filtered.size();

    IContextInformation[] result = (IContextInformation[]) filtered.toArray(new IContextInformation[filtered.size()]);
    monitor.done();
    return result;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getCompletionProposalAutoActivationCharacters()
   */
  @Override
  public final char[] getCompletionProposalAutoActivationCharacters() {
    return fCompletionAutoActivationCharacters;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getContextInformationAutoActivationCharacters()
   */
  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#
   * getContextInformationValidator()
   */
  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage ()
   */
  @Override
  public String getErrorMessage() {
    if (fNumberOfComputedResults > 0) {
      return null;
    }
    if (fErrorMessage != null) {
      return fErrorMessage;
    }
    return DartUIMessages.JavaEditor_codeassist_noCompletions;
  }

  /**
   * Sets this processor's set of characters triggering the activation of the completion proposal
   * computation.
   * 
   * @param activationSet the activation set
   */
  public final void setCompletionProposalAutoActivationCharacters(char[] activationSet) {
    fCompletionAutoActivationCharacters = activationSet;
  }

  /**
   * Creates the context that is passed to the completion proposal computers.
   * 
   * @param viewer the viewer that content assist is invoked on
   * @param offset the content assist offset
   * @return the context to be passed to the computers
   */
  protected ContentAssistInvocationContext createContext(ITextViewer viewer, int offset) {
    return new ContentAssistInvocationContext(viewer, offset);
  }

  /**
   * Creates a progress monitor.
   * <p>
   * The default implementation creates a <code>NullProgressMonitor</code>.
   * </p>
   * 
   * @return a progress monitor
   */
  protected IProgressMonitor createProgressMonitor() {
    return new NullProgressMonitor();
  }

  /**
   * Filters and sorts the context information objects. The passed list may be modified and
   * returned, or a new list may be created and returned.
   * 
   * @param contexts the list of collected proposals (element type: {@link IContextInformation})
   * @param monitor a progress monitor
   * @return the list of filtered and sorted proposals, ready for display (element type:
   *         {@link IContextInformation})
   */
  protected List filterAndSortContextInformation(List contexts, IProgressMonitor monitor) {
    return contexts;
  }

  /**
   * Filters and sorts the proposals. The passed list may be modified and returned, or a new list
   * may be created and returned.
   * 
   * @param proposals the list of collected proposals (element type: {@link ICompletionProposal})
   * @param monitor a progress monitor
   * @param context TODO
   * @return the list of filtered and sorted proposals, ready for display (element type:
   *         {@link ICompletionProposal})
   */
  protected List filterAndSortProposals(List proposals, IProgressMonitor monitor,
      ContentAssistInvocationContext context) {
    return proposals;
  }

  private void clearState() {
    fErrorMessage = null;
    fNumberOfComputedResults = 0;
  }

  private List collectContextInformation(ITextViewer viewer, int offset, IProgressMonitor monitor) {
    List proposals = new ArrayList();
    ContentAssistInvocationContext context = createContext(viewer, offset);

    List providers = getCategories();
    for (Iterator it = providers.iterator(); it.hasNext();) {
      CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      List computed = cat.computeContextInformation(context, fPartition, new SubProgressMonitor(
          monitor,
          1));
      proposals.addAll(computed);
      if (fErrorMessage == null) {
        fErrorMessage = cat.getErrorMessage();
      }
    }

    return proposals;
  }

  private List collectProposals(ITextViewer viewer, int offset, IProgressMonitor monitor,
      ContentAssistInvocationContext context) {
    List proposals = new ArrayList();
    InstrumentationBuilder instrumentation = Instrumentation.builder("CollectProposals");
    try {
      List providers = getCategories();
      for (Iterator it = providers.iterator(); it.hasNext();) {
        CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
        List computed = cat.computeCompletionProposals(context, fPartition, new SubProgressMonitor(
            monitor,
            1));
        proposals.addAll(computed);
        if (fErrorMessage == null) {
          fErrorMessage = cat.getErrorMessage();
        }
      }
      instrumentation.metric("ProposalsComplete", true);
    } finally {
      instrumentation.log();
    }
    return proposals;
  }

  private String createEmptyMessage() {
    return Messages.format(
        DartTextMessages.ContentAssistProcessor_empty_message,
        new String[] {getCategoryLabel(fRepetition)});
  }

  private String createIterationMessage() {
    return Messages.format(
        DartTextMessages.ContentAssistProcessor_toggle_affordance_update_message,
        new String[] {
            getCategoryLabel(fRepetition), fIterationGesture, getCategoryLabel(fRepetition + 1)});
  }

  private List getCategories() {
    if (fCategoryIteration == null) {
      return fCategories;
    }

    int iteration = fRepetition % fCategoryIteration.size();
    fAssistant.setStatusMessage(createIterationMessage());
    fAssistant.setEmptyMessage(createEmptyMessage());
    fRepetition++;

//		fAssistant.setShowMessage(fRepetition % 2 != 0);
//		
    return fCategoryIteration.get(iteration);
  }

  private List getCategoryIteration() {
    List sequence = new ArrayList();
    sequence.add(getDefaultCategories());
    for (Iterator it = getSeparateCategories().iterator(); it.hasNext();) {
      CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      sequence.add(Collections.singletonList(cat));
    }
    return sequence;
  }

  private String getCategoryLabel(int repetition) {
    int iteration = repetition % fCategoryIteration.size();
    if (iteration == 0) {
      return DartTextMessages.ContentAssistProcessor_defaultProposalCategory;
    }
    return toString((CompletionProposalCategory) ((List) fCategoryIteration.get(iteration)).get(0));
  }

  private List getDefaultCategories() {
    // default mix - enable all included computers
    List included = getDefaultCategoriesUnchecked();

    if ((DartPartitions.DART_DOC.equals(fPartition)
        || DartPartitions.DART_SINGLE_LINE_DOC.equals(fPartition) || IDocument.DEFAULT_CONTENT_TYPE.equals(fPartition))
        && included.isEmpty() && !fCategories.isEmpty()) {
      if (informUserAboutEmptyDefaultCategory()) {
        // preferences were restored - recompute the default categories
        included = getDefaultCategoriesUnchecked();
      }
    }

    return included;
  }

  private List getDefaultCategoriesUnchecked() {
    List included = new ArrayList();
    for (Iterator it = fCategories.iterator(); it.hasNext();) {
      CompletionProposalCategory category = (CompletionProposalCategory) it.next();
      if (category.isIncluded() && category.hasComputers(fPartition)) {
        included.add(category);
      }
    }
    return included;
  }

  private KeySequence getIterationBinding() {
    final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(
        IBindingService.class);
    TriggerSequence binding = bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    if (binding instanceof KeySequence) {
      return (KeySequence) binding;
    }
    return null;
  }

  private String getIterationGesture() {
    TriggerSequence binding = getIterationBinding();
    return binding != null ? Messages.format(
        DartTextMessages.ContentAssistProcessor_toggle_affordance_press_gesture,
        new Object[] {binding.format()})
        : DartTextMessages.ContentAssistProcessor_toggle_affordance_click_gesture;
  }

  private List getSeparateCategories() {
    ArrayList sorted = new ArrayList();
    for (Iterator it = fCategories.iterator(); it.hasNext();) {
      CompletionProposalCategory category = (CompletionProposalCategory) it.next();
      if (category.isSeparateCommand() && category.hasComputers(fPartition)) {
        sorted.add(category);
      }
    }
    Collections.sort(sorted, ORDER_COMPARATOR);
    return sorted;
  }

  /**
   * Informs the user about the fact that there are no enabled categories in the default content
   * assist set and shows a link to the preferences.
   */
  private boolean informUserAboutEmptyDefaultCategory() {
    if (OptionalMessageDialog.isDialogEnabled(PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY)) {
      final Shell shell = DartToolsPlugin.getActiveWorkbenchShell();
      String title = DartTextMessages.ContentAssistProcessor_all_disabled_title;
      String message = DartTextMessages.ContentAssistProcessor_all_disabled_message;
      // see PreferencePage#createControl for the 'defaults' label
      final String restoreButtonLabel = JFaceResources.getString("defaults"); //$NON-NLS-1$
      final String linkMessage = Messages.format(
          DartTextMessages.ContentAssistProcessor_all_disabled_preference_link,
          LegacyActionTools.removeMnemonics(restoreButtonLabel));
      final int restoreId = IDialogConstants.CLIENT_ID + 10;
      final int settingsId = IDialogConstants.CLIENT_ID + 11;
      final OptionalMessageDialog dialog = new OptionalMessageDialog(
          PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY,
          shell,
          title,
          null /*
                * default image
                */,
          message,
          MessageDialog.WARNING,
          new String[] {restoreButtonLabel, IDialogConstants.CLOSE_LABEL},
          1) {
        /*
         * @see org.eclipse.jface.dialogs.MessageDialog#createButtonsForButtonBar
         * (org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected void createButtonsForButtonBar(Composite parent) {
          Button[] buttons = new Button[2];
          buttons[0] = createButton(parent, restoreId, restoreButtonLabel, false);
          buttons[1] = createButton(
              parent,
              IDialogConstants.CLOSE_ID,
              IDialogConstants.CLOSE_LABEL,
              true);
          setButtons(buttons);
        }

        /*
         * @see com.google.dart.tools.ui.dialogs.OptionalMessageDialog#createCustomArea
         * (org.eclipse.swt.widgets.Composite)
         */
        @Override
        protected Control createCustomArea(Composite composite) {
          // wrap link and checkbox in one composite without space
          Composite parent = new Composite(composite, SWT.NONE);
          GridLayout layout = new GridLayout();
          layout.marginHeight = 0;
          layout.marginWidth = 0;
          layout.verticalSpacing = 0;
          parent.setLayout(layout);

          Composite linkComposite = new Composite(parent, SWT.NONE);
          layout = new GridLayout();
          layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
          layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
          layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
          linkComposite.setLayout(layout);

          Link link = new Link(linkComposite, SWT.NONE);
          link.setText(linkMessage);
          link.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              setReturnCode(settingsId);
              close();
            }
          });
          GridData gridData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
          gridData.widthHint = this.getMinimumMessageWidth();
          link.setLayoutData(gridData);

          // create checkbox and "don't show this message" prompt
          super.createCustomArea(parent);

          return parent;
        }
      };
      int returnValue = dialog.open();
      if (restoreId == returnValue || settingsId == returnValue) {
        if (restoreId == returnValue) {
          IPreferenceStore store = DartToolsPlugin.getDefault().getPreferenceStore();
          store.setToDefault(PreferenceConstants.CODEASSIST_CATEGORY_ORDER);
          store.setToDefault(PreferenceConstants.CODEASSIST_EXCLUDED_CATEGORIES);
        }
        if (settingsId == returnValue) {
          PreferencesUtil.createPreferenceDialogOn(
              shell,
              "com.google.dart.tools.ui.internal.preferences.CodeAssistPreferenceAdvanced", null, null).open(); //$NON-NLS-1$
        }
        CompletionProposalComputerRegistry registry = CompletionProposalComputerRegistry.getDefault();
        registry.reload();
        return true;
      }
    }
    return false;
  }

  private String toString(CompletionProposalCategory category) {
    return category.getDisplayName();
  }
}
