package org.mysearch.strategy;

import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;

/**
 * Wraps a base strategy and, after the first solution is found,
 * runs a myopic bound heuristic at each node to prune subtrees
 * that cannot beat the current incumbent (max–min objective).
 */
public class HeuristicPruningWrapperPDD extends AbstractStrategy<IntVar> {

    private final AbstractStrategy<IntVar> baseStrategy; // user-defined branching
    private final IntVar[] variables;                    // branching variables (e.g., facility choices)
    private final int[][] distanceMatrix;                // distance matrix

    public HeuristicPruningWrapperPDD(IntVar[] vars, int[][] dist, AbstractStrategy<IntVar> baseStrategy) {
        super(vars);
        this.variables = vars;
        this.distanceMatrix = dist;
        this.baseStrategy = baseStrategy;
    }

    @Override public boolean init() {               // <<< important
        return baseStrategy.init();
    }
    @Override public void remove() {             // tidy up on backtracks / restarts
        baseStrategy.remove();
    }

    @Override
    public Decision<IntVar> getDecision() {

        var model  = variables[0].getModel();
        var solver = model.getSolver();

        // Only attempt pruning when an incumbent exists
        if (solver.getSolutionCount() > 0) {
            Number incumbent = solver.getObjectiveManager().getBestSolutionValue();
            if (incumbent != null && shouldPrunePath(incumbent.intValue())) {
                //System.out.println("Heuristic Pruning Decision Made -> CUT");
                // Return a failing decision to force immediate backtrack
                return new PruneDecision(solver, "bound not better than incumbent");
            }
        }

        // Delegate to the base strategy. Returning null here is OK (true leaf).
        return baseStrategy.getDecision();
    }

    /**
     * Returns true if the myopic (admissible) bound of the current subtree
     * cannot beat the incumbent of a max–min objective.
     */
    private boolean shouldPrunePath(int incumbent) {
        //System.out.println("Executing branch pruning heuristic... Best Objective: " + incumbent + ".");

        // Copy current domains (no side effects)
        Map<IntVar, Set<Integer>> domainCopies = new HashMap<>();
        Map<IntVar, Integer> simulated = new HashMap<>();

        for (IntVar var : variables) {
            if (var.isInstantiated()) {
                simulated.put(var, var.getValue());
            } else {
                Set<Integer> dom = new HashSet<>();
                for (int v = var.getLB(); v <= var.getUB(); v = var.nextValue(v)) {
                    dom.add(v);
                }
                domainCopies.put(var, dom);
            }
        }

        // Greedy completion: for each unassigned variable, pick the value
        // maximizing the current min-distance to already-picked values.
        // Track the global MIN across the whole simulated assignment
        // (consistent with max–min objective).
        int estimation = Integer.MAX_VALUE;
        for (IntVar var : variables) {
            if (!var.isInstantiated()) {
                Set<Integer> dom = domainCopies.get(var);
                if (dom == null || dom.isEmpty()) {
                    // Empty Domain => cannot extend
                    return true;
                }

                int[] pick = selectBestValue(var, dom, simulated);
                int bestVal   = pick[0];
                int stepScore = pick[1]; // min-distance after placing bestVal vs existing picks

                if (bestVal == -1) {
                    // No feasible value found
                    return true;
                }
                domainCopies.remove(var);
                simulated.put(var, bestVal);
                for (IntVar variable : domainCopies.keySet()) {
                    domainCopies.get(variable).remove(bestVal);
                }
                // Max–min: keep the min across the whole simulated assignment
                //globalMin = Math.min(globalMin, stepScore);
                estimation = stepScore;
            }
        }

        // If everything was already instantiated, evaluate the induced min once.
        if (estimation == Integer.MAX_VALUE) {
            return false;
        }

        // MAXIMIZATION: prune if optimistic bound <= incumbent.
        boolean prune = false;
        prune = (estimation <= incumbent);
        //System.out.printf("Heuristic bound=%d incumbent=%d => %s%n",
        //        estimation, incumbent, prune ? "PRUNE" : "KEEP");
        return prune;
    }

    /**
     * For a candidate assignment of 'var' -> 'val', compute the score:
     * the min distance to already-picked values (myopic upper bound step).
     */
    private int scoreFor(IntVar var, int val, Map<IntVar, Integer> simulated) {
        if (simulated.isEmpty()) {
            // With no anchors yet, be conservative (no info → 0).
            return 0;
        }
        // Copy current placements (values) and include the candidate
        List<Integer> selected_vals = new ArrayList<>(simulated.values());
        selected_vals.add(val);
        int minDist = Integer.MAX_VALUE;
        for (int i=0; i<selected_vals.size() -1; i++) {
            for (int j=i+1; j<selected_vals.size(); j++) {
                int d = distanceMatrix[selected_vals.get(i)][selected_vals.get(j)];
                if (d < minDist)
                    minDist = d;
            }
        }

        return minDist;

    }

    /** Pick the value in 'domain' maximizing the step score against 'simulated'. */
    private int[] selectBestValue(IntVar var, Set<Integer> domain, Map<IntVar, Integer> simulated) {
        int bestVal   = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int val : domain) {
            int s = scoreFor(var, val, simulated);
            if (s > bestScore) {
                bestScore = s;
                bestVal   = val;
            }
        }
        return new int[]{bestVal, bestScore};
    }
}
