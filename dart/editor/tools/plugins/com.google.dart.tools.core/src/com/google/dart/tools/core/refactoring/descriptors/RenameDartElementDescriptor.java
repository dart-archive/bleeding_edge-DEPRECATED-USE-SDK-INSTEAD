package com.google.dart.tools.core.refactoring.descriptors;

import com.google.dart.tools.core.internal.refactoring.descriptors.DartRefactoringDescriptorUtil;
import com.google.dart.tools.core.internal.refactoring.descriptors.DescriptorMessages;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.Field;
import com.google.dart.tools.core.model.Method;
import com.google.dart.tools.core.model.Type;
import com.google.dart.tools.core.refactoring.IDartRefactorings;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.util.Map;

/**
 * Refactoring descriptor for the rename Dart element refactoring.
 * <p>
 * An instance of this refactoring descriptor may be obtained by calling
 * {@link RefactoringContribution#createDescriptor()} on a refactoring contribution requested by
 * invoking {@link RefactoringCore#getRefactoringContribution(String)} with the appropriate
 * refactoring id.
 * </p>
 * <p>
 * Note: this class is not intended to be instantiated by clients.
 * </p>
 */
public final class RenameDartElementDescriptor extends DartRefactoringDescriptor {

//  /** The delegate attribute */
//  private static final String ATTRIBUTE_DELEGATE = "delegate"; //$NON-NLS-1$

  /** The match strategy attribute */
  private static final String ATTRIBUTE_MATCH_STRATEGY = "matchStrategy"; //$NON-NLS-1$

//  /** The parameter attribute */
//  private static final String ATTRIBUTE_PARAMETER = "parameter"; //$NON-NLS-1$

  /** The patterns attribute */
  private static final String ATTRIBUTE_PATTERNS = "patterns"; //$NON-NLS-1$

  /** The similar declarations attribute */
  private static final String ATTRIBUTE_SIMILAR_DECLARATIONS = "similarDeclarations"; //$NON-NLS-1$

  /** The textual matches attribute */
  private static final String ATTRIBUTE_TEXTUAL_MATCHES = "textual"; //$NON-NLS-1$

  /**
   * Similar declaration updating strategy which finds exact names and embedded names as well
   * (value: <code>2</code>).
   */
  public static final int STRATEGY_EMBEDDED = 2;

  /**
   * Similar declaration updating strategy which finds exact names only (value: <code>1</code>).
   */
  public static final int STRATEGY_EXACT = 1;

  /**
   * Similar declaration updating strategy which finds exact names, embedded names and name suffixes
   * (value: <code>3</code>).
   */
  public static final int STRATEGY_SUFFIX = 3;

  /**
   * @deprecated Replaced by
   *             {@link org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor#ID}
   */
  @Deprecated
  private static final String RENAME_RESOURCE = IDartRefactorings.RENAME_RESOURCE;

  /** The delegate attribute */
  private boolean fDelegate = false;

  /**
   * The Dart element attribute. WARNING: may not exist, see comment in
   * {@link DartRefactoringDescriptorUtil#handleToElement(org.eclipse.jdt.core.WorkingCopyOwner, String, String, boolean)}
   * .
   */
  private DartElement fDartElement = null;

  /** The match strategy */
  private int fMatchStrategy = STRATEGY_EXACT;

  /** The name attribute */
  private String fName = null;

  /** The patterns attribute */
  private String fPatterns = null;

  /** The references attribute */
  private boolean fReferences = false;

  /** The similar declarations attribute */
  private boolean fSimilarDeclarations = false;

  /** The textual attribute */
  private boolean fTextual = false;

  /**
   * Creates a new refactoring descriptor.
   * 
   * @param id the unique id of the rename refactoring
   * @see IDartRefactorings
   */
  public RenameDartElementDescriptor(final String id) {
    super(id);
    Assert.isLegal(checkId(id), "Refactoring id is not a rename refactoring id"); //$NON-NLS-1$
  }

