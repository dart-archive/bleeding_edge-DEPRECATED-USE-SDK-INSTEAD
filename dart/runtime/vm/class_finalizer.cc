// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/class_finalizer.h"

#include "vm/flags.h"
#include "vm/heap.h"
#include "vm/isolate.h"
#include "vm/longjump.h"
#include "vm/object_store.h"
#include "vm/parser.h"
#include "vm/symbols.h"

namespace dart {

DEFINE_FLAG(bool, error_on_bad_override, false,
            "Report error for bad overrides.");
DEFINE_FLAG(bool, error_on_bad_type, false,
            "Report error for malformed types.");
DEFINE_FLAG(bool, print_classes, false, "Prints details about loaded classes.");
DEFINE_FLAG(bool, trace_class_finalization, false, "Trace class finalization.");
DEFINE_FLAG(bool, trace_type_finalization, false, "Trace type finalization.");
DECLARE_FLAG(bool, enable_type_checks);
DECLARE_FLAG(bool, use_cha);

bool ClassFinalizer::AllClassesFinalized() {
  ObjectStore* object_store = Isolate::Current()->object_store();
  const GrowableObjectArray& classes =
      GrowableObjectArray::Handle(object_store->pending_classes());
  return classes.Length() == 0;
}


// Removes optimized code once we load more classes, since --use_cha based
// optimizations may have become invalid.
// Only methods which owner classes where subclasses can be invalid.
// TODO(srdjan): Be even more precise by recording the exact CHA optimization.
static void RemoveOptimizedCode(
    const GrowableArray<intptr_t>& added_subclasses_to_cids) {
  ASSERT(FLAG_use_cha);
  if (added_subclasses_to_cids.is_empty()) return;
  // Deoptimize all live frames.
  DeoptimizeIfOwner(added_subclasses_to_cids);
  // Switch all functions' code to unoptimized.
  const ClassTable& class_table = *Isolate::Current()->class_table();
  Class& cls = Class::Handle();
  Array& array = Array::Handle();
  Function& function = Function::Handle();
  for (intptr_t i = 0; i < added_subclasses_to_cids.length(); i++) {
    intptr_t cid = added_subclasses_to_cids[i];
    cls = class_table.At(cid);
    ASSERT(!cls.IsNull());
    array = cls.functions();
    const intptr_t num_functions = array.IsNull() ? 0 : array.Length();
    for (intptr_t f = 0; f < num_functions; f++) {
      function ^= array.At(f);
      ASSERT(!function.IsNull());
      if (function.HasOptimizedCode()) {
        function.SwitchToUnoptimizedCode();
      }
    }
  }
}


void AddSuperType(const AbstractType& type,
                  GrowableArray<intptr_t>* finalized_super_classes) {
  ASSERT(type.HasResolvedTypeClass());
  ASSERT(!type.IsDynamicType());
  if (type.IsObjectType()) {
    return;
  }
  const Class& cls = Class::Handle(type.type_class());
  ASSERT(cls.is_finalized());
  const intptr_t cid = cls.id();
  for (intptr_t i = 0; i < finalized_super_classes->length(); i++) {
    if ((*finalized_super_classes)[i] == cid) {
      // Already added.
      return;
    }
  }
  finalized_super_classes->Add(cid);
  const AbstractType& super_type = AbstractType::Handle(cls.super_type());
  AddSuperType(super_type, finalized_super_classes);
}


// Use array instead of set since we expect very few subclassed classes
// to occur.
static void CollectFinalizedSuperClasses(
    const GrowableObjectArray& pending_classes,
    GrowableArray<intptr_t>* finalized_super_classes) {
  Class& cls = Class::Handle();
  AbstractType& super_type = Type::Handle();
  for (intptr_t i = 0; i < pending_classes.Length(); i++) {
    cls ^= pending_classes.At(i);
    ASSERT(!cls.is_finalized());
    super_type = cls.super_type();
    if (!super_type.IsNull()) {
      if (!super_type.IsMalformed() && super_type.HasResolvedTypeClass()) {
        cls ^= super_type.type_class();
        if (cls.is_finalized()) {
          AddSuperType(super_type, finalized_super_classes);
        }
      }
    }
  }
}


// Class finalization occurs:
// a) when bootstrap process completes (VerifyBootstrapClasses).
// b) after the user classes are loaded (dart_api).
bool ClassFinalizer::FinalizePendingClasses() {
  bool retval = true;
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate != NULL);
  HANDLESCOPE(isolate);
  ObjectStore* object_store = isolate->object_store();
  const Error& error = Error::Handle(isolate, object_store->sticky_error());
  if (!error.IsNull()) {
    return false;
  }
  if (AllClassesFinalized()) {
    return true;
  }

  GrowableArray<intptr_t> added_subclasses_to_cids;
  LongJump* base = isolate->long_jump_base();
  LongJump jump;
  isolate->set_long_jump_base(&jump);
  if (setjmp(*jump.Set()) == 0) {
    GrowableObjectArray& class_array = GrowableObjectArray::Handle();
    class_array = object_store->pending_classes();
    ASSERT(!class_array.IsNull());
    // Collect superclasses that were already finalized before this run of
    // finalization.
    CollectFinalizedSuperClasses(class_array, &added_subclasses_to_cids);
    Class& cls = Class::Handle();
    // First resolve all superclasses.
    for (intptr_t i = 0; i < class_array.Length(); i++) {
      cls ^= class_array.At(i);
      GrowableArray<intptr_t> visited_interfaces;
      ResolveSuperTypeAndInterfaces(cls, &visited_interfaces);
    }
    // Finalize all classes.
    for (intptr_t i = 0; i < class_array.Length(); i++) {
      cls ^= class_array.At(i);
      FinalizeTypesInClass(cls);
    }
    if (FLAG_print_classes) {
      for (intptr_t i = 0; i < class_array.Length(); i++) {
        cls ^= class_array.At(i);
        PrintClassInformation(cls);
      }
    }
    // Clear pending classes array.
    class_array = GrowableObjectArray::New();
    object_store->set_pending_classes(class_array);
    VerifyImplicitFieldOffsets();  // Verification after an error may fail.
  } else {
    retval = false;
  }
  isolate->set_long_jump_base(base);
  if (FLAG_use_cha) {
    RemoveOptimizedCode(added_subclasses_to_cids);
  }
  return retval;
}


// Adds all interfaces of cls into 'collected'. Duplicate entries may occur.
// No cycles are allowed.
void ClassFinalizer::CollectInterfaces(const Class& cls,
                                       const GrowableObjectArray& collected) {
  const Array& interface_array = Array::Handle(cls.interfaces());
  AbstractType& interface = AbstractType::Handle();
  Class& interface_class = Class::Handle();
  for (intptr_t i = 0; i < interface_array.Length(); i++) {
    interface ^= interface_array.At(i);
    interface_class = interface.type_class();
    collected.Add(interface_class);
    CollectInterfaces(interface_class, collected);
  }
}


void ClassFinalizer::VerifyBootstrapClasses() {
  if (FLAG_trace_class_finalization) {
    OS::Print("VerifyBootstrapClasses START.\n");
  }
  ObjectStore* object_store = Isolate::Current()->object_store();

  Class& cls = Class::Handle();
#if defined(DEBUG)
  // Basic checking.
  cls = object_store->object_class();
  ASSERT(Instance::InstanceSize() == cls.instance_size());
  cls = object_store->integer_implementation_class();
  ASSERT(Integer::InstanceSize() == cls.instance_size());
  cls = object_store->smi_class();
  ASSERT(Smi::InstanceSize() == cls.instance_size());
  cls = object_store->mint_class();
  ASSERT(Mint::InstanceSize() == cls.instance_size());
  cls = object_store->bigint_class();
  ASSERT(Bigint::InstanceSize() == cls.instance_size());
  cls = object_store->one_byte_string_class();
  ASSERT(OneByteString::InstanceSize() == cls.instance_size());
  cls = object_store->two_byte_string_class();
  ASSERT(TwoByteString::InstanceSize() == cls.instance_size());
  cls = object_store->external_one_byte_string_class();
  ASSERT(ExternalOneByteString::InstanceSize() == cls.instance_size());
  cls = object_store->external_two_byte_string_class();
  ASSERT(ExternalTwoByteString::InstanceSize() == cls.instance_size());
  cls = object_store->double_class();
  ASSERT(Double::InstanceSize() == cls.instance_size());
  cls = object_store->bool_class();
  ASSERT(Bool::InstanceSize() == cls.instance_size());
  cls = object_store->array_class();
  ASSERT(Array::InstanceSize() == cls.instance_size());
  cls = object_store->immutable_array_class();
  ASSERT(ImmutableArray::InstanceSize() == cls.instance_size());
  cls = object_store->weak_property_class();
  ASSERT(WeakProperty::InstanceSize() == cls.instance_size());
#endif  // defined(DEBUG)

  // Remember the currently pending classes.
  const GrowableObjectArray& class_array =
      GrowableObjectArray::Handle(object_store->pending_classes());
  for (intptr_t i = 0; i < class_array.Length(); i++) {
    // TODO(iposva): Add real checks.
    cls ^= class_array.At(i);
    if (cls.is_finalized() || cls.is_prefinalized()) {
      // Pre-finalized bootstrap classes must not define any fields.
      ASSERT(!cls.HasInstanceFields());
    }
  }

  // Finalize classes that aren't pre-finalized by Object::Init().
  if (!FinalizePendingClasses()) {
    // TODO(srdjan): Exit like a real VM instead.
    const Error& err = Error::Handle(object_store->sticky_error());
    OS::PrintErr("Could not verify bootstrap classes : %s\n",
                 err.ToErrorCString());
    OS::Exit(255);
  }
  if (FLAG_trace_class_finalization) {
    OS::Print("VerifyBootstrapClasses END.\n");
  }
  Isolate::Current()->heap()->Verify();
}


// Resolve unresolved_class in the library of cls, or return null.
RawClass* ClassFinalizer::ResolveClass(
      const Class& cls,
      const UnresolvedClass& unresolved_class) {
  const String& class_name = String::Handle(unresolved_class.ident());
  Library& lib = Library::Handle();
  Class& resolved_class = Class::Handle();
  if (unresolved_class.library_prefix() == LibraryPrefix::null()) {
    lib = cls.library();
    ASSERT(!lib.IsNull());
    resolved_class = lib.LookupClass(class_name);
  } else {
    LibraryPrefix& lib_prefix = LibraryPrefix::Handle();
    lib_prefix = unresolved_class.library_prefix();
    ASSERT(!lib_prefix.IsNull());
    resolved_class = lib_prefix.LookupClass(class_name);
  }
  return resolved_class.raw();
}



void ClassFinalizer::ResolveRedirectingFactory(const Class& cls,
                                               const Function& factory) {
  const Function& target = Function::Handle(factory.RedirectionTarget());
  if (target.IsNull()) {
    Type& type = Type::Handle(factory.RedirectionType());
    if (!type.IsMalformed()) {
      const GrowableObjectArray& visited_factories =
          GrowableObjectArray::Handle(GrowableObjectArray::New());
      ResolveRedirectingFactoryTarget(cls, factory, visited_factories);
    }
    if (factory.is_const()) {
      type = factory.RedirectionType();
      if (type.IsMalformed()) {
        ReportError(Error::Handle(type.malformed_error()));
      }
    }
  }
}


