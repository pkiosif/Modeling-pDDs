from dataclasses import dataclass
from typing import List, Optional


@dataclass
class DistanceData:
    flat_distances: List[int]
    flat_constraints: List[int]
    points: int
    facilities: int
    clients: int
    flat_cl_distances: Optional[List[int]]
    flat_cl_sp_distances: Optional[List[int]]
    cl_constraints: Optional[List[int]]


def _flatten_2d(matrix: List[List[int]]) -> List[int]:
    return [val for row in matrix for val in row]


def read_distance_and_constraints(file_path: str) -> DistanceData:

    with open(file_path, "r", encoding="utf-8") as f:
        lines = [ln.strip() for ln in f if ln.strip()]

    header = lines[0].split()
    points = int(header[0])
    facilities = int(header[1])

    distances = [[0] * points for _ in range(points)]
    dist_idx = 1

    for i in range(points - 1):
        for j in range(i + 1, points):
            parts = lines[dist_idx].split()
            dist_idx += 1
            val = int(float(parts[2])) # * 1_000_000)
            distances[i][j] = val
            distances[j][i] = val

    flat_distances = _flatten_2d(distances)

    cons_len = facilities * (facilities - 1) // 2
    d_cons = [[0] * facilities for _ in range(facilities)]
    for k in range(cons_len):
        parts = lines[dist_idx + k].split()
        a = int(parts[0])
        b = int(parts[1])
        val = int(float(parts[2]))# * 1_000_000)
        d_cons[a][b] = val
        d_cons[b][a] = val

    flat_constraints = _flatten_2d(d_cons)

    return DistanceData(
        flat_distances=flat_distances,
        flat_constraints=flat_constraints,
        points=points,
        facilities=facilities,
        clients=0,
        flat_cl_distances=None,
        flat_cl_sp_distances=None,
        cl_constraints=None,
    )


def print_2d_array(array_2d: List[List[int]]) -> None:
    for row in array_2d:
        print(row)
