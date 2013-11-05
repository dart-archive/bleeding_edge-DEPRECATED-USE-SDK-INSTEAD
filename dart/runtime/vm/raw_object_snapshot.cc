// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/bigint_operations.h"
#include "vm/object.h"
#include "vm/object_store.h"
#include "vm/snapshot.h"
#include "vm/symbols.h"
#include "vm/visitor.h"

namespace dart {

DECLARE_FLAG(bool, error_on_bad_type);


#define NEW_OBJECT(type)                                                       \
  ((kind == Snapshot::kFull) ? reader->New##type() : type::New())

#define NEW_OBJECT_WITH_LEN(type, len)                                         \
  ((kind == Snapshot::kFull) ? reader->New##type(len) : type::New(len))

#define NEW_OBJECT_WITH_LEN_SPACE(type, len, kind)                             \
  ((kind == Snapshot::kFull) ?                                                 \
  reader->New##type(len) : type::New(len, HEAP_SPACE(kind)))


static uword BigintAllocator(intptr_t size) {
  Zone* zone = Isolate::Current()->current_zone();
  return zone->AllocUnsafe(size);
}


RawClass* Class::ReadFrom(SnapshotReader* reader,
                          intptr_t object_id,
                          intptr_t tags,
                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  Class& cls = Class::ZoneHandle(reader->isolate(), Class::null());
  if ((kind == Snapshot::kFull) ||
      (kind == Snapshot::kScript && !RawObject::IsCreatedFromSnapshot(tags))) {
    // Read in the base information.
    intptr_t class_id = reader->ReadIntptrValue();

    // Allocate class object of specified kind.
    if (kind == Snapshot::kFull) {
      cls = reader->NewClass(class_id);
    } else {
      if (class_id < kNumPredefinedCids) {
        ASSERT((class_id >= kInstanceCid) && (class_id <= kMirrorReferenceCid));
        cls = reader->isolate()->class_table()->At(class_id);
      } else {
        cls = New<Instance>(kIllegalCid);
      }
    }
    reader->AddBackRef(object_id, &cls, kIsDeserialized);

    // Set the object tags.
    cls.set_tags(tags);

    // Set all non object fields.
    if (!RawObject::IsInternalVMdefinedClassId(class_id)) {
      // Instance size of a VM defined class is already set up.
      cls.set_instance_size_in_words(reader->ReadIntptrValue());
      cls.set_next_field_offset_in_words(reader->ReadIntptrValue());
    }
    cls.set_type_arguments_field_offset_in_words(reader->ReadIntptrValue());
    cls.set_num_type_arguments(reader->Read<int16_t>());
    cls.set_num_own_type_arguments(reader->Read<int16_t>());
    cls.set_num_native_fields(reader->Read<uint16_t>());
    cls.set_token_pos(reader->ReadIntptrValue());
    cls.set_state_bits(reader->Read<uint16_t>());

    // Set all the object fields.
    // TODO(5411462): Need to assert No GC can happen here, even though
    // allocations may happen.
    intptr_t num_flds = (cls.raw()->to() - cls.raw()->from());
    for (intptr_t i = 0; i <= num_flds; i++) {
      *(cls.raw()->from() + i) = reader->ReadObjectRef();
    }
  } else {
    cls ^= reader->ReadClassId(object_id);
  }
  return cls.raw();
}


void RawClass::WriteTo(SnapshotWriter* writer,
                       intptr_t object_id,
                       Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  if ((kind == Snapshot::kFull) ||
      (kind == Snapshot::kScript &&
       !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this)))) {
    // Write out the class and tags information.
    writer->WriteVMIsolateObject(kClassCid);
    writer->WriteIntptrValue(writer->GetObjectTags(this));

    // Write out all the non object pointer fields.
    // NOTE: cpp_vtable_ is not written.
    intptr_t class_id = ptr()->id_;
    writer->WriteIntptrValue(class_id);
    if (!RawObject::IsInternalVMdefinedClassId(class_id)) {
      // We don't write the instance size of VM defined classes as they
      // are already setup during initialization as part of pre populating
      // the class table.
      writer->WriteIntptrValue(ptr()->instance_size_in_words_);
      writer->WriteIntptrValue(ptr()->next_field_offset_in_words_);
    }
    writer->WriteIntptrValue(ptr()->type_arguments_field_offset_in_words_);
    writer->Write<int16_t>(ptr()->num_type_arguments_);
    writer->Write<int16_t>(ptr()->num_own_type_arguments_);
    writer->Write<uint16_t>(ptr()->num_native_fields_);
    writer->WriteIntptrValue(ptr()->token_pos_);
    writer->Write<uint16_t>(ptr()->state_bits_);

    // Write out all the object pointer fields.
    SnapshotWriterVisitor visitor(writer);
    visitor.VisitPointers(from(), to());
  } else {
    writer->WriteClassId(this);
  }
}


RawUnresolvedClass* UnresolvedClass::ReadFrom(SnapshotReader* reader,
                                              intptr_t object_id,
                                              intptr_t tags,
                                              Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate unresolved class object.
  UnresolvedClass& unresolved_class = UnresolvedClass::ZoneHandle(
      reader->isolate(), NEW_OBJECT(UnresolvedClass));
  reader->AddBackRef(object_id, &unresolved_class, kIsDeserialized);

  // Set the object tags.
  unresolved_class.set_tags(tags);

  // Set all non object fields.
  unresolved_class.set_token_pos(reader->ReadIntptrValue());

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (unresolved_class.raw()->to() -
                       unresolved_class.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    unresolved_class.StorePointer((unresolved_class.raw()->from() + i),
                                  reader->ObjectHandle()->raw());
  }
  return unresolved_class.raw();
}


void RawUnresolvedClass::WriteTo(SnapshotWriter* writer,
                                 intptr_t object_id,
                                 Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kUnresolvedClassCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the non object pointer fields.
  writer->WriteIntptrValue(ptr()->token_pos_);

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawAbstractType* AbstractType::ReadFrom(SnapshotReader* reader,
                                        intptr_t object_id,
                                        intptr_t tags,
                                        Snapshot::Kind kind) {
  UNREACHABLE();  // AbstractType is an abstract class.
  return NULL;
}


void RawAbstractType::WriteTo(SnapshotWriter* writer,
                              intptr_t object_id,
                              Snapshot::Kind kind) {
  UNREACHABLE();  // AbstractType is an abstract class.
}


RawType* Type::ReadFrom(SnapshotReader* reader,
                        intptr_t object_id,
                        intptr_t tags,
                        Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate type object.
  Type& type = Type::ZoneHandle(reader->isolate(), NEW_OBJECT(Type));
  reader->AddBackRef(object_id, &type, kIsDeserialized);

  // Set all non object fields.
  type.set_token_pos(reader->ReadIntptrValue());
  type.set_type_state(reader->Read<int8_t>());

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (type.raw()->to() - type.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectImpl();
    type.StorePointer((type.raw()->from() + i), reader->ObjectHandle()->raw());
  }

  // If object needs to be a canonical object, Canonicalize it.
  // When reading a full snapshot we don't need to canonicalize the object
  // as it would already be a canonical object.
  // When reading a script snapshot we need to canonicalize only those object
  // references that are objects from the core library (loaded from a
  // full snapshot). Objects that are only in the script need not be
  // canonicalized as they are already canonical.
  // When reading a message snapshot we always have to canonicalize the object.
  if ((kind != Snapshot::kFull) && RawObject::IsCanonical(tags) &&
      (RawObject::IsCreatedFromSnapshot(tags) ||
       (kind == Snapshot::kMessage))) {
    type ^= type.Canonicalize();
  }

  // Set the object tags (This is done after 'Canonicalize', which
  // does not canonicalize a type already marked as canonical).
  type.set_tags(tags);

  return type.raw();
}


static const char* RawOneByteStringToCString(RawOneByteString* str) {
  const char* start = reinterpret_cast<char*>(str) - kHeapObjectTag +
      OneByteString::data_offset();
  const int len = Smi::Value(*reinterpret_cast<RawSmi**>(
      reinterpret_cast<uword>(str) - kHeapObjectTag + String::length_offset()));
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len + 1);
  memmove(chars, start, len);
  chars[len] = '\0';
  return chars;
}


void RawType::WriteTo(SnapshotWriter* writer,
                      intptr_t object_id,
                      Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Only resolved and finalized types should be written to a snapshot.
  // TODO(regis): Replace the test below by an ASSERT() or remove the flag test.
  if (FLAG_error_on_bad_type &&
      (ptr()->type_state_ != RawType::kFinalizedInstantiated) &&
      (ptr()->type_state_ != RawType::kFinalizedUninstantiated)) {
    // Print the name of the class of the unfinalized type, as well as the
    // token location from where it is referred to, making sure not
    // to allocate any handles. Unfortunately, we cannot print the script name.
    const intptr_t cid = ClassIdTag::decode(*reinterpret_cast<uword*>(
        reinterpret_cast<uword>(ptr()->type_class_) - kHeapObjectTag +
            Object::tags_offset()));
    if (cid == kUnresolvedClassCid) {
      OS::Print("Snapshotting unresolved type '%s' at token pos %" Pd "\n",
                RawOneByteStringToCString(
                    reinterpret_cast<RawOneByteString*>(
                        reinterpret_cast<RawUnresolvedClass*>(
                            ptr()->type_class_)->ptr()->ident_)),
                ptr()->token_pos_);
    } else {
      // Assume cid == kClassId, but it can also be kIllegalCid.
      OS::Print("Snapshotting unfinalized type '%s' at token pos %" Pd "\n",
                RawOneByteStringToCString(
                    reinterpret_cast<RawOneByteString*>(
                        reinterpret_cast<RawClass*>(
                            ptr()->type_class_)->ptr()->name_)),
                ptr()->token_pos_);
    }
    UNREACHABLE();
  }

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kTypeCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the non object pointer fields.
  writer->WriteIntptrValue(ptr()->token_pos_);
  writer->Write<int8_t>(ptr()->type_state_);

  // Write out all the object pointer fields. Since we will be canonicalizing
  // the type object when reading it back we should write out all the fields
  // inline and not as references.
  SnapshotWriterVisitor visitor(writer, false);
  visitor.VisitPointers(from(), to());
}


RawTypeParameter* TypeParameter::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate type parameter object.
  TypeParameter& type_parameter = TypeParameter::ZoneHandle(
      reader->isolate(), NEW_OBJECT(TypeParameter));
  reader->AddBackRef(object_id, &type_parameter, kIsDeserialized);

  // Set the object tags.
  type_parameter.set_tags(tags);

  // Set all non object fields.
  type_parameter.set_index(reader->ReadIntptrValue());
  type_parameter.set_token_pos(reader->ReadIntptrValue());
  type_parameter.set_type_state(reader->Read<int8_t>());

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (type_parameter.raw()->to() -
                       type_parameter.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    type_parameter.StorePointer((type_parameter.raw()->from() + i),
                                reader->ObjectHandle()->raw());
  }

  return type_parameter.raw();
}


void RawTypeParameter::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Only finalized type parameters should be written to a snapshot.
  // TODO(regis): Replace the test below by an ASSERT() or remove the flag test.
  if (FLAG_error_on_bad_type &&
      (ptr()->type_state_ != RawTypeParameter::kFinalizedUninstantiated)) {
    // Print the name of the unfinalized type parameter, the name of the class
    // it parameterizes, as well as the token location from where it is referred
    // to, making sure not to allocate any handles. Unfortunately, we cannot
    // print the script name.
    OS::Print("Snapshotting unfinalized type parameter '%s' of class '%s' at "
              "token pos %" Pd "\n",
              RawOneByteStringToCString(
                  reinterpret_cast<RawOneByteString*>(ptr()->name_)),
              RawOneByteStringToCString(
                  reinterpret_cast<RawOneByteString*>(
                      reinterpret_cast<RawClass*>(
                          ptr()->parameterized_class_)->ptr()->name_)),
              ptr()->token_pos_);
    UNREACHABLE();
  }

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kTypeParameterCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the non object pointer fields.
  writer->WriteIntptrValue(ptr()->index_);
  writer->WriteIntptrValue(ptr()->token_pos_);
  writer->Write<int8_t>(ptr()->type_state_);

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawBoundedType* BoundedType::ReadFrom(SnapshotReader* reader,
                                      intptr_t object_id,
                                      intptr_t tags,
                                      Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate bounded type object.
  BoundedType& bounded_type = BoundedType::ZoneHandle(
      reader->isolate(), NEW_OBJECT(BoundedType));
  reader->AddBackRef(object_id, &bounded_type, kIsDeserialized);

  // Set the object tags.
  bounded_type.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (bounded_type.raw()->to() -
                       bounded_type.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    bounded_type.StorePointer((bounded_type.raw()->from() + i),
                              reader->ObjectHandle()->raw());
  }

  bounded_type.set_is_being_checked(false);

  return bounded_type.raw();
}


void RawBoundedType::WriteTo(SnapshotWriter* writer,
                             intptr_t object_id,
                             Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kBoundedTypeCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawMixinAppType* MixinAppType::ReadFrom(SnapshotReader* reader,
                                        intptr_t object_id,
                                        intptr_t tags,
                                        Snapshot::Kind kind) {
  UNREACHABLE();  // MixinAppType objects do not survive finalization.
  return MixinAppType::null();
}


void RawMixinAppType::WriteTo(SnapshotWriter* writer,
                              intptr_t object_id,
                              Snapshot::Kind kind) {
  UNREACHABLE();  // MixinAppType objects do not survive finalization.
}


RawAbstractTypeArguments* AbstractTypeArguments::ReadFrom(
    SnapshotReader* reader,
    intptr_t object_id,
    intptr_t tags,
    Snapshot::Kind kind) {
  UNREACHABLE();  // AbstractTypeArguments is an abstract class.
  return TypeArguments::null();
}


void RawAbstractTypeArguments::WriteTo(SnapshotWriter* writer,
                                       intptr_t object_id,
                                       Snapshot::Kind kind) {
  UNREACHABLE();  // AbstractTypeArguments is an abstract class.
}


RawTypeArguments* TypeArguments::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Read the length so that we can determine instance size to allocate.
  intptr_t len = reader->ReadSmiValue();

  TypeArguments& type_arguments = TypeArguments::ZoneHandle(
      reader->isolate(), NEW_OBJECT_WITH_LEN_SPACE(TypeArguments, len, kind));
  reader->AddBackRef(object_id, &type_arguments, kIsDeserialized);

  // Now set all the object fields.
  for (intptr_t i = 0; i < len; i++) {
    *reader->TypeHandle() ^= reader->ReadObjectImpl();
    type_arguments.SetTypeAt(i, *reader->TypeHandle());
  }

  // If object needs to be a canonical object, Canonicalize it.
  // When reading a full snapshot we don't need to canonicalize the object
  // as it would already be a canonical object.
  // When reading a script snapshot we need to canonicalize only those object
  // references that are objects from the core library (loaded from a
  // full snapshot). Objects that are only in the script need not be
  // canonicalized as they are already canonical.
  // When reading a message snapshot we always have to canonicalize the object.
  if ((kind != Snapshot::kFull) && RawObject::IsCanonical(tags) &&
      (RawObject::IsCreatedFromSnapshot(tags) ||
       (kind == Snapshot::kMessage))) {
    type_arguments ^= type_arguments.Canonicalize();
  }

  // Set the object tags (This is done after setting the object fields
  // because 'SetTypeAt' has an assertion to check if the object is not
  // already canonical. Also, this is done after 'Canonicalize', which
  // does not canonicalize a type already marked as canonical).
  type_arguments.set_tags(tags);

  return type_arguments.raw();
}


void RawTypeArguments::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kTypeArgumentsCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the length field.
  writer->Write<RawObject*>(ptr()->length_);

