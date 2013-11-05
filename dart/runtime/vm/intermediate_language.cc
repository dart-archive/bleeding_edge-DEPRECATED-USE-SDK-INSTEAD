// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/intermediate_language.h"

#include "vm/bigint_operations.h"
#include "vm/bit_vector.h"
#include "vm/dart_entry.h"
#include "vm/flow_graph_allocator.h"
#include "vm/flow_graph_builder.h"
#include "vm/flow_graph_compiler.h"
#include "vm/flow_graph_optimizer.h"
#include "vm/locations.h"
#include "vm/object.h"
#include "vm/object_store.h"
#include "vm/os.h"
#include "vm/resolver.h"
#include "vm/scopes.h"
#include "vm/stub_code.h"
#include "vm/symbols.h"

#include "vm/il_printer.h"

namespace dart {

DEFINE_FLAG(bool, new_identity_spec, true,
    "Use new identity check rules for numbers.");
DEFINE_FLAG(bool, propagate_ic_data, true,
    "Propagate IC data from unoptimized to optimized IC calls.");
DECLARE_FLAG(bool, enable_type_checks);
DECLARE_FLAG(bool, eliminate_type_checks);
DECLARE_FLAG(bool, trace_optimization);
DECLARE_FLAG(bool, trace_constant_propagation);
DECLARE_FLAG(bool, throw_on_javascript_int_overflow);

Definition::Definition()
    : range_(NULL),
      type_(NULL),
      temp_index_(-1),
      ssa_temp_index_(-1),
      input_use_list_(NULL),
      env_use_list_(NULL),
      use_kind_(kValue),  // Phis and parameters rely on this default.
      constant_value_(Object::ZoneHandle(ConstantPropagator::Unknown())) {
}


ICData* Instruction::GetICData(const Array& ic_data_array) const {
  ICData& ic_data = ICData::ZoneHandle();
  // The deopt_id can be outside the range of the IC data array for
  // computations added in the optimizing compiler.
  if (!ic_data_array.IsNull() && (deopt_id_ < ic_data_array.Length())) {
    ic_data ^= ic_data_array.At(deopt_id_);
  }
  return &ic_data;
}


intptr_t Instruction::Hashcode() const {
  intptr_t result = tag();
  for (intptr_t i = 0; i < InputCount(); ++i) {
    Value* value = InputAt(i);
    intptr_t j = value->definition()->ssa_temp_index();
    result = result * 31 + j;
  }
  return result;
}


bool Instruction::Equals(Instruction* other) const {
  if (tag() != other->tag()) return false;
  for (intptr_t i = 0; i < InputCount(); ++i) {
    if (!InputAt(i)->Equals(other->InputAt(i))) return false;
  }
  return AttributesEqual(other);
}


bool Value::Equals(Value* other) const {
  return definition() == other->definition();
}


CheckClassInstr::CheckClassInstr(Value* value,
                                 intptr_t deopt_id,
                                 const ICData& unary_checks)
    : unary_checks_(unary_checks) {
  ASSERT(unary_checks.IsZoneHandle());
  // Expected useful check data.
  ASSERT(!unary_checks_.IsNull());
  ASSERT(unary_checks_.NumberOfChecks() > 0);
  ASSERT(unary_checks_.num_args_tested() == 1);
  SetInputAt(0, value);
  deopt_id_ = deopt_id;
  // Otherwise use CheckSmiInstr.
  ASSERT((unary_checks_.NumberOfChecks() != 1) ||
         (unary_checks_.GetReceiverClassIdAt(0) != kSmiCid));
}


bool CheckClassInstr::AttributesEqual(Instruction* other) const {
  CheckClassInstr* other_check = other->AsCheckClass();
  ASSERT(other_check != NULL);
  if (unary_checks().NumberOfChecks() !=
      other_check->unary_checks().NumberOfChecks()) {
    return false;
  }
  for (intptr_t i = 0; i < unary_checks().NumberOfChecks(); ++i) {
    // TODO(fschneider): Make sure ic_data are sorted to hit more cases.
    if (unary_checks().GetReceiverClassIdAt(i) !=
        other_check->unary_checks().GetReceiverClassIdAt(i)) {
      return false;
    }
  }
  return true;
}


EffectSet CheckClassInstr::Dependencies() const {
  // Externalization of strings via the API can change the class-id.
  const bool externalizable =
      unary_checks().HasReceiverClassId(kOneByteStringCid) ||
      unary_checks().HasReceiverClassId(kTwoByteStringCid);
  return externalizable ? EffectSet::Externalization() : EffectSet::None();
}


bool CheckClassInstr::IsNullCheck() const {
  if (unary_checks().NumberOfChecks() != 1) {
    return false;
  }
  CompileType* in_type = value()->Type();
  const intptr_t cid = unary_checks().GetCidAt(0);
  // Performance check: use CheckSmiInstr instead.
  ASSERT(cid != kSmiCid);
  return in_type->is_nullable() && (in_type->ToNullableCid() == cid);
}


bool GuardFieldInstr::AttributesEqual(Instruction* other) const {
  return field().raw() == other->AsGuardField()->field().raw();
}


bool AssertAssignableInstr::AttributesEqual(Instruction* other) const {
  AssertAssignableInstr* other_assert = other->AsAssertAssignable();
  ASSERT(other_assert != NULL);
  // This predicate has to be commutative for DominatorBasedCSE to work.
  // TODO(fschneider): Eliminate more asserts with subtype relation.
  return dst_type().raw() == other_assert->dst_type().raw();
}


bool StrictCompareInstr::AttributesEqual(Instruction* other) const {
  StrictCompareInstr* other_op = other->AsStrictCompare();
  ASSERT(other_op != NULL);
  return kind() == other_op->kind();
}


bool MathMinMaxInstr::AttributesEqual(Instruction* other) const {
  MathMinMaxInstr* other_op = other->AsMathMinMax();
  ASSERT(other_op != NULL);
  return (op_kind() == other_op->op_kind()) &&
      (result_cid() == other_op->result_cid());
}


bool BinarySmiOpInstr::AttributesEqual(Instruction* other) const {
  BinarySmiOpInstr* other_op = other->AsBinarySmiOp();
  ASSERT(other_op != NULL);
  return (op_kind() == other_op->op_kind()) &&
      (overflow_ == other_op->overflow_) &&
      (is_truncating_ == other_op->is_truncating_);
}


EffectSet LoadFieldInstr::Dependencies() const {
  return immutable_ ? EffectSet::None() : EffectSet::All();
}


bool LoadFieldInstr::AttributesEqual(Instruction* other) const {
  LoadFieldInstr* other_load = other->AsLoadField();
  ASSERT(other_load != NULL);
  if (field() != NULL) {
    return (other_load->field() != NULL) &&
        (field()->raw() == other_load->field()->raw());
  }
  return (other_load->field() == NULL) &&
      (offset_in_bytes() == other_load->offset_in_bytes());
}


EffectSet LoadStaticFieldInstr::Dependencies() const {
  return StaticField().is_final() ? EffectSet::None() : EffectSet::All();
}


bool LoadStaticFieldInstr::AttributesEqual(Instruction* other) const {
  LoadStaticFieldInstr* other_load = other->AsLoadStaticField();
  ASSERT(other_load != NULL);
  // Assert that the field is initialized.
  ASSERT(StaticField().value() != Object::sentinel().raw());
  ASSERT(StaticField().value() != Object::transition_sentinel().raw());
  return StaticField().raw() == other_load->StaticField().raw();
}


const Field& LoadStaticFieldInstr::StaticField() const {
  Field& field = Field::Handle();
  field ^= field_value()->BoundConstant().raw();
  return field;
}


EffectSet LoadIndexedInstr::Dependencies() const {
  return EffectSet::All();
}


bool LoadIndexedInstr::AttributesEqual(Instruction* other) const {
  LoadIndexedInstr* other_load = other->AsLoadIndexed();
  ASSERT(other_load != NULL);
  return class_id() == other_load->class_id();
}


ConstantInstr::ConstantInstr(const Object& value) : value_(value) {
  // Check that the value is not an incorrect Integer representation.
  ASSERT(!value.IsBigint() ||
         !BigintOperations::FitsIntoSmi(Bigint::Cast(value)));
  ASSERT(!value.IsBigint() ||
         !BigintOperations::FitsIntoInt64(Bigint::Cast(value)));
  ASSERT(!value.IsMint() || !Smi::IsValid64(Mint::Cast(value).AsInt64Value()));
}


bool ConstantInstr::AttributesEqual(Instruction* other) const {
  ConstantInstr* other_constant = other->AsConstant();
  ASSERT(other_constant != NULL);
  return (value().raw() == other_constant->value().raw());
}


// Returns true if the value represents a constant.
bool Value::BindsToConstant() const {
  return definition()->IsConstant();
}


// Returns true if the value represents constant null.
bool Value::BindsToConstantNull() const {
  ConstantInstr* constant = definition()->AsConstant();
  return (constant != NULL) && constant->value().IsNull();
}


const Object& Value::BoundConstant() const {
  ASSERT(BindsToConstant());
  ConstantInstr* constant = definition()->AsConstant();
  ASSERT(constant != NULL);
  return constant->value();
}


GraphEntryInstr::GraphEntryInstr(const ParsedFunction* parsed_function,
                                 TargetEntryInstr* normal_entry,
                                 intptr_t osr_id)
    : BlockEntryInstr(0, CatchClauseNode::kInvalidTryIndex),
      parsed_function_(parsed_function),
      normal_entry_(normal_entry),
      catch_entries_(),
      initial_definitions_(),
      osr_id_(osr_id),
      entry_count_(0),
      spill_slot_count_(0),
      fixed_slot_count_(0) {
}


ConstantInstr* GraphEntryInstr::constant_null() {
  ASSERT(initial_definitions_.length() > 0);
  for (intptr_t i = 0; i < initial_definitions_.length(); ++i) {
    ConstantInstr* defn = initial_definitions_[i]->AsConstant();
    if (defn != NULL && defn->value().IsNull()) return defn;
  }
  UNREACHABLE();
  return NULL;
}


CatchBlockEntryInstr* GraphEntryInstr::GetCatchEntry(intptr_t index) {
  // TODO(fschneider): Sort the catch entries by catch_try_index to avoid
  // searching.
  for (intptr_t i = 0; i < catch_entries_.length(); ++i) {
    if (catch_entries_[i]->catch_try_index() == index) return catch_entries_[i];
  }
  return NULL;
}


static bool StartsWith(const String& name, const char* prefix, intptr_t n) {
  ASSERT(name.IsOneByteString());

  if (name.Length() < n) {
    return false;
  }

  for (intptr_t i = 0; i < n; i++) {
    if (name.CharAt(i) != prefix[i]) {
      return false;
    }
  }

  return true;
}


static bool CompareNames(const Library& lib,
                         const char* test_name,
                         const String& name) {
  const char* kPrivateGetterPrefix = "get:_";
  const char* kPrivateSetterPrefix = "set:_";

  if (test_name[0] == '_') {
    if (name.CharAt(0) != '_') {
      return false;
    }
  } else if (strncmp(test_name,
                     kPrivateGetterPrefix,
                     strlen(kPrivateGetterPrefix)) == 0) {
    if (!StartsWith(name, kPrivateGetterPrefix, strlen(kPrivateGetterPrefix))) {
      return false;
    }
  } else if (strncmp(test_name,
                     kPrivateSetterPrefix,
                     strlen(kPrivateSetterPrefix)) == 0) {
    if (!StartsWith(name, kPrivateSetterPrefix, strlen(kPrivateSetterPrefix))) {
      return false;
    }
  } else {
    // Compare without mangling.
    return name.Equals(test_name);
  }

  // Both names are private. Mangle test_name before comparison.
  const String& test_name_symbol = String::Handle(Symbols::New(test_name));
  return String::Handle(lib.PrivateName(test_name_symbol)).Equals(name);
}


static bool IsRecognizedLibrary(const Library& library) {
  // List of libraries where methods can be recognized.
  return (library.raw() == Library::CoreLibrary())
      || (library.raw() == Library::MathLibrary())
      || (library.raw() == Library::TypedDataLibrary())
      || (library.raw() == Library::CollectionDevLibrary());
}


MethodRecognizer::Kind MethodRecognizer::RecognizeKind(
    const Function& function) {
  if (!function.is_recognized()) {
    return kUnknown;
  }

  const Class& function_class = Class::Handle(function.Owner());
  const Library& lib = Library::Handle(function_class.library());
  const String& function_name = String::Handle(function.name());
  const String& class_name = String::Handle(function_class.Name());

#define RECOGNIZE_FUNCTION(test_class_name, test_function_name, enum_name, fp) \
  if (CompareNames(lib, #test_function_name, function_name) &&                 \
      CompareNames(lib, #test_class_name, class_name)) {                       \
    ASSERT(function.CheckSourceFingerprint(fp));                               \
    return k##enum_name;                                                       \
  }
RECOGNIZED_LIST(RECOGNIZE_FUNCTION)
#undef RECOGNIZE_FUNCTION
  UNREACHABLE();
  return kUnknown;
}


bool MethodRecognizer::AlwaysInline(const Function& function) {
  const Class& function_class = Class::Handle(function.Owner());
  const Library& lib = Library::Handle(function_class.library());
  if (!IsRecognizedLibrary(lib)) {
    return false;
  }

  const String& function_name = String::Handle(function.name());
  const String& class_name = String::Handle(function_class.Name());

#define RECOGNIZE_FUNCTION(test_class_name, test_function_name, enum_name, fp) \
  if (CompareNames(lib, #test_function_name, function_name) &&                 \
      CompareNames(lib, #test_class_name, class_name)) {                       \
    ASSERT(function.CheckSourceFingerprint(fp));                               \
    return true;                                                               \
  }
INLINE_WHITE_LIST(RECOGNIZE_FUNCTION)
#undef RECOGNIZE_FUNCTION
  return false;
}


bool MethodRecognizer::PolymorphicTarget(const Function& function) {
  const Class& function_class = Class::Handle(function.Owner());
  const Library& lib = Library::Handle(function_class.library());
  if (!IsRecognizedLibrary(lib)) {
    return false;
  }

  const String& function_name = String::Handle(function.name());
  const String& class_name = String::Handle(function_class.Name());

#define RECOGNIZE_FUNCTION(test_class_name, test_function_name, enum_name, fp) \
  if (CompareNames(lib, #test_function_name, function_name) &&                 \
      CompareNames(lib, #test_class_name, class_name)) {                       \
    ASSERT(function.CheckSourceFingerprint(fp));                               \
    return true;                                                               \
  }
POLYMORPHIC_TARGET_LIST(RECOGNIZE_FUNCTION)
#undef RECOGNIZE_FUNCTION
  return false;
}


const char* MethodRecognizer::KindToCString(Kind kind) {
#define KIND_TO_STRING(class_name, function_name, enum_name, fp)               \
  if (kind == k##enum_name) return #enum_name;
RECOGNIZED_LIST(KIND_TO_STRING)
#undef KIND_TO_STRING
  return "?";
}


void MethodRecognizer::InitializeState() {
  GrowableArray<Library*> libs(3);
  libs.Add(&Library::ZoneHandle(Library::CoreLibrary()));
  libs.Add(&Library::ZoneHandle(Library::MathLibrary()));
  libs.Add(&Library::ZoneHandle(Library::TypedDataLibrary()));
  Function& func = Function::Handle();

#define SET_IS_RECOGNIZED(class_name, function_name, dest, fp)                 \
  func = Library::GetFunction(libs, #class_name, #function_name);              \
  ASSERT(!func.IsNull());                                                      \
  func.set_is_recognized(true);                                                \

  RECOGNIZED_LIST(SET_IS_RECOGNIZED);

#undef SET_IS_RECOGNIZED
}

// ==== Support for visiting flow graphs.

#define DEFINE_ACCEPT(ShortName)                                               \
void ShortName##Instr::Accept(FlowGraphVisitor* visitor) {                     \
  visitor->Visit##ShortName(this);                                             \
}

FOR_EACH_INSTRUCTION(DEFINE_ACCEPT)

#undef DEFINE_ACCEPT


void Instruction::SetEnvironment(Environment* deopt_env) {
  intptr_t use_index = 0;
  for (Environment::DeepIterator it(deopt_env); !it.Done(); it.Advance()) {
    Value* use = it.CurrentValue();
    use->set_instruction(this);
    use->set_use_index(use_index++);
  }
  env_ = deopt_env;
}


void Instruction::RemoveEnvironment() {
  for (Environment::DeepIterator it(env()); !it.Done(); it.Advance()) {
    it.CurrentValue()->RemoveFromUseList();
  }
  env_ = NULL;
}


Instruction* Instruction::RemoveFromGraph(bool return_previous) {
  ASSERT(!IsBlockEntry());
  ASSERT(!IsControl());
  ASSERT(!IsThrow());
  ASSERT(!IsReturn());
  ASSERT(!IsReThrow());
  ASSERT(!IsGoto());
  ASSERT(previous() != NULL);
  // We cannot assert that the instruction, if it is a definition, has no
  // uses.  This function is used to remove instructions from the graph and
  // reinsert them elsewhere (e.g., hoisting).
  Instruction* prev_instr = previous();
  Instruction* next_instr = next();
  ASSERT(next_instr != NULL);
  ASSERT(!next_instr->IsBlockEntry());
  prev_instr->LinkTo(next_instr);
  UnuseAllInputs();
  // Reset the successor and previous instruction to indicate that the
  // instruction is removed from the graph.
  set_previous(NULL);
  set_next(NULL);
  return return_previous ? prev_instr : next_instr;
}


void Instruction::InsertAfter(Instruction* prev) {
  ASSERT(previous_ == NULL);
  ASSERT(next_ == NULL);
  previous_ = prev;
  next_ = prev->next_;
  next_->previous_ = this;
  previous_->next_ = this;

  // Update def-use chains whenever instructions are added to the graph
  // after initial graph construction.
  for (intptr_t i = InputCount() - 1; i >= 0; --i) {
    Value* input = InputAt(i);
    input->definition()->AddInputUse(input);
  }
}


Instruction* Instruction::AppendInstruction(Instruction* tail) {
  LinkTo(tail);
  // Update def-use chains whenever instructions are added to the graph
  // after initial graph construction.
  for (intptr_t i = tail->InputCount() - 1; i >= 0; --i) {
    Value* input = tail->InputAt(i);
    input->definition()->AddInputUse(input);
  }
  return tail;
}


BlockEntryInstr* Instruction::GetBlock() const {
  // TODO(fschneider): Implement a faster way to get the block of an
  // instruction.
  ASSERT(previous() != NULL);
  Instruction* result = previous();
  while (!result->IsBlockEntry()) result = result->previous();
  return result->AsBlockEntry();
}


void ForwardInstructionIterator::RemoveCurrentFromGraph() {
  current_ = current_->RemoveFromGraph(true);  // Set current_ to previous.
}


// Default implementation of visiting basic blocks.  Can be overridden.
void FlowGraphVisitor::VisitBlocks() {
  ASSERT(current_iterator_ == NULL);
  for (intptr_t i = 0; i < block_order_.length(); ++i) {
    BlockEntryInstr* entry = block_order_[i];
    entry->Accept(this);
    ForwardInstructionIterator it(entry);
    current_iterator_ = &it;
    for (; !it.Done(); it.Advance()) {
      it.Current()->Accept(this);
    }
    current_iterator_ = NULL;
  }
}


bool Value::NeedsStoreBuffer() {
  if (Type()->IsNull() ||
      (Type()->ToNullableCid() == kSmiCid) ||
      (Type()->ToNullableCid() == kBoolCid)) {
    return false;
  }

  return !BindsToConstant();
}


void JoinEntryInstr::AddPredecessor(BlockEntryInstr* predecessor) {
  // Require the predecessors to be sorted by block_id to make managing
  // their corresponding phi inputs simpler.
  intptr_t pred_id = predecessor->block_id();
  intptr_t index = 0;
  while ((index < predecessors_.length()) &&
         (predecessors_[index]->block_id() < pred_id)) {
    ++index;
  }
#if defined(DEBUG)
  for (intptr_t i = index; i < predecessors_.length(); ++i) {
    ASSERT(predecessors_[i]->block_id() != pred_id);
  }
#endif
  predecessors_.InsertAt(index, predecessor);
}


intptr_t JoinEntryInstr::IndexOfPredecessor(BlockEntryInstr* pred) const {
  for (intptr_t i = 0; i < predecessors_.length(); ++i) {
    if (predecessors_[i] == pred) return i;
  }
  return -1;
}


void Value::AddToList(Value* value, Value** list) {
  Value* next = *list;
  *list = value;
  value->set_next_use(next);
  value->set_previous_use(NULL);
  if (next != NULL) next->set_previous_use(value);
}


void Value::RemoveFromUseList() {
  Definition* def = definition();
  Value* next = next_use();
  if (this == def->input_use_list()) {
    def->set_input_use_list(next);
    if (next != NULL) next->set_previous_use(NULL);
  } else if (this == def->env_use_list()) {
    def->set_env_use_list(next);
    if (next != NULL) next->set_previous_use(NULL);
  } else {
    Value* prev = previous_use();
    prev->set_next_use(next);
    if (next != NULL) next->set_previous_use(prev);
  }

  set_previous_use(NULL);
  set_next_use(NULL);
}


// True if the definition has a single input use and is used only in
// environments at the same instruction as that input use.
bool Definition::HasOnlyUse(Value* use) const {
  if ((input_use_list() != use) || (use->next_use() != NULL)) return false;

  Instruction* target = use->instruction();
  for (Value::Iterator it(env_use_list()); !it.Done(); it.Advance()) {
    if (it.Current()->instruction() != target) return false;
  }
  return true;
}


void Definition::ReplaceUsesWith(Definition* other) {
  ASSERT(other != NULL);
  ASSERT(this != other);

  Value* current = NULL;
  Value* next = input_use_list();
  if (next != NULL) {
    // Change all the definitions.
    while (next != NULL) {
      current = next;
      current->set_definition(other);
      next = current->next_use();
    }

    // Concatenate the lists.
    next = other->input_use_list();
    current->set_next_use(next);
    if (next != NULL) next->set_previous_use(current);
    other->set_input_use_list(input_use_list());
    set_input_use_list(NULL);
  }

  // Repeat for environment uses.
  current = NULL;
  next = env_use_list();
  if (next != NULL) {
    while (next != NULL) {
      current = next;
      current->set_definition(other);
      next = current->next_use();
    }
    next = other->env_use_list();
    current->set_next_use(next);
    if (next != NULL) next->set_previous_use(current);
    other->set_env_use_list(env_use_list());
    set_env_use_list(NULL);
  }
}


void Instruction::UnuseAllInputs() {
  for (intptr_t i = InputCount() - 1; i >= 0; --i) {
    InputAt(i)->RemoveFromUseList();
  }
  for (Environment::DeepIterator it(env()); !it.Done(); it.Advance()) {
    it.CurrentValue()->RemoveFromUseList();
  }
}


void Instruction::InheritDeoptTargetAfter(Instruction* other) {
  ASSERT(other->env() != NULL);
  deopt_id_ = Isolate::ToDeoptAfter(other->deopt_id_);
  other->env()->DeepCopyTo(this);
  env()->set_deopt_id(deopt_id_);
}


void Instruction::InheritDeoptTarget(Instruction* other) {
  ASSERT(other->env() != NULL);
  deopt_id_ = other->deopt_id_;
  other->env()->DeepCopyTo(this);
  env()->set_deopt_id(deopt_id_);
}


void BranchInstr::InheritDeoptTarget(Instruction* other) {
  ASSERT(env() == NULL);
  Instruction::InheritDeoptTarget(other);
  comparison()->SetDeoptId(GetDeoptId());
}


void Definition::ReplaceWith(Definition* other,
                             ForwardInstructionIterator* iterator) {
  // Record other's input uses.
  for (intptr_t i = other->InputCount() - 1; i >= 0; --i) {
    Value* input = other->InputAt(i);
    input->definition()->AddInputUse(input);
  }
  // Take other's environment from this definition.
  ASSERT(other->env() == NULL);
  other->SetEnvironment(env());
  env_ = NULL;
  // Replace all uses of this definition with other.
  ReplaceUsesWith(other);
  // Reuse this instruction's SSA name for other.
  ASSERT(!other->HasSSATemp());
  if (HasSSATemp()) other->set_ssa_temp_index(ssa_temp_index());

  // Finally insert the other definition in place of this one in the graph.
  previous()->LinkTo(other);
  if ((iterator != NULL) && (this == iterator->Current())) {
    // Remove through the iterator.
    other->LinkTo(this);
    iterator->RemoveCurrentFromGraph();
  } else {
    other->LinkTo(next());
    // Remove this definition's input uses.
    UnuseAllInputs();
  }
  set_previous(NULL);
  set_next(NULL);
}


BranchInstr::BranchInstr(ComparisonInstr* comparison, bool is_checked)
    : comparison_(comparison),
      is_checked_(is_checked),
      constrained_type_(NULL),
      constant_target_(NULL) {
  for (intptr_t i = comparison->InputCount() - 1; i >= 0; --i) {
    comparison->InputAt(i)->set_instruction(this);
  }
}


void BranchInstr::RawSetInputAt(intptr_t i, Value* value) {
  comparison()->RawSetInputAt(i, value);
}


// A misleadingly named function for use in template functions that replace
// both definitions with definitions and branch comparisons with
// comparisons.  In the branch case, leave the branch intact and replace its
// comparison with a new comparison not currently in the graph.
void BranchInstr::ReplaceWith(ComparisonInstr* other,
                              ForwardInstructionIterator* ignored) {
  SetComparison(other);
}


void BranchInstr::SetComparison(ComparisonInstr* comp) {
  for (intptr_t i = comp->InputCount() - 1; i >= 0; --i) {
    Value* input = comp->InputAt(i);
    input->definition()->AddInputUse(input);
    input->set_instruction(this);
  }
  // There should be no need to copy or unuse an environment.
  ASSERT(comparison()->env() == NULL);
  // Remove the current comparison's input uses.
  comparison()->UnuseAllInputs();
  ASSERT(!comp->HasUses());
  comparison_ = comp;
}


// ==== Postorder graph traversal.
static bool IsMarked(BlockEntryInstr* block,
                     GrowableArray<BlockEntryInstr*>* preorder) {
  // Detect that a block has been visited as part of the current
  // DiscoverBlocks (we can call DiscoverBlocks multiple times).  The block
  // will be 'marked' by (1) having a preorder number in the range of the
  // preorder array and (2) being in the preorder array at that index.
  intptr_t i = block->preorder_number();
  return (i >= 0) && (i < preorder->length()) && ((*preorder)[i] == block);
}


// Base class implementation used for JoinEntry and TargetEntry.
void BlockEntryInstr::DiscoverBlocks(
    BlockEntryInstr* predecessor,
    GrowableArray<BlockEntryInstr*>* preorder,
    GrowableArray<BlockEntryInstr*>* postorder,
    GrowableArray<intptr_t>* parent,
    intptr_t variable_count,
    intptr_t fixed_parameter_count) {
  // If this block has a predecessor (i.e., is not the graph entry) we can
  // assume the preorder array is non-empty.
  ASSERT((predecessor == NULL) || !preorder->is_empty());
  // Blocks with a single predecessor cannot have been reached before.
  ASSERT(IsJoinEntry() || !IsMarked(this, preorder));

  // 1. If the block has already been reached, add current_block as a
  // basic-block predecessor and we are done.
  if (IsMarked(this, preorder)) {
    ASSERT(predecessor != NULL);
    AddPredecessor(predecessor);
    return;
  }

  // 2. Otherwise, clear the predecessors which might have been computed on
  // some earlier call to DiscoverBlocks and record this predecessor.
  ClearPredecessors();
  if (predecessor != NULL) AddPredecessor(predecessor);

  // 3. The predecessor is the spanning-tree parent.  The graph entry has no
  // parent, indicated by -1.
  intptr_t parent_number =
      (predecessor == NULL) ? -1 : predecessor->preorder_number();
  parent->Add(parent_number);

  // 4. Assign the preorder number and add the block entry to the list.
  set_preorder_number(preorder->length());
  preorder->Add(this);

  // The preorder and parent arrays are indexed by
  // preorder block number, so they should stay in lockstep.
  ASSERT(preorder->length() == parent->length());

  // 5. Iterate straight-line successors to record assigned variables and
  // find the last instruction in the block.  The graph entry block consists
  // of only the entry instruction, so that is the last instruction in the
  // block.
  Instruction* last = this;
  for (ForwardInstructionIterator it(this); !it.Done(); it.Advance()) {
    last = it.Current();
  }
  set_last_instruction(last);

  // Visit the block's successors in reverse so that they appear forwards
  // the reverse postorder block ordering.
  for (intptr_t i = last->SuccessorCount() - 1; i >= 0; --i) {
    last->SuccessorAt(i)->DiscoverBlocks(this,
                                         preorder,
                                         postorder,
                                         parent,
                                         variable_count,
                                         fixed_parameter_count);
  }

  // 6. Assign postorder number and add the block entry to the list.
  set_postorder_number(postorder->length());
  postorder->Add(this);
}


bool BlockEntryInstr::PruneUnreachable(FlowGraphBuilder* builder,
                                       GraphEntryInstr* graph_entry,
                                       Instruction* parent,
                                       intptr_t osr_id,
                                       BitVector* block_marks) {
  // Search for the instruction with the OSR id.  Use a depth first search
  // because basic blocks have not been discovered yet.  Prune unreachable
  // blocks by replacing the normal entry with a jump to the block
  // containing the OSR entry point.

  // Do not visit blocks more than once.
  if (block_marks->Contains(block_id())) return false;
  block_marks->Add(block_id());

  // Search this block for the OSR id.
  Instruction* instr = this;
  for (ForwardInstructionIterator it(this); !it.Done(); it.Advance()) {
    instr = it.Current();
    if (instr->GetDeoptId() == osr_id) {
      // Sanity check that we found a stack check instruction.
      ASSERT(instr->IsCheckStackOverflow());
      // Loop stack check checks are always in join blocks so that they can
      // be the target of a goto.
      ASSERT(IsJoinEntry());
      // The instruction should be the first instruction in the block so
      // we can simply jump to the beginning of the block.
      ASSERT(instr->previous() == this);

      GotoInstr* goto_join = new GotoInstr(AsJoinEntry());
      goto_join->deopt_id_ = parent->deopt_id_;
      graph_entry->normal_entry()->LinkTo(goto_join);
      return true;
    }
  }

  // Recursively search the successors.
  for (intptr_t i = instr->SuccessorCount() - 1; i >= 0; --i) {
    if (instr->SuccessorAt(i)->PruneUnreachable(builder,
                                                graph_entry,
                                                instr,
                                                osr_id,
                                                block_marks)) {
      return true;
    }
  }
  return false;
}


bool BlockEntryInstr::Dominates(BlockEntryInstr* other) const {
  // TODO(fschneider): Make this faster by e.g. storing dominators for each
  // block while computing the dominator tree.
  ASSERT(other != NULL);
  BlockEntryInstr* current = other;
  while (current != NULL && current != this) {
    current = current->dominator();
  }
  return current == this;
}


// Helper to mutate the graph during inlining. This block should be
// replaced with new_block as a predecessor of all of this block's
// successors.  For each successor, the predecessors will be reordered
// to preserve block-order sorting of the predecessors as well as the
// phis if the successor is a join.
void BlockEntryInstr::ReplaceAsPredecessorWith(BlockEntryInstr* new_block) {
  // Set the last instruction of the new block to that of the old block.
  Instruction* last = last_instruction();
  new_block->set_last_instruction(last);
  // For each successor, update the predecessors.
  for (intptr_t sidx = 0; sidx < last->SuccessorCount(); ++sidx) {
    // If the successor is a target, update its predecessor.
    TargetEntryInstr* target = last->SuccessorAt(sidx)->AsTargetEntry();
    if (target != NULL) {
      target->predecessor_ = new_block;
      continue;
    }
    // If the successor is a join, update each predecessor and the phis.
    JoinEntryInstr* join = last->SuccessorAt(sidx)->AsJoinEntry();
    ASSERT(join != NULL);
    // Find the old predecessor index.
    intptr_t old_index = join->IndexOfPredecessor(this);
    intptr_t pred_count = join->PredecessorCount();
    ASSERT(old_index >= 0);
    ASSERT(old_index < pred_count);
    // Find the new predecessor index while reordering the predecessors.
    intptr_t new_id = new_block->block_id();
    intptr_t new_index = old_index;
    if (block_id() < new_id) {
      // Search upwards, bubbling down intermediate predecessors.
      for (; new_index < pred_count - 1; ++new_index) {
        if (join->predecessors_[new_index + 1]->block_id() > new_id) break;
        join->predecessors_[new_index] = join->predecessors_[new_index + 1];
      }
    } else {
      // Search downwards, bubbling up intermediate predecessors.
      for (; new_index > 0; --new_index) {
        if (join->predecessors_[new_index - 1]->block_id() < new_id) break;
        join->predecessors_[new_index] = join->predecessors_[new_index - 1];
      }
    }
    join->predecessors_[new_index] = new_block;
    // If the new and old predecessor index match there is nothing to update.
    if ((join->phis() == NULL) || (old_index == new_index)) return;
    // Otherwise, reorder the predecessor uses in each phi.
    for (PhiIterator it(join); !it.Done(); it.Advance()) {
      PhiInstr* phi = it.Current();
      ASSERT(phi != NULL);
      ASSERT(pred_count == phi->InputCount());
      // Save the predecessor use.
      Value* pred_use = phi->InputAt(old_index);
      // Move uses between old and new.
      intptr_t step = (old_index < new_index) ? 1 : -1;
      for (intptr_t use_idx = old_index;
           use_idx != new_index;
           use_idx += step) {
        phi->SetInputAt(use_idx, phi->InputAt(use_idx + step));
      }
      // Write the predecessor use.
      phi->SetInputAt(new_index, pred_use);
    }
  }
}


void JoinEntryInstr::InsertPhi(intptr_t var_index, intptr_t var_count) {
  // Lazily initialize the array of phis.
  // Currently, phis are stored in a sparse array that holds the phi
  // for variable with index i at position i.
  // TODO(fschneider): Store phis in a more compact way.
  if (phis_ == NULL) {
    phis_ = new ZoneGrowableArray<PhiInstr*>(var_count);
    for (intptr_t i = 0; i < var_count; i++) {
      phis_->Add(NULL);
    }
  }
  ASSERT((*phis_)[var_index] == NULL);
  (*phis_)[var_index] = new PhiInstr(this, PredecessorCount());
}


void JoinEntryInstr::InsertPhi(PhiInstr* phi) {
  // Lazily initialize the array of phis.
  if (phis_ == NULL) {
    phis_ = new ZoneGrowableArray<PhiInstr*>(1);
  }
  phis_->Add(phi);
}

void JoinEntryInstr::RemovePhi(PhiInstr* phi) {
  ASSERT(phis_ != NULL);
  for (intptr_t index = 0; index < phis_->length(); ++index) {
    if (phi == (*phis_)[index]) {
      (*phis_)[index] = phis_->Last();
      phis_->RemoveLast();
      return;
    }
  }
}

void JoinEntryInstr::RemoveDeadPhis(Definition* replacement) {
  if (phis_ == NULL) return;

  intptr_t to_index = 0;
  for (intptr_t from_index = 0; from_index < phis_->length(); ++from_index) {
    PhiInstr* phi = (*phis_)[from_index];
    if (phi != NULL) {
      if (phi->is_alive()) {
        (*phis_)[to_index++] = phi;
        for (intptr_t i = phi->InputCount() - 1; i >= 0; --i) {
          Value* input = phi->InputAt(i);
          input->definition()->AddInputUse(input);
        }
      } else {
        phi->ReplaceUsesWith(replacement);
      }
    }
  }
  if (to_index == 0) {
    phis_ = NULL;
  } else {
    phis_->TruncateTo(to_index);
  }
}


intptr_t Instruction::SuccessorCount() const {
  return 0;
}


BlockEntryInstr* Instruction::SuccessorAt(intptr_t index) const {
  // Called only if index is in range.  Only control-transfer instructions
  // can have non-zero successor counts and they override this function.
  UNREACHABLE();
  return NULL;
}


intptr_t GraphEntryInstr::SuccessorCount() const {
  return 1 + catch_entries_.length();
}


BlockEntryInstr* GraphEntryInstr::SuccessorAt(intptr_t index) const {
  if (index == 0) return normal_entry_;
  return catch_entries_[index - 1];
}


intptr_t ControlInstruction::SuccessorCount() const {
  return 2;
}


BlockEntryInstr* ControlInstruction::SuccessorAt(intptr_t index) const {
  if (index == 0) return true_successor_;
  if (index == 1) return false_successor_;
  UNREACHABLE();
  return NULL;
}


intptr_t GotoInstr::SuccessorCount() const {
  return 1;
}


BlockEntryInstr* GotoInstr::SuccessorAt(intptr_t index) const {
  ASSERT(index == 0);
  return successor();
}


void Instruction::Goto(JoinEntryInstr* entry) {
  LinkTo(new GotoInstr(entry));
}


bool BinarySmiOpInstr::CanDeoptimize() const {
  if (FLAG_throw_on_javascript_int_overflow && (Smi::kBits > 32)) {
    // If Smi's are bigger than 32-bits, then the instruction could deoptimize
    // if the result is too big.
    return true;
  }
  switch (op_kind()) {
    case Token::kBIT_AND:
    case Token::kBIT_OR:
    case Token::kBIT_XOR:
      return false;
    case Token::kSHR: {
      // Can't deopt if shift-count is known positive.
      Range* right_range = this->right()->definition()->range();
      return (right_range == NULL)
          || !right_range->IsWithin(0, RangeBoundary::kPlusInfinity);
    }
    case Token::kSHL: {
      Range* right_range = this->right()->definition()->range();
      if ((right_range != NULL) && is_truncating()) {
        // Can deoptimize if right can be negative.
        return !right_range->IsWithin(0, RangeBoundary::kPlusInfinity);
      }
      return true;
    }
    default:
      return overflow_;
  }
}


bool BinarySmiOpInstr::RightIsPowerOfTwoConstant() const {
  if (!right()->definition()->IsConstant()) return false;
  const Object& constant = right()->definition()->AsConstant()->value();
  if (!constant.IsSmi()) return false;
  const intptr_t int_value = Smi::Cast(constant).Value();
  return Utils::IsPowerOfTwo(Utils::Abs(int_value));
}


static bool ToIntegerConstant(Value* value, intptr_t* result) {
  if (!value->BindsToConstant()) {
    if (value->definition()->IsUnboxDouble()) {
      return ToIntegerConstant(value->definition()->AsUnboxDouble()->value(),
                               result);
    }

    return false;
  }

  const Object& constant = value->BoundConstant();
  if (constant.IsDouble()) {
    const Double& double_constant = Double::Cast(constant);
    *result = static_cast<intptr_t>(double_constant.value());
    return (static_cast<double>(*result) == double_constant.value());
  } else if (constant.IsSmi()) {
    *result = Smi::Cast(constant).Value();
    return true;
  }

  return false;
}


static Definition* CanonicalizeCommutativeArithmetic(Token::Kind op,
                                                     intptr_t cid,
                                                     Value* left,
                                                     Value* right) {
  ASSERT((cid == kSmiCid) || (cid == kDoubleCid) || (cid == kMintCid));

  intptr_t left_value;
  if (!ToIntegerConstant(left, &left_value)) {
    return NULL;
  }

  switch (op) {
    case Token::kMUL:
      if (left_value == 1) {
        if ((cid == kDoubleCid) &&
            (right->definition()->representation() != kUnboxedDouble)) {
          // Can't yet apply the equivalence because representation selection
          // did not run yet. We need it to guarantee that right value is
          // correctly coerced to double. The second canonicalization pass
          // will apply this equivalence.
          return NULL;
        } else {
          return right->definition();
        }
      } else if ((left_value == 0) && (cid != kDoubleCid)) {
        // Can't apply this equivalence to double operation because
        // 0.0 * NaN is NaN not 0.0.
        return left->definition();
      }
      break;
    case Token::kADD:
      if ((left_value == 0) && (cid != kDoubleCid)) {
        // Can't apply this equivalence to double operations because
        // 0.0 + (-0.0) is 0.0 not -0.0.
        return right->definition();
      }
      break;
    case Token::kBIT_AND:
      ASSERT(cid != kDoubleCid);
      if (left_value == 0) {
        return left->definition();
      } else if (left_value == -1) {
        return right->definition();
      }
      break;
    case Token::kBIT_OR:
      ASSERT(cid != kDoubleCid);
      if (left_value == 0) {
        return right->definition();
      } else if (left_value == -1) {
        return left->definition();
      }
      break;
    case Token::kBIT_XOR:
      ASSERT(cid != kDoubleCid);
      if (left_value == 0) {
        return right->definition();
      }
      break;
    default:
      break;
  }

  return NULL;
}


Definition* BinaryDoubleOpInstr::Canonicalize(FlowGraph* flow_graph) {
  Definition* result = NULL;

  result = CanonicalizeCommutativeArithmetic(op_kind(),
                                             kDoubleCid,
                                             left(),
                                             right());
  if (result != NULL) {
    return result;
  }

  result = CanonicalizeCommutativeArithmetic(op_kind(),
                                             kDoubleCid,
                                             right(),
                                             left());
  if (result != NULL) {
    return result;
  }

  return this;
}


Definition* BinarySmiOpInstr::Canonicalize(FlowGraph* flow_graph) {
  Definition* result = NULL;

  result = CanonicalizeCommutativeArithmetic(op_kind(),
                                             kSmiCid,
                                             left(),
                                             right());
  if (result != NULL) {
    return result;
  }

  result = CanonicalizeCommutativeArithmetic(op_kind(),
                                             kSmiCid,
                                             right(),
                                             left());
  if (result != NULL) {
    return result;
  }

  return this;
}


Definition* BinaryMintOpInstr::Canonicalize(FlowGraph* flow_graph) {
  Definition* result = NULL;

  result = CanonicalizeCommutativeArithmetic(op_kind(),
                                             kMintCid,
                                             left(),
                                             right());
  if (result != NULL) {
    return result;
  }

  result = CanonicalizeCommutativeArithmetic(op_kind(),
                                             kMintCid,
                                             right(),
                                             left());
  if (result != NULL) {
    return result;
  }

  return this;
}


// Optimizations that eliminate or simplify individual instructions.
Instruction* Instruction::Canonicalize(FlowGraph* flow_graph) {
  return this;
}


Definition* Definition::Canonicalize(FlowGraph* flow_graph) {
  return this;
}


bool LoadFieldInstr::IsImmutableLengthLoad() const {
  switch (recognized_kind()) {
    case MethodRecognizer::kObjectArrayLength:
    case MethodRecognizer::kImmutableArrayLength:
    case MethodRecognizer::kTypedDataLength:
    case MethodRecognizer::kStringBaseLength:
      return true;
    default:
      return false;
  }
}


MethodRecognizer::Kind LoadFieldInstr::RecognizedKindFromArrayCid(
    intptr_t cid) {
  if (RawObject::IsTypedDataClassId(cid) ||
      RawObject::IsExternalTypedDataClassId(cid)) {
    return MethodRecognizer::kTypedDataLength;
  }
  switch (cid) {
    case kArrayCid:
      return MethodRecognizer::kObjectArrayLength;
    case kImmutableArrayCid:
      return MethodRecognizer::kImmutableArrayLength;
    case kGrowableObjectArrayCid:
      return MethodRecognizer::kGrowableArrayLength;
    default:
      UNREACHABLE();
      return MethodRecognizer::kUnknown;
  }
}


bool LoadFieldInstr::IsFixedLengthArrayCid(intptr_t cid) {
  if (RawObject::IsTypedDataClassId(cid) ||
      RawObject::IsExternalTypedDataClassId(cid)) {
    return true;
  }

  switch (cid) {
    case kArrayCid:
    case kImmutableArrayCid:
      return true;
    default:
      return false;
  }
}


Definition* ConstantInstr::Canonicalize(FlowGraph* flow_graph) {
  return HasUses() ? this : NULL;
}


Definition* LoadFieldInstr::Canonicalize(FlowGraph* flow_graph) {
  if (!HasUses()) return NULL;
  if (!IsImmutableLengthLoad()) return this;

  // For fixed length arrays if the array is the result of a known constructor
  // call we can replace the length load with the length argument passed to
  // the constructor.
  StaticCallInstr* call = instance()->definition()->AsStaticCall();
  if ((call != NULL) &&
      call->is_known_list_constructor() &&
      IsFixedLengthArrayCid(call->Type()->ToCid())) {
    return call->ArgumentAt(1);
  }
  // For arrays with guarded lengths, replace the length load
  // with a constant.
  LoadFieldInstr* load_array = instance()->definition()->AsLoadField();
  if (load_array != NULL) {
    const Field* field = load_array->field();
    if ((field != NULL) && (field->guarded_list_length() >= 0)) {
      return flow_graph->GetConstant(
          Smi::Handle(Smi::New(field->guarded_list_length())));
    }
  }
  return this;
}


Definition* AssertBooleanInstr::Canonicalize(FlowGraph* flow_graph) {
  if (FLAG_eliminate_type_checks && (value()->Type()->ToCid() == kBoolCid)) {
    return value()->definition();
  }

  return this;
}


Definition* AssertAssignableInstr::Canonicalize(FlowGraph* flow_graph) {
  if (FLAG_eliminate_type_checks &&
      value()->Type()->IsAssignableTo(dst_type())) {
    return value()->definition();
  }

  // For uninstantiated target types: If the instantiator type arguments
  // are constant, instantiate the target type here.
  if (dst_type().IsInstantiated()) return this;

  ConstantInstr* constant_type_args =
      instantiator_type_arguments()->definition()->AsConstant();
  if (constant_type_args != NULL &&
      !constant_type_args->value().IsNull() &&
      constant_type_args->value().IsTypeArguments()) {
    const TypeArguments& instantiator_type_args =
        TypeArguments::Cast(constant_type_args->value());
    const AbstractType& new_dst_type = AbstractType::Handle(
        dst_type().InstantiateFrom(instantiator_type_args, NULL));
    // If dst_type is instantiated to dynamic or Object, skip the test.
    if (!new_dst_type.IsMalformed() && !new_dst_type.IsMalbounded() &&
        (new_dst_type.IsDynamicType() || new_dst_type.IsObjectType())) {
      return value()->definition();
    }
    set_dst_type(AbstractType::ZoneHandle(new_dst_type.Canonicalize()));
    if (FLAG_eliminate_type_checks &&
        value()->Type()->IsAssignableTo(dst_type())) {
      return value()->definition();
    }
    ConstantInstr* null_constant = flow_graph->constant_null();
    instantiator_type_arguments()->BindTo(null_constant);
  }
  return this;
}


Definition* BoxDoubleInstr::Canonicalize(FlowGraph* flow_graph) {
  if (input_use_list() == NULL) {
    // Environments can accomodate any representation. No need to box.
    return value()->definition();
  }

  // Fold away BoxDouble(UnboxDouble(v)) if value is known to be double.
  UnboxDoubleInstr* defn = value()->definition()->AsUnboxDouble();
  if ((defn != NULL) && (defn->value()->Type()->ToCid() == kDoubleCid)) {
    return defn->value()->definition();
  }

  return this;
}


Definition* UnboxDoubleInstr::Canonicalize(FlowGraph* flow_graph) {
  // Fold away UnboxDouble(BoxDouble(v)).
  BoxDoubleInstr* defn = value()->definition()->AsBoxDouble();
  return (defn != NULL) ? defn->value()->definition() : this;
}


Definition* BoxFloat32x4Instr::Canonicalize(FlowGraph* flow_graph) {
  if (input_use_list() == NULL) {
    // Environments can accomodate any representation. No need to box.
    return value()->definition();
  }

  // Fold away BoxFloat32x4(UnboxFloat32x4(v)).
  UnboxFloat32x4Instr* defn = value()->definition()->AsUnboxFloat32x4();
  if ((defn != NULL) && (defn->value()->Type()->ToCid() == kFloat32x4Cid)) {
    return defn->value()->definition();
  }

  return this;
}


Definition* UnboxFloat32x4Instr::Canonicalize(FlowGraph* flow_graph) {
  // Fold away UnboxFloat32x4(BoxFloat32x4(v)).
  BoxFloat32x4Instr* defn = value()->definition()->AsBoxFloat32x4();
  return (defn != NULL) ? defn->value()->definition() : this;
}


Definition* BoxInt32x4Instr::Canonicalize(FlowGraph* flow_graph) {
  if (input_use_list() == NULL) {
    // Environments can accomodate any representation. No need to box.
    return value()->definition();
  }

  // Fold away BoxInt32x4(UnboxInt32x4(v)).
  UnboxInt32x4Instr* defn = value()->definition()->AsUnboxInt32x4();
  if ((defn != NULL) && (defn->value()->Type()->ToCid() == kInt32x4Cid)) {
    return defn->value()->definition();
  }

  return this;
}


Definition* UnboxInt32x4Instr::Canonicalize(FlowGraph* flow_graph) {
  // Fold away UnboxInt32x4(BoxInt32x4(v)).
  BoxInt32x4Instr* defn = value()->definition()->AsBoxInt32x4();
  return (defn != NULL) ? defn->value()->definition() : this;
}


Definition* BooleanNegateInstr::Canonicalize(FlowGraph* flow_graph) {
  Definition* defn = value()->definition();
  if (defn->IsComparison() && defn->HasOnlyUse(value())) {
    // Comparisons always have a bool result.
    ASSERT(value()->Type()->ToCid() == kBoolCid);
    defn->AsComparison()->NegateComparison();
    return defn;
  }
  return this;
}


static bool MayBeBoxableNumber(intptr_t cid) {
  return (cid == kDynamicCid) ||
         (cid == kMintCid) ||
         (cid == kBigintCid) ||
         (cid == kDoubleCid);
}


static bool MaybeNumber(CompileType* type) {
  ASSERT(Type::Handle(Type::Number()).IsMoreSpecificThan(
         Type::Handle(Type::Number()), NULL));
  return type->ToAbstractType()->IsDynamicType()
      || type->ToAbstractType()->IsTypeParameter()
      || type->IsMoreSpecificThan(Type::Handle(Type::Number()));
}


// Returns a replacement for a strict comparison and signals if the result has
// to be negated.
static Definition* CanonicalizeStrictCompare(StrictCompareInstr* compare,
                                             bool* negated) {
  // Use propagated cid and type information to eliminate number checks.
  // If one of the inputs is not a boxable number (Mint, Double, Bigint), or
  // is not a subtype of num, no need for number checks.
  if (compare->needs_number_check()) {
    if (!MayBeBoxableNumber(compare->left()->Type()->ToCid()) ||
        !MayBeBoxableNumber(compare->right()->Type()->ToCid()))  {
      compare->set_needs_number_check(false);
    } else if (!MaybeNumber(compare->left()->Type()) ||
               !MaybeNumber(compare->right()->Type())) {
      compare->set_needs_number_check(false);
    }
  }

  *negated = false;
  Object& constant = Object::Handle();
  Value* other = NULL;
  if (compare->right()->BindsToConstant()) {
    constant = compare->right()->BoundConstant().raw();
    other = compare->left();
  } else if (compare->left()->BindsToConstant()) {
    constant = compare->left()->BoundConstant().raw();
    other = compare->right();
  } else {
    return compare;
  }

  Definition* other_defn = other->definition();
  Token::Kind kind = compare->kind();
  // Handle e === true.
  if ((kind == Token::kEQ_STRICT) &&
      (constant.raw() == Bool::True().raw()) &&
      (other->Type()->ToCid() == kBoolCid)) {
    return other_defn;
  }
  // Handle e !== false.
  if ((kind == Token::kNE_STRICT) &&
      (constant.raw() == Bool::False().raw()) &&
      (other->Type()->ToCid() == kBoolCid)) {
    return other_defn;
  }
  // Handle e !== true.
  if ((kind == Token::kNE_STRICT) &&
      (constant.raw() == Bool::True().raw()) &&
      other_defn->IsComparison() &&
      (other->Type()->ToCid() == kBoolCid) &&
      other_defn->HasOnlyUse(other)) {
    *negated = true;
    return other_defn;
  }
  // Handle e === false.
  if ((kind == Token::kEQ_STRICT) &&
      (constant.raw() == Bool::False().raw()) &&
      other_defn->IsComparison() &&
      (other->Type()->ToCid() == kBoolCid) &&
      other_defn->HasOnlyUse(other)) {
    *negated = true;
    return other_defn;
  }
  return compare;
}


Instruction* BranchInstr::Canonicalize(FlowGraph* flow_graph) {
  // Only handle strict-compares.
  if (comparison()->IsStrictCompare()) {
    bool negated = false;
    Definition* replacement =
        CanonicalizeStrictCompare(comparison()->AsStrictCompare(), &negated);
    if (replacement == comparison()) {
      return this;
    }
    ComparisonInstr* comp = replacement->AsComparison();
    if ((comp == NULL) || comp->CanDeoptimize()) {
      return this;
    }

    // Assert that the comparison is not serving as a pending deoptimization
    // target for conversions.
    for (intptr_t i = 0; i < comp->InputCount(); i++) {
      if (comp->RequiredInputRepresentation(i) !=
          comp->InputAt(i)->definition()->representation()) {
        return this;
      }
    }

    // Replace the comparison if the replacement is used at this branch,
    // and has exactly one use.
    Value* use = comp->input_use_list();
    if ((use->instruction() == this) && comp->HasOnlyUse(use)) {
      if (negated) {
        comp->NegateComparison();
      }
      RemoveEnvironment();
      flow_graph->CopyDeoptTarget(this, comp);
      // Unlink environment from the comparison since it is copied to the
      // branch instruction.
      comp->RemoveEnvironment();

      comp->RemoveFromGraph();
      SetComparison(comp);
      if (FLAG_trace_optimization) {
        OS::Print("Merging comparison v%" Pd "\n", comp->ssa_temp_index());
      }
      // Clear the comparison's temp index and ssa temp index since the
      // value of the comparison is not used outside the branch anymore.
      ASSERT(comp->input_use_list() == NULL);
      comp->ClearSSATempIndex();
      comp->ClearTempIndex();
    }
  }
  return this;
}


Definition* StrictCompareInstr::Canonicalize(FlowGraph* flow_graph) {
  bool negated = false;
  Definition* replacement = CanonicalizeStrictCompare(this, &negated);
  if (negated && replacement->IsComparison()) {
    ASSERT(replacement != this);
    replacement->AsComparison()->NegateComparison();
  }
  return replacement;
}


Instruction* CheckClassInstr::Canonicalize(FlowGraph* flow_graph) {
  // TODO(vegorov): Replace class checks with null checks when ToNullableCid
  // matches.

  const intptr_t value_cid = value()->Type()->ToCid();
  if (value_cid == kDynamicCid) {
    return this;
  }

  return unary_checks().HasReceiverClassId(value_cid) ? NULL : this;
}


Instruction* GuardFieldInstr::Canonicalize(FlowGraph* flow_graph) {
  if (field().guarded_cid() == kDynamicCid) {
    return NULL;  // Nothing to guard.
  }

  if (field().guarded_list_length() != Field::kNoFixedLength) {
    // We are still guarding the list length.
    return this;
  }

  if (field().is_nullable() && value()->Type()->IsNull()) {
    return NULL;
  }

  const intptr_t cid = field().is_nullable() ? value()->Type()->ToNullableCid()
                                             : value()->Type()->ToCid();
  if (field().guarded_cid() == cid) {
    return NULL;  // Value is guaranteed to have this cid.
  }

  return this;
}


Instruction* CheckSmiInstr::Canonicalize(FlowGraph* flow_graph) {
  return (value()->Type()->ToCid() == kSmiCid) ?  NULL : this;
}


Instruction* CheckEitherNonSmiInstr::Canonicalize(FlowGraph* flow_graph) {
  if ((left()->Type()->ToCid() == kDoubleCid) ||
      (right()->Type()->ToCid() == kDoubleCid)) {
    return NULL;  // Remove from the graph.
  }
  return this;
}


// Shared code generation methods (EmitNativeCode and
// MakeLocationSummary). Only assembly code that can be shared across all
// architectures can be used. Machine specific register allocation and code
// generation is located in intermediate_language_<arch>.cc

#define __ compiler->assembler()->

LocationSummary* GraphEntryInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


LocationSummary* JoinEntryInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


void JoinEntryInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  __ Bind(compiler->GetJumpLabel(this));
  if (!compiler->is_optimizing()) {
    compiler->AddCurrentDescriptor(PcDescriptors::kDeopt,
                                   deopt_id_,
                                   Scanner::kDummyTokenIndex);
  }
  if (HasParallelMove()) {
    compiler->parallel_move_resolver()->EmitNativeCode(parallel_move());
  }
}


LocationSummary* TargetEntryInstr::MakeLocationSummary() const {
  // FlowGraphCompiler::EmitInstructionPrologue is not called for block
  // entry instructions, so this function is unused.  If it becomes
  // reachable, note that the deoptimization descriptor in unoptimized code
  // comes after the point of local register allocation due to pattern
  // matching the edge counter code backwards (as a code reuse convenience
  // on some platforms).
  UNREACHABLE();
  return NULL;
}


LocationSummary* PhiInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


void PhiInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  UNREACHABLE();
}


LocationSummary* RedefinitionInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


void RedefinitionInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  UNREACHABLE();
}


LocationSummary* ParameterInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


void ParameterInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  UNREACHABLE();
}


LocationSummary* ParallelMoveInstr::MakeLocationSummary() const {
  return NULL;
}


void ParallelMoveInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  UNREACHABLE();
}


LocationSummary* ConstraintInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


void ConstraintInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  UNREACHABLE();
}


LocationSummary* MaterializeObjectInstr::MakeLocationSummary() const {
  UNREACHABLE();
  return NULL;
}


void MaterializeObjectInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  UNREACHABLE();
}


LocationSummary* StoreContextInstr::MakeLocationSummary() const {
  const intptr_t kNumInputs = 1;
  const intptr_t kNumTemps = 0;
  LocationSummary* summary =
      new LocationSummary(kNumInputs, kNumTemps, LocationSummary::kNoCall);
  summary->set_in(0, Location::RegisterLocation(CTX));
  return summary;
}


LocationSummary* PushTempInstr::MakeLocationSummary() const {
  return LocationSummary::Make(1,
                               Location::NoLocation(),
                               LocationSummary::kNoCall);
}


void PushTempInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  ASSERT(!compiler->is_optimizing());
  // Nothing to do.
}


