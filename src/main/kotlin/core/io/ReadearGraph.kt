package core.io

import core.models.graph.Edge
import java.util.Scanner
import java.io.File
import core.models.graph.Graph

// Пока заглушка
fun readGraphFromFile(fileName: String): Graph {
    val graph = Graph()
    val scanner = Scanner(File(fileName))

    val countVertexes = scanner.nextInt()
    val countEdges = scanner.nextInt()

    repeat(countEdges) {
        val from = scanner.nextInt()
        val to = scanner.nextInt()
        val weight = scanner.nextInt()

        graph.edges.add(Edge(from, to, weight))
    }

    return graph
}