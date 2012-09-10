/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sean Montgomery, sean_montgomery@comcast.net - https://bugs.eclipse.org/bugs/show_bug.cgi?id=116454
 *******************************************************************************/
package org.eclipse.jface.text.contentassist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.osgi.util.TextProcessor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;

import org.eclipse.core.runtime.Assert;

import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.SWTKeySupport;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.internal.text.InformationControlReplacer;
import org.eclipse.jface.internal.text.TableOwnerDrawSupport;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.viewers.StyledString;

import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.AbstractInformationControlManager.Anchor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IEditingSupport;
import org.eclipse.jface.text.IEditingSupportRegistry;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension;
import org.eclipse.jface.text.TextUtilities;


/**
 * This class is used to present proposals to the user. If additional
 * information exists for a proposal, then selecting that proposal
 * will result in the information being displayed in a secondary
 * window.
 *
 * @see org.eclipse.jface.text.contentassist.ICompletionProposal
 * @see org.eclipse.jface.text.contentassist.AdditionalInfoController
 */
class CompletionProposalPopup implements IContentAssistListener {
	/**
	 * Set to <code>true</code> to use a Table with SWT.VIRTUAL.
	 * XXX: This is a workaround for: https://bugs.eclipse.org/bugs/show_bug.cgi?id=90321
	 * 		More details see also: https://bugs.eclipse.org/bugs/show_bug.cgi?id=98585#c36
	 * @since 3.1
	 */
	private static final boolean USE_VIRTUAL= !"motif".equals(SWT.getPlatform()); //$NON-NLS-1$


	/**
	 * Completion proposal selection handler.
	 *
	 * @since 3.4
	 */
	final class ProposalSelectionHandler extends AbstractHandler {

		/**
		 * Selection operation codes.
		 */
		static final int SELECT_NEXT= 1;
		static final int SELECT_PREVIOUS= 2;


		private final int fOperationCode;

		/**
		 * Creates a new selection handler.
		 *
		 * @param operationCode the operation code
		 * @since 3.4
		 */
		public ProposalSelectionHandler(int operationCode) {
			Assert.isLegal(operationCode == SELECT_NEXT || operationCode == SELECT_PREVIOUS);
			fOperationCode= operationCode;
		}

		/*
		 * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
		 * @since 3.4
		 */
		public Object execute(ExecutionEvent event) throws ExecutionException {
			int itemCount= fProposalTable.getItemCount();
			int selectionIndex= fProposalTable.getSelectionIndex();
			switch (fOperationCode) {
			case SELECT_NEXT:
				selectionIndex+= 1;
				if (selectionIndex > itemCount - 1)
					selectionIndex= 0;
				break;
			case SELECT_PREVIOUS:
				selectionIndex-= 1;
				if (selectionIndex < 0)
					selectionIndex= itemCount - 1;
				break;
			}
			selectProposal(selectionIndex, false);
			return null;
		}

	}


	/**
	 * The empty proposal displayed if there is nothing else to show.
	 *
	 * @since 3.2
	 */
	private static final class EmptyProposal implements ICompletionProposal, ICompletionProposalExtension {

		String fDisplayString;
		int fOffset;
		/*
		 * @see ICompletionProposal#apply(IDocument)
		 */
		public void apply(IDocument document) {
		}

		/*
		 * @see ICompletionProposal#getSelection(IDocument)
		 */
		public Point getSelection(IDocument document) {
			return new Point(fOffset, 0);
		}

		/*
		 * @see ICompletionProposal#getContextInformation()
		 */
		public IContextInformation getContextInformation() {
			return null;
		}

		/*
		 * @see ICompletionProposal#getImage()
		 */
		public Image getImage() {
			return null;
		}

		/*
		 * @see ICompletionProposal#getDisplayString()
		 */
		public String getDisplayString() {
			return fDisplayString;
		}

