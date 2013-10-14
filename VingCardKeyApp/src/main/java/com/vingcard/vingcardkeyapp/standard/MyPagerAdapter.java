package com.vingcard.vingcardkeyapp.standard;

import java.util.List;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import com.vingcard.vingcardkeyapp.model.KeyCard;
import com.vingcard.vingcardkeyapp.ui.CardFragment;

public class MyPagerAdapter extends FragmentStatePagerAdapter implements
		ViewPager.OnPageChangeListener {
	
	public final static float BIG_SCALE = 1.0f;
	public final static float SMALL_SCALE = 0.7f;
	public final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;

	private Context context;
    private boolean firstObject = true;
	private int curPos = 0;
	private List<KeyCard> data;
	private SparseArray<Fragment> mFragmentMap = new SparseArray<Fragment>();

	public MyPagerAdapter(Context context, FragmentManager fm, List<KeyCard> data) {
		super(fm);
		this.context = context;
        this.data = data;
	}

	@Override
	public Fragment getItem(int position){
        Fragment f = mFragmentMap.get(position);
        if(f != null){
            return f;
        }

        // make the first pager bigger than others
        float scale;
        if (firstObject){
        	scale = BIG_SCALE;     	
        	firstObject = false;
        }
        else
        	scale = SMALL_SCALE;
        
        Fragment createdFragment = CardFragment.newInstance(context, scale, data.get(position));
        mFragmentMap.put(position, createdFragment);
        return createdFragment;
	}
	
	public int getItemPosition(Object item) {
		return POSITION_NONE;
    }
	
	public void destroyItem(ViewGroup container, int position, Object object) {
	    super.destroyItem(container, position, object);
	    mFragmentMap.remove(position);
	}

	@Override
	public int getCount(){		
		return data.size();
	}
	
	@Override
	public void setPrimaryItem(ViewGroup container, int position, Object object) {
		super.setPrimaryItem(container, position, object);
		if(object instanceof CardFragment){
			CardFragment f = (CardFragment) object;
			View v = f.getView();
			if(v != null){
				v.bringToFront();
			}
		}
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels){	
		
		if (positionOffset >= 0f && positionOffset <= 1f && position+1 < getCount())
		{
			float newScale1 = BIG_SCALE 
					- DIFF_SCALE * positionOffset;
			
			float newScale2 = SMALL_SCALE 
					+ DIFF_SCALE * positionOffset;
			
			getFragment(position).setScale(newScale1);
			getFragment(position + 1).setScale(newScale2);
		}
	}

	@Override
	public void onPageSelected(int position) {
		curPos = position;
	}
	
	@Override
	public void onPageScrollStateChanged(int state) {
		if(state == ViewPager.SCROLL_STATE_IDLE){
			if(curPos > 0){
				CardFragment f = getFragment(curPos-1);
				if(f != null){
					f.turnCardToFront();
				}
			}
			if(curPos+1 < getCount()){
				CardFragment f = getFragment(curPos+1);
				if(f != null){
					f.turnCardToFront();
				}
			}	
		}
	}
	
	private CardFragment getFragment(int position){
        Fragment f = mFragmentMap.get(position);
        if(f == null){
            f = getItem(position);
        }
		return (CardFragment) f;
	}

}
