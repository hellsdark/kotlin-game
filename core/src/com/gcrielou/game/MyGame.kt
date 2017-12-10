package com.gcrielou.game

import com.badlogic.gdx.*
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport
import utils.*

class MyGame : GameBase() {

    lateinit var batch: SpriteBatch
    private lateinit var spritesCharacter: Texture
    private lateinit var spritesCubicMonster: Texture
    private lateinit var spritesEnv: Texture
    private lateinit var spritesEnemy: Texture
    private lateinit var camera: OrthographicCamera
    private lateinit var viewport: FitViewport
    private lateinit var renderer: ShapeRenderer
    private lateinit var font: BitmapFont

    private lateinit var music: Music
    private lateinit var walkSound: Music
    private lateinit var gruntSound: Music
    private lateinit var monsterGruntSound: Music
    private lateinit var monsterGruntSound2: Music
    private lateinit var monsterGruntSound3: Music

    lateinit var player: Player
    lateinit var level: Level
    lateinit var enemies: List<Character>

    private var displayGrid = false
    private var displayCoords = false
    private var hasMusic = false

    override fun create() {
        batch = SpriteBatch()
        spritesCharacter = Texture("char_sprites.png")
        spritesEnv = Texture("env_sprites.png")
        spritesEnemy = Texture("zombies_and_skeletons.png")
        spritesCubicMonster = Texture("cubic_monsters.png")
        level = Level(spritesEnv)
        camera = OrthographicCamera()
        viewport = FitViewport(Config.WORLD_WIDTH, Config.WORLD_HEIGHT, camera)
        renderer = ShapeRenderer()
        Gdx.input.inputProcessor = this

        enemies = listOf(
                Enemy(spritesEnemy, 11, 13),
                CubicMonster(spritesCubicMonster, 6, 3),
                CubicMonster(spritesCubicMonster, 8, 2)
        )
        player = Player(spritesCharacter)

        val generator = FreeTypeFontGenerator(FileHandle("OpenSans-Regular.ttf"))
        val param = FreeTypeFontGenerator.FreeTypeFontParameter()
        param.size = 16
        font = generator.generateFont(param)
        generator.dispose()

        setSounds()

        if (hasMusic) {
            music.play()
        }
    }

    private fun setSounds() {
        music = Gdx.audio.newMusic(FileHandle("winds_of_stories.mp3"))
        music.volume = 0.7f
        walkSound = Gdx.audio.newMusic(FileHandle("player/sfx_step_grass_l.mp3"))
        walkSound.volume = 0.2f
        gruntSound = Gdx.audio.newMusic(FileHandle("player/gruntsound.wav"))
        gruntSound.volume = 0.2f
        monsterGruntSound = Gdx.audio.newMusic(FileHandle("monster/cubic_hurt_sound01.wav"))
        monsterGruntSound2 = Gdx.audio.newMusic(FileHandle("monster/cubic_hurt_sound02.wav"))
        monsterGruntSound3 = Gdx.audio.newMusic(FileHandle("monster/cubic_hurt_sound03.wav"))
        monsterGruntSound.volume = 0.2f
        monsterGruntSound2.volume = 0.2f
        monsterGruntSound3.volume = 0.2f
    }

    /*
    Called 60 times/second
     */
    override fun render() {
        clearScreen()
        batch.projectionMatrix = camera.combined

        batch.use {
            draw()
        }

        if (displayGrid) {
            drawGrid()
        }
    }


    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    private fun draw() {
        handleKeys()

        level.draw(batch)

        if (player.isAlive())
            player.drawCharacter(batch)

        drawEnemies()

        if (displayCoords) {
            drawCoords()
        }

        if (spritesStack.size > 0) {
            val positionedSprite = spritesStack.removeAt(0)
            println(listOf<Any>(positionedSprite.texture, positionedSprite.x, positionedSprite.y,
                    Config.SPRITE_SIZE,
                    "x:" + positionedSprite.sprite.x, "y:" + positionedSprite.sprite.y).joinToString(" - "))
            batch.drawSprite(positionedSprite.texture, positionedSprite.x, positionedSprite.y,
                    Config.SPRITE_SIZE,
                    positionedSprite.sprite.x, positionedSprite.sprite.y)
        }
    }

    fun drawEnemies() {
        for (enemy in enemies) {

            if (enemy.isAlive())
                enemy.drawCharacter(batch)

            if (enemy.isAlive())
                handleEnemyBehavior(enemy)

        }
    }

    class PositionedSprite(var sprite: Sprite, var x: Float, var y: Float, var texture: Texture)

    var spritesStack: MutableList<PositionedSprite> = mutableListOf()

