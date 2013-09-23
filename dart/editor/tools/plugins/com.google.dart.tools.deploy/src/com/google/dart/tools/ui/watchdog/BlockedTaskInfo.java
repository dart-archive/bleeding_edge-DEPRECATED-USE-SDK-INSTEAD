package com.google.dart.tools.ui.watchdog;

public class BlockedTaskInfo {
  private String reason;
  private String taskName;
  private long startTime;

  public BlockedTaskInfo(String taskName, String reason, long startTime) {
    this.taskName = taskName;
    this.reason = reason;
    this.startTime = startTime;
  }

  public String getReason() {
    return reason;
  }

  public long getStartTimeInMilliseconds() {
    return startTime;
  }

  public String getTaskName() {
    return taskName;
  }
}
