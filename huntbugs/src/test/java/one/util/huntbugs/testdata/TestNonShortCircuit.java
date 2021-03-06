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
package one.util.huntbugs.testdata;

import one.util.huntbugs.registry.anno.AssertNoWarning;
import one.util.huntbugs.registry.anno.AssertWarning;

/**
 * @author Tagir Valeev
 *
 */
public class TestNonShortCircuit {
    boolean f = true;
    static boolean sf = true;
    
    @AssertNoWarning("NonShortCircuit*")
    public int testNormal(int a, int b) {
        if(a > 0 && b > 0)
            return 1;
        if((a & b) > 0)
            return 2;
        if(a > 0 || b > 0)
            return 3;
        if((a | b) > 0)
            return 4;
        boolean x = a > 0;
        x |= b > 0;
        if(x |= b > 0) {
            return 1;
        }
        boolean[] bb = {true, false};
        bb[0] |= x;
        f |= x;
        sf |= x;
        a = (f ? 1 : 0) | (b << 1) | (b << 2);
        return 5+(x?2:3);
    }
    
    @AssertWarning(value="NonShortCircuit", minScore=40, maxScore=60) 
    public int testAnd(int a, int b) {
        if(a > 0 & b > 0)
            return 1;
        return 2;
    }
    
    @AssertWarning(value="NonShortCircuit", minScore=40, maxScore=60) 
    public int testOr(int a, int b) {
        if(a > 0 | b > 0)
            return 1;
        return 2;
    }
    
    @AssertNoWarning("NonShortCircuit") 
    @AssertWarning(value="NonShortCircuitDangerous", minScore=40, maxScore=60) 
    public int testOrBoxing(Integer a, Integer b) {
        if(a > 0 | ++b > 0)
            return 1;
        return 2;
    }
    
    @AssertWarning(value="NonShortCircuit", minScore=40, maxScore=60) 
    public int testOrBoxing2(Integer a, Integer b) {
        if(++b > 0 | a > 0)
            return 1;
        return 2;
    }
    
    @AssertWarning(value="NonShortCircuitDangerous", minScore = 60, maxScore = 70)
    @AssertNoWarning("NonShortCircuit") 
    public int testMethod(Integer a, Integer b) {
        if(testOrBoxing(a, a) > 0 | ++b > 0)
            return 1;
        return 2;
    }
    
    @AssertWarning(value="NonShortCircuitDangerous", minScore=75) 
    @AssertNoWarning("NonShortCircuit") 
    public int testNull(String s) {
        if(s != null & s.length() > 2)
            return 1;
        return 2;
    }
    
    @AssertWarning(value="NonShortCircuitDangerous", minScore=75) 
    @AssertNoWarning("NonShortCircuit") 
    public int testNullOr(String s) {
        if(s == null | s.length() > 2)
            return 1;
        return 2;
    }
    
    @AssertWarning(value="NonShortCircuitDangerous", minScore=75) 
    @AssertNoWarning("NonShortCircuit") 
    public int testInstanceOf(Object s) {
        if(s instanceof String & ((String)s).length() > 2)
            return 1;
        return 2;
    }
    
    @AssertWarning("NonShortCircuit") 
    @AssertNoWarning("NonShortCircuitDangerous") 
    public int testInstanceOf(Object s, boolean b) {
        if(s instanceof String & b)
            return 1;
        return 2;
    }
}
