package com.google.dart.tools.internal.corext.refactoring.reorg;

public interface IReorgQueries {

  public static final int CONFIRM_DELETE_EMPTY_CUS = 2;

  public static final int CONFIRM_DELETE_FOLDERS_CONTAINING_SOURCE_FOLDERS = 4;

  public static final int CONFIRM_DELETE_GETTER_SETTER = 1;

  public static final int CONFIRM_DELETE_LINKED_PARENT = 8;

  public static final int CONFIRM_DELETE_REFERENCED_ARCHIVES = 3;

  public static final int CONFIRM_OVERWRITING = 6;

  public static final int CONFIRM_READ_ONLY_ELEMENTS = 5;

  public static final int CONFIRM_SKIPPING = 7;

  IConfirmQuery createSkipQuery(String queryTitle, int queryID);

  IConfirmQuery createYesNoQuery(String queryTitle, boolean allowCancel, int queryID);

  IConfirmQuery createYesYesToAllNoNoToAllQuery(String queryTitle, boolean allowCancel, int queryID);
}