  // Write out the individual types.
  intptr_t len = Smi::Value(ptr()->length_);
  for (intptr_t i = 0; i < len; i++) {
    writer->WriteObjectImpl(ptr()->types_[i]);
  }
}


RawInstantiatedTypeArguments* InstantiatedTypeArguments::ReadFrom(
    SnapshotReader* reader,
    intptr_t object_id,
    intptr_t tags,
    Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(kind == Snapshot::kMessage);

  // Allocate instantiated types object.
  InstantiatedTypeArguments& instantiated_type_arguments =
      InstantiatedTypeArguments::ZoneHandle(reader->isolate(),
                                            InstantiatedTypeArguments::New());
  reader->AddBackRef(object_id, &instantiated_type_arguments, kIsDeserialized);

  // Set the object tags.
  instantiated_type_arguments.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (instantiated_type_arguments.raw()->to() -
                       instantiated_type_arguments.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    instantiated_type_arguments.StorePointer(
        (instantiated_type_arguments.raw()->from() + i),
        reader->ObjectHandle()->raw());
  }
  return instantiated_type_arguments.raw();
}


void RawInstantiatedTypeArguments::WriteTo(SnapshotWriter* writer,
                                           intptr_t object_id,
                                           Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(kind == Snapshot::kMessage);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kInstantiatedTypeArgumentsCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawPatchClass* PatchClass::ReadFrom(SnapshotReader* reader,
                                    intptr_t object_id,
                                    intptr_t tags,
                                    Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate function object.
  PatchClass& cls = PatchClass::ZoneHandle(reader->isolate(),
                                            NEW_OBJECT(PatchClass));
  reader->AddBackRef(object_id, &cls, kIsDeserialized);

  // Set the object tags.
  cls.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (cls.raw()->to() - cls.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(cls.raw()->from() + i) = reader->ReadObjectRef();
  }

  return cls.raw();
}


void RawPatchClass::WriteTo(SnapshotWriter* writer,
                            intptr_t object_id,
                            Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kPatchClassCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));
  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawClosureData* ClosureData::ReadFrom(SnapshotReader* reader,
                                      intptr_t object_id,
                                      intptr_t tags,
                                      Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate closure data object.
  ClosureData& data = ClosureData::ZoneHandle(
      reader->isolate(), NEW_OBJECT(ClosureData));
  reader->AddBackRef(object_id, &data, kIsDeserialized);

  // Set the object tags.
  data.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (data.raw()->to() - data.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(data.raw()->from() + i) = reader->ReadObjectRef();
  }

  return data.raw();
}


