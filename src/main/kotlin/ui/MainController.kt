package ui

import core.models.graph.Graph
import core.io.readGraphFromFile
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.canvas.Canvas
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.FileChooser
import java.io.File
import java.io.FileWriter

class MainController {

    val root = BorderPane()
    private val canvas = GraphCanvas()
    private val miniMapCanvas = Canvas(200.0, 150.0)  // Мини-карта
    private val logArea = TextArea()
    private val statsLabel = Label("Готов к работе")

    // Элементы управления
    private val stepForwardBtn = Button("Шаг вперед")
    private val stepBackBtn = Button("Шаг назад")
    private val startBtn = Button("Автозапуск")
    private val pauseBtn = Button("Пауза")
    private val resetBtn = Button("Перезапуск")
    private val clearBtn = Button("Очистить")
    private val loadBtn = Button("Загрузить")
    private val saveBtn = Button("Сохранить")

    private val speedSlider = Slider(1.0, 10.0, 5.0)
    private val speedLabel = Label("Скорость: 5")

    private val mstWeightLabel = Label("Вес MST: 0")
    private val edgesCountLabel = Label("Ребер в MST: 0")
    private val stepLabel = Label("Шаг: 0/0")

    private var isRunning = false
    private var currentStep = 0
    private var totalSteps = 0

    init {
        setupUI()
        setupEventHandlers()
    }

    private fun setupUI() {
        root.widthProperty().addListener { _, _, newWidth ->
            canvas.width = newWidth.toDouble()
        }
        root.heightProperty().addListener { _, _, newHeight ->
            canvas.height = (newHeight.toDouble() - 200).coerceAtLeast(400.0)
        }
        canvas.width = root.width
        canvas.height = root.height - 200

        logArea.isEditable = false
        logArea.prefHeight = 150.0
        logArea.appendText("Добро пожаловать в визуализатор алгоритма Краскала!\n")

        //верхняя панель(кнопки файла и управления)
        val topPanel = HBox(10.0).apply {
            children.addAll(
                loadBtn, saveBtn, clearBtn,
                Separator().apply { orientation = javafx.geometry.Orientation.VERTICAL },
                startBtn, pauseBtn
            )
            padding = Insets(10.0)
            alignment = Pos.CENTER_LEFT
        }

        //правая панель
        val rightPanel = VBox(15.0).apply {
            padding = Insets(15.0)
            alignment = Pos.TOP_CENTER
            prefWidth = 350.0

            val titleLabel = Label("ALGORITHM: Kruskal").apply {
                style = "-fx-font-size: 16px; -fx-font-weight: bold;"
            }

            val stepForwardBtnStyled = Button("Шаг вперед").apply {
                prefWidth = 200.0
                prefHeight = 40.0
            }
            val stepBackBtnStyled = Button("Шаг назад").apply {
                prefWidth = 200.0
                prefHeight = 40.0
            }
            val currentStepLabel = Label("[Current: Step 0]").apply {
                style = "-fx-font-size: 14px; -fx-font-family: monospace;"
            }
            val resetBtnStyled = Button("Перезапуск").apply {
                prefWidth = 200.0
                prefHeight = 40.0
            }

            val speedTitleLabel = Label("Playback Speed")
            val speedControlBox = HBox(10.0).apply {
                alignment = Pos.CENTER
                children.addAll(
                    Label("🐢").apply { style = "-fx-font-size: 20px; -fx-text-fill: green;" },
                    Label("Slow"),
                    speedSlider.apply { prefWidth = 150.0 },
                    Label("Fast"),
                    Label("🐇").apply { style = "-fx-font-size: 20px;" }
                )
            }
            speedSlider.valueProperty().addListener { _, _, newValue ->
                speedLabel.text = "Скорость: ${newValue.toInt()}"
            }


            val miniMapLabel = Label("Overview").apply {
                style = "-fx-font-weight: bold; -fx-font-size: 12px;"
            }
            val miniMapBorder = BorderPane(miniMapCanvas).apply {
                style = "-fx-border-color: gray; -fx-border-width: 1; -fx-background-color: white;"
                prefWidth = 200.0
                prefHeight = 150.0
            }

            children.addAll(
                titleLabel,
                Separator(),
                stepForwardBtnStyled,
                stepBackBtnStyled,
                currentStepLabel,
                resetBtnStyled,
                speedTitleLabel,
                speedControlBox,
                speedLabel,
                miniMapLabel,
                miniMapBorder
            )
        }

        val bottomPanel = VBox(10.0).apply {
            val statsBox = HBox(20.0).apply {
                children.addAll(mstWeightLabel, edgesCountLabel, stepLabel)
                padding = Insets(5.0, 10.0, 5.0, 10.0)
                alignment = Pos.CENTER_LEFT
            }

            val logTitleLabel = Label("LOG OUTPUT").apply {
                style = "-fx-font-weight: bold; -fx-font-size: 12px;"
            }

            children.addAll(statsBox, logTitleLabel, logArea)
        }

        val centerPane = ScrollPane(canvas).apply {
            isFitToWidth = true
            isFitToHeight = true
        }

        root.top = topPanel
        root.center = centerPane
        root.right = rightPanel
        root.bottom = bottomPanel

        BorderPane.setMargin(canvas, Insets(10.0))
        BorderPane.setMargin(rightPanel, Insets(0.0, 0.0, 0.0, 5.0))

        drawMiniMap()
    }

