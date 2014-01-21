package cn.encore.lib.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;

public class CoverFlowViewPager extends ViewPager {
	public CoverFlowViewPager(Context paramContext) {
		super(paramContext);
		init();
	}

	public CoverFlowViewPager(Context paramContext, AttributeSet paramAttributeSet) {
		super(paramContext, paramAttributeSet);
		init();
	}

	private void init() {
		setFadingEdgeLength(0);
	}

	protected void dispatchDraw(Canvas paramCanvas) {
		super.dispatchDraw(paramCanvas);
	}

	protected boolean drawChild(Canvas paramCanvas, View paramView, long paramLong) {
		paramCanvas.save();
		int scrollPos = getScrollX() + getWidth() / 2;
		int widthCenter = paramView.getLeft() + paramView.getWidth() / 2;
		int heightCenter = paramView.getTop() + paramView.getHeight() / 2;

		int m = scrollPos - widthCenter;
		int isLeftOrRight = 0; // 左边还是右边
		if (m > 0)
			isLeftOrRight = 1;
		int i1 = Math.abs(m);
		float postionXY = 1.0F - 0.3F * (1.0F * Math.min(getWidth(), i1) / getWidth());

		if (isLeftOrRight != 0) { // 右边
			paramCanvas.scale(postionXY, postionXY, paramView.getLeft() + paramView.getWidth(), heightCenter);
		} else {
			paramCanvas.scale(postionXY, postionXY, paramView.getLeft(), heightCenter);
		}
		super.drawChild(paramCanvas, paramView, paramLong);
		paramCanvas.restore();
		return true;
	}

	

}
