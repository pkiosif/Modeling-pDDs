package org.mysearch;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.InputOrder;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.mysearch.constraints.DistanceGT;
import org.mysearch.strategy.*;

import org.mysearch.util.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.chocosolver.solver.search.strategy.Search.domOverWDegSearch;


public class Main {

    private static final String CSV_PATH = "experiments.csv";
    private static boolean TRACK_PM = false;   // enable/disable pruning metrics
    private static String SEED = "0";


    static boolean hasFlag(String[] args, String flag) {
        if (args == null) return false;
        for (String a : args) if (flag.equalsIgnoreCase(a)) return true;
        return false;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: java Main <file_path>");
            return;
        }
        long maxHeap = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        System.out.println("Max heap (MB): " + maxHeap);


        String file = args[0];
        System.out.println("\n------------------------------------------------------------------------------------------");
        System.out.println("Filename " + file);
        String ptype = args[1];
        boolean pruneBool = false;
        String ordering = args[2];
        String decimalPoints = args[3];


        TRACK_PM  = hasFlag(args, "--prune-metrics"); // enable with this flag
        boolean restartOnSol = hasFlag(args, "--restart");
        if(restartOnSol) System.out.println("Restarts on solution enabled.");
        int solvepDD = 0;



        System.out.println("Arguments= model: " + ptype + "\tordering:" + ordering);

