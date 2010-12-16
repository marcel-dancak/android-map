package sk.utils;

import java.util.Timer;
import java.util.TimerTask;

public class MyAnimation extends TimerTask {

	private int duration;
	private int count;
	private Timer timer;
	private long startTime;
	
	private MyAnimationListener listener;
	
	public MyAnimation(int duration, int count) {
		this.duration = duration;
		this.count = count;
	}
	
	public void start() {
		if (timer != null) {
			timer.cancel();
		}
		timer = new Timer();
		startTime = System.currentTimeMillis();
		timer.schedule(this, 0, duration/count);
	}
	
	public void onAnimationStep(MyAnimationListener listener) {
		this.listener = listener;
	}
	
	@Override
	public void run() {
		float fraction = (System.currentTimeMillis()-startTime)/(float) duration;
		if (fraction <= 1) {
			if (listener != null) {
				listener.onFrame(fraction);
			}
		} else {
			timer.cancel();
			if (listener != null) {
				listener.onFrame(1);
				listener.onEnd();
			}
		}
	}

	
	public interface MyAnimationListener {
		void onFrame(float fraction);
		void onEnd();
	}
}