void RawClosureData::WriteTo(SnapshotWriter* writer,
                             intptr_t object_id,
                             Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kClosureDataCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Context scope.
  // We don't write the context scope in the snapshot.
  writer->WriteObjectImpl(Object::null());

  // Parent function.
  writer->WriteObjectImpl(ptr()->parent_function_);

  // Signature class.
  writer->WriteObjectImpl(ptr()->signature_class_);

  // Static closure/Closure allocation stub.
  // We don't write the closure or allocation stub in the snapshot.
  writer->WriteObjectImpl(Object::null());
}


RawRedirectionData* RedirectionData::ReadFrom(SnapshotReader* reader,
                                              intptr_t object_id,
                                              intptr_t tags,
                                              Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate redirection data object.
  RedirectionData& data = RedirectionData::ZoneHandle(
      reader->isolate(), NEW_OBJECT(RedirectionData));
  reader->AddBackRef(object_id, &data, kIsDeserialized);

  // Set the object tags.
  data.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (data.raw()->to() - data.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(data.raw()->from() + i) = reader->ReadObjectRef();
  }

  return data.raw();
}


void RawRedirectionData::WriteTo(SnapshotWriter* writer,
                                 intptr_t object_id,
                                 Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kRedirectionDataCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawFunction* Function::ReadFrom(SnapshotReader* reader,
                                intptr_t object_id,
                                intptr_t tags,
                                Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate function object.
  Function& func = Function::ZoneHandle(
      reader->isolate(), NEW_OBJECT(Function));
  reader->AddBackRef(object_id, &func, kIsDeserialized);

  // Set the object tags.
  func.set_tags(tags);

  // Set all the non object fields.
  func.set_token_pos(reader->ReadIntptrValue());
  func.set_end_token_pos(reader->ReadIntptrValue());
  func.set_usage_counter(reader->ReadIntptrValue());
  func.set_num_fixed_parameters(reader->ReadIntptrValue());
  func.set_num_optional_parameters(reader->ReadIntptrValue());
  func.set_deoptimization_counter(reader->ReadIntptrValue());
  func.set_kind_tag(reader->Read<uint16_t>());
  func.set_optimized_instruction_count(reader->Read<uint16_t>());
  func.set_optimized_call_site_count(reader->Read<uint16_t>());

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (func.raw()->to() - func.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(func.raw()->from() + i) = reader->ReadObjectRef();
  }

  return func.raw();
}


void RawFunction::WriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kFunctionCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the non object fields.
  writer->WriteIntptrValue(ptr()->token_pos_);
  writer->WriteIntptrValue(ptr()->end_token_pos_);
  writer->WriteIntptrValue(ptr()->usage_counter_);
  writer->WriteIntptrValue(ptr()->num_fixed_parameters_);
  writer->WriteIntptrValue(ptr()->num_optional_parameters_);
  writer->WriteIntptrValue(ptr()->deoptimization_counter_);
  writer->Write<uint16_t>(ptr()->kind_tag_);
  writer->Write<uint16_t>(ptr()->optimized_instruction_count_);
  writer->Write<uint16_t>(ptr()->optimized_call_site_count_);

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawField* Field::ReadFrom(SnapshotReader* reader,
                          intptr_t object_id,
                          intptr_t tags,
                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate field object.
  Field& field = Field::ZoneHandle(reader->isolate(), NEW_OBJECT(Field));
  reader->AddBackRef(object_id, &field, kIsDeserialized);

  // Set the object tags.
  field.set_tags(tags);

  // Set all non object fields.
  field.set_token_pos(reader->ReadIntptrValue());
  field.set_guarded_cid(reader->ReadIntptrValue());
  field.set_is_nullable(reader->ReadIntptrValue());
  field.set_kind_bits(reader->Read<uint8_t>());

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (field.raw()->to() - field.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(field.raw()->from() + i) = reader->ReadObjectRef();
  }

  return field.raw();
}


void RawField::WriteTo(SnapshotWriter* writer,
                       intptr_t object_id,
                       Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kFieldCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the non object fields.
  writer->WriteIntptrValue(ptr()->token_pos_);
  writer->WriteIntptrValue(ptr()->guarded_cid_);
  writer->WriteIntptrValue(ptr()->is_nullable_);
  writer->Write<uint8_t>(ptr()->kind_bits_);

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawLiteralToken* LiteralToken::ReadFrom(SnapshotReader* reader,
                                        intptr_t object_id,
                                        intptr_t tags,
                                        Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(kind != Snapshot::kMessage);

  // Create the literal token object.
  LiteralToken& literal_token = LiteralToken::ZoneHandle(
      reader->isolate(), NEW_OBJECT(LiteralToken));
  reader->AddBackRef(object_id, &literal_token, kIsDeserialized);

  // Set the object tags.
  literal_token.set_tags(tags);

  // Read the token attributes.
  Token::Kind token_kind = static_cast<Token::Kind>(reader->ReadIntptrValue());
  literal_token.set_kind(token_kind);
  *reader->StringHandle() ^= reader->ReadObjectImpl();
  literal_token.set_literal(*reader->StringHandle());
  *reader->ObjectHandle() = reader->ReadObjectImpl();
  literal_token.set_value(*reader->ObjectHandle());

  return literal_token.raw();
}


void RawLiteralToken::WriteTo(SnapshotWriter* writer,
                              intptr_t object_id,
                              Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(kind != Snapshot::kMessage);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kLiteralTokenCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the kind field.
  writer->Write<intptr_t>(ptr()->kind_);

  // Write out literal and value fields.
  writer->WriteObjectImpl(ptr()->literal_);
  writer->WriteObjectImpl(ptr()->value_);
}


RawTokenStream* TokenStream::ReadFrom(SnapshotReader* reader,
                                      intptr_t object_id,
                                      intptr_t tags,
                                      Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Read the length so that we can determine number of tokens to read.
  intptr_t len = reader->ReadSmiValue();

  // Create the token stream object.
  TokenStream& token_stream = TokenStream::ZoneHandle(
      reader->isolate(), NEW_OBJECT_WITH_LEN(TokenStream, len));
  reader->AddBackRef(object_id, &token_stream, kIsDeserialized);

  // Set the object tags.
  token_stream.set_tags(tags);

  // Read the stream of tokens into the TokenStream object for script
  // snapshots as we made a copy of token stream.
  if (kind == Snapshot::kScript) {
    NoGCScope no_gc;
    RawExternalTypedData* stream = token_stream.GetStream();
    reader->ReadBytes(stream->ptr()->data_, len);
  }

  // Read in the literal/identifier token array.
  *(reader->TokensHandle()) ^= reader->ReadObjectImpl();
  token_stream.SetTokenObjects(*(reader->TokensHandle()));
  // Read in the private key in use by the token stream.
  *(reader->StringHandle()) ^= reader->ReadObjectImpl();
  token_stream.SetPrivateKey(*(reader->StringHandle()));

  return token_stream.raw();
}


void RawTokenStream::WriteTo(SnapshotWriter* writer,
                             intptr_t object_id,
                             Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kTokenStreamCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the length field and the token stream.
  RawExternalTypedData* stream = ptr()->stream_;
  intptr_t len = Smi::Value(stream->ptr()->length_);
  writer->Write<RawObject*>(stream->ptr()->length_);
  writer->WriteBytes(stream->ptr()->data_, len);

  // Write out the literal/identifier token array.
  writer->WriteObjectImpl(ptr()->token_objects_);
  // Write out the private key in use by the token stream.
  writer->WriteObjectImpl(ptr()->private_key_);
}


RawScript* Script::ReadFrom(SnapshotReader* reader,
                            intptr_t object_id,
                            intptr_t tags,
                            Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate script object.
  Script& script = Script::ZoneHandle(reader->isolate(), NEW_OBJECT(Script));
  reader->AddBackRef(object_id, &script, kIsDeserialized);

  // Set the object tags.
  script.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  *reader->StringHandle() ^= reader->ReadObjectImpl();
  script.set_url(*reader->StringHandle());
  *reader->StringHandle() ^= String::null();
  script.set_source(*reader->StringHandle());
  TokenStream& stream = TokenStream::Handle();
  stream ^= reader->ReadObjectImpl();
  script.set_tokens(stream);

  script.raw_ptr()->line_offset_ = reader->Read<int32_t>();
  script.raw_ptr()->col_offset_ = reader->Read<int32_t>();
  script.raw_ptr()->kind_ = reader->Read<int8_t>();

  return script.raw();
}


void RawScript::WriteTo(SnapshotWriter* writer,
                        intptr_t object_id,
                        Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(tokens_ != TokenStream::null());
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kScriptCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  writer->WriteObjectImpl(ptr()->url_);
  writer->WriteObjectImpl(ptr()->tokens_);

  writer->Write<int32_t>(ptr()->line_offset_);
  writer->Write<int32_t>(ptr()->col_offset_);
  writer->Write<int8_t>(ptr()->kind_);
}


RawLibrary* Library::ReadFrom(SnapshotReader* reader,
                              intptr_t object_id,
                              intptr_t tags,
                              Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(kind != Snapshot::kMessage);

  Library& library = Library::ZoneHandle(reader->isolate(), Library::null());
  reader->AddBackRef(object_id, &library, kIsDeserialized);

  if ((kind == Snapshot::kScript) && RawObject::IsCreatedFromSnapshot(tags)) {
    ASSERT(kind != Snapshot::kFull);
    // Lookup the object as it should already exist in the heap.
    *reader->StringHandle() ^= reader->ReadObjectImpl();
    library = Library::LookupLibrary(*reader->StringHandle());
  } else {
    // Allocate library object.
    library = NEW_OBJECT(Library);

    // Set the object tags.
    library.set_tags(tags);

    // Set all non object fields.
    library.raw_ptr()->index_ = reader->ReadIntptrValue();
    library.raw_ptr()->num_imports_ = reader->ReadIntptrValue();
    library.raw_ptr()->num_anonymous_ = reader->ReadIntptrValue();
    library.raw_ptr()->corelib_imported_ = reader->Read<bool>();
    library.raw_ptr()->debuggable_ = reader->Read<bool>();
    library.raw_ptr()->load_state_ = reader->Read<int8_t>();
    // The native resolver is not serialized.
    Dart_NativeEntryResolver resolver =
        reader->Read<Dart_NativeEntryResolver>();
    ASSERT(resolver == NULL);
    library.set_native_entry_resolver(resolver);
    // The cache of loaded scripts is not serialized.
    library.raw_ptr()->loaded_scripts_ = Array::null();

    // Set all the object fields.
    // TODO(5411462): Need to assert No GC can happen here, even though
    // allocations may happen.
    intptr_t num_flds = (library.raw()->to() - library.raw()->from());
    for (intptr_t i = 0; i <= num_flds; i++) {
      *(library.raw()->from() + i) = reader->ReadObjectRef();
    }
    if (kind != Snapshot::kFull) {
      library.Register();
    }
  }
  return library.raw();
}


void RawLibrary::WriteTo(SnapshotWriter* writer,
                         intptr_t object_id,
                         Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(kind != Snapshot::kMessage);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kLibraryCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  if ((kind == Snapshot::kScript) &&
      RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) {
    ASSERT(kind != Snapshot::kFull);
    // Write out library URL so that it can be looked up when reading.
    writer->WriteObjectImpl(ptr()->url_);
  } else {
    // Write out all non object fields.
    writer->WriteIntptrValue(ptr()->index_);
    writer->WriteIntptrValue(ptr()->num_imports_);
    writer->WriteIntptrValue(ptr()->num_anonymous_);
    writer->Write<bool>(ptr()->corelib_imported_);
    writer->Write<bool>(ptr()->debuggable_);
    writer->Write<int8_t>(ptr()->load_state_);
    // We do not serialize the native resolver over, this needs to be explicitly
    // set after deserialization.
    writer->Write<Dart_NativeEntryResolver>(NULL);
    // We do not write the loaded_scripts_ cache to the snapshot. It gets
    // set to NULL when reading the library from the snapshot, and will
    // be rebuilt lazily.

    // Write out all the object pointer fields.
    SnapshotWriterVisitor visitor(writer);
    visitor.VisitPointers(from(), to());
  }
}


RawLibraryPrefix* LibraryPrefix::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate library prefix object.
  LibraryPrefix& prefix = LibraryPrefix::ZoneHandle(
      reader->isolate(), NEW_OBJECT(LibraryPrefix));
  reader->AddBackRef(object_id, &prefix, kIsDeserialized);

  // Set the object tags.
  prefix.set_tags(tags);

  // Set all non object fields.
  prefix.raw_ptr()->num_imports_ = reader->ReadIntptrValue();

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (prefix.raw()->to() - prefix.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(prefix.raw()->from() + i) = reader->ReadObjectRef();
  }

  return prefix.raw();
}


void RawLibraryPrefix::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kLibraryPrefixCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all non object fields.
  writer->WriteIntptrValue(ptr()->num_imports_);

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawNamespace* Namespace::ReadFrom(SnapshotReader* reader,
                                  intptr_t object_id,
                                  intptr_t tags,
                                  Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(tags)) ||
         (kind == Snapshot::kFull));

  // Allocate Namespace object.
  Namespace& ns = Namespace::ZoneHandle(
      reader->isolate(), NEW_OBJECT(Namespace));
  reader->AddBackRef(object_id, &ns, kIsDeserialized);

  // Set the object tags.
  ns.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (ns.raw()->to() - ns.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    *(ns.raw()->from() + i) = reader->ReadObjectRef();
  }

  return ns.raw();
}


