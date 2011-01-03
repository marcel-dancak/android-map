package sk.gista.android.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.view.View;

public class Animator extends TimerTask {

	private int duration;
	private int count;
	private int frame;
	private Timer timer;
	private CustomAnimation animation;
	private View view; // can be list later
	
	public Animator(int duration, int count, CustomAnimation animation) {
		this.duration = duration;
		this.count = count;
		this.animation = animation;
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
		frame++;
		if (frame > count) {
			timer.cancel();
			view.post(new Runnable() {
				@Override
				public void run() {
					animation.onEnd();
					view.invalidate();
				}
			});
		} else {
			final float fraction = frame/(float) count;
			view.post(new Runnable() {
				
				@Override
				public void run() {
					animation.onFrame(fraction);
					view.invalidate();
				}
			});
		}
	}
	
	public interface CustomAnimation {
		void onFrame(float fraction);
		void onEnd();
	}
	
	public static class CompositeAnimation implements CustomAnimation {

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
