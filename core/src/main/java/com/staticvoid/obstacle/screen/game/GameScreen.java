package com.staticvoid.obstacle.screen.game;

import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.staticvoid.obstacle.ObstacleAvoidGame;
import com.staticvoid.obstacle.assets.AssetDescriptors;
import com.staticvoid.obstacle.assets.RegionNames;
import com.staticvoid.obstacle.common.GameManager;
import com.staticvoid.obstacle.config.DifficultyLevel;
import com.staticvoid.obstacle.config.GameConfig;
import com.staticvoid.obstacle.entity.ObstacleActor;
import com.staticvoid.obstacle.entity.PlayerActor;
import com.staticvoid.obstacle.screen.menu.MenuScreen;
import com.staticvoid.obstacle.util.GdxUtils;
import com.staticvoid.obstacle.util.ViewportUtils;
import com.staticvoid.obstacle.util.debug.DebugCameraController;

import java.util.Iterator;

// Since with Stage we can't separate rendering logic from  UI rendering
// Screen will be like mix between controller/renderer
public class GameScreen extends ScreenAdapter {

    private static final Logger log =
            new Logger(GameScreen.class.getName(),
                    Logger.DEBUG);
    // == constants
    private static final float PADDING = 20.0f;

    // == attributes
    private final ObstacleAvoidGame game;
    private final AssetManager assetManager;
    private final SpriteBatch batch;
    private final GlyphLayout layout = new GlyphLayout();

    private OrthographicCamera camera;
    private Viewport viewport;
    private Stage stage;
    private ShapeRenderer renderer;

    private OrthographicCamera uiCamera;
    private Viewport uiViewport;
    private BitmapFont font;

    private float obstacleTimer;
    private float scoreTimer;
    private int lives = GameConfig.LIVES_START;
    private int score;
    private int displayScore;
    private Sound hitSound;

    private float startPlayerX = (GameConfig.WORLD_WIDTH - GameConfig.PLAYER_SIZE) / 2f;
    private float startPlayerY = GameConfig.PLAYER_SIZE / 2f;

    private DebugCameraController debugCameraController;
    private TextureRegion obstacleRegion;
    private TextureRegion backgroundRegion;

    private PlayerActor player;
    private Image background;
    private final Array<ObstacleActor> obstacles = new Array<ObstacleActor>();
    private final Pool<ObstacleActor> obstaclePool = Pools.get(ObstacleActor.class);


    // == constructor
    public GameScreen(ObstacleAvoidGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
        batch = game.getBatch();
    }

    // == public methods -->

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT,
                camera);
        stage = new Stage(viewport, batch);
        stage.setDebugAll(true); // show debug lines for all Actors on this Stage instance

        renderer = new ShapeRenderer();

        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(GameConfig.HUD_WIDTH,
                GameConfig.HUD_HEIGHT, uiCamera);
        font = assetManager.get(AssetDescriptors.FONT);

        debugCameraController = new DebugCameraController();
        debugCameraController.setStartPosition(GameConfig.WORLD_CENTER_X,
                GameConfig.WORLD_CENTER_Y);

        hitSound = assetManager.get(AssetDescriptors.HIT_SOUND);

        TextureAtlas gamePlayAtlas =
                assetManager.get(AssetDescriptors.GAME_PLAY);
        TextureRegion playerRegion = gamePlayAtlas.findRegion(RegionNames.PLAYER);
        obstacleRegion = gamePlayAtlas.findRegion(RegionNames.OBSTACLE);
        backgroundRegion = gamePlayAtlas.findRegion(RegionNames.BACKGROUND);

        // Image extends Widget which extends Actor, subclasses Actor
        background = new Image(backgroundRegion);
        background.setSize(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
        // background.setZIndex();

        player = new PlayerActor();
        player.setPosition(startPlayerX, startPlayerY);
        player.setRegion(playerRegion);

        stage.addActor(background);
        stage.addActor(player);

    }

    @Override
    public void render(float deltaTime) {
        // handle debug input and apply configuration to camera
        debugCameraController.handleDebugInput(deltaTime);
        debugCameraController.applyTo(camera);

        update(deltaTime);

        // clear screen
        GdxUtils.clearScreen();

        viewport.apply(); // apply our game viewport
        renderGamePlay();

        uiViewport.apply(); // apply user interface viewport
        renderUi();

        viewport.apply(); // apply our game viewport
        renderDebug();

        if (isGameOver()) {
            game.setScreen(new MenuScreen(game));
        }
    }

    private void update(float deltaTime) {
        if (isGameOver()) {
            return;
        }
        // create new obstacle every interval and remove passed obstacles
        createNewObstacle(deltaTime);
        removePassedObstacles();

        updateScore(deltaTime);
        updateDisplayScore(deltaTime);

        if (!isGameOver() && isPlayerCollidingWithObstacle()) {
            log.debug("Lives: " + lives);
            lives--;

            if (isGameOver()) {
                GameManager.INSTANCE.updateHighScore(score);
            } else {
                restartII();
            }
        }

    }


    private void removePassedObstacles() {
        if (obstacles.size > 0) {
            ObstacleActor first = obstacles.first();

            float minObstacleY = -GameConfig.OBSTACLE_SIZE;  // 0 end of bottom world bounds

            if (first.getY() < minObstacleY) {
                // remove from array
                obstacles.removeValue(first, true);

                // removes Actor from Parent, which in this case is Stage
                first.remove();

                // return to pool
                obstaclePool.free(first); // put back in pool, pool resets
            }
        }
    }


    private void createNewObstacle(float deltaTime) {
        obstacleTimer += deltaTime;

        if (obstacleTimer >= GameConfig.OBSTACLE_SPAWN_TIME) {
            float min = 0;
            float max = GameConfig.WORLD_WIDTH - GameConfig.OBSTACLE_SIZE;

            float obstacleX = MathUtils.random(min, max);
            float obstacleY = GameConfig.WORLD_HEIGHT;

            ObstacleActor obstacle = obstaclePool.obtain();
            DifficultyLevel difficultyLevel =
                    GameManager.INSTANCE.getDifficultyLevel();
            obstacle.setYSpeed(difficultyLevel.getObstacleSpeed());
            obstacle.setPosition(obstacleX, obstacleY);
            obstacle.setRegion(obstacleRegion);

            obstacles.add(obstacle);
            stage.addActor(obstacle);

            obstacleTimer = 0f;
        }
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true); // true to center camera
        uiViewport.update(width, height, true);

        ViewportUtils.debugPixelPerUnit(viewport);
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }

    // == private methods
    private void renderGamePlay() {
        batch.setProjectionMatrix(camera.combined);
//        batch.begin();
//
//        batch.draw(backgroundRegion,
//                0, 0,
//                GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
//
//        batch.end();

        stage.act(); // calls act on all actors same for draw
        stage.draw(); // encapsulates begin/end, no need to explicitly put inside begin/end
    }

