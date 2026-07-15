package ui

import core.kruskal.KruskalStep
import core.models.graph.Edge
import core.models.graph.Graph
import core.models.graph.Vertex
import kotlin.math.pow
import kotlin.math.sqrt
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.text.TextAlignment

class GraphCanvas : Canvas(1000.0, 600.0) {
    var graph = Graph()

    private var vertexes = mutableMapOf<Int, Vertex>()     //Локальное хранилище вершин с координатами(для отрисовки)
    private var edges = mutableListOf<Edge>()


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
                val isTooClose = vertexes.values.any { existing ->
                    val distance = sqrt((existing.x - x).pow(2.0) + (existing.y - y).pow(2.0))
                    distance < VERTEX_RADIUS * 2.0
                }

                if (isTooClose) {
                    showAlert(
                        "Ошибка размещения",
                        "Нельзя поставить вершину так близко к другой!"
                        )
                } else {
                    createVertex(x, y)
                }
            }

        }
    }

    private fun createVertex(x: Double, y: Double) {
        val id = nextVertexId++
        vertexes[id] = Vertex(id, x, y)
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
                edges.add(Edge(from, to, weight))
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
        /*
        Лучше так не писать, потому что если менять forEach на !in-line функцию,
        то оператор return внутри {} лямбда функции будет выдавать ошибку (возврата в findVertexAt нет)
        vertexes.forEach { (id, vertex) ->
            val distance = sqrt((vertex.x - x).pow(2.0) + (vertex.y - y).pow(2.0))
            if (distance < VERTEX_RADIUS) {
                return id
            }
        }
        return null

        Поэтому такой вариант будет лучше
        */
        return vertexes.values // берем значение
            .find {  vertex ->
                sqrt((vertex.x - x).pow(2.0) + (vertex.y - y).pow(2.0)) < VERTEX_RADIUS
            }
            ?.id

    }

    fun loadGraph(graph: Graph) {
        this.graph = graph
        vertexes.clear()
        edges.clear()
        edgeStates.clear()
        nextVertexId = graph.vertexes.maxOfOrNull { it.id }?.plus(1) ?: 0

        val totalVertices = graph.vertexes.size

        // Находим центр холста
        val centerX = width / 2.0
        val centerY = height / 2.0

        // Выбираем радиус так, чтобы вершины не прижимались вплотную к краям экрана
        val radius = minOf(width, height) / 2.5

        graph.vertexes.forEachIndexed { index, vertex ->
            val id = vertex.id

            // Распределяем угол равномерно: 2 * PI * (текущий_индекс / всего_вершин)
            val angle = 2.0 * Math.PI * index / totalVertices

            // Считаем тригонометрические координаты
            val x = centerX + radius * Math.cos(angle)
            val y = centerY + radius * Math.sin(angle)

            vertexes[id] = Vertex(id, x, y)
        }

        // Загружаем рёбра
        graph.edges.forEach { edge ->
            edges.add(Edge(edge.from, edge.to, edge.weight))
            edgeStates[edge] = EdgeState.PENDING
        }

        draw()
    }

    // Не то, не должен знать канвас про алгоритм
    /*fun stepForward(): AlgorithmStepResult {
        return AlgorithmStepResult("Алгоритм не реализован (backend)", 0, 0, false)
    }


    fun stepBackward(): AlgorithmStepResult {
        return AlgorithmStepResult("Откат не реализован (backend)", 0, 0, false)
    }*/

    // Вместо этого попробую делать подсветку нужного шага и все.
    // Этот метод подсвечивает ребро на шаге вперед
    fun highlightStep(step: KruskalStep) {
        val edgeOnCanvas = edges.find {
            (it.from == step.edge.from && it.to == step.edge.to) ||
                    (it.to == step.edge.from && it.from == step.edge.to)
        }

        if (edgeOnCanvas != null) {
            // Если isAdded, то зеленый, а если создает цикл, то красим в красный
            edgeStates[edgeOnCanvas] = if (step.isAdded) EdgeState.ADDED else EdgeState.REJECTED
            // Обновление экрана
            draw()
        }
    }

    // Данный метод при возврате шага красит выделенное ребро в серый
    fun unhighlightStep(step: KruskalStep) {
        val edgeOnCanvas = edges.find {
            (it.from == step.edge.from && it.to == step.edge.to) ||
                    (it.to == step.edge.from && it.from == step.edge.to)
        }

        if (edgeOnCanvas != null) {
            edgeStates[edgeOnCanvas] = EdgeState.PENDING
            draw()
        }

    }

    // Метод для временной подсветки текущего проверяемого ребра желтым
    fun highlightCurrentEdge(step: KruskalStep) {
        val edgeOnCanvas = edges.find {
            (it.from == step.edge.from && it.to == step.edge.to) ||
                    (it.to == step.edge.from && it.from == step.edge.to)
        }
        if (edgeOnCanvas != null) {
            edgeStates[edgeOnCanvas] = EdgeState.CURRENT
            draw()
        }
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
        vertexes.clear()
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
            val fromVertex = vertexes[edgeData.from] ?: return@forEach
            val toVertex = vertexes[edgeData.to] ?: return@forEach

            val edge = Edge(edgeData.from, edgeData.to, edgeData.weight)
            val state = edgeStates[edge] ?: EdgeState.PENDING

            drawEdge(gc, fromVertex, toVertex, edgeData.weight, state)
        }

        vertexes.values.forEach { vertex ->
            drawVertex(gc, vertex, vertex.id == selectedVertex)
        }
    }

    private fun drawVertex(gc: GraphicsContext, vertex: Vertex, isSelected: Boolean) {
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
        from: Vertex,
        to: Vertex,
        weight: Int,
        state: EdgeState
    ) {
        val color = when (state) {
            EdgeState.ADDED -> Color.LIMEGREEN
            EdgeState.CURRENT -> Color.YELLOW
            EdgeState.REJECTED -> Color.RED
            EdgeState.PENDING -> Color.DARKGRAY
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

    // Надо не забыть махнуть на геттеры из основного класса Graph
    fun getVertices(): Map<Int, Vertex> = vertexes
    fun getEdges(): List<Edge> = edges
    fun getEdgeState(edge: Edge): EdgeState? = edgeStates[edge]

    companion object {
        const val VERTEX_RADIUS = 20.0
    }
}

enum class EdgeState {
    PENDING,    // Ожидает проверки (сер)
    CURRENT,    // Текущее проверяемое (ж)
    ADDED,      // Добавлено в MST (зел)
    REJECTED    // Отклонено (кр)
}