LocationSummary* DropTempsInstr::MakeLocationSummary() const {
  return LocationSummary::Make(1,
                               Location::SameAsFirstInput(),
                               LocationSummary::kNoCall);
}


void DropTempsInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  ASSERT(!compiler->is_optimizing());
  Register value = locs()->in(0).reg();
  Register result = locs()->out().reg();
  ASSERT(result == value);  // Assert that register assignment is correct.
  __ Drop(num_temps());
}


void StoreContextInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  // Nothing to do.  Context register was loaded by the register allocator.
  ASSERT(locs()->in(0).reg() == CTX);
}


StrictCompareInstr::StrictCompareInstr(intptr_t token_pos,
                                       Token::Kind kind,
                                       Value* left,
                                       Value* right)
    : ComparisonInstr(token_pos, kind, left, right),
      needs_number_check_(FLAG_new_identity_spec) {
  ASSERT((kind == Token::kEQ_STRICT) || (kind == Token::kNE_STRICT));
}


LocationSummary* InstanceCallInstr::MakeLocationSummary() const {
  return MakeCallSummary();
}


void InstanceCallInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  ICData& call_ic_data = ICData::ZoneHandle(ic_data()->raw());
  if (!FLAG_propagate_ic_data || !compiler->is_optimizing()) {
    const Array& arguments_descriptor =
        Array::Handle(ArgumentsDescriptor::New(ArgumentCount(),
                                               argument_names()));
    call_ic_data = ICData::New(compiler->parsed_function().function(),
                               function_name(),
                               arguments_descriptor,
                               deopt_id(),
                               checked_argument_count());
  }
  if (compiler->is_optimizing()) {
    ASSERT(HasICData());
    if (ic_data()->NumberOfChecks() > 0) {
      const ICData& unary_ic_data =
          ICData::ZoneHandle(ic_data()->AsUnaryClassChecks());
      compiler->GenerateInstanceCall(deopt_id(),
                                     token_pos(),
                                     ArgumentCount(),
                                     argument_names(),
                                     locs(),
                                     unary_ic_data);
    } else {
      // Call was not visited yet, use original ICData in order to populate it.
      compiler->GenerateInstanceCall(deopt_id(),
                                     token_pos(),
                                     ArgumentCount(),
                                     argument_names(),
                                     locs(),
                                     call_ic_data);
    }
  } else {
    // Unoptimized code.
    ASSERT(!HasICData());
    compiler->AddCurrentDescriptor(PcDescriptors::kDeopt,
                                   deopt_id(),
                                   token_pos());
    compiler->GenerateInstanceCall(deopt_id(),
                                   token_pos(),
                                   ArgumentCount(),
                                   argument_names(),
                                   locs(),
                                   call_ic_data);
  }
}


