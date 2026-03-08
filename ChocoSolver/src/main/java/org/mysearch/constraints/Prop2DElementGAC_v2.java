package org.mysearch.constraints;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.BitSet;

public class Prop2DElementGAC_v2 extends Propagator<IntVar> {

    private final int[][] matrix;
    private final IntVar x1;
    private final IntVar x2;
    private final IntVar value;

    private final BitSet supportedX1 = new BitSet();
    private final BitSet supportedX2 = new BitSet();
    private final BitSet supportedValues = new BitSet();

    public Prop2DElementGAC_v2(int[][] matrix, IntVar x1, IntVar x2, IntVar value) {
        super(new IntVar[]{x1, x2, value}, PropagatorPriority.LINEAR, false);
        this.matrix = matrix;
        this.x1 = x1;
        this.x2 = x2;
        this.value = value;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        // Καθαρίζουμε τα trackers από το προηγούμενο propagation
        supportedX1.clear();
        supportedX2.clear();
        supportedValues.clear();

        // 1. ΕΝΑ ΚΑΙ ΜΟΝΑΔΙΚΟ SWEEP ΓΙΑ ΤΑ ΣΥΓΧΡΟΝΑ SUPPORTS
        int ubX1 = x1.getUB();
        for (int v1 = x1.getLB(); v1 <= ubX1; v1 = x1.nextValue(v1)) {
            int ubX2 = x2.getUB();
            for (int v2 = x2.getLB(); v2 <= ubX2; v2 = x2.nextValue(v2)) {
                if (isValidIndex(v1, v2)) {
                    int val = matrix[v1][v2];
                    // Αν η τιμή του κελιού υπάρχει στο domain της μεταβλητής value
                    if (value.contains(val)) {
                        supportedX1.set(v1);
                        supportedX2.set(v2);
                        supportedValues.set(val);
                    }
                }
            }
        }

        // 2. ΜΑΖΙΚΟ ΦΙΛΤΡΑΡΙΣΜΑ (Pruning)
        // Αν μια τιμή των x1, x2 ή value δεν βρήκε υποστήριξη, διαγράφεται.

        int ubX1_filter = x1.getUB();
        for (int v1 = x1.getLB(); v1 <= ubX1_filter; v1 = x1.nextValue(v1)) {
            if (!supportedX1.get(v1)) {
                x1.removeValue(v1, this);
            }
        }

        int ubX2_filter = x2.getUB();
        for (int v2 = x2.getLB(); v2 <= ubX2_filter; v2 = x2.nextValue(v2)) {
            if (!supportedX2.get(v2)) {
                x2.removeValue(v2, this);
            }
        }

        int ubV_filter = value.getUB();
        for (int v = value.getLB(); v <= ubV_filter; v = value.nextValue(v)) {
            if (!supportedValues.get(v)) {
                value.removeValue(v, this);
            }
        }
    }

    // Βοηθητική μέθοδος για ασφαλή προσπέλαση του πίνακα
    private boolean isValidIndex(int v1, int v2) {
        return v1 >= 0 && v1 < matrix.length && v2 >= 0 && v2 < matrix[v1].length;
    }

    @Override
    public ESat isEntailed() {
        if (x1.isInstantiated() && x2.isInstantiated() && value.isInstantiated()) {
            int v1 = x1.getValue();
            int v2 = x2.getValue();
            if (isValidIndex(v1, v2) && matrix[v1][v2] == value.getValue()) {
                return ESat.TRUE;
            }
            return ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }
}