void ClassFinalizer::ResolveRedirectingFactoryTarget(
    const Class& cls,
    const Function& factory,
    const GrowableObjectArray& visited_factories) {
  ASSERT(factory.IsRedirectingFactory());

  // Check for redirection cycle.
  for (intptr_t i = 0; i < visited_factories.Length(); i++) {
    if (visited_factories.At(i) == factory.raw()) {
      // A redirection cycle is reported as a compile-time error.
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, factory.token_pos(),
                  "factory '%s' illegally redirects to itself",
                  String::Handle(factory.name()).ToCString());
    }
  }
  visited_factories.Add(factory);

  // Check if target is already resolved.
  Type& type = Type::Handle(factory.RedirectionType());
  Function& target = Function::Handle(factory.RedirectionTarget());
  if (type.IsMalformed()) {
    // Already resolved to a malformed type. Will throw on usage.
    ASSERT(target.IsNull());
    return;
  }
  if (!target.IsNull()) {
    // Already resolved.
    return;
  }

  // Target is not resolved yet.
  if (FLAG_trace_class_finalization) {
    OS::Print("Resolving redirecting factory: %s\n",
              String::Handle(factory.name()).ToCString());
  }
  ResolveType(cls, type, kCanonicalize);
  type ^= FinalizeType(cls, type, kCanonicalize);
  factory.SetRedirectionType(type);
  if (type.IsMalformed()) {
    ASSERT(factory.RedirectionTarget() == Function::null());
    return;
  }
  ASSERT(!type.IsTypeParameter());  // Resolved in parser.
  if (type.IsDynamicType()) {
    // Replace the type with a malformed type and compile a throw when called.
    type = NewFinalizedMalformedType(
        Error::Handle(),  // No previous error.
        Script::Handle(cls.script()),
        factory.token_pos(),
        "factory may not redirect to 'dynamic'");
    factory.SetRedirectionType(type);
    ASSERT(factory.RedirectionTarget() == Function::null());
    return;
  }
  const Class& target_class = Class::Handle(type.type_class());
  String& target_class_name = String::Handle(target_class.Name());
  String& target_name = String::Handle(
      String::Concat(target_class_name, Symbols::Dot()));
  const String& identifier = String::Handle(factory.RedirectionIdentifier());
  if (!identifier.IsNull()) {
    target_name = String::Concat(target_name, identifier);
  }

  // Verify that the target constructor of the redirection exists.
  target = target_class.LookupConstructor(target_name);
  if (target.IsNull()) {
    target = target_class.LookupFactory(target_name);
  }
  if (target.IsNull()) {
    const String& user_visible_target_name =
        identifier.IsNull() ? target_class_name : target_name;
    // Replace the type with a malformed type and compile a throw when called.
    type = NewFinalizedMalformedType(
        Error::Handle(),  // No previous error.
        Script::Handle(target_class.script()),
        factory.token_pos(),
        "class '%s' has no constructor or factory named '%s'",
        target_class_name.ToCString(),
        user_visible_target_name.ToCString());
    factory.SetRedirectionType(type);
    ASSERT(factory.RedirectionTarget() == Function::null());
    return;
  }

  if (FLAG_error_on_bad_override) {
    // Verify that the target is compatible with the redirecting factory.
    Error& error = Error::Handle();
    if (!target.HasCompatibleParametersWith(factory, &error)) {
      const Script& script = Script::Handle(target_class.script());
      type = NewFinalizedMalformedType(
          error, script, target.token_pos(),
          "constructor '%s' has incompatible parameters with "
          "redirecting factory '%s'",
          String::Handle(target.name()).ToCString(),
          String::Handle(factory.name()).ToCString());
      factory.SetRedirectionType(type);
      ASSERT(factory.RedirectionTarget() == Function::null());
      return;
    }
  }

  // Verify that the target is const if the redirecting factory is const.
  if (factory.is_const() && !target.is_const()) {
    const Script& script = Script::Handle(target_class.script());
    ReportError(Error::Handle(),  // No previous error.
                script, target.token_pos(),
                "constructor '%s' must be const as required by redirecting "
                "const factory '%s'",
                String::Handle(target.name()).ToCString(),
                String::Handle(factory.name()).ToCString());
  }

  // Update redirection data with resolved target.
  factory.SetRedirectionTarget(target);
  // Not needed anymore.
  factory.SetRedirectionIdentifier(Object::null_string());
  if (!target.IsRedirectingFactory()) {
    return;
  }

  // The target is itself a redirecting factory. Recursively resolve its own
  // target and update the current redirection data to point to the end target
  // of the redirection chain.
  ResolveRedirectingFactoryTarget(target_class, target, visited_factories);
  Type& target_type = Type::Handle(target.RedirectionType());
  Function& target_target = Function::Handle(target.RedirectionTarget());
  if (target_target.IsNull()) {
    ASSERT(target_type.IsMalformed());
  } else {
    // If the target type refers to type parameters, substitute them with the
    // type arguments of the redirection type.
    if (!target_type.IsInstantiated()) {
      const AbstractTypeArguments& type_args = AbstractTypeArguments::Handle(
          type.arguments());
      Error& malformed_error = Error::Handle();
      target_type ^= target_type.InstantiateFrom(type_args, &malformed_error);
      if (malformed_error.IsNull()) {
        target_type ^= FinalizeType(cls, target_type, kCanonicalize);
      } else {
        const Script& script = Script::Handle(target_class.script());
        FinalizeMalformedType(malformed_error, script, target_type,
                              "cannot resolve redirecting factory");
        target_target = Function::null();
      }
    }
  }
  factory.SetRedirectionType(target_type);
  factory.SetRedirectionTarget(target_target);
}


void ClassFinalizer::ResolveType(const Class& cls,
                                 const AbstractType& type,
                                 FinalizationKind finalization) {
  if (type.IsResolved() || type.IsFinalized()) {
    return;
  }
  if (FLAG_trace_type_finalization) {
    OS::Print("Resolve type '%s'\n", String::Handle(type.Name()).ToCString());
  }

  // Resolve the type class.
  if (!type.HasResolvedTypeClass()) {
    // Type parameters are always resolved in the parser in the correct
    // non-static scope or factory scope. That resolution scope is unknown here.
    // Being able to resolve a type parameter from class cls here would indicate
    // that the type parameter appeared in a static scope. Leaving the type as
    // unresolved is the correct thing to do.

    // Lookup the type class.
    const UnresolvedClass& unresolved_class =
        UnresolvedClass::Handle(type.unresolved_class());
    const Class& type_class =
        Class::Handle(ResolveClass(cls, unresolved_class));

    // Replace unresolved class with resolved type class.
    const Type& parameterized_type = Type::Cast(type);
    if (type_class.IsNull()) {
      // The type class could not be resolved. The type is malformed.
      FinalizeMalformedType(
          Error::Handle(),  // No previous error.
          Script::Handle(cls.script()),
          parameterized_type,
          "cannot resolve class '%s' from '%s'",
          String::Handle(unresolved_class.Name()).ToCString(),
          String::Handle(cls.Name()).ToCString());
      return;
    }
    parameterized_type.set_type_class(type_class);
  }

  // Resolve type arguments, if any.
  const AbstractTypeArguments& arguments =
      AbstractTypeArguments::Handle(type.arguments());
  if (!arguments.IsNull()) {
    const intptr_t num_arguments = arguments.Length();
    AbstractType& type_argument = AbstractType::Handle();
    for (intptr_t i = 0; i < num_arguments; i++) {
      type_argument = arguments.TypeAt(i);
      ResolveType(cls, type_argument, finalization);
    }
  }
}


void ClassFinalizer::FinalizeTypeParameters(const Class& cls) {
  if (cls.IsMixinApplication()) {
    // Setup the type parameters of the mixin application and finalize the
    // mixin type.
    ApplyMixinType(cls);
  }
  // The type parameter bounds are not finalized here.
  const TypeArguments& type_parameters =
      TypeArguments::Handle(cls.type_parameters());
  if (!type_parameters.IsNull()) {
    TypeParameter& type_parameter = TypeParameter::Handle();
    const intptr_t num_types = type_parameters.Length();
    for (intptr_t i = 0; i < num_types; i++) {
      type_parameter ^= type_parameters.TypeAt(i);
      type_parameter ^= FinalizeType(cls,
                                     type_parameter,
                                     kCanonicalizeWellFormed);
      type_parameters.SetTypeAt(i, type_parameter);
    }
  }
}


// Finalize the type argument vector 'arguments' of the type defined by the
// class 'cls' parameterized with the type arguments 'cls_args'.
// The vector 'cls_args' is already initialized as a subvector at the correct
// position in the passed in 'arguments' vector.
// The subvector 'cls_args' has length cls.NumTypeParameters() and starts at
// offset cls.NumTypeArguments() - cls.NumTypeParameters() of the 'arguments'
// vector.
// The type argument vector of cls may overlap the type argument vector of its
// super class. In case of an overlap, the overlapped type arguments of the
// super class are already initialized. The still uninitialized ones have an
// offset smaller than 'num_uninitialized_arguments'.
// Example 1 (without overlap):
//   Declared: class C<K, V> extends B<V> { ... }
//             class B<T> extends A<int> { ... }
//   Input:    C<String, double> expressed as
//             cls = C, arguments = [dynamic, dynamic, String, double],
//             num_uninitialized_arguments = 2,
//             i.e. cls_args = [String, double], offset = 2, length = 2.
//   Output:   arguments = [int, double, String, double]
// Example 2 (with overlap):
//   Declared: class C<K, V> extends B<K> { ... }
//             class B<T> extends A<int> { ... }
//   Input:    C<String, double> expressed as
//             cls = C, arguments = [dynamic, String, double],
//             num_uninitialized_arguments = 1,
//             i.e. cls_args = [String, double], offset = 1, length = 2.
//   Output:   arguments = [int, String, double]
void ClassFinalizer::FinalizeTypeArguments(
    const Class& cls,
    const AbstractTypeArguments& arguments,
    intptr_t num_uninitialized_arguments,
    FinalizationKind finalization,
    Error* bound_error) {
  ASSERT(arguments.Length() >= cls.NumTypeArguments());
  if (!cls.is_type_finalized()) {
    FinalizeTypeParameters(cls);
    ResolveUpperBounds(cls);
  }
  AbstractType& super_type = AbstractType::Handle(cls.super_type());
  if (!super_type.IsNull()) {
    const Class& super_class = Class::Handle(super_type.type_class());
    AbstractTypeArguments& super_type_args = AbstractTypeArguments::Handle();
    if (super_type.IsBeingFinalized()) {
      // This type references itself via its type arguments. This is legal, but
      // we must avoid endless recursion. We therefore map the innermost
      // super type to dynamic.
      // Note that a direct self-reference via the super class chain is illegal
      // and reported as an error earlier.
      // Such legal self-references occur with F-bounded quantification.
      // Example 1: class Derived extends Base<Derived>.
      // The type 'Derived' forms a cycle by pointing to itself via its
      // flattened type argument vector: Derived[Derived[...]]
      // We break the cycle as follows: Derived[Derived[dynamic]]
      // Example 2: class Derived extends Base<Middle<Derived>> results in
      // Derived[Middle[Derived[dynamic]]]
      // Example 3: class Derived<T> extends Base<Derived<T>> results in
      // Derived[Derived[dynamic], T].
      ASSERT(super_type_args.IsNull());  // Same as a vector of dynamic.
    } else {
      super_type ^= FinalizeType(cls, super_type, finalization);
      cls.set_super_type(super_type);
      super_type_args = super_type.arguments();
    }
    const intptr_t num_super_type_params = super_class.NumTypeParameters();
    const intptr_t offset = super_class.NumTypeArguments();
    const intptr_t super_offset = offset - num_super_type_params;
    ASSERT(offset == (cls.NumTypeArguments() - cls.NumOwnTypeArguments()));
    AbstractType& super_type_arg = AbstractType::Handle(Type::DynamicType());
    for (intptr_t i = 0; super_offset + i < num_uninitialized_arguments; i++) {
      if (!super_type_args.IsNull()) {
        super_type_arg = super_type_args.TypeAt(super_offset + i);
        if (!super_type_arg.IsInstantiated()) {
          Error& malformed_error = Error::Handle();
          super_type_arg = super_type_arg.InstantiateFrom(arguments,
                                                          &malformed_error);
          if (!malformed_error.IsNull()) {
            if (!super_type_arg.IsInstantiated()) {
              // CheckTypeArgumentBounds will insert a BoundedType.
            } else if (bound_error->IsNull()) {
              *bound_error = malformed_error.raw();
            }
          }
        }
        if (finalization >= kCanonicalize) {
          super_type_arg = super_type_arg.Canonicalize();
        }
      }
      arguments.SetTypeAt(super_offset + i, super_type_arg);
    }
    FinalizeTypeArguments(super_class, arguments, super_offset,
                          finalization, bound_error);
  }
}


// Check the type argument vector 'arguments' against the corresponding bounds
// of the type parameters of class 'cls' and, recursively, of its superclasses.
// Replace a type argument that cannot be checked at compile time by a
// BoundedType, thereby postponing the bound check to run time.
// Return a bound error if a type argument is not within bound at compile time.
void ClassFinalizer::CheckTypeArgumentBounds(
    const Class& cls,
    const AbstractTypeArguments& arguments,
    Error* bound_error) {
  if (!cls.is_type_finalized()) {
    FinalizeUpperBounds(cls);
  }
  // Note that when finalizing a type, we need to verify the bounds in both
  // production mode and checked mode, because the finalized type may be written
  // to a snapshot. It would be wrong to ignore bounds when generating the
  // snapshot in production mode and then use the unchecked type in checked mode
  // after reading it from the snapshot.
  // However, we do not immediately report a bound error, which would be wrong
  // in production mode, but simply postpone the bound checking to runtime.
  const intptr_t num_type_params = cls.NumTypeParameters();
  const intptr_t offset = cls.NumTypeArguments() - num_type_params;
  AbstractType& type_arg = AbstractType::Handle();
  AbstractType& cls_type_param = AbstractType::Handle();
  AbstractType& declared_bound = AbstractType::Handle();
  AbstractType& instantiated_bound = AbstractType::Handle();
  const TypeArguments& cls_type_params =
      TypeArguments::Handle(cls.type_parameters());
  ASSERT((cls_type_params.IsNull() && (num_type_params == 0)) ||
         (cls_type_params.Length() == num_type_params));
  // In case of overlapping type argument vectors, the same type argument may
  // get checked against different bounds.
  for (intptr_t i = 0; i < num_type_params; i++) {
    type_arg = arguments.TypeAt(offset + i);
    if (type_arg.IsDynamicType()) {
      continue;
    }
    cls_type_param = cls_type_params.TypeAt(i);
    const TypeParameter& type_param = TypeParameter::Cast(cls_type_param);
    ASSERT(type_param.IsFinalized());
    declared_bound = type_param.bound();
    if (!declared_bound.IsObjectType() && !declared_bound.IsDynamicType()) {
      if (!declared_bound.IsFinalized() && !declared_bound.IsBeingFinalized()) {
        declared_bound = FinalizeType(cls, declared_bound, kCanonicalize);
        type_param.set_bound(declared_bound);
      }
      ASSERT(declared_bound.IsFinalized() || declared_bound.IsBeingFinalized());
      Error& malformed_error = Error::Handle();
      // Note that the bound may be malformed, in which case the bound check
      // will return an error and the bound check will be postponed to run time.
      if (declared_bound.IsInstantiated()) {
        instantiated_bound = declared_bound.raw();
      } else {
        instantiated_bound =
            declared_bound.InstantiateFrom(arguments, &malformed_error);
      }
      if (!instantiated_bound.IsFinalized()) {
        // The bound refers to type parameters, creating a cycle; postpone
        // bound check to run time, when the bound will be finalized.
        // The bound may not necessarily be 'IsBeingFinalized' yet, as is the
        // case with a pair of type parameters of the same class referring to
        // each other via their bounds.
        type_arg = BoundedType::New(type_arg, instantiated_bound, type_param);
        arguments.SetTypeAt(offset + i, type_arg);
        continue;
      }
      // TODO(regis): We could simplify this code if we could differentiate
      // between a failed bound check and a bound check that is undecidable at
      // compile time.
      // Shortcut the special case where we check a type parameter against its
      // declared upper bound.
      bool below_bound = true;
      if (malformed_error.IsNull() &&
          (!type_arg.Equals(type_param) ||
           !instantiated_bound.Equals(declared_bound))) {
        // Pass NULL to prevent expensive and unnecessary error formatting in
        // the case the bound check is postponed to run time.
        below_bound = type_param.CheckBound(type_arg, instantiated_bound, NULL);
      }
      if (!malformed_error.IsNull() || !below_bound) {
        if (!type_arg.IsInstantiated() ||
            !instantiated_bound.IsInstantiated()) {
          type_arg = BoundedType::New(type_arg, instantiated_bound, type_param);
          arguments.SetTypeAt(offset + i, type_arg);
        } else if (bound_error->IsNull()) {
          if (malformed_error.IsNull()) {
            // Call CheckBound again to format error message.
            type_param.CheckBound(type_arg,
                                  instantiated_bound,
                                  &malformed_error);
          }
          ASSERT(!malformed_error.IsNull());
          *bound_error = malformed_error.raw();
        }
      }
    }
  }
  AbstractType& super_type = AbstractType::Handle(cls.super_type());
  if (!super_type.IsNull()) {
    const Class& super_class = Class::Handle(super_type.type_class());
    CheckTypeArgumentBounds(super_class, arguments, bound_error);
  }
}


