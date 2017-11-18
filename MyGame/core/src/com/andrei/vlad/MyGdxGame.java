package com.andrei.vlad;


import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.Animation.PlayMode;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;


public class MyGdxGame extends ApplicationAdapter {

    enum GameState {
        Init, Action, GameOver
    }

    SpriteBatch batch;

    //Logs fps. Helps debuggin
    FPSLogger fpsLogger;
    OrthographicCamera camera;


    //Init Textures
    TextureRegion bgRegion;
    TextureRegion terrainBelow;
    TextureRegion terrainAbove;
    TextureRegion pillarUp;
    TextureRegion pillarDown;
    Texture gameOver;


    float tapDrawTime;
    private static final float tapDrawTimeMax = 1.0f;
    float terrainOffset;
    Animation plane;
    float planeAnimTime;

    Vector2 planeVelocity = new Vector2();
    Vector2 scrollVelocity = new Vector2();
    Vector2 planePosition = new Vector2();
    Vector2 planeDefaultPosition = new Vector2();
    Vector2 gravity = new Vector2();
    Vector3 touchPosition = new Vector3();
    Vector2 tmpVector = new Vector2();


    private static final int touchImpulse = 200;

    TextureAtlas atlas;
    Viewport viewport;
    GameState gameState = GameState.Init;
    Array<Pillar> pillars = new Array<Pillar>();
    Vector2 lastPillarPosition = new Vector2();
    float deltaPosition;
    Rectangle planeRect = new Rectangle();
    Rectangle obstacleRect = new Rectangle();


    BitmapFont scoreFont;
    private int score = 0;


    //Audio Sounds
    Music music;
    Sound tapSound;
    Sound crashSound;


    @Override
    public void create() {
        fpsLogger = new FPSLogger();
        batch = new SpriteBatch();

        //Camera setup
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);
        camera.position.set(400, 240, 0);
        viewport = new FitViewport(800, 480, camera);

        //Textures Specifications setup
        atlas = new TextureAtlas(Gdx.files.internal("thrustcopterassets.txt"));

        //Textures Setup
        bgRegion = atlas.findRegion("background");
        terrainBelow = atlas.findRegion("groundGrass");
        pillarUp = atlas.findRegion("rockGrassUp");
        pillarDown = atlas.findRegion("rockGrassDown");
        gameOver = new Texture("gameover.png");
        terrainAbove = new TextureRegion(terrainBelow);
        terrainAbove.flip(true, true);

        scoreFont = new BitmapFont();


        //Plane Setup
        plane = new Animation(0.05f, atlas.findRegion("planeRed1"),
                atlas.findRegion("planeRed2"),
                atlas.findRegion("planeRed3"),
                atlas.findRegion("planeRed2"));
        plane.setPlayMode(PlayMode.LOOP);


        //Music setup
        music = Gdx.audio.newMusic(Gdx.files.internal("sounds/journey.mp3"));
        music.setLooping(true);
        music.play();


        //General sounds setup
        tapSound = Gdx.audio.newSound(Gdx.files.internal("sounds/pop.ogg"));
        crashSound = Gdx.audio.newSound(Gdx.files.internal("sounds/crash.ogg"));
        crashSound = Gdx.audio.newSound(Gdx.files.internal("sounds/crash.ogg"));

        resetScene();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        fpsLogger.log();


