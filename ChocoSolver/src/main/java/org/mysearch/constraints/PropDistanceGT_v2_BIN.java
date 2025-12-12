package org.mysearch.constraints;

import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.mysearch.util.SharedBest;

import java.util.BitSet;

public final class PropDistanceGT_v2_BIN extends Propagator<IntVar> {
    private final IntVar F1, F2;
    private final int[][] distanceMatrix;
    private final SharedBest best;   // global, non-backtrackable
    private final int d_lb, P;

    private final BitSet[] supF1, supF2;


    private final IStateInt thr;

    public PropDistanceGT_v2_BIN(IntVar F1, IntVar F2, SharedBest best, int[][] dist, int d_lb) {
        super(new IntVar[]{F1, F2}, PropagatorPriority.BINARY, false);
        this.F1 = F1; this.F2 = F2;
        this.best = best;
        this.distanceMatrix = dist;
        this.d_lb = d_lb;
        this.P = dist.length;

        int baseGE = d_lb + 1;
        this.supF1 = new BitSet[P];
        this.supF2 = new BitSet[P];
        for (int a = 0; a < P; a++) supF1[a] = new BitSet(P);
        for (int b = 0; b < P; b++) supF2[b] = new BitSet(P);
        for (int a = 0; a < P; a++) for (int b = 0; b < P; b++) {
            if (dist[a][b] >= baseGE) { supF1[a].set(b); supF2[b].set(a); }
        }

        // (backtrackable)
        Solver s = F1.getModel().getSolver();
        this.thr = s.getEnvironment().makeInt(Math.max(baseGE, best.get()));
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        // Scope is only {F1, F2}.
        return IntEventType.REMOVE.getMask()
                | IntEventType.BOUND.getMask()
                | IntEventType.INSTANTIATE.getMask();
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        boolean changed;
        do {
            changed = false;

            int desired = Math.max(d_lb + 1, best.get());
            if (desired > thr.get()) thr.set(desired);

            int T = thr.get();

            changed |= reviseF1wrtF2(T);
            changed |= reviseF2wrtF1(T);
        } while (changed);
    }

    private boolean reviseF1wrtF2(int T) throws ContradictionException {
        boolean removed = false;
        if (F1.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                if (!hasSupportAinF2(a, T)) { F1.removeValue(a, this); removed = true; }
            }
        } else {
            for (int a = F1.getLB(); a <= F1.getUB(); a++) {
                if (F1.contains(a) && !hasSupportAinF2(a, T)) { F1.removeValue(a, this); removed = true; }
            }
        }
        return removed;
    }

    private boolean reviseF2wrtF1(int T) throws ContradictionException {
        boolean removed = false;
        if (F2.hasEnumeratedDomain()) {
            for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                if (!hasSupportBinF1(b, T)) { F2.removeValue(b, this); removed = true; }
            }
        } else {
            for (int b = F2.getLB(); b <= F2.getUB(); b++) {
                if (F2.contains(b) && !hasSupportBinF1(b, T)) { F2.removeValue(b, this); removed = true; }
            }
        }
        return removed;
    }

    private boolean hasSupportAinF2(int a, int T) {
        int baseGE = d_lb + 1;
        if (T == baseGE && F2.hasEnumeratedDomain()) {
            for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                if (supF1[a].get(b)) return true;
            }
            return false;
        }
        if (F2.hasEnumeratedDomain()) {
            for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                if (distanceMatrix[a][b] >= T) return true;
            }
            return false;
        } else {
            for (int b = F2.getLB(); b <= F2.getUB(); b++) {
                if (F2.contains(b) && distanceMatrix[a][b] >= T) return true;
            }
            return false;
        }
    }

    private boolean hasSupportBinF1(int b, int T) {
        int baseGE = d_lb + 1;
        if (T == baseGE && F1.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                if (supF2[b].get(a)) return true;
            }
            return false;
        }
        if (F1.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                if (distanceMatrix[a][b] >= T) return true;
            }
            return false;
        } else {
            for (int a = F1.getLB(); a <= F1.getUB(); a++) {
                if (F1.contains(a) && distanceMatrix[a][b] >= T) return true;
            }
            return false;
        }
    }

    @Override
    public ESat isEntailed() {
        int T = thr.get();

        boolean exists = false;
        if (F1.hasEnumeratedDomain() && F2.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE && !exists; a = F1.nextValue(a)) {
                for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                    if (distanceMatrix[a][b] >= T) { exists = true; break; }
                }
            }
        } else {
            int lb1 = F1.getLB(), ub1 = F1.getUB();
            int lb2 = F2.getLB(), ub2 = F2.getUB();
            outer: for (int a = lb1; a <= ub1; a++) if (F1.contains(a)) {
                for (int b = lb2; b <= ub2; b++) if (F2.contains(b)) {
                    if (distanceMatrix[a][b] >= T) { exists = true; break outer; }
                }
            }
        }
        if (!exists) return ESat.FALSE;

        if (isUniversallyAllowed(T)) return ESat.TRUE;
        return ESat.UNDEFINED;
    }

    private boolean isUniversallyAllowed(int T) {
        if (F1.hasEnumeratedDomain() && F2.hasEnumeratedDomain()) {
            for (int a = F1.getLB(); a != Integer.MAX_VALUE; a = F1.nextValue(a)) {
                for (int b = F2.getLB(); b != Integer.MAX_VALUE; b = F2.nextValue(b)) {
                    if (distanceMatrix[a][b] < T) return false;
                }
            }
            return true;
        } else {
            int lb1 = F1.getLB(), ub1 = F1.getUB();
            int lb2 = F2.getLB(), ub2 = F2.getUB();
            for (int a = lb1; a <= ub1; a++) if (F1.contains(a)) {
                for (int b = lb2; b <= ub2; b++) if (F2.contains(b)) {
                    if (distanceMatrix[a][b] < T) return false;
                }
            }
            return true;
        }
    }
}