RawAbstractType* ClassFinalizer::FinalizeType(const Class& cls,
                                              const AbstractType& type,
                                              FinalizationKind finalization) {
  if (type.IsFinalized()) {
    // Ensure type is canonical if canonicalization is requested, unless type is
    // malformed.
    if ((finalization >= kCanonicalize) && !type.IsMalformed()) {
      return type.Canonicalize();
    }
    return type.raw();
  }
  ASSERT(type.IsResolved());
  ASSERT(finalization >= kFinalize);

  if (FLAG_trace_type_finalization) {
    OS::Print("Finalizing type '%s' for class '%s'\n",
              String::Handle(type.Name()).ToCString(),
              cls.ToCString());
  }

  if (type.IsTypeParameter()) {
    const TypeParameter& type_parameter = TypeParameter::Cast(type);
    const Class& parameterized_class =
        Class::Handle(type_parameter.parameterized_class());
    ASSERT(!parameterized_class.IsNull());
    // The index must reflect the position of this type parameter in the type
    // arguments vector of its parameterized class. The offset to add is the
    // number of type arguments in the super type, which is equal to the
    // difference in number of type arguments and type parameters of the
    // parameterized class.
    const intptr_t offset = parameterized_class.NumTypeArguments() -
                            parameterized_class.NumTypeParameters();
    // Calling NumTypeParameters() may finalize this type parameter if it
    // belongs to a mixin application class.
    if (!type_parameter.IsFinalized()) {
      type_parameter.set_index(type_parameter.index() + offset);
      type_parameter.set_is_finalized();
    } else {
      ASSERT(cls.IsMixinApplication());
    }

    if (FLAG_trace_type_finalization) {
      OS::Print("Done finalizing type parameter '%s' with index %" Pd "\n",
                String::Handle(type_parameter.name()).ToCString(),
                type_parameter.index());
    }

    // We do not canonicalize type parameters.
    return type_parameter.raw();
  }

  // At this point, we can only have a parameterized_type.
  const Type& parameterized_type = Type::Cast(type);

  // Types illegally referring to themselves should have been detected earlier.
  ASSERT(!parameterized_type.IsBeingFinalized());

  // Mark type as being finalized in order to detect illegal self reference.
  parameterized_type.set_is_being_finalized();

  // The type class does not need to be finalized in order to finalize the type,
  // however, it must at least be resolved (this was done as part of resolving
  // the type itself, a precondition to calling FinalizeType).
  // Also, the interfaces of the type class must be resolved and the type
  // parameters of the type class must be finalized.
  Class& type_class = Class::Handle(parameterized_type.type_class());
  if (!type_class.is_type_finalized()) {
    FinalizeTypeParameters(type_class);
    ResolveUpperBounds(type_class);
  }

  // Finalize the current type arguments of the type, which are still the
  // parsed type arguments.
  AbstractTypeArguments& arguments =
      AbstractTypeArguments::Handle(parameterized_type.arguments());
  if (!arguments.IsNull()) {
    const intptr_t num_arguments = arguments.Length();
    AbstractType& type_argument = AbstractType::Handle();
    for (intptr_t i = 0; i < num_arguments; i++) {
      type_argument = arguments.TypeAt(i);
      type_argument = FinalizeType(cls, type_argument, finalization);
      if (type_argument.IsMalformed()) {
        // Malformed type arguments are mapped to dynamic.
        type_argument = Type::DynamicType();
      }
      arguments.SetTypeAt(i, type_argument);
    }
  }

  // The finalized type argument vector needs num_type_arguments types.
  const intptr_t num_type_arguments = type_class.NumTypeArguments();
  // The type class has num_type_parameters type parameters.
  const intptr_t num_type_parameters = type_class.NumTypeParameters();

  // Initialize the type argument vector.
  // Check the number of parsed type arguments, if any.
  // Specifying no type arguments indicates a raw type, which is not an error.
  // However, type parameter bounds are checked below, even for a raw type.
  if (!arguments.IsNull() && (arguments.Length() != num_type_parameters)) {
    // Wrong number of type arguments. The type is malformed.
    if (FLAG_error_on_bad_type) {
      const Script& script = Script::Handle(cls.script());
      const String& type_class_name = String::Handle(type_class.Name());
      ReportError(Error::Handle(),  // No previous error.
                  script, parameterized_type.token_pos(),
                  "wrong number of type arguments for class '%s'",
                  type_class_name.ToCString());
    }
    // Make the type raw and continue without reporting any error.
    // A static warning should have been reported.
    arguments = AbstractTypeArguments::null();
    parameterized_type.set_arguments(arguments);
  }
  // The full type argument vector consists of the type arguments of the
  // super types of type_class, which are initialized from the parsed
  // type arguments, followed by the parsed type arguments.
  TypeArguments& full_arguments = TypeArguments::Handle();
  Error& bound_error = Error::Handle();
  if (num_type_arguments > 0) {
    // If no type arguments were parsed and if the super types do not prepend
    // type arguments to the vector, we can leave the vector as null.
    if (!arguments.IsNull() || (num_type_arguments > num_type_parameters)) {
      full_arguments = TypeArguments::New(num_type_arguments);
      // Copy the parsed type arguments at the correct offset in the full type
      // argument vector.
      const intptr_t offset = num_type_arguments - num_type_parameters;
      AbstractType& type_arg = AbstractType::Handle(Type::DynamicType());
      for (intptr_t i = 0; i < offset; i++) {
        // Temporarily set the type arguments of the super classes to dynamic.
        full_arguments.SetTypeAt(i, type_arg);
      }
      for (intptr_t i = 0; i < num_type_parameters; i++) {
        // If no type parameters were provided, a raw type is desired, so we
        // create a vector of dynamic.
        if (!arguments.IsNull()) {
          type_arg = arguments.TypeAt(i);
        }
        ASSERT(type_arg.IsFinalized());  // Index of type parameter is adjusted.
        full_arguments.SetTypeAt(offset + i, type_arg);
      }
      // Replace the compile-time argument vector (of length zero or
      // num_type_parameters) of this type being finalized with the still
      // unfinalized run-time argument vector (of length num_type_arguments).
      // This type being finalized may be recursively reached via bounds
      // checking, in which case type arguments of super classes will be seen
      // as dynamic.
      parameterized_type.set_arguments(full_arguments);
      // If the type class is a signature class, the full argument vector
      // must include the argument vector of the super type.
      // If the signature class is a function type alias, it is also the owner
      // of its signature function and no super type is involved.
      // If the signature class is canonical (not an alias), the owner of its
      // signature function may either be an alias or the enclosing class of a
      // local function, in which case the super type of the enclosing class is
      // also considered when filling up the argument vector.
      if (type_class.IsSignatureClass()) {
        const Function& signature_fun =
            Function::Handle(type_class.signature_function());
        ASSERT(!signature_fun.is_static());
        const Class& sig_fun_owner = Class::Handle(signature_fun.Owner());
        if (offset > 0) {
          FinalizeTypeArguments(sig_fun_owner, full_arguments, offset,
                                finalization, &bound_error);
        }
        CheckTypeArgumentBounds(sig_fun_owner, full_arguments, &bound_error);
      } else {
        if (offset > 0) {
          FinalizeTypeArguments(type_class, full_arguments, offset,
                                finalization, &bound_error);
        }
        CheckTypeArgumentBounds(type_class, full_arguments, &bound_error);
      }
      if (full_arguments.IsRaw(num_type_arguments)) {
        // The parameterized_type is raw. Set its argument vector to null, which
        // is more efficient in type tests.
        full_arguments = TypeArguments::null();
      } else if (finalization >= kCanonicalize) {
        // FinalizeTypeArguments can modify 'full_arguments',
        // canonicalize afterwards.
        full_arguments ^= full_arguments.Canonicalize();
      }
      parameterized_type.set_arguments(full_arguments);
    } else {
      ASSERT(full_arguments.IsNull());  // Use null vector for raw type.
    }
  }

  // Self referencing types may get finalized indirectly.
  if (!parameterized_type.IsFinalized()) {
    // Mark the type as finalized.
    parameterized_type.SetIsFinalized();
  }

  // If the type class is a signature class, we are currently finalizing a
  // signature type, i.e. finalizing the result type and parameter types of the
  // signature function of this signature type.
  // We do this after marking this type as finalized in order to allow a
  // function type to refer to itself via its parameter types and result type.
  if (type_class.IsSignatureClass()) {
    // The class may be created while parsing a function body, after all
    // pending classes have already been finalized.
    FinalizeTypesInClass(type_class);
  }

  // If a bound error occurred, return a BoundedType with a malformed bound.
  // The malformed bound will be ignored in production mode.
  if (!bound_error.IsNull()) {
    // No compile-time error during finalization.
    const String& parameterized_type_name = String::Handle(
        parameterized_type.UserVisibleName());
    const Type& malformed_bound = Type::Handle(
        NewFinalizedMalformedType(bound_error,
                                  Script::Handle(cls.script()),
                                  parameterized_type.token_pos(),
                                  "type '%s' has an out of bound type argument",
                                  parameterized_type_name.ToCString()));

    if (FLAG_trace_type_finalization) {
      OS::Print("Done finalizing malbounded type '%s' with bound error: %s\n",
                String::Handle(parameterized_type.Name()).ToCString(),
                bound_error.ToCString());
    }

    return BoundedType::New(parameterized_type,
                            malformed_bound,
                            TypeParameter::Handle());
  }

  if (FLAG_trace_type_finalization) {
    OS::Print("Done finalizing type '%s' with %" Pd " type args\n",
              String::Handle(parameterized_type.Name()).ToCString(),
              parameterized_type.arguments() == AbstractTypeArguments::null() ?
                  0 : num_type_arguments);
  }

  if (finalization >= kCanonicalize) {
    return parameterized_type.Canonicalize();
  } else {
    return parameterized_type.raw();
  }
}


void ClassFinalizer::ResolveAndFinalizeSignature(const Class& cls,
                                                 const Function& function) {
  // Resolve result type.
  AbstractType& type = AbstractType::Handle(function.result_type());
  // It is not a compile time error if this name does not resolve to a class or
  // interface.
  ResolveType(cls, type, kCanonicalize);
  type = FinalizeType(cls, type, kCanonicalize);
  // The result type may be malformed or malbounded.
  function.set_result_type(type);
  // Resolve formal parameter types.
  const intptr_t num_parameters = function.NumParameters();
  for (intptr_t i = 0; i < num_parameters; i++) {
    type = function.ParameterTypeAt(i);
    ResolveType(cls, type, kCanonicalize);
    type = FinalizeType(cls, type, kCanonicalize);
    // The parameter type may be malformed or malbounded.
    function.SetParameterTypeAt(i, type);
  }
}


// Check if an instance field, getter, or method of same name exists
// in any super class.
static RawClass* FindSuperOwnerOfInstanceMember(const Class& cls,
                                                const String& name,
                                                const String& getter_name) {
  Class& super_class = Class::Handle();
  Function& function = Function::Handle();
  Field& field = Field::Handle();
  super_class = cls.SuperClass();
  while (!super_class.IsNull()) {
    function = super_class.LookupFunction(name);
    if (!function.IsNull() && !function.is_static()) {
      return super_class.raw();
    }
    function = super_class.LookupFunction(getter_name);
    if (!function.IsNull() && !function.is_static()) {
      return super_class.raw();
    }
    field = super_class.LookupField(name);
    if (!field.IsNull() && !field.is_static()) {
      return super_class.raw();
    }
    super_class = super_class.SuperClass();
  }
  return Class::null();
}


// Check if an instance method of same name exists in any super class.
static RawClass* FindSuperOwnerOfFunction(const Class& cls,
                                          const String& name) {
  Class& super_class = Class::Handle();
  Function& function = Function::Handle();
  super_class = cls.SuperClass();
  while (!super_class.IsNull()) {
    function = super_class.LookupFunction(name);
    if (!function.IsNull() &&
        !function.is_static() &&
        !function.IsMethodExtractor()) {
      return super_class.raw();
    }
    super_class = super_class.SuperClass();
  }
  return Class::null();
}


// Resolve the upper bounds of the type parameters of class cls.
void ClassFinalizer::ResolveUpperBounds(const Class& cls) {
  const intptr_t num_type_params = cls.NumTypeParameters();
  TypeParameter& type_param = TypeParameter::Handle();
  AbstractType& bound = AbstractType::Handle();
  const AbstractTypeArguments& type_params =
      AbstractTypeArguments::Handle(cls.type_parameters());
  ASSERT((type_params.IsNull() && (num_type_params == 0)) ||
         (type_params.Length() == num_type_params));
  // In a first pass, resolve all bounds. This guarantees that finalization
  // of mutually referencing bounds will not encounter an unresolved bound.
  for (intptr_t i = 0; i < num_type_params; i++) {
    type_param ^= type_params.TypeAt(i);
    bound = type_param.bound();
    ResolveType(cls, bound, kCanonicalize);
  }
}


