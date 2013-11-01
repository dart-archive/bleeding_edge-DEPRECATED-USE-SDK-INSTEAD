// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_OBJECT_H_
#define VM_OBJECT_H_

#include "include/dart_api.h"
#include "platform/assert.h"
#include "platform/utils.h"
#include "vm/json_stream.h"
#include "vm/bitmap.h"
#include "vm/dart.h"
#include "vm/globals.h"
#include "vm/handles.h"
#include "vm/heap.h"
#include "vm/isolate.h"
#include "vm/os.h"
#include "vm/raw_object.h"
#include "vm/scanner.h"

namespace dart {

// Forward declarations.
#define DEFINE_FORWARD_DECLARATION(clazz)                                      \
  class clazz;
CLASS_LIST(DEFINE_FORWARD_DECLARATION)
#undef DEFINE_FORWARD_DECLARATION
class Api;
class ArgumentsDescriptor;
class Assembler;
class Closure;
class Code;
class DeoptInstr;
class FinalizablePersistentHandle;
class LocalScope;
class ReusableHandleScope;
class ReusableObjectHandleScope;
class Symbols;

#if defined(DEBUG)
#define CHECK_HANDLE() CheckHandle();
#else
#define CHECK_HANDLE()
#endif

#define BASE_OBJECT_IMPLEMENTATION(object, super)                              \
 public:  /* NOLINT */                                                         \
  Raw##object* raw() const { return reinterpret_cast<Raw##object*>(raw_); }    \
  bool Is##object() const { return true; }                                     \
  static object& Handle(Isolate* isolate, Raw##object* raw_ptr) {              \
    object* obj =                                                              \
        reinterpret_cast<object*>(VMHandles::AllocateHandle(isolate));         \
    initializeHandle(obj, raw_ptr);                                            \
    return *obj;                                                               \
  }                                                                            \
  static object& Handle() {                                                    \
    return Handle(Isolate::Current(), object::null());                         \
  }                                                                            \
  static object& Handle(Isolate* isolate) {                                    \
    return Handle(isolate, object::null());                                    \
  }                                                                            \
  static object& Handle(Raw##object* raw_ptr) {                                \
    return Handle(Isolate::Current(), raw_ptr);                                \
  }                                                                            \
  static object& CheckedHandle(Isolate* isolate, RawObject* raw_ptr) {         \
    object* obj =                                                              \
        reinterpret_cast<object*>(VMHandles::AllocateHandle(isolate));         \
    initializeHandle(obj, raw_ptr);                                            \
    if (!obj->Is##object()) {                                                  \
      FATAL2("Handle check failed: saw %s expected %s",                        \
             obj->ToCString(), #object);                                       \
    }                                                                          \
    return *obj;                                                               \
  }                                                                            \
  static object& CheckedHandle(RawObject* raw_ptr) {                           \
    return CheckedHandle(Isolate::Current(), raw_ptr);                         \
  }                                                                            \
  static object& ZoneHandle(Isolate* isolate, Raw##object* raw_ptr) {          \
    object* obj = reinterpret_cast<object*>(                                   \
        VMHandles::AllocateZoneHandle(isolate));                               \
    initializeHandle(obj, raw_ptr);                                            \
    return *obj;                                                               \
  }                                                                            \
  static object* ReadOnlyHandle() {                                            \
    object* obj = reinterpret_cast<object*>(                                   \
        Dart::AllocateReadOnlyHandle());                                       \
    initializeHandle(obj, object::null());                                     \
    return obj;                                                                \
  }                                                                            \
  static object& ZoneHandle() {                                                \
    return ZoneHandle(Isolate::Current(), object::null());                     \
  }                                                                            \
  static object& ZoneHandle(Raw##object* raw_ptr) {                            \
    return ZoneHandle(Isolate::Current(), raw_ptr);                            \
  }                                                                            \
  static object& CheckedZoneHandle(Isolate* isolate, RawObject* raw_ptr) {     \
    object* obj = reinterpret_cast<object*>(                                   \
        VMHandles::AllocateZoneHandle(isolate));                               \
    initializeHandle(obj, raw_ptr);                                            \
    if (!obj->Is##object()) {                                                  \
      FATAL2("Handle check failed: saw %s expected %s",                        \
             obj->ToCString(), #object);                                       \
    }                                                                          \
    return *obj;                                                               \
  }                                                                            \
  static object& CheckedZoneHandle(RawObject* raw_ptr) {                       \
    return CheckedZoneHandle(Isolate::Current(), raw_ptr);                     \
  }                                                                            \
  /* T::Cast cannot be applied to a null Object, because the object vtable */  \
  /* is not setup for type T, although some methods are supposed to work   */  \
  /* with null, for example Instance::Equals().                            */  \
  static const object& Cast(const Object& obj) {                               \
    ASSERT(obj.Is##object());                                                  \
    return reinterpret_cast<const object&>(obj);                               \
  }                                                                            \
  static Raw##object* RawCast(RawObject* raw) {                                \
    ASSERT(Object::Handle(raw).Is##object());                                  \
    return reinterpret_cast<Raw##object*>(raw);                                \
  }                                                                            \
  static Raw##object* null() {                                                 \
    return reinterpret_cast<Raw##object*>(Object::null());                     \
  }                                                                            \
  virtual const char* ToCString() const;                                       \
  /* Object is printed as JSON into stream. If ref is true only a header */    \
  /* with an object id is printed. If ref is false the object is fully   */    \
  /* printed.                                                            */    \
  virtual void PrintToJSONStream(JSONStream* stream, bool ref = true) const;   \
  virtual const char* JSONType(bool ref) const {                               \
    return ref ? "@"#object : ""#object;                                       \
  }                                                                            \
  static const ClassId kClassId = k##object##Cid;                              \
 private:  /* NOLINT */                                                        \
  /* Initialize the handle based on the raw_ptr in the presence of null. */    \
  static void initializeHandle(object* obj, RawObject* raw_ptr) {              \
    if (raw_ptr != Object::null()) {                                           \
      obj->SetRaw(raw_ptr);                                                    \
    } else {                                                                   \
      obj->raw_ = Object::null();                                              \
      object fake_object;                                                      \
      obj->set_vtable(fake_object.vtable());                                   \
    }                                                                          \
  }                                                                            \
  /* Disallow allocation, copy constructors and override super assignment. */  \
 public:  /* NOLINT */                                                         \
  void operator delete(void* pointer) {                                        \
    UNREACHABLE();                                                             \
  }                                                                            \
 private:  /* NOLINT */                                                        \
  void* operator new(size_t size);                                             \
  object(const object& value);                                                 \
  void operator=(Raw##super* value);                                           \
  void operator=(const object& value);                                         \
  void operator=(const super& value);                                          \

#define SNAPSHOT_READER_SUPPORT(object)                                        \
  static Raw##object* ReadFrom(SnapshotReader* reader,                         \
                               intptr_t object_id,                             \
                               intptr_t tags,                                  \
                               Snapshot::Kind);                                \
  friend class SnapshotReader;                                                 \

#define OBJECT_IMPLEMENTATION(object, super)                                   \
 public:  /* NOLINT */                                                         \
  void operator=(Raw##object* value) {                                         \
    initializeHandle(this, value);                                             \
  }                                                                            \
  void operator^=(RawObject* value) {                                          \
    initializeHandle(this, value);                                             \
    ASSERT(IsNull() || Is##object());                                          \
  }                                                                            \
 protected:  /* NOLINT */                                                      \
  object() : super() {}                                                        \
  BASE_OBJECT_IMPLEMENTATION(object, super)                                    \

#define HEAP_OBJECT_IMPLEMENTATION(object, super)                              \
  OBJECT_IMPLEMENTATION(object, super);                                        \
  Raw##object* raw_ptr() const {                                               \
    ASSERT(raw() != null());                                                   \
    return raw()->ptr();                                                       \
  }                                                                            \
  SNAPSHOT_READER_SUPPORT(object)                                              \
  friend class Isolate;                                                        \
  friend class StackFrame;                                                     \

// This macro is used to denote types that do not have a sub-type.
#define FINAL_HEAP_OBJECT_IMPLEMENTATION(object, super)                        \
 public:  /* NOLINT */                                                         \
  void operator=(Raw##object* value) {                                         \
    raw_ = value;                                                              \
    CHECK_HANDLE();                                                            \
  }                                                                            \
  void operator^=(RawObject* value) {                                          \
    raw_ = value;                                                              \
    CHECK_HANDLE();                                                            \
  }                                                                            \
 private:  /* NOLINT */                                                        \
  object() : super() {}                                                        \
  BASE_OBJECT_IMPLEMENTATION(object, super)                                    \
  Raw##object* raw_ptr() const {                                               \
    ASSERT(raw() != null());                                                   \
    return raw()->ptr();                                                       \
  }                                                                            \
  SNAPSHOT_READER_SUPPORT(object)                                              \
  friend class Isolate;                                                        \
  friend class StackFrame;                                                     \

class Object {
 public:
  virtual ~Object() { }

  RawObject* raw() const { return raw_; }
  void operator=(RawObject* value) {
    initializeHandle(this, value);
  }

  void set_tags(intptr_t value) const {
    // TODO(asiva): Remove the capability of setting tags in general. The mask
    // here only allows for canonical and from_snapshot flags to be set.
    ASSERT(!IsNull());
    uword tags = raw()->ptr()->tags_ & ~0x0000000c;
    raw()->ptr()->tags_ = tags | (value & 0x0000000c);
  }
  void SetCreatedFromSnapshot() const {
    ASSERT(!IsNull());
    raw()->SetCreatedFromSnapshot();
  }
  bool IsCanonical() const {
    ASSERT(!IsNull());
    return raw()->IsCanonical();
  }
  void SetCanonical() const {
    ASSERT(!IsNull());
    raw()->SetCanonical();
  }
  intptr_t GetClassId() const {
    return !raw()->IsHeapObject() ?
        static_cast<intptr_t>(kSmiCid) : raw()->GetClassId();
  }
  inline RawClass* clazz() const;
  static intptr_t tags_offset() { return OFFSET_OF(RawObject, tags_); }

  // Class testers.
#define DEFINE_CLASS_TESTER(clazz)                                             \
  virtual bool Is##clazz() const { return false; }
  CLASS_LIST_FOR_HANDLES(DEFINE_CLASS_TESTER);
#undef DEFINE_CLASS_TESTER

  bool IsNull() const { return raw_ == null_; }

  virtual const char* ToCString() const {
    if (IsNull()) {
      return "null";
    } else {
      return "Object";
    }
  }

  virtual void PrintToJSONStream(JSONStream* stream, bool ref = true) const {
    JSONObject jsobj(stream);
    jsobj.AddProperty("type", JSONType(ref));
  }

  virtual const char* JSONType(bool ref) const {
    return IsNull() ? "null" : "Object";
  }

  // Returns the name that is used to identify an object in the
  // namespace dictionary.
  // Object::DictionaryName() returns String::null(). Only subclasses
  // of Object that need to be entered in the library and library prefix
  // namespaces need to provide an implementation.
  virtual RawString* DictionaryName() const;

  bool IsNew() const { return raw()->IsNewObject(); }
  bool IsOld() const { return raw()->IsOldObject(); }
  bool InVMHeap() const {
#if defined(DEBUG)
    if (raw()->IsVMHeapObject()) {
      Heap* vm_isolate_heap = Dart::vm_isolate()->heap();
      ASSERT(vm_isolate_heap->Contains(RawObject::ToAddr(raw())));
    }
#endif
    return raw()->IsVMHeapObject();
  }

  // Print the object on stdout for debugging.
  void Print() const;

  bool IsZoneHandle() const {
    return VMHandles::IsZoneHandle(reinterpret_cast<uword>(this));
  }

  bool IsReadOnlyHandle() const;

  bool IsNotTemporaryScopedHandle() const;

  static RawObject* Clone(const Object& src, Heap::Space space = Heap::kNew);

  static Object& Handle(Isolate* isolate, RawObject* raw_ptr) {
    Object* obj = reinterpret_cast<Object*>(VMHandles::AllocateHandle(isolate));
    initializeHandle(obj, raw_ptr);
    return *obj;
  }
  static Object* ReadOnlyHandle() {
    Object* obj = reinterpret_cast<Object*>(
        Dart::AllocateReadOnlyHandle());
    initializeHandle(obj, Object::null());
    return obj;
  }

  static Object& Handle() {
    return Handle(Isolate::Current(), null_);
  }

  static Object& Handle(Isolate* isolate) {
    return Handle(isolate, null_);
  }

  static Object& Handle(RawObject* raw_ptr) {
    return Handle(Isolate::Current(), raw_ptr);
  }

  static Object& ZoneHandle(Isolate* isolate, RawObject* raw_ptr) {
    Object* obj = reinterpret_cast<Object*>(
        VMHandles::AllocateZoneHandle(isolate));
    initializeHandle(obj, raw_ptr);
    return *obj;
  }

  static Object& ZoneHandle() {
    return ZoneHandle(Isolate::Current(), null_);
  }

  static Object& ZoneHandle(RawObject* raw_ptr) {
    return ZoneHandle(Isolate::Current(), raw_ptr);
  }

  static RawObject* null() { return null_; }

  static const Object& null_object() {
    ASSERT(null_object_ != NULL);
    return *null_object_;
  }
  static const Array& null_array() {
    ASSERT(null_array_ != NULL);
    return *null_array_;
  }
  static const String& null_string() {
    ASSERT(null_string_ != NULL);
    return *null_string_;
  }
  static const Instance& null_instance() {
    ASSERT(null_instance_ != NULL);
    return *null_instance_;
  }
  static const AbstractTypeArguments& null_abstract_type_arguments() {
    ASSERT(null_abstract_type_arguments_ != NULL);
    return *null_abstract_type_arguments_;
  }

  static const Array& empty_array() {
    ASSERT(empty_array_ != NULL);
    return *empty_array_;
  }

  // The sentinel is a value that cannot be produced by Dart code.
  // It can be used to mark special values, for example to distinguish
  // "uninitialized" fields.
  static const Instance& sentinel() {
    ASSERT(sentinel_ != NULL);
    return *sentinel_;
  }
  // Value marking that we are transitioning from sentinel, e.g., computing
  // a field value. Used to detect circular initialization.
  static const Instance& transition_sentinel() {
    ASSERT(transition_sentinel_ != NULL);
    return *transition_sentinel_;
  }

  // Compiler's constant propagation constants.
  static const Instance& unknown_constant() {
    ASSERT(unknown_constant_ != NULL);
    return *unknown_constant_;
  }
  static const Instance& non_constant() {
    ASSERT(non_constant_ != NULL);
    return *non_constant_;
  }

  static const Bool& bool_true() {
    ASSERT(bool_true_ != NULL);
    return *bool_true_;
  }
  static const Bool& bool_false() {
    ASSERT(bool_false_ != NULL);
    return *bool_false_;
  }

  static const Smi& smi_illegal_cid() {
    ASSERT(smi_illegal_cid_ != NULL);
    return *smi_illegal_cid_;
  }
  static const LanguageError& snapshot_writer_error() {
    ASSERT(snapshot_writer_error_ != NULL);
    return *snapshot_writer_error_;
  }

  static const LanguageError& branch_offset_error() {
    ASSERT(branch_offset_error_ != NULL);
    return *branch_offset_error_;
  }

  static RawClass* class_class() { return class_class_; }
  static RawClass* dynamic_class() { return dynamic_class_; }
  static RawClass* void_class() { return void_class_; }
  static RawType* dynamic_type() { return dynamic_type_; }
  static RawType* void_type() { return void_type_; }
  static RawClass* unresolved_class_class() { return unresolved_class_class_; }
  static RawClass* type_arguments_class() { return type_arguments_class_; }
  static RawClass* instantiated_type_arguments_class() {
      return instantiated_type_arguments_class_;
  }
  static RawClass* patch_class_class() { return patch_class_class_; }
  static RawClass* function_class() { return function_class_; }
  static RawClass* closure_data_class() { return closure_data_class_; }
  static RawClass* redirection_data_class() { return redirection_data_class_; }
  static RawClass* field_class() { return field_class_; }
  static RawClass* literal_token_class() { return literal_token_class_; }
  static RawClass* token_stream_class() { return token_stream_class_; }
  static RawClass* script_class() { return script_class_; }
  static RawClass* library_class() { return library_class_; }
  static RawClass* library_prefix_class() { return library_prefix_class_; }
  static RawClass* namespace_class() { return namespace_class_; }
  static RawClass* code_class() { return code_class_; }
  static RawClass* instructions_class() { return instructions_class_; }
  static RawClass* pc_descriptors_class() { return pc_descriptors_class_; }
  static RawClass* stackmap_class() { return stackmap_class_; }
  static RawClass* var_descriptors_class() { return var_descriptors_class_; }
  static RawClass* exception_handlers_class() {
    return exception_handlers_class_;
  }
  static RawClass* deopt_info_class() { return deopt_info_class_; }
  static RawClass* context_class() { return context_class_; }
  static RawClass* context_scope_class() { return context_scope_class_; }
  static RawClass* api_error_class() { return api_error_class_; }
  static RawClass* language_error_class() { return language_error_class_; }
  static RawClass* unhandled_exception_class() {
    return unhandled_exception_class_;
  }
  static RawClass* unwind_error_class() { return unwind_error_class_; }
  static RawClass* icdata_class() { return icdata_class_; }
  static RawClass* megamorphic_cache_class() {
    return megamorphic_cache_class_;
  }
  static RawClass* subtypetestcache_class() { return subtypetestcache_class_; }

  static RawError* Init(Isolate* isolate);
  static void InitFromSnapshot(Isolate* isolate);
  static void InitOnce();
  static void RegisterSingletonClassNames();
  static void CreateInternalMetaData();
  static void MakeUnusedSpaceTraversable(const Object& obj,
                                         intptr_t original_size,
                                         intptr_t used_size);

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawObject));
  }

  static void VerifyBuiltinVtables();

  static const ClassId kClassId = kObjectCid;

  // Different kinds of type tests.
  enum TypeTestKind {
    kIsSubtypeOf = 0,
    kIsMoreSpecificThan
  };

  // Different kinds of name visibility.
  enum NameVisibility {
    kInternalName = 0,
    kUserVisibleName
  };

 protected:
  // Used for extracting the C++ vtable during bringup.
  Object() : raw_(null_) {}

  uword raw_value() const {
    return reinterpret_cast<uword>(raw());
  }

  inline void SetRaw(RawObject* value);
  void CheckHandle() const;

  cpp_vtable vtable() const { return bit_copy<cpp_vtable>(*this); }
  void set_vtable(cpp_vtable value) { *vtable_address() = value; }

  static RawObject* Allocate(intptr_t cls_id,
                             intptr_t size,
                             Heap::Space space);

  static intptr_t RoundedAllocationSize(intptr_t size) {
    return Utils::RoundUp(size, kObjectAlignment);
  }

  bool Contains(uword addr) const {
    intptr_t this_size = raw()->Size();
    uword this_addr = RawObject::ToAddr(raw());
    return (addr >= this_addr) && (addr < (this_addr + this_size));
  }

  template<typename type> void StorePointer(type* addr, type value) const {
    // Ensure that this object contains the addr.
    ASSERT(Contains(reinterpret_cast<uword>(addr)));
    *addr = value;
    // Filter stores based on source and target.
    if (!value->IsHeapObject()) return;
    if (value->IsNewObject() && raw()->IsOldObject() &&
        !raw()->IsRemembered()) {
      raw()->SetRememberedBit();
      Isolate::Current()->store_buffer()->AddObject(raw());
    }
  }

  RawObject* raw_;  // The raw object reference.

 private:
  static void InitializeObject(uword address, intptr_t id, intptr_t size);

  static void RegisterClass(const Class& cls,
                            const String& name,
                            const Library& lib);
  static void RegisterPrivateClass(const Class& cls,
                                   const String& name,
                                   const Library& lib);

  /* Initialize the handle based on the raw_ptr in the presence of null. */
  static void initializeHandle(Object* obj, RawObject* raw_ptr) {
    if (raw_ptr != Object::null()) {
      obj->SetRaw(raw_ptr);
    } else {
      obj->raw_ = Object::null();
      Object fake_object;
      obj->set_vtable(fake_object.vtable());
    }
  }

  cpp_vtable* vtable_address() const {
    uword vtable_addr = reinterpret_cast<uword>(this);
    return reinterpret_cast<cpp_vtable*>(vtable_addr);
  }

  static cpp_vtable handle_vtable_;
  static cpp_vtable builtin_vtables_[kNumPredefinedCids];

  // The static values below are singletons shared between the different
  // isolates. They are all allocated in the non-GC'd Dart::vm_isolate_.
  static RawObject* null_;

  static RawClass* class_class_;  // Class of the Class vm object.
  static RawClass* dynamic_class_;  // Class of the 'dynamic' type.
  static RawClass* void_class_;  // Class of the 'void' type.
  static RawType* dynamic_type_;  // Class of the 'dynamic' type.
  static RawType* void_type_;  // Class of the 'void' type.
  static RawClass* unresolved_class_class_;  // Class of UnresolvedClass.
  // Class of the TypeArguments vm object.
  static RawClass* type_arguments_class_;
  static RawClass* instantiated_type_arguments_class_;  // Class of Inst..ments.
  static RawClass* patch_class_class_;  // Class of the PatchClass vm object.
  static RawClass* function_class_;  // Class of the Function vm object.
  static RawClass* closure_data_class_;  // Class of ClosureData vm obj.
  static RawClass* redirection_data_class_;  // Class of RedirectionData vm obj.
  static RawClass* field_class_;  // Class of the Field vm object.
  static RawClass* literal_token_class_;  // Class of LiteralToken vm object.
  static RawClass* token_stream_class_;  // Class of the TokenStream vm object.
  static RawClass* script_class_;  // Class of the Script vm object.
  static RawClass* library_class_;  // Class of the Library vm object.
  static RawClass* library_prefix_class_;  // Class of Library prefix vm object.
  static RawClass* namespace_class_;  // Class of Namespace vm object.
  static RawClass* code_class_;  // Class of the Code vm object.
  static RawClass* instructions_class_;  // Class of the Instructions vm object.
  static RawClass* pc_descriptors_class_;  // Class of PcDescriptors vm object.
  static RawClass* stackmap_class_;  // Class of Stackmap vm object.
  static RawClass* var_descriptors_class_;  // Class of LocalVarDescriptors.
  static RawClass* exception_handlers_class_;  // Class of ExceptionHandlers.
  static RawClass* deopt_info_class_;  // Class of DeoptInfo.
  static RawClass* context_class_;  // Class of the Context vm object.
  static RawClass* context_scope_class_;  // Class of ContextScope vm object.
  static RawClass* icdata_class_;  // Class of ICData.
  static RawClass* megamorphic_cache_class_;  // Class of MegamorphiCache.
  static RawClass* subtypetestcache_class_;  // Class of SubtypeTestCache.
  static RawClass* api_error_class_;  // Class of ApiError.
  static RawClass* language_error_class_;  // Class of LanguageError.
  static RawClass* unhandled_exception_class_;  // Class of UnhandledException.
  static RawClass* unwind_error_class_;  // Class of UnwindError.

  // The static values below are read-only handle pointers for singleton
  // objects that are shared between the different isolates.
  static Object* null_object_;
  static Array* null_array_;
  static String* null_string_;
  static Instance* null_instance_;
  static AbstractTypeArguments* null_abstract_type_arguments_;
  static Array* empty_array_;
  static Instance* sentinel_;
  static Instance* transition_sentinel_;
  static Instance* unknown_constant_;
  static Instance* non_constant_;
  static Bool* bool_true_;
  static Bool* bool_false_;
  static Smi* smi_illegal_cid_;
  static LanguageError* snapshot_writer_error_;
  static LanguageError* branch_offset_error_;

  friend void ClassTable::Register(const Class& cls);
  friend void RawObject::Validate(Isolate* isolate) const;
  friend class Closure;
  friend class SnapshotReader;
  friend class OneByteString;
  friend class TwoByteString;
  friend class ExternalOneByteString;
  friend class ExternalTwoByteString;
  friend class Isolate;
  friend class ReusableHandleScope;
  friend class ReusableObjectHandleScope;

  DISALLOW_ALLOCATION();
  DISALLOW_COPY_AND_ASSIGN(Object);
};


class Class : public Object {
 public:
  intptr_t instance_size() const {
    ASSERT(is_finalized() || is_prefinalized());
    return (raw_ptr()->instance_size_in_words_ * kWordSize);
  }
  void set_instance_size(intptr_t value_in_bytes) const {
    ASSERT(kWordSize != 0);
    set_instance_size_in_words(value_in_bytes / kWordSize);
  }
  void set_instance_size_in_words(intptr_t value) const {
    ASSERT(Utils::IsAligned((value * kWordSize), kObjectAlignment));
    raw_ptr()->instance_size_in_words_ = value;
  }

  intptr_t next_field_offset() const {
    return raw_ptr()->next_field_offset_in_words_ * kWordSize;
  }
  void set_next_field_offset(intptr_t value_in_bytes) const {
    ASSERT(kWordSize != 0);
    set_next_field_offset_in_words(value_in_bytes / kWordSize);
  }
  void set_next_field_offset_in_words(intptr_t value) const {
    ASSERT((Utils::IsAligned((value * kWordSize), kObjectAlignment) &&
            (value == raw_ptr()->instance_size_in_words_)) ||
           (!Utils::IsAligned((value * kWordSize), kObjectAlignment) &&
            ((value + 1) == raw_ptr()->instance_size_in_words_)));
    raw_ptr()->next_field_offset_in_words_ = value;
  }

  cpp_vtable handle_vtable() const { return raw_ptr()->handle_vtable_; }
  void set_handle_vtable(cpp_vtable value) const {
    raw_ptr()->handle_vtable_ = value;
  }

  intptr_t id() const { return raw_ptr()->id_; }
  void set_id(intptr_t value) const {
    raw_ptr()->id_ = value;
  }

