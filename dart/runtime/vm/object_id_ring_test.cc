// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// TODO(zra): Remove when tests are ready to enable.
#include "platform/globals.h"
#if !defined(TARGET_ARCH_ARM64)

#include "platform/assert.h"
#include "vm/globals.h"
#include "vm/object_id_ring.h"
#include "vm/unit_test.h"
#include "vm/dart_api_impl.h"
#include "vm/dart_api_state.h"

namespace dart {


class ObjectIdRingTestHelper {
 public:
  static void SetCapacityAndMaxSerial(ObjectIdRing* ring, int32_t capacity,
                                      int32_t max_serial) {
    ring->SetCapacityAndMaxSerial(capacity, max_serial);
  }

  static void ExpectIdIsValid(ObjectIdRing* ring, intptr_t id) {
    EXPECT(ring->IsValidId(id));
  }

  static void ExpectIdIsInvalid(ObjectIdRing* ring, intptr_t id) {
    EXPECT(!ring->IsValidId(id));
  }

  static RawObject* MakeString(const char* s) {
    return String::New(s);
  }

  static void ExpectString(RawObject* obj, const char* s) {
    String& str = String::Handle();
    str ^= obj;
    EXPECT(str.Equals(s));
  }
};


// Test that serial number wrapping works.
TEST_CASE(ObjectIdRingSerialWrapTest) {
  Isolate* isolate = Isolate::Current();
  ObjectIdRing* ring = isolate->object_id_ring();
  ObjectIdRingTestHelper::SetCapacityAndMaxSerial(ring, 2, 4);
  intptr_t id;
  id = ring->GetIdForObject(ObjectIdRingTestHelper::MakeString("0"));
  EXPECT_EQ(0, id);
  id = ring->GetIdForObject(ObjectIdRingTestHelper::MakeString("1"));
  EXPECT_EQ(1, id);
  // Test that id 1 gives us the "1" string.
  ObjectIdRingTestHelper::ExpectString(ring->GetObjectForId(id), "1");
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 0);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 1);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 2);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 3);
  id = ring->GetIdForObject(ObjectIdRingTestHelper::MakeString("2"));
  EXPECT_EQ(2, id);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 0);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 1);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 2);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 3);
  id = ring->GetIdForObject(ObjectIdRingTestHelper::MakeString("3"));
  EXPECT_EQ(3, id);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 0);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 1);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 2);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 3);
  id = ring->GetIdForObject(ObjectIdRingTestHelper::MakeString("4"));
  EXPECT_EQ(0, id);
  ObjectIdRingTestHelper::ExpectString(ring->GetObjectForId(id), "4");
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 0);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 1);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 2);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 3);
  id = ring->GetIdForObject(ObjectIdRingTestHelper::MakeString("5"));
  EXPECT_EQ(1, id);
  ObjectIdRingTestHelper::ExpectString(ring->GetObjectForId(id), "5");
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 0);
  ObjectIdRingTestHelper::ExpectIdIsValid(ring, 1);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 2);
  ObjectIdRingTestHelper::ExpectIdIsInvalid(ring, 3);
}