// Finalize the upper bounds of the type parameters of class cls.
void ClassFinalizer::FinalizeUpperBounds(const Class& cls) {
  const intptr_t num_type_params = cls.NumTypeParameters();
  TypeParameter& type_param = TypeParameter::Handle();
  AbstractType& bound = AbstractType::Handle();
  const AbstractTypeArguments& type_params =
      AbstractTypeArguments::Handle(cls.type_parameters());
  ASSERT((type_params.IsNull() && (num_type_params == 0)) ||
         (type_params.Length() == num_type_params));
  for (intptr_t i = 0; i < num_type_params; i++) {
    type_param ^= type_params.TypeAt(i);
    bound = type_param.bound();
    if (bound.IsFinalized() || bound.IsBeingFinalized()) {
      // A bound involved in F-bounded quantification may form a cycle.
      continue;
    }
    bound = FinalizeType(cls, bound, kCanonicalize);
    type_param.set_bound(bound);
  }
}


void ClassFinalizer::ResolveAndFinalizeMemberTypes(const Class& cls) {
  // Note that getters and setters are explicitly listed as such in the list of
  // functions of a class, so we do not need to consider fields as implicitly
  // generating getters and setters.
  // Most overriding conflicts are only static warnings, i.e. they are not
  // reported as compile-time errors by the vm. However, signature conflicts in
  // overrides can be reported if the flag --error_on_bad_override is specified.
  // Static warning examples are:
  // - a static getter 'v' conflicting with an inherited instance setter 'v='.
  // - a static setter 'v=' conflicting with an inherited instance member 'v'.
  // - an instance member 'v' conflicting with an accessible static member 'v'
  //   or 'v=' of a super class (except that an instance method 'v' does not
  //   conflict with an accessible static setter 'v=' of a super class).
  // The compile-time errors we report are:
  // - a static member 'v' conflicting with an inherited instance member 'v'.
  // - a static setter 'v=' conflicting with an inherited instance setter 'v='.
  // - an instance method conflicting with an inherited instance field or
  //   instance getter.
  // - an instance field or instance getter conflicting with an inherited
  //   instance method.

  // Resolve type of fields and check for conflicts in super classes.
  Array& array = Array::Handle(cls.fields());
  Field& field = Field::Handle();
  AbstractType& type = AbstractType::Handle();
  String& name = String::Handle();
  String& getter_name = String::Handle();
  String& setter_name = String::Handle();
  Class& super_class = Class::Handle();
  const intptr_t num_fields = array.Length();
  for (intptr_t i = 0; i < num_fields; i++) {
    field ^= array.At(i);
    type = field.type();
    ResolveType(cls, type, kCanonicalize);
    type = FinalizeType(cls, type, kCanonicalize);
    field.set_type(type);
    name = field.name();
    if (field.is_static()) {
      getter_name = Field::GetterSymbol(name);
      super_class = FindSuperOwnerOfInstanceMember(cls, name, getter_name);
      if (!super_class.IsNull()) {
        const String& class_name = String::Handle(cls.Name());
        const String& super_class_name = String::Handle(super_class.Name());
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, field.token_pos(),
                    "static field '%s' of class '%s' conflicts with "
                    "instance member '%s' of super class '%s'",
                    name.ToCString(),
                    class_name.ToCString(),
                    name.ToCString(),
                    super_class_name.ToCString());
      }
      // An implicit setter is not generated for a static field, therefore, we
      // cannot rely on the code below handling the static setter case to report
      // a conflict with an instance setter. So we check explicitly here.
      setter_name = Field::SetterSymbol(name);
      super_class = FindSuperOwnerOfFunction(cls, setter_name);
      if (!super_class.IsNull()) {
        const String& class_name = String::Handle(cls.Name());
        const String& super_class_name = String::Handle(super_class.Name());
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, field.token_pos(),
                    "static field '%s' of class '%s' conflicts with "
                    "instance setter '%s=' of super class '%s'",
                    name.ToCString(),
                    class_name.ToCString(),
                    name.ToCString(),
                    super_class_name.ToCString());
      }

    } else {
      // Instance field. Check whether the field overrides a method
      // (but not getter).
      super_class = FindSuperOwnerOfFunction(cls, name);
      if (!super_class.IsNull()) {
        const String& class_name = String::Handle(cls.Name());
        const String& super_class_name = String::Handle(super_class.Name());
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, field.token_pos(),
                    "field '%s' of class '%s' conflicts with method '%s' "
                    "of super class '%s'",
                    name.ToCString(),
                    class_name.ToCString(),
                    name.ToCString(),
                    super_class_name.ToCString());
      }
    }
    if (field.is_static() && (field.is_const() || field.is_final()) &&
        (field.value() != Object::null()) &&
        (field.value() != Object::sentinel().raw())) {
      // The parser does not preset the value if the type is a type parameter or
      // is parameterized unless the value is null.
      Error& malformed_error = Error::Handle();
      if (type.IsMalformed()) {
        malformed_error = type.malformed_error();
      } else {
        ASSERT(type.IsInstantiated());
      }
      const Instance& const_value = Instance::Handle(field.value());
      if (!malformed_error.IsNull() ||
          (!type.IsDynamicType() &&
           !const_value.IsInstanceOf(type,
                                     AbstractTypeArguments::Handle(),
                                     &malformed_error))) {
        if (FLAG_error_on_bad_type) {
          const AbstractType& const_value_type = AbstractType::Handle(
              const_value.GetType());
          const String& const_value_type_name = String::Handle(
              const_value_type.UserVisibleName());
          const String& type_name = String::Handle(type.UserVisibleName());
          const Script& script = Script::Handle(cls.script());
          ReportError(malformed_error, script, field.token_pos(),
                      "error initializing static %s field '%s': "
                      "type '%s' is not a subtype of type '%s'",
                      field.is_const() ? "const" : "final",
                      name.ToCString(),
                      const_value_type_name.ToCString(),
                      type_name.ToCString());
        } else {
          // Do not report an error yet, even in checked mode, since the field
          // may not actually be used.
          // Also, we may be generating a snapshot in production mode that will
          // later be executed in checked mode, in which case an error needs to
          // be reported, should the field be accessed.
          // Therefore, we undo the optimization performed by the parser, i.e.
          // we create an implicit static final getter and reset the field value
          // to the sentinel value.
          const Function& getter = Function::Handle(
              Function::New(getter_name,
                            RawFunction::kImplicitStaticFinalGetter,
                            /* is_static = */ true,
                            /* is_const = */ field.is_const(),
                            /* is_abstract = */ false,
                            /* is_external = */ false,
                            cls,
                            field.token_pos()));
          getter.set_result_type(type);
          cls.AddFunction(getter);
          field.set_value(Instance::Handle(Object::sentinel().raw()));

          // Create initializer function.
          if (!field.is_const()) {
            const Function& init_function = Function::ZoneHandle(
                Function::NewStaticInitializer(field));
            cls.AddFunction(init_function);
          }
        }
      }
    }
  }
  // Collect interfaces, super interfaces, and super classes of this class.
  const GrowableObjectArray& interfaces =
      GrowableObjectArray::Handle(GrowableObjectArray::New());
  CollectInterfaces(cls, interfaces);
  // Include superclasses in list of interfaces and super interfaces.
  super_class = cls.SuperClass();
  while (!super_class.IsNull()) {
    interfaces.Add(super_class);
    CollectInterfaces(super_class, interfaces);
    super_class = super_class.SuperClass();
  }
  // Resolve function signatures and check for conflicts in super classes and
  // interfaces.
  array = cls.functions();
  Function& function = Function::Handle();
  Function& overridden_function = Function::Handle();
  const intptr_t num_functions = array.Length();
  Error& error = Error::Handle();
  for (intptr_t i = 0; i < num_functions; i++) {
    function ^= array.At(i);
    ResolveAndFinalizeSignature(cls, function);
    name = function.name();
    if (FLAG_error_on_bad_override &&  // Report signature conflicts only.
        !function.is_static() && !function.IsConstructor()) {
      // A constructor cannot override anything.
      for (intptr_t i = 0; i < interfaces.Length(); i++) {
        super_class ^= interfaces.At(i);
        overridden_function = super_class.LookupDynamicFunction(name);
        if (!overridden_function.IsNull() &&
            !function.HasCompatibleParametersWith(overridden_function,
                                                  &error)) {
          const String& class_name = String::Handle(cls.Name());
          const String& super_class_name = String::Handle(super_class.Name());
          const Script& script = Script::Handle(cls.script());
          ReportError(error, script, function.token_pos(),
                      "class '%s' overrides method '%s' of super class '%s' "
                      "with incompatible parameters",
                      class_name.ToCString(),
                      name.ToCString(),
                      super_class_name.ToCString());
        }
      }
    }
    if (function.IsSetterFunction() || function.IsImplicitSetterFunction()) {
      if (function.is_static()) {
        super_class = FindSuperOwnerOfFunction(cls, name);
        if (!super_class.IsNull()) {
          const String& class_name = String::Handle(cls.Name());
          const String& super_class_name = String::Handle(super_class.Name());
          const Script& script = Script::Handle(cls.script());
          ReportError(Error::Handle(),  // No previous error.
                      script, function.token_pos(),
                      "static setter '%s=' of class '%s' conflicts with "
                      "instance setter '%s=' of super class '%s'",
                      name.ToCString(),
                      class_name.ToCString(),
                      name.ToCString(),
                      super_class_name.ToCString());
        }
      }
      continue;
    }
    if (function.IsGetterFunction() || function.IsImplicitGetterFunction()) {
      getter_name = name.raw();
      name = Field::NameFromGetter(getter_name);
    } else {
      getter_name = Field::GetterSymbol(name);
    }
    if (function.is_static()) {
      super_class = FindSuperOwnerOfInstanceMember(cls, name, getter_name);
      if (!super_class.IsNull()) {
        const String& class_name = String::Handle(cls.Name());
        const String& super_class_name = String::Handle(super_class.Name());
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, function.token_pos(),
                    "static %s '%s' of class '%s' conflicts with "
                    "instance member '%s' of super class '%s'",
                    (function.IsGetterFunction() ||
                     function.IsImplicitGetterFunction()) ? "getter" : "method",
                    name.ToCString(),
                    class_name.ToCString(),
                    name.ToCString(),
                    super_class_name.ToCString());
      }
      // The function may be a still unresolved redirecting factory. Do not yet
      // try to resolve it in order to avoid cycles in class finalization.
    } else if (function.IsGetterFunction() ||
               function.IsImplicitGetterFunction()) {
      super_class = FindSuperOwnerOfFunction(cls, name);
      if (!super_class.IsNull()) {
        const String& class_name = String::Handle(cls.Name());
        const String& super_class_name = String::Handle(super_class.Name());
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, function.token_pos(),
                    "getter '%s' of class '%s' conflicts with "
                    "method '%s' of super class '%s'",
                    name.ToCString(),
                    class_name.ToCString(),
                    name.ToCString(),
                    super_class_name.ToCString());
      }
    } else if (!function.IsSetterFunction() &&
               !function.IsImplicitSetterFunction()) {
      // A function cannot conflict with a setter, since they cannot
      // have the same name. Thus, we do not need to check setters.
      super_class = FindSuperOwnerOfFunction(cls, getter_name);
      if (!super_class.IsNull()) {
        const String& class_name = String::Handle(cls.Name());
        const String& super_class_name = String::Handle(super_class.Name());
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, function.token_pos(),
                    "method '%s' of class '%s' conflicts with "
                    "getter '%s' of super class '%s'",
                    name.ToCString(),
                    class_name.ToCString(),
                    name.ToCString(),
                    super_class_name.ToCString());
      }
    }
  }
}


