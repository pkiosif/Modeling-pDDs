package org.mysearch.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class DataReader {

    public static class DistanceData {
        public final int[] flatDistances;
        public final int[] flatConstraints;
        public final int points;
        public final int facilities;
        public final int clients;
        public final int[] flatCLDistances;
        public final int[] flatCLSPDistances;
        public final int[] clConstraints;

        public DistanceData(int[] flatDistances, int[] flatConstraints, int points, int facilities, int clients, int[] flatCLDistances, int[] flatCLSPDistances, int[] clConstraints) {
            this.flatDistances = flatDistances;
            this.flatConstraints = flatConstraints;
            this.points = points;
            this.facilities = facilities;
            this.clients = clients;
            this.flatCLDistances = flatCLDistances;
            this.flatCLSPDistances = flatCLSPDistances;
            this.clConstraints = clConstraints;
        }
    }

    public static DistanceData readDistanceAndConstraints(String filePath, String decimalPoints) throws IOException {
        int dPoints = Integer.parseInt(decimalPoints);
        List<String> lines = Files.readAllLines(Paths.get(filePath))
                .stream()
                .filter(l -> !l.trim().isEmpty())
                .toList();

        // First line contains: points facilities
        String[] header = lines.get(0).trim().split("\\s+");
        int points = Integer.parseInt(header[0]);
        System.out.println("Found " + points + " points");

        int facilities = Integer.parseInt(header[1]);
        System.out.println("Found " + facilities + " facilities");
        int[][] distances = new int[points][points];
        //int distLen = points * (points - 1) / 2;

        int distIdx = 1;

//        if (format.equalsIgnoreCase("flat")) {
//            for (int i = 1; i <= distLen; i++) {
//                String[] parts = lines.get(i).split("\\s+");
//                int a = Integer.parseInt(parts[0]);
//                int b = Integer.parseInt(parts[1]);
//                int val = (int) (Double.parseDouble(parts[2]) * 1_000_000);
//                distances[a][b] = val;
//                distances[b][a] = val;
//            }
//            distIdx = distLen + 1;
//        } else if (format.equalsIgnoreCase("upper")) {
//            for (int i = 0; i < points - 1; i++) {
//                for (int j = i + 1; j < points; j++) {
//                    String[] parts = lines.get(distIdx++).split("\\s+");
//                    int a = Integer.parseInt(parts[0]);
//                    int b = Integer.parseInt(parts[1]);
//                    int val = (int) (Double.parseDouble(parts[2]) * 1_000_000);
//                    distances[a][b] = val;
//                    distances[b][a] = val;
//                }
//            }
//        } else {
//            throw new IllegalArgumentException("Unsupported format: " + format);
//        }

        for (int i = 0; i < points - 1; i++) {
            for (int j = i + 1; j < points; j++) {
                String[] parts = lines.get(distIdx++).split("\\s+");
                //System.out.println(Arrays.toString(parts));
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                int val = (int) (Double.parseDouble(parts[2]) * Math.pow(10,dPoints)); // * 1_000_000);
                distances[i][j] = val;
                distances[j][i] = val;
            }
        }
        //System.out.println(Arrays.deepToString(distances));
        //print2DArray(distances);
        // Flatten distances
        int[] flatDistances = new int[points * points];
        for (int i = 0; i < points; i++) {
            System.arraycopy(distances[i], 0, flatDistances, i * points, points);
        }

        int consLen = facilities * (facilities - 1) / 2;
        int[][] dCons = new int[facilities][facilities];
        for (int i = 0; i < consLen; i++) {
            String[] parts = lines.get(distIdx + i).split("\\s+");
            int a = Integer.parseInt(parts[0]);
            int b = Integer.parseInt(parts[1]);
            int val = (int) (Double.parseDouble(parts[2]) * Math.pow(10,dPoints));//* 1_000_000);
            dCons[a][b] = val;
            dCons[b][a] = val;
        }

        //print2DArray(dCons);

        //System.out.println(Arrays.deepToString(dCons));

        int[] flatConstraints = new int[facilities * facilities];
        for (int i = 0; i < facilities; i++) {
            System.arraycopy(dCons[i], 0, flatConstraints, i * facilities, facilities);
        }

        return new DistanceData(flatDistances, flatConstraints, points, facilities, 0, null, null, null);
    }


    public static void print2DArray(int[][] array) {
        for (int[] row : array) {
            System.out.println(Arrays.toString(row));
        }
    }

}
