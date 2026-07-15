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
    private val stepForwardBtn = Button("Шаг вперёд")
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
    private var kruskal: core.kruskal.KruskalAlgorithm? = null
    private var isCurrentStepHighlightedYellow = false
    private var previousHighlightedEdge: core.models.graph.Edge? = null

    init {
        setupUI()
        setupEventHandlers()

        drawMiniMap()
        updateButtonStates()
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

            stepForwardBtn.apply {
                prefWidth = 200.0
                prefHeight = 40.0
            }
            stepBackBtn.apply {
                prefWidth = 200.0
                prefHeight = 40.0
            }
            val currentStepLabel = Label("[Current: Step 0]").apply {
                style = "-fx-font-size: 14px; -fx-font-family: monospace;"
            }
            resetBtn.apply {
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
                stepForwardBtn,
                stepBackBtn,
                currentStepLabel,
                resetBtn,
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

        if (kruskal == null) {
            // 1. Оставляем в списке исходного графа только уникальные связи
            val uniqueEdges = canvas.graph.edges.distinctBy {
                if (it.from < it.to) "${it.from}-${it.to}" else "${it.to}-${it.from}"
            }
            canvas.graph.edges.clear()
            canvas.graph.edges.addAll(uniqueEdges)

            // 2. Скармливаем алгоритму очищенный граф
            kruskal = core.kruskal.KruskalAlgorithm(canvas.graph).apply { run() }

            totalSteps = kruskal!!.steps.size
            currentStep = 0
            isCurrentStepHighlightedYellow = false
        }

        val algorithm = kruskal!!

        if (currentStep < totalSteps) {
            val step = algorithm.steps[currentStep]

            if (!isCurrentStepHighlightedYellow) {
                // Желтое ребро
                canvas.highlightCurrentEdge(step)
                logArea.appendText("Шаг ${currentStep + 1}: рассматриваем ребро (${step.edge.from} - ${step.edge.to}) с весом ${step.edge.weight}. ")
                isCurrentStepHighlightedYellow = true
            } else {
                // Фикса цвета
                canvas.unhighlightStep(step)
                canvas.highlightStep(step)

                val logMsg = if (step.isAdded) {
                    "Ребро (${step.edge.from} - ${step.edge.to}) с весом ${step.edge.weight} добавлено в MST"
                } else {
                    "Ребро (${step.edge.from} - ${step.edge.to}) с весом ${step.edge.weight} не добавлено в MST (цикл)"
                }

                logArea.appendText(logMsg + "\n")

                val result = AlgorithmStepResult(logMsg, step.currentWeight, step.countEdges, step.isAdded)
                updateStats(result)

                currentStep++
                isCurrentStepHighlightedYellow = false // Сбрасываем флаг для следующего ребра

                // обновление статуса кнопок ибо надо смотреть можно ли назад/вперед
                updateButtonStates()
            }

        } else {
            val mstEdges = algorithm.steps.filter { it.isAdded }.map { it.edge }
            val totalWeight = mstEdges.sumOf { it.weight }
            val totalVertexes = canvas.graph.vertexes.size

            logArea.appendText("Алгоритм завершен\n")

            if (mstEdges.size == totalVertexes - 1) {
                logArea.appendText("MST построено успешно\n")
                logArea.appendText("Список ребер:\n")
                mstEdges.forEach { edge ->
                    logArea.appendText("№ Вершины ${edge.from} <-> № Вершины ${edge.to} (вес: ${edge.weight})\n")
                }
                logArea.appendText("Общий вес MST: $totalWeight\n")
            } else {
                logArea.appendText("Поскольку исходный граф несвязный, успешно построен минимальный остовный лес. \n")
                logArea.appendText("Список ребер леса:\n")
                mstEdges.forEach { edge ->
                    logArea.appendText("№ Вершины ${edge.from} <-> № Вершины ${edge.to} (вес: ${edge.weight})\n")
                }
                logArea.appendText("Общий вес остовного леса: $totalWeight\n")
            }

            isRunning = false
            updateButtonStates()
        }
    }

    private fun stepBackward() {
        val algorithm = kruskal ?: return

        // Если мы находимся в середине шага (горит желтый), просто гасим его и возвращаем к обычному состоянию
        if (isCurrentStepHighlightedYellow) {
            if (currentStep < totalSteps) {
                val step = algorithm.steps[currentStep]
                canvas.unhighlightStep(step) // Гасим желтый цвет на холсте
            }
            isCurrentStepHighlightedYellow = false // Возвращаем фазу в начало
            logArea.appendText("Откат: Снят фокус с текущего ребра\n")
            return
        }

        // Если желтый не горел, значит мы откатываем полностью завершенный шаг назад
        if (currentStep > 0) {
            currentStep--
            val step = algorithm.steps[currentStep]

            canvas.unhighlightStep(step) // Красим ребро обратно в серый (PENDING)

            logArea.appendText("Откат: Шаг ${currentStep + 1} отменен \n")

            // Обновляем статистику на основе предыдущего шага
            if (currentStep > 0) {
                val prevStep = algorithm.steps[currentStep - 1]
                val result = AlgorithmStepResult("", prevStep.currentWeight, prevStep.countEdges, prevStep.isAdded)
                updateStats(result)
            } else {
                updateStats(null)
            }

            // После отката шага мы находимся в состоянии, когда это ребро еще не исследовано (следующий шаг вперед должен подсветить его желтым)
            isCurrentStepHighlightedYellow = false

            updateButtonStates()
        }
    }

    private fun startAlgorithm() {
        if (canvas.graph.edges.isEmpty()) {
            showAlert("Ошибка", "Граф пуст!")
            return
        }

        if (kruskal == null) {
            // Очищаем граф от дубликатов на лету
            val uniqueEdges = canvas.graph.edges.distinctBy {
                if (it.from < it.to) "${it.from}-${it.to}" else "${it.to}-${it.from}"
            }
            canvas.graph.edges.clear()
            canvas.graph.edges.addAll(uniqueEdges)

            kruskal = core.kruskal.KruskalAlgorithm(canvas.graph).apply { run() }
            totalSteps = kruskal!!.steps.size
            currentStep = 0
            isCurrentStepHighlightedYellow = false
        }

        isRunning = true
        logArea.appendText("Запуск алгоритма...\n")

        // Запускаем автоматическое выполнение
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
        isCurrentStepHighlightedYellow = false
        currentStep = 0
        canvas.reset()
        kruskal = null
        logArea.appendText("Сброс алгоритма\n")
        updateStats(null)
        drawMiniMap()  // Обновляем мини-карту
        updateButtonStates()
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

                if (graph.vertexes.isEmpty() || graph.edges.isEmpty()) {
                    throw NoSuchElementException("файл пуст или не содержит корректных данных графа")
                }

                canvas.loadGraph(graph)
                logArea.appendText("Граф загружен из ${file.name}\n")
                logArea.appendText("Вершин: ${graph.vertexes.size}, Ребер: ${graph.edges.size}\n")
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
            writer.write("${graph.vertexes.size} ${graph.edges.size}\n")
            graph.edges.forEach { edge ->
                writer.write("${edge.from} ${edge.to} ${edge.weight}\n")
            }
        }
    }

    private fun updateButtonStates(){
        // Стартовое состояние кнопок
        if (kruskal == null || currentStep == 0) {
            stepBackBtn.isDisable = true
            stepForwardBtn.isDisable = false
        }

        // Конечное состояние кнопок
        else if (currentStep >= totalSteps && !isCurrentStepHighlightedYellow) {
            stepBackBtn.isDisable = false
            stepForwardBtn.isDisable = true
        }

        else {
            stepBackBtn.isDisable = false
            stepForwardBtn.isDisable = false
        }


    }

    private fun updateStats(result: AlgorithmStepResult?) {
        if (result != null) {
            mstWeightLabel.text = "Вес MST: ${result.currentWeight}"
            edgesCountLabel.text = "Ребер в MST: ${result.edgesCount}"
            stepLabel.text = "Шаг: ${currentStep}/${totalSteps}"
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