// Clone the type parameters of the super class and of the mixin class of this
// mixin application class and use them as the type parameters of this mixin
// application class. Set the type arguments of the super type, of the mixin
// type (as well as of the interface type, which is identical to the mixin type)
// to refer to the respective type parameters of the mixin application class.
// In other words, decorate this mixin application class with type parameters
// that forward to the super type and mixin type (and interface type).
// Example:
//   class S<T> { }
//   class M<T> { }
//   class C<E> extends S<E> with M<List<E>> { }
// results in
//   class S&M<T`, T> extends S<T`> implements M<T> { } // mixin == M<T>
//   class C<E> extends S&M<E, List<E>> { }
// CloneMixinAppTypeParameters decorates class S&M with type parameters T` and
// T, and use them as type arguments in S<T`> and M<T>.
void ClassFinalizer::CloneMixinAppTypeParameters(const Class& mixin_app_class) {
  ASSERT(mixin_app_class.type_parameters() == AbstractTypeArguments::null());
  const AbstractType& super_type = AbstractType::Handle(
      mixin_app_class.super_type());
  ASSERT(super_type.IsResolved());
  const Class& super_class = Class::Handle(super_type.type_class());
  const intptr_t num_super_type_params = super_class.NumTypeParameters();
  const Type& mixin_type = Type::Handle(mixin_app_class.mixin());
  const Class& mixin_class = Class::Handle(mixin_type.type_class());
  const intptr_t num_mixin_type_params = mixin_class.NumTypeParameters();
  // The mixin class cannot be Object and this was checked earlier.
  ASSERT(!mixin_class.IsObjectClass());

  // Add the mixin type to the interfaces that the mixin application
  // class implements. This is necessary so that type tests work.
  const Array& interfaces = Array::Handle(Array::New(1));
  const Type& interface = Type::Handle(Type::New(
      mixin_class,
      Object::null_abstract_type_arguments(),  // Set again below if generic.
      mixin_app_class.token_pos()));
  ASSERT(!interface.IsFinalized());
  interfaces.SetAt(0, interface);
  ASSERT(mixin_app_class.interfaces() == Object::empty_array().raw());
  mixin_app_class.set_interfaces(interfaces);

  // If both the super type and the mixin type are non generic, the mixin
  // application class is non generic as well and we can skip type parameter
  // cloning.
  if ((num_super_type_params + num_mixin_type_params) > 0) {
    // First, clone the super class type parameters. Rename them so that
    // there can be no name conflict between the parameters of the super
    // class and the mixin class.
    const TypeArguments& cloned_type_params = TypeArguments::Handle(
        TypeArguments::New(num_super_type_params + num_mixin_type_params));
    TypeParameter& param = TypeParameter::Handle();
    TypeParameter& cloned_param = TypeParameter::Handle();
    String& param_name = String::Handle();
    AbstractType& param_bound = AbstractType::Handle();
    intptr_t cloned_index = 0;
    if (num_super_type_params > 0) {
      const TypeArguments& super_type_params =
          TypeArguments::Handle(super_class.type_parameters());
      const TypeArguments& super_type_args =
          TypeArguments::Handle(TypeArguments::New(num_super_type_params));
      for (intptr_t i = 0; i < num_super_type_params; i++) {
        param ^= super_type_params.TypeAt(i);
        param_name = param.name();
        param_bound = param.bound();
        // TODO(14453): handle type bounds.
        if (!param_bound.IsObjectType()) {
          const Script& script = Script::Handle(mixin_app_class.script());
          ReportError(Error::Handle(),  // No previous error.
                      script, param.token_pos(),
                      "type parameter '%s': type bounds not yet"
                      " implemented for mixins\n",
                      param_name.ToCString());
        }
        param_name = String::Concat(param_name, Symbols::Backtick());
        param_name = Symbols::New(param_name);
        cloned_param = TypeParameter::New(mixin_app_class,
                                          cloned_index,
                                          param_name,
                                          param_bound,
                                          param.token_pos());
        cloned_type_params.SetTypeAt(cloned_index, cloned_param);
        // Change the type arguments of the super type to refer to the
        // cloned type parameters of the mixin application class.
        super_type_args.SetTypeAt(cloned_index, cloned_param);
        cloned_index++;
      }
      // TODO(14453): May need to handle BoundedType here.
      ASSERT(super_type.IsType());
      Type::Cast(super_type).set_arguments(super_type_args);
      ASSERT(!super_type.IsFinalized());
    }

    // Second, clone the type parameters of the mixin class.
    // We need to retain the parameter names of the mixin class
    // since the code that will be compiled in the context of the
    // mixin application class may refer to the type parameters
    // with that name.
    if (num_mixin_type_params > 0) {
      const TypeArguments& mixin_params =
          TypeArguments::Handle(mixin_class.type_parameters());
      const TypeArguments& mixin_type_args = TypeArguments::Handle(
          TypeArguments::New(num_mixin_type_params));
      // TODO(regis): Can we share interface type and mixin_type?
      const TypeArguments& interface_type_args = TypeArguments::Handle(
          TypeArguments::New(num_mixin_type_params));
      for (intptr_t i = 0; i < num_mixin_type_params; i++) {
        param ^= mixin_params.TypeAt(i);
        param_name = param.name();
        param_bound = param.bound();

        // TODO(14453): handle type bounds.
        if (!param_bound.IsObjectType()) {
          const Script& script = Script::Handle(mixin_app_class.script());
          ReportError(Error::Handle(),  // No previous error.
                      script, param.token_pos(),
                      "type parameter '%s': type bounds not yet"
                      " implemented for mixins\n",
                      param_name.ToCString());
        }
        cloned_param = TypeParameter::New(mixin_app_class,
                                          cloned_index,
                                          param_name,
                                          param_bound,
                                          param.token_pos());
        cloned_type_params.SetTypeAt(cloned_index, cloned_param);
        interface_type_args.SetTypeAt(i, cloned_param);
        mixin_type_args.SetTypeAt(i, cloned_param);
        cloned_index++;
      }

      // Lastly, set the type arguments of the mixin type and of the single
      // interface type.
      ASSERT(!mixin_type.IsFinalized());
      mixin_type.set_arguments(mixin_type_args);
      ASSERT(!interface.IsFinalized());
      interface.set_arguments(interface_type_args);
    }
    mixin_app_class.set_type_parameters(cloned_type_params);
  }
  // If the mixin class is a mixin application typedef class, we insert a new
  // synthesized mixin application class in the super chain of this mixin
  // application class. The new class will have the aliased mixin as actual
  // mixin.
  if (mixin_class.is_mixin_typedef()) {
    ApplyMixinTypedef(mixin_app_class);
  }
}


/* Support for mixin typedef.
Consider the following example:

class I<T> { }
class J<T> { }
class S<T> { }
class M<T> { }
class A<U, V> = Object with M<Map<U, V>> implements I<V>;
class C<T, K> = S<T> with A<T, List<K>> implements J<K>;

Before the call to ApplyMixinTypedef, the VM has already synthesized 2 mixin
application classes Object&M and S&A:

Object&M<T> extends Object implements M<T> { ... members of M applied here ... }
A<U, V> extends Object&M<Map<U, V>> implements I<V> { }

S&A<T`, U, V> extends S<T`> implements A<U, V> { }
C<T, K> extends S&A<T, T, List<K>> implements J<K> { }

In theory, class A should be an alias of Object&M instead of extending it.
In practice, the additional class provides a hook for implemented interfaces
(e.g. I<V>) and for type argument substitution via the super type relation (e.g.
type parameter T of Object&M is substituted with Map<U, V>, U and V being the
type parameters of the typedef A).

Similarly, class C should be an alias of S&A instead of extending it.

Since A is used as a mixin, it must extend Object. The fact that it extends
Object&M must be hidden so that no error is wrongly reported.

Now, A does not have any members to be mixed into S&A, because A is a typedef.
The members to be mixed in are actually those of M, and they should appear in a
scope where the type parameter T is visible. The class S&A declares the type
parameters of A, i.e. U and V, but not T.

Therefore, the call to ApplyMixinTypedef inserts another synthesized class S&A`
as the superclass of S&A. The class S&A` declares a type argument T:

Instead of
S&A<T`, U, V> extends S<T`> implements A<U, V> { }

We now have:
S&A`<T`, T> extends S<T`> implements M<T> { ... members of M applied here ... }
S&A<T`, U, V> extends S&A`<T`, Map<U, V>> implements A<U, V> { }

The main implementation difficulty resides in the fact that the type parameters
U and V in the super type S&A`<T`, Map<U, V>> of S&A must refer to the type
parameters U and V of S&A. However, Map<U, V> is copied from the super type
Object&M<Map<U, V>> of A and, therefore, U and V refer to A. An instantiation
step with a properly crafted instantiator vector takes care of the required type
parameter substitution.

The instantiator vector must end with the type parameters U and V of S&A.
The offset of the first type parameter U of S&A must be at the finalized index
of type parameter U of A.
*/
// TODO(regis): The syntax does not use 'typedef' anymore. Rename to 'alias'?
void ClassFinalizer::ApplyMixinTypedef(const Class& mixin_app_class) {
  // If this mixin typedef is aliasing another mixin typedef, another class
  // will be inserted via recursion. No need to check here.
  // The mixin type may or may not be finalized yet.
  AbstractType& super_type = AbstractType::Handle(mixin_app_class.super_type());
  const Type& mixin_type = Type::Handle(mixin_app_class.mixin());
  const Class& mixin_class = Class::Handle(mixin_type.type_class());
  ASSERT(mixin_class.is_mixin_typedef());
  const Class& aliased_mixin_app_class = Class::Handle(
      mixin_class.SuperClass());
  const Type& aliased_mixin_type = Type::Handle(
      aliased_mixin_app_class.mixin());
  // The name of the inserted mixin application class is the name of mixin
  // class name with a backtick added.
  String& inserted_class_name = String::Handle(mixin_app_class.Name());
  inserted_class_name = String::Concat(inserted_class_name,
                                       Symbols::Backtick());
  const Library& library = Library::Handle(mixin_app_class.library());
  Class& inserted_class = Class::Handle(
      library.LookupLocalClass(inserted_class_name));
  if (inserted_class.IsNull()) {
    inserted_class_name = Symbols::New(inserted_class_name);
    const Script& script = Script::Handle(mixin_app_class.script());
    inserted_class = Class::New(
        inserted_class_name, script, mixin_app_class.token_pos());
    inserted_class.set_is_synthesized_class();
    library.AddClass(inserted_class);

    if (FLAG_trace_class_finalization) {
      OS::Print("Creating mixin typedef application %s\n",
                inserted_class.ToCString());
    }

    // The super type of the inserted class is identical to the super type of
    // this mixin application class, except that it must refer to the type
    // parameters of the inserted class rather than to those of the mixin
    // application class.
    // The type arguments of the super type will be set properly when calling
    // CloneMixinAppTypeParameters on the inserted class, as long as the super
    // type class is set properly.
    inserted_class.set_super_type(super_type);  // Super class only is used.

    // The mixin type and interface type must also be set before calling
    // CloneMixinAppTypeParameters.
    // After FinalizeTypesInClass, they will refer to the type parameters of
    // the mixin class typedef.
    const Type& generic_mixin_type = Type::Handle(
        Type::New(Class::Handle(aliased_mixin_type.type_class()),
                  Object::null_abstract_type_arguments(),
                  aliased_mixin_type.token_pos()));
    inserted_class.set_mixin(generic_mixin_type);
    // The interface will be set in CloneMixinAppTypeParameters.
  }

  // Finalize the types and call CloneMixinAppTypeParameters.
  FinalizeTypesInClass(inserted_class);

  // The super type of this mixin application class must point to the
  // inserted class. The super type arguments are the concatenation of the
  // old super type arguments (propagating type arguments to the super class)
  // with new type arguments providing type arguments to the mixin.
  // The appended type arguments are those of the super type of the mixin
  // typedef that are forwarding to the aliased mixin type, except
  // that they must refer to the type parameters of the mixin application
  // class rather than to those of the mixin typedef class.
  // This type parameter substitution is performed by an instantiation step.
  // It is important that the type parameters of the mixin application class
  // are not finalized yet, because new type parameters may have been added
  // to the super class.
  Class& super_class = Class::Handle(super_type.type_class());
  ASSERT(mixin_app_class.SuperClass() == super_class.raw());
  while (super_class.IsMixinApplication()) {
    super_class = super_class.SuperClass();
  }
  const intptr_t num_super_type_params = super_class.NumTypeParameters();
  const intptr_t num_mixin_type_params = mixin_class.NumTypeParameters();
  intptr_t offset =
      mixin_class.NumTypeArguments() - mixin_class.NumTypeParameters();
  const TypeArguments& type_params =
      TypeArguments::Handle(mixin_app_class.type_parameters());
  TypeArguments& instantiator = TypeArguments::Handle(
      TypeArguments::New(offset + num_mixin_type_params));
  AbstractType& type = AbstractType::Handle();
  for (intptr_t i = 0; i < num_mixin_type_params; i++) {
    type = type_params.TypeAt(num_super_type_params + i);
    instantiator.SetTypeAt(offset + i, type);
  }
  ASSERT(aliased_mixin_type.IsFinalized());
  const Class& aliased_mixin_type_class = Class::Handle(
      aliased_mixin_type.type_class());
  const intptr_t num_aliased_mixin_type_params =
      aliased_mixin_type_class.NumTypeParameters();
  const intptr_t num_aliased_mixin_type_args =
      aliased_mixin_type_class.NumTypeArguments();
  offset = num_aliased_mixin_type_args - num_aliased_mixin_type_params;
  ASSERT(inserted_class.NumTypeParameters() ==
         (num_super_type_params + num_aliased_mixin_type_params));
  // The aliased_mixin_type may be raw.
  const AbstractTypeArguments& mixin_class_super_type_args =
      AbstractTypeArguments::Handle(
          AbstractType::Handle(mixin_class.super_type()).arguments());
  TypeArguments& new_mixin_type_args = TypeArguments::Handle();
  if ((num_aliased_mixin_type_params > 0) &&
      !mixin_class_super_type_args.IsNull()) {
    new_mixin_type_args = TypeArguments::New(num_aliased_mixin_type_params);
    for (intptr_t i = 0; i < num_aliased_mixin_type_params; i++) {
      type = mixin_class_super_type_args.TypeAt(offset + i);
      new_mixin_type_args.SetTypeAt(i, type);
    }
  }
  if (!new_mixin_type_args.IsNull() &&
      !new_mixin_type_args.IsInstantiated()) {
    Error& bound_error = Error::Handle();
    new_mixin_type_args ^=
        new_mixin_type_args.InstantiateFrom(instantiator, &bound_error);
    // TODO(14453): Handle bound error.
    ASSERT(bound_error.IsNull());
  }
  TypeArguments& new_super_type_args = TypeArguments::Handle();
  if ((num_super_type_params + num_aliased_mixin_type_params) > 0) {
    new_super_type_args = TypeArguments::New(num_super_type_params +
                                             num_aliased_mixin_type_params);
    for (intptr_t i = 0; i < num_super_type_params; i++) {
      type = type_params.TypeAt(i);
      new_super_type_args.SetTypeAt(i, type);
    }
    for (intptr_t i = 0; i < num_aliased_mixin_type_params; i++) {
      if (new_mixin_type_args.IsNull()) {
        type = Type::DynamicType();
      } else {
        type = new_mixin_type_args.TypeAt(i);
      }
      new_super_type_args.SetTypeAt(num_super_type_params + i, type);
    }
  }
  super_type = Type::New(inserted_class,
                         new_super_type_args,
                         mixin_app_class.token_pos());
  mixin_app_class.set_super_type(super_type);
  // Mark this mixin application class as being a typedef.
  mixin_app_class.set_is_mixin_typedef();
  ASSERT(!mixin_app_class.is_type_finalized());
  ASSERT(!mixin_app_class.is_mixin_type_applied());
  if (FLAG_trace_class_finalization) {
    OS::Print("Inserting class %s to mixin typedef application %s "
              "with super type '%s'\n",
              inserted_class.ToCString(),
              mixin_app_class.ToCString(),
              String::Handle(super_type.Name()).ToCString());
  }
}


