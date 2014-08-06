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
package com.google.dart.tools.ui.text.dart;

import com.google.dart.tools.core.completion.CompletionContext;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.completion.CompletionRequestor;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.problem.Problem;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.completion.DartCompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.DartMethodCompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.LazyDartCompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.LazyDartTypeCompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.NamedArgumentCompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.OptionalArgumentCompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.OverrideCompletionProposal;
import com.google.dart.tools.ui.internal.text.dart.ProposalContextInformation;
import com.google.dart.tools.ui.internal.util.TypeFilter;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;
import com.google.dart.tools.ui.text.editor.tmp.Signature;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Dart UI implementation of <code>CompletionRequestor</code>. Produces
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
 * IDartCompletionProposal[] proposals= collector.getDartCompletionProposals();
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
 */
public class CompletionProposalCollector extends CompletionRequestor {

  /** Tells whether this class is in debug mode. */
  private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("com.google.dart.tools.core/debug/ResultCollector")); //$NON-NLS-1$//$NON-NLS-2$

  /** Triggers for method proposals without parameters. Do not modify. */
  protected final static char[] METHOD_TRIGGERS = new char[] {';', ',', '.', '\t', '[', ' '};
  /** Triggers for method proposals. Do not modify. */
  protected final static char[] METHOD_WITH_ARGUMENTS_TRIGGERS = new char[] {'(', '-', ' '};
  /** Triggers for types. Do not modify. */
  protected final static char[] TYPE_TRIGGERS = new char[] {'.', '\t', '[', '(', ' '};
  /** Triggers for variables. Do not modify. */
  protected final static char[] VAR_TRIGGER = new char[] {'\t', ' ', '=', ';', '.'};

  /**
   * Returns an array containing all of the elements in the given collection. This is a compile-time
   * type-safe alternative to {@link Collection#toArray(Object[])}.
   * 
   * @param collection the source collection
   * @param clazz the type of the array elements
   * @param <A> the type of the array elements
   * @return an array of type <code>A</code> containing all of the elements in the given collection
   * @throws NullPointerException if the specified collection or class is null
   */
  public static <A> A[] toArray(Collection<? extends A> collection, Class<A> clazz) {
    Object array = Array.newInstance(clazz, collection.size());
    @SuppressWarnings("unchecked")
    A[] typedArray = collection.toArray((A[]) array);
    return typedArray;
  }

  private final CompletionProposalLabelProvider fLabelProvider = new CompletionProposalLabelProvider();
  private final ImageDescriptorRegistry fRegistry = DartToolsPlugin.getImageDescriptorRegistry();

  private final List<IDartCompletionProposal> fDartProposals = new ArrayList<IDartCompletionProposal>();
  private final List<IDartCompletionProposal> fKeywords = new ArrayList<IDartCompletionProposal>();
  private final Set<String> fSuggestedMethodNames = new HashSet<String>();

  private final CompilationUnit fCompilationUnit;
  private final DartProject fDartProject;
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
   * is not contained in an {@link DartProject}, no Dart doc will be available as
   * {@link org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   * additional info} on the created proposals.
   * 
   * @param cu the compilation unit that the result collector will operate on
   */
  public CompletionProposalCollector(CompilationUnit cu) {
    this(cu.getDartProject(), cu, false);
  }

  /**
   * Creates a new instance ready to collect proposals. If the passed <code>CompilationUnit</code>
   * is not contained in an {@link DartProject}, no Dart doc will be available as
   * {@link org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   * additional info} on the created proposals.
   * 
   * @param cu the compilation unit that the result collector will operate on
   * @param ignoreAll <code>true</code> to ignore all kinds of completion proposals
   */
  public CompletionProposalCollector(CompilationUnit cu, boolean ignoreAll) {
    this(cu == null ? null : cu.getDartProject(), cu, ignoreAll); // TODO Remove getDartProject()
  }

  private CompletionProposalCollector(DartProject project, CompilationUnit cu, boolean ignoreAll) {
    super(ignoreAll);
    fDartProject = project;
    fCompilationUnit = cu;

    fUserReplacementLength = -1;
    if (!ignoreAll) {
      setRequireExtendedContext(true);
    }
  }

  /**
   * {@inheritDoc}
   * <p>
   * Subclasses may replace, but usually should not need to. Consider replacing
   * {@linkplain #createDartCompletionProposal(CompletionProposal) createDartCompletionProposal}
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

      DartContentAssistInvocationContext ctxt = getInvocationContext();
      proposal.applyPartitionOffset(ctxt.getPartitionOffset());
      if (proposal.getKind() == CompletionProposal.POTENTIAL_METHOD_DECLARATION) {
      } else {
        IDartCompletionProposal dartProposal = createDartCompletionProposal(proposal);
        if (dartProposal != null) {
          fDartProposals.add(dartProposal);
          if (proposal.getKind() == CompletionProposal.KEYWORD) {
            fKeywords.add(dartProposal);
          }
        }
      }
    } catch (IllegalArgumentException e) {
      // all signature processing method may throw IAEs
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=84657
      // don't abort, but log and show all the valid proposals
      DartToolsPlugin.log(new Status(
          IStatus.ERROR,
          DartToolsPlugin.getPluginId(),
          IStatus.OK,
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
    fDartProposals.clear();
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
   * Returns the unsorted list of received proposals.
   * 
   * @return the unsorted list of received proposals
   */
  public final IDartCompletionProposal[] getDartCompletionProposals() {
    return toArray(fDartProposals, IDartCompletionProposal.class);
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

  @Override
  public com.google.dart.engine.ast.CompilationUnit getInputUnit() {
    return fInvocationContext.getInputUnit();
  }

  /**
   * Returns the unsorted list of received keyword proposals.
   * 
   * @return the unsorted list of received keyword proposals
   */
  public final IDartCompletionProposal[] getKeywordCompletionProposals() {
    return toArray(fKeywords, IDartCompletionProposal.class);
  }

  @Override
  public void setIgnored(int completionProposalKind, boolean ignore) {
    super.setIgnored(completionProposalKind, ignore);
    if (completionProposalKind == CompletionProposal.METHOD_DECLARATION && !ignore) {
      setRequireExtendedContext(true);
    }
  }

  /**
   * Sets the invocation context.
   * <p>
   * Subclasses may extend.
   * </p>
   * 
   * @param context the invocation context
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
      case CompletionProposal.LIBRARY_PREFIX:
        return baseRelevance + 0;
      case CompletionProposal.LABEL_REF:
        return baseRelevance + 1;
      case CompletionProposal.KEYWORD:
        return baseRelevance + 2;
      case CompletionProposal.TYPE_REF:
//      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
//      case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
        return baseRelevance + 3;
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
      case CompletionProposal.METHOD_NAME_REFERENCE:
      case CompletionProposal.METHOD_DECLARATION:
//      case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
        return baseRelevance + 4;
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
        return baseRelevance + 4 /* + 99 */;
      case CompletionProposal.FIELD_REF:
        return baseRelevance + 5;
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return baseRelevance + 6;
      case CompletionProposal.ARGUMENT_LIST:
        return baseRelevance + 7;
      default:
        return baseRelevance;
    }
  }

  /**
   * Creates a new Dart completion proposal from a core proposal. This may involve computing the
   * display label and setting up some context.
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
   * @return the created Dart completion proposal, or <code>null</code> if no proposal should be
   *         displayed
   */
  protected IDartCompletionProposal createDartCompletionProposal(CompletionProposal proposal) {
    switch (proposal.getKind()) {
      case CompletionProposal.KEYWORD:
        return createKeywordProposal(proposal);
      case CompletionProposal.LIBRARY_PREFIX:
        return createLibraryPrefixProposal(proposal);
      case CompletionProposal.TYPE_REF:
        return createTypeProposal(proposal);
//      case CompletionProposal.JAVADOC_TYPE_REF:
//        return createJavadocLinkTypeProposal(proposal);
      case CompletionProposal.FIELD_REF:
//      case CompletionProposal.JAVADOC_FIELD_REF:
//      case CompletionProposal.JAVADOC_VALUE_REF:
        return createFieldProposal(proposal);
//      case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER:
//        return createFieldWithCastedReceiverProposal(proposal);
      case CompletionProposal.ARGUMENT_LIST:
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
//      case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
      case CompletionProposal.METHOD_NAME_REFERENCE:
//      case CompletionProposal.JAVADOC_METHOD_REF:
        return createMethodReferenceProposal(proposal);
      case CompletionProposal.METHOD_DECLARATION:
        return createMethodDeclarationProposal(proposal);
//      case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
//        return createAnonymousTypeProposal(proposal, getInvocationContext());
//      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
//        return createAnonymousTypeProposal(proposal, null);
      case CompletionProposal.LABEL_REF:
        return createLabelProposal(proposal);
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
        return createLocalVariableProposal(proposal);
      case CompletionProposal.TYPE_IMPORT:
        return createImportProposal(proposal);
      case CompletionProposal.OPTIONAL_ARGUMENT:
        return new OptionalArgumentCompletionProposal(proposal);
      case CompletionProposal.NAMED_ARGUMENT:
        return new NamedArgumentCompletionProposal(proposal);
//      case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
//        return createAnnotationAttributeReferenceProposal(proposal);
//      case CompletionProposal.JAVADOC_BLOCK_TAG:
//      case CompletionProposal.JAVADOC_PARAM_REF:
//        return createJavadocSimpleProposal(proposal);
//      case CompletionProposal.JAVADOC_INLINE_TAG:
//        return createJavadocInlineTagProposal(proposal);
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
   */
  protected final CompletionContext getContext() {
    return fContext;
  }

  /**
   * Returns the type signature of the declaring type of a <code>CompletionProposal</code>, or
   * <code>null</code> for proposals that do not have a declaring type. The return value is
   * <em>not</em> <code>null</code> for proposals of the following kinds:
   * <ul>
   * <li>METHOD_DECLARATION</li>
   * <li>METHOD_NAME_REFERENCE</li>
   * <li>METHOD_REF</li>
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
   */
  protected final char[] getDeclaringType(CompletionProposal proposal) {
    switch (proposal.getKind()) {
      case CompletionProposal.METHOD_DECLARATION:
      case CompletionProposal.METHOD_NAME_REFERENCE:
//      case CompletionProposal.JAVADOC_METHOD_REF:
      case CompletionProposal.METHOD_REF:
      case CompletionProposal.ARGUMENT_LIST:
      case CompletionProposal.CONSTRUCTOR_INVOCATION:
//      case CompletionProposal.ANONYMOUS_CLASS_CONSTRUCTOR_INVOCATION:
//      case CompletionProposal.METHOD_REF_WITH_CASTED_RECEIVER:
//      case CompletionProposal.ANNOTATION_ATTRIBUTE_REF:
      case CompletionProposal.POTENTIAL_METHOD_DECLARATION:
//      case CompletionProposal.ANONYMOUS_CLASS_DECLARATION:
      case CompletionProposal.FIELD_REF:
//      case CompletionProposal.FIELD_REF_WITH_CASTED_RECEIVER:
//      case CompletionProposal.JAVADOC_FIELD_REF:
//      case CompletionProposal.JAVADOC_VALUE_REF:
        char[] declaration = proposal.getDeclarationSignature();
        // special methods may not have a declaring type: methods defined on arrays etc.
        // Currently known: class literals don't have a declaring type - use Object
//        if (declaration == null) {
//          return "java.lang.Object".toCharArray(); //$NON-NLS-1$
//        }
        return Signature.toCharArray(declaration);
      case CompletionProposal.LIBRARY_PREFIX:
        return proposal.getDeclarationSignature();
//      case CompletionProposal.JAVADOC_TYPE_REF:
      case CompletionProposal.TYPE_REF:
        return Signature.toCharArray(proposal.getSignature());
      case CompletionProposal.LOCAL_VARIABLE_REF:
      case CompletionProposal.VARIABLE_DECLARATION:
      case CompletionProposal.KEYWORD:
      case CompletionProposal.LABEL_REF:
      case CompletionProposal.TYPE_IMPORT:
      case CompletionProposal.OPTIONAL_ARGUMENT:
      case CompletionProposal.NAMED_ARGUMENT:
//      case CompletionProposal.JAVADOC_BLOCK_TAG:
//      case CompletionProposal.JAVADOC_INLINE_TAG:
//      case CompletionProposal.JAVADOC_PARAM_REF:
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
    char[] declaringType = getDeclaringType(proposal);
    return declaringType != null && TypeFilter.isFiltered(declaringType);
  }

  private void adaptLength(LazyDartCompletionProposal proposal, CompletionProposal coreProposal) {
    if (fUserReplacementLength != -1) {
      proposal.setReplacementLength(getLength(coreProposal));
    }
  }

//  private IDartCompletionProposal createAnnotationAttributeReferenceProposal(
//      CompletionProposal proposal) {
//    StyledString displayString= fLabelProvider.createLabelWithTypeAndDeclaration(proposal);
//    ImageDescriptor descriptor= fLabelProvider.createMethodImageDescriptor(proposal);
//    String completion= String.valueOf(proposal.getCompletion());
//    DartCompletionProposal javaProposal= new DartCompletionProposal(completion, proposal.getReplaceStart(), getLength(proposal), getImage(descriptor), displayString, computeRelevance(proposal));
//    if (fJavaProject != null)
//      javaProposal.setProposalInfo(new AnnotationAtttributeProposalInfo(fJavaProject, proposal));
//    return javaProposal;
//  }

//  private IDartCompletionProposal createAnonymousTypeProposal(CompletionProposal proposal,
//      DartContentAssistInvocationContext invocationContext) {
//    if (fCompilationUnit == null || fJavaProject == null)
//      return null;
//
//    char[] declarationKey= proposal.getDeclarationKey();
//    if (declarationKey == null)
//      return null;
//
//    try {
//      DartElement element= fJavaProject.findElement(new String(declarationKey), null);
//      if (!(element instanceof Type))
//        return null;
//
//      Type type= (Type) element;
//
//      String completion= String.valueOf(proposal.getCompletion());
//      int start= proposal.getReplaceStart();
//      int length= getLength(proposal);
//      int relevance= computeRelevance(proposal);
//
//      StyledString label= fLabelProvider.createAnonymousTypeLabel(proposal);
//
//      DartCompletionProposal javaProposal= new AnonymousTypeCompletionProposal(fJavaProject, fCompilationUnit, invocationContext, start, length, completion, label, String.valueOf(proposal
//          .getDeclarationSignature()), type, relevance);
//      javaProposal.setProposalInfo(new AnonymousTypeProposalInfo(fJavaProject, proposal));
//      return javaProposal;
//    } catch (DartModelException e) {
//      return null;
//    }
//  }

  private IDartCompletionProposal createFieldProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    StyledString label = fLabelProvider.createStyledLabel(proposal);
    Image image = getImage(fLabelProvider.createFieldImageDescriptor(proposal));
    int relevance = computeRelevance(proposal);

    @SuppressWarnings("deprecation")
    DartCompletionProposal dartProposal = new DartCompletionProposal(
        completion,
        start,
        length,
        getLengthIdentifier(proposal),
        image,
        label,
        relevance,
        getContext().isInJavadoc(),
        proposal.getElementDocSummary(),
        proposal.getElementDocDetails(),
        getInvocationContext());
    // TODO(scheglov) implement documentation comment
//    if (fDartProject != null) {
//      dartProposal.setProposalInfo(new FieldProposalInfo(fDartProject, proposal));
//    }

    dartProposal.setTriggerCharacters(VAR_TRIGGER);

    return dartProposal;
  }

  private IDartCompletionProposal createImportProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    StyledString label = new StyledString(fLabelProvider.createSimpleLabel(proposal));//TODO(messick)
    int relevance = computeRelevance(proposal);
    ImageDescriptor imageDesc = fLabelProvider.createImageDescriptor(proposal);
    Image image = DartToolsPlugin.getImageDescriptorRegistry().get(imageDesc);
    return new DartCompletionProposal(
        completion,
        start,
        length,
        getLengthIdentifier(proposal),
        image,
        label,
        relevance,
        proposal.getElementDocSummary(),
        proposal.getElementDocDetails());
  }

//  /**
//   * Creates the Java completion proposal for the JDT Core
//   * {@link CompletionProposal#FIELD_REF_WITH_CASTED_RECEIVER} proposal.
//   * 
//   * @param proposal the JDT Core proposal
//   * @return the Java completion proposal
//   */
//  private IDartCompletionProposal createFieldWithCastedReceiverProposal(CompletionProposal proposal) {
//    String completion= String.valueOf(proposal.getCompletion());
//    completion= CodeFormatterUtil.format(CodeFormatter.K_EXPRESSION, completion, 0, "\n", fJavaProject); //$NON-NLS-1$
//    int start= proposal.getReplaceStart();
//    int length= getLength(proposal);
//    StyledString label= fLabelProvider.createStyledLabel(proposal);
//    Image image= getImage(fLabelProvider.createFieldImageDescriptor(proposal));
//    int relevance= computeRelevance(proposal);
//
//    DartCompletionProposal javaProposal= new DartFieldWithCastedReceiverCompletionProposal(completion, start, length, image, label, relevance, getContext().isInJavadoc(), getInvocationContext(), proposal);
//    if (fJavaProject != null)
//      javaProposal.setProposalInfo(new FieldProposalInfo(fJavaProject, proposal));
//
//    javaProposal.setTriggerCharacters(VAR_TRIGGER);
//
//    return javaProposal;
//  }

//  private IDartCompletionProposal createJavadocInlineTagProposal(CompletionProposal javadocProposal) {
//    LazyDartCompletionProposal proposal= new JavadocInlineTagCompletionProposal(javadocProposal, getInvocationContext());
//    adaptLength(proposal, javadocProposal);
//    return proposal;
//  }

//  private IDartCompletionProposal createJavadocLinkTypeProposal(CompletionProposal typeProposal) {
//    LazyDartCompletionProposal proposal= new JavadocLinkTypeCompletionProposal(typeProposal, getInvocationContext());
//    adaptLength(proposal, typeProposal);
//    return proposal;
//  }

//  private IDartCompletionProposal createJavadocSimpleProposal(CompletionProposal javadocProposal) {
  // TODO do better with javadoc proposals
//    String completion= String.valueOf(proposal.getCompletion());
//    int start= proposal.getReplaceStart();
//    int length= getLength(proposal);
//    String label= fLabelProvider.createSimpleLabel(proposal);
//    Image image= getImage(fLabelProvider.createImageDescriptor(proposal));
//    int relevance= computeRelevance(proposal);
//
//    DartCompletionProposal javaProposal= new DartCompletionProposal(completion, start, length, image, label, relevance);
//    if (fJavaProject != null)
//      javaProposal.setProposalInfo(new FieldProposalInfo(fJavaProject, proposal));
//
//    javaProposal.setTriggerCharacters(VAR_TRIGGER);
//
//    return javaProposal;
//    LazyDartCompletionProposal proposal = new LazyDartCompletionProposal(javadocProposal,
//        getInvocationContext());
//    adaptLength(proposal, javadocProposal);
//    return proposal;
//  }

  private IDartCompletionProposal createKeywordProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    StyledString label = new StyledString(fLabelProvider.createSimpleLabel(proposal));//TODO(messick)
    int relevance = computeRelevance(proposal);
    return new DartCompletionProposal(
        completion,
        start,
        length,
        getLengthIdentifier(proposal),
        null,
        label,
        relevance,
        proposal.getElementDocSummary(),
        proposal.getElementDocDetails());
  }

  private IDartCompletionProposal createLabelProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    StyledString label = new StyledString(fLabelProvider.createSimpleLabel(proposal));//TODO(messick)
    int relevance = computeRelevance(proposal);

    return new DartCompletionProposal(
        completion,
        start,
        length,
        getLengthIdentifier(proposal),
        null,
        label,
        relevance,
        proposal.getElementDocSummary(),
        proposal.getElementDocDetails());
  }

  private IDartCompletionProposal createLibraryPrefixProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    StyledString label = new StyledString(fLabelProvider.createSimpleLabel(proposal));//TODO(messick)
    Image image = getImage(fLabelProvider.createLibraryImageDescriptor(proposal));
    int relevance = computeRelevance(proposal);

    return new DartCompletionProposal(
        completion,
        start,
        length,
        getLengthIdentifier(proposal),
        image,
        label,
        relevance,
        proposal.getElementDocSummary(),
        proposal.getElementDocDetails());
  }

  private IDartCompletionProposal createLocalVariableProposal(CompletionProposal proposal) {
    String completion = String.valueOf(proposal.getCompletion());
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);
    Image image = getImage(fLabelProvider.createLocalImageDescriptor(proposal));
    StyledString label = fLabelProvider.createLabelWithType(proposal);
    int relevance = computeRelevance(proposal);
    final DartCompletionProposal dartProposal = new DartCompletionProposal(
        completion,
        start,
        length,
        getLengthIdentifier(proposal),
        image,
        label,
        relevance,
        proposal.getElementDocSummary(),
        proposal.getElementDocDetails());
    dartProposal.setTriggerCharacters(VAR_TRIGGER);
    return dartProposal;
  }

  private IDartCompletionProposal createMethodDeclarationProposal(CompletionProposal proposal) {
    if (fCompilationUnit == null || fDartProject == null) {
      return null;
    }

    String name = String.valueOf(proposal.getName());
    String[] paramTypes = Signature.getParameterTypes(String.valueOf(proposal.getSignature()));
    for (int index = 0; index < paramTypes.length; index++) {
      paramTypes[index] = Signature.toString(paramTypes[index]);
    }
    int start = proposal.getReplaceStart();
    int length = getLength(proposal);

    StyledString label = new StyledString(
        fLabelProvider.createOverrideMethodProposalLabel(proposal));//TODO(messick)

    DartCompletionProposal dartProposal = new OverrideCompletionProposal(
        fDartProject,
        fCompilationUnit,
        name,
        paramTypes,
        start,
        length,
        getLengthIdentifier(proposal),
        label,
        String.valueOf(proposal.getCompletion()));
    dartProposal.setImage(getImage(fLabelProvider.createMethodImageDescriptor(proposal)));
    // TODO(scheglov) implement documentation comment
//    dartProposal.setProposalInfo(new MethodProposalInfo(fDartProject, proposal));
    dartProposal.setRelevance(computeRelevance(proposal));

    fSuggestedMethodNames.add(new String(name));
    return dartProposal;
  }

  private IDartCompletionProposal createMethodReferenceProposal(CompletionProposal methodProposal) {
    LazyDartCompletionProposal proposal = new DartMethodCompletionProposal(
        methodProposal,
        getInvocationContext());
    adaptLength(proposal, methodProposal);
    return proposal;
  }

  private IDartCompletionProposal createTypeProposal(CompletionProposal typeProposal) {
    LazyDartCompletionProposal proposal = new LazyDartTypeCompletionProposal(
        typeProposal,
        getInvocationContext());
    adaptLength(proposal, typeProposal);
    return proposal;
  }

  private int getLengthIdentifier(CompletionProposal proposal) {
    return proposal.getReplaceEndIdentifier() - proposal.getReplaceStart();
  }
}
