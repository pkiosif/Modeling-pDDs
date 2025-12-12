# cpo_element_simple_ortools.py
# OR-Tools CP-SAT model (Element + objective) for p-dispersion
# Reads instance via data_reader.read_distance_and_constraints(filename)

import argparse
from itertools import combinations
from typing import Optional, Dict, Any, List

from ortools.sat.python import cp_model

from data_reader import read_distance_and_constraints


def solve_instance_with_ortools_element_obj(
    filename: str,
    time_limit: float = 3600.0,
    workers: Optional[int] = None,
    log_verbosity: str = "Normal",
    cp_presolve: str = "on",
    max_presolve_iterations: Optional[int] = None,
    linearization_level: Optional[int] = None,
    probing_level: Optional[int] = None,
) -> Dict[str, Any]:

    dd = read_distance_and_constraints(filename)
    P: int = dd.points
    FAC: int = dd.facilities
    distances: List[int] = dd.flat_distances      # flattened P x P 
    d_cons: List[int] = dd.flat_constraints       # flattened FAC x FAC

    model = cp_model.CpModel()

    # Decision variables: facility indices in 0..P-1
    F = [model.NewIntVar(0, P - 1, f"F_{i}") for i in range(FAC)]

    # Pairs (i < j)
    pairs = list(combinations(range(FAC), 2))

    #print(distances)

    distinct_distances = sorted(set(distances))
    if distinct_distances:
        FF_domain = cp_model.Domain.FromValues(distinct_distances)
    else:
        FF_domain = cp_model.Domain.FromIntervals([(0, 0)])

    # Auxiliary: pairwise distances and index into flattened table
    FF = [model.NewIntVarFromDomain(FF_domain, f"FF_{k}") for k in range(len(pairs))]
    I  = [model.NewIntVar(0, P * P - 1, f"I_{k}")  for k in range(len(pairs))]

    # Link FF[k] == distances[F[f1]*P + F[f2]] and enforce pair-specific distance thresholds
    for k, (f1, f2) in enumerate(pairs):
        # Linearized 2D -> 1D index: I_k = P * F[f1] + F[f2]
        model.Add(I[k] == F[f1] * P + F[f2])

        # Element: FF[k] = distances[I[k]]
        model.AddElement(I[k], distances, FF[k])

        
        model.Add(FF[k] > d_cons[f1 * FAC + f2])

    # Objective: maximize the minimum pairwise distance
    min_dist = model.NewIntVarFromDomain(FF_domain, "min_dist")
    model.AddMinEquality(min_dist, FF)
    model.Maximize(min_dist)

    # Solver parameters
    solver = cp_model.CpSolver()
    solver.parameters.max_time_in_seconds = float(time_limit)
    if workers is not None:
        solver.parameters.num_search_workers = max(1, int(workers))
    # Verbosity
    if str(log_verbosity).lower() in {"quiet", "silent"}:
        solver.parameters.log_search_progress = False
        solver.parameters.cp_model_presolve = True  
        solver.log_callback = None
    else:
        solver.parameters.log_search_progress = True



    # Presolve on/off
    if cp_presolve.lower() == "off":
        solver.parameters.cp_model_presolve = False
    elif cp_presolve.lower() == "on":
        solver.parameters.cp_model_presolve = True

    # Limit presolve iterations (leave unset if None)
    if max_presolve_iterations is not None:
        solver.parameters.max_presolve_iterations = int(max_presolve_iterations)

    # Linearization level (0 disables linear relaxation bits)
    if linearization_level is not None:
        solver.parameters.linearization_level = int(linearization_level)

    # Probing level (0 disables probing)
    if probing_level is not None:
        solver.parameters.cp_model_probing_level = int(probing_level)
    # ----------------------------------------




    status = solver.Solve(model)

   
    status_map = {
        cp_model.OPTIMAL: "Optimal",
        cp_model.FEASIBLE: "Feasible",
        cp_model.INFEASIBLE: "Infeasible",
        cp_model.MODEL_INVALID: "ModelInvalid",
        cp_model.UNKNOWN: "Unknown",
    }
    status_str = status_map.get(status, str(status))

    if status in (cp_model.FEASIBLE, cp_model.OPTIMAL):
        F_val = [solver.Value(v) for v in F]
        obj = solver.Value(min_dist)
        print("solution found:")
        print("F:", F_val)
        print("Objective value:", obj)
        print("Solver status:", status_str)
        print("Filename:", filename)
        return {
            "status": status_str,
            "objective": int(obj),
            "facilities": [int(x) for x in F_val],
            "filename": filename,
        }
    else:
        print("no solution found")
        return {
            "status": status_str,
            "objective": None,
            "facilities": None,
            "filename": filename,
        }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="OR-Tools CP-SAT model (Element+Objective) for p-dispersion")
    parser.add_argument("-f", "--file", type=str, required=True, help="Path to instance file")
    parser.add_argument(
        "-m",
        "--modelType",
        type=str,
        default="1DElement",
        help='Model type flag to match cpo_simple.py interface (use "1DElement")',
    )
    parser.add_argument("-t", "--timeLimit", type=float, default=3600.0, help="Time limit in seconds")
    parser.add_argument("-w", "--workers", type=int, default=1, help="Number of workers (threads)")
    parser.add_argument("-v", "--verbosity", type=str, default="Normal", help="Log verbosity (e.g., Normal, Quiet)")

    parser.add_argument("--cp_presolve", choices=["on", "off"], default="off",
                        help="Enable/disable CP-SAT presolve (default: on)")
    parser.add_argument("--max_presolve_iterations", type=int, default=0,
                        help="Cap the number of presolve iterations (unset by default)")
    parser.add_argument("--linearization_level", type=int, choices=[0, 1, 2], default=0,
                        help="Linearization level (0 disables linear relaxation; unset keeps default)")
    parser.add_argument("--probing_level", type=int, choices=[0, 1, 2], default=0,
                        help="Probing level (0 disables probing; unset keeps default)")

    args = parser.parse_args()

    if args.file is not None:
        res = solve_instance_with_ortools_element_obj(
            filename=args.file,
            time_limit=args.timeLimit,
            workers=args.workers,
            log_verbosity=args.verbosity,
            cp_presolve=args.cp_presolve,
            max_presolve_iterations=args.max_presolve_iterations,
            linearization_level=args.linearization_level,
            probing_level=args.probing_level,
        )
        # print(res["status"], res["objective"], res["facilities"])
