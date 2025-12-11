package org.mysearch.strategy;

import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.mysearch.util.SharedBest;

public class StrategyWrapperPDDSimpleBIN extends AbstractStrategy<IntVar> {

    private final AbstractStrategy<IntVar> baseStrategy; // user-defined branching
    private final IntVar[] variables;                    // facility choices
    private final SharedBest minDist;
    private final int[][] distanceMatrix;

    public StrategyWrapperPDDSimpleBIN(IntVar[] vars, SharedBest minDist, int[][] dist,
                                       AbstractStrategy<IntVar> baseStrategy) {
        super(vars);
        this.variables = vars;
        this.minDist = minDist;
        this.distanceMatrix = dist;
        this.baseStrategy = baseStrategy;
    }

    @Override
    public boolean init() {
        return baseStrategy.init();
    }

    @Override
    public void remove() {
        baseStrategy.remove();
    }

    @Override
    public Decision<IntVar> getDecision() {
        // 1) Delegate to the base strategy first
        Decision<IntVar> d = baseStrategy.getDecision();
        if (d != null) return d;

        // 2) Only act at a true leaf (all F instantiated)
        for (IntVar v : variables) {
            if (!v.isInstantiated()) return null;
        }
        if (variables.length < 2) return null;

        // 3) Compute the min pairwise distance of the selected facilities
        int dmin = Integer.MAX_VALUE;
        for (int i = 0; i < variables.length - 1; i++) {
            int ai = variables[i].getValue();
            for (int j = i + 1; j < variables.length; j++) {
                int aj = variables[j].getValue();
                int dij = distanceMatrix[ai][aj];
                if (dij < dmin) dmin = dij;
            }
        }
        minDist.raiseTo(dmin+1);

        return null;
    }
}