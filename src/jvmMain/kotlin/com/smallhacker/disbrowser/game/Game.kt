package com.smallhacker.disbrowser.game

import com.smallhacker.disbrowser.memory.SnesMemory
import com.smallhacker.disbrowser.util.JsonFile

class Game(
    val id: String,
    val memory: SnesMemory,
    val gameData: GameData,
    private val gameDataFile: JsonFile<GameData>
) {
    fun saveGameData() {
        gameData.cleanUp()
        gameDataFile.save(gameData)
    }
}