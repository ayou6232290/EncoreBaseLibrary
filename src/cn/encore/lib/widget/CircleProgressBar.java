package cn.encore.lib.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ProgressBar;
/**
 * 圆形进度条
 * @author Encore.liang
 */
public class CircleProgressBar extends ProgressBar {

	private float mStartAngle = -90.0f;
	
	private float mAngle = 0.0f;
	
	private Paint mPaint;
	
	private PorterDuffXfermode mPorter;

	public CircleProgressBar(Context context) {
		this(context, null);
	}

	public CircleProgressBar(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.progressBarStyle);
	}

	public CircleProgressBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mPorter = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
	}

	public void setStartAngle(float startAngle) {
		mStartAngle = startAngle;
	}

	@Override
	public synchronized void setProgress(int progress) {
		super.setProgress(progress);
		float per = (float) progress / (float) getMax();
		mAngle = 360.0f * per;
		invalidate();
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		Drawable d = getIndeterminateDrawable();
		if (d != null) {
			Rect outRect = new Rect();
			getDrawingRect(outRect);
			
			//图片抗锯齿
			canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG)); 

			canvas.saveLayer(null, null, Canvas.ALL_SAVE_FLAG);
			mPaint.setXfermode(null);
			canvas.translate(getPaddingLeft(), getPaddingTop());
			canvas.drawBitmap(((BitmapDrawable) d).getBitmap(), null, outRect, mPaint);

			mPaint.setXfermode(mPorter);
			RectF rectF = new RectF(outRect);
			rectF.left -= rectF.right;
			rectF.top -= rectF.bottom;
			rectF.right *= 2;
			rectF.bottom *= 2;
			canvas.drawArc(rectF, mStartAngle + mAngle, 360 - mAngle, true, mPaint);

			canvas.restore();
		}
	}
}
