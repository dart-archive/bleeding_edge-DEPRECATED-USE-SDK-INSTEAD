// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "vm/globals.h"  // Needed here to get TARGET_ARCH_XXX.

#include "vm/flow_graph_compiler.h"

#include "vm/cha.h"
#include "vm/dart_entry.h"
#include "vm/debugger.h"
#include "vm/deopt_instructions.h"
#include "vm/flow_graph_allocator.h"
#include "vm/il_printer.h"
#include "vm/intrinsifier.h"
#include "vm/locations.h"
#include "vm/longjump.h"
#include "vm/object_store.h"
#include "vm/parser.h"
#include "vm/stub_code.h"
#include "vm/symbols.h"

namespace dart {

DECLARE_FLAG(bool, code_comments);
DECLARE_FLAG(bool, enable_type_checks);
DECLARE_FLAG(bool, intrinsify);
DECLARE_FLAG(bool, propagate_ic_data);
DECLARE_FLAG(bool, report_usage_count);
DECLARE_FLAG(int, optimization_counter_threshold);
DECLARE_FLAG(bool, use_cha);
DECLARE_FLAG(bool, use_osr);


// Assign locations to incoming arguments, i.e., values pushed above spill slots
// with PushArgument.  Recursively allocates from outermost to innermost
// environment.
void CompilerDeoptInfo::AllocateIncomingParametersRecursive(
    Environment* env,
    intptr_t* stack_height) {
  if (env == NULL) return;
  AllocateIncomingParametersRecursive(env->outer(), stack_height);
  for (Environment::ShallowIterator it(env); !it.Done(); it.Advance()) {
    if (it.CurrentLocation().IsInvalid() &&
        it.CurrentValue()->definition()->IsPushArgument()) {
      it.SetCurrentLocation(Location::StackSlot((*stack_height)++));
    }
  }
}


void CompilerDeoptInfo::EmitMaterializations(Environment* env,
                                             DeoptInfoBuilder* builder) {
  for (Environment::DeepIterator it(env); !it.Done(); it.Advance()) {
    if (it.CurrentLocation().IsInvalid()) {
      MaterializeObjectInstr* mat =
          it.CurrentValue()->definition()->AsMaterializeObject();
      ASSERT(mat != NULL);
      builder->AddMaterialization(mat);
    }
  }
}


FlowGraphCompiler::FlowGraphCompiler(Assembler* assembler,
                                     FlowGraph* flow_graph,
                                     bool is_optimizing)
    : assembler_(assembler),
      parsed_function_(flow_graph->parsed_function()),
      flow_graph_(*flow_graph),
      block_order_(*flow_graph->CodegenBlockOrder(is_optimizing)),
      current_block_(NULL),
      exception_handlers_list_(NULL),
      pc_descriptors_list_(NULL),
      stackmap_table_builder_(
          is_optimizing ? new StackmapTableBuilder() : NULL),
      block_info_(block_order_.length()),
      deopt_infos_(),
      static_calls_target_table_(GrowableObjectArray::ZoneHandle(
          GrowableObjectArray::New())),
      is_optimizing_(is_optimizing),
      may_reoptimize_(false),
      double_class_(Class::ZoneHandle(
          Isolate::Current()->object_store()->double_class())),
      float32x4_class_(Class::ZoneHandle(
          Isolate::Current()->object_store()->float32x4_class())),
      int32x4_class_(Class::ZoneHandle(
          Isolate::Current()->object_store()->int32x4_class())),
      list_class_(Class::ZoneHandle(
          Library::Handle(Library::CoreLibrary()).
              LookupClass(Symbols::List()))),
      parallel_move_resolver_(this),
      pending_deoptimization_env_(NULL) {
  ASSERT(assembler != NULL);
  ASSERT(!list_class_.IsNull());
}


bool FlowGraphCompiler::HasFinally() const {
  return parsed_function().function().has_finally();
}


void FlowGraphCompiler::InitCompiler() {
  pc_descriptors_list_ = new DescriptorList(64);
  exception_handlers_list_ = new ExceptionHandlerList();
  block_info_.Clear();
  // Conservative detection of leaf routines used to remove the stack check
  // on function entry.
  bool is_leaf = !parsed_function().function().IsClosureFunction()
      && is_optimizing()
      && !flow_graph().IsCompiledForOsr();
  // Initialize block info and search optimized (non-OSR) code for calls
  // indicating a non-leaf routine and calls without IC data indicating
  // possible reoptimization.
  for (int i = 0; i < block_order_.length(); ++i) {
    block_info_.Add(new BlockInfo());
    if (is_optimizing() && !flow_graph().IsCompiledForOsr()) {
      BlockEntryInstr* entry = block_order_[i];
      for (ForwardInstructionIterator it(entry); !it.Done(); it.Advance()) {
        Instruction* current = it.Current();
        if (current->IsBranch()) {
          current = current->AsBranch()->comparison();
        }
        // In optimized code, ICData is always set in the instructions.
        const ICData* ic_data = NULL;
        if (current->IsInstanceCall()) {
          ic_data = current->AsInstanceCall()->ic_data();
          ASSERT(ic_data != NULL);
        }
        if ((ic_data != NULL) && (ic_data->NumberOfChecks() == 0)) {
          may_reoptimize_ = true;
        }
        if (is_leaf && !current->IsCheckStackOverflow()) {
          // Note that we do not care if the code contains instructions that
          // can deoptimize.
          LocationSummary* locs = current->locs();
          if ((locs != NULL) && locs->can_call()) {
            is_leaf = false;
          }
        }
      }
    }
  }
  if (is_leaf) {
    // Remove the stack overflow check at function entry.
    Instruction* first = flow_graph_.graph_entry()->normal_entry()->next();
    if (first->IsCheckStackOverflow()) first->RemoveFromGraph();
  }
}


bool FlowGraphCompiler::CanOptimize() {
  return !FLAG_report_usage_count &&
         (FLAG_optimization_counter_threshold >= 0);
}


bool FlowGraphCompiler::CanOptimizeFunction() const {
  return CanOptimize() && !parsed_function().function().HasBreakpoint();
}


bool FlowGraphCompiler::CanOSRFunction() const {
  return FLAG_use_osr & CanOptimizeFunction() && !is_optimizing();
}


static bool IsEmptyBlock(BlockEntryInstr* block) {
  return !block->HasParallelMove() &&
         block->next()->IsGoto() &&
         !block->next()->AsGoto()->HasParallelMove();
}


void FlowGraphCompiler::CompactBlock(BlockEntryInstr* block) {
  BlockInfo* block_info = block_info_[block->postorder_number()];

  // Break out of cycles in the control flow graph.
  if (block_info->is_marked()) {
    return;
  }
  block_info->mark();

  if (IsEmptyBlock(block)) {
    // For empty blocks, record a corresponding nonempty target as their
    // jump label.
    BlockEntryInstr* target = block->next()->AsGoto()->successor();
    CompactBlock(target);
    block_info->set_jump_label(GetJumpLabel(target));
  }
}


void FlowGraphCompiler::CompactBlocks() {
  // This algorithm does not garbage collect blocks in place, but merely
  // records forwarding label information.  In this way it avoids having to
  // change join and target entries.
  Label* nonempty_label = NULL;
  for (intptr_t i = block_order().length() - 1; i >= 1; --i) {
    BlockEntryInstr* block = block_order()[i];

    // Unoptimized code must emit all possible deoptimization points.
    if (is_optimizing()) {
      CompactBlock(block);
    }

    // For nonempty blocks, record the next nonempty block in the block
    // order.  Since no code is emitted for empty blocks, control flow is
    // eligible to fall through to the next nonempty one.
    if (!WasCompacted(block)) {
      BlockInfo* block_info = block_info_[block->postorder_number()];
      block_info->set_next_nonempty_label(nonempty_label);
      nonempty_label = GetJumpLabel(block);
    }
  }

  ASSERT(block_order()[0]->IsGraphEntry());
  BlockInfo* block_info = block_info_[block_order()[0]->postorder_number()];
  block_info->set_next_nonempty_label(nonempty_label);
}


void FlowGraphCompiler::EmitInstructionPrologue(Instruction* instr) {
  if (!is_optimizing()) {
    if (FLAG_enable_type_checks && instr->IsAssertAssignable()) {
      AssertAssignableInstr* assert = instr->AsAssertAssignable();
      AddCurrentDescriptor(PcDescriptors::kDeopt,
                           assert->deopt_id(),
                           assert->token_pos());
    } else if (instr->IsGuardField() ||
               (instr->CanBecomeDeoptimizationTarget() && !instr->IsGoto())) {
      // GuardField and instructions that can be deoptimization targets need
      // to record their deopt id.  GotoInstr records its own so that it can
      // control the placement.
      AddCurrentDescriptor(PcDescriptors::kDeopt,
                           instr->deopt_id(),
                           Scanner::kDummyTokenIndex);
    }
    AllocateRegistersLocally(instr);
  } else if (instr->MayThrow()  &&
             (CurrentTryIndex() != CatchClauseNode::kInvalidTryIndex)) {
    // Optimized try-block: Sync locals to fixed stack locations.
    EmitTrySync(instr, CurrentTryIndex());
  }
}


void FlowGraphCompiler::VisitBlocks() {
  CompactBlocks();

  for (intptr_t i = 0; i < block_order().length(); ++i) {
    // Compile the block entry.
    BlockEntryInstr* entry = block_order()[i];
    assembler()->Comment("B%" Pd "", entry->block_id());
    set_current_block(entry);

    if (WasCompacted(entry)) {
      continue;
    }

    entry->EmitNativeCode(this);
    // Compile all successors until an exit, branch, or a block entry.
    for (ForwardInstructionIterator it(entry); !it.Done(); it.Advance()) {
      Instruction* instr = it.Current();
      if (FLAG_code_comments) EmitComment(instr);
      if (instr->IsParallelMove()) {
        parallel_move_resolver_.EmitNativeCode(instr->AsParallelMove());
      } else {
        ASSERT(instr->locs() != NULL);
        EmitInstructionPrologue(instr);
        ASSERT(pending_deoptimization_env_ == NULL);
        pending_deoptimization_env_ = instr->env();
        instr->EmitNativeCode(this);
        pending_deoptimization_env_ = NULL;
        EmitInstructionEpilogue(instr);
      }
    }
  }
  set_current_block(NULL);
}


void FlowGraphCompiler::Bailout(const char* reason) {
  const char* kFormat = "FlowGraphCompiler Bailout: %s %s.";
  const char* function_name = parsed_function().function().ToCString();
  intptr_t len = OS::SNPrint(NULL, 0, kFormat, function_name, reason) + 1;
  char* chars = Isolate::Current()->current_zone()->Alloc<char>(len);
  OS::SNPrint(chars, len, kFormat, function_name, reason);
  const Error& error = Error::Handle(
      LanguageError::New(String::Handle(String::New(chars))));
  Isolate::Current()->long_jump_base()->Jump(1, error);
}


intptr_t FlowGraphCompiler::StackSize() const {
  if (is_optimizing_) {
    return flow_graph_.graph_entry()->spill_slot_count();
  } else {
    return parsed_function_.num_stack_locals() +
        parsed_function_.num_copied_params();
  }
}


Label* FlowGraphCompiler::GetJumpLabel(
    BlockEntryInstr* block_entry) const {
  const intptr_t block_index = block_entry->postorder_number();
  return block_info_[block_index]->jump_label();
}


bool FlowGraphCompiler::WasCompacted(
    BlockEntryInstr* block_entry) const {
  const intptr_t block_index = block_entry->postorder_number();
  return block_info_[block_index]->WasCompacted();
}


bool FlowGraphCompiler::CanFallThroughTo(BlockEntryInstr* block_entry) const {
  const intptr_t current_index = current_block()->postorder_number();
  Label* next_nonempty = block_info_[current_index]->next_nonempty_label();
  return next_nonempty == GetJumpLabel(block_entry);
}


void FlowGraphCompiler::AddSlowPathCode(SlowPathCode* code) {
  slow_path_code_.Add(code);
}


void FlowGraphCompiler::GenerateDeferredCode() {
  for (intptr_t i = 0; i < slow_path_code_.length(); i++) {
    slow_path_code_[i]->EmitNativeCode(this);
  }
  for (intptr_t i = 0; i < deopt_infos_.length(); i++) {
    deopt_infos_[i]->GenerateCode(this, i);
  }
}


void FlowGraphCompiler::AddExceptionHandler(intptr_t try_index,
                                            intptr_t outer_try_index,
                                            intptr_t pc_offset,
                                            const Array& handler_types,
                                            bool needs_stacktrace) {
  exception_handlers_list_->AddHandler(try_index,
                                       outer_try_index,
                                       pc_offset,
                                       handler_types,
                                       needs_stacktrace);
}


void FlowGraphCompiler::SetNeedsStacktrace(intptr_t try_index) {
  exception_handlers_list_->SetNeedsStacktrace(try_index);
}


// Uses current pc position and try-index.
void FlowGraphCompiler::AddCurrentDescriptor(PcDescriptors::Kind kind,
                                             intptr_t deopt_id,
                                             intptr_t token_pos) {
  pc_descriptors_list()->AddDescriptor(kind,
                                       assembler()->CodeSize(),
                                       deopt_id,
                                       token_pos,
                                       CurrentTryIndex());
}


void FlowGraphCompiler::AddStaticCallTarget(const Function& func) {
  ASSERT(Code::kSCallTableEntryLength == 3);
  ASSERT(Code::kSCallTableOffsetEntry == 0);
  static_calls_target_table_.Add(
      Smi::Handle(Smi::New(assembler()->CodeSize())));
  ASSERT(Code::kSCallTableFunctionEntry == 1);
  static_calls_target_table_.Add(func);
  ASSERT(Code::kSCallTableCodeEntry == 2);
  static_calls_target_table_.Add(Code::Handle());
}


void FlowGraphCompiler::AddDeoptIndexAtCall(intptr_t deopt_id,
                                            intptr_t token_pos) {
  ASSERT(is_optimizing());
  CompilerDeoptInfo* info =
      new CompilerDeoptInfo(deopt_id,
                            kDeoptAtCall,
                            pending_deoptimization_env_);
  info->set_pc_offset(assembler()->CodeSize());
  deopt_infos_.Add(info);
}


// This function must be in sync with FlowGraphCompiler::SaveLiveRegisters
// and FlowGraphCompiler::SlowPathEnvironmentFor.
void FlowGraphCompiler::RecordSafepoint(LocationSummary* locs) {
  if (is_optimizing()) {
    BitmapBuilder* bitmap = locs->stack_bitmap();
    ASSERT(bitmap != NULL);
    ASSERT(bitmap->Length() <= StackSize());
    // Pad the bitmap out to describe all the spill slots.
    bitmap->SetLength(StackSize());

    // Mark the bits in the stack map in the same order we push registers in
    // slow path code (see FlowGraphCompiler::SaveLiveRegisters).
    //
    // Slow path code can have registers at the safepoint.
    if (!locs->always_calls()) {
      RegisterSet* regs = locs->live_registers();
      if (regs->FpuRegisterCount() > 0) {
        // Denote FPU registers with 0 bits in the stackmap.  Based on the
        // assumption that there are normally few live FPU registers, this
        // encoding is simpler and roughly as compact as storing a separate
        // count of FPU registers.
        //
        // FPU registers have the highest register number at the highest
        // address (i.e., first in the stackmap).
        const intptr_t kFpuRegisterSpillFactor =
            kFpuRegisterSize / kWordSize;
        for (intptr_t i = kNumberOfFpuRegisters - 1; i >= 0; --i) {
          FpuRegister reg = static_cast<FpuRegister>(i);
          if (regs->ContainsFpuRegister(reg)) {
            for (intptr_t j = 0; j < kFpuRegisterSpillFactor; ++j) {
              bitmap->Set(bitmap->Length(), false);
            }
          }
        }
      }
      // General purpose registers have the lowest register number at the
      // highest address (i.e., first in the stackmap).
      for (intptr_t i = 0; i < kNumberOfCpuRegisters; ++i) {
        Register reg = static_cast<Register>(i);
        if (locs->live_registers()->ContainsRegister(reg)) {
          bitmap->Set(bitmap->Length(), true);
        }
      }
    }

    intptr_t register_bit_count = bitmap->Length() - StackSize();
    stackmap_table_builder_->AddEntry(assembler()->CodeSize(),
                                      bitmap,
                                      register_bit_count);
  }
}


// This function must be in sync with FlowGraphCompiler::RecordSafepoint and
// FlowGraphCompiler::SaveLiveRegisters.
Environment* FlowGraphCompiler::SlowPathEnvironmentFor(
    Instruction* instruction) {
  if (instruction->env() == NULL) return NULL;

  Environment* env = instruction->env()->DeepCopy();
  // 1. Iterate the registers in the order they will be spilled to compute
  //    the slots they will be spilled to.
  intptr_t next_slot = StackSize();
  RegisterSet* regs = instruction->locs()->live_registers();
  intptr_t fpu_reg_slots[kNumberOfFpuRegisters];
  intptr_t cpu_reg_slots[kNumberOfCpuRegisters];
  const intptr_t kFpuRegisterSpillFactor = kFpuRegisterSize / kWordSize;
  // FPU registers are spilled first from highest to lowest register number.
  for (intptr_t i = kNumberOfFpuRegisters - 1; i >= 0; --i) {
    FpuRegister reg = static_cast<FpuRegister>(i);
    if (regs->ContainsFpuRegister(reg)) {
      // We use the lowest address (thus highest index) to identify a
      // multi-word spill slot.
      next_slot += kFpuRegisterSpillFactor;
      fpu_reg_slots[i] = (next_slot - 1);
    } else {
      fpu_reg_slots[i] = -1;
    }
  }
  // General purpose registers are spilled from lowest to highest register
  // number.
  for (intptr_t i = 0; i < kNumberOfCpuRegisters; ++i) {
    Register reg = static_cast<Register>(i);
    if (regs->ContainsRegister(reg)) {
      cpu_reg_slots[i] = next_slot++;
    } else {
      cpu_reg_slots[i] = -1;
    }
  }

  // 2. Iterate the environment and replace register locations with the
  //    corresponding spill slot locations.
  for (Environment::DeepIterator it(env); !it.Done(); it.Advance()) {
    Location loc = it.CurrentLocation();
    if (loc.IsRegister()) {
      intptr_t index = cpu_reg_slots[loc.reg()];
      ASSERT(index >= 0);
      it.SetCurrentLocation(Location::StackSlot(index));
    } else if (loc.IsFpuRegister()) {
      assembler()->Comment("You betcha!");
      intptr_t index = fpu_reg_slots[loc.fpu_reg()];
      ASSERT(index >= 0);
      Value* value = it.CurrentValue();
      switch (value->definition()->representation()) {
        case kUnboxedDouble:
        case kUnboxedMint:
          it.SetCurrentLocation(Location::DoubleStackSlot(index));
          break;
        case kUnboxedFloat32x4:
        case kUnboxedInt32x4:
          it.SetCurrentLocation(Location::QuadStackSlot(index));
          break;
        default:
          UNREACHABLE();
      }
    }
  }

  return env;
}


Label* FlowGraphCompiler::AddDeoptStub(intptr_t deopt_id,
                                       DeoptReasonId reason) {
  ASSERT(is_optimizing_);
  CompilerDeoptInfoWithStub* stub =
      new CompilerDeoptInfoWithStub(deopt_id,
                                    reason,
                                    pending_deoptimization_env_);
  deopt_infos_.Add(stub);
  return stub->entry_label();
}


void FlowGraphCompiler::FinalizeExceptionHandlers(const Code& code) {
  ASSERT(exception_handlers_list_ != NULL);
  const ExceptionHandlers& handlers = ExceptionHandlers::Handle(
      exception_handlers_list_->FinalizeExceptionHandlers(code.EntryPoint()));
  code.set_exception_handlers(handlers);
}


void FlowGraphCompiler::FinalizePcDescriptors(const Code& code) {
  ASSERT(pc_descriptors_list_ != NULL);
  const PcDescriptors& descriptors = PcDescriptors::Handle(
      pc_descriptors_list_->FinalizePcDescriptors(code.EntryPoint()));
  if (!is_optimizing_) descriptors.Verify(parsed_function_.function());
  code.set_pc_descriptors(descriptors);
}


void FlowGraphCompiler::FinalizeDeoptInfo(const Code& code) {
  // For functions with optional arguments, all incoming arguments are copied
  // to spill slots. The deoptimization environment does not track them.
  const Function& function = parsed_function().function();
  const intptr_t incoming_arg_count =
      function.HasOptionalParameters() ? 0 : function.num_fixed_parameters();
  DeoptInfoBuilder builder(incoming_arg_count);

  const Array& array =
      Array::Handle(Array::New(DeoptTable::SizeFor(deopt_infos_.length()),
                               Heap::kOld));
  Smi& offset = Smi::Handle();
  DeoptInfo& info = DeoptInfo::Handle();
  Smi& reason = Smi::Handle();
  for (intptr_t i = 0; i < deopt_infos_.length(); i++) {
    offset = Smi::New(deopt_infos_[i]->pc_offset());
    info = deopt_infos_[i]->CreateDeoptInfo(this, &builder, array);
    reason = Smi::New(deopt_infos_[i]->reason());
    DeoptTable::SetEntry(array, i, offset, info, reason);
  }
  code.set_deopt_info_array(array);
  const Array& object_array =
      Array::Handle(Array::MakeArray(builder.object_table()));
  ASSERT(code.object_table() == Array::null());
  code.set_object_table(object_array);
}


void FlowGraphCompiler::FinalizeStackmaps(const Code& code) {
  if (stackmap_table_builder_ == NULL) {
    // The unoptimizing compiler has no stack maps.
    code.set_stackmaps(Object::null_array());
  } else {
    // Finalize the stack map array and add it to the code object.
    ASSERT(is_optimizing());
    code.set_stackmaps(
        Array::Handle(stackmap_table_builder_->FinalizeStackmaps(code)));
  }
}


void FlowGraphCompiler::FinalizeVarDescriptors(const Code& code) {
  const LocalVarDescriptors& var_descs = LocalVarDescriptors::Handle(
          parsed_function_.node_sequence()->scope()->GetVarDescriptors(
              parsed_function_.function()));
  code.set_var_descriptors(var_descs);
}


void FlowGraphCompiler::FinalizeComments(const Code& code) {
  code.set_comments(assembler()->GetCodeComments());
}


void FlowGraphCompiler::FinalizeStaticCallTargetsTable(const Code& code) {
  ASSERT(code.static_calls_target_table() == Array::null());
  code.set_static_calls_target_table(
      Array::Handle(Array::MakeArray(static_calls_target_table_)));
}


// Returns 'true' if code generation for this function is complete, i.e.,
// no fall-through to regular code is needed.
void FlowGraphCompiler::TryIntrinsify() {
  if (!CanOptimizeFunction()) return;
  // Intrinsification skips arguments checks, therefore disable if in checked
  // mode.
  if (FLAG_intrinsify && !FLAG_enable_type_checks) {
    if (parsed_function().function().kind() == RawFunction::kImplicitGetter) {
      // An implicit getter must have a specific AST structure.
      const SequenceNode& sequence_node = *parsed_function().node_sequence();
      ASSERT(sequence_node.length() == 1);
      ASSERT(sequence_node.NodeAt(0)->IsReturnNode());
      const ReturnNode& return_node = *sequence_node.NodeAt(0)->AsReturnNode();
      ASSERT(return_node.value()->IsLoadInstanceFieldNode());
      const LoadInstanceFieldNode& load_node =
          *return_node.value()->AsLoadInstanceFieldNode();
      GenerateInlinedGetter(load_node.field().Offset());
      return;
    }
    if (parsed_function().function().kind() == RawFunction::kImplicitSetter) {
      // An implicit setter must have a specific AST structure.
      // Sequence node has one store node and one return NULL node.
      const SequenceNode& sequence_node = *parsed_function().node_sequence();
      ASSERT(sequence_node.length() == 2);
      ASSERT(sequence_node.NodeAt(0)->IsStoreInstanceFieldNode());
      ASSERT(sequence_node.NodeAt(1)->IsReturnNode());
      const StoreInstanceFieldNode& store_node =
          *sequence_node.NodeAt(0)->AsStoreInstanceFieldNode();
      if (store_node.field().guarded_cid() == kDynamicCid) {
        GenerateInlinedSetter(store_node.field().Offset());
        return;
      }
    }
  }
  // Even if an intrinsified version of the function was successfully
  // generated, it may fall through to the non-intrinsified method body.
  Intrinsifier::Intrinsify(parsed_function().function(), assembler());
}


void FlowGraphCompiler::GenerateInstanceCall(
    intptr_t deopt_id,
    intptr_t token_pos,
    intptr_t argument_count,
    const Array& argument_names,
    LocationSummary* locs,
    const ICData& ic_data) {
  ASSERT(!ic_data.IsNull());
  ASSERT(FLAG_propagate_ic_data || (ic_data.NumberOfChecks() == 0));
  uword label_address = 0;
  if (is_optimizing() && (ic_data.NumberOfChecks() == 0)) {
    if (ic_data.is_closure_call()) {
      // This IC call may be closure call only.
      label_address = StubCode::ClosureCallInlineCacheEntryPoint();
      ExternalLabel target_label("InlineCache", label_address);
      EmitInstanceCall(&target_label,
                       ICData::ZoneHandle(ic_data.AsUnaryClassChecks()),
                       argument_count, deopt_id, token_pos, locs);
      return;
    }
    // Emit IC call that will count and thus may need reoptimization at
    // function entry.
    ASSERT(!is_optimizing()
           || may_reoptimize()
           || flow_graph().IsCompiledForOsr());
    switch (ic_data.num_args_tested()) {
      case 1:
        label_address = StubCode::OneArgOptimizedCheckInlineCacheEntryPoint();
        break;
      case 2:
        label_address = StubCode::TwoArgsOptimizedCheckInlineCacheEntryPoint();
        break;
      case 3:
        label_address =
            StubCode::ThreeArgsOptimizedCheckInlineCacheEntryPoint();
        break;
      default:
        UNIMPLEMENTED();
    }
    ExternalLabel target_label("InlineCache", label_address);
    EmitOptimizedInstanceCall(&target_label, ic_data,
                              argument_count, deopt_id, token_pos, locs);
    return;
  }

  if (is_optimizing()) {
    EmitMegamorphicInstanceCall(ic_data, argument_count,
                                deopt_id, token_pos, locs);
    return;
  }

  switch (ic_data.num_args_tested()) {
    case 1:
      label_address = StubCode::OneArgCheckInlineCacheEntryPoint();
      break;
    case 2:
      label_address = StubCode::TwoArgsCheckInlineCacheEntryPoint();
      break;
    case 3:
      label_address = StubCode::ThreeArgsCheckInlineCacheEntryPoint();
      break;
    default:
      UNIMPLEMENTED();
  }
  ExternalLabel target_label("InlineCache", label_address);
  EmitInstanceCall(&target_label, ic_data, argument_count,
                   deopt_id, token_pos, locs);
}


void FlowGraphCompiler::GenerateStaticCall(intptr_t deopt_id,
                                           intptr_t token_pos,
                                           const Function& function,
                                           intptr_t argument_count,
                                           const Array& argument_names,
                                           LocationSummary* locs) {
  const Array& arguments_descriptor =
      Array::ZoneHandle(ArgumentsDescriptor::New(argument_count,
                                                 argument_names));
  if (is_optimizing()) {
    EmitOptimizedStaticCall(function, arguments_descriptor, argument_count,
                            deopt_id, token_pos, locs);
  } else {
    EmitUnoptimizedStaticCall(function, arguments_descriptor, argument_count,
                              deopt_id, token_pos, locs);
  }
}


void FlowGraphCompiler::GenerateNumberTypeCheck(Register kClassIdReg,
                                                const AbstractType& type,
                                                Label* is_instance_lbl,
                                                Label* is_not_instance_lbl) {
  assembler()->Comment("NumberTypeCheck");
  GrowableArray<intptr_t> args;
  if (type.IsNumberType()) {
    args.Add(kDoubleCid);
    args.Add(kMintCid);
    args.Add(kBigintCid);
  } else if (type.IsIntType()) {
    args.Add(kMintCid);
    args.Add(kBigintCid);
  } else if (type.IsDoubleType()) {
    args.Add(kDoubleCid);
  }
  CheckClassIds(kClassIdReg, args, is_instance_lbl, is_not_instance_lbl);
}


void FlowGraphCompiler::GenerateStringTypeCheck(Register kClassIdReg,
                                                Label* is_instance_lbl,
                                                Label* is_not_instance_lbl) {
  assembler()->Comment("StringTypeCheck");
  GrowableArray<intptr_t> args;
  args.Add(kOneByteStringCid);
  args.Add(kTwoByteStringCid);
  args.Add(kExternalOneByteStringCid);
  args.Add(kExternalTwoByteStringCid);
  CheckClassIds(kClassIdReg, args, is_instance_lbl, is_not_instance_lbl);
}


void FlowGraphCompiler::GenerateListTypeCheck(Register kClassIdReg,
                                              Label* is_instance_lbl) {
  assembler()->Comment("ListTypeCheck");
  Label unknown;
  GrowableArray<intptr_t> args;
  args.Add(kArrayCid);
  args.Add(kGrowableObjectArrayCid);
  args.Add(kImmutableArrayCid);
  CheckClassIds(kClassIdReg, args, is_instance_lbl, &unknown);
  assembler()->Bind(&unknown);
}


void FlowGraphCompiler::EmitComment(Instruction* instr) {
  char buffer[256];
  BufferFormatter f(buffer, sizeof(buffer));
  instr->PrintTo(&f);
  assembler()->Comment("%s", buffer);
}


// Allocate a register that is not explicitly blocked.
static Register AllocateFreeRegister(bool* blocked_registers) {
  for (intptr_t regno = 0; regno < kNumberOfCpuRegisters; regno++) {
    if (!blocked_registers[regno]) {
      blocked_registers[regno] = true;
      return static_cast<Register>(regno);
    }
  }
  UNREACHABLE();
  return kNoRegister;
}


void FlowGraphCompiler::AllocateRegistersLocally(Instruction* instr) {
  ASSERT(!is_optimizing());

  LocationSummary* locs = instr->locs();

  bool blocked_registers[kNumberOfCpuRegisters];

  // Mark all available registers free.
  for (intptr_t i = 0; i < kNumberOfCpuRegisters; i++) {
    blocked_registers[i] = false;
  }

  // Mark all fixed input, temp and output registers as used.
  for (intptr_t i = 0; i < locs->input_count(); i++) {
    Location loc = locs->in(i);
    if (loc.IsRegister()) {
      // Check that a register is not specified twice in the summary.
      ASSERT(!blocked_registers[loc.reg()]);
      blocked_registers[loc.reg()] = true;
    }
  }

  for (intptr_t i = 0; i < locs->temp_count(); i++) {
    Location loc = locs->temp(i);
    if (loc.IsRegister()) {
      // Check that a register is not specified twice in the summary.
      ASSERT(!blocked_registers[loc.reg()]);
      blocked_registers[loc.reg()] = true;
    }
  }

  if (locs->out().IsRegister()) {
    // Fixed output registers are allowed to overlap with
    // temps and inputs.
    blocked_registers[locs->out().reg()] = true;
  }

  // Do not allocate known registers.
  blocked_registers[CTX] = true;
  blocked_registers[SPREG] = true;
  blocked_registers[FPREG] = true;
  if (TMP != kNoRegister) {
    blocked_registers[TMP] = true;
  }
  if (PP != kNoRegister) {
    blocked_registers[PP] = true;
  }

  // Block all non-free registers.
  for (intptr_t i = 0; i < kFirstFreeCpuRegister; i++) {
    blocked_registers[i] = true;
  }
  for (intptr_t i = kLastFreeCpuRegister + 1; i < kNumberOfCpuRegisters; i++) {
    blocked_registers[i] = true;
  }

  // Allocate all unallocated input locations.
  const bool should_pop = !instr->IsPushArgument() && !instr->IsPushTemp();
  for (intptr_t i = locs->input_count() - 1; i >= 0; i--) {
    Location loc = locs->in(i);
    Register reg = kNoRegister;
    if (loc.IsRegister()) {
      reg = loc.reg();
    } else if (loc.IsUnallocated() || loc.IsConstant()) {
      ASSERT(loc.IsConstant() ||
             ((loc.policy() == Location::kRequiresRegister) ||
              (loc.policy() == Location::kWritableRegister) ||
              (loc.policy() == Location::kAny)));
      reg = AllocateFreeRegister(blocked_registers);
      locs->set_in(i, Location::RegisterLocation(reg));
    }
    ASSERT(reg != kNoRegister);

    // Inputs are consumed from the simulated frame. In case of a call argument
    // we leave it until the call instruction.
    if (should_pop) {
      assembler()->PopRegister(reg);
    }
  }

  // Allocate all unallocated temp locations.
  for (intptr_t i = 0; i < locs->temp_count(); i++) {
    Location loc = locs->temp(i);
    if (loc.IsUnallocated()) {
      ASSERT(loc.policy() == Location::kRequiresRegister);
      loc = Location::RegisterLocation(
        AllocateFreeRegister(blocked_registers));
      locs->set_temp(i, loc);
    }
  }

  Location result_location = locs->out();
  if (result_location.IsUnallocated()) {
    switch (result_location.policy()) {
      case Location::kAny:
      case Location::kPrefersRegister:
      case Location::kRequiresRegister:
      case Location::kWritableRegister:
        result_location = Location::RegisterLocation(
            AllocateFreeRegister(blocked_registers));
        break;
      case Location::kSameAsFirstInput:
        result_location = locs->in(0);
        break;
      case Location::kRequiresFpuRegister:
        UNREACHABLE();
        break;
    }
    locs->set_out(result_location);
  }
}


ParallelMoveResolver::ParallelMoveResolver(FlowGraphCompiler* compiler)
    : compiler_(compiler), moves_(32) {}


void ParallelMoveResolver::EmitNativeCode(ParallelMoveInstr* parallel_move) {
  ASSERT(moves_.is_empty());
  // Build up a worklist of moves.
  BuildInitialMoveList(parallel_move);

  for (int i = 0; i < moves_.length(); ++i) {
    const MoveOperands& move = *moves_[i];
    // Skip constants to perform them last.  They don't block other moves
    // and skipping such moves with register destinations keeps those
    // registers free for the whole algorithm.
    if (!move.IsEliminated() && !move.src().IsConstant()) PerformMove(i);
  }

  // Perform the moves with constant sources.
  for (int i = 0; i < moves_.length(); ++i) {
    const MoveOperands& move = *moves_[i];
    if (!move.IsEliminated()) {
      ASSERT(move.src().IsConstant());
      EmitMove(i);
    }
  }

  moves_.Clear();
}


void ParallelMoveResolver::BuildInitialMoveList(
    ParallelMoveInstr* parallel_move) {
  // Perform a linear sweep of the moves to add them to the initial list of
  // moves to perform, ignoring any move that is redundant (the source is
  // the same as the destination, the destination is ignored and
  // unallocated, or the move was already eliminated).
  for (int i = 0; i < parallel_move->NumMoves(); i++) {
    MoveOperands* move = parallel_move->MoveOperandsAt(i);
    if (!move->IsRedundant()) moves_.Add(move);
  }
}


void ParallelMoveResolver::PerformMove(int index) {
  // Each call to this function performs a move and deletes it from the move
  // graph.  We first recursively perform any move blocking this one.  We
  // mark a move as "pending" on entry to PerformMove in order to detect
  // cycles in the move graph.  We use operand swaps to resolve cycles,
  // which means that a call to PerformMove could change any source operand
  // in the move graph.

  ASSERT(!moves_[index]->IsPending());
  ASSERT(!moves_[index]->IsRedundant());

  // Clear this move's destination to indicate a pending move.  The actual
  // destination is saved in a stack-allocated local.  Recursion may allow
  // multiple moves to be pending.
  ASSERT(!moves_[index]->src().IsInvalid());
  Location destination = moves_[index]->MarkPending();

  // Perform a depth-first traversal of the move graph to resolve
  // dependencies.  Any unperformed, unpending move with a source the same
  // as this one's destination blocks this one so recursively perform all
  // such moves.
  for (int i = 0; i < moves_.length(); ++i) {
    const MoveOperands& other_move = *moves_[i];
    if (other_move.Blocks(destination) && !other_move.IsPending()) {
      // Though PerformMove can change any source operand in the move graph,
      // this call cannot create a blocking move via a swap (this loop does
      // not miss any).  Assume there is a non-blocking move with source A
      // and this move is blocked on source B and there is a swap of A and
      // B.  Then A and B must be involved in the same cycle (or they would
      // not be swapped).  Since this move's destination is B and there is
      // only a single incoming edge to an operand, this move must also be
      // involved in the same cycle.  In that case, the blocking move will
      // be created but will be "pending" when we return from PerformMove.
      PerformMove(i);
    }
  }

  // We are about to resolve this move and don't need it marked as
  // pending, so restore its destination.
  moves_[index]->ClearPending(destination);

  // This move's source may have changed due to swaps to resolve cycles and
  // so it may now be the last move in the cycle.  If so remove it.
  if (moves_[index]->src().Equals(destination)) {
    moves_[index]->Eliminate();
    return;
  }

  // The move may be blocked on a (at most one) pending move, in which case
  // we have a cycle.  Search for such a blocking move and perform a swap to
  // resolve it.
  for (int i = 0; i < moves_.length(); ++i) {
    const MoveOperands& other_move = *moves_[i];
    if (other_move.Blocks(destination)) {
      ASSERT(other_move.IsPending());
      EmitSwap(index);
      return;
    }
  }

  // This move is not blocked.
  EmitMove(index);
}


bool ParallelMoveResolver::IsScratchLocation(Location loc) {
  for (int i = 0; i < moves_.length(); ++i) {
    if (moves_[i]->Blocks(loc)) {
      return false;
    }
  }

  for (int i = 0; i < moves_.length(); ++i) {
    if (moves_[i]->dest().Equals(loc)) {
      return true;
    }
  }

  return false;
}


intptr_t ParallelMoveResolver::AllocateScratchRegister(Location::Kind kind,
                                                       intptr_t blocked,
                                                       intptr_t register_count,
                                                       bool* spilled) {
  intptr_t scratch = -1;
  for (intptr_t reg = 0; reg < register_count; reg++) {
    if ((blocked != reg) &&
        IsScratchLocation(Location::MachineRegisterLocation(kind, reg))) {
      scratch = reg;
      break;
    }
  }

  if (scratch == -1) {
    *spilled = true;
    for (intptr_t reg = 0; reg < register_count; reg++) {
      if (blocked != reg) {
        scratch = reg;
      }
    }
  } else {
    *spilled = false;
  }

  return scratch;
}


ParallelMoveResolver::ScratchFpuRegisterScope::ScratchFpuRegisterScope(
    ParallelMoveResolver* resolver, FpuRegister blocked)
    : resolver_(resolver),
      reg_(kNoFpuRegister),
      spilled_(false) {
  reg_ = static_cast<FpuRegister>(
      resolver_->AllocateScratchRegister(Location::kFpuRegister,
                                         blocked,
                                         kNumberOfFpuRegisters,
                                         &spilled_));

  if (spilled_) {
    resolver->SpillFpuScratch(reg_);
  }
}


ParallelMoveResolver::ScratchFpuRegisterScope::~ScratchFpuRegisterScope() {
  if (spilled_) {
    resolver_->RestoreFpuScratch(reg_);
  }
}


ParallelMoveResolver::ScratchRegisterScope::ScratchRegisterScope(
    ParallelMoveResolver* resolver, Register blocked)
    : resolver_(resolver),
      reg_(kNoRegister),
      spilled_(false) {
  reg_ = static_cast<Register>(
      resolver_->AllocateScratchRegister(Location::kRegister,
                                         blocked,
                                         kNumberOfCpuRegisters,
                                         &spilled_));

  if (spilled_) {
    resolver->SpillScratch(reg_);
  }
}


ParallelMoveResolver::ScratchRegisterScope::~ScratchRegisterScope() {
  if (spilled_) {
    resolver_->RestoreScratch(reg_);
  }
}


intptr_t FlowGraphCompiler::ElementSizeFor(intptr_t cid) {
  if (RawObject::IsExternalTypedDataClassId(cid)) {
    return ExternalTypedData::ElementSizeInBytes(cid);
  } else if (RawObject::IsTypedDataClassId(cid)) {
    return TypedData::ElementSizeInBytes(cid);
  }
  switch (cid) {
    case kArrayCid:
    case kImmutableArrayCid:
      return Array::kBytesPerElement;
    case kOneByteStringCid:
      return OneByteString::kBytesPerElement;
    case kTwoByteStringCid:
      return TwoByteString::kBytesPerElement;
    default:
      UNIMPLEMENTED();
      return 0;
  }
}


intptr_t FlowGraphCompiler::DataOffsetFor(intptr_t cid) {
  if (RawObject::IsExternalTypedDataClassId(cid)) {
    // Elements start at offset 0 of the external data.
    return 0;
  }
  if (RawObject::IsTypedDataClassId(cid)) {
    return TypedData::data_offset();
  }
  switch (cid) {
    case kArrayCid:
    case kImmutableArrayCid:
      return Array::data_offset();
    case kOneByteStringCid:
      return OneByteString::data_offset();
    case kTwoByteStringCid:
      return TwoByteString::data_offset();
    default:
      UNIMPLEMENTED();
      return Array::data_offset();
  }
}


// Returns true if checking against this type is a direct class id comparison.
bool FlowGraphCompiler::TypeCheckAsClassEquality(const AbstractType& type) {
  ASSERT(type.IsFinalized() && !type.IsMalformed() && !type.IsMalbounded());
  // Requires CHA, which can be applied in optimized code only,
  if (!FLAG_use_cha || !is_optimizing()) return false;
  if (!type.IsInstantiated()) return false;
  const Class& type_class = Class::Handle(type.type_class());
  // Signature classes have different type checking rules.
  if (type_class.IsSignatureClass()) return false;
  // Could be an interface check?
  if (type_class.is_implemented()) return false;
  const intptr_t type_cid = type_class.id();
  if (CHA::HasSubclasses(type_cid)) return false;
  if (type_class.NumTypeArguments() > 0) {
    // Only raw types can be directly compared, thus disregarding type
    // arguments.
    const AbstractTypeArguments& type_arguments =
        AbstractTypeArguments::Handle(type.arguments());
    const bool is_raw_type = type_arguments.IsNull() ||
        type_arguments.IsRaw(type_arguments.Length());
    return is_raw_type;
  }
  return true;
}


static int HighestCountFirst(const CidTarget* a, const CidTarget* b) {
  // Negative if 'a' should sort before 'b'.
  return b->count - a->count;
}


// Returns 'sorted' array in decreasing count order.
// The expected number of elements to sort is less than 10.
void FlowGraphCompiler::SortICDataByCount(const ICData& ic_data,
                                          GrowableArray<CidTarget>* sorted) {
  ASSERT(ic_data.num_args_tested() == 1);
  const intptr_t len = ic_data.NumberOfChecks();
  sorted->Clear();

  for (int i = 0; i < len; i++) {
    sorted->Add(CidTarget(ic_data.GetReceiverClassIdAt(i),
                          &Function::ZoneHandle(ic_data.GetTargetAt(i)),
                          ic_data.GetCountAt(i)));
  }
  sorted->Sort(HighestCountFirst);
}

}  // namespace dart
