import argparse
from itertools import combinations
from typing import Optional, Dict, Any, List

from docplex.cp.model import CpoModel
from docplex.cp.modeler import all_diff, element

from data_reader import read_distance_and_constraints


def solve_instance_with_cpoptimizer_table_obj(
    filename: str,
    time_limit: float = 3600.0,
    workers: Optional[int] = None,
    log_verbosity: str = "Normal",
) -> Dict[str, Any]:
   
    dd = read_distance_and_constraints(filename)
    P: int = dd.points
    FAC: int = dd.facilities
    distances: List[int] = dd.flat_distances      # flattened P x P
    d_cons: List[int] = dd.flat_constraints       # flattened FAC x FAC

    mdl = CpoModel("pDD_TableObj (CPO)")

    # Decision variables: facility indices in 0..P-1
    F = [mdl.integer_var(0, P - 1, name=f"F_{i}") for i in range(FAC)]
    
    

    # Pairs (i < j)
    pairs = list(combinations(range(FAC), 2))
    
    distinct_distances = sorted(list(set(distances)))
    
    # Auxiliary vars: pairwise distances between chosen facilities
    FF = [mdl.integer_var(domain=distinct_distances, name=f"FF_{k}") for k in range(len(pairs))]

    # 
    # Build Table Constraints for each pair
    for k, (f1, f2) in enumerate(pairs):
        
        
        min_required = d_cons[f1 * FAC + f2]
        
        valid_tuples = []
        
        
        for loc1 in range(P):
            for loc2 in range(P):
                
                
                if loc1 == loc2:
                    continue
                
                
                dist_val = distances[loc1 * P + loc2]
                
                # FILTER: Only allow if distance > min_required
                if dist_val > min_required:
                    valid_tuples.append((loc1, loc2, dist_val))
        
        # 3. Post the Table Constraint
        mdl.add(mdl.allowed_assignments((F[f1], F[f2], FF[k]), valid_tuples))

    # Objective: maximize the minimum pairwise distance
    min_dist = mdl.min(FF)
    mdl.maximize(min_dist)

    # Solve
    solve_kwargs = {"TimeLimit": time_limit, "LogVerbosity": log_verbosity}
    if workers is not None:
        solve_kwargs["Workers"] = int(workers)

    msol = mdl.solve(**solve_kwargs)

    if not msol:
        print("no solution found")
        return {
            "status": str(mdl.get_solve_status()),
            "objective": None,
            "facilities": None,
            "filename": filename,
        }

    F_val = [int(msol[v]) for v in F]
    
    obj_vals = msol.get_objective_values()
    obj = int(obj_vals[0]) if obj_vals else int(msol[min_dist])

    print("solution found:")
    print("F:", F_val)
    print("Objective value:", obj)
    print("Solver status:", msol.get_solve_status())
    print("Filename:", filename)

    return {
        "status": str(msol.get_solve_status()),
        "objective": obj,
        "facilities": F_val,
        "filename": filename,
    }


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="CP model Table for p-dispersion")
    parser.add_argument("-f", "--file", type=str, required=True, help="Path to instance file")
    parser.add_argument("-t", "--timeLimit", type=float, default=3600.0, help="Time limit in seconds")
    parser.add_argument("-w", "--workers", type=int, default=1, help="Number of workers (threads)")
    parser.add_argument("-v", "--verbosity", type=str, default="Normal", help="Log verbosity (e.g., Normal, Quiet)")

    args = parser.parse_args()

  
    if args.file is not None:
        res = solve_instance_with_cpoptimizer_table_obj(
            filename=args.file,
            time_limit=args.timeLimit,
            workers=args.workers,
            log_verbosity=args.verbosity,
        )
     
