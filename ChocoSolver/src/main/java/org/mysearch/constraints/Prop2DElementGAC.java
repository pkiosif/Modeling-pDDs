package org.mysearch.constraints;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

public class Prop2DElementGAC extends Propagator<IntVar> {

    private final int[][] matrix;
    private final IntVar row;
    private final IntVar col;
    private final IntVar value;


    public Prop2DElementGAC(int[][] matrix, IntVar row, IntVar col, IntVar value) {
        super(new IntVar[]{row, col, value}, PropagatorPriority.LINEAR, false);
        this.matrix = matrix;
        this.row = row;
        this.col = col;
        this.value = value;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        boolean changed;
        do {
            changed = false;
            changed |= filterRowAndCol();
            changed |= filterValue();
        } while (changed);
    }

    private boolean filterRowAndCol() throws ContradictionException {
        boolean changed = false;

        int ubR = row.getUB();
        for (int r = row.getLB(); r <= ubR; r = row.nextValue(r)) {
            boolean hasSupport = false;
            int ubC = col.getUB();
            for (int c = col.getLB(); c <= ubC; c = col.nextValue(c)) {
                if (isValidIndex(r, c) && value.contains(matrix[r][c])) {
                    hasSupport = true;
                    break;
                }
            }
            if (!hasSupport) {
                changed |= row.removeValue(r, this);
            }
        }

        int ubC = col.getUB();
        for (int c = col.getLB(); c <= ubC; c = col.nextValue(c)) {
            boolean hasSupport = false;
            ubR = row.getUB();
            for (int r = row.getLB(); r <= ubR; r = row.nextValue(r)) {
                if (isValidIndex(r, c) && value.contains(matrix[r][c])) {
                    hasSupport = true;
                    break;
                }
            }
            if (!hasSupport) {
                changed |= col.removeValue(c, this);
            }
        }

        return changed;
    }

    private boolean filterValue() throws ContradictionException {
        boolean changed = false;

        int ubV = value.getUB();
        for (int v = value.getLB(); v <= ubV; v = value.nextValue(v)) {
            boolean hasSupport = false;
            int ubR = row.getUB();

            searchSupport:
            for (int r = row.getLB(); r <= ubR; r = row.nextValue(r)) {
                int ubC = col.getUB();
                for (int c = col.getLB(); c <= ubC; c = col.nextValue(c)) {
                    if (isValidIndex(r, c) && matrix[r][c] == v) {
                        hasSupport = true;
                        break searchSupport;
                    }
                }
            }

            if (!hasSupport) {
                changed |= value.removeValue(v, this);
            }
        }
        return changed;
    }

    private boolean isValidIndex(int r, int c) {
        return r >= 0 && r < matrix.length && c >= 0 && c < matrix[r].length;
    }

    @Override
    public ESat isEntailed() {
        if (row.isInstantiated() && col.isInstantiated() && value.isInstantiated()) {
            int r = row.getValue();
            int c = col.getValue();
            if (isValidIndex(r, c) && matrix[r][c] == value.getValue()) {
                return ESat.TRUE;
            }
            return ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }
}