  RawString* Name() const;
  RawString* UserVisibleName() const;

  virtual RawString* DictionaryName() const { return Name(); }

  RawScript* script() const { return raw_ptr()->script_; }
  void set_script(const Script& value) const;

  intptr_t token_pos() const { return raw_ptr()->token_pos_; }
  void set_token_pos(intptr_t value) const;

  // This class represents the signature class of a closure function if
  // signature_function() is not null.
  // The associated function may be a closure function (with code) or a
  // signature function (without code) solely describing the result type and
  // parameter types of the signature.
  RawFunction* signature_function() const {
    return raw_ptr()->signature_function_;
  }
  static intptr_t signature_function_offset() {
    return OFFSET_OF(RawClass, signature_function_);
  }

  // Return the signature type of this signature class.
  // For example, if this class represents a signature of the form
  // 'F<T, R>(T, [b: B, c: C]) => R', then its signature type is a parameterized
  // type with this class as the type class and type parameters 'T' and 'R'
  // as its type argument vector.
  // SignatureType is used as the type of formal parameters representing a
  // function.
  RawType* SignatureType() const;

  // Return the Type with type parameters declared by this class filled in with
  // dynamic and type parameters declared in superclasses filled in as declared
  // in superclass clauses.
  RawAbstractType* RareType() const;

  // Return the Type whose arguments are the type parameters declared by this
  // class preceded by the type arguments declared for superclasses, etc.
  // e.g. given
  // class B<T, S>
  // class C<R> extends B<R, int>
  // C.DeclarationType() --> C [R, int, R]
  RawAbstractType* DeclarationType() const;

  RawLibrary* library() const { return raw_ptr()->library_; }
  void set_library(const Library& value) const;

  // The type parameters (and their bounds) are specified as an array of
  // TypeParameter.
  RawTypeArguments* type_parameters() const {
      return raw_ptr()->type_parameters_;
  }
  void set_type_parameters(const TypeArguments& value) const;
  intptr_t NumTypeParameters() const;
  static intptr_t type_parameters_offset() {
    return OFFSET_OF(RawClass, type_parameters_);
  }

  // Return a TypeParameter if the type_name is a type parameter of this class.
  // Return null otherwise.
  RawTypeParameter* LookupTypeParameter(const String& type_name) const;

  // The type argument vector is flattened and includes the type arguments of
  // the super class.
  intptr_t NumTypeArguments() const;

  // Return the number of type arguments that are specific to this class, i.e.
  // not overlapping with the type arguments of the super class of this class.
  intptr_t NumOwnTypeArguments() const;

  // If this class is parameterized, each instance has a type_arguments field.
  static const intptr_t kNoTypeArguments = -1;
  intptr_t type_arguments_field_offset() const {
    ASSERT(is_type_finalized() || is_prefinalized());
    if (raw_ptr()->type_arguments_field_offset_in_words_ == kNoTypeArguments) {
      return kNoTypeArguments;
    }
    return raw_ptr()->type_arguments_field_offset_in_words_ * kWordSize;
  }
  void set_type_arguments_field_offset(intptr_t value_in_bytes) const {
    intptr_t value;
    if (value_in_bytes == kNoTypeArguments) {
      value = kNoTypeArguments;
    } else {
      ASSERT(kWordSize != 0);
      value = value_in_bytes / kWordSize;
    }
    set_type_arguments_field_offset_in_words(value);
  }
  void set_type_arguments_field_offset_in_words(intptr_t value) const {
    raw_ptr()->type_arguments_field_offset_in_words_ = value;
  }
  static intptr_t type_arguments_field_offset_in_words_offset() {
    return OFFSET_OF(RawClass, type_arguments_field_offset_in_words_);
  }

  // The super type of this class, Object type if not explicitly specified.
  // Note that the super type may be bounded, as in this example:
  // class C<T> extends S<T> { }; class S<T extends num> { };
  RawAbstractType* super_type() const { return raw_ptr()->super_type_; }
  void set_super_type(const AbstractType& value) const;
  static intptr_t super_type_offset() {
    return OFFSET_OF(RawClass, super_type_);
  }

  // Asserts that the class of the super type has been resolved.
  RawClass* SuperClass() const;

  RawType* mixin() const { return raw_ptr()->mixin_; }
  void set_mixin(const Type& value) const;

  bool IsMixinApplication() const;
  bool IsAnonymousMixinApplication() const;

  RawClass* patch_class() const {
    return raw_ptr()->patch_class_;
  }
  void set_patch_class(const Class& patch_class) const;

  // Interfaces is an array of Types.
  RawArray* interfaces() const { return raw_ptr()->interfaces_; }
  void set_interfaces(const Array& value) const;
  static intptr_t interfaces_offset() {
    return OFFSET_OF(RawClass, interfaces_);
  }

  // Returns the list of classes having this class as direct superclass.
  RawGrowableObjectArray* direct_subclasses() const {
    return raw_ptr()->direct_subclasses_;
  }
  void AddDirectSubclass(const Class& subclass) const;
  // TODO(regis): Implement RemoveDirectSubclass for class unloading support.

  // Check if this class represents the class of null.
  bool IsNullClass() const { return id() == kNullCid; }

  // Check if this class represents the 'dynamic' class.
  bool IsDynamicClass() const { return id() == kDynamicCid; }

  // Check if this class represents the 'void' class.
  bool IsVoidClass() const { return id() == kVoidCid; }

  // Check if this class represents the 'Object' class.
  bool IsObjectClass() const { return id() == kInstanceCid; }

  // Check if this class represents the 'Function' class.
  bool IsFunctionClass() const;

  // Check if this class represents a signature class.
  bool IsSignatureClass() const {
    return signature_function() != Object::null();
  }
  static bool IsSignatureClass(RawClass* cls) {
    return cls->ptr()->signature_function_ != Object::null();
  }

  // Check if this class represents a canonical signature class, i.e. not an
  // alias as defined in a typedef.
  bool IsCanonicalSignatureClass() const;

  // Check the subtype relationship.
  bool IsSubtypeOf(const AbstractTypeArguments& type_arguments,
                   const Class& other,
                   const AbstractTypeArguments& other_type_arguments,
                   Error* bound_error) const {
    return TypeTest(kIsSubtypeOf,
                    type_arguments,
                    other,
                    other_type_arguments,
                    bound_error);
  }

  // Check the 'more specific' relationship.
  bool IsMoreSpecificThan(const AbstractTypeArguments& type_arguments,
                          const Class& other,
                          const AbstractTypeArguments& other_type_arguments,
                          Error* bound_error) const {
    return TypeTest(kIsMoreSpecificThan,
                    type_arguments,
                    other,
                    other_type_arguments,
                    bound_error);
  }

  // Check if this is the top level class.
  bool IsTopLevel() const;

  RawArray* fields() const { return raw_ptr()->fields_; }
  void SetFields(const Array& value) const;

  // Returns an array of all fields of this class and its superclasses indexed
  // by offset in words.
  RawArray* OffsetToFieldMap() const;

  // Returns true if non-static fields are defined.
  bool HasInstanceFields() const;

  RawArray* functions() const { return raw_ptr()->functions_; }
  void SetFunctions(const Array& value) const;
  void AddFunction(const Function& function) const;

  RawGrowableObjectArray* closures() const {
    return raw_ptr()->closure_functions_;
  }
  void AddClosureFunction(const Function& function) const;
  RawFunction* LookupClosureFunction(intptr_t token_pos) const;

  RawFunction* LookupDynamicFunction(const String& name) const;
  RawFunction* LookupDynamicFunctionAllowPrivate(const String& name) const;
  RawFunction* LookupStaticFunction(const String& name) const;
  RawFunction* LookupStaticFunctionAllowPrivate(const String& name) const;
  RawFunction* LookupConstructor(const String& name) const;
  RawFunction* LookupConstructorAllowPrivate(const String& name) const;
  RawFunction* LookupFactory(const String& name) const;
  RawFunction* LookupFunction(const String& name) const;
  RawFunction* LookupFunctionAllowPrivate(const String& name) const;
  RawFunction* LookupGetterFunction(const String& name) const;
  RawFunction* LookupSetterFunction(const String& name) const;
  RawFunction* LookupFunctionAtToken(intptr_t token_pos) const;
  RawField* LookupInstanceField(const String& name) const;
  RawField* LookupStaticField(const String& name) const;
  RawField* LookupField(const String& name) const;

  RawLibraryPrefix* LookupLibraryPrefix(const String& name) const;

  void InsertCanonicalConstant(intptr_t index, const Instance& constant) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawClass));
  }

  bool is_implemented() const {
    return ImplementedBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_implemented() const;

  bool is_abstract() const {
    return AbstractBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_abstract() const;

  bool is_type_finalized() const {
    return TypeFinalizedBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_type_finalized() const;

  bool is_patch() const {
    return PatchBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_patch() const;

  bool is_synthesized_class() const {
    return SynthesizedClassBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_synthesized_class() const;

  bool is_finalized() const {
    return ClassFinalizedBits::decode(raw_ptr()->state_bits_)
        == RawClass::kFinalized;
  }
  void set_is_finalized() const;

  bool is_prefinalized() const {
    return ClassFinalizedBits::decode(raw_ptr()->state_bits_)
        == RawClass::kPreFinalized;
  }

  void set_is_prefinalized() const;

  bool is_marked_for_parsing() const {
    return MarkedForParsingBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_marked_for_parsing() const;
  void reset_is_marked_for_parsing() const;

  bool is_const() const { return ConstBit::decode(raw_ptr()->state_bits_); }
  void set_is_const() const;

  bool is_mixin_typedef() const {
    return MixinTypedefBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_mixin_typedef() const;

  bool is_mixin_type_applied() const {
    return MixinTypeAppliedBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_mixin_type_applied() const;

  uint16_t num_native_fields() const {
    return raw_ptr()->num_native_fields_;
  }
  void set_num_native_fields(uint16_t value) const {
    raw_ptr()->num_native_fields_ = value;
  }

  RawCode* allocation_stub() const {
    return raw_ptr()->allocation_stub_;
  }
  void set_allocation_stub(const Code& value) const;

  RawArray* constants() const;

  RawFunction* GetInvocationDispatcher(const String& target_name,
                                       const Array& args_desc,
                                       RawFunction::Kind kind) const;

  void Finalize() const;

  // Apply given patch class to this class.
  // Return true on success, or false and error otherwise.
  bool ApplyPatch(const Class& patch, Error* error) const;

  // Evaluate the given expression as if it appeared in a static
  // method of this class and return the resulting value, or an
  // error object if evaluating the expression fails.
  RawObject* Evaluate(const String& expr) const;

  RawError* EnsureIsFinalized(Isolate* isolate) const;

  // Allocate a class used for VM internal objects.
  template <class FakeObject> static RawClass* New();

  // Allocate instance classes.
  static RawClass* New(const String& name,
                       const Script& script,
                       intptr_t token_pos);
  static RawClass* NewNativeWrapper(const Library& library,
                                    const String& name,
                                    int num_fields);

  // Allocate the raw string classes.
  static RawClass* NewStringClass(intptr_t class_id);

  // Allocate the raw TypedData classes.
  static RawClass* NewTypedDataClass(intptr_t class_id);

  // Allocate the raw TypedDataView classes.
  static RawClass* NewTypedDataViewClass(intptr_t class_id);

  // Allocate the raw ExternalTypedData classes.
  static RawClass* NewExternalTypedDataClass(intptr_t class_id);

  // Allocate a class representing a function signature described by
  // signature_function, which must be a closure function or a signature
  // function.
  // The class may be type parameterized unless the signature_function is in a
  // static scope. In that case, the type parameters are copied from the owner
  // class of signature_function.
  // A null signature function may be passed in and patched later. See below.
  static RawClass* NewSignatureClass(const String& name,
                                     const Function& signature_function,
                                     const Script& script,
                                     intptr_t token_pos);

  // Patch the signature function of a signature class allocated without it.
  void PatchSignatureFunction(const Function& signature_function) const;

  // Return a class object corresponding to the specified kind. If
  // a canonicalized version of it exists then that object is returned
  // otherwise a new object is allocated and returned.
  static RawClass* GetClass(intptr_t class_id, bool is_signature_class);

 private:
  enum {
    kAny = 0,
    kStatic,
    kInstance,
    kConstructor,
    kFactory,
  };
  enum {
    kConstBit = 0,
    kImplementedBit = 1,
    kTypeFinalizedBit = 2,
    kClassFinalizedBits = 3,
    kClassFinalizedSize = 2,
    kAbstractBit = 5,
    kPatchBit = 6,
    kSynthesizedClassBit = 7,
    kMarkedForParsingBit = 8,
    kMixinTypedefBit = 9,
    kMixinTypeAppliedBit = 10,
  };
  class ConstBit : public BitField<bool, kConstBit, 1> {};
  class ImplementedBit : public BitField<bool, kImplementedBit, 1> {};
  class TypeFinalizedBit : public BitField<bool, kTypeFinalizedBit, 1> {};
  class ClassFinalizedBits : public BitField<RawClass::ClassFinalizedState,
      kClassFinalizedBits, kClassFinalizedSize> {};  // NOLINT
  class AbstractBit : public BitField<bool, kAbstractBit, 1> {};
  class PatchBit : public BitField<bool, kPatchBit, 1> {};
  class SynthesizedClassBit : public BitField<bool, kSynthesizedClassBit, 1> {};
  class MarkedForParsingBit : public BitField<bool, kMarkedForParsingBit, 1> {};
  class MixinTypedefBit : public BitField<bool, kMixinTypedefBit, 1> {};
  class MixinTypeAppliedBit : public BitField<bool, kMixinTypeAppliedBit, 1> {};

  void set_name(const String& value) const;
  void set_signature_function(const Function& value) const;
  void set_signature_type(const AbstractType& value) const;
  void set_state_bits(intptr_t bits) const;

  void set_constants(const Array& value) const;

  void set_canonical_types(const Array& value) const;
  RawArray* canonical_types() const;

  RawArray* invocation_dispatcher_cache() const;
  void set_invocation_dispatcher_cache(const Array& cache) const;
  RawFunction* CreateInvocationDispatcher(const String& target_name,
                                          const Array& args_desc,
                                          RawFunction::Kind kind) const;
  void CalculateFieldOffsets() const;

  // Initial value for the cached number of type arguments.
  static const intptr_t kUnknownNumTypeArguments = -1;

  int16_t num_type_arguments() const {
    return raw_ptr()->num_type_arguments_;
  }
  void set_num_type_arguments(intptr_t value) const;

  int16_t num_own_type_arguments() const {
    return raw_ptr()->num_own_type_arguments_;
  }
  void set_num_own_type_arguments(intptr_t value) const;

  // Assigns empty array to all raw class array fields.
  void InitEmptyFields();

  static RawFunction* CheckFunctionType(const Function& func, intptr_t type);
  RawFunction* LookupFunction(const String& name, intptr_t type) const;
  RawFunction* LookupFunctionAllowPrivate(const String& name,
                                          intptr_t type) const;
  RawField* LookupField(const String& name, intptr_t type) const;

  RawFunction* LookupAccessorFunction(const char* prefix,
                                      intptr_t prefix_length,
                                      const String& name) const;

  // Allocate an instance class which has a VM implementation.
  template <class FakeInstance> static RawClass* New(intptr_t id);

  // Check the subtype or 'more specific' relationship.
  bool TypeTest(TypeTestKind test_kind,
                const AbstractTypeArguments& type_arguments,
                const Class& other,
                const AbstractTypeArguments& other_type_arguments,
                Error* bound_error) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Class, Object);
  friend class AbstractType;
  friend class Instance;
  friend class Object;
  friend class Type;
};


// Unresolved class is used for storing unresolved names which will be resolved
// to a class after all classes have been loaded and finalized.
class UnresolvedClass : public Object {
 public:
  RawLibraryPrefix* library_prefix() const {
    return raw_ptr()->library_prefix_;
  }
  RawString* ident() const { return raw_ptr()->ident_; }
  intptr_t token_pos() const { return raw_ptr()->token_pos_; }

  RawString* Name() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawUnresolvedClass));
  }
  static RawUnresolvedClass* New(const LibraryPrefix& library_prefix,
                                 const String& ident,
                                 intptr_t token_pos);

 private:
  void set_library_prefix(const LibraryPrefix& library_prefix) const;
  void set_ident(const String& ident) const;
  void set_token_pos(intptr_t token_pos) const;

  static RawUnresolvedClass* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(UnresolvedClass, Object);
  friend class Class;
};


// AbstractTypeArguments is an abstract superclass.
// Subclasses of AbstractTypeArguments are TypeArguments and
// InstantiatedTypeArguments.
class AbstractTypeArguments : public Object {
 public:
  // Returns true if all types of this vector are finalized.
  virtual bool IsFinalized() const { return true; }

  // Returns true if both arguments represent vectors of equal types.
  static bool AreEqual(const AbstractTypeArguments& arguments,
                       const AbstractTypeArguments& other_arguments);

  // Return 'this' if this type argument vector is instantiated, i.e. if it does
  // not refer to type parameters. Otherwise, return a new type argument vector
  // where each reference to a type parameter is replaced with the corresponding
  // type of the instantiator type argument vector.
  // If bound_error is not NULL, it may be set to reflect a bound error.
  virtual RawAbstractTypeArguments* InstantiateFrom(
      const AbstractTypeArguments& instantiator_type_arguments,
      Error* bound_error) const;

  // Do not clone InstantiatedTypeArguments or null vectors, since they are
  // considered finalized.
  virtual RawAbstractTypeArguments* CloneUnfinalized() const {
    return this->raw();
  }

  // Do not canonicalize InstantiatedTypeArguments or null vectors.
  virtual RawAbstractTypeArguments* Canonicalize() const { return this->raw(); }

  // The name of this type argument vector, e.g. "<T, dynamic, List<T>, Smi>".
  virtual RawString* Name() const {
    return SubvectorName(0, Length(), kInternalName);
  }

  // The name of this type argument vector, e.g. "<T, dynamic, List<T>, int>".
  // Names of internal classes are mapped to their public interfaces.
  virtual RawString* UserVisibleName() const {
    return SubvectorName(0, Length(), kUserVisibleName);
  }

  // Check if this type argument vector consists solely of DynamicType,
  // considering only a prefix of length 'len'.
  bool IsRaw(intptr_t len) const {
    return IsDynamicTypes(false, len);
  }

  // Check if this type argument vector would consist solely of DynamicType if
  // it was instantiated from a raw (null) instantiator, i.e. consider each type
  // parameter as it would be first instantiated from a vector of dynamic types.
  // Consider only a prefix of length 'len'.
  bool IsRawInstantiatedRaw(intptr_t len) const {
    return IsDynamicTypes(true, len);
  }

  // Check the subtype relationship, considering only a prefix of length 'len'.
  bool IsSubtypeOf(const AbstractTypeArguments& other,
                   intptr_t len,
                   Error* bound_error) const {
    return TypeTest(kIsSubtypeOf, other, len, bound_error);
  }

  // Check the 'more specific' relationship, considering only a prefix of
  // length 'len'.
  bool IsMoreSpecificThan(const AbstractTypeArguments& other,
                          intptr_t len,
                          Error* bound_error) const {
    return TypeTest(kIsMoreSpecificThan, other, len, bound_error);
  }

  bool Equals(const AbstractTypeArguments& other) const;

  // UNREACHABLEs as AbstractTypeArguments is an abstract class.
  virtual intptr_t Length() const;
  virtual RawAbstractType* TypeAt(intptr_t index) const;
  virtual void SetTypeAt(intptr_t index, const AbstractType& value) const;
  virtual bool IsResolved() const;
  virtual bool IsInstantiated() const;
  virtual bool IsUninstantiatedIdentity() const;
  virtual bool CanShareInstantiatorTypeArguments(
      const Class& instantiator_class) const;
  virtual bool IsBounded() const;

  virtual intptr_t Hash() const;

 private:
  // Check if this type argument vector consists solely of DynamicType,
  // considering only a prefix of length 'len'.
  // If raw_instantiated is true, consider each type parameter to be first
  // instantiated from a vector of dynamic types.
  bool IsDynamicTypes(bool raw_instantiated, intptr_t len) const;

  // Check the subtype or 'more specific' relationship, considering only a
  // prefix of length 'len'.
  bool TypeTest(TypeTestKind test_kind,
                const AbstractTypeArguments& other,
                intptr_t len,
                Error* bound_error) const;

  // Return the internal or public name of a subvector of this type argument
  // vector, e.g. "<T, dynamic, List<T>, int>".
  RawString* SubvectorName(intptr_t from_index,
                           intptr_t len,
                           NameVisibility name_visibility) const;

 protected:
  HEAP_OBJECT_IMPLEMENTATION(AbstractTypeArguments, Object);
  friend class AbstractType;
  friend class Class;
};


// A TypeArguments is an array of AbstractType.
class TypeArguments : public AbstractTypeArguments {
 public:
  virtual intptr_t Length() const;
  virtual RawAbstractType* TypeAt(intptr_t index) const;
  static intptr_t type_at_offset(intptr_t index) {
    return OFFSET_OF(RawTypeArguments, types_) + index * kWordSize;
  }
  virtual void SetTypeAt(intptr_t index, const AbstractType& value) const;
  virtual bool IsResolved() const;
  virtual bool IsInstantiated() const;
  virtual bool IsUninstantiatedIdentity() const;
  virtual bool CanShareInstantiatorTypeArguments(
      const Class& instantiator_class) const;
  virtual bool IsFinalized() const;
  virtual bool IsBounded() const;
  virtual RawAbstractTypeArguments* CloneUnfinalized() const;
  // Canonicalize only if instantiated, otherwise returns 'this'.
  virtual RawAbstractTypeArguments* Canonicalize() const;

  virtual RawAbstractTypeArguments* InstantiateFrom(
      const AbstractTypeArguments& instantiator_type_arguments,
      Error* bound_error) const;

  static const intptr_t kBytesPerElement = kWordSize;
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t length_offset() {
    return OFFSET_OF(RawTypeArguments, length_);
  }

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawTypeArguments) == OFFSET_OF(RawTypeArguments, types_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    // Ensure that the types_ is not adding to the object length.
    ASSERT(sizeof(RawTypeArguments) == (sizeof(RawObject) + (1 * kWordSize)));
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(
        sizeof(RawTypeArguments) + (len * kBytesPerElement));
  }

  static RawTypeArguments* New(intptr_t len, Heap::Space space = Heap::kOld);

 private:
  RawAbstractType** TypeAddr(intptr_t index) const;
  void SetLength(intptr_t value) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(TypeArguments, AbstractTypeArguments);
  friend class Class;
};


// An instance of InstantiatedTypeArguments is never encountered at compile
// time, but only at run time, when type parameters can be matched to actual
// types.
// An instance of InstantiatedTypeArguments consists of a pair of
// AbstractTypeArguments objects. The first type argument vector is
// uninstantiated, because it contains type expressions referring to at least
// one TypeParameter object, i.e. to a type that is not known at compile time.
// The second type argument vector is the instantiator, because each type
// parameter with index i in the first vector can be substituted (or
// "instantiated") with the type at index i in the second type argument vector.
class InstantiatedTypeArguments : public AbstractTypeArguments {
 public:
  virtual intptr_t Length() const;
  virtual RawAbstractType* TypeAt(intptr_t index) const;
  virtual void SetTypeAt(intptr_t index, const AbstractType& value) const;
  virtual bool IsResolved() const { return true; }
  virtual bool IsInstantiated() const { return true; }
  virtual bool IsUninstantiatedIdentity() const {
    UNREACHABLE();
    return false;
  }
  virtual bool CanShareInstantiatorTypeArguments(
      const Class& instantiator_class) const {
    UNREACHABLE();
    return false;
  }
  virtual bool IsBounded() const { return false; }  // Bounds were checked.

  RawAbstractTypeArguments* uninstantiated_type_arguments() const {
    return raw_ptr()->uninstantiated_type_arguments_;
  }
  static intptr_t uninstantiated_type_arguments_offset() {
    return OFFSET_OF(RawInstantiatedTypeArguments,
                     uninstantiated_type_arguments_);
  }

  RawAbstractTypeArguments* instantiator_type_arguments() const {
    return raw_ptr()->instantiator_type_arguments_;
  }
  static intptr_t instantiator_type_arguments_offset() {
    return OFFSET_OF(RawInstantiatedTypeArguments,
                     instantiator_type_arguments_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawInstantiatedTypeArguments));
  }

  static RawInstantiatedTypeArguments* New(
      const AbstractTypeArguments& uninstantiated_type_arguments,
      const AbstractTypeArguments& instantiator_type_arguments);

 private:
  void set_uninstantiated_type_arguments(
      const AbstractTypeArguments& value) const;
  void set_instantiator_type_arguments(
      const AbstractTypeArguments& value) const;
  static RawInstantiatedTypeArguments* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(InstantiatedTypeArguments,
                                   AbstractTypeArguments);
  friend class Class;
};


class PatchClass : public Object {
 public:
  RawClass* patched_class() const { return raw_ptr()->patched_class_; }
  RawClass* source_class() const { return raw_ptr()->source_class_; }
  RawScript* Script() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawPatchClass));
  }

  static RawPatchClass* New(const Class& patched_class,
                            const Class& source_class);

 private:
  void set_patched_class(const Class& value) const;
  void set_source_class(const Class& value) const;
  static RawPatchClass* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(PatchClass, Object);
  friend class Class;
};


class Function : public Object {
 public:
  RawString* name() const { return raw_ptr()->name_; }
  RawString* UserVisibleName() const;
  RawString* QualifiedUserVisibleName() const;
  virtual RawString* DictionaryName() const { return name(); }

  // Build a string of the form 'C<T, R>(T, {b: B, c: C}) => R' representing the
  // internal signature of the given function. In this example, T and R are
  // type parameters of class C, the owner of the function.
  RawString* Signature() const {
    const bool instantiate = false;
    return BuildSignature(instantiate, kInternalName, TypeArguments::Handle());
  }

