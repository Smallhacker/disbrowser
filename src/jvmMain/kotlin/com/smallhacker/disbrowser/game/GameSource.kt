package com.smallhacker.disbrowser.game

import com.smallhacker.disbrowser.memory.SnesMemory
import com.smallhacker.disbrowser.util.jsonFile
import org.glassfish.jersey.server.ResourceConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import javax.ws.rs.core.Configuration

private val VALID_FILE_NAME = Regex("""^[a-zA-Z0-9_ ]*$""")

class GameSource(private val gameDataDir: Path) {
    private val gameCache = ConcurrentHashMap<String, Game?>()

    fun getGame(name: String): Game? {
        return gameCache.computeIfAbsent(name) {
            loadGame(name, it)
        }
    }

    private fun loadGame(name: String, it: String): Game? {
        if (!validFileName(name)) {
            return null
        }

        val gameDataFile = gameDataDir.resolve("$it.json").toFile()
        if (!gameDataFile.exists()) {
            return null
        }

        return try {
            val gameDataJsonFile = jsonFile<GameData>(gameDataFile.toPath(), true)

            val gameData = gameDataJsonFile.load()
            val gamePath = Paths.get(gameData.path)
            val romData = Files.readAllBytes(gamePath).toUByteArray()
            val rom = SnesMemory.loadRom(romData)
            Game(name, rom, gameData, gameDataJsonFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun validFileName(name: String) = VALID_FILE_NAME.matches(name)
}

private const val GAME_SOURCE_PROPERTY = "gameSource"

fun ResourceConfig.addGameSource(path: Path) = this.property(GAME_SOURCE_PROPERTY, GameSource(path))!!
fun Configuration.getGameSource() = getProperty(GAME_SOURCE_PROPERTY)!! as GameSource