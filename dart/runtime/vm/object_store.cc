// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/object_store.h"

#include "vm/exceptions.h"
#include "vm/dart_entry.h"
#include "vm/isolate.h"
#include "vm/object.h"
#include "vm/raw_object.h"
#include "vm/symbols.h"
#include "vm/visitor.h"

namespace dart {

ObjectStore::ObjectStore()
  : object_class_(Class::null()),
    object_type_(Type::null()),
    null_class_(Class::null()),
    null_type_(Type::null()),
    function_type_(Type::null()),
    function_impl_type_(Type::null()),
    library_prefix_class_(Class::null()),
    type_class_(Class::null()),
    number_type_(Type::null()),
    int_type_(Type::null()),
    integer_implementation_class_(Class::null()),
    smi_class_(Class::null()),
    mint_class_(Class::null()),
    bigint_class_(Class::null()),
    double_class_(Class::null()),
    float32x4_type_(Type::null()),
    int32x4_type_(Type::null()),
    float64x2_type_(Type::null()),
    string_type_(Type::null()),
    one_byte_string_class_(Class::null()),
    two_byte_string_class_(Class::null()),
    external_one_byte_string_class_(Class::null()),
    external_two_byte_string_class_(Class::null()),
    bool_type_(Type::null()),
    bool_class_(Class::null()),
    array_class_(Class::null()),
    array_type_(Type::null()),
    immutable_array_class_(Class::null()),
    growable_object_array_class_(Class::null()),
    float32x4_class_(Class::null()),
    int32x4_class_(Class::null()),
    float64x2_class_(Class::null()),
    typed_data_classes_(Array::null()),
    error_class_(Class::null()),
    stacktrace_class_(Class::null()),
    jsregexp_class_(Class::null()),
    weak_property_class_(Class::null()),
    mirror_reference_class_(Class::null()),
    symbol_table_(Array::null()),
    canonical_type_arguments_(Array::null()),
    async_library_(Library::null()),
    builtin_library_(Library::null()),
    core_library_(Library::null()),
    isolate_library_(Library::null()),
    math_library_(Library::null()),
    mirrors_library_(Library::null()),
    native_wrappers_library_(Library::null()),
    root_library_(Library::null()),
    typed_data_library_(Library::null()),
    libraries_(GrowableObjectArray::null()),
    pending_classes_(GrowableObjectArray::null()),
    pending_functions_(GrowableObjectArray::null()),
    sticky_error_(Error::null()),
    unhandled_exception_handler_(String::null()),
    empty_context_(Context::null()),
    stack_overflow_(Instance::null()),
    out_of_memory_(Instance::null()),
    preallocated_stack_trace_(Stacktrace::null()),
    receive_port_create_function_(Function::null()),
    lookup_receive_port_function_(Function::null()),
    handle_message_function_(Function::null()) {
}


ObjectStore::~ObjectStore() {
}


void ObjectStore::VisitObjectPointers(ObjectPointerVisitor* visitor) {
  ASSERT(visitor != NULL);
  visitor->VisitPointers(from(), to());
}


void ObjectStore::Init(Isolate* isolate) {
  ASSERT(isolate->object_store() == NULL);
  ObjectStore* store = new ObjectStore();
  isolate->set_object_store(store);
}


bool ObjectStore::PreallocateObjects() {
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate != NULL && isolate->object_store() == this);
  if (this->stack_overflow() != Instance::null()) {
    ASSERT(this->out_of_memory() != Instance::null());
    ASSERT(this->preallocated_stack_trace() != Stacktrace::null());
    return true;
  }
  ASSERT(this->stack_overflow() == Instance::null());
  ASSERT(this->out_of_memory() == Instance::null());
  ASSERT(this->preallocated_stack_trace() == Stacktrace::null());

  ASSERT(this->pending_functions() == GrowableObjectArray::null());
  this->pending_functions_ = GrowableObjectArray::New();

  Object& result = Object::Handle();
  const Library& library = Library::Handle(Library::CoreLibrary());

  result = DartLibraryCalls::InstanceCreate(library,
                                            Symbols::StackOverflowError(),
                                            Symbols::Dot(),
                                            Object::empty_array());
  if (result.IsError()) {
    return false;
  }
  set_stack_overflow(Instance::Cast(result));

  result = DartLibraryCalls::InstanceCreate(library,
                                            Symbols::OutOfMemoryError(),
                                            Symbols::Dot(),
                                            Object::empty_array());
  if (result.IsError()) {
    return false;
  }
  set_out_of_memory(Instance::Cast(result));
  const Array& code_array = Array::Handle(
      isolate,
      Array::New(Stacktrace::kPreallocatedStackdepth, Heap::kOld));
  const Array& pc_offset_array = Array::Handle(
      isolate,
      Array::New(Stacktrace::kPreallocatedStackdepth, Heap::kOld));
  const Stacktrace& stack_trace =
      Stacktrace::Handle(Stacktrace::New(code_array, pc_offset_array));
  // Expansion of inlined functions requires additional memory at run time,
  // avoid it.
  stack_trace.set_expand_inlined(false);
  set_preallocated_stack_trace(stack_trace);

  return true;
}

}  // namespace dart