  // Build a string of the form '(T, {b: B, c: C}) => R' representing the
  // user visible signature of the given function. In this example, T and R are
  // type parameters of class C, the owner of the function.
  // Implicit parameters are hidden, as well as the prefix denoting the
  // signature class and its type parameters.
  RawString* UserVisibleSignature() const {
    const bool instantiate = false;
    return BuildSignature(
        instantiate, kUserVisibleName, TypeArguments::Handle());
  }

  // Build a string of the form '(A, {b: B, c: C}) => D' representing the
  // signature of the given function, where all generic types (e.g. '<T, R>' in
  // 'C<T, R>(T, {b: B, c: C}) => R') are instantiated using the given
  // instantiator type argument vector of a C instance (e.g. '<A, D>').
  RawString* InstantiatedSignatureFrom(
      const AbstractTypeArguments& instantiator,
      NameVisibility name_visibility) const {
    const bool instantiate = true;
    return BuildSignature(instantiate, name_visibility, instantiator);
  }

  // Returns true if the signature of this function is instantiated, i.e. if it
  // does not involve generic parameter types or generic result type.
  bool HasInstantiatedSignature() const;

  // Build a string of the form 'T, {b: B, c: C} representing the user
  // visible formal parameters of the function.
  RawString* UserVisibleFormalParameters() const;

  RawClass* Owner() const;
  RawClass* origin() const;
  RawScript* script() const;

  RawAbstractType* result_type() const { return raw_ptr()->result_type_; }
  void set_result_type(const AbstractType& value) const;

  RawAbstractType* ParameterTypeAt(intptr_t index) const;
  void SetParameterTypeAt(intptr_t index, const AbstractType& value) const;
  RawArray* parameter_types() const { return raw_ptr()->parameter_types_; }
  void set_parameter_types(const Array& value) const;

  // Parameter names are valid for all valid parameter indices, and are not
  // limited to named optional parameters.
  RawString* ParameterNameAt(intptr_t index) const;
  void SetParameterNameAt(intptr_t index, const String& value) const;
  RawArray* parameter_names() const { return raw_ptr()->parameter_names_; }
  void set_parameter_names(const Array& value) const;

  // Sets function's code and code's function.
  void SetCode(const Code& value) const;

  // Detaches code from the function by setting the code to null, and patches
  // the code to be non-entrant.
  void DetachCode() const;

  // Reattaches code to the function, and patches the code to be entrant.
  void ReattachCode(const Code& code) const;

  // Disables optimized code and switches to unoptimized code.
  void SwitchToUnoptimizedCode() const;

  // Return the most recently compiled and installed code for this function.
  // It is not the only Code object that points to this function.
  RawCode* CurrentCode() const { return raw_ptr()->code_; }

  RawCode* unoptimized_code() const { return raw_ptr()->unoptimized_code_; }
  void set_unoptimized_code(const Code& value) const;
  static intptr_t code_offset() { return OFFSET_OF(RawFunction, code_); }
  static intptr_t unoptimized_code_offset() {
    return OFFSET_OF(RawFunction, unoptimized_code_);
  }
  inline bool HasCode() const;

  // Returns true if there is at least one debugger breakpoint
  // set in this function.
  bool HasBreakpoint() const;

  RawContextScope* context_scope() const;
  void set_context_scope(const ContextScope& value) const;

  // Enclosing function of this local function.
  RawFunction* parent_function() const;

  // Signature class of this closure function or signature function.
  RawClass* signature_class() const;
  void set_signature_class(const Class& value) const;

  RawCode* closure_allocation_stub() const;
  void set_closure_allocation_stub(const Code& value) const;

  void set_extracted_method_closure(const Function& function) const;
  RawFunction* extracted_method_closure() const;

  void set_saved_args_desc(const Array& array) const;
  RawArray* saved_args_desc() const;

  bool IsMethodExtractor() const {
    return kind() == RawFunction::kMethodExtractor;
  }

  bool IsNoSuchMethodDispatcher() const {
    return kind() == RawFunction::kNoSuchMethodDispatcher;
  }

  bool IsInvokeFieldDispatcher() const {
    return kind() == RawFunction::kInvokeFieldDispatcher;
  }

  // Returns true iff an implicit closure function has been created
  // for this function.
  bool HasImplicitClosureFunction() const {
    return implicit_closure_function() != null();
  }

  // Return the closure function implicitly created for this function.
  // If none exists yet, create one and remember it.
  RawFunction* ImplicitClosureFunction() const;

  // Return the closure implicitly created for this function.
  // If none exists yet, create one and remember it.
  RawInstance* ImplicitStaticClosure() const;

  // Redirection information for a redirecting factory.
  bool IsRedirectingFactory() const;
  RawType* RedirectionType() const;
  void SetRedirectionType(const Type& type) const;
  RawString* RedirectionIdentifier() const;
  void SetRedirectionIdentifier(const String& identifier) const;
  RawFunction* RedirectionTarget() const;
  void SetRedirectionTarget(const Function& target) const;

  RawFunction::Kind kind() const {
    return KindBits::decode(raw_ptr()->kind_tag_);
  }

  bool is_static() const { return StaticBit::decode(raw_ptr()->kind_tag_); }
  bool is_const() const { return ConstBit::decode(raw_ptr()->kind_tag_); }
  bool is_external() const { return ExternalBit::decode(raw_ptr()->kind_tag_); }
  bool IsConstructor() const {
    return (kind() == RawFunction::kConstructor) && !is_static();
  }
  bool IsImplicitConstructor() const;
  bool IsFactory() const {
    return (kind() == RawFunction::kConstructor) && is_static();
  }
  bool IsDynamicFunction() const {
    if (is_static() || is_abstract()) {
      return false;
    }
    switch (kind()) {
      case RawFunction::kRegularFunction:
      case RawFunction::kGetterFunction:
      case RawFunction::kSetterFunction:
      case RawFunction::kImplicitGetter:
      case RawFunction::kImplicitSetter:
      case RawFunction::kMethodExtractor:
      case RawFunction::kNoSuchMethodDispatcher:
      case RawFunction::kInvokeFieldDispatcher:
        return true;
      case RawFunction::kClosureFunction:
      case RawFunction::kConstructor:
      case RawFunction::kImplicitStaticFinalGetter:
      case RawFunction::kStaticInitializer:
        return false;
      default:
        UNREACHABLE();
        return false;
    }
  }
  bool IsStaticFunction() const {
    if (!is_static()) {
      return false;
    }
    switch (kind()) {
      case RawFunction::kRegularFunction:
      case RawFunction::kGetterFunction:
      case RawFunction::kSetterFunction:
      case RawFunction::kImplicitGetter:
      case RawFunction::kImplicitSetter:
      case RawFunction::kImplicitStaticFinalGetter:
      case RawFunction::kStaticInitializer:
        return true;
      case RawFunction::kClosureFunction:
      case RawFunction::kConstructor:
        return false;
      default:
        UNREACHABLE();
        return false;
    }
  }
  bool IsInFactoryScope() const;

  intptr_t token_pos() const { return raw_ptr()->token_pos_; }
  void set_token_pos(intptr_t value) const;

  intptr_t end_token_pos() const { return raw_ptr()->end_token_pos_; }
  void set_end_token_pos(intptr_t value) const {
    raw_ptr()->end_token_pos_ = value;
  }

  intptr_t num_fixed_parameters() const {
    return raw_ptr()->num_fixed_parameters_;
  }
  void set_num_fixed_parameters(intptr_t value) const;

  bool HasOptionalParameters() const {
    return raw_ptr()->num_optional_parameters_ != 0;
  }
  bool HasOptionalPositionalParameters() const {
    return raw_ptr()->num_optional_parameters_ > 0;
  }
  bool HasOptionalNamedParameters() const {
    return raw_ptr()->num_optional_parameters_ < 0;
  }
  intptr_t NumOptionalParameters() const {
    const intptr_t num_opt_params = raw_ptr()->num_optional_parameters_;
    return (num_opt_params >= 0) ? num_opt_params : -num_opt_params;
  }
  void SetNumOptionalParameters(intptr_t num_optional_parameters,
                                bool are_optional_positional) const;

  intptr_t NumOptionalPositionalParameters() const {
    const intptr_t num_opt_params = raw_ptr()->num_optional_parameters_;
    return (num_opt_params > 0) ? num_opt_params : 0;
  }
  intptr_t NumOptionalNamedParameters() const {
    const intptr_t num_opt_params = raw_ptr()->num_optional_parameters_;
    return (num_opt_params < 0) ? -num_opt_params : 0;
  }

  intptr_t NumParameters() const;

  intptr_t NumImplicitParameters() const;

  static intptr_t usage_counter_offset() {
    return OFFSET_OF(RawFunction, usage_counter_);
  }
  intptr_t usage_counter() const {
    return raw_ptr()->usage_counter_;
  }
  void set_usage_counter(intptr_t value) const {
    raw_ptr()->usage_counter_ = value;
  }

  int16_t deoptimization_counter() const {
    return raw_ptr()->deoptimization_counter_;
  }
  void set_deoptimization_counter(int16_t value) const {
    raw_ptr()->deoptimization_counter_ = value;
  }

  static const intptr_t kMaxInstructionCount = (1 << 16) - 1;
  intptr_t optimized_instruction_count() const {
    return raw_ptr()->optimized_instruction_count_;
  }
  void set_optimized_instruction_count(intptr_t value) const {
    ASSERT(value >= 0);
    if (value > kMaxInstructionCount) {
      value = kMaxInstructionCount;
    }
    raw_ptr()->optimized_instruction_count_ = static_cast<uint16_t>(value);
  }

  intptr_t optimized_call_site_count() const {
    return raw_ptr()->optimized_call_site_count_;
  }
  void set_optimized_call_site_count(intptr_t value) const {
    ASSERT(value >= 0);
    if (value > kMaxInstructionCount) {
      value = kMaxInstructionCount;
    }
    raw_ptr()->optimized_call_site_count_ = static_cast<uint16_t>(value);
  }

  bool is_optimizable() const;
  void set_is_optimizable(bool value) const;

  bool has_finally() const {
    return HasFinallyBit::decode(raw_ptr()->kind_tag_);
  }
  void set_has_finally(bool value) const;

  bool is_native() const { return NativeBit::decode(raw_ptr()->kind_tag_); }
  void set_is_native(bool value) const;

  bool is_abstract() const { return AbstractBit::decode(raw_ptr()->kind_tag_); }
  void set_is_abstract(bool value) const;

  bool IsInlineable() const;
  void set_is_inlinable(bool value) const;

  bool is_visible() const {
    return VisibleBit::decode(raw_ptr()->kind_tag_);
  }
  void set_is_visible(bool value) const;

  bool is_intrinsic() const {
    return IntrinsicBit::decode(raw_ptr()->kind_tag_);
  }
  void set_is_intrinsic(bool value) const;

  bool is_recognized() const {
    return RecognizedBit::decode(raw_ptr()->kind_tag_);
  }
  void set_is_recognized(bool value) const;

  bool is_redirecting() const {
    return RedirectingBit::decode(raw_ptr()->kind_tag_);
  }
  void set_is_redirecting(bool value) const;

  bool HasOptimizedCode() const;

  // Returns true if the argument counts are valid for calling this function.
  // Otherwise, it returns false and the reason (if error_message is not NULL).
  bool AreValidArgumentCounts(intptr_t num_arguments,
                              intptr_t num_named_arguments,
                              String* error_message) const;

  // Returns true if the total argument count and the names of optional
  // arguments are valid for calling this function.
  // Otherwise, it returns false and the reason (if error_message is not NULL).
  bool AreValidArguments(intptr_t num_arguments,
                         const Array& argument_names,
                         String* error_message) const;
  bool AreValidArguments(const ArgumentsDescriptor& args_desc,
                         String* error_message) const;

  // Fully qualified name uniquely identifying the function under gdb and during
  // ast printing. The special ':' character, if present, is replaced by '_'.
  const char* ToFullyQualifiedCString() const;

  // Returns true if this function has parameters that are compatible with the
  // parameters of the other function in order for this function to override the
  // other function.
  bool HasCompatibleParametersWith(const Function& other,
                                   Error* bound_error) const;

  // Returns true if the type of this function is a subtype of the type of
  // the other function.
  bool IsSubtypeOf(const AbstractTypeArguments& type_arguments,
                   const Function& other,
                   const AbstractTypeArguments& other_type_arguments,
                   Error* bound_error) const {
    return TypeTest(kIsSubtypeOf,
                    type_arguments,
                    other,
                    other_type_arguments,
                    bound_error);
  }

  // Returns true if the type of this function is more specific than the type of
  // the other function.
  bool IsMoreSpecificThan(const AbstractTypeArguments& type_arguments,
                          const Function& other,
                          const AbstractTypeArguments& other_type_arguments,
                          Error* bound_error) const {
    return TypeTest(kIsMoreSpecificThan,
                    type_arguments,
                    other,
                    other_type_arguments,
                    bound_error);
  }

  // Returns true if this function represents an explicit getter function.
  bool IsGetterFunction() const {
    return kind() == RawFunction::kGetterFunction;
  }

  // Returns true if this function represents an implicit getter function.
  bool IsImplicitGetterFunction() const {
    return kind() == RawFunction::kImplicitGetter;
  }

  // Returns true if this function represents an explicit setter function.
  bool IsSetterFunction() const {
    return kind() == RawFunction::kSetterFunction;
  }

  // Returns true if this function represents an implicit setter function.
  bool IsImplicitSetterFunction() const {
    return kind() == RawFunction::kImplicitSetter;
  }

  // Returns true if this function represents an static initializer function.
  bool IsStaticInitializerFunction() const {
    return kind() == RawFunction::kStaticInitializer;
  }

  // Returns true if this function represents a (possibly implicit) closure
  // function.
  bool IsClosureFunction() const {
    return kind() == RawFunction::kClosureFunction;
  }

  // Returns true if this function represents an implicit closure function.
  bool IsImplicitClosureFunction() const;

  // Returns true if this function represents a non implicit closure function.
  bool IsNonImplicitClosureFunction() const {
    return IsClosureFunction() && !IsImplicitClosureFunction();
  }

  // Returns true if this function represents an implicit static closure
  // function.
  bool IsImplicitStaticClosureFunction() const {
    return is_static() && IsImplicitClosureFunction();
  }

  // Returns true if this function represents an implicit instance closure
  // function.
  bool IsImplicitInstanceClosureFunction() const {
    return !is_static() && IsImplicitClosureFunction();
  }

  // Returns true if this function represents a local function.
  bool IsLocalFunction() const {
    return parent_function() != Function::null();
  }

  // Returns true if this function represents a signature function without code.
  bool IsSignatureFunction() const {
    return kind() == RawFunction::kSignatureFunction;
  }


  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawFunction));
  }

  static RawFunction* New(const String& name,
                          RawFunction::Kind kind,
                          bool is_static,
                          bool is_const,
                          bool is_abstract,
                          bool is_external,
                          const Object& owner,
                          intptr_t token_pos);

  // Allocates a new Function object representing a closure function, as well as
  // a new associated Class object representing the signature class of the
  // function.
  // The function and the class share the same given name.
  static RawFunction* NewClosureFunction(const String& name,
                                         const Function& parent,
                                         intptr_t token_pos);

  // Creates a new static initializer function which is invoked in the implicit
  // static getter function.
  static RawFunction* NewStaticInitializer(const String& field_name,
                                           const AbstractType& result_type,
                                           const Class& cls,
                                           intptr_t token_pos);

  // Allocate new function object, clone values from this function. The
  // owner of the clone is new_owner.
  RawFunction* Clone(const Class& new_owner) const;

  // Slow function, use in asserts to track changes in important library
  // functions.
  int32_t SourceFingerprint() const;

  // Return false and report an error if the fingerprint does not match.
  bool CheckSourceFingerprint(int32_t fp) const;

  static const int kCtorPhaseInit = 1 << 0;
  static const int kCtorPhaseBody = 1 << 1;
  static const int kCtorPhaseAll = (kCtorPhaseInit | kCtorPhaseBody);

 private:
  enum KindTagBits {
    kKindTagBit = 0,
    kKindTagSize = 4,
    kStaticBit = 4,
    kConstBit = 5,
    kAbstractBit = 6,
    kVisibleBit = 7,
    kOptimizableBit = 8,
    kInlinableBit = 9,
    kIntrinsicBit = 10,
    kRecognizedBit = 11,
    kHasFinallyBit = 12,
    kNativeBit = 13,
    kRedirectingBit = 14,
    kExternalBit = 15,
  };
  class KindBits :
    public BitField<RawFunction::Kind, kKindTagBit, kKindTagSize> {};  // NOLINT
  class StaticBit : public BitField<bool, kStaticBit, 1> {};
  class ConstBit : public BitField<bool, kConstBit, 1> {};
  class AbstractBit : public BitField<bool, kAbstractBit, 1> {};
  class VisibleBit : public BitField<bool, kVisibleBit, 1> {};
  class OptimizableBit : public BitField<bool, kOptimizableBit, 1> {};
  class InlinableBit : public BitField<bool, kInlinableBit, 1> {};
  class IntrinsicBit : public BitField<bool, kIntrinsicBit, 1> {};
  class RecognizedBit : public BitField<bool, kRecognizedBit, 1> {};
  class HasFinallyBit : public BitField<bool, kHasFinallyBit, 1> {};
  class NativeBit : public BitField<bool, kNativeBit, 1> {};
  class ExternalBit : public BitField<bool, kExternalBit, 1> {};
  class RedirectingBit : public BitField<bool, kRedirectingBit, 1> {};

  void set_name(const String& value) const;
  void set_kind(RawFunction::Kind value) const;
  void set_is_static(bool value) const;
  void set_is_const(bool value) const;
  void set_is_external(bool value) const;
  void set_parent_function(const Function& value) const;
  void set_owner(const Object& value) const;
  RawFunction* implicit_closure_function() const;
  void set_implicit_closure_function(const Function& value) const;
  RawInstance* implicit_static_closure() const;
  void set_implicit_static_closure(const Instance& closure) const;
  void set_num_optional_parameters(intptr_t value) const;  // Encoded value.
  void set_kind_tag(intptr_t value) const;
  void set_data(const Object& value) const;
  static RawFunction* New();

  void BuildSignatureParameters(bool instantiate,
                                NameVisibility name_visibility,
                                const AbstractTypeArguments& instantiator,
                                const GrowableObjectArray& pieces) const;
  RawString* BuildSignature(bool instantiate,
                            NameVisibility name_visibility,
                            const AbstractTypeArguments& instantiator) const;

  // Check the subtype or 'more specific' relationship.
  bool TypeTest(TypeTestKind test_kind,
                const AbstractTypeArguments& type_arguments,
                const Function& other,
                const AbstractTypeArguments& other_type_arguments,
                Error* bound_error) const;

  // Checks the type of the formal parameter at the given position for
  // subtyping or 'more specific' relationship between the type of this function
  // and the type of the other function.
  bool TestParameterType(TypeTestKind test_kind,
                         intptr_t parameter_position,
                         intptr_t other_parameter_position,
                         const AbstractTypeArguments& type_arguments,
                         const Function& other,
                         const AbstractTypeArguments& other_type_arguments,
                         Error* bound_error) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Function, Object);
  friend class Class;
};


class ClosureData: public Object {
 public:
  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawClosureData));
  }

 private:
  RawContextScope* context_scope() const { return raw_ptr()->context_scope_; }
  void set_context_scope(const ContextScope& value) const;

  // Enclosing function of this local function.
  RawFunction* parent_function() const { return raw_ptr()->parent_function_; }
  void set_parent_function(const Function& value) const;

  // Signature class of this closure function or signature function.
  RawClass* signature_class() const { return raw_ptr()->signature_class_; }
  void set_signature_class(const Class& value) const;

  RawInstance* implicit_static_closure() const {
    return raw_ptr()->closure_;
  }
  void set_implicit_static_closure(const Instance& closure) const;

  RawCode* closure_allocation_stub() const {
    return raw_ptr()->closure_allocation_stub_;
  }
  void set_closure_allocation_stub(const Code& value) const;

  static RawClosureData* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(ClosureData, Object);
  friend class Class;
  friend class Function;
  friend class HeapProfiler;
};


class RedirectionData: public Object {
 public:
  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawRedirectionData));
  }

 private:
  // The type specifies the class and type arguments of the target constructor.
  RawType* type() const { return raw_ptr()->type_; }
  void set_type(const Type& value) const;

  // The optional identifier specifies a named constructor.
  RawString* identifier() const { return raw_ptr()->identifier_; }
  void set_identifier(const String& value) const;

  // The resolved constructor or factory target of the redirection.
  RawFunction* target() const { return raw_ptr()->target_; }
  void set_target(const Function& value) const;

  static RawRedirectionData* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(RedirectionData, Object);
  friend class Class;
  friend class Function;
  friend class HeapProfiler;
};


class Field : public Object {
 public:
  RawString* name() const { return raw_ptr()->name_; }
  RawString* UserVisibleName() const;
  virtual RawString* DictionaryName() const { return name(); }

  bool is_static() const { return StaticBit::decode(raw_ptr()->kind_bits_); }
  bool is_final() const { return FinalBit::decode(raw_ptr()->kind_bits_); }
  bool is_const() const { return ConstBit::decode(raw_ptr()->kind_bits_); }

  inline intptr_t Offset() const;
  inline void SetOffset(intptr_t value_in_bytes) const;

  RawInstance* value() const;
  void set_value(const Instance& value) const;

  RawClass* owner() const;
  RawClass* origin() const;  // Either mixin class, or same as owner().

  RawAbstractType* type() const  { return raw_ptr()->type_; }
  void set_type(const AbstractType& value) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawField));
  }

  static RawField* New(const String& name,
                       bool is_static,
                       bool is_final,
                       bool is_const,
                       const Class& owner,
                       intptr_t token_pos);

  // Allocate new field object, clone values from this field. The
  // owner of the clone is new_owner.
  RawField* Clone(const Class& new_owner) const;

  static intptr_t value_offset() { return OFFSET_OF(RawField, value_); }

  intptr_t token_pos() const { return raw_ptr()->token_pos_; }

  bool has_initializer() const {
    return HasInitializerBit::decode(raw_ptr()->kind_bits_);
  }
  void set_has_initializer(bool has_initializer) const {
    set_kind_bits(HasInitializerBit::update(has_initializer,
                                            raw_ptr()->kind_bits_));
  }

  // Return class id that any non-null value read from this field is guaranteed
  // to have or kDynamicCid if such class id is not known.
  // Stores to this field must update this information hence the name.
  intptr_t guarded_cid() const { return raw_ptr()->guarded_cid_; }

  void set_guarded_cid(intptr_t cid) const {
    raw_ptr()->guarded_cid_ = cid;
  }
  static intptr_t guarded_cid_offset() {
    return OFFSET_OF(RawField, guarded_cid_);
  }
  // Return the list length that any list stored in this field is guaranteed
  // to have. If length is kUnknownFixedLength the length has not
  // been determined. If length is kNoFixedLength this field has multiple
  // list lengths associated with it and cannot be predicted.
  intptr_t guarded_list_length() const;
  void set_guarded_list_length(intptr_t list_length) const;
  static intptr_t guarded_list_length_offset() {
    return OFFSET_OF(RawField, guarded_list_length_);
  }
  bool needs_length_check() const {
    const bool r = guarded_list_length() >= Field::kUnknownFixedLength;
    ASSERT(!r || is_final());
    return r;
  }

  static bool IsExternalizableCid(intptr_t cid) {
    return (cid == kOneByteStringCid) || (cid == kTwoByteStringCid);
  }

  enum {
    kUnknownFixedLength = -1,
    kNoFixedLength = -2,
  };
  // Returns false if any value read from this field is guaranteed to be
  // not null.
  // Internally we is_nullable_ field contains either kNullCid (nullable) or
  // any other value (non-nullable) instead of boolean. This is done to simplify
  // guarding sequence in the generated code.
  bool is_nullable() const {
    return raw_ptr()->is_nullable_ == kNullCid;
  }
  void set_is_nullable(bool val) const {
    raw_ptr()->is_nullable_ = val ? kNullCid : kIllegalCid;
  }
  static intptr_t is_nullable_offset() {
    return OFFSET_OF(RawField, is_nullable_);
  }

  // Update guarded cid and guarded length for this field. May trigger
  // deoptimization of dependent optimized code.
  bool UpdateGuardedCidAndLength(const Object& value) const;

  // Return the list of optimized code objects that were optimized under
  // assumptions about guarded class id and nullability of this field.
  // These code objects must be deoptimized when field's properties change.
  // Code objects are held weakly via an indirection through WeakProperty.
  RawArray* dependent_code() const;
  void set_dependent_code(const Array& array) const;

  // Add the given code object to the list of dependent ones.
  void RegisterDependentCode(const Code& code) const;

  // Deoptimize all dependent code objects.
  void DeoptimizeDependentCode() const;

  bool IsUninitialized() const;

  // Constructs getter and setter names for fields and vice versa.
  static RawString* GetterName(const String& field_name);
  static RawString* GetterSymbol(const String& field_name);
  static RawString* SetterName(const String& field_name);
  static RawString* SetterSymbol(const String& field_name);
  static RawString* NameFromGetter(const String& getter_name);
  static RawString* NameFromSetter(const String& setter_name);
  static bool IsGetterName(const String& function_name);
  static bool IsSetterName(const String& function_name);

 private:
  enum {
    kConstBit = 1,
    kStaticBit,
    kFinalBit,
    kHasInitializerBit,
  };
  class ConstBit : public BitField<bool, kConstBit, 1> {};
  class StaticBit : public BitField<bool, kStaticBit, 1> {};
  class FinalBit : public BitField<bool, kFinalBit, 1> {};
  class HasInitializerBit : public BitField<bool, kHasInitializerBit, 1> {};

  // Update guarded class id and nullability of the field to reflect assignment
  // of the value with the given class id to this field. Returns true, if
  // deoptimization of dependent code is required.
  bool UpdateCid(intptr_t cid) const;

  // Update guarded class length of the field to reflect assignment of the
  // value with the given length. Returns true if deoptimization of dependent
  // code is required.
  bool UpdateLength(intptr_t length) const;

  void set_name(const String& value) const;
  void set_is_static(bool is_static) const {
    set_kind_bits(StaticBit::update(is_static, raw_ptr()->kind_bits_));
  }
  void set_is_final(bool is_final) const {
    set_kind_bits(FinalBit::update(is_final, raw_ptr()->kind_bits_));
  }
  void set_is_const(bool value) const {
    set_kind_bits(ConstBit::update(value, raw_ptr()->kind_bits_));
  }
  void set_owner(const Object& value) const {
    StorePointer(&raw_ptr()->owner_, value.raw());
  }
  void set_token_pos(intptr_t token_pos) const {
    raw_ptr()->token_pos_ = token_pos;
  }
  void set_kind_bits(intptr_t value) const {
    raw_ptr()->kind_bits_ = static_cast<uint8_t>(value);
  }
  static RawField* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Field, Object);
  friend class Class;
  friend class HeapProfiler;
};


