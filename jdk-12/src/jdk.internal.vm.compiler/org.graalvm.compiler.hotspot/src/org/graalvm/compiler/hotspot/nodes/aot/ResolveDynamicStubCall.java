/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */


package org.graalvm.compiler.hotspot.nodes.aot;

import static org.graalvm.compiler.nodeinfo.NodeCycles.CYCLES_UNKNOWN;
import static org.graalvm.compiler.nodeinfo.NodeSize.SIZE_16;

import org.graalvm.compiler.debug.GraalError;
import org.graalvm.compiler.graph.Node;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.graph.spi.Canonicalizable;
import org.graalvm.compiler.graph.spi.CanonicalizerTool;
import org.graalvm.compiler.hotspot.HotSpotLIRGenerator;
import org.graalvm.compiler.lir.LIRFrameState;
import org.graalvm.compiler.nodeinfo.InputType;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.DeoptimizingNode;
import org.graalvm.compiler.nodes.FrameState;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.compiler.nodes.util.GraphUtil;
import org.graalvm.compiler.nodes.memory.AbstractMemoryCheckpoint;
import org.graalvm.compiler.nodes.memory.MemoryCheckpoint;
import jdk.internal.vm.compiler.word.LocationIdentity;

import jdk.vm.ci.meta.Constant;
import jdk.vm.ci.meta.Value;

/**
 * A call to the VM via a regular stub.
 */
@NodeInfo(allowedUsageTypes = {InputType.Memory}, cycles = CYCLES_UNKNOWN, size = SIZE_16)
public class ResolveDynamicStubCall extends AbstractMemoryCheckpoint implements LIRLowerable, Canonicalizable, DeoptimizingNode.DeoptBefore, MemoryCheckpoint.Single {
    public static final NodeClass<ResolveDynamicStubCall> TYPE = NodeClass.create(ResolveDynamicStubCall.class);

    @OptionalInput protected ValueNode value;
    @OptionalInput(InputType.State) protected FrameState stateBefore;
    protected Constant constant;

    public ResolveDynamicStubCall(ValueNode value) {
        super(TYPE, value.stamp(NodeView.DEFAULT));
        this.value = value;
    }

    @NodeIntrinsic
    public static native Object resolveInvoke(Object value);

    @Override
    public Node canonical(CanonicalizerTool tool) {
        if (value != null) {
            constant = GraphUtil.foldIfConstantAndRemove(this, value);
        }
        return this;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        assert constant != null : "Expected the value to fold: " + value;
        Value result;
        LIRFrameState fs = gen.state(this);
        assert fs != null : "The stateAfter is null";
        result = ((HotSpotLIRGenerator) gen.getLIRGeneratorTool()).emitResolveDynamicInvoke(constant, fs);
        gen.setResult(this, result);
    }

    @Override
    public boolean canDeoptimize() {
        return true;
    }

    @Override
    public LocationIdentity getLocationIdentity() {
        return LocationIdentity.any();
    }

    @Override
    public FrameState stateBefore() {
        return stateBefore;
    }

    @Override
    public void setStateBefore(FrameState f) {
        updateUsages(stateBefore, f);
        stateBefore = f;
    }

    @Override
    public void markDeleted() {
        throw GraalError.shouldNotReachHere("ResolveDynamicStubCall node deleted");
    }

}