void RawNamespace::WriteTo(SnapshotWriter* writer,
                           intptr_t object_id,
                           Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(((kind == Snapshot::kScript) &&
          !RawObject::IsCreatedFromSnapshot(writer->GetObjectTags(this))) ||
         (kind == Snapshot::kFull));

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kNamespaceCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawCode* Code::ReadFrom(SnapshotReader* reader,
                        intptr_t object_id,
                        intptr_t tags,
                        Snapshot::Kind kind) {
  UNREACHABLE();
  return Code::null();
}


void RawCode::WriteTo(SnapshotWriter* writer,
                      intptr_t object_id,
                      Snapshot::Kind kind) {
  // We have already checked for this and written a NULL object, hence we
  // should not reach here.
  UNREACHABLE();
}


RawInstructions* Instructions::ReadFrom(SnapshotReader* reader,
                                        intptr_t object_id,
                                        intptr_t tags,
                                        Snapshot::Kind kind) {
  UNREACHABLE();
  return Instructions::null();
}


void RawInstructions::WriteTo(SnapshotWriter* writer,
                              intptr_t object_id,
                              Snapshot::Kind kind) {
  UNREACHABLE();
}


RawPcDescriptors* PcDescriptors::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  UNREACHABLE();
  return PcDescriptors::null();
}


void RawPcDescriptors::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  UNREACHABLE();
}


RawStackmap* Stackmap::ReadFrom(SnapshotReader* reader,
                                intptr_t object_id,
                                intptr_t tags,
                                Snapshot::Kind kind) {
  UNREACHABLE();
  return Stackmap::null();
}


void RawStackmap::WriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind) {
  UNREACHABLE();
}


RawLocalVarDescriptors* LocalVarDescriptors::ReadFrom(SnapshotReader* reader,
                                                      intptr_t object_id,
                                                      intptr_t tags,
                                                      Snapshot::Kind kind) {
  UNREACHABLE();
  return LocalVarDescriptors::null();
}


void RawLocalVarDescriptors::WriteTo(SnapshotWriter* writer,
                                     intptr_t object_id,
                                     Snapshot::Kind kind) {
  UNREACHABLE();
}


RawExceptionHandlers* ExceptionHandlers::ReadFrom(SnapshotReader* reader,
                                                  intptr_t object_id,
                                                  intptr_t tags,
                                                  Snapshot::Kind kind) {
  UNREACHABLE();
  return ExceptionHandlers::null();
}


void RawExceptionHandlers::WriteTo(SnapshotWriter* writer,
                                   intptr_t object_id,
                                   Snapshot::Kind kind) {
  UNREACHABLE();
}


RawDeoptInfo* DeoptInfo::ReadFrom(SnapshotReader* reader,
                                  intptr_t object_id,
                                  intptr_t tags,
                                  Snapshot::Kind kind) {
  UNREACHABLE();
  return DeoptInfo::null();
}


void RawDeoptInfo::WriteTo(SnapshotWriter* writer,
                           intptr_t object_id,
                           Snapshot::Kind kind) {
  UNREACHABLE();
}


RawContext* Context::ReadFrom(SnapshotReader* reader,
                              intptr_t object_id,
                              intptr_t tags,
                              Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate context object.
  intptr_t num_vars = reader->ReadIntptrValue();
  Context& context = Context::ZoneHandle(reader->isolate(), Context::null());
  if (kind == Snapshot::kFull) {
    context = reader->NewContext(num_vars);
  } else {
    context = Context::New(num_vars, HEAP_SPACE(kind));
  }
  reader->AddBackRef(object_id, &context, kIsDeserialized);

  // Set the object tags.
  context.set_tags(tags);

  // Set the isolate implicitly.
  context.set_isolate(Isolate::Current());

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (context.raw()->to(num_vars) - context.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    context.StorePointer((context.raw()->from() + i),
                         reader->ObjectHandle()->raw());
  }

  return context.raw();
}


void RawContext::WriteTo(SnapshotWriter* writer,
                         intptr_t object_id,
                         Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kContextCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out num of variables in the context.
  writer->WriteIntptrValue(ptr()->num_variables_);

  // Can't serialize the isolate pointer, we set it implicitly on read.

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to(ptr()->num_variables_));
}