bool PolymorphicInstanceCallInstr::HasRecognizedTarget() const {
  return ic_data().HasOneTarget() &&
      (MethodRecognizer::RecognizeKind(
          Function::Handle(ic_data().GetTargetAt(0))) !=
       MethodRecognizer::kUnknown);
}


LocationSummary* StaticCallInstr::MakeLocationSummary() const {
  return MakeCallSummary();
}


void StaticCallInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  if (!compiler->is_optimizing()) {
    // Some static calls can be optimized by the optimizing compiler (e.g. sqrt)
    // and therefore need a deoptimization descriptor.
    compiler->AddCurrentDescriptor(PcDescriptors::kDeopt,
                                   deopt_id(),
                                   token_pos());
  }
  compiler->GenerateStaticCall(deopt_id(),
                               token_pos(),
                               function(),
                               ArgumentCount(),
                               argument_names(),
                               locs());
}


void AssertAssignableInstr::EmitNativeCode(FlowGraphCompiler* compiler) {
  compiler->GenerateAssertAssignable(token_pos(),
                                     deopt_id(),
                                     dst_type(),
                                     dst_name(),
                                     locs());
  ASSERT(locs()->in(0).reg() == locs()->out().reg());
}


Environment* Environment::From(const GrowableArray<Definition*>& definitions,
                               intptr_t fixed_parameter_count,
                               const Function& function) {
  Environment* env =
      new Environment(definitions.length(),
                      fixed_parameter_count,
                      Isolate::kNoDeoptId,
                      function,
                      NULL);
  for (intptr_t i = 0; i < definitions.length(); ++i) {
    env->values_.Add(new Value(definitions[i]));
  }
  return env;
}


