package core.kruskal

class Dsu(val countVertex: Int) {
    // idx - id вершины, val = parents[idx] - Значение родителя
    val parents = IntArray(countVertex) {id -> id}

    fun find(id: Int): Int {
        if (parents[id] == id) return id

        parents[id] = find(parents[id])

        return parents[id]
    }

    // Объединение множеств
    fun union(x: Int, y: Int): Boolean{
        val rootX = find(x)
        val rootY = find(y)

        if (rootX == rootY) return false

        parents[rootX] = rootY
        return true
    }
}