RawContextScope* ContextScope::ReadFrom(SnapshotReader* reader,
                                        intptr_t object_id,
                                        intptr_t tags,
                                        Snapshot::Kind kind) {
  UNREACHABLE();
  return NULL;
}


void RawContextScope::WriteTo(SnapshotWriter* writer,
                              intptr_t object_id,
                              Snapshot::Kind kind) {
  UNREACHABLE();
}


RawICData* ICData::ReadFrom(SnapshotReader* reader,
                            intptr_t object_id,
                            intptr_t tags,
                            Snapshot::Kind kind) {
  UNREACHABLE();
  return NULL;
}


void RawICData::WriteTo(SnapshotWriter* writer,
                        intptr_t object_id,
                        Snapshot::Kind kind) {
  UNREACHABLE();
}


RawMegamorphicCache* MegamorphicCache::ReadFrom(SnapshotReader* reader,
                                                intptr_t object_id,
                                                intptr_t tags,
                                                Snapshot::Kind kind) {
  UNREACHABLE();
  return NULL;
}


void RawMegamorphicCache::WriteTo(SnapshotWriter* writer,
                                  intptr_t object_id,
                                  Snapshot::Kind kind) {
  UNREACHABLE();
}


RawSubtypeTestCache* SubtypeTestCache::ReadFrom(SnapshotReader* reader,
                                                intptr_t object_id,
                                                intptr_t tags,
                                                Snapshot::Kind kind) {
  UNREACHABLE();
  return NULL;
}


void RawSubtypeTestCache::WriteTo(SnapshotWriter* writer,
                                  intptr_t object_id,
                                  Snapshot::Kind kind) {
  UNREACHABLE();
}


RawError* Error::ReadFrom(SnapshotReader* reader,
                          intptr_t object_id,
                          intptr_t tags,
                          Snapshot::Kind kind) {
  UNREACHABLE();
  return Error::null();  // Error is an abstract class.
}


void RawError::WriteTo(SnapshotWriter* writer,
                       intptr_t object_id,
                       Snapshot::Kind kind) {
  UNREACHABLE();  // Error is an abstract class.
}


RawApiError* ApiError::ReadFrom(SnapshotReader* reader,
                                intptr_t object_id,
                                intptr_t tags,
                                Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate ApiError object.
  ApiError& api_error =
      ApiError::ZoneHandle(reader->isolate(), NEW_OBJECT(ApiError));
  reader->AddBackRef(object_id, &api_error, kIsDeserialized);

  // Set the object tags.
  api_error.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds = (api_error.raw()->to() - api_error.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    api_error.StorePointer((api_error.raw()->from() + i),
                           reader->ObjectHandle()->raw());
  }

  return api_error.raw();
}


void RawApiError::WriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kApiErrorCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawLanguageError* LanguageError::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate LanguageError object.
  LanguageError& language_error =
      LanguageError::ZoneHandle(reader->isolate(), NEW_OBJECT(LanguageError));
  reader->AddBackRef(object_id, &language_error, kIsDeserialized);

  // Set the object tags.
  language_error.set_tags(tags);

  // Set all the object fields.
  // TODO(5411462): Need to assert No GC can happen here, even though
  // allocations may happen.
  intptr_t num_flds =
      (language_error.raw()->to() - language_error.raw()->from());
  for (intptr_t i = 0; i <= num_flds; i++) {
    (*reader->ObjectHandle()) = reader->ReadObjectRef();
    language_error.StorePointer((language_error.raw()->from() + i),
                                reader->ObjectHandle()->raw());
  }

  return language_error.raw();
}


void RawLanguageError::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteVMIsolateObject(kLanguageErrorCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the object pointer fields.
  SnapshotWriterVisitor visitor(writer);
  visitor.VisitPointers(from(), to());
}


RawUnhandledException* UnhandledException::ReadFrom(SnapshotReader* reader,
                                                    intptr_t object_id,
                                                    intptr_t tags,
                                                    Snapshot::Kind kind) {
  UNREACHABLE();
  return UnhandledException::null();
}


void RawUnhandledException::WriteTo(SnapshotWriter* writer,
                                    intptr_t object_id,
                                    Snapshot::Kind kind) {
  UNREACHABLE();
}


RawUnwindError* UnwindError::ReadFrom(SnapshotReader* reader,
                                      intptr_t object_id,
                                      intptr_t tags,
                                      Snapshot::Kind kind) {
  UNREACHABLE();
  return UnwindError::null();
}


void RawUnwindError::WriteTo(SnapshotWriter* writer,
                             intptr_t object_id,
                             Snapshot::Kind kind) {
  UNREACHABLE();
}


RawInstance* Instance::ReadFrom(SnapshotReader* reader,
                                intptr_t object_id,
                                intptr_t tags,
                                Snapshot::Kind kind) {
  UNREACHABLE();
  return Instance::null();
}


void RawInstance::WriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind) {
  UNREACHABLE();
}


RawMint* Mint::ReadFrom(SnapshotReader* reader,
                        intptr_t object_id,
                        intptr_t tags,
                        Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Read the 64 bit value for the object.
  int64_t value = reader->Read<int64_t>();

  // Create a Mint object or get canonical one if it is a canonical constant.
  Mint& mint = Mint::ZoneHandle(reader->isolate(), Mint::null());
  if (kind == Snapshot::kFull) {
    mint = reader->NewMint(value);
  } else {
    // When reading a script snapshot we need to canonicalize only those object
    // references that are objects from the core library (loaded from a
    // full snapshot). Objects that are only in the script need not be
    // canonicalized as they are already canonical.
    // When reading a message snapshot we always have to canonicalize.
    if (RawObject::IsCanonical(tags) &&
        (RawObject::IsCreatedFromSnapshot(tags) ||
         (kind == Snapshot::kMessage))) {
      mint = Mint::NewCanonical(value);
    } else {
      mint = Mint::New(value, HEAP_SPACE(kind));
    }
  }
  reader->AddBackRef(object_id, &mint, kIsDeserialized);

  // Set the object tags.
  mint.set_tags(tags);

  return mint.raw();
}


void RawMint::WriteTo(SnapshotWriter* writer,
                      intptr_t object_id,
                      Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kMintCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the 64 bit value.
  writer->Write<int64_t>(ptr()->value_);
}


RawBigint* Bigint::ReadFrom(SnapshotReader* reader,
                            intptr_t object_id,
                            intptr_t tags,
                            Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Read in the HexCString representation of the bigint.
  intptr_t len = reader->ReadIntptrValue();
  char* str = Isolate::Current()->current_zone()->Alloc<char>(len + 1);
  str[len] = '\0';
  reader->ReadBytes(reinterpret_cast<uint8_t*>(str), len);

  // Create a Bigint object from HexCString.
  Bigint& obj = Bigint::ZoneHandle(
      reader->isolate(),
      ((kind == Snapshot::kFull) ? reader->NewBigint(str) :
       BigintOperations::FromHexCString(str, HEAP_SPACE(kind))));

  // If it is a canonical constant make it one.
  // When reading a full snapshot we don't need to canonicalize the object
  // as it would already be a canonical object.
  // When reading a script snapshot we need to canonicalize only those object
  // references that are objects from the core library (loaded from a
  // full snapshot). Objects that are only in the script need not be
  // canonicalized as they are already canonical.
  // When reading a message snapshot we always have to canonicalize the object.
  if ((kind != Snapshot::kFull) && RawObject::IsCanonical(tags) &&
      (RawObject::IsCreatedFromSnapshot(tags) ||
       (kind == Snapshot::kMessage))) {
    obj ^= obj.CheckAndCanonicalize(NULL);
    ASSERT(!obj.IsNull());
  }
  reader->AddBackRef(object_id, &obj, kIsDeserialized);

  // Set the object tags.
  obj.set_tags(tags);

  return obj.raw();
}


void RawBigint::WriteTo(SnapshotWriter* writer,
                        intptr_t object_id,
                        Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kBigintCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the bigint value as a HEXCstring.
  intptr_t length = ptr()->signed_length_;
  bool is_negative = false;
  if (length <= 0) {
    length = -length;
    is_negative = true;
  }
  uword data_start = reinterpret_cast<uword>(ptr()) + sizeof(RawBigint);
  const char* str = BigintOperations::ToHexCString(
      length,
      is_negative,
      reinterpret_cast<void*>(data_start),
      &BigintAllocator);
  bool neg = false;
  if (*str == '-') {
    neg = true;
    str++;
  }
  intptr_t len = strlen(str);
  ASSERT(len > 2 && str[0] == '0' && str[1] == 'x');
  if (neg) {
    writer->WriteIntptrValue(len - 1);  // Include '-' in length.
    writer->Write<uint8_t>('-');
  } else {
    writer->WriteIntptrValue(len - 2);
  }
  writer->WriteBytes(reinterpret_cast<const uint8_t*>(&(str[2])), (len - 2));
}


