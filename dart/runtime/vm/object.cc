// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/object.h"

#include "include/dart_api.h"
#include "platform/assert.h"
#include "vm/assembler.h"
#include "vm/cpu.h"
#include "vm/bigint_operations.h"
#include "vm/bootstrap.h"
#include "vm/class_finalizer.h"
#include "vm/code_generator.h"
#include "vm/code_observers.h"
#include "vm/code_patcher.h"
#include "vm/compiler.h"
#include "vm/compiler_stats.h"
#include "vm/dart.h"
#include "vm/dart_api_state.h"
#include "vm/dart_entry.h"
#include "vm/datastream.h"
#include "vm/debugger.h"
#include "vm/deopt_instructions.h"
#include "vm/disassembler.h"
#include "vm/double_conversion.h"
#include "vm/exceptions.h"
#include "vm/flow_graph_builder.h"
#include "vm/growable_array.h"
#include "vm/heap.h"
#include "vm/intermediate_language.h"
#include "vm/intrinsifier.h"
#include "vm/longjump.h"
#include "vm/object_id_ring.h"
#include "vm/object_store.h"
#include "vm/parser.h"
#include "vm/reusable_handles.h"
#include "vm/runtime_entry.h"
#include "vm/scopes.h"
#include "vm/stack_frame.h"
#include "vm/symbols.h"
#include "vm/timer.h"
#include "vm/unicode.h"

namespace dart {

DEFINE_FLAG(int, huge_method_cutoff_in_code_size, 200000,
    "Huge method cutoff in unoptimized code size (in bytes).");
DEFINE_FLAG(int, huge_method_cutoff_in_tokens, 20000,
    "Huge method cutoff in tokens: Disables optimizations for huge methods.");
DEFINE_FLAG(bool, overlap_type_arguments, true,
    "When possible, partially or fully overlap the type arguments of a type "
    "with the type arguments of its super type.");
DEFINE_FLAG(bool, show_internal_names, false,
    "Show names of internal classes (e.g. \"OneByteString\") in error messages "
    "instead of showing the corresponding interface names (e.g. \"String\")");
DEFINE_FLAG(bool, trace_disabling_optimized_code, false,
    "Trace disabling optimized code.");
DEFINE_FLAG(bool, throw_on_javascript_int_overflow, false,
    "Throw an exception when the result of an integer calculation will not "
    "fit into a javascript integer.");
DECLARE_FLAG(bool, eliminate_type_checks);
DECLARE_FLAG(bool, enable_type_checks);
DECLARE_FLAG(bool, error_on_bad_override);
DECLARE_FLAG(bool, error_on_bad_type);
DECLARE_FLAG(bool, trace_compiler);
DECLARE_FLAG(bool, trace_deoptimization);
DECLARE_FLAG(bool, trace_deoptimization_verbose);
DECLARE_FLAG(bool, verbose_stacktrace);
DECLARE_FLAG(charp, coverage_dir);

static const char* kGetterPrefix = "get:";
static const intptr_t kGetterPrefixLength = strlen(kGetterPrefix);
static const char* kSetterPrefix = "set:";
static const intptr_t kSetterPrefixLength = strlen(kSetterPrefix);

cpp_vtable Object::handle_vtable_ = 0;
cpp_vtable Object::builtin_vtables_[kNumPredefinedCids] = { 0 };
cpp_vtable Smi::handle_vtable_ = 0;

// These are initialized to a value that will force a illegal memory access if
// they are being used.
#if defined(RAW_NULL)
#error RAW_NULL should not be defined.
#endif
#define RAW_NULL kHeapObjectTag
Object* Object::null_object_ = NULL;
Array* Object::null_array_ = NULL;
String* Object::null_string_ = NULL;
Instance* Object::null_instance_ = NULL;
AbstractTypeArguments* Object::null_abstract_type_arguments_ = NULL;
Array* Object::empty_array_ = NULL;
Instance* Object::sentinel_ = NULL;
Instance* Object::transition_sentinel_ = NULL;
Instance* Object::unknown_constant_ = NULL;
Instance* Object::non_constant_ = NULL;
Bool* Object::bool_true_ = NULL;
Bool* Object::bool_false_ = NULL;
Smi* Object::smi_illegal_cid_ = NULL;
LanguageError* Object::snapshot_writer_error_ = NULL;
LanguageError* Object::branch_offset_error_ = NULL;

RawObject* Object::null_ = reinterpret_cast<RawObject*>(RAW_NULL);
RawClass* Object::class_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::dynamic_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::void_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawType* Object::dynamic_type_ = reinterpret_cast<RawType*>(RAW_NULL);
RawType* Object::void_type_ = reinterpret_cast<RawType*>(RAW_NULL);
RawClass* Object::unresolved_class_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::type_arguments_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::instantiated_type_arguments_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::patch_class_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::function_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::closure_data_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::redirection_data_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::field_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::literal_token_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::token_stream_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::script_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::library_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::library_prefix_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::namespace_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::code_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::instructions_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::pc_descriptors_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::stackmap_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::var_descriptors_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::exception_handlers_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::deopt_info_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::context_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::context_scope_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::icdata_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::megamorphic_cache_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::subtypetestcache_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::api_error_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::language_error_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::unhandled_exception_class_ =
    reinterpret_cast<RawClass*>(RAW_NULL);
RawClass* Object::unwind_error_class_ = reinterpret_cast<RawClass*>(RAW_NULL);
#undef RAW_NULL


const double MegamorphicCache::kLoadFactor = 0.75;


// The following functions are marked as invisible, meaning they will be hidden
// in the stack trace and will be hidden from reflective access.
// (Library, class name, method name)
#define INVISIBLE_LIST(V)                                                      \
  V(CoreLibrary, Object, _noSuchMethod)                                        \
  V(CoreLibrary, Object, _as)                                                  \
  V(CoreLibrary, Object, _instanceOf)                                          \
  V(CoreLibrary, _List, _List.)                                                \
  V(CoreLibrary, AssertionError, _throwNew)                                    \
  V(CoreLibrary, TypeError, _throwNew)                                         \
  V(CoreLibrary, FallThroughError, _throwNew)                                  \
  V(CoreLibrary, AbstractClassInstantiationError, _throwNew)                   \
  V(CoreLibrary, int, _throwFormatException)                                   \
  V(CoreLibrary, int, _parse)                                                  \
  V(CoreLibrary, StackTrace, _setupFullStackTrace)                             \
  V(CoreLibrary, _OneByteString, _setAt)                                       \
  V(TypedDataLibrary, _TypedList, _getInt8)                                    \
  V(TypedDataLibrary, _TypedList, _setInt8)                                    \
  V(TypedDataLibrary, _TypedList, _getUint8)                                   \
  V(TypedDataLibrary, _TypedList, _setUint8)                                   \
  V(TypedDataLibrary, _TypedList, _getInt16)                                   \
  V(TypedDataLibrary, _TypedList, _setInt16)                                   \
  V(TypedDataLibrary, _TypedList, _getUint16)                                  \
  V(TypedDataLibrary, _TypedList, _setUint16)                                  \
  V(TypedDataLibrary, _TypedList, _getInt32)                                   \
  V(TypedDataLibrary, _TypedList, _setInt32)                                   \
  V(TypedDataLibrary, _TypedList, _getUint32)                                  \
  V(TypedDataLibrary, _TypedList, _setUint32)                                  \
  V(TypedDataLibrary, _TypedList, _getInt64)                                   \
  V(TypedDataLibrary, _TypedList, _setInt64)                                   \
  V(TypedDataLibrary, _TypedList, _getUint64)                                  \
  V(TypedDataLibrary, _TypedList, _setUint64)                                  \
  V(TypedDataLibrary, _TypedList, _getFloat32)                                 \
  V(TypedDataLibrary, _TypedList, _setFloat32)                                 \
  V(TypedDataLibrary, _TypedList, _getFloat64)                                 \
  V(TypedDataLibrary, _TypedList, _setFloat64)                                 \
  V(TypedDataLibrary, _TypedList, _getFloat32x4)                               \
  V(TypedDataLibrary, _TypedList, _setFloat32x4)                               \



static void MarkFunctionAsInvisible(const Library& lib,
                                    const char* class_name,
                                    const char* function_name) {
  ASSERT(!lib.IsNull());
  const Class& cls = Class::Handle(
      lib.LookupClassAllowPrivate(String::Handle(String::New(class_name))));
  ASSERT(!cls.IsNull());
  const Function& function =
      Function::Handle(
          cls.LookupFunctionAllowPrivate(
              String::Handle(String::New(function_name))));
  ASSERT(!function.IsNull());
  function.set_is_visible(false);
}


static void MarkInvisibleFunctions() {
#define MARK_FUNCTION(lib, class_name, function_name)                          \
  MarkFunctionAsInvisible(Library::Handle(Library::lib()),                     \
      #class_name, #function_name);                                            \

INVISIBLE_LIST(MARK_FUNCTION)
#undef MARK_FUNCTION
}


// Takes a vm internal name and makes it suitable for external user.
//
// Examples:
//
// Internal getter and setter prefixes are changed:
//
//   get:foo -> foo
//   set:foo -> foo=
//
// Private name mangling is removed, possibly multiple times:
//
//   _ReceivePortImpl@6be832b -> _ReceivePortImpl
//   _ReceivePortImpl@6be832b._internal@6be832b -> _ReceivePortImpl._internal
//   _C@0x2b4ab9cc&_E@0x2b4ab9cc&_F@0x2b4ab9cc -> _C&_E&_F
//
// The trailing . on the default constructor name is dropped:
//
//   List. -> List
//
// And so forth:
//
//   get:foo@6be832b -> foo
//   _MyClass@6b3832b. -> _MyClass
//   _MyClass@6b3832b.named -> _MyClass.named
//
RawString* String::IdentifierPrettyName(const String& name) {
  if (name.Equals(Symbols::TopLevel())) {
    // Name of invisible top-level class.
    return Symbols::Empty().raw();
  }

  // First remove all private name mangling.
  String& unmangled_name = String::Handle(Symbols::Empty().raw());
  String& segment = String::Handle();
  intptr_t start_pos = 0;
  for (intptr_t i = 0; i < name.Length(); i++) {
    if (name.CharAt(i) == '@') {
      // Append the current segment to the unmangled name.
      segment = String::SubString(name, start_pos, (i - start_pos));
      unmangled_name = String::Concat(unmangled_name, segment);

      // Advance until past the name mangling.
      i++;  // Skip the '@'.
      while ((i < name.Length()) &&
             (name.CharAt(i) != '&') &&
             (name.CharAt(i) != '.')) {
        i++;
      }
      start_pos = i;
      i--;  // Account for for-loop increment.
    }
  }
  if (start_pos == 0) {
    // No name unmangling needed, reuse the name that was passed in.
    unmangled_name = name.raw();
  } else if (name.Length() != start_pos) {
    // Append the last segment.
    segment = String::SubString(name, start_pos, (name.Length() - start_pos));
    unmangled_name = String::Concat(unmangled_name, segment);
  }

  intptr_t len = unmangled_name.Length();
  intptr_t start = 0;
  intptr_t dot_pos = -1;  // Position of '.' in the name, if any.
  bool is_setter = false;

  for (intptr_t i = start; i < len; i++) {
    if (unmangled_name.CharAt(i) == ':') {
      ASSERT(start == 0);  // Only one : is possible in getters or setters.
      if (unmangled_name.CharAt(0) == 's') {
        is_setter = true;
      }
      start = i + 1;
    } else if (unmangled_name.CharAt(i) == '.') {
      ASSERT(dot_pos == -1);  // Only one dot is supported.
      dot_pos = i;
    }
  }

  if ((start == 0) && (dot_pos == -1)) {
    // This unmangled_name is fine as it is.
    return unmangled_name.raw();
  }

  // Drop the trailing dot if needed.
  intptr_t end = ((dot_pos + 1) == len) ? dot_pos : len;

  const String& result =
      String::Handle(String::SubString(unmangled_name, start, (end - start)));

  if (is_setter) {
    // Setters need to end with '='.
    return String::Concat(result, Symbols::Equals());
  }

  return result.raw();
}


RawString* String::IdentifierPrettyNameRetainPrivate(const String& name) {
  intptr_t len = name.Length();
  intptr_t start = 0;
  intptr_t at_pos = -1;  // Position of '@' in the name, if any.
  bool is_setter = false;

  for (intptr_t i = start; i < len; i++) {
    if (name.CharAt(i) == ':') {
      ASSERT(start == 0);  // Only one : is possible in getters or setters.
      if (name.CharAt(0) == 's') {
        is_setter = true;
      }
      start = i + 1;
    } else if (name.CharAt(i) == '@') {
      // Setters should have only one @ so we know where to put the =.
      ASSERT(!is_setter || (at_pos == -1));
      at_pos = i;
    }
  }

  if (start == 0) {
    // This unmangled_name is fine as it is.
    return name.raw();
  }

  String& result =
      String::Handle(String::SubString(name, start, (len - start)));

  if (is_setter) {
    // Setters need to end with '='.
    if (at_pos == -1) {
      return String::Concat(result, Symbols::Equals());
    } else {
      const String& pre_at =
          String::Handle(String::SubString(result, 0, at_pos - 4));
      const String& post_at =
          String::Handle(String::SubString(name, at_pos, len - at_pos));
      result = String::Concat(pre_at, Symbols::Equals());
      result = String::Concat(result, post_at);
    }
  }

  return result.raw();
}


template<typename type>
static bool IsSpecialCharacter(type value) {
  return ((value == '"') ||
          (value == '\n') ||
          (value == '\f') ||
          (value == '\b') ||
          (value == '\t') ||
          (value == '\v') ||
          (value == '\r') ||
          (value == '\\') ||
          (value == '$'));
}


template<typename type>
static type SpecialCharacter(type value) {
  if (value == '"') {
    return '"';
  } else if (value == '\n') {
    return 'n';
  } else if (value == '\f') {
    return 'f';
  } else if (value == '\b') {
    return 'b';
  } else if (value == '\t') {
    return 't';
  } else if (value == '\v') {
    return 'v';
  } else if (value == '\r') {
    return 'r';
  } else if (value == '\\') {
    return '\\';
  } else if (value == '$') {
    return '$';
  }
  UNREACHABLE();
  return '\0';
}


static void DeleteWeakPersistentHandle(Dart_WeakPersistentHandle handle) {
  ApiState* state = Isolate::Current()->api_state();
  ASSERT(state != NULL);
  FinalizablePersistentHandle* weak_ref =
      reinterpret_cast<FinalizablePersistentHandle*>(handle);
  ASSERT(state->IsValidWeakPersistentHandle(handle));
  state->weak_persistent_handles().FreeHandle(weak_ref);
}


void Object::InitOnce() {
  // TODO(iposva): NoGCScope needs to be added here.
  ASSERT(class_class() == null_);
  // Initialize the static vtable values.
  {
    Object fake_object;
    Smi fake_smi;
    Object::handle_vtable_ = fake_object.vtable();
    Smi::handle_vtable_ = fake_smi.vtable();
  }

  Isolate* isolate = Isolate::Current();
  Heap* heap = isolate->heap();

  // Allocate the read only object handles here.
  null_object_ = Object::ReadOnlyHandle();
  null_array_ = Array::ReadOnlyHandle();
  null_string_ = String::ReadOnlyHandle();
  null_instance_ = Instance::ReadOnlyHandle();
  null_abstract_type_arguments_ = AbstractTypeArguments::ReadOnlyHandle();
  empty_array_ = Array::ReadOnlyHandle();
  sentinel_ = Instance::ReadOnlyHandle();
  transition_sentinel_ = Instance::ReadOnlyHandle();
  unknown_constant_ =  Instance::ReadOnlyHandle();
  non_constant_ =  Instance::ReadOnlyHandle();
  bool_true_ = Bool::ReadOnlyHandle();
  bool_false_ = Bool::ReadOnlyHandle();
  smi_illegal_cid_ = Smi::ReadOnlyHandle();
  snapshot_writer_error_ = LanguageError::ReadOnlyHandle();
  branch_offset_error_ = LanguageError::ReadOnlyHandle();


  // Allocate and initialize the null instance.
  // 'null_' must be the first object allocated as it is used in allocation to
  // clear the object.
  {
    uword address = heap->Allocate(Instance::InstanceSize(), Heap::kOld);
    null_ = reinterpret_cast<RawInstance*>(address + kHeapObjectTag);
    // The call below is using 'null_' to initialize itself.
    InitializeObject(address, kNullCid, Instance::InstanceSize());
  }

  *null_object_ = Object::null();
  *null_array_ = Array::null();
  *null_string_ = String::null();
  *null_instance_ = Instance::null();
  *null_abstract_type_arguments_ = AbstractTypeArguments::null();

  // Initialize the empty array handle to null_ in order to be able to check
  // if the empty array was allocated (RAW_NULL is not available).
  *empty_array_ = Array::null();

  Class& cls = Class::Handle();

  // Allocate and initialize the class class.
  {
    intptr_t size = Class::InstanceSize();
    uword address = heap->Allocate(size, Heap::kOld);
    class_class_ = reinterpret_cast<RawClass*>(address + kHeapObjectTag);
    InitializeObject(address, Class::kClassId, size);

    Class fake;
    // Initialization from Class::New<Class>.
    // Directly set raw_ to break a circular dependency: SetRaw will attempt
    // to lookup class class in the class table where it is not registered yet.
    cls.raw_ = class_class_;
    cls.set_handle_vtable(fake.vtable());
    cls.set_instance_size(Class::InstanceSize());
    cls.set_next_field_offset(Class::NextFieldOffset());
    cls.set_id(Class::kClassId);
    cls.set_state_bits(0);
    cls.set_is_finalized();
    cls.set_is_type_finalized();
    cls.set_type_arguments_field_offset_in_words(Class::kNoTypeArguments);
    cls.set_num_type_arguments(0);
    cls.set_num_own_type_arguments(0);
    cls.set_num_native_fields(0);
    cls.InitEmptyFields();
    isolate->RegisterClass(cls);
  }

  // Allocate and initialize the null class.
  cls = Class::New<Instance>(kNullCid);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  isolate->object_store()->set_null_class(cls);

  // Allocate and initialize the free list element class.
  cls = Class::New<FreeListElement::FakeInstance>(kFreeListElement);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_finalized();
  cls.set_is_type_finalized();

  // Allocate and initialize the sentinel values of Null class.
  {
    *sentinel_ ^=
        Object::Allocate(kNullCid, Instance::InstanceSize(), Heap::kOld);

    *transition_sentinel_ ^=
        Object::Allocate(kNullCid, Instance::InstanceSize(), Heap::kOld);
  }

  // Allocate and initialize optimizing compiler constants.
  {
    *unknown_constant_ ^=
        Object::Allocate(kNullCid, Instance::InstanceSize(), Heap::kOld);
    *non_constant_ ^=
        Object::Allocate(kNullCid, Instance::InstanceSize(), Heap::kOld);
  }

  // Allocate the remaining VM internal classes.
  cls = Class::New<UnresolvedClass>();
  unresolved_class_class_ = cls.raw();

  cls = Class::New<TypeArguments>();
  type_arguments_class_ = cls.raw();

  cls = Class::New<InstantiatedTypeArguments>();
  instantiated_type_arguments_class_ = cls.raw();

  cls = Class::New<PatchClass>();
  patch_class_class_ = cls.raw();

  cls = Class::New<Function>();
  function_class_ = cls.raw();

  cls = Class::New<ClosureData>();
  closure_data_class_ = cls.raw();

  cls = Class::New<RedirectionData>();
  redirection_data_class_ = cls.raw();

  cls = Class::New<Field>();
  field_class_ = cls.raw();

  cls = Class::New<LiteralToken>();
  literal_token_class_ = cls.raw();

  cls = Class::New<TokenStream>();
  token_stream_class_ = cls.raw();

  cls = Class::New<Script>();
  script_class_ = cls.raw();

  cls = Class::New<Library>();
  library_class_ = cls.raw();

  cls = Class::New<LibraryPrefix>();
  library_prefix_class_ = cls.raw();

  cls = Class::New<Namespace>();
  namespace_class_ = cls.raw();

  cls = Class::New<Code>();
  code_class_ = cls.raw();

  cls = Class::New<Instructions>();
  instructions_class_ = cls.raw();

  cls = Class::New<PcDescriptors>();
  pc_descriptors_class_ = cls.raw();

  cls = Class::New<Stackmap>();
  stackmap_class_ = cls.raw();

  cls = Class::New<LocalVarDescriptors>();
  var_descriptors_class_ = cls.raw();

  cls = Class::New<ExceptionHandlers>();
  exception_handlers_class_ = cls.raw();

  cls = Class::New<DeoptInfo>();
  deopt_info_class_ = cls.raw();

  cls = Class::New<Context>();
  context_class_ = cls.raw();

  cls = Class::New<ContextScope>();
  context_scope_class_ = cls.raw();

  cls = Class::New<ICData>();
  icdata_class_ = cls.raw();

  cls = Class::New<MegamorphicCache>();
  megamorphic_cache_class_ = cls.raw();

  cls = Class::New<SubtypeTestCache>();
  subtypetestcache_class_ = cls.raw();

  cls = Class::New<ApiError>();
  api_error_class_ = cls.raw();

  cls = Class::New<LanguageError>();
  language_error_class_ = cls.raw();

  cls = Class::New<UnhandledException>();
  unhandled_exception_class_ = cls.raw();

  cls = Class::New<UnwindError>();
  unwind_error_class_ = cls.raw();

  ASSERT(class_class() != null_);

  // Pre-allocate classes in the vm isolate so that we can for example create a
  // symbol table and populate it with some frequently used strings as symbols.
  cls = Class::New<Array>();
  isolate->object_store()->set_array_class(cls);
  cls.set_type_arguments_field_offset(Array::type_arguments_offset());
  cls.set_num_type_arguments(1);
  cls.set_num_own_type_arguments(1);
  cls = Class::New<Array>(kImmutableArrayCid);
  isolate->object_store()->set_immutable_array_class(cls);
  cls.set_type_arguments_field_offset(Array::type_arguments_offset());
  cls.set_num_type_arguments(1);
  cls.set_num_own_type_arguments(1);
  cls = Class::NewStringClass(kOneByteStringCid);
  isolate->object_store()->set_one_byte_string_class(cls);
  cls = Class::NewStringClass(kTwoByteStringCid);
  isolate->object_store()->set_two_byte_string_class(cls);

  // Allocate and initialize the empty_array instance.
  {
    uword address = heap->Allocate(Array::InstanceSize(0), Heap::kOld);
    InitializeObject(address, kArrayCid, Array::InstanceSize(0));
    Array::initializeHandle(
        empty_array_,
        reinterpret_cast<RawArray*>(address + kHeapObjectTag));
    empty_array_->raw()->ptr()->length_ = Smi::New(0);
  }

  cls = Class::New<Instance>(kDynamicCid);
  cls.set_is_abstract();
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_type_finalized();
  cls.set_is_finalized();
  dynamic_class_ = cls.raw();

  cls = Class::New<Instance>(kVoidCid);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_type_finalized();
  cls.set_is_finalized();
  void_class_ = cls.raw();

  cls = Class::New<Type>();
  cls.set_is_type_finalized();
  cls.set_is_finalized();
  isolate->object_store()->set_type_class(cls);

  cls = dynamic_class_;
  dynamic_type_ = Type::NewNonParameterizedType(cls);

  cls = void_class_;
  void_type_ = Type::NewNonParameterizedType(cls);

  // Allocate and initialize singleton true and false boolean objects.
  cls = Class::New<Bool>();
  isolate->object_store()->set_bool_class(cls);
  *bool_true_ = Bool::New(true);
  *bool_false_ = Bool::New(false);

  *smi_illegal_cid_ = Smi::New(kIllegalCid);

  String& error_str = String::Handle();
  error_str = String::New("SnapshotWriter Error");
  *snapshot_writer_error_ = LanguageError::New(error_str);
  error_str = String::New("Branch offset overflow");
  *branch_offset_error_ = LanguageError::New(error_str);

  ASSERT(!null_object_->IsSmi());
  ASSERT(!null_array_->IsSmi());
  ASSERT(null_array_->IsArray());
  ASSERT(!null_string_->IsSmi());
  ASSERT(null_string_->IsString());
  ASSERT(!null_instance_->IsSmi());
  ASSERT(null_instance_->IsInstance());
  ASSERT(!null_abstract_type_arguments_->IsSmi());
  ASSERT(null_abstract_type_arguments_->IsAbstractTypeArguments());
  ASSERT(!empty_array_->IsSmi());
  ASSERT(empty_array_->IsArray());
  ASSERT(!sentinel_->IsSmi());
  ASSERT(sentinel_->IsInstance());
  ASSERT(!transition_sentinel_->IsSmi());
  ASSERT(transition_sentinel_->IsInstance());
  ASSERT(!unknown_constant_->IsSmi());
  ASSERT(unknown_constant_->IsInstance());
  ASSERT(!non_constant_->IsSmi());
  ASSERT(non_constant_->IsInstance());
  ASSERT(!bool_true_->IsSmi());
  ASSERT(bool_true_->IsBool());
  ASSERT(!bool_false_->IsSmi());
  ASSERT(bool_false_->IsBool());
  ASSERT(smi_illegal_cid_->IsSmi());
  ASSERT(!snapshot_writer_error_->IsSmi());
  ASSERT(snapshot_writer_error_->IsLanguageError());
  ASSERT(!branch_offset_error_->IsSmi());
  ASSERT(branch_offset_error_->IsLanguageError());
}


#define SET_CLASS_NAME(class_name, name)                                       \
  cls = class_name##_class();                                                  \
  cls.set_name(Symbols::name());                                               \

void Object::RegisterSingletonClassNames() {
  Class& cls = Class::Handle();

  // Set up names for all VM singleton classes.
  SET_CLASS_NAME(class, Class);
  SET_CLASS_NAME(dynamic, Dynamic);
  SET_CLASS_NAME(void, Void);
  SET_CLASS_NAME(unresolved_class, UnresolvedClass);
  SET_CLASS_NAME(type_arguments, TypeArguments);
  SET_CLASS_NAME(instantiated_type_arguments, InstantiatedTypeArguments);
  SET_CLASS_NAME(patch_class, PatchClass);
  SET_CLASS_NAME(function, Function);
  SET_CLASS_NAME(closure_data, ClosureData);
  SET_CLASS_NAME(redirection_data, RedirectionData);
  SET_CLASS_NAME(field, Field);
  SET_CLASS_NAME(literal_token, LiteralToken);
  SET_CLASS_NAME(token_stream, TokenStream);
  SET_CLASS_NAME(script, Script);
  SET_CLASS_NAME(library, LibraryClass);
  SET_CLASS_NAME(library_prefix, LibraryPrefix);
  SET_CLASS_NAME(namespace, Namespace);
  SET_CLASS_NAME(code, Code);
  SET_CLASS_NAME(instructions, Instructions);
  SET_CLASS_NAME(pc_descriptors, PcDescriptors);
  SET_CLASS_NAME(stackmap, Stackmap);
  SET_CLASS_NAME(var_descriptors, LocalVarDescriptors);
  SET_CLASS_NAME(exception_handlers, ExceptionHandlers);
  SET_CLASS_NAME(deopt_info, DeoptInfo);
  SET_CLASS_NAME(context, Context);
  SET_CLASS_NAME(context_scope, ContextScope);
  SET_CLASS_NAME(icdata, ICData);
  SET_CLASS_NAME(megamorphic_cache, MegamorphicCache);
  SET_CLASS_NAME(subtypetestcache, SubtypeTestCache);
  SET_CLASS_NAME(api_error, ApiError);
  SET_CLASS_NAME(language_error, LanguageError);
  SET_CLASS_NAME(unhandled_exception, UnhandledException);
  SET_CLASS_NAME(unwind_error, UnwindError);

  // Set up names for object array and one byte string class which are
  // pre-allocated in the vm isolate also.
  cls = Dart::vm_isolate()->object_store()->array_class();
  cls.set_name(Symbols::_List());
  cls = Dart::vm_isolate()->object_store()->one_byte_string_class();
  cls.set_name(Symbols::OneByteString());
}


void Object::CreateInternalMetaData() {
  // Initialize meta data for VM internal classes.
  Class& cls = Class::Handle();
  Array& fields = Array::Handle();
  Field& fld = Field::Handle();
  String& name = String::Handle();

  // TODO(iposva): Add more of the VM classes here.
  cls = context_class_;
  fields = Array::New(1);
  name = Symbols::New("@parent_");
  fld = Field::New(name, false, false, false, cls, 0);
  fields.SetAt(0, fld);
  cls.SetFields(fields);
}


// Make unused space in an object whose type has been transformed safe
// for traversing during GC.
// The unused part of the transformed object is marked as an TypedDataInt8Array
// object.
void Object::MakeUnusedSpaceTraversable(const Object& obj,
                                        intptr_t original_size,
                                        intptr_t used_size) {
  ASSERT(Isolate::Current()->no_gc_scope_depth() > 0);
  ASSERT(!obj.IsNull());
  ASSERT(original_size >= used_size);
  if (original_size > used_size) {
    intptr_t leftover_size = original_size - used_size;

    uword addr = RawObject::ToAddr(obj.raw()) + used_size;
    if (leftover_size >= TypedData::InstanceSize(0)) {
      // Update the leftover space as an TypedDataInt8Array object.
      RawTypedData* raw =
          reinterpret_cast<RawTypedData*>(RawObject::FromAddr(addr));
      uword tags = 0;
      tags = RawObject::SizeTag::update(leftover_size, tags);
      tags = RawObject::ClassIdTag::update(kTypedDataInt8ArrayCid, tags);
      raw->ptr()->tags_ = tags;
      intptr_t leftover_len = (leftover_size - TypedData::InstanceSize(0));
      ASSERT(TypedData::InstanceSize(leftover_len) == leftover_size);
      raw->ptr()->length_ = Smi::New(leftover_len);
    } else {
      // Update the leftover space as a basic object.
      ASSERT(leftover_size == Object::InstanceSize());
      RawObject* raw = reinterpret_cast<RawObject*>(RawObject::FromAddr(addr));
      uword tags = 0;
      tags = RawObject::SizeTag::update(leftover_size, tags);
      tags = RawObject::ClassIdTag::update(kInstanceCid, tags);
      raw->ptr()->tags_ = tags;
    }
  }
}


void Object::VerifyBuiltinVtables() {
#if defined(DEBUG)
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate != NULL);
  Class& cls = Class::Handle(isolate, Class::null());
  for (intptr_t cid = (kIllegalCid + 1); cid < kNumPredefinedCids; cid++) {
    if (isolate->class_table()->HasValidClassAt(cid)) {
      cls ^= isolate->class_table()->At(cid);
      ASSERT(builtin_vtables_[cid] == cls.raw_ptr()->handle_vtable_);
    }
  }
#endif
}


void Object::RegisterClass(const Class& cls,
                           const String& name,
                           const Library& lib) {
  ASSERT(name.Length() > 0);
  ASSERT(name.CharAt(0) != '_');
  cls.set_name(name);
  lib.AddClass(cls);
}


void Object::RegisterPrivateClass(const Class& cls,
                                  const String& public_class_name,
                                  const Library& lib) {
  ASSERT(public_class_name.Length() > 0);
  ASSERT(public_class_name.CharAt(0) == '_');
  String& str = String::Handle();
  str = lib.PrivateName(public_class_name);
  cls.set_name(str);
  lib.AddClass(cls);
}


RawError* Object::Init(Isolate* isolate) {
  TIMERSCOPE(time_bootstrap);
  ObjectStore* object_store = isolate->object_store();

  Class& cls = Class::Handle();
  Type& type = Type::Handle();
  Array& array = Array::Handle();
  Library& lib = Library::Handle();

  // All RawArray fields will be initialized to an empty array, therefore
  // initialize array class first.
  cls = Class::New<Array>();
  object_store->set_array_class(cls);

  // Array and ImmutableArray are the only VM classes that are parameterized.
  // Since they are pre-finalized, CalculateFieldOffsets() is not called, so we
  // need to set the offset of their type_arguments_ field, which is explicitly
  // declared in RawArray.
  cls.set_type_arguments_field_offset(Array::type_arguments_offset());
  cls.set_num_type_arguments(1);
  cls.set_num_own_type_arguments(1);

  // Set up the growable object array class (Has to be done after the array
  // class is setup as one of its field is an array object).
  cls = Class::New<GrowableObjectArray>();
  object_store->set_growable_object_array_class(cls);
  cls.set_type_arguments_field_offset(
      GrowableObjectArray::type_arguments_offset());
  cls.set_num_type_arguments(1);
  cls.set_num_own_type_arguments(1);

  // canonical_type_arguments_ are Smi terminated.
  // Last element contains the count of used slots.
  const intptr_t kInitialCanonicalTypeArgumentsSize = 4;
  array = Array::New(kInitialCanonicalTypeArgumentsSize + 1);
  array.SetAt(kInitialCanonicalTypeArgumentsSize, Smi::Handle(Smi::New(0)));
  object_store->set_canonical_type_arguments(array);

  // Setup type class early in the process.
  cls = Class::New<Type>();
  object_store->set_type_class(cls);

  cls = Class::New<TypeParameter>();
  object_store->set_type_parameter_class(cls);

  cls = Class::New<BoundedType>();
  object_store->set_bounded_type_class(cls);

  cls = Class::New<MixinAppType>();
  object_store->set_mixin_app_type_class(cls);

  // Pre-allocate the OneByteString class needed by the symbol table.
  cls = Class::NewStringClass(kOneByteStringCid);
  object_store->set_one_byte_string_class(cls);

  // Pre-allocate the TwoByteString class needed by the symbol table.
  cls = Class::NewStringClass(kTwoByteStringCid);
  object_store->set_two_byte_string_class(cls);

  // Setup the symbol table for the symbols created in the isolate.
  Symbols::SetupSymbolTable(isolate);

  // Set up the libraries array before initializing the core library.
  const GrowableObjectArray& libraries =
      GrowableObjectArray::Handle(GrowableObjectArray::New(Heap::kOld));
  object_store->set_libraries(libraries);

  // Pre-register the core library.
  Library::InitCoreLibrary(isolate);

  // Basic infrastructure has been setup, initialize the class dictionary.
  Library& core_lib = Library::Handle(Library::CoreLibrary());
  ASSERT(!core_lib.IsNull());

  const GrowableObjectArray& pending_classes =
      GrowableObjectArray::Handle(GrowableObjectArray::New());
  object_store->set_pending_classes(pending_classes);

  Context& context = Context::Handle(Context::New(0, Heap::kOld));
  object_store->set_empty_context(context);

  // Now that the symbol table is initialized and that the core dictionary as
  // well as the core implementation dictionary have been setup, preallocate
  // remaining classes and register them by name in the dictionaries.
  String& name = String::Handle();
  cls = object_store->array_class();  // Was allocated above.
  RegisterPrivateClass(cls, Symbols::_List(), core_lib);
  pending_classes.Add(cls);
  // We cannot use NewNonParameterizedType(cls), because Array is parameterized.
  type ^= Type::New(Object::Handle(cls.raw()),
                    TypeArguments::Handle(),
                    Scanner::kDummyTokenIndex);
  type.SetIsFinalized();
  type ^= type.Canonicalize();
  object_store->set_array_type(type);

  cls = object_store->growable_object_array_class();  // Was allocated above.
  RegisterPrivateClass(cls, Symbols::_GrowableList(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Array>(kImmutableArrayCid);
  object_store->set_immutable_array_class(cls);
  cls.set_type_arguments_field_offset(Array::type_arguments_offset());
  cls.set_num_type_arguments(1);
  cls.set_num_own_type_arguments(1);
  ASSERT(object_store->immutable_array_class() != object_store->array_class());
  cls.set_is_prefinalized();
  RegisterPrivateClass(cls, Symbols::_ImmutableList(), core_lib);
  pending_classes.Add(cls);

  cls = object_store->one_byte_string_class();  // Was allocated above.
  RegisterPrivateClass(cls, Symbols::OneByteString(), core_lib);
  pending_classes.Add(cls);

  cls = object_store->two_byte_string_class();  // Was allocated above.
  RegisterPrivateClass(cls, Symbols::TwoByteString(), core_lib);
  pending_classes.Add(cls);

  cls = Class::NewStringClass(kExternalOneByteStringCid);
  object_store->set_external_one_byte_string_class(cls);
  RegisterPrivateClass(cls, Symbols::ExternalOneByteString(), core_lib);
  pending_classes.Add(cls);

  cls = Class::NewStringClass(kExternalTwoByteStringCid);
  object_store->set_external_two_byte_string_class(cls);
  RegisterPrivateClass(cls, Symbols::ExternalTwoByteString(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Stacktrace>();
  object_store->set_stacktrace_class(cls);
  RegisterClass(cls, Symbols::StackTrace(), core_lib);
  pending_classes.Add(cls);
  // Super type set below, after Object is allocated.

  cls = Class::New<JSRegExp>();
  object_store->set_jsregexp_class(cls);
  RegisterPrivateClass(cls, Symbols::JSSyntaxRegExp(), core_lib);
  pending_classes.Add(cls);

  // Initialize the base interfaces used by the core VM classes.

  // Allocate and initialize the pre-allocated classes in the core library.
  // The script and token index of these pre-allocated classes is set up in
  // the parser when the corelib script is compiled (see
  // Parser::ParseClassDefinition).
  cls = Class::New<Instance>(kInstanceCid);
  object_store->set_object_class(cls);
  cls.set_name(Symbols::Object());
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  core_lib.AddClass(cls);
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_object_type(type);

  cls = Class::New<Bool>();
  object_store->set_bool_class(cls);
  RegisterClass(cls, Symbols::Bool(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Instance>(kNullCid);
  object_store->set_null_class(cls);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  RegisterClass(cls, Symbols::Null(), core_lib);
  pending_classes.Add(cls);

  cls = object_store->type_class();
  RegisterPrivateClass(cls, Symbols::Type(), core_lib);
  pending_classes.Add(cls);

  cls = object_store->type_parameter_class();
  RegisterPrivateClass(cls, Symbols::TypeParameter(), core_lib);
  pending_classes.Add(cls);

  cls = object_store->bounded_type_class();
  RegisterPrivateClass(cls, Symbols::BoundedType(), core_lib);
  pending_classes.Add(cls);

  cls = object_store->mixin_app_type_class();
  RegisterPrivateClass(cls, Symbols::MixinAppType(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Integer>();
  object_store->set_integer_implementation_class(cls);
  RegisterPrivateClass(cls, Symbols::IntegerImplementation(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Smi>();
  object_store->set_smi_class(cls);
  RegisterPrivateClass(cls, Symbols::_Smi(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Mint>();
  object_store->set_mint_class(cls);
  RegisterPrivateClass(cls, Symbols::_Mint(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Bigint>();
  object_store->set_bigint_class(cls);
  RegisterPrivateClass(cls, Symbols::_Bigint(), core_lib);
  pending_classes.Add(cls);

  cls = Class::New<Double>();
  object_store->set_double_class(cls);
  RegisterPrivateClass(cls, Symbols::_Double(), core_lib);
  pending_classes.Add(cls);

  // Abstract super class for all signature classes.
  cls = Class::New<Instance>(kIllegalCid);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  RegisterPrivateClass(cls, Symbols::FunctionImpl(), core_lib);
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_function_impl_type(type);

  cls = Class::New<WeakProperty>();
  object_store->set_weak_property_class(cls);
  RegisterPrivateClass(cls, Symbols::_WeakProperty(), core_lib);

  // Pre-register the mirrors library so we can place the vm class
  // MirrorReference there rather than the core library.
  lib = Library::LookupLibrary(Symbols::DartMirrors());
  if (lib.IsNull()) {
    lib = Library::NewLibraryHelper(Symbols::DartMirrors(), true);
    lib.Register();
    isolate->object_store()->set_bootstrap_library(ObjectStore::kMirrors,
                                                   lib);
  }
  ASSERT(!lib.IsNull());
  ASSERT(lib.raw() == Library::MirrorsLibrary());

  cls = Class::New<MirrorReference>();
  object_store->set_mirror_reference_class(cls);
  RegisterPrivateClass(cls, Symbols::_MirrorReference(), lib);

  // Setup some default native field classes which can be extended for
  // specifying native fields in dart classes.
  Library::InitNativeWrappersLibrary(isolate);
  ASSERT(isolate->object_store()->native_wrappers_library() != Library::null());

  // Pre-register the typed_data library so the native class implementations
  // can be hooked up before compiling it.
  lib = Library::LookupLibrary(Symbols::DartTypedData());
  if (lib.IsNull()) {
    lib = Library::NewLibraryHelper(Symbols::DartTypedData(), true);
    lib.Register();
    isolate->object_store()->set_bootstrap_library(ObjectStore::kTypedData,
                                                   lib);
  }
  ASSERT(!lib.IsNull());
  ASSERT(lib.raw() == Library::TypedDataLibrary());
  const intptr_t typed_data_class_array_length =
      RawObject::NumberOfTypedDataClasses();
  Array& typed_data_classes =
      Array::Handle(Array::New(typed_data_class_array_length));
  int index = 0;
#define REGISTER_TYPED_DATA_CLASS(clazz)                                       \
  cls = Class::NewTypedDataClass(kTypedData##clazz##Cid);                      \
  index = kTypedData##clazz##Cid - kTypedDataInt8ArrayCid;                     \
  typed_data_classes.SetAt(index, cls);                                        \
  RegisterPrivateClass(cls, Symbols::_##clazz(), lib);                         \

  CLASS_LIST_TYPED_DATA(REGISTER_TYPED_DATA_CLASS);
#undef REGISTER_TYPED_DATA_CLASS
#define REGISTER_TYPED_DATA_VIEW_CLASS(clazz)                                  \
  cls = Class::NewTypedDataViewClass(kTypedData##clazz##ViewCid);              \
  index = kTypedData##clazz##ViewCid - kTypedDataInt8ArrayCid;                 \
  typed_data_classes.SetAt(index, cls);                                        \
  RegisterPrivateClass(cls, Symbols::_##clazz##View(), lib);                   \
  pending_classes.Add(cls);                                                    \

  CLASS_LIST_TYPED_DATA(REGISTER_TYPED_DATA_VIEW_CLASS);
  cls = Class::NewTypedDataViewClass(kByteDataViewCid);
  index = kByteDataViewCid - kTypedDataInt8ArrayCid;
  typed_data_classes.SetAt(index, cls);
  RegisterPrivateClass(cls, Symbols::_ByteDataView(), lib);
  pending_classes.Add(cls);
#undef REGISTER_TYPED_DATA_VIEW_CLASS
#define REGISTER_EXT_TYPED_DATA_CLASS(clazz)                                   \
  cls = Class::NewExternalTypedDataClass(kExternalTypedData##clazz##Cid);      \
  index = kExternalTypedData##clazz##Cid - kTypedDataInt8ArrayCid;             \
  typed_data_classes.SetAt(index, cls);                                        \
  RegisterPrivateClass(cls, Symbols::_External##clazz(), lib);                 \

  CLASS_LIST_TYPED_DATA(REGISTER_EXT_TYPED_DATA_CLASS);
#undef REGISTER_EXT_TYPED_DATA_CLASS
  // Register Float32x4 and Int32x4 in the object store.
  cls = Class::New<Float32x4>();
  object_store->set_float32x4_class(cls);
  RegisterPrivateClass(cls, Symbols::_Float32x4(), lib);
  cls = Class::New<Int32x4>();
  object_store->set_int32x4_class(cls);
  RegisterPrivateClass(cls, Symbols::_Int32x4(), lib);

  cls = Class::New<Instance>(kIllegalCid);
  RegisterClass(cls, Symbols::Float32x4(), lib);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_float32x4_type(type);

  cls = Class::New<Instance>(kIllegalCid);
  RegisterClass(cls, Symbols::Int32x4(), lib);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_int32x4_type(type);

  object_store->set_typed_data_classes(typed_data_classes);

  // Set the super type of class Stacktrace to Object type so that the
  // 'toString' method is implemented.
  cls = object_store->stacktrace_class();
  type = object_store->object_type();
  cls.set_super_type(type);

  // Abstract class that represents the Dart class Function.
  cls = Class::New<Instance>(kIllegalCid);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  RegisterClass(cls, Symbols::Function(), core_lib);
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_function_type(type);

  cls = Class::New<Number>();
  RegisterClass(cls, Symbols::Number(), core_lib);
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_number_type(type);

  cls = Class::New<Instance>(kIllegalCid);
  RegisterClass(cls, Symbols::Int(), core_lib);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_int_type(type);

  cls = Class::New<Instance>(kIllegalCid);
  RegisterClass(cls, Symbols::Double(), core_lib);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_double_type(type);

  name = Symbols::New("String");
  cls = Class::New<Instance>(kIllegalCid);
  RegisterClass(cls, name, core_lib);
  cls.set_num_type_arguments(0);
  cls.set_num_own_type_arguments(0);
  cls.set_is_prefinalized();
  pending_classes.Add(cls);
  type = Type::NewNonParameterizedType(cls);
  object_store->set_string_type(type);

  cls = object_store->bool_class();
  type = Type::NewNonParameterizedType(cls);
  object_store->set_bool_type(type);

  cls = object_store->smi_class();
  type = Type::NewNonParameterizedType(cls);
  object_store->set_smi_type(type);

  cls = object_store->mint_class();
  type = Type::NewNonParameterizedType(cls);
  object_store->set_mint_type(type);

  // The classes 'void' and 'dynamic' are phoney classes to make type checking
  // more regular; they live in the VM isolate. The class 'void' is not
  // registered in the class dictionary because its name is a reserved word.
  // The class 'dynamic' is registered in the class dictionary because its name
  // is a built-in identifier (this is wrong).
  // The corresponding types are stored in the object store.
  cls = object_store->null_class();
  type = Type::NewNonParameterizedType(cls);
  object_store->set_null_type(type);

  // Consider removing when/if Null becomes an ordinary class.
  type = object_store->object_type();
  cls.set_super_type(type);

  // Finish the initialization by compiling the bootstrap scripts containing the
  // base interfaces and the implementation of the internal classes.
  const Error& error = Error::Handle(Bootstrap::LoadandCompileScripts());
  if (!error.IsNull()) {
    return error.raw();
  }

  ClassFinalizer::VerifyBootstrapClasses();
  MarkInvisibleFunctions();

  // Set up the intrinsic state of all functions (core, math and typed data).
  Intrinsifier::InitializeState();

  // Set up recognized state of all functions (core, math and typed data).
  MethodRecognizer::InitializeState();

  return Error::null();
}


void Object::InitFromSnapshot(Isolate* isolate) {
  TIMERSCOPE(time_bootstrap);
  ObjectStore* object_store = isolate->object_store();

  Class& cls = Class::Handle();

  // Set up empty classes in the object store, these will get
  // initialized correctly when we read from the snapshot.
  // This is done to allow bootstrapping of reading classes from the snapshot.
  cls = Class::New<Instance>(kInstanceCid);
  object_store->set_object_class(cls);

  cls = Class::New<Type>();
  object_store->set_type_class(cls);

  cls = Class::New<TypeParameter>();
  object_store->set_type_parameter_class(cls);

  cls = Class::New<BoundedType>();
  object_store->set_bounded_type_class(cls);

  cls = Class::New<MixinAppType>();
  object_store->set_mixin_app_type_class(cls);

  cls = Class::New<Array>();
  object_store->set_array_class(cls);

  cls = Class::New<Array>(kImmutableArrayCid);
  object_store->set_immutable_array_class(cls);

  cls = Class::New<GrowableObjectArray>();
  object_store->set_growable_object_array_class(cls);

  cls = Class::New<Float32x4>();
  object_store->set_float32x4_class(cls);

  cls = Class::New<Int32x4>();
  object_store->set_int32x4_class(cls);

#define REGISTER_TYPED_DATA_CLASS(clazz)                                       \
  cls = Class::NewTypedDataClass(kTypedData##clazz##Cid);
  CLASS_LIST_TYPED_DATA(REGISTER_TYPED_DATA_CLASS);
#undef REGISTER_TYPED_DATA_CLASS
#define REGISTER_TYPED_DATA_VIEW_CLASS(clazz)                                  \
  cls = Class::NewTypedDataViewClass(kTypedData##clazz##ViewCid);
  CLASS_LIST_TYPED_DATA(REGISTER_TYPED_DATA_VIEW_CLASS);
  cls = Class::NewTypedDataViewClass(kByteDataViewCid);
#undef REGISTER_TYPED_DATA_VIEW_CLASS
#define REGISTER_EXT_TYPED_DATA_CLASS(clazz)                                   \
  cls = Class::NewExternalTypedDataClass(kExternalTypedData##clazz##Cid);
  CLASS_LIST_TYPED_DATA(REGISTER_EXT_TYPED_DATA_CLASS);
#undef REGISTER_EXT_TYPED_DATA_CLASS

  cls = Class::New<Integer>();
  object_store->set_integer_implementation_class(cls);

  cls = Class::New<Smi>();
  object_store->set_smi_class(cls);

  cls = Class::New<Mint>();
  object_store->set_mint_class(cls);

  cls = Class::New<Double>();
  object_store->set_double_class(cls);

  cls = Class::New<Bigint>();
  object_store->set_bigint_class(cls);

  cls = Class::NewStringClass(kOneByteStringCid);
  object_store->set_one_byte_string_class(cls);

  cls = Class::NewStringClass(kTwoByteStringCid);
  object_store->set_two_byte_string_class(cls);

  cls = Class::NewStringClass(kExternalOneByteStringCid);
  object_store->set_external_one_byte_string_class(cls);

  cls = Class::NewStringClass(kExternalTwoByteStringCid);
  object_store->set_external_two_byte_string_class(cls);

  cls = Class::New<Bool>();
  object_store->set_bool_class(cls);

  cls = Class::New<Instance>(kNullCid);
  object_store->set_null_class(cls);

  cls = Class::New<Stacktrace>();
  object_store->set_stacktrace_class(cls);

  cls = Class::New<JSRegExp>();
  object_store->set_jsregexp_class(cls);

  // Some classes are not stored in the object store. Yet we still need to
  // create their Class object so that they get put into the class_table
  // (as a side effect of Class::New()).
  cls = Class::New<Number>();

  cls = Class::New<WeakProperty>();
  object_store->set_weak_property_class(cls);

  cls = Class::New<MirrorReference>();
  object_store->set_mirror_reference_class(cls);
}


void Object::Print() const {
  OS::Print("%s\n", ToCString());
}


RawString* Object::DictionaryName() const {
  return String::null();
}


void Object::InitializeObject(uword address, intptr_t class_id, intptr_t size) {
  // TODO(iposva): Get a proper halt instruction from the assembler which
  // would be needed here for code objects.
  uword initial_value = reinterpret_cast<uword>(null_);
  uword cur = address;
  uword end = address + size;
  while (cur < end) {
    *reinterpret_cast<uword*>(cur) = initial_value;
    cur += kWordSize;
  }
  uword tags = 0;
  ASSERT(class_id != kIllegalCid);
  tags = RawObject::ClassIdTag::update(class_id, tags);
  tags = RawObject::SizeTag::update(size, tags);
  reinterpret_cast<RawObject*>(address)->tags_ = tags;
}


void Object::CheckHandle() const {
#if defined(DEBUG)
  if (raw_ != Object::null()) {
    if ((reinterpret_cast<uword>(raw_) & kSmiTagMask) == kSmiTag) {
      ASSERT(vtable() == Smi::handle_vtable_);
      return;
    }
    intptr_t cid = raw_->GetClassId();
    if (cid >= kNumPredefinedCids) {
      cid = kInstanceCid;
    }
    ASSERT(vtable() == builtin_vtables_[cid]);
    if (FLAG_verify_handles) {
      Isolate* isolate = Isolate::Current();
      Heap* isolate_heap = isolate->heap();
      Heap* vm_isolate_heap = Dart::vm_isolate()->heap();
      ASSERT(isolate_heap->Contains(RawObject::ToAddr(raw_)) ||
             vm_isolate_heap->Contains(RawObject::ToAddr(raw_)));
    }
  }
#endif
}


RawObject* Object::Allocate(intptr_t cls_id,
                            intptr_t size,
                            Heap::Space space) {
  ASSERT(Utils::IsAligned(size, kObjectAlignment));
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate->no_callback_scope_depth() == 0);
  Heap* heap = isolate->heap();

  uword address = heap->Allocate(size, space);
  if (address == 0) {
    // Use the preallocated out of memory exception to avoid calling
    // into dart code or allocating any code.
    const Instance& exception =
        Instance::Handle(isolate->object_store()->out_of_memory());
    Exceptions::Throw(exception);
    UNREACHABLE();
  }
  NoGCScope no_gc;
  InitializeObject(address, cls_id, size);
  RawObject* raw_obj = reinterpret_cast<RawObject*>(address + kHeapObjectTag);
  ASSERT(cls_id == RawObject::ClassIdTag::decode(raw_obj->ptr()->tags_));
  return raw_obj;
}


class StoreBufferUpdateVisitor : public ObjectPointerVisitor {
 public:
  explicit StoreBufferUpdateVisitor(Isolate* isolate, RawObject* obj) :
      ObjectPointerVisitor(isolate), old_obj_(obj) {
    ASSERT(old_obj_->IsOldObject());
  }

  void VisitPointers(RawObject** first, RawObject** last) {
    for (RawObject** curr = first; curr <= last; ++curr) {
      RawObject* raw_obj = *curr;
      if (raw_obj->IsHeapObject() && raw_obj->IsNewObject()) {
        old_obj_->SetRememberedBit();
        isolate()->store_buffer()->AddObject(old_obj_);
        // Remembered this object. There is no need to continue searching.
        return;
      }
    }
  }

 private:
  RawObject* old_obj_;

  DISALLOW_COPY_AND_ASSIGN(StoreBufferUpdateVisitor);
};


bool Object::IsReadOnlyHandle() const {
  return Dart::IsReadOnlyHandle(reinterpret_cast<uword>(this));
}


bool Object::IsNotTemporaryScopedHandle() const {
  return (IsZoneHandle() || IsReadOnlyHandle());
}



RawObject* Object::Clone(const Object& src, Heap::Space space) {
  const Class& cls = Class::Handle(src.clazz());
  intptr_t size = src.raw()->Size();
  RawObject* raw_obj = Object::Allocate(cls.id(), size, space);
  NoGCScope no_gc;
  memmove(raw_obj->ptr(), src.raw()->ptr(), size);
  if ((space == Heap::kOld) && !raw_obj->IsRemembered()) {
    StoreBufferUpdateVisitor visitor(Isolate::Current(), raw_obj);
    raw_obj->VisitPointers(&visitor);
  }
  return raw_obj;
}


RawString* Class::Name() const {
  ASSERT(raw_ptr()->name_ != String::null());
  return raw_ptr()->name_;
}


RawString* Class::UserVisibleName() const {
  if (FLAG_show_internal_names) {
    return Name();
  }
  switch (id()) {
    case kIntegerCid:
    case kSmiCid:
    case kMintCid:
    case kBigintCid:
      return Symbols::Int().raw();
    case kDoubleCid:
      return Symbols::Double().raw();
    case kOneByteStringCid:
    case kTwoByteStringCid:
    case kExternalOneByteStringCid:
    case kExternalTwoByteStringCid:
      return Symbols::New("String");
    case kArrayCid:
    case kImmutableArrayCid:
    case kGrowableObjectArrayCid:
      return Symbols::List().raw();
    case kFloat32x4Cid:
      return Symbols::Float32x4().raw();
    case kInt32x4Cid:
      return Symbols::Int32x4().raw();
    case kTypedDataInt8ArrayCid:
    case kExternalTypedDataInt8ArrayCid:
      return Symbols::Int8List().raw();
    case kTypedDataUint8ArrayCid:
    case kExternalTypedDataUint8ArrayCid:
      return Symbols::Uint8List().raw();
    case kTypedDataUint8ClampedArrayCid:
    case kExternalTypedDataUint8ClampedArrayCid:
      return Symbols::Uint8ClampedList().raw();
    case kTypedDataInt16ArrayCid:
    case kExternalTypedDataInt16ArrayCid:
      return Symbols::Int16List().raw();
    case kTypedDataUint16ArrayCid:
    case kExternalTypedDataUint16ArrayCid:
      return Symbols::Uint16List().raw();
    case kTypedDataInt32ArrayCid:
    case kExternalTypedDataInt32ArrayCid:
      return Symbols::Int32List().raw();
    case kTypedDataUint32ArrayCid:
    case kExternalTypedDataUint32ArrayCid:
      return Symbols::Uint32List().raw();
    case kTypedDataInt64ArrayCid:
    case kExternalTypedDataInt64ArrayCid:
      return Symbols::Int64List().raw();
    case kTypedDataUint64ArrayCid:
    case kExternalTypedDataUint64ArrayCid:
      return Symbols::Uint64List().raw();
    case kTypedDataFloat32x4ArrayCid:
    case kExternalTypedDataFloat32x4ArrayCid:
      return Symbols::Float32x4List().raw();
    case kTypedDataFloat32ArrayCid:
    case kExternalTypedDataFloat32ArrayCid:
      return Symbols::Float32List().raw();
    case kTypedDataFloat64ArrayCid:
    case kExternalTypedDataFloat64ArrayCid:
      return Symbols::Float64List().raw();
    default:
      if (!IsSignatureClass()) {
        const String& name = String::Handle(Name());
        return String::IdentifierPrettyName(name);
      } else {
        return Name();
      }
  }
  UNREACHABLE();
}


RawType* Class::SignatureType() const {
  ASSERT(IsSignatureClass());
  const Function& function = Function::Handle(signature_function());
  ASSERT(!function.IsNull());
  if (function.signature_class() != raw()) {
    // This class is a function type alias. Return the canonical signature type.
    const Class& canonical_class = Class::Handle(function.signature_class());
    return canonical_class.SignatureType();
  }
  // Return the first canonical signature type if already computed at class
  // finalization time. The optimizer may canonicalize instantiated function
  // types of the same signature class, but these will be added after the
  // uninstantiated signature class at index 0.
  const Array& signature_types = Array::Handle(canonical_types());
  // The canonical_types array is initialized to the empty array.
  ASSERT(!signature_types.IsNull());
  if (signature_types.Length() > 0) {
    Type& signature_type = Type::Handle();
    signature_type ^= signature_types.At(0);
    ASSERT(!signature_type.IsNull());
    return signature_type.raw();
  }
  // A signature class extends class Instance and is parameterized in the same
  // way as the owner class of its non-static signature function.
  // It is not type parameterized if its signature function is static.
  // See Class::NewSignatureClass() for the setup of its type parameters.
  // During type finalization, the type arguments of the super class of the
  // owner class of its signature function will be prepended to the type
  // argument vector. Therefore, we only need to set the type arguments
  // matching the type parameters here.
  const TypeArguments& signature_type_arguments =
      TypeArguments::Handle(type_parameters());
  const Type& signature_type = Type::Handle(
      Type::New(*this, signature_type_arguments, token_pos()));

  // Return the still unfinalized signature type.
  ASSERT(!signature_type.IsFinalized());
  return signature_type.raw();
}


RawAbstractType* Class::RareType() const {
  const Type& type = Type::Handle(Type::New(
      *this,
      Object::null_abstract_type_arguments(),
      Scanner::kDummyTokenIndex));
  return ClassFinalizer::FinalizeType(*this,
                                      type,
                                      ClassFinalizer::kCanonicalize);
}


RawAbstractType* Class::DeclarationType() const {
  const TypeArguments& args = TypeArguments::Handle(type_parameters());
  const Type& type = Type::Handle(Type::New(
      *this,
      args,
      Scanner::kDummyTokenIndex));
  return ClassFinalizer::FinalizeType(*this,
                                      type,
                                      ClassFinalizer::kCanonicalize);
}


template <class FakeObject>
RawClass* Class::New() {
  ASSERT(Object::class_class() != Class::null());
  Class& result = Class::Handle();
  {
    RawObject* raw = Object::Allocate(Class::kClassId,
                                      Class::InstanceSize(),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
  }
  FakeObject fake;
  result.set_handle_vtable(fake.vtable());
  result.set_instance_size(FakeObject::InstanceSize());
  result.set_next_field_offset(FakeObject::NextFieldOffset());
  ASSERT((FakeObject::kClassId != kInstanceCid));
  result.set_id(FakeObject::kClassId);
  result.set_state_bits(0);
  // VM backed classes are almost ready: run checks and resolve class
  // references, but do not recompute size.
  result.set_is_prefinalized();
  result.set_type_arguments_field_offset_in_words(kNoTypeArguments);
  result.set_num_type_arguments(0);
  result.set_num_own_type_arguments(0);
  result.set_num_native_fields(0);
  result.set_token_pos(Scanner::kDummyTokenIndex);
  result.InitEmptyFields();
  Isolate::Current()->RegisterClass(result);
  return result.raw();
}


static RawError* FormatError(const Error& prev_error,
                             const Script& script,
                             intptr_t token_pos,
                             const char* format, ...) {
  va_list args;
  va_start(args, format);
  if (prev_error.IsNull()) {
    return Parser::FormatError(script, token_pos, "Error", format, args);
  } else {
    return Parser::FormatErrorWithAppend(prev_error, script, token_pos,
                                         "Error", format, args);
  }
}


static void ReportTooManyTypeArguments(const Class& cls) {
  const Error& error = Error::Handle(
      FormatError(Error::Handle(),  // No previous error.
                  Script::Handle(cls.script()), cls.token_pos(),
                  "too many type parameters declared in class '%s' or in its "
                  "super classes",
                  String::Handle(cls.Name()).ToCString()));
  Isolate::Current()->long_jump_base()->Jump(1, error);
  UNREACHABLE();
}


void Class::set_num_type_arguments(intptr_t value) const {
  if (!Utils::IsInt(16, value)) {
    ReportTooManyTypeArguments(*this);
  }
  raw_ptr()->num_type_arguments_ = value;
}


void Class::set_num_own_type_arguments(intptr_t value) const {
  if (!Utils::IsInt(16, value)) {
    ReportTooManyTypeArguments(*this);
  }
  raw_ptr()->num_own_type_arguments_ = value;
}


// Initialize class fields of type Array with empty array.
void Class::InitEmptyFields() {
  if (Object::empty_array().raw() == Array::null()) {
    // The empty array has not been initialized yet.
    return;
  }
  StorePointer(&raw_ptr()->interfaces_, Object::empty_array().raw());
  StorePointer(&raw_ptr()->constants_, Object::empty_array().raw());
  StorePointer(&raw_ptr()->canonical_types_, Object::empty_array().raw());
  StorePointer(&raw_ptr()->functions_, Object::empty_array().raw());
  StorePointer(&raw_ptr()->fields_, Object::empty_array().raw());
  StorePointer(&raw_ptr()->invocation_dispatcher_cache_,
               Object::empty_array().raw());
}


RawArray* Class::OffsetToFieldMap() const {
  Array& array = Array::Handle(raw_ptr()->offset_in_words_to_field_);
  if (array.IsNull()) {
    ASSERT(is_finalized());
    const intptr_t length = raw_ptr()->instance_size_in_words_;
    array = Array::New(length, Heap::kOld);
    Class& cls = Class::Handle(this->raw());
    Array& fields = Array::Handle();
    Field& f = Field::Handle();
    while (!cls.IsNull()) {
      fields = cls.fields();
      for (intptr_t i = 0; i < fields.Length(); ++i) {
        f ^= fields.At(i);
        if (!f.is_static()) {
          array.SetAt(f.Offset() >> kWordSizeLog2, f);
        }
      }
      cls = cls.SuperClass();
    }
    StorePointer(&raw_ptr()->offset_in_words_to_field_, array.raw());
  }
  return array.raw();
}


bool Class::HasInstanceFields() const {
  const Array& field_array = Array::Handle(fields());
  Field& field = Field::Handle();
  for (intptr_t i = 0; i < field_array.Length(); ++i) {
    field ^= field_array.At(i);
    if (!field.is_static()) {
      return true;
    }
  }
  return false;
}


void Class::SetFunctions(const Array& value) const {
  ASSERT(!value.IsNull());
#if defined(DEBUG)
  // Verify that all the functions in the array have this class as owner.
  Function& func = Function::Handle();
  intptr_t len = value.Length();
  for (intptr_t i = 0; i < len; i++) {
    func ^= value.At(i);
    ASSERT(func.Owner() == raw());
  }
#endif
  StorePointer(&raw_ptr()->functions_, value.raw());
}


void Class::AddFunction(const Function& function) const {
  const Array& arr = Array::Handle(functions());
  const Array& new_arr = Array::Handle(Array::Grow(arr, arr.Length() + 1));
  new_arr.SetAt(arr.Length(), function);
  SetFunctions(new_arr);
}


void Class::AddClosureFunction(const Function& function) const {
  GrowableObjectArray& closures =
      GrowableObjectArray::Handle(raw_ptr()->closure_functions_);
  if (closures.IsNull()) {
    closures = GrowableObjectArray::New(4);
    StorePointer(&raw_ptr()->closure_functions_, closures.raw());
  }
  ASSERT(function.IsNonImplicitClosureFunction());
  closures.Add(function);
}


// Lookup the innermost closure function that contains token at token_pos.
RawFunction* Class::LookupClosureFunction(intptr_t token_pos) const {
  if (raw_ptr()->closure_functions_ == GrowableObjectArray::null()) {
    return Function::null();
  }
  const GrowableObjectArray& closures =
      GrowableObjectArray::Handle(raw_ptr()->closure_functions_);
  Function& closure = Function::Handle();
  intptr_t num_closures = closures.Length();
  intptr_t best_fit_token_pos = -1;
  intptr_t best_fit_index = -1;
  for (intptr_t i = 0; i < num_closures; i++) {
    closure ^= closures.At(i);
    ASSERT(!closure.IsNull());
    if ((closure.token_pos() <= token_pos) &&
        (token_pos < closure.end_token_pos()) &&
        (best_fit_token_pos < closure.token_pos())) {
      best_fit_index = i;
      best_fit_token_pos = closure.token_pos();
    }
  }
  closure = Function::null();
  if (best_fit_index >= 0) {
    closure ^= closures.At(best_fit_index);
  }
  return closure.raw();
}


void Class::set_signature_function(const Function& value) const {
  ASSERT(value.IsClosureFunction() || value.IsSignatureFunction());
  StorePointer(&raw_ptr()->signature_function_, value.raw());
}


void Class::set_state_bits(intptr_t bits) const {
  raw_ptr()->state_bits_ = static_cast<uint16_t>(bits);
}


void Class::set_library(const Library& value) const {
  StorePointer(&raw_ptr()->library_, value.raw());
}


void Class::set_type_parameters(const TypeArguments& value) const {
  StorePointer(&raw_ptr()->type_parameters_, value.raw());
}


intptr_t Class::NumTypeParameters() const {
  if (IsMixinApplication() && !is_mixin_type_applied()) {
    ClassFinalizer::ApplyMixinType(*this);
  }
  if (type_parameters() == TypeArguments::null()) {
    return 0;
  }
  const TypeArguments& type_params = TypeArguments::Handle(type_parameters());
  return type_params.Length();
}


intptr_t Class::NumOwnTypeArguments() const {
  // Return cached value if already calculated.
  if (num_own_type_arguments() != kUnknownNumTypeArguments) {
    return num_own_type_arguments();
  }
  Isolate* isolate = Isolate::Current();
  const intptr_t num_type_params = NumTypeParameters();
  if (!FLAG_overlap_type_arguments ||
      (num_type_params == 0) ||
      (super_type() == AbstractType::null()) ||
      (super_type() == isolate->object_store()->object_type())) {
    set_num_own_type_arguments(num_type_params);
    return num_type_params;
  }
  ASSERT(!IsMixinApplication() || is_mixin_type_applied());
  const AbstractType& sup_type = AbstractType::Handle(isolate, super_type());
  ASSERT(sup_type.IsResolved());
  const AbstractTypeArguments& sup_type_args =
      AbstractTypeArguments::Handle(isolate, sup_type.arguments());
  if (sup_type_args.IsNull()) {
    // The super type is raw or the super class is non generic.
    // In either case, overlapping is not possible.
    set_num_own_type_arguments(num_type_params);
    return num_type_params;
  }
  const intptr_t num_sup_type_args = sup_type_args.Length();
  // At this point, the super type may or may not be finalized. In either case,
  // the result of this function must remain the same.
  // The value of num_sup_type_args may increase when the super type is
  // finalized, but the last num_sup_type_args type arguments will not be
  // modified by finalization, only shifted to higher indices in the vector.
  // They may however get wrapped in a BoundedType, which we skip.
  const AbstractTypeArguments& type_params =
      AbstractTypeArguments::Handle(isolate, type_parameters());
  // Determine the maximum overlap of a prefix of the vector consisting of the
  // type parameters of this class with a suffix of the vector consisting of the
  // type arguments of the super type of this class.
  // The number of own type arguments of this class is the number of its type
  // parameters minus the number of type arguments in the overlap.
  // Attempt to overlap the whole vector of type parameters; reduce the size
  // of the vector (keeping the first type parameter) until it fits or until
  // its size is zero.
  TypeParameter& type_param = TypeParameter::Handle(isolate);
  AbstractType& sup_type_arg = AbstractType::Handle(isolate);
  for (intptr_t num_overlapping_type_args =
           (num_type_params < num_sup_type_args) ?
               num_type_params : num_sup_type_args;
       num_overlapping_type_args > 0; num_overlapping_type_args--) {
    intptr_t i = 0;
    for (; i < num_overlapping_type_args; i++) {
      type_param ^= type_params.TypeAt(i);
      sup_type_arg = sup_type_args.TypeAt(
          num_sup_type_args - num_overlapping_type_args + i);
      // BoundedType can nest in case the finalized super type has bounded type
      // arguments that overlap multiple times in its own super class chain.
      while (sup_type_arg.IsBoundedType()) {
        sup_type_arg = BoundedType::Cast(sup_type_arg).type();
      }
      if (!type_param.Equals(sup_type_arg)) break;
    }
    if (i == num_overlapping_type_args) {
      // Overlap found.
      set_num_own_type_arguments(num_type_params - num_overlapping_type_args);
      return num_type_params - num_overlapping_type_args;
    }
  }
  // No overlap found.
  set_num_own_type_arguments(num_type_params);
  return num_type_params;
}


intptr_t Class::NumTypeArguments() const {
  // Return cached value if already calculated.
  if (num_type_arguments() != kUnknownNumTypeArguments) {
    return num_type_arguments();
  }
  // To work properly, this call requires the super class of this class to be
  // resolved, which is checked by the type_class() call on the super type.
  // Note that calling type_class() on a MixinAppType fails.
  Isolate* isolate = Isolate::Current();
  Class& cls = Class::Handle(isolate);
  AbstractType& sup_type = AbstractType::Handle(isolate);
  cls = raw();
  intptr_t num_type_args = 0;
  do {
    if (cls.IsSignatureClass()) {
      Function& signature_fun = Function::Handle(isolate);
      signature_fun ^= cls.signature_function();
      if (!signature_fun.is_static() &&
          !signature_fun.HasInstantiatedSignature()) {
        cls = signature_fun.Owner();
      }
    }
    // Calling NumOwnTypeArguments() on a mixin application class will setup the
    // type parameters if not already done.
    num_type_args += cls.NumOwnTypeArguments();
    // Super type of Object class is null.
    if ((cls.super_type() == AbstractType::null()) ||
        (cls.super_type() == isolate->object_store()->object_type())) {
      break;
    }
    sup_type = cls.super_type();
    cls = sup_type.type_class();
  } while (true);
  set_num_type_arguments(num_type_args);
  return num_type_args;
}


RawClass* Class::SuperClass() const {
  if (super_type() == AbstractType::null()) {
    return Class::null();
  }
  const AbstractType& sup_type = AbstractType::Handle(super_type());
  return sup_type.type_class();
}


void Class::set_super_type(const AbstractType& value) const {
  ASSERT(value.IsNull() ||
         (value.IsType() && !value.IsDynamicType()) ||
         value.IsBoundedType() ||
         value.IsMixinAppType());
  StorePointer(&raw_ptr()->super_type_, value.raw());
}


// Return a TypeParameter if the type_name is a type parameter of this class.
// Return null otherwise.
RawTypeParameter* Class::LookupTypeParameter(const String& type_name) const {
  ASSERT(!type_name.IsNull());
  Isolate* isolate = Isolate::Current();
  ReusableHandleScope reused_handles(isolate);
  TypeArguments& type_params = reused_handles.TypeArgumentsHandle();
  type_params ^= type_parameters();
  TypeParameter& type_param = reused_handles.TypeParameterHandle();
  String& type_param_name = reused_handles.StringHandle();
  if (!type_params.IsNull()) {
    const intptr_t num_type_params = type_params.Length();
    for (intptr_t i = 0; i < num_type_params; i++) {
      type_param ^= type_params.TypeAt(i);
      type_param_name = type_param.name();
      if (type_param_name.Equals(type_name)) {
        return type_param.raw();
      }
    }
  }
  return TypeParameter::null();
}


void Class::CalculateFieldOffsets() const {
  Array& flds = Array::Handle(fields());
  const Class& super = Class::Handle(SuperClass());
  intptr_t offset = 0;
  intptr_t type_args_field_offset = kNoTypeArguments;
  if (super.IsNull()) {
    offset = Instance::NextFieldOffset();
    ASSERT(offset > 0);
  } else {
    ASSERT(super.is_finalized() || super.is_prefinalized());
    type_args_field_offset = super.type_arguments_field_offset();
    offset = super.next_field_offset();
    ASSERT(offset > 0);
    // We should never call CalculateFieldOffsets for native wrapper
    // classes, assert this.
    ASSERT(num_native_fields() == 0);
    set_num_native_fields(super.num_native_fields());
  }
  // If the super class is parameterized, use the same type_arguments field,
  // otherwise, if this class is the first in the super chain to be
  // parameterized, introduce a new type_arguments field.
  if (type_args_field_offset == kNoTypeArguments) {
    const TypeArguments& type_params = TypeArguments::Handle(type_parameters());
    if (!type_params.IsNull()) {
      ASSERT(type_params.Length() > 0);
      // The instance needs a type_arguments field.
      type_args_field_offset = offset;
      offset += kWordSize;
    }
  }
  set_type_arguments_field_offset(type_args_field_offset);
  ASSERT(offset > 0);
  Field& field = Field::Handle();
  intptr_t len = flds.Length();
  for (intptr_t i = 0; i < len; i++) {
    field ^= flds.At(i);
    // Offset is computed only for instance fields.
    if (!field.is_static()) {
      ASSERT(field.Offset() == 0);
      field.SetOffset(offset);
      offset += kWordSize;
    }
  }
  set_instance_size(RoundedAllocationSize(offset));
  set_next_field_offset(offset);
}


RawFunction* Class::GetInvocationDispatcher(const String& target_name,
                                            const Array& args_desc,
                                            RawFunction::Kind kind) const {
  enum {
    kNameIndex = 0,
    kArgsDescIndex,
    kFunctionIndex,
    kEntrySize
  };

  ASSERT(kind == RawFunction::kNoSuchMethodDispatcher ||
         kind == RawFunction::kInvokeFieldDispatcher);
  Function& dispatcher = Function::Handle();
  Array& cache = Array::Handle(invocation_dispatcher_cache());
  ASSERT(!cache.IsNull());
  String& name = String::Handle();
  Array& desc = Array::Handle();
  intptr_t i = 0;
  for (; i < cache.Length(); i += kEntrySize) {
    name ^= cache.At(i + kNameIndex);
    if (name.IsNull()) break;  // Reached last entry.
    if (!name.Equals(target_name)) continue;
    desc ^= cache.At(i + kArgsDescIndex);
    if (desc.raw() != args_desc.raw()) continue;
    dispatcher ^= cache.At(i + kFunctionIndex);
    if (dispatcher.kind() == kind) {
      // Found match.
      ASSERT(dispatcher.IsFunction());
      break;
    }
  }

  if (dispatcher.IsNull()) {
    if (i == cache.Length()) {
      // Allocate new larger cache.
      intptr_t new_len = (cache.Length() == 0)
          ? static_cast<intptr_t>(kEntrySize)
          : cache.Length() * 2;
      cache ^= Array::Grow(cache, new_len);
      set_invocation_dispatcher_cache(cache);
    }
    dispatcher ^= CreateInvocationDispatcher(target_name, args_desc, kind);
    cache.SetAt(i + kNameIndex, target_name);
    cache.SetAt(i + kArgsDescIndex, args_desc);
    cache.SetAt(i + kFunctionIndex, dispatcher);
  }
  return dispatcher.raw();
}


RawFunction* Class::CreateInvocationDispatcher(const String& target_name,
                                               const Array& args_desc,
                                               RawFunction::Kind kind) const {
  Function& invocation = Function::Handle(
      Function::New(String::Handle(Symbols::New(target_name)),
                    kind,
                    false,  // Not static.
                    false,  // Not const.
                    false,  // Not abstract.
                    false,  // Not external.
                    *this,
                    0));  // No token position.
  ArgumentsDescriptor desc(args_desc);
  invocation.set_num_fixed_parameters(desc.PositionalCount());
  invocation.SetNumOptionalParameters(desc.NamedCount(),
                                      false);  // Not positional.
  invocation.set_parameter_types(Array::Handle(Array::New(desc.Count(),
                                                          Heap::kOld)));
  invocation.set_parameter_names(Array::Handle(Array::New(desc.Count(),
                                                          Heap::kOld)));
  // Receiver.
  invocation.SetParameterTypeAt(0, Type::Handle(Type::DynamicType()));
  invocation.SetParameterNameAt(0, Symbols::This());
  // Remaining positional parameters.
  intptr_t i = 1;
  for (; i < desc.PositionalCount(); i++) {
    invocation.SetParameterTypeAt(i, Type::Handle(Type::DynamicType()));
    char name[64];
    OS::SNPrint(name, 64, ":p%" Pd, i);
    invocation.SetParameterNameAt(i, String::Handle(Symbols::New(name)));
  }

  // Named parameters.
  for (; i < desc.Count(); i++) {
    invocation.SetParameterTypeAt(i, Type::Handle(Type::DynamicType()));
    intptr_t index = i - desc.PositionalCount();
    invocation.SetParameterNameAt(i, String::Handle(desc.NameAt(index)));
  }
  invocation.set_result_type(Type::Handle(Type::DynamicType()));
  invocation.set_is_visible(false);  // Not visible in stack trace.
  invocation.set_saved_args_desc(args_desc);

  return invocation.raw();
}


RawArray* Class::invocation_dispatcher_cache() const {
  return raw_ptr()->invocation_dispatcher_cache_;
}


void Class::set_invocation_dispatcher_cache(const Array& cache) const {
  StorePointer(&raw_ptr()->invocation_dispatcher_cache_, cache.raw());
}


void Class::Finalize() const {
  ASSERT(!is_finalized());
  // Prefinalized classes have a VM internal representation and no Dart fields.
  // Their instance size  is precomputed and field offsets are known.
  if (!is_prefinalized()) {
    // Compute offsets of instance fields and instance size.
    CalculateFieldOffsets();
  }
  set_is_finalized();
}


// Apply the members from the patch class to the original class.
bool Class::ApplyPatch(const Class& patch, Error* error) const {
  ASSERT(error != NULL);
  ASSERT(!is_finalized());
  // Shared handles used during the iteration.
  String& member_name = String::Handle();

  const PatchClass& patch_class =
      PatchClass::Handle(PatchClass::New(*this, patch));

  Array& orig_list = Array::Handle(functions());
  intptr_t orig_len = orig_list.Length();
  Array& patch_list = Array::Handle(patch.functions());
  intptr_t patch_len = patch_list.Length();

  // TODO(iposva): Verify that only patching existing methods and adding only
  // new private methods.
  Function& func = Function::Handle();
  Function& orig_func = Function::Handle();
  // Lookup the original implicit constructor, if any.
  member_name = Name();
  member_name = String::Concat(member_name, Symbols::Dot());
  Function& orig_implicit_ctor = Function::Handle(LookupFunction(member_name));
  if (!orig_implicit_ctor.IsNull() &&
      !orig_implicit_ctor.IsImplicitConstructor()) {
    // Not an implicit constructor, but a user declared one.
    orig_implicit_ctor = Function::null();
  }
  const GrowableObjectArray& new_functions = GrowableObjectArray::Handle(
      GrowableObjectArray::New(orig_len));
  for (intptr_t i = 0; i < orig_len; i++) {
    orig_func ^= orig_list.At(i);
    member_name ^= orig_func.name();
    func = patch.LookupFunction(member_name);
    if (func.IsNull()) {
      // Non-patched function is preserved, all patched functions are added in
      // the loop below.
      // However, an implicitly created constructor should not be preserved if
      // the patch provides a constructor or a factory. Wait for now.
      if (orig_func.raw() != orig_implicit_ctor.raw()) {
        new_functions.Add(orig_func);
      }
    } else if (func.UserVisibleSignature() !=
               orig_func.UserVisibleSignature()) {
      // Compare user visible signatures to ignore different implicit parameters
      // when patching a constructor with a factory.
      *error = FormatError(*error,  // No previous error.
                           Script::Handle(patch.script()), func.token_pos(),
                           "signature mismatch: '%s'", member_name.ToCString());
      return false;
    }
  }
  for (intptr_t i = 0; i < patch_len; i++) {
    func ^= patch_list.At(i);
    if (func.IsConstructor() || func.IsFactory()) {
      // Do not preserve the original implicit constructor, if any.
      orig_implicit_ctor = Function::null();
    }
    func.set_owner(patch_class);
    new_functions.Add(func);
  }
  if (!orig_implicit_ctor.IsNull()) {
    // Preserve the original implicit constructor.
    new_functions.Add(orig_implicit_ctor);
  }
  Array& new_list = Array::Handle(Array::MakeArray(new_functions));
  SetFunctions(new_list);

  // Merge the two list of fields. Raise an error when duplicates are found or
  // when a public field is being added.
  orig_list = fields();
  orig_len = orig_list.Length();
  patch_list = patch.fields();
  patch_len = patch_list.Length();

  Field& field = Field::Handle();
  Field& orig_field = Field::Handle();
  new_list = Array::New(patch_len + orig_len);
  for (intptr_t i = 0; i < patch_len; i++) {
    field ^= patch_list.At(i);
    field.set_owner(*this);
    member_name = field.name();
    // TODO(iposva): Verify non-public fields only.

    // Verify no duplicate additions.
    orig_field ^= LookupField(member_name);
    if (!orig_field.IsNull()) {
      *error = FormatError(*error,  // No previous error.
                           Script::Handle(patch.script()), field.token_pos(),
                           "duplicate field: %s", member_name.ToCString());
      return false;
    }
    new_list.SetAt(i, field);
  }
  for (intptr_t i = 0; i < orig_len; i++) {
    field ^= orig_list.At(i);
    new_list.SetAt(patch_len + i, field);
  }
  SetFields(new_list);

  // The functions and fields in the patch class are no longer needed.
  patch.SetFunctions(Object::empty_array());
  patch.SetFields(Object::empty_array());
  return true;
}


static RawPatchClass* MakeTempPatchClass(const Class& cls,
                                         const String& expr) {
  String& src = String::Handle(String::New("() => "));
  src = String::Concat(src, expr);
  src = String::Concat(src, Symbols::Semicolon());
  Script& script = Script::Handle();
  script = Script::New(Symbols::Empty(), src, RawScript::kSourceTag);
  // In order to tokenize the source, we need to get the key to mangle
  // private names from the library from which the object's class
  // originates.
  const Library& lib = Library::Handle(cls.library());
  ASSERT(!lib.IsNull());
  const String& lib_key = String::Handle(lib.private_key());
  script.Tokenize(lib_key);

  const String& src_class_name = String::Handle(Symbols::New(":internal"));
  const Class& src_class = Class::Handle(
      Class::New(src_class_name, script, Scanner::kDummyTokenIndex));
  src_class.set_is_finalized();
  src_class.set_library(lib);
  return PatchClass::New(cls, src_class);
}


RawObject* Class::Evaluate(const String& expr) const {
  const PatchClass& temp_class =
      PatchClass::Handle(MakeTempPatchClass(*this, expr));
  const String& eval_func_name = String::Handle(Symbols::New(":eval"));
  const Function& eval_func =
      Function::Handle(Function::New(eval_func_name,
                                     RawFunction::kRegularFunction,
                                     true,   // Static.
                                     false,  // Not const.
                                     false,  // Not abstract.
                                     false,  // Not external.
                                     temp_class,
                                     0));
  eval_func.set_result_type(Type::Handle(Type::DynamicType()));
  eval_func.set_num_fixed_parameters(0);
  eval_func.SetNumOptionalParameters(0, true);
  eval_func.set_is_optimizable(false);

  const Array& args = Array::Handle(Array::New(0));
  const Object& result =
      Object::Handle(DartEntry::InvokeFunction(eval_func, args));
  return result.raw();
}


// Ensure that top level parsing of the class has been done.
RawError* Class::EnsureIsFinalized(Isolate* isolate) const {
  // Finalized classes have already been parsed.
  if (is_finalized()) {
    return Error::null();
  }
  ASSERT(isolate != NULL);
  const Error& error = Error::Handle(isolate, Compiler::CompileClass(*this));
  if (!error.IsNull() && (isolate->long_jump_base() != NULL)) {
    isolate->long_jump_base()->Jump(1, error);
    UNREACHABLE();
  }
  return error.raw();
}


void Class::SetFields(const Array& value) const {
  ASSERT(!value.IsNull());
#if defined(DEBUG)
  // Verify that all the fields in the array have this class as owner.
  Field& field = Field::Handle();
  intptr_t len = value.Length();
  for (intptr_t i = 0; i < len; i++) {
    field ^= value.At(i);
    ASSERT(field.owner() == raw());
  }
#endif
  // The value of static fields is already initialized to null.
  StorePointer(&raw_ptr()->fields_, value.raw());
}


template <class FakeInstance>
RawClass* Class::New(intptr_t index) {
  ASSERT(Object::class_class() != Class::null());
  Class& result = Class::Handle();
  {
    RawObject* raw = Object::Allocate(Class::kClassId,
                                      Class::InstanceSize(),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
  }
  FakeInstance fake;
  ASSERT(fake.IsInstance());
  result.set_handle_vtable(fake.vtable());
  result.set_instance_size(FakeInstance::InstanceSize());
  result.set_next_field_offset(FakeInstance::NextFieldOffset());
  result.set_id(index);
  result.set_state_bits(0);
  result.set_type_arguments_field_offset_in_words(kNoTypeArguments);
  result.set_num_type_arguments(kUnknownNumTypeArguments);
  result.set_num_own_type_arguments(kUnknownNumTypeArguments);
  result.set_num_native_fields(0);
  result.set_token_pos(Scanner::kDummyTokenIndex);
  result.InitEmptyFields();
  Isolate::Current()->RegisterClass(result);
  return result.raw();
}


RawClass* Class::New(const String& name,
                     const Script& script,
                     intptr_t token_pos) {
  Class& result = Class::Handle(New<Instance>(kIllegalCid));
  result.set_name(name);
  result.set_script(script);
  result.set_token_pos(token_pos);
  return result.raw();
}


RawClass* Class::NewSignatureClass(const String& name,
                                   const Function& signature_function,
                                   const Script& script,
                                   intptr_t token_pos) {
  const Class& result = Class::Handle(New(name, script, token_pos));
  // Instances of a signature class can only be closures.
  result.set_instance_size(Closure::InstanceSize());
  result.set_next_field_offset(Closure::NextFieldOffset());
  // Signature classes extend the _FunctionImpl class.
  result.set_super_type(Type::Handle(
      Isolate::Current()->object_store()->function_impl_type()));
  result.set_is_synthesized_class();
  result.set_type_arguments_field_offset(Closure::type_arguments_offset());
  if (!signature_function.IsNull()) {
    result.PatchSignatureFunction(signature_function);
  }
  return result.raw();
}


void Class::PatchSignatureFunction(const Function& signature_function) const {
  ASSERT(!signature_function.IsNull());
  set_signature_function(signature_function);
  const Class& owner_class = Class::Handle(signature_function.Owner());
  ASSERT(!owner_class.IsNull());
  // A signature class extends class Instance and is either not parameterized or
  // parameterized with exactly the same list of type parameters as the owner
  // class of its function.
  // In case of a function type alias, the function owner is the alias class,
  // which is also the signature class. The signature class is therefore
  // parameterized according to the alias class declaration, even if the
  // function type is not generic.
  // Otherwise, if the function is static or if its signature type is
  // non-generic, i.e. it does not depend on any type parameter of the owner
  // class, then the signature class is not parameterized, although the owner
  // class may be.
  if (owner_class.raw() == raw()) {
    // This signature class is an alias, which cannot be the canonical
    // signature class for this signature function.
    ASSERT(!IsCanonicalSignatureClass());
    // Do not modify the declared type parameters of the alias, even if unused.
  } else {
    // Copy the type parameters only for an instance function type that is not
    // instantiated, i.e. that depends on the type parameters of the owner
    // class.
    // TODO(regis): Verify that it is not a problem for the copied type
    // parameters to refer to the owner class rather than to the signature
    // class. In other words, uninstantiated function types should only get
    // instantiated by the owner class as instantiator and never by the
    // signature class itself.
    TypeArguments& type_parameters = TypeArguments::Handle();
    if (!signature_function.is_static() &&
        (owner_class.NumTypeParameters() > 0) &&
        !signature_function.HasInstantiatedSignature()) {
      type_parameters = owner_class.type_parameters();
    }
    set_type_parameters(type_parameters);
    if (signature_function.signature_class() == Object::null()) {
      // Make this signature class the canonical signature class.
      signature_function.set_signature_class(*this);
      ASSERT(IsCanonicalSignatureClass());
    }
  }
  set_is_prefinalized();
}


RawClass* Class::NewNativeWrapper(const Library& library,
                                  const String& name,
                                  int field_count) {
  Class& cls = Class::Handle(library.LookupClass(name));
  if (cls.IsNull()) {
    cls = New(name, Script::Handle(), Scanner::kDummyTokenIndex);
    cls.SetFields(Object::empty_array());
    cls.SetFunctions(Object::empty_array());
    // Set super class to Object.
    cls.set_super_type(Type::Handle(Type::ObjectType()));
    // Compute instance size. First word contains a pointer to a properly
    // sized typed array once the first native field has been set.
    intptr_t instance_size = sizeof(RawInstance) + kWordSize;
    cls.set_instance_size(RoundedAllocationSize(instance_size));
    cls.set_next_field_offset(instance_size);
    cls.set_num_native_fields(field_count);
    cls.set_is_finalized();
    cls.set_is_type_finalized();
    library.AddClass(cls);
    return cls.raw();
  } else {
    return Class::null();
  }
}


RawClass* Class::NewStringClass(intptr_t class_id) {
  intptr_t instance_size;
  if (class_id == kOneByteStringCid) {
    instance_size = OneByteString::InstanceSize();
  } else if (class_id == kTwoByteStringCid) {
    instance_size = TwoByteString::InstanceSize();
  } else if (class_id == kExternalOneByteStringCid) {
    instance_size = ExternalOneByteString::InstanceSize();
  } else {
    ASSERT(class_id == kExternalTwoByteStringCid);
    instance_size = ExternalTwoByteString::InstanceSize();
  }
  Class& result = Class::Handle(New<String>(class_id));
  result.set_instance_size(instance_size);
  result.set_next_field_offset(String::NextFieldOffset());
  result.set_is_prefinalized();
  return result.raw();
}


RawClass* Class::NewTypedDataClass(intptr_t class_id) {
  ASSERT(RawObject::IsTypedDataClassId(class_id));
  intptr_t instance_size = TypedData::InstanceSize();
  Class& result = Class::Handle(New<TypedData>(class_id));
  result.set_instance_size(instance_size);
  result.set_next_field_offset(TypedData::NextFieldOffset());
  result.set_is_prefinalized();
  return result.raw();
}


RawClass* Class::NewTypedDataViewClass(intptr_t class_id) {
  ASSERT(RawObject::IsTypedDataViewClassId(class_id));
  Class& result = Class::Handle(New<Instance>(class_id));
  result.set_instance_size(0);
  result.set_next_field_offset(-kWordSize);
  return result.raw();
}


RawClass* Class::NewExternalTypedDataClass(intptr_t class_id) {
  ASSERT(RawObject::IsExternalTypedDataClassId(class_id));
  intptr_t instance_size = ExternalTypedData::InstanceSize();
  Class& result = Class::Handle(New<ExternalTypedData>(class_id));
  result.set_instance_size(instance_size);
  result.set_next_field_offset(ExternalTypedData::NextFieldOffset());
  result.set_is_prefinalized();
  return result.raw();
}


void Class::set_name(const String& value) const {
  ASSERT(value.IsSymbol());
  StorePointer(&raw_ptr()->name_, value.raw());
}


void Class::set_script(const Script& value) const {
  StorePointer(&raw_ptr()->script_, value.raw());
}


void Class::set_token_pos(intptr_t token_pos) const {
  ASSERT(token_pos >= 0);
  raw_ptr()->token_pos_ = token_pos;
}


void Class::set_is_implemented() const {
  set_state_bits(ImplementedBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_abstract() const {
  set_state_bits(AbstractBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_type_finalized() const {
  set_state_bits(TypeFinalizedBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_patch() const {
  set_state_bits(PatchBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_synthesized_class() const {
  set_state_bits(SynthesizedClassBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_const() const {
  set_state_bits(ConstBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_mixin_typedef() const {
  set_state_bits(MixinTypedefBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_mixin_type_applied() const {
  set_state_bits(MixinTypeAppliedBit::update(true, raw_ptr()->state_bits_));
}


void Class::set_is_finalized() const {
  ASSERT(!is_finalized());
  set_state_bits(ClassFinalizedBits::update(RawClass::kFinalized,
                                            raw_ptr()->state_bits_));
}


void Class::set_is_prefinalized() const {
  ASSERT(!is_finalized());
  set_state_bits(ClassFinalizedBits::update(RawClass::kPreFinalized,
                                            raw_ptr()->state_bits_));
}


void Class::set_is_marked_for_parsing() const {
  set_state_bits(MarkedForParsingBit::update(true, raw_ptr()->state_bits_));
}


void Class::reset_is_marked_for_parsing() const {
  set_state_bits(MarkedForParsingBit::update(false, raw_ptr()->state_bits_));
}


void Class::set_interfaces(const Array& value) const {
  // Verification and resolving of interfaces occurs in finalizer.
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->interfaces_, value.raw());
}


void Class::set_mixin(const Type& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->mixin_, value.raw());
}


bool Class::IsMixinApplication() const {
  return mixin() != Type::null();
}

bool Class::IsAnonymousMixinApplication() const {
  return IsMixinApplication() && !is_mixin_typedef();
}

void Class::set_patch_class(const Class& cls) const {
  ASSERT(patch_class() == Class::null());
  StorePointer(&raw_ptr()->patch_class_, cls.raw());
}


void Class::AddDirectSubclass(const Class& subclass) const {
  ASSERT(!subclass.IsNull());
  ASSERT(subclass.SuperClass() == raw());
  // Do not keep track of the direct subclasses of class Object.
  ASSERT(!IsObjectClass());
  GrowableObjectArray& direct_subclasses =
      GrowableObjectArray::Handle(raw_ptr()->direct_subclasses_);
  if (direct_subclasses.IsNull()) {
    direct_subclasses = GrowableObjectArray::New(4, Heap::kOld);
    StorePointer(&raw_ptr()->direct_subclasses_, direct_subclasses.raw());
  }
#if defined(DEBUG)
  // Verify that the same class is not added twice.
  for (intptr_t i = 0; i < direct_subclasses.Length(); i++) {
    ASSERT(direct_subclasses.At(i) != subclass.raw());
  }
#endif
  direct_subclasses.Add(subclass);
}


RawArray* Class::constants() const {
  return raw_ptr()->constants_;
}

void Class::set_constants(const Array& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->constants_, value.raw());
}


RawArray* Class::canonical_types() const {
  return raw_ptr()->canonical_types_;
}

void Class::set_canonical_types(const Array& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->canonical_types_, value.raw());
}


void Class::set_allocation_stub(const Code& value) const {
  ASSERT(!value.IsNull());
  ASSERT(raw_ptr()->allocation_stub_ == Code::null());
  StorePointer(&raw_ptr()->allocation_stub_, value.raw());
}


bool Class::IsFunctionClass() const {
  return raw() == Type::Handle(Type::Function()).type_class();
}


bool Class::IsCanonicalSignatureClass() const {
  const Function& function = Function::Handle(signature_function());
  return (!function.IsNull() && (function.signature_class() == raw()));
}


// If test_kind == kIsSubtypeOf, checks if type S is a subtype of type T.
// If test_kind == kIsMoreSpecificThan, checks if S is more specific than T.
// Type S is specified by this class parameterized with 'type_arguments', and
// type T by class 'other' parameterized with 'other_type_arguments'.
// This class and class 'other' do not need to be finalized, however, they must
// be resolved as well as their interfaces.
bool Class::TypeTestNonRecursive(
    const Class& cls,
    Class::TypeTestKind test_kind,
    const AbstractTypeArguments& type_arguments,
    const Class& other,
    const AbstractTypeArguments& other_type_arguments,
    Error* bound_error) {
  // Use the thsi object as if it was the receiver of this method, but instead
  // of recursing reset it to the super class and loop.
  Class& thsi = Class::Handle(cls.raw());
  while (true) {
    ASSERT(!thsi.IsVoidClass());
    // Check for DynamicType.
    // Each occurrence of DynamicType in type T is interpreted as the dynamic
    // type, a supertype of all types.
    if (other.IsDynamicClass()) {
      return true;
    }
    // In the case of a subtype test, each occurrence of DynamicType in type S
    // is interpreted as the bottom type, a subtype of all types.
    // However, DynamicType is not more specific than any type.
    if (thsi.IsDynamicClass()) {
      return test_kind == Class::kIsSubtypeOf;
    }
    // Check for NullType, which is only a subtype of ObjectType, of
    // DynamicType, or of itself, and which is more specific than any type.
    if (thsi.IsNullClass()) {
      // We already checked for other.IsDynamicClass() above.
      return (test_kind == Class::kIsMoreSpecificThan) ||
      other.IsObjectClass() || other.IsNullClass();
    }
    // Check for ObjectType. Any type that is not NullType or DynamicType
    // (already checked above), is more specific than ObjectType.
    if (other.IsObjectClass()) {
      return true;
    }
    // Check for reflexivity.
    if (thsi.raw() == other.raw()) {
      const intptr_t len = thsi.NumTypeArguments();
      if (len == 0) {
        return true;
      }
      // Since we do not truncate the type argument vector of a subclass (see
      // below), we only check a prefix of the proper length.
      // Check for covariance.
      if (other_type_arguments.IsNull() || other_type_arguments.IsRaw(len)) {
        return true;
      }
      if (type_arguments.IsNull() || type_arguments.IsRaw(len)) {
        // Other type can't be more specific than this one because for that
        // it would have to have all dynamic type arguments which is checked
        // above.
        return test_kind == Class::kIsSubtypeOf;
      }
      return type_arguments.TypeTest(test_kind,
                                     other_type_arguments,
                                     len,
                                     bound_error);
    }
    const bool other_is_function_class = other.IsFunctionClass();
    if (other.IsSignatureClass() || other_is_function_class) {
      const Function& other_fun = Function::Handle(other.signature_function());
      if (thsi.IsSignatureClass()) {
        if (other_is_function_class) {
          return true;
        }
        // Check for two function types.
        const Function& fun = Function::Handle(thsi.signature_function());
        return fun.TypeTest(test_kind,
                            type_arguments,
                            other_fun,
                            other_type_arguments,
                            bound_error);
      }
      // Check if type S has a call() method of function type T.
      Function& function =
      Function::Handle(thsi.LookupDynamicFunction(Symbols::Call()));
      if (function.IsNull()) {
        // Walk up the super_class chain.
        Class& cls = Class::Handle(thsi.SuperClass());
        while (!cls.IsNull() && function.IsNull()) {
          function = cls.LookupDynamicFunction(Symbols::Call());
          cls = cls.SuperClass();
        }
      }
      if (!function.IsNull()) {
        if (other_is_function_class ||
            function.TypeTest(test_kind,
                              type_arguments,
                              other_fun,
                              other_type_arguments,
                              bound_error)) {
              return true;
            }
      }
    }
    // Check for 'direct super type' specified in the implements clause
    // and check for transitivity at the same time.
    Array& interfaces = Array::Handle(thsi.interfaces());
    AbstractType& interface = AbstractType::Handle();
    Class& interface_class = Class::Handle();
    AbstractTypeArguments& interface_args = AbstractTypeArguments::Handle();
    Error& error = Error::Handle();
    for (intptr_t i = 0; i < interfaces.Length(); i++) {
      interface ^= interfaces.At(i);
      if (!interface.IsFinalized()) {
        // We may be checking bounds at finalization time. Skipping this
        // unfinalized interface will postpone bound checking to run time.
        continue;
      }
      error = Error::null();
      if (interface.IsMalboundedWithError(&error)) {
        // Return the first bound error to the caller if it requests it.
        if ((bound_error != NULL) && bound_error->IsNull()) {
          ASSERT(!error.IsNull());
          *bound_error = error.raw();
        }
        continue;  // Another interface may work better.
      }
      interface_class = interface.type_class();
      interface_args = interface.arguments();
      if (!interface_args.IsNull() && !interface_args.IsInstantiated()) {
        // This type class implements an interface that is parameterized with
        // generic type(s), e.g. it implements List<T>.
        // The uninstantiated type T must be instantiated using the type
        // parameters of this type before performing the type test.
        // The type arguments of this type that are referred to by the type
        // parameters of the interface are at the end of the type vector,
        // after the type arguments of the super type of this type.
        // The index of the type parameters is adjusted upon finalization.
        error = Error::null();
        interface_args = interface_args.InstantiateFrom(type_arguments, &error);
        if (!error.IsNull()) {
          // Return the first bound error to the caller if it requests it.
          if ((bound_error != NULL) && bound_error->IsNull()) {
            *bound_error = error.raw();
          }
          continue;  // Another interface may work better.
        }
      }
      if (interface_class.TypeTest(test_kind,
                                   interface_args,
                                   other,
                                   other_type_arguments,
                                   bound_error)) {
        return true;
      }
    }
    // "Recurse" up the class hierarchy until we have reached the top.
    thsi = thsi.SuperClass();
    if (thsi.IsNull()) {
      return false;
    }
  }
  UNREACHABLE();
  return false;
}


// If test_kind == kIsSubtypeOf, checks if type S is a subtype of type T.
// If test_kind == kIsMoreSpecificThan, checks if S is more specific than T.
// Type S is specified by this class parameterized with 'type_arguments', and
// type T by class 'other' parameterized with 'other_type_arguments'.
// This class and class 'other' do not need to be finalized, however, they must
// be resolved as well as their interfaces.
bool Class::TypeTest(
                     TypeTestKind test_kind,
                     const AbstractTypeArguments& type_arguments,
                     const Class& other,
                     const AbstractTypeArguments& other_type_arguments,
                     Error* bound_error) const {
  return TypeTestNonRecursive(*this,
                              test_kind,
                              type_arguments,
                              other,
                              other_type_arguments,
                              bound_error);
}


bool Class::IsTopLevel() const {
  return Name() == Symbols::TopLevel().raw();
}


RawFunction* Class::LookupDynamicFunction(const String& name) const {
  return LookupFunction(name, kInstance);
}


RawFunction* Class::LookupDynamicFunctionAllowPrivate(
    const String& name) const {
  return LookupFunctionAllowPrivate(name, kInstance);
}


RawFunction* Class::LookupStaticFunction(const String& name) const {
  return LookupFunction(name, kStatic);
}


RawFunction* Class::LookupStaticFunctionAllowPrivate(const String& name) const {
  return LookupFunctionAllowPrivate(name, kStatic);
}


RawFunction* Class::LookupConstructor(const String& name) const {
  return LookupFunction(name, kConstructor);
}


RawFunction* Class::LookupConstructorAllowPrivate(const String& name) const {
  return LookupFunctionAllowPrivate(name, kConstructor);
}


RawFunction* Class::LookupFactory(const String& name) const {
  return LookupFunction(name, kFactory);
}


RawFunction* Class::LookupFunction(const String& name) const {
  return LookupFunction(name, kAny);
}


RawFunction* Class::LookupFunctionAllowPrivate(const String& name) const {
  return LookupFunctionAllowPrivate(name, kAny);
}


// Returns true if 'prefix' and 'accessor_name' match 'name'.
static bool MatchesAccessorName(const String& name,
                                const char* prefix,
                                intptr_t prefix_length,
                                const String& accessor_name) {
  intptr_t name_len = name.Length();
  intptr_t accessor_name_len = accessor_name.Length();

  if (name_len != (accessor_name_len + prefix_length)) {
    return false;
  }
  for (intptr_t i = 0; i < prefix_length; i++) {
    if (name.CharAt(i) != prefix[i]) {
      return false;
    }
  }
  for (intptr_t i = 0, j = prefix_length; i < accessor_name_len; i++, j++) {
    if (name.CharAt(j) != accessor_name.CharAt(i)) {
      return false;
    }
  }
  return true;
}


RawFunction* Class::CheckFunctionType(const Function& func, intptr_t type) {
  if (type == kInstance) {
    if (func.IsDynamicFunction()) {
      return func.raw();
    }
  } else if (type == kStatic) {
    if (func.IsStaticFunction()) {
      return func.raw();
    }
  } else if (type == kConstructor) {
    if (func.IsConstructor()) {
      ASSERT(!func.is_static());
      return func.raw();
    }
  } else if (type == kFactory) {
    if (func.IsFactory()) {
      ASSERT(func.is_static());
      return func.raw();
    }
  } else if (type == kAny) {
    return func.raw();
  }
  return Function::null();
}


RawFunction* Class::LookupFunction(const String& name, intptr_t type) const {
  Isolate* isolate = Isolate::Current();
  if (EnsureIsFinalized(isolate) != Error::null()) {
    return Function::null();
  }
  ReusableHandleScope reused_handles(isolate);
  Array& funcs = reused_handles.ArrayHandle();
  funcs ^= functions();
  ASSERT(!funcs.IsNull());
  Function& function = reused_handles.FunctionHandle();
  const intptr_t len = funcs.Length();
  if (name.IsSymbol()) {
    // Quick Symbol compare.
    NoGCScope no_gc;
    for (intptr_t i = 0; i < len; i++) {
      function ^= funcs.At(i);
      if (function.name() == name.raw()) {
        return CheckFunctionType(function, type);
      }
    }
  } else {
    String& function_name = reused_handles.StringHandle();
    for (intptr_t i = 0; i < len; i++) {
      function ^= funcs.At(i);
      function_name ^= function.name();
      if (function_name.Equals(name)) {
        return CheckFunctionType(function, type);
      }
    }
  }
  // No function found.
  return Function::null();
}


RawFunction* Class::LookupFunctionAllowPrivate(const String& name,
                                               intptr_t type) const {
  Isolate* isolate = Isolate::Current();
  if (EnsureIsFinalized(isolate) != Error::null()) {
    return Function::null();
  }
  ReusableHandleScope reused_handles(isolate);
  Array& funcs = reused_handles.ArrayHandle();
  funcs ^= functions();
  ASSERT(!funcs.IsNull());
  Function& function = reused_handles.FunctionHandle();
  String& function_name = reused_handles.StringHandle();
  intptr_t len = funcs.Length();
  for (intptr_t i = 0; i < len; i++) {
    function ^= funcs.At(i);
    function_name ^= function.name();
    if (String::EqualsIgnoringPrivateKey(function_name, name)) {
      return CheckFunctionType(function, type);
    }
  }
  // No function found.
  return Function::null();
}


RawFunction* Class::LookupGetterFunction(const String& name) const {
  return LookupAccessorFunction(kGetterPrefix, kGetterPrefixLength, name);
}


RawFunction* Class::LookupSetterFunction(const String& name) const {
  return LookupAccessorFunction(kSetterPrefix, kSetterPrefixLength, name);
}


RawFunction* Class::LookupAccessorFunction(const char* prefix,
                                           intptr_t prefix_length,
                                           const String& name) const {
  Isolate* isolate = Isolate::Current();
  if (EnsureIsFinalized(isolate) != Error::null()) {
    return Function::null();
  }
  ReusableHandleScope reused_handles(isolate);
  Array& funcs = reused_handles.ArrayHandle();
  funcs ^= functions();
  Function& function = reused_handles.FunctionHandle();
  String& function_name = reused_handles.StringHandle();
  intptr_t len = funcs.Length();
  for (intptr_t i = 0; i < len; i++) {
    function ^= funcs.At(i);
    function_name ^= function.name();
    if (MatchesAccessorName(function_name, prefix, prefix_length, name)) {
      return function.raw();
    }
  }

  // No function found.
  return Function::null();
}


RawFunction* Class::LookupFunctionAtToken(intptr_t token_pos) const {
  // TODO(hausner): we can shortcut the negative case if we knew the
  // beginning and end token position of the class.
  Isolate* isolate = Isolate::Current();
  if (EnsureIsFinalized(isolate) != Error::null()) {
    return Function::null();
  }
  Function& func = Function::Handle(isolate);
  func = LookupClosureFunction(token_pos);
  if (!func.IsNull()) {
    return func.raw();
  }
  Array& funcs = Array::Handle(isolate, functions());
  intptr_t len = funcs.Length();
  for (intptr_t i = 0; i < len; i++) {
    func ^= funcs.At(i);
    if ((func.token_pos() <= token_pos) &&
        (token_pos <= func.end_token_pos())) {
      return func.raw();
    }
  }
  // No function found.
  return Function::null();
}


RawField* Class::LookupInstanceField(const String& name) const {
  return LookupField(name, kInstance);
}


RawField* Class::LookupStaticField(const String& name) const {
  return LookupField(name, kStatic);
}


RawField* Class::LookupField(const String& name) const {
  return LookupField(name, kAny);
}


RawField* Class::LookupField(const String& name, intptr_t type) const {
  Isolate* isolate = Isolate::Current();
  if (EnsureIsFinalized(isolate) != Error::null()) {
    return Field::null();
  }
  ReusableHandleScope reused_handles(isolate);
  Array& flds = reused_handles.ArrayHandle();
  flds ^= fields();
  ASSERT(!flds.IsNull());
  Field& field = reused_handles.FieldHandle();
  String& field_name = reused_handles.StringHandle();
  intptr_t len = flds.Length();
  for (intptr_t i = 0; i < len; i++) {
    field ^= flds.At(i);
    field_name ^= field.name();
    if (String::EqualsIgnoringPrivateKey(field_name, name)) {
      if (type == kInstance) {
        if (!field.is_static()) {
          return field.raw();
        }
      } else if (type == kStatic) {
        if (field.is_static()) {
          return field.raw();
        }
      } else if (type == kAny) {
        return field.raw();
      }
      return Field::null();
    }
  }
  // No field found.
  return Field::null();
}


RawLibraryPrefix* Class::LookupLibraryPrefix(const String& name) const {
  Isolate* isolate = Isolate::Current();
  const Library& lib = Library::Handle(isolate, library());
  const Object& obj = Object::Handle(isolate, lib.LookupLocalObject(name));
  if (!obj.IsNull() && obj.IsLibraryPrefix()) {
    return LibraryPrefix::Cast(obj).raw();
  }
  return LibraryPrefix::null();
}


const char* Class::ToCString() const {
  const char* format = "%s Class: %s";
  const Library& lib = Library::Handle(library());
  const char* library_name = lib.IsNull() ? "" : lib.ToCString();
  const char* class_name = String::Handle(Name()).ToCString();
  intptr_t len = OS::SNPrint(NULL, 0, format, library_name, class_name) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, library_name, class_name);
  return chars;
}


void Class::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
  if ((raw() == Class::null()) || (id() == kFreeListElement)) {
    jsobj.AddProperty("type", "Null");
    return;
  }
  const char* internal_class_name = String::Handle(Name()).ToCString();
  const char* user_visible_class_name =
      String::Handle(UserVisibleName()).ToCString();
  jsobj.AddProperty("type", JSONType(ref));
  jsobj.AddProperty("id", id());
  jsobj.AddProperty("name", internal_class_name);
  jsobj.AddProperty("user_name", user_visible_class_name);
  if (!ref) {
    jsobj.AddProperty("implemented", is_implemented());
    jsobj.AddProperty("abstract", is_abstract());
    jsobj.AddProperty("patch", is_patch());
    jsobj.AddProperty("finalized", is_finalized());
    jsobj.AddProperty("const", is_const());
    jsobj.AddProperty("super", Class::Handle(SuperClass()));
    jsobj.AddProperty("library", Object::Handle(library()));
    {
      JSONArray fields_array(&jsobj, "fields");
      const Array& field_array = Array::Handle(fields());
      Field& field = Field::Handle();
      if (!field_array.IsNull()) {
        for (intptr_t i = 0; i < field_array.Length(); ++i) {
          field ^= field_array.At(i);
          fields_array.AddValue(field);
        }
      }
    }
    {
      JSONArray functions_array(&jsobj, "functions");
      const Array& function_array = Array::Handle(functions());
      Function& function = Function::Handle();
      if (!function_array.IsNull()) {
        for (intptr_t i = 0; i < function_array.Length(); i++) {
          function ^= function_array.At(i);
          functions_array.AddValue(function);
        }
      }
    }
  }
}


void Class::InsertCanonicalConstant(intptr_t index,
                                    const Instance& constant) const {
  // The constant needs to be added to the list. Grow the list if it is full.
  Array& canonical_list = Array::Handle(constants());
  const intptr_t list_len = canonical_list.Length();
  if (index >= list_len) {
    const intptr_t new_length = (list_len == 0) ? 4 : list_len + 4;
    const Array& new_canonical_list =
        Array::Handle(Array::Grow(canonical_list, new_length, Heap::kOld));
    set_constants(new_canonical_list);
    new_canonical_list.SetAt(index, constant);
  } else {
    canonical_list.SetAt(index, constant);
  }
}


RawUnresolvedClass* UnresolvedClass::New(const LibraryPrefix& library_prefix,
                                         const String& ident,
                                         intptr_t token_pos) {
  const UnresolvedClass& type = UnresolvedClass::Handle(UnresolvedClass::New());
  type.set_library_prefix(library_prefix);
  type.set_ident(ident);
  type.set_token_pos(token_pos);
  return type.raw();
}


RawUnresolvedClass* UnresolvedClass::New() {
  ASSERT(Object::unresolved_class_class() != Class::null());
  RawObject* raw = Object::Allocate(UnresolvedClass::kClassId,
                                    UnresolvedClass::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawUnresolvedClass*>(raw);
}


void UnresolvedClass::set_token_pos(intptr_t token_pos) const {
  ASSERT(token_pos >= 0);
  raw_ptr()->token_pos_ = token_pos;
}


void UnresolvedClass::set_ident(const String& ident) const {
  StorePointer(&raw_ptr()->ident_, ident.raw());
}


void UnresolvedClass::set_library_prefix(
    const LibraryPrefix& library_prefix) const {
  StorePointer(&raw_ptr()->library_prefix_, library_prefix.raw());
}


RawString* UnresolvedClass::Name() const {
  if (library_prefix() != LibraryPrefix::null()) {
    const LibraryPrefix& lib_prefix = LibraryPrefix::Handle(library_prefix());
    String& name = String::Handle();
    name = lib_prefix.name();  // Qualifier.
    name = String::Concat(name, Symbols::Dot());
    const String& str = String::Handle(ident());
    name = String::Concat(name, str);
    return name.raw();
  } else {
    return ident();
  }
}


const char* UnresolvedClass::ToCString() const {
  const char* format = "unresolved class '%s'";
  const char* cname =  String::Handle(Name()).ToCString();
  intptr_t len = OS::SNPrint(NULL, 0, format, cname) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, cname);
  return chars;
}


void UnresolvedClass::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


intptr_t AbstractTypeArguments::Length() const  {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return -1;
}


RawAbstractType* AbstractTypeArguments::TypeAt(intptr_t index) const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return NULL;
}


void AbstractTypeArguments::SetTypeAt(intptr_t index,
                                      const AbstractType& value) const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
}


bool AbstractTypeArguments::IsResolved() const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractTypeArguments::IsInstantiated() const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractTypeArguments::IsUninstantiatedIdentity() const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractTypeArguments::CanShareInstantiatorTypeArguments(
      const Class& instantiator_class) const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractTypeArguments::IsBounded() const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return false;
}


static intptr_t FinalizeHash(uword hash) {
  hash += hash << 3;
  hash ^= hash >> 11;
  hash += hash << 15;
  return hash;
}


intptr_t AbstractTypeArguments::Hash() const {
  if (IsNull()) return 0;
  uword result = 0;
  const intptr_t num_types = Length();
  AbstractType& type = AbstractType::Handle();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    result += type.Hash();
    result += result << 10;
    result ^= result >> 6;
  }
  return FinalizeHash(result);
}


RawString* AbstractTypeArguments::SubvectorName(
    intptr_t from_index,
    intptr_t len,
    NameVisibility name_visibility) const {
  ASSERT(from_index + len <= Length());
  String& name = String::Handle();
  const intptr_t num_strings = 2*len + 1;  // "<""T"", ""T"">".
  const Array& strings = Array::Handle(Array::New(num_strings));
  intptr_t s = 0;
  strings.SetAt(s++, Symbols::LAngleBracket());
  AbstractType& type = AbstractType::Handle();
  for (intptr_t i = 0; i < len; i++) {
    type = TypeAt(from_index + i);
    name = type.BuildName(name_visibility);
    strings.SetAt(s++, name);
    if (i < len - 1) {
      strings.SetAt(s++, Symbols::CommaSpace());
    }
  }
  strings.SetAt(s++, Symbols::RAngleBracket());
  ASSERT(s == num_strings);
  name = String::ConcatAll(strings);
  return Symbols::New(name);
}


bool AbstractTypeArguments::Equals(const AbstractTypeArguments& other) const {
  ASSERT(!IsNull());  // Use AbstractTypeArguments::AreEqual().
  if (this->raw() == other.raw()) {
    return true;
  }
  if (other.IsNull()) {
    return false;
  }
  const intptr_t num_types = Length();
  if (num_types != other.Length()) {
    return false;
  }
  AbstractType& type = AbstractType::Handle();
  AbstractType& other_type = AbstractType::Handle();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    other_type = other.TypeAt(i);
    if (!type.Equals(other_type)) {
      return false;
    }
  }
  return true;
}


bool AbstractTypeArguments::AreEqual(
    const AbstractTypeArguments& arguments,
    const AbstractTypeArguments& other_arguments) {
  if (arguments.raw() == other_arguments.raw()) {
    return true;
  }
  if (arguments.IsNull()) {
    return other_arguments.IsDynamicTypes(false, other_arguments.Length());
  }
  if (other_arguments.IsNull()) {
    return arguments.IsDynamicTypes(false, arguments.Length());
  }
  return arguments.Equals(other_arguments);
}


RawAbstractTypeArguments* AbstractTypeArguments::InstantiateFrom(
    const AbstractTypeArguments& instantiator_type_arguments,
    Error* bound_error) const {
  // AbstractTypeArguments is an abstract class.
  UNREACHABLE();
  return NULL;
}


bool AbstractTypeArguments::IsDynamicTypes(bool raw_instantiated,
                                           intptr_t len) const {
  ASSERT(Length() >= len);
  AbstractType& type = AbstractType::Handle();
  Class& type_class = Class::Handle();
  for (intptr_t i = 0; i < len; i++) {
    type = TypeAt(i);
    ASSERT(!type.IsNull());
    if (!type.HasResolvedTypeClass()) {
      if (raw_instantiated && type.IsTypeParameter()) {
        // An uninstantiated type parameter is equivalent to dynamic (even in
        // the presence of a malformed bound in checked mode).
        continue;
      }
      ASSERT((!raw_instantiated && type.IsTypeParameter()) ||
             type.IsBoundedType() ||
             type.IsMalformed());
      return false;
    }
    type_class = type.type_class();
    if (!type_class.IsDynamicClass()) {
      return false;
    }
  }
  return true;
}


bool AbstractTypeArguments::TypeTest(TypeTestKind test_kind,
                                     const AbstractTypeArguments& other,
                                     intptr_t len,
                                     Error* bound_error) const {
  ASSERT(Length() >= len);
  ASSERT(!other.IsNull());
  ASSERT(other.Length() >= len);
  AbstractType& type = AbstractType::Handle();
  AbstractType& other_type = AbstractType::Handle();
  for (intptr_t i = 0; i < len; i++) {
    type = TypeAt(i);
    ASSERT(!type.IsNull());
    other_type = other.TypeAt(i);
    ASSERT(!other_type.IsNull());
    if (!type.TypeTest(test_kind, other_type, bound_error)) {
      return false;
    }
  }
  return true;
}


const char* AbstractTypeArguments::ToCString() const {
  // AbstractTypeArguments is an abstract class, valid only for representing
  // null.
  if (IsNull()) {
    return "NULL AbstractTypeArguments";
  }
  UNREACHABLE();
  return "AbstractTypeArguments";
}


void AbstractTypeArguments::PrintToJSONStream(JSONStream* stream,
                                              bool ref) const {
  JSONObject jsobj(stream);
}


intptr_t TypeArguments::Length() const {
  ASSERT(!IsNull());
  return Smi::Value(raw_ptr()->length_);
}


RawAbstractType* TypeArguments::TypeAt(intptr_t index) const {
  return *TypeAddr(index);
}


void TypeArguments::SetTypeAt(intptr_t index, const AbstractType& value) const {
  ASSERT(!IsCanonical());
  StorePointer(TypeAddr(index), value.raw());
}


bool TypeArguments::IsResolved() const {
  AbstractType& type = AbstractType::Handle();
  const intptr_t num_types = Length();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    if (!type.IsResolved()) {
      return false;
    }
  }
  return true;
}


bool TypeArguments::IsInstantiated() const {
  AbstractType& type = AbstractType::Handle();
  const intptr_t num_types = Length();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    ASSERT(!type.IsNull());
    if (!type.IsInstantiated()) {
      return false;
    }
  }
  return true;
}


bool TypeArguments::IsUninstantiatedIdentity() const {
  ASSERT(!IsInstantiated());
  AbstractType& type = AbstractType::Handle();
  const intptr_t num_types = Length();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    if (!type.IsTypeParameter()) {
      return false;
    }
    const TypeParameter& type_param = TypeParameter::Cast(type);
    ASSERT(type_param.IsFinalized());
    if ((type_param.index() != i)) {
      return false;
    }
    // If this type parameter specifies an upper bound, then the type argument
    // vector does not really represent the identity vector. It cannot be
    // substituted by the instantiator's type argument vector without checking
    // the upper bound.
    const AbstractType& bound = AbstractType::Handle(type_param.bound());
    ASSERT(bound.IsResolved());
    if (!bound.IsObjectType() && !bound.IsDynamicType()) {
      return false;
    }
  }
  return true;
  // Note that it is not necessary to verify at runtime that the instantiator
  // type vector is long enough, since this uninstantiated vector contains as
  // many different type parameters as it is long.
}


// Return true if this uninstantiated type argument vector, once instantiated
// at runtime, is a prefix of the type argument vector of its instantiator.
bool TypeArguments::CanShareInstantiatorTypeArguments(
      const Class& instantiator_class) const {
  ASSERT(!IsInstantiated());
  const intptr_t num_type_args = Length();
  const intptr_t num_instantiator_type_args =
      instantiator_class.NumTypeArguments();
  if (num_type_args > num_instantiator_type_args) {
    // This vector cannot be a prefix of a shorter vector.
    return false;
  }
  const intptr_t num_instantiator_type_params =
      instantiator_class.NumTypeParameters();
  const intptr_t first_type_param_offset =
      num_instantiator_type_args - num_instantiator_type_params;
  // At compile time, the type argument vector of the instantiator consists of
  // the type argument vector of its super type, which may refer to the type
  // parameters of the instantiator class, followed by (or overlapping partially
  // or fully with) the type parameters of the instantiator class in declaration
  // order.
  // In other words, the only variables are the type parameters of the
  // instantiator class.
  // This uninstantiated type argument vector is also expressed in terms of the
  // type parameters of the instantiator class. Therefore, in order to be a
  // prefix once instantiated at runtime, every one of its type argument must be
  // equal to the type argument of the instantiator vector at the same index.

  // As a first requirement, the last num_instantiator_type_params type
  // arguments of this type argument vector must refer to the corresponding type
  // parameters of the instantiator class.
  AbstractType& type_arg = AbstractType::Handle();
  for (intptr_t i = first_type_param_offset; i < num_type_args; i++) {
    type_arg = TypeAt(i);
    if (!type_arg.IsTypeParameter()) {
      return false;
    }
    const TypeParameter& type_param = TypeParameter::Cast(type_arg);
    ASSERT(type_param.IsFinalized());
    if ((type_param.index() != i)) {
      return false;
    }
  }
  // As a second requirement, the type arguments corresponding to the super type
  // must be identical. Overlapping ones have already been checked starting at
  // first_type_param_offset.
  if (first_type_param_offset == 0) {
    return true;
  }
  AbstractType& super_type = AbstractType::Handle(
      instantiator_class.super_type());
  const AbstractTypeArguments& super_type_args = AbstractTypeArguments::Handle(
      super_type.arguments());
  if (super_type_args.IsNull()) {
    return false;
  }
  AbstractType& super_type_arg = AbstractType::Handle();
  for (intptr_t i = 0;
       (i < first_type_param_offset) && (i < num_type_args); i++) {
    type_arg = TypeAt(i);
    super_type_arg = super_type_args.TypeAt(i);
    if (!type_arg.Equals(super_type_arg)) {
      return false;
    }
  }
  return true;
}


bool TypeArguments::IsFinalized() const {
  ASSERT(!IsNull());
  AbstractType& type = AbstractType::Handle();
  const intptr_t num_types = Length();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    if (!type.IsFinalized()) {
      return false;
    }
  }
  return true;
}


bool TypeArguments::IsBounded() const {
  AbstractType& type = AbstractType::Handle();
  const intptr_t num_types = Length();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    if (type.IsBoundedType()) {
      return true;
    }
    if (type.IsTypeParameter()) {
      const AbstractType& bound = AbstractType::Handle(
          TypeParameter::Cast(type).bound());
      if (!bound.IsObjectType() && !bound.IsDynamicType()) {
        return true;
      }
      continue;
    }
    const AbstractTypeArguments& type_args = AbstractTypeArguments::Handle(
        Type::Cast(type).arguments());
    if (!type_args.IsNull() && type_args.IsBounded()) {
      return true;
    }
  }
  return false;
}


RawAbstractTypeArguments* TypeArguments::InstantiateFrom(
    const AbstractTypeArguments& instantiator_type_arguments,
    Error* bound_error) const {
  ASSERT(!IsInstantiated());
  if (!instantiator_type_arguments.IsNull() &&
      IsUninstantiatedIdentity() &&
      (instantiator_type_arguments.Length() == Length())) {
    return instantiator_type_arguments.raw();
  }
  const intptr_t num_types = Length();
  TypeArguments& instantiated_array =
      TypeArguments::Handle(TypeArguments::New(num_types, Heap::kNew));
  AbstractType& type = AbstractType::Handle();
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    if (!type.IsInstantiated()) {
      type = type.InstantiateFrom(instantiator_type_arguments, bound_error);
    }
    instantiated_array.SetTypeAt(i, type);
  }
  return instantiated_array.raw();
}


RawTypeArguments* TypeArguments::New(intptr_t len, Heap::Space space) {
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in TypeArguments::New: invalid len %" Pd "\n", len);
  }
  TypeArguments& result = TypeArguments::Handle();
  {
    RawObject* raw = Object::Allocate(TypeArguments::kClassId,
                                      TypeArguments::InstanceSize(len),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    // Length must be set before we start storing into the array.
    result.SetLength(len);
  }
  return result.raw();
}



RawAbstractType** TypeArguments::TypeAddr(intptr_t index) const {
  // TODO(iposva): Determine if we should throw an exception here.
  ASSERT((index >= 0) && (index < Length()));
  return &raw_ptr()->types_[index];
}


void TypeArguments::SetLength(intptr_t value) const {
  ASSERT(!IsCanonical());
  // This is only safe because we create a new Smi, which does not cause
  // heap allocation.
  raw_ptr()->length_ = Smi::New(value);
}


static void GrowCanonicalTypeArguments(Isolate* isolate, const Array& table) {
  // Last element of the array is the number of used elements.
  intptr_t table_size = table.Length() - 1;
  intptr_t new_table_size = table_size * 2;
  Array& new_table = Array::Handle(isolate, Array::New(new_table_size + 1));
  // Copy all elements from the original table to the newly allocated
  // array.
  TypeArguments& element = TypeArguments::Handle(isolate);
  Object& new_element = Object::Handle(isolate);
  for (intptr_t i = 0; i < table_size; i++) {
    element ^= table.At(i);
    if (!element.IsNull()) {
      intptr_t hash = element.Hash();
      ASSERT(Utils::IsPowerOfTwo(new_table_size));
      intptr_t index = hash & (new_table_size - 1);
      new_element = new_table.At(index);
      while (!new_element.IsNull()) {
        index = (index + 1) & (new_table_size - 1);  // Move to next element.
        new_element = new_table.At(index);
      }
      new_table.SetAt(index, element);
    }
  }
  // Copy used count.
  new_element = table.At(table_size);
  new_table.SetAt(new_table_size, new_element);
  // Remember the new table now.
  isolate->object_store()->set_canonical_type_arguments(new_table);
}


static void InsertIntoCanonicalTypeArguments(Isolate* isolate,
                                             const Array& table,
                                             const TypeArguments& arguments,
                                             intptr_t index) {
  arguments.SetCanonical();  // Mark object as being canonical.
  table.SetAt(index, arguments);  // Remember the new element.
  // Update used count.
  // Last element of the array is the number of used elements.
  intptr_t table_size = table.Length() - 1;
  intptr_t used_elements = Smi::Value(Smi::RawCast(table.At(table_size))) + 1;
  const Smi& used = Smi::Handle(isolate, Smi::New(used_elements));
  table.SetAt(table_size, used);

  // Rehash if table is 75% full.
  if (used_elements > ((table_size / 4) * 3)) {
    GrowCanonicalTypeArguments(isolate, table);
  }
}


static intptr_t FindIndexInCanonicalTypeArguments(
    Isolate* isolate,
    const Array& table,
    const TypeArguments& arguments,
    intptr_t hash) {
  // Last element of the array is the number of used elements.
  intptr_t table_size = table.Length() - 1;
  ASSERT(Utils::IsPowerOfTwo(table_size));
  intptr_t index = hash & (table_size - 1);

  TypeArguments& current = TypeArguments::Handle(isolate);
  current ^= table.At(index);
  while (!current.IsNull() && !current.Equals(arguments)) {
    index = (index + 1) & (table_size - 1);  // Move to next element.
    current ^= table.At(index);
  }
  return index;  // Index of element if found or slot into which to add it.
}


RawAbstractTypeArguments* TypeArguments::CloneUnfinalized() const {
  if (IsFinalized()) {
    return raw();
  }
  AbstractType& type = AbstractType::Handle();
  const intptr_t num_types = Length();
  const TypeArguments& clone = TypeArguments::Handle(
      TypeArguments::New(num_types));
  for (intptr_t i = 0; i < num_types; i++) {
    type = TypeAt(i);
    type = type.CloneUnfinalized();
    clone.SetTypeAt(i, type);
  }
  return clone.raw();
}


RawAbstractTypeArguments* TypeArguments::Canonicalize() const {
  if (IsNull() || IsCanonical()) {
    ASSERT(IsOld());
    return this->raw();
  }
  Isolate* isolate = Isolate::Current();
  ObjectStore* object_store = isolate->object_store();
  const Array& table = Array::Handle(isolate,
                                     object_store->canonical_type_arguments());
  ASSERT(table.Length() > 0);
  intptr_t index = FindIndexInCanonicalTypeArguments(isolate,
                                                     table,
                                                     *this,
                                                     Hash());
  TypeArguments& result = TypeArguments::Handle(isolate);
  result ^= table.At(index);
  if (result.IsNull()) {
    // Make sure we have an old space object and add it to the table.
    if (this->IsNew()) {
      result ^= Object::Clone(*this, Heap::kOld);
    } else {
      result ^= this->raw();
    }
    ASSERT(result.IsOld());
    InsertIntoCanonicalTypeArguments(isolate, table, result, index);
  }
  ASSERT(result.Equals(*this));
  ASSERT(!result.IsNull());
  ASSERT(result.IsTypeArguments());
  return result.raw();
}


const char* TypeArguments::ToCString() const {
  if (IsNull()) {
    return "NULL TypeArguments";
  }
  const char* format = "%s [%s]";
  const char* prev_cstr = "TypeArguments:";
  for (int i = 0; i < Length(); i++) {
    const AbstractType& type_at = AbstractType::Handle(TypeAt(i));
    const char* type_cstr = type_at.IsNull() ? "null" : type_at.ToCString();
    intptr_t len = OS::SNPrint(NULL, 0, format, prev_cstr, type_cstr) + 1;
    char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
    OS::SNPrint(chars, len, format, prev_cstr, type_cstr);
    prev_cstr = chars;
  }
  return prev_cstr;
}


void TypeArguments::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


intptr_t InstantiatedTypeArguments::Length() const {
  return AbstractTypeArguments::Handle(
      uninstantiated_type_arguments()).Length();
}


RawAbstractType* InstantiatedTypeArguments::TypeAt(intptr_t index) const {
  AbstractType& type = AbstractType::Handle(AbstractTypeArguments::Handle(
      uninstantiated_type_arguments()).TypeAt(index));
  if (!type.IsInstantiated()) {
    const AbstractTypeArguments& instantiator_type_args =
        AbstractTypeArguments::Handle(instantiator_type_arguments());
    Error& bound_error = Error::Handle();
    type = type.InstantiateFrom(instantiator_type_args, &bound_error);
    // InstantiatedTypeArguments cannot include unchecked bounds.
    // In the presence of unchecked bounds, no InstantiatedTypeArguments are
    // allocated, but the type arguments are instantiated individually and their
    // bounds are checked.
    ASSERT(bound_error.IsNull());
  }
  return type.raw();
}


void InstantiatedTypeArguments::SetTypeAt(intptr_t index,
                                          const AbstractType& value) const {
  // We only replace individual argument types during resolution at compile
  // time, when no type parameters are instantiated yet.
  UNREACHABLE();
}


void InstantiatedTypeArguments::set_uninstantiated_type_arguments(
    const AbstractTypeArguments& value) const {
  StorePointer(&raw_ptr()->uninstantiated_type_arguments_, value.raw());
}


void InstantiatedTypeArguments::set_instantiator_type_arguments(
    const AbstractTypeArguments& value) const {
  StorePointer(&raw_ptr()->instantiator_type_arguments_, value.raw());
}


RawInstantiatedTypeArguments* InstantiatedTypeArguments::New() {
  ASSERT(Object::instantiated_type_arguments_class() != Class::null());
  RawObject* raw = Object::Allocate(InstantiatedTypeArguments::kClassId,
                                    InstantiatedTypeArguments::InstanceSize(),
                                    Heap::kNew);
  return reinterpret_cast<RawInstantiatedTypeArguments*>(raw);
}


RawInstantiatedTypeArguments* InstantiatedTypeArguments::New(
    const AbstractTypeArguments& uninstantiated_type_arguments,
    const AbstractTypeArguments& instantiator_type_arguments) {
  const InstantiatedTypeArguments& result =
      InstantiatedTypeArguments::Handle(InstantiatedTypeArguments::New());
  result.set_uninstantiated_type_arguments(uninstantiated_type_arguments);
  result.set_instantiator_type_arguments(instantiator_type_arguments);
  return result.raw();
}


const char* InstantiatedTypeArguments::ToCString() const {
  if (IsNull()) {
    return "NULL InstantiatedTypeArguments";
  }
  const char* format = "InstantiatedTypeArguments: [%s] instantiator: [%s]";
  const char* arg_cstr =
      AbstractTypeArguments::Handle(
          uninstantiated_type_arguments()).ToCString();
  const char* instantiator_cstr =
      AbstractTypeArguments::Handle(instantiator_type_arguments()).ToCString();
  intptr_t len =
      OS::SNPrint(NULL, 0, format, arg_cstr, instantiator_cstr) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, arg_cstr, instantiator_cstr);
  return chars;
}


void InstantiatedTypeArguments::PrintToJSONStream(JSONStream* stream,
                                                  bool ref) const {
  JSONObject jsobj(stream);
}


const char* PatchClass::ToCString() const {
  const char* kFormat = "PatchClass for %s";
  const Class& cls = Class::Handle(patched_class());
  const char* cls_name = cls.ToCString();
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, cls_name) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, cls_name);
  return chars;
}


void PatchClass::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawPatchClass* PatchClass::New(const Class& patched_class,
                               const Class& source_class) {
  const PatchClass& result = PatchClass::Handle(PatchClass::New());
  result.set_patched_class(patched_class);
  result.set_source_class(source_class);
  return result.raw();
}


RawPatchClass* PatchClass::New() {
  ASSERT(Object::patch_class_class() != Class::null());
  RawObject* raw = Object::Allocate(PatchClass::kClassId,
                                    PatchClass::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawPatchClass*>(raw);
}


RawScript* PatchClass::Script() const {
  const Class& source_class = Class::Handle(this->source_class());
  return source_class.script();
}


void PatchClass::set_patched_class(const Class& value) const {
  StorePointer(&raw_ptr()->patched_class_, value.raw());
}


void PatchClass::set_source_class(const Class& value) const {
  StorePointer(&raw_ptr()->source_class_, value.raw());
}


bool Function::HasBreakpoint() const {
  return Isolate::Current()->debugger()->HasBreakpoint(*this);
}


void Function::SetCode(const Code& value) const {
  StorePointer(&raw_ptr()->code_, value.raw());
  ASSERT(Function::Handle(value.function()).IsNull() ||
    (value.function() == this->raw()));
  value.set_function(*this);
}


void Function::DetachCode() const {
  // Set unoptimized code as non-entrant, and set code and unoptimized code
  // to null.
  CodePatcher::PatchEntry(Code::Handle(unoptimized_code()));
  StorePointer(&raw_ptr()->code_, Code::null());
  StorePointer(&raw_ptr()->unoptimized_code_, Code::null());
}


void Function::ReattachCode(const Code& code) const {
  StorePointer(&raw_ptr()->code_, code.raw());
  StorePointer(&raw_ptr()->unoptimized_code_, code.raw());
  CodePatcher::RestoreEntry(code);
}


void Function::SwitchToUnoptimizedCode() const {
  ASSERT(HasOptimizedCode());

  const Code& current_code = Code::Handle(CurrentCode());

  // Optimized code object might have been actually fully produced by the
  // intrinsifier in this case nothing has to be done. In fact an attempt to
  // patch such code will cause crash.
  // TODO(vegorov): if intrisifier can fully intrinsify the function then we
  // should not later try to optimize it.
  if (PcDescriptors::Handle(current_code.pc_descriptors()).Length() == 0) {
    return;
  }

  if (FLAG_trace_disabling_optimized_code) {
    OS::Print("Disabling optimized code: '%s' entry: %#" Px "\n",
      ToFullyQualifiedCString(),
      current_code.EntryPoint());
  }
  // Patch entry of the optimized code.
  CodePatcher::PatchEntry(current_code);
  // Use previously compiled unoptimized code.
  SetCode(Code::Handle(unoptimized_code()));
  CodePatcher::RestoreEntry(Code::Handle(unoptimized_code()));
}


void Function::set_unoptimized_code(const Code& value) const {
  ASSERT(!value.is_optimized());
  StorePointer(&raw_ptr()->unoptimized_code_, value.raw());
}


RawContextScope* Function::context_scope() const {
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    return ClosureData::Cast(obj).context_scope();
  }
  return ContextScope::null();
}


void Function::set_context_scope(const ContextScope& value) const {
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    ClosureData::Cast(obj).set_context_scope(value);
    return;
  }
  UNREACHABLE();
}


RawInstance* Function::implicit_static_closure() const {
  if (IsImplicitStaticClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    return ClosureData::Cast(obj).implicit_static_closure();
  }
  return Instance::null();
}


void Function::set_implicit_static_closure(const Instance& closure) const {
  if (IsImplicitStaticClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    ClosureData::Cast(obj).set_implicit_static_closure(closure);
    return;
  }
  UNREACHABLE();
}


RawCode* Function::closure_allocation_stub() const {
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    return ClosureData::Cast(obj).closure_allocation_stub();
  }
  return Code::null();
}


void Function::set_closure_allocation_stub(const Code& value) const {
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    ClosureData::Cast(obj).set_closure_allocation_stub(value);
    return;
  }
  UNREACHABLE();
}


RawFunction* Function::extracted_method_closure() const {
  ASSERT(kind() == RawFunction::kMethodExtractor);
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(obj.IsFunction());
  return Function::Cast(obj).raw();
}


void Function::set_extracted_method_closure(const Function& value) const {
  ASSERT(kind() == RawFunction::kMethodExtractor);
  ASSERT(raw_ptr()->data_ == Object::null());
  set_data(value);
}


RawArray* Function::saved_args_desc() const {
  ASSERT(kind() == RawFunction::kNoSuchMethodDispatcher ||
         kind() == RawFunction::kInvokeFieldDispatcher);
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(obj.IsArray());
  return Array::Cast(obj).raw();
}


void Function::set_saved_args_desc(const Array& value) const {
  ASSERT(kind() == RawFunction::kNoSuchMethodDispatcher ||
         kind() == RawFunction::kInvokeFieldDispatcher);
  ASSERT(raw_ptr()->data_ == Object::null());
  set_data(value);
}


RawField* Function::saved_static_field() const {
  ASSERT(kind() == RawFunction::kStaticInitializer);
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(obj.IsField());
  return Field::Cast(obj).raw();
}


void Function::set_saved_static_field(const Field& value) const {
  ASSERT(kind() == RawFunction::kStaticInitializer);
  ASSERT(raw_ptr()->data_ == Object::null());
  set_data(value);
}


RawFunction* Function::parent_function() const {
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    return ClosureData::Cast(obj).parent_function();
  }
  return Function::null();
}


void Function::set_parent_function(const Function& value) const {
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    ClosureData::Cast(obj).set_parent_function(value);
    return;
  }
  UNREACHABLE();
}


RawFunction* Function::implicit_closure_function() const {
  if (IsClosureFunction() ||
      IsSignatureFunction() ||
      IsStaticInitializerFunction()) {
    return Function::null();
  }
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(obj.IsNull() || obj.IsFunction());
  return (obj.IsNull()) ? Function::null() : Function::Cast(obj).raw();
}


void Function::set_implicit_closure_function(const Function& value) const {
  ASSERT(!IsClosureFunction() && !IsSignatureFunction());
  set_data(value);
}


RawClass* Function::signature_class() const {
  if (IsSignatureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(obj.IsNull() || obj.IsClass());
    return (obj.IsNull()) ? Class::null() : Class::Cast(obj).raw();
  }
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    return ClosureData::Cast(obj).signature_class();
  }
  return Class::null();
}


void Function::set_signature_class(const Class& value) const {
  if (IsSignatureFunction()) {
    set_data(value);
    return;
  }
  if (IsClosureFunction()) {
    const Object& obj = Object::Handle(raw_ptr()->data_);
    ASSERT(!obj.IsNull());
    ClosureData::Cast(obj).set_signature_class(value);
    return;
  }
  UNREACHABLE();
}


bool Function::IsRedirectingFactory() const {
  if (!IsFactory() || (raw_ptr()->data_ == Object::null())) {
    return false;
  }
  ASSERT(!IsClosureFunction());  // A factory cannot also be a closure.
  return true;
}


RawType* Function::RedirectionType() const {
  ASSERT(IsRedirectingFactory());
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(!obj.IsNull());
  return RedirectionData::Cast(obj).type();
}


void Function::SetRedirectionType(const Type& type) const {
  ASSERT(IsFactory());
  Object& obj = Object::Handle(raw_ptr()->data_);
  if (obj.IsNull()) {
    obj = RedirectionData::New();
    set_data(obj);
  }
  RedirectionData::Cast(obj).set_type(type);
}


RawString* Function::RedirectionIdentifier() const {
  ASSERT(IsRedirectingFactory());
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(!obj.IsNull());
  return RedirectionData::Cast(obj).identifier();
}


void Function::SetRedirectionIdentifier(const String& identifier) const {
  ASSERT(IsFactory());
  Object& obj = Object::Handle(raw_ptr()->data_);
  if (obj.IsNull()) {
    obj = RedirectionData::New();
    set_data(obj);
  }
  RedirectionData::Cast(obj).set_identifier(identifier);
}


RawFunction* Function::RedirectionTarget() const {
  ASSERT(IsRedirectingFactory());
  const Object& obj = Object::Handle(raw_ptr()->data_);
  ASSERT(!obj.IsNull());
  return RedirectionData::Cast(obj).target();
}


void Function::SetRedirectionTarget(const Function& target) const {
  ASSERT(IsFactory());
  Object& obj = Object::Handle(raw_ptr()->data_);
  if (obj.IsNull()) {
    obj = RedirectionData::New();
    set_data(obj);
  }
  RedirectionData::Cast(obj).set_target(target);
}


void Function::set_data(const Object& value) const {
  StorePointer(&raw_ptr()->data_, value.raw());
}


bool Function::IsInFactoryScope() const {
  if (!IsLocalFunction()) {
    return IsFactory();
  }
  Function& outer_function = Function::Handle(parent_function());
  while (outer_function.IsLocalFunction()) {
    outer_function = outer_function.parent_function();
  }
  return outer_function.IsFactory();
}


void Function::set_name(const String& value) const {
  ASSERT(value.IsSymbol());
  StorePointer(&raw_ptr()->name_, value.raw());
}


void Function::set_owner(const Object& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->owner_, value.raw());
}


void Function::set_result_type(const AbstractType& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->result_type_, value.raw());
}


RawAbstractType* Function::ParameterTypeAt(intptr_t index) const {
  const Array& parameter_types = Array::Handle(raw_ptr()->parameter_types_);
  AbstractType& parameter_type = AbstractType::Handle();
  parameter_type ^= parameter_types.At(index);
  return parameter_type.raw();
}


void Function::SetParameterTypeAt(
    intptr_t index, const AbstractType& value) const {
  ASSERT(!value.IsNull());
  const Array& parameter_types = Array::Handle(raw_ptr()->parameter_types_);
  parameter_types.SetAt(index, value);
}


void Function::set_parameter_types(const Array& value) const {
  StorePointer(&raw_ptr()->parameter_types_, value.raw());
}


RawString* Function::ParameterNameAt(intptr_t index) const {
  const Array& parameter_names = Array::Handle(raw_ptr()->parameter_names_);
  String& parameter_name = String::Handle();
  parameter_name ^= parameter_names.At(index);
  return parameter_name.raw();
}


void Function::SetParameterNameAt(intptr_t index, const String& value) const {
  ASSERT(!value.IsNull() && value.IsSymbol());
  const Array& parameter_names = Array::Handle(raw_ptr()->parameter_names_);
  parameter_names.SetAt(index, value);
}


void Function::set_parameter_names(const Array& value) const {
  StorePointer(&raw_ptr()->parameter_names_, value.raw());
}


void Function::set_kind(RawFunction::Kind value) const {
  set_kind_tag(KindBits::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_intrinsic(bool value) const {
  set_kind_tag(IntrinsicBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_recognized(bool value) const {
  set_kind_tag(RecognizedBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_redirecting(bool value) const {
  set_kind_tag(RedirectingBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_static(bool value) const {
  set_kind_tag(StaticBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_const(bool value) const {
  set_kind_tag(ConstBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_external(bool value) const {
  set_kind_tag(ExternalBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_token_pos(intptr_t value) const {
  ASSERT(value >= 0);
  raw_ptr()->token_pos_ = value;
}


void Function::set_kind_tag(intptr_t value) const {
  raw_ptr()->kind_tag_ = static_cast<uint16_t>(value);
}


void Function::set_num_fixed_parameters(intptr_t value) const {
  ASSERT(value >= 0);
  ASSERT(Utils::IsInt(16, value));
  raw_ptr()->num_fixed_parameters_ = static_cast<int16_t>(value);
}


void Function::set_num_optional_parameters(intptr_t value) const {
  // A positive value indicates positional params, a negative one named params.
  ASSERT(Utils::IsInt(16, value));
  raw_ptr()->num_optional_parameters_ = static_cast<int16_t>(value);
}


void Function::SetNumOptionalParameters(intptr_t num_optional_parameters,
                                        bool are_optional_positional) const {
  ASSERT(num_optional_parameters >= 0);
  set_num_optional_parameters(are_optional_positional ?
                              num_optional_parameters :
                              -num_optional_parameters);
}


bool Function::is_optimizable() const {
  if (FLAG_coverage_dir != NULL) {
    // Do not optimize if collecting coverage data.
    return false;
  }
  if (OptimizableBit::decode(raw_ptr()->kind_tag_) &&
      (script() != Script::null()) &&
      ((end_token_pos() - token_pos()) < FLAG_huge_method_cutoff_in_tokens)) {
    // Additional check needed for implicit getters.
    if (HasCode() &&
       (Code::Handle(unoptimized_code()).Size() >=
        FLAG_huge_method_cutoff_in_code_size)) {
      return false;
    } else {
      return true;
    }
  }
  return false;
}


void Function::set_is_optimizable(bool value) const {
  set_kind_tag(OptimizableBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_has_finally(bool value) const {
  set_kind_tag(HasFinallyBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_native(bool value) const {
  set_kind_tag(NativeBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_abstract(bool value) const {
  set_kind_tag(AbstractBit::update(value, raw_ptr()->kind_tag_));
}


void Function::set_is_inlinable(bool value) const {
  set_kind_tag(InlinableBit::update(value, raw_ptr()->kind_tag_));
}


bool Function::IsInlineable() const {
  return (InlinableBit::decode(raw_ptr()->kind_tag_) &&
          HasCode() &&
          !Isolate::Current()->debugger()->HasBreakpoint(*this));
}


void Function::set_is_visible(bool value) const {
  set_kind_tag(VisibleBit::update(value, raw_ptr()->kind_tag_));
}


intptr_t Function::NumParameters() const {
  return num_fixed_parameters() + NumOptionalParameters();
}


intptr_t Function::NumImplicitParameters() const {
  if (kind() == RawFunction::kConstructor) {
    if (is_static()) {
      ASSERT(IsFactory());
      return 1;  // Type arguments.
    } else {
      ASSERT(IsConstructor());
      return 2;  // Instance, phase.
    }
  }
  if ((kind() == RawFunction::kClosureFunction) ||
      (kind() == RawFunction::kSignatureFunction)) {
    return 1;  // Closure object.
  }
  if (!is_static()) {
    // Closure functions defined inside instance (i.e. non-static) functions are
    // marked as non-static, but they do not have a receiver.
    // Closures are handled above.
    ASSERT((kind() != RawFunction::kClosureFunction) &&
           (kind() != RawFunction::kSignatureFunction));
    return 1;  // Receiver.
  }
  return 0;  // No implicit parameters.
}


bool Function::AreValidArgumentCounts(intptr_t num_arguments,
                                      intptr_t num_named_arguments,
                                      String* error_message) const {
  if (num_named_arguments > NumOptionalNamedParameters()) {
    if (error_message != NULL) {
      const intptr_t kMessageBufferSize = 64;
      char message_buffer[kMessageBufferSize];
      OS::SNPrint(message_buffer,
                  kMessageBufferSize,
                  "%" Pd " named passed, at most %" Pd " expected",
                  num_named_arguments,
                  NumOptionalNamedParameters());
      *error_message = String::New(message_buffer);
    }
    return false;  // Too many named arguments.
  }
  const int num_pos_args = num_arguments - num_named_arguments;
  const int num_opt_pos_params = NumOptionalPositionalParameters();
  const int num_pos_params = num_fixed_parameters() + num_opt_pos_params;
  if (num_pos_args > num_pos_params) {
    if (error_message != NULL) {
      const intptr_t kMessageBufferSize = 64;
      char message_buffer[kMessageBufferSize];
      // Hide implicit parameters to the user.
      const intptr_t num_hidden_params = NumImplicitParameters();
      OS::SNPrint(message_buffer,
                  kMessageBufferSize,
                  "%" Pd "%s passed, %s%" Pd " expected",
                  num_pos_args - num_hidden_params,
                  num_opt_pos_params > 0 ? " positional" : "",
                  num_opt_pos_params > 0 ? "at most " : "",
                  num_pos_params - num_hidden_params);
      *error_message = String::New(message_buffer);
    }
    return false;  // Too many fixed and/or positional arguments.
  }
  if (num_pos_args < num_fixed_parameters()) {
    if (error_message != NULL) {
      const intptr_t kMessageBufferSize = 64;
      char message_buffer[kMessageBufferSize];
      // Hide implicit parameters to the user.
      const intptr_t num_hidden_params = NumImplicitParameters();
      OS::SNPrint(message_buffer,
                  kMessageBufferSize,
                  "%" Pd "%s passed, %s%" Pd " expected",
                  num_pos_args - num_hidden_params,
                  num_opt_pos_params > 0 ? " positional" : "",
                  num_opt_pos_params > 0 ? "at least " : "",
                  num_fixed_parameters() - num_hidden_params);
      *error_message = String::New(message_buffer);
    }
    return false;  // Too few fixed and/or positional arguments.
  }
  return true;
}


bool Function::AreValidArguments(intptr_t num_arguments,
                                 const Array& argument_names,
                                 String* error_message) const {
  const intptr_t num_named_arguments =
      argument_names.IsNull() ? 0 : argument_names.Length();
  if (!AreValidArgumentCounts(num_arguments,
                              num_named_arguments,
                              error_message)) {
    return false;
  }
  // Verify that all argument names are valid parameter names.
  Isolate* isolate = Isolate::Current();
  String& argument_name = String::Handle(isolate);
  String& parameter_name = String::Handle(isolate);
  for (intptr_t i = 0; i < num_named_arguments; i++) {
    argument_name ^= argument_names.At(i);
    ASSERT(argument_name.IsSymbol());
    bool found = false;
    const intptr_t num_positional_args = num_arguments - num_named_arguments;
    const intptr_t num_parameters = NumParameters();
    for (intptr_t j = num_positional_args;
         !found && (j < num_parameters);
         j++) {
      parameter_name = ParameterNameAt(j);
      ASSERT(argument_name.IsSymbol());
      if (argument_name.Equals(parameter_name)) {
        found = true;
      }
    }
    if (!found) {
      if (error_message != NULL) {
        const intptr_t kMessageBufferSize = 64;
        char message_buffer[kMessageBufferSize];
        OS::SNPrint(message_buffer,
                    kMessageBufferSize,
                    "no optional formal parameter named '%s'",
                    argument_name.ToCString());
        *error_message = String::New(message_buffer);
      }
      return false;
    }
  }
  return true;
}


bool Function::AreValidArguments(const ArgumentsDescriptor& args_desc,
                                 String* error_message) const {
  const intptr_t num_arguments = args_desc.Count();
  const intptr_t num_named_arguments = args_desc.NamedCount();

  if (!AreValidArgumentCounts(num_arguments,
                              num_named_arguments,
                              error_message)) {
    return false;
  }
  // Verify that all argument names are valid parameter names.
  Isolate* isolate = Isolate::Current();
  String& argument_name = String::Handle(isolate);
  String& parameter_name = String::Handle(isolate);
  for (intptr_t i = 0; i < num_named_arguments; i++) {
    argument_name ^= args_desc.NameAt(i);
    ASSERT(argument_name.IsSymbol());
    bool found = false;
    const intptr_t num_positional_args = num_arguments - num_named_arguments;
    const int num_parameters = NumParameters();
    for (intptr_t j = num_positional_args;
         !found && (j < num_parameters);
         j++) {
      parameter_name = ParameterNameAt(j);
      ASSERT(argument_name.IsSymbol());
      if (argument_name.Equals(parameter_name)) {
        found = true;
      }
    }
    if (!found) {
      if (error_message != NULL) {
        const intptr_t kMessageBufferSize = 64;
        char message_buffer[kMessageBufferSize];
        OS::SNPrint(message_buffer,
                    kMessageBufferSize,
                    "no optional formal parameter named '%s'",
                    argument_name.ToCString());
        *error_message = String::New(message_buffer);
      }
      return false;
    }
  }
  return true;
}


// Helper allocating a C string buffer in the zone, printing the fully qualified
// name of a function in it, and replacing ':' by '_' to make sure the
// constructed name is a valid C++ identifier for debugging purpose.
// Set 'chars' to allocated buffer and return number of written characters.
static intptr_t ConstructFunctionFullyQualifiedCString(const Function& function,
                                                       char** chars,
                                                       intptr_t reserve_len) {
  const char* name = String::Handle(function.name()).ToCString();
  const char* function_format = (reserve_len == 0) ? "%s" : "%s_";
  reserve_len += OS::SNPrint(NULL, 0, function_format, name);
  const Function& parent = Function::Handle(function.parent_function());
  intptr_t written = 0;
  if (parent.IsNull()) {
    const Class& function_class = Class::Handle(function.Owner());
    ASSERT(!function_class.IsNull());
    const char* class_name = String::Handle(function_class.Name()).ToCString();
    ASSERT(class_name != NULL);
    const Library& library = Library::Handle(function_class.library());
    ASSERT(!library.IsNull());
    const char* library_name = String::Handle(library.url()).ToCString();
    ASSERT(library_name != NULL);
    const char* lib_class_format =
        (library_name[0] == '\0') ? "%s%s_" : "%s_%s_";
    reserve_len +=
        OS::SNPrint(NULL, 0, lib_class_format, library_name, class_name);
    ASSERT(chars != NULL);
    *chars = Isolate::Current()->current_zone()->Alloc<char>(reserve_len + 1);
    written = OS::SNPrint(
        *chars, reserve_len + 1, lib_class_format, library_name, class_name);
  } else {
    written = ConstructFunctionFullyQualifiedCString(parent,
                                                     chars,
                                                     reserve_len);
  }
  ASSERT(*chars != NULL);
  char* next = *chars + written;
  written += OS::SNPrint(next, reserve_len + 1, function_format, name);
  // Replace ":" with "_".
  while (true) {
    next = strchr(next, ':');
    if (next == NULL) break;
    *next = '_';
  }
  return written;
}


const char* Function::ToFullyQualifiedCString() const {
  char* chars = NULL;
  ConstructFunctionFullyQualifiedCString(*this, &chars, 0);
  return chars;
}


bool Function::HasCompatibleParametersWith(const Function& other,
                                           Error* bound_error) const {
  ASSERT(FLAG_error_on_bad_override);
  // Check that this function's signature type is a subtype of the other
  // function's signature type.
  if (!TypeTest(kIsSubtypeOf, Object::null_abstract_type_arguments(),
                other, Object::null_abstract_type_arguments(), bound_error)) {
    // For more informative error reporting, use the location of the other
    // function here, since the caller will use the location of this function.
    *bound_error = FormatError(
        *bound_error,  // A bound error if non null.
        Script::Handle(other.script()),
        other.token_pos(),
        "signature type '%s' of function '%s' is not a subtype of signature "
        "type '%s' of function '%s'",
        String::Handle(UserVisibleSignature()).ToCString(),
        String::Handle(UserVisibleName()).ToCString(),
        String::Handle(other.UserVisibleSignature()).ToCString(),
        String::Handle(other.UserVisibleName()).ToCString());
    return false;
  }
  // We should also check that if the other function explicitly specifies a
  // default value for a formal parameter, this function does not specify a
  // different default value for the same parameter. However, this check is not
  // possible in the current implementation, because the default parameter
  // values are not stored in the Function object, but discarded after a
  // function is compiled.
  return true;
}


// If test_kind == kIsSubtypeOf, checks if the type of the specified parameter
// of this function is a subtype or a supertype of the type of the specified
// parameter of the other function.
// If test_kind == kIsMoreSpecificThan, checks if the type of the specified
// parameter of this function is more specific than the type of the specified
// parameter of the other function.
// Note that we do not apply contravariance of parameter types, but covariance
// of both parameter types and result type.
bool Function::TestParameterType(
    TypeTestKind test_kind,
    intptr_t parameter_position,
    intptr_t other_parameter_position,
    const AbstractTypeArguments& type_arguments,
    const Function& other,
    const AbstractTypeArguments& other_type_arguments,
    Error* bound_error) const {
  AbstractType& other_param_type =
      AbstractType::Handle(other.ParameterTypeAt(other_parameter_position));
  if (!other_param_type.IsInstantiated()) {
    other_param_type = other_param_type.InstantiateFrom(other_type_arguments,
                                                        bound_error);
    ASSERT((bound_error == NULL) || bound_error->IsNull());
  }
  if (other_param_type.IsDynamicType()) {
    return true;
  }
  AbstractType& param_type =
      AbstractType::Handle(ParameterTypeAt(parameter_position));
  if (!param_type.IsInstantiated()) {
    param_type = param_type.InstantiateFrom(type_arguments, bound_error);
    ASSERT((bound_error == NULL) || bound_error->IsNull());
  }
  if (param_type.IsDynamicType()) {
    return test_kind == kIsSubtypeOf;
  }
  if (test_kind == kIsSubtypeOf) {
    if (!param_type.IsSubtypeOf(other_param_type, bound_error) &&
        !other_param_type.IsSubtypeOf(param_type, bound_error)) {
      return false;
    }
  } else {
    ASSERT(test_kind == kIsMoreSpecificThan);
    if (!param_type.IsMoreSpecificThan(other_param_type, bound_error)) {
      return false;
    }
  }
  return true;
}


bool Function::TypeTest(TypeTestKind test_kind,
                        const AbstractTypeArguments& type_arguments,
                        const Function& other,
                        const AbstractTypeArguments& other_type_arguments,
                        Error* bound_error) const {
  const intptr_t num_fixed_params = num_fixed_parameters();
  const intptr_t num_opt_pos_params = NumOptionalPositionalParameters();
  const intptr_t num_opt_named_params = NumOptionalNamedParameters();
  const intptr_t other_num_fixed_params = other.num_fixed_parameters();
  const intptr_t other_num_opt_pos_params =
      other.NumOptionalPositionalParameters();
  const intptr_t other_num_opt_named_params =
      other.NumOptionalNamedParameters();
  // This function requires the same arguments or less and accepts the same
  // arguments or more.
  // A generative constructor may be compared to a redirecting factory and be
  // compatible although it has an additional phase parameter.
  // More generally, we can ignore implicit parameters.
  const intptr_t num_ignored_params = NumImplicitParameters();
  const intptr_t other_num_ignored_params = other.NumImplicitParameters();
  if (((num_fixed_params - num_ignored_params) >
       (other_num_fixed_params - other_num_ignored_params)) ||
      ((num_fixed_params - num_ignored_params + num_opt_pos_params) <
       (other_num_fixed_params - other_num_ignored_params +
        other_num_opt_pos_params)) ||
      (num_opt_named_params < other_num_opt_named_params)) {
    return false;
  }
  // Check the result type.
  AbstractType& other_res_type = AbstractType::Handle(other.result_type());
  if (!other_res_type.IsInstantiated()) {
    other_res_type = other_res_type.InstantiateFrom(other_type_arguments,
                                                    bound_error);
    ASSERT((bound_error == NULL) || bound_error->IsNull());
  }
  if (!other_res_type.IsDynamicType() && !other_res_type.IsVoidType()) {
    AbstractType& res_type = AbstractType::Handle(result_type());
    if (!res_type.IsInstantiated()) {
      res_type = res_type.InstantiateFrom(type_arguments, bound_error);
      ASSERT((bound_error == NULL) || bound_error->IsNull());
    }
    if (res_type.IsVoidType()) {
      return false;
    }
    if (test_kind == kIsSubtypeOf) {
      if (!res_type.IsSubtypeOf(other_res_type, bound_error) &&
          !other_res_type.IsSubtypeOf(res_type, bound_error)) {
        return false;
      }
    } else {
      ASSERT(test_kind == kIsMoreSpecificThan);
      if (!res_type.IsMoreSpecificThan(other_res_type, bound_error)) {
        return false;
      }
    }
  }
  // Check the types of fixed and optional positional parameters.
  for (intptr_t i = 0; i < (other_num_fixed_params - other_num_ignored_params +
                            other_num_opt_pos_params); i++) {
    if (!TestParameterType(test_kind,
                           i + num_ignored_params, i + other_num_ignored_params,
                           type_arguments, other, other_type_arguments,
                           bound_error)) {
      return false;
    }
  }
  // Check the names and types of optional named parameters.
  if (other_num_opt_named_params == 0) {
    return true;
  }
  // Check that for each optional named parameter of type T of the other
  // function type, there exists an optional named parameter of this function
  // type with an identical name and with a type S that is a either a subtype
  // or supertype of T (if test_kind == kIsSubtypeOf) or that is more specific
  // than T (if test_kind == kIsMoreSpecificThan).
  // Note that SetParameterNameAt() guarantees that names are symbols, so we
  // can compare their raw pointers.
  const int num_params = num_fixed_params + num_opt_named_params;
  const int other_num_params =
      other_num_fixed_params + other_num_opt_named_params;
  bool found_param_name;
  String& other_param_name = String::Handle();
  for (intptr_t i = other_num_fixed_params; i < other_num_params; i++) {
    other_param_name = other.ParameterNameAt(i);
    ASSERT(other_param_name.IsSymbol());
    found_param_name = false;
    for (intptr_t j = num_fixed_params; j < num_params; j++) {
      ASSERT(String::Handle(ParameterNameAt(j)).IsSymbol());
      if (ParameterNameAt(j) == other_param_name.raw()) {
        found_param_name = true;
        if (!TestParameterType(test_kind,
                               j, i,
                               type_arguments, other, other_type_arguments,
                               bound_error)) {
          return false;
        }
        break;
      }
    }
    if (!found_param_name) {
      return false;
    }
  }
  return true;
}


// The compiler generates an implicit constructor if a class definition
// does not contain an explicit constructor or factory. The implicit
// constructor has the same token position as the owner class.
bool Function::IsImplicitConstructor() const {
  return IsConstructor() && (token_pos() == end_token_pos());
}


bool Function::IsImplicitClosureFunction() const {
  if (!IsClosureFunction()) {
    return false;
  }
  const Function& parent = Function::Handle(parent_function());
  return (parent.implicit_closure_function() == raw());
}


RawFunction* Function::New() {
  ASSERT(Object::function_class() != Class::null());
  RawObject* raw = Object::Allocate(Function::kClassId,
                                    Function::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawFunction*>(raw);
}


RawFunction* Function::New(const String& name,
                           RawFunction::Kind kind,
                           bool is_static,
                           bool is_const,
                           bool is_abstract,
                           bool is_external,
                           const Object& owner,
                           intptr_t token_pos) {
  ASSERT(!owner.IsNull());
  const Function& result = Function::Handle(Function::New());
  result.set_parameter_types(Object::empty_array());
  result.set_parameter_names(Object::empty_array());
  result.set_name(name);
  result.set_kind(kind);
  result.set_is_static(is_static);
  result.set_is_const(is_const);
  result.set_is_abstract(is_abstract);
  result.set_is_external(is_external);
  result.set_is_visible(true);  // Will be computed later.
  result.set_is_intrinsic(false);
  result.set_is_recognized(false);
  result.set_is_redirecting(false);
  result.set_owner(owner);
  result.set_token_pos(token_pos);
  result.set_end_token_pos(token_pos);
  result.set_num_fixed_parameters(0);
  result.set_num_optional_parameters(0);
  result.set_usage_counter(0);
  result.set_deoptimization_counter(0);
  result.set_optimized_instruction_count(0);
  result.set_optimized_call_site_count(0);
  result.set_is_optimizable(true);
  result.set_has_finally(false);
  result.set_is_native(false);
  result.set_is_inlinable(true);
  if (kind == RawFunction::kClosureFunction) {
    const ClosureData& data = ClosureData::Handle(ClosureData::New());
    result.set_data(data);
  }
  return result.raw();
}


RawFunction* Function::Clone(const Class& new_owner) const {
  ASSERT(!IsConstructor());
  Function& clone = Function::Handle();
  clone ^= Object::Clone(*this, Heap::kOld);
  const Class& owner = Class::Handle(this->Owner());
  const PatchClass& clone_owner =
      PatchClass::Handle(PatchClass::New(new_owner, owner));
  clone.set_owner(clone_owner);
  clone.StorePointer(&clone.raw_ptr()->code_, Code::null());
  clone.StorePointer(&clone.raw_ptr()->unoptimized_code_, Code::null());
  clone.set_usage_counter(0);
  clone.set_deoptimization_counter(0);
  clone.set_optimized_instruction_count(0);
  clone.set_optimized_call_site_count(0);
  return clone.raw();
}


RawFunction* Function::NewClosureFunction(const String& name,
                                          const Function& parent,
                                          intptr_t token_pos) {
  ASSERT(!parent.IsNull());
  // Use the owner defining the parent function and not the class containing it.
  const Object& parent_owner = Object::Handle(parent.raw_ptr()->owner_);
  ASSERT(!parent_owner.IsNull());
  const Function& result = Function::Handle(
      Function::New(name,
                    RawFunction::kClosureFunction,
                    /* is_static = */ parent.is_static(),
                    /* is_const = */ false,
                    /* is_abstract = */ false,
                    /* is_external = */ false,
                    parent_owner,
                    token_pos));
  result.set_parent_function(parent);
  return result.raw();
}


RawFunction* Function::ImplicitClosureFunction() const {
  // Return the existing implicit closure function if any.
  if (implicit_closure_function() != Function::null()) {
    return implicit_closure_function();
  }
  ASSERT(!IsSignatureFunction() && !IsClosureFunction());
  // Create closure function.
  const String& closure_name = String::Handle(name());
  const Function& closure_function = Function::Handle(
      NewClosureFunction(closure_name, *this, token_pos()));

  // Set closure function's context scope.
  ContextScope& context_scope = ContextScope::Handle();
  if (is_static()) {
    context_scope = ContextScope::New(0);
  } else {
    context_scope = LocalScope::CreateImplicitClosureScope(*this);
  }
  closure_function.set_context_scope(context_scope);

  // Set closure function's result type to this result type.
  closure_function.set_result_type(AbstractType::Handle(result_type()));

  // Set closure function's end token to this end token.
  closure_function.set_end_token_pos(end_token_pos());

  // Set closure function's formal parameters to this formal parameters,
  // removing the receiver if this is an instance method and adding the closure
  // object as first parameter.
  const int kClosure = 1;
  const int has_receiver = is_static() ? 0 : 1;
  const int num_fixed_params = kClosure - has_receiver + num_fixed_parameters();
  const int num_opt_params = NumOptionalParameters();
  const bool has_opt_pos_params = HasOptionalPositionalParameters();
  const int num_params = num_fixed_params + num_opt_params;
  closure_function.set_num_fixed_parameters(num_fixed_params);
  closure_function.SetNumOptionalParameters(num_opt_params, has_opt_pos_params);
  closure_function.set_parameter_types(Array::Handle(Array::New(num_params,
                                                                Heap::kOld)));
  closure_function.set_parameter_names(Array::Handle(Array::New(num_params,
                                                                Heap::kOld)));
  AbstractType& param_type = AbstractType::Handle();
  String& param_name = String::Handle();
  // Add implicit closure object parameter.
  param_type = Type::DynamicType();
  closure_function.SetParameterTypeAt(0, param_type);
  closure_function.SetParameterNameAt(0, Symbols::ClosureParameter());
  for (int i = kClosure; i < num_params; i++) {
    param_type = ParameterTypeAt(has_receiver - kClosure + i);
    closure_function.SetParameterTypeAt(i, param_type);
    param_name = ParameterNameAt(has_receiver - kClosure + i);
    closure_function.SetParameterNameAt(i, param_name);
  }

  // Lookup or create a new signature class for the closure function in the
  // library of the owner class.
  const Class& owner_class = Class::Handle(Owner());
  ASSERT(!owner_class.IsNull() && (Owner() == closure_function.Owner()));
  const Library& library = Library::Handle(owner_class.library());
  ASSERT(!library.IsNull());
  const String& signature = String::Handle(closure_function.Signature());
  Class& signature_class = Class::ZoneHandle(
      library.LookupLocalClass(signature));
  if (signature_class.IsNull()) {
    const Script& script = Script::Handle(this->script());
    signature_class = Class::NewSignatureClass(signature,
                                               closure_function,
                                               script,
                                               closure_function.token_pos());
    library.AddClass(signature_class);
  } else {
    closure_function.set_signature_class(signature_class);
  }
  const Type& signature_type = Type::Handle(signature_class.SignatureType());
  if (!signature_type.IsFinalized()) {
    ClassFinalizer::FinalizeType(
        signature_class, signature_type, ClassFinalizer::kCanonicalize);
  }
  ASSERT(closure_function.signature_class() == signature_class.raw());
  set_implicit_closure_function(closure_function);
  ASSERT(closure_function.IsImplicitClosureFunction());
  return closure_function.raw();
}


RawString* Function::UserVisibleFormalParameters() const {
  const GrowableObjectArray& pieces =
      GrowableObjectArray::Handle(GrowableObjectArray::New());
  const TypeArguments& instantiator = TypeArguments::Handle();
  BuildSignatureParameters(false, kUserVisibleName, instantiator, pieces);
  const Array& strings = Array::Handle(Array::MakeArray(pieces));
  return String::ConcatAll(strings);
}


void Function::BuildSignatureParameters(
    bool instantiate,
    NameVisibility name_visibility,
    const AbstractTypeArguments& instantiator,
    const GrowableObjectArray& pieces) const {
  AbstractType& param_type = AbstractType::Handle();
  const intptr_t num_params = NumParameters();
  const intptr_t num_fixed_params = num_fixed_parameters();
  const intptr_t num_opt_pos_params = NumOptionalPositionalParameters();
  const intptr_t num_opt_named_params = NumOptionalNamedParameters();
  const intptr_t num_opt_params = num_opt_pos_params + num_opt_named_params;
  ASSERT((num_fixed_params + num_opt_params) == num_params);
  String& name = String::Handle();
  intptr_t i = 0;
  if (name_visibility == kUserVisibleName) {
    // Hide implicit parameters.
    i = NumImplicitParameters();
  }
  while (i < num_fixed_params) {
    param_type = ParameterTypeAt(i);
    ASSERT(!param_type.IsNull());
    if (instantiate && !param_type.IsInstantiated()) {
      param_type = param_type.InstantiateFrom(instantiator, NULL);
    }
    name = param_type.BuildName(name_visibility);
    pieces.Add(name);
    if (i != (num_params - 1)) {
      pieces.Add(Symbols::CommaSpace());
    }
    i++;
  }
  if (num_opt_params > 0) {
    if (num_opt_pos_params > 0) {
      pieces.Add(Symbols::LBracket());
    } else {
      pieces.Add(Symbols::LBrace());
    }
    for (intptr_t i = num_fixed_params; i < num_params; i++) {
      // The parameter name of an optional positional parameter does not need
      // to be part of the signature, since it is not used.
      if (num_opt_named_params > 0) {
        name = ParameterNameAt(i);
        pieces.Add(name);
        pieces.Add(Symbols::ColonSpace());
      }
      param_type = ParameterTypeAt(i);
      if (instantiate && !param_type.IsInstantiated()) {
        param_type = param_type.InstantiateFrom(instantiator, NULL);
      }
      ASSERT(!param_type.IsNull());
      name = param_type.BuildName(name_visibility);
      pieces.Add(name);
      if (i != (num_params - 1)) {
        pieces.Add(Symbols::CommaSpace());
      }
    }
    if (num_opt_pos_params > 0) {
      pieces.Add(Symbols::RBracket());
    } else {
      pieces.Add(Symbols::RBrace());
    }
  }
}


RawInstance* Function::ImplicitStaticClosure() const {
  if (implicit_static_closure() == Instance::null()) {
    ObjectStore* object_store = Isolate::Current()->object_store();
    const Context& context = Context::Handle(object_store->empty_context());
    const Instance& closure =
        Instance::Handle(Closure::New(*this, context, Heap::kOld));
    set_implicit_static_closure(closure);
  }
  return implicit_static_closure();
}


RawString* Function::BuildSignature(
    bool instantiate,
    NameVisibility name_visibility,
    const AbstractTypeArguments& instantiator) const {
  const GrowableObjectArray& pieces =
      GrowableObjectArray::Handle(GrowableObjectArray::New());
  String& name = String::Handle();
  if (!instantiate && !is_static() && (name_visibility == kInternalName)) {
    // Prefix the signature with its signature class and type parameters, if any
    // (e.g. "Map<K, V>(K) => bool"). In case of a function type alias, the
    // signature class name is the alias name.
    // The signature of static functions cannot be type parameterized.
    const Class& function_class = Class::Handle(Owner());
    ASSERT(!function_class.IsNull());
    const TypeArguments& type_parameters = TypeArguments::Handle(
        function_class.type_parameters());
    if (!type_parameters.IsNull()) {
      const String& function_class_name = String::Handle(function_class.Name());
      pieces.Add(function_class_name);
      const intptr_t num_type_parameters = type_parameters.Length();
      pieces.Add(Symbols::LAngleBracket());
      TypeParameter& type_parameter = TypeParameter::Handle();
      AbstractType& bound = AbstractType::Handle();
      for (intptr_t i = 0; i < num_type_parameters; i++) {
        type_parameter ^= type_parameters.TypeAt(i);
        name = type_parameter.name();
        pieces.Add(name);
        bound = type_parameter.bound();
        if (!bound.IsNull() && !bound.IsObjectType()) {
          pieces.Add(Symbols::SpaceExtendsSpace());
          name = bound.BuildName(name_visibility);
          pieces.Add(name);
        }
        if (i < num_type_parameters - 1) {
          pieces.Add(Symbols::CommaSpace());
        }
      }
      pieces.Add(Symbols::RAngleBracket());
    }
  }
  pieces.Add(Symbols::LParen());
  BuildSignatureParameters(instantiate,
                           name_visibility,
                           instantiator,
                           pieces);
  pieces.Add(Symbols::RParenArrow());
  AbstractType& res_type = AbstractType::Handle(result_type());
  if (instantiate && !res_type.IsInstantiated()) {
    res_type = res_type.InstantiateFrom(instantiator, NULL);
  }
  name = res_type.BuildName(name_visibility);
  pieces.Add(name);
  const Array& strings = Array::Handle(Array::MakeArray(pieces));
  return Symbols::New(String::Handle(String::ConcatAll(strings)));
}


bool Function::HasInstantiatedSignature() const {
  AbstractType& type = AbstractType::Handle(result_type());
  if (!type.IsInstantiated()) {
    return false;
  }
  const intptr_t num_parameters = NumParameters();
  for (intptr_t i = 0; i < num_parameters; i++) {
    type = ParameterTypeAt(i);
    if (!type.IsInstantiated()) {
      return false;
    }
  }
  return true;
}


RawClass* Function::Owner() const {
  const Object& obj = Object::Handle(raw_ptr()->owner_);
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  ASSERT(obj.IsPatchClass());
  return PatchClass::Cast(obj).patched_class();
}


RawClass* Function::origin() const {
  const Object& obj = Object::Handle(raw_ptr()->owner_);
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  ASSERT(obj.IsPatchClass());
  return PatchClass::Cast(obj).source_class();
}


RawScript* Function::script() const {
  const Object& obj = Object::Handle(raw_ptr()->owner_);
  if (obj.IsClass()) {
    return Class::Cast(obj).script();
  }
  ASSERT(obj.IsPatchClass());
  return PatchClass::Cast(obj).Script();
}


bool Function::HasOptimizedCode() const {
  return HasCode() && Code::Handle(raw_ptr()->code_).is_optimized();
}


RawString* Function::UserVisibleName() const {
  const String& str = String::Handle(name());
  return String::IdentifierPrettyName(str);
}


RawString* Function::QualifiedUserVisibleName() const {
  String& tmp = String::Handle();
  const Class& cls = Class::Handle(Owner());

  if (IsClosureFunction()) {
    if (IsLocalFunction() && !IsImplicitClosureFunction()) {
      const Function& parent = Function::Handle(parent_function());
      tmp = parent.QualifiedUserVisibleName();
    } else {
      return UserVisibleName();
    }
  } else {
    if (cls.IsTopLevel()) {
      return UserVisibleName();
    } else {
      tmp = cls.UserVisibleName();
    }
  }
  tmp = String::Concat(tmp, Symbols::Dot());
  const String& suffix = String::Handle(UserVisibleName());
  return String::Concat(tmp, suffix);
}


// Construct fingerprint from token stream. The token stream contains also
// arguments.
int32_t Function::SourceFingerprint() const {
  uint32_t result = String::Handle(Signature()).Hash();
  TokenStream::Iterator tokens_iterator(TokenStream::Handle(
      Script::Handle(script()).tokens()), token_pos());
  Object& obj = Object::Handle();
  String& literal = String::Handle();
  while (tokens_iterator.CurrentPosition() < end_token_pos()) {
    uint32_t val = 0;
    obj = tokens_iterator.CurrentToken();
    if (obj.IsSmi()) {
      val = Smi::Cast(obj).Value();
    } else {
      literal = tokens_iterator.MakeLiteralToken(obj);
      val = literal.Hash();
    }
    result = 31 * result + val;
    tokens_iterator.Advance();
  }
  result = result & ((static_cast<uint32_t>(1) << 31) - 1);
  ASSERT(result <= static_cast<uint32_t>(kMaxInt32));
  return result;
}


bool Function::CheckSourceFingerprint(int32_t fp) const {
  if (SourceFingerprint() != fp) {
    const bool recalculatingFingerprints = false;
    if (recalculatingFingerprints) {
      // This output can be copied into a file, then used with sed
      // to replace the old values.
      // sed -i .bak -f /tmp/newkeys runtime/vm/intrinsifier.h
      // sed -i .bak -f /tmp/newkeys runtime/vm/intermediate_language.h
      // sed -i .bak -f /tmp/newkeys runtime/vm/flow_graph_builder.h
      OS::Print("s/%d/%d/\n", fp, SourceFingerprint());
    } else {
      OS::Print("FP mismatch while recognizing method %s:"
                " expecting %d found %d\n",
                ToFullyQualifiedCString(),
                fp,
                SourceFingerprint());
      return false;
    }
  }
  return true;
}


RawFunction* Function::NewStaticInitializer(const Field& field) {
  ASSERT(field.is_static());
  const String& field_name = String::Handle(field.name());
  const String& init_name =
      String::Handle(Symbols::New(String::Handle(
          String::Concat(Symbols::InitPrefix(), field_name))));
  const Function& init_function = Function::ZoneHandle(
      Function::New(init_name,
                    RawFunction::kStaticInitializer,
                    true,  // static
                    false,  // !const
                    false,  // !abstract
                    false,  // !external
                    Class::Handle(field.owner()),
                    field.token_pos()));
  init_function.set_result_type(AbstractType::Handle(field.type()));
  // Static initializer functions are generated by the VM and are therfore
  // hidden from the user. Since they are only executed once, we avoid
  // optimizing and inlining them. After the field is initialized, the
  // optimizing compiler can eliminate the call to the static initializer
  // via constant folding.
  init_function.set_is_visible(false);
  init_function.set_is_optimizable(false);
  init_function.set_is_inlinable(false);
  init_function.set_saved_static_field(field);
  return init_function.raw();
}


const char* Function::ToCString() const {
  const char* static_str = is_static() ? " static" : "";
  const char* abstract_str = is_abstract() ? " abstract" : "";
  const char* kind_str = NULL;
  const char* const_str = is_const() ? " const" : "";
  switch (kind()) {
    case RawFunction::kRegularFunction:
    case RawFunction::kClosureFunction:
    case RawFunction::kGetterFunction:
    case RawFunction::kSetterFunction:
      kind_str = "";
      break;
    case RawFunction::kSignatureFunction:
      kind_str = " signature";
      break;
    case RawFunction::kConstructor:
      kind_str = is_static() ? " factory" : " constructor";
      break;
    case RawFunction::kImplicitGetter:
      kind_str = " getter";
      break;
    case RawFunction::kImplicitSetter:
      kind_str = " setter";
      break;
    case RawFunction::kStaticInitializer:
      kind_str = " static-initializer";
      break;
    case RawFunction::kImplicitStaticFinalGetter:
      kind_str = " static-final-getter";
      break;
    case RawFunction::kMethodExtractor:
      kind_str = " method-extractor";
      break;
    case RawFunction::kNoSuchMethodDispatcher:
      kind_str = " no-such-method-dispatcher";
      break;
    case RawFunction::kInvokeFieldDispatcher:
      kind_str = "invoke-field-dispatcher";
      break;
    default:
      UNREACHABLE();
  }
  const char* kFormat = "Function '%s':%s%s%s%s.";
  const char* function_name = String::Handle(name()).ToCString();
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, function_name,
                             static_str, abstract_str, kind_str, const_str) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, function_name,
              static_str, abstract_str, kind_str, const_str);
  return chars;
}


void Function::PrintToJSONStream(JSONStream* stream, bool ref) const {
  const char* internal_function_name = String::Handle(name()).ToCString();
  const char* function_name =
      String::Handle(QualifiedUserVisibleName()).ToCString();
  ObjectIdRing* ring = Isolate::Current()->object_id_ring();
  intptr_t id = ring->GetIdForObject(raw());
  JSONObject jsobj(stream);
  jsobj.AddProperty("type", JSONType(ref));
  jsobj.AddProperty("id", id);
  jsobj.AddProperty("name", internal_function_name);
  jsobj.AddProperty("user_name", function_name);
  if (ref) return;
  jsobj.AddProperty("is_static", is_static());
  jsobj.AddProperty("is_const", is_const());
  jsobj.AddProperty("is_optimizable", is_optimizable());
  jsobj.AddProperty("is_inlinable", IsInlineable());
  const char* kind_string = NULL;
  switch (kind()) {
      case RawFunction::kRegularFunction:
        kind_string = "regular";
        break;
      case RawFunction::kGetterFunction:
        kind_string = "getter";
        break;
      case RawFunction::kSetterFunction:
        kind_string = "setter";
        break;
      case RawFunction::kImplicitGetter:
        kind_string = "implicit getter";
        break;
      case RawFunction::kImplicitSetter:
        kind_string = "implicit setter";
        break;
      case RawFunction::kMethodExtractor:
        kind_string = "method extractor";
        break;
      case RawFunction::kNoSuchMethodDispatcher:
        kind_string = "no such method";
        break;
      case RawFunction::kClosureFunction:
        kind_string = "closure";
        break;
      case RawFunction::kConstructor:
        kind_string = "constructor";
        break;
      case RawFunction::kStaticInitializer:
        kind_string = "static initializer";
        break;
      case RawFunction::kImplicitStaticFinalGetter:
        kind_string = "static final getter";
        break;
      default:
        UNREACHABLE();
  }
  jsobj.AddProperty("kind", kind_string);
  jsobj.AddProperty("unoptimized_code", Object::Handle(unoptimized_code()));
  jsobj.AddProperty("usage_counter", usage_counter());
  jsobj.AddProperty("optimized_call_site_count", optimized_call_site_count());
  jsobj.AddProperty("code", Object::Handle(CurrentCode()));
  jsobj.AddProperty("deoptimizations",
                    static_cast<intptr_t>(deoptimization_counter()));
}


void ClosureData::set_context_scope(const ContextScope& value) const {
  StorePointer(&raw_ptr()->context_scope_, value.raw());
}


void ClosureData::set_implicit_static_closure(const Instance& closure) const {
  ASSERT(!closure.IsNull());
  ASSERT(raw_ptr()->closure_ == Instance::null());
  StorePointer(&raw_ptr()->closure_, closure.raw());
}


void ClosureData::set_closure_allocation_stub(const Code& value) const {
  ASSERT(!value.IsNull());
  ASSERT(raw_ptr()->closure_allocation_stub_ == Code::null());
  StorePointer(&raw_ptr()->closure_allocation_stub_, value.raw());
}


void ClosureData::set_parent_function(const Function& value) const {
  StorePointer(&raw_ptr()->parent_function_, value.raw());
}


void ClosureData::set_signature_class(const Class& value) const {
  StorePointer(&raw_ptr()->signature_class_, value.raw());
}


RawClosureData* ClosureData::New() {
  ASSERT(Object::closure_data_class() != Class::null());
  RawObject* raw = Object::Allocate(ClosureData::kClassId,
                                    ClosureData::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawClosureData*>(raw);
}


const char* ClosureData::ToCString() const {
  return "ClosureData class";
}


void ClosureData::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


void RedirectionData::set_type(const Type& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->type_, value.raw());
}


void RedirectionData::set_identifier(const String& value) const {
  StorePointer(&raw_ptr()->identifier_, value.raw());
}


void RedirectionData::set_target(const Function& value) const {
  StorePointer(&raw_ptr()->target_, value.raw());
}


RawRedirectionData* RedirectionData::New() {
  ASSERT(Object::redirection_data_class() != Class::null());
  RawObject* raw = Object::Allocate(RedirectionData::kClassId,
                                    RedirectionData::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawRedirectionData*>(raw);
}


const char* RedirectionData::ToCString() const {
  return "RedirectionData class";
}


void RedirectionData::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawString* Field::GetterName(const String& field_name) {
  return String::Concat(Symbols::GetterPrefix(), field_name);
}


RawString* Field::GetterSymbol(const String& field_name) {
  const String& str = String::Handle(Field::GetterName(field_name));
  return Symbols::New(str);
}


RawString* Field::SetterName(const String& field_name) {
  return String::Concat(Symbols::SetterPrefix(), field_name);
}


RawString* Field::SetterSymbol(const String& field_name) {
  const String& str = String::Handle(Field::SetterName(field_name));
  return Symbols::New(str);
}


RawString* Field::NameFromGetter(const String& getter_name) {
  return String::SubString(getter_name, strlen(kGetterPrefix));
}


RawString* Field::NameFromSetter(const String& setter_name) {
  return String::SubString(setter_name, strlen(kSetterPrefix));
}


bool Field::IsGetterName(const String& function_name) {
  return function_name.StartsWith(Symbols::GetterPrefix());
}


bool Field::IsSetterName(const String& function_name) {
  return function_name.StartsWith(Symbols::SetterPrefix());
}


void Field::set_name(const String& value) const {
  ASSERT(value.IsSymbol());
  StorePointer(&raw_ptr()->name_, value.raw());
}


RawClass* Field::owner() const {
  const Object& obj = Object::Handle(raw_ptr()->owner_);
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  ASSERT(obj.IsPatchClass());
  return PatchClass::Cast(obj).patched_class();
}


RawClass* Field::origin() const {
  const Object& obj = Object::Handle(raw_ptr()->owner_);
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  ASSERT(obj.IsPatchClass());
  return PatchClass::Cast(obj).source_class();
}


RawInstance* Field::value() const {
  ASSERT(is_static());  // Valid only for static dart fields.
  return raw_ptr()->value_;
}


void Field::set_value(const Instance& value) const {
  ASSERT(is_static());  // Valid only for static dart fields.
  StorePointer(&raw_ptr()->value_, value.raw());
}


void Field::set_type(const AbstractType& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->type_, value.raw());
}


RawField* Field::New() {
  ASSERT(Object::field_class() != Class::null());
  RawObject* raw = Object::Allocate(Field::kClassId,
                                    Field::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawField*>(raw);
}


RawField* Field::New(const String& name,
                     bool is_static,
                     bool is_final,
                     bool is_const,
                     const Class& owner,
                     intptr_t token_pos) {
  ASSERT(!owner.IsNull());
  const Field& result = Field::Handle(Field::New());
  result.set_name(name);
  result.set_is_static(is_static);
  if (is_static) {
    result.set_value(Object::null_instance());
  } else {
    result.SetOffset(0);
  }
  result.set_is_final(is_final);
  result.set_is_const(is_const);
  result.set_owner(owner);
  result.set_token_pos(token_pos);
  result.set_has_initializer(false);
  result.set_guarded_cid(kIllegalCid);
  result.set_is_nullable(false);
  // Presently, we only attempt to remember the list length for final fields.
  if (is_final) {
    result.set_guarded_list_length(Field::kUnknownFixedLength);
  } else {
    result.set_guarded_list_length(Field::kNoFixedLength);
  }
  result.set_dependent_code(Object::null_array());
  return result.raw();
}



RawField* Field::Clone(const Class& new_owner) const {
  Field& clone = Field::Handle();
  clone ^= Object::Clone(*this, Heap::kOld);
  const Class& owner = Class::Handle(this->owner());
  const PatchClass& clone_owner =
      PatchClass::Handle(PatchClass::New(new_owner, owner));
  clone.set_owner(clone_owner);
  clone.set_dependent_code(Object::null_array());
  if (!clone.is_static()) {
    clone.SetOffset(0);
  }
  return clone.raw();
}


RawString* Field::UserVisibleName() const {
  const String& str = String::Handle(name());
  return String::IdentifierPrettyName(str);
}


intptr_t Field::guarded_list_length() const {
  return Smi::Value(raw_ptr()->guarded_list_length_);
}


void Field::set_guarded_list_length(intptr_t list_length) const {
  raw_ptr()->guarded_list_length_ = Smi::New(list_length);
}


const char* Field::ToCString() const {
  if (IsNull()) {
    return "Field::null";
  }
  const char* kF0 = is_static() ? " static" : "";
  const char* kF1 = is_final() ? " final" : "";
  const char* kF2 = is_const() ? " const" : "";
  const char* kFormat = "Field <%s.%s>:%s%s%s";
  const char* field_name = String::Handle(name()).ToCString();
  const Class& cls = Class::Handle(owner());
  const char* cls_name = String::Handle(cls.Name()).ToCString();
  intptr_t len =
      OS::SNPrint(NULL, 0, kFormat, cls_name, field_name, kF0, kF1, kF2) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, cls_name, field_name, kF0, kF1, kF2);
  return chars;
}


void Field::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
  const char* internal_field_name = String::Handle(name()).ToCString();
  const char* field_name = String::Handle(UserVisibleName()).ToCString();
  ObjectIdRing* ring = Isolate::Current()->object_id_ring();
  intptr_t id = ring->GetIdForObject(raw());
  jsobj.AddProperty("type", JSONType(ref));
  jsobj.AddProperty("id", id);
  jsobj.AddProperty("name", internal_field_name);
  jsobj.AddProperty("user_name", field_name);
  if (ref) {
    return;
  }
  Class& cls = Class::Handle(owner());
  jsobj.AddProperty("type", "Field");
  jsobj.AddProperty("id", id);
  jsobj.AddProperty("name", internal_field_name);
  jsobj.AddProperty("user_name", field_name);
  jsobj.AddProperty("class", cls);
  jsobj.AddProperty("static", is_static());
  jsobj.AddProperty("final", is_final());
  jsobj.AddProperty("const", is_const());
  jsobj.AddProperty("guard_nullable", is_nullable());
  if (guarded_cid() == kIllegalCid) {
    jsobj.AddProperty("guard_class", "unknown");
  } else if (guarded_cid() == kDynamicCid) {
    jsobj.AddProperty("guard_class", "dynamic");
  } else {
    ClassTable* table = Isolate::Current()->class_table();
    ASSERT(table->IsValidIndex(guarded_cid()));
    cls ^= table->At(guarded_cid());
    jsobj.AddProperty("guard_class", cls);
  }
  if (guarded_list_length() == kUnknownFixedLength) {
    jsobj.AddProperty("guard_length", "unknown");
  } else if (guarded_list_length() == kNoFixedLength) {
    jsobj.AddProperty("guard_length", "variable");
  } else {
    jsobj.AddProperty("guard_length", guarded_list_length());
  }
}


RawArray* Field::dependent_code() const {
  return raw_ptr()->dependent_code_;
}


void Field::set_dependent_code(const Array& array) const {
  raw_ptr()->dependent_code_ = array.raw();
}


void Field::RegisterDependentCode(const Code& code) const {
  const Array& dependent = Array::Handle(dependent_code());

  if (!dependent.IsNull()) {
    // Try to find and reuse cleared WeakProperty to avoid allocating new one.
    WeakProperty& weak_property = WeakProperty::Handle();
    for (intptr_t i = 0; i < dependent.Length(); i++) {
      weak_property ^= dependent.At(i);
      if (weak_property.key() == Code::null()) {
        // Empty property found. Reuse it.
        weak_property.set_key(code);
        return;
      }
    }
  }

  const WeakProperty& weak_property = WeakProperty::Handle(
      WeakProperty::New(Heap::kOld));
  weak_property.set_key(code);

  intptr_t length = dependent.IsNull() ? 0 : dependent.Length();
  const Array& new_dependent = Array::Handle(
      Array::Grow(dependent, length + 1, Heap::kOld));
  new_dependent.SetAt(length, weak_property);
  set_dependent_code(new_dependent);
}


static bool IsDependentCode(const Array& dependent_code, const Code& code) {
  if (!code.is_optimized()) {
    return false;
  }

  WeakProperty& weak_property = WeakProperty::Handle();
  for (intptr_t i = 0; i < dependent_code.Length(); i++) {
    weak_property ^= dependent_code.At(i);
    if (code.raw() == weak_property.key()) {
      return true;
    }
  }

  return false;
}


void Field::DeoptimizeDependentCode() const {
  const Array& code_objects = Array::Handle(dependent_code());

  if (code_objects.IsNull()) {
    return;
  }
  set_dependent_code(Object::null_array());

  // Deoptimize all dependent code on the stack.
  Code& code = Code::Handle();
  {
    DartFrameIterator iterator;
    StackFrame* frame = iterator.NextFrame();
    while (frame != NULL) {
      code = frame->LookupDartCode();
      if (IsDependentCode(code_objects, code)) {
        if (FLAG_trace_deoptimization || FLAG_trace_deoptimization_verbose) {
          Function& function = Function::Handle(code.function());
          OS::PrintErr("Deoptimizing %s because guard on field %s failed.\n",
              function.ToFullyQualifiedCString(),
              ToCString());
        }
        DeoptimizeAt(code, frame->pc());
      }
      frame = iterator.NextFrame();
    }
  }

  // Switch functions that use dependent code to unoptimized code.
  WeakProperty& weak_property = WeakProperty::Handle();
  Function& function = Function::Handle();
  for (intptr_t i = 0; i < code_objects.Length(); i++) {
    weak_property ^= code_objects.At(i);
    code ^= weak_property.key();
    if (code.IsNull()) {
      // Code was garbage collected already.
      continue;
    }

    function ^= code.function();
    // If function uses dependent code switch it to unoptimized.
    if (function.CurrentCode() == code.raw()) {
      ASSERT(function.HasOptimizedCode());
      if (FLAG_trace_deoptimization || FLAG_trace_deoptimization_verbose) {
        OS::PrintErr("Switching %s to unoptimized code because guard"
                     " on field %s was violated.\n",
                     function.ToFullyQualifiedCString(),
                     ToCString());
      }
      function.SwitchToUnoptimizedCode();
    }
  }
}


bool Field::IsUninitialized() const {
  const Instance& value = Instance::Handle(raw_ptr()->value_);
  ASSERT(value.raw() != Object::transition_sentinel().raw());
  return value.raw() == Object::sentinel().raw();
}


static intptr_t GetListLength(const Object& value) {
  const intptr_t cid = value.GetClassId();
  ASSERT(RawObject::IsBuiltinListClassId(cid));
  // Extract list length.
  if (value.IsTypedData()) {
    const TypedData& list = TypedData::Cast(value);
    return list.Length();
  } else if (value.IsArray()) {
    const Array& list = Array::Cast(value);
    return list.Length();
  } else if (value.IsGrowableObjectArray()) {
    // List length is variable.
    return Field::kNoFixedLength;
  } else if (value.IsExternalTypedData()) {
    // TODO(johnmccutchan): Enable for external typed data.
    return Field::kNoFixedLength;
  } else if (RawObject::IsTypedDataViewClassId(cid)) {
    // TODO(johnmccutchan): Enable for typed data views.
    return Field::kNoFixedLength;
  }
  UNIMPLEMENTED();
  return Field::kNoFixedLength;
}


bool Field::UpdateGuardedCidAndLength(const Object& value) const {
  const intptr_t cid = value.GetClassId();
  bool deoptimize = UpdateCid(cid);
  intptr_t list_length = Field::kNoFixedLength;
  if ((guarded_cid() != kDynamicCid) &&
      is_final() && RawObject::IsBuiltinListClassId(cid)) {
    list_length = GetListLength(value);
  }
  deoptimize = UpdateLength(list_length) || deoptimize;
  if (deoptimize) {
    DeoptimizeDependentCode();
  }
  return deoptimize;
}


bool Field::UpdateCid(intptr_t cid) const {
  if (guarded_cid() == kIllegalCid) {
    // Field is assigned first time.
    set_guarded_cid(cid);
    set_is_nullable(cid == kNullCid);
    return false;
  }

  if ((cid == guarded_cid()) || ((cid == kNullCid) && is_nullable())) {
    // Class id of the assigned value matches expected class id and nullability.
    return false;
  }

  if ((cid == kNullCid) && !is_nullable()) {
    // Assigning null value to a non-nullable field makes it nullable.
    set_is_nullable(true);
  } else if ((cid != kNullCid) && (guarded_cid() == kNullCid)) {
    // Assigning non-null value to a field that previously contained only null
    // turns it into a nullable field with the given class id.
    ASSERT(is_nullable());
    set_guarded_cid(cid);
  } else {
    // Give up on tracking class id of values contained in this field.
    ASSERT(guarded_cid() != cid);
    set_guarded_cid(kDynamicCid);
    set_is_nullable(true);
  }

  // Expected class id or nullability of the field changed.
  return true;
}


bool Field::UpdateLength(intptr_t list_length) const {
  ASSERT(is_final() || (!is_final() &&
                        (list_length < Field::kUnknownFixedLength)));
  ASSERT((list_length == Field::kNoFixedLength) ||
         (list_length > Field::kUnknownFixedLength));
  ASSERT(guarded_cid() != kIllegalCid);

  const bool force_invalidate = (guarded_cid() == kDynamicCid) &&
                                (list_length != Field::kNoFixedLength);

  const bool list_length_unknown =
      (guarded_list_length() == Field::kUnknownFixedLength);
  const bool list_length_changed = (guarded_list_length() != list_length);

  if (list_length_unknown && list_length_changed && !force_invalidate) {
    // List length set for first time.
    set_guarded_list_length(list_length);
    return false;
  }

  if (!list_length_changed && !force_invalidate) {
    // List length unchanged.
    return false;
  }
  // Multiple list lengths assigned here, stop tracking length.
  set_guarded_list_length(Field::kNoFixedLength);
  return true;
}


void LiteralToken::set_literal(const String& literal) const {
  StorePointer(&raw_ptr()->literal_, literal.raw());
}


void LiteralToken::set_value(const Object& value) const {
  StorePointer(&raw_ptr()->value_, value.raw());
}


RawLiteralToken* LiteralToken::New() {
  ASSERT(Object::literal_token_class() != Class::null());
  RawObject* raw = Object::Allocate(LiteralToken::kClassId,
                                    LiteralToken::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawLiteralToken*>(raw);
}


RawLiteralToken* LiteralToken::New(Token::Kind kind, const String& literal) {
  const LiteralToken& result = LiteralToken::Handle(LiteralToken::New());
  result.set_kind(kind);
  result.set_literal(literal);
  if (kind == Token::kINTEGER) {
    const Integer& value = Integer::Handle(Integer::NewCanonical(literal));
    ASSERT(value.IsSmi() || value.IsOld());
    result.set_value(value);
  } else if (kind == Token::kDOUBLE) {
    const Double& value = Double::Handle(Double::NewCanonical(literal));
    result.set_value(value);
  } else {
    ASSERT(Token::NeedsLiteralToken(kind));
    result.set_value(literal);
  }
  return result.raw();
}


const char* LiteralToken::ToCString() const {
  const String& token = String::Handle(literal());
  return token.ToCString();
}


void LiteralToken::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawArray* TokenStream::TokenObjects() const {
  return raw_ptr()->token_objects_;
}


void TokenStream::SetTokenObjects(const Array& value) const {
  StorePointer(&raw_ptr()->token_objects_, value.raw());
}


RawExternalTypedData* TokenStream::GetStream() const {
  return raw_ptr()->stream_;
}


void TokenStream::SetStream(const ExternalTypedData& value) const {
  StorePointer(&raw_ptr()->stream_, value.raw());
}


void TokenStream::DataFinalizer(Dart_WeakPersistentHandle handle, void *peer) {
  ASSERT(peer != NULL);
  ::free(peer);
  DeleteWeakPersistentHandle(handle);
}


RawString* TokenStream::PrivateKey() const {
  return raw_ptr()->private_key_;
}


void TokenStream::SetPrivateKey(const String& value) const {
  StorePointer(&raw_ptr()->private_key_, value.raw());
}


RawString* TokenStream::GenerateSource() const {
  Iterator iterator(*this, 0, Iterator::kAllTokens);
  const ExternalTypedData& data = ExternalTypedData::Handle(GetStream());
  const GrowableObjectArray& literals =
      GrowableObjectArray::Handle(GrowableObjectArray::New(data.Length()));
  const String& private_key = String::Handle(PrivateKey());
  intptr_t private_len = private_key.Length();

  Token::Kind curr = iterator.CurrentTokenKind();
  Token::Kind prev = Token::kILLEGAL;
  // Handles used in the loop.
  Object& obj = Object::Handle();
  String& literal = String::Handle();
  // Current indentation level.
  int indent = 0;

  while (curr != Token::kEOS) {
    // Remember current values for this token.
    obj = iterator.CurrentToken();
    literal = iterator.MakeLiteralToken(obj);
    // Advance to be able to use next token kind.
    iterator.Advance();
    Token::Kind next = iterator.CurrentTokenKind();

    // Handle the current token.
    if (curr == Token::kSTRING) {
      bool escape_characters = false;
      for (intptr_t i = 0; i < literal.Length(); i++) {
        if (IsSpecialCharacter(literal.CharAt(i))) {
          escape_characters = true;
        }
      }
      if ((prev != Token::kINTERPOL_VAR) && (prev != Token::kINTERPOL_END)) {
        literals.Add(Symbols::DoubleQuotes());
      }
      if (escape_characters) {
        literal = String::EscapeSpecialCharacters(literal);
        literals.Add(literal);
      } else {
        literals.Add(literal);
      }
      if ((next != Token::kINTERPOL_VAR) && (next != Token::kINTERPOL_START)) {
        literals.Add(Symbols::DoubleQuotes());
      }
    } else if (curr == Token::kINTERPOL_VAR) {
      literals.Add(Symbols::Dollar());
      if (literal.CharAt(0) == Scanner::kPrivateIdentifierStart) {
        literal = String::SubString(literal, 0, literal.Length() - private_len);
      }
      literals.Add(literal);
    } else if (curr == Token::kIDENT) {
      if (literal.CharAt(0) == Scanner::kPrivateIdentifierStart) {
        literal = String::SubString(literal, 0, literal.Length() - private_len);
      }
      literals.Add(literal);
    } else {
      literals.Add(literal);
    }
    // Determine the separation text based on this current token.
    const String* separator = NULL;
    switch (curr) {
      case Token::kLBRACE:
      case Token::kRBRACE:
      case Token::kPERIOD:
      case Token::kLBRACK:
      case Token::kINTERPOL_VAR:
      case Token::kINTERPOL_START:
      case Token::kINTERPOL_END:
      case Token::kBIT_NOT:
        break;
      // In case we see an opening parentheses '(' we increase the indent to
      // align multi-line parameters accordingly. The indent will be removed as
      // soon as we see the matching closing parentheses ')'.
      //
      // Example:
      // SomeVeryLongMethod(
      //     "withVeryLongParameter",
      //     "andAnotherVeryLongParameter",
      //     "andAnotherVeryLongParameter2") { ...
      case Token::kLPAREN:
        indent += 2;
        break;
      case Token::kRPAREN:
        indent -= 2;
        separator = &Symbols::Blank();
        break;
      case Token::kNEWLINE:
        if (prev == Token::kLBRACE) {
          indent++;
        }
        if (next == Token::kRBRACE) {
          indent--;
        }
        break;
      default:
        separator = &Symbols::Blank();
        break;
    }

    // Determine whether the separation text needs to be updated based on the
    // next token.
    switch (next) {
      case Token::kRBRACE:
        break;
      case Token::kSEMICOLON:
      case Token::kPERIOD:
      case Token::kCOMMA:
      case Token::kLPAREN:
      case Token::kRPAREN:
      case Token::kLBRACK:
      case Token::kRBRACK:
      case Token::kINTERPOL_VAR:
      case Token::kINTERPOL_START:
      case Token::kINTERPOL_END:
        separator = NULL;
        break;
      case Token::kELSE:
        separator = &Symbols::Blank();
        break;
      default:
        // Do nothing.
        break;
    }

    // Update the few cases where both tokens need to be taken into account.
    if (((curr == Token::kIF) || (curr == Token::kFOR)) &&
        (next == Token::kLPAREN)) {
      separator = &Symbols::Blank();
    } else if ((curr == Token::kASSIGN) && (next == Token::kLPAREN)) {
      separator = &Symbols::Blank();
    } else if ((curr == Token::kRETURN  ||
                curr == Token::kCONDITIONAL ||
                Token::IsBinaryOperator(curr) ||
                Token::IsEqualityOperator(curr)) && (next == Token::kLPAREN)) {
      separator = &Symbols::Blank();
    } else if ((curr == Token::kLBRACE) && (next == Token::kRBRACE)) {
      separator = NULL;
    } else if ((curr == Token::kSEMICOLON) && (next != Token::kNEWLINE)) {
      separator = &Symbols::Blank();
    }

    // Add the separator.
    if (separator != NULL) {
      literals.Add(*separator);
    }

    // Account for indentation in case we printed a newline.
    if (curr == Token::kNEWLINE) {
        for (int i = 0; i < indent; i++) {
          literals.Add(Symbols::TwoSpaces());
        }
    }

    // Setup for next iteration.
    prev = curr;
    curr = next;
  }
  const Array& source = Array::Handle(Array::MakeArray(literals));
  return String::ConcatAll(source);
}


intptr_t TokenStream::ComputeSourcePosition(intptr_t tok_pos) const {
  Iterator iterator(*this, 0, Iterator::kAllTokens);
  intptr_t src_pos = 0;
  Token::Kind kind = iterator.CurrentTokenKind();
  while (iterator.CurrentPosition() < tok_pos && kind != Token::kEOS) {
    iterator.Advance();
    kind = iterator.CurrentTokenKind();
    src_pos += 1;
  }
  return src_pos;
}


RawTokenStream* TokenStream::New() {
  ASSERT(Object::token_stream_class() != Class::null());
  RawObject* raw = Object::Allocate(TokenStream::kClassId,
                                    TokenStream::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawTokenStream*>(raw);
}


RawTokenStream* TokenStream::New(intptr_t len) {
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in TokenStream::New: invalid len %" Pd "\n", len);
  }
  uint8_t* data = reinterpret_cast<uint8_t*>(::malloc(len));
  ASSERT(data != NULL);
  const ExternalTypedData& stream = ExternalTypedData::Handle(
      ExternalTypedData::New(kExternalTypedDataUint8ArrayCid,
                             data, len, Heap::kOld));
  stream.AddFinalizer(data, DataFinalizer);
  const TokenStream& result = TokenStream::Handle(TokenStream::New());
  result.SetStream(stream);
  return result.raw();
}


// Helper class for creation of compressed token stream data.
class CompressedTokenStreamData : public ValueObject {
 public:
  static const intptr_t kInitialSize = 16 * KB;
  CompressedTokenStreamData() :
      buffer_(NULL),
      stream_(&buffer_, Reallocate, kInitialSize),
      token_objects_(GrowableObjectArray::Handle(
          GrowableObjectArray::New(kInitialTokenCount, Heap::kOld))),
      token_obj_(Object::Handle()),
      literal_token_(LiteralToken::Handle()),
      literal_str_(String::Handle()) {
    token_objects_.Add(Object::null_string());
  }
  ~CompressedTokenStreamData() {
  }

  // Add an IDENT token into the stream and the token objects array.
  void AddIdentToken(const String* ident) {
    if (ident != NULL) {
      // If the IDENT token is already in the tokens object array use the
      // same index instead of duplicating it.
      intptr_t index = FindIdentIndex(ident);
      if (index == -1) {
        WriteIndex(token_objects_.Length());
        ASSERT(ident != NULL);
        token_objects_.Add(*ident);
      } else {
        WriteIndex(index);
      }
    } else {
      WriteIndex(0);
    }
  }

  // Add a LITERAL token into the stream and the token objects array.
  void AddLiteralToken(Token::Kind kind, const String* literal) {
    if (literal != NULL) {
      // If the literal token is already in the tokens object array use the
      // same index instead of duplicating it.
      intptr_t index = FindLiteralIndex(kind, literal);
      if (index == -1) {
        WriteIndex(token_objects_.Length());
        ASSERT(literal != NULL);
        literal_token_ = LiteralToken::New(kind, *literal);
        token_objects_.Add(literal_token_);
      } else {
        WriteIndex(index);
      }
    } else {
      WriteIndex(0);
    }
  }

  // Add a simple token into the stream.
  void AddSimpleToken(intptr_t kind) {
    stream_.WriteUnsigned(kind);
  }

  // Return the compressed token stream.
  uint8_t* GetStream() const { return buffer_; }

  // Return the compressed token stream length.
  intptr_t Length() const { return stream_.bytes_written(); }

  // Return the token objects array.
  const GrowableObjectArray& TokenObjects() const {
    return token_objects_;
  }

 private:
  intptr_t FindIdentIndex(const String* ident) {
    ASSERT(ident != NULL);
    intptr_t hash_value = ident->Hash() % kTableSize;
    GrowableArray<intptr_t>& value = ident_table_[hash_value];
    for (intptr_t i = 0; i < value.length(); i++) {
      intptr_t index = value[i];
      token_obj_ = token_objects_.At(index);
      if (token_obj_.IsString()) {
        const String& ident_str = String::Cast(token_obj_);
        if (ident->Equals(ident_str)) {
          return index;
        }
      }
    }
    value.Add(token_objects_.Length());
    return -1;
  }

  intptr_t FindLiteralIndex(Token::Kind kind, const String* literal) {
    ASSERT(literal != NULL);
    intptr_t hash_value = literal->Hash() % kTableSize;
    GrowableArray<intptr_t>& value = literal_table_[hash_value];
    for (intptr_t i = 0; i < value.length(); i++) {
      intptr_t index = value[i];
      token_obj_ = token_objects_.At(index);
      if (token_obj_.IsLiteralToken()) {
        const LiteralToken& token = LiteralToken::Cast(token_obj_);
        literal_str_ = token.literal();
        if (kind == token.kind() && literal->Equals(literal_str_)) {
          return index;
        }
      }
    }
    value.Add(token_objects_.Length());
    return -1;
  }

  void WriteIndex(intptr_t value) {
    stream_.WriteUnsigned(value + Token::kNumTokens);
  }

  static uint8_t* Reallocate(uint8_t* ptr,
                             intptr_t old_size,
                             intptr_t new_size) {
    void* new_ptr = ::realloc(reinterpret_cast<void*>(ptr), new_size);
    return reinterpret_cast<uint8_t*>(new_ptr);
  }

  static const int kInitialTokenCount = 32;
  static const intptr_t kTableSize = 1024;

  uint8_t* buffer_;
  WriteStream stream_;
  GrowableArray<intptr_t> ident_table_[kTableSize];
  GrowableArray<intptr_t> literal_table_[kTableSize];
  const GrowableObjectArray& token_objects_;
  Object& token_obj_;
  LiteralToken& literal_token_;
  String& literal_str_;

  DISALLOW_COPY_AND_ASSIGN(CompressedTokenStreamData);
};


RawTokenStream* TokenStream::New(const Scanner::GrowableTokenStream& tokens,
                                 const String& private_key) {
  // Copy the relevant data out of the scanner into a compressed stream of
  // tokens.
  CompressedTokenStreamData data;
  intptr_t len = tokens.length();
  for (intptr_t i = 0; i < len; i++) {
    Scanner::TokenDescriptor token = tokens[i];
    if (token.kind == Token::kIDENT) {  // Identifier token.
      if (FLAG_compiler_stats) {
        CompilerStats::num_ident_tokens_total += 1;
      }
      data.AddIdentToken(token.literal);
    } else if (Token::NeedsLiteralToken(token.kind)) {  // Literal token.
      if (FLAG_compiler_stats) {
        CompilerStats::num_literal_tokens_total += 1;
      }
      data.AddLiteralToken(token.kind, token.literal);
    } else {  // Keyword, pseudo keyword etc.
      ASSERT(token.kind < Token::kNumTokens);
      data.AddSimpleToken(token.kind);
    }
  }
  if (FLAG_compiler_stats) {
    CompilerStats::num_tokens_total += len;
  }
  data.AddSimpleToken(Token::kEOS);  // End of stream.

  // Create and setup the token stream object.
  const ExternalTypedData& stream = ExternalTypedData::Handle(
      ExternalTypedData::New(kExternalTypedDataUint8ArrayCid,
                             data.GetStream(), data.Length(), Heap::kOld));
  stream.AddFinalizer(data.GetStream(), DataFinalizer);
  const TokenStream& result = TokenStream::Handle(New());
  result.SetPrivateKey(private_key);
  {
    NoGCScope no_gc;
    result.SetStream(stream);
    const Array& tokens = Array::Handle(Array::MakeArray(data.TokenObjects()));
    result.SetTokenObjects(tokens);
  }
  return result.raw();
}


const char* TokenStream::ToCString() const {
  return "TokenStream";
}


void TokenStream::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


TokenStream::Iterator::Iterator(const TokenStream& tokens,
                                intptr_t token_pos,
                                Iterator::StreamType stream_type)
    : tokens_(TokenStream::Handle(tokens.raw())),
      data_(ExternalTypedData::Handle(tokens.GetStream())),
      stream_(reinterpret_cast<uint8_t*>(data_.DataAddr(0)), data_.Length()),
      token_objects_(Array::Handle(tokens.TokenObjects())),
      obj_(Object::Handle()),
      cur_token_pos_(token_pos),
      cur_token_kind_(Token::kILLEGAL),
      cur_token_obj_index_(-1),
      stream_type_(stream_type) {
  SetCurrentPosition(token_pos);
}


void TokenStream::Iterator::SetStream(const TokenStream& tokens,
                                      intptr_t token_pos) {
  tokens_ = tokens.raw();
  data_ = tokens.GetStream();
  stream_.SetStream(reinterpret_cast<uint8_t*>(data_.DataAddr(0)),
                    data_.Length());
  token_objects_ = tokens.TokenObjects();
  obj_ = Object::null();
  cur_token_pos_ = token_pos;
  cur_token_kind_ = Token::kILLEGAL;
  cur_token_obj_index_ = -1;
  SetCurrentPosition(token_pos);
}


bool TokenStream::Iterator::IsValid() const {
  return !tokens_.IsNull();
}


Token::Kind TokenStream::Iterator::LookaheadTokenKind(intptr_t num_tokens) {
  intptr_t saved_position = stream_.Position();
  Token::Kind kind = Token::kILLEGAL;
  intptr_t value = -1;
  intptr_t count = 0;
  while (count < num_tokens && value != Token::kEOS) {
    value = ReadToken();
    if ((stream_type_ == kAllTokens) ||
        (static_cast<Token::Kind>(value) != Token::kNEWLINE)) {
      count += 1;
    }
  }
  if (value < Token::kNumTokens) {
    kind = static_cast<Token::Kind>(value);
  } else {
    value = value - Token::kNumTokens;
    obj_ = token_objects_.At(value);
    if (obj_.IsLiteralToken()) {
      const LiteralToken& literal_token = LiteralToken::Cast(obj_);
      kind = literal_token.kind();
    } else {
      ASSERT(obj_.IsString());  // Must be an identifier.
      kind = Token::kIDENT;
    }
  }
  stream_.SetPosition(saved_position);
  return kind;
}


intptr_t TokenStream::Iterator::CurrentPosition() const {
  return cur_token_pos_;
}


void TokenStream::Iterator::SetCurrentPosition(intptr_t value) {
  stream_.SetPosition(value);
  Advance();
}


void TokenStream::Iterator::Advance() {
  intptr_t value;
  do {
    cur_token_pos_ = stream_.Position();
    value = ReadToken();
  } while ((stream_type_ == kNoNewlines) &&
           (static_cast<Token::Kind>(value) == Token::kNEWLINE));
  if (value < Token::kNumTokens) {
    cur_token_kind_ = static_cast<Token::Kind>(value);
    cur_token_obj_index_ = -1;
    return;
  }
  cur_token_obj_index_ = value - Token::kNumTokens;
  obj_ = token_objects_.At(cur_token_obj_index_);
  if (obj_.IsLiteralToken()) {
    const LiteralToken& literal_token = LiteralToken::Cast(obj_);
    cur_token_kind_ = literal_token.kind();
    return;
  }
  ASSERT(obj_.IsString());  // Must be an identifier.
  cur_token_kind_ = Token::kIDENT;
}


RawObject* TokenStream::Iterator::CurrentToken() const {
  if (cur_token_obj_index_ != -1) {
    return token_objects_.At(cur_token_obj_index_);
  } else {
    return Smi::New(cur_token_kind_);
  }
}


RawString* TokenStream::Iterator::CurrentLiteral() const {
  obj_ = CurrentToken();
  return MakeLiteralToken(obj_);
}


RawString* TokenStream::Iterator::MakeLiteralToken(const Object& obj) const {
  if (obj.IsString()) {
    return reinterpret_cast<RawString*>(obj.raw());
  } else if (obj.IsSmi()) {
    Token::Kind kind = static_cast<Token::Kind>(
        Smi::Value(reinterpret_cast<RawSmi*>(obj.raw())));
    ASSERT(kind < Token::kNumTokens);
    if (Token::IsPseudoKeyword(kind) || Token::IsKeyword(kind)) {
      Isolate* isolate = Isolate::Current();
      ObjectStore* object_store = isolate->object_store();
      const Array& symbols = Array::Handle(isolate,
                                           object_store->keyword_symbols());
      ASSERT(!symbols.IsNull());
      ASSERT(symbols.At(kind - Token::kFirstKeyword) != Object::null());
      return String::RawCast(symbols.At(kind - Token::kFirstKeyword));
    }
    return Symbols::New(Token::Str(kind));
  } else {
    ASSERT(obj.IsLiteralToken());  // Must be a literal token.
    const LiteralToken& literal_token = LiteralToken::Cast(obj);
    return literal_token.literal();
  }
}


bool Script::HasSource() const {
  return raw_ptr()->source_ != String::null();
}


RawString* Script::Source() const {
  String& source = String::Handle(raw_ptr()->source_);
  if (source.IsNull()) {
    return GenerateSource();
  }
  return raw_ptr()->source_;
}


RawString* Script::GenerateSource() const {
  const TokenStream& token_stream = TokenStream::Handle(tokens());
  return token_stream.GenerateSource();
}


void Script::set_url(const String& value) const {
  StorePointer(&raw_ptr()->url_, value.raw());
}


void Script::set_source(const String& value) const {
  StorePointer(&raw_ptr()->source_, value.raw());
}


void Script::set_kind(RawScript::Kind value) const {
  raw_ptr()->kind_ = value;
}


void Script::set_tokens(const TokenStream& value) const {
  StorePointer(&raw_ptr()->tokens_, value.raw());
}


void Script::Tokenize(const String& private_key) const {
  const TokenStream& tkns = TokenStream::Handle(tokens());
  if (!tkns.IsNull()) {
    // Already tokenized.
    return;
  }

  // Get the source, scan and allocate the token stream.
  TimerScope timer(FLAG_compiler_stats, &CompilerStats::scanner_timer);
  const String& src = String::Handle(Source());
  Scanner scanner(src, private_key);
  set_tokens(TokenStream::Handle(TokenStream::New(scanner.GetStream(),
                                                  private_key)));
  if (FLAG_compiler_stats) {
    CompilerStats::src_length += src.Length();
  }
}


void Script::SetLocationOffset(intptr_t line_offset,
                               intptr_t col_offset) const {
  ASSERT(line_offset >= 0);
  ASSERT(col_offset >= 0);
  raw_ptr()->line_offset_ = line_offset;
  raw_ptr()->col_offset_ = col_offset;
}


void Script::GetTokenLocation(intptr_t token_pos,
                              intptr_t* line,
                              intptr_t* column) const {
  ASSERT(line != NULL);
  const TokenStream& tkns = TokenStream::Handle(tokens());
  if (column == NULL) {
    TokenStream::Iterator tkit(tkns, 0, TokenStream::Iterator::kAllTokens);
    intptr_t cur_line = line_offset() + 1;
    while (tkit.CurrentPosition() < token_pos &&
           tkit.CurrentTokenKind() != Token::kEOS) {
      if (tkit.CurrentTokenKind() == Token::kNEWLINE) {
        cur_line++;
      }
      tkit.Advance();
    }
    *line = cur_line;
  } else {
    const String& src = String::Handle(Source());
    intptr_t src_pos = tkns.ComputeSourcePosition(token_pos);
    Scanner scanner(src, Symbols::Empty());
    scanner.ScanTo(src_pos);
    intptr_t relative_line = scanner.CurrentPosition().line;
    *line = relative_line + line_offset();
    *column = scanner.CurrentPosition().column;
    // On the first line of the script we must add the column offset.
    if (relative_line == 1) {
      *column += col_offset();
    }
  }
}


void Script::TokenRangeAtLine(intptr_t line_number,
                              intptr_t* first_token_index,
                              intptr_t* last_token_index) const {
  ASSERT(first_token_index != NULL && last_token_index != NULL);
  ASSERT(line_number > 0);
  *first_token_index = -1;
  *last_token_index = -1;
  const TokenStream& tkns = TokenStream::Handle(tokens());
  line_number -= line_offset();
  if (line_number < 1) line_number = 1;
  TokenStream::Iterator tkit(tkns, 0, TokenStream::Iterator::kAllTokens);
  // Scan through the token stream to the required line.
  intptr_t cur_line = 1;
  while (cur_line < line_number && tkit.CurrentTokenKind() != Token::kEOS) {
    if (tkit.CurrentTokenKind() == Token::kNEWLINE) {
      cur_line++;
    }
    tkit.Advance();
  }
  if (tkit.CurrentTokenKind() == Token::kEOS) {
    // End of token stream before reaching required line.
    return;
  }
  if (tkit.CurrentTokenKind() == Token::kNEWLINE) {
    // No tokens on the current line. If there is a valid token afterwards, put
    // it into first_token_index.
    while (tkit.CurrentTokenKind() == Token::kNEWLINE &&
           tkit.CurrentTokenKind() != Token::kEOS) {
      tkit.Advance();
    }
    if (tkit.CurrentTokenKind() != Token::kEOS) {
      *first_token_index = tkit.CurrentPosition();
    }
    return;
  }
  *first_token_index = tkit.CurrentPosition();
  // We cannot do "CurrentPosition() - 1" for the last token, because we do not
  // know whether the previous token is a simple one or not.
  intptr_t end_pos = *first_token_index;
  while (tkit.CurrentTokenKind() != Token::kNEWLINE &&
         tkit.CurrentTokenKind() != Token::kEOS) {
    end_pos = tkit.CurrentPosition();
    tkit.Advance();
  }
  *last_token_index = end_pos;
}


RawString* Script::GetLine(intptr_t line_number) const {
  const String& src = String::Handle(Source());
  intptr_t relative_line_number = line_number - line_offset();
  intptr_t current_line = 1;
  intptr_t line_start_idx = -1;
  intptr_t last_char_idx = -1;
  for (intptr_t ix = 0;
       (ix < src.Length()) && (current_line <= relative_line_number);
       ix++) {
    if ((current_line == relative_line_number) && (line_start_idx < 0)) {
      line_start_idx = ix;
    }
    if (src.CharAt(ix) == '\n') {
      current_line++;
    } else if (src.CharAt(ix) == '\r') {
      if ((ix + 1 != src.Length()) && (src.CharAt(ix + 1) != '\n')) {
        current_line++;
      }
    } else {
      last_char_idx = ix;
    }
  }
  // Guarantee that returned string is never NULL.

  if (line_start_idx >= 0) {
    return String::SubString(src,
                             line_start_idx,
                             last_char_idx - line_start_idx + 1);
  } else {
    return Symbols::Empty().raw();
  }
}


RawString* Script::GetSnippet(intptr_t from_line,
                              intptr_t from_column,
                              intptr_t to_line,
                              intptr_t to_column) const {
  const String& src = String::Handle(Source());
  intptr_t length = src.Length();
  intptr_t line = 1 + line_offset();
  intptr_t column = 1;
  intptr_t lookahead = 0;
  intptr_t snippet_start = -1;
  intptr_t snippet_end = -1;
  if (from_line - line_offset() == 1) {
    column += col_offset();
  }
  char c = src.CharAt(lookahead);
  while (lookahead != length) {
    if (snippet_start == -1) {
      if ((line == from_line) && (column == from_column)) {
        snippet_start = lookahead;
      }
    } else if ((line == to_line) && (column == to_column)) {
      snippet_end = lookahead;
      break;
    }
    if (c == '\n') {
      line++;
      column = 0;
    }
    column++;
    lookahead++;
    if (lookahead != length) {
      // Replace '\r' with '\n' and a sequence of '\r' '\n' with a single '\n'.
      if (src.CharAt(lookahead) == '\r') {
        c = '\n';
        if (lookahead + 1 != length && src.CharAt(lookahead) == '\n') {
          lookahead++;
        }
      } else {
        c = src.CharAt(lookahead);
      }
    }
  }
  String& snippet = String::Handle();
  if ((snippet_start != -1) && (snippet_end != -1)) {
    snippet =
        String::SubString(src, snippet_start, snippet_end - snippet_start);
  }
  return snippet.raw();
}


RawScript* Script::New() {
  ASSERT(Object::script_class() != Class::null());
  RawObject* raw = Object::Allocate(Script::kClassId,
                                    Script::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawScript*>(raw);
}


RawScript* Script::New(const String& url,
                       const String& source,
                       RawScript::Kind kind) {
  const Script& result = Script::Handle(Script::New());
  result.set_url(String::Handle(Symbols::New(url)));
  result.set_source(source);
  result.set_kind(kind);
  result.SetLocationOffset(0, 0);
  return result.raw();
}


const char* Script::ToCString() const {
  return "Script";
}


void Script::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


DictionaryIterator::DictionaryIterator(const Library& library)
    : array_(Array::Handle(library.dictionary())),
      // Last element in array is a Smi indicating the number of entries used.
      size_(Array::Handle(library.dictionary()).Length() - 1),
      next_ix_(0) {
  MoveToNextObject();
}


RawObject* DictionaryIterator::GetNext() {
  ASSERT(HasNext());
  int ix = next_ix_++;
  MoveToNextObject();
  ASSERT(array_.At(ix) != Object::null());
  return array_.At(ix);
}


void DictionaryIterator::MoveToNextObject() {
  Object& obj = Object::Handle(array_.At(next_ix_));
  while (obj.IsNull() && HasNext()) {
    next_ix_++;
    obj = array_.At(next_ix_);
  }
}


ClassDictionaryIterator::ClassDictionaryIterator(const Library& library,
                                                 IterationKind kind)
    : DictionaryIterator(library),
      anon_array_((kind == kIteratePrivate) ?
          Array::Handle(library.anonymous_classes()) : Object::empty_array()),
      anon_size_((kind == kIteratePrivate) ?
                 library.num_anonymous_classes() : 0),
      anon_ix_(0) {
  MoveToNextClass();
}


RawClass* ClassDictionaryIterator::GetNextClass() {
  ASSERT(HasNext());
  Class& cls = Class::Handle();
  if (next_ix_ < size_) {
    int ix = next_ix_++;
    cls ^= array_.At(ix);
    MoveToNextClass();
    return cls.raw();
  }
  ASSERT(anon_ix_ < anon_size_);
  cls ^= anon_array_.At(anon_ix_++);
  return cls.raw();
}


void ClassDictionaryIterator::MoveToNextClass() {
  Object& obj = Object::Handle();
  while (next_ix_ < size_) {
    obj = array_.At(next_ix_);
    if (obj.IsClass()) {
      return;
    }
    next_ix_++;
  }
}


LibraryPrefixIterator::LibraryPrefixIterator(const Library& library)
    : DictionaryIterator(library) {
  Advance();
}


RawLibraryPrefix* LibraryPrefixIterator::GetNext() {
  ASSERT(HasNext());
  int ix = next_ix_++;
  Object& obj = Object::Handle(array_.At(ix));
  Advance();
  return LibraryPrefix::Cast(obj).raw();
}


void LibraryPrefixIterator::Advance() {
  Object& obj = Object::Handle(array_.At(next_ix_));
  while (!obj.IsLibraryPrefix() && HasNext()) {
    next_ix_++;
    obj = array_.At(next_ix_);
  }
}


void Library::SetName(const String& name) const {
  // Only set name once.
  ASSERT(!Loaded());
  ASSERT(name.IsSymbol());
  StorePointer(&raw_ptr()->name_, name.raw());
}


void Library::SetLoadInProgress() const {
  // Should not be already loaded.
  ASSERT(raw_ptr()->load_state_ == RawLibrary::kAllocated);
  raw_ptr()->load_state_ = RawLibrary::kLoadInProgress;
}


void Library::SetLoaded() const {
  // Should not be already loaded or just allocated.
  ASSERT(LoadInProgress());
  raw_ptr()->load_state_ = RawLibrary::kLoaded;
}


void Library::SetLoadError() const {
  // Should not be already loaded or just allocated.
  ASSERT(LoadInProgress());
  raw_ptr()->load_state_ = RawLibrary::kLoadError;
}


static RawString* MakeClassMetaName(const Class& cls) {
  String& cname = String::Handle(cls.Name());
  return String::Concat(Symbols::At(), cname);
}


static RawString* MakeFieldMetaName(const Field& field) {
  const String& cname =
      String::Handle(MakeClassMetaName(Class::Handle(field.origin())));
  String& fname = String::Handle(field.name());
  fname = String::Concat(Symbols::At(), fname);
  return String::Concat(cname, fname);
}


static RawString* MakeFunctionMetaName(const Function& func) {
  const String& cname =
      String::Handle(MakeClassMetaName(Class::Handle(func.origin())));
  String& fname = String::Handle(func.name());
  fname = String::Concat(Symbols::At(), fname);
  return String::Concat(cname, fname);
}


static RawString* MakeTypeParameterMetaName(const TypeParameter& param) {
  const String& cname = String::Handle(
      MakeClassMetaName(Class::Handle(param.parameterized_class())));
  String& pname = String::Handle(param.name());
  pname = String::Concat(Symbols::At(), pname);
  return String::Concat(cname, pname);
}


void Library::AddMetadata(const Class& cls,
                          const String& name,
                          intptr_t token_pos) const {
  const String& metaname = String::Handle(Symbols::New(name));
  Field& field = Field::Handle(Field::New(metaname,
                                          true,   // is_static
                                          false,  // is_final
                                          false,  // is_const
                                          cls,
                                          token_pos));
  field.set_type(Type::Handle(Type::DynamicType()));
  field.set_value(Array::empty_array());
  GrowableObjectArray& metadata =
      GrowableObjectArray::Handle(this->metadata());
  metadata.Add(field, Heap::kOld);
}


void Library::AddClassMetadata(const Class& cls, intptr_t token_pos) const {
  AddMetadata(cls, String::Handle(MakeClassMetaName(cls)), token_pos);
}


void Library::AddFieldMetadata(const Field& field,
                               intptr_t token_pos) const {
  AddMetadata(Class::Handle(field.origin()),
              String::Handle(MakeFieldMetaName(field)),
              token_pos);
}


void Library::AddFunctionMetadata(const Function& func,
                                  intptr_t token_pos) const {
  AddMetadata(Class::Handle(func.origin()),
              String::Handle(MakeFunctionMetaName(func)),
              token_pos);
}


void Library::AddTypeParameterMetadata(const TypeParameter& param,
                                       intptr_t token_pos) const {
  AddMetadata(Class::Handle(param.parameterized_class()),
              String::Handle(MakeTypeParameterMetaName(param)),
              token_pos);
}


void Library::AddLibraryMetadata(const Class& cls, intptr_t token_pos) const {
  AddMetadata(cls, Symbols::TopLevel(), token_pos);
}


RawString* Library::MakeMetadataName(const Object& obj) const {
  if (obj.IsClass()) {
    return MakeClassMetaName(Class::Cast(obj));
  } else if (obj.IsField()) {
    return MakeFieldMetaName(Field::Cast(obj));
  } else if (obj.IsFunction()) {
    return MakeFunctionMetaName(Function::Cast(obj));
  } else if (obj.IsLibrary()) {
    return Symbols::TopLevel().raw();
  } else if (obj.IsTypeParameter()) {
    return MakeTypeParameterMetaName(TypeParameter::Cast(obj));
  }
  UNIMPLEMENTED();
  return String::null();
}


RawField* Library::GetMetadataField(const String& metaname) const {
  const GrowableObjectArray& metadata =
      GrowableObjectArray::Handle(this->metadata());
  Field& entry = Field::Handle();
  String& entryname = String::Handle();
  intptr_t num_entries = metadata.Length();
  for (intptr_t i = 0; i < num_entries; i++) {
    entry ^= metadata.At(i);
    entryname = entry.name();
    if (entryname.Equals(metaname)) {
      return entry.raw();
    }
  }
  return Field::null();
}


RawObject* Library::GetMetadata(const Object& obj) const {
  if (!obj.IsClass() && !obj.IsField() && !obj.IsFunction() &&
      !obj.IsLibrary() && !obj.IsTypeParameter()) {
    return Object::null();
  }
  const String& metaname = String::Handle(MakeMetadataName(obj));
  Field& field = Field::Handle(GetMetadataField(metaname));
  if (field.IsNull()) {
    // There is no metadata for this object.
    return Object::empty_array().raw();;
  }
  Object& metadata = Object::Handle();
  metadata = field.value();
  if (field.value() == Object::empty_array().raw()) {
    metadata = Parser::ParseMetadata(Class::Handle(field.owner()),
                                     field.token_pos());
    if (metadata.IsArray()) {
      ASSERT(Array::Cast(metadata).raw() != Object::empty_array().raw());
      field.set_value(Array::Cast(metadata));
    }
  }
  return metadata.raw();
}


void Library::GrowDictionary(const Array& dict, intptr_t dict_size) const {
  // TODO(iposva): Avoid exponential growth.
  intptr_t new_dict_size = dict_size * 2;
  const Array& new_dict =
      Array::Handle(Array::New(new_dict_size + 1, Heap::kOld));
  // Rehash all elements from the original dictionary
  // to the newly allocated array.
  Object& entry = Class::Handle();
  String& entry_name = String::Handle();
  Object& new_entry = Object::Handle();
  for (intptr_t i = 0; i < dict_size; i++) {
    entry = dict.At(i);
    if (!entry.IsNull()) {
      entry_name = entry.DictionaryName();
      ASSERT(!entry_name.IsNull());
      intptr_t hash = entry_name.Hash();
      intptr_t index = hash % new_dict_size;
      new_entry = new_dict.At(index);
      while (!new_entry.IsNull()) {
        index = (index + 1) % new_dict_size;  // Move to next element.
        new_entry = new_dict.At(index);
      }
      new_dict.SetAt(index, entry);
    }
  }
  // Copy used count.
  new_entry = dict.At(dict_size);
  new_dict.SetAt(new_dict_size, new_entry);
  // Remember the new dictionary now.
  StorePointer(&raw_ptr()->dictionary_, new_dict.raw());
}


void Library::AddObject(const Object& obj, const String& name) const {
  ASSERT(obj.IsClass() ||
         obj.IsFunction() ||
         obj.IsField() ||
         obj.IsLibraryPrefix());
  ASSERT(name.Equals(String::Handle(obj.DictionaryName())));
  ASSERT(LookupLocalObject(name) == Object::null());
  const Array& dict = Array::Handle(dictionary());
  intptr_t dict_size = dict.Length() - 1;
  intptr_t index = name.Hash() % dict_size;

  Object& entry = Object::Handle();
  entry = dict.At(index);
  // An empty spot will be found because we keep the hash set at most 75% full.
  while (!entry.IsNull()) {
    index = (index + 1) % dict_size;
    entry = dict.At(index);
  }

  // Insert the object at the empty slot.
  dict.SetAt(index, obj);
  // One more element added.
  intptr_t used_elements = Smi::Value(Smi::RawCast(dict.At(dict_size))) + 1;
  const Smi& used = Smi::Handle(Smi::New(used_elements));
  dict.SetAt(dict_size, used);  // Update used count.

  // Rehash if symbol_table is 75% full.
  if (used_elements > ((dict_size / 4) * 3)) {
    GrowDictionary(dict, dict_size);
  }

  // Invalidate the cache of loaded scripts.
  if (loaded_scripts() != Array::null()) {
    StorePointer(&raw_ptr()->loaded_scripts_, Array::null());
  }
}


// Lookup a name in the library's re-export namespace. The name is
// unmangled, i.e. no getter or setter names should be looked up.
RawObject* Library::LookupReExport(const String& name) const {
  if (HasExports()) {
    const Array& exports = Array::Handle(this->exports());
    // Break potential export cycle while looking up name.
    StorePointer(&raw_ptr()->exports_, Object::empty_array().raw());
    Namespace& ns = Namespace::Handle();
    Object& obj = Object::Handle();
    for (int i = 0; i < exports.Length(); i++) {
      ns ^= exports.At(i);
      obj = ns.Lookup(name);
      if (!obj.IsNull()) {
        break;
      }
    }
    StorePointer(&raw_ptr()->exports_, exports.raw());
    return obj.raw();
  }
  return Object::null();
}


RawObject* Library::LookupEntry(const String& name, intptr_t *index) const {
  Isolate* isolate = Isolate::Current();
  ReusableHandleScope reused_handles(isolate);
  Array& dict = reused_handles.ArrayHandle();
  dict ^= dictionary();
  intptr_t dict_size = dict.Length() - 1;
  *index = name.Hash() % dict_size;

  Object& entry = reused_handles.ObjectHandle();
  String& entry_name = reused_handles.StringHandle();
  entry = dict.At(*index);
  // Search the entry in the hash set.
  while (!entry.IsNull()) {
    entry_name = entry.DictionaryName();
    ASSERT(!entry_name.IsNull());
     if (entry_name.Equals(name)) {
      return entry.raw();
    }
    *index = (*index + 1) % dict_size;
    entry = dict.At(*index);
  }
  return Object::null();
}


void Library::ReplaceObject(const Object& obj, const String& name) const {
  ASSERT(obj.IsClass() || obj.IsFunction() || obj.IsField());
  ASSERT(LookupLocalObject(name) != Object::null());

  intptr_t index;
  LookupEntry(name, &index);
  // The value is guaranteed to be found.
  const Array& dict = Array::Handle(dictionary());
  dict.SetAt(index, obj);
}


void Library::AddClass(const Class& cls) const {
  AddObject(cls, String::Handle(cls.Name()));
  // Link class to this library.
  cls.set_library(*this);
}

static void AddScriptIfUnique(const GrowableObjectArray& scripts,
                              Script& candidate) {
  if (candidate.IsNull()) {
    return;
  }
  Script& script_obj = Script::Handle();

  for (int i = 0; i < scripts.Length(); i++) {
    script_obj ^= scripts.At(i);
    if (script_obj.raw() == candidate.raw()) {
      // We already have a reference to this script.
      return;
    }
  }
  // Add script to the list of scripts.
  scripts.Add(candidate);
}

RawArray* Library::LoadedScripts() const {
  // We compute the list of loaded scripts lazily. The result is
  // cached in loaded_scripts_.
  if (loaded_scripts() == Array::null()) {
    // Iterate over the library dictionary and collect all scripts.
    const GrowableObjectArray& scripts =
        GrowableObjectArray::Handle(GrowableObjectArray::New(8));
    Object& entry = Object::Handle();
    Class& cls = Class::Handle();
    Class& patch_cls = Class::Handle();
    Script& owner_script = Script::Handle();
    Script& patch_script = Script::Handle();
    DictionaryIterator it(*this);
    while (it.HasNext()) {
      entry = it.GetNext();
      if (entry.IsClass()) {
        owner_script = Class::Cast(entry).script();
        patch_cls = Class::Cast(entry).patch_class();
        if (!patch_cls.IsNull()) {
          patch_script = patch_cls.script();
          AddScriptIfUnique(scripts, patch_script);
        }
      } else if (entry.IsFunction()) {
        owner_script = Function::Cast(entry).script();
      } else if (entry.IsField()) {
        cls = Field::Cast(entry).owner();
        owner_script = cls.script();
      } else {
        continue;
      }
      AddScriptIfUnique(scripts, owner_script);
    }

    // Create the array of scripts and cache it in loaded_scripts_.
    const Array& scripts_array = Array::Handle(Array::MakeArray(scripts));
    StorePointer(&raw_ptr()->loaded_scripts_, scripts_array.raw());
  }
  return loaded_scripts();
}


// TODO(hausner): we might want to add a script dictionary to the
// library class to make this lookup faster.
RawScript* Library::LookupScript(const String& url) const {
  const Array& scripts = Array::Handle(LoadedScripts());
  Script& script = Script::Handle();
  String& script_url = String::Handle();
  intptr_t num_scripts = scripts.Length();
  for (int i = 0; i < num_scripts; i++) {
    script ^= scripts.At(i);
    script_url = script.url();
    if (script_url.Equals(url)) {
      return script.raw();
    }
  }
  return Script::null();
}


RawFunction* Library::LookupFunctionInScript(const Script& script,
                                             intptr_t token_pos) const {
  Class& cls = Class::Handle();
  Function& func = Function::Handle();
  ClassDictionaryIterator it(*this, ClassDictionaryIterator::kIteratePrivate);
  while (it.HasNext()) {
    cls = it.GetNextClass();
    if (script.raw() == cls.script()) {
      func = cls.LookupFunctionAtToken(token_pos);
      if (!func.IsNull()) {
        return func.raw();
      }
    }
  }
  return Function::null();
}


RawObject* Library::LookupLocalObject(const String& name) const {
  intptr_t index;
  return LookupEntry(name, &index);
}


static bool ShouldBePrivate(const String& name) {
  return
      (name.Length() >= 1 &&
       name.CharAt(0) == '_') ||
      (name.Length() >= 5 &&
       (name.CharAt(4) == '_' &&
        (name.CharAt(0) == 'g' || name.CharAt(0) == 's') &&
        name.CharAt(1) == 'e' &&
        name.CharAt(2) == 't' &&
        name.CharAt(3) == ':'));
}


RawField* Library::LookupFieldAllowPrivate(const String& name) const {
  Object& obj = Object::Handle(LookupObjectAllowPrivate(name));
  if (obj.IsField()) {
    return Field::Cast(obj).raw();
  }
  return Field::null();
}


RawField* Library::LookupLocalField(const String& name) const {
  Object& obj = Object::Handle(LookupLocalObjectAllowPrivate(name));
  if (obj.IsField()) {
    return Field::Cast(obj).raw();
  }
  return Field::null();
}


RawFunction* Library::LookupFunctionAllowPrivate(const String& name) const {
  Object& obj = Object::Handle(LookupObjectAllowPrivate(name));
  if (obj.IsFunction()) {
    return Function::Cast(obj).raw();
  }
  return Function::null();
}


RawFunction* Library::LookupLocalFunction(const String& name) const {
  Object& obj = Object::Handle(LookupLocalObjectAllowPrivate(name));
  if (obj.IsFunction()) {
    return Function::Cast(obj).raw();
  }
  return Function::null();
}


RawObject* Library::LookupLocalObjectAllowPrivate(const String& name) const {
  Isolate* isolate = Isolate::Current();
  Object& obj = Object::Handle(isolate, Object::null());
  obj = LookupLocalObject(name);
  if (obj.IsNull() && ShouldBePrivate(name)) {
    String& private_name = String::Handle(isolate, PrivateName(name));
    obj = LookupLocalObject(private_name);
  }
  return obj.raw();
}


RawObject* Library::LookupObjectAllowPrivate(const String& name) const {
  // First check if name is found in the local scope of the library.
  Object& obj = Object::Handle(LookupLocalObjectAllowPrivate(name));
  if (!obj.IsNull()) {
    return obj.raw();
  }

  // Do not look up private names in imported libraries.
  if (ShouldBePrivate(name)) {
    return Object::null();
  }

  // Now check if name is found in any imported libs.
  return LookupImportedObject(name);
}


RawObject* Library::LookupObject(const String& name) const {
  // First check if name is found in the local scope of the library.
  Object& obj = Object::Handle(LookupLocalObject(name));
  if (!obj.IsNull()) {
    return obj.raw();
  }
  // Now check if name is found in any imported libs.
  return LookupImportedObject(name);
}


RawObject* Library::LookupImportedObject(const String& name) const {
  Object& obj = Object::Handle();
  Namespace& import = Namespace::Handle();
  Library& import_lib = Library::Handle();
  String& import_lib_url = String::Handle();
  String& first_import_lib_url = String::Handle();
  Object& found_obj = Object::Handle();
  for (intptr_t i = 0; i < num_imports(); i++) {
    import ^= ImportAt(i);
    obj = import.Lookup(name);
    if (!obj.IsNull()) {
      import_lib = import.library();
      import_lib_url = import_lib.url();
      if (found_obj.raw() != obj.raw()) {
        if (first_import_lib_url.IsNull() ||
            first_import_lib_url.StartsWith(Symbols::DartScheme())) {
          // This is the first object we found, or the
          // previously found object is exported from a Dart
          // system library. The newly found object hides the one
          // from the Dart library.
          first_import_lib_url = import_lib.url();
          found_obj = obj.raw();
        } else if (import_lib_url.StartsWith(Symbols::DartScheme())) {
          // The newly found object is exported from a Dart system
          // library. It is hidden by the previously found object.
          // We continue to search.
        } else {
          // We found two different objects with the same name.
          return Object::null();
        }
      }
    }
  }
  return found_obj.raw();
}


RawClass* Library::LookupClass(const String& name) const {
  Object& obj = Object::Handle(LookupObject(name));
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  return Class::null();
}


RawClass* Library::LookupLocalClass(const String& name) const {
  Object& obj = Object::Handle(LookupLocalObject(name));
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  return Class::null();
}


RawClass* Library::LookupClassAllowPrivate(const String& name) const {
  // See if the class is available in this library or in the top level
  // scope of any imported library.
  Isolate* isolate = Isolate::Current();
  const Class& cls = Class::Handle(isolate, LookupClass(name));
  if (!cls.IsNull()) {
    return cls.raw();
  }

  // Now try to lookup the class using its private name, but only in
  // this library (not in imported libraries).
  if (ShouldBePrivate(name)) {
    String& private_name = String::Handle(isolate, PrivateName(name));
    const Object& obj = Object::Handle(LookupLocalObject(private_name));
    if (obj.IsClass()) {
      return Class::Cast(obj).raw();
    }
  }
  return Class::null();
}


RawLibraryPrefix* Library::LookupLocalLibraryPrefix(const String& name) const {
  const Object& obj = Object::Handle(LookupLocalObject(name));
  if (obj.IsLibraryPrefix()) {
    return LibraryPrefix::Cast(obj).raw();
  }
  return LibraryPrefix::null();
}


void Library::AddAnonymousClass(const Class& cls) const {
  intptr_t num_anonymous = this->raw_ptr()->num_anonymous_;
  Array& anon_array = Array::Handle(this->raw_ptr()->anonymous_classes_);
  if (num_anonymous == anon_array.Length()) {
    intptr_t new_len = (num_anonymous == 0) ? 4 : num_anonymous * 2;
    anon_array = Array::Grow(anon_array, new_len);
    StorePointer(&raw_ptr()->anonymous_classes_, anon_array.raw());
  }
  anon_array.SetAt(num_anonymous, cls);
  num_anonymous++;
  raw_ptr()->num_anonymous_ = num_anonymous;
}


RawLibrary* Library::ImportLibraryAt(intptr_t index) const {
  Namespace& import = Namespace::Handle(ImportAt(index));
  if (import.IsNull()) {
    return Library::null();
  }
  return import.library();
}


RawNamespace* Library::ImportAt(intptr_t index) const {
  if ((index < 0) || index >= num_imports()) {
    return Namespace::null();
  }
  const Array& import_list = Array::Handle(imports());
  return Namespace::RawCast(import_list.At(index));
}


bool Library::ImportsCorelib() const {
  Isolate* isolate = Isolate::Current();
  Library& imported = Library::Handle(isolate);
  intptr_t count = num_imports();
  for (int i = 0; i < count; i++) {
    imported = ImportLibraryAt(i);
    if (imported.IsCoreLibrary()) {
      return true;
    }
  }
  LibraryPrefix& prefix = LibraryPrefix::Handle(isolate);
  LibraryPrefixIterator it(*this);
  while (it.HasNext()) {
    prefix = it.GetNext();
    count = prefix.num_imports();
    for (int i = 0; i < count; i++) {
      imported = prefix.GetLibrary(i);
      if (imported.IsCoreLibrary()) {
        return true;
      }
    }
  }
  return false;
}


void Library::AddImport(const Namespace& ns) const {
  Array& imports = Array::Handle(this->imports());
  intptr_t capacity = imports.Length();
  if (num_imports() == capacity) {
    capacity = capacity + kImportsCapacityIncrement;
    imports = Array::Grow(imports, capacity);
    StorePointer(&raw_ptr()->imports_, imports.raw());
  }
  intptr_t index = num_imports();
  imports.SetAt(index, ns);
  set_num_imports(index + 1);
}


// Convenience function to determine whether the export list is
// non-empty.
bool Library::HasExports() const {
  return exports() != Object::empty_array().raw();
}


// We add one namespace at a time to the exports array and don't
// pre-allocate any unused capacity. The assumption is that
// re-exports are quite rare.
void Library::AddExport(const Namespace& ns) const {
  Array &exports = Array::Handle(this->exports());
  intptr_t num_exports = exports.Length();
  exports = Array::Grow(exports, num_exports + 1);
  StorePointer(&raw_ptr()->exports_, exports.raw());
  exports.SetAt(num_exports, ns);
}


void Library::InitClassDictionary() const {
  // The last element of the dictionary specifies the number of in use slots.
  // TODO(iposva): Find reasonable initial size.
  const int kInitialElementCount = 16;

  const Array& dictionary =
      Array::Handle(Array::New(kInitialElementCount + 1, Heap::kOld));
  dictionary.SetAt(kInitialElementCount, Smi::Handle(Smi::New(0)));
  StorePointer(&raw_ptr()->dictionary_, dictionary.raw());
}


void Library::InitImportList() const {
  const Array& imports =
      Array::Handle(Array::New(kInitialImportsCapacity, Heap::kOld));
  StorePointer(&raw_ptr()->imports_, imports.raw());
  raw_ptr()->num_imports_ = 0;
}


RawLibrary* Library::New() {
  ASSERT(Object::library_class() != Class::null());
  RawObject* raw = Object::Allocate(Library::kClassId,
                                    Library::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawLibrary*>(raw);
}


RawLibrary* Library::NewLibraryHelper(const String& url,
                                      bool import_core_lib) {
  const Library& result = Library::Handle(Library::New());
  result.StorePointer(&result.raw_ptr()->name_, Symbols::Empty().raw());
  result.StorePointer(&result.raw_ptr()->url_, url.raw());
  result.raw_ptr()->private_key_ = Scanner::AllocatePrivateKey(result);
  result.raw_ptr()->dictionary_ = Object::empty_array().raw();
  result.StorePointer(&result.raw_ptr()->metadata_,
                      GrowableObjectArray::New(4, Heap::kOld));
  result.raw_ptr()->anonymous_classes_ = Object::empty_array().raw();
  result.raw_ptr()->num_anonymous_ = 0;
  result.raw_ptr()->imports_ = Object::empty_array().raw();
  result.raw_ptr()->exports_ = Object::empty_array().raw();
  result.raw_ptr()->loaded_scripts_ = Array::null();
  result.set_native_entry_resolver(NULL);
  result.raw_ptr()->corelib_imported_ = true;
  result.set_debuggable(false);
  result.raw_ptr()->load_state_ = RawLibrary::kAllocated;
  result.raw_ptr()->index_ = -1;
  result.InitClassDictionary();
  result.InitImportList();
  if (import_core_lib) {
    const Library& core_lib = Library::Handle(Library::CoreLibrary());
    ASSERT(!core_lib.IsNull());
    const Namespace& ns = Namespace::Handle(
        Namespace::New(core_lib, Object::null_array(), Object::null_array()));
    result.AddImport(ns);
  }
  return result.raw();
}


RawLibrary* Library::New(const String& url) {
  return NewLibraryHelper(url, false);
}


void Library::InitCoreLibrary(Isolate* isolate) {
  const String& core_lib_url = Symbols::DartCore();
  const Library& core_lib =
      Library::Handle(Library::NewLibraryHelper(core_lib_url, false));
  core_lib.Register();
  isolate->object_store()->set_bootstrap_library(ObjectStore::kCore, core_lib);
  isolate->object_store()->set_root_library(Library::Handle());

  // Hook up predefined classes without setting their library pointers. These
  // classes are coming from the VM isolate, and are shared between multiple
  // isolates so setting their library pointers would be wrong.
  const Class& cls = Class::Handle(Object::dynamic_class());
  core_lib.AddObject(cls, String::Handle(cls.Name()));
}


RawObject* Library::Evaluate(const String& expr) const {
  // Make a fake top-level class and evaluate the expression
  // as a static function of the class.
  Script& script = Script::Handle();
  script = Script::New(Symbols::Empty(),
                       Symbols::Empty(),
                       RawScript::kSourceTag);
  Class& temp_class =
      Class::Handle(Class::New(Symbols::TopLevel(), script, 0));
  temp_class.set_library(*this);
  temp_class.set_is_finalized();
  return temp_class.Evaluate(expr);
}


void Library::InitNativeWrappersLibrary(Isolate* isolate) {
  static const int kNumNativeWrappersClasses = 4;
  ASSERT(kNumNativeWrappersClasses > 0 && kNumNativeWrappersClasses < 10);
  const String& native_flds_lib_url = Symbols::DartNativeWrappers();
  const Library& native_flds_lib = Library::Handle(
      Library::NewLibraryHelper(native_flds_lib_url, false));
  native_flds_lib.Register();
  isolate->object_store()->set_native_wrappers_library(native_flds_lib);
  static const char* const kNativeWrappersClass = "NativeFieldWrapperClass";
  static const int kNameLength = 25;
  ASSERT(kNameLength == (strlen(kNativeWrappersClass) + 1 + 1));
  char name_buffer[kNameLength];
  String& cls_name = String::Handle();
  for (int fld_cnt = 1; fld_cnt <= kNumNativeWrappersClasses; fld_cnt++) {
    OS::SNPrint(name_buffer,
                kNameLength,
                "%s%d",
                kNativeWrappersClass,
                fld_cnt);
    cls_name = Symbols::New(name_buffer);
    Class::NewNativeWrapper(native_flds_lib, cls_name, fld_cnt);
  }
}


RawLibrary* Library::LookupLibrary(const String &url) {
  Isolate* isolate = Isolate::Current();
  Library& lib = Library::Handle(isolate, Library::null());
  String& lib_url = String::Handle(isolate, String::null());
  GrowableObjectArray& libs = GrowableObjectArray::Handle(
      isolate, isolate->object_store()->libraries());
  for (int i = 0; i < libs.Length(); i++) {
    lib ^= libs.At(i);
    lib_url ^= lib.url();
    if (lib_url.Equals(url)) {
      return lib.raw();
    }
  }
  return Library::null();
}


RawError* Library::Patch(const Script& script) const {
  ASSERT(script.kind() == RawScript::kPatchTag);
  return Compiler::Compile(*this, script);
}


bool Library::IsKeyUsed(intptr_t key) {
  intptr_t lib_key;
  const GrowableObjectArray& libs = GrowableObjectArray::Handle(
      Isolate::Current()->object_store()->libraries());
  Library& lib = Library::Handle();
  String& lib_url = String::Handle();
  for (int i = 0; i < libs.Length(); i++) {
    lib ^= libs.At(i);
    lib_url ^= lib.url();
    lib_key = lib_url.Hash();
    if (lib_key == key) {
      return true;
    }
  }
  return false;
}


bool Library::IsPrivate(const String& name) {
  if (ShouldBePrivate(name)) return true;
  // Factory names: List._fromLiteral.
  for (intptr_t i = 1; i < name.Length() - 1; i++) {
    if (name.CharAt(i) == '.') {
      if (name.CharAt(i + 1) == '_') {
        return true;
      }
    }
  }
  return false;
}


const String& Library::PrivateCoreLibName(const String& member) {
  const Library& core_lib = Library::Handle(Library::CoreLibrary());
  const String& private_name = String::ZoneHandle(core_lib.PrivateName(member));
  return private_name;
}


RawClass* Library::LookupCoreClass(const String& class_name) {
  const Library& core_lib = Library::Handle(Library::CoreLibrary());
  String& name = String::Handle(class_name.raw());
  if (class_name.CharAt(0) == Scanner::kPrivateIdentifierStart) {
    // Private identifiers are mangled on a per library basis.
    name = String::Concat(name, String::Handle(core_lib.private_key()));
    name = Symbols::New(name);
  }
  return core_lib.LookupClass(name);
}


// Cannot handle qualified names properly as it only appends private key to
// the end (e.g. _Alfa.foo -> _Alfa.foo@...).
RawString* Library::PrivateName(const String& name) const {
  ASSERT(IsPrivate(name));
  // ASSERT(strchr(name, '@') == NULL);
  String& str = String::Handle();
  str = name.raw();
  str = String::Concat(str, String::Handle(this->private_key()));
  str = Symbols::New(str);
  return str.raw();
}


RawLibrary* Library::GetLibrary(intptr_t index) {
  Isolate* isolate = Isolate::Current();
  const GrowableObjectArray& libs =
      GrowableObjectArray::Handle(isolate->object_store()->libraries());
  ASSERT(!libs.IsNull());
  if ((0 <= index) && (index < libs.Length())) {
    Library& lib = Library::Handle();
    lib ^= libs.At(index);
    return lib.raw();
  }
  return Library::null();
}


void Library::Register() const {
  ASSERT(Library::LookupLibrary(String::Handle(url())) == Library::null());
  ObjectStore* object_store = Isolate::Current()->object_store();
  GrowableObjectArray& libs =
      GrowableObjectArray::Handle(object_store->libraries());
  ASSERT(!libs.IsNull());
  set_index(libs.Length());
  libs.Add(*this);
}


RawLibrary* Library::AsyncLibrary() {
  return Isolate::Current()->object_store()->async_library();
}


RawLibrary* Library::CoreLibrary() {
  return Isolate::Current()->object_store()->core_library();
}


RawLibrary* Library::CollectionLibrary() {
  return Isolate::Current()->object_store()->collection_library();
}


RawLibrary* Library::CollectionDevLibrary() {
  return Isolate::Current()->object_store()->collection_dev_library();
}


RawLibrary* Library::IsolateLibrary() {
  return Isolate::Current()->object_store()->isolate_library();
}


RawLibrary* Library::MathLibrary() {
  return Isolate::Current()->object_store()->math_library();
}


RawLibrary* Library::MirrorsLibrary() {
  return Isolate::Current()->object_store()->mirrors_library();
}


RawLibrary* Library::NativeWrappersLibrary() {
  return Isolate::Current()->object_store()->native_wrappers_library();
}


RawLibrary* Library::TypedDataLibrary() {
  return Isolate::Current()->object_store()->typed_data_library();
}


const char* Library::ToCString() const {
  const char* kFormat = "Library:'%s'";
  const String& name = String::Handle(url());
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, name.ToCString()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, name.ToCString());
  return chars;
}


void Library::PrintToJSONStream(JSONStream* stream, bool ref) const {
  const char* library_name = String::Handle(name()).ToCString();
  const char* library_url = String::Handle(url()).ToCString();
  ObjectIdRing* ring = Isolate::Current()->object_id_ring();
  intptr_t id = ring->GetIdForObject(raw());
  JSONObject jsobj(stream);
  jsobj.AddProperty("type", JSONType(ref));
  jsobj.AddProperty("id", id);
  jsobj.AddProperty("name", library_name);
  if (ref) return;
  jsobj.AddProperty("url", library_url);
  {
    JSONArray jsarr(&jsobj, "classes");
    ClassDictionaryIterator class_iter(*this);
    Class& klass = Class::Handle();
    while (class_iter.HasNext()) {
      klass = class_iter.GetNextClass();
      jsarr.AddValue(klass);
    }
  }
  {
    JSONArray jsarr(&jsobj, "libraries");
    Library& lib = Library::Handle();
    for (intptr_t i = 0; i < num_imports(); i++) {
      lib = ImportLibraryAt(i);
      jsarr.AddValue(lib);
    }
  }
}


RawLibrary* LibraryPrefix::GetLibrary(int index) const {
  if ((index >= 0) || (index < num_imports())) {
    const Array& imports = Array::Handle(this->imports());
    Namespace& import = Namespace::Handle();
    import ^= imports.At(index);
    return import.library();
  }
  return Library::null();
}


bool LibraryPrefix::ContainsLibrary(const Library& library) const {
  intptr_t num_current_imports = num_imports();
  if (num_current_imports > 0) {
    Library& lib = Library::Handle();
    const String& url = String::Handle(library.url());
    String& lib_url = String::Handle();
    for (intptr_t i = 0; i < num_current_imports; i++) {
      lib = GetLibrary(i);
      ASSERT(!lib.IsNull());
      lib_url = lib.url();
      if (url.Equals(lib_url)) {
        return true;
      }
    }
  }
  return false;
}


void LibraryPrefix::AddImport(const Namespace& import) const {
  intptr_t num_current_imports = num_imports();

  // The library needs to be added to the list.
  Array& imports = Array::Handle(this->imports());
  const intptr_t length = (imports.IsNull()) ? 0 : imports.Length();
  // Grow the list if it is full.
  if (num_current_imports >= length) {
    const intptr_t new_length = length + kIncrementSize;
    imports = Array::Grow(imports, new_length, Heap::kOld);
    set_imports(imports);
  }
  imports.SetAt(num_current_imports, import);
  set_num_imports(num_current_imports + 1);
}


RawObject* LibraryPrefix::LookupObject(const String& name) const {
  Array& imports = Array::Handle(this->imports());
  Object& obj = Object::Handle();
  Namespace& import = Namespace::Handle();
  Library& import_lib = Library::Handle();
  String& import_lib_url = String::Handle();
  String& first_import_lib_url = String::Handle();
  Object& found_obj = Object::Handle();
  for (intptr_t i = 0; i < num_imports(); i++) {
    import ^= imports.At(i);
    obj = import.Lookup(name);
    if (!obj.IsNull()) {
      import_lib = import.library();
      import_lib_url = import_lib.url();
      if (found_obj.raw() != obj.raw()) {
        if (first_import_lib_url.IsNull() ||
            first_import_lib_url.StartsWith(Symbols::DartScheme())) {
          // This is the first object we found, or the
          // previously found object is exported from a Dart
          // system library. The newly found object hides the one
          // from the Dart library.
          first_import_lib_url = import_lib.url();
          found_obj = obj.raw();
        } else if (import_lib_url.StartsWith(Symbols::DartScheme())) {
          // The newly found object is exported from a Dart system
          // library. It is hidden by the previously found object.
          // We continue to search.
        } else {
          // We found two different objects with the same name.
          return Object::null();
        }
      }
    }
  }
  return found_obj.raw();
}


RawClass* LibraryPrefix::LookupClass(const String& class_name) const {
  const Object& obj = Object::Handle(LookupObject(class_name));
  if (obj.IsClass()) {
    return Class::Cast(obj).raw();
  }
  return Class::null();
}


RawLibraryPrefix* LibraryPrefix::New() {
  ASSERT(Object::library_prefix_class() != Class::null());
  RawObject* raw = Object::Allocate(LibraryPrefix::kClassId,
                                    LibraryPrefix::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawLibraryPrefix*>(raw);
}


RawLibraryPrefix* LibraryPrefix::New(const String& name,
                                     const Namespace& import) {
  const LibraryPrefix& result = LibraryPrefix::Handle(LibraryPrefix::New());
  result.set_name(name);
  result.set_num_imports(0);
  result.set_imports(Array::Handle(Array::New(kInitialSize)));
  result.AddImport(import);
  return result.raw();
}


void LibraryPrefix::set_name(const String& value) const {
  ASSERT(value.IsSymbol());
  StorePointer(&raw_ptr()->name_, value.raw());
}


void LibraryPrefix::set_imports(const Array& value) const {
  StorePointer(&raw_ptr()->imports_, value.raw());
}


void LibraryPrefix::set_num_imports(intptr_t value) const {
  raw_ptr()->num_imports_ = value;
}


const char* LibraryPrefix::ToCString() const {
  const char* kFormat = "LibraryPrefix:'%s'";
  const String& prefix = String::Handle(name());
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, prefix.ToCString()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, prefix.ToCString());
  return chars;
}


void LibraryPrefix::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


const char* Namespace::ToCString() const {
  const char* kFormat = "Namespace for library '%s'";
  const Library& lib = Library::Handle(library());
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, lib.ToCString()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, lib.ToCString());
  return chars;
}


void Namespace::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


bool Namespace::HidesName(const String& name) const {
  // Quick check for common case with no combinators.
  if (hide_names() == show_names()) {
    ASSERT(hide_names() == Array::null());
    return false;
  }
  const String* plain_name = &name;
  if (Field::IsGetterName(name)) {
    plain_name = &String::Handle(Field::NameFromGetter(name));
  } else if (Field::IsSetterName(name)) {
    plain_name = &String::Handle(Field::NameFromSetter(name));
  }
  // Check whether the name is in the list of explicitly hidden names.
  if (hide_names() != Array::null()) {
    const Array& names = Array::Handle(hide_names());
    String& hidden = String::Handle();
    intptr_t num_names = names.Length();
    for (intptr_t i = 0; i < num_names; i++) {
      hidden ^= names.At(i);
      if (plain_name->Equals(hidden)) {
        return true;
      }
    }
  }
  // The name is not explicitly hidden. Now check whether it is in the
  // list of explicitly visible names, if there is one.
  if (show_names() != Array::null()) {
    const Array& names = Array::Handle(show_names());
    String& shown = String::Handle();
    intptr_t num_names = names.Length();
    for (intptr_t i = 0; i < num_names; i++) {
      shown ^= names.At(i);
      if (plain_name->Equals(shown)) {
        return false;
      }
    }
    // There is a list of visible names. The name we're looking for is not
    // contained in the list, so it is hidden.
    return true;
  }
  // The name is not filtered out.
  return false;
}


// Look up object with given name in library and filter out hidden
// names. Also look up getters and setters.
RawObject* Namespace::Lookup(const String& name) const {
  Isolate* isolate = Isolate::Current();
  const Library& lib = Library::Handle(isolate, library());
  intptr_t ignore = 0;

  // Lookup the name in the library's symbols.
  const String* filter_name = &name;
  Object& obj = Object::Handle(isolate, lib.LookupEntry(name, &ignore));
  if (Field::IsGetterName(name)) {
    filter_name = &String::Handle(Field::NameFromGetter(name));
  } else if (Field::IsSetterName(name)) {
    filter_name = &String::Handle(Field::NameFromGetter(name));
  } else {
    if (obj.IsNull() || obj.IsLibraryPrefix()) {
      obj = lib.LookupEntry(String::Handle(Field::GetterName(name)), &ignore);
      if (obj.IsNull()) {
        obj = lib.LookupEntry(String::Handle(Field::SetterName(name)), &ignore);
      }
    }
  }

  // Library prefixes are not exported.
  if (obj.IsNull() || obj.IsLibraryPrefix()) {
    // Lookup in the re-exported symbols.
    obj = lib.LookupReExport(name);
  }
  if (obj.IsNull() || HidesName(*filter_name) || obj.IsLibraryPrefix()) {
    return Object::null();
  }
  return obj.raw();
}


RawNamespace* Namespace::New() {
  ASSERT(Object::namespace_class() != Class::null());
  RawObject* raw = Object::Allocate(Namespace::kClassId,
                                    Namespace::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawNamespace*>(raw);
}


RawNamespace* Namespace::New(const Library& library,
                             const Array& show_names,
                             const Array& hide_names) {
  ASSERT(show_names.IsNull() || (show_names.Length() > 0));
  ASSERT(hide_names.IsNull() || (hide_names.Length() > 0));
  const Namespace& result = Namespace::Handle(Namespace::New());
  result.StorePointer(&result.raw_ptr()->library_, library.raw());
  result.StorePointer(&result.raw_ptr()->show_names_, show_names.raw());
  result.StorePointer(&result.raw_ptr()->hide_names_, hide_names.raw());
  return result.raw();
}


RawError* Library::CompileAll() {
  Error& error = Error::Handle();
  const GrowableObjectArray& libs = GrowableObjectArray::Handle(
      Isolate::Current()->object_store()->libraries());
  Library& lib = Library::Handle();
  Class& cls = Class::Handle();
  for (int i = 0; i < libs.Length(); i++) {
    lib ^= libs.At(i);
    ClassDictionaryIterator it(lib, ClassDictionaryIterator::kIteratePrivate);
    while (it.HasNext()) {
      cls = it.GetNextClass();
      error = cls.EnsureIsFinalized(Isolate::Current());
      if (!error.IsNull()) {
        return error.raw();
      }
      error = Compiler::CompileAllFunctions(cls);
      if (!error.IsNull()) {
        return error.raw();
      }
    }
  }
  return error.raw();
}


struct FpDiff {
  FpDiff(int32_t old_, int32_t new_): old_fp(old_), new_fp(new_) {}
  int32_t old_fp;
  int32_t new_fp;
};



// Return Function::null() if function does not exist in libs.
RawFunction* Library::GetFunction(const GrowableArray<Library*>& libs,
                                  const char* class_name,
                                  const char* function_name) {
  Function& func = Function::Handle();
  String& class_str = String::Handle();
  String& func_str = String::Handle();
  Class& cls = Class::Handle();
  for (intptr_t l = 0; l < libs.length(); l++) {
    const Library& lib = *libs[l];
    if (strcmp(class_name, "::") == 0) {
      func_str = Symbols::New(function_name);
      func = lib.LookupFunctionAllowPrivate(func_str);
    } else {
      class_str = String::New(class_name);
      cls = lib.LookupClassAllowPrivate(class_str);
      if (!cls.IsNull()) {
        func_str = String::New(function_name);
        if (function_name[0] == '.') {
          func_str = String::Concat(class_str, func_str);
        }
        func = cls.LookupFunctionAllowPrivate(func_str);
      }
    }
    if (!func.IsNull()) {
      return func.raw();
    }
  }
  return Function::null();
}


void Library::CheckFunctionFingerprints() {
  GrowableArray<FpDiff> collected_fp_diffs;
  GrowableArray<Library*> all_libs;
  Function& func = Function::Handle();
  bool has_errors = false;

#define CHECK_FINGERPRINTS(class_name, function_name, dest, fp)                \
  func = GetFunction(all_libs, #class_name, #function_name);                   \
  if (func.IsNull()) {                                                         \
    has_errors = true;                                                         \
    OS::Print("Function not found %s.%s\n", #class_name, #function_name);      \
  } else if (func.SourceFingerprint() != fp) {                                 \
    has_errors = true;                                                         \
    OS::Print("Wrong fingerprint for '%s': expecting %d found %d\n",           \
        func.ToFullyQualifiedCString(), fp, func.SourceFingerprint());         \
    collected_fp_diffs.Add(FpDiff(fp, func.SourceFingerprint()));              \
  }                                                                            \

  all_libs.Add(&Library::ZoneHandle(Library::CoreLibrary()));
  CORE_LIB_INTRINSIC_LIST(CHECK_FINGERPRINTS);
  CORE_INTEGER_LIB_INTRINSIC_LIST(CHECK_FINGERPRINTS);

  all_libs.Add(&Library::ZoneHandle(Library::MathLibrary()));
  all_libs.Add(&Library::ZoneHandle(Library::TypedDataLibrary()));
  RECOGNIZED_LIST(CHECK_FINGERPRINTS);
  INLINE_WHITE_LIST(CHECK_FINGERPRINTS);
  POLYMORPHIC_TARGET_LIST(CHECK_FINGERPRINTS);

  all_libs.Clear();
  all_libs.Add(&Library::ZoneHandle(Library::MathLibrary()));
  MATH_LIB_INTRINSIC_LIST(CHECK_FINGERPRINTS);

  all_libs.Clear();
  all_libs.Add(&Library::ZoneHandle(Library::TypedDataLibrary()));
  TYPED_DATA_LIB_INTRINSIC_LIST(CHECK_FINGERPRINTS);

#undef CHECK_FINGERPRINTS

Class& cls = Class::Handle();

#define CHECK_FACTORY_FINGERPRINTS(factory_symbol, cid, fp)                    \
  cls = Isolate::Current()->class_table()->At(cid);                            \
  func = cls.LookupFunctionAllowPrivate(Symbols::factory_symbol());            \
  if (func.IsNull()) {                                                         \
    has_errors = true;                                                         \
    OS::Print("Function not found %s.%s\n", cls.ToCString(),                   \
        Symbols::factory_symbol().ToCString());                                \
  } else if (func.SourceFingerprint() != fp) {                                 \
    has_errors = true;                                                         \
    OS::Print("Wrong fingerprint for '%s': expecting %d found %d\n",           \
        func.ToFullyQualifiedCString(), fp, func.SourceFingerprint());         \
    collected_fp_diffs.Add(FpDiff(fp, func.SourceFingerprint()));              \
  }                                                                            \

  RECOGNIZED_LIST_FACTORY_LIST(CHECK_FACTORY_FINGERPRINTS);

#undef CHECK_FACTORY_FINGERPRINTS

  if (has_errors) {
    for (intptr_t i = 0; i < collected_fp_diffs.length(); i++) {
      OS::Print("s/%d/%d/\n",
          collected_fp_diffs[i].old_fp, collected_fp_diffs[i].new_fp);
    }
    OS::Print("\n");
    FATAL("Fingerprint mismatch.");
  }
}


RawInstructions* Instructions::New(intptr_t size) {
  ASSERT(Object::instructions_class() != Class::null());
  if (size < 0 || size > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Instructions::New: invalid size %" Pd "\n", size);
  }
  Instructions& result = Instructions::Handle();
  {
    uword aligned_size = Instructions::InstanceSize(size);
    RawObject* raw = Object::Allocate(Instructions::kClassId,
                                      aligned_size,
                                      Heap::kCode);
    NoGCScope no_gc;
    result ^= raw;
    result.set_size(size);
  }
  return result.raw();
}


const char* Instructions::ToCString() const {
  return "Instructions";
}


void Instructions::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


intptr_t PcDescriptors::Length() const {
  return Smi::Value(raw_ptr()->length_);
}


void PcDescriptors::SetLength(intptr_t value) const {
  // This is only safe because we create a new Smi, which does not cause
  // heap allocation.
  raw_ptr()->length_ = Smi::New(value);
}


uword PcDescriptors::PC(intptr_t index) const {
  return static_cast<uword>(*(EntryAddr(index, kPcEntry)));
}


void PcDescriptors::SetPC(intptr_t index, uword value) const {
  *(EntryAddr(index, kPcEntry)) = static_cast<intptr_t>(value);
}


PcDescriptors::Kind PcDescriptors::DescriptorKind(intptr_t index) const {
  return static_cast<PcDescriptors::Kind>(*(EntryAddr(index, kKindEntry)));
}


void PcDescriptors::SetKind(intptr_t index, PcDescriptors::Kind value) const {
  *(EntryAddr(index, kKindEntry)) = value;
}


intptr_t PcDescriptors::DeoptId(intptr_t index) const {
  return *(EntryAddr(index, kDeoptIdEntry));
}


void PcDescriptors::SetDeoptId(intptr_t index, intptr_t value) const {
  *(EntryAddr(index, kDeoptIdEntry)) = value;
}


intptr_t PcDescriptors::TokenPos(intptr_t index) const {
  return *(EntryAddr(index, kTokenPosEntry));
}


void PcDescriptors::SetTokenPos(intptr_t index, intptr_t value) const {
  *(EntryAddr(index, kTokenPosEntry)) = value;
}


intptr_t PcDescriptors::TryIndex(intptr_t index) const {
  return *(EntryAddr(index, kTryIndexEntry));
}


void PcDescriptors::SetTryIndex(intptr_t index, intptr_t value) const {
  *(EntryAddr(index, kTryIndexEntry)) = value;
}


RawPcDescriptors* PcDescriptors::New(intptr_t num_descriptors) {
  ASSERT(Object::pc_descriptors_class() != Class::null());
  if (num_descriptors < 0 || num_descriptors > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in PcDescriptors::New: "
           "invalid num_descriptors %" Pd "\n", num_descriptors);
  }
  PcDescriptors& result = PcDescriptors::Handle();
  {
    uword size = PcDescriptors::InstanceSize(num_descriptors);
    RawObject* raw = Object::Allocate(PcDescriptors::kClassId,
                                      size,
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(num_descriptors);
  }
  return result.raw();
}


const char* PcDescriptors::KindAsStr(intptr_t index) const {
  switch (DescriptorKind(index)) {
    case PcDescriptors::kDeopt:           return "deopt        ";
    case PcDescriptors::kEntryPatch:      return "entry-patch  ";
    case PcDescriptors::kPatchCode:       return "patch        ";
    case PcDescriptors::kLazyDeoptJump:   return "lazy-deopt   ";
    case PcDescriptors::kIcCall:          return "ic-call      ";
    case PcDescriptors::kOptStaticCall:   return "opt-call     ";
    case PcDescriptors::kUnoptStaticCall: return "unopt-call   ";
    case PcDescriptors::kClosureCall:     return "closure-call ";
    case PcDescriptors::kReturn:          return "return       ";
    case PcDescriptors::kRuntimeCall:     return "runtime-call ";
    case PcDescriptors::kOsrEntry:        return "osr-entry    ";
    case PcDescriptors::kOther:           return "other        ";
  }
  UNREACHABLE();
  return "";
}


void PcDescriptors::PrintHeaderString() {
  // 4 bits per hex digit + 2 for "0x".
  const int addr_width = (kBitsPerWord / 4) + 2;
  // "*" in a printf format specifier tells it to read the field width from
  // the printf argument list.
  OS::Print("%-*s\tkind    \tdeopt-id\ttok-ix\ttry-ix\n",
            addr_width, "pc");
}


const char* PcDescriptors::ToCString() const {
  if (Length() == 0) {
    return "No pc descriptors\n";
  }
  // 4 bits per hex digit.
  const int addr_width = kBitsPerWord / 4;
  // "*" in a printf format specifier tells it to read the field width from
  // the printf argument list.
  const char* kFormat =
      "%#-*" Px "\t%s\t%" Pd "\t\t%" Pd "\t%" Pd "\n";
  // First compute the buffer size required.
  intptr_t len = 1;  // Trailing '\0'.
  for (intptr_t i = 0; i < Length(); i++) {
    len += OS::SNPrint(NULL, 0, kFormat, addr_width,
                       PC(i),
                       KindAsStr(i),
                       DeoptId(i),
                       TokenPos(i),
                       TryIndex(i));
  }
  // Allocate the buffer.
  char* buffer = Isolate::Current()->current_zone()->Alloc<char>(len);
  // Layout the fields in the buffer.
  intptr_t index = 0;
  for (intptr_t i = 0; i < Length(); i++) {
    index += OS::SNPrint((buffer + index), (len - index), kFormat, addr_width,
                         PC(i),
                         KindAsStr(i),
                         DeoptId(i),
                         TokenPos(i),
                         TryIndex(i));
  }
  return buffer;
}


void PcDescriptors::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


// Verify assumptions (in debug mode only).
// - No two deopt descriptors have the same deoptimization id.
// - No two ic-call descriptors have the same deoptimization id (type feedback).
// A function without unique ids is marked as non-optimizable (e.g., because of
// finally blocks).
void PcDescriptors::Verify(const Function& function) const {
#if defined(DEBUG)
  // TODO(srdjan): Implement a more efficient way to check, currently drop
  // the check for too large number of descriptors.
  if (Length() > 3000) {
    if (FLAG_trace_compiler) {
      OS::Print("Not checking pc decriptors, length %" Pd "\n", Length());
    }
    return;
  }
  // Only check ids for unoptimized code that is optimizable.
  if (!function.is_optimizable()) return;
  for (intptr_t i = 0; i < Length(); i++) {
    PcDescriptors::Kind kind = DescriptorKind(i);
    // 'deopt_id' is set for kDeopt and kIcCall and must be unique for one kind.
    intptr_t deopt_id = Isolate::kNoDeoptId;
    if ((DescriptorKind(i) != PcDescriptors::kDeopt) ||
        (DescriptorKind(i) != PcDescriptors::kIcCall)) {
      continue;
    }

    deopt_id = DeoptId(i);
    if (Isolate::IsDeoptAfter(deopt_id)) {
      // TODO(vegorov): some instructions contain multiple calls and have
      // multiple "after" targets recorded. Right now it is benign but might
      // lead to issues in the future. Fix that and enable verification.
      continue;
    }

    for (intptr_t k = i + 1; k < Length(); k++) {
      if (kind == DescriptorKind(k)) {
        if (deopt_id != Isolate::kNoDeoptId) {
          ASSERT(DeoptId(k) != deopt_id);
        }
      }
    }
  }
#endif  // DEBUG
}


uword PcDescriptors::GetPcForKind(Kind kind) const {
  for (intptr_t i = 0; i < Length(); i++) {
    if (DescriptorKind(i) == kind) {
      return PC(i);
    }
  }
  return 0;
}


void Stackmap::SetCode(const dart::Code& code) const {
  StorePointer(&raw_ptr()->code_, code.raw());
}


bool Stackmap::GetBit(intptr_t bit_index) const {
  ASSERT(InRange(bit_index));
  int byte_index = bit_index >> kBitsPerByteLog2;
  int bit_remainder = bit_index & (kBitsPerByte - 1);
  uint8_t byte_mask = 1U << bit_remainder;
  uint8_t byte = raw_ptr()->data_[byte_index];
  return (byte & byte_mask);
}


void Stackmap::SetBit(intptr_t bit_index, bool value) const {
  ASSERT(InRange(bit_index));
  int byte_index = bit_index >> kBitsPerByteLog2;
  int bit_remainder = bit_index & (kBitsPerByte - 1);
  uint8_t byte_mask = 1U << bit_remainder;
  uint8_t* byte_addr = &(raw_ptr()->data_[byte_index]);
  if (value) {
    *byte_addr |= byte_mask;
  } else {
    *byte_addr &= ~byte_mask;
  }
}


RawStackmap* Stackmap::New(intptr_t pc_offset,
                           BitmapBuilder* bmap,
                           intptr_t register_bit_count) {
  ASSERT(Object::stackmap_class() != Class::null());
  ASSERT(bmap != NULL);
  Stackmap& result = Stackmap::Handle();
  // Guard against integer overflow of the instance size computation.
  intptr_t length = bmap->Length();
  intptr_t payload_size =
      Utils::RoundUp(length, kBitsPerByte) / kBitsPerByte;
  if ((payload_size < 0) ||
      (payload_size > kMaxLengthInBytes)) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Stackmap::New: invalid length %" Pd "\n",
           length);
  }
  {
    // Stackmap data objects are associated with a code object, allocate them
    // in old generation.
    RawObject* raw = Object::Allocate(Stackmap::kClassId,
                                      Stackmap::InstanceSize(length),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(length);
  }
  // When constructing a stackmap we store the pc offset in the stackmap's
  // PC. StackmapTableBuilder::FinalizeStackmaps will replace it with the pc
  // address.
  ASSERT(pc_offset >= 0);
  result.SetPC(pc_offset);
  for (intptr_t i = 0; i < length; ++i) {
    result.SetBit(i, bmap->Get(i));
  }
  result.SetRegisterBitCount(register_bit_count);
  return result.raw();
}


const char* Stackmap::ToCString() const {
  if (IsNull()) {
    return "{null}";
  } else {
    const char* kFormat = "%#" Px ": ";
    intptr_t fixed_length = OS::SNPrint(NULL, 0, kFormat, PC()) + 1;
    Isolate* isolate = Isolate::Current();
    // Guard against integer overflow in the computation of alloc_size.
    //
    // TODO(kmillikin): We could just truncate the string if someone
    // tries to print a 2 billion plus entry stackmap.
    if (Length() > (kIntptrMax - fixed_length)) {
      FATAL1("Length() is unexpectedly large (%" Pd ")", Length());
    }
    intptr_t alloc_size = fixed_length + Length();
    char* chars = isolate->current_zone()->Alloc<char>(alloc_size);
    intptr_t index = OS::SNPrint(chars, alloc_size, kFormat, PC());
    for (intptr_t i = 0; i < Length(); i++) {
      chars[index++] = IsObject(i) ? '1' : '0';
    }
    chars[index] = '\0';
    return chars;
  }
}


void Stackmap::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawString* LocalVarDescriptors::GetName(intptr_t var_index) const {
  ASSERT(var_index < Length());
  const Array& names = Array::Handle(raw_ptr()->names_);
  ASSERT(Length() == names.Length());
  String& name = String::Handle();
  name ^= names.At(var_index);
  return name.raw();
}


void LocalVarDescriptors::SetVar(intptr_t var_index,
                                 const String& name,
                                 RawLocalVarDescriptors::VarInfo* info) const {
  ASSERT(var_index < Length());
  const Array& names = Array::Handle(raw_ptr()->names_);
  ASSERT(Length() == names.Length());
  names.SetAt(var_index, name);
  raw_ptr()->data_[var_index] = *info;
}


void LocalVarDescriptors::GetInfo(intptr_t var_index,
                                  RawLocalVarDescriptors::VarInfo* info) const {
  ASSERT(var_index < Length());
  *info = raw_ptr()->data_[var_index];
}


const char* LocalVarDescriptors::ToCString() const {
  intptr_t len = 1;  // Trailing '\0'.
  const char* kFormat =
      "%2" Pd " kind=%d scope=0x%04x begin=%" Pd " end=%" Pd " name=%s\n";
  for (intptr_t i = 0; i < Length(); i++) {
    String& var_name = String::Handle(GetName(i));
    RawLocalVarDescriptors::VarInfo info;
    GetInfo(i, &info);
    len += OS::SNPrint(NULL, 0, kFormat,
                       i,
                       info.kind,
                       info.scope_id,
                       info.begin_pos,
                       info.end_pos,
                       var_name.ToCString());
  }
  char* buffer = Isolate::Current()->current_zone()->Alloc<char>(len);
  intptr_t num_chars = 0;
  for (intptr_t i = 0; i < Length(); i++) {
    String& var_name = String::Handle(GetName(i));
    RawLocalVarDescriptors::VarInfo info;
    GetInfo(i, &info);
    num_chars += OS::SNPrint((buffer + num_chars),
                             (len - num_chars),
                             kFormat,
                             i,
                             info.kind,
                             info.scope_id,
                             info.begin_pos,
                             info.end_pos,
                             var_name.ToCString());
  }
  return buffer;
}


void LocalVarDescriptors::PrintToJSONStream(JSONStream* stream,
                                            bool ref) const {
  JSONObject jsobj(stream);
}


RawLocalVarDescriptors* LocalVarDescriptors::New(intptr_t num_variables) {
  ASSERT(Object::var_descriptors_class() != Class::null());
  if (num_variables < 0 || num_variables > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in LocalVarDescriptors::New: "
           "invalid num_variables %" Pd "\n", num_variables);
  }
  LocalVarDescriptors& result = LocalVarDescriptors::Handle();
  {
    uword size = LocalVarDescriptors::InstanceSize(num_variables);
    RawObject* raw = Object::Allocate(LocalVarDescriptors::kClassId,
                                      size,
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.raw_ptr()->length_ = num_variables;
  }
  const Array& names = Array::Handle(Array::New(num_variables, Heap::kOld));
  result.raw_ptr()->names_ = names.raw();
  return result.raw();
}


intptr_t LocalVarDescriptors::Length() const {
  return raw_ptr()->length_;
}


intptr_t ExceptionHandlers::Length() const {
  return raw_ptr()->length_;
}


void ExceptionHandlers::SetHandlerInfo(intptr_t try_index,
                                       intptr_t outer_try_index,
                                       intptr_t handler_pc,
                                       bool needs_stacktrace,
                                       bool has_catch_all) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  RawExceptionHandlers::HandlerInfo* info = &raw_ptr()->data_[try_index];
  info->outer_try_index = outer_try_index;
  info->handler_pc = handler_pc;
  info->needs_stacktrace = needs_stacktrace;
  info->has_catch_all = has_catch_all;
}

void ExceptionHandlers::GetHandlerInfo(
    intptr_t try_index,
    RawExceptionHandlers::HandlerInfo* info) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  ASSERT(info != NULL);
  *info = raw_ptr()->data_[try_index];
}


intptr_t ExceptionHandlers::HandlerPC(intptr_t try_index) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  return raw_ptr()->data_[try_index].handler_pc;
}


intptr_t ExceptionHandlers::OuterTryIndex(intptr_t try_index) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  return raw_ptr()->data_[try_index].outer_try_index;
}


bool ExceptionHandlers::NeedsStacktrace(intptr_t try_index) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  return raw_ptr()->data_[try_index].needs_stacktrace;
}


bool ExceptionHandlers::HasCatchAll(intptr_t try_index) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  return raw_ptr()->data_[try_index].has_catch_all;
}


void ExceptionHandlers::SetHandledTypes(intptr_t try_index,
                                        const Array& handled_types) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  const Array& handled_types_data =
      Array::Handle(raw_ptr()->handled_types_data_);
  handled_types_data.SetAt(try_index, handled_types);
}


RawArray* ExceptionHandlers::GetHandledTypes(intptr_t try_index) const {
  ASSERT((try_index >= 0) && (try_index < Length()));
  Array& array = Array::Handle(raw_ptr()->handled_types_data_);
  array ^= array.At(try_index);
  return array.raw();
}


void ExceptionHandlers::set_handled_types_data(const Array& value) const {
  StorePointer(&raw_ptr()->handled_types_data_, value.raw());
}


RawExceptionHandlers* ExceptionHandlers::New(intptr_t num_handlers) {
  ASSERT(Object::exception_handlers_class() != Class::null());
  if ((num_handlers < 0) || (num_handlers >= kMaxHandlers)) {
    FATAL1("Fatal error in ExceptionHandlers::New(): "
           "invalid num_handlers %" Pd "\n",
           num_handlers);
  }
  ExceptionHandlers& result = ExceptionHandlers::Handle();
  {
    uword size = ExceptionHandlers::InstanceSize(num_handlers);
    RawObject* raw = Object::Allocate(ExceptionHandlers::kClassId,
                                      size,
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.raw_ptr()->length_ = num_handlers;
  }
  const Array& handled_types_data = Array::Handle(Array::New(num_handlers));
  result.set_handled_types_data(handled_types_data);
  return result.raw();
}


const char* ExceptionHandlers::ToCString() const {
  if (Length() == 0) {
    return "No exception handlers\n";
  }
  Array& handled_types = Array::Handle();
  Type& type = Type::Handle();
  RawExceptionHandlers::HandlerInfo info;
  // First compute the buffer size required.
  const char* kFormat = "%" Pd " => %#" Px "  (%" Pd
                        " types) (outer %" Pd ")\n";
  const char* kFormat2 = "  %d. %s\n";
  intptr_t len = 1;  // Trailing '\0'.
  for (intptr_t i = 0; i < Length(); i++) {
    GetHandlerInfo(i, &info);
    handled_types = GetHandledTypes(i);
    ASSERT(!handled_types.IsNull());
    const intptr_t num_types = handled_types.Length();
    len += OS::SNPrint(NULL, 0, kFormat,
                       i,
                       info.handler_pc,
                       num_types,
                       info.outer_try_index);
    for (int k = 0; k < num_types; k++) {
      type ^= handled_types.At(k);
      ASSERT(!type.IsNull());
      len += OS::SNPrint(NULL, 0, kFormat2, k, type.ToCString());
    }
  }
  // Allocate the buffer.
  char* buffer = Isolate::Current()->current_zone()->Alloc<char>(len);
  // Layout the fields in the buffer.
  intptr_t num_chars = 0;
  for (intptr_t i = 0; i < Length(); i++) {
    GetHandlerInfo(i, &info);
    handled_types = GetHandledTypes(i);
    const intptr_t num_types = handled_types.Length();
    num_chars += OS::SNPrint((buffer + num_chars),
                             (len - num_chars),
                             kFormat,
                             i,
                             info.handler_pc,
                             num_types,
                             info.outer_try_index);
    for (int k = 0; k < num_types; k++) {
      type ^= handled_types.At(k);
      num_chars += OS::SNPrint((buffer + num_chars),
                               (len - num_chars),
                               kFormat2, k, type.ToCString());
    }
  }
  return buffer;
}


void ExceptionHandlers::PrintToJSONStream(JSONStream* stream,
                                          bool ref) const {
  JSONObject jsobj(stream);
}


intptr_t DeoptInfo::Length() const {
  return Smi::Value(raw_ptr()->length_);
}


intptr_t DeoptInfo::FromIndex(intptr_t index) const {
  return *(EntryAddr(index, kFromIndex));
}


intptr_t DeoptInfo::Instruction(intptr_t index) const {
  return *(EntryAddr(index, kInstruction));
}


intptr_t DeoptInfo::FrameSize() const {
  intptr_t pos = 0;
  while (Instruction(pos) == DeoptInstr::kMaterializeObject) {
    pos++;
  }
  return TranslationLength() - pos;
}


intptr_t DeoptInfo::TranslationLength() const {
  intptr_t length = Length();
  if (Instruction(length - 1) != DeoptInstr::kSuffix) return length;

  // If the last command is a suffix, add in the length of the suffix and
  // do not count the suffix command as a translation command.
  intptr_t ignored = 0;
  intptr_t suffix_length =
      DeoptInstr::DecodeSuffix(FromIndex(length - 1), &ignored);
  return length + suffix_length - 1;
}


void DeoptInfo::ToInstructions(const Array& table,
                               GrowableArray<DeoptInstr*>* instructions) const {
  ASSERT(instructions->is_empty());
  Smi& offset = Smi::Handle();
  DeoptInfo& info = DeoptInfo::Handle(raw());
  Smi& reason = Smi::Handle();
  intptr_t index = 0;
  intptr_t length = TranslationLength();
  while (index < length) {
    intptr_t instruction = info.Instruction(index);
    intptr_t from_index = info.FromIndex(index);
    if (instruction == DeoptInstr::kSuffix) {
      // Suffix instructions cause us to 'jump' to another translation,
      // changing info, length and index.
      intptr_t info_number = 0;
      intptr_t suffix_length =
          DeoptInstr::DecodeSuffix(from_index, &info_number);
      DeoptTable::GetEntry(table, info_number, &offset, &info, &reason);
      length = info.TranslationLength();
      index = length - suffix_length;
    } else {
      instructions->Add(DeoptInstr::Create(instruction, from_index));
      ++index;
    }
  }
}


const char* DeoptInfo::ToCString() const {
  if (Length() == 0) {
    return "No DeoptInfo";
  }
  // Convert to DeoptInstr.
  GrowableArray<DeoptInstr*> deopt_instrs(Length());
  for (intptr_t i = 0; i < Length(); i++) {
    deopt_instrs.Add(DeoptInstr::Create(Instruction(i), FromIndex(i)));
  }
  // Compute the buffer size required.
  intptr_t len = 1;  // Trailing '\0'.
  for (intptr_t i = 0; i < Length(); i++) {
    len += OS::SNPrint(NULL, 0, "[%s]", deopt_instrs[i]->ToCString());
  }
  // Allocate the buffer.
  char* buffer = Isolate::Current()->current_zone()->Alloc<char>(len);
  // Layout the fields in the buffer.
  intptr_t index = 0;
  for (intptr_t i = 0; i < Length(); i++) {
    index += OS::SNPrint((buffer + index),
                         (len - index),
                         "[%s]",
                         deopt_instrs[i]->ToCString());
  }
  return buffer;
}


// Returns a bool so it can be asserted.
bool DeoptInfo::VerifyDecompression(const GrowableArray<DeoptInstr*>& original,
                                    const Array& deopt_table) const {
  intptr_t length = TranslationLength();
  GrowableArray<DeoptInstr*> unpacked(length);
  ToInstructions(deopt_table, &unpacked);
  ASSERT(unpacked.length() == original.length());
  for (intptr_t i = 0; i < unpacked.length(); ++i) {
    ASSERT(unpacked[i]->Equals(*original[i]));
  }
  return true;
}


void DeoptInfo::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawDeoptInfo* DeoptInfo::New(intptr_t num_commands) {
  ASSERT(Object::deopt_info_class() != Class::null());
  if ((num_commands < 0) || (num_commands > kMaxElements)) {
    FATAL1("Fatal error in DeoptInfo::New(): invalid num_commands %" Pd "\n",
           num_commands);
  }
  DeoptInfo& result = DeoptInfo::Handle();
  {
    uword size = DeoptInfo::InstanceSize(num_commands);
    RawObject* raw = Object::Allocate(DeoptInfo::kClassId,
                                      size,
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(num_commands);
  }
  return result.raw();
}


void DeoptInfo::SetLength(intptr_t value) const {
  // This is only safe because we create a new Smi, which does not cause
  // heap allocation.
  raw_ptr()->length_ = Smi::New(value);
}


void DeoptInfo::SetAt(intptr_t index,
                      intptr_t instr_kind,
                      intptr_t from_index) const {
  *(EntryAddr(index, kInstruction)) = instr_kind;
  *(EntryAddr(index, kFromIndex)) = from_index;
}


Code::Comments& Code::Comments::New(intptr_t count) {
  Comments* comments;
  if (count < 0 || count > (kIntptrMax / kNumberOfEntries)) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Code::Comments::New: invalid count %" Pd "\n",
           count);
  }
  if (count == 0) {
    comments = new Comments(Object::empty_array());
  } else {
    const Array& data =
        Array::Handle(Array::New(count * kNumberOfEntries, Heap::kOld));
    comments = new Comments(data);
  }
  return *comments;
}


intptr_t Code::Comments::Length() const {
  if (comments_.IsNull()) {
    return 0;
  }
  return comments_.Length() / kNumberOfEntries;
}


intptr_t Code::Comments::PCOffsetAt(intptr_t idx) const {
  return Smi::Value(Smi::RawCast(
      comments_.At(idx * kNumberOfEntries + kPCOffsetEntry)));
}


void Code::Comments::SetPCOffsetAt(intptr_t idx, intptr_t pc)  {
  comments_.SetAt(idx * kNumberOfEntries + kPCOffsetEntry,
                  Smi::Handle(Smi::New(pc)));
}


RawString* Code::Comments::CommentAt(intptr_t idx) const {
  return String::RawCast(comments_.At(idx * kNumberOfEntries + kCommentEntry));
}


void Code::Comments::SetCommentAt(intptr_t idx, const String& comment) {
  comments_.SetAt(idx * kNumberOfEntries + kCommentEntry, comment);
}


Code::Comments::Comments(const Array& comments)
    : comments_(comments) {
}


void Code::set_state_bits(intptr_t bits) const {
  raw_ptr()->state_bits_ = bits;
}


void Code::set_is_optimized(bool value) const {
  set_state_bits(OptimizedBit::update(value, raw_ptr()->state_bits_));
}


void Code::set_is_alive(bool value) const {
  set_state_bits(AliveBit::update(value, raw_ptr()->state_bits_));
}


void Code::set_stackmaps(const Array& maps) const {
  ASSERT(maps.IsOld());
  StorePointer(&raw_ptr()->stackmaps_, maps.raw());
}


void Code::set_deopt_info_array(const Array& array) const {
  ASSERT(array.IsOld());
  StorePointer(&raw_ptr()->deopt_info_array_, array.raw());
}


void Code::set_object_table(const Array& array) const {
  ASSERT(array.IsOld());
  StorePointer(&raw_ptr()->object_table_, array.raw());
}


void Code::set_static_calls_target_table(const Array& value) const {
  StorePointer(&raw_ptr()->static_calls_target_table_, value.raw());
#if defined(DEBUG)
  // Check that the table is sorted by pc offsets.
  // FlowGraphCompiler::AddStaticCallTarget adds pc-offsets to the table while
  // emitting assembly. This guarantees that every succeeding pc-offset is
  // larger than the previously added one.
  for (intptr_t i = kSCallTableEntryLength;
      i < value.Length();
      i += kSCallTableEntryLength) {
    ASSERT(value.At(i - kSCallTableEntryLength) < value.At(i));
  }
#endif  // DEBUG
}


RawDeoptInfo* Code::GetDeoptInfoAtPc(uword pc, intptr_t* deopt_reason) const {
  ASSERT(is_optimized());
  const Instructions& instrs = Instructions::Handle(instructions());
  uword code_entry = instrs.EntryPoint();
  const Array& table = Array::Handle(deopt_info_array());
  ASSERT(!table.IsNull());
  // Linear search for the PC offset matching the target PC.
  intptr_t length = DeoptTable::GetLength(table);
  Smi& offset = Smi::Handle();
  Smi& reason = Smi::Handle();
  DeoptInfo& info = DeoptInfo::Handle();
  for (intptr_t i = 0; i < length; ++i) {
    DeoptTable::GetEntry(table, i, &offset, &info, &reason);
    if (pc == (code_entry + offset.Value())) {
      ASSERT(!info.IsNull());
      *deopt_reason = reason.Value();
      return info.raw();
    }
  }
  *deopt_reason = kDeoptUnknown;
  return DeoptInfo::null();
}


intptr_t Code::BinarySearchInSCallTable(uword pc) const {
  NoGCScope no_gc;
  const Array& table = Array::Handle(raw_ptr()->static_calls_target_table_);
  RawObject* key = reinterpret_cast<RawObject*>(Smi::New(pc - EntryPoint()));
  intptr_t imin = 0;
  intptr_t imax = table.Length() / kSCallTableEntryLength;
  while (imax >= imin) {
    const intptr_t imid = ((imax - imin) / 2) + imin;
    const intptr_t real_index = imid * kSCallTableEntryLength;
    RawObject* key_in_table = table.At(real_index);
    if (key_in_table < key) {
      imin = imid + 1;
    } else if (key_in_table > key) {
      imax = imid - 1;
    } else {
      return real_index;
    }
  }
  return -1;
}


RawFunction* Code::GetStaticCallTargetFunctionAt(uword pc) const {
  const intptr_t i = BinarySearchInSCallTable(pc);
  if (i < 0) {
    return Function::null();
  }
  const Array& array =
      Array::Handle(raw_ptr()->static_calls_target_table_);
  Function& function = Function::Handle();
  function ^= array.At(i + kSCallTableFunctionEntry);
  return function.raw();
}


RawCode* Code::GetStaticCallTargetCodeAt(uword pc) const {
  const intptr_t i = BinarySearchInSCallTable(pc);
  if (i < 0) {
    return Code::null();
  }
  const Array& array =
      Array::Handle(raw_ptr()->static_calls_target_table_);
  Code& code = Code::Handle();
  code ^= array.At(i + kSCallTableCodeEntry);
  return code.raw();
}


void Code::SetStaticCallTargetCodeAt(uword pc, const Code& code) const {
  const intptr_t i = BinarySearchInSCallTable(pc);
  ASSERT(i >= 0);
  const Array& array =
      Array::Handle(raw_ptr()->static_calls_target_table_);
  ASSERT(code.IsNull() ||
         (code.function() == array.At(i + kSCallTableFunctionEntry)));
  array.SetAt(i + kSCallTableCodeEntry, code);
}


void Code::Disassemble() const {
  const Instructions& instr = Instructions::Handle(instructions());
  uword start = instr.EntryPoint();
  Disassembler::Disassemble(start, start + instr.size(), comments());
}


const Code::Comments& Code::comments() const  {
  Comments* comments = new Code::Comments(Array::Handle(raw_ptr()->comments_));
  return *comments;
}


void Code::set_comments(const Code::Comments& comments) const {
  ASSERT(comments.comments_.IsOld());
  StorePointer(&raw_ptr()->comments_, comments.comments_.raw());
}


RawCode* Code::New(intptr_t pointer_offsets_length) {
  if (pointer_offsets_length < 0 || pointer_offsets_length > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Code::New: invalid pointer_offsets_length %" Pd "\n",
           pointer_offsets_length);
  }
  ASSERT(Object::code_class() != Class::null());
  Code& result = Code::Handle();
  {
    uword size = Code::InstanceSize(pointer_offsets_length);
    RawObject* raw = Object::Allocate(Code::kClassId, size, Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.set_pointer_offsets_length(pointer_offsets_length);
    result.set_is_optimized(false);
    result.set_is_alive(true);
    result.set_comments(Comments::New(0));
  }
  return result.raw();
}


RawCode* Code::FinalizeCode(const char* name,
                            Assembler* assembler,
                            bool optimized) {
  ASSERT(assembler != NULL);

  // Allocate the Instructions object.
  Instructions& instrs =
      Instructions::ZoneHandle(Instructions::New(assembler->CodeSize()));

  // Copy the instructions into the instruction area and apply all fixups.
  // Embedded pointers are still in handles at this point.
  MemoryRegion region(reinterpret_cast<void*>(instrs.EntryPoint()),
                      instrs.size());
  assembler->FinalizeInstructions(region);
  CPU::FlushICache(instrs.EntryPoint(), instrs.size());

  CodeObservers::NotifyAll(name,
                           instrs.EntryPoint(),
                           assembler->prologue_offset(),
                           instrs.size(),
                           optimized);

  const ZoneGrowableArray<intptr_t>& pointer_offsets =
      assembler->GetPointerOffsets();

  // Allocate the code object.
  Code& code = Code::ZoneHandle(Code::New(pointer_offsets.length()));
  {
    NoGCScope no_gc;

    // Set pointer offsets list in Code object and resolve all handles in
    // the instruction stream to raw objects.
    ASSERT(code.pointer_offsets_length() == pointer_offsets.length());
    for (intptr_t i = 0; i < pointer_offsets.length(); i++) {
      intptr_t offset_in_instrs = pointer_offsets[i];
      code.SetPointerOffsetAt(i, offset_in_instrs);
      const Object* object = region.Load<const Object*>(offset_in_instrs);
      region.Store<RawObject*>(offset_in_instrs, object->raw());
    }

    // Hook up Code and Instructions objects.
    instrs.set_code(code.raw());
    code.set_instructions(instrs.raw());

    // Set object pool in Instructions object.
    const GrowableObjectArray& object_pool = assembler->object_pool();
    if (object_pool.IsNull()) {
      instrs.set_object_pool(Object::empty_array().raw());
    } else {
      // TODO(regis): Once MakeArray takes a Heap::Space argument, call it here
      // with Heap::kOld and change the ARM and MIPS assemblers to work with a
      // GrowableObjectArray in new space.
      instrs.set_object_pool(Array::MakeArray(object_pool));
    }
  }
  return code.raw();
}


RawCode* Code::FinalizeCode(const Function& function,
                            Assembler* assembler,
                            bool optimized) {
  // Calling ToFullyQualifiedCString is very expensive, try to avoid it.
  if (CodeObservers::AreActive()) {
    return FinalizeCode(function.ToFullyQualifiedCString(),
                        assembler,
                        optimized);
  } else {
    return FinalizeCode("", assembler);
  }
}


// Check if object matches find condition.
bool Code::FindRawCodeVisitor::FindObject(RawObject* obj) {
  return RawInstructions::ContainsPC(obj, pc_);
}


RawCode* Code::LookupCode(uword pc) {
  Isolate* isolate = Isolate::Current();
  NoGCScope no_gc;
  FindRawCodeVisitor visitor(pc);
  RawInstructions* instr;
  instr = isolate->heap()->FindObjectInCodeSpace(&visitor);
  if (instr != Instructions::null()) {
    return instr->ptr()->code_;
  }
  return Code::null();
}


intptr_t Code::GetTokenIndexOfPC(uword pc) const {
  intptr_t token_pos = -1;
  const PcDescriptors& descriptors = PcDescriptors::Handle(pc_descriptors());
  for (intptr_t i = 0; i < descriptors.Length(); i++) {
    if (descriptors.PC(i) == pc) {
      token_pos = descriptors.TokenPos(i);
      break;
    }
  }
  return token_pos;
}


uword Code::GetPcForDeoptId(intptr_t deopt_id, PcDescriptors::Kind kind) const {
  const PcDescriptors& descriptors = PcDescriptors::Handle(pc_descriptors());
  for (intptr_t i = 0; i < descriptors.Length(); i++) {
    if ((descriptors.DeoptId(i) == deopt_id) &&
        (descriptors.DescriptorKind(i) == kind)) {
      uword pc = descriptors.PC(i);
      ASSERT(ContainsInstructionAt(pc));
      return pc;
    }
  }
  return 0;
}


intptr_t Code::GetDeoptIdForOsr(uword pc) const {
  const PcDescriptors& descriptors = PcDescriptors::Handle(pc_descriptors());
  for (intptr_t i = 0; i < descriptors.Length(); ++i) {
    if ((descriptors.PC(i) == pc) &&
        (descriptors.DescriptorKind(i) == PcDescriptors::kOsrEntry)) {
      return descriptors.DeoptId(i);
    }
  }
  return Isolate::kNoDeoptId;
}


const char* Code::ToCString() const {
  const char* kFormat = "Code entry:%p";
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, EntryPoint()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, EntryPoint());
  return chars;
}


void Code::PrintToJSONStream(JSONStream* stream, bool ref) const {
  ObjectIdRing* ring = Isolate::Current()->object_id_ring();
  intptr_t id = ring->GetIdForObject(raw());
  JSONObject jsobj(stream);
  if (ref) {
    jsobj.AddProperty("type", "@Code");
    jsobj.AddProperty("id", id);
    return;
  }
  jsobj.AddProperty("type", "Code");
  jsobj.AddProperty("id", id);
  jsobj.AddProperty("is_optimized", is_optimized());
  jsobj.AddProperty("is_alive", is_alive());
  jsobj.AddProperty("function", Object::Handle(function()));
  JSONArray jsarr(&jsobj, "disassembly");
  DisassembleToJSONStream formatter(jsarr);
  const Instructions& instr = Instructions::Handle(instructions());
  uword start = instr.EntryPoint();
  Disassembler::Disassemble(start, start + instr.size(), &formatter,
                            comments());
}


uword Code::GetPatchCodePc() const {
  const PcDescriptors& descriptors = PcDescriptors::Handle(pc_descriptors());
  return descriptors.GetPcForKind(PcDescriptors::kPatchCode);
}


uword Code::GetLazyDeoptPc() const {
  const PcDescriptors& descriptors = PcDescriptors::Handle(pc_descriptors());
  return descriptors.GetPcForKind(PcDescriptors::kLazyDeoptJump);
}


bool Code::ObjectExistsInArea(intptr_t start_offset,
                              intptr_t end_offset) const {
  for (intptr_t i = 0; i < this->pointer_offsets_length(); i++) {
    const intptr_t offset = this->GetPointerOffsetAt(i);
    if ((start_offset <= offset) && (offset < end_offset)) {
      return false;
    }
  }
  return true;
}


intptr_t Code::ExtractIcDataArraysAtCalls(
    GrowableArray<intptr_t>* node_ids,
    const GrowableObjectArray& ic_data_objs) const {
  ASSERT(node_ids != NULL);
  ASSERT(!ic_data_objs.IsNull());
  const PcDescriptors& descriptors =
      PcDescriptors::Handle(this->pc_descriptors());
  ICData& ic_data_obj = ICData::Handle();
  intptr_t max_id = -1;
  for (intptr_t i = 0; i < descriptors.Length(); i++) {
    PcDescriptors::Kind kind = descriptors.DescriptorKind(i);
    if ((kind == PcDescriptors::kIcCall) ||
        (kind == PcDescriptors::kUnoptStaticCall)) {
      intptr_t deopt_id = descriptors.DeoptId(i);
      if (deopt_id > max_id) {
        max_id = deopt_id;
      }
      node_ids->Add(deopt_id);
      uword ret_addr = descriptors.PC(i);
      if (kind == PcDescriptors::kIcCall) {
        CodePatcher::GetInstanceCallAt(ret_addr, *this, &ic_data_obj);
      } else {
        CodePatcher::GetUnoptimizedStaticCallAt(ret_addr, *this, &ic_data_obj);
      }
      ic_data_objs.Add(ic_data_obj);
    }
  }
  return max_id;
}


RawArray* Code::ExtractTypeFeedbackArray() const {
  ASSERT(!IsNull() && !is_optimized());
  GrowableArray<intptr_t> deopt_ids;
  const GrowableObjectArray& ic_data_objs =
      GrowableObjectArray::Handle(GrowableObjectArray::New());
  const intptr_t max_id =
      ExtractIcDataArraysAtCalls(&deopt_ids, ic_data_objs);
  const Array& result = Array::Handle(Array::New(max_id + 1));
  for (intptr_t i = 0; i < deopt_ids.length(); i++) {
    intptr_t result_index = deopt_ids[i];
    ASSERT(result.At(result_index) == Object::null());
    result.SetAt(result_index, Object::Handle(ic_data_objs.At(i)));
  }
  return result.raw();
}


RawStackmap* Code::GetStackmap(uword pc, Array* maps, Stackmap* map) const {
  // This code is used during iterating frames during a GC and hence it
  // should not in turn start a GC.
  NoGCScope no_gc;
  if (stackmaps() == Array::null()) {
    // No stack maps are present in the code object which means this
    // frame relies on tagged pointers.
    return Stackmap::null();
  }
  // A stack map is present in the code object, use the stack map to visit
  // frame slots which are marked as having objects.
  *maps = stackmaps();
  *map = Stackmap::null();
  for (intptr_t i = 0; i < maps->Length(); i++) {
    *map ^= maps->At(i);
    ASSERT(!map->IsNull());
    if (map->PC() == pc) {
      return map->raw();  // We found a stack map for this frame.
    }
  }
  // If the code has stackmaps, it must have them for all safepoints.
  UNREACHABLE();
  return Stackmap::null();
}


RawContext* Context::New(intptr_t num_variables, Heap::Space space) {
  ASSERT(num_variables >= 0);
  ASSERT(Object::context_class() != Class::null());

  if (num_variables < 0 || num_variables > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Context::New: invalid num_variables %" Pd "\n",
           num_variables);
  }
  Context& result = Context::Handle();
  {
    RawObject* raw = Object::Allocate(Context::kClassId,
                                      Context::InstanceSize(num_variables),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.set_num_variables(num_variables);
  }
  result.set_isolate(Isolate::Current());
  return result.raw();
}


const char* Context::ToCString() const {
  if (IsNull()) {
    return "Context (Null)";
  }
  const Context& parent_ctx = Context::Handle(parent());
  if (parent_ctx.IsNull()) {
    const char* kFormat = "Context num_variables:% " Pd "";
    intptr_t len = OS::SNPrint(NULL, 0, kFormat, num_variables()) + 1;
    char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
    OS::SNPrint(chars, len, kFormat, num_variables());
    return chars;
  } else {
    const char* parent_str = parent_ctx.ToCString();
    const char* kFormat = "Context num_variables:% " Pd " parent:{ %s }";
    intptr_t len = OS::SNPrint(NULL, 0, kFormat,
                               num_variables(), parent_str) + 1;
    char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
    OS::SNPrint(chars, len, kFormat, num_variables(), parent_str);
    return chars;
  }
}


void Context::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawContextScope* ContextScope::New(intptr_t num_variables) {
  ASSERT(Object::context_scope_class() != Class::null());
  if (num_variables < 0 || num_variables > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in ContextScope::New: invalid num_variables %" Pd "\n",
           num_variables);
  }
  intptr_t size = ContextScope::InstanceSize(num_variables);
  ContextScope& result = ContextScope::Handle();
  {
    RawObject* raw = Object::Allocate(ContextScope::kClassId,
                                      size,
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
    result.set_num_variables(num_variables);
  }
  return result.raw();
}


intptr_t ContextScope::TokenIndexAt(intptr_t scope_index) const {
  return Smi::Value(VariableDescAddr(scope_index)->token_pos);
}


void ContextScope::SetTokenIndexAt(intptr_t scope_index,
                                   intptr_t token_pos) const {
  VariableDescAddr(scope_index)->token_pos = Smi::New(token_pos);
}


RawString* ContextScope::NameAt(intptr_t scope_index) const {
  return VariableDescAddr(scope_index)->name;
}


void ContextScope::SetNameAt(intptr_t scope_index, const String& name) const {
  StorePointer(&(VariableDescAddr(scope_index)->name), name.raw());
}


bool ContextScope::IsFinalAt(intptr_t scope_index) const {
  return Bool::Handle(VariableDescAddr(scope_index)->is_final).value();
}


void ContextScope::SetIsFinalAt(intptr_t scope_index, bool is_final) const {
  VariableDescAddr(scope_index)->is_final = Bool::Get(is_final).raw();
}


bool ContextScope::IsConstAt(intptr_t scope_index) const {
  return Bool::Handle(VariableDescAddr(scope_index)->is_const).value();
}


void ContextScope::SetIsConstAt(intptr_t scope_index, bool is_const) const {
  VariableDescAddr(scope_index)->is_const = Bool::Get(is_const).raw();
}


RawAbstractType* ContextScope::TypeAt(intptr_t scope_index) const {
  ASSERT(!IsConstAt(scope_index));
  return VariableDescAddr(scope_index)->type;
}


void ContextScope::SetTypeAt(
    intptr_t scope_index, const AbstractType& type) const {
  StorePointer(&(VariableDescAddr(scope_index)->type), type.raw());
}


RawInstance* ContextScope::ConstValueAt(intptr_t scope_index) const {
  ASSERT(IsConstAt(scope_index));
  return VariableDescAddr(scope_index)->value;
}


void ContextScope::SetConstValueAt(
    intptr_t scope_index, const Instance& value) const {
  ASSERT(IsConstAt(scope_index));
  StorePointer(&(VariableDescAddr(scope_index)->value), value.raw());
}


intptr_t ContextScope::ContextIndexAt(intptr_t scope_index) const {
  return Smi::Value(VariableDescAddr(scope_index)->context_index);
}


void ContextScope::SetContextIndexAt(intptr_t scope_index,
                                     intptr_t context_index) const {
  VariableDescAddr(scope_index)->context_index = Smi::New(context_index);
}


intptr_t ContextScope::ContextLevelAt(intptr_t scope_index) const {
  return Smi::Value(VariableDescAddr(scope_index)->context_level);
}


void ContextScope::SetContextLevelAt(intptr_t scope_index,
                                     intptr_t context_level) const {
  VariableDescAddr(scope_index)->context_level = Smi::New(context_level);
}


const char* ContextScope::ToCString() const {
  return "ContextScope";
}


void ContextScope::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


const char* ICData::ToCString() const {
  const char* kFormat = "ICData target:'%s' num-args: %" Pd
                        " num-checks: %" Pd "";
  const String& name = String::Handle(target_name());
  const intptr_t num_args = num_args_tested();
  const intptr_t num_checks = NumberOfChecks();
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, name.ToCString(),
      num_args, num_checks) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, name.ToCString(), num_args, num_checks);
  return chars;
}


void ICData::set_function(const Function& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->function_, value.raw());
}


void ICData::set_target_name(const String& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->target_name_, value.raw());
}


void ICData::set_arguments_descriptor(const Array& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->args_descriptor_, value.raw());
}

void ICData::set_deopt_id(intptr_t value) const {
  raw_ptr()->deopt_id_ = value;
}


void ICData::set_num_args_tested(intptr_t value) const {
  raw_ptr()->num_args_tested_ = value;
}


void ICData::set_ic_data(const Array& value) const {
  ASSERT(!value.IsNull());
  StorePointer(&raw_ptr()->ic_data_, value.raw());
}


void ICData::set_deopt_reason(intptr_t deopt_reason) const {
  raw_ptr()->deopt_reason_ = deopt_reason;
}

void ICData::set_is_closure_call(bool value) const {
  raw_ptr()->is_closure_call_ = value ? 1 : 0;
}


intptr_t ICData::TestEntryLengthFor(intptr_t num_args) {
  return num_args + 1 /* target function*/ + 1 /* frequency */;
}


intptr_t ICData::TestEntryLength() const {
  return TestEntryLengthFor(num_args_tested());
}


intptr_t ICData::NumberOfChecks() const {
  // Do not count the sentinel;
  return (Array::Handle(ic_data()).Length() / TestEntryLength()) - 1;
}


void ICData::WriteSentinel(const Array& data) const {
  ASSERT(!data.IsNull());
  for (intptr_t i = 1; i <= TestEntryLength(); i++) {
    data.SetAt(data.Length() - i, smi_illegal_cid());
  }
}


#if defined(DEBUG)
// Used in asserts to verify that a check is not added twice.
bool ICData::HasCheck(const GrowableArray<intptr_t>& cids) const {
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 0; i < len; i++) {
    GrowableArray<intptr_t> class_ids;
    Function& target = Function::Handle();
    GetCheckAt(i, &class_ids, &target);
    bool matches = true;
    for (intptr_t k = 0; k < class_ids.length(); k++) {
      if (class_ids[k] != cids[k]) {
        matches = false;
        break;
      }
    }
    if (matches) {
      return true;
    }
  }
  return false;
}
#endif  // DEBUG


// Used for unoptimized static calls when no class-ids are checked.
void ICData::AddTarget(const Function& target) const {
  ASSERT(!target.IsNull());
  if (num_args_tested() > 0) {
    // Create a fake cid entry, so that we can store the target.
    GrowableArray<intptr_t> class_ids(num_args_tested());
    for (intptr_t i = 0; i < num_args_tested(); i++) {
      class_ids.Add(kObjectCid);
    }
    AddCheck(class_ids, target);
    return;
  }
  ASSERT(num_args_tested() >= 0);
  // Can add only once.
  const intptr_t old_num = NumberOfChecks();
  ASSERT(old_num == 0);
  Array& data = Array::Handle(ic_data());
  const intptr_t new_len = data.Length() + TestEntryLength();
  data = Array::Grow(data, new_len, Heap::kOld);
  set_ic_data(data);
  WriteSentinel(data);
  intptr_t data_pos = old_num * TestEntryLength();
  ASSERT(!target.IsNull());
  data.SetAt(data_pos++, target);
  const Smi& value = Smi::Handle(Smi::New(0));
  data.SetAt(data_pos, value);
}


void ICData::AddCheck(const GrowableArray<intptr_t>& class_ids,
                      const Function& target) const {
  ASSERT(!target.IsNull());
  DEBUG_ASSERT(!HasCheck(class_ids));
  ASSERT(num_args_tested() > 1);  // Otherwise use 'AddReceiverCheck'.
  ASSERT(class_ids.length() == num_args_tested());
  const intptr_t old_num = NumberOfChecks();
  Array& data = Array::Handle(ic_data());
  // ICData of static calls with num_args_tested() > 0 have initially a
  // dummy set of cids entered (see ICData::AddTarget). That entry is
  // overwritten by first real type feedback data.
  if (old_num == 1) {
    bool has_dummy_entry = true;
    for (intptr_t i = 0; i < num_args_tested(); i++) {
      if (Smi::Value(Smi::RawCast(data.At(i))) != kObjectCid) {
        has_dummy_entry = false;
        break;
      }
    }
    if (has_dummy_entry) {
      ASSERT(target.raw() == data.At(num_args_tested()));
      // Replace dummy entry.
      Smi& value = Smi::Handle();
      for (intptr_t i = 0; i < num_args_tested(); i++) {
        ASSERT(class_ids[i] != kIllegalCid);
        value = Smi::New(class_ids[i]);
        data.SetAt(i, value);
      }
      return;
    }
  }
  const intptr_t new_len = data.Length() + TestEntryLength();
  data = Array::Grow(data, new_len, Heap::kOld);
  set_ic_data(data);
  WriteSentinel(data);
  intptr_t data_pos = old_num * TestEntryLength();
  Smi& value = Smi::Handle();
  for (intptr_t i = 0; i < class_ids.length(); i++) {
    // kIllegalCid is used as terminating value, do not add it.
    ASSERT(class_ids[i] != kIllegalCid);
    value = Smi::New(class_ids[i]);
    data.SetAt(data_pos++, value);
  }
  ASSERT(!target.IsNull());
  data.SetAt(data_pos++, target);
  value = Smi::New(1);
  data.SetAt(data_pos, value);
}


void ICData::AddReceiverCheck(intptr_t receiver_class_id,
                              const Function& target,
                              intptr_t count) const {
#if defined(DEBUG)
  GrowableArray<intptr_t> class_ids(1);
  class_ids.Add(receiver_class_id);
  ASSERT(!HasCheck(class_ids));
#endif  // DEBUG
  ASSERT(!target.IsNull());
  ASSERT(num_args_tested() == 1);  // Otherwise use 'AddCheck'.
  ASSERT(receiver_class_id != kIllegalCid);

  const intptr_t old_num = NumberOfChecks();
  Array& data = Array::Handle(ic_data());
  const intptr_t new_len = data.Length() + TestEntryLength();
  data = Array::Grow(data, new_len, Heap::kOld);
  set_ic_data(data);
  WriteSentinel(data);
  intptr_t data_pos = old_num * TestEntryLength();
  if ((receiver_class_id == kSmiCid) && (data_pos > 0)) {
    ASSERT(GetReceiverClassIdAt(0) != kSmiCid);
    // Move class occupying position 0 to the data_pos.
    for (intptr_t i = 0; i < TestEntryLength(); i++) {
      data.SetAt(data_pos + i, Object::Handle(data.At(i)));
    }
    // Insert kSmiCid in position 0.
    data_pos = 0;
  }
  data.SetAt(data_pos, Smi::Handle(Smi::New(receiver_class_id)));
  data.SetAt(data_pos + 1, target);
  data.SetAt(data_pos + 2, Smi::Handle(Smi::New(count)));
}


void ICData::GetCheckAt(intptr_t index,
                        GrowableArray<intptr_t>* class_ids,
                        Function* target) const {
  ASSERT(index < NumberOfChecks());
  ASSERT(class_ids != NULL);
  ASSERT(target != NULL);
  class_ids->Clear();
  const Array& data = Array::Handle(ic_data());
  intptr_t data_pos = index * TestEntryLength();
  for (intptr_t i = 0; i < num_args_tested(); i++) {
    class_ids->Add(Smi::Value(Smi::RawCast(data.At(data_pos++))));
  }
  (*target) ^= data.At(data_pos++);
}


void ICData::GetOneClassCheckAt(intptr_t index,
                                intptr_t* class_id,
                                Function* target) const {
  ASSERT(class_id != NULL);
  ASSERT(target != NULL);
  ASSERT(num_args_tested() == 1);
  const Array& data = Array::Handle(ic_data());
  const intptr_t data_pos = index * TestEntryLength();
  *class_id = Smi::Value(Smi::RawCast(data.At(data_pos)));
  *target ^= data.At(data_pos + 1);
}


intptr_t ICData::GetCidAt(intptr_t index) const {
  ASSERT(num_args_tested() == 1);
  const Array& data = Array::Handle(ic_data());
  const intptr_t data_pos = index * TestEntryLength();
  return Smi::Value(Smi::RawCast(data.At(data_pos)));
}


intptr_t ICData::GetClassIdAt(intptr_t index, intptr_t arg_nr) const {
  GrowableArray<intptr_t> class_ids;
  Function& target = Function::Handle();
  GetCheckAt(index, &class_ids, &target);
  return class_ids[arg_nr];
}


intptr_t ICData::GetReceiverClassIdAt(intptr_t index) const {
  ASSERT(index < NumberOfChecks());
  const Array& data = Array::Handle(ic_data());
  const intptr_t data_pos = index * TestEntryLength();
  return Smi::Value(Smi::RawCast(data.At(data_pos)));
}


RawFunction* ICData::GetTargetAt(intptr_t index) const {
  const Array& data = Array::Handle(ic_data());
  const intptr_t data_pos = index * TestEntryLength() + num_args_tested();
  ASSERT(Object::Handle(data.At(data_pos)).IsFunction());
  return reinterpret_cast<RawFunction*>(data.At(data_pos));
}


void ICData::IncrementCountAt(intptr_t index, intptr_t value) const {
  ASSERT(0 <= value);
  ASSERT(value <= Smi::kMaxValue);
  SetCountAt(index, Utils::Minimum(GetCountAt(index) + value, Smi::kMaxValue));
}


void ICData::SetCountAt(intptr_t index, intptr_t value) const {
  ASSERT(0 <= value);
  ASSERT(value <= Smi::kMaxValue);

  const Array& data = Array::Handle(ic_data());
  const intptr_t data_pos = index * TestEntryLength() +
      CountIndexFor(num_args_tested());
  data.SetAt(data_pos, Smi::Handle(Smi::New(value)));
}


intptr_t ICData::GetCountAt(intptr_t index) const {
  const Array& data = Array::Handle(ic_data());
  const intptr_t data_pos = index * TestEntryLength() +
      CountIndexFor(num_args_tested());
  return Smi::Value(Smi::RawCast(data.At(data_pos)));
}


intptr_t ICData::AggregateCount() const {
  if (IsNull()) return 0;
  const intptr_t len = NumberOfChecks();
  intptr_t count = 0;
  for (intptr_t i = 0; i < len; i++) {
    count += GetCountAt(i);
  }
  return count;
}


RawFunction* ICData::GetTargetForReceiverClassId(intptr_t class_id) const {
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 0; i < len; i++) {
    if (GetReceiverClassIdAt(i) == class_id) {
      return GetTargetAt(i);
    }
  }
  return Function::null();
}


RawICData* ICData::AsUnaryClassChecksForArgNr(intptr_t arg_nr) const {
  ASSERT(!IsNull());
  ASSERT(num_args_tested() > arg_nr);
  if ((arg_nr == 0) && (num_args_tested() == 1)) {
    // Frequent case.
    return raw();
  }
  const intptr_t kNumArgsTested = 1;
  ICData& result = ICData::Handle(ICData::New(
      Function::Handle(function()),
      String::Handle(target_name()),
      Array::Handle(arguments_descriptor()),
      deopt_id(),
      kNumArgsTested));
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 0; i < len; i++) {
    const intptr_t class_id = GetClassIdAt(i, arg_nr);
    const intptr_t count = GetCountAt(i);
    intptr_t duplicate_class_id = -1;
    const intptr_t result_len = result.NumberOfChecks();
    for (intptr_t k = 0; k < result_len; k++) {
      if (class_id == result.GetReceiverClassIdAt(k)) {
        duplicate_class_id = k;
        break;
      }
    }
    if (duplicate_class_id >= 0) {
      // This check is valid only when checking the receiver.
      ASSERT((arg_nr != 0) ||
             (result.GetTargetAt(duplicate_class_id) == GetTargetAt(i)));
      result.IncrementCountAt(duplicate_class_id, count);
    } else {
      // This will make sure that Smi is first if it exists.
      result.AddReceiverCheck(class_id,
                              Function::Handle(GetTargetAt(i)),
                              count);
    }
  }
  // Copy deoptimization reason.
  result.set_deopt_reason(deopt_reason());

  return result.raw();
}


bool ICData::AllTargetsHaveSameOwner(intptr_t owner_cid) const {
  if (NumberOfChecks() == 0) return false;
  Class& cls = Class::Handle();
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 0; i < len; i++) {
    cls = Function::Handle(GetTargetAt(i)).Owner();
    if (cls.id() != owner_cid) {
      return false;
    }
  }
  return true;
}


bool ICData::AllReceiversAreNumbers() const {
  if (NumberOfChecks() == 0) return false;
  Class& cls = Class::Handle();
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 0; i < len; i++) {
    cls = Function::Handle(GetTargetAt(i)).Owner();
    const intptr_t cid = cls.id();
    if ((cid != kSmiCid) &&
        (cid != kMintCid) &&
        (cid != kBigintCid) &&
        (cid != kDoubleCid)) {
      return false;
    }
  }
  return true;
}


bool ICData::HasReceiverClassId(intptr_t class_id) const {
  ASSERT(num_args_tested() > 0);
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 0; i < len; i++) {
    const intptr_t test_class_id = GetReceiverClassIdAt(i);
    if (test_class_id == class_id) {
      return true;
    }
  }
  return false;
}


// Returns true if all targets are the same.
// TODO(srdjan): if targets are native use their C_function to compare.
bool ICData::HasOneTarget() const {
  ASSERT(NumberOfChecks() > 0);
  const Function& first_target = Function::Handle(GetTargetAt(0));
  const intptr_t len = NumberOfChecks();
  for (intptr_t i = 1; i < len; i++) {
    if (GetTargetAt(i) != first_target.raw()) {
      return false;
    }
  }
  return true;
}


RawICData* ICData::New(const Function& function,
                       const String& target_name,
                       const Array& arguments_descriptor,
                       intptr_t deopt_id,
                       intptr_t num_args_tested) {
  ASSERT(!function.IsNull());
  ASSERT(!target_name.IsNull());
  ASSERT(!arguments_descriptor.IsNull());
  ASSERT(Object::icdata_class() != Class::null());
  ASSERT(num_args_tested >= 0);
  ICData& result = ICData::Handle();
  {
    // IC data objects are long living objects, allocate them in old generation.
    RawObject* raw = Object::Allocate(ICData::kClassId,
                                      ICData::InstanceSize(),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_function(function);
  result.set_target_name(target_name);
  result.set_arguments_descriptor(arguments_descriptor);
  result.set_deopt_id(deopt_id);
  result.set_num_args_tested(num_args_tested);
  result.set_deopt_reason(kDeoptUnknown);
  result.set_is_closure_call(false);
  // Number of array elements in one test entry.
  intptr_t len = result.TestEntryLength();
  // IC data array must be null terminated (sentinel entry).
  const Array& ic_data = Array::Handle(Array::New(len, Heap::kOld));
  result.set_ic_data(ic_data);
  result.WriteSentinel(ic_data);
  return result.raw();
}


void ICData::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawArray* MegamorphicCache::buckets() const {
  return raw_ptr()->buckets_;
}


void MegamorphicCache::set_buckets(const Array& buckets) const {
  StorePointer(&raw_ptr()->buckets_, buckets.raw());
}


// Class IDs in the table are smi-tagged, so we use a smi-tagged mask
// and target class ID to avoid untagging (on each iteration of the
// test loop) in generated code.
intptr_t MegamorphicCache::mask() const {
  return Smi::Value(raw_ptr()->mask_);
}


void MegamorphicCache::set_mask(intptr_t mask) const {
  raw_ptr()->mask_ = Smi::New(mask);
}


intptr_t MegamorphicCache::filled_entry_count() const {
  return raw_ptr()->filled_entry_count_;
}


void MegamorphicCache::set_filled_entry_count(intptr_t count) const {
  raw_ptr()->filled_entry_count_ = count;
}


RawMegamorphicCache* MegamorphicCache::New() {
  MegamorphicCache& result = MegamorphicCache::Handle();
  { RawObject* raw = Object::Allocate(MegamorphicCache::kClassId,
                                      MegamorphicCache::InstanceSize(),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
  }
  const intptr_t capacity = kInitialCapacity;
  const Array& buckets = Array::Handle(Array::New(kEntryLength * capacity));
  const Function& handler = Function::Handle(
      Isolate::Current()->megamorphic_cache_table()->miss_handler());
  for (intptr_t i = 0; i < capacity; ++i) {
    SetEntry(buckets, i, smi_illegal_cid(), handler);
  }
  result.set_buckets(buckets);
  result.set_mask(capacity - 1);
  result.set_filled_entry_count(0);
  return result.raw();
}


void MegamorphicCache::EnsureCapacity() const {
  intptr_t old_capacity = mask() + 1;
  double load_limit = kLoadFactor * static_cast<double>(old_capacity);
  if (static_cast<double>(filled_entry_count() + 1) > load_limit) {
    const Array& old_buckets = Array::Handle(buckets());
    intptr_t new_capacity = old_capacity * 2;
    const Array& new_buckets =
        Array::Handle(Array::New(kEntryLength * new_capacity));

    Function& target = Function::Handle(
        Isolate::Current()->megamorphic_cache_table()->miss_handler());
    for (intptr_t i = 0; i < new_capacity; ++i) {
      SetEntry(new_buckets, i, smi_illegal_cid(), target);
    }
    set_buckets(new_buckets);
    set_mask(new_capacity - 1);
    set_filled_entry_count(0);

    // Rehash the valid entries.
    Smi& class_id = Smi::Handle();
    for (intptr_t i = 0; i < old_capacity; ++i) {
      class_id ^= GetClassId(old_buckets, i);
      if (class_id.Value() != kIllegalCid) {
        target ^= GetTargetFunction(old_buckets, i);
        Insert(class_id, target);
      }
    }
  }
}


void MegamorphicCache::Insert(const Smi& class_id,
                              const Function& target) const {
  ASSERT(static_cast<double>(filled_entry_count() + 1) <=
         (kLoadFactor * static_cast<double>(mask() + 1)));
  const Array& backing_array = Array::Handle(buckets());
  intptr_t id_mask = mask();
  intptr_t index = class_id.Value() & id_mask;
  intptr_t i = index;
  do {
    if (Smi::Value(Smi::RawCast(GetClassId(backing_array, i))) == kIllegalCid) {
      SetEntry(backing_array, i, class_id, target);
      set_filled_entry_count(filled_entry_count() + 1);
      return;
    }
    i = (i + 1) & id_mask;
  } while (i != index);
  UNREACHABLE();
}


const char* MegamorphicCache::ToCString() const {
  return "";
}


void MegamorphicCache::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawSubtypeTestCache* SubtypeTestCache::New() {
  ASSERT(Object::subtypetestcache_class() != Class::null());
  SubtypeTestCache& result = SubtypeTestCache::Handle();
  {
    // SubtypeTestCache objects are long living objects, allocate them in the
    // old generation.
    RawObject* raw = Object::Allocate(SubtypeTestCache::kClassId,
                                      SubtypeTestCache::InstanceSize(),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
  }
  const Array& cache = Array::Handle(Array::New(kTestEntryLength));
  result.set_cache(cache);
  return result.raw();
}


void SubtypeTestCache::set_cache(const Array& value) const {
  StorePointer(&raw_ptr()->cache_, value.raw());
}


intptr_t SubtypeTestCache::NumberOfChecks() const {
  // Do not count the sentinel;
  return (Array::Handle(cache()).Length() / kTestEntryLength) - 1;
}


void SubtypeTestCache::AddCheck(
    intptr_t instance_class_id,
    const AbstractTypeArguments& instance_type_arguments,
    const AbstractTypeArguments& instantiator_type_arguments,
    const Bool& test_result) const {
  intptr_t old_num = NumberOfChecks();
  Array& data = Array::Handle(cache());
  intptr_t new_len = data.Length() + kTestEntryLength;
  data = Array::Grow(data, new_len);
  set_cache(data);
  intptr_t data_pos = old_num * kTestEntryLength;
  data.SetAt(data_pos + kInstanceClassId,
      Smi::Handle(Smi::New(instance_class_id)));
  data.SetAt(data_pos + kInstanceTypeArguments, instance_type_arguments);
  data.SetAt(data_pos + kInstantiatorTypeArguments,
      instantiator_type_arguments);
  data.SetAt(data_pos + kTestResult, test_result);
}


void SubtypeTestCache::GetCheck(
    intptr_t ix,
    intptr_t* instance_class_id,
    AbstractTypeArguments* instance_type_arguments,
    AbstractTypeArguments* instantiator_type_arguments,
    Bool* test_result) const {
  Array& data = Array::Handle(cache());
  intptr_t data_pos = ix * kTestEntryLength;
  *instance_class_id =
      Smi::Value(Smi::RawCast(data.At(data_pos + kInstanceClassId)));
  *instance_type_arguments ^= data.At(data_pos + kInstanceTypeArguments);
  *instantiator_type_arguments ^=
      data.At(data_pos + kInstantiatorTypeArguments);
  *test_result ^= data.At(data_pos + kTestResult);
}


const char* SubtypeTestCache::ToCString() const {
  return "SubtypeTestCache";
}


void SubtypeTestCache::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


const char* Error::ToErrorCString() const {
  UNREACHABLE();
  return "Internal Error";
}


const char* Error::ToCString() const {
  // Error is an abstract class.  We should never reach here.
  UNREACHABLE();
  return "Error";
}


void Error::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawApiError* ApiError::New() {
  ASSERT(Object::api_error_class() != Class::null());
  RawObject* raw = Object::Allocate(ApiError::kClassId,
                                    ApiError::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawApiError*>(raw);
}


RawApiError* ApiError::New(const String& message, Heap::Space space) {
  ASSERT(Object::api_error_class() != Class::null());
  ApiError& result = ApiError::Handle();
  {
    RawObject* raw = Object::Allocate(ApiError::kClassId,
                                      ApiError::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_message(message);
  return result.raw();
}


void ApiError::set_message(const String& message) const {
  StorePointer(&raw_ptr()->message_, message.raw());
}


const char* ApiError::ToErrorCString() const {
  const String& msg_str = String::Handle(message());
  return msg_str.ToCString();
}


const char* ApiError::ToCString() const {
  return "ApiError";
}


void ApiError::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawLanguageError* LanguageError::New() {
  ASSERT(Object::language_error_class() != Class::null());
  RawObject* raw = Object::Allocate(LanguageError::kClassId,
                                    LanguageError::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawLanguageError*>(raw);
}


RawLanguageError* LanguageError::New(const String& message, Heap::Space space) {
  ASSERT(Object::language_error_class() != Class::null());
  LanguageError& result = LanguageError::Handle();
  {
    RawObject* raw = Object::Allocate(LanguageError::kClassId,
                                      LanguageError::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_message(message);
  return result.raw();
}


void LanguageError::set_message(const String& message) const {
  StorePointer(&raw_ptr()->message_, message.raw());
}


const char* LanguageError::ToErrorCString() const {
  const String& msg_str = String::Handle(message());
  return msg_str.ToCString();
}


const char* LanguageError::ToCString() const {
  return "LanguageError";
}


void LanguageError::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawUnhandledException* UnhandledException::New(const Instance& exception,
                                               const Instance& stacktrace,
                                               Heap::Space space) {
  ASSERT(Object::unhandled_exception_class() != Class::null());
  UnhandledException& result = UnhandledException::Handle();
  {
    RawObject* raw = Object::Allocate(UnhandledException::kClassId,
                                      UnhandledException::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_exception(exception);
  result.set_stacktrace(stacktrace);
  return result.raw();
}


void UnhandledException::set_exception(const Instance& exception) const {
  StorePointer(&raw_ptr()->exception_, exception.raw());
}


void UnhandledException::set_stacktrace(const Instance& stacktrace) const {
  StorePointer(&raw_ptr()->stacktrace_, stacktrace.raw());
}


const char* UnhandledException::ToErrorCString() const {
  Isolate* isolate = Isolate::Current();
  if (exception() == isolate->object_store()->out_of_memory()) {
    return "Unhandled exception:\nOut of memory";
  }
  if (exception() == isolate->object_store()->stack_overflow()) {
    return "Unhandled exception:\nStack overflow";
  }
  HANDLESCOPE(isolate);
  Object& strtmp = Object::Handle();
  const Instance& exc = Instance::Handle(exception());
  strtmp = DartLibraryCalls::ToString(exc);
  const char* exc_str =
      "<Received error while converting exception to string>";
  if (!strtmp.IsError()) {
    exc_str = strtmp.ToCString();
  }
  const Instance& stack = Instance::Handle(stacktrace());
  strtmp = DartLibraryCalls::ToString(stack);
  const char* stack_str =
      "<Received error while converting stack trace to string>";
  if (!strtmp.IsError()) {
    stack_str = strtmp.ToCString();
  }

  const char* format = "Unhandled exception:\n%s\n%s";
  int len = (strlen(exc_str) + strlen(stack_str) + strlen(format)
             - 4    // Two '%s'
             + 1);  // '\0'
  char* chars = isolate->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, exc_str, stack_str);
  return chars;
}


const char* UnhandledException::ToCString() const {
  return "UnhandledException";
}


void UnhandledException::PrintToJSONStream(JSONStream* stream,
                                           bool ref) const {
  JSONObject jsobj(stream);
}


RawUnwindError* UnwindError::New(const String& message, Heap::Space space) {
  ASSERT(Object::unwind_error_class() != Class::null());
  UnwindError& result = UnwindError::Handle();
  {
    RawObject* raw = Object::Allocate(UnwindError::kClassId,
                                      UnwindError::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_message(message);
  return result.raw();
}


void UnwindError::set_message(const String& message) const {
  StorePointer(&raw_ptr()->message_, message.raw());
}


const char* UnwindError::ToErrorCString() const {
  const String& msg_str = String::Handle(message());
  return msg_str.ToCString();
}


const char* UnwindError::ToCString() const {
  return "UnwindError";
}


void UnwindError::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawObject* Instance::Evaluate(const String& expr) const {
  const Class& cls = Class::Handle(clazz());
  const PatchClass& temp_class =
      PatchClass::Handle(MakeTempPatchClass(cls, expr));
  const String& eval_func_name = String::Handle(Symbols::New(":eval"));
  const Function& eval_func =
      Function::Handle(Function::New(eval_func_name,
                                     RawFunction::kRegularFunction,
                                     false,  // Not static.
                                     false,  // Not const.
                                     false,  // Not abstract.
                                     false,  // Not external.
                                     temp_class,
                                     0));
  eval_func.set_result_type(Type::Handle(Type::DynamicType()));
  eval_func.set_num_fixed_parameters(1);
  eval_func.SetNumOptionalParameters(0, true);
  eval_func.set_is_optimizable(false);

  const Array& args = Array::Handle(Array::New(1));
  args.SetAt(0, *this);
  const Object& result =
      Object::Handle(DartEntry::InvokeFunction(eval_func, args));
  return result.raw();
}



bool Instance::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    return true;  // "===".
  }

  if (other.IsNull() || (this->clazz() != other.clazz())) {
    return false;
  }

  {
    NoGCScope no_gc;
    // Raw bits compare.
    const intptr_t instance_size = Class::Handle(this->clazz()).instance_size();
    ASSERT(instance_size != 0);
    uword this_addr = reinterpret_cast<uword>(this->raw_ptr());
    uword other_addr = reinterpret_cast<uword>(other.raw_ptr());
    for (intptr_t offset = Instance::NextFieldOffset();
         offset < instance_size;
         offset += kWordSize) {
      if ((*reinterpret_cast<RawObject**>(this_addr + offset)) !=
          (*reinterpret_cast<RawObject**>(other_addr + offset))) {
        return false;
      }
    }
  }
  return true;
}


#if defined(DEBUG)
class CheckForPointers : public ObjectPointerVisitor {
 public:
  explicit CheckForPointers(Isolate* isolate)
      : ObjectPointerVisitor(isolate), has_pointers_(false) {}

  bool has_pointers() const { return has_pointers_; }

  void VisitPointers(RawObject** first, RawObject** last) {
    if (first != last) {
      has_pointers_ = true;
    }
  }

 private:
  bool has_pointers_;

  DISALLOW_COPY_AND_ASSIGN(CheckForPointers);
};
#endif  // DEBUG


bool Instance::CheckAndCanonicalizeFields(const char** error_str) const {
  const Class& cls = Class::Handle(this->clazz());
  if ((cls.id() >= kNumPredefinedCids)) {
    // Iterate over all fields, canonicalize numbers and strings, expect all
    // other instances to be canonical otherwise report error (return false).
    Object& obj = Object::Handle();
    intptr_t end_field_offset = cls.instance_size() - kWordSize;
    for (intptr_t field_offset = 0;
         field_offset <= end_field_offset;
         field_offset += kWordSize) {
      obj = *this->FieldAddrAtOffset(field_offset);
      if (obj.IsInstance() && !obj.IsSmi() && !obj.IsCanonical()) {
        if (obj.IsNumber() || obj.IsString()) {
          obj = Instance::Cast(obj).CheckAndCanonicalize(NULL);
          ASSERT(!obj.IsNull());
          this->SetFieldAtOffset(field_offset, obj);
        } else {
          ASSERT(error_str != NULL);
          const char* kFormat = "field: %s\n";
          const intptr_t len =
              OS::SNPrint(NULL, 0, kFormat, obj.ToCString()) + 1;
          char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
          OS::SNPrint(chars, len, kFormat, obj.ToCString());
          *error_str = chars;
          return false;
        }
      }
    }
  } else {
#if defined(DEBUG)
    // Make sure that we are not missing any fields.
    CheckForPointers has_pointers(Isolate::Current());
    this->raw()->VisitPointers(&has_pointers);
    ASSERT(!has_pointers.has_pointers());
#endif  // DEBUG
  }
  return true;
}


RawInstance* Instance::CheckAndCanonicalize(const char** error_str) const {
  ASSERT(!IsNull());
  if (this->IsCanonical()) {
    return this->raw();
  }
  if (!CheckAndCanonicalizeFields(error_str)) {
    return Instance::null();
  }
  Instance& result = Instance::Handle();
  const Class& cls = Class::Handle(this->clazz());
  Array& constants = Array::Handle(cls.constants());
  const intptr_t constants_len = constants.Length();
  // Linear search to see whether this value is already present in the
  // list of canonicalized constants.
  intptr_t index = 0;
  while (index < constants_len) {
    result ^= constants.At(index);
    if (result.IsNull()) {
      break;
    }
    if (this->Equals(result)) {
      return result.raw();
    }
    index++;
  }
  // The value needs to be added to the list. Grow the list if
  // it is full.
  result ^= this->raw();
  if (result.IsNew()) {
    // Create a canonical object in old space.
    result ^= Object::Clone(result, Heap::kOld);
  }
  ASSERT(result.IsOld());
  cls.InsertCanonicalConstant(index, result);
  result.SetCanonical();
  return result.raw();
}


RawType* Instance::GetType() const {
  if (IsNull()) {
    return Type::NullType();
  }
  const Class& cls = Class::Handle(clazz());
  AbstractTypeArguments& type_arguments = AbstractTypeArguments::Handle();
  if (cls.NumTypeArguments() > 0) {
    type_arguments = GetTypeArguments();
  }
  const Type& type = Type::Handle(
      Type::New(cls, type_arguments, Scanner::kDummyTokenIndex));
  type.SetIsFinalized();
  return type.raw();
}


RawAbstractTypeArguments* Instance::GetTypeArguments() const {
  const Class& cls = Class::Handle(clazz());
  intptr_t field_offset = cls.type_arguments_field_offset();
  ASSERT(field_offset != Class::kNoTypeArguments);
  AbstractTypeArguments& type_arguments = AbstractTypeArguments::Handle();
  type_arguments ^= *FieldAddrAtOffset(field_offset);
  return type_arguments.raw();
}


void Instance::SetTypeArguments(const AbstractTypeArguments& value) const {
  const Class& cls = Class::Handle(clazz());
  intptr_t field_offset = cls.type_arguments_field_offset();
  ASSERT(field_offset != Class::kNoTypeArguments);
  SetFieldAtOffset(field_offset, value);
}


bool Instance::IsInstanceOf(const AbstractType& other,
                            const AbstractTypeArguments& other_instantiator,
                            Error* bound_error) const {
  ASSERT(other.IsFinalized());
  ASSERT(!other.IsDynamicType());
  ASSERT(!other.IsMalformed());
  ASSERT(!other.IsMalbounded());
  if (other.IsVoidType()) {
    return false;
  }
  Isolate* isolate = Isolate::Current();
  const Class& cls = Class::Handle(isolate, clazz());
  AbstractTypeArguments& type_arguments =
      AbstractTypeArguments::Handle(isolate);
  if (cls.NumTypeArguments() > 0) {
    type_arguments = GetTypeArguments();
    if (!type_arguments.IsNull() && !type_arguments.IsCanonical()) {
      type_arguments = type_arguments.Canonicalize();
      SetTypeArguments(type_arguments);
    }
    // The number of type arguments in the instance must be greater or equal to
    // the number of type arguments expected by the instance class.
    // A discrepancy is allowed for closures, which borrow the type argument
    // vector of their instantiator, which may be of a subclass of the class
    // defining the closure. Truncating the vector to the correct length on
    // instantiation is unnecessary. The vector may therefore be longer.
    // Also, an optimization reuses the type argument vector of the instantiator
    // of generic instances when its layout is compatible.
    ASSERT(type_arguments.IsNull() ||
           (type_arguments.Length() >= cls.NumTypeArguments()));
  }
  Class& other_class = Class::Handle(isolate);
  AbstractTypeArguments& other_type_arguments =
      AbstractTypeArguments::Handle(isolate);
  // Note that we may encounter a bound error in checked mode.
  if (!other.IsInstantiated()) {
    const AbstractType& instantiated_other = AbstractType::Handle(
        isolate, other.InstantiateFrom(other_instantiator, bound_error));
    if ((bound_error != NULL) && !bound_error->IsNull()) {
      ASSERT(FLAG_enable_type_checks);
      return false;
    }
    other_class = instantiated_other.type_class();
    other_type_arguments = instantiated_other.arguments();
  } else {
    other_class = other.type_class();
    other_type_arguments = other.arguments();
  }
  return cls.IsSubtypeOf(type_arguments, other_class, other_type_arguments,
                         bound_error);
}


bool Instance::IsIdenticalTo(const Instance& other) const {
  if (raw() == other.raw()) return true;
  if (IsInteger() && other.IsInteger()) {
    return Equals(other);
  }
  if (IsDouble() && other.IsDouble()) {
    if (Equals(other)) return true;
    // Check for NaN.
    const Double& a_double = Double::Cast(*this);
    const Double& b_double = Double::Cast(other);
    if (isnan(a_double.value()) && isnan(b_double.value())) {
      return true;
    }
  }
  return false;
}


void Instance::SetNativeField(int index, intptr_t value) const {
  ASSERT(IsValidNativeIndex(index));
  Object& native_fields = Object::Handle(*NativeFieldsAddr());
  if (native_fields.IsNull()) {
    // Allocate backing storage for the native fields.
    const Class& cls = Class::Handle(clazz());
    int num_native_fields = cls.num_native_fields();
    native_fields = TypedData::New(kIntPtrCid, num_native_fields);
    StorePointer(NativeFieldsAddr(), native_fields.raw());
  }
  intptr_t byte_offset = index * sizeof(intptr_t);
  TypedData::Cast(native_fields).SetIntPtr(byte_offset, value);
}


bool Instance::IsClosure() const {
  const Class& cls = Class::Handle(clazz());
  return cls.IsSignatureClass();
}


bool Instance::IsCallable(Function* function, Context* context) const {
  Class& cls = Class::Handle(clazz());
  if (cls.IsSignatureClass()) {
    if (function != NULL) {
      *function = Closure::function(*this);
    }
    if (context != NULL) {
      *context = Closure::context(*this);
    }
    return true;
  }
  // Try to resolve a "call" method.
  Function& call_function = Function::Handle();
  do {
    call_function = cls.LookupDynamicFunction(Symbols::Call());
    if (!call_function.IsNull()) {
      if (function != NULL) {
        *function = call_function.raw();
      }
      if (context != NULL) {
        *context = Isolate::Current()->object_store()->empty_context();
      }
      return true;
    }
    cls = cls.SuperClass();
  } while (!cls.IsNull());
  return false;
}


RawInstance* Instance::New(const Class& cls, Heap::Space space) {
  Isolate* isolate = Isolate::Current();
  if (cls.EnsureIsFinalized(isolate) != Error::null()) {
    return Instance::null();
  }
  intptr_t instance_size = cls.instance_size();
  ASSERT(instance_size > 0);
  RawObject* raw = Object::Allocate(cls.id(), instance_size, space);
  return reinterpret_cast<RawInstance*>(raw);
}


bool Instance::IsValidFieldOffset(int offset) const {
  const Class& cls = Class::Handle(clazz());
  return (offset >= 0 && offset <= (cls.instance_size() - kWordSize));
}


const char* Instance::ToCString() const {
  if (IsNull()) {
    return "null";
  } else if (raw() == Object::sentinel().raw()) {
    return "sentinel";
  } else if (raw() == Object::transition_sentinel().raw()) {
    return "transition_sentinel";
  } else if (raw() == Object::unknown_constant().raw()) {
    return "unknown_constant";
  } else if (raw() == Object::non_constant().raw()) {
    return "non_constant";
  } else if (Isolate::Current()->no_gc_scope_depth() > 0) {
    // Can occur when running disassembler.
    return "Instance";
  } else {
    if (IsClosure()) {
      return Closure::ToCString(*this);
    }
    const char* kFormat = "Instance of '%s'";
    const Class& cls = Class::Handle(clazz());
    AbstractTypeArguments& type_arguments = AbstractTypeArguments::Handle();
    const intptr_t num_type_arguments = cls.NumTypeArguments();
    if (num_type_arguments > 0) {
      type_arguments = GetTypeArguments();
    }
    const Type& type =
        Type::Handle(Type::New(cls, type_arguments, Scanner::kDummyTokenIndex));
    const String& type_name = String::Handle(type.Name());
    // Calculate the size of the string.
    intptr_t len = OS::SNPrint(NULL, 0, kFormat, type_name.ToCString()) + 1;
    char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
    OS::SNPrint(chars, len, kFormat, type_name.ToCString());
    return chars;
  }
}


void Instance::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


bool AbstractType::IsResolved() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractType::HasResolvedTypeClass() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


RawClass* AbstractType::type_class() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return Class::null();
}


RawUnresolvedClass* AbstractType::unresolved_class() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return UnresolvedClass::null();
}


RawAbstractTypeArguments* AbstractType::arguments() const  {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return NULL;
}


intptr_t AbstractType::token_pos() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return -1;
}


bool AbstractType::IsInstantiated() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractType::IsFinalized() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractType::IsBeingFinalized() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractType::IsMalformed() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


bool AbstractType::IsMalboundedWithError(Error* bound_error) const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return false;
}


RawError* AbstractType::malformed_error() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return Error::null();
}


void AbstractType::set_malformed_error(const Error& value) const {
  // AbstractType is an abstract class.
  UNREACHABLE();
}


bool AbstractType::Equals(const Instance& other) const {
  // AbstractType is an abstract class.
  ASSERT(raw() == AbstractType::null());
  return other.IsNull();
}


RawAbstractType* AbstractType::InstantiateFrom(
    const AbstractTypeArguments& instantiator_type_arguments,
    Error* bound_error) const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return NULL;
}


RawAbstractType* AbstractType::CloneUnfinalized() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return NULL;
}


RawAbstractType* AbstractType::Canonicalize() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return NULL;
}


RawString* AbstractType::BuildName(NameVisibility name_visibility) const {
  if (IsBoundedType()) {
    // TODO(regis): Should the bound be visible in the name for debug purposes
    // if name_visibility is kInternalName?
    const AbstractType& type = AbstractType::Handle(
        BoundedType::Cast(*this).type());
    return type.BuildName(name_visibility);
  }
  if (IsTypeParameter()) {
    return TypeParameter::Cast(*this).name();
  }
  // If the type is still being finalized, we may be reporting an error about
  // a malformed type, so proceed with caution.
  const AbstractTypeArguments& args =
      AbstractTypeArguments::Handle(arguments());
  const intptr_t num_args = args.IsNull() ? 0 : args.Length();
  String& class_name = String::Handle();
  intptr_t first_type_param_index;
  intptr_t num_type_params;  // Number of type parameters to print.
  if (HasResolvedTypeClass()) {
    const Class& cls = Class::Handle(type_class());
    num_type_params = cls.NumTypeParameters();  // Do not print the full vector.
    if (name_visibility == kInternalName) {
      class_name = cls.Name();
    } else {
      ASSERT(name_visibility == kUserVisibleName);
      // Map internal types to their corresponding public interfaces.
      class_name = cls.UserVisibleName();
    }
    if (num_type_params > num_args) {
      first_type_param_index = 0;
      if (!IsFinalized() || IsBeingFinalized() || IsMalformed()) {
        // Most probably a malformed type. Do not fill up with "dynamic",
        // but use actual vector.
        num_type_params = num_args;
      } else {
        ASSERT(num_args == 0);  // Type is raw.
        // No need to fill up with "dynamic".
        num_type_params = 0;
      }
    } else {
      // The actual type argument vector can be longer than necessary, because
      // of type optimizations.
      if (IsFinalized() && cls.is_type_finalized()) {
        first_type_param_index = cls.NumTypeArguments() - num_type_params;
      } else {
        first_type_param_index = num_args - num_type_params;
      }
    }
    if (cls.IsSignatureClass()) {
      // We may be reporting an error about a malformed function type. In that
      // case, avoid instantiating the signature, since it may lead to cycles.
      if (!IsFinalized() || IsBeingFinalized() || IsMalformed()) {
        return class_name.raw();
      }
      // In order to avoid cycles, print the name of a typedef (non-canonical
      // signature class) as a regular, possibly parameterized, class.
      if (cls.IsCanonicalSignatureClass()) {
        const Function& signature_function = Function::Handle(
            cls.signature_function());
        // Signature classes have no super type, however, they take as many
        // type arguments as the owner class of their signature function (if it
        // is non static and generic, see Class::NumTypeArguments()). Therefore,
        // first_type_param_index may be greater than 0 here.
        return signature_function.InstantiatedSignatureFrom(args,
                                                            name_visibility);
      }
    }
  } else {
    const UnresolvedClass& cls = UnresolvedClass::Handle(unresolved_class());
    class_name = cls.Name();
    num_type_params = num_args;
    first_type_param_index = 0;
  }
  String& type_name = String::Handle();
  if (num_type_params == 0) {
    type_name = class_name.raw();
  } else {
    const String& args_name = String::Handle(
        args.SubvectorName(first_type_param_index,
                           num_type_params,
                           name_visibility));
    type_name = String::Concat(class_name, args_name);
  }
  // The name is only used for type checking and debugging purposes.
  // Unless profiling data shows otherwise, it is not worth caching the name in
  // the type.
  return Symbols::New(type_name);
}


RawString* AbstractType::ClassName() const {
  if (HasResolvedTypeClass()) {
    return Class::Handle(type_class()).Name();
  } else {
    return UnresolvedClass::Handle(unresolved_class()).Name();
  }
}


bool AbstractType::IsNullType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Isolate::Current()->object_store()->null_class());
}


bool AbstractType::IsBoolType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Isolate::Current()->object_store()->bool_class());
}


bool AbstractType::IsIntType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::IntType()).type_class());
}


bool AbstractType::IsDoubleType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::Double()).type_class());
}


bool AbstractType::IsFloat32x4Type() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::Float32x4()).type_class());
}


bool AbstractType::IsInt32x4Type() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::Int32x4()).type_class());
}


bool AbstractType::IsNumberType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::Number()).type_class());
}


bool AbstractType::IsStringType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::StringType()).type_class());
}


bool AbstractType::IsFunctionType() const {
  return HasResolvedTypeClass() &&
      (type_class() == Type::Handle(Type::Function()).type_class());
}


bool AbstractType::TypeTest(TypeTestKind test_kind,
                            const AbstractType& other,
                            Error* bound_error) const {
  ASSERT(IsResolved());
  ASSERT(other.IsResolved());
  ASSERT(!IsMalformed());
  ASSERT(!other.IsMalformed());
  // In case the type checked in a type test is malbounded, the code generator
  // may compile a throw instead of a run time call performing the type check.
  // However, in checked mode, a function type may include malbounded result
  // type and/or malbounded parameter types, which will then be encountered here
  // at run time.
  if (IsMalbounded()) {
    ASSERT(FLAG_enable_type_checks);
    if ((bound_error != NULL) && bound_error->IsNull()) {
      const bool is_malbounded = IsMalboundedWithError(bound_error);
      ASSERT(is_malbounded);
    }
    return false;
  }
  if (other.IsMalbounded()) {
    ASSERT(FLAG_enable_type_checks);
    if ((bound_error != NULL) && bound_error->IsNull()) {
      const bool other_is_malbounded = other.IsMalboundedWithError(bound_error);
      ASSERT(other_is_malbounded);
    }
    return false;
  }
  if (IsBoundedType() || other.IsBoundedType()) {
    if (Equals(other)) {
      return true;
    }
    return false;  // TODO(regis): We should return "maybe after instantiation".
  }
  // Type parameters cannot be handled by Class::TypeTest().
  // When comparing two uninstantiated function types, one returning type
  // parameter K, the other returning type parameter V, we cannot assume that K
  // is a subtype of V, or vice versa. We only return true if K equals V, as
  // defined by TypeParameter::Equals.
  // The same rule applies when checking the upper bound of a still
  // uninstantiated type at compile time. Returning false will defer the test
  // to run time.
  // There are however some cases can be decided at compile time.
  // For example, with class A<K, V extends K>, new A<T, T> called from within
  // a class B<T> will never require a run time bound check, even if T is
  // uninstantiated at compile time.
  if (IsTypeParameter()) {
    const TypeParameter& type_param = TypeParameter::Cast(*this);
    if (other.IsTypeParameter()) {
      const TypeParameter& other_type_param = TypeParameter::Cast(other);
      if (type_param.Equals(other_type_param)) {
        return true;
      }
    }
    const AbstractType& bound = AbstractType::Handle(type_param.bound());
    if (bound.IsMoreSpecificThan(other, bound_error)) {
      return true;
    }
    return false;  // TODO(regis): We should return "maybe after instantiation".
  }
  if (other.IsTypeParameter()) {
    return false;  // TODO(regis): We should return "maybe after instantiation".
  }
  const Class& cls = Class::Handle(type_class());
  return cls.TypeTest(test_kind,
                      AbstractTypeArguments::Handle(arguments()),
                      Class::Handle(other.type_class()),
                      AbstractTypeArguments::Handle(other.arguments()),
                      bound_error);
}


intptr_t AbstractType::Hash() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return 0;
}


const char* AbstractType::ToCString() const {
  // AbstractType is an abstract class.
  UNREACHABLE();
  return "AbstractType";
}


void AbstractType::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawType* Type::NullType() {
  return Isolate::Current()->object_store()->null_type();
}


RawType* Type::DynamicType() {
  return Object::dynamic_type();
}


RawType* Type::VoidType() {
  return Object::void_type();
}


RawType* Type::ObjectType() {
  return Isolate::Current()->object_store()->object_type();
}


RawType* Type::BoolType() {
  return Isolate::Current()->object_store()->bool_type();
}


RawType* Type::IntType() {
  return Isolate::Current()->object_store()->int_type();
}


RawType* Type::SmiType() {
  return Isolate::Current()->object_store()->smi_type();
}


RawType* Type::MintType() {
  return Isolate::Current()->object_store()->mint_type();
}


RawType* Type::Double() {
  return Isolate::Current()->object_store()->double_type();
}


RawType* Type::Float32x4() {
  return Isolate::Current()->object_store()->float32x4_type();
}


RawType* Type::Int32x4() {
  return Isolate::Current()->object_store()->int32x4_type();
}


RawType* Type::Number() {
  return Isolate::Current()->object_store()->number_type();
}


RawType* Type::StringType() {
  return Isolate::Current()->object_store()->string_type();
}


RawType* Type::ArrayType() {
  return Isolate::Current()->object_store()->array_type();
}


RawType* Type::Function() {
  return Isolate::Current()->object_store()->function_type();
}


RawType* Type::NewNonParameterizedType(const Class& type_class) {
  ASSERT(type_class.NumTypeArguments() == 0);
  const TypeArguments& no_type_arguments = TypeArguments::Handle();
  Type& type = Type::Handle();
  type ^= Type::New(Object::Handle(type_class.raw()),
                    no_type_arguments,
                    Scanner::kDummyTokenIndex);
  type.SetIsFinalized();
  type ^= type.Canonicalize();
  return type.raw();
}


void Type::SetIsFinalized() const {
  ASSERT(!IsFinalized());
  if (IsInstantiated()) {
    set_type_state(RawType::kFinalizedInstantiated);
  } else {
    set_type_state(RawType::kFinalizedUninstantiated);
  }
}


void Type::ResetIsFinalized() const {
  ASSERT(IsFinalized());
  set_type_state(RawType::kBeingFinalized);
  SetIsFinalized();
}


void Type::set_is_being_finalized() const {
  ASSERT(!IsFinalized() && !IsBeingFinalized());
  set_type_state(RawType::kBeingFinalized);
}


bool Type::IsMalformed() const {
  return raw_ptr()->malformed_error_ != Error::null();
}


bool Type::IsMalboundedWithError(Error* bound_error) const {
  if (!FLAG_enable_type_checks && !FLAG_error_on_bad_type) {
    return false;
  }
  ASSERT(IsResolved());
  ASSERT(!IsMalformed());  // Must be checked first.
  if (arguments() == AbstractTypeArguments::null()) {
    return false;
  }
  const AbstractTypeArguments& type_arguments =
      AbstractTypeArguments::Handle(arguments());
  const intptr_t num_type_args = type_arguments.Length();
  AbstractType& type_arg = AbstractType::Handle();
  for (intptr_t i = 0; i < num_type_args; i++) {
    type_arg = type_arguments.TypeAt(i);
    ASSERT(!type_arg.IsNull());
    if (type_arg.IsMalboundedWithError(bound_error)) {
      return true;
    }
  }
  return false;
}


void Type::set_malformed_error(const Error& value) const {
  StorePointer(&raw_ptr()->malformed_error_, value.raw());
}


RawError* Type::malformed_error() const {
  ASSERT(IsMalformed());
  return raw_ptr()->malformed_error_;
}


bool Type::IsResolved() const {
  if (IsFinalized()) {
    return true;
  }
  if (!HasResolvedTypeClass()) {
    return false;
  }
  const AbstractTypeArguments& args =
      AbstractTypeArguments::Handle(arguments());
  return args.IsNull() || args.IsResolved();
}


bool Type::HasResolvedTypeClass() const {
  const Object& type_class = Object::Handle(raw_ptr()->type_class_);
  return !type_class.IsNull() && type_class.IsClass();
}


RawClass* Type::type_class() const {
  ASSERT(HasResolvedTypeClass());
#ifdef DEBUG
  Class& type_class = Class::Handle();
  type_class ^= raw_ptr()->type_class_;
  return type_class.raw();
#else
  return reinterpret_cast<RawClass*>(raw_ptr()->type_class_);
#endif
}


RawUnresolvedClass* Type::unresolved_class() const {
  ASSERT(!HasResolvedTypeClass());
#ifdef DEBUG
  UnresolvedClass& unresolved_class = UnresolvedClass::Handle();
  unresolved_class ^= raw_ptr()->type_class_;
  ASSERT(!unresolved_class.IsNull());
  return unresolved_class.raw();
#else
  ASSERT(!Object::Handle(raw_ptr()->type_class_).IsNull());
  ASSERT(Object::Handle(raw_ptr()->type_class_).IsUnresolvedClass());
  return reinterpret_cast<RawUnresolvedClass*>(raw_ptr()->type_class_);
#endif
}


RawString* Type::TypeClassName() const {
  if (HasResolvedTypeClass()) {
    const Class& cls = Class::Handle(type_class());
    return cls.Name();
  } else {
    const UnresolvedClass& cls = UnresolvedClass::Handle(unresolved_class());
    return cls.Name();
  }
}


RawAbstractTypeArguments* Type::arguments() const {
  return raw_ptr()->arguments_;
}


bool Type::IsInstantiated() const {
  if (raw_ptr()->type_state_ == RawType::kFinalizedInstantiated) {
    return true;
  }
  if (raw_ptr()->type_state_ == RawType::kFinalizedUninstantiated) {
    return false;
  }
  const AbstractTypeArguments& args =
      AbstractTypeArguments::Handle(arguments());
  return args.IsNull() || args.IsInstantiated();
}


RawAbstractType* Type::InstantiateFrom(
    const AbstractTypeArguments& instantiator_type_arguments,
    Error* bound_error) const {
  ASSERT(IsResolved());
  ASSERT(!IsInstantiated());
  // Return the uninstantiated type unchanged if malformed. No copy needed.
  if (IsMalformed()) {
    return raw();
  }
  AbstractTypeArguments& type_arguments =
      AbstractTypeArguments::Handle(arguments());
  type_arguments = type_arguments.InstantiateFrom(instantiator_type_arguments,
                                                  bound_error);
  // Note that the type class has to be resolved at this time, but not
  // necessarily finalized yet. We may be checking bounds at compile time.
  const Class& cls = Class::Handle(type_class());
  // This uninstantiated type is not modified, as it can be instantiated
  // with different instantiators.
  Type& instantiated_type = Type::Handle(
      Type::New(cls, type_arguments, token_pos()));
  ASSERT(type_arguments.IsNull() ||
         (type_arguments.Length() == cls.NumTypeArguments()));
  instantiated_type.SetIsFinalized();
  return instantiated_type.raw();
}


bool Type::Equals(const Instance& other) const {
  if (raw() == other.raw()) {
    return true;
  }
  if (!other.IsType()) {
    return false;
  }
  const Type& other_type = Type::Cast(other);
  ASSERT(IsResolved() && other_type.IsResolved());
  if (IsMalformed() || other_type.IsMalformed()) {
    return false;
  }
  if (type_class() != other_type.type_class()) {
    return false;
  }
  if (!IsFinalized() || !other_type.IsFinalized()) {
    return false;
  }
  return AbstractTypeArguments::AreEqual(
      AbstractTypeArguments::Handle(arguments()),
      AbstractTypeArguments::Handle(other_type.arguments()));
}


RawAbstractType* Type::CloneUnfinalized() const {
  ASSERT(IsResolved());
  if (IsFinalized()) {
    return raw();
  }
  ASSERT(!IsMalformed());  // Malformed types are finalized.
  ASSERT(!IsBeingFinalized());  // Cloning must occur prior to finalization.
  AbstractTypeArguments& type_args = AbstractTypeArguments::Handle(arguments());
  type_args = type_args.CloneUnfinalized();
  const Class& type_cls = Class::Handle(type_class());
  return Type::New(type_cls, type_args, token_pos());
}


RawAbstractType* Type::Canonicalize() const {
  ASSERT(IsFinalized());
  if (IsCanonical() || IsMalformed()) {
    ASSERT(IsMalformed() || AbstractTypeArguments::Handle(arguments()).IsOld());
    return this->raw();
  }
  const Class& cls = Class::Handle(type_class());
  Array& canonical_types = Array::Handle(cls.canonical_types());
  if (canonical_types.IsNull()) {
    // Types defined in the VM isolate are canonicalized via the object store.
    return this->raw();
  }
  const intptr_t canonical_types_len = canonical_types.Length();
  // Linear search to see whether this type is already present in the
  // list of canonicalized types.
  // TODO(asiva): Try to re-factor this lookup code to make sharing
  // easy between the 4 versions of this loop.
  Type& type = Type::Handle();
  intptr_t index = 0;
  while (index < canonical_types_len) {
    type ^= canonical_types.At(index);
    if (type.IsNull()) {
      break;
    }
    ASSERT(type.IsFinalized());
    if (this->Equals(type)) {
      return type.raw();
    }
    index++;
  }
  // Canonicalize the type arguments.
  AbstractTypeArguments& type_args = AbstractTypeArguments::Handle(arguments());
  type_args = type_args.Canonicalize();
  set_arguments(type_args);
  // The type needs to be added to the list. Grow the list if it is full.
  if (index == canonical_types_len) {
    const intptr_t kLengthIncrement = 2;  // Raw and parameterized.
    const intptr_t new_length = canonical_types.Length() + kLengthIncrement;
    const Array& new_canonical_types =
        Array::Handle(Array::Grow(canonical_types, new_length, Heap::kOld));
    cls.set_canonical_types(new_canonical_types);
    new_canonical_types.SetAt(index, *this);
  } else {
    canonical_types.SetAt(index, *this);
  }
#ifdef DEBUG
  if ((index == 0) && cls.IsCanonicalSignatureClass()) {
    // Verify that the first canonical type is the signature type by checking
    // that the type argument vector of the canonical type ends with the
    // uninstantiated type parameters of the signature class.
    // The signature type is finalized during class finalization, before the
    // optimizer may canonicalize instantiated function types of the same
    // signature class.
    // Although the signature class extends class Instance, the type arguments
    // of the super class of the owner class of its signature function will be
    // prepended to the type argument vector during class finalization.
    const TypeArguments& type_params =
      TypeArguments::Handle(cls.type_parameters());
    const intptr_t num_type_params = cls.NumTypeParameters();
    const intptr_t num_type_args = cls.NumTypeArguments();
    TypeParameter& type_arg = TypeParameter::Handle();
    TypeParameter& type_param = TypeParameter::Handle();
    for (intptr_t i = 0; i < num_type_params; i++) {
      type_arg ^= type_args.TypeAt(num_type_args - num_type_params + i);
      type_param ^= type_params.TypeAt(i);
      ASSERT(type_arg.Equals(type_param));
    }
  }
#endif
  ASSERT(IsOld());
  SetCanonical();
  return this->raw();
}


intptr_t Type::Hash() const {
  ASSERT(IsFinalized());
  uword result = 1;
  if (IsMalformed()) return result;
  result += Class::Handle(type_class()).id();
  result += AbstractTypeArguments::Handle(arguments()).Hash();
  return FinalizeHash(result);
}


void Type::set_type_class(const Object& value) const {
  ASSERT(!value.IsNull() && (value.IsClass() || value.IsUnresolvedClass()));
  StorePointer(&raw_ptr()->type_class_, value.raw());
}


void Type::set_arguments(const AbstractTypeArguments& value) const {
  StorePointer(&raw_ptr()->arguments_, value.raw());
}


RawType* Type::New(Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->type_class() != Class::null());
  RawObject* raw = Object::Allocate(Type::kClassId,
                                    Type::InstanceSize(),
                                    space);
  return reinterpret_cast<RawType*>(raw);
}


RawType* Type::New(const Object& clazz,
                   const AbstractTypeArguments& arguments,
                   intptr_t token_pos,
                   Heap::Space space) {
  const Type& result = Type::Handle(Type::New(space));
  result.set_type_class(clazz);
  result.set_arguments(arguments);
  result.set_token_pos(token_pos);
  result.raw_ptr()->type_state_ = RawType::kAllocated;
  return result.raw();
}


void Type::set_token_pos(intptr_t token_pos) const {
  ASSERT(token_pos >= 0);
  raw_ptr()->token_pos_ = token_pos;
}


void Type::set_type_state(int8_t state) const {
  ASSERT((state == RawType::kAllocated) ||
         (state == RawType::kBeingFinalized) ||
         (state == RawType::kFinalizedInstantiated) ||
         (state == RawType::kFinalizedUninstantiated));
  raw_ptr()->type_state_ = state;
}


const char* Type::ToCString() const {
  if (IsResolved()) {
    const AbstractTypeArguments& type_arguments =
        AbstractTypeArguments::Handle(arguments());
    const char* class_name;
    if (HasResolvedTypeClass()) {
      class_name = String::Handle(
          Class::Handle(type_class()).Name()).ToCString();
    } else {
      class_name = UnresolvedClass::Handle(unresolved_class()).ToCString();
    }
    if (type_arguments.IsNull()) {
      const char* format = "Type: class '%s'";
      intptr_t len = OS::SNPrint(NULL, 0, format, class_name) + 1;
      char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
      OS::SNPrint(chars, len, format, class_name);
      return chars;
    } else {
      const char* format = "Type: class '%s', args:[%s]";
      const char* args_cstr =
          AbstractTypeArguments::Handle(arguments()).ToCString();
      intptr_t len = OS::SNPrint(NULL, 0, format, class_name, args_cstr) + 1;
      char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
      OS::SNPrint(chars, len, format, class_name, args_cstr);
      return chars;
    }
  } else {
    return "Unresolved Type";
  }
}


void Type::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


void TypeParameter::set_is_finalized() const {
  ASSERT(!IsFinalized());
  set_type_state(RawTypeParameter::kFinalizedUninstantiated);
}


bool TypeParameter::Equals(const Instance& other) const {
  if (raw() == other.raw()) {
    return true;
  }
  if (!other.IsTypeParameter()) {
    return false;
  }
  const TypeParameter& other_type_param = TypeParameter::Cast(other);
  if (parameterized_class() != other_type_param.parameterized_class()) {
    return false;
  }
  if (IsFinalized() == other_type_param.IsFinalized()) {
    return index() == other_type_param.index();
  }
  return name() == other_type_param.name();
}


void TypeParameter::set_parameterized_class(const Class& value) const {
  // Set value may be null.
  StorePointer(&raw_ptr()->parameterized_class_, value.raw());
}


void TypeParameter::set_index(intptr_t value) const {
  ASSERT(value >= 0);
  raw_ptr()->index_ = value;
}


void TypeParameter::set_name(const String& value) const {
  ASSERT(value.IsSymbol());
  StorePointer(&raw_ptr()->name_, value.raw());
}


void TypeParameter::set_bound(const AbstractType& value) const {
  StorePointer(&raw_ptr()->bound_, value.raw());
}


RawAbstractType* TypeParameter::InstantiateFrom(
    const AbstractTypeArguments& instantiator_type_arguments,
    Error* bound_error) const {
  ASSERT(IsFinalized());
  if (instantiator_type_arguments.IsNull()) {
    return Type::DynamicType();
  }
  // Bound checks may appear in the instantiator type arguments, as is the case
  // with a pair of type parameters of the same class referring to each other
  // via their bounds.
  AbstractType& type_arg = AbstractType::Handle(
      instantiator_type_arguments.TypeAt(index()));
  if (type_arg.IsBoundedType()) {
    const BoundedType& bounded_type = BoundedType::Cast(type_arg);
    // Bounds checking of a type is postponed to run time if the type is still
    // uninstantiated at compile time, or if the bound and the type are mutually
    // recursive. In the latter case, the type may already be instantiated.
    if (!bounded_type.IsInstantiated()) {
      ASSERT(AbstractType::Handle(bounded_type.bound()).IsInstantiated());
      type_arg = bounded_type.InstantiateFrom(AbstractTypeArguments::Handle(),
                                              bound_error);
    }
  }
  return type_arg.raw();
}


bool TypeParameter::CheckBound(const AbstractType& bounded_type,
                               const AbstractType& upper_bound,
                               Error* bound_error) const {
  ASSERT((bound_error == NULL) || bound_error->IsNull());
  ASSERT(bounded_type.IsFinalized());
  ASSERT(upper_bound.IsFinalized());
  ASSERT(!bounded_type.IsMalformed());
  if (bounded_type.IsSubtypeOf(upper_bound, bound_error)) {
    return true;
  }
  if ((bound_error != NULL) && bound_error->IsNull()) {
    // Report the bound error.
    const String& bounded_type_name = String::Handle(
        bounded_type.UserVisibleName());
    const String& upper_bound_name = String::Handle(
        upper_bound.UserVisibleName());
    const AbstractType& declared_bound = AbstractType::Handle(bound());
    const String& declared_bound_name = String::Handle(
        declared_bound.UserVisibleName());
    const String& type_param_name = String::Handle(UserVisibleName());
    const Class& cls = Class::Handle(parameterized_class());
    const String& class_name = String::Handle(cls.Name());
    const Script& script = Script::Handle(cls.script());
    // Since the bound may have been canonicalized, its token index is
    // meaningless, therefore use the token index of this type parameter.
    *bound_error = FormatError(
        *bound_error,
        script,
        token_pos(),
        "type parameter '%s' of class '%s' must extend bound '%s', "
        "but type argument '%s' is not a subtype of '%s'\n",
        type_param_name.ToCString(),
        class_name.ToCString(),
        declared_bound_name.ToCString(),
        bounded_type_name.ToCString(),
        upper_bound_name.ToCString());
  }
  return false;
}


RawAbstractType* TypeParameter::CloneUnfinalized() const {
  if (IsFinalized()) {
    return raw();
  }
  // No need to clone bound, as it is not part of the finalization state.
  return TypeParameter::New(Class::Handle(parameterized_class()),
                            index(),
                            String::Handle(name()),
                            AbstractType::Handle(bound()),
                            token_pos());
}


intptr_t TypeParameter::Hash() const {
  ASSERT(IsFinalized());
  uword result = 0;
  result += Class::Handle(parameterized_class()).id();
  // Do not include the hash of the bound, which could lead to cycles.
  result <<= index();
  return FinalizeHash(result);
}


RawTypeParameter* TypeParameter::New() {
  ASSERT(Isolate::Current()->object_store()->type_parameter_class() !=
         Class::null());
  RawObject* raw = Object::Allocate(TypeParameter::kClassId,
                                    TypeParameter::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawTypeParameter*>(raw);
}


RawTypeParameter* TypeParameter::New(const Class& parameterized_class,
                                     intptr_t index,
                                     const String& name,
                                     const AbstractType& bound,
                                     intptr_t token_pos) {
  const TypeParameter& result = TypeParameter::Handle(TypeParameter::New());
  result.set_parameterized_class(parameterized_class);
  result.set_index(index);
  result.set_name(name);
  result.set_bound(bound);
  result.set_token_pos(token_pos);
  result.raw_ptr()->type_state_ = RawTypeParameter::kAllocated;
  return result.raw();
}


void TypeParameter::set_token_pos(intptr_t token_pos) const {
  ASSERT(token_pos >= 0);
  raw_ptr()->token_pos_ = token_pos;
}


void TypeParameter::set_type_state(int8_t state) const {
  ASSERT((state == RawTypeParameter::kAllocated) ||
         (state == RawTypeParameter::kBeingFinalized) ||
         (state == RawTypeParameter::kFinalizedUninstantiated));
  raw_ptr()->type_state_ = state;
}


const char* TypeParameter::ToCString() const {
  const char* format = "TypeParameter: name %s; index: %d; class: %s";
  const char* name_cstr = String::Handle(Name()).ToCString();
  const Class& cls = Class::Handle(parameterized_class());
  const char* cls_cstr =
      cls.IsNull() ? " null" : String::Handle(cls.Name()).ToCString();
  intptr_t len = OS::SNPrint(NULL, 0, format, name_cstr, index(), cls_cstr) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, name_cstr, index(), cls_cstr);
  return chars;
}


void TypeParameter::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


bool BoundedType::IsMalformed() const {
  return AbstractType::Handle(type()).IsMalformed();
}


bool BoundedType::IsMalboundedWithError(Error* bound_error) const {
  if (!FLAG_enable_type_checks && !FLAG_error_on_bad_type) {
    return false;
  }
  const AbstractType& upper_bound = AbstractType::Handle(bound());
  if (upper_bound.IsMalformed()) {
    if (bound_error != NULL) {
      *bound_error = upper_bound.malformed_error();
      ASSERT(!bound_error->IsNull());
    }
    return true;
  }
  return false;
}


RawError* BoundedType::malformed_error() const {
  return AbstractType::Handle(type()).malformed_error();
}


bool BoundedType::Equals(const Instance& other) const {
  // BoundedType are not canonicalized, because their bound may get finalized
  // after the BoundedType is created and initialized.
  if (raw() == other.raw()) {
    return true;
  }
  if (!other.IsBoundedType()) {
    return false;
  }
  const BoundedType& other_bounded = BoundedType::Cast(other);
  if (type_parameter() != other_bounded.type_parameter()) {
    // Not a structural compare.
    // Note that a deep comparison of bounds could lead to cycles.
    return false;
  }
  const AbstractType& this_type = AbstractType::Handle(type());
  const AbstractType& other_type = AbstractType::Handle(other_bounded.type());
  if (!this_type.Equals(other_type)) {
    return false;
  }
  const AbstractType& this_bound = AbstractType::Handle(bound());
  const AbstractType& other_bound = AbstractType::Handle(other_bounded.bound());
  return this_bound.IsFinalized() &&
         other_bound.IsFinalized() &&
         this_bound.Equals(other_bound);
}


void BoundedType::set_type(const AbstractType& value) const {
  ASSERT(value.IsFinalized());
  ASSERT(!value.IsMalformed());
  StorePointer(&raw_ptr()->type_, value.raw());
}


void BoundedType::set_bound(const AbstractType& value) const {
  // The bound may still be unfinalized because of legal cycles.
  // It must be finalized before it is checked at run time, though.
  StorePointer(&raw_ptr()->bound_, value.raw());
}


void BoundedType::set_type_parameter(const TypeParameter& value) const {
  // A null type parameter is set when marking a type malformed because of a
  // bound error at compile time.
  ASSERT(value.IsNull() || value.IsFinalized());
  StorePointer(&raw_ptr()->type_parameter_, value.raw());
}


void BoundedType::set_is_being_checked(bool value) const {
  raw_ptr()->is_being_checked_ = value;
}


RawAbstractType* BoundedType::InstantiateFrom(
    const AbstractTypeArguments& instantiator_type_arguments,
    Error* bound_error) const {
  ASSERT(IsFinalized());
  AbstractType& bounded_type = AbstractType::Handle(type());
  if (!bounded_type.IsInstantiated()) {
    bounded_type = bounded_type.InstantiateFrom(instantiator_type_arguments,
                                                bound_error);
  }
  if (FLAG_enable_type_checks &&
      bound_error->IsNull() &&
      !is_being_checked()) {
    // Avoid endless recursion while checking and instantiating bound.
    set_is_being_checked(true);
    AbstractType& upper_bound = AbstractType::Handle(bound());
    ASSERT(!upper_bound.IsObjectType() && !upper_bound.IsDynamicType());
    const TypeParameter& type_param = TypeParameter::Handle(type_parameter());
    if (!upper_bound.IsInstantiated()) {
      upper_bound = upper_bound.InstantiateFrom(instantiator_type_arguments,
                                                bound_error);
    }
    if (bound_error->IsNull()) {
      type_param.CheckBound(bounded_type, upper_bound, bound_error);
    }
    set_is_being_checked(false);
  }
  return bounded_type.raw();
}


RawAbstractType* BoundedType::CloneUnfinalized() const {
  if (IsFinalized()) {
    return raw();
  }
  AbstractType& bounded_type = AbstractType::Handle(type());

  bounded_type = bounded_type.CloneUnfinalized();
  // No need to clone bound or type parameter, as they are not part of the
  // finalization state of this bounded type.
  return BoundedType::New(bounded_type,
                          AbstractType::Handle(bound()),
                          TypeParameter::Handle(type_parameter()));
}


intptr_t BoundedType::Hash() const {
  uword result = 0;
  result += AbstractType::Handle(type()).Hash();
  // Do not include the hash of the bound, which could lead to cycles.
  TypeParameter& type_param = TypeParameter::Handle(type_parameter());
  if (!type_param.IsNull()) {
    result += type_param.Hash();
  }
return FinalizeHash(result);
}


RawBoundedType* BoundedType::New() {
  ASSERT(Isolate::Current()->object_store()->bounded_type_class() !=
         Class::null());
  RawObject* raw = Object::Allocate(BoundedType::kClassId,
                                    BoundedType::InstanceSize(),
                                    Heap::kOld);
  return reinterpret_cast<RawBoundedType*>(raw);
}


RawBoundedType* BoundedType::New(const AbstractType& type,
                                 const AbstractType& bound,
                                 const TypeParameter& type_parameter) {
  const BoundedType& result = BoundedType::Handle(BoundedType::New());
  result.set_type(type);
  result.set_bound(bound);
  result.set_type_parameter(type_parameter);
  result.set_is_being_checked(false);
  return result.raw();
}


const char* BoundedType::ToCString() const {
  const char* format = "BoundedType: type %s; bound: %s; class: %s";
  const char* type_cstr = String::Handle(AbstractType::Handle(
      type()).Name()).ToCString();
  const char* bound_cstr = String::Handle(AbstractType::Handle(
      bound()).Name()).ToCString();
  const Class& cls = Class::Handle(TypeParameter::Handle(
      type_parameter()).parameterized_class());
  const char* cls_cstr =
      cls.IsNull() ? " null" : String::Handle(cls.Name()).ToCString();
  intptr_t len = OS::SNPrint(
      NULL, 0, format, type_cstr, bound_cstr, cls_cstr) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, type_cstr, bound_cstr, cls_cstr);
  return chars;
}


void BoundedType::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


intptr_t MixinAppType::token_pos() const {
  return AbstractType::Handle(MixinTypeAt(0)).token_pos();
}


intptr_t MixinAppType::Depth() const {
  return Array::Handle(mixin_types()).Length();
}


RawString* MixinAppType::Name() const {
  return String::New("MixinAppType");
}


const char* MixinAppType::ToCString() const {
  const char* format = "MixinAppType: super type: %s; first mixin type: %s";
  const char* super_type_cstr = String::Handle(AbstractType::Handle(
      super_type()).Name()).ToCString();
  const char* first_mixin_type_cstr = String::Handle(AbstractType::Handle(
      MixinTypeAt(0)).Name()).ToCString();
  intptr_t len = OS::SNPrint(
      NULL, 0, format, super_type_cstr, first_mixin_type_cstr) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, super_type_cstr, first_mixin_type_cstr);
  return chars;
}


void MixinAppType::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawAbstractType* MixinAppType::MixinTypeAt(intptr_t depth) const {
  return AbstractType::RawCast(Array::Handle(mixin_types()).At(depth));
}


void MixinAppType::set_super_type(const AbstractType& value) const {
  StorePointer(&raw_ptr()->super_type_, value.raw());
}


void MixinAppType::set_mixin_types(const Array& value) const {
  StorePointer(&raw_ptr()->mixin_types_, value.raw());
}


RawMixinAppType* MixinAppType::New() {
  ASSERT(Isolate::Current()->object_store()->mixin_app_type_class() !=
         Class::null());
  // MixinAppType objects do not survive finalization, so allocate
  // on new heap.
  RawObject* raw = Object::Allocate(MixinAppType::kClassId,
                                    MixinAppType::InstanceSize(),
                                    Heap::kNew);
  return reinterpret_cast<RawMixinAppType*>(raw);
}


RawMixinAppType* MixinAppType::New(const AbstractType& super_type,
                                   const Array& mixin_types) {
  const MixinAppType& result = MixinAppType::Handle(MixinAppType::New());
  result.set_super_type(super_type);
  result.set_mixin_types(mixin_types);
  return result.raw();
}


const char* Number::ToCString() const {
  // Number is an interface. No instances of Number should exist.
  UNREACHABLE();
  return "Number";
}


void Number::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


const char* Integer::ToCString() const {
  // Integer is an interface. No instances of Integer should exist.
  UNREACHABLE();
  return "Integer";
}


void Integer::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


// Throw JavascriptIntegerOverflow exception.
static void ThrowJavascriptIntegerOverflow(const Integer& i) {
  const Array& exc_args = Array::Handle(Array::New(1));
  const String& i_str = String::Handle(String::New(i.ToCString()));
  exc_args.SetAt(0, i_str);
  Exceptions::ThrowByType(Exceptions::kJavascriptIntegerOverflowError,
      exc_args);
}


RawInteger* Integer::New(const String& str, Heap::Space space) {
  // We are not supposed to have integers represented as two byte strings.
  ASSERT(str.IsOneByteString());
  int64_t value;
  if (!OS::StringToInt64(str.ToCString(), &value)) {
    const Bigint& big = Bigint::Handle(Bigint::New(str, space));
    ASSERT(!BigintOperations::FitsIntoSmi(big));
    ASSERT(!BigintOperations::FitsIntoInt64(big));
    if (FLAG_throw_on_javascript_int_overflow) {
      ThrowJavascriptIntegerOverflow(big);
    }
    return big.raw();
  }
  return Integer::New(value, space);
}


// This is called from LiteralToken::New() in the parser, so we can't
// raise an exception for javascript overflow here. Instead we do it in
// Parser::CurrentIntegerLiteral(), which is the point in the parser where
// integer literals escape, so we can call Parser::ErrorMsg().
RawInteger* Integer::NewCanonical(const String& str) {
  // We are not supposed to have integers represented as two byte strings.
  ASSERT(str.IsOneByteString());
  int64_t value;
  if (!OS::StringToInt64(str.ToCString(), &value)) {
    const Bigint& big = Bigint::Handle(Bigint::NewCanonical(str));
    ASSERT(!BigintOperations::FitsIntoSmi(big));
    ASSERT(!BigintOperations::FitsIntoInt64(big));
    return big.raw();
  }
  if ((value <= Smi::kMaxValue) && (value >= Smi::kMinValue)) {
    return Smi::New(value);
  }
  return Mint::NewCanonical(value);
}


// dart2js represents integers as double precision floats, which can represent
// anything in the range -2^53 ... 2^53.
static bool IsJavascriptInt(int64_t value) {
  return ((-0x20000000000000LL <= value) && (value <= 0x20000000000000LL));
}


RawInteger* Integer::New(int64_t value, Heap::Space space, const bool silent) {
  if ((value <= Smi::kMaxValue) && (value >= Smi::kMinValue)) {
    return Smi::New(value);
  }
  if (!silent &&
      FLAG_throw_on_javascript_int_overflow &&
      !IsJavascriptInt(value)) {
    const Integer &i = Integer::Handle(Mint::New(value));
    ThrowJavascriptIntegerOverflow(i);
  }
  return Mint::New(value, space);
}


RawInteger* Integer::NewFromUint64(uint64_t value, Heap::Space space) {
  if (value > static_cast<uint64_t>(Mint::kMaxValue)) {
    if (FLAG_throw_on_javascript_int_overflow) {
      const Integer &i =
          Integer::Handle(BigintOperations::NewFromUint64(value));
      ThrowJavascriptIntegerOverflow(i);
    }
    return BigintOperations::NewFromUint64(value);
  } else {
    return Integer::New(value);
  }
}


double Integer::AsDoubleValue() const {
  UNIMPLEMENTED();
  return 0.0;
}


int64_t Integer::AsInt64Value() const {
  UNIMPLEMENTED();
  return 0;
}


int Integer::CompareWith(const Integer& other) const {
  UNIMPLEMENTED();
  return 0;
}


// Returns true if the signed Integer does not fit into a
// Javascript integer.
bool Integer::CheckJavascriptIntegerOverflow() const {
  // Always overflow if the value doesn't fit into an int64_t.
  int64_t value = 1ULL << 63;
  if (IsSmi()) {
    value = AsInt64Value();
  } else if (IsMint()) {
    Mint& mint = Mint::Handle();
    mint ^= raw();
    value = mint.value();
  } else {
    ASSERT(IsBigint());
    Bigint& big_value = Bigint::Handle();
    big_value ^= raw();
    if (BigintOperations::FitsIntoInt64(big_value)) {
      value = BigintOperations::ToInt64(big_value);
    }
  }
  return !IsJavascriptInt(value);
}


RawInteger* Integer::AsValidInteger() const {
  if (FLAG_throw_on_javascript_int_overflow &&
      CheckJavascriptIntegerOverflow()) {
    ThrowJavascriptIntegerOverflow(*this);
  }
  if (IsSmi()) return raw();
  if (IsMint()) {
    Mint& mint = Mint::Handle();
    mint ^= raw();
    if (Smi::IsValid64(mint.value())) {
      return Smi::New(mint.value());
    } else {
      return raw();
    }
  }
  ASSERT(IsBigint());
  Bigint& big_value = Bigint::Handle();
  big_value ^= raw();
  if (BigintOperations::FitsIntoSmi(big_value)) {
    return BigintOperations::ToSmi(big_value);
  } else if (BigintOperations::FitsIntoInt64(big_value)) {
    return Mint::New(BigintOperations::ToInt64(big_value));
  } else {
    return big_value.raw();
  }
}


RawInteger* Integer::ArithmeticOp(Token::Kind operation,
                                  const Integer& other) const {
  // In 32-bit mode, the result of any operation between two Smis will fit in a
  // 32-bit signed result, except the product of two Smis, which will be 64-bit.
  // In 64-bit mode, the result of any operation between two Smis will fit in a
  // 64-bit signed result, except the product of two Smis (unless the Smis are
  // 32-bit or less).
  if (IsSmi() && other.IsSmi()) {
    const intptr_t left_value = Smi::Value(Smi::RawCast(raw()));
    const intptr_t right_value = Smi::Value(Smi::RawCast(other.raw()));
    switch (operation) {
      case Token::kADD:
        return Integer::New(left_value + right_value);
      case Token::kSUB:
        return Integer::New(left_value - right_value);
      case Token::kMUL: {
        if (Smi::kBits < 32) {
          // In 32-bit mode, the product of two Smis fits in a 64-bit result.
          return Integer::New(static_cast<int64_t>(left_value) *
                              static_cast<int64_t>(right_value));
        } else {
          // In 64-bit mode, the product of two 32-bit signed integers fits in a
          // 64-bit result.
          ASSERT(sizeof(intptr_t) == sizeof(int64_t));
          if (Utils::IsInt(32, left_value) && Utils::IsInt(32, right_value)) {
            return Integer::New(left_value * right_value);
          }
        }
        // Perform a Bigint multiplication below.
        break;
      }
      case Token::kTRUNCDIV:
        return Integer::New(left_value / right_value);
      case Token::kMOD: {
        const intptr_t remainder = left_value % right_value;
        if (remainder < 0) {
          if (right_value < 0) {
            return Integer::New(remainder - right_value);
          } else {
            return Integer::New(remainder + right_value);
          }
        }
        return Integer::New(remainder);
      }
      default:
        UNIMPLEMENTED();
    }
  }
  // In 32-bit mode, the result of any operation between two 63-bit signed
  // integers (or 32-bit for multiplication) will fit in a 64-bit signed result.
  // In 64-bit mode, 63-bit signed integers are Smis, already processed above.
  if ((Smi::kBits < 32) && !IsBigint() && !other.IsBigint()) {
    const int64_t left_value = AsInt64Value();
    if (Utils::IsInt(63, left_value)) {
      const int64_t right_value = other.AsInt64Value();
      if (Utils::IsInt(63, right_value)) {
        switch (operation) {
        case Token::kADD:
          return Integer::New(left_value + right_value);
        case Token::kSUB:
          return Integer::New(left_value - right_value);
        case Token::kMUL: {
          if (Utils::IsInt(32, left_value) && Utils::IsInt(32, right_value)) {
            return Integer::New(left_value * right_value);
          }
          // Perform a Bigint multiplication below.
          break;
        }
        case Token::kTRUNCDIV:
          return Integer::New(left_value / right_value);
        case Token::kMOD: {
          const int64_t remainder = left_value % right_value;
          if (remainder < 0) {
            if (right_value < 0) {
              return Integer::New(remainder - right_value);
            } else {
              return Integer::New(remainder + right_value);
            }
          }
          return Integer::New(remainder);
        }
        default:
          UNIMPLEMENTED();
        }
      }
    }
  }
  const Bigint& left_big = Bigint::Handle(AsBigint());
  const Bigint& right_big = Bigint::Handle(other.AsBigint());
  const Bigint& result =
      Bigint::Handle(left_big.BigArithmeticOp(operation, right_big));
  return Integer::Handle(result.AsValidInteger()).raw();
}


static bool Are64bitOperands(const Integer& op1, const Integer& op2) {
  return !op1.IsBigint() && !op2.IsBigint();
}


RawInteger* Integer::BitOp(Token::Kind kind, const Integer& other) const {
  if (IsSmi() && other.IsSmi()) {
    intptr_t op1_value = Smi::Value(Smi::RawCast(raw()));
    intptr_t op2_value = Smi::Value(Smi::RawCast(other.raw()));
    intptr_t result = 0;
    switch (kind) {
      case Token::kBIT_AND:
        result = op1_value & op2_value;
        break;
      case Token::kBIT_OR:
        result = op1_value | op2_value;
        break;
      case Token::kBIT_XOR:
        result = op1_value ^ op2_value;
        break;
      default:
        UNIMPLEMENTED();
    }
    ASSERT(Smi::IsValid(result));
    return Smi::New(result);
  } else if (Are64bitOperands(*this, other)) {
    int64_t a = AsInt64Value();
    int64_t b = other.AsInt64Value();
    switch (kind) {
      case Token::kBIT_AND:
        return Integer::New(a & b);
      case Token::kBIT_OR:
        return Integer::New(a | b);
      case Token::kBIT_XOR:
        return Integer::New(a ^ b);
      default:
        UNIMPLEMENTED();
    }
  } else {
    Bigint& op1 = Bigint::Handle(AsBigint());
    Bigint& op2 = Bigint::Handle(other.AsBigint());
    switch (kind) {
      case Token::kBIT_AND:
        return BigintOperations::BitAnd(op1, op2);
      case Token::kBIT_OR:
        return BigintOperations::BitOr(op1, op2);
      case Token::kBIT_XOR:
        return BigintOperations::BitXor(op1, op2);
      default:
        UNIMPLEMENTED();
    }
  }
  return Integer::null();
}


// TODO(srdjan): Clarify handling of negative right operand in a shift op.
RawInteger* Smi::ShiftOp(Token::Kind kind,
                         const Smi& other,
                         const bool silent) const {
  intptr_t result = 0;
  const intptr_t left_value = Value();
  const intptr_t right_value = other.Value();
  ASSERT(right_value >= 0);
  switch (kind) {
    case Token::kSHL: {
      if ((left_value == 0) || (right_value == 0)) {
        return raw();
      }
      { // Check for overflow.
        int cnt = Utils::HighestBit(left_value);
        if ((cnt + right_value) >= Smi::kBits) {
          if ((cnt + right_value) >= Mint::kBits) {
            return BigintOperations::ShiftLeft(
                Bigint::Handle(BigintOperations::NewFromSmi(*this)),
                               right_value);
          } else {
            int64_t left_64 = left_value;
            return Integer::New(left_64 << right_value, Heap::kNew, silent);
          }
        }
      }
      result = left_value << right_value;
      break;
    }
    case Token::kSHR: {
      const intptr_t shift_amount =
          (right_value >= kBitsPerWord) ? (kBitsPerWord - 1) : right_value;
      result = left_value >> shift_amount;
      break;
    }
    default:
      UNIMPLEMENTED();
  }
  ASSERT(Smi::IsValid(result));
  return Smi::New(result);
}


bool Smi::Equals(const Instance& other) const {
  if (other.IsNull() || !other.IsSmi()) {
    return false;
  }
  return (this->Value() == Smi::Cast(other).Value());
}


double Smi::AsDoubleValue() const {
  return static_cast<double>(this->Value());
}


int64_t Smi::AsInt64Value() const {
  return this->Value();
}


static bool FitsIntoSmi(const Integer& integer) {
  if (integer.IsSmi()) {
    return true;
  }
  if (integer.IsMint()) {
    int64_t mint_value = integer.AsInt64Value();
    return Smi::IsValid64(mint_value);
  }
  if (integer.IsBigint()) {
    return BigintOperations::FitsIntoSmi(Bigint::Cast(integer));
  }
  UNREACHABLE();
  return false;
}


int Smi::CompareWith(const Integer& other) const {
  if (other.IsSmi()) {
    const Smi& other_smi = Smi::Cast(other);
    if (this->Value() < other_smi.Value()) {
      return -1;
    } else if (this->Value() > other_smi.Value()) {
      return 1;
    } else {
      return 0;
    }
  }
  ASSERT(!FitsIntoSmi(other));
  if (other.IsMint() || other.IsBigint()) {
    if (this->IsNegative() == other.IsNegative()) {
      return this->IsNegative() ? 1 : -1;
    }
    return this->IsNegative() ? -1 : 1;
  }
  UNREACHABLE();
  return 0;
}


const char* Smi::ToCString() const {
  const char* kFormat = "%ld";
  // Calculate the size of the string.
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, Value()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, Value());
  return chars;
}


void Smi::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawClass* Smi::Class() {
  return Isolate::Current()->object_store()->smi_class();
}


void Mint::set_value(int64_t value) const {
  raw_ptr()->value_ = value;
}


RawMint* Mint::New(int64_t val, Heap::Space space) {
  // Do not allocate a Mint if Smi would do.
  ASSERT(!Smi::IsValid64(val));
  ASSERT(Isolate::Current()->object_store()->mint_class() != Class::null());
  Mint& result = Mint::Handle();
  {
    RawObject* raw = Object::Allocate(Mint::kClassId,
                                      Mint::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_value(val);
  return result.raw();
}


RawMint* Mint::NewCanonical(int64_t value) {
  // Do not allocate a Mint if Smi would do.
  ASSERT(!Smi::IsValid64(value));
  const Class& cls =
      Class::Handle(Isolate::Current()->object_store()->mint_class());
  const Array& constants = Array::Handle(cls.constants());
  const intptr_t constants_len = constants.Length();
  // Linear search to see whether this value is already present in the
  // list of canonicalized constants.
  Mint& canonical_value = Mint::Handle();
  intptr_t index = 0;
  while (index < constants_len) {
    canonical_value ^= constants.At(index);
    if (canonical_value.IsNull()) {
      break;
    }
    if (canonical_value.value() == value) {
      return canonical_value.raw();
    }
    index++;
  }
  // The value needs to be added to the constants list. Grow the list if
  // it is full.
  canonical_value = Mint::New(value, Heap::kOld);
  cls.InsertCanonicalConstant(index, canonical_value);
  canonical_value.SetCanonical();
  return canonical_value.raw();
}


bool Mint::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    // Both handles point to the same raw instance.
    return true;
  }
  if (!other.IsMint() || other.IsNull()) {
    return false;
  }
  return value() == Mint::Cast(other).value();
}


double Mint::AsDoubleValue() const {
  return static_cast<double>(this->value());
}


int64_t Mint::AsInt64Value() const {
  return this->value();
}


int Mint::CompareWith(const Integer& other) const {
  ASSERT(!FitsIntoSmi(*this));
  if (other.IsMint() || other.IsSmi()) {
    int64_t a = AsInt64Value();
    int64_t b = other.AsInt64Value();
    if (a < b) {
      return -1;
    } else if (a > b) {
      return 1;
    } else {
      return 0;
    }
  }
  if (other.IsBigint()) {
    ASSERT(!BigintOperations::FitsIntoInt64(Bigint::Cast(other)));
    if (this->IsNegative() == other.IsNegative()) {
      return this->IsNegative() ? 1 : -1;
    }
    return this->IsNegative() ? -1 : 1;
  }
  UNREACHABLE();
  return 0;
}


const char* Mint::ToCString() const {
  const char* kFormat = "%lld";
  // Calculate the size of the string.
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, value()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, value());
  return chars;
}


void Mint::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


void Double::set_value(double value) const {
  raw_ptr()->value_ = value;
}


bool Double::EqualsToDouble(double value) const {
  intptr_t value_offset = Double::value_offset();
  void* this_addr = reinterpret_cast<void*>(
      reinterpret_cast<uword>(this->raw_ptr()) + value_offset);
  void* other_addr = reinterpret_cast<void*>(&value);
  return (memcmp(this_addr, other_addr, sizeof(value)) == 0);
}


bool Double::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    return true;  // "===".
  }
  if (other.IsNull() || !other.IsDouble()) {
    return false;
  }
  return EqualsToDouble(Double::Cast(other).value());
}


RawDouble* Double::New(double d, Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->double_class() != Class::null());
  Double& result = Double::Handle();
  {
    RawObject* raw = Object::Allocate(Double::kClassId,
                                      Double::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_value(d);
  return result.raw();
}


RawDouble* Double::New(const String& str, Heap::Space space) {
  double double_value;
  if (!CStringToDouble(str.ToCString(), str.Length(), &double_value)) {
    return Double::Handle().raw();
  }
  return New(double_value, space);
}


RawDouble* Double::NewCanonical(double value) {
  const Class& cls =
      Class::Handle(Isolate::Current()->object_store()->double_class());
  const Array& constants = Array::Handle(cls.constants());
  const intptr_t constants_len = constants.Length();
  // Linear search to see whether this value is already present in the
  // list of canonicalized constants.
  Double& canonical_value = Double::Handle();
  intptr_t index = 0;
  while (index < constants_len) {
    canonical_value ^= constants.At(index);
    if (canonical_value.IsNull()) {
      break;
    }
    if (canonical_value.EqualsToDouble(value)) {
      return canonical_value.raw();
    }
    index++;
  }
  // The value needs to be added to the constants list. Grow the list if
  // it is full.
  canonical_value = Double::New(value, Heap::kOld);
  cls.InsertCanonicalConstant(index, canonical_value);
  canonical_value.SetCanonical();
  return canonical_value.raw();
}


RawDouble* Double::NewCanonical(const String& str) {
  double double_value;
  if (!CStringToDouble(str.ToCString(), str.Length(), &double_value)) {
    return Double::Handle().raw();
  }
  return NewCanonical(double_value);
}


const char* Double::ToCString() const {
  if (isnan(value())) {
    return "NaN";
  }
  if (isinf(value())) {
    return value() < 0 ? "-Infinity" : "Infinity";
  }
  const int kBufferSize = 128;
  char* buffer = Isolate::Current()->current_zone()->Alloc<char>(kBufferSize);
  buffer[kBufferSize - 1] = '\0';
  DoubleToCString(value(), buffer, kBufferSize);
  return buffer;
}


void Double::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawBigint* Integer::AsBigint() const {
  ASSERT(!IsNull());
  if (IsSmi()) {
    Smi& smi = Smi::Handle();
    smi ^= raw();
    return BigintOperations::NewFromSmi(smi);
  } else if (IsMint()) {
    Mint& mint = Mint::Handle();
    mint ^= raw();
    return BigintOperations::NewFromInt64(mint.value());
  } else {
    ASSERT(IsBigint());
    Bigint& big = Bigint::Handle();
    big ^= raw();
    ASSERT(!BigintOperations::FitsIntoSmi(big));
    return big.raw();
  }
}


RawBigint* Bigint::BigArithmeticOp(Token::Kind operation,
                                   const Bigint& other) const {
  switch (operation) {
    case Token::kADD:
      return BigintOperations::Add(*this, other);
    case Token::kSUB:
      return BigintOperations::Subtract(*this, other);
    case Token::kMUL:
      return BigintOperations::Multiply(*this, other);
    case Token::kTRUNCDIV:
      return BigintOperations::Divide(*this, other);
    case Token::kMOD:
      return BigintOperations::Modulo(*this, other);
    default:
      UNIMPLEMENTED();
      return Bigint::null();
  }
}


bool Bigint::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    // Both handles point to the same raw instance.
    return true;
  }

  if (!other.IsBigint() || other.IsNull()) {
    return false;
  }

  const Bigint& other_bgi = Bigint::Cast(other);

  if (this->IsNegative() != other_bgi.IsNegative()) {
    return false;
  }

  intptr_t len = this->Length();
  if (len != other_bgi.Length()) {
    return false;
  }

  for (intptr_t i = 0; i < len; i++) {
    if (this->GetChunkAt(i) != other_bgi.GetChunkAt(i)) {
      return false;
    }
  }
  return true;
}


RawBigint* Bigint::New(const String& str, Heap::Space space) {
  const Bigint& result = Bigint::Handle(
      BigintOperations::NewFromCString(str.ToCString(), space));
  ASSERT(!BigintOperations::FitsIntoInt64(result));
  return result.raw();
}


RawBigint* Bigint::NewCanonical(const String& str) {
  const Bigint& value = Bigint::Handle(
      BigintOperations::NewFromCString(str.ToCString(), Heap::kOld));
  ASSERT(!BigintOperations::FitsIntoInt64(value));
  const Class& cls =
      Class::Handle(Isolate::Current()->object_store()->bigint_class());
  const Array& constants = Array::Handle(cls.constants());
  const intptr_t constants_len = constants.Length();
  // Linear search to see whether this value is already present in the
  // list of canonicalized constants.
  Bigint& canonical_value = Bigint::Handle();
  intptr_t index = 0;
  while (index < constants_len) {
    canonical_value ^= constants.At(index);
    if (canonical_value.IsNull()) {
      break;
    }
    if (canonical_value.Equals(value)) {
      return canonical_value.raw();
    }
    index++;
  }
  // The value needs to be added to the constants list. Grow the list if
  // it is full.
  cls.InsertCanonicalConstant(index, value);
  value.SetCanonical();
  return value.raw();
}


double Bigint::AsDoubleValue() const {
  return Double::Handle(BigintOperations::ToDouble(*this)).value();
}


int64_t Bigint::AsInt64Value() const {
  if (!BigintOperations::FitsIntoInt64(*this)) {
    UNREACHABLE();
  }
  return BigintOperations::ToInt64(*this);
}


// For positive values: Smi < Mint < Bigint.
int Bigint::CompareWith(const Integer& other) const {
  ASSERT(!FitsIntoSmi(*this));
  ASSERT(!BigintOperations::FitsIntoInt64(*this));
  if (other.IsBigint()) {
    return BigintOperations::Compare(*this, Bigint::Cast(other));
  }
  if (this->IsNegative() == other.IsNegative()) {
    return this->IsNegative() ? -1 : 1;
  }
  return this->IsNegative() ? -1 : 1;
}


RawBigint* Bigint::Allocate(intptr_t length, Heap::Space space) {
  if (length < 0 || length > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Bigint::Allocate: invalid length %" Pd "\n", length);
  }
  ASSERT(Isolate::Current()->object_store()->bigint_class() != Class::null());
  Bigint& result = Bigint::Handle();
  {
    RawObject* raw = Object::Allocate(Bigint::kClassId,
                                      Bigint::InstanceSize(length),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.raw_ptr()->allocated_length_ = length;  // Chunk length allocated.
    result.raw_ptr()->signed_length_ = length;  // Chunk length in use.
  }
  return result.raw();
}


static uword BigintAllocator(intptr_t size) {
  Zone* zone = Isolate::Current()->current_zone();
  return zone->AllocUnsafe(size);
}


const char* Bigint::ToCString() const {
  return BigintOperations::ToDecimalCString(*this, &BigintAllocator);
}


void Bigint::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


// Synchronize with implementation in compiler (intrinsifier).
class StringHasher : ValueObject {
 public:
  StringHasher() : hash_(0) {}
  void Add(int32_t ch) {
    hash_ += ch;
    hash_ += hash_ << 10;
    hash_ ^= hash_ >> 6;
  }
  // Return a non-zero hash of at most 'bits' bits.
  intptr_t Finalize(int bits) {
    ASSERT(1 <= bits && bits <= (kBitsPerWord - 1));
    hash_ += hash_ << 3;
    hash_ ^= hash_ >> 11;
    hash_ += hash_ << 15;
    hash_ = hash_ & ((static_cast<intptr_t>(1) << bits) - 1);
    ASSERT(hash_ <= static_cast<uint32_t>(kMaxInt32));
    return hash_ == 0 ? 1 : hash_;
  }
 private:
  uint32_t hash_;
};


intptr_t String::Hash(const String& str, intptr_t begin_index, intptr_t len) {
  ASSERT(begin_index >= 0);
  ASSERT(len >= 0);
  ASSERT((begin_index + len) <= str.Length());
  StringHasher hasher;
  if (str.IsOneByteString()) {
    for (intptr_t i = 0; i < len; i++) {
      hasher.Add(*OneByteString::CharAddr(str, i + begin_index));
    }
  } else {
    CodePointIterator it(str, begin_index, len);
    while (it.Next()) {
      hasher.Add(it.Current());
    }
  }
  return hasher.Finalize(String::kHashBits);
}


template<typename T>
static intptr_t HashImpl(const T* characters, intptr_t len) {
  ASSERT(len >= 0);
  StringHasher hasher;
  for (intptr_t i = 0; i < len; i++) {
    hasher.Add(characters[i]);
  }
  return hasher.Finalize(String::kHashBits);
}


intptr_t String::Hash(const uint8_t* characters, intptr_t len) {
  return HashImpl(characters, len);
}


intptr_t String::Hash(const uint16_t* characters, intptr_t len) {
  StringHasher hasher;
  intptr_t i = 0;
  while (i < len) {
    hasher.Add(Utf16::Next(characters, &i, len));
  }
  return hasher.Finalize(String::kHashBits);
}


intptr_t String::Hash(const int32_t* characters, intptr_t len) {
  return HashImpl(characters, len);
}


int32_t String::CharAt(intptr_t index) const {
  intptr_t class_id = raw()->GetClassId();
  ASSERT(RawObject::IsStringClassId(class_id));
  NoGCScope no_gc;
  if (class_id == kOneByteStringCid) {
    return *OneByteString::CharAddr(*this, index);
  }
  if (class_id == kTwoByteStringCid) {
    return *TwoByteString::CharAddr(*this, index);
  }
  if (class_id == kExternalOneByteStringCid) {
    return *ExternalOneByteString::CharAddr(*this, index);
  }
  ASSERT(class_id == kExternalTwoByteStringCid);
  return *ExternalTwoByteString::CharAddr(*this, index);
}


intptr_t String::CharSize() const {
  intptr_t class_id = raw()->GetClassId();
  if (class_id == kOneByteStringCid || class_id == kExternalOneByteStringCid) {
    return kOneByteChar;
  }
  ASSERT(class_id == kTwoByteStringCid ||
         class_id == kExternalTwoByteStringCid);
  return kTwoByteChar;
}


void* String::GetPeer() const {
  intptr_t class_id = raw()->GetClassId();
  if (class_id == kExternalOneByteStringCid) {
    return ExternalOneByteString::GetPeer(*this);
  }
  ASSERT(class_id == kExternalTwoByteStringCid);
  return ExternalTwoByteString::GetPeer(*this);
}


bool String::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    // Both handles point to the same raw instance.
    return true;
  }

  if (!other.IsString() || other.IsNull()) {
    return false;
  }

  const String& other_string = String::Cast(other);
  if (this->HasHash() && other_string.HasHash() &&
      (this->Hash() != other_string.Hash())) {
    return false;  // Both sides have a hash code and it does not match.
  }
  return Equals(other_string, 0, other_string.Length());
}


bool String::Equals(const char* cstr) const {
  ASSERT(cstr != NULL);
  CodePointIterator it(*this);
  intptr_t len = strlen(cstr);
  while (it.Next()) {
    if (*cstr == '\0') {
      // Lengths don't match.
      return false;
    }
    int32_t ch;
    intptr_t consumed = Utf8::Decode(reinterpret_cast<const uint8_t*>(cstr),
                                     len,
                                     &ch);
    if (consumed == 0 || it.Current() != ch) {
      return false;
    }
    cstr += consumed;
    len -= consumed;
  }
  return *cstr == '\0';
}


bool String::Equals(const uint8_t* latin1_array, intptr_t len) const {
  if (len != this->Length()) {
    // Lengths don't match.
    return false;
  }

  for (intptr_t i = 0; i < len; i++) {
    if (this->CharAt(i) != latin1_array[i]) {
      return false;
    }
  }
  return true;
}


bool String::Equals(const uint16_t* utf16_array, intptr_t len) const {
  if (len != this->Length()) {
    // Lengths don't match.
    return false;
  }

  for (intptr_t i = 0; i < len; i++) {
    if (this->CharAt(i) != utf16_array[i]) {
      return false;
    }
  }
  return true;
}


bool String::Equals(const int32_t* utf32_array, intptr_t len) const {
  CodePointIterator it(*this);
  intptr_t i = 0;
  bool has_more = it.Next();
  while (has_more && (i < len)) {
    if ((it.Current() != static_cast<int32_t>(utf32_array[i]))) {
      return false;
    }
    // Advance both streams forward.
    ++i;
    has_more = it.Next();
  }
  // Strings are only true iff we reached the end in both streams.
  return (i == len) && !has_more;
}


intptr_t String::CompareTo(const String& other) const {
  const intptr_t this_len = this->Length();
  const intptr_t other_len = other.IsNull() ? 0 : other.Length();
  const intptr_t len = (this_len < other_len) ? this_len : other_len;
  for (intptr_t i = 0; i < len; i++) {
    int32_t this_code_point = this->CharAt(i);
    int32_t other_code_point = other.CharAt(i);
    if (this_code_point < other_code_point) {
      return -1;
    }
    if (this_code_point > other_code_point) {
      return 1;
    }
  }
  if (this_len < other_len) return -1;
  if (this_len > other_len) return 1;
  return 0;
}


bool String::StartsWith(const String& other) const {
  if (other.IsNull() || (other.Length() > this->Length())) {
    return false;
  }
  intptr_t slen = other.Length();
  for (int i = 0; i < slen; i++) {
    if (this->CharAt(i) != other.CharAt(i)) {
      return false;
    }
  }
  return true;
}


RawInstance* String::CheckAndCanonicalize(const char** error_str) const {
  if (IsCanonical()) {
    return this->raw();
  }
  return Symbols::New(*this);
}


RawString* String::New(const char* cstr, Heap::Space space) {
  ASSERT(cstr != NULL);
  intptr_t array_len = strlen(cstr);
  const uint8_t* utf8_array = reinterpret_cast<const uint8_t*>(cstr);
  return String::FromUTF8(utf8_array, array_len, space);
}


RawString* String::FromUTF8(const uint8_t* utf8_array,
                            intptr_t array_len,
                            Heap::Space space) {
  Utf8::Type type;
  intptr_t len = Utf8::CodeUnitCount(utf8_array, array_len, &type);
  if (type == Utf8::kLatin1) {
    const String& strobj = String::Handle(OneByteString::New(len, space));
    if (len > 0) {
      NoGCScope no_gc;
      Utf8::DecodeToLatin1(utf8_array, array_len,
                           OneByteString::CharAddr(strobj, 0), len);
    }
    return strobj.raw();
  }
  ASSERT((type == Utf8::kBMP) || (type == Utf8::kSupplementary));
  const String& strobj = String::Handle(TwoByteString::New(len, space));
  NoGCScope no_gc;
  Utf8::DecodeToUTF16(utf8_array, array_len,
                      TwoByteString::CharAddr(strobj, 0), len);
  return strobj.raw();
}


RawString* String::FromLatin1(const uint8_t* latin1_array,
                              intptr_t array_len,
                              Heap::Space space) {
  return OneByteString::New(latin1_array, array_len, space);
}


RawString* String::FromUTF16(const uint16_t* utf16_array,
                             intptr_t array_len,
                             Heap::Space space) {
  bool is_one_byte_string = true;
  for (intptr_t i = 0; i < array_len; ++i) {
    if (!Utf::IsLatin1(utf16_array[i])) {
      is_one_byte_string = false;
      break;
    }
  }
  if (is_one_byte_string) {
    return OneByteString::New(utf16_array, array_len, space);
  }
  return TwoByteString::New(utf16_array, array_len, space);
}


RawString* String::FromUTF32(const int32_t* utf32_array,
                             intptr_t array_len,
                             Heap::Space space) {
  bool is_one_byte_string = true;
  intptr_t utf16_len = array_len;
  for (intptr_t i = 0; i < array_len; ++i) {
    if (!Utf::IsLatin1(utf32_array[i])) {
      is_one_byte_string = false;
      if (Utf::IsSupplementary(utf32_array[i])) {
        utf16_len += 1;
      }
    }
  }
  if (is_one_byte_string) {
    return OneByteString::New(utf32_array, array_len, space);
  }
  return TwoByteString::New(utf16_len, utf32_array, array_len, space);
}


RawString* String::New(const String& str, Heap::Space space) {
  // Currently this just creates a copy of the string in the correct space.
  // Once we have external string support, this will also create a heap copy of
  // the string if necessary. Some optimizations are possible, such as not
  // copying internal strings into the same space.
  intptr_t len = str.Length();
  String& result = String::Handle();
  intptr_t char_size = str.CharSize();
  if (char_size == kOneByteChar) {
    result = OneByteString::New(len, space);
  } else {
    ASSERT(char_size == kTwoByteChar);
    result = TwoByteString::New(len, space);
  }
  String::Copy(result, 0, str, 0, len);
  return result.raw();
}


RawString* String::NewExternal(const uint8_t* characters,
                               intptr_t len,
                               void* peer,
                               Dart_PeerFinalizer callback,
                               Heap::Space space) {
  return ExternalOneByteString::New(characters, len, peer, callback, space);
}


RawString* String::NewExternal(const uint16_t* characters,
                               intptr_t len,
                               void* peer,
                               Dart_PeerFinalizer callback,
                               Heap::Space space) {
  return ExternalTwoByteString::New(characters, len, peer, callback, space);
}


void String::Copy(const String& dst, intptr_t dst_offset,
                  const uint8_t* characters,
                  intptr_t len) {
  ASSERT(dst_offset >= 0);
  ASSERT(len >= 0);
  ASSERT(len <= (dst.Length() - dst_offset));
  if (dst.IsOneByteString()) {
    NoGCScope no_gc;
    if (len > 0) {
      memmove(OneByteString::CharAddr(dst, dst_offset),
              characters,
              len);
    }
  } else if (dst.IsTwoByteString()) {
    for (intptr_t i = 0; i < len; ++i) {
      *TwoByteString::CharAddr(dst, i + dst_offset) = characters[i];
    }
  }
}


void String::Copy(const String& dst, intptr_t dst_offset,
                  const uint16_t* utf16_array,
                  intptr_t array_len) {
  ASSERT(dst_offset >= 0);
  ASSERT(array_len >= 0);
  ASSERT(array_len <= (dst.Length() - dst_offset));
  if (dst.IsOneByteString()) {
    NoGCScope no_gc;
    for (intptr_t i = 0; i < array_len; ++i) {
      ASSERT(Utf::IsLatin1(utf16_array[i]));
      *OneByteString::CharAddr(dst, i + dst_offset) = utf16_array[i];
    }
  } else {
    ASSERT(dst.IsTwoByteString());
    NoGCScope no_gc;
    if (array_len > 0) {
      memmove(TwoByteString::CharAddr(dst, dst_offset),
              utf16_array,
              array_len * 2);
    }
  }
}


void String::Copy(const String& dst, intptr_t dst_offset,
                  const String& src, intptr_t src_offset,
                  intptr_t len) {
  ASSERT(dst_offset >= 0);
  ASSERT(src_offset >= 0);
  ASSERT(len >= 0);
  ASSERT(len <= (dst.Length() - dst_offset));
  ASSERT(len <= (src.Length() - src_offset));
  if (len > 0) {
    intptr_t char_size = src.CharSize();
    if (char_size == kOneByteChar) {
      if (src.IsOneByteString()) {
        NoGCScope no_gc;
        String::Copy(dst,
                     dst_offset,
                     OneByteString::CharAddr(src, src_offset),
                     len);
      } else {
        ASSERT(src.IsExternalOneByteString());
        NoGCScope no_gc;
        String::Copy(dst,
                     dst_offset,
                     ExternalOneByteString::CharAddr(src, src_offset),
                     len);
      }
    } else {
      ASSERT(char_size == kTwoByteChar);
      if (src.IsTwoByteString()) {
        NoGCScope no_gc;
        String::Copy(dst,
                     dst_offset,
                     TwoByteString::CharAddr(src, src_offset),
                     len);
      } else {
        ASSERT(src.IsExternalTwoByteString());
        NoGCScope no_gc;
        String::Copy(dst,
                     dst_offset,
                     ExternalTwoByteString::CharAddr(src, src_offset),
                     len);
      }
    }
  }
}


RawString* String::EscapeSpecialCharacters(const String& str) {
  if (str.IsOneByteString()) {
    return OneByteString::EscapeSpecialCharacters(str);
  }
  if (str.IsTwoByteString()) {
    return TwoByteString::EscapeSpecialCharacters(str);
  }
  if (str.IsExternalOneByteString()) {
    return ExternalOneByteString::EscapeSpecialCharacters(str);
  }
  ASSERT(str.IsExternalTwoByteString());
  // If EscapeSpecialCharacters is frequently called on external two byte
  // strings, we should implement it directly on ExternalTwoByteString rather
  // than first converting to a TwoByteString.
  return TwoByteString::EscapeSpecialCharacters(
      String::Handle(TwoByteString::New(str, Heap::kNew)));
}


RawString* String::NewFormatted(const char* format, ...) {
  va_list args;
  va_start(args, format);
  RawString* result = NewFormattedV(format, args);
  NoGCScope no_gc;
  va_end(args);
  return result;
}


RawString* String::NewFormattedV(const char* format, va_list args) {
  va_list args_copy;
  va_copy(args_copy, args);
  intptr_t len = OS::VSNPrint(NULL, 0, format, args_copy);
  va_end(args_copy);

  Zone* zone = Isolate::Current()->current_zone();
  char* buffer = zone->Alloc<char>(len + 1);
  OS::VSNPrint(buffer, (len + 1), format, args);

  return String::New(buffer);
}


RawString* String::Concat(const String& str1,
                          const String& str2,
                          Heap::Space space) {
  ASSERT(!str1.IsNull() && !str2.IsNull());
  intptr_t char_size = Utils::Maximum(str1.CharSize(), str2.CharSize());
  if (char_size == kTwoByteChar) {
    return TwoByteString::Concat(str1, str2, space);
  }
  return OneByteString::Concat(str1, str2, space);
}


RawString* String::ConcatAll(const Array& strings,
                             Heap::Space space) {
  return ConcatAllRange(strings, 0, strings.Length(), space);
}


RawString* String::ConcatAllRange(const Array& strings,
                                  intptr_t start,
                                  intptr_t end,
                                  Heap::Space space) {
  ASSERT(!strings.IsNull());
  ASSERT(start >= 0);
  ASSERT(end <= strings.Length());
  intptr_t result_len = 0;
  String& str = String::Handle();
  intptr_t char_size = kOneByteChar;
  // Compute 'char_size' and 'result_len'.
  for (intptr_t i = start; i < end; i++) {
    str ^= strings.At(i);
    const intptr_t str_len = str.Length();
    if ((kMaxElements - result_len) < str_len) {
      Isolate* isolate = Isolate::Current();
      const Instance& exception =
          Instance::Handle(isolate->object_store()->out_of_memory());
      Exceptions::Throw(exception);
      UNREACHABLE();
    }
    result_len += str_len;
    char_size = Utils::Maximum(char_size, str.CharSize());
  }
  if (char_size == kOneByteChar) {
    return OneByteString::ConcatAll(strings, start, end, result_len, space);
  }
  ASSERT(char_size == kTwoByteChar);
  return TwoByteString::ConcatAll(strings, start, end, result_len, space);
}


RawString* String::SubString(const String& str,
                             intptr_t begin_index,
                             Heap::Space space) {
  ASSERT(!str.IsNull());
  if (begin_index >= str.Length()) {
    return String::null();
  }
  return String::SubString(str, begin_index, (str.Length() - begin_index));
}


RawString* String::SubString(const String& str,
                             intptr_t begin_index,
                             intptr_t length,
                             Heap::Space space) {
  ASSERT(!str.IsNull());
  ASSERT(begin_index >= 0);
  ASSERT(length >= 0);
  if (begin_index <= str.Length() && length == 0) {
    return Symbols::Empty().raw();
  }
  if (begin_index > str.Length()) {
    return String::null();
  }
  String& result = String::Handle();
  bool is_one_byte_string = true;
  intptr_t char_size = str.CharSize();
  if (char_size == kTwoByteChar) {
    for (intptr_t i = begin_index; i < begin_index + length; ++i) {
      if (!Utf::IsLatin1(str.CharAt(i))) {
        is_one_byte_string = false;
        break;
      }
    }
  }
  if (is_one_byte_string) {
    result = OneByteString::New(length, space);
  } else {
    result = TwoByteString::New(length, space);
  }
  String::Copy(result, 0, str, begin_index, length);
  return result.raw();
}


const char* String::ToCString() const {
  if (IsOneByteString()) {
    // Quick conversion if OneByteString contains only ASCII characters.
    intptr_t len = Length();
    if (len == 0) {
      return "";
    }
    Zone* zone = Isolate::Current()->current_zone();
    uint8_t* result = zone->Alloc<uint8_t>(len + 1);
    NoGCScope no_gc;
    const uint8_t* original_str = OneByteString::CharAddr(*this, 0);
    for (intptr_t i = 0; i < len; i++) {
      if (original_str[i] <= Utf8::kMaxOneByteChar) {
        result[i] = original_str[i];
      } else {
        len = -1;
        break;
      }
    }
    if (len > 0) {
      result[len] = 0;
      return reinterpret_cast<const char*>(result);
    }
  }
  const intptr_t len = Utf8::Length(*this);
  Zone* zone = Isolate::Current()->current_zone();
  uint8_t* result = zone->Alloc<uint8_t>(len + 1);
  ToUTF8(result, len);
  result[len] = 0;
  return reinterpret_cast<const char*>(result);
}


void String::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


void String::ToUTF8(uint8_t* utf8_array, intptr_t array_len) const {
  ASSERT(array_len >= Utf8::Length(*this));
  Utf8::Encode(*this, reinterpret_cast<char*>(utf8_array), array_len);
}


static FinalizablePersistentHandle* AddFinalizer(
    const Object& referent,
    void* peer,
    Dart_WeakPersistentHandleFinalizer callback) {
  ASSERT((callback != NULL && peer != NULL) ||
         (callback == NULL && peer == NULL));
  ApiState* state = Isolate::Current()->api_state();
  ASSERT(state != NULL);
  FinalizablePersistentHandle* weak_ref =
      state->weak_persistent_handles().AllocateHandle();
  weak_ref->set_raw(referent);
  weak_ref->set_peer(peer);
  weak_ref->set_callback(callback);
  return weak_ref;
}


RawString* String::MakeExternal(void* array,
                                intptr_t length,
                                void* peer,
                                Dart_PeerFinalizer cback) const {
  NoGCScope no_gc;
  ASSERT(array != NULL);
  intptr_t str_length = this->Length();
  ASSERT(length >= (str_length * this->CharSize()));
  intptr_t class_id = raw()->GetClassId();
  intptr_t used_size = 0;
  intptr_t original_size = 0;
  uword tags = raw_ptr()->tags_;

  ASSERT(!InVMHeap());
  if (class_id == kOneByteStringCid) {
    used_size = ExternalOneByteString::InstanceSize();
    original_size = OneByteString::InstanceSize(str_length);
    ASSERT(original_size >= used_size);

    // Copy the data into the external array.
    if (str_length > 0) {
      memmove(array, OneByteString::CharAddr(*this, 0), str_length);
    }

    // Update the class information of the object.
    const intptr_t class_id = kExternalOneByteStringCid;
    tags = RawObject::SizeTag::update(used_size, tags);
    tags = RawObject::ClassIdTag::update(class_id, tags);
    raw_ptr()->tags_ = tags;
    const String& result = String::Handle(this->raw());
    ExternalStringData<uint8_t>* ext_data = new ExternalStringData<uint8_t>(
        reinterpret_cast<const uint8_t*>(array), peer, cback);
    result.SetLength(str_length);
    result.SetHash(0);
    ExternalOneByteString::SetExternalData(result, ext_data);
    AddFinalizer(result, ext_data, ExternalOneByteString::Finalize);
  } else {
    ASSERT(class_id == kTwoByteStringCid);
    used_size = ExternalTwoByteString::InstanceSize();
    original_size = TwoByteString::InstanceSize(str_length);
    ASSERT(original_size >= used_size);

    // Copy the data into the external array.
    if (str_length > 0) {
      memmove(array,
              TwoByteString::CharAddr(*this, 0),
              (str_length * kTwoByteChar));
    }

    // Update the class information of the object.
    const intptr_t class_id = kExternalTwoByteStringCid;
    tags = RawObject::SizeTag::update(used_size, tags);
    tags = RawObject::ClassIdTag::update(class_id, tags);
    raw_ptr()->tags_ = tags;
    const String& result = String::Handle(this->raw());
    ExternalStringData<uint16_t>* ext_data = new ExternalStringData<uint16_t>(
        reinterpret_cast<const uint16_t*>(array), peer, cback);
    result.SetLength(str_length);
    result.SetHash(0);
    ExternalTwoByteString::SetExternalData(result, ext_data);
    AddFinalizer(result, ext_data, ExternalTwoByteString::Finalize);
  }

  // If there is any left over space fill it with either an Array object or
  // just a plain object (depending on the amount of left over space) so
  // that it can be traversed over successfully during garbage collection.
  Object::MakeUnusedSpaceTraversable(*this, original_size, used_size);

  return this->raw();
}


RawString* String::Transform(int32_t (*mapping)(int32_t ch),
                             const String& str,
                             Heap::Space space) {
  ASSERT(!str.IsNull());
  bool has_mapping = false;
  int32_t dst_max = 0;
  CodePointIterator it(str);
  while (it.Next()) {
    int32_t src = it.Current();
    int32_t dst = mapping(src);
    if (src != dst) {
      has_mapping = true;
    }
    dst_max = Utils::Maximum(dst_max, dst);
  }
  if (!has_mapping) {
    return str.raw();
  }
  if (Utf::IsLatin1(dst_max)) {
    return OneByteString::Transform(mapping, str, space);
  }
  ASSERT(Utf::IsBmp(dst_max) || Utf::IsSupplementary(dst_max));
  return TwoByteString::Transform(mapping, str, space);
}


RawString* String::ToUpperCase(const String& str, Heap::Space space) {
  // TODO(cshapiro): create a fast-path for OneByteString instances.
  return Transform(CaseMapping::ToUpper, str, space);
}


RawString* String::ToLowerCase(const String& str, Heap::Space space) {
  // TODO(cshapiro): create a fast-path for OneByteString instances.
  return Transform(CaseMapping::ToLower, str, space);
}


// Check to see if 'str1' matches 'str2' as is or
// once the private key separator is stripped from str2.
//
// Things are made more complicated by the fact that constructors are
// added *after* the private suffix, so "foo@123.named" should match
// "foo.named".
//
// Also, the private suffix can occur more than once in the name, as in:
//
//    _ReceivePortImpl@6be832b._internal@6be832b
//
template<typename T1, typename T2>
static bool EqualsIgnoringPrivateKey(const String& str1,
                                     const String& str2) {
  intptr_t len = str1.Length();
  intptr_t str2_len = str2.Length();
  if (len == str2_len) {
    for (intptr_t i = 0; i < len; i++) {
      if (T1::CharAt(str1, i) != T2::CharAt(str2, i)) {
        return false;
      }
    }
    return true;
  }
  if (len < str2_len) {
    return false;  // No way they can match.
  }
  intptr_t pos = 0;
  intptr_t str2_pos = 0;
  while (pos < len) {
    int32_t ch = T1::CharAt(str1, pos);
    pos++;

    if (ch == Scanner::kPrivateKeySeparator) {
      // Consume a private key separator.
      while ((pos < len) && (T1::CharAt(str1, pos) != '.')) {
        pos++;
      }
      // Resume matching characters.
      continue;
    }
    if ((str2_pos == str2_len) || (ch != T2::CharAt(str2, str2_pos))) {
      return false;
    }
    str2_pos++;
  }

  // We have reached the end of mangled_name string.
  ASSERT(pos == len);
  return (str2_pos == str2_len);
}


#define EQUALS_IGNORING_PRIVATE_KEY(class_id, type, str1, str2)                \
  switch (class_id) {                                                          \
    case kOneByteStringCid :                                                   \
      return dart::EqualsIgnoringPrivateKey<type, OneByteString>(str1, str2);  \
    case kTwoByteStringCid :                                                   \
      return dart::EqualsIgnoringPrivateKey<type, TwoByteString>(str1, str2);  \
    case kExternalOneByteStringCid :                                           \
      return dart::EqualsIgnoringPrivateKey<type, ExternalOneByteString>(str1, \
                                                                         str2);\
    case kExternalTwoByteStringCid :                                           \
      return dart::EqualsIgnoringPrivateKey<type, ExternalTwoByteString>(str1, \
                                                                         str2);\
  }                                                                            \
  UNREACHABLE();                                                               \


bool String::EqualsIgnoringPrivateKey(const String& str1,
                                      const String& str2) {
  if (str1.raw() == str2.raw()) {
    return true;  // Both handles point to the same raw instance.
  }
  NoGCScope no_gc;
  intptr_t str1_class_id = str1.raw()->GetClassId();
  intptr_t str2_class_id = str2.raw()->GetClassId();
  switch (str1_class_id) {
    case kOneByteStringCid :
      EQUALS_IGNORING_PRIVATE_KEY(str2_class_id, OneByteString, str1, str2);
      break;
    case kTwoByteStringCid :
      EQUALS_IGNORING_PRIVATE_KEY(str2_class_id, TwoByteString, str1, str2);
      break;
    case kExternalOneByteStringCid :
      EQUALS_IGNORING_PRIVATE_KEY(str2_class_id,
                                  ExternalOneByteString, str1, str2);
      break;
    case kExternalTwoByteStringCid :
      EQUALS_IGNORING_PRIVATE_KEY(str2_class_id,
                                  ExternalTwoByteString, str1, str2);
      break;
  }
  UNREACHABLE();
  return false;
}


bool String::CodePointIterator::Next() {
  ASSERT(index_ >= -1);
  intptr_t length = Utf16::Length(ch_);
  if (index_ < (end_ - length)) {
    index_ += length;
    ch_ = str_.CharAt(index_);
    if (Utf16::IsLeadSurrogate(ch_) && (index_ < (end_ - 1))) {
      int32_t ch2 = str_.CharAt(index_ + 1);
      if (Utf16::IsTrailSurrogate(ch2)) {
        ch_ = Utf16::Decode(ch_, ch2);
      }
    }
    return true;
  }
  index_ = end_;
  return false;
}


RawOneByteString* OneByteString::EscapeSpecialCharacters(const String& str) {
  intptr_t len = str.Length();
  if (len > 0) {
    intptr_t num_escapes = 0;
    for (intptr_t i = 0; i < len; i++) {
      if (IsSpecialCharacter(*CharAddr(str, i))) {
        num_escapes += 1;
      }
    }
    const String& dststr = String::Handle(
        OneByteString::New(len + num_escapes, Heap::kNew));
    intptr_t index = 0;
    for (intptr_t i = 0; i < len; i++) {
      if (IsSpecialCharacter(*CharAddr(str, i))) {
        *(CharAddr(dststr, index)) = '\\';
        *(CharAddr(dststr, index + 1)) = SpecialCharacter(*CharAddr(str, i));
        index += 2;
      } else {
        *(CharAddr(dststr, index)) = *CharAddr(str, i);
        index += 1;
      }
    }
    return OneByteString::raw(dststr);
  }
  return OneByteString::raw(Symbols::Empty());
}

RawOneByteString* ExternalOneByteString::EscapeSpecialCharacters(
    const String& str) {
  intptr_t len = str.Length();
  if (len > 0) {
    intptr_t num_escapes = 0;
    for (intptr_t i = 0; i < len; i++) {
      if (IsSpecialCharacter(*CharAddr(str, i))) {
        num_escapes += 1;
      }
    }
    const String& dststr = String::Handle(
        OneByteString::New(len + num_escapes, Heap::kNew));
    intptr_t index = 0;
    for (intptr_t i = 0; i < len; i++) {
      if (IsSpecialCharacter(*CharAddr(str, i))) {
        *(OneByteString::CharAddr(dststr, index)) = '\\';
        *(OneByteString::CharAddr(dststr, index + 1)) =
        SpecialCharacter(*CharAddr(str, i));
        index += 2;
      } else {
        *(OneByteString::CharAddr(dststr, index)) = *CharAddr(str, i);
        index += 1;
      }
    }
    return OneByteString::raw(dststr);
  }
  return OneByteString::raw(Symbols::Empty());
}


RawOneByteString* OneByteString::New(intptr_t len,
                                     Heap::Space space) {
  ASSERT(Isolate::Current() == Dart::vm_isolate() ||
         Isolate::Current()->object_store()->one_byte_string_class() !=
         Class::null());
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in OneByteString::New: invalid len %" Pd "\n", len);
  }
  {
    RawObject* raw = Object::Allocate(OneByteString::kClassId,
                                      OneByteString::InstanceSize(len),
                                      space);
    NoGCScope no_gc;
    RawOneByteString* result = reinterpret_cast<RawOneByteString*>(raw);
    result->ptr()->length_ = Smi::New(len);
    result->ptr()->hash_ = 0;
    return result;
  }
}


RawOneByteString* OneByteString::New(const uint8_t* characters,
                                     intptr_t len,
                                     Heap::Space space) {
  const String& result = String::Handle(OneByteString::New(len, space));
  if (len > 0) {
    NoGCScope no_gc;
    memmove(CharAddr(result, 0), characters, len);
  }
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::New(const uint16_t* characters,
                                     intptr_t len,
                                     Heap::Space space) {
  const String& result =String::Handle(OneByteString::New(len, space));
  for (intptr_t i = 0; i < len; ++i) {
    ASSERT(Utf::IsLatin1(characters[i]));
    *CharAddr(result, i) = characters[i];
  }
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::New(const int32_t* characters,
                                     intptr_t len,
                                     Heap::Space space) {
  const String& result = String::Handle(OneByteString::New(len, space));
  for (intptr_t i = 0; i < len; ++i) {
    ASSERT(Utf::IsLatin1(characters[i]));
    *CharAddr(result, i) = characters[i];
  }
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::New(const String& str,
                                     Heap::Space space) {
  intptr_t len = str.Length();
  const String& result = String::Handle(OneByteString::New(len, space));
  String::Copy(result, 0, str, 0, len);
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::New(const String& other_one_byte_string,
                                     intptr_t other_start_index,
                                     intptr_t other_len,
                                     Heap::Space space) {
  const String& result = String::Handle(OneByteString::New(other_len, space));
  ASSERT(other_one_byte_string.IsOneByteString());
  if (other_len > 0) {
    NoGCScope no_gc;
    memmove(OneByteString::CharAddr(result, 0),
            OneByteString::CharAddr(other_one_byte_string, other_start_index),
            other_len);
  }
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::Concat(const String& str1,
                                        const String& str2,
                                        Heap::Space space) {
  intptr_t len1 = str1.Length();
  intptr_t len2 = str2.Length();
  intptr_t len = len1 + len2;
  const String& result = String::Handle(OneByteString::New(len, space));
  String::Copy(result, 0, str1, 0, len1);
  String::Copy(result, len1, str2, 0, len2);
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::ConcatAll(const Array& strings,
                                           intptr_t start,
                                           intptr_t end,
                                           intptr_t len,
                                           Heap::Space space) {
  ASSERT(!strings.IsNull());
  ASSERT(start >= 0);
  ASSERT(end <= strings.Length());
  const String& result = String::Handle(OneByteString::New(len, space));
  String& str = String::Handle();
  intptr_t pos = 0;
  for (intptr_t i = start; i < end; i++) {
    str ^= strings.At(i);
    const intptr_t str_len = str.Length();
    String::Copy(result, pos, str, 0, str_len);
    ASSERT((kMaxElements - pos) >= str_len);
    pos += str_len;
  }
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::Transform(int32_t (*mapping)(int32_t ch),
                                           const String& str,
                                           Heap::Space space) {
  ASSERT(!str.IsNull());
  intptr_t len = str.Length();
  const String& result = String::Handle(OneByteString::New(len, space));
  for (intptr_t i = 0; i < len; ++i) {
    int32_t ch = mapping(str.CharAt(i));
    ASSERT(Utf::IsLatin1(ch));
    *CharAddr(result, i) = ch;
  }
  return OneByteString::raw(result);
}


RawOneByteString* OneByteString::SubStringUnchecked(const String& str,
                                                    intptr_t begin_index,
                                                    intptr_t length,
                                                    Heap::Space space) {
  ASSERT(!str.IsNull() && str.IsOneByteString());
  ASSERT(begin_index >= 0);
  ASSERT(length >= 0);
  if (begin_index <= str.Length() && length == 0) {
    return OneByteString::raw(Symbols::Empty());
  }
  ASSERT(begin_index < str.Length());
  RawOneByteString* result = OneByteString::New(length, space);
  NoGCScope no_gc;
  if (length > 0) {
    uint8_t* dest = &result->ptr()->data_[0];
    uint8_t* src =  &raw_ptr(str)->data_[begin_index];
    memmove(dest, src, length);
  }
  return result;
}


void OneByteString::SetPeer(const String& str,
                            void* peer,
                            Dart_PeerFinalizer cback) {
  ASSERT(!str.IsNull() && str.IsOneByteString());
  ASSERT(peer != NULL);
  ExternalStringData<uint8_t>* ext_data =
      new ExternalStringData<uint8_t>(NULL, peer, cback);
  AddFinalizer(str, ext_data, OneByteString::Finalize);
  Isolate::Current()->heap()->SetPeer(str.raw(), peer);
}


void OneByteString::Finalize(Dart_WeakPersistentHandle handle, void* peer) {
  delete reinterpret_cast<ExternalStringData<uint8_t>*>(peer);
  DeleteWeakPersistentHandle(handle);
}


RawTwoByteString* TwoByteString::EscapeSpecialCharacters(const String& str) {
  intptr_t len = str.Length();
  if (len > 0) {
    intptr_t num_escapes = 0;
    for (intptr_t i = 0; i < len; i++) {
      if (IsSpecialCharacter(*CharAddr(str, i))) {
        num_escapes += 1;
      }
    }
    const String& dststr = String::Handle(
        TwoByteString::New(len + num_escapes, Heap::kNew));
    intptr_t index = 0;
    for (intptr_t i = 0; i < len; i++) {
      if (IsSpecialCharacter(*CharAddr(str, i))) {
        *(CharAddr(dststr, index)) = '\\';
        *(CharAddr(dststr, index + 1)) = SpecialCharacter(*CharAddr(str, i));
        index += 2;
      } else {
        *(CharAddr(dststr, index)) = *CharAddr(str, i);
        index += 1;
      }
    }
    return TwoByteString::raw(dststr);
  }
  return TwoByteString::New(0, Heap::kNew);
}


RawTwoByteString* TwoByteString::New(intptr_t len,
                                     Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->two_byte_string_class());
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in TwoByteString::New: invalid len %" Pd "\n", len);
  }
  String& result = String::Handle();
  {
    RawObject* raw = Object::Allocate(TwoByteString::kClassId,
                                      TwoByteString::InstanceSize(len),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(len);
    result.SetHash(0);
  }
  return TwoByteString::raw(result);
}


RawTwoByteString* TwoByteString::New(const uint16_t* utf16_array,
                                     intptr_t array_len,
                                     Heap::Space space) {
  ASSERT(array_len > 0);
  const String& result = String::Handle(TwoByteString::New(array_len, space));
  {
    NoGCScope no_gc;
    memmove(CharAddr(result, 0), utf16_array, (array_len * 2));
  }
  return TwoByteString::raw(result);
}


RawTwoByteString* TwoByteString::New(intptr_t utf16_len,
                                     const int32_t* utf32_array,
                                     intptr_t array_len,
                                     Heap::Space space) {
  ASSERT((array_len > 0) && (utf16_len >= array_len));
  const String& result = String::Handle(TwoByteString::New(utf16_len, space));
  {
    NoGCScope no_gc;
    intptr_t j = 0;
    for (intptr_t i = 0; i < array_len; ++i) {
      if (Utf::IsSupplementary(utf32_array[i])) {
        ASSERT(j < (utf16_len - 1));
        Utf16::Encode(utf32_array[i], CharAddr(result, j));
        j += 2;
      } else {
        ASSERT(j < utf16_len);
        *CharAddr(result, j) = utf32_array[i];
        j += 1;
      }
    }
  }
  return TwoByteString::raw(result);
}


RawTwoByteString* TwoByteString::New(const String& str,
                                     Heap::Space space) {
  intptr_t len = str.Length();
  const String& result = String::Handle(TwoByteString::New(len, space));
  String::Copy(result, 0, str, 0, len);
  return TwoByteString::raw(result);
}


RawTwoByteString* TwoByteString::Concat(const String& str1,
                                        const String& str2,
                                        Heap::Space space) {
  intptr_t len1 = str1.Length();
  intptr_t len2 = str2.Length();
  intptr_t len = len1 + len2;
  const String& result = String::Handle(TwoByteString::New(len, space));
  String::Copy(result, 0, str1, 0, len1);
  String::Copy(result, len1, str2, 0, len2);
  return TwoByteString::raw(result);
}


RawTwoByteString* TwoByteString::ConcatAll(const Array& strings,
                                           intptr_t start,
                                           intptr_t end,
                                           intptr_t len,
                                           Heap::Space space) {
  ASSERT(!strings.IsNull());
  ASSERT(start >= 0);
  ASSERT(end <= strings.Length());
  const String& result = String::Handle(TwoByteString::New(len, space));
  String& str = String::Handle();
  intptr_t pos = 0;
  for (intptr_t i = start; i < end; i++) {
    str ^= strings.At(i);
    const intptr_t str_len = str.Length();
    String::Copy(result, pos, str, 0, str_len);
    ASSERT((kMaxElements - pos) >= str_len);
    pos += str_len;
  }
  return TwoByteString::raw(result);
}


RawTwoByteString* TwoByteString::Transform(int32_t (*mapping)(int32_t ch),
                                           const String& str,
                                           Heap::Space space) {
  ASSERT(!str.IsNull());
  intptr_t len = str.Length();
  const String& result = String::Handle(TwoByteString::New(len, space));
  String::CodePointIterator it(str);
  intptr_t i = 0;
  while (it.Next()) {
    int32_t src = it.Current();
    int32_t dst = mapping(src);
    ASSERT(dst >= 0 && dst <= 0x10FFFF);
    intptr_t len = Utf16::Length(dst);
    if (len == 1) {
      *CharAddr(result, i) = dst;
    } else {
      ASSERT(len == 2);
      Utf16::Encode(dst, CharAddr(result, i));
    }
    i += len;
  }
  return TwoByteString::raw(result);
}


void TwoByteString::SetPeer(const String& str,
                            void* peer,
                            Dart_PeerFinalizer cback) {
  ASSERT(!str.IsNull() && str.IsTwoByteString());
  ASSERT(peer != NULL);
  ExternalStringData<uint16_t>* ext_data =
      new ExternalStringData<uint16_t>(NULL, peer, cback);
  AddFinalizer(str, ext_data, TwoByteString::Finalize);
  Isolate::Current()->heap()->SetPeer(str.raw(), peer);
}


void TwoByteString::Finalize(Dart_WeakPersistentHandle handle, void* peer) {
  delete reinterpret_cast<ExternalStringData<uint16_t>*>(peer);
  DeleteWeakPersistentHandle(handle);
}


RawExternalOneByteString* ExternalOneByteString::New(
    const uint8_t* data,
    intptr_t len,
    void* peer,
    Dart_PeerFinalizer callback,
    Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->
         external_one_byte_string_class() != Class::null());
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in ExternalOneByteString::New: invalid len %" Pd "\n",
           len);
  }
  String& result = String::Handle();
  ExternalStringData<uint8_t>* external_data =
      new ExternalStringData<uint8_t>(data, peer, callback);
  {
    RawObject* raw = Object::Allocate(ExternalOneByteString::kClassId,
                                      ExternalOneByteString::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(len);
    result.SetHash(0);
    SetExternalData(result, external_data);
  }
  AddFinalizer(result, external_data, ExternalOneByteString::Finalize);
  return ExternalOneByteString::raw(result);
}


void ExternalOneByteString::Finalize(Dart_WeakPersistentHandle handle,
                                     void* peer) {
  delete reinterpret_cast<ExternalStringData<uint8_t>*>(peer);
  DeleteWeakPersistentHandle(handle);
}


RawExternalTwoByteString* ExternalTwoByteString::New(
    const uint16_t* data,
    intptr_t len,
    void* peer,
    Dart_PeerFinalizer callback,
    Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->external_two_byte_string_class() !=
         Class::null());
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in ExternalTwoByteString::New: invalid len %" Pd "\n",
           len);
  }
  String& result = String::Handle();
  ExternalStringData<uint16_t>* external_data =
      new ExternalStringData<uint16_t>(data, peer, callback);
  {
    RawObject* raw = Object::Allocate(ExternalTwoByteString::kClassId,
                                      ExternalTwoByteString::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(len);
    result.SetHash(0);
    SetExternalData(result, external_data);
  }
  AddFinalizer(result, external_data, ExternalTwoByteString::Finalize);
  return ExternalTwoByteString::raw(result);
}


void ExternalTwoByteString::Finalize(Dart_WeakPersistentHandle handle,
                                     void* peer) {
  delete reinterpret_cast<ExternalStringData<uint16_t>*>(peer);
  DeleteWeakPersistentHandle(handle);
}


RawBool* Bool::New(bool value) {
  ASSERT(Isolate::Current()->object_store()->bool_class() != Class::null());
  Bool& result = Bool::Handle();
  {
    // Since the two boolean instances are singletons we allocate them straight
    // in the old generation.
    RawObject* raw = Object::Allocate(Bool::kClassId,
                                      Bool::InstanceSize(),
                                      Heap::kOld);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_value(value);
  result.SetCanonical();
  return result.raw();
}


const char* Bool::ToCString() const {
  return value() ? "true" : "false";
}


void Bool::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


bool Array::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    // Both handles point to the same raw instance.
    return true;
  }

  if (!other.IsArray() || other.IsNull()) {
    return false;
  }

  // Must have the same type arguments.
  if (!AbstractTypeArguments::AreEqual(
      AbstractTypeArguments::Handle(GetTypeArguments()),
      AbstractTypeArguments::Handle(other.GetTypeArguments()))) {
    return false;
  }

  const Array& other_arr = Array::Cast(other);

  intptr_t len = this->Length();
  if (len != other_arr.Length()) {
    return false;
  }

  for (intptr_t i = 0; i < len; i++) {
    if (this->At(i) != other_arr.At(i)) {
      return false;
    }
  }
  return true;
}


RawArray* Array::New(intptr_t len, Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->array_class() != Class::null());
  return New(kClassId, len, space);
}


RawArray* Array::New(intptr_t class_id, intptr_t len, Heap::Space space) {
  if (len < 0 || len > Array::kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in Array::New: invalid len %" Pd "\n", len);
  }
  Array& result = Array::Handle();
  {
    RawObject* raw = Object::Allocate(class_id,
                                      Array::InstanceSize(len),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(len);
  }
  return result.raw();
}


void Array::MakeImmutable() const {
  NoGCScope no_gc;
  uword tags = raw_ptr()->tags_;
  tags = RawObject::ClassIdTag::update(kImmutableArrayCid, tags);
  raw_ptr()->tags_ = tags;
}


const char* Array::ToCString() const {
  if (IsNull()) {
    return IsImmutable() ? "_ImmutableList NULL" : "_List NULL";
  }
  const char* format = IsImmutable() ?
      "_ImmutableList len:%" Pd : "_List len:%" Pd;
  intptr_t len = OS::SNPrint(NULL, 0, format, Length()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, Length());
  return chars;
}


void Array::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawArray* Array::Grow(const Array& source, int new_length, Heap::Space space) {
  const Array& result = Array::Handle(Array::New(new_length, space));
  intptr_t len = 0;
  if (!source.IsNull()) {
    len = source.Length();
    result.SetTypeArguments(
        AbstractTypeArguments::Handle(source.GetTypeArguments()));
  }
  ASSERT(new_length >= len);  // Cannot copy 'source' into new array.
  ASSERT(new_length != len);  // Unnecessary copying of array.
  Object& obj = Object::Handle();
  for (int i = 0; i < len; i++) {
    obj = source.At(i);
    result.SetAt(i, obj);
  }
  return result.raw();
}


RawArray* Array::MakeArray(const GrowableObjectArray& growable_array) {
  ASSERT(!growable_array.IsNull());
  intptr_t used_len = growable_array.Length();
  if (used_len == 0) {
    return Object::empty_array().raw();
  }
  intptr_t capacity_len = growable_array.Capacity();
  Isolate* isolate = Isolate::Current();
  const Array& array = Array::Handle(isolate, growable_array.data());
  intptr_t capacity_size = Array::InstanceSize(capacity_len);
  intptr_t used_size = Array::InstanceSize(used_len);
  NoGCScope no_gc;

  // Update the size in the header field and length of the array object.
  uword tags = array.raw_ptr()->tags_;
  ASSERT(kArrayCid == RawObject::ClassIdTag::decode(tags));
  tags = RawObject::SizeTag::update(used_size, tags);
  array.raw_ptr()->tags_ = tags;
  array.SetLength(used_len);

  // Null the GrowableObjectArray, we are removing it's backing array.
  growable_array.SetLength(0);
  growable_array.SetData(Object::empty_array());

  // If there is any left over space fill it with either an Array object or
  // just a plain object (depending on the amount of left over space) so
  // that it can be traversed over successfully during garbage collection.
  Object::MakeUnusedSpaceTraversable(array, capacity_size, used_size);

  return array.raw();
}


bool Array::CheckAndCanonicalizeFields(const char** error_str) const {
  Object& obj = Object::Handle();
  // Iterate over all elements, canonicalize numbers and strings, expect all
  // other instances to be canonical otherwise report error (return false).
  for (intptr_t i = 0; i < Length(); i++) {
    obj = At(i);
    if (obj.IsInstance() && !obj.IsSmi() && !obj.IsCanonical()) {
      if (obj.IsNumber() || obj.IsString()) {
        obj = Instance::Cast(obj).CheckAndCanonicalize(NULL);
        ASSERT(!obj.IsNull());
        this->SetAt(i, obj);
      } else {
        ASSERT(error_str != NULL);
        const char* kFormat = "element at index %" Pd ": %s\n";
        const intptr_t len =
            OS::SNPrint(NULL, 0, kFormat, i, obj.ToCString()) + 1;
        char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
        OS::SNPrint(chars, len, kFormat, i, obj.ToCString());
        *error_str = chars;
        return false;
      }
    }
  }
  return true;
}


RawImmutableArray* ImmutableArray::New(intptr_t len,
                                       Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->immutable_array_class() !=
         Class::null());
  return reinterpret_cast<RawImmutableArray*>(Array::New(kClassId, len, space));
}


void GrowableObjectArray::Add(const Object& value, Heap::Space space) const {
  ASSERT(!IsNull());
  if (Length() == Capacity()) {
    // TODO(Issue 2500): Need a better growth strategy.
    intptr_t new_capacity = (Capacity() == 0) ? 4 : Capacity() * 2;
    if (new_capacity <= Capacity()) {
      // Use the preallocated out of memory exception to avoid calling
      // into dart code or allocating any code.
      Isolate* isolate = Isolate::Current();
      const Instance& exception =
          Instance::Handle(isolate->object_store()->out_of_memory());
      Exceptions::Throw(exception);
      UNREACHABLE();
    }
    Grow(new_capacity, space);
  }
  ASSERT(Length() < Capacity());
  intptr_t index = Length();
  SetLength(index + 1);
  SetAt(index, value);
}


void GrowableObjectArray::Grow(intptr_t new_capacity, Heap::Space space) const {
  ASSERT(new_capacity > Capacity());
  const Array& contents = Array::Handle(data());
  const Array& new_contents =
      Array::Handle(Array::Grow(contents, new_capacity, space));
  StorePointer(&(raw_ptr()->data_), new_contents.raw());
}


RawObject* GrowableObjectArray::RemoveLast() const {
  ASSERT(!IsNull());
  ASSERT(Length() > 0);
  intptr_t index = Length() - 1;
  const Array& contents = Array::Handle(data());
  const Object& obj = Object::Handle(contents.At(index));
  contents.SetAt(index, Object::null_object());
  SetLength(index);
  return obj.raw();
}


bool GrowableObjectArray::Equals(const Instance& other) const {
  // If both handles point to the same raw instance they are equal.
  if (this->raw() == other.raw()) {
    return true;
  }

  // Other instance must be non null and a GrowableObjectArray.
  if (!other.IsGrowableObjectArray() || other.IsNull()) {
    return false;
  }

  const GrowableObjectArray& other_arr = GrowableObjectArray::Cast(other);

  // The capacity and length of both objects must be equal.
  if (Capacity() != other_arr.Capacity() || Length() != other_arr.Length()) {
    return false;
  }

  // Both must have the same type arguments.
  if (!AbstractTypeArguments::AreEqual(
      AbstractTypeArguments::Handle(GetTypeArguments()),
      AbstractTypeArguments::Handle(other.GetTypeArguments()))) {
    return false;
  }

  // The data part in both arrays must be identical.
  const Array& contents = Array::Handle(data());
  const Array& other_contents = Array::Handle(other_arr.data());
  for (intptr_t i = 0; i < Length(); i++) {
    if (contents.At(i) != other_contents.At(i)) {
      return false;
    }
  }
  return true;
}


RawGrowableObjectArray* GrowableObjectArray::New(intptr_t capacity,
                                                 Heap::Space space) {
  const Array& data = Array::Handle(Array::New(capacity, space));
  return New(data, space);
}


RawGrowableObjectArray* GrowableObjectArray::New(const Array& array,
                                                 Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->growable_object_array_class()
         != Class::null());
  GrowableObjectArray& result = GrowableObjectArray::Handle();
  {
    RawObject* raw = Object::Allocate(GrowableObjectArray::kClassId,
                                      GrowableObjectArray::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(0);
    result.SetData(array);
  }
  return result.raw();
}


const char* GrowableObjectArray::ToCString() const {
  if (IsNull()) {
    return "_GrowableList NULL";
  }
  const char* format = "_GrowableList len:%" Pd "";
  intptr_t len = OS::SNPrint(NULL, 0, format, Length()) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, Length());
  return chars;
}


void GrowableObjectArray::PrintToJSONStream(JSONStream* stream,
                                            bool ref) const {
  JSONObject jsobj(stream);
}


RawFloat32x4* Float32x4::New(float v0, float v1, float v2, float v3,
                             Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->float32x4_class() !=
         Class::null());
  Float32x4& result = Float32x4::Handle();
  {
    RawObject* raw = Object::Allocate(Float32x4::kClassId,
                                      Float32x4::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_x(v0);
  result.set_y(v1);
  result.set_z(v2);
  result.set_w(v3);
  return result.raw();
}


RawFloat32x4* Float32x4::New(simd128_value_t value, Heap::Space space) {
ASSERT(Isolate::Current()->object_store()->float32x4_class() !=
         Class::null());
  Float32x4& result = Float32x4::Handle();
  {
    RawObject* raw = Object::Allocate(Float32x4::kClassId,
                                      Float32x4::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_value(value);
  return result.raw();
}


simd128_value_t Float32x4::value() const {
  return simd128_value_t().readFrom(&raw_ptr()->value_[0]);
}


void Float32x4::set_value(simd128_value_t value) const {
  value.writeTo(&raw_ptr()->value_[0]);
}


void Float32x4::set_x(float value) const {
  raw_ptr()->value_[0] = value;
}


void Float32x4::set_y(float value) const {
  raw_ptr()->value_[1] = value;
}


void Float32x4::set_z(float value) const {
  raw_ptr()->value_[2] = value;
}


void Float32x4::set_w(float value) const {
  raw_ptr()->value_[3] = value;
}


float Float32x4::x() const {
  return raw_ptr()->value_[0];
}


float Float32x4::y() const {
  return raw_ptr()->value_[1];
}


float Float32x4::z() const {
  return raw_ptr()->value_[2];
}


float Float32x4::w() const {
  return raw_ptr()->value_[3];
}


const char* Float32x4::ToCString() const {
  const char* kFormat = "[%f, %f, %f, %f]";
  float _x = x();
  float _y = y();
  float _z = z();
  float _w = w();
  // Calculate the size of the string.
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, _x, _y, _z, _w) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, _x, _y, _z, _w);
  return chars;
}


void Float32x4::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawInt32x4* Int32x4::New(int32_t v0, int32_t v1, int32_t v2, int32_t v3,
                         Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->int32x4_class() !=
         Class::null());
  Int32x4& result = Int32x4::Handle();
  {
    RawObject* raw = Object::Allocate(Int32x4::kClassId,
                                      Int32x4::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_x(v0);
  result.set_y(v1);
  result.set_z(v2);
  result.set_w(v3);
  return result.raw();
}


RawInt32x4* Int32x4::New(simd128_value_t value, Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->int32x4_class() !=
         Class::null());
  Int32x4& result = Int32x4::Handle();
  {
    RawObject* raw = Object::Allocate(Int32x4::kClassId,
                                      Int32x4::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_value(value);
  return result.raw();
}


void Int32x4::set_x(int32_t value) const {
  raw_ptr()->value_[0] = value;
}


void Int32x4::set_y(int32_t value) const {
  raw_ptr()->value_[1] = value;
}


void Int32x4::set_z(int32_t value) const {
  raw_ptr()->value_[2] = value;
}


void Int32x4::set_w(int32_t value) const {
  raw_ptr()->value_[3] = value;
}


int32_t Int32x4::x() const {
  return raw_ptr()->value_[0];
}


int32_t Int32x4::y() const {
  return raw_ptr()->value_[1];
}


int32_t Int32x4::z() const {
  return raw_ptr()->value_[2];
}


int32_t Int32x4::w() const {
  return raw_ptr()->value_[3];
}


simd128_value_t Int32x4::value() const {
  return simd128_value_t().readFrom(&raw_ptr()->value_[0]);
}


void Int32x4::set_value(simd128_value_t value) const {
  value.writeTo(&raw_ptr()->value_[0]);
}


const char* Int32x4::ToCString() const {
  const char* kFormat = "[%08x, %08x, %08x, %08x]";
  int32_t _x = x();
  int32_t _y = y();
  int32_t _z = z();
  int32_t _w = w();
  // Calculate the size of the string.
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, _x, _y, _z, _w) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, _x, _y, _z, _w);
  return chars;
}


void Int32x4::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


const intptr_t TypedData::element_size[] = {
  1,   // kTypedDataInt8ArrayCid.
  1,   // kTypedDataUint8ArrayCid.
  1,   // kTypedDataUint8ClampedArrayCid.
  2,   // kTypedDataInt16ArrayCid.
  2,   // kTypedDataUint16ArrayCid.
  4,   // kTypedDataInt32ArrayCid.
  4,   // kTypedDataUint32ArrayCid.
  8,   // kTypedDataInt64ArrayCid.
  8,   // kTypedDataUint64ArrayCid.
  4,   // kTypedDataFloat32ArrayCid.
  8,   // kTypedDataFloat64ArrayCid.
  16,  // kTypedDataFloat32x4ArrayCid.
  16,  // kTypedDataInt32x4ArrayCid.
};


RawTypedData* TypedData::New(intptr_t class_id,
                             intptr_t len,
                             Heap::Space space) {
  if (len < 0 || len > TypedData::MaxElements(class_id)) {
    FATAL1("Fatal error in TypedData::New: invalid len %" Pd "\n", len);
  }
  TypedData& result = TypedData::Handle();
  {
    intptr_t lengthInBytes = len * ElementSizeInBytes(class_id);
    RawObject* raw = Object::Allocate(class_id,
                                      TypedData::InstanceSize(lengthInBytes),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(len);
    if (len > 0) {
      memset(result.DataAddr(0), 0, lengthInBytes);
    }
  }
  return result.raw();
}


const char* TypedData::ToCString() const {
  return "TypedData";
}


void TypedData::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


FinalizablePersistentHandle* ExternalTypedData::AddFinalizer(
    void* peer, Dart_WeakPersistentHandleFinalizer callback) const {
  SetPeer(peer);
  return dart::AddFinalizer(*this, peer, callback);
}


RawExternalTypedData* ExternalTypedData::New(intptr_t class_id,
                                             uint8_t* data,
                                             intptr_t len,
                                             Heap::Space space) {
  ExternalTypedData& result = ExternalTypedData::Handle();
  {
    RawObject* raw = Object::Allocate(class_id,
                                      ExternalTypedData::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.SetLength(len);
    result.SetData(data);
  }
  return result.raw();
}


const char* ExternalTypedData::ToCString() const {
  return "ExternalTypedData";
}


void ExternalTypedData::PrintToJSONStream(JSONStream* stream,
                                          bool ref) const {
  JSONObject jsobj(stream);
}



const char* Closure::ToCString(const Instance& closure) {
  const Function& fun = Function::Handle(Closure::function(closure));
  const bool is_implicit_closure = fun.IsImplicitClosureFunction();
  const char* fun_sig = String::Handle(fun.UserVisibleSignature()).ToCString();
  const char* from = is_implicit_closure ? " from " : "";
  const char* fun_desc = is_implicit_closure ? fun.ToCString() : "";
  const char* format = "Closure: %s%s%s";
  intptr_t len = OS::SNPrint(NULL, 0, format, fun_sig, from, fun_desc) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, format, fun_sig, from, fun_desc);
  return chars;
}


RawInstance* Closure::New(const Function& function,
                          const Context& context,
                          Heap::Space space) {
  Isolate* isolate = Isolate::Current();
  ASSERT(context.isolate() == isolate);

  const Class& cls = Class::Handle(function.signature_class());
  ASSERT(cls.instance_size() == Closure::InstanceSize());
  Instance& result = Instance::Handle();
  {
    RawObject* raw = Object::Allocate(cls.id(), Closure::InstanceSize(), space);
    NoGCScope no_gc;
    result ^= raw;
  }
  Closure::set_function(result, function);
  Closure::set_context(result, context);
  return result.raw();
}


intptr_t Stacktrace::Length() const {
  const Array& code_array = Array::Handle(raw_ptr()->code_array_);
  return code_array.Length();
}


RawFunction* Stacktrace::FunctionAtFrame(intptr_t frame_index) const {
  const Code& code = Code::Handle(CodeAtFrame(frame_index));
  return code.IsNull() ? Function::null() : code.function();
}


RawCode* Stacktrace::CodeAtFrame(intptr_t frame_index) const {
  const Array& code_array = Array::Handle(raw_ptr()->code_array_);
  return reinterpret_cast<RawCode*>(code_array.At(frame_index));
}


void Stacktrace::SetCodeAtFrame(intptr_t frame_index,
                                const Code& code) const {
  const Array& code_array = Array::Handle(raw_ptr()->code_array_);
  code_array.SetAt(frame_index, code);
}


RawSmi* Stacktrace::PcOffsetAtFrame(intptr_t frame_index) const {
  const Array& pc_offset_array = Array::Handle(raw_ptr()->pc_offset_array_);
  return reinterpret_cast<RawSmi*>(pc_offset_array.At(frame_index));
}


void Stacktrace::SetPcOffsetAtFrame(intptr_t frame_index,
                                    const Smi& pc_offset) const {
  const Array& pc_offset_array = Array::Handle(raw_ptr()->pc_offset_array_);
  pc_offset_array.SetAt(frame_index, pc_offset);
}


void Stacktrace::set_code_array(const Array& code_array) const {
  StorePointer(&raw_ptr()->code_array_, code_array.raw());
}


void Stacktrace::set_pc_offset_array(const Array& pc_offset_array) const {
  StorePointer(&raw_ptr()->pc_offset_array_, pc_offset_array.raw());
}


void Stacktrace::set_catch_code_array(const Array& code_array) const {
  StorePointer(&raw_ptr()->catch_code_array_, code_array.raw());
}


void Stacktrace::set_catch_pc_offset_array(const Array& pc_offset_array) const {
  StorePointer(&raw_ptr()->catch_pc_offset_array_, pc_offset_array.raw());
}


void Stacktrace::set_expand_inlined(bool value) const {
  raw_ptr()->expand_inlined_ = value;
}


bool Stacktrace::expand_inlined() const {
  return raw_ptr()->expand_inlined_;
}


RawStacktrace* Stacktrace::New(const Array& code_array,
                               const Array& pc_offset_array,
                               Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->stacktrace_class() !=
         Class::null());
  Stacktrace& result = Stacktrace::Handle();
  {
    RawObject* raw = Object::Allocate(Stacktrace::kClassId,
                                      Stacktrace::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_code_array(code_array);
  result.set_pc_offset_array(pc_offset_array);
  result.SetCatchStacktrace(Object::empty_array(),
                            Object::empty_array());
  result.set_expand_inlined(true);  // default.
  return result.raw();
}


void Stacktrace::Append(const Array& code_list,
                        const Array& pc_offset_list) const {
  intptr_t old_length = Length();
  intptr_t new_length = old_length + pc_offset_list.Length();
  ASSERT(pc_offset_list.Length() == code_list.Length());

  // Grow the arrays for function, code and pc_offset triplet to accommodate
  // the new stack frames.
  Array& code_array = Array::Handle(raw_ptr()->code_array_);
  Array& pc_offset_array = Array::Handle(raw_ptr()->pc_offset_array_);
  code_array = Array::Grow(code_array, new_length);
  pc_offset_array = Array::Grow(pc_offset_array, new_length);
  set_code_array(code_array);
  set_pc_offset_array(pc_offset_array);
  // Now append the new function and code list to the existing arrays.
  intptr_t j = 0;
  Object& obj = Object::Handle();
  for (intptr_t i = old_length; i < new_length; i++, j++) {
    obj = code_list.At(j);
    code_array.SetAt(i, obj);
    obj = pc_offset_list.At(j);
    pc_offset_array.SetAt(i, obj);
  }
}


void Stacktrace::SetCatchStacktrace(const Array& code_array,
                                    const Array& pc_offset_array) const {
  StorePointer(&raw_ptr()->catch_code_array_, code_array.raw());
  StorePointer(&raw_ptr()->catch_pc_offset_array_, pc_offset_array.raw());
}


RawString* Stacktrace::FullStacktrace() const {
  const Array& code_array = Array::Handle(raw_ptr()->catch_code_array_);
  intptr_t idx = 0;
  if (!code_array.IsNull() && (code_array.Length() > 0)) {
    const Array& pc_offset_array =
        Array::Handle(raw_ptr()->catch_pc_offset_array_);
    const Stacktrace& catch_trace = Stacktrace::Handle(
        Stacktrace::New(code_array, pc_offset_array));
    const String& throw_string =
        String::Handle(String::New(ToCStringInternal(&idx)));
    const String& catch_string =
        String::Handle(String::New(catch_trace.ToCStringInternal(&idx)));
    return String::Concat(throw_string, catch_string);
  }
  return String::New(ToCStringInternal(&idx));
}


const char* Stacktrace::ToCString() const {
  const String& trace = String::Handle(FullStacktrace());
  return trace.ToCString();
}


void Stacktrace::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


static intptr_t PrintOneStacktrace(Isolate* isolate,
                                   GrowableArray<char*>* frame_strings,
                                   uword pc,
                                   const Function& function,
                                   const Code& code,
                                   intptr_t frame_index) {
  const char* kFormatWithCol = "#%-6d %s (%s:%d:%d)\n";
  const char* kFormatNoCol = "#%-6d %s (%s:%d)\n";
  const intptr_t token_pos = code.GetTokenIndexOfPC(pc);
  const Script& script = Script::Handle(isolate, function.script());
  const String& function_name =
      String::Handle(isolate, function.QualifiedUserVisibleName());
  const String& url = String::Handle(isolate, script.url());
  intptr_t line = -1;
  intptr_t column = -1;
  if (token_pos >= 0) {
    if (script.HasSource()) {
      script.GetTokenLocation(token_pos, &line, &column);
    } else {
      script.GetTokenLocation(token_pos, &line, NULL);
    }
  }
  intptr_t len = 0;
  char* chars = NULL;
  if (column >= 0) {
    len = OS::SNPrint(NULL, 0, kFormatWithCol,
                      frame_index, function_name.ToCString(),
                      url.ToCString(), line, column);
    chars = isolate->current_zone()->Alloc<char>(len + 1);
    OS::SNPrint(chars, (len + 1), kFormatWithCol,
                frame_index,
                function_name.ToCString(),
                url.ToCString(), line, column);
  } else {
    len = OS::SNPrint(NULL, 0, kFormatNoCol,
                      frame_index, function_name.ToCString(),
                      url.ToCString(), line);
    chars = isolate->current_zone()->Alloc<char>(len + 1);
    OS::SNPrint(chars, (len + 1), kFormatNoCol,
                frame_index, function_name.ToCString(),
                url.ToCString(), line);
  }
  frame_strings->Add(chars);
  return len;
}


const char* Stacktrace::ToCStringInternal(intptr_t* frame_index) const {
  Isolate* isolate = Isolate::Current();
  Function& function = Function::Handle();
  Code& code = Code::Handle();
  // Iterate through the stack frames and create C string description
  // for each frame.
  intptr_t total_len = 0;
  GrowableArray<char*> frame_strings;
  for (intptr_t i = 0; i < Length(); i++) {
    function = FunctionAtFrame(i);
    if (function.IsNull()) {
      // Check if null function object indicates a stack trace overflow.
      if ((i < (Length() - 1)) &&
          (FunctionAtFrame(i + 1) != Function::null())) {
        const char* kTruncated = "...\n...\n";
        intptr_t truncated_len = strlen(kTruncated) + 1;
        char* chars = isolate->current_zone()->Alloc<char>(truncated_len);
        OS::SNPrint(chars, truncated_len, "%s", kTruncated);
        frame_strings.Add(chars);
      }
    } else if (function.is_visible() || FLAG_verbose_stacktrace) {
      code = CodeAtFrame(i);
      ASSERT(function.raw() == code.function());
      uword pc = code.EntryPoint() + Smi::Value(PcOffsetAtFrame(i));
      if (code.is_optimized() && expand_inlined()) {
        // Traverse inlined frames.
        for (InlinedFunctionsIterator it(code, pc); !it.Done(); it.Advance()) {
          function = it.function();
          code = it.code();
          ASSERT(function.raw() == code.function());
          uword pc = it.pc();
          ASSERT(pc != 0);
          ASSERT(code.EntryPoint() <= pc);
          ASSERT(pc < (code.EntryPoint() + code.Size()));
          total_len += PrintOneStacktrace(
              isolate, &frame_strings, pc, function, code, *frame_index);
          (*frame_index)++;  // To account for inlined frames.
        }
      } else {
        total_len += PrintOneStacktrace(
            isolate, &frame_strings, pc, function, code, *frame_index);
        (*frame_index)++;
      }
    }
  }

  // Now concatenate the frame descriptions into a single C string.
  char* chars = isolate->current_zone()->Alloc<char>(total_len + 1);
  intptr_t index = 0;
  for (intptr_t i = 0; i < frame_strings.length(); i++) {
    index += OS::SNPrint((chars + index),
                         (total_len + 1 - index),
                         "%s",
                         frame_strings[i]);
  }
  chars[total_len] = '\0';
  return chars;
}


void JSRegExp::set_pattern(const String& pattern) const {
  StorePointer(&raw_ptr()->pattern_, pattern.raw());
}


void JSRegExp::set_num_bracket_expressions(intptr_t value) const {
  raw_ptr()->num_bracket_expressions_ = Smi::New(value);
}


RawJSRegExp* JSRegExp::New(intptr_t len, Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->jsregexp_class() !=
         Class::null());
  if (len < 0 || len > kMaxElements) {
    // This should be caught before we reach here.
    FATAL1("Fatal error in JSRegexp::New: invalid len %" Pd "\n", len);
  }
  JSRegExp& result = JSRegExp::Handle();
  {
    RawObject* raw = Object::Allocate(JSRegExp::kClassId,
                                      JSRegExp::InstanceSize(len),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
    result.set_type(kUnitialized);
    result.set_flags(0);
    result.SetLength(len);
  }
  return result.raw();
}


void* JSRegExp::GetDataStartAddress() const {
  intptr_t addr = reinterpret_cast<intptr_t>(raw_ptr());
  return reinterpret_cast<void*>(addr + sizeof(RawJSRegExp));
}


RawJSRegExp* JSRegExp::FromDataStartAddress(void* data) {
  JSRegExp& regexp = JSRegExp::Handle();
  intptr_t addr = reinterpret_cast<intptr_t>(data) - sizeof(RawJSRegExp);
  regexp ^= RawObject::FromAddr(addr);
  return regexp.raw();
}


const char* JSRegExp::Flags() const {
  switch (raw_ptr()->flags_) {
    case kGlobal | kIgnoreCase | kMultiLine :
    case kIgnoreCase | kMultiLine :
      return "im";
    case kGlobal | kIgnoreCase :
    case kIgnoreCase:
      return "i";
    case kGlobal | kMultiLine :
    case kMultiLine:
      return "m";
    default:
      break;
  }
  return "";
}


bool JSRegExp::Equals(const Instance& other) const {
  if (this->raw() == other.raw()) {
    return true;  // "===".
  }
  if (other.IsNull() || !other.IsJSRegExp()) {
    return false;
  }
  const JSRegExp& other_js = JSRegExp::Cast(other);
  // Match the pattern.
  const String& str1 = String::Handle(pattern());
  const String& str2 = String::Handle(other_js.pattern());
  if (!str1.Equals(str2)) {
    return false;
  }
  // Match the flags.
  if ((is_global() != other_js.is_global()) ||
      (is_ignore_case() != other_js.is_ignore_case()) ||
      (is_multi_line() != other_js.is_multi_line())) {
    return false;
  }
  return true;
}


const char* JSRegExp::ToCString() const {
  const String& str = String::Handle(pattern());
  const char* format = "JSRegExp: pattern=%s flags=%s";
  intptr_t len = OS::SNPrint(NULL, 0, format, str.ToCString(), Flags());
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len + 1);
  OS::SNPrint(chars, (len + 1), format, str.ToCString(), Flags());
  return chars;
}


void JSRegExp::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


RawWeakProperty* WeakProperty::New(Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->weak_property_class()
         != Class::null());
  RawObject* raw = Object::Allocate(WeakProperty::kClassId,
                                    WeakProperty::InstanceSize(),
                                    space);
  return reinterpret_cast<RawWeakProperty*>(raw);
}


const char* WeakProperty::ToCString() const {
  return "_WeakProperty";
}


void WeakProperty::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}

RawAbstractType* MirrorReference::GetAbstractTypeReferent() const {
  ASSERT(Object::Handle(referent()).IsAbstractType());
  return AbstractType::Cast(Object::Handle(referent())).raw();
}


RawClass* MirrorReference::GetClassReferent() const {
  ASSERT(Object::Handle(referent()).IsClass());
  return Class::Cast(Object::Handle(referent())).raw();
}


RawField* MirrorReference::GetFieldReferent() const {
  ASSERT(Object::Handle(referent()).IsField());
  return Field::Cast(Object::Handle(referent())).raw();
}


RawFunction* MirrorReference::GetFunctionReferent() const {
  ASSERT(Object::Handle(referent()).IsFunction());
  return Function::Cast(Object::Handle(referent())).raw();
}


RawLibrary* MirrorReference::GetLibraryReferent() const {
  ASSERT(Object::Handle(referent()).IsLibrary());
  return Library::Cast(Object::Handle(referent())).raw();
}


RawTypeParameter* MirrorReference::GetTypeParameterReferent() const {
  ASSERT(Object::Handle(referent()).IsTypeParameter());
  return TypeParameter::Cast(Object::Handle(referent())).raw();
}


RawMirrorReference* MirrorReference::New(const Object& referent,
                                         Heap::Space space) {
  ASSERT(Isolate::Current()->object_store()->mirror_reference_class()
         != Class::null());
  MirrorReference& result = MirrorReference::Handle();
  {
    RawObject* raw = Object::Allocate(MirrorReference::kClassId,
                                      MirrorReference::InstanceSize(),
                                      space);
    NoGCScope no_gc;
    result ^= raw;
  }
  result.set_referent(referent);
  return result.raw();
}


const char* MirrorReference::ToCString() const {
  return "_MirrorReference";
}


void MirrorReference::PrintToJSONStream(JSONStream* stream, bool ref) const {
  JSONObject jsobj(stream);
}


}  // namespace dart
