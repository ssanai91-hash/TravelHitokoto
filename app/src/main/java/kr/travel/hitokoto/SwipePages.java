package kr.travel.hitokoto;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/** Bounded horizontal pages. Child taps work; horizontal drags cancel child clicks. */
public final class SwipePages extends FrameLayout {
    private IntFunction<View> factory;
    private IntConsumer changed;
    private int position,total=1;
    private float startX,startY;
    private long started;
    private boolean dragging,animating;
    private final int slop;
    public SwipePages(Context context){
        super(context);slop=ViewConfiguration.get(context).getScaledTouchSlop();
        setClipChildren(true);setClipToPadding(true);setFocusable(true);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void configure(int total,int position,IntFunction<View> factory,IntConsumer changed){
        this.total=Math.max(1,total);this.position=Math.max(0,Math.min(position,this.total-1));
        this.factory=factory;this.changed=changed;animating=false;dragging=false;
        for(int i=0;i<getChildCount();i++)getChildAt(i).animate().cancel();
        removeAllViews();addView(factory.apply(this.position),new LayoutParams(-1,-1));
        describe();
    }
    private void describe(){setContentDescription("목록 "+(position+1)+" / "+total+"페이지, 좌우로 밀어 넘기기");}
    private void down(MotionEvent e){startX=e.getX();startY=e.getY();started=e.getEventTime();dragging=false;}
    @Override public boolean onInterceptTouchEvent(MotionEvent e){
        if(animating)return true;
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:down(e);break;
            case MotionEvent.ACTION_MOVE:
                float dx=e.getX()-startX,dy=e.getY()-startY;
                if(Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.25f){dragging=true;getParent().requestDisallowInterceptTouchEvent(true);return true;}
                break;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:dragging=false;break;
        }
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        if(animating)return true;
        switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:down(e);return true;
            case MotionEvent.ACTION_MOVE:
                float dx=e.getX()-startX,dy=e.getY()-startY;
                if(!dragging&&Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.25f)dragging=true;
                if(dragging&&getChildCount()>0){boolean edge=(dx>0&&position==0)||(dx<0&&position==total-1);getChildAt(0).setTranslationX(dx*(edge?0.16f:0.75f));}
                return true;
            case MotionEvent.ACTION_UP:
                float distance=e.getX()-startX;
                float velocity=Math.abs(distance)*1000f/Math.max(1,e.getEventTime()-started);
                float threshold=Math.max(slop*2,Math.min(getWidth()*0.18f,72*getResources().getDisplayMetrics().density));
                if(dragging&&(Math.abs(distance)>threshold||(Math.abs(distance)>slop*2&&velocity>650))&&move(distance<0?1:-1)){dragging=false;return true;}
                reset();if(!dragging)performClick();dragging=false;return true;
            case MotionEvent.ACTION_CANCEL:reset();dragging=false;return true;
        }
        return true;
    }
    private void reset(){if(getChildCount()>0)getChildAt(0).animate().translationX(0).setDuration(150).start();}
    public boolean move(int direction){
        int next=position+direction;if(animating||factory==null||next<0||next>=total){reset();return false;}
        View outgoing=getChildAt(0),incoming=factory.apply(next);int width=getWidth();
        position=next;describe();animating=true;
        incoming.setTranslationX(direction*width);addView(incoming,new LayoutParams(-1,-1));
        if(outgoing!=null)outgoing.animate().translationX(-direction*width).setDuration(190).start();
        incoming.animate().translationX(0).setDuration(190).withEndAction(()->{if(outgoing!=null)removeView(outgoing);animating=false;}).start();
        if(changed!=null)changed.accept(position);return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.HorizontalScrollView");info.setScrollable(total>1);
        if(position<total-1)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        if(position>0)info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)return move(1);
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)return move(-1);
        return super.performAccessibilityAction(action,args);
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_DPAD_RIGHT)return move(1);
        if(key==KeyEvent.KEYCODE_DPAD_LEFT)return move(-1);
        return super.onKeyDown(key,event);
    }
}
