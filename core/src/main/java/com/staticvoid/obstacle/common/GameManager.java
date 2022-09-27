package com.staticvoid.obstacle.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.staticvoid.obstacle.ObstacleAvoidGame;
import com.staticvoid.obstacle.config.DifficultyLevel;

public class GameManager {

    public static final GameManager INSTANCE = new GameManager();

    private static final String HIGH_SCORE_KEY = "highscore";
    private static final String DIFFICULTY_KEY = "difficulty";

    private Preferences PREFS;
    private int highscore;

    private DifficultyLevel difficultyLevel = DifficultyLevel.MEDIUM; // default


    private GameManager() {
        // SINGLETON
        PREFS = Gdx.app.getPreferences(ObstacleAvoidGame.class.getSimpleName());
        highscore = PREFS.getInteger(HIGH_SCORE_KEY, 0);
        String difficultyName = PREFS.getString(DIFFICULTY_KEY,
                DifficultyLevel.MEDIUM.name());
        difficultyLevel = DifficultyLevel.valueOf(difficultyName);
    }

    public void updateHighScore(int score) {
        if (score < highscore) {
            return;
        }

        highscore = score;

        PREFS.putInteger(HIGH_SCORE_KEY, highscore);
        // must ALWAYS flush prior to save or it will fail
        PREFS.flush();
    }

    public String getHighScoreString() {
        return String.valueOf(highscore);
    }

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void updateDifficulty(DifficultyLevel newDifficultyLevel) {
        if (difficultyLevel == newDifficultyLevel) {
            return;
        } // otherwise,
        difficultyLevel = newDifficultyLevel;
        PREFS.putString(DIFFICULTY_KEY, difficultyLevel.name());
        PREFS.flush();
    }
}