Environment* Environment::DeepCopy(intptr_t length) const {
  ASSERT(length <= values_.length());
  Environment* copy =
      new Environment(length,
                      fixed_parameter_count_,
                      deopt_id_,
                      function_,
                      (outer_ == NULL) ? NULL : outer_->DeepCopy());
  if (locations_ != NULL) {
    Location* new_locations =
        Isolate::Current()->current_zone()->Alloc<Location>(length);
    copy->set_locations(new_locations);
  }
  for (intptr_t i = 0; i < length; ++i) {
    copy->values_.Add(values_[i]->Copy());
    if (locations_ != NULL) {
      copy->locations_[i] = locations_[i];
    }
  }
  return copy;
}


// Copies the environment and updates the environment use lists.
void Environment::DeepCopyTo(Instruction* instr) const {
  for (Environment::DeepIterator it(instr->env()); !it.Done(); it.Advance()) {
    it.CurrentValue()->RemoveFromUseList();
  }

  Environment* copy = DeepCopy();
  instr->SetEnvironment(copy);
  for (Environment::DeepIterator it(copy); !it.Done(); it.Advance()) {
    Value* value = it.CurrentValue();
    value->definition()->AddEnvUse(value);
  }
}


// Copies the environment as outer on an inlined instruction and updates the
// environment use lists.
void Environment::DeepCopyToOuter(Instruction* instr) const {
  // Create a deep copy removing caller arguments from the environment.
  ASSERT(this != NULL);
  ASSERT(instr->env()->outer() == NULL);
  intptr_t argument_count = instr->env()->fixed_parameter_count();
  Environment* copy = DeepCopy(values_.length() - argument_count);
  instr->env()->outer_ = copy;
  intptr_t use_index = instr->env()->Length();  // Start index after inner.
  for (Environment::DeepIterator it(copy); !it.Done(); it.Advance()) {
    Value* value = it.CurrentValue();
    value->set_instruction(instr);
    value->set_use_index(use_index++);
    value->definition()->AddEnvUse(value);
  }
}


