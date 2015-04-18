
package com.peter.coolcleaner;

import java.util.List;
import java.util.Random;
import com.nineoldandroids.animation.TimeAnimator;
import com.nineoldandroids.view.ViewHelper;
import com.peter.coolcleaner.Main.AppInfo;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class Board extends RelativeLayout {
	
      static float MIN_SCALE = 0.2f;
      static float MAX_SCALE = 1f;

	public Board(Context context, AttributeSet as) {
		super(context, as);
		setWillNotDraw(true);
	}
	
	private Random sRNG = new Random();

	private float lerp(float a, float b, float f) {
		return (b - a) * f + a;
	}

	private float randfrange(float a, float b) {
		return lerp(a, b, sRNG.nextFloat());
	}

	private int randsign() {
		return sRNG.nextBoolean() ? 1 : -1;
	}

	private float mag(float x, float y) {
		return (float) Math.sqrt(x * x + y * y);
	}

	private float clamp(float x, float a, float b) {
		return ((x < a) ? a : ((x > b) ? b : x));
	}

	private class Head extends ImageView {
		
		private float x, y, a;

		private float va;
		private float vx, vy;

		private float z;

		private int h, w, diameter;

		private boolean grabbed;
		private float grabx, graby;
		private float grabx_offset, graby_offset;

		public Rect mRect;

		private Head(Context context, AttributeSet as) {
			super(context, as);
		}
		
		@Override
		public void getHitRect(Rect outRect) {
		    if (mRect == null){
		        super.getHitRect(outRect);
		    } else {
		        outRect.set(mRect);
		    }
		}
		

		private void reset() {

			a = randfrange(0, 360);
			va = randfrange(-30, 30);

			vx = randfrange(-40, 40) * z;
			vy = randfrange(-40, 40) * z;
			final float boardh = boardHeight;
			final float boardw = boardWidth;
			
			x = randfrange(0, boardw);
			y = randfrange(0, boardh);
		}

		private void update(float dt) {
			if (grabbed) {
				vx = (vx * 0.75f) + ((grabx - x) / dt) * 0.25f;
				x = grabx;
				vy = (vy * 0.75f) + ((graby - y) / dt) * 0.25f;
				y = graby;
				Log.i("peter~", "vy =" + vy);
			} else {
				x = (x + vx * dt);
				y = (y + vy * dt);
				a = (a + va * dt);
			}
		}
		

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouchEvent(MotionEvent e) {
			switch (e.getAction()) {
			case MotionEvent.ACTION_DOWN:
				grabbed = true;
				grabx_offset = e.getRawX() - x;
				graby_offset = e.getRawY() - y;
				va = 0;
			case MotionEvent.ACTION_MOVE:
				grabx = e.getRawX() - grabx_offset;
				graby = e.getRawY() - graby_offset;
				break;
			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				grabbed = false;
				float a = randsign() * clamp(mag(vx, vy) * 0.33f, 0, 1080f);
				va = randfrange(a * 0.5f, a);
				break;
			}
			return true;
		}
	}
	
	private TimeAnimator mAnim;
	private int boardWidth;
	private int boardHeight;
	
	private void reset() {
		removeAllViews();
		Main main = (Main) getContext();
		List<AppInfo> infos = main.getRunningAppInfos();
		for(int i = 0, size = infos.size(); i < size; i++) {
			Head nv = new Head(getContext(), null);
			nv.z = ((float) i / size);
			nv.z *= nv.z;
			BitmapDrawable drawable = infos.get(i).appIcon;
			nv.w = drawable.getBitmap().getWidth();
			nv.h= drawable.getBitmap().getHeight();
			nv.setImageDrawable(drawable);
			nv.diameter = 2 * nv.w;
			nv.reset();
			nv.x = (randfrange(0, boardWidth));
			nv.y = (randfrange(0, boardHeight));
			nv.setTag(infos.get(i));
			addView(nv);
		}

		if (mAnim != null) {
			mAnim.cancel();
		}
		mAnim = new TimeAnimator();
		mAnim.setTimeListener(new TimeAnimator.TimeListener() {

			@Override
			public void onTimeUpdate(TimeAnimator animation, long totalTime,
					long deltaTime) {

				for (int i = 0; i < getChildCount(); i++) {
					View v = getChildAt(i);
					if (!(v instanceof Head))
						continue;
					Head nv = (Head) v;
					nv.update(deltaTime / 1000f);
					
					ViewHelper.setRotation(nv, nv.a);
					
					ViewHelper.setX(nv, nv.x - ViewHelper.getPivotX(nv));
					ViewHelper.setY(nv, nv.y - ViewHelper.getPivotY(nv));
			        RectF rect = new RectF();
			        rect.top = 0;
			        rect.bottom = (float) nv.h; 
			        rect.left = 0; 
			        rect.right = (float) nv.w;  
			        rect.offset((float) ViewHelper.getX(nv), (float) ViewHelper.getY(nv));

			        if (nv.mRect == null) nv.mRect = new Rect();
			        rect.round(nv.mRect);
			        
					if ((nv.x < -nv.diameter || nv.x > boardWidth
							|| nv.y < -nv.diameter
							|| nv.y > boardHeight)) {
						if(nv.vx > 2000 || nv.vy > 2000 || nv.vx < -2000 || nv.vy < -2000) {
							Toast.makeText(getContext(), "delete it", Toast.LENGTH_SHORT).show();
							removeView(nv);
							AppInfo info = (AppInfo) nv.getTag();
							Main main = (Main) getContext();
							if(main.isRoot) {
								removeView(nv);
								main.forceStop(info);
							}else {
								main.showForceStopView(info, nv);
							}
						}else {
							nv.reset();
						}
					}

				}
			}
		});
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		boardWidth = w;
		boardHeight = h;
	}

	public void startAnimation() {
		stopAnimation();
		if (mAnim == null) {
			post(new Runnable() {
				public void run() {
					reset();
					startAnimation();
				}
			});
		} else {
			mAnim.start();
		}
	}

	public void stopAnimation() {
		if (mAnim != null)
			mAnim.cancel();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		stopAnimation();
	}

	@Override
	public boolean isOpaque() {
		return false;
	}
}