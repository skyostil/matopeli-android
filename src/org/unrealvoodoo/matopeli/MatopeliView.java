package org.unrealvoodoo.matopeli;

import java.util.Vector;
import android.content.Context;
import android.content.res.Resources;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.KeyEvent;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.graphics.Matrix;
import android.media.MediaPlayer;

public class MatopeliView extends SurfaceView implements SurfaceHolder.Callback {
	private MatopeliThread mThread;

	public MatopeliView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);
		
		mThread = new MatopeliThread(holder, context);
		setFocusable(true);
	}
	
	public void surfaceCreated(SurfaceHolder holder) {
		mThread.start();
	}
	
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		mThread.setSurfaceSize(width, height);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		mThread.quit();
		try {
			mThread.join();
		} catch (InterruptedException e) {
		}
		
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		return mThread.onKeyDown(keyCode, msg);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent msg) {
		return mThread.onKeyUp(keyCode, msg);
	}
	
	protected class MatopeliThread extends Thread {
		private SurfaceHolder mSurfaceHolder;
		private Context mContext;
		private boolean mDone = false;
		private boolean mGameRunning = false;
		private boolean mGameOver = false;
		private Drawable mBodyImage;
		private Drawable mHeadImage;
		private Drawable mAppleImage;
		private Bitmap mBackgroundImage;
		private PointF mHeadPos = new PointF();
		private PointF mApplePos = new PointF();
		private float mHeadAngle;
		private Vector<TailSegment> mTail;
		private long mLastTime;
		private int mLength;
		private float mScale = 0.75f;
		private int mTurnDirection;
		private MediaPlayer mMusicPlayer;
		private MediaPlayer mEffectPlayer;
		private final int mTailDensity = 10;

		protected class TailSegment {
			public PointF pos = new PointF();
		}
		
		public MatopeliThread(SurfaceHolder holder, Context context) {
			mSurfaceHolder = holder;
			mContext = context;
			
			Resources res = context.getResources();
			mHeadImage = res.getDrawable(R.drawable.head); 
			mBodyImage = res.getDrawable(R.drawable.body); 
			mAppleImage = res.getDrawable(R.drawable.apple); 

			mMusicPlayer = MediaPlayer.create(context, R.raw.menuman);
			mMusicPlayer.setLooping(true);
			mEffectPlayer = MediaPlayer.create(context, R.raw.effect1);
/*
			try {
				mMusicPlayer.prepare();
				mEffectPlayer.prepare();
			} catch (java.io.IOException e) {
			}*/
		}
		
		public void quit() {
			mDone = true;
		}
		
		public void startGame() {
			mGameRunning = true;
			mGameOver = false;
			mHeadPos.x = mBackgroundImage.getWidth() / 2;
			mHeadPos.y = mBackgroundImage.getHeight() / 2;
			mHeadAngle = 0.0f;
			mLength = 20;
			mTurnDirection = 0;
			mTail = new Vector<TailSegment>();
			mLastTime = System.currentTimeMillis() + 100;
			placeApple();
			/*mMusicPlayer.start();*/
		}

		protected boolean onKeyDown(int keyCode, KeyEvent msg) {
			if (mGameRunning) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
					mTurnDirection = -1;
					return true;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					mTurnDirection = 1;
					return true;
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
					mTurnDirection = 0;
					return true;
				}
			}
			return false;
		}
		
		protected boolean onKeyUp(int keyCode, KeyEvent msg) {
			if (mGameRunning) {
				if (mGameOver) {
					switch (keyCode) {
					case KeyEvent.KEYCODE_DPAD_UP:
						startGame();
					}
				} else {
					switch (keyCode) {
/*					
					case KeyEvent.KEYCODE_DPAD_LEFT:
						if (mTurnDirection == -1) {
							mTurnDirection = 0;
						}
						return true;
					case KeyEvent.KEYCODE_DPAD_RIGHT:
						if (mTurnDirection == 1) {
							mTurnDirection = 0;
						}
						return true;
*/						
					}
				}
			} else {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
					startGame();
					break;
				}
			}
			return false;
		}
		
		private void updateGame(double dt) {
			if (mGameOver) {
				return;
			}

			TailSegment seg = new TailSegment();
			seg.pos.x = mHeadPos.x;
			seg.pos.y = mHeadPos.y;
			mTail.add(seg);
			
			if (mTail.size() > mLength) {
				mTail.remove(0);
			}
			
			mHeadPos.x += FloatMath.cos(mHeadAngle) * 50 * dt; 
			mHeadPos.y -= FloatMath.sin(mHeadAngle) * 50 * dt;
			mHeadAngle -= 2 * mTurnDirection * dt;
			
			if (mHeadPos.x > mApplePos.x - mAppleImage.getIntrinsicWidth()  / 2 &&
    			mHeadPos.y > mApplePos.y - mAppleImage.getIntrinsicHeight() / 2 &&
    			mHeadPos.x < mApplePos.x + mAppleImage.getIntrinsicWidth()  / 2 &&
    	    	mHeadPos.y < mApplePos.y + mAppleImage.getIntrinsicHeight() / 2) {
				mLength += 20;
				/*mEffectPlayer.start();*/
				placeApple();
			}
			
			for (int i = 0; i < mTail.size(); i += mTailDensity) {
				int w = mBodyImage.getIntrinsicWidth() / 2;
				int h = mBodyImage.getIntrinsicWidth() / 2;
				if (i < mTail.size() - 30 &&
					mHeadPos.x > mTail.get(i).pos.x - w &&
		    	    mHeadPos.y > mTail.get(i).pos.y - h &&
		    		mHeadPos.x < mTail.get(i).pos.x + w &&
		    	    mHeadPos.y < mTail.get(i).pos.y + h) {
					mGameOver = true;
		    	}
			}
			if (mHeadPos.x < 0 || mHeadPos.y < 0 ||
				mHeadPos.x > mBackgroundImage.getWidth() ||
				mHeadPos.y > mBackgroundImage.getHeight()) {
				mGameOver = true;
			}
		}
		
		protected void placeApple() {
			mApplePos.x = (float)(Math.random() * (mBackgroundImage.getWidth() - 80)) + 40;
			mApplePos.y = (float)(Math.random() * (mBackgroundImage.getHeight() - 80)) + 40;
		}
		
		public void run() {
			while (!mDone) {
				Canvas c = null;
				
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						if (mGameRunning) {
							long now = System.currentTimeMillis();
							double dt = (now - mLastTime) / 1000.0;
							if (dt > 0) {
								updateGame(dt);
							}
							mLastTime = now;
						}
						render(c);
					}
				} finally {
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}
		
		public void setSurfaceSize(int width, int height) {
			synchronized (mSurfaceHolder) {
				Resources res = mContext.getResources();
				mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.background);
				if (height < width) {
					Matrix m = new Matrix();
					m.setRotate(90);
					m.setScale((float)width / mBackgroundImage.getHeight(), (float)(height) / mBackgroundImage.getWidth());
					m.setTranslate(width / 2, height / 2);
					mBackgroundImage = Bitmap.createBitmap(mBackgroundImage, 0, 0, width, height, m, true);
				}
				mBackgroundImage = Bitmap.createScaledBitmap(mBackgroundImage, width, height, true);
			}
		}
		
		protected void render(Canvas canvas) {
			if (mBackgroundImage == null) {
				return;
			}
			
			int cx = mBackgroundImage.getWidth() / 2;
			int cy = mBackgroundImage.getHeight() / 2;
			Paint paint = new Paint();
			paint.setTextAlign(Align.CENTER);
			paint.setColor(0xffffffff);
			paint.setTextSize(paint.getTextSize() * 1.5f);
			paint.setFlags(1);
			canvas.drawBitmap(mBackgroundImage, 0, 0, null);
			
			if (!mGameRunning) {
				canvas.drawText("Press up to start", cx, cy, paint);
				return;
			}
			
			canvas.drawText((new Integer(mTail.size())).toString(), cx, 32, paint);
			canvas.save();
			
			// draw apple
			canvas.save();
			canvas.translate(mApplePos.x, mApplePos.y);
			canvas.scale(mScale, mScale);
			mAppleImage.setBounds(-33, -34, 33, 34);
			mAppleImage.draw(canvas);
			canvas.restore();
			
			// draw body
			for (int i = 0; i < mTail.size(); i += mTailDensity) {
				canvas.save();
				canvas.translate(mTail.get(i).pos.x, mTail.get(i).pos.y);
				canvas.scale(mScale, mScale);
				mBodyImage.setBounds(-15, -17, 16, 17);
				mBodyImage.draw(canvas);
				canvas.restore();
			}
			
			// draw head
			canvas.save();
			canvas.translate(mHeadPos.x, mHeadPos.y);
			canvas.rotate((float)(mHeadAngle * -180.0f / Math.PI));
			canvas.scale(mScale, mScale);
			mHeadImage.setBounds(-16, -15, 38, 16);
			mHeadImage.draw(canvas);
			canvas.restore();

			canvas.restore();
			if (mGameOver) {
				canvas.drawText("You died! Press up to restart", cx, cy, paint);
			}
		}
	}
}
