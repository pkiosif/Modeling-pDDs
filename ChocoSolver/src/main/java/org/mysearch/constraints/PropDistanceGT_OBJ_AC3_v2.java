package org.mysearch.constraints;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

import java.util.BitSet;

/**
 * Enforces: distanceMatrix[F1][F2] >= max(minDist, d_lb + 1)
 * AC-3 style over two vars (F1,F2), plus listening to minDist bound increases.
 * Values of F1, F2 are indices in 0..P-1.
 *
 * Note: Baseline precomputed supports are for GE-threshold baseGE = d_lb + 1.
 */
public final class PropDistanceGT_OBJ_AC3_v2 extends Propagator<IntVar> {
    private final IntVar F1, F2, minDist;
    private final int[][] distanceMatrix;
    private final int d_lb;         // static per-pair strict lower bound; threshold is d_lb+1 here
    private final int P;

    // Precomputed supports for the static base threshold (>= d_lb + 1):
    private final BitSet[] supF1;   // supF1[a] = { b | dist[a][b] >= d_lb+1 }
    private final BitSet[] supF2;   // supF2[b] = { a | dist[a][b] >= d_lb+1 }

    public PropDistanceGT_OBJ_AC3_v2(IntVar F1, IntVar F2, IntVar minDist, int[][] dist, int d_lb) {
        // Scope = F1, F2, minDist
        super(new IntVar[]{F1, F2, minDist}, PropagatorPriority.TERNARY, false);
        this.F1 = F1;
        this.F2 = F2;
        this.minDist = minDist;
        this.distanceMatrix = dist;
        this.d_lb = d_lb;
        this.P = dist.length;

        // Precompute supports at baseGE = d_lb + 1  (since integers: > d_lb <=> >= d_lb+1)
        int baseGE = d_lb + 1;
        this.supF1 = new BitSet[P];
        this.supF2 = new BitSet[P];
        for (int a = 0; a < P; a++) supF1[a] = new BitSet(P);
        for (int b = 0; b < P; b++) supF2[b] = new BitSet(P);
        for (int a = 0; a < P; a++) {
            for (int b = 0; b < P; b++) {
                if (dist[a][b] >= baseGE) {
                    supF1[a].set(b);
                    supF2[b].set(a);
                }
            }
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        if (vIdx <= 1) {
            return IntEventType.REMOVE.getMask()
                    | IntEventType.BOUND.getMask()
                    | IntEventType.INSTANTIATE.getMask();
        } else {
            // Mainly care about LB increases of minDist; BOUND covers it.
            return IntEventType.BOUND.getMask()
                    | IntEventType.INSTANTIATE.getMask();
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        boolean changed;
        do {
            changed = false;
            // GE semantics: current threshold is max(minDist.LB, d_lb+1)
            final int thrGE = Math.max(minDist.getLB(), d_lb + 1);


            changed |= reviseF1wrtF2(thrGE);
            changed |= reviseF2wrtF1(thrGE);

            // Tighten minDist.UB using this pair
            if (F1.isInstantiated() && F2.isInstantiated()) {
                int d = distanceMatrix[F1.getValue()][F2.getValue()];
                // GE semantics ⇒ UB is d (NOT d-1)
                minDist.updateUpperBound(d, this);
            } else {
                // minDist ≤ max_{a∈Dom(F1), b∈Dom(F2)} dist[a][b]
                int ubPair = maxDistanceOverDomains();
                minDist.updateUpperBound(ubPair, this);
            }

            // Do NOT passivate here; minDist.LB rises during optimization.
        } while (changed);
    }

    private boolean reviseF1wrtF2(int thrGE) throws ContradictionException {
        boolean removed = false;
        if (F1.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                if (!hasSupportAinF2(a, thrGE)) {
                    F1.removeValue(a, this);
                    removed = true;
                }
            }
        } else {
            for (int a = F1.getLB(); a <= F1.getUB(); a++) {
                if (F1.contains(a) && !hasSupportAinF2(a, thrGE)) {
                    F1.removeValue(a, this);
                    removed = true;
                }
            }
        }
        return removed;
    }

    private boolean reviseF2wrtF1(int thrGE) throws ContradictionException {
        boolean removed = false;
        if (F2.hasEnumeratedDomain()) {
            for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                if (!hasSupportBinF1(b, thrGE)) {
                    F2.removeValue(b, this);
                    removed = true;
                }
            }
        } else {
            for (int b = F2.getLB(); b <= F2.getUB(); b++) {
                if (F2.contains(b) && !hasSupportBinF1(b, thrGE)) {
                    F2.removeValue(b, this);
                    removed = true;
                }
            }
        }
        return removed;
    }

