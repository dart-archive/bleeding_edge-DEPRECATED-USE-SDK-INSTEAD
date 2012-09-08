package com.google.dart.tools.core.analysis;

import java.io.File;

public class BlockingTask extends Task {

  private Object lock = new Object();
  private boolean started = false;
  private boolean blocked = true;
  private boolean performed = false;

  @Override
  public boolean canRemove(File discarded) {
    return false;
  }

  @Override
  public boolean isPriority() {
    return false;
  }

  @Override
  public void perform() {
    synchronized (lock) {
      started = true;
      lock.notifyAll();
      while (blocked) {
        try {
          lock.wait();
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      performed = true;
      lock.notifyAll();
    }
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "[blocking," + hashCode() + "]";
  }

  public boolean wasPerformed() {
    synchronized (lock) {
      return performed;
    }
  }

  void unblock() {
    synchronized (lock) {
      blocked = false;
      lock.notifyAll();
    }
  }

  boolean waitUntilStarted(long milliseconds) {
    long end = System.currentTimeMillis() + milliseconds;
    synchronized (lock) {
      while (!started) {
        long delta = end - System.currentTimeMillis();
        if (delta <= 0) {
          return false;
        }
        try {
          lock.wait(delta);
        } catch (InterruptedException e) {
          //$FALL-THROUGH$
        }
      }
      return true;
    }
  }
}