RangeBoundary RangeBoundary::FromDefinition(Definition* defn, intptr_t offs) {
  if (defn->IsConstant() && defn->AsConstant()->value().IsSmi()) {
    return FromConstant(Smi::Cast(defn->AsConstant()->value()).Value() + offs);
  }
  return RangeBoundary(kSymbol, reinterpret_cast<intptr_t>(defn), offs);
}


RangeBoundary RangeBoundary::LowerBound() const {
  if (IsConstant()) return *this;
  return Add(Range::ConstantMin(symbol()->range()),
             RangeBoundary::FromConstant(offset_),
             OverflowedMinSmi());
}


RangeBoundary RangeBoundary::UpperBound() const {
  if (IsConstant()) return *this;
  return Add(Range::ConstantMax(symbol()->range()),
             RangeBoundary::FromConstant(offset_),
             OverflowedMaxSmi());
}


static Definition* UnwrapConstraint(Definition* defn) {
  while (defn->IsConstraint()) {
    defn = defn->AsConstraint()->value()->definition();
  }
  return defn;
}


static bool AreEqualDefinitions(Definition* a, Definition* b) {
  a = UnwrapConstraint(a);
  b = UnwrapConstraint(b);
  return (a == b) ||
      (a->AllowsCSE() &&
       a->Dependencies().IsNone() &&
       b->AllowsCSE() &&
       b->Dependencies().IsNone() &&
       a->Equals(b));
}


