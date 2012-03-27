package com.google.dart.tools.core.internal.refactoring.descriptors;

import org.eclipse.osgi.util.NLS;

public class DescriptorMessages extends NLS {

  private static final String BUNDLE_NAME = DescriptorMessages.class.getName();

  public static String DartRefactoringDescriptor_no_description;

  public static String DartRefactoringDescriptor_no_resulting_descriptor;

  public static String DartRefactoringDescriptor_not_available;

  public static String MoveDescriptor_no_destination_set;

  public static String MoveDescriptor_no_elements_set;

  public static String MoveStaticMembersDescriptor_invalid_members;

  public static String MoveStaticMembersDescriptor_no_members;

  public static String MoveStaticMembersDescriptor_no_type;

  public static String RenameDartElementDescriptor_accessor_constraint;

  public static String RenameDartElementDescriptor_delegate_constraint;

  public static String RenameDartElementDescriptor_deprecation_constraint;

  public static String RenameDartElementDescriptor_hierarchical_constraint;

  public static String RenameDartElementDescriptor_no_dart_element;

  public static String RenameDartElementDescriptor_patterns_constraint;

  public static String RenameDartElementDescriptor_patterns_qualified_constraint;

  public static String RenameDartElementDescriptor_project_constraint;

  public static String RenameDartElementDescriptor_qualified_constraint;

  public static String RenameDartElementDescriptor_reference_constraint;

  public static String RenameDartElementDescriptor_similar_constraint;

  public static String RenameDartElementDescriptor_textual_constraint;

  public static String RenameLocalVariableDescriptor_no_compilation_unit;

  public static String RenameLocalVariableDescriptor_no_selection;

  public static String RenameResourceDescriptor_no_new_name;

  public static String RenameResourceDescriptor_no_resource;

  public static String RenameResourceDescriptor_project_constraint;

  public static String RenameResourceRefactoringContribution_error_cannot_access;

  public static String UseSupertypeDescriptor_no_subtype;

  public static String UseSupertypeDescriptor_no_supertype;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, DescriptorMessages.class);
  }

  private DescriptorMessages() {
  }
}
