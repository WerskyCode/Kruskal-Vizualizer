package core.io

import java.util.Scanner
import java.io.File
import core.models.graph.Graph
import core.models.graph.Edge
import core.models.graph.Vertex
import java.io.FileNotFoundException
import java.util.InputMismatchException

// Функция для считывания графа из файла
fun readGraphFromFile(fileName: String): Graph {
    val graph = Graph()

    try {
        Scanner(File(fileName)).use { scanner ->
            val countVertexes = scanner.nextInt()
            val countEdges = scanner.nextInt()

            repeat(countVertexes) { i ->
                graph.vertexes.add(Vertex(id = i))
            }

           repeat(countEdges) {
               val edge = Edge(scanner.nextInt(), scanner.nextInt(), scanner.nextInt())
               if (!graph.hasEdge(edge)) {
                   graph.edges.add(edge)
               }

           }
            return graph
        }
    }

    catch (e: FileNotFoundException) {
        println("Ошибка: Файл не найден!")
        return Graph()
    }

    // Для ошибки несоответсвия типов
    // Это еще разновидность ошибок нехватки данных и наследник класса NoSuchElementException.
    // Сначала надо ловить ее и потом ловить ее и потом уже NoSuchElement
    catch (e: InputMismatchException) {
        println("Ошибка: Несоответсвие типов! Ожидаются целые числа!")
        return Graph()
    }

    catch (e: NoSuchElementException) {
        println("Ошибка: Недостаточно данных в файле!")
        return Graph()
    }

        // Не факт что отрицательный, может просто строка из буков
    catch (e: IllegalArgumentException) {
        println("Ошибка: Нельзя передать отрицательный вес ребер!")
        return Graph()
    }

    catch (e: Exception) {
        println("Неизвестная ошибка ${e.message}!")
        return Graph()
    }
}