// Returns true if two range boundaries refer to the same symbol.
static bool DependOnSameSymbol(const RangeBoundary& a, const RangeBoundary& b) {
  return a.IsSymbol() && b.IsSymbol() &&
      AreEqualDefinitions(a.symbol(), b.symbol());
}


// Returns true if range has a least specific minimum value.
static bool IsMinSmi(Range* range) {
  return (range == NULL) ||
      (range->min().IsConstant() &&
       (range->min().value() <= Smi::kMinValue));
}


// Returns true if range has a least specific maximium value.
static bool IsMaxSmi(Range* range) {
  return (range == NULL) ||
      (range->max().IsConstant() &&
       (range->max().value() >= Smi::kMaxValue));
}


// Returns true if two range boundaries can be proven to be equal.
static bool IsEqual(const RangeBoundary& a, const RangeBoundary& b) {
  if (a.IsConstant() && b.IsConstant()) {
    return a.value() == b.value();
  } else if (a.IsSymbol() && b.IsSymbol()) {
    return (a.offset() == b.offset()) && DependOnSameSymbol(a, b);
  } else {
    return false;
  }
}


static RangeBoundary CanonicalizeBoundary(const RangeBoundary& a,
                                          const RangeBoundary& overflow) {
  if (a.IsConstant()) return a;

  intptr_t offset = a.offset();
  Definition* symbol = a.symbol();

  bool changed;
  do {
    changed = false;
    if (symbol->IsConstraint()) {
      symbol = symbol->AsConstraint()->value()->definition();
      changed = true;
    } else if (symbol->IsBinarySmiOp()) {
      BinarySmiOpInstr* op = symbol->AsBinarySmiOp();
      Definition* left = op->left()->definition();
      Definition* right = op->right()->definition();
      switch (op->op_kind()) {
        case Token::kADD:
          if (right->IsConstant()) {
            offset += Smi::Cast(right->AsConstant()->value()).Value();
            symbol = left;
            changed = true;
          } else if (left->IsConstant()) {
            offset += Smi::Cast(left->AsConstant()->value()).Value();
            symbol = right;
            changed = true;
          }
          break;

        case Token::kSUB:
          if (right->IsConstant()) {
            offset -= Smi::Cast(right->AsConstant()->value()).Value();
            symbol = left;
            changed = true;
          }
          break;

        default:
          break;
      }
    }

    if (!Smi::IsValid(offset)) return overflow;
  } while (changed);

  return RangeBoundary::FromDefinition(symbol, offset);
}


