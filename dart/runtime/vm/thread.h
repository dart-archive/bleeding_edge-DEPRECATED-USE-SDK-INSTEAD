// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_THREAD_H_
#define VM_THREAD_H_

#include "vm/base_isolate.h"
#include "vm/globals.h"
#include "vm/os_thread.h"

namespace dart {

class CHA;
class Isolate;

// A VM thread; may be executing Dart code or performing helper tasks like
// garbage collection or compilation. The Thread structure associated with
// a thread is allocated by EnsureInit before entering an isolate, and destroyed
// automatically when the underlying OS thread exits. NOTE: On Windows, CleanUp
// must currently be called manually (issue 23474).
class Thread {
 public:
  // The currently executing thread, or NULL if not yet initialized.
  static Thread* Current() {
    return reinterpret_cast<Thread*>(OSThread::GetThreadLocal(thread_key_));
  }

  // Initializes the current thread as a VM thread, if not already done.
  static void EnsureInit();

  // Makes the current thread enter 'isolate'.
  static void EnterIsolate(Isolate* isolate);
  // Makes the current thread exit its isolate.
  static void ExitIsolate();

  // A VM thread other than the main mutator thread can enter an isolate as a
  // "helper" to gain limited concurrent access to the isolate. One example is
  // SweeperTask (which uses the class table, which is copy-on-write).
  // TODO(koda): Properly synchronize heap access to expand allowed operations.
  static void EnterIsolateAsHelper(Isolate* isolate);
  static void ExitIsolateAsHelper();

#if defined(TARGET_OS_WINDOWS)
  // Clears the state of the current thread and frees the allocation.
  static void CleanUp();
#endif

  // Called at VM startup.
  static void InitOnce();

  // The topmost zone used for allocation in this thread.
  Zone* zone() {
    return reinterpret_cast<BaseIsolate*>(isolate())->current_zone();
  }

  // The isolate that this thread is operating on, or NULL if none.
  Isolate* isolate() const { return isolate_; }

  // The (topmost) CHA for the compilation in the isolate of this thread.
  // TODO(23153): Move this out of Isolate/Thread.
  CHA* cha() const;
  void set_cha(CHA* value);

 private:
  static ThreadLocalKey thread_key_;

  Isolate* isolate_;

  Thread()
      : isolate_(NULL) {}

  static void SetCurrent(Thread* current);

  DISALLOW_COPY_AND_ASSIGN(Thread);
};

}  // namespace dart

#endif  // VM_THREAD_H_
