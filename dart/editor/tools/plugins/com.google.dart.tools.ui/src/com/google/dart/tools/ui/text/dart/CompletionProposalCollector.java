/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.text.dart;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.completion.CompletionContext;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.internal.completion.InternalCompletionProposal;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.DartMethodCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.FieldProposalInfo;
import com.google.dart.tools.ui.internal.text.dart.FilledArgumentNamesMethodProposal;
import com.google.dart.tools.ui.internal.text.dart.GetterSetterCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.LazyDartCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.LazyDartTypeCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.MethodDeclarationCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.MethodProposalInfo;
import com.google.dart.tools.ui.internal.text.dart.OverrideCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.ParameterGuessingProposal;
import com.google.dart.tools.ui.internal.text.dart.ProposalContextInformation;
import com.google.dart.tools.ui.internal.util.TypeFilter;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JavaScript UI implementation of <code>CompletionRequestor</code>. Produces
 * {@link IDartCompletionProposal}s from the proposal descriptors received via the
 * <code>CompletionRequestor</code> interface.
 * <p>
 * The lifecycle of a <code>CompletionProposalCollector</code> instance is very simple:
 * 
 * <pre>
 * CompilationUnit unit= ...
 * int offset= ...
 * 
 * CompletionProposalCollector collector= new CompletionProposalCollector(unit);
 * unit.codeComplete(offset, collector);
 * IDartCompletionProposal[] proposals= collector.getJavaCompletionProposals();
 * String errorMessage= collector.getErrorMessage();
 * 
 * &#x2f;&#x2f; display &#x2f; process proposals
 * </pre>
 * Note that after a code completion operation, the collector will store any received proposals,
 * which may require a considerable amount of memory, so the collector should not be kept as a
 * reference after a completion operation.
 * </p>
 * <p>
 * Clients may instantiate or subclass.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class CompletionProposalCollector extends CompletionRequestor {

  /** Tells whether this class is in debug mode. */
  private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("com.google.dart.tools.ui/debug/ResultCollector")); //$NON-NLS-1$//$NON-NLS-2$

  /** Triggers for method proposals without parameters. Do not modify. */
  protected final static char[] METHOD_TRIGGERS = new char[] {';', ',', '.', '\t', '[', ' '};
  /** Triggers for method proposals. Do not modify. */
  protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS = new char[] {'(', '-', ' '};
  /** Triggers for types. Do not modify. */
  protected final static char[] TYPE_TRIGGERS = new char[] {'.', '\t', '[', '(', ' '};
  /** Triggers for variables. Do not modify. */
  protected final static char[] VAR_TRIGGER = new char[] {'\t', ' ', '=', ';', '.'};

  private final CompletionProposalLabelProvider fLabelProvider = new CompletionProposalLabelProvider();
  private final ImageDescriptorRegistry fRegistry = DartToolsPlugin.getImageDescriptorRegistry();

  private final List fJavaProposals = new ArrayList();
  private final List fKeywords = new ArrayList();
  private final Set fSuggestedMethodNames = new HashSet();

  private final CompilationUnit fCompilationUnit;
  private final DartProject fJavaProject;
  private int fUserReplacementLength;

  private CompletionContext fContext;
  private Problem fLastProblem;

  /* performance instrumentation */
  private long fStartTime;
  private long fUITime;

  /**
   * The UI invocation context or <code>null</code>.
   */
  private DartContentAssistInvocationContext fInvocationContext;

  /**
   * Creates a new instance ready to collect proposals. If the passed <code>CompilationUnit</code>
   * is not contained in an {@link DartProject}, no javadoc will be available as
   * {@link org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   * additional info} on the created proposals.
   * 
   * @param cu the compilation unit that the result collector will operate on
   */
  public CompletionProposalCollector(CompilationUnit cu) {
    this(cu == null ? null : cu.getDartProject(), cu);
  }

  /**
   * Creates a new instance ready to collect proposals. Note that proposals for anonymous types and
   * method declarations are not created when using this constructor, as those need to know the
   * compilation unit that they are created on. Use
   * {@link CompletionProposalCollector#CompletionProposalCollector(CompilationUnit)} instead to get
   * all proposals.
   * <p>
   * If the passed JavaScript project is <code>null</code>, no javadoc will be available as
   * {@link org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   * additional info} on the created (e.g. method and type) proposals.
   * </p>
   * 
   * @param project the project that the result collector will operate on, or <code>null</code>
   */
  public CompletionProposalCollector(DartProject project) {
    this(project, null);
  }

  private CompletionProposalCollector(DartProject project, CompilationUnit cu) {
    fJavaProject = project;
    fCompilationUnit = cu;

    fUserReplacementLength = -1;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Subclasses may replace, but usually should not need to. Consider replacing
   * {@linkplain #createJavaCompletionProposal(CompletionProposal) createJavaCompletionProposal}
   * instead.
   * </p>
   */
  @Override
  public void accept(CompletionProposal proposal) {
    long start = DEBUG ? System.currentTimeMillis() : 0;
    try {
      if (isFiltered(proposal)) {
        return;
      }

      if (proposal.getKind() == CompletionProposal.POTENTIAL_METHOD_DECLARATION) {
        acceptPotentialMethodDeclaration(proposal);
      } else {
        IDartCompletionProposal javaProposal = createJavaCompletionProposal(proposal);
        if (javaProposal != null) {
          fJavaProposals.add(javaProposal);
          if (proposal.getKind() == CompletionProposal.KEYWORD) {
            fKeywords.add(javaProposal);
          }
        }
      }
    } catch (IllegalArgumentException e) {
      // all signature processing method may throw IAEs
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=84657
      // don't abort, but log and show all the valid proposals
      DartToolsPlugin.log(new Status(IStatus.ERROR, DartToolsPlugin.getPluginId(), IStatus.OK,
          "Exception when processing proposal for: " + String.valueOf(proposal.getCompletion()), e)); //$NON-NLS-1$
    }

    if (DEBUG) {
      fUITime += System.currentTimeMillis() - start;
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Subclasses may extend, but usually should not need to.
   * </p>
   * 
   * @see #getContext()
   */
  @Override
  public void acceptContext(CompletionContext context) {
    fContext = context;
    fLabelProvider.setContext(context);
  }

  /**
   * {@inheritDoc} Subclasses may extend, but must call the super implementation.
   */
  @Override
  public void beginReporting() {
    if (DEBUG) {
      fStartTime = System.currentTimeMillis();
      fUITime = 0;
    }

    fLastProblem = null;
    fJavaProposals.clear();
    fKeywords.clear();
    fSuggestedMethodNames.clear();
  }

  /**
   * {@inheritDoc} Subclasses may extend, but must call the super implementation.
   */
  @Override
  public void completionFailure(Problem problem) {
    fLastProblem = problem;
  }

  /**
   * {@inheritDoc} Subclasses may extend, but must call the super implementation.
   */
  @Override
  public void endReporting() {
    if (DEBUG) {
      long total = System.currentTimeMillis() - fStartTime;
      System.err.println("Core Collector (core):\t" + (total - fUITime)); //$NON-NLS-1$
      System.err.println("Core Collector (ui):\t" + fUITime); //$NON-NLS-1$
    }
  }

  /**
   * Returns an error message about any error that may have occurred during code completion, or the
   * empty string if none.
   * <p>
   * Subclasses may replace or extend.
   * </p>
   * 
   * @return an error message or the empty string
   */
  public String getErrorMessage() {
    if (fLastProblem != null) {
      return fLastProblem.getMessage();
    }
    return ""; //$NON-NLS-1$
  }

  /**
   * Returns the unsorted list of received proposals.
   * 
   * @return the unsorted list of received proposals
   */
  public final IDartCompletionProposal[] getJavaCompletionProposals() {
    return (IDartCompletionProposal[]) fJavaProposals.toArray(new IDartCompletionProposal[fJavaProposals.size()]);
  }

  /**
   * Returns the unsorted list of received keyword proposals.
   * 
   * @return the unsorted list of received keyword proposals
   */
  public final DartCompletionProposal[] getKeywordCompletionProposals() {
    return (DartCompletionProposal[]) fKeywords.toArray(new DartCompletionProposal[fKeywords.size()]);
  }

  /**
   * Sets the invocation context.
   * <p>
   * Subclasses may extend.
   * </p>
   * 
   * @param context the invocation context
   * @see #getInvocationContext()
   */
  public void setInvocationContext(DartContentAssistInvocationContext context) {
    Assert.isNotNull(context);
    fInvocationContext = context;
    context.setCollector(this);
  }

  /**
   * If the replacement length is set, it overrides the length returned from the content assist
   * infrastructure. Use this setting if code assist is called with a none empty selection.
   * 
   * @param length the new replacement length, relative to the code assist offset. Must be equal to
   *          or greater than zero.
   */
  public final void setReplacementLength(int length) {
    Assert.isLegal(length >= 0);
    fUserReplacementLength = length;
  }

  /**
   * Computes the relevance for a given <code>CompletionProposal</code>.
   * <p>
   * Subclasses may replace, but usually should not need to.
   * </p>
   * 
   * @param proposal the proposal to compute the relevance for
   * @return the relevance for <code>proposal</code>
   */
  protected int computeRelevance(CompletionProposal proposal) {
    final int baseRelevance = proposal.getRelevance() * 16;
    switch (proposal.getKind()) {
      case CompletionProposal.PACKAGE_REF:
        return baseRelevance + 0;
      case CompletionProposal.LABEL_REF:
        return baseRelevance + 1;
      case CompletionProposal.KEYWORD:
        return baseRelevance + 2;
      case CompletionProposal.TYPE_REF:
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
        return baseRelevance + 3;
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.METHOD_DECLARATION:
        return baseRelevance + 4;
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        return baseRelevance + 4 /* + 99 */;
      case CompletionProposal.FIELD_REF:
        return baseRelevance + 5;
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return baseRelevance + 6;
      default:
        return baseRelevance;
    }
  }

  /**
   * Creates a new JavaScript completion proposal from a core proposal. This may involve computing
   * the display label and setting up some context.
   * <p>
   * This method is called for every proposal that will be displayed to the user, which may be
   * hundreds. Implementations should therefore defer as much work as possible: Labels should be
   * computed lazily to leverage virtual table usage, and any information only needed when
   * <em>applying</em> a proposal should not be computed yet.
   * </p>
   * <p>
   * Implementations may return <code>null</code> if a proposal should not be included in the list
   * presented to the user.
   * </p>
   * <p>
   * Subclasses may extend or replace this method.
   * </p>
   * 
   * @param proposal the core completion proposal to create a UI proposal for
   * @return the created JavaScript completion proposal, or <code>null</code> if no proposal should
   *         be displayed
   */
  protected IDartCompletionProposal createJavaCompletionProposal(CompletionProposal proposal) {
    switch (proposal.getKind()) {
      case CompletionProposal.KEYWORD:
        return createKeywordProposal(proposal);
      case CompletionProposal.PACKAGE_REF:
        return createPackageProposal(proposal);
      case CompletionProposal.TYPE_REF:
        return createTypeProposal(proposal);
      case CompletionProposal.JAVADOC_TYPE_REF:
        return createJavadocLinkTypeProposal(proposal);
      case CompletionProposal.FIELD_REF:
      case CompletionProposal.JAVADOC_FIELD_REF:
        return createFieldProposal(proposal);
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.JAVADOC_METHOD_REF:
        return createMethodReferenceProposal(proposal);
      case CompletionProposal.METHOD_DECLARATION:
        return createMethodDeclarationProposal(proposal);
//      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
//        return createAnonymousTypeProposal(proposal);
      case CompletionProposal.LABEL_REF:
        return createLabelProposal(proposal);
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return createLocalVariableProposal(proposal);
      case CompletionProposal.JAVADOC_BLOCK_TAG:
      case CompletionProposal.JAVADOC_PARAM_REF:
        return createJavadocSimpleProposal(proposal);
      case CompletionProposal.JAVADOC_INLINE_TAG:
        return createJavadocInlineTagProposal(proposal);
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
      default:
        return null;
    }
  }

  /**
   * Creates the context information for a given method reference proposal. The passed proposal must
   * be of kind {@link CompletionProposal#METHOD_REF}.
   * 
   * @param methodProposal the method proposal for which to create context information
   * @return the context information for <code>methodProposal</code>
   */
  protected final IContextInformation createMethodContextInformation(
      CompletionProposal methodProposal) {
    Assert.isTrue(methodProposal.getKind() == CompletionProposal.METHOD_REF);
    return new ProposalContextInformation(methodProposal);
  }

  /**
   * Returns the compilation unit that the receiver operates on, or <code>null</code> if the
   * <code>DartProject</code> constructor was used to create the receiver.
   * 
   * @return the compilation unit that the receiver operates on, or <code>null</code>
   */
  protected final CompilationUnit getCompilationUnit() {
    return fCompilationUnit;
  }

  /**
   * Returns the <code>CompletionContext</code> for this completion operation.
   * 
   * @return the <code>CompletionContext</code> for this completion operation
   * @see CompletionRequestor#acceptContext(CompletionContext)
   */
  protected final CompletionContext getContext() {
    return fContext;
  }

  /**
   * Returns the type signature of the declaring type of a <code>CompletionProposal</code>, or
   * <code>null</code> for proposals that do not have a declaring type. The return value is
   * <em>not</em> <code>null</code> for proposals of the following kinds:
   * <ul>
   * <li>FUNCTION_DECLARATION</li>
   * <li>METHOD_NAME_REFERENCE</li>
   * <li>FUNCTION_REF</li>
   * <li>ANNOTATION_ATTRIBUTE_REF</li>
   * <li>POTENTIAL_METHOD_DECLARATION</li>
   * <li>ANONYMOUS_CLASS_DECLARATION</li>
   * <li>FIELD_REF</li>
   * <li>PACKAGE_REF (returns the package, but no type)</li>
   * <li>TYPE_REF</li>
   * </ul>
   * 
   * @param proposal the completion proposal to get the declaring type for
   * @return the type signature of the declaring type, or <code>null</code> if there is none
   * @see Signature#toCharArray(char[])
   */
  protected String getDeclaringType(CompletionProposal proposal) {
    switch (proposal.getKind()) {
      case CompletionProposal.METHOD_DECLARATION:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.JAVADOC_METHOD_REF:
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
      case CompletionProposal.FIELD_REF:
      case CompletionProposal.JAVADOC_FIELD_REF:
        String declaration = new String(proposal.getDeclarationSignature());
        // special methods may not have a declaring type: methods defined on
        // arrays etc.
        // Currently known: class literals don't have a declaring type - use
        // Object
        if (declaration == null) {
          return "java.lang.Object"; //$NON-NLS-1$
        }
        return declaration;
      case CompletionProposal.PACKAGE_REF:
        return new String(proposal.getDeclarationSignature());
      case CompletionProposal.JAVADOC_TYPE_REF:
      case CompletionProposal.TYPE_REF:
        return new String(proposal.getSignature());
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
      case CompletionProposal.KEYWORD:
      case CompletionProposal.LABEL_REF:
      case CompletionProposal.JAVADOC_BLOCK_TAG:
      case CompletionProposal.JAVADOC_INLINE_TAG:
      case CompletionProposal.JAVADOC_PARAM_REF:
        return null;
      default:
        Assert.isTrue(false);
        return null;
    }
  }

  /**
   * Returns a cached image for the given descriptor.
   * 
   * @param descriptor the image descriptor to get an image for, may be <code>null</code>
   * @return the image corresponding to <code>descriptor</code>
   */
  protected final Image getImage(ImageDescriptor descriptor) {
    return (descriptor == null) ? null : fRegistry.get(descriptor);
  }

  /**
   * Returns the invocation context. If none has been set via
   * {@link #setInvocationContext(DartContentAssistInvocationContext)}, a new one is created.
   * 
   * @return invocationContext the invocation context
   */
  protected final DartContentAssistInvocationContext getInvocationContext() {
    if (fInvocationContext == null) {
      setInvocationContext(new DartContentAssistInvocationContext(getCompilationUnit()));
    }
    return fInvocationContext;
  }

  /**
   * Returns the proposal label provider used by the receiver.
   * 
   * @return the proposal label provider used by the receiver
   */
  protected final CompletionProposalLabelProvider getLabelProvider() {
    return fLabelProvider;
  }

  /**
   * Returns the replacement length of a given completion proposal. The replacement length is
   * usually the difference between the return values of <code>proposal.getReplaceEnd</code> and
   * <code>proposal.getReplaceStart</code>, but this behavior may be overridden by calling
   * {@link #setReplacementLength(int)}.
   * 
   * @param proposal the completion proposal to get the replacement length for
   * @return the replacement length for <code>proposal</code>
   */
  protected final int getLength(CompletionProposal proposal) {
    int start = proposal.getReplaceStart();
    int end = proposal.getReplaceEnd();
    int length;
    if (fUserReplacementLength == -1) {
      length = end - start;
    } else {
      length = fUserReplacementLength;
      // extend length to begin at start
      int behindCompletion = proposal.getCompletionLocation() + 1;
      if (start < behindCompletion) {
        length += behindCompletion - start;
      }
    }
    return length;
  }

  /**
   * Returns <code>true</code> if <code>proposal</code> is filtered, e.g. should not be proposed to
   * the user, <code>false</code> if it is valid.
   * <p>
   * Subclasses may extends this method. The default implementation filters proposals set to be
   * ignored via {@linkplain CompletionRequestor#setIgnored(int, boolean) setIgnored} and types set
   * to be ignored in the preferences.
   * </p>
   * 
   * @param proposal the proposal to filter
   * @return <code>true</code> to filter <code>proposal</code>, <code>false</code> to let it pass
   */
  protected boolean isFiltered(CompletionProposal proposal) {
    if (isIgnored(proposal.getKind())) {
      return true;
    }
    String declaringType = getDeclaringType(proposal);
    return declaringType != null && TypeFilter.isFiltered(declaringType);
  }

  private void acceptPotentialMethodDeclaration(CompletionProposal proposal) {
    if (fCompilationUnit == null) {
      return;
    }

    String prefix = String.valueOf(proposal.getName());
    int completionStart = proposal.getReplaceStart();
    int completionEnd = proposal.getReplaceEnd();
    int relevance = computeRelevance(proposal);

    try {
      DartElement element = fCompilationUnit.getElementAt(proposal.getCompletionLocation() + 1);
      if (element != null) {
        Type type = element.getAncestor(Type.class);
        if (type != null) {
          GetterSetterCompletionProposal.evaluateProposals(type, prefix, completionStart,
              completionEnd - completionStart, relevance + 1, fSuggestedMethodNames, fJavaProposals);
          MethodDeclarationCompletionProposal.evaluateProposals(type, prefix, completionStart,
              completionEnd - completionStart, relevance, fSuggestedMethodNames, fJavaProposals);
        }
      }
    } catch (CoreException e) {
      DartToolsPlugin.log(e);
    }
  }

  private void adaptLength(LazyDartCompletionProposal proposal, CompletionProposal coreProposal) {
    if (fUserReplacementLength != -1) {
      proposal.setReplacementLength(getLength(coreProposal));
    }
  }

//  private IDartCompletionProposal createAnonymousTypeProposal(
//      CompletionProposal proposal) {
//    if (fCompilationUnit == null || fJavaProject == null)
//      return null;
//
//    String completion = String.valueOf(proposal.getCompletion());
//    int start = proposal.getReplaceStart();
//    int length = getLength(proposal);
//    int relevance = computeRelevance(proposal);
//
//    String label = fLabelProvider.createAnonymousTypeLabel(proposal);
//
//    DartCompletionProposal javaProposal = new AnonymousTypeCompletionProposal(
//        fJavaProject, fCompilationUnit, start, length, completion, label,
//        String.valueOf(proposal.getDeclarationSignature()), relevance);
//    javaProposal.setProposalInfo(new AnonymousTypeProposalInfo(fJavaProject,
//        proposal));
//    return javaProposal;
//  }

  private IDartCompletionProposal createFieldProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    String label = fLabelProvider.createLabel(proposal);
    Image image = getImage(fLabelProvider.createFieldImageDescriptor(proposal));
    int relevance = computeRelevance(proposal);

    DartCompletionProposal javaProposal = new DartCompletionProposal(completion, start, length,
        image, label, relevance, getContext().isInJavadoc(), getInvocationContext());
    if (fJavaProject != null) {
      javaProposal.setProposalInfo(new FieldProposalInfo(fJavaProject, proposal));
    }

    javaProposal.setTriggerCharacters(VAR_TRIGGER);

    return javaProposal;
  }

  private IDartCompletionProposal createJavadocInlineTagProposal(CompletionProposal javadocProposal) {
//    LazyDartCompletionProposal proposal = new JavadocInlineTagCompletionProposal(
//        javadocProposal, getInvocationContext());
//    adaptLength(proposal, javadocProposal);
//    return proposal;
    DartCore.notYetImplemented();
    return null;
  }

  private IDartCompletionProposal createJavadocLinkTypeProposal(CompletionProposal typeProposal) {
//    LazyDartCompletionProposal proposal = new JavadocLinkTypeCompletionProposal(
//        typeProposal, getInvocationContext());
//    adaptLength(proposal, typeProposal);
//    return proposal;
    DartCore.notYetImplemented();
    return null;
  }

  private IDartCompletionProposal createJavadocSimpleProposal(CompletionProposal javadocProposal) {
    // TODO do better with javadoc proposals
    // String completion= String.valueOf(proposal.getCompletion());
    // int start= proposal.getReplaceStart();
    // int length= getLength(proposal);
    // String label= fLabelProvider.createSimpleLabel(proposal);
    // Image image= getImage(fLabelProvider.createImageDescriptor(proposal));
    // int relevance= computeRelevance(proposal);
    //
    // DartCompletionProposal javaProposal= new
    // DartCompletionProposal(completion, start, length, image, label,
    // relevance);
    // if (fJavaProject != null)
    // javaProposal.setProposalInfo(new FieldProposalInfo(fJavaProject,
    // proposal));
    //
    // javaProposal.setTriggerCharacters(VAR_TRIGGER);
    //
    // return javaProposal;
    LazyDartCompletionProposal proposal = new LazyDartCompletionProposal(javadocProposal,
        getInvocationContext());
    // adaptLength(proposal, javadocProposal);
    return proposal;
  }

  private IDartCompletionProposal createKeywordProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    String label = fLabelProvider.createSimpleLabel(proposal);
    int relevance = computeRelevance(proposal);
    return new DartCompletionProposal(completion, start, length, null, label, relevance);
  }

  private IDartCompletionProposal createLabelProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    String label = fLabelProvider.createSimpleLabel(proposal);
    int relevance = computeRelevance(proposal);

    return new DartCompletionProposal(completion, start, length, null, label, relevance);
  }

  private IDartCompletionProposal createLocalVariableProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    Image image = getImage(fLabelProvider.createLocalImageDescriptor(proposal));
    String label = fLabelProvider.createSimpleLabelWithType(proposal);
    int relevance = computeRelevance(proposal);

    final DartCompletionProposal javaProposal = new DartCompletionProposal(completion, start,
        length, image, label, relevance);
    javaProposal.setTriggerCharacters(VAR_TRIGGER);
    return javaProposal;
  }

  private IDartCompletionProposal createMethodDeclarationProposal(CompletionProposal proposal) {
    if (fCompilationUnit == null || fJavaProject == null) {
      return null;
    }

    String name = String.valueOf(proposal.getName());
//    String[] paramTypes = Signature.getParameterTypes(String.valueOf(proposal.getSignature()));
    String[] paramTypes = toStrings(((InternalCompletionProposal) proposal).getParameterTypeNames());
//    for (int index = 0; index < paramTypes.length; index++)
//      paramTypes[index] = Signature.toString(paramTypes[index]);
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);

    String label = fLabelProvider.createOverrideMethodProposalLabel(proposal);

    DartCompletionProposal javaProposal = new OverrideCompletionProposal(fJavaProject,
        fCompilationUnit, name, paramTypes, start, length, label,
        String.valueOf(proposal.getCompletion()));
    javaProposal.setImage(getImage(fLabelProvider.createMethodImageDescriptor(proposal)));
    javaProposal.setProposalInfo(new MethodProposalInfo(fJavaProject, proposal));
    javaProposal.setRelevance(computeRelevance(proposal));

    fSuggestedMethodNames.add(new String(name));
    return javaProposal;
  }

  private IDartCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
    IPreferenceStore preferenceStore = DartToolsPlugin.getDefault().getPreferenceStore();
    LazyDartCompletionProposal proposal = null;

    if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_FILL_ARGUMENT_NAMES)) {
      String completion = String.valueOf(methodProposal.getCompletion());
      // normal behavior if this is not a normal completion or has no parameters
      if ((completion.length() == 0) || ((completion.length() == 1) && completion.charAt(0) == ')')
          || Signature.getParameterCount(methodProposal.getSignature()) == 0
          || getContext().isInJavadoc()) {
        proposal = new DartMethodCompletionProposal(methodProposal, getInvocationContext());
      } else {
        if (preferenceStore.getBoolean(PreferenceConstants.CODEASSIST_GUESS_METHOD_ARGUMENTS)) {
          proposal = new ParameterGuessingProposal(methodProposal, getInvocationContext());
        } else {
          proposal = new FilledArgumentNamesMethodProposal(methodProposal, getInvocationContext());
        }
      }
    }

    if (proposal == null) {
      proposal = new DartMethodCompletionProposal(methodProposal, getInvocationContext());
    }

    adaptLength(proposal, methodProposal);
    return proposal;
  }

  private IDartCompletionProposal createPackageProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    String label = fLabelProvider.createSimpleLabel(proposal);
    Image image = getImage(fLabelProvider.createPackageImageDescriptor(proposal));
    int relevance = computeRelevance(proposal);

    return new DartCompletionProposal(completion, start, length, image, label, relevance);
  }

  private IDartCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
    LazyDartCompletionProposal proposal = new LazyDartTypeCompletionProposal(typeProposal,
        getInvocationContext());
    adaptLength(proposal, typeProposal);
    return proposal;
  }

  private String[] toStrings(char[][] chars) {
    String[] strings = new String[chars.length];
    for (int i = 0; i < chars.length; i++) {
      strings[i] = new String(chars[i]);
    }
    return strings;
  }
}