void ClassFinalizer::ApplyMixinType(const Class& mixin_app_class) {
  if (mixin_app_class.is_mixin_type_applied()) {
    return;
  }
  Type& mixin_type = Type::Handle(mixin_app_class.mixin());
  ASSERT(!mixin_type.IsNull());
  ASSERT(mixin_type.HasResolvedTypeClass());
  const Class& mixin_class = Class::Handle(mixin_type.type_class());

  if (FLAG_trace_class_finalization) {
    OS::Print("Applying mixin type '%s' to %s at pos %" Pd "\n",
              String::Handle(mixin_type.Name()).ToCString(),
              mixin_app_class.ToCString(),
              mixin_app_class.token_pos());
  }

  // Check for illegal self references. This has to be done before checking
  // that the super class of the mixin class is class Object.
  GrowableArray<intptr_t> visited_mixins;
  if (!IsMixinCycleFree(mixin_class, &visited_mixins)) {
    const Script& script = Script::Handle(mixin_class.script());
    const String& class_name = String::Handle(mixin_class.Name());
    ReportError(Error::Handle(),  // No previous error.
                script, mixin_class.token_pos(),
                "mixin class '%s' illegally refers to itself",
                class_name.ToCString());
  }

  // Check that the super class of the mixin class is class Object.
  Class& mixin_super_class = Class::Handle(mixin_class.SuperClass());
  // Skip over mixin application typedef classes, which are aliases (but are
  // implemented as subclasses) of the mixin application classes they name.
  if (!mixin_super_class.IsNull() && mixin_class.is_mixin_typedef()) {
    while (mixin_super_class.is_mixin_typedef()) {
      mixin_super_class = mixin_super_class.SuperClass();
    }
    mixin_super_class = mixin_super_class.SuperClass();
  }
  if (mixin_super_class.IsNull() || !mixin_super_class.IsObjectClass()) {
    const Script& script = Script::Handle(mixin_app_class.script());
    const String& class_name = String::Handle(mixin_class.Name());
    ReportError(Error::Handle(),  // No previous error.
                script, mixin_app_class.token_pos(),
                "mixin class '%s' must extend class 'Object'",
                class_name.ToCString());
  }

  // Copy type parameters to mixin application class.
  CloneMixinAppTypeParameters(mixin_app_class);

  // Verify that no restricted class is used as a mixin by checking the
  // interfaces of the mixin application class, which implements its mixin.
  GrowableArray<intptr_t> visited_interfaces;
  ResolveSuperTypeAndInterfaces(mixin_app_class, &visited_interfaces);

  if (FLAG_trace_class_finalization) {
    OS::Print("Done applying mixin type '%s' to class '%s' %s extending '%s'\n",
              String::Handle(mixin_type.Name()).ToCString(),
              String::Handle(mixin_app_class.Name()).ToCString(),
              TypeArguments::Handle(
                  mixin_app_class.type_parameters()).ToCString(),
              AbstractType::Handle(mixin_app_class.super_type()).ToCString());
  }
  // Mark the application class as having been applied its mixin type in order
  // to avoid cycles while finalizing its mixin type.
  mixin_app_class.set_is_mixin_type_applied();
  // Finalize the mixin type, which may have been changed in case
  // mixin_app_class is a typedef.
  mixin_type = mixin_app_class.mixin();
  ASSERT(!mixin_type.IsBeingFinalized());
  mixin_type ^=
      FinalizeType(mixin_app_class, mixin_type, kCanonicalizeWellFormed);
  // TODO(14453): Check for a malbounded mixin_type.
  mixin_app_class.set_mixin(mixin_type);
}


void ClassFinalizer::CreateForwardingConstructors(
    const Class& mixin_app,
    const GrowableObjectArray& cloned_funcs) {
  const String& mixin_name = String::Handle(mixin_app.Name());
  const Class& super_class = Class::Handle(mixin_app.SuperClass());
  const String& super_name = String::Handle(super_class.Name());
  const Type& dynamic_type = Type::Handle(Type::DynamicType());
  const Array& functions = Array::Handle(super_class.functions());
  const intptr_t num_functions = functions.Length();
  Function& func = Function::Handle();
  for (intptr_t i = 0; i < num_functions; i++) {
    func ^= functions.At(i);
    if (func.IsConstructor()) {
      // Build constructor name from mixin application class name
      // and name of cloned super class constructor.
      const String& ctor_name = String::Handle(func.name());
      String& clone_name = String::Handle(
          String::SubString(ctor_name, super_name.Length()));
      clone_name = String::Concat(mixin_name, clone_name);
      clone_name = Symbols::New(clone_name);

      if (FLAG_trace_class_finalization) {
        OS::Print("Cloning constructor '%s' as '%s'\n",
                  ctor_name.ToCString(),
                  clone_name.ToCString());
      }
      const Function& clone = Function::Handle(
          Function::New(clone_name,
                        func.kind(),
                        func.is_static(),
                        false,  // Not const.
                        false,  // Not abstract.
                        false,  // Not external.
                        mixin_app,
                        mixin_app.token_pos()));

      clone.set_num_fixed_parameters(func.num_fixed_parameters());
      clone.SetNumOptionalParameters(func.NumOptionalParameters(),
                                     func.HasOptionalPositionalParameters());
      clone.set_result_type(dynamic_type);

      const intptr_t num_parameters = func.NumParameters();
      // The cloned ctor shares the parameter names array with the
      // original.
      const Array& parameter_names = Array::Handle(func.parameter_names());
      ASSERT(parameter_names.Length() == num_parameters);
      clone.set_parameter_names(parameter_names);
      // The parameter types of the cloned constructor are 'dynamic'.
      clone.set_parameter_types(Array::Handle(Array::New(num_parameters)));
      for (intptr_t n = 0; n < num_parameters; n++) {
        clone.SetParameterTypeAt(n, dynamic_type);
      }
      cloned_funcs.Add(clone);
    }
  }
}


void ClassFinalizer::ApplyMixinMembers(const Class& cls) {
  Isolate* isolate = Isolate::Current();
  const Type& mixin_type = Type::Handle(isolate, cls.mixin());
  ASSERT(!mixin_type.IsNull());
  ASSERT(mixin_type.HasResolvedTypeClass());
  const Class& mixin_cls = Class::Handle(isolate, mixin_type.type_class());
  mixin_cls.EnsureIsFinalized(isolate);
  // If the mixin is a mixin application typedef class, there are no members to
  // apply here. A new synthesized class representing the aliased mixin
  // application class was inserted in the super chain of this mixin application
  // class. Members of the actual mixin class will be applied when visiting
  // the mixin application class referring to the actual mixin.
  ASSERT(!mixin_cls.is_mixin_typedef() ||
         Class::Handle(isolate, cls.SuperClass()).IsMixinApplication());
  // A default constructor will be created for the typedef class.

  if (FLAG_trace_class_finalization) {
    OS::Print("Applying mixin members of %s to %s at pos %" Pd "\n",
              mixin_cls.ToCString(),
              cls.ToCString(),
              cls.token_pos());
  }

  const GrowableObjectArray& cloned_funcs =
      GrowableObjectArray::Handle(isolate, GrowableObjectArray::New());

  CreateForwardingConstructors(cls, cloned_funcs);

  Array& functions = Array::Handle(isolate);
  Function& func = Function::Handle(isolate);
  // The parser creates the mixin application class with no functions.
  ASSERT((functions = cls.functions(), functions.Length() == 0));
  // Now clone the functions from the mixin class.
  functions = mixin_cls.functions();
  const intptr_t num_functions = functions.Length();
  for (intptr_t i = 0; i < num_functions; i++) {
    func ^= functions.At(i);
    if (func.IsConstructor()) {
      // A mixin class must not have explicit constructors.
      if (!func.IsImplicitConstructor()) {
        const Script& script = Script::Handle(isolate, cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, cls.token_pos(),
                    "mixin class '%s' must not have constructors\n",
                    String::Handle(isolate, mixin_cls.Name()).ToCString());
      }
      continue;  // Skip the implicit constructor.
    }
    if (!func.is_static()) {
      func = func.Clone(cls);
      cloned_funcs.Add(func);
    }
  }
  functions = Array::MakeArray(cloned_funcs);
  cls.SetFunctions(functions);

  // Now clone the fields from the mixin class. There should be no
  // existing fields in the mixin application class.
  ASSERT(Array::Handle(cls.fields()).Length() == 0);
  Array& fields = Array::Handle(isolate, mixin_cls.fields());
  Field& field = Field::Handle(isolate);
  const GrowableObjectArray& cloned_fields =
      GrowableObjectArray::Handle(isolate, GrowableObjectArray::New());
  const intptr_t num_fields = fields.Length();
  for (intptr_t i = 0; i < num_fields; i++) {
    field ^= fields.At(i);
    if (!field.is_static()) {
      field = field.Clone(cls);
      cloned_fields.Add(field);
    }
  }
  fields = Array::MakeArray(cloned_fields);
  cls.SetFields(fields);

  if (FLAG_trace_class_finalization) {
    OS::Print("Done applying mixin members of %s to %s\n",
              mixin_cls.ToCString(),
              cls.ToCString());
  }
}


void ClassFinalizer::FinalizeTypesInClass(const Class& cls) {
  HANDLESCOPE(Isolate::Current());
  if (cls.is_type_finalized()) {
    return;
  }
  if (FLAG_trace_class_finalization) {
    OS::Print("Finalize types in %s\n", cls.ToCString());
  }
  if (!IsSuperCycleFree(cls)) {
    const String& name = String::Handle(cls.Name());
    const Script& script = Script::Handle(cls.script());
    ReportError(Error::Handle(),  // No previous error.
                script, cls.token_pos(),
                "class '%s' has a cycle in its superclass relationship",
                name.ToCString());
  }
  // Finalize super class.
  Class& super_class = Class::Handle(cls.SuperClass());
  if (!super_class.IsNull()) {
    FinalizeTypesInClass(super_class);
  }
  // Finalize type parameters before finalizing the super type.
  FinalizeTypeParameters(cls);  // May change super type.
  super_class = cls.SuperClass();
  ASSERT(super_class.IsNull() || super_class.is_type_finalized());
  ResolveUpperBounds(cls);
  // Finalize super type.
  AbstractType& super_type = AbstractType::Handle(cls.super_type());
  if (!super_type.IsNull()) {
    // In case of a bound error in the super type in production mode, the
    // finalized super type will be a BoundedType with a malformed bound.
    // It should not be a problem if the class is written to a snapshot and
    // later executed in checked mode. Note that the finalized type argument
    // vector of any type of the base class will contain a BoundedType for the
    // out of bound type argument.
    super_type = FinalizeType(cls, super_type, kCanonicalizeWellFormed);
    cls.set_super_type(super_type);
  }
  if (cls.IsSignatureClass()) {
    // Check for illegal self references.
    GrowableArray<intptr_t> visited_aliases;
    if (!IsAliasCycleFree(cls, &visited_aliases)) {
      const String& name = String::Handle(cls.Name());
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, cls.token_pos(),
                  "typedef '%s' illegally refers to itself",
                  name.ToCString());
    }
    cls.set_is_type_finalized();

    // The type parameters of signature classes may have bounds.
    FinalizeUpperBounds(cls);

    // Resolve and finalize the result and parameter types of the signature
    // function of this signature class.
    const Function& sig_function = Function::Handle(cls.signature_function());
    ResolveAndFinalizeSignature(cls, sig_function);

    // Resolve and finalize the signature type of this signature class.
    const Type& sig_type = Type::Handle(cls.SignatureType());
    FinalizeType(cls, sig_type, kCanonicalizeWellFormed);
    return;
  }
  // Finalize interface types (but not necessarily interface classes).
  Array& interface_types = Array::Handle(cls.interfaces());
  AbstractType& interface_type = AbstractType::Handle();
  AbstractType& seen_interf = AbstractType::Handle();
  for (intptr_t i = 0; i < interface_types.Length(); i++) {
    interface_type ^= interface_types.At(i);
    interface_type = FinalizeType(cls, interface_type, kCanonicalizeWellFormed);
    interface_types.SetAt(i, interface_type);

    // Check whether the interface is duplicated. We need to wait with
    // this check until the super type and interface types are finalized,
    // so that we can use Type::Equals() for the test.
    ASSERT(interface_type.IsFinalized());
    ASSERT(super_type.IsNull() || super_type.IsFinalized());
    if (!super_type.IsNull() && interface_type.Equals(super_type)) {
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, cls.token_pos(),
                  "super type '%s' may not be listed in "
                  "implements clause of class '%s'",
                  String::Handle(super_type.Name()).ToCString(),
                  String::Handle(cls.Name()).ToCString());
    }
    for (intptr_t j = 0; j < i; j++) {
      seen_interf ^= interface_types.At(j);
      if (interface_type.Equals(seen_interf)) {
        const Script& script = Script::Handle(cls.script());
        ReportError(Error::Handle(),  // No previous error.
                    script, cls.token_pos(),
                    "interface '%s' appears twice in "
                    "implements clause of class '%s'",
                    String::Handle(interface_type.Name()).ToCString(),
                    String::Handle(cls.Name()).ToCString());
      }
    }
  }
  // Mark as type finalized before resolving type parameter upper bounds
  // in order to break cycles.
  cls.set_is_type_finalized();
  // Finalize bounds even if running in production mode, so that a snapshot
  // contains them.
  FinalizeUpperBounds(cls);
  // Add this class to the direct subclasses of the superclass, unless the
  // superclass is Object.
  if (!super_type.IsNull() && !super_type.IsObjectType()) {
    ASSERT(!super_class.IsNull());
    super_class.AddDirectSubclass(cls);
  }
  // A top level class is parsed eagerly so just finalize it.
  if (cls.IsTopLevel()) {
    FinalizeClass(cls);
  } else {
    // This class should not contain any fields or functions yet, because it has
    // not been compiled yet. Since 'ResolveAndFinalizeMemberTypes(cls)' has not
    // been called yet, unfinalized member types could choke the snapshotter.
    ASSERT(Array::Handle(cls.fields()).Length() == 0);
    ASSERT(Array::Handle(cls.functions()).Length() == 0);
  }
}


