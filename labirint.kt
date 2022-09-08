package ua.wwind.glotov.labirint_maker

import java.util.*

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
        filterType: ((CellType?) -> Boolean)?,
    ): List<Neighbor> {
        return cellList.filter {
            (it.x == x - 1 || it.x == x + 1 || it.y == y - 1 || it.y == y + 1)
                    && (it.x == x || it.y == y) && !(it.x == x && it.y == y)
                    && filterType?.let { filter -> filter(it.type) } ?: true
        }.let { cells ->
            Array(cells.size) {
                val cell = cells[it]
                Neighbor(cell, getDirectionToNeighbor(this, cell))
            }.toList()
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

    private fun buildPathToNextCell(move: Move, nextMove: Move, crossStack: Stack<Move>): Move {
        nextMove.cell.type = CellType.PATH
        if (move.direction != nextMove.direction
            && (move.neighbors?.size ?: 0) > 1
        ) {
            move.cell.type = CellType.CROSS
            crossStack.push(move)
        }
        move.cell.getNeighborsWithCellType(cellList) {
            it !in listOf(CellType.PATH, CellType.CROSS, CellType.FULL_LOCK)
        }.also {
            for (neighbor in it) {
                when (neighbor.cell.type) {
                    CellType.LOCK -> if (!neighbor.cell.isEndOfMap(this)) neighbor.cell.type = CellType.FULL_LOCK
                    null -> neighbor.cell.type = CellType.LOCK
                    else -> {}
                }
            }
        }
        return buildPathFromCell(nextMove, crossStack)
    }

    private fun buildPathFromCell(move: Move, crossStack: Stack<Move>): Move {
        move.neighbors = move.cell.getNeighborsWithCellType(cellList) { it == null }
        return if (move.neighbors?.isEmpty() != false) move
        else move.neighbors!!.random().run {
            buildPathToNextCell(move, Move(cell, direction), crossStack)
        }
    }

    init {
        currentCell.type = CellType.CROSS
        val crossStack: Stack<Move> = Stack()
        (Move(currentCell).let {
            crossStack.push(it)
            buildPathFromCell(it, crossStack)
        }).let {
            println("Last move is ${it.cell} with dir ${it.direction}")
            var lastMove: Move = it
            var pathFound: Boolean = it.cell.isEndOfMap(this)
            while (!crossStack.isEmpty()) {
                val lastCross = crossStack.pop()
                if (pathFound && (1..20).random() != 1) continue
                lastCross.neighbors?.filter { neighbor ->
                    neighbor.cell.type == CellType.LOCK
                }.let { neighborList ->
                    if (neighborList?.isEmpty() != false) null
                    else {
                        for (neighbor in neighborList) {
                            neighbor.cell.type = CellType.PATH
                            lastMove =
                                buildPathFromCell(Move(neighbor.cell, neighbor.direction),
                                    crossStack)
                            pathFound = pathFound || lastMove.cell.isEndOfMap(this)
                        }
                    }
                }
            }
            println("Last move is ${lastMove.cell} with dir ${lastMove.direction}")
        }
        for (cell in cellList.filter { it.type !in listOf(CellType.PATH, CellType.CROSS) }) {
            cell.type = CellType.WALL
        }

        printMap()
    }

    fun printMap() {
        var y: Int = 0
        var mapLine = ""
        for (cell in cellList) {
            if (cell.y != y) {
                y = cell.y
                println(mapLine)
                mapLine = ""
            }
            mapLine += cell.type?.sign ?: "_"
        }
        println(mapLine)
    }
}

fun main(args: Array<String>) {
    val game = Game(25, 25)
}
