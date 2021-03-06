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

import java.util.List;

import com.strobel.assembler.metadata.TypeReference;
import com.strobel.decompiler.ast.AstCode;
import com.strobel.decompiler.ast.CatchBlock;
import com.strobel.decompiler.ast.Expression;
import com.strobel.decompiler.ast.Node;
import com.strobel.decompiler.ast.Variable;

import one.util.huntbugs.registry.MethodContext;
import one.util.huntbugs.registry.anno.AstVisitor;
import one.util.huntbugs.registry.anno.WarningDefinition;
import one.util.huntbugs.util.Exprs;
import one.util.huntbugs.util.Nodes;
import one.util.huntbugs.warning.Roles;

/**
 * @author lan
 *
 */
@WarningDefinition(category="BadPractice", name="IgnoredException", maxScore=43)
public class IgnoredException {
    @AstVisitor
    public void visit(Node node, MethodContext mc) {
        if(node instanceof CatchBlock) {
            CatchBlock cb = (CatchBlock)node;
            TypeReference tr = cb.getExceptionType();
            if(tr != null && isTrivial(cb.getBody(), cb.getExceptionVariable())) {
                int priority = getExceptionPriority(tr);
                if(priority >= 0) {
                    mc.report("IgnoredException", priority, node, Roles.EXCEPTION.create(tr));
                }
            }
        }
    }
    
    private static int getExceptionPriority(TypeReference tr) {
        switch(tr.getInternalName()) {
        case "java/lang/Throwable":
            return 0;
        case "java/lang/Error":
            return 1;
        case "java/lang/Exception":
            return 5;
        case "java/lang/RuntimeException":
            return 7;
        default:
            return -1;
        }
    }

    private boolean isTrivial(List<Node> body, Variable variable) {
        if(body.isEmpty())
            return true;
        if(body.size() == 1) {
            Node node = body.get(0);
            if(Nodes.isOp(node, AstCode.LoopContinue) || Nodes.isOp(node, AstCode.LoopOrSwitchBreak))
                return true;
            if(Nodes.isOp(node, AstCode.Return)) {
                return Exprs.stream((Expression) node).noneMatch(e -> e.getOperand() == variable);
            }
        }
        return false;
    }
}
