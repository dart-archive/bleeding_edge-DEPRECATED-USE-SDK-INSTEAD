// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_REUSABLE_HANDLES_H_
#define VM_REUSABLE_HANDLES_H_

#include "vm/allocation.h"
#include "vm/handles.h"
#include "vm/object.h"

namespace dart {

// The class ReusableHandleScope is used in regions of the
// virtual machine where isolate specific reusable handles are used.
// This class asserts that we do not add code that will result in recursive
// uses of reusable handles.
// It is used as follows:
// {
//   ReusableHandleScope reused_handles(isolate);
//   ....
//   .....
//   code that uses isolate specific reusable handles.
//   Array& funcs = reused_handles.ArrayHandle();
//   ....
// }

#if defined(DEBUG)
#define REUSABLE_SCOPE(name)                                                   \
  class Reusable##name##HandleScope : public ValueObject {                     \
    public:                                                                    \
    explicit Reusable##name##HandleScope(Isolate* isolate)                     \
        : isolate_(isolate) {                                                  \
      ASSERT(!isolate->reusable_##name##_handle_scope_active());               \
      isolate->set_reusable_##name##_handle_scope_active(true);                \
    }                                                                          \
    Reusable##name##HandleScope() : isolate_(Isolate::Current()) {             \
      ASSERT(!isolate_->reusable_##name##_handle_scope_active());              \
      isolate_->set_reusable_##name##_handle_scope_active(true);               \
    }                                                                          \
    ~Reusable##name##HandleScope() {                                           \
      ASSERT(isolate_->reusable_##name##_handle_scope_active());               \
      isolate_->set_reusable_##name##_handle_scope_active(false);              \
      Handle().raw_ = name::null();                                            \
    }                                                                          \
    name& Handle() const {                                                     \
      ASSERT(isolate_->name##_handle_ != NULL);                                \
      return *isolate_->name##_handle_;                                        \
    }                                                                          \
    private:                                                                   \
    Isolate* isolate_;                                                         \
    DISALLOW_COPY_AND_ASSIGN(Reusable##name##HandleScope);                     \
  };
#else
#define REUSABLE_SCOPE(name)                                                   \
  class Reusable##name##HandleScope : public ValueObject {                     \
    public:                                                                    \
    explicit Reusable##name##HandleScope(Isolate* isolate)                     \
        : handle_(isolate->name##_handle_) {                                   \
    }                                                                          \
    Reusable##name##HandleScope()                                              \
        : handle_(Isolate::Current()->name##_handle_) {                        \
    }                                                                          \
    ~Reusable##name##HandleScope() {                                           \
      handle_->raw_ = name::null();                                            \
    }                                                                          \
    name& Handle() const {                                                     \
      ASSERT(handle_ != NULL);                                                 \
      return *handle_;                                                         \
    }                                                                          \
    private:                                                                   \
    name* handle_;                                                             \
    DISALLOW_COPY_AND_ASSIGN(Reusable##name##HandleScope);                     \
  };
#endif  // defined(DEBUG)
REUSABLE_HANDLE_LIST(REUSABLE_SCOPE)
#undef REUSABLE_SCOPE

#define REUSABLE_OBJECT_HANDLESCOPE(isolate)                                   \
  ReusableObjectHandleScope reused_object_handle(isolate);
#define REUSABLE_ERROR_HANDLESCOPE(isolate)                                    \
  ReusableErrorHandleScope reused_error_handle(isolate);
#define REUSABLE_ARRAY_HANDLESCOPE(isolate)                                    \
  ReusableArrayHandleScope reused_array_handle(isolate);
#define REUSABLE_STRING_HANDLESCOPE(isolate)                                   \
  ReusableStringHandleScope reused_string_handle(isolate);
#define REUSABLE_INSTANCE_HANDLESCOPE(isolate)                                 \
  ReusableInstanceHandleScope reused_instance_handle(isolate);
#define REUSABLE_FUNCTION_HANDLESCOPE(isolate)                                 \
  ReusableFunctionHandleScope reused_function_handle(isolate);
#define REUSABLE_FIELD_HANDLESCOPE(isolate)                                    \
  ReusableFieldHandleScope reused_field_handle(isolate);
#define REUSABLE_CLASS_HANDLESCOPE(isolate)                                    \
  ReusableClassHandleScope reused_class_handle(isolate);
#define REUSABLE_ABSTRACT_TYPE_HANDLESCOPE(isolate)                            \
  ReusableAbstractTypeHandleScope reused_abstract_type(isolate);
#define REUSABLE_TYPE_PARAMETER_HANDLESCOPE(isolate)                           \
  ReusableTypeParameterHandleScope reused_type_parameter(isolate);
#define REUSABLE_TYPE_ARGUMENTS_HANDLESCOPE(isolate)                           \
  ReusableTypeArgumentsHandleScope reused_type_arguments_handle(isolate);

}  // namespace dart

#endif  // VM_REUSABLE_HANDLES_H_
