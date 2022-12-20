package ua.wwind.glotov.labirint_maker


enum class Directions {
    NORTH, SOUTH, EAST, WEST, START, END,
}

enum class CellType(val sign: String) {
    WALL("▓▓"),
    PATH("▫▫"),
    CROSS("++"),
    LOCK("ûû"),
    FULL_LOCK("ÛÛ"),
}

data class Cell(val x: Int, val y: Int, var type: CellType? = null) {
    fun getNeighborsWithCellType(
        cellList: List<Cell>,
        filterType: (CellType?) -> Boolean,
    ): List<Neighbor> {
        return cellList.filter {
            (it.x == x - 1 || it.x == x + 1 || it.y == y - 1 || it.y == y + 1)
                    && (it.x == x || it.y == y)
                    && filterType(it.type)
        }.map {
            Neighbor(it, getDirectionToNeighbor(this, it))
        }
    }

    private fun getDirectionToNeighbor(cell: Cell, neighbor: Cell): Directions {
        return if (neighbor.x < cell.x) Directions.WEST
        else if (neighbor.x > cell.x) Directions.EAST
        else if (neighbor.y < cell.y) Directions.NORTH
        else Directions.SOUTH
    }

    fun isEndOfMap(game: Game) = x == game.width - 1 && y == game.height - 1
}

data class Neighbor(val cell: Cell, val direction: Directions)

data class Move(
    val cell: Cell,
    val direction: Directions? = null,
    var neighbors: List<Neighbor>? = null,
)

class Game(
    val width: Int, val height: Int,
    var path: MutableList<Directions> = mutableListOf(Directions.START),
) {

    private val cellList: List<Cell> = run {
        val mutList: MutableList<Cell> = mutableListOf()
        for (y in 0 until width)
            for (x in 0 until height)
                mutList.add(Cell(x, y))
        mutList.toList()
    }
    private var currentCell: Cell = cellList[0]

    private fun buildPathToNextCell(
        move: Move,
        nextMove: Move,
        crossStack: MutableList<Move>,
    ): Move {
        nextMove.cell.type = CellType.PATH
        if (move.direction != nextMove.direction
            && (move.neighbors?.size ?: 0) > 1
        ) {
            move.cell.type = CellType.CROSS
            crossStack.add(move)
        }
        move.cell.getNeighborsWithCellType(cellList) {
            it !in listOf(CellType.PATH, CellType.CROSS, CellType.FULL_LOCK)
        }.also {
            for (neighbor in it) {
                when (neighbor.cell.type) {
                    CellType.LOCK -> if (!neighbor.cell.isEndOfMap(this)) neighbor.cell.type =
                        CellType.FULL_LOCK
                    null -> neighbor.cell.type = CellType.LOCK
                    else -> {}
                }
            }
        }
        return buildPathFromCell(nextMove, crossStack)
    }

    private fun buildPathFromCell(move: Move, crossStack: MutableList<Move>): Move {
        move.neighbors = move.cell.getNeighborsWithCellType(cellList) { it == null }
        return if (move.neighbors?.isEmpty() != false) move
        else if (move.cell.isEndOfMap(this)) {
            for (neighbor in move.neighbors!!) neighbor.cell.type = CellType.FULL_LOCK
            return move
        } else move.neighbors!!.random().run {
            buildPathToNextCell(move, Move(cell, direction), crossStack)
        }
    }

    init {
        currentCell.type = CellType.CROSS
        val crossList: MutableList<Move> = mutableListOf()
        (Move(currentCell).let {
            crossList.add(it)
            buildPathFromCell(it, crossList)
        }).let {
            println("Last move is ${it.cell} with dir ${it.direction}")
            printMap()
            var lastMove: Move = it
            var pathFound: Boolean = it.cell.isEndOfMap(this)
            while (crossList.isNotEmpty()) {
                val indexCross = crossList.size / 3
                val middleCross = crossList[indexCross]
                middleCross.neighbors?.filter { neighbor ->
                    neighbor.cell.type == CellType.LOCK
                }.let { neighborList ->
                    if ((neighborList?.size ?: 0) < 2 || pathFound) crossList.removeAt(indexCross)
                    if (neighborList?.isEmpty() != false) null
                    else {
                        val neighbor = neighborList.random()
                        neighbor.cell.type = CellType.PATH
                        lastMove =
                            buildPathFromCell(Move(neighbor.cell, neighbor.direction),
                                crossList)
                        if (!pathFound && lastMove.cell.isEndOfMap(this)) {
                            pathFound = true
                            // remove last 10 cross to take path in safe
                            for (i in crossList.size - 1 downTo kotlin.math.max(crossList.size - 10,
                                0))
                                crossList.removeAt(i)
                        }
                        println("Last move is ${lastMove.cell} with dir ${lastMove.direction}")
                    }
                }
            }
        }
        for (cell in cellList.filter { it.type !in listOf(CellType.PATH, CellType.CROSS) }) {
            cell.type = CellType.WALL
        }

        printMap()
    }

    fun printMap() {
        var y: Int = 0
        var mapLine = "  "
        println("".padEnd((width + 2) * 2, CellType.WALL.sign.toCharArray()[0]))
        for (cell in cellList) {
            if (cell.y != y) {
                y = cell.y
                mapLine += CellType.WALL.sign
                println(mapLine)
                mapLine = CellType.WALL.sign
            }
            mapLine += cell.type?.sign ?: "__"
        }
        println(mapLine)
        println("".padEnd((width + 2) * 2, CellType.WALL.sign.toCharArray()[0]))
    }
}

fun main(args: Array<String>) {
    val game = Game(30, 30)
}
