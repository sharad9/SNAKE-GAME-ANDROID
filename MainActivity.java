package com.mycompany.myapp2;


import android.app.Activity;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.LinearLayout;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.media.MediaPlayer;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.Window;
public class MainActivity extends Activity {



    // Declare an instance of SnakeEngine
    SnakeEngine snakeEngine;
    Boolean Pause=false;
    Button endGameButton;
    static MediaPlayer mp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the pixel dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();

        // Initialize the result into a Point object
        Point size = new Point();
        display.getSize(size);

        // Create a new instance of the SnakeEngine class

        // Make snakeEngine the view of the Activity

		mp=MediaPlayer.create(this,R.raw.full);
		mp.start();
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        FrameLayout game = new FrameLayout(this);
        snakeEngine = new SnakeEngine(this, size);

        LinearLayout gameWidgets = new LinearLayout (this);

        endGameButton = new Button(this);


        endGameButton.setWidth(120);
        endGameButton.setText("Pause");
		

        endGameButton.setX((size.x/2)+180);
      endGameButton.setY(0);
		
        endGameButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view)
				{
					if(Pause==false){
						endGameButton.setText("Play");
						snakeEngine.pause();
						
						Pause=true;

					}else{
						endGameButton.setText("Pause");
						
						snakeEngine.resume();
						
						Pause=false;
					}

				}
			});
        gameWidgets.addView(endGameButton);

        game.addView(snakeEngine);
        game.addView(gameWidgets);

        setContentView(game);
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onResume(){
        super.onResume();
        snakeEngine.resume();
		
		

    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void onPause(){
        super.onPause();
		
		
        snakeEngine.pause();

    }

}
class SnakeEngine extends SurfaceView implements Runnable {

    // Our game thread for the main game loop
    private Thread thread = null;

    // To hold a reference to the Activity
    private Context context;

    // for plaing sound effects
    private SoundPool soundPool;
    private int eat_bob = -1;
    private int snake_crash = -1;


    // For tracking movement Heading
    public enum Heading {UP, RIGHT, DOWN, LEFT}
    // Start by heading to the right
    private Heading heading = Heading.RIGHT;

    // To hold the screen size in pixels
    private int screenX;
    private int screenY;

    // How long is the snake
    private int snakeLength;

    // Where is Bob hiding?
    private int bobX;
    private int bobY;

    private int highest;
    // The size in pixels of a snake segment
    private int blockSize;

    // The size in segments of the playable area
    private final int NUM_BLOCKS_WIDE = 40;
    private int numBlocksHigh;

    // Control pausing between updates
    private long nextFrameTime;
    // Update the game 10 times per second
    private final long FPS = 10;
    // There are 1000 milliseconds in a second
    private final long MILLIS_PER_SECOND = 1000;
// We will draw the frame much more often

    // How many points does the player have
    private int score;

    // The location in the grid of all the segments
    private int[] snakeXs;
    private int[] snakeYs;

    // Everything we need for drawing
// Is the game currently playing?
    private volatile boolean isPlaying;

    // A canvas for our paint
    private Canvas canvas;

    // Required to use canvas
    private SurfaceHolder surfaceHolder;

    // Some paint for our canvas
    private Paint paint;
    public SnakeEngine(Context context, Point size) {
        super(context);

        context = context;

        screenX = size.x;
        screenY = size.y;

        // Work out how many pixels each block is
        blockSize = screenX / NUM_BLOCKS_WIDE;
        // How many blocks of the same size will fit into the height
        numBlocksHigh = screenY / blockSize;

        // Set the sound up
		soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

		/*
		 try {
		 // Create objects of the 2 required classes
		 // Use m_Context because this is a reference to the Activity
		 AssetManager assetManager = context.getAssets();
		 AssetFileDescriptor descriptor;
		 // Prepare the two sounds in memory
		 descriptor = assetManager.openFd("get_mouse_sound.ogg");
		 eat_bob = soundPool.load(descriptor, 0);
		 descriptor = assetManager.openFd("death_sound.ogg");
		 snake_crash = soundPool.load(descriptor, 0);
		 } catch (IOException e) {
		 // Error
		 }
		 */
		 eat_bob=soundPool.load(context,R.raw.eat,1);
		snake_crash=soundPool.load(context,R.raw.dead,1);



        // Initialize the drawing objects
        surfaceHolder = getHolder();
        paint = new Paint();

        // If you score 200 you are rewarded with a crash achievement!
        snakeXs = new int[2000];
        snakeYs = new int[2000];

        // Start the game
        newGame();
    }
    @Override
    public void run() {

        while (isPlaying) {

            // Update 10 times a second
            if(updateRequired()) {
                update();
                if(score>highest){highest=score;}
                draw();
            }

        }
    }

