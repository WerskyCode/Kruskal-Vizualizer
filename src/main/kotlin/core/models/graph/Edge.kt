package core.models.graph

data class Edge(
    val from: Int,
    val to: Int,
    val weight: Int
) {
    // Отрисовка кривая поэтому пусть меньшее значение входящей вершины - from, а большее - to
    val u: Int = if (from < to) from else to
    val v: Int = if (to < from) to else from

    override fun equals(other: Any?): Boolean {
        // Проверка того же объекта в памяти
        if (this === other) return true
        // Проверка, что объект другой
        if (other !is Edge) return false

        return weight == other.weight && u == other.u && v == other.v

    }

    override fun hashCode(): Int {
        // Ищем наименьший id вершины и наибольший id вершины
        val minV = u
        val maxV = v

        // Чтобы хеш коды были одинаковыми
        var result = minV
        result = 31 * result + maxV
        result = 31 * result + weight
        return result
    }

}