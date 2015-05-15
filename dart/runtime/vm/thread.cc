// Copyright (c) 2015, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/thread.h"

#include "vm/isolate.h"
#include "vm/os_thread.h"
#include "vm/profiler.h"
#include "vm/thread_interrupter.h"


namespace dart {

// The single thread local key which stores all the thread local data
// for a thread.
// TODO(koda): Can we merge this with ThreadInterrupter::thread_state_key_?
ThreadLocalKey Thread::thread_key_ = OSThread::kUnsetThreadLocalKey;


static void DeleteThread(void* thread) {
  delete reinterpret_cast<Thread*>(thread);
}


void Thread::InitOnce() {
  ASSERT(thread_key_ == OSThread::kUnsetThreadLocalKey);
  thread_key_ = OSThread::CreateThreadLocal(DeleteThread);
  ASSERT(thread_key_ != OSThread::kUnsetThreadLocalKey);
}


void Thread::SetCurrent(Thread* current) {
  OSThread::SetThreadLocal(thread_key_, reinterpret_cast<uword>(current));
}


void Thread::EnsureInit() {
  if (Thread::Current() == NULL) {
    SetCurrent(new Thread());
  }
}


#if defined(TARGET_OS_WINDOWS)
void Thread::CleanUp() {
  Thread* current = Current();
  if (current != NULL) {
    delete current;
  }
  SetCurrent(NULL);
}
#endif


void Thread::EnterIsolate(Isolate* isolate) {
  Thread* thread = Thread::Current();
  ASSERT(thread != NULL);
  ASSERT(thread->isolate() == NULL);
  ASSERT(isolate->mutator_thread() == NULL);
  thread->isolate_ = isolate;
  isolate->set_mutator_thread(thread);
  // TODO(koda): Migrate thread_state_ and profile_data_ to Thread, to allow
  // helper threads concurrent with mutator.
  ASSERT(isolate->thread_state() == NULL);
  InterruptableThreadState* thread_state =
      ThreadInterrupter::GetCurrentThreadState();
#if defined(DEBUG)
  Isolate::CheckForDuplicateThreadState(thread_state);
#endif
  ASSERT(thread_state != NULL);
  Profiler::BeginExecution(isolate);
  isolate->set_thread_state(thread_state);
  isolate->set_vm_tag(VMTag::kVMTagId);
}


void Thread::ExitIsolate() {
  Thread* thread = Thread::Current();
  // TODO(koda): Audit callers; they should know whether they're in an isolate.
  if (thread == NULL || thread->isolate() == NULL) return;
  Isolate* isolate = thread->isolate();
  if (isolate->is_runnable()) {
    isolate->set_vm_tag(VMTag::kIdleTagId);
  } else {
    isolate->set_vm_tag(VMTag::kLoadWaitTagId);
  }
  isolate->set_thread_state(NULL);
  Profiler::EndExecution(isolate);
  isolate->set_mutator_thread(NULL);
  thread->isolate_ = NULL;
  ASSERT(Isolate::Current() == NULL);
}


void Thread::EnterIsolateAsHelper(Isolate* isolate) {
  Thread* thread = Thread::Current();
  ASSERT(thread != NULL);
  ASSERT(thread->isolate() == NULL);
  thread->isolate_ = isolate;
  // Do not update isolate->mutator_thread, but perform sanity check:
  // this thread should not be both the main mutator and helper.
  ASSERT(isolate->mutator_thread() != thread);
}


void Thread::ExitIsolateAsHelper() {
  Thread* thread = Thread::Current();
  Isolate* isolate = thread->isolate();
  ASSERT(isolate != NULL);
  thread->isolate_ = NULL;
  ASSERT(isolate->mutator_thread() != thread);
}


CHA* Thread::cha() const {
  ASSERT(isolate_ != NULL);
  return isolate_->cha_;
}


void Thread::set_cha(CHA* value) {
  ASSERT(isolate_ != NULL);
  isolate_->cha_ = value;
}

}  // namespace dart
