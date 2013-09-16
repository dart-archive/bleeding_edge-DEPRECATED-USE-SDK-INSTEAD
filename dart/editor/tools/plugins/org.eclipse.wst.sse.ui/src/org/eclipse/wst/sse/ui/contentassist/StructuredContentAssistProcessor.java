/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.contentassist;

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
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension2;
import org.eclipse.jface.text.contentassist.IContentAssistantExtension3;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.ExtendedConfigurationBuilder;
import org.eclipse.wst.sse.ui.internal.IReleasable;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;
import org.eclipse.wst.sse.ui.internal.SSEUIPlugin;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalCategory;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposalComputerRegistry;
import org.eclipse.wst.sse.ui.internal.contentassist.CompletionProposoalCatigoriesConfigurationRegistry;
import org.eclipse.wst.sse.ui.internal.contentassist.CompoundContentAssistProcessor;
import org.eclipse.wst.sse.ui.internal.contentassist.ContextInformationValidator;
import org.eclipse.wst.sse.ui.internal.contentassist.OptionalMessageDialog;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationReader;
import org.eclipse.wst.sse.ui.preferences.ICompletionProposalCategoriesConfigurationWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * A content assist processor that aggregates the proposals of the
 * {@link org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer}s contributed via the
 * <code>org.eclipse.wst.sse.ui.completionProposal</code> extension point.
 * </p>
 * <p>
 * Extenders may extend:
 * <ul>
 * <li>{@link #propertyChange(PropertyChangeEvent)}to react to property change events that occur in
 * the {@link IPreferenceStore} given to the constructor in case the behavior of the processor needs
 * to change according to user preferences</li>
 * <li>{@link #getCompletionProposalAutoActivationCharacters()}</li>
 * <li>{@link #getContextInformationAutoActivationCharacters()}</li>
 * <li>{@link #filterAndSortProposals(List, IProgressMonitor, CompletionProposalInvocationContext)}
 * to add sorting and filtering</li>
 * <li>{@link #filterAndSortContextInformation(List, IProgressMonitor)} to add sorting and filtering
 * </li>
 * <li>{@link #createProgressMonitor()} to change the way progress is reported</li>
 * <li>{@link #createContext(ITextViewer, int)} to provide the context object passed to the
 * computers</li>
 * <li>{@link #getContextInformationValidator()} to add context validation (needed if any contexts
 * are provided)</li>
 * <li>{@link #getErrorMessage()} to change error reporting</li>
 * </ul>
 * </p>
 * 
 * @base org.eclipse.jdt.internal.ui.text.java.ContentAssistProcessor
 */
public class StructuredContentAssistProcessor implements IContentAssistProcessor,
    IPropertyChangeListener, IReleasable {

  /** Legacy editor configuration extension point. */
  private static final String CONTENT_ASSIST_PROCESSOR_EXTENDED_ID = "contentassistprocessor"; //$NON-NLS-1$

  /** Content assist processors added through the now legacy editor configuration extension point */
  private List fLegacyExtendedContentAssistProcessors;

  /**
   * Dialog settings key for the "all categories are disabled" warning dialog. See
   * {@link OptionalMessageDialog}.
   */
  private static final String PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY = "EmptyDefaultAssistCategory"; //$NON-NLS-1$

  /**
   * Used to sort categories by their page order so they are cycled in the correct order
   */
  private final Comparator PAGE_ORDER_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      CompletionProposalCategory d1 = (CompletionProposalCategory) o1;
      CompletionProposalCategory d2 = (CompletionProposalCategory) o2;

      return d1.getPageSortRank(fContentTypeID) - d2.getPageSortRank(fContentTypeID);
    }
  };

  /**
   * Used to sort categories by their default page order so they are ordered correctly on the
   * default page
   */
  private final Comparator DEFAULT_PAGE_ORDER_COMPARATOR = new Comparator() {
    public int compare(Object o1, Object o2) {
      CompletionProposalCategory d1 = (CompletionProposalCategory) o1;
      CompletionProposalCategory d2 = (CompletionProposalCategory) o2;

      return d1.getDefaultPageSortRank(fContentTypeID) - d2.getDefaultPageSortRank(fContentTypeID);
    }
  };

  /** List of {@link CompletionProposalCategory}s supported by this processor */
  private List fCategories;

  /** content type ID this processor is associated with */
  String fContentTypeID;

  /** partition type ID this processor is associated with */
  private final String fPartitionTypeID;

  /** Content assistant used for giving the user status messages and listening to completion results */
  private ContentAssistant fAssistant;

  /* cycling stuff */
  private int fRepetition = -1;
  private List fCategoryIteration = null;
  private String fIterationGesture = null;
  private int fNumberOfComputedResults = 0;
  private String fErrorMessage;

  /** Optionally specified preference store for listening to property change events */
  private IPreferenceStore fPreferenceStore;

  /** The viewer this processor is associated with */
  private ITextViewer fViewer;

  /**
   * the {@link ITextInputListener} used to set the content type when a document is set for this
   * processors associated viewer.
   */
  private ITextInputListener fTextInputListener;

  private CompletionListener fCompletionListener;

  /** the context information validator for this processor */
  private IContextInformationValidator fContextInformationValidator;

  private AutoActivationDelegate fAutoActivation;

  /**
   * <p>
   * Create a new content assist processor for a specific partition type. The content type will be
   * determined when a document is set on the viewer
   * </p>
   * <p>
   * If the given {@link IPreferenceStore} is not <code>null</code> then this processor will be
   * registered as a {@link IPropertyChangeListener} on the given store so that implementers of this
   * class can change the way the processor acts based on user preferences
   * </p>
   * 
   * @param assistant {@link ContentAssistant} to use
   * @param partitionTypeID the partition type this processor is for
   * @param viewer {@link ITextViewer} this processor is acting in
   * @param preferenceStore This processor will be registered as a {@link IPropertyChangeListener}
   *          on this store and the processor itself will take care of removing itself as a
   *          listener, if <code>null</code> then will not be registered as a
   *          {@link IPropertyChangeListener}
   */
  public StructuredContentAssistProcessor(ContentAssistant assistant, String partitionTypeID,
      ITextViewer viewer, IPreferenceStore preferenceStore) {

    Assert.isNotNull(partitionTypeID);
    Assert.isNotNull(assistant);

    //be sure the registry has been loaded, none blocking
    CompletionProposalComputerRegistry.getDefault().initialize();

    //register on the preference store
    this.fPreferenceStore = preferenceStore;
    if (this.fPreferenceStore != null) {
      this.fPreferenceStore.addPropertyChangeListener(this);
    }

    //The content type can not be determined until a document has been set
    this.fContentTypeID = null;
    this.fViewer = viewer;
    if (viewer != null) {
      this.fTextInputListener = new TextInputListener();
      viewer.addTextInputListener(this.fTextInputListener);

      if (viewer.getDocument() != null) {
        /*
         * it is highly unlike the document has already been set, but check just for sanity
         */
        this.fTextInputListener.inputDocumentChanged(null, viewer.getDocument());
      }
    }

    //set the associated partition type
    this.fPartitionTypeID = partitionTypeID;

    //add completion listener
    fAssistant = assistant;
    fCompletionListener = new CompletionListener();
    fAssistant.addCompletionListener(fCompletionListener);

    //lazy load these to speed up initial editor opening
    fLegacyExtendedContentAssistProcessors = null;
    fCategories = null;
  }

  /**
   * <p>
   * Collect the proposals using the extension points
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer,
   *      int)
   */
  public final ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
    clearState();

    IProgressMonitor monitor = createProgressMonitor();
    monitor.beginTask(SSEUIMessages.ContentAssist_computing_proposals,
        getProposalCategories().size() + 1);

    CompletionProposalInvocationContext context = createContext(viewer, offset);

    monitor.subTask(SSEUIMessages.ContentAssist_collecting_proposals);
    List proposals = collectProposals(viewer, offset, monitor, context);

    monitor.subTask(SSEUIMessages.ContentAssist_sorting_proposals);
    List filtered = filterAndSortProposals(proposals, monitor, context);
    fNumberOfComputedResults = filtered.size();

    ICompletionProposal[] result = (ICompletionProposal[]) filtered.toArray(new ICompletionProposal[filtered.size()]);
    monitor.done();

    return result;
  }

  /**
   * <p>
   * Collect the context information using the extension points
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer,
   *      int)
   */
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
    clearState();

    IProgressMonitor monitor = createProgressMonitor();
    monitor.beginTask(SSEUIMessages.ContentAssist_computing_contexts,
        getProposalCategories().size() + 1);

    monitor.subTask(SSEUIMessages.ContentAssist_collecting_contexts);
    List proposals = collectContextInformation(viewer, offset, monitor);

    monitor.subTask(SSEUIMessages.ContentAssist_sorting_contexts);
    List filtered = filterAndSortContextInformation(proposals, monitor);
    fNumberOfComputedResults = filtered.size();

    IContextInformation[] result = (IContextInformation[]) filtered.toArray(new IContextInformation[filtered.size()]);
    monitor.done();
    return result;
  }

  /**
   * <p>
   * Default implementation is to return <code>null</code>
   * </p>
   * <p>
   * Extenders may override
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
   */
  public char[] getCompletionProposalAutoActivationCharacters() {
    return (fAutoActivation != null)
        ? fAutoActivation.getCompletionProposalAutoActivationCharacters() : null;
  }

  /**
   * <p>
   * Default implementation is to return <code>null</code>
   * </p>
   * <p>
   * Extenders may override
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
   */
  public char[] getContextInformationAutoActivationCharacters() {
    return (fAutoActivation != null)
        ? fAutoActivation.getContextInformationAutoActivationCharacters() : null;
  }

  /**
   * <p>
   * Extenders may override this function to change error reporting
   * </p>
   * 
   * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
   */
  public String getErrorMessage() {
    if (fErrorMessage != null)
      return fErrorMessage;
    if (fNumberOfComputedResults > 0)
      return null;
    return SSEUIMessages.ContentAssist_no_completions;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.StructuredContentAssistProcessor#getContextInformationValidator()
   */
  public IContextInformationValidator getContextInformationValidator() {
    if (this.fContextInformationValidator == null) {
      this.fContextInformationValidator = new ContextInformationValidator();
    }
    return this.fContextInformationValidator;
  }

  public void install(ITextViewer viewer) {
    if (fPreferenceStore != null) {
      fPreferenceStore.addPropertyChangeListener(this);
    }
    if (fViewer != null) {
      fViewer.removeTextInputListener(fTextInputListener);
    }
    fViewer = viewer;
    if (fViewer != null) {
      fViewer.addTextInputListener(fTextInputListener);
    }
    if (fAssistant != null) {
      fAssistant.addCompletionListener(fCompletionListener);
    }
  }

  /**
   * <p>
   * Extenders may override, but should always be sure to call the super implementation
   * </p>
   * 
   * @see org.eclipse.wst.sse.ui.internal.IReleasable#release()
   */
  public void release() {
    if (fAutoActivation != null) {
      fAutoActivation.dispose();
      fAutoActivation = null;
    }
    if (this.fPreferenceStore != null) {
      this.fPreferenceStore.removePropertyChangeListener(this);
    }

    if (this.fViewer != null) {
      this.fViewer.removeTextInputListener(this.fTextInputListener);
      this.fViewer = null;
    }
    if (this.fAssistant != null) {
      this.fAssistant.removeCompletionListener(fCompletionListener);
    }
  }

  /**
   * <p>
   * Intended to be overridden by extenders wishing to change the behavior of the processor based on
   * user preferences from the store optionally associated with this processor. If no store was
   * given to the constructor when creating this assistant then this method will never be invoked.
   * </p>
   * <p>
   * The default implementation does not react to the events in any way
   * </p>
   * 
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
  }

  /**
   * <p>
   * Filters and sorts the proposals. The passed list may be modified and returned, or a new list
   * may be created and returned.
   * </p>
   * <p>
   * The default implementation does not do any sorting or filtering.
   * </p>
   * <p>
   * Extenders may override this function.
   * </p>
   * 
   * @param proposals the list of collected proposals (element type: {@link ICompletionProposal})
   * @param monitor a progress monitor
   * @param context TODO
   * @return the list of filtered and sorted proposals, ready for display (element type:
   *         {@link ICompletionProposal})
   */
  protected List filterAndSortProposals(List proposals, IProgressMonitor monitor,
      CompletionProposalInvocationContext context) {
    return proposals;
  }

  /**
   * <p>
   * Filters and sorts the context information objects. The passed list may be modified and
   * returned, or a new list may be created and returned.
   * </p>
   * <p>
   * The default implementation does not do any sorting or filtering
   * </p>
   * <p>
   * Extenders may override this method
   * </p>
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
   * <p>
   * Creates a progress monitor.
   * </p>
   * <p>
   * The default implementation creates a {@link NullProgressMonitor}.
   * </p>
   * <p>
   * Extenders may override this method
   * </p>
   * 
   * @return a progress monitor
   */
  protected IProgressMonitor createProgressMonitor() {
    return new NullProgressMonitor();
  }

  /**
   * <p>
   * Creates the context that is passed to the completion proposal computers.
   * </p>
   * <p>
   * Extenders may override this method
   * </p>
   * 
   * @param viewer the viewer that content assist is invoked on
   * @param offset the content assist offset
   * @return the context to be passed to the computers
   */
  protected CompletionProposalInvocationContext createContext(ITextViewer viewer, int offset) {
    return new CompletionProposalInvocationContext(viewer, offset);
  }

  /**
   * @return the associated preference store
   */
  protected IPreferenceStore getPreferenceStore() {
    return this.fPreferenceStore;
  }

  /**
   * Clears the state
   */
  private void clearState() {
    fErrorMessage = null;
    fNumberOfComputedResults = 0;
  }

  /**
   * <p>
   * Collects the proposals from the extensions.
   * </p>
   * 
   * @param viewer the text viewer
   * @param offset the offset
   * @param monitor the progress monitor
   * @param context the code assist invocation context
   * @return the list of proposals
   */
  private List collectProposals(ITextViewer viewer, int offset, IProgressMonitor monitor,
      CompletionProposalInvocationContext context) {
    List proposals = new ArrayList();
    List categories = getCategories();
    for (Iterator it = categories.iterator(); it.hasNext();) {
      CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      List computed = cat.computeCompletionProposals(context, this.fContentTypeID,
          this.fPartitionTypeID, new SubProgressMonitor(monitor, 1));
      proposals.addAll(computed);
      if (fErrorMessage == null) {
        fErrorMessage = cat.getErrorMessage();
      }
    }

    // if default page
    // Deal with adding in proposals from processors added through the legacy extension
    if (isFirstPage() && getLegacyExtendedContentAssistProcessors() != null
        && !getLegacyExtendedContentAssistProcessors().isEmpty()) {

      Iterator iter = getLegacyExtendedContentAssistProcessors().iterator();
      while (iter.hasNext()) {
        IContentAssistProcessor legacyProcessor = (IContentAssistProcessor) iter.next();
        ICompletionProposal[] legacyComputed = legacyProcessor.computeCompletionProposals(viewer,
            offset);
        if (legacyComputed != null) {
          proposals.addAll(Arrays.asList(legacyComputed));
        }
      }
    }

    return proposals;
  }

  /**
   * <p>
   * Collects the context information from the extensions.
   * </p>
   * 
   * @param viewer
   * @param offset
   * @param monitor
   * @return
   */
  private List collectContextInformation(ITextViewer viewer, int offset, IProgressMonitor monitor) {
    List proposals = new ArrayList();
    CompletionProposalInvocationContext context = createContext(viewer, offset);

    List providers = getCategories();
    for (Iterator it = providers.iterator(); it.hasNext();) {
      CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      List computed = cat.computeContextInformation(context, this.fContentTypeID,
          this.fPartitionTypeID, new SubProgressMonitor(monitor, 1));
      proposals.addAll(computed);
      if (fErrorMessage == null) {
        fErrorMessage = cat.getErrorMessage();
      }
    }

    // Deal with adding in contexts from processors added through the legacy extension
    if (getLegacyExtendedContentAssistProcessors() != null
        && !getLegacyExtendedContentAssistProcessors().isEmpty()) {

      Iterator iter = getLegacyExtendedContentAssistProcessors().iterator();
      while (iter.hasNext()) {
        IContentAssistProcessor legacyProcessor = (IContentAssistProcessor) iter.next();
        IContextInformation[] legacyComputed = legacyProcessor.computeContextInformation(viewer,
            offset);
        if (legacyComputed != null) {
          proposals.addAll(Arrays.asList(legacyComputed));
        }
      }
    }

    return proposals;
  }

  /**
   * @return the next set of categories
   */
  private List getCategories() {
    List categories;
    if (fCategoryIteration == null) {
      categories = getProposalCategories();
    } else {
      int iteration = fRepetition % fCategoryIteration.size();
      fAssistant.setStatusMessage(createIterationMessage());
      fAssistant.setEmptyMessage(createEmptyMessage());
      fRepetition++;

      categories = (List) fCategoryIteration.get(iteration);
    }

    return categories;
  }

  /**
   * This may show the warning dialog if all categories are disabled
   */
  private void resetCategoryIteration() {
    fCategoryIteration = getCategoryIteration();
  }

  /**
   * @return {@link List} of {@link List}s of {@link CompletionProposalCategory}s, this is the
   *         ordered list of the completion categories to cycle through
   */
  private List getCategoryIteration() {
    List sequence = new ArrayList();
    sequence.add(getDefaultCategories());
    for (Iterator it = getSortedOwnPageCategories().iterator(); it.hasNext();) {
      CompletionProposalCategory cat = (CompletionProposalCategory) it.next();
      sequence.add(Collections.singletonList(cat));
    }
    return sequence;
  }

  /**
   * @return the sorted categories for the default page
   */
  private List getDefaultCategories() {
    // default mix - enable all included computers
    List included = getDefaultCategoriesUnchecked();

    if (included.size() == 0
        && CompletionProposalComputerRegistry.getDefault().hasUninstalledComputers()) {
      if (informUserAboutEmptyDefaultCategory()) {
        // preferences were restored - recompute the default categories
        included = getDefaultCategoriesUnchecked();
      }
      CompletionProposalComputerRegistry.getDefault().resetUnistalledComputers();
    }

    Collections.sort(included, DEFAULT_PAGE_ORDER_COMPARATOR);

    return included;
  }

  /**
   * <p>
   * Gets the default categories with no error checking.
   * </p>
   * 
   * @return the default {@link CompletionProposalCategory}s
   */
  private List getDefaultCategoriesUnchecked() {
    List included = new ArrayList();
    for (Iterator it = getProposalCategories().iterator(); it.hasNext();) {
      CompletionProposalCategory category = (CompletionProposalCategory) it.next();
      if (category.isIncludedOnDefaultPage(this.fContentTypeID)
          && category.hasComputers(fContentTypeID, fPartitionTypeID))
        included.add(category);
    }
    return included;
  }

  /**
   * <p>
   * Informs the user about the fact that there are no enabled categories in the default content
   * assist set and shows a link to the preferences.
   * </p>
   * 
   * @return <code>true</code> if the default should be restored
   */
  private boolean informUserAboutEmptyDefaultCategory() {
    /*
     * If warn about empty default category and there are associated properties for this processors
     * content type and those properties have an associated properties page then display warning
     * message to user.
     */
    ICompletionProposalCategoriesConfigurationReader properties = CompletionProposoalCatigoriesConfigurationRegistry.getDefault().getReadableConfiguration(
        this.fContentTypeID);
    if (OptionalMessageDialog.isDialogEnabled(PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY)
        && properties instanceof ICompletionProposalCategoriesConfigurationWriter
        && ((ICompletionProposalCategoriesConfigurationWriter) properties).hasAssociatedPropertiesPage()) {

      ICompletionProposalCategoriesConfigurationWriter propertiesExtension = (ICompletionProposalCategoriesConfigurationWriter) properties;

      final Shell shell = SSEUIPlugin.getActiveWorkbenchShell();
      String title = SSEUIMessages.ContentAssist_all_disabled_title;
      String message = SSEUIMessages.ContentAssist_all_disabled_message;
      // see PreferencePage#createControl for the 'defaults' label
      final String restoreButtonLabel = JFaceResources.getString("defaults"); //$NON-NLS-1$
      final String linkMessage = NLS.bind(SSEUIMessages.ContentAssist_all_disabled_preference_link,
          LegacyActionTools.removeMnemonics(restoreButtonLabel));
      final int restoreId = IDialogConstants.CLIENT_ID + 10;
      final int settingsId = IDialogConstants.CLIENT_ID + 11;
      final OptionalMessageDialog dialog = new OptionalMessageDialog(
          PREF_WARN_ABOUT_EMPTY_ASSIST_CATEGORY, shell, title, null /* default image */, message,
          MessageDialog.WARNING, new String[] {restoreButtonLabel, IDialogConstants.CLOSE_LABEL}, 1) {
        /*
         * @see
         * org.eclipse.jdt.internal.ui.dialogs.OptionalMessageDialog#createCustomArea(org.eclipse
         * .swt.widgets.Composite)
         */
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

        /*
         * @see
         * org.eclipse.jface.dialogs.MessageDialog#createButtonsForButtonBar(org.eclipse.swt.widgets
         * .Composite)
         */
        protected void createButtonsForButtonBar(Composite parent) {
          Button[] buttons = new Button[2];
          buttons[0] = createButton(parent, restoreId, restoreButtonLabel, false);
          buttons[1] = createButton(parent, IDialogConstants.CLOSE_ID,
              IDialogConstants.CLOSE_LABEL, true);
          setButtons(buttons);
        }
      };
      int returnValue = dialog.open();

      //based on user actions either reset defaults or open preference dialog
      if (restoreId == returnValue || settingsId == returnValue) {
        if (restoreId == returnValue) {
          propertiesExtension.loadDefaults();
          propertiesExtension.saveConfiguration();
        }
        if (settingsId == returnValue) {
          PreferencesUtil.createPreferenceDialogOn(shell,
              propertiesExtension.getPropertiesPageID(), null, null).open();
        }

        return true;
      }
    }
    return false;
  }

  /**
   * @return a sorted {@link List} of {@link CompletionProposalCategory}s that should be displayed
   *         on their own content assist page
   */
  private List getSortedOwnPageCategories() {
    ArrayList sorted = new ArrayList();
    for (Iterator it = getProposalCategories().iterator(); it.hasNext();) {
      CompletionProposalCategory category = (CompletionProposalCategory) it.next();
      if (category.isDisplayedOnOwnPage(this.fContentTypeID)
          && category.hasComputers(fContentTypeID, fPartitionTypeID)) {

        sorted.add(category);
      }
    }
    Collections.sort(sorted, PAGE_ORDER_COMPARATOR);
    return sorted;
  }

  /**
   * @return a user message describing that there are no content assist suggestions for the current
   *         page
   */
  private String createEmptyMessage() {
    return NLS.bind(SSEUIMessages.ContentAssist_no_message,
        new String[] {getCategoryLabel(fRepetition)});
  }

  /**
   * @return user message describing what the next page of content assist holds
   */
  private String createIterationMessage() {
    return NLS.bind(SSEUIMessages.ContentAssist_toggle_affordance_update_message, new String[] {
        getCategoryLabel(fRepetition), fIterationGesture, getCategoryLabel(fRepetition + 1)});
  }

  /**
   * @param repetition which category to get the label for
   * @return the label of the category
   */
  private String getCategoryLabel(int repetition) {
    int iteration = (fCategoryIteration != null ? repetition % fCategoryIteration.size() : 0);
    if (iteration == 0)
      return SSEUIMessages.ContentAssist_defaultProposalCategory_title;
    return ((CompletionProposalCategory) ((List) fCategoryIteration.get(iteration)).get(0)).getDisplayName();
  }

  /**
   * @return {@link String} representing the user command to iterate to the next page
   */
  private String getIterationGesture() {
    TriggerSequence binding = getIterationBinding();
    return binding != null ? NLS.bind(SSEUIMessages.ContentAssist_press,
        new Object[] {binding.format()}) : SSEUIMessages.ContentAssist_click;
  }

  /**
   * @return {@link KeySequence} used by user to iterate to the next page
   */
  private KeySequence getIterationBinding() {
    final IBindingService bindingSvc = (IBindingService) PlatformUI.getWorkbench().getAdapter(
        IBindingService.class);
    TriggerSequence binding = bindingSvc.getBestActiveBindingFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
    if (binding instanceof KeySequence)
      return (KeySequence) binding;
    return null;
  }

  /**
   * @return <code>true</code> if displaying first page, <code>false</code> otherwise
   */
  private boolean isFirstPage() {
    return fCategoryIteration == null || fCategoryIteration.size() == 1
        || fRepetition % fCategoryIteration.size() == 1;
  }

  /**
   * <p>
   * <b>NOTE: </b>This method should be used over accessing the
   * {@link #fLegacyExtendedContentAssistProcessors} field directly so as to facilitate the lazy
   * initialization of the field.
   * </p>
   * 
   * @return the legacy extended content assist processors
   */
  private List getLegacyExtendedContentAssistProcessors() {
    if (fLegacyExtendedContentAssistProcessors == null) {
      fLegacyExtendedContentAssistProcessors = ExtendedConfigurationBuilder.getInstance().getConfigurations(
          CONTENT_ASSIST_PROCESSOR_EXTENDED_ID, fPartitionTypeID);
    }

    return fLegacyExtendedContentAssistProcessors;
  }

  /**
   * <p>
   * <b>NOTE: </b>This method should be used over accessing the {@link #fCategories} field directly
   * so as to facilitate the lazy initialization of the field.
   * </p>
   * 
   * @return the categories associated with the content type this processor is associated with
   */
  private List getProposalCategories() {
    if (fCategories == null) {
      fCategories = CompletionProposalComputerRegistry.getDefault().getProposalCategories(
          fContentTypeID);
    }

    return fCategories;
  }

  /**
   * The completion listener class for this processor.
   */
  private final class CompletionListener implements ICompletionListener,
      ICompletionListenerExtension {
    /**
     * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionStarted(org.eclipse.jface.text.contentassist.ContentAssistEvent)
     */
    public void assistSessionStarted(ContentAssistEvent event) {
      if (event.processor == StructuredContentAssistProcessor.this
          || (event.processor instanceof CompoundContentAssistProcessor && ((CompoundContentAssistProcessor) event.processor).containsProcessor(StructuredContentAssistProcessor.this))) {

        fIterationGesture = getIterationGesture();
        KeySequence binding = getIterationBinding();

        // This may show the warning dialog if all categories are disabled
        resetCategoryIteration();
        for (Iterator it = StructuredContentAssistProcessor.this.getProposalCategories().iterator(); it.hasNext();) {
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
    }

    /**
     * @see org.eclipse.jface.text.contentassist.ICompletionListener#assistSessionEnded(org.eclipse.jface.text.contentassist.ContentAssistEvent)
     */
    public void assistSessionEnded(ContentAssistEvent event) {
      if (event.processor == StructuredContentAssistProcessor.this
          || (event.processor instanceof CompoundContentAssistProcessor && ((CompoundContentAssistProcessor) event.processor).containsProcessor(StructuredContentAssistProcessor.this))) {
        for (Iterator it = StructuredContentAssistProcessor.this.getProposalCategories().iterator(); it.hasNext();) {
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
    }

    /**
     * @see org.eclipse.jface.text.contentassist.ICompletionListener#selectionChanged(org.eclipse.jface.text.contentassist.ICompletionProposal,
     *      boolean)
     */
    public void selectionChanged(ICompletionProposal proposal, boolean smartToggle) {
      //ignore
    }

    /**
     * @see org.eclipse.jface.text.contentassist.ICompletionListenerExtension#assistSessionRestarted(org.eclipse.jface.text.contentassist.ContentAssistEvent)
     */
    public void assistSessionRestarted(ContentAssistEvent event) {
      fRepetition = 0;
    }
  }

  /**
	 * 
	 */
  private class TextInputListener implements ITextInputListener {

    /**
     * <p>
     * Set the content type based on the new document if it has not already been set yet.
     * </p>
     * 
     * @see org.eclipse.jface.text.ITextInputListener#inputDocumentChanged(org.eclipse.jface.text.IDocument,
     *      org.eclipse.jface.text.IDocument)
     */
    public void inputDocumentChanged(IDocument oldInput, IDocument newInput) {
      if (fContentTypeID == null) {
        if (newInput instanceof IStructuredDocument) {
          IStructuredModel model = null;
          try {
            model = StructuredModelManager.getModelManager().getModelForRead(
                (IStructuredDocument) newInput);
            if (model != null) {
              fContentTypeID = model.getContentTypeIdentifier();
              if (fAutoActivation != null) {
                fAutoActivation.dispose();
              }
              fAutoActivation = CompletionProposalComputerRegistry.getDefault().getActivator(
                  fContentTypeID, fPartitionTypeID);
            }
          } finally {
            if (model != null) {
              model.releaseFromRead();
            }
          }
        }
      }
    }

    /**
     * <p>
     * Ignored
     * </p>
     * 
     * @see org.eclipse.jface.text.ITextInputListener#inputDocumentAboutToBeChanged(org.eclipse.jface.text.IDocument,
     *      org.eclipse.jface.text.IDocument)
     */
    public void inputDocumentAboutToBeChanged(IDocument oldInput, IDocument newInput) {
      //ignore
    }
  }

  protected void setAutoActivationDelay(int delay) {
    fAssistant.setAutoActivationDelay(delay);
  }

}