    private fun drawMiniMap() {
        val gc = miniMapCanvas.graphicsContext2D
        val canvasWidth = miniMapCanvas.width
        val canvasHeight = miniMapCanvas.height

        gc.clearRect(0.0, 0.0, canvasWidth, canvasHeight)

        val vertices = canvas.getVertices()
        val edges = canvas.getEdges()

        if (vertices.isEmpty()) {
            gc.stroke = Color.GRAY
            gc.strokeText("No graph loaded", canvasWidth / 2 - 50, canvasHeight / 2)
            return
        }

        val maxX = vertices.values.maxOf { it.x }
        val maxY = vertices.values.maxOf { it.y }
        val minX = vertices.values.minOf { it.x }
        val minY = vertices.values.minOf { it.y }

        val rangeX = maxX - minX
        val rangeY = maxY - minY

        val padding = 10.0
        val scaleX = (canvasWidth - 2 * padding) / rangeX.coerceAtLeast(1.0)
        val scaleY = (canvasHeight - 2 * padding) / rangeY.coerceAtLeast(1.0)
        val scale = minOf(scaleX, scaleY)

        //Вычисляем смещение для центрирования
        val offsetX = (canvasWidth - rangeX * scale) / 2 - minX * scale
        val offsetY = (canvasHeight - rangeY * scale) / 2 - minY * scale

        //Рисуем рёбра
        edges.forEach { edgeData ->
            val fromVertex = vertices[edgeData.from] ?: return@forEach
            val toVertex = vertices[edgeData.to] ?: return@forEach

            val fromX = fromVertex.x * scale + offsetX
            val fromY = fromVertex.y * scale + offsetY
            val toX = toVertex.x * scale + offsetX
            val toY = toVertex.y * scale + offsetY

            //Определяем цвет ребра по состоянию
            val edge = core.models.graph.Edge(edgeData.from, edgeData.to, edgeData.weight)
            val state = canvas.getEdgeState(edge)

            val color = when (state) {
                EdgeState.ADDED -> Color.LIMEGREEN
                EdgeState.CURRENT -> Color.YELLOW
                EdgeState.REJECTED -> Color.RED
                else -> Color.GRAY
            }

            gc.stroke = color
            gc.lineWidth = 1.0
            gc.strokeLine(fromX, fromY, toX, toY)
        }

        //вершины
        vertices.values.forEach { vertex ->
            val x = vertex.x * scale + offsetX
            val y = vertex.y * scale + offsetY

            gc.fill = Color.STEELBLUE
            gc.fillOval(x - 3, y - 3, 6.0, 6.0)
        }
    }

    private fun createTopPanel(): HBox {
        val btnPanel = HBox(10.0).apply {
            children.addAll(
                loadBtn, saveBtn, clearBtn,
                Separator().apply { orientation = javafx.geometry.Orientation.VERTICAL },
                startBtn, pauseBtn, stepForwardBtn, stepBackBtn, resetBtn
            )
            padding = Insets(10.0)
            alignment = Pos.CENTER_LEFT
        }

        val speedPanel = VBox(5.0).apply {
            children.addAll(speedSlider, speedLabel)
            padding = Insets(10.0)
            alignment = Pos.CENTER
        }

        return HBox(20.0, btnPanel, speedPanel)
    }

    private fun createBottomPanel(): HBox {
        val statsPanel = HBox(20.0).apply {
            children.addAll(mstWeightLabel, edgesCountLabel, stepLabel)
            padding = Insets(10.0)
            alignment = Pos.CENTER_LEFT
        }

        return HBox(statsPanel, Region().apply { HBox.setHgrow(this, Priority.ALWAYS) })
    }

    private fun setupEventHandlers() {
        // Кнопки
        stepForwardBtn.setOnAction {
            stepForward()
            drawMiniMap()
        }
        stepBackBtn.setOnAction {
            stepBackward()
            drawMiniMap()
        }
        startBtn.setOnAction { startAlgorithm() }
        pauseBtn.setOnAction { pauseAlgorithm() }
        resetBtn.setOnAction {
            resetAlgorithm()
            drawMiniMap()
        }
        clearBtn.setOnAction {
            clearGraph()
            drawMiniMap()
        }
        loadBtn.setOnAction {
            loadGraph()
            drawMiniMap()
        }
        saveBtn.setOnAction { saveGraph() }

        // Ползунок скорости
        speedSlider.valueProperty().addListener { _, _, newValue ->
            speedLabel.text = "Скорость: ${newValue.toInt()}"
        }

        // Обработка кликов на холсте
        canvas.onVertexAdded = { id, x, y ->
            logArea.appendText("Добавлена вершина $id в точке ($x, $y)\n")
            drawMiniMap()  // Обновляем мини-карту
        }

        canvas.onEdgeAdded = { from, to, weight ->
            logArea.appendText("Добавлено ребро $from -> $to с весом $weight\n")
            drawMiniMap()  // Обновляем мини-карту
        }
    }