RawDouble* Double::ReadFrom(SnapshotReader* reader,
                            intptr_t object_id,
                            intptr_t tags,
                            Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  // Read the double value for the object.
  double value = reader->Read<double>();

  // Create a Double object or get canonical one if it is a canonical constant.
  Double& dbl = Double::ZoneHandle(reader->isolate(), Double::null());
  if (kind == Snapshot::kFull) {
    dbl = reader->NewDouble(value);
  } else {
    // When reading a script snapshot we need to canonicalize only those object
    // references that are objects from the core library (loaded from a
    // full snapshot). Objects that are only in the script need not be
    // canonicalized as they are already canonical.
    // When reading a message snapshot we always have to canonicalize.
    if (RawObject::IsCanonical(tags) &&
        (RawObject::IsCreatedFromSnapshot(tags) ||
         (kind == Snapshot::kMessage))) {
      dbl = Double::NewCanonical(value);
    } else {
      dbl = Double::New(value, HEAP_SPACE(kind));
    }
  }
  reader->AddBackRef(object_id, &dbl, kIsDeserialized);

  // Set the object tags.
  dbl.set_tags(tags);

  return dbl.raw();
}


void RawDouble::WriteTo(SnapshotWriter* writer,
                        intptr_t object_id,
                        Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kDoubleCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the double value.
  writer->Write<double>(ptr()->value_);
}


RawString* String::ReadFrom(SnapshotReader* reader,
                            intptr_t object_id,
                            intptr_t tags,
                            Snapshot::Kind kind) {
  UNREACHABLE();  // String is an abstract class.
  return String::null();
}


void RawString::WriteTo(SnapshotWriter* writer,
                        intptr_t object_id,
                        Snapshot::Kind kind) {
  UNREACHABLE();  // String is an abstract class.
}


template<typename StringType, typename CharacterType, typename CallbackType>
void String::ReadFromImpl(SnapshotReader* reader,
                          String* str_obj,
                          intptr_t len,
                          intptr_t tags,
                          CallbackType new_symbol,
                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  if (RawObject::IsCanonical(tags)) {
    // Set up canonical string object.
    ASSERT(reader != NULL);
    CharacterType* ptr =
        Isolate::Current()->current_zone()->Alloc<CharacterType>(len);
    for (intptr_t i = 0; i < len; i++) {
      ptr[i] = reader->Read<CharacterType>();
    }
    *str_obj ^= (*new_symbol)(ptr, len);
  } else {
    // Set up the string object.
    *str_obj = StringType::New(len, HEAP_SPACE(kind));
    str_obj->set_tags(tags);
    str_obj->SetHash(0);  // Will get computed when needed.
    for (intptr_t i = 0; i < len; i++) {
      *StringType::CharAddr(*str_obj, i) = reader->Read<CharacterType>();
    }
  }
}


RawOneByteString* OneByteString::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  // Read the length so that we can determine instance size to allocate.
  ASSERT(reader != NULL);
  intptr_t len = reader->ReadSmiValue();
  intptr_t hash = reader->ReadSmiValue();
  String& str_obj = String::Handle(reader->isolate(), String::null());

  if (kind == Snapshot::kFull) {
    ASSERT(reader->isolate()->no_gc_scope_depth() != 0);
    RawOneByteString* obj = reader->NewOneByteString(len);
    str_obj = obj;
    str_obj.set_tags(tags);
    obj->ptr()->hash_ = Smi::New(hash);
    if (len > 0) {
      uint8_t* raw_ptr = CharAddr(str_obj, 0);
      reader->ReadBytes(raw_ptr, len);
    }
    ASSERT((hash == 0) || (String::Hash(str_obj, 0, str_obj.Length()) == hash));
  } else {
    String::ReadFromImpl<OneByteString, uint8_t>(
        reader, &str_obj, len, tags, Symbols::FromLatin1, kind);
  }
  reader->AddBackRef(object_id, &str_obj, kIsDeserialized);
  return raw(str_obj);
}


RawTwoByteString* TwoByteString::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  // Read the length so that we can determine instance size to allocate.
  ASSERT(reader != NULL);
  intptr_t len = reader->ReadSmiValue();
  intptr_t hash = reader->ReadSmiValue();
  String& str_obj = String::Handle(reader->isolate(), String::null());

  if (kind == Snapshot::kFull) {
    RawTwoByteString* obj = reader->NewTwoByteString(len);
    str_obj = obj;
    str_obj.set_tags(tags);
    obj->ptr()->hash_ = Smi::New(hash);
    uint16_t* raw_ptr = (len > 0)? CharAddr(str_obj, 0) : NULL;
    for (intptr_t i = 0; i < len; i++) {
      ASSERT(CharAddr(str_obj, i) == raw_ptr);  // Will trigger assertions.
      *raw_ptr = reader->Read<uint16_t>();
      raw_ptr += 1;
    }
    ASSERT(String::Hash(str_obj, 0, str_obj.Length()) == hash);
  } else {
    String::ReadFromImpl<TwoByteString, uint16_t>(
        reader, &str_obj, len, tags, Symbols::FromUTF16, kind);
  }
  reader->AddBackRef(object_id, &str_obj, kIsDeserialized);
  return raw(str_obj);
}


template<typename T>
static void StringWriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind,
                          intptr_t class_id,
                          intptr_t tags,
                          RawSmi* length,
                          RawSmi* hash,
                          T* data) {
  ASSERT(writer != NULL);
  intptr_t len = Smi::Value(length);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(class_id);
  writer->WriteIntptrValue(tags);

  // Write out the length field.
  writer->Write<RawObject*>(length);

  // Write out the hash field.
  writer->Write<RawObject*>(hash);

  // Write out the string.
  if (len > 0) {
    if (class_id == kOneByteStringCid) {
      writer->WriteBytes(reinterpret_cast<const uint8_t*>(data), len);
    } else {
      for (intptr_t i = 0; i < len; i++) {
        writer->Write(data[i]);
      }
    }
  }
}


void RawOneByteString::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  StringWriteTo(writer,
                object_id,
                kind,
                kOneByteStringCid,
                writer->GetObjectTags(this),
                ptr()->length_,
                ptr()->hash_,
                ptr()->data_);
}


void RawTwoByteString::WriteTo(SnapshotWriter* writer,
                               intptr_t object_id,
                               Snapshot::Kind kind) {
  StringWriteTo(writer,
                object_id,
                kind,
                kTwoByteStringCid,
                writer->GetObjectTags(this),
                ptr()->length_,
                ptr()->hash_,
                ptr()->data_);
}


RawExternalOneByteString* ExternalOneByteString::ReadFrom(
    SnapshotReader* reader,
    intptr_t object_id,
    intptr_t tags,
    Snapshot::Kind kind) {
  UNREACHABLE();
  return ExternalOneByteString::null();
}


RawExternalTwoByteString* ExternalTwoByteString::ReadFrom(
    SnapshotReader* reader,
    intptr_t object_id,
    intptr_t tags,
    Snapshot::Kind kind) {
  UNREACHABLE();
  return ExternalTwoByteString::null();
}


void RawExternalOneByteString::WriteTo(SnapshotWriter* writer,
                                       intptr_t object_id,
                                       Snapshot::Kind kind) {
  // Serialize as a non-external one byte string.
  StringWriteTo(writer,
                object_id,
                kind,
                kOneByteStringCid,
                writer->GetObjectTags(this),
                ptr()->length_,
                ptr()->hash_,
                ptr()->external_data_->data());
}


void RawExternalTwoByteString::WriteTo(SnapshotWriter* writer,
                                       intptr_t object_id,
                                       Snapshot::Kind kind) {
  // Serialize as a non-external two byte string.
  StringWriteTo(writer,
                object_id,
                kind,
                kTwoByteStringCid,
                writer->GetObjectTags(this),
                ptr()->length_,
                ptr()->hash_,
                ptr()->external_data_->data());
}


RawBool* Bool::ReadFrom(SnapshotReader* reader,
                        intptr_t object_id,
                        intptr_t tags,
                        Snapshot::Kind kind) {
  UNREACHABLE();
  return Bool::null();
}


void RawBool::WriteTo(SnapshotWriter* writer,
                      intptr_t object_id,
                      Snapshot::Kind kind) {
  UNREACHABLE();
}


RawArray* Array::ReadFrom(SnapshotReader* reader,
                          intptr_t object_id,
                          intptr_t tags,
                          Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Read the length so that we can determine instance size to allocate.
  intptr_t len = reader->ReadSmiValue();
  Array* array = reinterpret_cast<Array*>(
      reader->GetBackRef(object_id));
  if (array == NULL) {
    array = &(Array::ZoneHandle(reader->isolate(),
                                NEW_OBJECT_WITH_LEN_SPACE(Array, len, kind)));
    reader->AddBackRef(object_id, array, kIsDeserialized);
  }
  reader->ArrayReadFrom(*array, len, tags);
  return array->raw();
}


