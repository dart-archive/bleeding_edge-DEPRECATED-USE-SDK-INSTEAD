package com.google.dart.tools.core.refactoring;

import com.google.dart.tools.core.refactoring.descriptors.RenameDartElementDescriptor;

import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;

/**
 * Interface for refactoring ids offered by the Dart tooling.
 * <p>
 * This interface provides refactoring ids for refactorings offered by the Dart tooling. Refactoring
 * instances corresponding to such an id may be instantiated by the refactoring framework using
 * {@link RefactoringCore#getRefactoringContribution(String)}. The resulting refactoring instance
 * may be executed on the workspace with a {@link PerformRefactoringOperation}.
 * <p>
 * Clients may obtain customizable refactoring descriptors for a certain refactoring by calling
 * {@link RefactoringCore#getRefactoringContribution(String)} with the appropriate refactoring id
 * and then calling {@link RefactoringContribution#createDescriptor()} to obtain a customizable
 * refactoring descriptor. The concrete subtype of refactoring descriptors is dependent from the
 * <code>id</code> argument.
 * </p>
 * <p>
 * Note: this interface is not intended to be implemented by clients.
 * </p>
 */
public interface IDartRefactorings {

  /**
   * Refactoring id of the 'Change Method Signature' refactoring (value:
   * <code>com.google.dart.tools.ui.change.method.signature</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ChangeMethodSignatureDescriptor}.
   * </p>
   */
  public static final String CHANGE_METHOD_SIGNATURE = "com.google.dart.tools.ui.change.method.signature"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Convert Anonymous To Nested' refactoring (value:
   * <code>com.google.dart.tools.ui.convert.anonymous</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ConvertAnonymousDescriptor}.
   * </p>
   */
  public static final String CONVERT_ANONYMOUS = "com.google.dart.tools.ui.convert.anonymous"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Convert Local Variable to Field' refactoring (value:
   * <code>com.google.dart.tools.ui.promote.temp</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ConvertLocalVariableDescriptor}.
   * </p>
   */
  public static final String CONVERT_LOCAL_VARIABLE = "com.google.dart.tools.ui.promote.temp"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Convert Member Type to Top Level' refactoring (value:
   * <code>com.google.dart.tools.ui.move.inner</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ConvertMemberTypeDescriptor}.
   * </p>
   */
  public static final String CONVERT_MEMBER_TYPE = "com.google.dart.tools.ui.move.inner"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Copy' refactoring (value: <code>com.google.dart.tools.ui.copy</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link CopyDescriptor}.
   * </p>
   */
  public static final String COPY = "com.google.dart.tools.ui.copy"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Delete' refactoring (value: <code>com.google.dart.tools.ui.delete</code>
   * ).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link DeleteDescriptor}.
   * </p>
   */
  public static final String DELETE = "com.google.dart.tools.ui.delete"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Encapsulate Field' refactoring (value:
   * <code>com.google.dart.tools.ui.self.encapsulate</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link EncapsulateFieldDescriptor}.
   * </p>
   */
  public static final String ENCAPSULATE_FIELD = "com.google.dart.tools.ui.self.encapsulate"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Extract Class' refactoring (value:
   * <code>"com.google.dart.tools.ui.extract.class</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link ExtractClassDescriptor}.
   * </p>
   * 
   * @since 1.2
   */
  public static final String EXTRACT_CLASS = "com.google.dart.tools.ui.extract.class"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Extract Constant' refactoring (value:
   * <code>com.google.dart.tools.ui.extract.constant</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ExtractConstantDescriptor}.
   * </p>
   */
  public static final String EXTRACT_CONSTANT = "com.google.dart.tools.ui.extract.constant"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Extract Interface' refactoring (value:
   * <code>com.google.dart.tools.ui.extract.interface</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ExtractInterfaceDescriptor}.
   * </p>
   */
  public static final String EXTRACT_INTERFACE = "com.google.dart.tools.ui.extract.interface"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Extract Local Variable' refactoring (value:
   * <code>com.google.dart.tools.ui.extract.temp</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link ExtractLocalDescriptor}.
   * </p>
   */
  public static final String EXTRACT_LOCAL_VARIABLE = "com.google.dart.tools.ui.extract.temp"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Extract Method' refactoring (value:
   * <code>com.google.dart.tools.ui.extract.method</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link ExtractMethodDescriptor}.
   * </p>
   */
  public static final String EXTRACT_METHOD = "com.google.dart.tools.ui.extract.method"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Extract Superclass' refactoring (value:
   * <code>com.google.dart.tools.ui.extract.superclass</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link ExtractSuperclassDescriptor}.
   * </p>
   */
  public static final String EXTRACT_SUPERCLASS = "com.google.dart.tools.ui.extract.superclass"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Generalize Declared Type' refactoring (value:
   * <code>com.google.dart.tools.ui.change.type</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link GeneralizeTypeDescriptor}
   * .
   * </p>
   */
  public static final String GENERALIZE_TYPE = "com.google.dart.tools.ui.change.type"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Infer Type Arguments' refactoring (value:
   * <code>com.google.dart.tools.ui.infer.typearguments</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link InferTypeArgumentsDescriptor}.
   * </p>
   */
  public static final String INFER_TYPE_ARGUMENTS = "com.google.dart.tools.ui.infer.typearguments"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Inline Constant' refactoring (value:
   * <code>com.google.dart.tools.ui.inline.constant</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link InlineConstantDescriptor}
   * .
   * </p>
   */
  public static final String INLINE_CONSTANT = "com.google.dart.tools.ui.inline.constant"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Inline Local Variable' refactoring (value:
   * <code>com.google.dart.tools.ui.inline.temp</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link InlineLocalVariableDescriptor}.
   * </p>
   */
  public static final String INLINE_LOCAL_VARIABLE = "com.google.dart.tools.ui.inline.temp"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Inline Method' refactoring (value:
   * <code>com.google.dart.tools.ui.inline.method</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link InlineMethodDescriptor}.
   * </p>
   */
  public static final String INLINE_METHOD = "com.google.dart.tools.ui.inline.method"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Introduce Factory' refactoring (value:
   * <code>com.google.dart.tools.ui.introduce.factory</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link IntroduceFactoryDescriptor}.
   * </p>
   */
  public static final String INTRODUCE_FACTORY = "com.google.dart.tools.ui.introduce.factory"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Introduce Indirection' refactoring (value:
   * <code>com.google.dart.tools.ui.introduce.indirection</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link IntroduceIndirectionDescriptor}.
   * </p>
   */
  public static final String INTRODUCE_INDIRECTION = "com.google.dart.tools.ui.introduce.indirection"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Introduce Parameter' refactoring (value:
   * <code>com.google.dart.tools.ui.introduce.parameter</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link IntroduceParameterDescriptor}.
   * </p>
   */
  public static final String INTRODUCE_PARAMETER = "com.google.dart.tools.ui.introduce.parameter"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Introduce Parameter Object' refactoring (value:
   * <code>com.google.dart.tools.ui.introduce.parameter.object</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link IntroduceParameterObjectDescriptor}.
   * </p>
   * 
   * @since 1.2
   */
  public static final String INTRODUCE_PARAMETER_OBJECT = "com.google.dart.tools.ui.introduce.parameter.object"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Move' refactoring (value: <code>com.google.dart.tools.ui.move</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link MoveDescriptor}.
   * </p>
   */
  public static final String MOVE = "com.google.dart.tools.ui.move"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Move Method' refactoring (value:
   * <code>com.google.dart.tools.ui.move.method</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link MoveMethodDescriptor}.
   * </p>
   */
  public static final String MOVE_METHOD = "com.google.dart.tools.ui.move.method"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Move Static Members' refactoring (value:
   * <code>com.google.dart.tools.ui.move.static</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link MoveStaticMembersDescriptor}.
   * </p>
   */
  public static final String MOVE_STATIC_MEMBERS = "com.google.dart.tools.ui.move.static"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Pull Up' refactoring (value:
   * <code>com.google.dart.tools.ui.pull.up</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link PullUpDescriptor}.
   * </p>
   */
  public static final String PULL_UP = "com.google.dart.tools.ui.pull.up"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Push Down' refactoring (value:
   * <code>com.google.dart.tools.ui.push.down</code> ).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link PushDownDescriptor}.
   * </p>
   */
  public static final String PUSH_DOWN = "com.google.dart.tools.ui.push.down"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Compilation Unit' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.compilationunit</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_COMPILATION_UNIT = "com.google.dart.tools.ui.rename.compilationunit"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Enum Constant' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.enum.constant</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_ENUM_CONSTANT = "com.google.dart.tools.ui.rename.enum.constant"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Field' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.field</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_FIELD = "com.google.dart.tools.ui.rename.field"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Java Project' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.java.project</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_DART_PROJECT = "com.google.dart.tools.ui.rename.java.project"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Local Variable' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.local.variable</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_LOCAL_VARIABLE = "com.google.dart.tools.ui.rename.local.variable"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Method' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.method</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_METHOD = "com.google.dart.tools.ui.rename.method"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Package' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.package</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_PACKAGE = "com.google.dart.tools.ui.rename.package"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Resource' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.resource</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link org.eclipse.jdt.core.refactoring.descriptors.RenameResourceDescriptor}.
   * </p>
   * 
   * @deprecated Since 1.2. Use
   *             {@link org.eclipse.ltk.core.refactoring.resource.RenameResourceDescriptor#ID}
   *             instead.
   */
  @Deprecated
  public static final String RENAME_RESOURCE = "com.google.dart.tools.ui.rename.resource"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Source Folder' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.source.folder</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_SOURCE_FOLDER = "com.google.dart.tools.ui.rename.source.folder"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Type' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.type</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_TYPE = "com.google.dart.tools.ui.rename.type"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Rename Type Parameter' refactoring (value:
   * <code>com.google.dart.tools.ui.rename.type.parameter</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to
   * {@link RenameDartElementDescriptor}.
   * </p>
   */
  public static final String RENAME_TYPE_PARAMETER = "com.google.dart.tools.ui.rename.type.parameter"; //$NON-NLS-1$

  /**
   * Refactoring id of the 'Use Supertype Where Possible' refactoring (value:
   * <code>com.google.dart.tools.ui.use.supertype</code>).
   * <p>
   * Clients may safely cast the obtained refactoring descriptor to {@link UseSupertypeDescriptor}.
   * </p>
   */
  public static final String USE_SUPER_TYPE = "com.google.dart.tools.ui.use.supertype"; //$NON-NLS-1$
}
