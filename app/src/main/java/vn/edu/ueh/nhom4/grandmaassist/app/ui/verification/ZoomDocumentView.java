package vn.edu.ueh.nhom4.grandmaassist.app.ui.verification;

import android.content.Context;
import android.graphics.Matrix;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.widget.AppCompatImageView;

/** Pinch to zoom and drag. Accessibility actions provide zoom without multi-touch. */
public final class ZoomDocumentView extends AppCompatImageView {
    private final Matrix transform=new Matrix();
    private final ScaleGestureDetector detector;
    private float x,y,scale=1,base=1;
    public ZoomDocumentView(Context context,byte[] bytes){
        super(context);setImageBitmap(vn.edu.ueh.nhom4.grandmaassist.app.data.repository.VerificationImages.preview(bytes,3072));setScaleType(ScaleType.MATRIX);
        setContentDescription("Ảnh giấy tờ. Phóng to bằng hai ngón tay, kéo để di chuyển.");
        detector=new ScaleGestureDetector(context,new ScaleGestureDetector.SimpleOnScaleGestureListener(){
            @Override public boolean onScale(ScaleGestureDetector d){zoom(d.getScaleFactor(),d.getFocusX(),d.getFocusY());return true;}
        });
        androidx.core.view.ViewCompat.replaceAccessibilityAction(this,androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
                "Phóng to",(v,a)->{zoom(1.5f,getWidth()/2f,getHeight()/2f);return true;});
        androidx.core.view.ViewCompat.replaceAccessibilityAction(this,androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_SCROLL_BACKWARD,
                "Thu nhỏ",(v,a)->{zoom(1/1.5f,getWidth()/2f,getHeight()/2f);return true;});
        setFocusable(true);
    }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){
        super.onSizeChanged(w,h,oldw,oldh);if(getDrawable()==null)return;
        base=Math.min((float)w/getDrawable().getIntrinsicWidth(),(float)h/getDrawable().getIntrinsicHeight());
        scale=base;transform.setScale(base,base);transform.postTranslate((w-getDrawable().getIntrinsicWidth()*base)/2,
                (h-getDrawable().getIntrinsicHeight()*base)/2);setImageMatrix(transform);
    }
    private void zoom(float factor,float focusX,float focusY){
        float next=Math.max(base,Math.min(base*8,scale*factor));factor=next/scale;scale=next;
        transform.postScale(factor,factor,focusX,focusY);setImageMatrix(transform);
    }
    @Override public boolean onTouchEvent(MotionEvent e){
        detector.onTouchEvent(e);
        if(e.getActionMasked()==MotionEvent.ACTION_DOWN){x=e.getX();y=e.getY();}
        if(e.getActionMasked()==MotionEvent.ACTION_MOVE && !detector.isInProgress() && e.getPointerCount()==1){
            transform.postTranslate(e.getX()-x,e.getY()-y);setImageMatrix(transform);
        }
        if(e.getActionMasked()==MotionEvent.ACTION_UP)performClick();
        x=e.getX();y=e.getY();return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