static bool CanonicalizeMaxBoundary(RangeBoundary* a) {
  if (!a->IsSymbol()) return false;

  Range* range = a->symbol()->range();
  if ((range == NULL) || !range->max().IsSymbol()) return false;

  const intptr_t offset = range->max().offset() + a->offset();

  if (!Smi::IsValid(offset)) {
    *a = RangeBoundary::OverflowedMaxSmi();
    return true;
  }

  *a = CanonicalizeBoundary(
      RangeBoundary::FromDefinition(range->max().symbol(), offset),
      RangeBoundary::OverflowedMaxSmi());

  return true;
}


static bool CanonicalizeMinBoundary(RangeBoundary* a) {
  if (!a->IsSymbol()) return false;

  Range* range = a->symbol()->range();
  if ((range == NULL) || !range->min().IsSymbol()) return false;

  const intptr_t offset = range->min().offset() + a->offset();
  if (!Smi::IsValid(offset)) {
    *a = RangeBoundary::OverflowedMinSmi();
    return true;
  }

  *a = CanonicalizeBoundary(
      RangeBoundary::FromDefinition(range->min().symbol(), offset),
      RangeBoundary::OverflowedMinSmi());

  return true;
}


RangeBoundary RangeBoundary::Min(RangeBoundary a, RangeBoundary b) {
  if (DependOnSameSymbol(a, b)) {
    return (a.offset() <= b.offset()) ? a : b;
  }

  const intptr_t min_a = a.LowerBound().Clamp().value();
  const intptr_t min_b = b.LowerBound().Clamp().value();

  return RangeBoundary::FromConstant(Utils::Minimum(min_a, min_b));
}


RangeBoundary RangeBoundary::Max(RangeBoundary a, RangeBoundary b) {
  if (DependOnSameSymbol(a, b)) {
    return (a.offset() >= b.offset()) ? a : b;
  }

  const intptr_t max_a = a.UpperBound().Clamp().value();
  const intptr_t max_b = b.UpperBound().Clamp().value();

  return RangeBoundary::FromConstant(Utils::Maximum(max_a, max_b));
}


void Definition::InferRange() {
  ASSERT(Type()->ToCid() == kSmiCid);  // Has meaning only for smis.
  if (range_ == NULL) {
    range_ = Range::Unknown();
  }
}


void ConstantInstr::InferRange() {
  ASSERT(value_.IsSmi());
  if (range_ == NULL) {
    intptr_t value = Smi::Cast(value_).Value();
    range_ = new Range(RangeBoundary::FromConstant(value),
                       RangeBoundary::FromConstant(value));
  }
}


void ConstraintInstr::InferRange() {
  Range* value_range = value()->definition()->range();

  RangeBoundary min;
  RangeBoundary max;

  if (IsMinSmi(value_range) && !IsMinSmi(constraint())) {
    min = constraint()->min();
  } else if (IsMinSmi(constraint()) && !IsMinSmi(value_range)) {
    min = value_range->min();
  } else if ((value_range != NULL) &&
             IsEqual(constraint()->min(), value_range->min())) {
    min = constraint()->min();
  } else {
    if (value_range != NULL) {
      RangeBoundary canonical_a =
          CanonicalizeBoundary(constraint()->min(),
                               RangeBoundary::OverflowedMinSmi());
      RangeBoundary canonical_b =
          CanonicalizeBoundary(value_range->min(),
                               RangeBoundary::OverflowedMinSmi());

      do {
        if (DependOnSameSymbol(canonical_a, canonical_b)) {
          min = (canonical_a.offset() <= canonical_b.offset()) ? canonical_b
                                                               : canonical_a;
        }
      } while (CanonicalizeMinBoundary(&canonical_a) ||
               CanonicalizeMinBoundary(&canonical_b));
    }

    if (min.IsUnknown()) {
      min = RangeBoundary::Max(Range::ConstantMin(value_range),
                               Range::ConstantMin(constraint()));
    }
  }

  if (IsMaxSmi(value_range) && !IsMaxSmi(constraint())) {
    max = constraint()->max();
  } else if (IsMaxSmi(constraint()) && !IsMaxSmi(value_range)) {
    max = value_range->max();
  } else if ((value_range != NULL) &&
             IsEqual(constraint()->max(), value_range->max())) {
    max = constraint()->max();
  } else {
    if (value_range != NULL) {
      RangeBoundary canonical_b =
          CanonicalizeBoundary(value_range->max(),
                               RangeBoundary::OverflowedMaxSmi());
      RangeBoundary canonical_a =
          CanonicalizeBoundary(constraint()->max(),
                               RangeBoundary::OverflowedMaxSmi());

      do {
        if (DependOnSameSymbol(canonical_a, canonical_b)) {
          max = (canonical_a.offset() <= canonical_b.offset()) ? canonical_a
                                                               : canonical_b;
          break;
        }
      } while (CanonicalizeMaxBoundary(&canonical_a) ||
               CanonicalizeMaxBoundary(&canonical_b));
    }

    if (max.IsUnknown()) {
      max = RangeBoundary::Min(Range::ConstantMax(value_range),
                               Range::ConstantMax(constraint()));
    }
  }

  range_ = new Range(min, max);

  // Mark branches that generate unsatisfiable constraints as constant.
  if (target() != NULL && range_->IsUnsatisfiable()) {
    BranchInstr* branch =
        target()->PredecessorAt(0)->last_instruction()->AsBranch();
    if (target() == branch->true_successor()) {
      // True unreachable.
      if (FLAG_trace_constant_propagation) {
        OS::Print("Range analysis: True unreachable (B%" Pd ")\n",
                  branch->true_successor()->block_id());
      }
      branch->set_constant_target(branch->false_successor());
    } else {
      ASSERT(target() == branch->false_successor());
      // False unreachable.
      if (FLAG_trace_constant_propagation) {
        OS::Print("Range analysis: False unreachable (B%" Pd ")\n",
                  branch->false_successor()->block_id());
      }
      branch->set_constant_target(branch->true_successor());
    }
  }
}


void LoadFieldInstr::InferRange() {
  if ((range_ == NULL) &&
      ((recognized_kind() == MethodRecognizer::kObjectArrayLength) ||
       (recognized_kind() == MethodRecognizer::kImmutableArrayLength))) {
    range_ = new Range(RangeBoundary::FromConstant(0),
                       RangeBoundary::FromConstant(Array::kMaxElements));
    return;
  }
  if ((range_ == NULL) &&
      (recognized_kind() == MethodRecognizer::kTypedDataLength)) {
    range_ = new Range(RangeBoundary::FromConstant(0), RangeBoundary::MaxSmi());
    return;
  }
  if ((range_ == NULL) &&
      (recognized_kind() == MethodRecognizer::kStringBaseLength)) {
    range_ = new Range(RangeBoundary::FromConstant(0),
                       RangeBoundary::FromConstant(String::kMaxElements));
    return;
  }
  Definition::InferRange();
}



void LoadIndexedInstr::InferRange() {
  switch (class_id()) {
    case kTypedDataInt8ArrayCid:
      range_ = new Range(RangeBoundary::FromConstant(-128),
                         RangeBoundary::FromConstant(127));
      break;
    case kTypedDataUint8ArrayCid:
    case kTypedDataUint8ClampedArrayCid:
    case kExternalTypedDataUint8ArrayCid:
    case kExternalTypedDataUint8ClampedArrayCid:
      range_ = new Range(RangeBoundary::FromConstant(0),
                         RangeBoundary::FromConstant(255));
      break;
    case kTypedDataInt16ArrayCid:
      range_ = new Range(RangeBoundary::FromConstant(-32768),
                         RangeBoundary::FromConstant(32767));
      break;
    case kTypedDataUint16ArrayCid:
      range_ = new Range(RangeBoundary::FromConstant(0),
                         RangeBoundary::FromConstant(65535));
      break;
    case kOneByteStringCid:
      range_ = new Range(RangeBoundary::FromConstant(0),
                         RangeBoundary::FromConstant(0xFF));
      break;
    case kTwoByteStringCid:
      range_ = new Range(RangeBoundary::FromConstant(0),
                         RangeBoundary::FromConstant(0xFFFF));
      break;
    default:
      Definition::InferRange();
      break;
  }
}


void IfThenElseInstr::InferRange() {
  const intptr_t min = Utils::Minimum(if_true_, if_false_);
  const intptr_t max = Utils::Maximum(if_true_, if_false_);
  range_ = new Range(RangeBoundary::FromConstant(min),
                     RangeBoundary::FromConstant(max));
}


void PhiInstr::InferRange() {
  RangeBoundary new_min;
  RangeBoundary new_max;

  for (intptr_t i = 0; i < InputCount(); i++) {
    Range* input_range = InputAt(i)->definition()->range();
    if (input_range == NULL) {
      range_ = Range::Unknown();
      return;
    }

    if (new_min.IsUnknown()) {
      new_min = Range::ConstantMin(input_range);
    } else {
      new_min = RangeBoundary::Min(new_min, Range::ConstantMin(input_range));
    }

    if (new_max.IsUnknown()) {
      new_max = Range::ConstantMax(input_range);
    } else {
      new_max = RangeBoundary::Max(new_max, Range::ConstantMax(input_range));
    }
  }

  ASSERT(new_min.IsUnknown() == new_max.IsUnknown());
  if (new_min.IsUnknown()) {
    range_ = Range::Unknown();
    return;
  }

  range_ = new Range(new_min, new_max);
}


static bool SymbolicSub(const RangeBoundary& a,
                        const RangeBoundary& b,
                        RangeBoundary* result) {
  if (a.IsSymbol() && b.IsConstant() && !b.Overflowed()) {
    const intptr_t offset = a.offset() - b.value();
    if (!Smi::IsValid(offset)) return false;

    *result = RangeBoundary::FromDefinition(a.symbol(), offset);
    return true;
  }
  return false;
}


static bool SymbolicAdd(const RangeBoundary& a,
                        const RangeBoundary& b,
                        RangeBoundary* result) {
  if (a.IsSymbol() && b.IsConstant() && !b.Overflowed()) {
    const intptr_t offset = a.offset() + b.value();
    if (!Smi::IsValid(offset)) return false;

    *result = RangeBoundary::FromDefinition(a.symbol(), offset);
    return true;
  } else if (b.IsSymbol() && a.IsConstant() && !a.Overflowed()) {
    const intptr_t offset = b.offset() + a.value();
    if (!Smi::IsValid(offset)) return false;

    *result = RangeBoundary::FromDefinition(b.symbol(), offset);
    return true;
  }
  return false;
}


static bool IsArrayLength(Definition* defn) {
  LoadFieldInstr* load = defn->AsLoadField();
  return (load != NULL) && load->IsImmutableLengthLoad();
}


