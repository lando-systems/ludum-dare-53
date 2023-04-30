package lando.systems.ld53.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import lando.systems.ld53.Config;
import lando.systems.ld53.audio.AudioManager;
import lando.systems.ld53.entities.*;
import lando.systems.ld53.physics.Collidable;
import lando.systems.ld53.physics.Influencer;
import lando.systems.ld53.physics.PhysicsSystem;
import lando.systems.ld53.physics.test.TestAttractor;
import lando.systems.ld53.physics.test.TestBall;
import lando.systems.ld53.physics.test.TestRepulser;
import lando.systems.ld53.ui.IndividualSkillUI;
import lando.systems.ld53.ui.TopGameUI;
import lando.systems.ld53.ui.TopTrapezoid;
import lando.systems.ld53.world.Map;

public class GameScreen extends BaseScreen {

    private Map map;
    private Ball ball;
    private Player player;
    private Enemy enemy;
    private BulletEnemy bulletEnemy;

    public final Array<Bullet> bullets;

    private final PhysicsSystem physicsSystem;
    private final Array<Collidable> physicsObjects;
    private final Array<Influencer> influencers;
    private final Array<TestBall> testBalls;

    private TopTrapezoid trapezoid;
    private IndividualSkillUI testSkillUI;

    public GameScreen() {
        super();

        worldCamera.setToOrtho(false, Config.Screen.framebuffer_width, Config.Screen.framebuffer_height);
        worldCamera.update();

        bullets = new Array<>();

        map = new Map("maps/test-80x80.tmx");
        ball = new Ball(assets, worldCamera.viewportWidth / 2f, worldCamera.viewportHeight * (2f / 3f));
        player = new Player(assets);
        enemy = new Enemy(assets, player.position.x - 200f, player.position.y + 80f);
        bulletEnemy = new BulletEnemy(assets, this, 5, -100f);

        influencers = new Array<>();
        physicsObjects = new Array<>();
        physicsSystem = new PhysicsSystem(new Rectangle(0,0, Config.Screen.window_width, Config.Screen.window_height));

        testBalls = new Array<>();
        for (int i = 0; i < 100; i++){
            Vector2 pos = new Vector2(Gdx.graphics.getWidth() * MathUtils.random(.2f, .8f), Gdx.graphics.getHeight() * MathUtils.random(.2f, .5f));
            Vector2 vel = new Vector2(MathUtils.random(-60f, 60f), MathUtils.random(-60f, 60f));
            testBalls.add(new TestBall(pos, vel));
        }
        influencers.add(new TestAttractor(new Vector2(400, 500)));
        influencers.add(new TestRepulser(new Vector2(700, 450)));

        Gdx.input.setInputProcessor(uiStage);

        audioManager.playMusic(AudioManager.Musics.level1Thin);
//        audioManager.playMusic(AudioManager.Musics.level1Full);
//        audioManager.playSound(AudioManager.Sounds.coin);
        trapezoid = new TopTrapezoid(player, assets);
        testSkillUI = new IndividualSkillUI(this, player.currentAbility);
        //uiStage.addActor(testSkillUI);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            audioManager.stopMusic();
            game.setScreen(new TitleScreen());
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            if(assets.level1Full.isPlaying()) {
                assets.level1Thin.play();
                assets.level1Thin.setVolume(audioManager.musicVolume.floatValue());
                assets.level1Thin.setPosition(assets.level1Full.getPosition());
                assets.level1Full.stop();
            }
            else if(assets.level1Thin.isPlaying()) {

                assets.level1Full.setVolume(audioManager.musicVolume.floatValue());
                assets.level1Full.play();
//                audioManager.playMusic(AudioManager.Musics.level1Full);
                assets.level1Full.setPosition(assets.level1Thin.getPosition());
                assets.level1Thin.stop();
            }
        }

        physicsObjects.clear();

        physicsObjects.addAll(map.wallSegments);
        physicsObjects.addAll(map.pegs);
        physicsObjects.addAll(testBalls);
        physicsObjects.addAll(bullets);
        physicsObjects.add(ball);
        physicsObjects.add(player);
        physicsObjects.add(enemy);

        physicsSystem.update(delta, physicsObjects, influencers);

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet bullet = bullets.get(i);
            bullet.update(delta);

            boolean isDead = !bullet.alive;
            boolean isOffscreen = !bullet.isInside(0, 0, worldCamera.viewportWidth, worldCamera.viewportHeight);
            if (isDead || isOffscreen) {
                Bullet.pool.free(bullet);
                bullets.removeIndex(i);
            }
        }

        ball.update(delta);
        bulletEnemy.update(delta);
        enemy.update(delta);
        player.update(delta);
        map.update(delta);

        trapezoid.update();
//        topGameUI.update(player.getStaminaPercentage());
        uiStage.setDebugAll(Config.Debug.ui);
    }

    @Override
    public void render(SpriteBatch batch) {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        map.render(batch, worldCamera);

        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        {
            for (Peg peg : map.pegs) {
                peg.render(batch);
            }
            for (Goal goal : map.goals) {
                goal.render(batch);
            }
            bulletEnemy.render(batch);
            enemy.render(batch);
            player.render(batch);
            ball.render(batch);
            for (Bullet bullet : bullets) {
                bullet.render(batch);
            }

            if (Config.Debug.general){
                for (Collidable collidable : physicsObjects) {
                    collidable.renderDebug(assets.shapes);
                }
                for (Influencer i : influencers) {
                    i.debugRender(batch);
                }
            }
        }
        batch.end();
        trapezoid.render(batch);
        uiStage.draw();
    }

    @Override
    public void initializeUI() {
        super.initializeUI();
        //topGameUI = new TopGameUI(this);
        //uiStage.addActor(topGameUI);
    }

}
