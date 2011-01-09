package sk.gista.android.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.view.View;

public abstract class CustomAnimation extends TimerTask {

	private int duration;
	private int count;
	private int frame;
	private Timer timer;
	private View view;
	
	private boolean stooped;
	
	public CustomAnimation() {
	}
	
	public CustomAnimation(int duration, int count) {
		this.duration = duration;
		this.count = count;
	}
	
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	public void setFramesCount(int count) {
		this.count = count;
	}
	
	public synchronized void stop() {
		cancel();
		stooped = true;
	}
	
	private synchronized boolean isStopped() {
		return stooped;
	}
	/**
	 * Sets view that will be invalidated on every animation step
	 * 
	 * @param view
	 */
	public void setView(View view) {
		this.view = view;
	}
	
	public void start() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		frame = 0;
		timer.schedule(this, 0, duration/count);
	}
	
	@Override
	public void run() {
		if (isStopped()) {
			return;
		}
		frame++;
		if (frame > count) {
			timer.cancel();
			view.post(new Runnable() {
				@Override
				public void run() {
					onEnd();
					view.invalidate();
				}
			});
		} else {
			final float fraction = frame/(float) count;
			view.post(new Runnable() {
				
				@Override
				public void run() {
					onFrame(fraction);
					view.invalidate();
				}
			});
		}
	}
	
	public abstract void onFrame(float fraction);
	public abstract void onEnd();
	
	public static class CompositeAnimation extends CustomAnimation {

		public CompositeAnimation(int duration, int count) {
			super(duration, count);
		}

		private List<CustomAnimation> animations = new ArrayList<CustomAnimation>(5);
		
		public void addAnimation(CustomAnimation animation) {
			animations.add(animation);
		}
		
		@Override
		public void onFrame(float fraction) {
			for (CustomAnimation animation : animations) {
				animation.onFrame(fraction);
			}
		}

		@Override
		public void onEnd() {
			for (CustomAnimation animation : animations) {
				animation.onEnd();
			}
		}
	}
}