class LiteralToken : public Object {
 public:
  Token::Kind kind() const { return raw_ptr()->kind_; }
  RawString* literal() const { return raw_ptr()->literal_; }
  RawObject* value() const { return raw_ptr()->value_; }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawLiteralToken));
  }

  static RawLiteralToken* New();
  static RawLiteralToken* New(Token::Kind kind, const String& literal);

 private:
  void set_kind(Token::Kind kind) const { raw_ptr()->kind_ = kind; }
  void set_literal(const String& literal) const;
  void set_value(const Object& value) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(LiteralToken, Object);
  friend class Class;
};


class TokenStream : public Object {
 public:
  RawArray* TokenObjects() const;
  void SetTokenObjects(const Array& value) const;

  RawExternalTypedData* GetStream() const;
  void SetStream(const ExternalTypedData& stream) const;

  RawString* GenerateSource() const;
  intptr_t ComputeSourcePosition(intptr_t tok_pos) const;

  RawString* PrivateKey() const;

  static const intptr_t kBytesPerElement = 1;
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawTokenStream));
  }

  static RawTokenStream* New(intptr_t length);
  static RawTokenStream* New(const Scanner::GrowableTokenStream& tokens,
                             const String& private_key);

  // The class Iterator encapsulates iteration over the tokens
  // in a TokenStream object.
  class Iterator : ValueObject {
   public:
    enum StreamType {
      kNoNewlines,
      kAllTokens
    };

    Iterator(const TokenStream& tokens,
                   intptr_t token_pos,
                   Iterator::StreamType stream_type = kNoNewlines);

    void SetStream(const TokenStream& tokens, intptr_t token_pos);
    bool IsValid() const;

    inline Token::Kind CurrentTokenKind() const {
      return cur_token_kind_;
    }

    Token::Kind LookaheadTokenKind(intptr_t num_tokens);

    intptr_t CurrentPosition() const;
    void SetCurrentPosition(intptr_t value);

    void Advance();

    RawObject* CurrentToken() const;
    RawString* CurrentLiteral() const;
    RawString* MakeLiteralToken(const Object& obj) const;

   private:
    // Read token from the token stream (could be a simple token or an index
    // into the token objects array for IDENT or literal tokens).
    intptr_t ReadToken() {
      int64_t value = stream_.ReadUnsigned();
      ASSERT((value >= 0) && (value <= kIntptrMax));
      return static_cast<intptr_t>(value);
    }

    TokenStream& tokens_;
    ExternalTypedData& data_;
    ReadStream stream_;
    Array& token_objects_;
    Object& obj_;
    intptr_t cur_token_pos_;
    Token::Kind cur_token_kind_;
    intptr_t cur_token_obj_index_;
    Iterator::StreamType stream_type_;
  };

 private:
  void SetPrivateKey(const String& value) const;

  static RawTokenStream* New();
  static void DataFinalizer(Dart_WeakPersistentHandle handle, void *peer);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(TokenStream, Object);
  friend class Class;
};


class Script : public Object {
 public:
  RawString* url() const { return raw_ptr()->url_; }
  bool HasSource() const;
  RawString* Source() const;
  RawString* GenerateSource() const;  // Generates source code from Tokenstream.
  RawScript::Kind kind() const {
    return static_cast<RawScript::Kind>(raw_ptr()->kind_);
  }
  intptr_t line_offset() const { return raw_ptr()->line_offset_; }
  intptr_t col_offset() const { return raw_ptr()->col_offset_; }

  RawTokenStream* tokens() const { return raw_ptr()->tokens_; }

  void Tokenize(const String& private_key) const;

  RawString* GetLine(intptr_t line_number) const;

  RawString* GetSnippet(intptr_t from_line,
                        intptr_t from_column,
                        intptr_t to_line,
                        intptr_t to_column) const;

  void SetLocationOffset(intptr_t line_offset, intptr_t col_offset) const;

  void GetTokenLocation(intptr_t token_pos,
                        intptr_t* line, intptr_t* column) const;

  // Returns index of first and last token on the given line. Returns both
  // indices < 0 if no token exists on or after the line. If a token exists
  // after, but not on given line, returns in *first_token_index the index of
  // the first token after the line, and a negative value in *last_token_index.
  void TokenRangeAtLine(intptr_t line_number,
                        intptr_t* first_token_index,
                        intptr_t* last_token_index) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawScript));
  }

  static RawScript* New(const String& url,
                        const String& source,
                        RawScript::Kind kind);

 private:
  void set_url(const String& value) const;
  void set_source(const String& value) const;
  void set_kind(RawScript::Kind value) const;
  void set_tokens(const TokenStream& value) const;
  static RawScript* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Script, Object);
  friend class Class;
};


class DictionaryIterator : public ValueObject {
 public:
  explicit DictionaryIterator(const Library& library);

  bool HasNext() const { return next_ix_ < size_; }

  // Returns next non-null raw object.
  RawObject* GetNext();

 private:
  void MoveToNextObject();

  const Array& array_;
  const int size_;  // Number of elements to iterate over.
  int next_ix_;  // Index of next element.

  friend class ClassDictionaryIterator;
  friend class LibraryPrefixIterator;
  DISALLOW_COPY_AND_ASSIGN(DictionaryIterator);
};


class ClassDictionaryIterator : public DictionaryIterator {
 public:
  enum IterationKind {
    kIteratePrivate,
    kNoIteratePrivate
  };

  ClassDictionaryIterator(const Library& library,
                          IterationKind kind = kNoIteratePrivate);

  bool HasNext() const { return (next_ix_ < size_) || (anon_ix_ < anon_size_); }

  // Returns a non-null raw class.
  RawClass* GetNextClass();

 private:
  void MoveToNextClass();

  const Array& anon_array_;
  const int anon_size_;  // Number of anonymous classes to iterate over.
  int anon_ix_;  // Index of next anonymous class.

  DISALLOW_COPY_AND_ASSIGN(ClassDictionaryIterator);
};


class LibraryPrefixIterator : public DictionaryIterator {
 public:
  explicit LibraryPrefixIterator(const Library& library);
  RawLibraryPrefix* GetNext();
 private:
  void Advance();
  DISALLOW_COPY_AND_ASSIGN(LibraryPrefixIterator);
};


class Library : public Object {
 public:
  RawString* name() const { return raw_ptr()->name_; }
  void SetName(const String& name) const;

  RawString* url() const { return raw_ptr()->url_; }
  RawString* private_key() const { return raw_ptr()->private_key_; }
  bool LoadNotStarted() const {
    return raw_ptr()->load_state_ == RawLibrary::kAllocated;
  }
  bool LoadInProgress() const {
    return raw_ptr()->load_state_ == RawLibrary::kLoadInProgress;
  }
  void SetLoadInProgress() const;
  bool Loaded() const { return raw_ptr()->load_state_ == RawLibrary::kLoaded; }
  void SetLoaded() const;
  bool LoadError() const {
    return raw_ptr()->load_state_ == RawLibrary::kLoadError;
  }
  void SetLoadError() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawLibrary));
  }

  static RawLibrary* New(const String& url);

  RawObject* Evaluate(const String& expr) const;

  // Library scope name dictionary.
  //
  // TODO(turnidge): The Lookup functions are not consistent in how
  // they deal with private names.  Go through and make them a bit
  // more regular.
  void AddClass(const Class& cls) const;
  void AddObject(const Object& obj, const String& name) const;
  void ReplaceObject(const Object& obj, const String& name) const;
  RawObject* LookupReExport(const String& name) const;
  RawObject* LookupObject(const String& name) const;
  RawObject* LookupObjectAllowPrivate(const String& name) const;
  RawObject* LookupLocalObjectAllowPrivate(const String& name) const;
  RawObject* LookupLocalObject(const String& name) const;
  RawObject* LookupImportedObject(const String& name) const;
  RawClass* LookupClass(const String& name) const;
  RawClass* LookupClassAllowPrivate(const String& name) const;
  RawClass* LookupLocalClass(const String& name) const;
  RawField* LookupFieldAllowPrivate(const String& name) const;
  RawField* LookupLocalField(const String& name) const;
  RawFunction* LookupFunctionAllowPrivate(const String& name) const;
  RawFunction* LookupLocalFunction(const String& name) const;
  RawLibraryPrefix* LookupLocalLibraryPrefix(const String& name) const;
  RawScript* LookupScript(const String& url) const;
  RawArray* LoadedScripts() const;

  void AddAnonymousClass(const Class& cls) const;

  void AddExport(const Namespace& ns) const;

  void AddClassMetadata(const Class& cls, intptr_t token_pos) const;
  void AddFieldMetadata(const Field& field, intptr_t token_pos) const;
  void AddFunctionMetadata(const Function& func, intptr_t token_pos) const;
  void AddLibraryMetadata(const Class& cls, intptr_t token_pos) const;
  void AddTypeParameterMetadata(const TypeParameter& param,
                                intptr_t token_pos) const;
  RawObject* GetMetadata(const Object& obj) const;

  intptr_t num_anonymous_classes() const { return raw_ptr()->num_anonymous_; }
  RawArray* anonymous_classes() const { return raw_ptr()->anonymous_classes_; }

  // Library imports.
  void AddImport(const Namespace& ns) const;
  intptr_t num_imports() const { return raw_ptr()->num_imports_; }
  RawNamespace* ImportAt(intptr_t index) const;
  RawLibrary* ImportLibraryAt(intptr_t index) const;
  bool ImportsCorelib() const;

  RawFunction* LookupFunctionInScript(const Script& script,
                                      intptr_t token_pos) const;

  // Resolving native methods for script loaded in the library.
  Dart_NativeEntryResolver native_entry_resolver() const {
    return raw_ptr()->native_entry_resolver_;
  }
  void set_native_entry_resolver(Dart_NativeEntryResolver value) const {
    raw_ptr()->native_entry_resolver_ = value;
  }

  RawError* Patch(const Script& script) const;

  RawString* PrivateName(const String& name) const;

  intptr_t index() const { return raw_ptr()->index_; }
  void set_index(intptr_t value) const {
    raw_ptr()->index_ = value;
  }

  void Register() const;

  bool IsDebuggable() const {
    return raw_ptr()->debuggable_;
  }
  void set_debuggable(bool value) const {
    raw_ptr()->debuggable_ = value;
  }

  bool IsCoreLibrary() const {
    return raw() == CoreLibrary();
  }

  static RawLibrary* LookupLibrary(const String& url);
  static RawLibrary* GetLibrary(intptr_t index);
  static bool IsKeyUsed(intptr_t key);

  static void InitCoreLibrary(Isolate* isolate);
  static void InitNativeWrappersLibrary(Isolate* isolate);

  static RawLibrary* AsyncLibrary();
  static RawLibrary* CoreLibrary();
  static RawLibrary* CollectionLibrary();
  static RawLibrary* CollectionDevLibrary();
  static RawLibrary* IsolateLibrary();
  static RawLibrary* MathLibrary();
  static RawLibrary* MirrorsLibrary();
  static RawLibrary* NativeWrappersLibrary();
  static RawLibrary* TypedDataLibrary();

  // Eagerly compile all classes and functions in the library.
  static RawError* CompileAll();

  // Checks function fingerprints. Prints mismatches and aborts if
  // mismatch found.
  static void CheckFunctionFingerprints();

  static bool IsPrivate(const String& name);
  // Construct the full name of a corelib member.
  static const String& PrivateCoreLibName(const String& member);
  // Lookup class in the core lib which also contains various VM
  // helper methods and classes. Allow look up of private classes.
  static RawClass* LookupCoreClass(const String& class_name);


  // Return Function::null() if function does not exist in libs.
  static RawFunction* GetFunction(const GrowableArray<Library*>& libs,
                                  const char* class_name,
                                  const char* function_name);

 private:
  static const int kInitialImportsCapacity = 4;
  static const int kImportsCapacityIncrement = 8;
  static RawLibrary* New();

  void set_num_imports(intptr_t value) const {
    raw_ptr()->num_imports_ = value;
  }
  RawArray* imports() const { return raw_ptr()->imports_; }
  RawArray* exports() const { return raw_ptr()->exports_; }
  bool HasExports() const;
  RawArray* loaded_scripts() const { return raw_ptr()->loaded_scripts_; }
  RawGrowableObjectArray* metadata() const { return raw_ptr()->metadata_; }
  RawArray* dictionary() const { return raw_ptr()->dictionary_; }
  void InitClassDictionary() const;
  void InitImportList() const;
  void GrowDictionary(const Array& dict, intptr_t dict_size) const;
  static RawLibrary* NewLibraryHelper(const String& url,
                                      bool import_core_lib);
  RawObject* LookupEntry(const String& name, intptr_t *index) const;

  RawString* MakeMetadataName(const Object& obj) const;
  RawField* GetMetadataField(const String& metaname) const;
  void AddMetadata(const Class& cls,
                   const String& name,
                   intptr_t token_pos) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Library, Object);

  friend class Bootstrap;
  friend class Class;
  friend class Debugger;
  friend class DictionaryIterator;
  friend class Namespace;
  friend class Object;
};


class LibraryPrefix : public Object {
 public:
  RawString* name() const { return raw_ptr()->name_; }
  virtual RawString* DictionaryName() const { return name(); }

  RawArray* imports() const { return raw_ptr()->imports_; }
  intptr_t num_imports() const { return raw_ptr()->num_imports_; }

  bool ContainsLibrary(const Library& library) const;
  RawLibrary* GetLibrary(int index) const;
  void AddImport(const Namespace& import) const;
  RawObject* LookupObject(const String& name) const;
  RawClass* LookupClass(const String& class_name) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawLibraryPrefix));
  }

  static RawLibraryPrefix* New(const String& name, const Namespace& import);

 private:
  static const int kInitialSize = 2;
  static const int kIncrementSize = 2;

  void set_name(const String& value) const;
  void set_imports(const Array& value) const;
  void set_num_imports(intptr_t value) const;
  static RawLibraryPrefix* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(LibraryPrefix, Object);
  friend class Class;
};


// A Namespace contains the names in a library dictionary, filtered by
// the show/hide combinators.
class Namespace : public Object {
 public:
  RawLibrary* library() const { return raw_ptr()->library_; }
  RawArray* show_names() const { return raw_ptr()->show_names_; }
  RawArray* hide_names() const { return raw_ptr()->hide_names_; }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawNamespace));
  }

  bool HidesName(const String& name) const;
  RawObject* Lookup(const String& name) const;

  static RawNamespace* New(const Library& library,
                           const Array& show_names,
                           const Array& hide_names);
 private:
  static RawNamespace* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Namespace, Object);
  friend class Class;
};


class Instructions : public Object {
 public:
  intptr_t size() const { return raw_ptr()->size_; }  // Excludes HeaderSize().
  RawCode* code() const { return raw_ptr()->code_; }
  static intptr_t code_offset() {
    return OFFSET_OF(RawInstructions, code_);
  }
  RawArray* object_pool() const { return raw_ptr()->object_pool_; }
  static intptr_t object_pool_offset() {
    return OFFSET_OF(RawInstructions, object_pool_);
  }

  uword EntryPoint() const {
    return reinterpret_cast<uword>(raw_ptr()) + HeaderSize();
  }

  static const intptr_t kMaxElements = (kIntptrMax -
                                        (sizeof(RawInstructions) +
                                         sizeof(RawObject) +
                                         (2 * OS::kMaxPreferredCodeAlignment)));

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawInstructions) == OFFSET_OF(RawInstructions, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t size) {
    intptr_t instructions_size = Utils::RoundUp(size,
                                                OS::PreferredCodeAlignment());
    intptr_t result = instructions_size + HeaderSize();
    ASSERT(result % OS::PreferredCodeAlignment() == 0);
    return result;
  }

  static intptr_t HeaderSize() {
    intptr_t alignment = OS::PreferredCodeAlignment();
    return Utils::RoundUp(sizeof(RawInstructions), alignment);
  }

  static RawInstructions* FromEntryPoint(uword entry_point) {
    return reinterpret_cast<RawInstructions*>(
        entry_point - HeaderSize() + kHeapObjectTag);
  }

 private:
  void set_size(intptr_t size) const {
    raw_ptr()->size_ = size;
  }
  void set_code(RawCode* code) const {
    raw_ptr()->code_ = code;
  }
  void set_object_pool(RawArray* object_pool) const {
    StorePointer(&raw_ptr()->object_pool_, object_pool);
  }

  // New is a private method as RawInstruction and RawCode objects should
  // only be created using the Code::FinalizeCode method. This method creates
  // the RawInstruction and RawCode objects, sets up the pointer offsets
  // and links the two in a GC safe manner.
  static RawInstructions* New(intptr_t size);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Instructions, Object);
  friend class Class;
  friend class Code;
};


class LocalVarDescriptors : public Object {
 public:
  intptr_t Length() const;

  RawString* GetName(intptr_t var_index) const;

  void SetVar(intptr_t var_index,
              const String& name,
              RawLocalVarDescriptors::VarInfo* info) const;

  void GetInfo(intptr_t var_index, RawLocalVarDescriptors::VarInfo* info) const;

  static const intptr_t kBytesPerElement =
      sizeof(RawLocalVarDescriptors::VarInfo);
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawLocalVarDescriptors) ==
        OFFSET_OF(RawLocalVarDescriptors, data_));
    return 0;
  }
  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(
        sizeof(RawLocalVarDescriptors) + (len * kBytesPerElement));
  }

  static RawLocalVarDescriptors* New(intptr_t num_variables);

 private:
  FINAL_HEAP_OBJECT_IMPLEMENTATION(LocalVarDescriptors, Object);
  friend class Class;
};


class PcDescriptors : public Object {
 private:
  // Describes the layout of PC descriptor data.
  enum {
    kPcEntry = 0,      // PC value of the descriptor, unique.
    kKindEntry = 1,
    kDeoptIdEntry = 2,      // Deopt id.
    kTokenPosEntry = 3,     // Token position in source.
    kTryIndexEntry = 4,     // Try block index.
    // We would potentially be adding other objects here like
    // pointer maps for optimized functions, local variables information  etc.
    kNumberOfEntries = 5,
  };

 public:
  enum Kind {
    kDeopt,            // Deoptimization continuation point.
    kEntryPatch,       // Location where to patch entry.
    kPatchCode,        // Buffer for patching code entry.
    kLazyDeoptJump,    // Lazy deoptimization trampoline.
    kIcCall,           // IC call.
    kOptStaticCall,    // Call directly to known target, e.g. static call.
    kUnoptStaticCall,  // Call to a known target via a stub.
    kClosureCall,      // Closure call.
    kRuntimeCall,      // Runtime call.
    kReturn,           // Return from function.
    kOsrEntry,         // OSR entry point in unoptimized code.
    kOther
  };

  intptr_t Length() const;

  uword PC(intptr_t index) const;
  PcDescriptors::Kind DescriptorKind(intptr_t index) const;
  const char* KindAsStr(intptr_t index) const;
  intptr_t DeoptId(intptr_t index) const;
  intptr_t TokenPos(intptr_t index) const;
  intptr_t TryIndex(intptr_t index) const;

  void AddDescriptor(intptr_t index,
                     uword pc,
                     PcDescriptors::Kind kind,
                     intptr_t deopt_id,
                     intptr_t token_pos,  // Or deopt reason.
                     intptr_t try_index) const {  // Or deopt index.
    SetPC(index, pc);
    SetKind(index, kind);
    SetDeoptId(index, deopt_id);
    SetTokenPos(index, token_pos);
    SetTryIndex(index, try_index);
  }

  static const intptr_t kBytesPerElement = (kNumberOfEntries * kWordSize);
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawPcDescriptors) == OFFSET_OF(RawPcDescriptors, data_));
    return 0;
  }
  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(
        sizeof(RawPcDescriptors) + (len * kBytesPerElement));
  }

  static RawPcDescriptors* New(intptr_t num_descriptors);

  // Returns 0 if not found.
  uword GetPcForKind(Kind kind) const;

  // Verify (assert) assumptions about pc descriptors in debug mode.
  void Verify(const Function& function) const;

  static void PrintHeaderString();

  // We would have a VisitPointers function here to traverse the
  // pc descriptors table to visit objects if any in the table.

 private:
  void SetPC(intptr_t index, uword value) const;
  void SetKind(intptr_t index, PcDescriptors::Kind kind) const;
  void SetDeoptId(intptr_t index, intptr_t value) const;
  void SetTokenPos(intptr_t index, intptr_t value) const;
  void SetTryIndex(intptr_t index, intptr_t value) const;

  void SetLength(intptr_t value) const;

  intptr_t* EntryAddr(intptr_t index, intptr_t entry_offset) const {
    ASSERT((index >=0) && (index < Length()));
    intptr_t data_index = (index * kNumberOfEntries) + entry_offset;
    return &raw_ptr()->data_[data_index];
  }
  RawSmi** SmiAddr(intptr_t index, intptr_t entry_offset) const {
    return reinterpret_cast<RawSmi**>(EntryAddr(index, entry_offset));
  }

  FINAL_HEAP_OBJECT_IMPLEMENTATION(PcDescriptors, Object);
  friend class Class;
};


class Stackmap : public Object {
 public:
  static const intptr_t kNoMaximum = -1;
  static const intptr_t kNoMinimum = -1;

  bool IsObject(intptr_t index) const {
    ASSERT(InRange(index));
    return GetBit(index);
  }

  RawCode* Code() const { return raw_ptr()->code_; }
  void SetCode(const dart::Code& code) const;

  intptr_t Length() const { return raw_ptr()->length_; }

  uword PC() const { return raw_ptr()->pc_; }
  void SetPC(uword value) const { raw_ptr()->pc_ = value; }

  intptr_t RegisterBitCount() const { return raw_ptr()->register_bit_count_; }
  void SetRegisterBitCount(intptr_t register_bit_count) const {
    raw_ptr()->register_bit_count_ = register_bit_count;
  }

  static const intptr_t kMaxLengthInBytes = kSmiMax;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawStackmap) == OFFSET_OF(RawStackmap, data_));
    return 0;
  }
  static intptr_t InstanceSize(intptr_t length) {
    ASSERT(length >= 0);
    // The stackmap payload is in an array of bytes.
    intptr_t payload_size =
        Utils::RoundUp(length, kBitsPerByte) / kBitsPerByte;
    return RoundedAllocationSize(sizeof(RawStackmap) + payload_size);
  }
  static RawStackmap* New(intptr_t pc_offset,
                          BitmapBuilder* bmap,
                          intptr_t register_bit_count);

 private:
  void SetLength(intptr_t length) const { raw_ptr()->length_ = length; }

  bool InRange(intptr_t index) const { return index < Length(); }

  bool GetBit(intptr_t bit_index) const;
  void SetBit(intptr_t bit_index, bool value) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Stackmap, Object);
  friend class BitmapBuilder;
  friend class Class;
};


class ExceptionHandlers : public Object {
 public:
  intptr_t Length() const;

  void GetHandlerInfo(intptr_t try_index,
                      RawExceptionHandlers::HandlerInfo* info) const;

  intptr_t HandlerPC(intptr_t try_index) const;
  intptr_t OuterTryIndex(intptr_t try_index) const;
  bool NeedsStacktrace(intptr_t try_index) const;

  void SetHandlerInfo(intptr_t try_index,
                      intptr_t outer_try_index,
                      intptr_t handler_pc,
                      bool needs_stacktrace,
                      bool has_catch_all) const;

  RawArray* GetHandledTypes(intptr_t try_index) const;
  void SetHandledTypes(intptr_t try_index, const Array& handled_types) const;
  bool HasCatchAll(intptr_t try_index) const;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawExceptionHandlers) == OFFSET_OF(RawExceptionHandlers,
                                                     data_));
    return 0;
  }
  static intptr_t InstanceSize(intptr_t len) {
    return RoundedAllocationSize(
        sizeof(RawExceptionHandlers) +
            (len * sizeof(RawExceptionHandlers::HandlerInfo)));
  }

  static RawExceptionHandlers* New(intptr_t num_handlers);

  // We would have a VisitPointers function here to traverse the
  // exception handler table to visit objects if any in the table.

 private:
  // Pick somewhat arbitrary maximum number of exception handlers
  // for a function. This value is used to catch potentially
  // malicious code.
  static const intptr_t kMaxHandlers = 1024 * 1024;

  void set_handled_types_data(const Array& value) const;
  FINAL_HEAP_OBJECT_IMPLEMENTATION(ExceptionHandlers, Object);
  friend class Class;
};


