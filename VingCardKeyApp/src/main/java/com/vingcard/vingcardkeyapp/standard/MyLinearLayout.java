package com.vingcard.vingcardkeyapp.standard;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {
	
	private float scale = MyPagerAdapter.BIG_SCALE;
	private boolean scaleOnCenter = true;

	public MyLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyLinearLayout(Context context) {
		super(context);
	}

	public void setScaleBoth(float scale, boolean scaleOnCenter)
	{
		this.scale = scale;
		this.scaleOnCenter = scaleOnCenter;
		this.invalidate(); 	// If you want to see the scale every time you set
							// scale you need to have this line here, 
							// invalidate() function will call onDraw(Canvas)
							// to redraw the view for you
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// The main mechanism to display scale animation, you can customize it
		// as your needs
		int pivotX = scaleOnCenter ? this.getWidth()/2 : 0;
		int pivotY = scaleOnCenter ? this.getHeight()/2 : 0;
		canvas.scale(scale, scale, pivotX, pivotY);
		
		super.onDraw(canvas);
		
	}

}