  /**
   * Creates a new refactoring descriptor.
   * 
   * @param id the ID of this descriptor
   * @param project the non-empty name of the project associated with this refactoring, or
   *          <code>null</code> for a workspace refactoring
   * @param description a non-empty human-readable description of the particular refactoring
   *          instance
   * @param comment the human-readable comment of the particular refactoring instance, or
   *          <code>null</code> for no comment
   * @param arguments a map of arguments that will be persisted and describes all settings for this
   *          refactoring
   * @param flags the flags of the refactoring descriptor
   * @throws IllegalArgumentException if the argument map contains invalid keys/values
   */
  public RenameDartElementDescriptor(String id, String project, String description, String comment,
      Map<String, String> arguments, int flags) {
    super(id, project, description, comment, arguments, flags);
    Assert.isLegal(checkId(id), "Refactoring id is not a rename refactoring id"); //$NON-NLS-1$
    fName = DartRefactoringDescriptorUtil.getString(fArguments, ATTRIBUTE_NAME);
    if (getID().equals(IDartRefactorings.RENAME_TYPE_PARAMETER)) {
//      fJavaElement = JavaRefactoringDescriptorUtil.getJavaElement(fArguments, ATTRIBUTE_INPUT,
//          getProject());
//      String parameterName = JavaRefactoringDescriptorUtil.getString(fArguments,
//          ATTRIBUTE_PARAMETER);
//      if (fJavaElement instanceof IType) {
//        fJavaElement = ((IType) fJavaElement).getTypeParameter(parameterName);
//      }
//      if (fJavaElement instanceof IMethod) {
//        fJavaElement = ((IMethod) fJavaElement).getTypeParameter(parameterName);
//      }
    } else {
      fDartElement = DartRefactoringDescriptorUtil.getDartElement(fArguments, ATTRIBUTE_INPUT,
          getProject());
    }
    final int type = fDartElement.getElementType();
//    if (type != DartElement.PACKAGE_FRAGMENT_ROOT)
    {
      fReferences = DartRefactoringDescriptorUtil.getBoolean(fArguments, ATTRIBUTE_REFERENCES,
          fReferences);
    }
    switch (type) {
//      case DartElement.PACKAGE_FRAGMENT:
      case DartElement.TYPE:
      case DartElement.FIELD:
        fTextual = DartRefactoringDescriptorUtil.getBoolean(fArguments, ATTRIBUTE_TEXTUAL_MATCHES,
            fTextual);
        break;
      default:
        break;
    }
//    switch (type) {
//      case DartElement.METHOD:
//      case DartElement.FIELD:
//        fDelegate = DartRefactoringDescriptorUtil.getBoolean(fArguments, ATTRIBUTE_DELEGATE,
//            fDelegate);
//        break;
//      default:
//        break;
//    }
    switch (type) {
//      case DartElement.PACKAGE_FRAGMENT:
      case DartElement.TYPE:
//        fQualified = DartRefactoringDescriptorUtil.getBoolean(fArguments, ATTRIBUTE_QUALIFIED,
//            fQualified);
        fPatterns = DartRefactoringDescriptorUtil.getString(fArguments, ATTRIBUTE_PATTERNS, true);
        break;
      default:
        break;
    }
    switch (type) {
      case DartElement.TYPE:
        fSimilarDeclarations = DartRefactoringDescriptorUtil.getBoolean(fArguments,
            ATTRIBUTE_SIMILAR_DECLARATIONS, fSimilarDeclarations);
        fMatchStrategy = DartRefactoringDescriptorUtil.getInt(fArguments, ATTRIBUTE_MATCH_STRATEGY,
            fMatchStrategy);
        break;
      default:
        break;
    }
    switch (type) {
//      case DartElement.PACKAGE_FRAGMENT:
//        fHierarchical = JavaRefactoringDescriptorUtil.getBoolean(fArguments,
//            ATTRIBUTE_HIERARCHICAL, fHierarchical);
//        break;
      default:
        break;
    }
  }

//  /**
//   * Determines whether the delegate for a Dart element should be declared as deprecated.
//   * <p>
//   * Note: Deprecation of the delegate is currently applicable to the Dart elements {@link Method}
//   * and {@link Field}. The default is to not deprecate the delegate.
//   * </p>
//   * 
//   * @param deprecate <code>true</code> to deprecate the delegate, <code>false</code> otherwise
//   */
//  public void setDeprecateDelegate(final boolean deprecate) {
//    fDeprecate = deprecate;
//  }