		/*
		 * @see ICompletionProposal#getAdditionalProposalInfo()
		 */
		public String getAdditionalProposalInfo() {
			return null;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#apply(org.eclipse.jface.text.IDocument, char, int)
		 */
		public void apply(IDocument document, char trigger, int offset) {
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#isValidFor(org.eclipse.jface.text.IDocument, int)
		 */
		public boolean isValidFor(IDocument document, int offset) {
			return false;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#getTriggerCharacters()
		 */
		public char[] getTriggerCharacters() {
			return null;
		}

		/*
		 * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#getContextInformationPosition()
		 */
		public int getContextInformationPosition() {
			return -1;
		}
	}

	private final class ProposalSelectionListener implements KeyListener {
		public void keyPressed(KeyEvent e) {
			if (!Helper.okToUse(fProposalShell))
				return;

			if (e.character == 0 && e.keyCode == SWT.CTRL) {
				// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
				int index= fProposalTable.getSelectionIndex();
				if (index >= 0)
					selectProposal(index, true);
			}
		}

		public void keyReleased(KeyEvent e) {
			if (!Helper.okToUse(fProposalShell))
				return;

			if (e.character == 0 && e.keyCode == SWT.CTRL) {
				// http://dev.eclipse.org/bugs/show_bug.cgi?id=34754
				int index= fProposalTable.getSelectionIndex();
				if (index >= 0)
					selectProposal(index, false);
			}
		}
	}

	private final class CommandKeyListener extends KeyAdapter {
		private final KeySequence fCommandSequence;

		private CommandKeyListener(KeySequence keySequence) {
			fCommandSequence= keySequence;
		}

		public void keyPressed(KeyEvent e) {
			if (!Helper.okToUse(fProposalShell))
				return;

			int accelerator= SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
			KeySequence sequence= KeySequence.getInstance(SWTKeySupport.convertAcceleratorToKeyStroke(accelerator));
			if (sequence.equals(fCommandSequence))
				if (fContentAssistant.isPrefixCompletionEnabled())
					incrementalComplete();
				else
					showProposals(false);

		}
	}


	/** The associated text viewer. */
	private ITextViewer fViewer;
	/** The associated content assistant. */
	private final ContentAssistant fContentAssistant;
	/** The used additional info controller, or <code>null</code> if none. */
	private final AdditionalInfoController fAdditionalInfoController;
	/** The closing strategy for this completion proposal popup. */
	private final PopupCloser fPopupCloser= new PopupCloser();
	/** The popup shell. */
	private Shell fProposalShell;
	/** The proposal table. */
	private Table fProposalTable;
	/** Indicates whether a completion proposal is being inserted. */
	private boolean fInserting= false;
	/** The key listener to control navigation. */
	private ProposalSelectionListener fKeyListener;
	/** List of document events used for filtering proposals. */
	private final List fDocumentEvents= new ArrayList();
	/** Listener filling the document event queue. */
	private IDocumentListener fDocumentListener;
	/** The filter list of proposals. */
	private ICompletionProposal[] fFilteredProposals;
	/** The computed list of proposals. */
	private ICompletionProposal[] fComputedProposals;
	/** The offset for which the proposals have been computed. */
	private int fInvocationOffset;
	/** The offset for which the computed proposals have been filtered. */
	private int fFilterOffset;
	/**
	 * The most recently selected proposal.
	 * @since 3.0
	 */
	private ICompletionProposal fLastProposal;
	/**
	 * The content assist subject control.
	 * This replaces <code>fViewer</code>
	 *
	 * @since 3.0
	 */
	private IContentAssistSubjectControl fContentAssistSubjectControl;
	/**
	 * The content assist subject control adapter.
	 * This replaces <code>fViewer</code>
	 *
	 * @since 3.0
	 */
	private final ContentAssistSubjectControlAdapter fContentAssistSubjectControlAdapter;
	/**
	 * Remembers the size for this completion proposal popup.
	 * @since 3.0
	 */
	private Point fSize;
	/**
	 * Editor helper that communicates that the completion proposal popup may
	 * have focus while the 'logical focus' is still with the editor.
	 * @since 3.1
	 */
	private IEditingSupport fFocusHelper;
	/**
	 * Set to true by {@link #computeFilteredProposals(int, DocumentEvent)} if
	 * the returned proposals are a subset of {@link #fFilteredProposals},
	 * <code>false</code> if not.
	 * @since 3.1
	 */
	private boolean fIsFilteredSubset;
	/**
	 * The filter runnable.
	 *
	 * @since 3.1.1
	 */
	private final Runnable fFilterRunnable= new Runnable() {
		public void run() {
			if (!fIsFilterPending)
				return;

			fIsFilterPending= false;

			if (!Helper.okToUse(fContentAssistSubjectControlAdapter.getControl()))
				return;

			int offset= fContentAssistSubjectControlAdapter.getSelectedRange().x;
			ICompletionProposal[] proposals= null;
			try  {
				if (offset > -1) {
					DocumentEvent event= TextUtilities.mergeProcessedDocumentEvents(fDocumentEvents);
					proposals= computeFilteredProposals(offset, event);
				}
			} catch (BadLocationException x)  {
			} finally  {
				fDocumentEvents.clear();
			}
			fFilterOffset= offset;

			if (proposals != null && proposals.length > 0)
				setProposals(proposals, fIsFilteredSubset);
			else
				hide();
		}
	};
	/**
	 * <code>true</code> if <code>fFilterRunnable</code> has been
	 * posted, <code>false</code> if not.
	 *
	 * @since 3.1.1
	 */
	private boolean fIsFilterPending= false;
	/**
	 * The info message at the bottom of the popup, or <code>null</code> for no popup (if
	 * ContentAssistant does not provide one).
	 *
	 * @since 3.2
	 */
	private Label fMessageText;
	/**
	 * The font used for <code>fMessageText</code> or null; dispose when done.
	 *
	 * @since 3.2
	 */
	private Font fMessageTextFont;
	/**
	 * The most recent completion offset (used to determine repeated invocation)
	 *
	 * @since 3.2
	 */
	private int fLastCompletionOffset;
	/**
	 * The (reusable) empty proposal.
	 *
	 * @since 3.2
	 */
	private final EmptyProposal fEmptyProposal= new EmptyProposal();
	/**
	 * The text for the empty proposal, or <code>null</code> to use the default text.
	 *
	 * @since 3.2
	 */
	private String fEmptyMessage= null;
	/**
	 * Tells whether colored labels support is enabled.
	 * Only valid while the popup is active.
	 *
	 * @since 3.4
	 */
	private boolean fIsColoredLabelsSupportEnabled= false;


	/**
	 * Creates a new completion proposal popup for the given elements.
	 *
	 * @param contentAssistant the content assistant feeding this popup
	 * @param viewer the viewer on top of which this popup appears
	 * @param infoController the information control collaborating with this popup, or <code>null</code>
	 * @since 2.0
	 */
	public CompletionProposalPopup(ContentAssistant contentAssistant, ITextViewer viewer, AdditionalInfoController infoController) {
		fContentAssistant= contentAssistant;
		fViewer= viewer;
		fAdditionalInfoController= infoController;
		fContentAssistSubjectControlAdapter= new ContentAssistSubjectControlAdapter(fViewer);
	}

	/**
	 * Creates a new completion proposal popup for the given elements.
	 *
	 * @param contentAssistant the content assistant feeding this popup
	 * @param contentAssistSubjectControl the content assist subject control on top of which this popup appears
	 * @param infoController the information control collaborating with this popup, or <code>null</code>
	 * @since 3.0
	 */
	public CompletionProposalPopup(ContentAssistant contentAssistant, IContentAssistSubjectControl contentAssistSubjectControl, AdditionalInfoController infoController) {
		fContentAssistant= contentAssistant;
		fContentAssistSubjectControl= contentAssistSubjectControl;
		fAdditionalInfoController= infoController;
		fContentAssistSubjectControlAdapter= new ContentAssistSubjectControlAdapter(fContentAssistSubjectControl);
	}

	/**
	 * Computes and presents completion proposals. The flag indicates whether this call has
	 * be made out of an auto activation context.
	 *
	 * @param autoActivated <code>true</code> if auto activation context
	 * @return an error message or <code>null</code> in case of no error
	 */
	public String showProposals(final boolean autoActivated) {

		if (fKeyListener == null)
			fKeyListener= new ProposalSelectionListener();

		final Control control= fContentAssistSubjectControlAdapter.getControl();

		if (!Helper.okToUse(fProposalShell) && control != null && !control.isDisposed()) {
			// add the listener before computing the proposals so we don't move the caret
			// when the user types fast.
			fContentAssistSubjectControlAdapter.addKeyListener(fKeyListener);

			BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
				public void run() {

					fInvocationOffset= fContentAssistSubjectControlAdapter.getSelectedRange().x;
					fFilterOffset= fInvocationOffset;
					fLastCompletionOffset= fFilterOffset;
					fComputedProposals= computeProposals(fInvocationOffset);

					int count= (fComputedProposals == null ? 0 : fComputedProposals.length);
					if (count == 0 && hideWhenNoProposals(autoActivated))
						return;

					if (count == 1 && !autoActivated && canAutoInsert(fComputedProposals[0])) {
						insertProposal(fComputedProposals[0], (char) 0, 0, fInvocationOffset);
						hide();
					} else {
						createProposalSelector();
						setProposals(fComputedProposals, false);
						displayProposals();
					}
				}
			});
		} else {
			fLastCompletionOffset= fFilterOffset;
			handleRepeatedInvocation();
		}

		return getErrorMessage();
	}

	/**
	 * Hides the popup and returns <code>true</code> if the popup is configured
	 * to never display an empty list. Returns <code>false</code> otherwise.
	 *
	 * @param autoActivated whether the invocation was auto-activated
	 * @return <code>false</code> if an empty list should be displayed, <code>true</code> otherwise
	 * @since 3.2
	 */
	private boolean hideWhenNoProposals(boolean autoActivated) {
		if (autoActivated || !fContentAssistant.isShowEmptyList()) {
			if (!autoActivated) {
				Control control= fContentAssistSubjectControlAdapter.getControl();
				if (control != null && !control.isDisposed())
					control.getDisplay().beep();
			}
			hide();
			return true;
		}
		return false;
	}