    private fun handleEnemyBehavior(enemy: Character) {
        val distanceEnemy = distance(Pair(player.positionX, player.positionY), Pair(enemy.positionX, enemy.positionY))
        if (distanceEnemy.toSpriteUnits() < 3 && distanceEnemy.toSpriteUnits() >= 0.8) {
            val enemyMoveLength = enemy.computeMoveLength()
            if (player.positionX < enemy.positionX) {
                if (level.canMoveLeft(enemy.positionX, enemy.positionY, enemyMoveLength)) {
                    enemy.moveLeft(enemyMoveLength)
                }
            } else if (player.positionX > enemy.positionX) {
                if (level.canMoveRight(enemy.positionX, enemy.positionY, enemyMoveLength)) {
                    enemy.moveRight(enemyMoveLength)
                }
            }
            if (player.positionY > enemy.positionY) {
                if (level.canMoveUp(enemy.positionX, enemy.positionY, enemyMoveLength)) {
                    enemy.moveUp(enemyMoveLength)
                }
            } else if (player.positionY < enemy.positionY) {
                if (level.canMoveDown(enemy.positionX, enemy.positionY, enemyMoveLength)) {
                    enemy.moveDown(enemyMoveLength)
                }
            }
        } else {
            enemy.hold()
        }

        if (player.isAlive() && distanceEnemy.toSpriteUnits() < 1) {
            player.loseHealth()
            if (!gruntSound.isPlaying)
                gruntSound.play()
        }
    }

    private var lastAttackTime: Float = 0f

    private fun handleKeys() {
        val distance: Float = player.computePlayerMoveLength()
        if (Input.Keys.DOWN.isKeyPressed()) {
            if (level.canMoveDown(player.positionX, player.positionY, distance))
                player.moveDown(distance)
        }
        if (Input.Keys.UP.isKeyPressed()) {
            if (level.canMoveUp(player.positionX, player.positionY, distance))
                player.moveUp(distance)
        }
        if (Input.Keys.RIGHT.isKeyPressed()) {
            if (level.canMoveRight(player.positionX, player.positionY, distance))
                player.moveRight(distance)
        }
        if (Input.Keys.LEFT.isKeyPressed()) {
            if (level.canMoveLeft(player.positionX, player.positionY, distance))
                player.moveLeft(distance)
        }
        if (Input.Keys.SPACE.isKeyPressed()) {
            player.currentState = "JUMP"
        }
        if (Input.Keys.ALT_LEFT.isKeyPressed()) {
            lastAttackTime += Gdx.graphics.deltaTime
            if (lastAttackTime > 0.2) {
                lastAttackTime = 0f

                player.currentState = "FIGHT"
                for (enemy in enemies) {
                    if (enemy.isAlive()) {
                        val distanceEnemy = distance(Pair(player.positionX, player.positionY), Pair(enemy.positionX, enemy.positionY))
                        if (distanceEnemy.toSpriteUnits() < 2) {
                            enemy.loseHealth()

                            if (Math.random() > 0.5)
                                monsterGruntSound.play()
                            else if ((Math.random() > 0.7)) monsterGruntSound2.play()
                            else monsterGruntSound3.play()
                        }
                    }
                }

                // try sword animation
                var x = player.positionX + Config.SPRITE_SIZE_WORLD_UNIT * 2 / 3
                var y = player.positionY + Config.SPRITE_SIZE_WORLD_UNIT / 3
                spritesStack.add(PositionedSprite(Sprite(0, 2), x, y, spritesCubicMonster))
                spritesStack.add(PositionedSprite(Sprite(1, 2), x, y, spritesCubicMonster))
                spritesStack.add(PositionedSprite(Sprite(2, 2), x, y, spritesCubicMonster))
                spritesStack.add(PositionedSprite(Sprite(3, 2), x, y, spritesCubicMonster))
            }

        }

        if (player.isAlive() && player.currentState.startsWith("RUNNING") && !walkSound.isPlaying) {
            walkSound.play()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        when (keycode) {
            Input.Keys.P -> displayCoords = !displayCoords
            Input.Keys.ENTER -> displayGrid = !displayGrid
            Input.Keys.M -> {
                hasMusic = !hasMusic
                if (music.isPlaying) music.pause()
                else music.play()
            }
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        player.currentState = "IDLE"
        return false
    }

    override fun dispose() {
        batch.dispose()
        spritesCharacter.dispose()
        spritesEnv.dispose()
        renderer.dispose()
        music.dispose()
        walkSound.dispose()
        font.dispose()
    }

    private fun drawGrid() {
        renderer.projectionMatrix = camera.combined
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color.WHITE

        for (y in 0..Config.WORLD_HEIGHT.toInt()) {
            renderer.line(0f, y.toFloat() * Config.SPRITE_SIZE_WORLD_UNIT, Config.WORLD_WIDTH, y.toFloat() * Config.SPRITE_SIZE_WORLD_UNIT)
        }

        for (x in 0..Config.WORLD_WIDTH.toInt()) {
            renderer.line(x.toFloat() * Config.SPRITE_SIZE_WORLD_UNIT, 0f, x.toFloat() * Config.SPRITE_SIZE_WORLD_UNIT, Config.WORLD_HEIGHT)
        }

        renderer.end()
    }

    private fun drawCoords() {
        if (displayCoords) {
            font.draw(batch,
                    "${(player.positionX.toSpriteUnits().floor())},${(player.positionY.toSpriteUnits().floor())} (Sprites floor)",
                    Config.WORLD_HEIGHT - 20f, Config.WORLD_HEIGHT - 20f)
            font.draw(batch,
                    "${(player.positionX.toSpriteUnits())},${(player.positionY.toSpriteUnits())} (Sprites)",
                    Config.WORLD_HEIGHT - 20f, Config.WORLD_HEIGHT - 40f)
            font.draw(batch,
                    "${player.positionX},${player.positionY} (World Units)",
                    Config.WORLD_HEIGHT - 20f, Config.WORLD_HEIGHT - 60f)
        }
    }
}
