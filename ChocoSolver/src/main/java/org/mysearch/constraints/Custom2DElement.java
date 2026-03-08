package org.mysearch.constraints;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;

public final class Custom2DElement extends Constraint {

        public Custom2DElement(int[][] matrix, IntVar x_i, IntVar x_j, IntVar value) {
            super("Custom2DElement", new Prop2DElementGAC_v2(matrix, x_i, x_j, value));
        }
}
