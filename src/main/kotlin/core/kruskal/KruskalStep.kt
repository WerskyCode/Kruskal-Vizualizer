package core.kruskal

import core.models.graph.Edge

class KruskalStep (
    val edge: Edge,
    val isAdded: Boolean,
    val currentWeight: Int,
    val countEdges: Int
)