    /** Support check for value 'a' in F1 against current Dom(F2) and threshold 'thrGE' (>=). */
    private boolean hasSupportAinF2(int a, int thrGE) {
        int baseGE = d_lb + 1;
        if (thrGE == baseGE && F2.hasEnumeratedDomain()) {

            for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                if (supF1[a].get(b)) return true;
            }
            return false;
        }

        if (F2.hasEnumeratedDomain()) {
            for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                if (distanceMatrix[a][b] >= thrGE) return true;
            }
            return false;
        } else {
            int lb = F2.getLB(), ub = F2.getUB();
            for (int b = lb; b <= ub; b++) {
                if (F2.contains(b) && distanceMatrix[a][b] >= thrGE) return true;
            }
            return false;
        }
    }

    /** Support check for value 'b' in F2 against current Dom(F1) and threshold 'thrGE' (>=). */
    private boolean hasSupportBinF1(int b, int thrGE) {
        int baseGE = d_lb + 1;
        if (thrGE == baseGE && F1.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                if (supF2[b].get(a)) return true;
            }
            return false;
        }
        if (F1.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                if (distanceMatrix[a][b] >= thrGE) return true;
            }
            return false;
        } else {
            int lb = F1.getLB(), ub = F1.getUB();
            for (int a = lb; a <= ub; a++) {
                if (F1.contains(a) && distanceMatrix[a][b] >= thrGE) return true;
            }
            return false;
        }
    }

    private int maxDistanceOverDomains() {
        int best = Integer.MIN_VALUE;
        if (F1.hasEnumeratedDomain() && F2.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                    int d = distanceMatrix[a][b];
                    if (d > best) best = d;
                }
            }
        } else {
            int lb1 = F1.getLB(), ub1 = F1.getUB();
            int lb2 = F2.getLB(), ub2 = F2.getUB();
            for (int a = lb1; a <= ub1; a++) if (F1.contains(a)) {
                for (int b = lb2; b <= ub2; b++) if (F2.contains(b)) {
                    int d = distanceMatrix[a][b];
                    if (d > best) best = d;
                }
            }
        }
        return best;
    }

    @Override
    public ESat isEntailed() {
        final int thrGE = Math.max(minDist.getLB(), d_lb + 1);

        // If no allowed pair remains (all < thrGE), FALSE
        boolean exists = false;
        if (F1.hasEnumeratedDomain() && F2.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE && !exists; a = F1.nextValue(a)) {
                for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                    if (distanceMatrix[a][b] >= thrGE) { exists = true; break; }
                }
            }
        } else {
            int lb1 = F1.getLB(), ub1 = F1.getUB();
            int lb2 = F2.getLB(), ub2 = F2.getUB();
            outer: for (int a = lb1; a <= ub1; a++) if (F1.contains(a)) {
                for (int b = lb2; b <= ub2; b++) if (F2.contains(b)) {
                    if (distanceMatrix[a][b] >= thrGE) { exists = true; break outer; }
                }
            }
        }
        if (!exists) return ESat.FALSE;

        // If every remaining cross-pair satisfies the threshold, TRUE; else UNDEFINED
        if (isUniversallyAllowed(thrGE)) return ESat.TRUE;
        return ESat.UNDEFINED;
    }

    private boolean isUniversallyAllowed(int thrGE) {
        if (F1.hasEnumeratedDomain() && F2.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                    if (distanceMatrix[a][b] < thrGE) return false;
                }
            }
            return true;
        } else {
            int lb1 = F1.getLB(), ub1 = F1.getUB();
            int lb2 = F2.getLB(), ub2 = F2.getUB();
            for (int a = lb1; a <= ub1; a++) if (F1.contains(a)) {
                for (int b = lb2; b <= ub2; b++) if (F2.contains(b)) {
                    if (distanceMatrix[a][b] < thrGE) return false;
                }
            }
            return true;
        }
    }
}
