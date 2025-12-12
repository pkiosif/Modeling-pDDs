# pDD Problem Code and Benchmarks

This repository contains the code and benchmarks for solving the pDD problem, which accompanies the paper titled **"Modeling the p-dispersion problem with distance constraints"**.

## Folder Structure

- **Benchmarks**: Contains various pDD classes of different sizes. There are three categories of problems: MDPLIB, GRID and BINS problem classes.
- **CP Optimizer**: Includes the (M<sub>g</sub>) model for the pDD.
- **OR-Tools CP-SAT**: Includes the (M<sub>g</sub>) model for the pDD.
- **ChocoSolver**: Contains the different models (M<sub>g</sub>, M<sub>b</sub>, M<sub>t</sub>) for the pDD.

## DistanceGT Propagator
 The **DistanceGT** **propagator** implementation can be found in **ChocoSolver/src/main/java/org/mysearch/constraints/PropDistanceGT_v2.java**

The actual definition of the **DistanceGT** constraints exists in the **DistanceGT.java** class and it is:

**DistanceGT(IntVar F1, IntVar F2, IntVar minDist, int[][] dist, int d_lb)**

**! Note** that the DistanceGT.java class contains two more definitions:
- DistanceGT(IntVar F1, IntVar F2, SharedBest minDist, int[][] dist, int d_lb)
- DistanceGT(IntVar F1, IntVar F2, int[][] dist, int d_lb)

but they **DO NOT** implement the ternary constraints mentioned in the paper.



## Running the Models

### Note: The -Xms1g and -Xmx30g flags are not mandatory.

### 1. **Ternary Model (M_t) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDTernary lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDTernary default 0`

### 2. **Global Model (M_g) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDD lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDD default 0`

### 3. **Binary Model (M_b) with ChocoSolver**
   - **Lexicographic Variable/Value Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDBinary lexico 0`
   - **Default Ordering**:  
     `java -Xms1g -Xmx30g -jar ./ChocoSolver/ChocoSolver.jar [problem_filepath] pDDBinary default 0`

### 4. **Global Model (M_g) with CP Optimizer**
   - Run using the command:  
     `python3 cpo_element.py -f [problem_filepath]`

### 5. **Global Model (M_g) with OR-Tools CP-SAT**
   - Run using the command:  
     `python3 ort_element.py -f [problem_filepath]`
