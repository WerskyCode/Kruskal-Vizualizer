package core.models.graph

class Graph{
    val vertexes = mutableListOf<Vertex>()
    val edges = mutableListOf<Edge>()

    // Геттеры
    val vertexCount : Int get() = vertexes.size
    val edgeCount : Int get() = edges.size

    // Проверяет повторное вхождение ребра в граф
    fun hasEdge(edge: Edge): Boolean {
        // any пройдется по всему списку
        return edges.any {
            (it.from == edge.from && it.to == edge.to) ||
                    (it.from == edge.to && it.to == edge.from)
        }
    }

    // Проверка на правильность заполнения (from->to->w)
}
