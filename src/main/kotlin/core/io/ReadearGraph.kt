package core.io

import java.util.Scanner
import java.io.File
import core.models.graph.Graph
import core.models.graph.Edge
import core.models.graph.Vertex
import java.io.FileNotFoundException
import java.io.IOException
import java.util.InputMismatchException

// Функция для считывания графа из файла
fun readGraphFromFile(fileName: String): Graph {
    val file = File(fileName)
    // Для экономии ресурсов, сразу чекает пустоту по весу
    if (!file.exists() || file.length() == 0L) {
        throw IOException("Выбранный файл пуст или не существует")
    }

    val graph = Graph()

    try {
        Scanner(file).use { scanner ->
            if (!scanner.hasNextInt()) {
                throw NoSuchElementException("Выбранный файл пуст или имеет неверный формат заголовка!")
            }
            // Считываем количество вершин и количество ребер с первой строки файла
            val countVertexes = scanner.nextInt()
            val countEdges = scanner.nextInt()

            if (countVertexes <= 0) {
                throw IllegalArgumentException("Количество вершин должно быть больше 0")
            }

            if (countEdges <= 0) {
                throw IllegalArgumentException("Количество ребер должно быть больше 0")
            }

            /*  В репите проблема, он читал с файла вершины и давал им номера 0-1-2 и тд,
                и ломалась логика работы,
                поэтому пробую временный mutableList для того чтобы просто записать все ребра
                а потом вытащить уникальные id

               * repeat(countVertexes) { i ->
               * graph.vertexes.add(Vertex(id = i))
            }
            */

            val tempEdges = mutableListOf<Edge>()
            var actualLinesRead = 0 // Сколько реально ребер прочитали

            while (scanner.hasNext()) {
                if (scanner.hasNextInt()) {
                    val from = scanner.nextInt()


                    if (!scanner.hasNextInt()) throw InputMismatchException("Неполная запись ребра: отсутствует вершина 'to'")
                    val to = scanner.nextInt()

                    if (!scanner.hasNextInt()) throw InputMismatchException("Неполная запись ребра: отсутствует параметр weight")
                    val weight = scanner.nextInt()

                    // Заперт петель
                    if (from == to) {
                        throw IllegalArgumentException("Петля на вершине $from запрещена")
                    }

                    tempEdges.add(Edge(from, to, weight))
                    actualLinesRead++

                } else {
                    // Мусорок
                    throw InputMismatchException("Несоответсвие типов. Ожидаемые значения должны быть типа Int")
                }
            }

            if (actualLinesRead != countEdges) {
                throw InputMismatchException("Несовпадение данных. В заголовке $countEdges а на деле $actualLinesRead")
            }

            // Ну тут по сути должно будет работать с любыми упомянутыми вершинами,
            // конечно, если валиден файл
            // flatMap преобразует все в один плоский список а distinct удаляет дубликаты
            val vertexIdsInEdges = tempEdges.flatMap { listOf(it.from, it.to) }.distinct().sorted()

            // НА ВОТ ЭТО:
            if (vertexIdsInEdges.size != countVertexes) {
                throw InputMismatchException("В графе есть изолированные вершины! Заявлено вершин: $countVertexes, а в рёбрах задействовано только: ${vertexIdsInEdges.size}.")
            }
            if (vertexIdsInEdges.any { it < 0 || it >= countVertexes }) {
                throw InputMismatchException("Несоответствие формата: индексы вершин выходят за границы от 0 до ${countVertexes - 1}")
            }

            vertexIdsInEdges.forEach { id ->
                graph.vertexes.add(Vertex(id = id))
            }

            tempEdges.forEach { edge ->
                if (!graph.hasEdge(edge)) {
                graph.edges.add(edge)
                }
            }
        }
    }

    catch (e: FileNotFoundException) {
        println("Ошибка: Файл не найден")
        throw FileNotFoundException("Файл не найден по указанному пути")
    }

    // Для ошибки несоответсвия типов
    // Это еще разновидность ошибок нехватки данных и наследник класса NoSuchElementException.
    // Сначала надо ловить ее и потом ловить ее и потом уже NoSuchElement
    catch (e: InputMismatchException) {
        println("Ошибка: Несоответсвие типов. Ожидаются целые числа")
        throw IOException(e.message ?: "Несоответсвтие типов")
    }

    catch (e: NoSuchElementException) {
        println("Ошибка: Недостаточно данных в файле!")
        throw IOException(e.message ?: "Недостаточно данных в файле графа")
    }

        // Не факт что отрицательный, может просто строка из буков
    catch (e: IllegalArgumentException) {
        println("Ошибка: недопустимые параметры графа")
        throw IOException(e.message ?: "Недопустимые параметры графа!")
    }

    catch (e: Exception) {
        println("Неизвестная ошибка ${e.message}!")
        throw IOException(e.message ?: "Неизвестная ошибка")
    }

    return graph
}