RawImmutableArray* ImmutableArray::ReadFrom(SnapshotReader* reader,
                                            intptr_t object_id,
                                            intptr_t tags,
                                            Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Read the length so that we can determine instance size to allocate.
  intptr_t len = reader->ReadSmiValue();
  Array* array = reinterpret_cast<Array*>(reader->GetBackRef(object_id));
  if (array == NULL) {
    array = &(Array::ZoneHandle(
        reader->isolate(),
        NEW_OBJECT_WITH_LEN_SPACE(ImmutableArray, len, kind)));
    reader->AddBackRef(object_id, array, kIsDeserialized);
  }
  reader->ArrayReadFrom(*array, len, tags);
  return raw(*array);
}


void RawArray::WriteTo(SnapshotWriter* writer,
                       intptr_t object_id,
                       Snapshot::Kind kind) {
  writer->ArrayWriteTo(object_id,
                       kArrayCid,
                       writer->GetObjectTags(this),
                       ptr()->length_,
                       ptr()->type_arguments_,
                       ptr()->data());
}


void RawImmutableArray::WriteTo(SnapshotWriter* writer,
                                intptr_t object_id,
                                Snapshot::Kind kind) {
  writer->ArrayWriteTo(object_id,
                       kImmutableArrayCid,
                       writer->GetObjectTags(this),
                       ptr()->length_,
                       ptr()->type_arguments_,
                       ptr()->data());
}


RawGrowableObjectArray* GrowableObjectArray::ReadFrom(SnapshotReader* reader,
                                                      intptr_t object_id,
                                                      intptr_t tags,
                                                      Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Read the length so that we can determine instance size to allocate.
  GrowableObjectArray& array = GrowableObjectArray::ZoneHandle(
      reader->isolate(), GrowableObjectArray::null());
  if (kind == Snapshot::kFull) {
    array = reader->NewGrowableObjectArray();
  } else {
    array = GrowableObjectArray::New(0, HEAP_SPACE(kind));
  }
  reader->AddBackRef(object_id, &array, kIsDeserialized);
  intptr_t length = reader->ReadSmiValue();
  array.SetLength(length);
  Array& contents = Array::Handle();
  contents ^= reader->ReadObjectImpl();
  array.SetData(contents);
  const AbstractTypeArguments& type_arguments =
      AbstractTypeArguments::Handle(contents.GetTypeArguments());
  array.SetTypeArguments(type_arguments);
  return array.raw();
}


void RawGrowableObjectArray::WriteTo(SnapshotWriter* writer,
                                     intptr_t object_id,
                                     Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kGrowableObjectArrayCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the used length field.
  writer->Write<RawObject*>(ptr()->length_);

  // Write out the Array object.
  writer->WriteObjectImpl(ptr()->data_);
}


RawFloat32x4* Float32x4::ReadFrom(SnapshotReader* reader,
                                            intptr_t object_id,
                                            intptr_t tags,
                                            Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  // Read the values.
  float value0 = reader->Read<float>();
  float value1 = reader->Read<float>();
  float value2 = reader->Read<float>();
  float value3 = reader->Read<float>();

  // Create a Float32x4 object.
  Float32x4& simd = Float32x4::ZoneHandle(reader->isolate(),
                                          Float32x4::null());
  if (kind == Snapshot::kFull) {
    simd = reader->NewFloat32x4(value0, value1, value2, value3);
  } else {
    simd = Float32x4::New(value0, value1, value2, value3, HEAP_SPACE(kind));
  }
  reader->AddBackRef(object_id, &simd, kIsDeserialized);
  // Set the object tags.
  simd.set_tags(tags);
  return simd.raw();
}


void RawFloat32x4::WriteTo(SnapshotWriter* writer,
                                intptr_t object_id,
                                Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kFloat32x4Cid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the float values.
  writer->Write<float>(ptr()->value_[0]);
  writer->Write<float>(ptr()->value_[1]);
  writer->Write<float>(ptr()->value_[2]);
  writer->Write<float>(ptr()->value_[3]);
}


RawInt32x4* Int32x4::ReadFrom(SnapshotReader* reader,
                                      intptr_t object_id,
                                      intptr_t tags,
                                      Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  // Read the values.
  uint32_t value0 = reader->Read<uint32_t>();
  uint32_t value1 = reader->Read<uint32_t>();
  uint32_t value2 = reader->Read<uint32_t>();
  uint32_t value3 = reader->Read<uint32_t>();

  // Create a Float32x4 object.
  Int32x4& simd = Int32x4::ZoneHandle(reader->isolate(), Int32x4::null());

  if (kind == Snapshot::kFull) {
    simd = reader->NewInt32x4(value0, value1, value2, value3);
  } else {
    simd = Int32x4::New(value0, value1, value2, value3, HEAP_SPACE(kind));
  }
  reader->AddBackRef(object_id, &simd, kIsDeserialized);
  // Set the object tags.
  simd.set_tags(tags);
  return simd.raw();
}


void RawInt32x4::WriteTo(SnapshotWriter* writer,
                             intptr_t object_id,
                             Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kInt32x4Cid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the mask values.
  writer->Write<uint32_t>(ptr()->value_[0]);
  writer->Write<uint32_t>(ptr()->value_[1]);
  writer->Write<uint32_t>(ptr()->value_[2]);
  writer->Write<uint32_t>(ptr()->value_[3]);
}


#define TYPED_DATA_READ(setter, type)                                          \
  for (intptr_t i = 0; i < lengthInBytes; i += element_size) {                 \
    result.Set##setter(i, reader->Read<type>());                               \
  }                                                                            \


RawTypedData* TypedData::ReadFrom(SnapshotReader* reader,
                                  intptr_t object_id,
                                  intptr_t tags,
                                  Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  intptr_t cid = RawObject::ClassIdTag::decode(tags);
  intptr_t len = reader->ReadSmiValue();
  TypedData& result = TypedData::ZoneHandle(
      reader->isolate(), TypedData::New(cid, len, HEAP_SPACE(kind)));
  reader->AddBackRef(object_id, &result, kIsDeserialized);

  // Set the object tags.
  result.set_tags(tags);

  // Setup the array elements.
  intptr_t element_size = ElementSizeInBytes(cid);
  intptr_t lengthInBytes = len * element_size;
  switch (cid) {
    case kTypedDataInt8ArrayCid:
      TYPED_DATA_READ(Int8, int8_t);
      break;
    case kTypedDataUint8ArrayCid:
      TYPED_DATA_READ(Uint8, uint8_t);
      break;
    case kTypedDataUint8ClampedArrayCid:
      TYPED_DATA_READ(Uint8, uint8_t);
      break;
    case kTypedDataInt16ArrayCid:
      TYPED_DATA_READ(Int16, int16_t);
      break;
    case kTypedDataUint16ArrayCid:
      TYPED_DATA_READ(Uint16, uint16_t);
      break;
    case kTypedDataInt32ArrayCid:
      TYPED_DATA_READ(Int32, int32_t);
      break;
    case kTypedDataUint32ArrayCid:
      TYPED_DATA_READ(Uint32, uint32_t);
      break;
    case kTypedDataInt64ArrayCid:
      TYPED_DATA_READ(Int64, int64_t);
      break;
    case kTypedDataUint64ArrayCid:
      TYPED_DATA_READ(Uint64, uint64_t);
      break;
    case kTypedDataFloat32ArrayCid:
      TYPED_DATA_READ(Float32, float);
      break;
    case kTypedDataFloat64ArrayCid:
      TYPED_DATA_READ(Float64, double);
      break;
    default:
      UNREACHABLE();
  }
  return result.raw();
}
#undef TYPED_DATA_READ


RawExternalTypedData* ExternalTypedData::ReadFrom(SnapshotReader* reader,
                                                  intptr_t object_id,
                                                  intptr_t tags,
                                                  Snapshot::Kind kind) {
  ASSERT(kind != Snapshot::kFull);
  intptr_t cid = RawObject::ClassIdTag::decode(tags);
  intptr_t length = reader->ReadSmiValue();
  uint8_t* data = reinterpret_cast<uint8_t*>(reader->ReadIntptrValue());
  const ExternalTypedData& obj = ExternalTypedData::Handle(
      ExternalTypedData::New(cid, data, length));
  void* peer = reinterpret_cast<void*>(reader->ReadIntptrValue());
  Dart_WeakPersistentHandleFinalizer callback =
      reinterpret_cast<Dart_WeakPersistentHandleFinalizer>(
          reader->ReadIntptrValue());
  obj.AddFinalizer(peer, callback);
  return obj.raw();
}


#define TYPED_DATA_WRITE(type)                                                 \
  {                                                                            \
    type* data = reinterpret_cast<type*>(ptr()->data_);                        \
    for (intptr_t i = 0; i < len; i++) {                                       \
      writer->Write(data[i]);                                                  \
    }                                                                          \
  }                                                                            \