        DataReader.DistanceData data = null;
        try {
            if (ptype.equals("pDD")){
                data = DataReader.readDistanceAndConstraints(file, decimalPoints);
                solvepDDModel(data, ordering, restartOnSol);
            } else if (ptype.equals("pDDTernary")) {
                data = DataReader.readDistanceAndConstraints(file, decimalPoints);
                solvepDDTernaryModel(data, ordering, restartOnSol);
            } else if (ptype.equals("pDDBinary")) {
                data = DataReader.readDistanceAndConstraints(file, decimalPoints);
                solvepDDBinaryModel(data, ordering);
            }
            else
            {
                return;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static void solvepDDModel(DataReader.DistanceData data, String ordering, boolean restartOnSol) {
        System.out.println("Model with 1D Element Constraints");

        int F = data.facilities;
        int P = data.points;
        int[] distances = data.flatDistances;
        int[] dCons = data.flatConstraints;


        int[][] distanceMatrix = unflatten(distances, P, P);


        Model model = new Model("P-Dispersion with Distance Contraints");

        IntVar[] F_vars = model.intVarArray("F", F, 0, P - 1);

        List<IntVar> FF = new ArrayList<>();
        int idx = 0;
        for (int f1 = 0; f1 < F - 1; f1++) {
            for (int f2 = f1 + 1; f2 < F; f2++) {
                IntVar ff = model.intVar("FF_" + idx, Arrays.stream(distances).distinct().toArray());
                FF.add(ff);

                IntVar index = model.intVar("index_" + f1 + "_" + f2, 0, P * P - 1);
                model.scalar(new IntVar[]{F_vars[f1], F_vars[f2]}, new int[]{P, 1}, "=", index).post();

                model.element(ff, distances, index, 0).post();

                // ff > d_cons[i][j]
                model.arithm(ff, ">", dCons[f1 * F + f2]).post();
                idx++;
            }
        }
        // Objective: maximize the minimum distance
        IntVar minDist = model.intVar("minDist", 0, Arrays.stream(distances).max().getAsInt());
        model.min(minDist, FF.toArray(new IntVar[0])).post();
        model.setObjective(Model.MAXIMIZE, minDist);


        Solver solver = model.getSolver();

        AbstractStrategy<IntVar> orderingStrategy;
        if(ordering.equals("lexico")){
            System.out.println("Using lexico var/val ordering.");
            orderingStrategy = Search.intVarSearch(
                new InputOrder<>(model),
                new IntDomainMin(),
                F_vars
        );
            solver.setSearch(orderingStrategy);
        }else{
            System.out.println("Using default var/val ordering.");
        }


        if (restartOnSol) {
            System.out.println("Using restarts on solutions");
            solver.setRestartOnSolutions();
        }

        solver.limitTime("3600s");

        Solution sol = new Solution(model);
        System.out.print("\n");
        int solindex = 0;
        long startTime = System.currentTimeMillis();
        long endTime = 0;

        System.out.println("--Started solving...");

        //solver.showDecisions();
        solver.showSolutions();
        while (solver.solve()) {
            endTime = System.currentTimeMillis();
            sol.record();
            solindex++;
            System.out.println("#" + solindex + "   obj: " + sol.getIntVal(minDist) + "   " + ((endTime - startTime) / 1000) + "s");
        }
        System.out.print("\n");
        solver.printStatistics();

    }


    private static void solvepDDTernaryModel(DataReader.DistanceData data, String ordering, boolean restartOnSol){
        System.out.println("Model with Simple (Ternary) Constraints");

        int F = data.facilities;
        int P = data.points;
        int[] distances = data.flatDistances;
        int[] dCons = data.flatConstraints;


        int[][] distanceMatrix = unflatten(distances, P, P);

        Model model = new Model("P-Dispersion with Distance Contraints");

        IntVar[] F_vars = model.intVarArray("F", F, 0, P - 1);
        IntVar minDist = model.intVar("minDist", 0, Arrays.stream(distances).max().getAsInt());

        for (int i = 0; i < F - 1; i++) {
            for (int j = i + 1; j < F; j++) {
                int d_lb = dCons[i * F + j];
                model.post(new DistanceGT(F_vars[i], F_vars[j], minDist, distanceMatrix, d_lb));
            }
        }

        // Objective: maximize the minimum distance

        model.setObjective(Model.MAXIMIZE, minDist);

        Solver solver = model.getSolver();
        AbstractStrategy<IntVar> stratF;
        AbstractStrategy<IntVar> stratMinDist;
        if(ordering.equals("lexico")){
            System.out.println("Using lexico var/val ordering.");
            stratF = Search.intVarSearch(
                    new InputOrder<>(model),
                    new IntDomainMin(),
                    F_vars
            );

            stratMinDist = Search.intVarSearch(minDist);
            solver.setSearch(stratF, stratMinDist);

        }else{
            System.out.println("Using default var/val ordering.");
        }

        if (restartOnSol) {
            System.out.println("Using restarts on solutions");
            solver.setRestartOnSolutions();
        }
        solver.limitTime("3600s");

        Solution sol = new Solution(model);
        System.out.print("\n");
        int solindex = 0;
        long startTime = System.currentTimeMillis();
        long endTime = 0;

        System.out.println("--Started solving...");


        //solver.showDecisions();
        solver.showSolutions();
        while(solver.solve()) {
            endTime = System.currentTimeMillis();
            sol.record();
            solindex++;
            System.out.println("#" + solindex + "   obj: " + sol.getIntVal(minDist)  + "   " + ((endTime - startTime)/1000)+"s");
        }
        System.out.print("\n");
        solver.printStatistics();
    }

    private static void solvepDDBinaryModel(DataReader.DistanceData data, String ordering){
        System.out.println("Model with Simple BINARY Constraints");

        int F = data.facilities;
        int P = data.points;
        int[] distances = data.flatDistances;
        int[] dCons = data.flatConstraints;


        int[][] distanceMatrix = unflatten(distances, P, P);

        SharedBest minDist = new SharedBest();

        Model model = new Model("P-Dispersion with Distance Contraints");

        IntVar[] F_vars = model.intVarArray("F", F, 0, P - 1);

        for (int i = 0; i < F - 1; i++) {
            for (int j = i + 1; j < F; j++) {
                int d_lb = dCons[i * F + j];
                model.post(new DistanceGT(F_vars[i], F_vars[j], minDist, distanceMatrix, d_lb));
            }
        }

        AbstractStrategy<IntVar> orderingStrategy;
        if(ordering.equals("lexico")){
            System.out.println("Using lexico var/val ordering.");
            orderingStrategy = Search.intVarSearch(
                    new InputOrder<>(model),
                    new IntDomainMin(),
                    F_vars
            );
        }else{
            System.out.println("Using default var/val ordering.");
            orderingStrategy = domOverWDegSearch(F_vars);
        }

        StrategyWrapperPDDSimpleBIN simpleStrategy = new StrategyWrapperPDDSimpleBIN(F_vars, minDist, distanceMatrix, orderingStrategy);

        Solver solver = model.getSolver();
        solver.setSearch(simpleStrategy);

        solver.limitTime("3600s");

        Solution sol = new Solution(model);
        System.out.print("\n");
        int solindex = 0;
        long startTime = System.currentTimeMillis();
        long endTime = 0;

        System.out.println("--Started solving...");

        int best=0;
        //solver.showDecisions();
        //solver.showSolutions();
        while(solver.solve()) {
            endTime = System.currentTimeMillis();
            sol.record();
            solindex++;

            int dmin = Integer.MAX_VALUE;
            for (int i = 0; i < F - 1; i++) {
                int ai = sol.getIntVal(F_vars[i]);
                for (int j = i + 1; j < F; j++) {
                    int aj = sol.getIntVal(F_vars[j]);
                    int dij = distanceMatrix[ai][aj];
                    if (dij < dmin) dmin = dij;
                }
            }
            if(best<dmin){
                best=dmin;

                System.out.println("#" + solindex + "   obj: " + dmin  + "   " + ((endTime - startTime)/1000)+"s");
            }
        }
        System.out.print("\n");
        solver.printStatistics();
    }



    // Placeholder methods to simulate input loading
    private static int[][] loadDistanceMatrix(int P) {
        int[][] mat = new int[P][P];
        for (int i = 0; i < P; i++) {
            for (int j = 0; j < P; j++) {
                mat[i][j] = (i == j) ? 0 : 1000000 + Math.abs(i - j) * 10000;
            }
        }
        return mat;
    }

    private static int[][] loadDistanceConstraints(int F) {
        int[][] d_cons = new int[F][F];
        for (int i = 0; i < F; i++) {
            for (int j = 0; j < F; j++) {
                if (i != j) {
                    d_cons[i][j] = 1000000; // example constraint
                }
            }
        }
        return d_cons;
    }

    public static int[][] unflatten(int[] flat, int size1, int size2) {
        int[][] matrix = new int[size1][size2];
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size2; j++) {
                matrix[i][j] = flat[i * size2 + j];
            }
        }
        return matrix;
    }


    public static double[][] unflattenDouble(double[] flat, int size1, int size2) {
        double[][] matrix = new double[size1][size2];
        for (int i = 0; i < size1; i++) {
            for (int j = 0; j < size2; j++) {
                matrix[i][j] = flat[i * size2 + j];
            }
        }
        return matrix;
    }

}