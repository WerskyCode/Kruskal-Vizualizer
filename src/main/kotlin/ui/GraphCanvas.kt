package ui

import core.models.graph.Edge
import core.models.graph.Graph
import core.models.graph.Vertex
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment

class GraphCanvas : Canvas(1000.0, 600.0) {
    var graph = Graph()

    private var vertices = mutableMapOf<Int, VertexData>()     //Локальное хранилище вершин с координатами(для отрисовки)
    private var edges = mutableListOf<EdgeData>()


    private var selectedVertex: Int? = null
    private var nextVertexId = 0


    private var edgeStates = mutableMapOf<Edge, EdgeState>()

    var onVertexAdded: ((Int, Double, Double) -> Unit)? = null
    var onEdgeAdded: ((Int, Int, Int) -> Unit)? = null

    init {
        setupMouseHandlers()
        draw()
    }

    private fun setupMouseHandlers() {
        addEventHandler(MouseEvent.MOUSE_CLICKED) { event ->
            val x = event.x
            val y = event.y

            val clickedVertex = findVertexAt(x, y)

            if (clickedVertex != null) {
                handleVertexClick(clickedVertex)
            } else {
                createVertex(x, y)
            }
        }
    }

    private fun createVertex(x: Double, y: Double) {
        val id = nextVertexId++
        vertices[id] = VertexData(id, x, y)
        graph.vertexes.add(Vertex(id))
        onVertexAdded?.invoke(id, x, y)
        draw()
    }

    private fun handleVertexClick(vertexId: Int) {
        if (selectedVertex == null) {
            selectedVertex = vertexId
        } else {
            if (selectedVertex != vertexId) {
                createEdge(selectedVertex!!, vertexId)
            }
            selectedVertex = null
        }
        draw()
    }

    private fun createEdge(from: Int, to: Int) {
        val existingEdge = edges.find {
            (it.from == from && it.to == to) || (it.from == to && it.to == from)
        }

        if (existingEdge != null) {
            showAlert("Ребро существует", "Ребро между этими вершинами уже есть!")
            return
        }

        val dialog = javafx.scene.control.TextInputDialog("1").apply {
            title = "Ввод веса ребра"
            headerText = "Введите вес ребра между вершинами $from и $to"
            contentText = "Вес (целое число):"
        }

        val result = dialog.showAndWait()
        result.ifPresent { weightStr ->
            try {
                val weight = weightStr.toInt()
                val edge = Edge(from, to, weight)
                edges.add(EdgeData(from, to, weight))
                graph.edges.add(edge)
                edgeStates[edge] = EdgeState.PENDING
                onEdgeAdded?.invoke(from, to, weight)
                draw()
            } catch (e: NumberFormatException) {
                showAlert("Ошибка", "Введите корректное число!")
            }
        }
    }

    private fun findVertexAt(x: Double, y: Double): Int? {
        vertices.forEach { (id, vertex) ->
            val distance = Math.sqrt(
                Math.pow(vertex.x - x, 2.0) + Math.pow(vertex.y - y, 2.0)
            )
            if (distance < VERTEX_RADIUS) {
                return id
            }
        }
        return null
    }

    fun loadGraph(graph: Graph) {
        this.graph = graph
        vertices.clear()
        edges.clear()
        edgeStates.clear()
        nextVertexId = graph.vertexes.maxOfOrNull{ it.id }?.plus(1) ?: 0

        graph.vertexes.forEach { vertex ->
            val id = vertex.id
            val x = 100.0 + (id * 150) % (width - 200)
            val y = 100.0 + (id * 100) % (height - 200)
            vertices[id] = VertexData(id, x, y)
        }

        graph.edges.forEach { edge ->
            edges.add(EdgeData(edge.from, edge.to, edge.weight))
            edgeStates[edge] = EdgeState.PENDING
        }

        draw()
    }

    fun stepForward(): AlgorithmStepResult {
        return AlgorithmStepResult("Алгоритм не реализован (backend)", 0, 0, false)
    }


    fun stepBackward(): AlgorithmStepResult {
        return AlgorithmStepResult("Откат не реализован (backend)", 0, 0, false)
    }


    fun reset() {
        edgeStates.clear()
        graph.edges.forEach { edge ->
            edgeStates[edge] = EdgeState.PENDING
        }
        draw()
    }

    fun clear() {
        graph = Graph()
        vertices.clear()
        edges.clear()
        edgeStates.clear()
        nextVertexId = 0
        selectedVertex = null
        draw()
    }

    private fun draw() {
        val gc = graphicsContext2D
        gc.clearRect(0.0, 0.0, width, height)

        edges.forEach { edgeData ->
            val fromVertex = vertices[edgeData.from] ?: return@forEach
            val toVertex = vertices[edgeData.to] ?: return@forEach

            val edge = Edge(edgeData.from, edgeData.to, edgeData.weight)
            val state = edgeStates[edge] ?: EdgeState.PENDING

            drawEdge(gc, fromVertex, toVertex, edgeData.weight, state)
        }

        vertices.values.forEach { vertex ->
            drawVertex(gc, vertex, vertex.id == selectedVertex)
        }
    }

    private fun drawVertex(gc: GraphicsContext, vertex: VertexData, isSelected: Boolean) {
        gc.fill = if (isSelected) Color.ORANGE else Color.STEELBLUE
        gc.stroke = Color.BLACK
        gc.lineWidth = 2.0

        gc.fillOval(
            vertex.x - VERTEX_RADIUS,
            vertex.y - VERTEX_RADIUS,
            VERTEX_RADIUS * 2,
            VERTEX_RADIUS * 2
        )
        gc.strokeOval(
            vertex.x - VERTEX_RADIUS,
            vertex.y - VERTEX_RADIUS,
            VERTEX_RADIUS * 2,
            VERTEX_RADIUS * 2
        )

        gc.fill = Color.WHITE
        gc.textAlign = TextAlignment.CENTER
        gc.fillText(
            vertex.id.toString(),
            vertex.x,
            vertex.y + 5
        )
    }

    private fun drawEdge(
        gc: GraphicsContext,
        from: VertexData,
        to: VertexData,
        weight: Int,
        state: EdgeState
    ) {
        val color = when (state) {
            EdgeState.ADDED -> Color.LIMEGREEN
            EdgeState.CURRENT -> Color.YELLOW
            EdgeState.REJECTED -> Color.RED
            EdgeState.PENDING -> Color.GRAY
        }

        gc.stroke = color
        gc.lineWidth = 3.0
        gc.strokeLine(from.x, from.y, to.x, to.y)

        val midX = (from.x + to.x) / 2.0
        val midY = (from.y + to.y) / 2.0

        gc.fill = Color.BLACK
        gc.fillText(weight.toString(), midX, midY - 5)
    }

    private fun showAlert(title: String, message: String) {
        val alert = javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING).apply {
            this.title = title
            headerText = null
            contentText = message
        }
        alert.showAndWait()
    }

    fun getVertices(): Map<Int, VertexData> = vertices
    fun getEdges(): List<EdgeData> = edges
    fun getEdgeState(edge: Edge): EdgeState? = edgeStates[edge]

    companion object {
        const val VERTEX_RADIUS = 20.0
    }
}

data class VertexData(val id: Int, val x: Double, val y: Double)

data class EdgeData(val from: Int, val to: Int, val weight: Int)

enum class EdgeState {
    PENDING,    // Ожидает проверки
    CURRENT,    // Текущее проверяемое
    ADDED,      // Добавлено в MST
    REJECTED    // Отклонено
}
