package core.io

import java.util.Scanner
import java.io.File
import core.models.graph.Graph
import core.models.graph.Edge
import java.io.FileNotFoundException
import java.util.InputMismatchException
import java.util.NoSuchElementException

fun readGraphFromFile(fileName: String): Graph {
    val graph = Graph()

    try {
        Scanner(File(fileName)).use { scanner ->
            val countVertexes = scanner.nextInt()
            val countEdges = scanner.nextInt()

            repeat(countVertexes) { i ->
                if (i !in graph.vertexes) {
                    graph.vertexes.add(i)
                }
            }

            repeat(countEdges) {
                val from = scanner.nextInt()
                val to = scanner.nextInt()
                val weight = scanner.nextInt()

                val edge = Edge(from, to, weight)

                if (!graph.hasEdge(edge)) {
                    graph.edges.add(edge)


                    if (from !in graph.vertexes) graph.vertexes.add(from)
                    if (to !in graph.vertexes) graph.vertexes.add(to)
                }
            }
            return graph
        }
    }
    catch (e: FileNotFoundException) {
        println("Ошибка: Файл не найден!")
        return Graph()
    }
    catch (e: InputMismatchException) {
        println("Ошибка: Несоответствие типов! Ожидаются целые числа!")
        return Graph()
    }
    catch (e: NoSuchElementException) {
        println("Ошибка: Недостаточно данных в файле!")
        return Graph()
    }
    catch (e: IllegalArgumentException) {
        println("Ошибка: Нельзя передать отрицательный вес ребер!")
        return Graph()
    }
    catch (e: Exception) {
        println("Неизвестная ошибка: ${e.message}!")
        return Graph()
    }
}