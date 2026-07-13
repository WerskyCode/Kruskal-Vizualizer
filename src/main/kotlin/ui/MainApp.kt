package  ui

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import core.models.graph.Graph

class MainApp : Application() {
    override fun start(primaryStage: Stage){
        val controller = MainController() //главный контроллер
        val scene = Scene(controller.root, 1200.0, 800.0) //окно

        primaryStage.title = "Визуализация алгоритма Краскала"
        primaryStage.scene = scene
        primaryStage.isResizable = true
        primaryStage.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(MainApp::class.java)
        }
    }
}