    private fun stepForward() {
        if (canvas.graph.edges.isEmpty()) {
            showAlert("Ошибка", "Граф пуст! Создайте или загрузите граф.")
            return
        }

        val result = canvas.stepForward()
        updateStats(result)
        logArea.appendText("Шаг ${currentStep + 1}: ${result.message}\n")
        currentStep++
    }

    private fun stepBackward() {
        if (currentStep > 0) {
            val result = canvas.stepBackward()
            updateStats(result)
            logArea.appendText("Откат к шагу ${currentStep - 1}\n")
            currentStep--
        }
    }

    private fun startAlgorithm() {
        if (canvas.graph.edges.isEmpty()) {
            showAlert("Ошибка", "Граф пуст!")
            return
        }

        isRunning = true
        logArea.appendText("Запуск алгоритма...\n")

        //Запускаем автоматическое выполнение
        Thread {
            while (isRunning && currentStep < totalSteps) {
                Thread.sleep((11 - speedSlider.value).toLong() * 100)
                javafx.application.Platform.runLater {
                    stepForward()
                    drawMiniMap()  // Обновляем мини-карту
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private fun pauseAlgorithm() {
        isRunning = false
        logArea.appendText("Пауза\n")
    }

    private fun resetAlgorithm() {
        isRunning = false
        currentStep = 0
        canvas.reset()
        logArea.appendText("Сброс алгоритма\n")
        updateStats(null)
        drawMiniMap()  // Обновляем мини-карту
    }

    private fun clearGraph() {
        resetAlgorithm()
        canvas.clear()
        logArea.appendText("Граф очищен\n")
        drawMiniMap()  // Обновляем мини-карту
    }

    private fun loadGraph() {
        val fileChooser = FileChooser().apply {
            title = "Загрузить граф"
            extensionFilters.add(FileChooser.ExtensionFilter("Text files", "*.txt"))
        }

        val file = fileChooser.showOpenDialog(canvas.scene.window)
        if (file != null) {
            try {
                val graph = readGraphFromFile(file.absolutePath)
                canvas.loadGraph(graph)
                logArea.appendText("Граф загружен из ${file.name}\n")
                logArea.appendText("Вершин: ${graph.vertices.size}, Ребер: ${graph.edges.size}\n")
                drawMiniMap()  // Обновляем мини-карту
            } catch (e: Exception) {
                showAlert("Ошибка загрузки", "Не удалось загрузить файл: ${e.message}")
            }
        }
    }

    private fun saveGraph() {
        val fileChooser = FileChooser().apply {
            title = "Сохранить граф"
            extensionFilters.add(FileChooser.ExtensionFilter("Text files", "*.txt"))
        }

        val file = fileChooser.showSaveDialog(canvas.scene.window)
        if (file != null) {
            try {
                saveGraphToFile(file, canvas.graph)
                logArea.appendText("Граф сохранен в ${file.name}\n")
            } catch (e: Exception) {
                showAlert("Ошибка сохранения", "Не удалось сохранить файл: ${e.message}")
            }
        }
    }

    private fun saveGraphToFile(file: File, graph: Graph) {
        FileWriter(file).use { writer ->
            writer.write("${graph.vertices.size} ${graph.edges.size}\n")
            graph.edges.forEach { edge ->
                writer.write("${edge.from} ${edge.to} ${edge.weight}\n")
            }
        }
    }

    private fun updateStats(result: AlgorithmStepResult?) {
        if (result != null) {
            mstWeightLabel.text = "Вес MST: ${result.currentWeight}"
            edgesCountLabel.text = "Ребер в MST: ${result.edgesCount}"
            stepLabel.text = "Шаг: ${currentStep + 1}/${totalSteps}"
        } else {
            mstWeightLabel.text = "Вес MST: 0"
            edgesCountLabel.text = "Ребер в MST: 0"
            stepLabel.text = "Шаг: 0/0"
        }
    }

    private fun showAlert(title: String, message: String) {
        val alert = Alert(Alert.AlertType.WARNING).apply {
            setTitle(title)
            headerText = null
            contentText = message
        }
        alert.showAndWait()
    }
}

// Класс для хранения результата шага алгоритма
data class AlgorithmStepResult(
    val message: String,
    val currentWeight: Int,
    val edgesCount: Int,
    val isEdgeAdded: Boolean
)