  /**
   * Sets the Dart element to be renamed.
   * <p>
   * Note: If the Dart element to be renamed is of type {@link DartElement#DART_PROJECT}, clients
   * are required to to set the project name to <code>null</code>.
   * </p>
   * 
   * @param element the Dart element to be renamed
   */
  public void setDartElement(final DartElement element) {
    Assert.isNotNull(element);
    fDartElement = element;
  }

  /**
   * Sets the file name patterns to use during qualified name updating.
   * <p>
   * The syntax of the file name patterns is a sequence of individual name patterns, separated by
   * comma. Additionally, wildcard characters '*' (any string) and '?' (any character) may be used.
   * </p>
   * <p>
   * Note: If file name patterns are set, qualified name updating must be enabled by calling
   * {@link #setUpdateQualifiedNames(boolean)}.
   * </p>
   * <p>
   * Note: Qualified name updating is currently applicable to the Dart elements {@link Type}. The
   * default is to use no file name patterns (meaning that all files are processed).
   * </p>
   * 
   * @param patterns the non-empty file name patterns string
   */
  public void setFileNamePatterns(final String patterns) {
    Assert.isNotNull(patterns);
    Assert.isLegal(!"".equals(patterns), "Pattern must not be empty"); //$NON-NLS-1$ //$NON-NLS-2$
    fPatterns = patterns;
  }

  /**
   * Determines whether the the original Dart element should be kept as delegate to the renamed one.
   * <p>
   * Note: Keeping of original elements as delegates is currently applicable to the Dart elements
   * {@link Method} and {@link Field}. The default is to not keep the original as delegate.
   * </p>
   * 
   * @param delegate <code>true</code> to keep the original, <code>false</code> otherwise
   */
  public void setKeepOriginal(final boolean delegate) {
    fDelegate = delegate;
  }

  /**
   * Determines which strategy should be used during similar declaration updating.
   * <p>
   * Valid arguments are {@link #STRATEGY_EXACT}, {@link #STRATEGY_EMBEDDED} or
   * {@link #STRATEGY_SUFFIX}.
   * </p>
   * <p>
   * Note: Similar declaration updating is currently applicable to Dart elements of type
   * {@link Type}. The default is to use the {@link #STRATEGY_EXACT} match strategy.
   * </p>
   * 
   * @param strategy the match strategy to use
   */
  public void setMatchStrategy(final int strategy) {
    Assert.isLegal(strategy == STRATEGY_EXACT || strategy == STRATEGY_EMBEDDED
        || strategy == STRATEGY_SUFFIX, "Wrong match strategy argument"); //$NON-NLS-1$
    fMatchStrategy = strategy;
  }

  /**
   * Sets the new name to rename the Dart element to.
   * 
   * @param name the non-empty new name to set
   */
  public void setNewName(final String name) {
    Assert.isNotNull(name);
    Assert.isLegal(!"".equals(name), "Name must not be empty"); //$NON-NLS-1$//$NON-NLS-2$
    fName = name;
  }

