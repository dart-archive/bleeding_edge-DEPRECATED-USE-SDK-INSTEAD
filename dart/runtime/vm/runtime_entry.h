// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_RUNTIME_ENTRY_H_
#define VM_RUNTIME_ENTRY_H_

#include "vm/allocation.h"
#include "vm/assembler.h"
#include "vm/flags.h"
#include "vm/native_arguments.h"
#include "vm/tags.h"

namespace dart {

DECLARE_FLAG(bool, trace_runtime_calls);

typedef void (*RuntimeFunction)(NativeArguments arguments);


// Class RuntimeEntry is used to encapsulate runtime functions, it includes
// the entry point for the runtime function and the number of arguments expected
// by the function.
class RuntimeEntry : public ValueObject {
 public:
  RuntimeEntry(const char* name, RuntimeFunction function,
               intptr_t argument_count, bool is_leaf, bool is_float)
      : name_(name),
        function_(function),
        argument_count_(argument_count),
        is_leaf_(is_leaf),
        is_float_(is_float),
        next_(NULL) {
    // Strip off const for registration.
    VMTag::RegisterRuntimeEntry(const_cast<RuntimeEntry*>(this));
  }
  ~RuntimeEntry() {}

  const char* name() const { return name_; }
  RuntimeFunction function() const { return function_; }
  intptr_t argument_count() const { return argument_count_; }
  bool is_leaf() const { return is_leaf_; }
  bool is_float() const { return is_float_; }
  uword GetEntryPoint() const { return reinterpret_cast<uword>(function()); }

  // Generate code to call the runtime entry.
  void Call(Assembler* assembler, intptr_t argument_count) const;

  void set_next(const RuntimeEntry* next) { next_ = next; }
  const RuntimeEntry* next() const { return next_; }

 private:
  const char* name_;
  const RuntimeFunction function_;
  const intptr_t argument_count_;
  const bool is_leaf_;
  const bool is_float_;
  const RuntimeEntry* next_;

  DISALLOW_COPY_AND_ASSIGN(RuntimeEntry);
};


// Helper macros for declaring and defining runtime entries.

#define DEFINE_RUNTIME_ENTRY(name, argument_count)                             \
  extern void DRT_##name(NativeArguments arguments);                           \
  extern const RuntimeEntry k##name##RuntimeEntry(                             \
      "DRT_"#name, &DRT_##name, argument_count, false, false);                 \
  static void DRT_Helper##name(Isolate* isolate,                               \
                               Thread* thread,                                 \
                               Zone* zone,                                     \
                               NativeArguments arguments);                     \
  void DRT_##name(NativeArguments arguments) {                                 \
    CHECK_STACK_ALIGNMENT;                                                     \
    VERIFY_ON_TRANSITION;                                                      \
    ASSERT(arguments.ArgCount() == argument_count);                            \
    if (FLAG_trace_runtime_calls) OS::Print("Runtime call: %s\n", ""#name);    \
    {                                                                          \
      Isolate* isolate = arguments.isolate();                                  \
      Thread* thread = isolate->mutator_thread();                              \
      ASSERT(thread == Thread::Current());                                     \
      StackZone zone(isolate);                                                 \
      HANDLESCOPE(isolate);                                                    \
      DRT_Helper##name(isolate, thread, zone.GetZone(), arguments);            \
    }                                                                          \
    VERIFY_ON_TRANSITION;                                                      \
  }                                                                            \
  static void DRT_Helper##name(Isolate* isolate,                               \
                               Thread* thread,                                 \
                               Zone* zone,                                     \
                               NativeArguments arguments)

#define DECLARE_RUNTIME_ENTRY(name)                                            \
  extern const RuntimeEntry k##name##RuntimeEntry

#define DEFINE_LEAF_RUNTIME_ENTRY(type, name, argument_count, ...)             \
  extern "C" type DLRT_##name(__VA_ARGS__);                                    \
  extern const RuntimeEntry k##name##RuntimeEntry(                             \
      "DLRT_"#name, reinterpret_cast<RuntimeFunction>(&DLRT_##name),           \
      argument_count, true, false);                                            \
  type DLRT_##name(__VA_ARGS__) {                                              \
    CHECK_STACK_ALIGNMENT;                                                     \
    NoSafepointScope no_safepoint_scope;                                       \

#define END_LEAF_RUNTIME_ENTRY }

#define DECLARE_LEAF_RUNTIME_ENTRY(type, name, ...)                            \
  extern const RuntimeEntry k##name##RuntimeEntry;                             \
  extern "C" type DLRT_##name(__VA_ARGS__)

}  // namespace dart

#endif  // VM_RUNTIME_ENTRY_H_