// Holds deopt information at one deoptimization point. The information consists
// of two parts:
//  - first a prefix consiting of kMaterializeObject instructions describing
//    objects which had their allocation removed as part of AllocationSinking
//    pass and have to be materialized;
//  - followed by a list of DeoptInstr objects, specifying transformation
//    information for each slot in unoptimized frame(s).
// Arguments for object materialization (class of instance to be allocated and
// field-value pairs) are added as artificial slots to the expression stack
// of the bottom-most frame. They are removed from the stack at the very end
// of deoptimization by the deoptimization stub.
class DeoptInfo : public Object {
 private:
  // Describes the layout of deopt info data. The index of a deopt-info entry
  // is implicitly the target slot in which the value is written into.
  enum {
    kInstruction = 0,
    kFromIndex,
    kNumberOfEntries,
  };

 public:
  // The number of instructions.
  intptr_t Length() const;

  // The number of real (non-suffix) instructions needed to execute the
  // deoptimization translation.
  intptr_t TranslationLength() const;

  // Size of the frame part of the translation not counting kMaterializeObject
  // instructions in the prefix.
  intptr_t FrameSize() const;

  static RawDeoptInfo* New(intptr_t num_commands);

  static const intptr_t kBytesPerElement = (kNumberOfEntries * kWordSize);
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawDeoptInfo) == OFFSET_OF(RawDeoptInfo, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(sizeof(RawDeoptInfo) +
                                 (len * kBytesPerElement));
  }

  // 'index' corresponds to target, to-index.
  void SetAt(intptr_t index,
             intptr_t instr_kind,
             intptr_t from_index) const;

  intptr_t Instruction(intptr_t index) const;
  intptr_t FromIndex(intptr_t index) const;
  intptr_t ToIndex(intptr_t index) const {
    return index;
  }

  // Unpack the entire translation into an array of deoptimization
  // instructions.  This copies any shared suffixes into the array.
  void ToInstructions(const Array& table,
                      GrowableArray<DeoptInstr*>* instructions) const;


  // Returns true iff decompression yields the same instructions as the
  // original.
  bool VerifyDecompression(const GrowableArray<DeoptInstr*>& original,
                           const Array& deopt_table) const;

 private:
  intptr_t* EntryAddr(intptr_t index, intptr_t entry_offset) const {
    ASSERT((index >=0) && (index < Length()));
    intptr_t data_index = (index * kNumberOfEntries) + entry_offset;
    return &raw_ptr()->data_[data_index];
  }

  void SetLength(intptr_t value) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(DeoptInfo, Object);
  friend class Class;
};


class Code : public Object {
 public:
  RawInstructions* instructions() const { return raw_ptr()->instructions_; }
  static intptr_t instructions_offset() {
    return OFFSET_OF(RawCode, instructions_);
  }
  intptr_t pointer_offsets_length() const {
    return raw_ptr()->pointer_offsets_length_;
  }

  bool is_optimized() const {
    return OptimizedBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_optimized(bool value) const;
  bool is_alive() const {
    return AliveBit::decode(raw_ptr()->state_bits_);
  }
  void set_is_alive(bool value) const;

  uword EntryPoint() const {
    const Instructions& instr = Instructions::Handle(instructions());
    return instr.EntryPoint();
  }
  intptr_t Size() const {
    const Instructions& instr = Instructions::Handle(instructions());
    return instr.size();
  }
  RawArray* ObjectPool() const {
    const Instructions& instr = Instructions::Handle(instructions());
    return instr.object_pool();
  }
  bool ContainsInstructionAt(uword addr) const {
    const Instructions& instr = Instructions::Handle(instructions());
    const uword offset = addr - instr.EntryPoint();
    return offset < static_cast<uword>(instr.size());
  }

  RawPcDescriptors* pc_descriptors() const {
    return raw_ptr()->pc_descriptors_;
  }
  void set_pc_descriptors(const PcDescriptors& descriptors) const {
    ASSERT(descriptors.IsOld());
    StorePointer(&raw_ptr()->pc_descriptors_, descriptors.raw());
  }

  // Array of DeoptInfo objects.
  RawArray* deopt_info_array() const {
    return raw_ptr()->deopt_info_array_;
  }
  void set_deopt_info_array(const Array& array) const;

  RawArray* object_table() const {
    return raw_ptr()->object_table_;
  }
  void set_object_table(const Array& array) const;

  RawArray* stackmaps() const {
    return raw_ptr()->stackmaps_;
  }
  void set_stackmaps(const Array& maps) const;
  RawStackmap* GetStackmap(uword pc, Array* stackmaps, Stackmap* map) const;

  enum {
    kSCallTableOffsetEntry = 0,
    kSCallTableFunctionEntry = 1,
    kSCallTableCodeEntry = 2,
    kSCallTableEntryLength = 3,
  };

  void set_static_calls_target_table(const Array& value) const;
  RawArray* static_calls_target_table() const {
    return raw_ptr()->static_calls_target_table_;
  }

  RawDeoptInfo* GetDeoptInfoAtPc(uword pc, intptr_t* deopt_reason) const;

  // Returns null if there is no static call at 'pc'.
  RawFunction* GetStaticCallTargetFunctionAt(uword pc) const;
  // Returns null if there is no static call at 'pc'.
  RawCode* GetStaticCallTargetCodeAt(uword pc) const;
  // Aborts if there is no static call at 'pc'.
  void SetStaticCallTargetCodeAt(uword pc, const Code& code) const;

  void Disassemble() const;

  class Comments : public ZoneAllocated {
   public:
    static Comments& New(intptr_t count);

    intptr_t Length() const;

    void SetPCOffsetAt(intptr_t idx, intptr_t pc_offset);
    void SetCommentAt(intptr_t idx, const String& comment);

    intptr_t PCOffsetAt(intptr_t idx) const;
    RawString* CommentAt(intptr_t idx) const;

   private:
    explicit Comments(const Array& comments);

    // Layout of entries describing comments.
    enum {
      kPCOffsetEntry = 0,  // PC offset to a comment as a Smi.
      kCommentEntry,  // Comment text as a String.
      kNumberOfEntries
    };

    const Array& comments_;

    friend class Code;

    DISALLOW_COPY_AND_ASSIGN(Comments);
  };


  const Comments& comments() const;
  void set_comments(const Comments& comments) const;

  RawLocalVarDescriptors* var_descriptors() const {
    return raw_ptr()->var_descriptors_;
  }
  void set_var_descriptors(const LocalVarDescriptors& value) const {
    ASSERT(value.IsOld());
    StorePointer(&raw_ptr()->var_descriptors_, value.raw());
  }

  RawExceptionHandlers* exception_handlers() const {
    return raw_ptr()->exception_handlers_;
  }
  void set_exception_handlers(const ExceptionHandlers& handlers) const {
    ASSERT(handlers.IsOld());
    StorePointer(&raw_ptr()->exception_handlers_, handlers.raw());
  }

  RawFunction* function() const {
    return raw_ptr()->function_;
  }
  void set_function(const Function& function) const {
    ASSERT(function.IsOld());
    StorePointer(&raw_ptr()->function_, function.raw());
  }

  // We would have a VisitPointers function here to traverse all the
  // embedded objects in the instructions using pointer_offsets.

  static const intptr_t kBytesPerElement =
      sizeof(reinterpret_cast<RawCode*>(0)->data_[0]);
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawCode) == OFFSET_OF(RawCode, data_));
    return 0;
  }
  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(sizeof(RawCode) + (len * kBytesPerElement));
  }
  static RawCode* FinalizeCode(const Function& function,
                               Assembler* assembler,
                               bool optimized = false);
  static RawCode* FinalizeCode(const char* name,
                               Assembler* assembler,
                               bool optimized = false);
  static RawCode* LookupCode(uword pc);

  int32_t GetPointerOffsetAt(int index) const {
    return *PointerOffsetAddrAt(index);
  }
  intptr_t GetTokenIndexOfPC(uword pc) const;

  // Find pc, return 0 if not found.
  uword GetPatchCodePc() const;
  uword GetLazyDeoptPc() const;

  uword GetPcForDeoptId(intptr_t deopt_id, PcDescriptors::Kind kind) const;
  intptr_t GetDeoptIdForOsr(uword pc) const;

  // Returns true if there is an object in the code between 'start_offset'
  // (inclusive) and 'end_offset' (exclusive).
  bool ObjectExistsInArea(intptr_t start_offest, intptr_t end_offset) const;

  // Each (*node_ids)[n] has a an extracted ic data array (*arrays)[n].
  // Returns the maximum id found.
  intptr_t ExtractIcDataArraysAtCalls(
      GrowableArray<intptr_t>* node_ids,
      const GrowableObjectArray& ic_data_objs) const;

  // Returns an array indexed by deopt id, containing the extracted ICData.
  RawArray* ExtractTypeFeedbackArray() const;

 private:
  void set_state_bits(intptr_t bits) const;

  friend class RawCode;
  enum {
    kOptimizedBit = 0,
    kAliveBit = 1,
  };

  class OptimizedBit : public BitField<bool, kOptimizedBit, 1> {};
  class AliveBit : public BitField<bool, kAliveBit, 1> {};

  // An object finder visitor interface.
  class FindRawCodeVisitor : public FindObjectVisitor {
   public:
    explicit FindRawCodeVisitor(uword pc)
        : FindObjectVisitor(Isolate::Current()), pc_(pc) { }
    virtual ~FindRawCodeVisitor() { }

    // Check if object matches find condition.
    virtual bool FindObject(RawObject* obj);

   private:
    const uword pc_;

    DISALLOW_COPY_AND_ASSIGN(FindRawCodeVisitor);
  };

  static const intptr_t kEntrySize = sizeof(int32_t);  // NOLINT

  void set_instructions(RawInstructions* instructions) {
    // RawInstructions are never allocated in New space and hence a
    // store buffer update is not needed here.
    raw_ptr()->instructions_ = instructions;
  }
  void set_pointer_offsets_length(intptr_t value) {
    ASSERT(value >= 0);
    raw_ptr()->pointer_offsets_length_ = value;
  }
  int32_t* PointerOffsetAddrAt(int index) const {
    ASSERT(index >= 0);
    ASSERT(index < pointer_offsets_length());
    // TODO(iposva): Unit test is missing for this functionality.
    return &raw_ptr()->data_[index];
  }
  void SetPointerOffsetAt(int index, int32_t offset_in_instructions) {
    *PointerOffsetAddrAt(index) = offset_in_instructions;
  }

  intptr_t BinarySearchInSCallTable(uword pc) const;

  // New is a private method as RawInstruction and RawCode objects should
  // only be created using the Code::FinalizeCode method. This method creates
  // the RawInstruction and RawCode objects, sets up the pointer offsets
  // and links the two in a GC safe manner.
  static RawCode* New(intptr_t pointer_offsets_length);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Code, Object);
  friend class Class;
};


class Context : public Object {
 public:
  RawContext* parent() const { return raw_ptr()->parent_; }
  void set_parent(const Context& parent) const {
    ASSERT(parent.IsNull() || parent.isolate() == Isolate::Current());
    StorePointer(&raw_ptr()->parent_, parent.raw());
  }
  static intptr_t parent_offset() { return OFFSET_OF(RawContext, parent_); }

  Isolate* isolate() const { return raw_ptr()->isolate_; }
  static intptr_t isolate_offset() { return OFFSET_OF(RawContext, isolate_); }

  intptr_t num_variables() const { return raw_ptr()->num_variables_; }
  static intptr_t num_variables_offset() {
    return OFFSET_OF(RawContext, num_variables_);
  }

  RawInstance* At(intptr_t context_index) const {
    return *InstanceAddr(context_index);
  }
  inline void SetAt(intptr_t context_index, const Instance& value) const;

  static const intptr_t kBytesPerElement = kWordSize;
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t variable_offset(intptr_t context_index) {
    return OFFSET_OF(RawContext, data_[context_index]);
  }

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawContext) == OFFSET_OF(RawContext, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(sizeof(RawContext) + (len * kBytesPerElement));
  }

  static RawContext* New(intptr_t num_variables,
                         Heap::Space space = Heap::kNew);

 private:
  RawInstance** InstanceAddr(intptr_t context_index) const {
    ASSERT((context_index >= 0) && (context_index < num_variables()));
    return &raw_ptr()->data_[context_index];
  }

  void set_isolate(Isolate* isolate) const {
    raw_ptr()->isolate_ = isolate;
  }

  void set_num_variables(intptr_t num_variables) const {
    raw_ptr()->num_variables_ = num_variables;
  }

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Context, Object);
  friend class Class;
};


// The ContextScope class makes it possible to delay the compilation of a local
// function until it is invoked. A ContextScope instance collects the local
// variables that are referenced by the local function to be compiled and that
// belong to the outer scopes, that is, to the local scopes of (possibly nested)
// functions enclosing the local function. Each captured variable is represented
// by its token position in the source, its name, its type, its allocation index
// in the context, and its context level. The function nesting level and loop
// nesting level are not preserved, since they are only used until the context
// level is assigned.
class ContextScope : public Object {
 public:
  intptr_t num_variables() const { return raw_ptr()->num_variables_; }

  intptr_t TokenIndexAt(intptr_t scope_index) const;
  void SetTokenIndexAt(intptr_t scope_index, intptr_t token_pos) const;

  RawString* NameAt(intptr_t scope_index) const;
  void SetNameAt(intptr_t scope_index, const String& name) const;

  bool IsFinalAt(intptr_t scope_index) const;
  void SetIsFinalAt(intptr_t scope_index, bool is_final) const;

  bool IsConstAt(intptr_t scope_index) const;
  void SetIsConstAt(intptr_t scope_index, bool is_const) const;

  RawAbstractType* TypeAt(intptr_t scope_index) const;
  void SetTypeAt(intptr_t scope_index, const AbstractType& type) const;

  RawInstance* ConstValueAt(intptr_t scope_index) const;
  void SetConstValueAt(intptr_t scope_index, const Instance& value) const;

  intptr_t ContextIndexAt(intptr_t scope_index) const;
  void SetContextIndexAt(intptr_t scope_index, intptr_t context_index) const;

  intptr_t ContextLevelAt(intptr_t scope_index) const;
  void SetContextLevelAt(intptr_t scope_index, intptr_t context_level) const;

  static const intptr_t kBytesPerElement =
      sizeof(RawContextScope::VariableDesc);
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawContextScope) == OFFSET_OF(RawContextScope, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(
        sizeof(RawContextScope) + (len * kBytesPerElement));
  }

  static RawContextScope* New(intptr_t num_variables);

 private:
  void set_num_variables(intptr_t num_variables) const {
    raw_ptr()->num_variables_ = num_variables;
  }

  RawContextScope::VariableDesc* VariableDescAddr(intptr_t index) const {
    ASSERT((index >= 0) && (index < num_variables()));
    uword raw_addr = reinterpret_cast<uword>(raw_ptr());
    raw_addr += sizeof(RawContextScope) +
        (index * sizeof(RawContextScope::VariableDesc));
    return reinterpret_cast<RawContextScope::VariableDesc*>(raw_addr);
  }

  FINAL_HEAP_OBJECT_IMPLEMENTATION(ContextScope, Object);
  friend class Class;
};


// Object holding information about an IC: test classes and their
// corresponding targets.
class ICData : public Object {
 public:
  RawFunction* function() const {
    return raw_ptr()->function_;
  }

  RawString* target_name() const {
    return raw_ptr()->target_name_;
  }

  RawArray* arguments_descriptor() const {
    return raw_ptr()->args_descriptor_;
  }

  intptr_t num_args_tested() const {
    return raw_ptr()->num_args_tested_;
  }

  intptr_t deopt_id() const {
    return raw_ptr()->deopt_id_;
  }

  intptr_t deopt_reason() const {
    return raw_ptr()->deopt_reason_;
  }

  void set_deopt_reason(intptr_t reason) const;

  bool is_closure_call() const {
    return raw_ptr()->is_closure_call_ == 1;
  }

  void set_is_closure_call(bool value) const;

  intptr_t NumberOfChecks() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawICData));
  }

  static intptr_t target_name_offset() {
    return OFFSET_OF(RawICData, target_name_);
  }

  static intptr_t num_args_tested_offset() {
    return OFFSET_OF(RawICData, num_args_tested_);
  }

  static intptr_t arguments_descriptor_offset() {
    return OFFSET_OF(RawICData, args_descriptor_);
  }

  static intptr_t ic_data_offset() {
    return OFFSET_OF(RawICData, ic_data_);
  }

  static intptr_t function_offset() {
    return OFFSET_OF(RawICData, function_);
  }

  static intptr_t is_closure_call_offset() {
    return OFFSET_OF(RawICData, is_closure_call_);
  }

  // Used for unoptimized static calls when no class-ids are checked.
  void AddTarget(const Function& target) const;

  // Adding checks.

  // Adds one more class test to ICData. Length of 'classes' must be equal to
  // the number of arguments tested. Use only for num_args_tested > 1.
  void AddCheck(const GrowableArray<intptr_t>& class_ids,
                const Function& target) const;
  // Adds sorted so that Smi is the first class-id. Use only for
  // num_args_tested == 1.
  void AddReceiverCheck(intptr_t receiver_class_id,
                        const Function& target,
                        intptr_t count = 1) const;

  // Retrieving checks.

  void GetCheckAt(intptr_t index,
                  GrowableArray<intptr_t>* class_ids,
                  Function* target) const;
  // Only for 'num_args_checked == 1'.
  void GetOneClassCheckAt(intptr_t index,
                          intptr_t* class_id,
                          Function* target) const;
  // Only for 'num_args_checked == 1'.
  intptr_t GetCidAt(intptr_t index) const;

  intptr_t GetReceiverClassIdAt(intptr_t index) const;
  intptr_t GetClassIdAt(intptr_t index, intptr_t arg_nr) const;

  RawFunction* GetTargetAt(intptr_t index) const;
  RawFunction* GetTargetForReceiverClassId(intptr_t class_id) const;

  void IncrementCountAt(intptr_t index, intptr_t value) const;
  void SetCountAt(intptr_t index, intptr_t value) const;
  intptr_t GetCountAt(intptr_t index) const;
  intptr_t AggregateCount() const;

  // Returns this->raw() if num_args_tested == 1 and arg_nr == 1, otherwise
  // returns a new ICData object containing only unique arg_nr checks.
  RawICData* AsUnaryClassChecksForArgNr(intptr_t arg_nr) const;
  RawICData* AsUnaryClassChecks() const {
    return AsUnaryClassChecksForArgNr(0);
  }

  bool AllTargetsHaveSameOwner(intptr_t owner_cid) const;
  bool AllReceiversAreNumbers() const;
  bool HasOneTarget() const;
  bool HasReceiverClassId(intptr_t class_id) const;

  static RawICData* New(const Function& caller_function,
                        const String& target_name,
                        const Array& arguments_descriptor,
                        intptr_t deopt_id,
                        intptr_t num_args_tested);

  static intptr_t TestEntryLengthFor(intptr_t num_args);

  static intptr_t TargetIndexFor(intptr_t num_args) {
    return num_args;
  }

  static intptr_t CountIndexFor(intptr_t num_args) {
    return (num_args + 1);
  }

 private:
  RawArray* ic_data() const {
    return raw_ptr()->ic_data_;
  }

  void set_function(const Function& value) const;
  void set_target_name(const String& value) const;
  void set_arguments_descriptor(const Array& value) const;
  void set_deopt_id(intptr_t value) const;
  void set_num_args_tested(intptr_t value) const;
  void set_ic_data(const Array& value) const;

#if defined(DEBUG)
  // Used in asserts to verify that a check is not added twice.
  bool HasCheck(const GrowableArray<intptr_t>& cids) const;
#endif  // DEBUG

  intptr_t TestEntryLength() const;
  void WriteSentinel(const Array& data) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(ICData, Object);
  friend class Class;
};


class MegamorphicCache : public Object {
 public:
  static const int kInitialCapacity = 16;
  static const double kLoadFactor;

  RawArray* buckets() const;
  void set_buckets(const Array& buckets) const;

  intptr_t mask() const;
  void set_mask(intptr_t mask) const;

  intptr_t filled_entry_count() const;
  void set_filled_entry_count(intptr_t num) const;

  static intptr_t buckets_offset() {
    return OFFSET_OF(RawMegamorphicCache, buckets_);
  }
  static intptr_t mask_offset() {
    return OFFSET_OF(RawMegamorphicCache, mask_);
  }

  static RawMegamorphicCache* New();

  void EnsureCapacity() const;

  void Insert(const Smi& class_id, const Function& target) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawMegamorphicCache));
  }

 private:
  friend class Class;

  enum {
    kClassIdIndex,
    kTargetFunctionIndex,
    kEntryLength,
  };

  static inline void SetEntry(const Array& array,
                              intptr_t index,
                              const Smi& class_id,
                              const Function& target);

  static inline RawObject* GetClassId(const Array& array, intptr_t index);
  static inline RawObject* GetTargetFunction(const Array& array,
                                             intptr_t index);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(MegamorphicCache, Object);
};


class SubtypeTestCache : public Object {
 public:
  enum Entries {
    kInstanceClassId = 0,
    kInstanceTypeArguments = 1,
    kInstantiatorTypeArguments = 2,
    kTestResult = 3,
    kTestEntryLength  = 4,
  };

  intptr_t NumberOfChecks() const;
  void AddCheck(intptr_t class_id,
                const AbstractTypeArguments& instance_type_arguments,
                const AbstractTypeArguments& instantiator_type_arguments,
                const Bool& test_result) const;
  void GetCheck(intptr_t ix,
                intptr_t* class_id,
                AbstractTypeArguments* instance_type_arguments,
                AbstractTypeArguments* instantiator_type_arguments,
                Bool* test_result) const;

  static RawSubtypeTestCache* New();

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawSubtypeTestCache));
  }

  static intptr_t cache_offset() {
    return OFFSET_OF(RawSubtypeTestCache, cache_);
  }

 private:
  RawArray* cache() const {
    return raw_ptr()->cache_;
  }

  void set_cache(const Array& value) const;

  intptr_t TestEntryLength() const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(SubtypeTestCache, Object);
  friend class Class;
};


class Error : public Object {
 public:
  virtual const char* ToErrorCString() const;

 private:
  HEAP_OBJECT_IMPLEMENTATION(Error, Object);
};


class ApiError : public Error {
 public:
  RawString* message() const { return raw_ptr()->message_; }
  static intptr_t message_offset() {
    return OFFSET_OF(RawApiError, message_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawApiError));
  }

  static RawApiError* New(const String& message,
                          Heap::Space space = Heap::kNew);

  virtual const char* ToErrorCString() const;

 private:
  void set_message(const String& message) const;
  static RawApiError* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(ApiError, Error);
  friend class Class;
};


class LanguageError : public Error {
 public:
  RawString* message() const { return raw_ptr()->message_; }
  static intptr_t message_offset() {
    return OFFSET_OF(RawLanguageError, message_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawLanguageError));
  }

  static RawLanguageError* New(const String& message,
                               Heap::Space space = Heap::kNew);

  virtual const char* ToErrorCString() const;

 private:
  void set_message(const String& message) const;
  static RawLanguageError* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(LanguageError, Error);
  friend class Class;
};


class UnhandledException : public Error {
 public:
  RawInstance* exception() const { return raw_ptr()->exception_; }
  static intptr_t exception_offset() {
    return OFFSET_OF(RawUnhandledException, exception_);
  }

  RawInstance* stacktrace() const { return raw_ptr()->stacktrace_; }
  static intptr_t stacktrace_offset() {
    return OFFSET_OF(RawUnhandledException, stacktrace_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawUnhandledException));
  }

  static RawUnhandledException* New(const Instance& exception,
                                    const Instance& stacktrace,
                                    Heap::Space space = Heap::kNew);

  virtual const char* ToErrorCString() const;

 private:
  void set_exception(const Instance& exception) const;
  void set_stacktrace(const Instance& stacktrace) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(UnhandledException, Error);
  friend class Class;
};


class UnwindError : public Error {
 public:
  RawString* message() const { return raw_ptr()->message_; }
  static intptr_t message_offset() {
    return OFFSET_OF(RawUnwindError, message_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawUnwindError));
  }

  static RawUnwindError* New(const String& message,
                             Heap::Space space = Heap::kNew);

  virtual const char* ToErrorCString() const;

 private:
  void set_message(const String& message) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(UnwindError, Error);
  friend class Class;
};


// Instance is the base class for all instance objects (aka the Object class
// in Dart source code.
class Instance : public Object {
 public:
  virtual bool Equals(const Instance& other) const;
  // Returns Instance::null() if instance cannot be canonicalized.
  // Any non-canonical number of string will be canonicalized here.
  // An instance cannot be canonicalized if it still contains non-canonical
  // instances in its fields.
  // Returns error in error_str, pass NULL if an error cannot occur.
  virtual RawInstance* CheckAndCanonicalize(const char** error_str) const;

  // Returns true if all fields are OK for canonicalization.
  virtual bool CheckAndCanonicalizeFields(const char** error_str) const;

  RawObject* GetField(const Field& field) const {
    return *FieldAddr(field);
  }

  void SetField(const Field& field, const Object& value) const {
    field.UpdateGuardedCidAndLength(value);
    StorePointer(FieldAddr(field), value.raw());
  }

  RawType* GetType() const;

  virtual RawAbstractTypeArguments* GetTypeArguments() const;
  virtual void SetTypeArguments(const AbstractTypeArguments& value) const;

  // Check if the type of this instance is a subtype of the given type.
  bool IsInstanceOf(const AbstractType& type,
                    const AbstractTypeArguments& type_instantiator,
                    Error* bound_error) const;

  // Check whether this instance is identical to the argument according to the
  // specification of dare:core's identical().
  bool IsIdenticalTo(const Instance& other) const;

  bool IsValidNativeIndex(int index) const {
    return ((index >= 0) && (index < clazz()->ptr()->num_native_fields_));
  }

  inline intptr_t GetNativeField(Isolate* isolate, int index) const;
  void SetNativeField(int index, intptr_t value) const;

  // Returns true if the instance is a closure object.
  bool IsClosure() const;

  // If the instance is a callable object, i.e. a closure or the instance of a
  // class implementing a 'call' method, return true and set the function
  // (if not NULL) to call and the context (if not NULL) to pass to the
  // function.
  bool IsCallable(Function* function, Context* context) const;

