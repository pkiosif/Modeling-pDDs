# pDD Problem Code and Benchmarks

This repository contains the code and benchmarks for solving the pDD problem, which accompanies the paper titled **"Modeling the p-dispersion problem with distance constraints"**.

## Folder Structure

- **Benchmarks**: Contains various pDD classes of different sizes. There are three categories of problems: MDPLIB, GRID and BINS problem classes.
- **CP Optimizer**: Includes the (M<sub>el</sub> and M<sub>tb</sub>) models for the pDD.
- **OR-Tools CP-SAT**: Includes the (M<sub>el</sub> and M<sub>tb</sub>) models for the pDD.
- **OscaR**: Includes the (M<sub>el</sub> and M<sub>tb</sub>) models for the pDD.
- **ChocoSolver**: Contains the different models (M<sub>el</sub>, M<sub>tb</sub>, M<sub>t</sub>, M<sub>b</sub>) for the pDD.

## DistanceGT Propagator
 The **DistanceGT** **propagator** implementation can be found in **ChocoSolver/src/main/java/org/mysearch/constraints/PropDistanceGT_v2.java**

The actual definition of the **DistanceGT** constraints exists in the **DistanceGT.java** class and it is:

**DistanceGT(IntVar F1, IntVar F2, IntVar minDist, int[][] dist, int d_lb)**

**! Note** that the DistanceGT.java class contains two more definitions:
- DistanceGT(IntVar F1, IntVar F2, SharedBest minDist, int[][] dist, int d_lb)
- DistanceGT(IntVar F1, IntVar F2, int[][] dist, int d_lb)

but they **DO NOT** refer to the ternary constraints mentioned in the paper.



## Running the Models

**Note**: The -Xms1g and -Xmx30g flags are not mandatory.

### 1. **Ternary Model (M<sub>t</sub>) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDTernary lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDTernary domwdeg 0`

### 2. **Element Model (M<sub>el</sub>) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDD lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDD domwdeg 0`

### 3. **Table Model (M<sub>tb</sub>) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDTable2 lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDTable2 domwdeg 0`

### 4. **Binary Model (M<sub>b</sub>) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDBinary lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDBinary domwdeg 0`

### 5. **Element Model (M<sub>el</sub>) with CP Optimizer**
   - Run using the command:  
     `python3 cpo_element.py -f [problem_filepath]`

### 6. **Table Model (M<sub>tb</sub>) with CP Optimizer**
   - Run using the command:  
     `python3 cpo_table.py -f [problem_filepath]`

### 7. **Element Model (M<sub>el</sub>) with OR-Tools CP-SAT**
   - Run using the command:  
     `python3 ort_element.py -f [problem_filepath]`

### 8. **Table Model (M<sub>tb</sub>) with OR-Tools CP-SAT**
   - Run using the command:  
     `python3 ort_table.py -f [problem_filepath]`

### 9. **Element Model (M<sub>el</sub>) with OscaR**
   - Run using the command:  
     `java -Xms1g -Xmx30g -jar ./OscaR/oscar-element.jar [problem_filepath] domwdeg`

### 10. **Table Model (M<sub>tb</sub>) with OscaR**
   - Run using the command:  
     `java -Xms1g -Xmx30g -jar ./OscaR/oscar-table.jar [problem_filepath] domwdeg`