package cn.encore.lib.swipeback;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;

public class SwipeBackActivity extends FragmentActivity implements SwipeBackActivityBase {
	private SwipeBackActivityHelper mHelper;
	/**
	 * 是否初始化swipeBack控件
	 */
	private boolean mIsInnitSwipeBack = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mIsInnitSwipeBack) {
			mHelper = new SwipeBackActivityHelper(this);
			mHelper.onActivityCreate();
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		if(mHelper != null)
			mHelper.onPostCreate();
	}
	
	public void setIsInnitSwipeBack(boolean mIsInnitSwipeBack) {
		this.mIsInnitSwipeBack = mIsInnitSwipeBack;
	}

	@Override
	public View findViewById(int id) {
		View v = super.findViewById(id);
		if (v == null && mHelper != null)
			return mHelper.findViewById(id);
		return v;
	}

	@Override
	public SwipeBackLayout getSwipeBackLayout() {
		return mHelper.getSwipeBackLayout();
	}

	@Override
	public void setSwipeBackEnable(boolean enable) {
		getSwipeBackLayout().setEnableGesture(enable);
	}

	@Override
	public void scrollToFinishActivity() {
		getSwipeBackLayout().scrollToFinishActivity();
	}
}