  // Evaluate the given expression as if it appeared in an instance
  // method of this instance and return the resulting value, or an
  // error object if evaluating the expression fails.
  RawObject* Evaluate(const String& expr) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawInstance));
  }

  static RawInstance* New(const Class& cls, Heap::Space space = Heap::kNew);

 private:
  RawObject** FieldAddrAtOffset(intptr_t offset) const {
    ASSERT(IsValidFieldOffset(offset));
    return reinterpret_cast<RawObject**>(raw_value() - kHeapObjectTag + offset);
  }
  RawObject** FieldAddr(const Field& field) const {
    return FieldAddrAtOffset(field.Offset());
  }
  RawObject** NativeFieldsAddr() const {
    return FieldAddrAtOffset(sizeof(RawObject));
  }
  void SetFieldAtOffset(intptr_t offset, const Object& value) const {
    StorePointer(FieldAddrAtOffset(offset), value.raw());
  }
  bool IsValidFieldOffset(int offset) const;

  // TODO(iposva): Determine if this gets in the way of Smi.
  HEAP_OBJECT_IMPLEMENTATION(Instance, Object);
  friend class Class;
  friend class Closure;
  friend class TypedDataView;
};


// AbstractType is an abstract superclass.
// Subclasses of AbstractType are Type and TypeParameter.
class AbstractType : public Instance {
 public:
  virtual bool IsFinalized() const;
  virtual bool IsBeingFinalized() const;
  virtual bool IsMalformed() const;
  virtual bool IsMalbounded() const { return IsMalboundedWithError(NULL); }
  virtual bool IsMalboundedWithError(Error* bound_error) const;
  virtual RawError* malformed_error() const;
  virtual void set_malformed_error(const Error& value) const;
  virtual bool IsResolved() const;
  virtual bool HasResolvedTypeClass() const;
  virtual RawClass* type_class() const;
  virtual RawUnresolvedClass* unresolved_class() const;
  virtual RawAbstractTypeArguments* arguments() const;
  virtual intptr_t token_pos() const;
  virtual bool IsInstantiated() const;
  virtual bool Equals(const Instance& other) const;

  // Instantiate this type using the given type argument vector.
  // Return a new type, or return 'this' if it is already instantiated.
  // If bound_error is not NULL, it may be set to reflect a bound error.
  virtual RawAbstractType* InstantiateFrom(
      const AbstractTypeArguments& instantiator_type_arguments,
      Error* bound_error) const;

  // Return a clone of this unfinalized type or the type itself if it is
  // already finalized. Apply recursively to type arguments, i.e. finalized
  // type arguments of an unfinalized type are not cloned, but shared.
  virtual RawAbstractType* CloneUnfinalized() const;

  virtual RawInstance* CheckAndCanonicalize(const char** error_str) const {
    return Canonicalize();
  }

  // Return the canonical version of this type.
  virtual RawAbstractType* Canonicalize() const;

  // The name of this type, including the names of its type arguments, if any.
  virtual RawString* Name() const {
    return BuildName(kInternalName);
  }

  // The name of this type, including the names of its type arguments, if any.
  // Names of internal classes are mapped to their public interfaces.
  virtual RawString* UserVisibleName() const {
    return BuildName(kUserVisibleName);
  }

  virtual intptr_t Hash() const;

  // The name of this type's class, i.e. without the type argument names of this
  // type.
  RawString* ClassName() const;

  // Check if this type represents the 'dynamic' type.
  bool IsDynamicType() const {
    return HasResolvedTypeClass() && (type_class() == Object::dynamic_class());
  }

  // Check if this type represents the 'Null' type.
  bool IsNullType() const;

  // Check if this type represents the 'void' type.
  bool IsVoidType() const {
    return HasResolvedTypeClass() && (type_class() == Object::void_class());
  }

  bool IsObjectType() const {
    return HasResolvedTypeClass() &&
    Class::Handle(type_class()).IsObjectClass();
  }

  // Check if this type represents the 'bool' type.
  bool IsBoolType() const;

  // Check if this type represents the 'int' type.
  bool IsIntType() const;

  // Check if this type represents the 'double' type.
  bool IsDoubleType() const;

  // Check if this type represents the 'Float32x4' type.
  bool IsFloat32x4Type() const;

  // Check if this type represents the 'Uint32x4' type.
  bool IsUint32x4Type() const;

  // Check if this type represents the 'num' type.
  bool IsNumberType() const;

  // Check if this type represents the 'String' type.
  bool IsStringType() const;

  // Check if this type represents the 'Function' type.
  bool IsFunctionType() const;

  // Check the subtype relationship.
  bool IsSubtypeOf(const AbstractType& other, Error* bound_error) const {
    return TypeTest(kIsSubtypeOf, other, bound_error);
  }

  // Check the 'more specific' relationship.
  bool IsMoreSpecificThan(const AbstractType& other,
                          Error* bound_error) const {
    return TypeTest(kIsMoreSpecificThan, other, bound_error);
  }

 private:
  // Check the subtype or 'more specific' relationship.
  bool TypeTest(TypeTestKind test_kind,
                const AbstractType& other,
                Error* bound_error) const;

  // Return the internal or public name of this type, including the names of its
  // type arguments, if any.
  RawString* BuildName(NameVisibility visibility) const;

 protected:
  HEAP_OBJECT_IMPLEMENTATION(AbstractType, Instance);
  friend class AbstractTypeArguments;
  friend class Class;
  friend class Function;
};


// A Type consists of a class, possibly parameterized with type
// arguments. Example: C<T1, T2>.
// An unresolved class is a String specifying the class name.
//
// Caution: 'RawType*' denotes a 'raw' pointer to a VM object of class Type, as
// opposed to 'Type' denoting a 'handle' to the same object. 'RawType' does not
// relate to a 'raw type', as opposed to a 'cooked type' or 'rare type'.
class Type : public AbstractType {
 public:
  static intptr_t type_class_offset() {
    return OFFSET_OF(RawType, type_class_);
  }
  virtual bool IsFinalized() const {
    return
    (raw_ptr()->type_state_ == RawType::kFinalizedInstantiated) ||
    (raw_ptr()->type_state_ == RawType::kFinalizedUninstantiated);
  }
  void SetIsFinalized() const;
  void ResetIsFinalized() const;  // Ignore current state and set again.
  virtual bool IsBeingFinalized() const {
    return raw_ptr()->type_state_ == RawType::kBeingFinalized;
  }
  void set_is_being_finalized() const;
  virtual bool IsMalformed() const;
  virtual bool IsMalbounded() const { return IsMalboundedWithError(NULL); }
  virtual bool IsMalboundedWithError(Error* bound_error) const;
  virtual RawError* malformed_error() const;
  virtual void set_malformed_error(const Error& value) const;
  virtual bool IsResolved() const;  // Class and all arguments classes resolved.
  virtual bool HasResolvedTypeClass() const;  // Own type class resolved.
  virtual RawClass* type_class() const;
  void set_type_class(const Object& value) const;
  virtual RawUnresolvedClass* unresolved_class() const;
  RawString* TypeClassName() const;
  virtual RawAbstractTypeArguments* arguments() const;
  void set_arguments(const AbstractTypeArguments& value) const;
  virtual intptr_t token_pos() const { return raw_ptr()->token_pos_; }
  virtual bool IsInstantiated() const;
  virtual bool Equals(const Instance& other) const;
  virtual RawAbstractType* InstantiateFrom(
      const AbstractTypeArguments& instantiator_type_arguments,
      Error* malformed_error) const;
  virtual RawAbstractType* CloneUnfinalized() const;
  virtual RawAbstractType* Canonicalize() const;

  virtual intptr_t Hash() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawType));
  }

  // The type of the literal 'null'.
  static RawType* NullType();

  // The 'dynamic' type.
  static RawType* DynamicType();

  // The 'void' type.
  static RawType* VoidType();

  // The 'Object' type.
  static RawType* ObjectType();

  // The 'bool' type.
  static RawType* BoolType();

  // The 'int' type.
  static RawType* IntType();

  // The 'Smi' type.
  static RawType* SmiType();

  // The 'Mint' type.
  static RawType* MintType();

  // The 'double' type.
  static RawType* Double();

  // The 'Float32x4' type.
  static RawType* Float32x4();

  // The 'Uint32x4' type.
  static RawType* Uint32x4();

  // The 'num' type.
  static RawType* Number();

  // The 'String' type.
  static RawType* StringType();

  // The 'Array' type.
  static RawType* ArrayType();

  // The 'Function' type.
  static RawType* Function();

  // The finalized type of the given non-parameterized class.
  static RawType* NewNonParameterizedType(const Class& type_class);

  static RawType* New(const Object& clazz,
                      const AbstractTypeArguments& arguments,
                      intptr_t token_pos,
                      Heap::Space space = Heap::kOld);

 private:
  void set_token_pos(intptr_t token_pos) const;
  void set_type_state(int8_t state) const;

  static RawType* New(Heap::Space space = Heap::kOld);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Type, AbstractType);
  friend class Class;
};


// A TypeParameter represents a type parameter of a parameterized class.
// It specifies its index (and its name for debugging purposes), as well as its
// upper bound.
// For example, the type parameter 'V' is specified as index 1 in the context of
// the class HashMap<K, V>. At compile time, the TypeParameter is not
// instantiated yet, i.e. it is only a place holder.
// Upon finalization, the TypeParameter index is changed to reflect its position
// as type argument (rather than type parameter) of the parameterized class.
// If the type parameter is declared without an extends clause, its bound is set
// to the ObjectType.
class TypeParameter : public AbstractType {
 public:
  virtual bool IsFinalized() const {
    ASSERT(raw_ptr()->type_state_ != RawTypeParameter::kFinalizedInstantiated);
    return raw_ptr()->type_state_ == RawTypeParameter::kFinalizedUninstantiated;
  }
  void set_is_finalized() const;
  virtual bool IsBeingFinalized() const { return false; }
  virtual bool IsMalformed() const { return false; }
  virtual bool IsMalbounded() const { return false; }
  virtual bool IsMalboundedWithError(Error* bound_error) const { return false; }
  virtual bool IsResolved() const { return true; }
  virtual bool HasResolvedTypeClass() const { return false; }
  RawClass* parameterized_class() const {
    return raw_ptr()->parameterized_class_;
  }
  RawString* name() const { return raw_ptr()->name_; }
  intptr_t index() const { return raw_ptr()->index_; }
  void set_index(intptr_t value) const;
  RawAbstractType* bound() const { return raw_ptr()->bound_; }
  void set_bound(const AbstractType& value) const;
  // Returns true if bounded_type is below upper_bound, otherwise return false
  // and set bound_error if not NULL.
  bool CheckBound(const AbstractType& bounded_type,
                  const AbstractType& upper_bound,
                  Error* bound_error) const;
  virtual intptr_t token_pos() const { return raw_ptr()->token_pos_; }
  virtual bool IsInstantiated() const { return false; }
  virtual bool Equals(const Instance& other) const;
  virtual RawAbstractType* InstantiateFrom(
      const AbstractTypeArguments& instantiator_type_arguments,
      Error* bound_error) const;
  virtual RawAbstractType* CloneUnfinalized() const;
  virtual RawAbstractType* Canonicalize() const { return raw(); }

  virtual intptr_t Hash() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawTypeParameter));
  }

  static RawTypeParameter* New(const Class& parameterized_class,
                               intptr_t index,
                               const String& name,
                               const AbstractType& bound,
                               intptr_t token_pos);

 private:
  void set_parameterized_class(const Class& value) const;
  void set_name(const String& value) const;
  void set_token_pos(intptr_t token_pos) const;
  void set_type_state(int8_t state) const;
  static RawTypeParameter* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(TypeParameter, AbstractType);
  friend class Class;
};


// A BoundedType represents a type instantiated at compile time from a type
// parameter specifying a bound that either cannot be checked at compile time
// because the type or the bound are still uninstantiated or can be checked and
// would trigger a bound error in checked mode. The bound must be checked at
// runtime once the type and its bound are instantiated and when the execution
// mode is known to be checked mode.
class BoundedType : public AbstractType {
 public:
  virtual bool IsFinalized() const {
    return AbstractType::Handle(type()).IsFinalized();
  }
  virtual bool IsBeingFinalized() const {
    return AbstractType::Handle(type()).IsBeingFinalized();
  }
  virtual bool IsMalformed() const;
  virtual bool IsMalbounded() const { return IsMalboundedWithError(NULL); }
  virtual bool IsMalboundedWithError(Error* bound_error) const;
  virtual RawError* malformed_error() const;
  virtual bool IsResolved() const { return true; }
  virtual bool HasResolvedTypeClass() const {
    return AbstractType::Handle(type()).HasResolvedTypeClass();
  }
  virtual RawClass* type_class() const {
    return AbstractType::Handle(type()).type_class();
  }
  virtual RawUnresolvedClass* unresolved_class() const {
    return AbstractType::Handle(type()).unresolved_class();
  }
  virtual RawAbstractTypeArguments* arguments() const {
    return AbstractType::Handle(type()).arguments();
  }
  RawAbstractType* type() const { return raw_ptr()->type_; }
  RawAbstractType* bound() const { return raw_ptr()->bound_; }
  RawTypeParameter* type_parameter() const {
    return raw_ptr()->type_parameter_;
  }
  virtual intptr_t token_pos() const {
    return AbstractType::Handle(type()).token_pos();
  }
  virtual bool IsInstantiated() const {
    return AbstractType::Handle(type()).IsInstantiated();
  }
  virtual bool Equals(const Instance& other) const;
  virtual RawAbstractType* InstantiateFrom(
      const AbstractTypeArguments& instantiator_type_arguments,
      Error* bound_error) const;
  virtual RawAbstractType* CloneUnfinalized() const;
  virtual RawAbstractType* Canonicalize() const { return raw(); }

  virtual intptr_t Hash() const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawBoundedType));
  }

  static RawBoundedType* New(const AbstractType& type,
                             const AbstractType& bound,
                             const TypeParameter& type_parameter);

 private:
  void set_type(const AbstractType& value) const;
  void set_bound(const AbstractType& value) const;
  void set_type_parameter(const TypeParameter& value) const;

  bool is_being_checked() const {
    return raw_ptr()->is_being_checked_;
  }
  void set_is_being_checked(bool value) const;

  static RawBoundedType* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(BoundedType, AbstractType);
  friend class Class;
};


// A MixinAppType represents a parsed mixin application clause, e.g.
// "S<T> with M<U>, N<V>".
// MixinAppType objects do not survive finalization, so they do not
// need to be written to and read from snapshots.
// The class finalizer creates synthesized classes S&M and S&M&N if they do not
// yet exist in the library declaring the mixin application clause.
class MixinAppType : public AbstractType {
 public:
  // A MixinAppType object is unfinalized by definition, since it is replaced at
  // class finalization time with a finalized Type or BoundedType object.
  virtual bool IsFinalized() const { return false; }
  // TODO(regis): Handle malformed and malbounded MixinAppType.
  virtual bool IsMalformed() const { return false; }
  virtual bool IsMalbounded() const { return false; }
  virtual bool IsResolved() const { return false; }
  virtual bool HasResolvedTypeClass() const { return false; }
  virtual RawString* Name() const;
  virtual intptr_t token_pos() const;

  // Returns the mixin composition depth of this mixin application type.
  intptr_t Depth() const;

  // Returns the declared super type of the mixin application, which will also
  // be the super type of the first synthesized class, e.g. class "S&M" will
  // refer to super type "S<T>".
  RawAbstractType* super_type() const { return raw_ptr()->super_type_; }

  // Returns the mixin type at the given mixin composition depth, e.g. N<V> at
  // depth 0 and M<U> at depth 1.
  RawAbstractType* MixinTypeAt(intptr_t depth) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawMixinAppType));
  }

  static RawMixinAppType* New(const AbstractType& super_type,
                              const Array& mixin_types);

 private:
  void set_super_type(const AbstractType& value) const;

  RawArray* mixin_types() const { return raw_ptr()->mixin_types_; }
  void set_mixin_types(const Array& value) const;

  static RawMixinAppType* New();

  FINAL_HEAP_OBJECT_IMPLEMENTATION(MixinAppType, AbstractType);
  friend class Class;
};


class Number : public Instance {
 public:
  // TODO(iposva): Fill in a useful Number interface.
  virtual bool IsZero() const {
    // Number is an abstract class.
    UNREACHABLE();
    return false;
  }
  virtual bool IsNegative() const {
    // Number is an abstract class.
    UNREACHABLE();
    return false;
  }
  OBJECT_IMPLEMENTATION(Number, Instance);
  friend class Class;
};


class Integer : public Number {
 public:
  static RawInteger* New(const String& str, Heap::Space space = Heap::kNew);
  static RawInteger* NewFromUint64(
      uint64_t value, Heap::Space space = Heap::kNew);

  // Returns a canonical Integer object allocated in the old gen space.
  static RawInteger* NewCanonical(const String& str);

  // Do not throw JavascriptIntegerOverflow if 'silent' is true.
  static RawInteger* New(int64_t value,
                         Heap::Space space = Heap::kNew,
                         const bool silent = false);

  virtual double AsDoubleValue() const;
  virtual int64_t AsInt64Value() const;

  // Returns 0, -1 or 1.
  virtual int CompareWith(const Integer& other) const;

  // Return the most compact presentation of an integer.
  RawInteger* AsValidInteger() const;

  RawInteger* ArithmeticOp(Token::Kind operation, const Integer& other) const;
  RawInteger* BitOp(Token::Kind operation, const Integer& other) const;

  // Returns true if the Integer does not fit in a Javascript integer.
  bool CheckJavascriptIntegerOverflow() const;

 private:
  // Return an integer in the form of a RawBigint.
  RawBigint* AsBigint() const;

  OBJECT_IMPLEMENTATION(Integer, Number);
  friend class Class;
};


class Smi : public Integer {
 public:
  static const intptr_t kBits = kSmiBits;
  static const intptr_t kMaxValue = kSmiMax;
  static const intptr_t kMinValue =  kSmiMin;

  intptr_t Value() const {
    return ValueFromRaw(raw_value());
  }

  virtual bool Equals(const Instance& other) const;
  virtual bool IsZero() const { return Value() == 0; }
  virtual bool IsNegative() const { return Value() < 0; }
  // Smi values are implicitly canonicalized.
  virtual RawInstance* CheckAndCanonicalize(const char** error_str) const {
    return reinterpret_cast<RawSmi*>(raw_value());
  }

  virtual double AsDoubleValue() const;
  virtual int64_t AsInt64Value() const;

  virtual int CompareWith(const Integer& other) const;

  static intptr_t InstanceSize() { return 0; }

  static RawSmi* New(intptr_t value) {
    word raw_smi = (value << kSmiTagShift) | kSmiTag;
    ASSERT(ValueFromRaw(raw_smi) == value);
    return reinterpret_cast<RawSmi*>(raw_smi);
  }

  static RawClass* Class();

  static intptr_t Value(const RawSmi* raw_smi) {
    return ValueFromRaw(reinterpret_cast<uword>(raw_smi));
  }

  static intptr_t RawValue(intptr_t value) {
    return reinterpret_cast<intptr_t>(New(value));
  }

  static bool IsValid(intptr_t value) {
    return (value >= kMinValue) && (value <= kMaxValue);
  }

  static bool IsValid64(int64_t value) {
    return (value >= kMinValue) && (value <= kMaxValue);
  }

  RawInteger* ShiftOp(Token::Kind kind,
                      const Smi& other,
                      const bool silent = false) const;

  void operator=(RawSmi* value) {
    raw_ = value;
    CHECK_HANDLE();
  }
  void operator^=(RawObject* value) {
    raw_ = value;
    CHECK_HANDLE();
  }

 private:
  static intptr_t ValueFromRaw(uword raw_value) {
    intptr_t value = raw_value;
    ASSERT((value & kSmiTagMask) == kSmiTag);
    return (value >> kSmiTagShift);
  }
  static cpp_vtable handle_vtable_;

  Smi() : Integer() {}
  BASE_OBJECT_IMPLEMENTATION(Smi, Integer);

  friend class Api;  // For ValueFromRaw
  friend class Class;
  friend class Object;
};


class Mint : public Integer {
 public:
  static const intptr_t kBits = 63;  // 64-th bit is sign.
  static const int64_t kMaxValue =
      static_cast<int64_t>(DART_2PART_UINT64_C(0x7FFFFFFF, FFFFFFFF));
  static const int64_t kMinValue =
      static_cast<int64_t>(DART_2PART_UINT64_C(0x80000000, 00000000));

  int64_t value() const {
    return raw_ptr()->value_;
  }
  static intptr_t value_offset() { return OFFSET_OF(RawMint, value_); }

  virtual bool IsZero() const {
    return value() == 0;
  }
  virtual bool IsNegative() const {
    return value() < 0;
  }

  virtual bool Equals(const Instance& other) const;

  virtual double AsDoubleValue() const;
  virtual int64_t AsInt64Value() const;

  virtual int CompareWith(const Integer& other) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawMint));
  }

 protected:
  // Only Integer::NewXXX is allowed to call Mint::NewXXX directly.
  friend class Integer;

  static RawMint* New(int64_t value, Heap::Space space = Heap::kNew);

  static RawMint* NewCanonical(int64_t value);

 private:
  void set_value(int64_t value) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Mint, Integer);
  friend class Class;
};


class Bigint : public Integer {
 private:
  typedef uint32_t Chunk;
  typedef uint64_t DoubleChunk;
  static const int kChunkSize = sizeof(Chunk);

 public:
  virtual bool IsZero() const { return raw_ptr()->signed_length_ == 0; }
  virtual bool IsNegative() const { return raw_ptr()->signed_length_ < 0; }

  virtual bool Equals(const Instance& other) const;

  virtual double AsDoubleValue() const;
  virtual int64_t AsInt64Value() const;

  virtual int CompareWith(const Integer& other) const;

  static const intptr_t kBytesPerElement = kChunkSize;
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() { return 0; }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(sizeof(RawBigint) + (len * kBytesPerElement));
  }

 protected:
  // Only Integer::NewXXX is allowed to call Bigint::NewXXX directly.
  friend class Integer;

  RawBigint* BigArithmeticOp(Token::Kind operation, const Bigint& other) const;

  static RawBigint* New(const String& str, Heap::Space space = Heap::kNew);

  // Returns a canonical Bigint object allocated in the old gen space.
  static RawBigint* NewCanonical(const String& str);

 private:
  Chunk GetChunkAt(intptr_t i) const {
    return *ChunkAddr(i);
  }

  void SetChunkAt(intptr_t i, Chunk newValue) const {
    *ChunkAddr(i) = newValue;
  }

  // Returns the number of chunks in use.
  intptr_t Length() const {
    intptr_t signed_length = raw_ptr()->signed_length_;
    return Utils::Abs(signed_length);
  }

  // SetLength does not change the sign.
  void SetLength(intptr_t length) const {
    ASSERT(length >= 0);
    bool is_negative = IsNegative();
    raw_ptr()->signed_length_ = length;
    if (is_negative) ToggleSign();
  }

  void SetSign(bool is_negative) const {
    if (is_negative != IsNegative()) {
      ToggleSign();
    }
  }

  void ToggleSign() const {
    raw_ptr()->signed_length_ = -raw_ptr()->signed_length_;
  }

  Chunk* ChunkAddr(intptr_t index) const {
    ASSERT(0 <= index);
    ASSERT(index < Length());
    uword digits_start = reinterpret_cast<uword>(raw_ptr()) + sizeof(RawBigint);
    return &(reinterpret_cast<Chunk*>(digits_start)[index]);
  }

  static RawBigint* Allocate(intptr_t length, Heap::Space space = Heap::kNew);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Bigint, Integer);
  friend class BigintOperations;
  friend class Class;
};


// Class Double represents class Double in corelib_impl, which implements
// abstract class double in corelib.
class Double : public Number {
 public:
  double value() const {
    return raw_ptr()->value_;
  }

  bool EqualsToDouble(double value) const;
  virtual bool Equals(const Instance& other) const;

  static RawDouble* New(double d, Heap::Space space = Heap::kNew);

  static RawDouble* New(const String& str, Heap::Space space = Heap::kNew);

  // Returns a canonical double object allocated in the old gen space.
  static RawDouble* NewCanonical(double d);

  // Returns a canonical double object (allocated in the old gen space) or
  // Double::null() if str points to a string that does not convert to a
  // double value.
  static RawDouble* NewCanonical(const String& str);

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawDouble));
  }

  static intptr_t value_offset() { return OFFSET_OF(RawDouble, value_); }

 private:
  void set_value(double value) const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Double, Number);
  friend class Class;
};


// String may not be '\0' terminated.
class String : public Instance {
 public:
  // We use 30 bits for the hash code so that we consistently use a
  // 32bit Smi representation for the hash code on all architectures.
  static const intptr_t kHashBits = 30;

  static const intptr_t kOneByteChar = 1;
  static const intptr_t kTwoByteChar = 2;

  // All strings share the same maximum element count to keep things
  // simple.  We choose a value that will prevent integer overflow for
  // 2 byte strings, since it is the worst case.
  static const intptr_t kSizeofRawString =
      sizeof(RawInstance) + (2 * kWordSize);
  static const intptr_t kMaxElements = kSmiMax / kTwoByteChar;

  class CodePointIterator : public ValueObject {
   public:
    explicit CodePointIterator(const String& str)
        : str_(str),
          ch_(0),
          index_(-1),
          end_(str.Length()) {
    }

    CodePointIterator(const String& str, intptr_t start, intptr_t length)
        : str_(str),
          ch_(0),
          index_(start - 1),
          end_(start + length) {
      ASSERT(start >= 0);
      ASSERT(end_ <= str.Length());
    }

    int32_t Current() const {
      ASSERT(index_ >= 0);
      ASSERT(index_ < end_);
      return ch_;
    }

    bool Next();

   private:
    const String& str_;
    int32_t ch_;
    intptr_t index_;
    intptr_t end_;
    DISALLOW_IMPLICIT_CONSTRUCTORS(CodePointIterator);
  };

