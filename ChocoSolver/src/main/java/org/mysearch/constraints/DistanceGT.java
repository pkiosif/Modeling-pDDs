package org.mysearch.constraints;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.IntVar;
import org.mysearch.util.SharedBest;

/** Convenience wrapper to post PropDistanceGT_AC3. */

public final class DistanceGT extends Constraint {
    public DistanceGT(IntVar F1, IntVar F2, IntVar minDist, int[][] dist, int d_lb) {
        super("DistanceGT", new PropDistanceGT_OBJ_AC3_v2(F1, F2, minDist, dist, d_lb));
    }
    public DistanceGT(IntVar F1, IntVar F2, SharedBest minDist, int[][] dist, int d_lb) {
        super("DistanceGT", new PropDistanceGT_OBJ_AC3_v2_BIN(F1, F2, minDist, dist, d_lb));
    }
    public DistanceGT(IntVar F1, IntVar F2, int[][] dist, int d_lb) {
        super("DistanceGT", new PropDistanceGT_AC3_v2(F1, F2, dist, d_lb));
    }
}
