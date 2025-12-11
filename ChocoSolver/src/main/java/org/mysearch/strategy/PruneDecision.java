package org.mysearch.strategy;

import org.chocosolver.solver.Solver;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.variables.IntVar;

/**
 * A decision that always fails immediately to prune the current subtree.
 */
public final class PruneDecision extends Decision<IntVar> {

    private final Solver solver;
    private final String reason;

    public PruneDecision(Solver solver, String reason) {
        super(1);   // arity=1 (no refutation branch)
        this.solver = solver;
        this.reason = reason;
    }

    @Override
    public void apply() throws ContradictionException {
        // Raise a managed contradiction: the solver backtracks from this node.
        solver.throwsException(this, null, "cut: " + reason);
    }

    @Override
    public Object getDecisionValue() {
        // No concrete value; this decision only prunes.
        return null;
    }

    @Override
    public void free() {
        // Nothing to recycle.
    }
}
