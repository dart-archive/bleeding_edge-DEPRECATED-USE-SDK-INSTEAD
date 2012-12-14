package com.google.dart.tools.core.internal.builder;

import com.google.dart.engine.source.Source;

/**
 * Used to represent an expected method call
 */
class Call {
  final String name;
  final Object[] args;

  public Call(String name, Object... args) {
    this.name = name;
    this.args = args;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Call)) {
      return false;
    }
    Call call = (Call) obj;
    if (!name.equals(call.name) || args.length != call.args.length) {
      return false;
    }
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        if (call.args[i] != null) {
          return false;
        }
      } else {
        if (!args[i].equals(call.args[i])) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(name);
    builder.append("(");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        builder.append(",");
      }
      Object arg = args[i];
      if (arg instanceof Source) {
        builder.append("Source[" + ((Source) arg).getFullName() + "]");
        continue;
      }
      builder.append(arg);
    }
    builder.append(")");
    return builder.toString();
  }
}