        updateScene();
        drawScene();
    }

    private void resetScene() {
        terrainOffset = 0;
        planeAnimTime = 0;
        tapDrawTime = 0;
        score = 0;
        planeVelocity.set(100, 0);
        scrollVelocity.set(5, 0);
        gravity.set(0, -3);
        planeDefaultPosition.set(250 - 88 / 2, 240 - 73 / 2);
        planePosition.set(planeDefaultPosition.x, planeDefaultPosition.y);
        pillars.clear();
        addPillar();
    }

    private void updateScene() {
        if (isJusTouched()) {
            tapSound.play();

            if (gameState == GameState.Init) {
                gameState = GameState.Action;
                return;
            }
            if (gameState == GameState.GameOver) {
                gameState = GameState.Init;
                resetScene();
                return;
            }


            tmpVector.set(planePosition.x, planePosition.y).nor();
            System.out.println(tmpVector + "");

            planeVelocity.set(100, 0);
            planeVelocity.mulAdd(tmpVector, touchImpulse);
            tapDrawTime = tapDrawTimeMax;
        }
        if (gameState == GameState.Init || gameState == GameState.GameOver) {
            return;
        }

        float deltaTime = Gdx.graphics.getDeltaTime();
        planeAnimTime += deltaTime;
        planeVelocity.add(gravity);
//		planeVelocity.add(scrollVelocity);
        System.out.println(planeVelocity);
        planePosition.mulAdd(planeVelocity, deltaTime);
        deltaPosition = planePosition.x - planeDefaultPosition.x;
        terrainOffset -= deltaPosition;
        planePosition.x = planeDefaultPosition.x;


        if (terrainOffset * -1 > terrainBelow.getRegionWidth()) {
            terrainOffset = 0;
        }
        if (terrainOffset > 0) {
            terrainOffset = -terrainBelow.getRegionWidth();
        }
        planeRect.set(planePosition.x + 16, planePosition.y, 50, 73);


        //Update pillars state and check if plane hit a pillar
        for (Pillar vec : pillars) {
            vec.getVector2().x -= deltaPosition;
            if (vec.getVector2().x + pillarUp.getRegionWidth() < -10) {
                pillars.removeValue(vec, false);
            }
            if (vec.getVector2().y == 1) {
                obstacleRect.set(vec.getVector2().x + 10, 0, pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight() - 10);
            } else {
                obstacleRect.set(vec.getVector2().x + 10, 480 - pillarDown.getRegionHeight() + 10, pillarUp.getRegionWidth() - 20, pillarUp.getRegionHeight());
            }
            if (planeRect.overlaps(obstacleRect)) {
                if (gameState != GameState.GameOver) {
                    crashSound.play();
                    gameState = GameState.GameOver;
                }
            } else if (planeRect.getX() > obstacleRect.getX() + obstacleRect.getWidth()) {
                if (!vec.isPassed()) {
                    score++;
                    vec.setPassed(true);
                }
            }
        }

        //Add new Pillar if neccessary
        if (lastPillarPosition.x < 400) {
            addPillar();
        }


        //Check if plane crashed
        if (planePosition.y < terrainBelow.getRegionHeight() - 35 ||
                planePosition.y + 73 > 480 - terrainBelow.getRegionHeight() + 35) {
            if (gameState != GameState.GameOver) {
                crashSound.play();
                gameState = GameState.GameOver;
            }
        }
        tapDrawTime -= deltaTime;
    }

    //Render everything
    private void drawScene() {

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.disableBlending();
        batch.draw(bgRegion, 0, 0);
        batch.enableBlending();
        for (Pillar vec : pillars) {
            if (vec.getVector2().y == 1) {
                batch.draw(pillarUp, vec.getVector2().x, 0);
            } else {
                batch.draw(pillarDown, vec.getVector2().x, 480 - pillarDown.getRegionHeight());
            }
        }


        batch.draw(terrainBelow, terrainOffset, 0);
        batch.draw(terrainBelow, terrainOffset + terrainBelow.getRegionWidth(), 0);
        batch.draw(terrainAbove, terrainOffset, 480 - terrainAbove.getRegionHeight());
        batch.draw(terrainAbove, terrainOffset + terrainAbove.getRegionWidth(), 480 - terrainAbove.getRegionHeight());


        if (gameState == GameState.GameOver) {
            batch.draw(gameOver, 400 - 206, 240 - 80);
        }


        batch.draw((TextureRegion) plane.getKeyFrame(planeAnimTime), planePosition.x, planePosition.y);
        scoreFont.setColor(Color.WHITE);
        scoreFont.getData().setScale(2);
        scoreFont.draw(batch, "Score: " + score, 50, 450);
        batch.end();
    }


    private void addPillar() {
        Vector2 pillarPosition = new Vector2();
        if (pillars.size == 0) {
            pillarPosition.x = (float) (800 + Math.random() * 600);
        } else {
            pillarPosition.x = lastPillarPosition.x + (float) (600 + Math.random() * 600);
        }
        if (MathUtils.randomBoolean()) {
            pillarPosition.y = 1;
        } else {
            pillarPosition.y = -1;//upside down
        }
        lastPillarPosition = pillarPosition;
        pillars.add(new Pillar(pillarPosition));
    }


    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }


    //Release resources
    @Override
    public void dispose() {
        tapSound.dispose();
        crashSound.dispose();
        music.dispose();
        batch.dispose();
        pillars.clear();
        atlas.dispose();
        scoreFont.dispose();
    }


    private int getInputX() {
        return Gdx.input.getX();
    }

    private int getInputY() {
        return Gdx.input.getY();
    }

    private boolean isJusTouched() {
        return Gdx.input.justTouched();
    }

}
