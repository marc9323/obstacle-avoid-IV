package com.staticvoid.obstacle.screen.game._old;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Logger;
import com.staticvoid.obstacle.ObstacleAvoidGame;
import com.staticvoid.obstacle.screen.menu.MenuScreen;

// Rendering and game logic are neatly separated
@Deprecated
public class GameScreenOld implements Screen {

    private static final Logger log = new Logger(GameScreenOld.class.getName(), Logger.DEBUG);

    private final ObstacleAvoidGame game;
    private final AssetManager assetManager;

    private GameControllerOld controller;
    private GameRendererOld renderer;

    public GameScreenOld(ObstacleAvoidGame game) {
        this.game = game;
        assetManager = game.getAssetManager();
    }

    @Override
    public void show() {
        log.debug("show()");
        controller = new GameControllerOld(game);
        renderer = new GameRendererOld(game.getBatch(), assetManager, controller);
    }

    @Override
    public void render(float delta) {
        controller.update(delta);
        renderer.render(delta);

        if(controller.isGameOver()) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {
        log.debug("hide");
        dispose();
    }

    @Override
    public void dispose() {
        renderer.dispose();
    }
}

