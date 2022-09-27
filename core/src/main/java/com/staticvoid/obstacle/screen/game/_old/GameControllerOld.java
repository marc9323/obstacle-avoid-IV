package com.staticvoid.obstacle.screen.game._old;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.staticvoid.obstacle.ObstacleAvoidGame;
import com.staticvoid.obstacle.assets.AssetDescriptors;
import com.staticvoid.obstacle.common.GameManager;
import com.staticvoid.obstacle.config.DifficultyLevel;
import com.staticvoid.obstacle.config.GameConfig;
import com.staticvoid.obstacle.entity.ObstacleActor;
import com.staticvoid.obstacle.entity._old.Background;
import com.staticvoid.obstacle.entity._old.Obstacle;
import com.staticvoid.obstacle.entity._old.Player;

@Deprecated
public class GameControllerOld {

    private static final Logger log = new Logger(GameControllerOld.class.getName(), Application.LOG_DEBUG);

    private Player player;
    private Array<Obstacle> obstacles = new Array<Obstacle>();
    private Background background;
    private float obstacleTimer;
    private float scoreTimer;

    private int lives = GameConfig.LIVES_START;
    private int score;
    private int displayScore;

    // private DifficultyLevel difficultyLevel = DifficultyLevel.EASY;

    // returns a ReflectionPool, requires class with no args constructor
    private Pool<Obstacle> obstaclePool;
    private Sound hit;

    private final ObstacleAvoidGame game;
    private final AssetManager assetManager;
    // TODO: Music class
    // see videos 85-95 for positioning direction
    private final float startPlayerX = (GameConfig.WORLD_WIDTH - GameConfig.PLAYER_SIZE) / 2f; // 2.6
    private final float startPlayerY = 1 - GameConfig.PLAYER_SIZE / 2f; // .6

    public GameControllerOld(ObstacleAvoidGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
        init();
    }

    private void init() {
        // create player and FitViewport
        player = new Player();

        // position player
        player.setPosition(startPlayerX, startPlayerY);

        obstaclePool = Pools.get(Obstacle.class, 40);

        background = new Background();
        background.setPosition(0, 0);
        background.setSize(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);

        hit = assetManager.get(AssetDescriptors.HIT_SOUND);
    }

    public void update(float deltaTime) {
        if (isGameOver()) {
            return;
        }

        updatePlayer();
        updateObstacles(deltaTime);
        updateScore(deltaTime);
        updateDisplayScore(deltaTime);

        if (isPlayerCollidingWithObstacle()) {
            log.debug("Collision Detected!  BAM!");
            lives--;

            if (isGameOver()) {
                log.debug("Game Over");
                // update high score
                GameManager.INSTANCE.updateHighScore(score);
                //  game.setScreen(new MenuScreen(game));
            } else {
                restart();
            }
        }
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    private void restart() {
        obstaclePool.freeAll(obstacles);
        obstacles.clear();
        player.setPosition(startPlayerX, startPlayerY);
    }

    private boolean isPlayerCollidingWithObstacle() {
        for (Obstacle obstacle : obstacles) {
            if (!obstacle.isHit() && obstacle.isPlayerColliding(player)) {
                hit.play();
                return true;
            }
        }
        // NOTE:  video 79 -- I like above better, interchangeable
//        if (obstacle.isNotHit() && obstacle.isPlayerColliding(player)) {
//            return true;
//        }
        return false;
    }

    // private methods
    private void updatePlayer() {
        // discrete input handling would be optimal
        float xSpeed = 0;

        // polling, recall this is happening inside Game Screen render method
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            xSpeed = GameConfig.MAX_PLAYER_X_SPEED;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            xSpeed -= GameConfig.MAX_PLAYER_X_SPEED;
        }

        player.setX(player.getX() + xSpeed);

        blockPlayerFromLeavingTheWorld();
    }

    private void blockPlayerFromLeavingTheWorld() {
        // this line is equivalent to the following commented out lines
        // feed clamp:  value to clamp, minimum, maximum.
        float playerX = MathUtils.clamp(player.getX(),
                0,
                GameConfig.WORLD_WIDTH - player.getWidth());

        player.setPosition(playerX, player.getY());
    }

    private void updateScore(float deltaTime) {
        // score is added to a random intervals
        // the longer player lives, the more points racked up
        scoreTimer += deltaTime;

        if (scoreTimer >= GameConfig.SCORE_MAX_TIME) {
            score += MathUtils.random(1, 5); // min 1, max 4, inclusive/exclusive
            scoreTimer = 0.0f;
        }
    }

    private void updateDisplayScore(float deltaTime) {
        // 1/60 * 60 --> score increments by one each frame
        if (displayScore < score) {
            displayScore = Math.min(
                    score,
                    displayScore + (int) (60 * deltaTime)
            );
        }
    }

    // once marked as hit it no longer will count as collision
    // for overlap on next few frames
    // result:  player loses just a single life
//    private boolean isPlayerCollidingWithObstacle() {
//        for (Obstacle obstacle : obstacles) {
//            if (obstacle.isNotHit() && obstacle.isPlayerColliding(player)) {
//                hit.play();
//                return true;
//            }
//        }
//
//        return false;
//    }

    private void updateObstacles(float deltaTime) {
        for (Obstacle obstacle : obstacles) {
            obstacle.update();
        }

        createNewObstacle(deltaTime);

        removePassedObstacles();
    }

    private void createNewObstacle(float deltaTime) {
        obstacleTimer += deltaTime;

        if (obstacleTimer > GameConfig.OBSTACLE_SPAWN_TIME) {
            float min = 0;
            float max = GameConfig.WORLD_WIDTH - GameConfig.OBSTACLE_SIZE;

            float obstacleX = MathUtils.random(min, max);
            float obstacleY = GameConfig.WORLD_HEIGHT;

            Obstacle obstacle = obstaclePool.obtain();
            DifficultyLevel difficultyLevel =
                    GameManager.INSTANCE.getDifficultyLevel();
            obstacle.setYSpeed(difficultyLevel.getObstacleSpeed());
            obstacle.setPosition(obstacleX, obstacleY);

            obstacles.add(obstacle);

            obstacleTimer = 0f;
        }
    }

    private void removePassedObstacles() {
        if (obstacles.size > 0) {
            Obstacle first = obstacles.first();

            float minObstacleY = -GameConfig.OBSTACLE_SIZE;  // 0 end of bottom world bounds

            if (first.getY() < minObstacleY) {
                // remove from array
                obstacles.removeValue(first, true);
                obstaclePool.free(first); // put back in pool, pool resets
            }
        }
    }



    public Player getPlayer() {
        return player;
    }

    public Array<Obstacle> getObstacles() {
        return obstacles;
    }

    public Background getBackground() {
        return background;
    }

    public int getLives() {
        return lives;
    }

    public int getDisplayScore() {
        return displayScore;
    }
}

/*
NOTE:  textures render from bottom left hand corner
shaperenderer renders circles from center

our solution will move the circle half width to right, half width upwards
 */
