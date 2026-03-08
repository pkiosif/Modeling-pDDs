name := "oscar-main"
version := "4.1.0"
scalaVersion := "2.13.11"

// ΜΟΝΟ οι απαραίτητες εξωτερικές βιβλιοθήκες (όχι ο ίδιος ο oscar)
libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
  "org.scalatest" %% "scalatest" % "3.2.16" % Test
)

// Αφαιρέσαμε τα resolvers και τα credentials που προκαλούσαν το σφάλμα