void BinarySmiOpInstr::InferRange() {
  // TODO(vegorov): canonicalize BinarySmiOp to always have constant on the
  // right and a non-constant on the left.
  Definition* left_defn = left()->definition();

  Range* left_range = left_defn->range();
  Range* right_range = right()->definition()->range();

  if ((left_range == NULL) || (right_range == NULL)) {
    range_ = new Range(RangeBoundary::MinSmi(), RangeBoundary::MaxSmi());
    return;
  }

  RangeBoundary left_min =
    IsArrayLength(left_defn) ?
        RangeBoundary::FromDefinition(left_defn) : left_range->min();

  RangeBoundary left_max =
    IsArrayLength(left_defn) ?
        RangeBoundary::FromDefinition(left_defn) : left_range->max();

  RangeBoundary min;
  RangeBoundary max;
  switch (op_kind()) {
    case Token::kADD:
      if (!SymbolicAdd(left_min, right_range->min(), &min)) {
        min =
          RangeBoundary::Add(Range::ConstantMin(left_range),
                             Range::ConstantMin(right_range),
                             RangeBoundary::OverflowedMinSmi());
      }

      if (!SymbolicAdd(left_max, right_range->max(), &max)) {
        max =
          RangeBoundary::Add(Range::ConstantMax(right_range),
                             Range::ConstantMax(left_range),
                             RangeBoundary::OverflowedMaxSmi());
      }
      break;

    case Token::kSUB:
      if (!SymbolicSub(left_min, right_range->max(), &min)) {
        min =
          RangeBoundary::Sub(Range::ConstantMin(left_range),
                             Range::ConstantMax(right_range),
                             RangeBoundary::OverflowedMinSmi());
      }

      if (!SymbolicSub(left_max, right_range->min(), &max)) {
        max =
          RangeBoundary::Sub(Range::ConstantMax(left_range),
                             Range::ConstantMin(right_range),
                             RangeBoundary::OverflowedMaxSmi());
      }
      break;

    case Token::kBIT_AND:
      if (Range::ConstantMin(right_range).value() >= 0) {
        min = RangeBoundary::FromConstant(0);
        max = Range::ConstantMax(right_range);
        break;
      }
      if (Range::ConstantMin(left_range).value() >= 0) {
        min = RangeBoundary::FromConstant(0);
        max = Range::ConstantMax(left_range);
        break;
      }

      if (range_ == NULL) {
        range_ = Range::Unknown();
      }
      return;

    default:
      if (range_ == NULL) {
        range_ = Range::Unknown();
      }
      return;
  }

  ASSERT(!min.IsUnknown() && !max.IsUnknown());
  set_overflow(min.LowerBound().Overflowed() || max.UpperBound().Overflowed());

  if (min.IsConstant()) min.Clamp();
  if (max.IsConstant()) max.Clamp();

  range_ = new Range(min, max);
}


// Inclusive.
bool Range::IsWithin(intptr_t min_int, intptr_t max_int) const {
  if (min().LowerBound().value() < min_int) return false;
  if (max().UpperBound().value() > max_int) return false;
  return true;
}


bool Range::IsUnsatisfiable() const {
  // Constant case: For example [0, -1].
  if (Range::ConstantMin(this).value() > Range::ConstantMax(this).value()) {
    return true;
  }
  // Symbol case: For example [v+1, v].
  if (DependOnSameSymbol(min(), max()) && min().offset() > max().offset()) {
    return true;
  }
  return false;
}


bool CheckArrayBoundInstr::IsFixedLengthArrayType(intptr_t cid) {
  return LoadFieldInstr::IsFixedLengthArrayCid(cid);
}


bool CheckArrayBoundInstr::IsRedundant(RangeBoundary length) {
  Range* index_range = index()->definition()->range();

  // Range of the index is unknown can't decide if the check is redundant.
  if (index_range == NULL) {
    return false;
  }

  // Range of the index is not positive. Check can't be redundant.
  if (Range::ConstantMin(index_range).value() < 0) {
    return false;
  }

  RangeBoundary max = CanonicalizeBoundary(index_range->max(),
                                           RangeBoundary::OverflowedMaxSmi());

  if (max.Overflowed()) {
    return false;
  }

  // Try to compare constant boundaries.
  if (max.UpperBound().value() < length.LowerBound().value()) {
    return true;
  }

  length = CanonicalizeBoundary(length, RangeBoundary::OverflowedMaxSmi());
  if (length.Overflowed()) {
    return false;
  }

  // Try symbolic comparison.
  do {
    if (DependOnSameSymbol(max, length)) return max.offset() < length.offset();
  } while (CanonicalizeMaxBoundary(&max) || CanonicalizeMinBoundary(&length));

  // Failed to prove that maximum is bounded with array length.
  return false;
}


Instruction* CheckArrayBoundInstr::Canonicalize(FlowGraph* flow_graph) {
  return IsRedundant(RangeBoundary::FromDefinition(length()->definition())) ?
      NULL : this;
}


intptr_t CheckArrayBoundInstr::LengthOffsetFor(intptr_t class_id) {
  if (RawObject::IsExternalTypedDataClassId(class_id)) {
    return ExternalTypedData::length_offset();
  }
  if (RawObject::IsTypedDataClassId(class_id)) {
    return TypedData::length_offset();
  }
  switch (class_id) {
    case kGrowableObjectArrayCid:
      return GrowableObjectArray::length_offset();
    case kOneByteStringCid:
    case kTwoByteStringCid:
      return String::length_offset();
    case kArrayCid:
    case kImmutableArrayCid:
      return Array::length_offset();
    default:
      UNREACHABLE();
      return -1;
  }
}


const Function& StringInterpolateInstr::CallFunction() const {
  if (function_.IsNull()) {
    const int kNumberOfArguments = 1;
    const Array& kNoArgumentNames = Object::null_array();
    const Class& cls =
        Class::Handle(Library::LookupCoreClass(Symbols::StringBase()));
    ASSERT(!cls.IsNull());
    function_ =
        Resolver::ResolveStatic(
            cls,
            Library::PrivateCoreLibName(Symbols::Interpolate()),
            kNumberOfArguments,
            kNoArgumentNames);
  }
  ASSERT(!function_.IsNull());
  return function_;
}


// Replace StringInterpolateInstr with a constant string if all inputs are
// constant of [string, number, boolean, null].
// Leave the CreateArrayInstr and StoreIndexedInstr in the stream in case
// deoptimization occurs.
Definition* StringInterpolateInstr::Canonicalize(FlowGraph* flow_graph) {
  // The following graph structure is generated by the graph builder:
  //   v2 <- CreateArray(v0)
  //   StoreIndexed(v2, v3, v4)   -- v3:constant index, v4: value.
  //   ..
  //   v8 <- StringInterpolate(v2)
  CreateArrayInstr* create_array = value()->definition()->AsCreateArray();
  ASSERT(create_array != NULL);
  // Check if the string interpolation has only constant inputs.
  GrowableArray<ConstantInstr*> constants(create_array->num_elements());
  for (intptr_t i = 0; i < create_array->num_elements(); i++) {
    constants.Add(NULL);
  }
  for (Value::Iterator it(create_array->input_use_list());
       !it.Done();
       it.Advance()) {
    Instruction* curr = it.Current()->instruction();
    if (curr != this) {
      StoreIndexedInstr* store = curr->AsStoreIndexed();
      ASSERT(store != NULL);
      if (store->value()->definition()->IsConstant()) {
        ASSERT(store->index()->BindsToConstant());
        const Object& obj = store->value()->definition()->AsConstant()->value();
        if (obj.IsNumber() || obj.IsString() || obj.IsBool() || obj.IsNull()) {
          constants[Smi::Cast(store->index()->BoundConstant()).Value()] =
              store->value()->definition()->AsConstant();
        } else {
          return this;
        }
      } else {
        return this;
      }
    }
  }
  // Interpolate string at compile time.
  const Array& array_argument =
      Array::Handle(Array::New(create_array->num_elements()));
  for (intptr_t i = 0; i < constants.length(); i++) {
    array_argument.SetAt(i, constants[i]->value());
  }
  // Build argument array to pass to the interpolation function.
  const Array& interpolate_arg = Array::Handle(Array::New(1));
  interpolate_arg.SetAt(0, array_argument);
  // Call interpolation function.
  String& concatenated = String::ZoneHandle();
  concatenated ^=
      DartEntry::InvokeFunction(CallFunction(), interpolate_arg);
  if (concatenated.IsUnhandledException()) {
    return this;
  }
  concatenated = Symbols::New(concatenated);
  return flow_graph->GetConstant(concatenated);
}


InvokeMathCFunctionInstr::InvokeMathCFunctionInstr(
    ZoneGrowableArray<Value*>* inputs,
    intptr_t original_deopt_id,
    MethodRecognizer::Kind recognized_kind)
    : inputs_(inputs),
      locs_(NULL),
      recognized_kind_(recognized_kind) {
  ASSERT(inputs_->length() == ArgumentCountFor(recognized_kind_));
  for (intptr_t i = 0; i < inputs_->length(); ++i) {
    ASSERT((*inputs)[i] != NULL);
    (*inputs)[i]->set_instruction(this);
    (*inputs)[i]->set_use_index(i);
  }
  deopt_id_ = original_deopt_id;
}


intptr_t InvokeMathCFunctionInstr::ArgumentCountFor(
    MethodRecognizer::Kind kind) {
  switch (kind) {
    case MethodRecognizer::kDoubleTruncate:
    case MethodRecognizer::kDoubleFloor:
    case MethodRecognizer::kDoubleCeil: {
      ASSERT(!CPUFeatures::double_truncate_round_supported());
      return 1;
    }
    case MethodRecognizer::kDoubleRound:
      return 1;
    case MethodRecognizer::kDoubleMod:
    case MethodRecognizer::kMathDoublePow:
      return 2;
    default:
      UNREACHABLE();
  }
  return 0;
}

// Use expected function signatures to help MSVC compiler resolve overloading.
typedef double (*UnaryMathCFunction) (double x);
typedef double (*BinaryMathCFunction) (double x, double y);

extern const RuntimeEntry kPowRuntimeEntry(
    "libc_pow", reinterpret_cast<RuntimeFunction>(
        static_cast<BinaryMathCFunction>(&pow)), 2, true, true);

extern const RuntimeEntry kModRuntimeEntry(
    "DartModulo", reinterpret_cast<RuntimeFunction>(
        static_cast<BinaryMathCFunction>(&DartModulo)), 2, true, true);

extern const RuntimeEntry kFloorRuntimeEntry(
    "libc_floor", reinterpret_cast<RuntimeFunction>(
        static_cast<UnaryMathCFunction>(&floor)), 1, true, true);

extern const RuntimeEntry kCeilRuntimeEntry(
    "libc_ceil", reinterpret_cast<RuntimeFunction>(
        static_cast<UnaryMathCFunction>(&ceil)), 1, true, true);

extern const RuntimeEntry kTruncRuntimeEntry(
    "libc_trunc", reinterpret_cast<RuntimeFunction>(
        static_cast<UnaryMathCFunction>(&trunc)), 1, true, true);

extern const RuntimeEntry kRoundRuntimeEntry(
    "libc_round", reinterpret_cast<RuntimeFunction>(
        static_cast<UnaryMathCFunction>(&round)), 1, true, true);


const RuntimeEntry& InvokeMathCFunctionInstr::TargetFunction() const {
  switch (recognized_kind_) {
    case MethodRecognizer::kDoubleTruncate:
      return kTruncRuntimeEntry;
    case MethodRecognizer::kDoubleRound:
      return kRoundRuntimeEntry;
    case MethodRecognizer::kDoubleFloor:
      return kFloorRuntimeEntry;
    case MethodRecognizer::kDoubleCeil:
      return kCeilRuntimeEntry;
    case MethodRecognizer::kMathDoublePow:
      return kPowRuntimeEntry;
    case MethodRecognizer::kDoubleMod:
      return kModRuntimeEntry;
    default:
      UNREACHABLE();
  }
  return kPowRuntimeEntry;
}


extern const RuntimeEntry kCosRuntimeEntry(
    "libc_cos", reinterpret_cast<RuntimeFunction>(
        static_cast<UnaryMathCFunction>(&cos)), 1, true, true);

extern const RuntimeEntry kSinRuntimeEntry(
    "libc_sin", reinterpret_cast<RuntimeFunction>(
        static_cast<UnaryMathCFunction>(&sin)), 1, true, true);


const RuntimeEntry& MathUnaryInstr::TargetFunction() const {
  switch (kind()) {
    case MethodRecognizer::kMathSin:
      return kSinRuntimeEntry;
    case MethodRecognizer::kMathCos:
      return kCosRuntimeEntry;
    default:
      UNREACHABLE();
  }
  return kSinRuntimeEntry;
}

#undef __

}  // namespace dart