	/**
	 * If content assist is set up to handle cycling, then the proposals are recomputed. Otherwise,
	 * nothing happens.
	 *
	 * @since 3.2
	 */
	private void handleRepeatedInvocation() {
		if (fContentAssistant.isRepeatedInvocationMode()) {
			fComputedProposals= computeProposals(fFilterOffset);
			setProposals(fComputedProposals, false);
		}
	}

	/**
	 * Returns the completion proposal available at the given offset of the
	 * viewer's document. Delegates the work to the content assistant.
	 *
	 * @param offset the offset
	 * @return the completion proposals available at this offset
	 */
	private ICompletionProposal[] computeProposals(int offset) {
		if (fContentAssistSubjectControl != null)
			return fContentAssistant.computeCompletionProposals(fContentAssistSubjectControl, offset);
		return fContentAssistant.computeCompletionProposals(fViewer, offset);
	}

	/**
	 * Returns the error message.
	 *
	 * @return the error message
	 */
	private String getErrorMessage() {
		return fContentAssistant.getErrorMessage();
	}

	/**
	 * Creates the proposal selector.
	 */
	private void createProposalSelector() {
		if (Helper.okToUse(fProposalShell))
			return;

		Control control= fContentAssistSubjectControlAdapter.getControl();
		fProposalShell= new Shell(control.getShell(), SWT.ON_TOP | SWT.RESIZE );
		fProposalShell.setFont(JFaceResources.getDefaultFont());
		if (USE_VIRTUAL) {
			fProposalTable= new Table(fProposalShell, SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL);

			Listener listener= new Listener() {
				public void handleEvent(Event event) {
					handleSetData(event);
				}
			};
			fProposalTable.addListener(SWT.SetData, listener);
		} else {
			fProposalTable= new Table(fProposalShell, SWT.H_SCROLL | SWT.V_SCROLL);
		}

		fIsColoredLabelsSupportEnabled= fContentAssistant.isColoredLabelsSupportEnabled();
		if (fIsColoredLabelsSupportEnabled)
			TableOwnerDrawSupport.install(fProposalTable);

		fProposalTable.setLocation(0, 0);
		if (fAdditionalInfoController != null)
			fAdditionalInfoController.setSizeConstraints(50, 10, true, true);

		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.marginHeight= 0;
		layout.verticalSpacing= 1;
		fProposalShell.setLayout(layout);

		if (fContentAssistant.isStatusLineVisible()) {
			createMessageText();
		}

		GridData data= new GridData(GridData.FILL_BOTH);

		Point size= fContentAssistant.restoreCompletionProposalPopupSize();
		if (size != null) {
			fProposalTable.setLayoutData(data);
			fProposalShell.setSize(size);
		} else {
			int height= fProposalTable.getItemHeight() * 10;
			// use golden ratio as default aspect ratio
			final double aspectRatio= (1 + Math.sqrt(5)) / 2;
			int width= (int) (height * aspectRatio);
			Rectangle trim= fProposalTable.computeTrim(0, 0, width, height);
			data.heightHint= trim.height;
			data.widthHint= trim.width;
			fProposalTable.setLayoutData(data);
			fProposalShell.pack();
		}
		fContentAssistant.addToLayout(this, fProposalShell, ContentAssistant.LayoutManager.LAYOUT_PROPOSAL_SELECTOR, fContentAssistant.getSelectionOffset());

		fProposalShell.addControlListener(new ControlListener() {

			public void controlMoved(ControlEvent e) {}

			public void controlResized(ControlEvent e) {
				if (fAdditionalInfoController != null) {
					// reset the cached resize constraints
					fAdditionalInfoController.setSizeConstraints(50, 10, true, false);
					fAdditionalInfoController.hideInformationControl();
					fAdditionalInfoController.handleTableSelectionChanged();
				}

				fSize= fProposalShell.getSize();
			}
		});

		fProposalShell.setBackground(control.getDisplay().getSystemColor(SWT.COLOR_GRAY));

		Color c= getBackgroundColor(control);
		fProposalTable.setBackground(c);

		c= getForegroundColor(control);
		fProposalTable.setForeground(c);

		fProposalTable.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {}

			public void widgetDefaultSelected(SelectionEvent e) {
				insertSelectedProposalWithMask(e.stateMask);
			}
		});

		fPopupCloser.install(fContentAssistant, fProposalTable, fAdditionalInfoController);

