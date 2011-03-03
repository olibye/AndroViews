package info.staticfree.android.utils;

import java.lang.reflect.Method;

import android.os.Build;
import android.view.MotionEvent;

public class MotionEventWrapper {
	
    public static float getX(MotionEvent ev, int optPointerIndex){
    	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT){
    		return ev.getX();
    	}else{
    		try {
				Method getX = MotionEvent.class.getMethod("getX", int.class);
				return ((Float) getX.invoke(ev, optPointerIndex)).floatValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    public  static float getY(MotionEvent ev, int optPointerIndex){
    	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT){
    		return ev.getY();
    	}else{
    		try {
				Method getY = MotionEvent.class.getMethod("getY", int.class);
				return ((Float) getY.invoke(ev, optPointerIndex)).floatValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    public static int getPointerId(MotionEvent ev, int optPointerIndex){
    	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT){
    		return 0;
    	}else{
    		try {
				Method getPointerId = MotionEvent.class.getMethod("getPointerId", int.class);
				return ((Integer) getPointerId.invoke(ev, optPointerIndex)).intValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
    
    public static int findPointerIndex(MotionEvent ev, int activePointerId){
    	if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT){
    		return 0;
    	}else{
    		try {
				Method findPointerIndex = MotionEvent.class.getMethod("findPointerIndex", int.class);
				return ((Integer) findPointerIndex.invoke(ev, activePointerId)).intValue();
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
    	}
    }
}
