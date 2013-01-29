package com.google.dart.engine.services.completion;

/**
 * The various kinds of completion proposals. Each specifies the kind of completion to be created,
 * corresponding to different syntactical elements.
 */
public enum ProposalKind {
  CLASS,
  CLASS_ALIAS,
  CONSTRUCTOR,
  FIELD,
  FUNCTION,
  FUNCTION_ALIAS,
  GETTER,
  IMPORT,
  LIBRARY_PREFIX,
  METHOD,
  PARAMETER,
  SETTER,
  VARIABLE,
  TYPE_VARIABLE
}