void ClassFinalizer::FinalizeClass(const Class& cls) {
  HANDLESCOPE(Isolate::Current());
  if (cls.is_finalized()) {
    return;
  }
  if (FLAG_trace_class_finalization) {
    OS::Print("Finalize %s\n", cls.ToCString());
  }
  if (cls.IsMixinApplication()) {
    // Copy instance methods and fields from the mixin class.
    // This has to happen before the check whether the methods of
    // the class conflict with inherited methods.
    ApplyMixinMembers(cls);
  }
  // Ensure super class is finalized.
  const Class& super = Class::Handle(cls.SuperClass());
  if (!super.IsNull()) {
    FinalizeClass(super);
  }
  // Mark as parsed and finalized.
  cls.Finalize();
  // Mixin typedef classes may still lack their forwarding constructor.
  if (cls.is_mixin_typedef() &&
      (cls.functions() == Object::empty_array().raw())) {
    const GrowableObjectArray& cloned_funcs =
        GrowableObjectArray::Handle(GrowableObjectArray::New());
    CreateForwardingConstructors(cls, cloned_funcs);
    const Array& functions = Array::Handle(Array::MakeArray(cloned_funcs));
    cls.SetFunctions(functions);
  }
  // Every class should have at least a constructor, unless it is a top level
  // class or a signature class.
  ASSERT(cls.IsTopLevel() ||
         cls.IsSignatureClass() ||
         (Array::Handle(cls.functions()).Length() > 0));
  // Resolve and finalize all member types.
  ResolveAndFinalizeMemberTypes(cls);
  // Run additional checks after all types are finalized.
  if (cls.is_const()) {
    CheckForLegalConstClass(cls);
  }
}


bool ClassFinalizer::IsSuperCycleFree(const Class& cls) {
  Class& test1 = Class::Handle(cls.raw());
  Class& test2 = Class::Handle(cls.SuperClass());
  // A finalized class has been checked for cycles.
  // Using the hare and tortoise algorithm for locating cycles.
  while (!test1.is_type_finalized() &&
         !test2.IsNull() && !test2.is_type_finalized()) {
    if (test1.raw() == test2.raw()) {
      // Found a cycle.
      return false;
    }
    test1 = test1.SuperClass();
    test2 = test2.SuperClass();
    if (!test2.IsNull()) {
      test2 = test2.SuperClass();
    }
  }
  // No cycles.
  return true;
}


// Helper function called by IsAliasCycleFree.
bool ClassFinalizer::IsTypeCycleFree(
    const Class& cls,
    const AbstractType& type,
    GrowableArray<intptr_t>* visited) {
  ASSERT(visited != NULL);
  ResolveType(cls, type, kCanonicalize);
  if (type.IsType() && !type.IsMalformed()) {
    const Class& type_class = Class::Handle(type.type_class());
    if (!type_class.is_type_finalized() &&
        type_class.IsSignatureClass() &&
        !IsAliasCycleFree(type_class, visited)) {
      return false;
    }
    const AbstractTypeArguments& type_args = AbstractTypeArguments::Handle(
        type.arguments());
    if (!type_args.IsNull()) {
      AbstractType& type_arg = AbstractType::Handle();
      for (intptr_t i = 0; i < type_args.Length(); i++) {
        type_arg = type_args.TypeAt(i);
        if (!IsTypeCycleFree(cls, type_arg, visited)) {
          return false;
        }
      }
    }
  }
  return true;
}


// Returns false if the function type alias illegally refers to itself.
bool ClassFinalizer::IsAliasCycleFree(const Class& cls,
                                      GrowableArray<intptr_t>* visited) {
  ASSERT(cls.IsSignatureClass());
  ASSERT(!cls.is_type_finalized());
  ASSERT(visited != NULL);
  const intptr_t cls_index = cls.id();
  for (intptr_t i = 0; i < visited->length(); i++) {
    if ((*visited)[i] == cls_index) {
      // We have already visited alias 'cls'. We found a cycle.
      return false;
    }
  }

  // Visit the bounds, result type, and parameter types of this signature type.
  visited->Add(cls.id());
  AbstractType& type = AbstractType::Handle();

  // Check the bounds of this signature type.
  const intptr_t num_type_params = cls.NumTypeParameters();
  TypeParameter& type_param = TypeParameter::Handle();
  const AbstractTypeArguments& type_params =
      AbstractTypeArguments::Handle(cls.type_parameters());
  ASSERT((type_params.IsNull() && (num_type_params == 0)) ||
         (type_params.Length() == num_type_params));
  for (intptr_t i = 0; i < num_type_params; i++) {
    type_param ^= type_params.TypeAt(i);
    type = type_param.bound();
    if (!IsTypeCycleFree(cls, type, visited)) {
      return false;
    }
  }
  // Check the result type of the function of this signature type.
  const Function& function = Function::Handle(cls.signature_function());
  type = function.result_type();
  if (!IsTypeCycleFree(cls, type, visited)) {
    return false;
  }
  // Check the formal parameter types of the function of this signature type.
  const intptr_t num_parameters = function.NumParameters();
  for (intptr_t i = 0; i < num_parameters; i++) {
    type = function.ParameterTypeAt(i);
    if (!IsTypeCycleFree(cls, type, visited)) {
      return false;
    }
  }
  visited->RemoveLast();
  return true;
}


// Returns false if the mixin illegally refers to itself.
bool ClassFinalizer::IsMixinCycleFree(const Class& cls,
                                      GrowableArray<intptr_t>* visited) {
  ASSERT(visited != NULL);
  const intptr_t cls_index = cls.id();
  for (intptr_t i = 0; i < visited->length(); i++) {
    if ((*visited)[i] == cls_index) {
      // We have already visited mixin 'cls'. We found a cycle.
      return false;
    }
  }

  // Visit the super chain of cls.
  visited->Add(cls.id());
  Class& super_class = Class::Handle(cls.raw());
  do {
    if (super_class.IsMixinApplication()) {
      const Type& mixin_type = Type::Handle(super_class.mixin());
      ASSERT(!mixin_type.IsNull());
      ASSERT(mixin_type.HasResolvedTypeClass());
      const Class& mixin_class = Class::Handle(mixin_type.type_class());
      if (!IsMixinCycleFree(mixin_class, visited)) {
        return false;
      }
    }
    super_class = super_class.SuperClass();
  } while (!super_class.IsNull());
  visited->RemoveLast();
  return true;
}


void ClassFinalizer::CollectTypeArguments(
    const Class& cls,
    const Type& type,
    const GrowableObjectArray& collected_args) {
  ASSERT(type.HasResolvedTypeClass());
  Class& type_class = Class::Handle(type.type_class());
  AbstractTypeArguments& type_args =
      AbstractTypeArguments::Handle(type.arguments());
  const intptr_t num_type_parameters = type_class.NumTypeParameters();
  const intptr_t num_type_arguments =
      type_args.IsNull() ? 0 : type_args.Length();
  AbstractType& arg = AbstractType::Handle();
  if (num_type_arguments > 0) {
    if (num_type_arguments == num_type_parameters) {
      for (intptr_t i = 0; i < num_type_arguments; i++) {
        arg = type_args.TypeAt(i);
        arg = arg.CloneUnfinalized();
        ASSERT(!arg.IsBeingFinalized());
        collected_args.Add(arg);
      }
      return;
    }
    if (FLAG_error_on_bad_type) {
      const Script& script = Script::Handle(cls.script());
      const String& type_class_name = String::Handle(type_class.Name());
      ReportError(Error::Handle(),  // No previous error.
                  script, type.token_pos(),
                  "wrong number of type arguments for class '%s'",
                  type_class_name.ToCString());
    }
    // Discard provided type arguments and treat type as raw.
  }
  // Fill arguments with type dynamic.
  for (intptr_t i = 0; i < num_type_parameters; i++) {
    arg = Type::DynamicType();
    collected_args.Add(arg);
  }
}


RawType* ClassFinalizer::ResolveMixinAppType(
    const Class& cls,
    const MixinAppType& mixin_app_type) {
  // Lookup or create mixin application classes in the library of cls
  // and resolve super type and mixin types.
  const Library& library = Library::Handle(cls.library());
  ASSERT(!library.IsNull());
  const Script& script = Script::Handle(cls.script());
  ASSERT(!script.IsNull());
  const GrowableObjectArray& type_args =
      GrowableObjectArray::Handle(GrowableObjectArray::New());
  AbstractType& mixin_super_type =
      AbstractType::Handle(mixin_app_type.super_type());
  ResolveType(cls, mixin_super_type, kCanonicalizeWellFormed);
  ASSERT(mixin_super_type.HasResolvedTypeClass());
  // TODO(14453): May need to handle BoundedType here.
  ASSERT(mixin_super_type.IsType());
  CollectTypeArguments(cls, Type::Cast(mixin_super_type), type_args);
  AbstractType& mixin_type = AbstractType::Handle();
  Type& generic_mixin_type = Type::Handle();
  Class& mixin_type_class = Class::Handle();
  Class& mixin_app_class = Class::Handle();
  String& mixin_app_class_name = String::Handle();
  String& mixin_type_class_name = String::Handle();
  const intptr_t depth = mixin_app_type.Depth();
  for (intptr_t i = 0; i < depth; i++) {
    mixin_type = mixin_app_type.MixinTypeAt(i);
    ASSERT(!mixin_type.IsNull());
    ResolveType(cls, mixin_type, kCanonicalizeWellFormed);
    ASSERT(mixin_type.HasResolvedTypeClass());
    ASSERT(mixin_type.IsType());
    CollectTypeArguments(cls, Type::Cast(mixin_type), type_args);

    // The name of the mixin application class is a combination of
    // the super class name and mixin class name.
    mixin_app_class_name = mixin_super_type.ClassName();
    mixin_app_class_name = String::Concat(mixin_app_class_name,
                                          Symbols::Ampersand());
    mixin_type_class_name = mixin_type.ClassName();
    mixin_app_class_name = String::Concat(mixin_app_class_name,
                                          mixin_type_class_name);
    mixin_app_class = library.LookupLocalClass(mixin_app_class_name);
    if (mixin_app_class.IsNull()) {
      mixin_app_class_name = Symbols::New(mixin_app_class_name);
      mixin_app_class = Class::New(mixin_app_class_name,
                                   script,
                                   mixin_type.token_pos());
      mixin_app_class.set_super_type(mixin_super_type);
      mixin_type_class = mixin_type.type_class();
      generic_mixin_type = Type::New(mixin_type_class,
                                     Object::null_abstract_type_arguments(),
                                     mixin_type.token_pos());
      mixin_app_class.set_mixin(generic_mixin_type);
      mixin_app_class.set_is_synthesized_class();
      library.AddClass(mixin_app_class);

      // No need to add the new class to pending_classes, since it will be
      // processed via the super_type chain of a pending class.

      if (FLAG_trace_class_finalization) {
        OS::Print("Creating mixin application %s\n",
                  mixin_app_class.ToCString());
      }
    }
    // This mixin application class becomes the type class of the super type of
    // the next mixin application class. It is however too early to provide the
    // correct super type arguments. We use the raw type for now.
    mixin_super_type = Type::New(mixin_app_class,
                                 Object::null_abstract_type_arguments(),
                                 mixin_type.token_pos());
  }
  AbstractType& type_arg = AbstractType::Handle();
  const TypeArguments& mixin_app_args =
    TypeArguments::Handle(TypeArguments::New(type_args.Length()));
  for (intptr_t i = 0; i < type_args.Length(); i++) {
    type_arg ^= type_args.At(i);
    mixin_app_args.SetTypeAt(i, type_arg);
  }
  if (FLAG_trace_class_finalization) {
    OS::Print("ResolveMixinAppType: mixin appl type args: %s\n",
              mixin_app_args.ToCString());
  }
  // The mixin application class at depth k is a subclass of mixin application
  // class at depth k - 1. Build a new super type with the class at the highest
  // depth (the last one processed by the loop above) as the type class and the
  // collected type arguments from the super type and all mixin types.
  // This super type replaces the MixinAppType object in the class that extends
  // the mixin application.
  return Type::New(mixin_app_class, mixin_app_args, mixin_app_type.token_pos());
}