  /**
   * Sets the project name of this refactoring.
   * <p>
   * Note: If the Dart element to be renamed is of type {@link DartElement#DART_PROJECT}, clients
   * are required to to set the project name to <code>null</code>.
   * </p>
   * <p>
   * The default is to associate the refactoring with the workspace.
   * </p>
   * 
   * @param project the non-empty project name to set, or <code>null</code> for the workspace
   * @see #getProject()
   */
  @Override
  public void setProject(final String project) {
    super.setProject(project);
  }

//  /**
//   * Determines whether other Dart elements in the hierarchy of the input element should be renamed
//   * as well.
//   * <p>
//   * Note: Hierarchical updating is currently applicable for Dart elements of type
//   * {@link IPackageFragment}. The default is to not update Dart elements hierarchically.
//   * </p>
//   * 
//   * @param update <code>true</code> to update hierarchically, <code>false</code> otherwise
//   */
//  public void setUpdateHierarchy(final boolean update) {
//    fHierarchical = update;
//  }

//  /**
//   * Determines whether qualified names of the Dart element should be renamed.
//   * <p>
//   * Qualified name updating adapts fully qualified names of the Dart element to be renamed in
//   * non-Dart text files. Clients may specify file name patterns by calling
//   * {@link #setFileNamePatterns(String)} to constrain the set of text files to be processed.
//   * </p>
//   * <p>
//   * Note: Qualified name updating is currently applicable to the Dart elements {@link Type}. The
//   * default is to not rename qualified names.
//   * </p>
//   * 
//   * @param update <code>true</code> to update qualified names, <code>false</code> otherwise
//   */
//  public void setUpdateQualifiedNames(final boolean update) {
//    fQualified = update;
//  }

  /**
   * Determines whether references to the Dart element should be renamed.
   * <p>
   * Note: Reference updating is currently applicable to all Dart element types. The default is to
   * not update references.
   * </p>
   * 
   * @param update <code>true</code> to update references, <code>false</code> otherwise
   */
  public void setUpdateReferences(final boolean update) {
    fReferences = update;
  }

  /**
   * Determines whether similar declarations of the Dart element should be updated.
   * <p>
   * Note: Similar declaration updating is currently applicable to Dart elements of type
   * {@link Type}. The default is to not update similar declarations.
   * </p>
   * 
   * @param update <code>true</code> to update similar declarations, <code>false</code> otherwise
   */
  public void setUpdateSimilarDeclarations(final boolean update) {
    fSimilarDeclarations = update;
  }

  /**
   * Determines whether textual occurrences of the Dart element should be renamed.
   * <p>
   * Textual occurrence updating adapts textual occurrences of the Dart element to be renamed in
   * Dart comments and Dart strings.
   * </p>
   * <p>
   * Note: Textual occurrence updating is currently applicable to the Dart elements {@link Type} and
   * {@link Field}. The default is to not rename textual occurrences.
   * </p>
   * 
   * @param update <code>true</code> to update occurrences, <code>false</code> otherwise
   */
  public void setUpdateTextualOccurrences(final boolean update) {
    fTextual = update;
  }

  @Override
  public RefactoringStatus validateDescriptor() {
    RefactoringStatus status = super.validateDescriptor();
    if (fName == null || fName.isEmpty()) {
      status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameResourceDescriptor_no_new_name));
    }
    if (fDartElement == null) {
      status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameDartElementDescriptor_no_dart_element));
    } else {
      final int type = fDartElement.getElementType();
      if (type == DartElement.DART_PROJECT && getProject() != null) {
        status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameDartElementDescriptor_project_constraint));
      }
//      if (type == DartElement.PACKAGE_FRAGMENT_ROOT && fReferences) {
//        status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameJavaElementDescriptor_reference_constraint));
//      }
      if (fTextual) {
        switch (type) {
//          case DartElement.PACKAGE_FRAGMENT:
          case DartElement.TYPE:
          case DartElement.FIELD:
            break;
          default:
            status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameDartElementDescriptor_textual_constraint));
        }
      }