		fProposalShell.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				unregister(); // but don't dispose the shell, since we're being called from its disposal event!
			}
		});

		fProposalTable.setHeaderVisible(false);

		addCommandSupport(fProposalTable);
	}

	/**
	 * Returns the minimal required height for the proposal, may return 0 if the popup has not been
	 * created yet.
	 *
	 * @return the minimal height
	 * @since 3.3
	 */
	int getMinimalHeight() {
		int height= 0;
		if (Helper.okToUse(fProposalTable)) {
			int items= fProposalTable.getItemHeight() * 10;
			Rectangle trim= fProposalTable.computeTrim(0, 0, SWT.DEFAULT, items);
			height= trim.height;
		}
		if (Helper.okToUse(fMessageText))
			height+= fMessageText.getSize().y + 1;
		return height;
	}

	/**
	 * Adds command support to the given control.
	 *
	 * @param control the control to watch for focus
	 * @since 3.2
	 */
    private void addCommandSupport(final Control control) {
    	final KeySequence commandSequence= fContentAssistant.getRepeatedInvocationKeySequence();
    	if (commandSequence != null && !commandSequence.isEmpty() && fContentAssistant.isRepeatedInvocationMode()) {
    		control.addFocusListener(new FocusListener() {
    			private CommandKeyListener fCommandKeyListener;
    			public void focusGained(FocusEvent e) {
    				if (Helper.okToUse(control)) {
    					if (fCommandKeyListener == null) {
    						fCommandKeyListener= new CommandKeyListener(commandSequence);
    						fProposalTable.addKeyListener(fCommandKeyListener);
    					}
    				}
    			}
    			public void focusLost(FocusEvent e) {
    				if (fCommandKeyListener != null) {
    					control.removeKeyListener(fCommandKeyListener);
    					fCommandKeyListener= null;
    				}
    			}
    		});
    	}
    	if (fAdditionalInfoController != null) {
	    	control.addFocusListener(new FocusListener() {
	    		private TraverseListener fTraverseListener;
	    		public void focusGained(FocusEvent e) {
	    			if (Helper.okToUse(control)) {
	    				if (fTraverseListener == null) {
	    					fTraverseListener= new TraverseListener() {
	    						public void keyTraversed(TraverseEvent event) {
	    							if (event.detail == SWT.TRAVERSE_TAB_NEXT) {
	    								IInformationControl iControl= fAdditionalInfoController.getCurrentInformationControl2();
	    								if (fAdditionalInfoController.getInternalAccessor().canReplace(iControl)) {
	    									fAdditionalInfoController.getInternalAccessor().replaceInformationControl(true);
	    									event.doit= false;
	    								}
	    							}
	    						}
	    					};
	    					fProposalTable.addTraverseListener(fTraverseListener);
	    				}
	    			}
	    		}
	    		public void focusLost(FocusEvent e) {
	    			if (fTraverseListener != null) {
	    				control.removeTraverseListener(fTraverseListener);
	    				fTraverseListener= null;
	    			}
	    		}
	    	});
    	}
    }

	/**
	 * Returns the background color to use.
	 *
	 * @param control the control to get the display from
	 * @return the background color
	 * @since 3.2
	 */
	private Color getBackgroundColor(Control control) {
		Color c= fContentAssistant.getProposalSelectorBackground();
		if (c == null)
			c= JFaceResources.getColorRegistry().get(JFacePreferences.CONTENT_ASSIST_BACKGROUND_COLOR);
		return c;
	}

	/**
	 * Returns the foreground color to use.
	 *
	 * @param control the control to get the display from
	 * @return the foreground color
	 * @since 3.2
	 */
	private Color getForegroundColor(Control control) {
		Color c= fContentAssistant.getProposalSelectorForeground();
		if (c == null)
			c= JFaceResources.getColorRegistry().get(JFacePreferences.CONTENT_ASSIST_FOREGROUND_COLOR);
		return c;
	}

	/**
	 * Creates the caption line under the proposal table.
	 *
	 * @since 3.2
	 */
	private void createMessageText() {
		if (fMessageText == null) {
			fMessageText= new Label(fProposalShell, SWT.RIGHT);
			GridData textData= new GridData(SWT.FILL, SWT.BOTTOM, true, false);
			fMessageText.setLayoutData(textData);
			fMessageText.setText(fContentAssistant.getStatusMessage() + " "); //$NON-NLS-1$
			if (fMessageTextFont == null) {
				Font font= fMessageText.getFont();
				Display display= fProposalShell.getDisplay();
				FontData[] fontDatas= font.getFontData();
				for (int i= 0; i < fontDatas.length; i++)
					fontDatas[i].setHeight(fontDatas[i].getHeight() * 9 / 10);
				fMessageTextFont= new Font(display, fontDatas);
			}
			fMessageText.setFont(fMessageTextFont);
			fMessageText.setBackground(getBackgroundColor(fProposalShell));
			fMessageText.setForeground(getForegroundColor(fProposalShell));

			if (fContentAssistant.isRepeatedInvocationMode()) {
				fMessageText.setCursor(fProposalShell.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
				fMessageText.addMouseListener(new MouseAdapter() {
					public void mouseUp(MouseEvent e) {
						fLastCompletionOffset= fFilterOffset;
						fProposalTable.setFocus();
						handleRepeatedInvocation();
					}

					public void mouseDown(MouseEvent e) {
					}
				});
			}
		}
	}

	/*
	 * @since 3.1
	 */
	private void handleSetData(Event event) {
		TableItem item= (TableItem) event.item;
		int index= fProposalTable.indexOf(item);

		if (0 <= index && index < fFilteredProposals.length) {
			ICompletionProposal current= fFilteredProposals[index];

			String displayString;
			StyleRange[] styleRanges= null;
			if (fIsColoredLabelsSupportEnabled && current instanceof ICompletionProposalExtension6) {
				StyledString styledString= ((ICompletionProposalExtension6)current).getStyledDisplayString();
				displayString= styledString.getString();
				styleRanges= styledString.getStyleRanges();
			} else
				displayString= current.getDisplayString();

			item.setText(displayString);
			if (fIsColoredLabelsSupportEnabled)
				TableOwnerDrawSupport.storeStyleRanges(item, 0, styleRanges);

			item.setImage(current.getImage());
			item.setData(current);
		} else {
			// this should not happen, but does on win32
		}
	}

	/**
	 * Returns the proposal selected in the proposal selector.
	 *
	 * @return the selected proposal
	 * @since 2.0
	 */
	private ICompletionProposal getSelectedProposal() {
		/* Make sure that there is no filter runnable pending.
		 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=31427
		 */
		if (fIsFilterPending)
			fFilterRunnable.run();

		// filter runnable may have hidden the proposals
		if (!Helper.okToUse(fProposalTable))
			return null;

		int i= fProposalTable.getSelectionIndex();
		if (fFilteredProposals == null || i < 0 || i >= fFilteredProposals.length)
			return null;
		return fFilteredProposals[i];
	}

	/**
	 * Takes the selected proposal and applies it.
	 *
	 * @param stateMask the state mask
	 * @since 3.2
	 */
	private void insertSelectedProposalWithMask(int stateMask) {
		ICompletionProposal p= getSelectedProposal();
		hide();
		if (p != null)
			insertProposal(p, (char) 0, stateMask, fContentAssistSubjectControlAdapter.getSelectedRange().x);
	}

	/**
	 * Applies the given proposal at the given offset. The given character is the
	 * one that triggered the insertion of this proposal.
	 *
	 * @param p the completion proposal
	 * @param trigger the trigger character
	 * @param stateMask the state mask
	 * @param offset the offset
	 * @since 2.1
	 */
	private void insertProposal(ICompletionProposal p, char trigger, int stateMask, final int offset) {

		fInserting= true;
		IRewriteTarget target= null;
		IEditingSupport helper= new IEditingSupport() {

			public boolean isOriginator(DocumentEvent event, IRegion focus) {
				return focus.getOffset() <= offset && focus.getOffset() + focus.getLength() >= offset;
			}

			public boolean ownsFocusShell() {
				return false;
			}

		};

		try {

			IDocument document= fContentAssistSubjectControlAdapter.getDocument();

			if (fViewer instanceof ITextViewerExtension) {
				ITextViewerExtension extension= (ITextViewerExtension) fViewer;
				target= extension.getRewriteTarget();
			}

			if (target != null)
				target.beginCompoundChange();

			if (fViewer instanceof IEditingSupportRegistry) {
				IEditingSupportRegistry registry= (IEditingSupportRegistry) fViewer;
				registry.register(helper);
			}


			if (p instanceof ICompletionProposalExtension2 && fViewer != null) {
				ICompletionProposalExtension2 e= (ICompletionProposalExtension2) p;
				e.apply(fViewer, trigger, stateMask, offset);
			} else if (p instanceof ICompletionProposalExtension) {
				ICompletionProposalExtension e= (ICompletionProposalExtension) p;
				e.apply(document, trigger, offset);
			} else {
				p.apply(document);
			}

			Point selection= p.getSelection(document);
			if (selection != null) {
				fContentAssistSubjectControlAdapter.setSelectedRange(selection.x, selection.y);
				fContentAssistSubjectControlAdapter.revealRange(selection.x, selection.y);
			}

			IContextInformation info= p.getContextInformation();
			if (info != null) {

				int contextInformationOffset;
				if (p instanceof ICompletionProposalExtension) {
					ICompletionProposalExtension e= (ICompletionProposalExtension) p;
					contextInformationOffset= e.getContextInformationPosition();
				} else {
					if (selection == null)
						selection= fContentAssistSubjectControlAdapter.getSelectedRange();
					contextInformationOffset= selection.x + selection.y;
				}

				fContentAssistant.showContextInformation(info, contextInformationOffset);
			} else
				fContentAssistant.showContextInformation(null, -1);


		} finally {
			if (target != null)
				target.endCompoundChange();

			if (fViewer instanceof IEditingSupportRegistry) {
				IEditingSupportRegistry registry= (IEditingSupportRegistry) fViewer;
				registry.unregister(helper);
			}
			fInserting= false;
		}
	}

	/**
	 * Returns whether this popup has the focus.
	 *
	 * @return <code>true</code> if the popup has the focus
	 */
	public boolean hasFocus() {
		if (Helper.okToUse(fProposalShell)) {
			if ((fProposalShell.getDisplay().getActiveShell() == fProposalShell))
				return true;
			/*
			 * We have to delegate this query to the additional info controller
			 * as well, since the content assistant is the widget token owner
			 * and its closer does not know that the additional info control can
			 * now also take focus.
			 */
			if (fAdditionalInfoController != null) {
				IInformationControl informationControl= fAdditionalInfoController.getCurrentInformationControl2();
				if (informationControl != null && informationControl.isFocusControl())
					return true;
				InformationControlReplacer replacer= fAdditionalInfoController.getInternalAccessor().getInformationControlReplacer();
				if (replacer != null) {
					informationControl= replacer.getCurrentInformationControl2();
					if (informationControl != null && informationControl.isFocusControl())
						return true;
				}
			}
		}

		return false;
	}

	/**
	 * Hides this popup.
	 */
	public void hide() {

		unregister();

		if (fViewer instanceof IEditingSupportRegistry) {
			IEditingSupportRegistry registry= (IEditingSupportRegistry) fViewer;
			registry.unregister(fFocusHelper);
		}

		if (Helper.okToUse(fProposalShell)) {

			fContentAssistant.removeContentAssistListener(this, ContentAssistant.PROPOSAL_SELECTOR);

			fPopupCloser.uninstall();
			fProposalShell.setVisible(false);
			fProposalShell.dispose();
			fProposalShell= null;
		}

		if (fMessageTextFont != null) {
			fMessageTextFont.dispose();
			fMessageTextFont= null;
		}

		if (fMessageText != null) {
			fMessageText= null;
		}

		fEmptyMessage= null;

		fLastCompletionOffset= -1;

		fContentAssistant.fireSessionEndEvent();
	}

	/**
	 * Unregister this completion proposal popup.
	 *
	 * @since 3.0
	 */
	private void unregister() {
		if (fDocumentListener != null) {
			IDocument document= fContentAssistSubjectControlAdapter.getDocument();
			if (document != null)
				document.removeDocumentListener(fDocumentListener);
			fDocumentListener= null;
		}
		fDocumentEvents.clear();

		if (fKeyListener != null && fContentAssistSubjectControlAdapter.getControl() != null && !fContentAssistSubjectControlAdapter.getControl().isDisposed()) {
			fContentAssistSubjectControlAdapter.removeKeyListener(fKeyListener);
			fKeyListener= null;
		}

		if (fLastProposal != null) {
			if (fLastProposal instanceof ICompletionProposalExtension2 && fViewer != null) {
				ICompletionProposalExtension2 extension= (ICompletionProposalExtension2) fLastProposal;
				extension.unselected(fViewer);
			}
			fLastProposal= null;
		}

		fFilteredProposals= null;
		fComputedProposals= null;

		fContentAssistant.possibleCompletionsClosed();
	}

	/**
	 *Returns whether this popup is active. It is active if the proposal selector is visible.
	 *
	 * @return <code>true</code> if this popup is active
	 */
	public boolean isActive() {
		return fProposalShell != null && !fProposalShell.isDisposed();
	}

	/**
	 * Initializes the proposal selector with these given proposals.
	 * @param proposals the proposals
	 * @param isFilteredSubset if <code>true</code>, the proposal table is
	 *        not cleared, but the proposals that are not in the passed array
	 *        are removed from the displayed set
	 */
	private void setProposals(ICompletionProposal[] proposals, boolean isFilteredSubset) {
		ICompletionProposal[] oldProposals= fFilteredProposals;
		ICompletionProposal oldProposal= getSelectedProposal(); // may trigger filtering and a reentrant call to setProposals()
		if (oldProposals != fFilteredProposals) // reentrant call was first - abort
			return;

		if (Helper.okToUse(fProposalTable)) {
			if (oldProposal instanceof ICompletionProposalExtension2 && fViewer != null)
				((ICompletionProposalExtension2) oldProposal).unselected(fViewer);

			if (proposals == null || proposals.length == 0) {
				fEmptyProposal.fOffset= fFilterOffset;
				fEmptyProposal.fDisplayString= fEmptyMessage != null ? fEmptyMessage : JFaceTextMessages.getString("CompletionProposalPopup.no_proposals"); //$NON-NLS-1$
				proposals= new ICompletionProposal[] { fEmptyProposal };
			}

			fFilteredProposals= proposals;
			final int newLen= proposals.length;
			if (USE_VIRTUAL) {
				fProposalTable.setItemCount(newLen);
				fProposalTable.clearAll();
			} else {
				fProposalTable.setRedraw(false);
				fProposalTable.setItemCount(newLen);
				TableItem[] items= fProposalTable.getItems();
				for (int i= 0; i < items.length; i++) {
					TableItem item= items[i];
					ICompletionProposal proposal= proposals[i];
					item.setText(proposal.getDisplayString());
					item.setImage(proposal.getImage());
					item.setData(proposal);
				}
				fProposalTable.setRedraw(true);
			}

			Point currentLocation= fProposalShell.getLocation();
			Point newLocation= getLocation();
			if ((newLocation.x < currentLocation.x && newLocation.y == currentLocation.y) || newLocation.y < currentLocation.y)
				fProposalShell.setLocation(newLocation);

			selectProposal(0, false);
		}
	}

	/**
	 * Returns the graphical location at which this popup should be made visible.
	 *
	 * @return the location of this popup
	 */
	private Point getLocation() {
		int caret= fContentAssistSubjectControlAdapter.getCaretOffset();
		Rectangle location= fContentAssistant.getLayoutManager().computeBoundsBelowAbove(fProposalShell, fSize == null ? fProposalShell.getSize() : fSize, caret, this);
		return Geometry.getLocation(location);
	}

	/**
	 * Returns the size of this completion proposal popup.
	 *
	 * @return a Point containing the size
	 * @since 3.0
	 */
	Point getSize() {
		return fSize;
	}

	/**
	 * Displays this popup and install the additional info controller, so that additional info
	 * is displayed when a proposal is selected and additional info is available.
	 */
	private void displayProposals() {

		if (!Helper.okToUse(fProposalShell) ||  !Helper.okToUse(fProposalTable))
			return;

		if (fContentAssistant.addContentAssistListener(this, ContentAssistant.PROPOSAL_SELECTOR)) {

			ensureDocumentListenerInstalled();

			if (fFocusHelper == null) {
				fFocusHelper= new IEditingSupport() {

					public boolean isOriginator(DocumentEvent event, IRegion focus) {
						return false; // this helper just covers the focus change to the proposal shell, no remote editions
					}

					public boolean ownsFocusShell() {
						return true;
					}

				};
			}
			if (fViewer instanceof IEditingSupportRegistry) {
				IEditingSupportRegistry registry= (IEditingSupportRegistry) fViewer;
				registry.register(fFocusHelper);
			}


			/* https://bugs.eclipse.org/bugs/show_bug.cgi?id=52646
			 * on GTK, setVisible and such may run the event loop
			 * (see also https://bugs.eclipse.org/bugs/show_bug.cgi?id=47511)
			 * Since the user may have already canceled the popup or selected
			 * an entry (ESC or RETURN), we have to double check whether
			 * the table is still okToUse. See comments below
			 */
			fProposalShell.setVisible(true); // may run event loop on GTK
			// transfer focus since no verify key listener can be attached
			if (!fContentAssistSubjectControlAdapter.supportsVerifyKeyListener() && Helper.okToUse(fProposalShell))
				fProposalShell.setFocus(); // may run event loop on GTK ??

			if (fAdditionalInfoController != null && Helper.okToUse(fProposalTable)) {
				fAdditionalInfoController.install(fProposalTable);
				fAdditionalInfoController.handleTableSelectionChanged();
			}
		} else
			hide();
	}

	/**
	 * Installs the document listener if not already done.
	 *
	 * @since 3.2
	 */
	private void ensureDocumentListenerInstalled() {
		if (fDocumentListener == null) {
			fDocumentListener=  new IDocumentListener()  {
				public void documentAboutToBeChanged(DocumentEvent event) {
					if (!fInserting)
						fDocumentEvents.add(event);
				}

				public void documentChanged(DocumentEvent event) {
					if (!fInserting)
						filterProposals();
				}
			};
			IDocument document= fContentAssistSubjectControlAdapter.getDocument();
			if (document != null)
				document.addDocumentListener(fDocumentListener);
		}
	}

	/*
	 * @see IContentAssistListener#verifyKey(VerifyEvent)
	 */
	public boolean verifyKey(VerifyEvent e) {
		if (!Helper.okToUse(fProposalShell))
			return true;

		char key= e.character;
		if (key == 0) {
			int newSelection= fProposalTable.getSelectionIndex();
			int visibleRows= (fProposalTable.getSize().y / fProposalTable.getItemHeight()) - 1;
			int itemCount= fProposalTable.getItemCount();
			switch (e.keyCode) {

				case SWT.ARROW_LEFT :
				case SWT.ARROW_RIGHT :
					filterProposals();
					return true;

				case SWT.ARROW_UP :
					newSelection -= 1;
					if (newSelection < 0)
						newSelection= itemCount - 1;
					break;

				case SWT.ARROW_DOWN :
					newSelection += 1;
					if (newSelection > itemCount - 1)
						newSelection= 0;
					break;

				case SWT.PAGE_DOWN :
					newSelection += visibleRows;
					if (newSelection >= itemCount)
						newSelection= itemCount - 1;
					break;

				case SWT.PAGE_UP :
					newSelection -= visibleRows;
					if (newSelection < 0)
						newSelection= 0;
					break;

				case SWT.HOME :
					newSelection= 0;
					break;

				case SWT.END :
					newSelection= itemCount - 1;
					break;

				default :
					if (e.keyCode != SWT.CAPS_LOCK && e.keyCode != SWT.MOD1 && e.keyCode != SWT.MOD2 && e.keyCode != SWT.MOD3 && e.keyCode != SWT.MOD4)
						hide();
					return true;
			}

			selectProposal(newSelection, (e.stateMask & SWT.CTRL) != 0);

			e.doit= false;
			return false;

		}

		// key != 0
		switch (key) {
		case 0x1B: // Esc
				e.doit= false; // fall-thru, no line number changes
		case '.': hide();
				break;

			case '\n': // Ctrl-Enter on w2k
			case '\r': // Enter
				e.doit= false;
				insertSelectedProposalWithMask(e.stateMask);
				break;

			case '\t':
				e.doit= false;
				fProposalShell.setFocus();
				return false;

			default:
				ICompletionProposal p= getSelectedProposal();
				if (p instanceof ICompletionProposalExtension) {
					ICompletionProposalExtension t= (ICompletionProposalExtension) p;
					char[] triggers= t.getTriggerCharacters();
					if (contains(triggers, key)) {
						e.doit= false;
						hide();
						insertProposal(p, key, e.stateMask, fContentAssistSubjectControlAdapter.getSelectedRange().x);
					}
			}
		}

		return true;
	}

	/**
	 * Selects the entry with the given index in the proposal selector and feeds
	 * the selection to the additional info controller.
	 *
	 * @param index the index in the list
	 * @param smartToggle <code>true</code> if the smart toggle key has been pressed
	 * @since 2.1
	 */
	private void selectProposal(int index, boolean smartToggle) {

		ICompletionProposal oldProposal= getSelectedProposal();
		if (oldProposal instanceof ICompletionProposalExtension2 && fViewer != null)
			((ICompletionProposalExtension2) oldProposal).unselected(fViewer);

		if (fFilteredProposals == null) {
			fireSelectionEvent(null, smartToggle);
			return;
		}

		ICompletionProposal proposal= fFilteredProposals[index];
		if (proposal instanceof ICompletionProposalExtension2 && fViewer != null)
			((ICompletionProposalExtension2) proposal).selected(fViewer, smartToggle);

		fireSelectionEvent(proposal, smartToggle);

		fLastProposal= proposal;

		fProposalTable.setSelection(index);
		fProposalTable.showSelection();
		if (fAdditionalInfoController != null)
			fAdditionalInfoController.handleTableSelectionChanged();
	}

	/**
	 * Fires a selection event, see {@link ICompletionListener}.
	 *
	 * @param proposal the selected proposal, possibly <code>null</code>
	 * @param smartToggle true if the smart toggle is on
	 * @since 3.2
	 */
	private void fireSelectionEvent(ICompletionProposal proposal, boolean smartToggle) {
		fContentAssistant.fireSelectionEvent(proposal, smartToggle);
	}

	/**
	 * Returns whether the given character is contained in the given array of
	 * characters.
	 *
	 * @param characters the list of characters
	 * @param c the character to look for in the list
	 * @return <code>true</code> if character belongs to the list
	 * @since 2.0
	 */
	private boolean contains(char[] characters, char c) {

		if (characters == null)
			return false;

		for (int i= 0; i < characters.length; i++) {
			if (c == characters[i])
				return true;
		}

		return false;
	}

	/*
	 * @see IEventConsumer#processEvent(VerifyEvent)
	 */
	public void processEvent(VerifyEvent e) {
	}

	/**
	 * Filters the displayed proposal based on the given cursor position and the
	 * offset of the original invocation of the content assistant.
	 */
	private void filterProposals() {
		if (!fIsFilterPending) {
			fIsFilterPending= true;
			Control control= fContentAssistSubjectControlAdapter.getControl();
			control.getDisplay().asyncExec(fFilterRunnable);
		}
	}

	/**
	 * Computes the subset of already computed proposals that are still valid for
	 * the given offset.
	 *
	 * @param offset the offset
	 * @param event the merged document event
	 * @return the set of filtered proposals
	 * @since 3.0
	 */
	private ICompletionProposal[] computeFilteredProposals(int offset, DocumentEvent event) {

		if (offset == fInvocationOffset && event == null) {
			fIsFilteredSubset= false;
			return fComputedProposals;
		}

		if (offset < fInvocationOffset) {
			fIsFilteredSubset= false;
			fInvocationOffset= offset;
			fContentAssistant.fireSessionRestartEvent();
			fComputedProposals= computeProposals(fInvocationOffset);
			return fComputedProposals;
		}

		ICompletionProposal[] proposals;
		if (offset < fFilterOffset) {
			proposals= fComputedProposals;
			fIsFilteredSubset= false;
		} else {
			proposals= fFilteredProposals;
			fIsFilteredSubset= true;
		}

		if (proposals == null) {
			fIsFilteredSubset= false;
			return null;
		}

		IDocument document= fContentAssistSubjectControlAdapter.getDocument();
		int length= proposals.length;
		List filtered= new ArrayList(length);
		for (int i= 0; i < length; i++) {

			if (proposals[i] instanceof ICompletionProposalExtension2) {

				ICompletionProposalExtension2 p= (ICompletionProposalExtension2) proposals[i];
				if (p.validate(document, offset, event))
					filtered.add(p);

			} else if (proposals[i] instanceof ICompletionProposalExtension) {

				ICompletionProposalExtension p= (ICompletionProposalExtension) proposals[i];
				if (p.isValidFor(document, offset))
					filtered.add(p);

			} else {
				// restore original behavior
				fIsFilteredSubset= false;
				fInvocationOffset= offset;
				fContentAssistant.fireSessionRestartEvent();
				fComputedProposals= computeProposals(fInvocationOffset);
				return fComputedProposals;
			}
		}

		return (ICompletionProposal[]) filtered.toArray(new ICompletionProposal[filtered.size()]);
	}

	/**
	 * Requests the proposal shell to take focus.
	 *
	 * @since 3.0
	 */
	public void setFocus() {
		if (Helper.okToUse(fProposalShell)) {
			fProposalShell.setFocus();
		}
	}

	/**
	 * Returns <code>true</code> if <code>proposal</code> should be auto-inserted,
	 * <code>false</code> otherwise.
	 *
	 * @param proposal the single proposal that might be automatically inserted
	 * @return <code>true</code> if <code>proposal</code> can be inserted automatically,
	 *         <code>false</code> otherwise
	 * @since 3.1
	 */
	private boolean canAutoInsert(ICompletionProposal proposal) {
		if (fContentAssistant.isAutoInserting()) {
			if (proposal instanceof ICompletionProposalExtension4) {
				ICompletionProposalExtension4 ext= (ICompletionProposalExtension4) proposal;
				return ext.isAutoInsertable();
			}
			return true; // default behavior before ICompletionProposalExtension4 was introduced
		}
		return false;
	}

	/**
	 * Completes the common prefix of all proposals directly in the code. If no
	 * common prefix can be found, the proposal popup is shown.
	 *
	 * @return an error message if completion failed.
	 * @since 3.0
	 */
	public String incrementalComplete() {
		if (Helper.okToUse(fProposalShell) && fFilteredProposals != null) {
			if (fLastCompletionOffset == fFilterOffset) {
				handleRepeatedInvocation();
			} else {
				fLastCompletionOffset= fFilterOffset;
				completeCommonPrefix();
			}
		} else {
			final Control control= fContentAssistSubjectControlAdapter.getControl();

			if (fKeyListener == null)
				fKeyListener= new ProposalSelectionListener();

			if (!Helper.okToUse(fProposalShell) && !control.isDisposed())
				fContentAssistSubjectControlAdapter.addKeyListener(fKeyListener);

			BusyIndicator.showWhile(control.getDisplay(), new Runnable() {
				public void run() {

					fInvocationOffset= fContentAssistSubjectControlAdapter.getSelectedRange().x;
					fFilterOffset= fInvocationOffset;
					fLastCompletionOffset= fFilterOffset;
					fFilteredProposals= computeProposals(fInvocationOffset);

					int count= (fFilteredProposals == null ? 0 : fFilteredProposals.length);
					if (count == 0 && hideWhenNoProposals(false))
						return;

					if (count == 1 && canAutoInsert(fFilteredProposals[0])) {
						insertProposal(fFilteredProposals[0], (char) 0, 0, fInvocationOffset);
						hide();
					} else {
						ensureDocumentListenerInstalled();
						if (count > 0 && completeCommonPrefix())
							hide();
						else {
							fComputedProposals= fFilteredProposals;
							createProposalSelector();
							setProposals(fComputedProposals, false);
							displayProposals();
						}
					}
				}
			});
		}
		return getErrorMessage();
	}

	/**
	 * Acts upon <code>fFilteredProposals</code>: if there is just one valid
	 * proposal, it is inserted, otherwise, the common prefix of all proposals
	 * is inserted into the document. If there is no common prefix, nothing
	 * happens and <code>false</code> is returned.
	 *
	 * @return <code>true</code> if a single proposal was inserted and the
	 *         selector can be closed, <code>false</code> otherwise
	 * @since 3.0
	 */
	private boolean completeCommonPrefix() {

		// 0: insert single proposals
		if (fFilteredProposals.length == 1) {
			if (canAutoInsert(fFilteredProposals[0])) {
				insertProposal(fFilteredProposals[0], (char) 0, 0, fFilterOffset);
				hide();
				return true;
			}
			return false;
		}

		// 1: extract pre- and postfix from all remaining proposals
		IDocument document= fContentAssistSubjectControlAdapter.getDocument();

		// contains the common postfix in the case that there are any proposals matching our LHS
		StringBuffer rightCasePostfix= null;
		List rightCase= new ArrayList();

		boolean isWrongCaseMatch= false;

		// the prefix of all case insensitive matches. This differs from the document
		// contents and will be replaced.
		CharSequence wrongCasePrefix= null;
		int wrongCasePrefixStart= 0;
		// contains the common postfix of all case-insensitive matches
		StringBuffer wrongCasePostfix= null;
		List wrongCase= new ArrayList();

		for (int i= 0; i < fFilteredProposals.length; i++) {
			ICompletionProposal proposal= fFilteredProposals[i];

			if (!(proposal instanceof ICompletionProposalExtension3))
				return false;

			int start= ((ICompletionProposalExtension3)proposal).getPrefixCompletionStart(fContentAssistSubjectControlAdapter.getDocument(), fFilterOffset);
			CharSequence insertion= ((ICompletionProposalExtension3)proposal).getPrefixCompletionText(fContentAssistSubjectControlAdapter.getDocument(), fFilterOffset);
			if (insertion == null)
				insertion= TextProcessor.deprocess(proposal.getDisplayString());
			try {
				int prefixLength= fFilterOffset - start;
				int relativeCompletionOffset= Math.min(insertion.length(), prefixLength);
				String prefix= document.get(start, prefixLength);
				if (!isWrongCaseMatch && insertion.toString().startsWith(prefix)) {
					isWrongCaseMatch= false;
					rightCase.add(proposal);
					CharSequence newPostfix= insertion.subSequence(relativeCompletionOffset, insertion.length());
					if (rightCasePostfix == null)
						rightCasePostfix= new StringBuffer(newPostfix.toString());
					else
						truncatePostfix(rightCasePostfix, newPostfix);
				} else if (i == 0 || isWrongCaseMatch) {
					CharSequence newPrefix= insertion.subSequence(0, relativeCompletionOffset);
					if (isPrefixCompatible(wrongCasePrefix, wrongCasePrefixStart, newPrefix, start, document)) {
						isWrongCaseMatch= true;
						wrongCasePrefix= newPrefix;
						wrongCasePrefixStart= start;
						CharSequence newPostfix= insertion.subSequence(relativeCompletionOffset, insertion.length());
						if (wrongCasePostfix == null)
							wrongCasePostfix= new StringBuffer(newPostfix.toString());
						else
							truncatePostfix(wrongCasePostfix, newPostfix);
						wrongCase.add(proposal);
					} else {
						return false;
					}
				} else
					return false;
			} catch (BadLocationException e2) {
				// bail out silently
				return false;
			}

			if (rightCasePostfix != null && rightCasePostfix.length() == 0 && rightCase.size() > 1)
				return false;
		}

		// 2: replace single proposals

		if (rightCase.size() == 1) {
			ICompletionProposal proposal= (ICompletionProposal) rightCase.get(0);
			if (canAutoInsert(proposal) && rightCasePostfix.length() > 0) {
				insertProposal(proposal, (char) 0, 0, fInvocationOffset);
				hide();
				return true;
			}
			return false;
		} else if (isWrongCaseMatch && wrongCase.size() == 1) {
			ICompletionProposal proposal= (ICompletionProposal) wrongCase.get(0);
			if (canAutoInsert(proposal)) {
				insertProposal(proposal, (char) 0, 0, fInvocationOffset);
				hide();
			return true;
			}
			return false;
		}

		// 3: replace post- / prefixes

		CharSequence prefix;
		if (isWrongCaseMatch)
			prefix= wrongCasePrefix;
		else
			prefix= "";  //$NON-NLS-1$

		CharSequence postfix;
		if (isWrongCaseMatch)
			postfix= wrongCasePostfix;
		else
			postfix= rightCasePostfix;

		if (prefix == null || postfix == null)
			return false;

		try {
			// 4: check if parts of the postfix are already in the document
			int to= Math.min(document.getLength(), fFilterOffset + postfix.length());
			StringBuffer inDocument= new StringBuffer(document.get(fFilterOffset, to - fFilterOffset));
			truncatePostfix(inDocument, postfix);

			// 5: replace and reveal
			document.replace(fFilterOffset - prefix.length(), prefix.length() + inDocument.length(), prefix.toString() + postfix.toString());

			fContentAssistSubjectControlAdapter.setSelectedRange(fFilterOffset + postfix.length(), 0);
			fContentAssistSubjectControlAdapter.revealRange(fFilterOffset + postfix.length(), 0);
			fFilterOffset+= postfix.length();
			fLastCompletionOffset= fFilterOffset;

			return false;
		} catch (BadLocationException e) {
			// ignore and return false
			return false;
		}
	}

	/*
	 * @since 3.1
	 */
	private boolean isPrefixCompatible(CharSequence oneSequence, int oneOffset, CharSequence twoSequence, int twoOffset, IDocument document) throws BadLocationException {
		if (oneSequence == null || twoSequence == null)
			return true;

		int min= Math.min(oneOffset, twoOffset);
		int oneEnd= oneOffset + oneSequence.length();
		int twoEnd= twoOffset + twoSequence.length();

		String one= document.get(oneOffset, min - oneOffset) + oneSequence + document.get(oneEnd, Math.min(fFilterOffset, fFilterOffset - oneEnd));
		String two= document.get(twoOffset, min - twoOffset) + twoSequence + document.get(twoEnd, Math.min(fFilterOffset, fFilterOffset - twoEnd));

		return one.equals(two);
	}

	/**
	 * Truncates <code>buffer</code> to the common prefix of <code>buffer</code>
	 * and <code>sequence</code>.
	 *
	 * @param buffer the common postfix to truncate
	 * @param sequence the characters to truncate with
	 */
	private void truncatePostfix(StringBuffer buffer, CharSequence sequence) {
		// find common prefix
		int min= Math.min(buffer.length(), sequence.length());
		for (int c= 0; c < min; c++) {
			if (sequence.charAt(c) != buffer.charAt(c)) {
				buffer.delete(c, buffer.length());
				return;
			}
		}

		// all equal up to minimum
		buffer.delete(min, buffer.length());
	}

	/**
	 * Sets the message for the repetition affordance text at the bottom of the proposal. Only has
	 * an effect if {@link ContentAssistant#isRepeatedInvocationMode()} returns <code>true</code>.
	 *
	 * @param message the new caption
	 * @since 3.2
	 */
	void setMessage(String message) {
		Assert.isNotNull(message);
		if (isActive() && fMessageText != null)
			fMessageText.setText(message + " "); //$NON-NLS-1$
	}

	/**
	 * Sets the text to be displayed if no proposals are available. Only has an effect if
	 * {@link ContentAssistant#isShowEmptyList()} returns <code>true</code>.
	 *
	 * @param message the empty message
	 * @since 3.2
	 */
	void setEmptyMessage(String message) {
		Assert.isNotNull(message);
		fEmptyMessage= message;
	}

	/**
	 * Enables or disables showing of the caption line. See also {@link #setMessage(String)}.
	 *
	 * @param show <code>true</code> if the status line is visible
	 * @since 3.2
	 */
	public void setStatusLineVisible(boolean show) {
		if (!isActive() || show == (fMessageText != null))
			return; // nothing to do

		if (show) {
			createMessageText();
		} else {
			fMessageText.dispose();
			fMessageText= null;
		}
		fProposalShell.layout();
	}

	/**
	 * Informs the popup that it is being placed above the caret line instead of below.
	 *
	 * @param above <code>true</code> if the location of the popup is above the caret line, <code>false</code> if it is below
	 * @since 3.3
	 */
	void switchedPositionToAbove(boolean above) {
		if (fAdditionalInfoController != null) {
			fAdditionalInfoController.setFallbackAnchors(new Anchor[] {
					AbstractInformationControlManager.ANCHOR_RIGHT,
					AbstractInformationControlManager.ANCHOR_LEFT,
					above ? AbstractInformationControlManager.ANCHOR_TOP : AbstractInformationControlManager.ANCHOR_BOTTOM
			});
		}
	}

	/**
	 * Returns a new proposal selection handler.
	 *
	 * @param operationCode the operation code
	 * @return the handler
	 * @since 3.4
	 */
	IHandler createProposalSelectionHandler(int operationCode) {
		return new ProposalSelectionHandler(operationCode);
	}

}
