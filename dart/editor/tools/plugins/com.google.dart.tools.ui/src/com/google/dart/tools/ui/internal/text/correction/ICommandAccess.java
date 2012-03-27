package com.google.dart.tools.ui.internal.text.correction;

/**
 * Correction proposals implement this interface to by invokable by a command. (e.g. keyboard
 * shortcut)
 */
public interface ICommandAccess {

  /**
   * Returns the id of the command that should invoke this correction proposal
   * 
   * @return the id of the command. This id must start with
   *         {@link CorrectionCommandInstaller#COMMAND_PREFIX} to be recognixes as correction
   *         command.
   */
  String getCommandId();

}