//      if (fDeprecate) {
//        switch (type) {
//          case DartElement.METHOD:
//          case DartElement.FIELD:
//            break;
//          default:
//            status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameDartElementDescriptor_deprecation_constraint));
//        }
//      }
      if (fDelegate) {
        switch (type) {
          case DartElement.METHOD:
          case DartElement.FIELD:
            break;
          default:
            status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameDartElementDescriptor_delegate_constraint));
        }
      }
      if (fSimilarDeclarations) {
        switch (type) {
          case DartElement.TYPE:
            break;
          default:
            status.merge(RefactoringStatus.createFatalErrorStatus(DescriptorMessages.RenameDartElementDescriptor_similar_constraint));
        }
      }
    }
    return status;
  }

  @Override
  protected void populateArgumentMap() {
    super.populateArgumentMap();
    DartRefactoringDescriptorUtil.setString(fArguments, ATTRIBUTE_NAME, fName);
//    if (getID().equals(IJavaRefactorings.RENAME_TYPE_PARAMETER)) {
//      final ITypeParameter parameter = (ITypeParameter) fJavaElement;
//      JavaRefactoringDescriptorUtil.setJavaElement(fArguments, ATTRIBUTE_INPUT, getProject(),
//          parameter.getDeclaringMember());
//      JavaRefactoringDescriptorUtil.setString(fArguments, ATTRIBUTE_PARAMETER,
//          parameter.getElementName());
//    } else {
//      JavaRefactoringDescriptorUtil.setJavaElement(fArguments, ATTRIBUTE_INPUT, getProject(),
//          fJavaElement);
//    }
    final int type = fDartElement.getElementType();
//    if (type != DartElement.PACKAGE_FRAGMENT_ROOT)
    {
      DartRefactoringDescriptorUtil.setBoolean(fArguments, ATTRIBUTE_REFERENCES, fReferences);
    }
    switch (type) {
//      case DartElement.PACKAGE_FRAGMENT:
      case DartElement.TYPE:
      case DartElement.FIELD:
        DartRefactoringDescriptorUtil.setBoolean(fArguments, ATTRIBUTE_TEXTUAL_MATCHES, fTextual);
        break;
      default:
        break;
    }
//    switch (type) {
//      case DartElement.METHOD:
//      case DartElement.FIELD:
//        DartRefactoringDescriptorUtil.setBoolean(fArguments, ATTRIBUTE_DELEGATE, fDelegate);
//        break;
//      default:
//        break;
//    }
    switch (type) {
//      case DartElement.PACKAGE_FRAGMENT:
      case DartElement.TYPE:
//        DartRefactoringDescriptorUtil.setBoolean(fArguments, ATTRIBUTE_QUALIFIED, fQualified);
        DartRefactoringDescriptorUtil.setString(fArguments, ATTRIBUTE_PATTERNS, fPatterns);
        break;
      default:
        break;
    }
    switch (type) {
      case DartElement.TYPE:
        DartRefactoringDescriptorUtil.setBoolean(fArguments, ATTRIBUTE_SIMILAR_DECLARATIONS,
            fSimilarDeclarations);
        DartRefactoringDescriptorUtil.setInt(fArguments, ATTRIBUTE_MATCH_STRATEGY, fMatchStrategy);
        break;
      default:
        break;
    }
    switch (type) {
//      case DartElement.PACKAGE_FRAGMENT:
//        JavaRefactoringDescriptorUtil.setBoolean(fArguments, ATTRIBUTE_HIERARCHICAL, fHierarchical);
//        break;
      default:
        break;
    }
  }

  /**
   * Checks whether the refactoring id is valid.
   * 
   * @param id the refactoring id
   * @return the outcome of the validation
   */
  private boolean checkId(final String id) {
    Assert.isNotNull(id);
    if (id.equals(IDartRefactorings.RENAME_COMPILATION_UNIT)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_ENUM_CONSTANT)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_FIELD)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_DART_PROJECT)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_LOCAL_VARIABLE)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_METHOD)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_PACKAGE)) {
      return true;
    } else if (id.equals(RENAME_RESOURCE)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_SOURCE_FOLDER)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_TYPE)) {
      return true;
    } else if (id.equals(IDartRefactorings.RENAME_TYPE_PARAMETER)) {
      return true;
    }
    return false;
  }
}