//    private void drawGamePlay() {
//
//    }

    private void renderUi() {
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        // draw lives
        String livesText = "LIVES: " + lives;
        layout.setText(font, livesText);
        font.draw(batch, layout, PADDING, GameConfig.HUD_HEIGHT - layout.height);

        // draw score
        String scoreText = "SCORE: " + displayScore;
        layout.setText(font, scoreText);
        font.draw(batch, layout,
                GameConfig.HUD_WIDTH - layout.width - PADDING,
                GameConfig.HUD_HEIGHT - layout.height);

        batch.end();
    }

    private void renderDebug() {
//        renderer.setProjectionMatrix(camera.combined);
//        renderer.begin(ShapeRenderer.ShapeType.Line);
//
//       // drawDebug(); we are4 drawing all debug lines using Stage
//        // drawDebugBounds overriden in ActorBase
//
//        renderer.end();

        // draw grid
        ViewportUtils.drawGrid(viewport, renderer);
    }

    public boolean isGameOver() {
        return lives <= 0;
    }

    // Superior Solution - use Iterator!
    private void restartII() { // Utilize Iterator
        Iterator<ObstacleActor> iterator = obstacles.iterator();

        while(iterator.hasNext()) {
            ObstacleActor obstacle = iterator.next();
            obstacle.remove(); // remove from Parent, Stage
            obstaclePool.free(obstacle); // return to pool
            iterator.remove(); // remove from array
        }

        player.setPosition(startPlayerX, startPlayerY);
    }

    private void restart() {
        log.debug("obstacles-size: " + obstacles.size);
        // why do we need Array in addition to Pool?
        // calling stage.clear() will remove all - even Background
        for (int i = 0; i < obstacles.size; i++) {
            ObstacleActor obstacle = obstacles.get(i);

            // remove obstacle from stage (parent)
            //  obstacle.remove();
            // solves issue of obstacles continuing to fall
            // despite collision
            //  stage.getActors().removeValue(obstacle, true);
            //     stage.getActors().removeAll(obstacles, true);

            // stage.clear();
            boolean removed = obstacle.remove(); // returns boolean true on success
            log.debug("removed is: " + removed);
            // return to Pool
            obstaclePool.free(obstacle);

            // remove obstacle at index from Array
            // obstacles.removeIndex(i);
        }

        obstacles.clear();
        // remove all actors
        //stage.clear(); // todo: delete line

        // add them back from background, plauyer, to obstacle order - reset
//        stage.addActor(background);
//        stage.addActor(player);
        player.setPosition(startPlayerX, startPlayerY);
    }

    private boolean isPlayerCollidingWithObstacle() {
        for (ObstacleActor obstacle : obstacles) {
            if (obstacle.isNotHit() && obstacle.isPlayerColliding(player)) {
                hitSound.play();
                return true;
            }
        }
        // NOTE:  video 79 -- I like above better, interchangeable
//        if (obstacle.isNotHit() && obstacle.isPlayerColliding(player)) {
//            return true;
//        }
        return false;
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
}
