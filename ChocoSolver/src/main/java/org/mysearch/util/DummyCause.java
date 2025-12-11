package org.mysearch.util;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.exception.ContradictionException;

public enum DummyCause implements ICause {
    INSTANCE;
}
