package info.staticfree.android.utils;

import java.lang.reflect.Method;

import android.os.Build;
import android.view.MotionEvent;

public class MotionEventWrapper {
	public static final String TAG = MotionEventWrapper.class.getSimpleName();
	private static Method
		getX,
		getY,
		getPointerId,
		findPointerIndex;
	
	private static final boolean IS_OLD = Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT;
	
	static {
		initCompatibility();
	}
	
	private static void initCompatibility(){
		if (!IS_OLD){
			try {
				getX = MotionEvent.class.getMethod("getX", int.class);
				getY = MotionEvent.class.getMethod("getY", int.class);
				getPointerId = MotionEvent.class.getMethod("getPointerId", int.class);
				findPointerIndex = MotionEvent.class.getMethod("findPointerIndex", int.class);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		}
	}
	
    public static float getX(MotionEvent ev, int optPointerIndex){
    	if (IS_OLD){
    		return ev.getX();
    	}else{
    		try {
				return ((Float) getX.invoke(ev, optPointerIndex)).floatValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    public  static float getY(MotionEvent ev, int optPointerIndex){
    	if (IS_OLD){
    		return ev.getY();
    	}else{
    		try {
				return ((Float) getY.invoke(ev, optPointerIndex)).floatValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    public static int getPointerId(MotionEvent ev, int optPointerIndex){
    	if (IS_OLD){
    		return 0;
    	}else{
    		try {
				return ((Integer) getPointerId.invoke(ev, optPointerIndex)).intValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    public static int findPointerIndex(MotionEvent ev, int activePointerId){
    	if (IS_OLD){
    		return 0;
    	}else{
    		try {
				return ((Integer) findPointerIndex.invoke(ev, activePointerId)).intValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
}