    public void pause() {
        isPlaying = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Error
        }
		MainActivity.mp.pause();
    }

    public void resume() {
        isPlaying = true;
        thread = new Thread(this);
        thread.start();
		
		MainActivity.mp.start();
    }
    public void newGame() {
        // Start with a single snake segment


        snakeLength = 1;
        snakeXs[0] = NUM_BLOCKS_WIDE / 2;
        snakeYs[0] = numBlocksHigh / 2;

        // Get Bob ready for dinner
        spawnBob();

        // Reset the score
        score = 0;

        // Setup nextFrameTime so an update is triggered
        nextFrameTime = System.currentTimeMillis();
		MainActivity.mp.start();

		// 
		MainActivity.mp.setLooping(true);
		
		
    }
    public void spawnBob() {
        Random random = new Random();
        bobX = random.nextInt(NUM_BLOCKS_WIDE - 10) + 10;
        bobY = random.nextInt(numBlocksHigh - 10) + 10;
        if(bobY>=numBlocksHigh-10){
            bobY=numBlocksHigh/2;
        }
    }
    private void eatBob(){
        //  Got him!
        // Increase the size of the snake
        snakeLength++;
        //replace Bob
        // This reminds me of Edge of Tomorrow. Oneday Bob will be ready!
        spawnBob();
        //add to the score
        score = score + 1;
		 soundPool.play(eat_bob, 1, 1, 1, 0, 1);
    }
    private void moveSnake(){
        // Move the body
        for (int i = snakeLength; i > 0; i--) {
            // Start at the back and move it
            // to the position of the segment in front of it
            snakeXs[i] = snakeXs[i - 1];
            snakeYs[i] = snakeYs[i - 1];

            // Exclude the head because
            // the head has nothing in front of it
        }

        // Move the head in the appropriate heading
        switch (heading) {
            case UP:
                snakeYs[0]--;
                break;

            case RIGHT:
                snakeXs[0]++;
                break;

            case DOWN:
                snakeYs[0]++;
                break;

            case LEFT:
                snakeXs[0]--;
                break;
        }
    }
    private boolean detectDeath(){
        // Has the snake died?
        boolean dead = false;

        // Hit the screen edge
        if (snakeXs[0] == -1) dead = true;
        if (snakeXs[0] >= NUM_BLOCKS_WIDE) dead = true;
        if (snakeYs[0] == -1) dead = true;
        if (snakeYs[0] == numBlocksHigh) dead = true;

        // Eaten itself?
        for (int i = snakeLength - 1; i > 0; i--) {
            if ((snakeXs[0] == snakeXs[i]) && (snakeYs[0] == snakeYs[i])) {
                dead = true;
            }
        }

        return dead;
    }
    public void update() {
        // Did the head of the snake eat Bob?
        if (snakeXs[0] == bobX && snakeYs[0] == bobY) {
            eatBob();
        }

        moveSnake();

        if (detectDeath()) {
            //start again
            soundPool.play(snake_crash, 1, 1, 0, 0, 1);

            MainActivity.mp.pause();
			MainActivity.mp.seekTo(0);
		
            try{thread.sleep(500);}catch(Exception e){}
			
            newGame();
        }
    }
    public void draw() {
        // Get a lock on the canvas
        if (surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            // Fill the screen with Game Code School blue
           // canvas.drawColor(Color.argb(255, 220, 201, 80));
			canvas.drawColor(Color.argb(255, 200, 150, 40));
			
            // Set the color of the paint to draw the snake white

            paint.setColor(Color.argb(255, 0, 0, 0));
            // Scale the HUD text
            paint.setTextSize(50);
            canvas.drawText("🅂🄲🄾🅁🄴 :" + score + " Highest:"+highest, 0, 120, paint);
            paint.setTextSize(35);
            canvas.drawText("©Sharad Maddheshiya", 0, 25, paint);
            
            paint.setColor(Color.argb(255, 255, 255, 255));
            // Draw the snake one block at a time
			paint.setStrokeCap(Paint.Cap.ROUND);
			paint.setStrokeWidth(28f);
            for (int i = 1; i < snakeLength; i++) {
                canvas.drawRect(snakeXs[i] * blockSize,
								(snakeYs[i] * blockSize),
								(snakeXs[i] * blockSize) + blockSize,
								(snakeYs[i] * blockSize) + blockSize,
								paint);
	          
		      //  canvas.drawPoint((snakeXs[i] * blockSize)+(blockSize/2),
					//			 (snakeYs[i] * blockSize)+(blockSize/2),paint);
            }
            paint.setColor(Color.argb(255, 0, 0, 0));
			/*
            canvas.drawRect(snakeXs[0] * blockSize,
							(snakeYs[0] * blockSize),
							(snakeXs[0] * blockSize) + blockSize,
							(snakeYs[0] * blockSize) + blockSize,
							paint);
			*/
			canvas.drawPoint((snakeXs[0] * blockSize)+(blockSize/2),
							 (snakeYs[0] * blockSize)+(blockSize/2),paint);
            // Set the color of the paint to draw Bob red
            paint.setColor(Color.argb(255, 255, 0, 0));

            // Draw Bob
			/*
            canvas.drawRect(bobX * blockSize,
							(bobY * blockSize),
							(bobX * blockSize) + blockSize,
							(bobY * blockSize) + blockSize,
							paint);
			*/
			canvas.drawPoint((bobX * blockSize)+(blockSize/2),
							(bobY * blockSize)+(blockSize/2),paint);
            // Unlock the canvas and reveal the graphics for this frame
            surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }
    public boolean updateRequired() {

        // Are we due to update the frame
        if(nextFrameTime <= System.currentTimeMillis()){
            // Tenth of a second has passed

            // Setup when the next update will be triggered
            nextFrameTime =System.currentTimeMillis() + MILLIS_PER_SECOND / FPS;

            // Return true so that the update and draw
            // functions are executed
            return true;
        }

        return false;
    }
    float x1,x2;
    float y1, y2;
    private static final int SWIPE_THRESHOLD = 20;


    @Override
    public boolean onTouchEvent(MotionEvent t)
    {
        switch (t.getAction())
        {
				// when user first touches the screen we get x and y coordinate
            case MotionEvent.ACTION_DOWN:
				{
					x1 = t.getX();
					y1 = t.getY();
					break;
				}
            case MotionEvent.ACTION_UP:
				{
					x2 = t.getX();
					y2 = t.getY();

					float diffY = y2 - y1;
					float diffX = x2 - x1;


					if (Math.abs(diffX) > Math.abs(diffY)) {
						if (Math.abs(diffX) > SWIPE_THRESHOLD ) {
							if (diffX > 0) {
								if(heading!=Heading.LEFT)
									heading=Heading.RIGHT;
							} else {
								if(heading!=Heading.RIGHT)
									heading=Heading.LEFT;
							}
						}
					} else {
						if (Math.abs(diffY) > SWIPE_THRESHOLD ) {
							if (diffY > 0) {
								if(heading!=Heading.UP)
									heading=Heading.DOWN;
							} else {
								if(heading!=Heading.DOWN)
									heading=Heading.UP;
							}
						}
					}}


        }	//if left to right sweep event on screen

        return true;
    }

}