// Test that the ring table is updated when the scavenger moves an object.
TEST_CASE(ObjectIdRingScavengeMoveTest) {
  const char* kScriptChars =
  "main() {\n"
  "  return [1, 2, 3];\n"
  "}\n";
  Dart_Handle lib = TestCase::LoadTestScript(kScriptChars, NULL);
  Dart_Handle result = Dart_Invoke(lib, NewString("main"), 0, NULL);
  intptr_t list_length = 0;
  EXPECT_VALID(result);
  EXPECT(!Dart_IsNull(result));
  EXPECT(Dart_IsList(result));
  EXPECT_VALID(Dart_ListLength(result, &list_length));
  EXPECT_EQ(3, list_length);
  Isolate* isolate = Isolate::Current();
  Heap* heap = isolate->heap();
  ObjectIdRing* ring = isolate->object_id_ring();
  RawObject* raw_obj = Api::UnwrapHandle(result);
  // Located in new heap.
  EXPECT(raw_obj->IsNewObject());
  EXPECT_NE(Object::null(), raw_obj);
  intptr_t raw_obj_id1 = ring->GetIdForObject(raw_obj);
  EXPECT_EQ(0, raw_obj_id1);
  intptr_t raw_obj_id2 = ring->GetIdForObject(raw_obj);
  EXPECT_EQ(1, raw_obj_id2);
  intptr_t raw_obj_id3 = ring->GetIdForObject(Object::null());
  RawObject* raw_obj1 = ring->GetObjectForId(raw_obj_id1);
  RawObject* raw_obj2 = ring->GetObjectForId(raw_obj_id2);
  RawObject* raw_obj3 = ring->GetObjectForId(raw_obj_id3);
  EXPECT_NE(Object::null(), raw_obj1);
  EXPECT_NE(Object::null(), raw_obj2);
  EXPECT_EQ(Object::null(), raw_obj3);
  EXPECT_EQ(RawObject::ToAddr(raw_obj), RawObject::ToAddr(raw_obj1));
  EXPECT_EQ(RawObject::ToAddr(raw_obj), RawObject::ToAddr(raw_obj2));
  // Force a scavenge.
  heap->CollectGarbage(Heap::kNew);
  RawObject* raw_object_moved1 = ring->GetObjectForId(raw_obj_id1);
  RawObject* raw_object_moved2 = ring->GetObjectForId(raw_obj_id2);
  RawObject* raw_object_moved3 = ring->GetObjectForId(raw_obj_id3);
  EXPECT_NE(Object::null(), raw_object_moved1);
  EXPECT_NE(Object::null(), raw_object_moved2);
  EXPECT_EQ(Object::null(), raw_object_moved3);
  EXPECT_EQ(RawObject::ToAddr(raw_object_moved1),
            RawObject::ToAddr(raw_object_moved2));
  // Test that objects have moved.
  EXPECT_NE(RawObject::ToAddr(raw_obj1), RawObject::ToAddr(raw_object_moved1));
  EXPECT_NE(RawObject::ToAddr(raw_obj2), RawObject::ToAddr(raw_object_moved2));
  // Test that we still point at the same list.
  Dart_Handle moved_handle = Api::NewHandle(isolate, raw_object_moved1);
  EXPECT_VALID(moved_handle);
  EXPECT(!Dart_IsNull(moved_handle));
  EXPECT(Dart_IsList(moved_handle));
  EXPECT_VALID(Dart_ListLength(moved_handle, &list_length));
  EXPECT_EQ(3, list_length);
}


// Test that the ring table is updated with nulls when the old GC collects.
TEST_CASE(ObjectIdRingOldGCTest) {
  Isolate* isolate = Isolate::Current();
  Heap* heap = isolate->heap();
  ObjectIdRing* ring = isolate->object_id_ring();

  intptr_t raw_obj_id1 = -1;
  intptr_t raw_obj_id2 = -1;
  {
    Dart_EnterScope();
    Dart_Handle result;
    // Create a string in the old heap.
    result = Api::NewHandle(isolate, String::New("old", Heap::kOld));
    EXPECT_VALID(result);
    intptr_t string_length = 0;
    // Inspect string.
    EXPECT(!Dart_IsNull(result));
    EXPECT(Dart_IsString(result));
    EXPECT_VALID(Dart_StringLength(result, &string_length));
    EXPECT_EQ(3, string_length);
    RawObject* raw_obj = Api::UnwrapHandle(result);
    // Verify that it is located in old heap.
    EXPECT(raw_obj->IsOldObject());
    EXPECT_NE(Object::null(), raw_obj);
    raw_obj_id1 = ring->GetIdForObject(raw_obj);
    EXPECT_EQ(0, raw_obj_id1);
    raw_obj_id2 = ring->GetIdForObject(raw_obj);
    EXPECT_EQ(1, raw_obj_id2);
    RawObject* raw_obj1 = ring->GetObjectForId(raw_obj_id1);
    RawObject* raw_obj2 = ring->GetObjectForId(raw_obj_id2);
    EXPECT_NE(Object::null(), raw_obj1);
    EXPECT_NE(Object::null(), raw_obj2);
    EXPECT_EQ(RawObject::ToAddr(raw_obj), RawObject::ToAddr(raw_obj1));
    EXPECT_EQ(RawObject::ToAddr(raw_obj), RawObject::ToAddr(raw_obj2));
    // Exit scope. Freeing result handle.
    Dart_ExitScope();
  }
  // Force a GC. No reference exist to the old string anymore. It should be
  // collected and the object id ring will now return the null object for
  // those ids.
  heap->CollectGarbage(Heap::kOld);
  RawObject* raw_object_moved1 = ring->GetObjectForId(raw_obj_id1);
  RawObject* raw_object_moved2 = ring->GetObjectForId(raw_obj_id2);
  // Objects should now be null.
  EXPECT_EQ(Object::null(), raw_object_moved1);
  EXPECT_EQ(Object::null(), raw_object_moved2);
}

}  // namespace dart

#endif  // !defined(TARGET_ARCH_ARM64)
