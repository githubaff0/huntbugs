/*
 * Copyright 2016 HuntBugs contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package one.util.huntbugs.detect;

import java.util.Set;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.CatchBlock;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Variable;

import one.util.huntbugs.flow.ValuesFlow;
import one.util.huntbugs.registry.MethodContext;
import one.util.huntbugs.registry.anno.AstNodes;
import one.util.huntbugs.registry.anno.AstVisitor;
import one.util.huntbugs.registry.anno.WarningDefinition;
import one.util.huntbugs.util.NodeChain;
import one.util.huntbugs.util.Nodes;
import one.util.huntbugs.util.Types;
import one.util.huntbugs.warning.Role.StringRole;

/**
 * @author Tagir Valeev
 *
 */
@WarningDefinition(category="RedundantCode", name="DeadStoreInReturn", maxScore=50)
@WarningDefinition(category="RedundantCode", name="DeadIncrementInReturn", maxScore=60)
@WarningDefinition(category="RedundantCode", name="DeadIncrementInAssignment", maxScore=60)
@WarningDefinition(category="RedundantCode", name="DeadParameterStore", maxScore=60)
@WarningDefinition(category="RedundantCode", name="DeadLocalStore", maxScore=50)
@WarningDefinition(category="RedundantCode", name="UnusedLocalVariable", maxScore=50)
public class DeadLocalStore {
    private static final StringRole EXPRESSION = StringRole.forName("EXPRESSION");
    
    @AstVisitor(nodes=AstNodes.EXPRESSIONS)
    public void visit(Expression expr, NodeChain nc, MethodContext mc) {
        if(expr.getCode() == AstCode.Return && expr.getArguments().size() == 1) {
            Expression arg = expr.getArguments().get(0);
            if(arg.getCode() == AstCode.Store) {
                mc.report("DeadStoreInReturn", 0, arg);
            } else if(arg.getCode() == AstCode.PreIncrement || arg.getCode() == AstCode.PostIncrement) {
                Expression var = arg.getArguments().get(0);
                if(var.getOperand() instanceof Variable)
                    mc.report("DeadIncrementInReturn", 0, var);
            }
        }
        if(expr.getCode() == AstCode.Store) {
            Variable var = (Variable) expr.getOperand();
            Expression arg = expr.getArguments().get(0);
            if(arg.getCode() == AstCode.PostIncrement) { // XXX: bug in Procyon? Seems that should be PreIncrement
                Expression load = arg.getArguments().get(0);
                if(load.getCode() == AstCode.Load && var.equals(load.getOperand()) && Integer.valueOf(1).equals(arg.getOperand())) {
                    mc.report("DeadIncrementInAssignment", 0, expr, EXPRESSION.create(var.getName() + " = " + var
                            .getName() + "++"));
                }
            }
            // exclude AstCode.Store (chaining assignment) as procyon sometimes reorders locals assignments
            if (mc.isAnnotated() && ValuesFlow.getSource(arg) == arg && arg.getCode() != AstCode.Store
                && arg.getCode() != AstCode.PutField && arg.getCode() != AstCode.PutStatic) {
                Set<Expression> usages = ValuesFlow.findUsages(arg);
                if(usages.size() == 1 && usages.iterator().next() == expr) {
                    Set<Expression> storeUsages = ValuesFlow.findUsages(expr);
                    if(storeUsages.isEmpty()) {
                        if(nc.getNode() instanceof CatchBlock && nc.getNode().getChildren().get(0) == expr
                                && ((CatchBlock)nc.getNode()).getCaughtTypes().size() > 1) {
                            // Exception variable in multi-catch block
                            return;
                        }
                        int priority = 0;
                        if(arg.getCode() == AstCode.LdC && !var.isParameter()) {
                            Object val = arg.getOperand();
                            TypeReference tr = arg.getInferredType();
                            if(tr != null && (tr.isPrimitive() || Types.isString(tr))) {
                                if (Nodes.find(nc.getRoot(), n -> n != expr && n instanceof Expression
                                        && ((Expression) n).getOperand() == var) == null
                                    && Nodes.find(nc.getRoot(), n -> n != arg && n instanceof Expression
                                        && val.equals(((Expression) n).getOperand())) != null)
                                    return;
                            }
                        }
                        if(var.isParameter()) {
                            mc.report("DeadParameterStore", priority, expr);
                        } else {
                            boolean unusedLocal = Nodes.find(nc.getRoot(), n -> n instanceof Expression
                                && ((Expression) n).getOperand() == var && ((Expression) n).getCode() != AstCode.Store) == null;
                            if(arg.getCode() == AstCode.AConstNull || arg.getCode() == AstCode.LdC &&
                                    arg.getOperand() instanceof Number && ((Number)arg.getOperand()).doubleValue() == 0.0)
                                priority = 20;
                            String type = unusedLocal ? "UnusedLocalVariable" : "DeadLocalStore";
                            mc.report(type, priority, expr);
                        }
                    }
                }
            }
        }
    }
}