  intptr_t Length() const { return Smi::Value(raw_ptr()->length_); }
  static intptr_t length_offset() { return OFFSET_OF(RawString, length_); }

  intptr_t Hash() const {
    intptr_t result = Smi::Value(raw_ptr()->hash_);
    if (result != 0) {
      return result;
    }
    result = String::Hash(*this, 0, this->Length());
    this->SetHash(result);
    return result;
  }

  static intptr_t hash_offset() { return OFFSET_OF(RawString, hash_); }
  static intptr_t Hash(const String& str, intptr_t begin_index, intptr_t len);
  static intptr_t Hash(const uint8_t* characters, intptr_t len);
  static intptr_t Hash(const uint16_t* characters, intptr_t len);
  static intptr_t Hash(const int32_t* characters, intptr_t len);

  int32_t CharAt(intptr_t index) const;

  intptr_t CharSize() const;

  inline bool Equals(const String& str) const;
  inline bool Equals(const String& str,
                     intptr_t begin_index,  // begin index on 'str'.
                     intptr_t len) const;  // len on 'str'.

  // Compares to a '\0' terminated array of UTF-8 encoded characters.
  bool Equals(const char* cstr) const;

  // Compares to an array of UTF-8 encoded characters.
  bool Equals(const uint8_t* characters, intptr_t len) const;

  // Compares to an array of UTF-16 encoded characters.
  bool Equals(const uint16_t* characters, intptr_t len) const;

  // Compares to an array of UTF-32 encoded characters.
  bool Equals(const int32_t* characters, intptr_t len) const;

  virtual bool Equals(const Instance& other) const;

  intptr_t CompareTo(const String& other) const;

  bool StartsWith(const String& other) const;

  virtual RawInstance* CheckAndCanonicalize(const char** error_str) const;

  bool IsSymbol() const { return raw()->IsCanonical(); }

  bool IsOneByteString() const {
    return raw()->GetClassId() == kOneByteStringCid;
  }

  bool IsTwoByteString() const {
    return raw()->GetClassId() == kTwoByteStringCid;
  }

  bool IsExternalOneByteString() const {
    return raw()->GetClassId() == kExternalOneByteStringCid;
  }

  bool IsExternalTwoByteString() const {
    return raw()->GetClassId() == kExternalTwoByteStringCid;
  }

  bool IsExternal() const {
    return RawObject::IsExternalStringClassId(raw()->GetClassId());
  }

  void* GetPeer() const;

  void ToUTF8(uint8_t* utf8_array, intptr_t array_len) const;

  // Copies the string characters into the provided external array
  // and morphs the string object into an external string object.
  // The remaining unused part of the original string object is marked as
  // an Array object or a regular Object so that it can be traversed during
  // garbage collection.
  RawString* MakeExternal(void* array,
                          intptr_t length,
                          void* peer,
                          Dart_PeerFinalizer cback) const;

  // Creates a new String object from a C string that is assumed to contain
  // UTF-8 encoded characters and '\0' is considered a termination character.
  // TODO(7123) - Rename this to FromCString(....).
  static RawString* New(const char* cstr, Heap::Space space = Heap::kNew);

  // Creates a new String object from an array of UTF-8 encoded characters.
  static RawString* FromUTF8(const uint8_t* utf8_array,
                             intptr_t array_len,
                             Heap::Space space = Heap::kNew);

  // Creates a new String object from an array of Latin-1 encoded characters.
  static RawString* FromLatin1(const uint8_t* latin1_array,
                               intptr_t array_len,
                               Heap::Space space = Heap::kNew);

  // Creates a new String object from an array of UTF-16 encoded characters.
  static RawString* FromUTF16(const uint16_t* utf16_array,
                              intptr_t array_len,
                              Heap::Space space = Heap::kNew);

  // Creates a new String object from an array of UTF-32 encoded characters.
  static RawString* FromUTF32(const int32_t* utf32_array,
                              intptr_t array_len,
                              Heap::Space space = Heap::kNew);

  // Create a new String object from another Dart String instance.
  static RawString* New(const String& str, Heap::Space space = Heap::kNew);

  // Creates a new External String object using the specified array of
  // UTF-8 encoded characters as the external reference.
  static RawString* NewExternal(const uint8_t* utf8_array,
                                intptr_t array_len,
                                void* peer,
                                Dart_PeerFinalizer callback,
                                Heap::Space = Heap::kNew);

  // Creates a new External String object using the specified array of
  // UTF-16 encoded characters as the external reference.
  static RawString* NewExternal(const uint16_t* utf16_array,
                                intptr_t array_len,
                                void* peer,
                                Dart_PeerFinalizer callback,
                                Heap::Space = Heap::kNew);

  static void Copy(const String& dst,
                   intptr_t dst_offset,
                   const uint8_t* characters,
                   intptr_t len);
  static void Copy(const String& dst,
                   intptr_t dst_offset,
                   const uint16_t* characters,
                   intptr_t len);
  static void Copy(const String& dst,
                   intptr_t dst_offset,
                   const String& src,
                   intptr_t src_offset,
                   intptr_t len);

  static RawString* EscapeSpecialCharacters(const String& str);

  static RawString* Concat(const String& str1,
                           const String& str2,
                           Heap::Space space = Heap::kNew);
  static RawString* ConcatAll(const Array& strings,
                              Heap::Space space = Heap::kNew);
  // Concat all strings in 'strings' from 'start' to 'end' (excluding).
  static RawString* ConcatAllRange(const Array& strings,
                                   intptr_t start,
                                   intptr_t end,
                                   Heap::Space space = Heap::kNew);

  static RawString* SubString(const String& str,
                              intptr_t begin_index,
                              Heap::Space space = Heap::kNew);
  static RawString* SubString(const String& str,
                              intptr_t begin_index,
                              intptr_t length,
                              Heap::Space space = Heap::kNew);

  static RawString* Transform(int32_t (*mapping)(int32_t ch),
                              const String& str,
                              Heap::Space space = Heap::kNew);

  static RawString* ToUpperCase(const String& str,
                                Heap::Space space = Heap::kNew);
  static RawString* ToLowerCase(const String& str,
                                Heap::Space space = Heap::kNew);

  static RawString* IdentifierPrettyName(const String& name);
  static RawString* IdentifierPrettyNameRetainPrivate(const String& name);

  static bool EqualsIgnoringPrivateKey(const String& str1,
                                       const String& str2);

  static RawString* NewFormatted(const char* format, ...)
      PRINTF_ATTRIBUTE(1, 2);
  static RawString* NewFormattedV(const char* format, va_list args);

 protected:
  bool HasHash() const {
    ASSERT(Smi::New(0) == NULL);
    return (raw_ptr()->hash_ != NULL);
  }

  void SetLength(intptr_t value) const {
    // This is only safe because we create a new Smi, which does not cause
    // heap allocation.
    raw_ptr()->length_ = Smi::New(value);
  }

  void SetHash(intptr_t value) const {
    // This is only safe because we create a new Smi, which does not cause
    // heap allocation.
    raw_ptr()->hash_ = Smi::New(value);
  }

  template<typename HandleType, typename ElementType, typename CallbackType>
  static void ReadFromImpl(SnapshotReader* reader,
                           String* str_obj,
                           intptr_t len,
                           intptr_t tags,
                           CallbackType new_symbol,
                           Snapshot::Kind kind);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(String, Instance);

  friend class Class;
  friend class Symbols;
  friend class OneByteString;
  friend class TwoByteString;
  friend class ExternalOneByteString;
  friend class ExternalTwoByteString;
};


class OneByteString : public AllStatic {
 public:
  static int32_t CharAt(const String& str, intptr_t index) {
    return *CharAddr(str, index);
  }

  static void SetCharAt(const String& str, intptr_t index, uint8_t code_point) {
    *CharAddr(str, index) = code_point;
  }
  static RawOneByteString* EscapeSpecialCharacters(const String& str);

  // We use the same maximum elements for all strings.
  static const intptr_t kBytesPerElement = 1;
  static const intptr_t kMaxElements = String::kMaxElements;

  static intptr_t data_offset() { return OFFSET_OF(RawOneByteString, data_); }

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawOneByteString) == OFFSET_OF(RawOneByteString, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(sizeof(RawOneByteString) == String::kSizeofRawString);
    ASSERT(0 <= len && len <= kMaxElements);
    return String::RoundedAllocationSize(
        sizeof(RawOneByteString) + (len * kBytesPerElement));
  }

  static RawOneByteString* New(intptr_t len,
                               Heap::Space space);
  static RawOneByteString* New(const char* c_string,
                               Heap::Space space = Heap::kNew) {
    return New(reinterpret_cast<const uint8_t*>(c_string),
               strlen(c_string),
               space);
  }
  static RawOneByteString* New(const uint8_t* characters,
                               intptr_t len,
                               Heap::Space space);
  static RawOneByteString* New(const uint16_t* characters,
                               intptr_t len,
                               Heap::Space space);
  static RawOneByteString* New(const int32_t* characters,
                               intptr_t len,
                               Heap::Space space);
  static RawOneByteString* New(const String& str,
                               Heap::Space space);
  // 'other' must be OneByteString.
  static RawOneByteString* New(const String& other_one_byte_string,
                               intptr_t other_start_index,
                               intptr_t other_len,
                               Heap::Space space);

  static RawOneByteString* Concat(const String& str1,
                                  const String& str2,
                                  Heap::Space space);
  static RawOneByteString* ConcatAll(const Array& strings,
                                     intptr_t start,
                                     intptr_t end,
                                     intptr_t len,
                                     Heap::Space space);

  static RawOneByteString* Transform(int32_t (*mapping)(int32_t ch),
                                     const String& str,
                                     Heap::Space space);

  // High performance version of substring for one-byte strings.
  // "str" must be OneByteString.
  static RawOneByteString* SubStringUnchecked(const String& str,
                                              intptr_t begin_index,
                                              intptr_t length,
                                              Heap::Space space);

  static void SetPeer(const String& str,
                      void* peer,
                      Dart_PeerFinalizer cback);

  static void Finalize(Dart_WeakPersistentHandle handle, void* peer);

  static const ClassId kClassId = kOneByteStringCid;

  static RawOneByteString* null() {
    return reinterpret_cast<RawOneByteString*>(Object::null());
  }

 private:
  static RawOneByteString* raw(const String& str) {
    return reinterpret_cast<RawOneByteString*>(str.raw());
  }

  static RawOneByteString* raw_ptr(const String& str) {
    return reinterpret_cast<RawOneByteString*>(str.raw_ptr());
  }

  static uint8_t* CharAddr(const String& str, intptr_t index) {
    ASSERT((index >= 0) && (index < str.Length()));
    ASSERT(str.IsOneByteString());
    NoGCScope no_gc;
    return &raw_ptr(str)->data_[index];
  }

  static RawOneByteString* ReadFrom(SnapshotReader* reader,
                                    intptr_t object_id,
                                    intptr_t tags,
                                    Snapshot::Kind kind);

  friend class Class;
  friend class String;
  friend class ExternalOneByteString;
  friend class SnapshotReader;
};


class TwoByteString : public AllStatic {
 public:
  static int32_t CharAt(const String& str, intptr_t index) {
    return *CharAddr(str, index);
  }

  static RawTwoByteString* EscapeSpecialCharacters(const String& str);

  // We use the same maximum elements for all strings.
  static const intptr_t kBytesPerElement = 2;
  static const intptr_t kMaxElements = String::kMaxElements;

  static intptr_t data_offset() { return OFFSET_OF(RawTwoByteString, data_); }

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawTwoByteString) == OFFSET_OF(RawTwoByteString, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(sizeof(RawTwoByteString) == String::kSizeofRawString);
    ASSERT(0 <= len && len <= kMaxElements);
    return String::RoundedAllocationSize(
        sizeof(RawTwoByteString) + (len * kBytesPerElement));
  }

  static RawTwoByteString* New(intptr_t len,
                               Heap::Space space);
  static RawTwoByteString* New(const uint16_t* characters,
                               intptr_t len,
                               Heap::Space space);
  static RawTwoByteString* New(intptr_t utf16_len,
                               const int32_t* characters,
                               intptr_t len,
                               Heap::Space space);
  static RawTwoByteString* New(const String& str,
                               Heap::Space space);

  static RawTwoByteString* Concat(const String& str1,
                                  const String& str2,
                                  Heap::Space space);
  static RawTwoByteString* ConcatAll(const Array& strings,
                                     intptr_t start,
                                     intptr_t end,
                                     intptr_t len,
                                     Heap::Space space);

  static RawTwoByteString* Transform(int32_t (*mapping)(int32_t ch),
                                     const String& str,
                                     Heap::Space space);

  static void SetPeer(const String& str,
                      void* peer,
                      Dart_PeerFinalizer cback);

  static void Finalize(Dart_WeakPersistentHandle handle, void* peer);

  static RawTwoByteString* null() {
    return reinterpret_cast<RawTwoByteString*>(Object::null());
  }


  static const ClassId kClassId = kTwoByteStringCid;

 private:
  static RawTwoByteString* raw(const String& str) {
    return reinterpret_cast<RawTwoByteString*>(str.raw());
  }

  static RawTwoByteString* raw_ptr(const String& str) {
    return reinterpret_cast<RawTwoByteString*>(str.raw_ptr());
  }

  static uint16_t* CharAddr(const String& str, intptr_t index) {
    ASSERT((index >= 0) && (index < str.Length()));
    ASSERT(str.IsTwoByteString());
    NoGCScope no_gc;
    return &raw_ptr(str)->data_[index];
  }

  static RawTwoByteString* ReadFrom(SnapshotReader* reader,
                                    intptr_t object_id,
                                    intptr_t tags,
                                    Snapshot::Kind kind);

  friend class Class;
  friend class String;
  friend class SnapshotReader;
};


class ExternalOneByteString : public AllStatic {
 public:
  static int32_t CharAt(const String& str, intptr_t index) {
    return *CharAddr(str, index);
  }

  static void* GetPeer(const String& str) {
    return raw_ptr(str)->external_data_->peer();
  }

  // We use the same maximum elements for all strings.
  static const intptr_t kBytesPerElement = 1;
  static const intptr_t kMaxElements = String::kMaxElements;

  static intptr_t InstanceSize() {
    return String::RoundedAllocationSize(sizeof(RawExternalOneByteString));
  }

  static RawExternalOneByteString* New(const uint8_t* characters,
                                       intptr_t len,
                                       void* peer,
                                       Dart_PeerFinalizer callback,
                                       Heap::Space space);

  static RawExternalOneByteString* null() {
    return reinterpret_cast<RawExternalOneByteString*>(Object::null());
  }

  static RawOneByteString* EscapeSpecialCharacters(const String& str);

  static const ClassId kClassId = kExternalOneByteStringCid;

 private:
  static RawExternalOneByteString* raw(const String& str) {
    return reinterpret_cast<RawExternalOneByteString*>(str.raw());
  }

  static RawExternalOneByteString* raw_ptr(const String& str) {
    return reinterpret_cast<RawExternalOneByteString*>(str.raw_ptr());
  }

  static const uint8_t* CharAddr(const String& str, intptr_t index) {
    ASSERT((index >= 0) && (index < str.Length()));
    ASSERT(str.IsExternalOneByteString());
    NoGCScope no_gc;
    return &(raw_ptr(str)->external_data_->data()[index]);
  }

  static void SetExternalData(const String& str,
                              ExternalStringData<uint8_t>* data) {
    ASSERT(str.IsExternalOneByteString());
    NoGCScope no_gc;
    raw_ptr(str)->external_data_ = data;
  }

  static void Finalize(Dart_WeakPersistentHandle handle, void* peer);

  static RawExternalOneByteString* ReadFrom(SnapshotReader* reader,
                                            intptr_t object_id,
                                            intptr_t tags,
                                            Snapshot::Kind kind);

  friend class Class;
  friend class String;
  friend class SnapshotReader;
};


class ExternalTwoByteString : public AllStatic {
 public:
  static int32_t CharAt(const String& str, intptr_t index) {
    return *CharAddr(str, index);
  }

  static void* GetPeer(const String& str) {
    return raw_ptr(str)->external_data_->peer();
  }

  // We use the same maximum elements for all strings.
  static const intptr_t kBytesPerElement = 2;
  static const intptr_t kMaxElements = String::kMaxElements;

  static intptr_t InstanceSize() {
    return String::RoundedAllocationSize(sizeof(RawExternalTwoByteString));
  }

  static RawExternalTwoByteString* New(const uint16_t* characters,
                                       intptr_t len,
                                       void* peer,
                                       Dart_PeerFinalizer callback,
                                       Heap::Space space = Heap::kNew);

  static RawExternalTwoByteString* null() {
    return reinterpret_cast<RawExternalTwoByteString*>(Object::null());
  }

  static const ClassId kClassId = kExternalTwoByteStringCid;

 private:
  static RawExternalTwoByteString* raw(const String& str) {
    return reinterpret_cast<RawExternalTwoByteString*>(str.raw());
  }

  static RawExternalTwoByteString* raw_ptr(const String& str) {
    return reinterpret_cast<RawExternalTwoByteString*>(str.raw_ptr());
  }

  static const uint16_t* CharAddr(const String& str, intptr_t index) {
    ASSERT((index >= 0) && (index < str.Length()));
    ASSERT(str.IsExternalTwoByteString());
    NoGCScope no_gc;
    return &(raw_ptr(str)->external_data_->data()[index]);
  }

  static void SetExternalData(const String& str,
                              ExternalStringData<uint16_t>* data) {
    ASSERT(str.IsExternalTwoByteString());
    NoGCScope no_gc;
    raw_ptr(str)->external_data_ = data;
  }

  static void Finalize(Dart_WeakPersistentHandle handle, void* peer);

  static RawExternalTwoByteString* ReadFrom(SnapshotReader* reader,
                                            intptr_t object_id,
                                            intptr_t tags,
                                            Snapshot::Kind kind);

  friend class Class;
  friend class String;
  friend class SnapshotReader;
};


// Class Bool implements Dart core class bool.
class Bool : public Instance {
 public:
  bool value() const {
    return raw_ptr()->value_;
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawBool));
  }

  static const Bool& True() {
    return Object::bool_true();
  }

  static const Bool& False() {
    return Object::bool_false();
  }

  static const Bool& Get(bool value) {
    return value ? Bool::True() : Bool::False();
  }

 private:
  void set_value(bool value) const { raw_ptr()->value_ = value; }

  // New should only be called to initialize the two legal bool values.
  static RawBool* New(bool value);

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Bool, Instance);
  friend class Class;
  friend class Object;  // To initialize the true and false values.
};


class Array : public Instance {
 public:
  intptr_t Length() const {
    ASSERT(!IsNull());
    return Smi::Value(raw_ptr()->length_);
  }
  static intptr_t length_offset() { return OFFSET_OF(RawArray, length_); }
  static intptr_t data_offset() { return length_offset() + kWordSize; }
  static intptr_t element_offset(intptr_t index) {
    return data_offset() + kWordSize * index;
  }

  RawObject* At(intptr_t index) const {
    return *ObjectAddr(index);
  }
  void SetAt(intptr_t index, const Object& value) const {
    // TODO(iposva): Add storing NoGCScope.
    StorePointer(ObjectAddr(index), value.raw());
  }

  bool IsImmutable() const {
    return raw()->GetClassId() == kImmutableArrayCid;
  }

  virtual RawAbstractTypeArguments* GetTypeArguments() const {
    return raw_ptr()->type_arguments_;
  }
  virtual void SetTypeArguments(const AbstractTypeArguments& value) const {
    // An Array is raw or takes one type argument. However, its type argument
    // vector may be longer than 1 due to a type optimization reusing the type
    // argument vector of the instantiator.
    ASSERT(value.IsNull() || ((value.Length() >= 1) && value.IsInstantiated()));
    StorePointer(&raw_ptr()->type_arguments_, value.raw());
  }

  virtual bool Equals(const Instance& other) const;

  static const intptr_t kBytesPerElement = kWordSize;
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t type_arguments_offset() {
    return OFFSET_OF(RawArray, type_arguments_);
  }

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawArray) == OFFSET_OF_RETURNED_VALUE(RawArray, data));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    // Ensure that variable length data is not adding to the object length.
    ASSERT(sizeof(RawArray) == (sizeof(RawInstance) + (2 * kWordSize)));
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(sizeof(RawArray) + (len * kBytesPerElement));
  }

  // Returns true if all elements are OK for canonicalization.
  virtual bool CheckAndCanonicalizeFields(const char** error_str) const;

  // Make the array immutable to Dart code by switching the class pointer
  // to ImmutableArray.
  void MakeImmutable() const;

  static RawArray* New(intptr_t len, Heap::Space space = Heap::kNew);

  // Creates and returns a new array with 'new_length'. Copies all elements from
  // 'source' to the new array. 'new_length' must be greater than or equal to
  // 'source.Length()'. 'source' can be null.
  static RawArray* Grow(const Array& source,
                        int new_length,
                        Heap::Space space = Heap::kNew);

  // Return an Array object that contains all the elements currently present
  // in the specified Growable Object Array. This is done by first truncating
  // the Growable Object Array's backing array to the currently used size and
  // returning the truncated backing array.
  // The remaining unused part of the backing array is marked as an Array
  // object or a regular Object so that it can be traversed during garbage
  // collection. The backing array of the original Growable Object Array is
  // set to an empty array.
  static RawArray* MakeArray(const GrowableObjectArray& growable_array);

 protected:
  static RawArray* New(intptr_t class_id,
                       intptr_t len,
                       Heap::Space space = Heap::kNew);

 private:
  RawObject** ObjectAddr(intptr_t index) const {
    // TODO(iposva): Determine if we should throw an exception here.
    ASSERT((index >= 0) && (index < Length()));
    return &raw_ptr()->data()[index];
  }

  void SetLength(intptr_t value) const {
    // This is only safe because we create a new Smi, which does not cause
    // heap allocation.
    raw_ptr()->length_ = Smi::New(value);
  }

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Array, Instance);
  friend class Class;
  friend class ImmutableArray;
  friend class Object;
  friend class String;
};


class ImmutableArray : public AllStatic {
 public:
  static RawImmutableArray* New(intptr_t len, Heap::Space space = Heap::kNew);

  static RawImmutableArray* ReadFrom(SnapshotReader* reader,
                                     intptr_t object_id,
                                     intptr_t tags,
                                     Snapshot::Kind kind);

  static const ClassId kClassId = kImmutableArrayCid;

  static intptr_t InstanceSize() {
    return Array::InstanceSize();
  }

  static intptr_t InstanceSize(intptr_t len) {
    return Array::InstanceSize(len);
  }

 private:
  static RawImmutableArray* raw(const Array& array) {
    return reinterpret_cast<RawImmutableArray*>(array.raw());
  }

  friend class Class;
};


class GrowableObjectArray : public Instance {
 public:
  intptr_t Capacity() const {
    NoGCScope no_gc;
    ASSERT(!IsNull());
    return Smi::Value(DataArray()->length_);
  }
  intptr_t Length() const {
    ASSERT(!IsNull());
    return Smi::Value(raw_ptr()->length_);
  }
  void SetLength(intptr_t value) const {
    // This is only safe because we create a new Smi, which does not cause
    // heap allocation.
    raw_ptr()->length_ = Smi::New(value);
  }

  RawArray* data() const { return raw_ptr()->data_; }
  void SetData(const Array& value) const {
    StorePointer(&raw_ptr()->data_, value.raw());
  }

  RawObject* At(intptr_t index) const {
    NoGCScope no_gc;
    ASSERT(!IsNull());
    ASSERT(index < Length());
    return *ObjectAddr(index);
  }
  void SetAt(intptr_t index, const Object& value) const {
    ASSERT(!IsNull());
    ASSERT(index < Length());

    // TODO(iposva): Add storing NoGCScope.
    DataStorePointer(ObjectAddr(index), value.raw());
  }

  void Add(const Object& value, Heap::Space space = Heap::kNew) const;

  void Grow(intptr_t new_capacity, Heap::Space space = Heap::kNew) const;
  RawObject* RemoveLast() const;

  virtual RawAbstractTypeArguments* GetTypeArguments() const {
    return raw_ptr()->type_arguments_;
  }
  virtual void SetTypeArguments(const AbstractTypeArguments& value) const {
    // A GrowableObjectArray is raw or takes one type argument. However, its
    // type argument vector may be longer than 1 due to a type optimization
    // reusing the type argument vector of the instantiator.
    ASSERT(value.IsNull() || ((value.Length() >= 1) && value.IsInstantiated()));
    const Array& contents = Array::Handle(data());
    contents.SetTypeArguments(value);
    StorePointer(&raw_ptr()->type_arguments_, value.raw());
  }

  virtual bool Equals(const Instance& other) const;

  virtual RawInstance* CheckAndCanonicalize(const char** error_str) const {
    UNREACHABLE();
    return Instance::null();
  }

  static intptr_t type_arguments_offset() {
    return OFFSET_OF(RawGrowableObjectArray, type_arguments_);
  }

  static intptr_t length_offset() {
    return OFFSET_OF(RawGrowableObjectArray, length_);
  }
  static intptr_t data_offset() {
    return OFFSET_OF(RawGrowableObjectArray, data_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawGrowableObjectArray));
  }

  static RawGrowableObjectArray* New(Heap::Space space = Heap::kNew) {
    return New(kDefaultInitialCapacity, space);
  }
  static RawGrowableObjectArray* New(intptr_t capacity,
                                     Heap::Space space = Heap::kNew);
  static RawGrowableObjectArray* New(const Array& array,
                                     Heap::Space space = Heap::kNew);

 private:
  RawArray* DataArray() const { return data()->ptr(); }
  RawObject** ObjectAddr(intptr_t index) const {
    ASSERT((index >= 0) && (index < Length()));
    return &(DataArray()->data()[index]);
  }
  bool DataContains(uword addr) const {
    intptr_t data_size = data()->Size();
    uword data_addr = RawObject::ToAddr(data());
    return (addr >= data_addr) && (addr < (data_addr + data_size));
  }
  void DataStorePointer(RawObject** addr, RawObject* value) const {
    // Ensure that the backing array object contains the addr.
    ASSERT(DataContains(reinterpret_cast<uword>(addr)));
    *addr = value;
    // Filter stores based on source and target.
    if (!value->IsHeapObject()) return;
    if (value->IsNewObject() && data()->IsOldObject() &&
        !data()->IsRemembered()) {
      data()->SetRememberedBit();
      Isolate::Current()->store_buffer()->AddObject(data());
    }
  }

  static const int kDefaultInitialCapacity = 4;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(GrowableObjectArray, Instance);
  friend class Array;
  friend class Class;
};


