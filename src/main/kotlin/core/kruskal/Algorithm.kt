package core.kruskal

import core.models.graph.Graph
import core.models.graph.Edge
import core.models.graph.Vertex

class KruskalAlgorithm(val graph: Graph){
    val finalEdges = mutableListOf<Edge>()
    val steps = mutableListOf<KruskalStep>() // история шагов для ui

    fun run(){
        // Перед повторным запуском очистка
        finalEdges.clear()

        val sortedEdge = graph.edges.sortedBy { it.weight }
        val dsu = Dsu(graph.vertexes.size + 1)
        var countWeightMst = 0

        sortedEdge.forEach { edge ->
            val isAdded = dsu.union(edge.from, edge.to)
            if (isAdded) {
                finalEdges.add(edge)
                countWeightMst += edge.weight
            }

            steps.add(
                KruskalStep(
                    edge = edge,
                    isAdded = isAdded,
                    currentWeight = countWeightMst,
                    countEdges = finalEdges.size
                )
            )
        }

    }

}