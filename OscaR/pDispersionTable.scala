package oscar.cp.mymodels

import oscar.cp._

import scala.io.Source

object pDispersionTable extends CPModel with App {

  val filename = if (args.length > 0) args(0) else "data/input.txt"
  val searchHeuristic = if (args.length > 1) args(1) else ""
  val lines = Source.fromFile(filename).getLines().flatMap(_.split("\\s+")).filter(_.nonEmpty)
  println(s"OscaR with Table constraints")
  println(s"Filename: ${filename}")

  def nextInt: Int = lines.next().toInt
  def nextDouble: Double = lines.next().toDouble

  val nLocations = nextInt
  val nFacilities = nextInt

  val distance = Array.ofDim[Int](nLocations, nLocations)
  for (i <- 0 until nLocations) {
    for (j <- i + 1 until nLocations) {
      val i_ = nextInt
      val j_ = nextInt
      distance(i)(j) = nextDouble.toInt
      distance(j)(i) = distance(i)(j)
    }
  }

  val minDistance = Array.ofDim[Int](nFacilities, nFacilities)
  for (i <- 0 until nFacilities) {
    for (j <- i + 1 until nFacilities) {
      val i_ = nextInt
      val j_ = nextInt
      minDistance(i)(j) = nextDouble.toInt
      minDistance(j)(i) = minDistance(i)(j)
    }
  }

  val maxDist = distance.map(_.max).max

  val x = Array.fill(nFacilities)(CPIntVar(0 until nLocations))

  val FF = for {
    i <- 0 until nFacilities
    j <- i + 1 until nFacilities
  } yield {

    val ff = CPIntVar(0 to maxDist)
    val dlb = minDistance(i)(j)

    val allowed = for {
      loc1 <- 0 until nLocations
      loc2 <- 0 until nLocations
      if loc1 != loc2
      dist = distance(loc1)(loc2)
      if dist > dlb
    } yield Array(loc1, loc2, dist)

    add(table(Array(x(i), x(j), ff), allowed.toArray))

    ff
  }

  val objectiveVar = minimum(FF)
  maximize(objectiveVar)

  if (searchHeuristic == "domwdeg") {
    println("Using dom/wdeg")
    search {
      binaryMinDomOnWeightedDegree(x)
    }
  }
  else {
    search {
      println("Using first-fail")
      binaryFirstFail(x)
    }
  }

  val t0 = System.currentTimeMillis()
  onSolution {
    val t1 = System.currentTimeMillis()
    val elapsedTime = (t1 - t0) / 1000.0
    println(s"Solution found! Objective: ${objectiveVar.value}")
    println(s"Locations: ${x.map(_.value).mkString(", ")}")
    println(s"Time (s): ${elapsedTime}")
  }

  val stats = start(timeLimit = 3600)

  println(stats)
  println(s"Total Time (s): ${stats.time / 1000.0}")
  println(s"Nodes: ${stats.nNodes}")
}