void RawTypedData::WriteTo(SnapshotWriter* writer,
                           intptr_t object_id,
                           Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  intptr_t tags = writer->GetObjectTags(this);
  intptr_t cid = ClassIdTag::decode(tags);
  intptr_t len = Smi::Value(ptr()->length_);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(cid);
  writer->WriteIntptrValue(tags);

  // Write out the length field.
  writer->Write<RawObject*>(ptr()->length_);

  // Write out the array elements.
  switch (cid) {
    case kTypedDataInt8ArrayCid:
      TYPED_DATA_WRITE(int8_t);
      break;
    case kTypedDataUint8ArrayCid:
      TYPED_DATA_WRITE(uint8_t);
      break;
    case kTypedDataUint8ClampedArrayCid:
      TYPED_DATA_WRITE(uint8_t);
      break;
    case kTypedDataInt16ArrayCid:
      TYPED_DATA_WRITE(int16_t);
      break;
    case kTypedDataUint16ArrayCid:
      TYPED_DATA_WRITE(uint16_t);
      break;
    case kTypedDataInt32ArrayCid:
      TYPED_DATA_WRITE(int32_t);
      break;
    case kTypedDataUint32ArrayCid:
      TYPED_DATA_WRITE(uint32_t);
      break;
    case kTypedDataInt64ArrayCid:
      TYPED_DATA_WRITE(int64_t);
      break;
    case kTypedDataUint64ArrayCid:
      TYPED_DATA_WRITE(uint64_t);
      break;
    case kTypedDataFloat32ArrayCid:
      TYPED_DATA_WRITE(float);  // NOLINT.
      break;
    case kTypedDataFloat64ArrayCid:
      TYPED_DATA_WRITE(double);  // NOLINT.
      break;
    default:
      UNREACHABLE();
  }
}


#define EXT_TYPED_DATA_WRITE(cid, type)                                        \
  writer->WriteIndexedObject(cid);                                             \
  writer->WriteIntptrValue(RawObject::ClassIdTag::update(cid, tags));          \
  writer->Write<RawObject*>(ptr()->length_);                                   \
  TYPED_DATA_WRITE(type)                                                       \


void RawExternalTypedData::WriteTo(SnapshotWriter* writer,
                                   intptr_t object_id,
                                   Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  intptr_t tags = writer->GetObjectTags(this);
  intptr_t cid = ClassIdTag::decode(tags);
  intptr_t len = Smi::Value(ptr()->length_);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  switch (cid) {
    case kExternalTypedDataInt8ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataInt8ArrayCid, int8_t);
      break;
    case kExternalTypedDataUint8ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataUint8ArrayCid, uint8_t);
      break;
    case kExternalTypedDataUint8ClampedArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataUint8ClampedArrayCid, uint8_t);
      break;
    case kExternalTypedDataInt16ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataInt16ArrayCid, int16_t);
      break;
    case kExternalTypedDataUint16ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataUint16ArrayCid, uint16_t);
      break;
    case kExternalTypedDataInt32ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataInt32ArrayCid, int32_t);
      break;
    case kExternalTypedDataUint32ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataUint32ArrayCid, uint32_t);
      break;
    case kExternalTypedDataInt64ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataInt64ArrayCid, int64_t);
      break;
    case kExternalTypedDataUint64ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataUint64ArrayCid, uint64_t);
      break;
    case kExternalTypedDataFloat32ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataFloat32ArrayCid, float);  // NOLINT.
      break;
    case kExternalTypedDataFloat64ArrayCid:
      EXT_TYPED_DATA_WRITE(kTypedDataFloat64ArrayCid, double);  // NOLINT.
      break;
    default:
      UNREACHABLE();
  }
}
#undef TYPED_DATA_WRITE
#undef EXT_TYPED_DATA_WRITE


RawStacktrace* Stacktrace::ReadFrom(SnapshotReader* reader,
                                    intptr_t object_id,
                                    intptr_t tags,
                                    Snapshot::Kind kind) {
  if (kind == Snapshot::kFull) {
    Stacktrace& result = Stacktrace::ZoneHandle(reader->isolate(),
                                                reader->NewStacktrace());
    reader->AddBackRef(object_id, &result, kIsDeserialized);

    // There are no non object pointer fields.

    // Read all the object pointer fields.
    Array& array = Array::Handle(reader->isolate());
    array ^= reader->ReadObjectRef();
    result.set_code_array(array);
    array ^= reader->ReadObjectRef();
    result.set_pc_offset_array(array);

    array ^= reader->ReadObjectRef();
    result.set_catch_code_array(array);
    array ^= reader->ReadObjectRef();
    result.set_catch_pc_offset_array(array);

    return result.raw();
  }
  UNREACHABLE();  // Stacktraces are not sent in a snapshot.
  return Stacktrace::null();
}


void RawStacktrace::WriteTo(SnapshotWriter* writer,
                            intptr_t object_id,
                            Snapshot::Kind kind) {
  if (kind == Snapshot::kFull) {
    ASSERT(writer != NULL);
    ASSERT(this == Isolate::Current()->object_store()->
           preallocated_stack_trace());

    // Write out the serialization header value for this object.
    writer->WriteInlinedObjectHeader(object_id);

    // Write out the class and tags information.
    writer->WriteIndexedObject(kStacktraceCid);
    writer->WriteIntptrValue(writer->GetObjectTags(this));

    // There are no non object pointer fields.

    // Write out all the object pointer fields.
    SnapshotWriterVisitor visitor(writer);
    visitor.VisitPointers(from(), to());
  } else {
    // Stacktraces are not allowed in other snapshot forms.
    writer->SetWriteException(Exceptions::kArgument,
                              "Illegal argument in isolate message"
                              " : (object is a stacktrace)");
  }
}


RawJSRegExp* JSRegExp::ReadFrom(SnapshotReader* reader,
                                intptr_t object_id,
                                intptr_t tags,
                                Snapshot::Kind kind) {
  ASSERT(reader != NULL);
  ASSERT(kind == Snapshot::kMessage);

  // Read the length so that we can determine instance size to allocate.
  intptr_t len = reader->ReadSmiValue();

  // Allocate JSRegExp object.
  JSRegExp& regex = JSRegExp::ZoneHandle(
      reader->isolate(), JSRegExp::New(len, HEAP_SPACE(kind)));
  reader->AddBackRef(object_id, &regex, kIsDeserialized);

  // Set the object tags.
  regex.set_tags(tags);

  // Read and Set all the other fields.
  regex.raw_ptr()->num_bracket_expressions_ = reader->ReadAsSmi();
  *reader->StringHandle() ^= reader->ReadObjectImpl();
  regex.set_pattern(*reader->StringHandle());
  regex.raw_ptr()->type_ = reader->ReadIntptrValue();
  regex.raw_ptr()->flags_ = reader->ReadIntptrValue();

  // TODO(5411462): Need to implement a way of recompiling the regex.

  return regex.raw();
}


void RawJSRegExp::WriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind) {
  ASSERT(writer != NULL);
  ASSERT(kind == Snapshot::kMessage);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kJSRegExpCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out the data length field.
  writer->Write<RawObject*>(ptr()->data_length_);

  // Write out all the other fields.
  writer->Write<RawObject*>(ptr()->num_bracket_expressions_);
  writer->WriteObjectImpl(ptr()->pattern_);
  writer->WriteIntptrValue(ptr()->type_);
  writer->WriteIntptrValue(ptr()->flags_);

  // Do not write out the data part which is native.
}


RawWeakProperty* WeakProperty::ReadFrom(SnapshotReader* reader,
                                        intptr_t object_id,
                                        intptr_t tags,
                                        Snapshot::Kind kind) {
  ASSERT(reader != NULL);

  // Allocate the weak property object.
  WeakProperty& weak_property = WeakProperty::ZoneHandle(
      reader->isolate(), WeakProperty::New(HEAP_SPACE(kind)));
  reader->AddBackRef(object_id, &weak_property, kIsDeserialized);

  // Set the object tags.
  weak_property.set_tags(tags);

  // Set all the object fields.
  weak_property.raw_ptr()->key_ = reader->ReadObjectRef();
  weak_property.raw_ptr()->value_ = reader->ReadObjectRef();

  return weak_property.raw();
}


void RawWeakProperty::WriteTo(SnapshotWriter* writer,
                          intptr_t object_id,
                          Snapshot::Kind kind) {
  ASSERT(writer != NULL);

  // Write out the serialization header value for this object.
  writer->WriteInlinedObjectHeader(object_id);

  // Write out the class and tags information.
  writer->WriteIndexedObject(kWeakPropertyCid);
  writer->WriteIntptrValue(writer->GetObjectTags(this));

  // Write out all the other fields.
  writer->Write<RawObject*>(ptr()->key_);
  writer->Write<RawObject*>(ptr()->value_);
}


RawMirrorReference* MirrorReference::ReadFrom(SnapshotReader* reader,
                                          intptr_t object_id,
                                          intptr_t tags,
                                          Snapshot::Kind kind) {
  UNREACHABLE();
  return MirrorReference::null();
}


void RawMirrorReference::WriteTo(SnapshotWriter* writer,
                                 intptr_t object_id,
                                 Snapshot::Kind kind) {
  if (kind == Snapshot::kMessage) {
    // We do not allow objects with native fields in an isolate message.
    writer->SetWriteException(Exceptions::kArgument,
                              "Illegal argument in isolate message"
                              " : (object is a MirrorReference)");
  } else {
    UNREACHABLE();
  }
}

}  // namespace dart