// Recursively walks the graph of explicitly declared super type and
// interfaces, resolving unresolved super types and interfaces.
// Reports an error if there is an interface reference that cannot be
// resolved, or if there is a cycle in the graph. We detect cycles by
// remembering interfaces we've visited in each path through the
// graph. If we visit an interface a second time on a given path,
// we found a loop.
void ClassFinalizer::ResolveSuperTypeAndInterfaces(
    const Class& cls, GrowableArray<intptr_t>* visited) {
  ASSERT(visited != NULL);
  if (FLAG_trace_class_finalization) {
    OS::Print("Resolving super and interfaces: %s\n", cls.ToCString());
  }
  const intptr_t cls_index = cls.id();
  for (intptr_t i = 0; i < visited->length(); i++) {
    if ((*visited)[i] == cls_index) {
      // We have already visited class 'cls'. We found a cycle.
      const String& class_name = String::Handle(cls.Name());
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, cls.token_pos(),
                  "cyclic reference found for class '%s'",
                  class_name.ToCString());
    }
  }

  // If the class/interface has no explicit super class/interfaces
  // and is not a mixin application, we are done.
  AbstractType& super_type = AbstractType::Handle(cls.super_type());
  Array& super_interfaces = Array::Handle(cls.interfaces());
  if ((super_type.IsNull() || super_type.IsObjectType()) &&
      (super_interfaces.Length() == 0)) {
    return;
  }

  if (super_type.IsMixinAppType()) {
    const MixinAppType& mixin_app_type = MixinAppType::Cast(super_type);
    super_type = ResolveMixinAppType(cls, mixin_app_type);
    cls.set_super_type(super_type);
  }

  // If cls belongs to core lib, restrictions about allowed interfaces
  // are lifted.
  const bool cls_belongs_to_core_lib = cls.library() == Library::CoreLibrary();

  // Resolve and check the super type and interfaces of cls.
  visited->Add(cls_index);
  AbstractType& interface = AbstractType::Handle();
  Class& interface_class = Class::Handle();

  // Resolve super type. Failures lead to a longjmp.
  ResolveType(cls, super_type, kCanonicalizeWellFormed);
  if (super_type.IsMalformed()) {
    ReportError(Error::Handle(super_type.malformed_error()));
  }
  if (super_type.IsDynamicType()) {
    const Script& script = Script::Handle(cls.script());
    ReportError(Error::Handle(),  // No previous error.
                script, cls.token_pos(),
                "class '%s' may not extend 'dynamic'",
                String::Handle(cls.Name()).ToCString());
  }
  interface_class = super_type.type_class();
  if (interface_class.IsSignatureClass()) {
    const Script& script = Script::Handle(cls.script());
    ReportError(Error::Handle(),  // No previous error.
                script, cls.token_pos(),
                "class '%s' may not extend function type alias '%s'",
                String::Handle(cls.Name()).ToCString(),
                String::Handle(super_type.UserVisibleName()).ToCString());
  }

  // If cls belongs to core lib or to core lib's implementation, restrictions
  // about allowed interfaces are lifted.
  if (!cls_belongs_to_core_lib) {
    // Prevent extending core implementation classes.
    bool is_error = false;
    switch (interface_class.id()) {
      case kNumberCid:
      case kIntegerCid:  // Class Integer, not int.
      case kSmiCid:
      case kMintCid:
      case kBigintCid:
      case kDoubleCid:  // Class Double, not double.
      case kOneByteStringCid:
      case kTwoByteStringCid:
      case kExternalOneByteStringCid:
      case kExternalTwoByteStringCid:
      case kBoolCid:
      case kNullCid:
      case kArrayCid:
      case kImmutableArrayCid:
      case kGrowableObjectArrayCid:
#define DO_NOT_EXTEND_TYPED_DATA_CLASSES(clazz)                                \
      case kTypedData##clazz##Cid:                                             \
      case kTypedData##clazz##ViewCid:                                         \
      case kExternalTypedData##clazz##Cid:
      CLASS_LIST_TYPED_DATA(DO_NOT_EXTEND_TYPED_DATA_CLASSES)
#undef DO_NOT_EXTEND_TYPED_DATA_CLASSES
      case kByteDataViewCid:
      case kWeakPropertyCid:
        is_error = true;
        break;
      default: {
        // Special case: classes for which we don't have a known class id.
        if (super_type.IsDoubleType() ||
            super_type.IsIntType() ||
            super_type.IsStringType()) {
          is_error = true;
        }
        break;
      }
    }
    if (is_error) {
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, cls.token_pos(),
                  "'%s' is not allowed to extend '%s'",
                  String::Handle(cls.Name()).ToCString(),
                  String::Handle(interface_class.Name()).ToCString());
    }
  }
  // Now resolve the super interfaces of the super type.
  ResolveSuperTypeAndInterfaces(interface_class, visited);

  // Resolve interfaces. Failures lead to a longjmp.
  for (intptr_t i = 0; i < super_interfaces.Length(); i++) {
    interface ^= super_interfaces.At(i);
    ResolveType(cls, interface, kCanonicalizeWellFormed);
    ASSERT(!interface.IsTypeParameter());  // Should be detected by parser.
    if (interface.IsMalformed()) {
      ReportError(Error::Handle(interface.malformed_error()));
    }
    if (interface.IsDynamicType()) {
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, cls.token_pos(),
                  "'dynamic' may not be used as interface");
    }
    interface_class = interface.type_class();
    if (interface_class.IsSignatureClass()) {
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, cls.token_pos(),
                  "function type alias '%s' may not be used as interface",
                  String::Handle(interface_class.Name()).ToCString());
    }
    // Verify that unless cls belongs to core lib, it cannot extend, implement,
    // or mixin any of Null, bool, num, int, double, String, dynamic.
    if (!cls_belongs_to_core_lib) {
      if (interface.IsBoolType() ||
          interface.IsNullType() ||
          interface.IsNumberType() ||
          interface.IsIntType() ||
          interface.IsDoubleType() ||
          interface.IsStringType() ||
          interface.IsDynamicType()) {
        const Script& script = Script::Handle(cls.script());
        const String& interface_name = String::Handle(interface_class.Name());
        if (cls.IsMixinApplication()) {
          ReportError(Error::Handle(),  // No previous error.
                      script, cls.token_pos(),
                      "illegal mixin of '%s'",
                      interface_name.ToCString());
        } else {
          ReportError(Error::Handle(),  // No previous error.
                      script, cls.token_pos(),
                      "'%s' is not allowed to extend or implement '%s'",
                      String::Handle(cls.Name()).ToCString(),
                      interface_name.ToCString());
        }
      }
    }
    interface_class.set_is_implemented();
    // Now resolve the super interfaces.
    ResolveSuperTypeAndInterfaces(interface_class, visited);
  }
  visited->RemoveLast();
}


// A class is marked as constant if it has one constant constructor.
// A constant class can only have final instance fields.
// Note: we must check for cycles before checking for const properties.
void ClassFinalizer::CheckForLegalConstClass(const Class& cls) {
  ASSERT(cls.is_const());
  const Array& fields_array = Array::Handle(cls.fields());
  intptr_t len = fields_array.Length();
  Field& field = Field::Handle();
  for (intptr_t i = 0; i < len; i++) {
    field ^= fields_array.At(i);
    if (!field.is_static() && !field.is_final()) {
      const String& class_name = String::Handle(cls.Name());
      const String& field_name = String::Handle(field.name());
      const Script& script = Script::Handle(cls.script());
      ReportError(Error::Handle(),  // No previous error.
                  script, field.token_pos(),
                  "const class '%s' has non-final field '%s'",
                  class_name.ToCString(), field_name.ToCString());
    }
  }
}


void ClassFinalizer::PrintClassInformation(const Class& cls) {
  HANDLESCOPE(Isolate::Current());
  const String& class_name = String::Handle(cls.Name());
  OS::Print("class '%s'", class_name.ToCString());
  const Library& library = Library::Handle(cls.library());
  if (!library.IsNull()) {
    OS::Print(" library '%s%s':\n",
              String::Handle(library.url()).ToCString(),
              String::Handle(library.private_key()).ToCString());
  } else {
    OS::Print(" (null library):\n");
  }
  const AbstractType& super_type = AbstractType::Handle(cls.super_type());
  if (super_type.IsNull()) {
    OS::Print("  Super: NULL");
  } else {
    const String& super_name = String::Handle(super_type.Name());
    OS::Print("  Super: %s", super_name.ToCString());
  }
  const Array& interfaces_array = Array::Handle(cls.interfaces());
  if (interfaces_array.Length() > 0) {
    OS::Print("; interfaces: ");
    AbstractType& interface = AbstractType::Handle();
    intptr_t len = interfaces_array.Length();
    for (intptr_t i = 0; i < len; i++) {
      interface ^= interfaces_array.At(i);
      OS::Print("  %s ", interface.ToCString());
    }
  }
  OS::Print("\n");
  const Array& functions_array = Array::Handle(cls.functions());
  Function& function = Function::Handle();
  intptr_t len = functions_array.Length();
  for (intptr_t i = 0; i < len; i++) {
    function ^= functions_array.At(i);
    OS::Print("  %s\n", function.ToCString());
  }
  const Array& fields_array = Array::Handle(cls.fields());
  Field& field = Field::Handle();
  len = fields_array.Length();
  for (intptr_t i = 0; i < len; i++) {
    field ^= fields_array.At(i);
    OS::Print("  %s\n", field.ToCString());
  }
}

// Either report an error or mark the type as malformed.
void ClassFinalizer::ReportMalformedType(const Error& prev_error,
                                         const Script& script,
                                         const Type& type,
                                         const char* format,
                                         va_list args) {
  LanguageError& error = LanguageError::Handle();
  if (prev_error.IsNull()) {
    error ^= Parser::FormatError(
        script, type.token_pos(), "Error", format, args);
  } else {
    error ^= Parser::FormatErrorWithAppend(
        prev_error, script, type.token_pos(), "Error", format, args);
  }
  if (FLAG_error_on_bad_type) {
    ReportError(error);
  }
  type.set_malformed_error(error);
  // Make the type raw, since it may not be possible to
  // properly finalize its type arguments.
  type.set_type_class(Class::Handle(Object::dynamic_class()));
  type.set_arguments(Object::null_abstract_type_arguments());
  if (!type.IsFinalized()) {
    type.SetIsFinalized();
    // Do not canonicalize malformed types, since they may not be resolved.
  } else {
    // The only case where the malformed type was already finalized is when its
    // type arguments are not within bounds. In that case, we have a prev_error.
    ASSERT(!prev_error.IsNull());
  }
}


RawType* ClassFinalizer::NewFinalizedMalformedType(const Error& prev_error,
                                                   const Script& script,
                                                   intptr_t type_pos,
                                                   const char* format, ...) {
  va_list args;
  va_start(args, format);
  const UnresolvedClass& unresolved_class = UnresolvedClass::Handle(
      UnresolvedClass::New(LibraryPrefix::Handle(),
                           Symbols::Empty(),
                           type_pos));
  const Type& type = Type::Handle(
      Type::New(unresolved_class, TypeArguments::Handle(), type_pos));
  ReportMalformedType(prev_error, script, type, format, args);
  va_end(args);
  ASSERT(type.IsMalformed());
  ASSERT(type.IsFinalized());
  return type.raw();
}


void ClassFinalizer::FinalizeMalformedType(const Error& prev_error,
                                           const Script& script,
                                           const Type& type,
                                           const char* format, ...) {
  va_list args;
  va_start(args, format);
  ReportMalformedType(prev_error, script, type, format, args);
  va_end(args);
}


void ClassFinalizer::ReportError(const Error& error) {
  Isolate::Current()->long_jump_base()->Jump(1, error);
  UNREACHABLE();
}


void ClassFinalizer::ReportError(const Error& prev_error,
                                 const Script& script,
                                 intptr_t token_pos,
                                 const char* format, ...) {
  va_list args;
  va_start(args, format);
  Error& error = Error::Handle();
  if (prev_error.IsNull()) {
    error ^= Parser::FormatError(script, token_pos, "Error", format, args);
  } else {
    error ^= Parser::FormatErrorWithAppend(
        prev_error, script, token_pos, "Error", format, args);
  }
  va_end(args);
  ReportError(error);
}


void ClassFinalizer::VerifyImplicitFieldOffsets() {
#ifdef DEBUG
  Isolate* isolate = Isolate::Current();
  const ClassTable& class_table = *(isolate->class_table());
  Class& cls = Class::Handle(isolate);
  Array& fields_array = Array::Handle(isolate);
  Field& field = Field::Handle(isolate);
  String& name = String::Handle(isolate);
  String& expected_name = String::Handle(isolate);
  Error& error = Error::Handle(isolate);

  // First verify field offsets of all the TypedDataView classes.
  for (intptr_t cid = kTypedDataInt8ArrayViewCid;
       cid <= kTypedDataFloat32x4ArrayViewCid;
       cid++) {
    cls = class_table.At(cid);  // Get the TypedDataView class.
    error = cls.EnsureIsFinalized(isolate);
    ASSERT(error.IsNull());
    cls = cls.SuperClass();  // Get it's super class '_TypedListView'.
    fields_array ^= cls.fields();
    ASSERT(fields_array.Length() == TypedDataView::NumberOfFields());
    field ^= fields_array.At(0);
    ASSERT(field.Offset() == TypedDataView::data_offset());
    name ^= field.name();
    expected_name ^= String::New("_typedData");
    ASSERT(String::EqualsIgnoringPrivateKey(name, expected_name));
    field ^= fields_array.At(1);
    ASSERT(field.Offset() == TypedDataView::offset_in_bytes_offset());
    name ^= field.name();
    ASSERT(name.Equals("offsetInBytes"));
    field ^= fields_array.At(2);
    ASSERT(field.Offset() == TypedDataView::length_offset());
    name ^= field.name();
    ASSERT(name.Equals("length"));
  }

  // Now verify field offsets of '_ByteDataView' class.
  cls = class_table.At(kByteDataViewCid);
  error = cls.EnsureIsFinalized(isolate);
  ASSERT(error.IsNull());
  fields_array ^= cls.fields();
  ASSERT(fields_array.Length() == TypedDataView::NumberOfFields());
  field ^= fields_array.At(0);
  ASSERT(field.Offset() == TypedDataView::data_offset());
  name ^= field.name();
  expected_name ^= String::New("_typedData");
  ASSERT(String::EqualsIgnoringPrivateKey(name, expected_name));
  field ^= fields_array.At(1);
  ASSERT(field.Offset() == TypedDataView::offset_in_bytes_offset());
  name ^= field.name();
  expected_name ^= String::New("_offset");
  ASSERT(String::EqualsIgnoringPrivateKey(name, expected_name));
  field ^= fields_array.At(2);
  ASSERT(field.Offset() == TypedDataView::length_offset());
  name ^= field.name();
  ASSERT(name.Equals("length"));
#endif
}

}  // namespace dart