class Float32x4 : public Instance {
 public:
  static RawFloat32x4* New(float value0, float value1, float value2,
                           float value3, Heap::Space space = Heap::kNew);
  static RawFloat32x4* New(simd128_value_t value,
                           Heap::Space space = Heap::kNew);

  float x() const;
  float y() const;
  float z() const;
  float w() const;

  void set_x(float x) const;
  void set_y(float y) const;
  void set_z(float z) const;
  void set_w(float w) const;

  simd128_value_t value() const;
  void set_value(simd128_value_t value) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawFloat32x4));
  }

  static intptr_t value_offset() {
    return OFFSET_OF(RawFloat32x4, value_);
  }

 private:
  FINAL_HEAP_OBJECT_IMPLEMENTATION(Float32x4, Instance);
  friend class Class;
};


class Uint32x4 : public Instance {
 public:
  static RawUint32x4* New(uint32_t value0, uint32_t value1, uint32_t value2,
                          uint32_t value3, Heap::Space space = Heap::kNew);
  static RawUint32x4* New(simd128_value_t value,
                          Heap::Space space = Heap::kNew);

  uint32_t x() const;
  uint32_t y() const;
  uint32_t z() const;
  uint32_t w() const;

  void set_x(uint32_t x) const;
  void set_y(uint32_t y) const;
  void set_z(uint32_t z) const;
  void set_w(uint32_t w) const;

  simd128_value_t value() const;
  void set_value(simd128_value_t value) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawUint32x4));
  }

  static intptr_t value_offset() {
    return OFFSET_OF(RawUint32x4, value_);
  }

 private:
  FINAL_HEAP_OBJECT_IMPLEMENTATION(Uint32x4, Instance);
  friend class Class;
};


class TypedData : public Instance {
 public:
  intptr_t Length() const {
    ASSERT(!IsNull());
    return Smi::Value(raw_ptr()->length_);
  }

  intptr_t ElementSizeInBytes() const {
    intptr_t cid = raw()->GetClassId();
    return ElementSizeInBytes(cid);
  }


  TypedDataElementType ElementType() const {
    intptr_t cid = raw()->GetClassId();
    return ElementType(cid);
  }

  intptr_t LengthInBytes() const {
    intptr_t cid = raw()->GetClassId();
    return (ElementSizeInBytes(cid) * Length());
  }

  void* DataAddr(intptr_t byte_offset) const {
    ASSERT((byte_offset == 0) ||
           ((byte_offset > 0) && (byte_offset < LengthInBytes())));
    return reinterpret_cast<void*>(raw_ptr()->data_ + byte_offset);
  }

#define TYPED_GETTER_SETTER(name, type)                                        \
  type Get##name(intptr_t byte_offset) const {                                 \
    return *reinterpret_cast<type*>(DataAddr(byte_offset));                    \
  }                                                                            \
  void Set##name(intptr_t byte_offset, type value) const {                     \
    *reinterpret_cast<type*>(DataAddr(byte_offset)) = value;                   \
  }
  TYPED_GETTER_SETTER(Int8, int8_t)
  TYPED_GETTER_SETTER(Uint8, uint8_t)
  TYPED_GETTER_SETTER(Int16, int16_t)
  TYPED_GETTER_SETTER(Uint16, uint16_t)
  TYPED_GETTER_SETTER(Int32, int32_t)
  TYPED_GETTER_SETTER(Uint32, uint32_t)
  TYPED_GETTER_SETTER(Int64, int64_t)
  TYPED_GETTER_SETTER(Uint64, uint64_t)
  TYPED_GETTER_SETTER(Float32, float)
  TYPED_GETTER_SETTER(Float64, double)
  TYPED_GETTER_SETTER(Float32x4, simd128_value_t)
  TYPED_GETTER_SETTER(Uint32x4, simd128_value_t)

#undef TYPED_GETTER_SETTER

  static intptr_t length_offset() {
    return OFFSET_OF(RawTypedData, length_);
  }

  static intptr_t data_offset() {
    return OFFSET_OF(RawTypedData, data_);
  }

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawTypedData) == OFFSET_OF(RawTypedData, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t lengthInBytes) {
    ASSERT(0 <= lengthInBytes && lengthInBytes <= kSmiMax);
    return RoundedAllocationSize(sizeof(RawTypedData) + lengthInBytes);
  }

  static intptr_t ElementSizeInBytes(intptr_t class_id) {
    ASSERT(RawObject::IsTypedDataClassId(class_id));
    return element_size[ElementType(class_id)];
  }

  static TypedDataElementType ElementType(intptr_t class_id) {
    ASSERT(RawObject::IsTypedDataClassId(class_id));
    return static_cast<TypedDataElementType>(
        class_id - kTypedDataInt8ArrayCid);
  }

  static intptr_t MaxElements(intptr_t class_id) {
    ASSERT(RawObject::IsTypedDataClassId(class_id));
    return (kSmiMax / ElementSizeInBytes(class_id));
  }

  static RawTypedData* New(intptr_t class_id,
                           intptr_t len,
                           Heap::Space space = Heap::kNew);

  template <typename DstType, typename SrcType>
  static void Copy(const DstType& dst, intptr_t dst_offset_in_bytes,
                   const SrcType& src, intptr_t src_offset_in_bytes,
                   intptr_t length_in_bytes) {
    ASSERT(dst.ElementType() == src.ElementType());
    ASSERT(Utils::RangeCheck(src_offset_in_bytes,
                             length_in_bytes,
                             src.LengthInBytes()));
    ASSERT(Utils::RangeCheck(dst_offset_in_bytes,
                             length_in_bytes,
                             dst.LengthInBytes()));
    {
      NoGCScope no_gc;
      if (length_in_bytes > 0) {
        memmove(dst.DataAddr(dst_offset_in_bytes),
                src.DataAddr(src_offset_in_bytes),
                length_in_bytes);
      }
    }
  }

  static bool IsTypedData(const Instance& obj) {
    ASSERT(!obj.IsNull());
    intptr_t cid = obj.raw()->GetClassId();
    return RawObject::IsTypedDataClassId(cid);
  }

 protected:
  void SetLength(intptr_t value) const {
    raw_ptr()->length_ = Smi::New(value);
  }

 private:
  static const intptr_t element_size[];

  FINAL_HEAP_OBJECT_IMPLEMENTATION(TypedData, Instance);
  friend class Class;
  friend class ExternalTypedData;
  friend class TypedDataView;
};


class ExternalTypedData : public Instance {
 public:
  intptr_t Length() const {
    ASSERT(!IsNull());
    return Smi::Value(raw_ptr()->length_);
  }

  intptr_t ElementSizeInBytes() const {
    intptr_t cid = raw()->GetClassId();
    return ElementSizeInBytes(cid);
  }

  TypedDataElementType ElementType() const {
    intptr_t cid = raw()->GetClassId();
    return ElementType(cid);
  }

  intptr_t LengthInBytes() const {
    intptr_t cid = raw()->GetClassId();
    return (ElementSizeInBytes(cid) * Length());
  }

  void* GetPeer() const {
    return raw_ptr()->peer_;
  }

  void* DataAddr(intptr_t byte_offset) const {
    ASSERT((byte_offset == 0) ||
           ((byte_offset > 0) && (byte_offset < LengthInBytes())));
    return reinterpret_cast<void*>(raw_ptr()->data_ + byte_offset);
  }

#define TYPED_GETTER_SETTER(name, type)                                        \
  type Get##name(intptr_t byte_offset) const {                                 \
    return *reinterpret_cast<type*>(DataAddr(byte_offset));                    \
  }                                                                            \
  void Set##name(intptr_t byte_offset, type value) const {                     \
    *reinterpret_cast<type*>(DataAddr(byte_offset)) = value;                   \
  }
  TYPED_GETTER_SETTER(Int8, int8_t)
  TYPED_GETTER_SETTER(Uint8, uint8_t)
  TYPED_GETTER_SETTER(Int16, int16_t)
  TYPED_GETTER_SETTER(Uint16, uint16_t)
  TYPED_GETTER_SETTER(Int32, int32_t)
  TYPED_GETTER_SETTER(Uint32, uint32_t)
  TYPED_GETTER_SETTER(Int64, int64_t)
  TYPED_GETTER_SETTER(Uint64, uint64_t)
  TYPED_GETTER_SETTER(Float32, float)
  TYPED_GETTER_SETTER(Float64, double)
  TYPED_GETTER_SETTER(Float32x4, simd128_value_t)
  TYPED_GETTER_SETTER(Uint32x4, simd128_value_t);

#undef TYPED_GETTER_SETTER

  FinalizablePersistentHandle* AddFinalizer(
      void* peer, Dart_WeakPersistentHandleFinalizer callback) const;

  static intptr_t length_offset() {
    return OFFSET_OF(RawExternalTypedData, length_);
  }

  static intptr_t data_offset() {
    return OFFSET_OF(RawExternalTypedData, data_);
  }

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawExternalTypedData));
  }

  static intptr_t ElementSizeInBytes(intptr_t class_id) {
    ASSERT(RawObject::IsExternalTypedDataClassId(class_id));
    return TypedData::element_size[ElementType(class_id)];
  }

  static TypedDataElementType ElementType(intptr_t class_id) {
    ASSERT(RawObject::IsExternalTypedDataClassId(class_id));
    return static_cast<TypedDataElementType>(
        class_id - kExternalTypedDataInt8ArrayCid);
  }

  static intptr_t MaxElements(intptr_t class_id) {
    ASSERT(RawObject::IsExternalTypedDataClassId(class_id));
    return (kSmiMax / ElementSizeInBytes(class_id));
  }

  static RawExternalTypedData* New(intptr_t class_id,
                                   uint8_t* data,
                                   intptr_t len,
                                   Heap::Space space = Heap::kNew);

  static bool IsExternalTypedData(const Instance& obj) {
    ASSERT(!obj.IsNull());
    intptr_t cid = obj.raw()->GetClassId();
    return RawObject::IsExternalTypedDataClassId(cid);
  }

 protected:
  void SetLength(intptr_t value) const {
    raw_ptr()->length_ = Smi::New(value);
  }

  void SetData(uint8_t* data) const {
    raw_ptr()->data_ = data;
  }

  void SetPeer(void* peer) const {
    raw_ptr()->peer_ = peer;
  }

 private:
  FINAL_HEAP_OBJECT_IMPLEMENTATION(ExternalTypedData, Instance);
  friend class Class;
};


class TypedDataView : public AllStatic {
 public:
  static intptr_t ElementSizeInBytes(const Instance& view_obj) {
    ASSERT(!view_obj.IsNull());
    intptr_t cid = view_obj.raw()->GetClassId();
    return ElementSizeInBytes(cid);
  }

  static RawInstance* Data(const Instance& view_obj) {
    ASSERT(!view_obj.IsNull());
    return *reinterpret_cast<RawInstance**>(view_obj.raw_ptr() + kDataOffset);
  }

  static RawSmi* OffsetInBytes(const Instance& view_obj) {
    ASSERT(!view_obj.IsNull());
    return *reinterpret_cast<RawSmi**>(
        view_obj.raw_ptr() + kOffsetInBytesOffset);
  }

  static RawSmi* Length(const Instance& view_obj) {
    ASSERT(!view_obj.IsNull());
    return *reinterpret_cast<RawSmi**>(view_obj.raw_ptr() + kLengthOffset);
  }

  static bool IsExternalTypedDataView(const Instance& view_obj) {
    const Instance& data = Instance::Handle(Data(view_obj));
    intptr_t cid = data.raw()->GetClassId();
    ASSERT(RawObject::IsTypedDataClassId(cid) ||
           RawObject::IsExternalTypedDataClassId(cid));
    return RawObject::IsExternalTypedDataClassId(cid);
  }

  static intptr_t NumberOfFields() {
    return (kLengthOffset - kTypeArguments);
  }

  static intptr_t data_offset() {
    return kWordSize * kDataOffset;
  }

  static intptr_t offset_in_bytes_offset() {
    return kWordSize * kOffsetInBytesOffset;
  }

  static intptr_t length_offset() {
    return kWordSize * kLengthOffset;
  }

  static intptr_t ElementSizeInBytes(intptr_t class_id) {
    ASSERT(RawObject::IsTypedDataViewClassId(class_id));
    return (class_id == kByteDataViewCid) ?
        TypedData::element_size[kTypedDataInt8ArrayCid] :
        TypedData::element_size[class_id - kTypedDataInt8ArrayViewCid];
  }

 private:
  enum {
    kTypeArguments = 1,
    kDataOffset = 2,
    kOffsetInBytesOffset = 3,
    kLengthOffset = 4,
  };
};


class Closure : public AllStatic {
 public:
  static RawFunction* function(const Instance& closure) {
    return *FunctionAddr(closure);
  }
  static intptr_t function_offset() {
    return static_cast<intptr_t>(kFunctionOffset * kWordSize);
  }

  static RawContext* context(const Instance& closure) {
    return *ContextAddr(closure);
  }
  static intptr_t context_offset() {
    return static_cast<intptr_t>(kContextOffset * kWordSize);
  }

  static RawAbstractTypeArguments* GetTypeArguments(const Instance& closure) {
    return *TypeArgumentsAddr(closure);
  }
  static void SetTypeArguments(const Instance& closure,
                               const AbstractTypeArguments& value) {
    closure.StorePointer(TypeArgumentsAddr(closure), value.raw());
  }
  static intptr_t type_arguments_offset() {
    return static_cast<intptr_t>(kTypeArgumentsOffset * kWordSize);
  }

  static const char* ToCString(const Instance& closure);

  static intptr_t InstanceSize() {
    intptr_t size = sizeof(RawInstance) + (kNumFields * kWordSize);
    ASSERT(size == Object::RoundedAllocationSize(size));
    return size;
  }

  static RawInstance* New(const Function& function,
                          const Context& context,
                          Heap::Space space = Heap::kNew);

 private:
  static const int kTypeArgumentsOffset = 1;
  static const int kFunctionOffset = 2;
  static const int kContextOffset = 3;
  static const int kNumFields = 3;

  static RawAbstractTypeArguments** TypeArgumentsAddr(const Instance& obj) {
    ASSERT(obj.IsClosure());
    return reinterpret_cast<RawAbstractTypeArguments**>(
        reinterpret_cast<intptr_t>(obj.raw_ptr()) + type_arguments_offset());
  }
  static RawFunction** FunctionAddr(const Instance& obj) {
    ASSERT(obj.IsClosure());
    return reinterpret_cast<RawFunction**>(
        reinterpret_cast<intptr_t>(obj.raw_ptr()) + function_offset());
  }
  static RawContext** ContextAddr(const Instance& obj) {
    ASSERT(obj.IsClosure());
    return reinterpret_cast<RawContext**>(
        reinterpret_cast<intptr_t>(obj.raw_ptr()) + context_offset());
  }
  static void set_function(const Instance& closure,
                           const Function& value) {
    closure.StorePointer(FunctionAddr(closure), value.raw());
  }
  static void set_context(const Instance& closure,
                          const Context& value) {
    closure.StorePointer(ContextAddr(closure), value.raw());
  }

  friend class Class;
};


// Internal stacktrace object used in exceptions for printing stack traces.
class Stacktrace : public Instance {
 public:
  static const int kPreallocatedStackdepth = 10;

  intptr_t Length() const;

  RawFunction* FunctionAtFrame(intptr_t frame_index) const;

  RawCode* CodeAtFrame(intptr_t frame_index) const;
  void SetCodeAtFrame(intptr_t frame_index, const Code& code) const;

  RawSmi* PcOffsetAtFrame(intptr_t frame_index) const;
  void SetPcOffsetAtFrame(intptr_t frame_index, const Smi& pc_offset) const;
  void SetCatchStacktrace(const Array& code_array,
                          const Array& pc_offset_array) const;
  void set_expand_inlined(bool value) const;

  void Append(const Array& code_list, const Array& pc_offset_list) const;

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawStacktrace));
  }
  static RawStacktrace* New(const Array& code_array,
                            const Array& pc_offset_array,
                            Heap::Space space = Heap::kNew);

  RawString* FullStacktrace() const;
  const char* ToCStringInternal(intptr_t* frame_index) const;

 private:
  void set_code_array(const Array& code_array) const;
  void set_pc_offset_array(const Array& pc_offset_array) const;
  void set_catch_code_array(const Array& code_array) const;
  void set_catch_pc_offset_array(const Array& pc_offset_array) const;
  bool expand_inlined() const;

  FINAL_HEAP_OBJECT_IMPLEMENTATION(Stacktrace, Instance);
  friend class Class;
};


// Internal JavaScript regular expression object.
class JSRegExp : public Instance {
 public:
  // Meaning of RegExType:
  // kUninitialized: the type of th regexp has not been initialized yet.
  // kSimple: A simple pattern to match against, using string indexOf operation.
  // kComplex: A complex pattern to match.
  enum RegExType {
    kUnitialized = 0,
    kSimple,
    kComplex,
  };

  // Flags are passed to a regex object as follows:
  // 'i': ignore case, 'g': do global matches, 'm': pattern is multi line.
  enum Flags {
    kNone = 0,
    kGlobal = 1,
    kIgnoreCase = 2,
    kMultiLine = 4,
  };

  bool is_initialized() const { return (raw_ptr()->type_ != kUnitialized); }
  bool is_simple() const { return (raw_ptr()->type_ == kSimple); }
  bool is_complex() const { return (raw_ptr()->type_ == kComplex); }

  bool is_global() const { return (raw_ptr()->flags_ & kGlobal); }
  bool is_ignore_case() const { return (raw_ptr()->flags_ & kIgnoreCase); }
  bool is_multi_line() const { return (raw_ptr()->flags_ & kMultiLine); }

  RawString* pattern() const { return raw_ptr()->pattern_; }
  RawSmi* num_bracket_expressions() const {
    return raw_ptr()->num_bracket_expressions_;
  }

  void set_pattern(const String& pattern) const;
  void set_num_bracket_expressions(intptr_t value) const;
  void set_is_global() const { raw_ptr()->flags_ |= kGlobal; }
  void set_is_ignore_case() const { raw_ptr()->flags_ |= kIgnoreCase; }
  void set_is_multi_line() const { raw_ptr()->flags_ |= kMultiLine; }
  void set_is_simple() const { raw_ptr()->type_ = kSimple; }
  void set_is_complex() const { raw_ptr()->type_ = kComplex; }

  void* GetDataStartAddress() const;
  static RawJSRegExp* FromDataStartAddress(void* data);
  const char* Flags() const;

  virtual bool Equals(const Instance& other) const;

  static const intptr_t kBytesPerElement = 1;
  static const intptr_t kMaxElements = kSmiMax / kBytesPerElement;

  static intptr_t InstanceSize() {
    ASSERT(sizeof(RawJSRegExp) == OFFSET_OF(RawJSRegExp, data_));
    return 0;
  }

  static intptr_t InstanceSize(intptr_t len) {
    ASSERT(0 <= len && len <= kMaxElements);
    return RoundedAllocationSize(
        sizeof(RawJSRegExp) + (len * kBytesPerElement));
  }

  static RawJSRegExp* New(intptr_t length, Heap::Space space = Heap::kNew);

 private:
  void set_type(RegExType type) const { raw_ptr()->type_ = type; }
  void set_flags(intptr_t value) const { raw_ptr()->flags_ = value; }

  void SetLength(intptr_t value) const {
    // This is only safe because we create a new Smi, which does not cause
    // heap allocation.
    raw_ptr()->data_length_ = Smi::New(value);
  }

  FINAL_HEAP_OBJECT_IMPLEMENTATION(JSRegExp, Instance);
  friend class Class;
};


class WeakProperty : public Instance {
 public:
  RawObject* key() const {
    return raw_ptr()->key_;
  }

  void set_key(const Object& key) const {
    StorePointer(&raw_ptr()->key_, key.raw());
  }

  RawObject* value() const {
    return raw_ptr()->value_;
  }

  void set_value(const Object& value) const {
    StorePointer(&raw_ptr()->value_, value.raw());
  }

  static RawWeakProperty* New(Heap::Space space = Heap::kNew);

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawWeakProperty));
  }

  static void Clear(RawWeakProperty* raw_weak) {
    raw_weak->ptr()->key_ = Object::null();
    raw_weak->ptr()->value_ = Object::null();
  }

 private:
  FINAL_HEAP_OBJECT_IMPLEMENTATION(WeakProperty, Instance);
  friend class Class;
};


class MirrorReference : public Instance {
 public:
  RawObject* referent() const {
    return raw_ptr()->referent_;
  }

  void set_referent(const Object& referent) const {
    StorePointer(&raw_ptr()->referent_, referent.raw());
  }

  RawAbstractType* GetAbstractTypeReferent() const;

  RawClass* GetClassReferent() const;

  RawField* GetFieldReferent() const;

  RawFunction* GetFunctionReferent() const;

  RawLibrary* GetLibraryReferent() const;

  RawTypeParameter* GetTypeParameterReferent() const;

  static RawMirrorReference* New(const Object& referent,
                                 Heap::Space space = Heap::kNew);

  static intptr_t InstanceSize() {
    return RoundedAllocationSize(sizeof(RawMirrorReference));
  }

 private:
  FINAL_HEAP_OBJECT_IMPLEMENTATION(MirrorReference, Instance);
  friend class Class;
};


// Breaking cycles and loops.
RawClass* Object::clazz() const {
  uword raw_value = reinterpret_cast<uword>(raw_);
  if ((raw_value & kSmiTagMask) == kSmiTag) {
    return Smi::Class();
  }
  return Isolate::Current()->class_table()->At(raw()->GetClassId());
}


DART_FORCE_INLINE void Object::SetRaw(RawObject* value) {
  // NOTE: The assignment "raw_ = value" should be the first statement in
  // this function. Also do not use 'value' in this function after the
  // assignment (use 'raw_' instead).
  raw_ = value;
  if ((reinterpret_cast<uword>(value) & kSmiTagMask) == kSmiTag) {
    set_vtable(Smi::handle_vtable_);
    return;
  }
  intptr_t cid = value->GetClassId();
  if (cid >= kNumPredefinedCids) {
    cid = kInstanceCid;
  }
  set_vtable(builtin_vtables_[cid]);
#if defined(DEBUG)
  if (FLAG_verify_handles) {
    Isolate* isolate = Isolate::Current();
    Heap* isolate_heap = isolate->heap();
    Heap* vm_isolate_heap = Dart::vm_isolate()->heap();
    ASSERT(isolate_heap->Contains(RawObject::ToAddr(raw_)) ||
           vm_isolate_heap->Contains(RawObject::ToAddr(raw_)));
  }
#endif
}


bool Function::HasCode() const {
  return raw_ptr()->code_ != Code::null();
}


intptr_t Field::Offset() const {
  ASSERT(!is_static());  // Offset is valid only for instance fields.
  intptr_t value = Smi::Value(reinterpret_cast<RawSmi*>(raw_ptr()->value_));
  return (value * kWordSize);
}


void Field::SetOffset(intptr_t value_in_bytes) const {
  ASSERT(!is_static());  // SetOffset is valid only for instance fields.
  ASSERT(kWordSize != 0);
  raw_ptr()->value_ = Smi::New(value_in_bytes / kWordSize);
}


void Context::SetAt(intptr_t index, const Instance& value) const {
  StorePointer(InstanceAddr(index), value.raw());
}


intptr_t Instance::GetNativeField(Isolate* isolate, int index) const {
  ASSERT(IsValidNativeIndex(index));
  NoGCScope no_gc;
  RawTypedData* native_fields =
      reinterpret_cast<RawTypedData*>(*NativeFieldsAddr());
  if (native_fields == TypedData::null()) {
    return 0;
  }
  return reinterpret_cast<intptr_t*>(native_fields->ptr()->data_)[index];
}


bool String::Equals(const String& str) const {
  if (raw() == str.raw()) {
    return true;  // Both handles point to the same raw instance.
  }
  if (str.IsNull()) {
    return false;
  }
  return Equals(str, 0, str.Length());
}


bool String::Equals(const String& str,
                    intptr_t begin_index,
                    intptr_t len) const {
  ASSERT(begin_index >= 0);
  ASSERT((begin_index == 0) || (begin_index < str.Length()));
  ASSERT(len >= 0);
  ASSERT(len <= str.Length());
  if (len != this->Length()) {
    return false;  // Lengths don't match.
  }
  for (intptr_t i = 0; i < len; i++) {
    if (this->CharAt(i) != str.CharAt(begin_index + i)) {
      return false;
    }
  }
  return true;
}


void MegamorphicCache::SetEntry(const Array& array,
                                intptr_t index,
                                const Smi& class_id,
                                const Function& target) {
  array.SetAt((index * kEntryLength) + kClassIdIndex, class_id);
  array.SetAt((index * kEntryLength) + kTargetFunctionIndex, target);
}


RawObject* MegamorphicCache::GetClassId(const Array& array, intptr_t index) {
  return array.At((index * kEntryLength) + kClassIdIndex);
}


RawObject* MegamorphicCache::GetTargetFunction(const Array& array,
                                               intptr_t index) {
  return array.At((index * kEntryLength) + kTargetFunctionIndex);
}

}  // namespace dart

#endif  // VM_OBJECT_H_
