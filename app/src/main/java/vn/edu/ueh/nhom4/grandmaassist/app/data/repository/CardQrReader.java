package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.CardIdentity;

/** Bounded, sequential retries on source pixels; crops are scan-only, never the submitted photo. */
public final class CardQrReader {
    public static final class Failure extends RuntimeException {
        public final String code, structure;
        Failure(String code) { this(code,""); }
        Failure(String code,String structure) { super(code);this.code=code;this.structure=structure; }
    }
    private CardQrReader() { }
    public static Task<CardIdentity> read(Executor worker,byte[] source,Supplier<Boolean> current) {
        return Tasks.call(worker,()->{
            if(!current.get())throw new Failure("CANCELLED");
            Bitmap pixels;
            try { pixels=VerificationImages.qrBitmap(source); }
            catch(Exception e) { throw new Failure("IMAGE"); }
            try { return new Session(worker,pixels,current); }
            catch(RuntimeException e) { pixels.recycle();throw new Failure("ENGINE"); }
        }).continueWithTask(prepared->{
            if(!prepared.isSuccessful())throw prepared.getException();
            Session session=prepared.getResult();
            try { return session.read().addOnCompleteListener(done->session.close()); }
            catch(RuntimeException e) { session.close();throw e; }
        });
    }
    private static final class Observation {
        final Set<String> values=new LinkedHashSet<>();
        final List<Rect> boxes=new ArrayList<>();
        boolean potential;
    }
    private static final class Session {
        final Executor worker;
        final Bitmap source;
        final Supplier<Boolean> current;
        final BarcodeScanner scanner=BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAllPotentialBarcodes().build());
        final Set<String> values=new LinkedHashSet<>();
        boolean potential;
        Session(Executor worker,Bitmap source,Supplier<Boolean> current){this.worker=worker;this.source=source;this.current=current;}
        Task<CardIdentity> read(){
            Rect all=new Rect(0,0,source.getWidth(),source.getHeight());
            return scan(all,2048).continueWithTask(full->{
                if(!full.isSuccessful())throw full.getException();
                collect(full.getResult());
                if(!values.isEmpty())return parsed();
                List<Rect> regions=new ArrayList<>();
                for(Rect box:full.getResult().boxes){
                    if(regions.size()==4)break;
                    Rect expanded=expand(box);
                    if(expanded.width()>0 && expanded.height()>0)regions.add(expanded);
                }
                if(regions.isEmpty()) {
                    int w=source.getWidth(),h=source.getHeight();
                    regions.add(new Rect(0,0,Math.max(1,w*3/5),Math.max(1,h*3/5)));
                    regions.add(new Rect(w*2/5,0,w,Math.max(1,h*3/5)));
                    regions.add(new Rect(0,h*2/5,Math.max(1,w*3/5),h));
                    regions.add(new Rect(w*2/5,h*2/5,w,h));
                    regions.add(new Rect(w/5,h/5,Math.max(1,w*4/5),Math.max(1,h*4/5)));
                }
                Task<Void> chain=Tasks.forResult(null);
                for(Rect region:regions)chain=chain.continueWithTask(previous->{
                    if(!previous.isSuccessful())throw previous.getException();
                    if(!current.get())throw new Failure("CANCELLED");
                    return scan(region,1600).continueWith(result->{
                        if(!result.isSuccessful())throw result.getException();collect(result.getResult());return null;
                    });
                });
                return chain.continueWithTask(done->{if(!done.isSuccessful())throw done.getException();return parsed();});
            });
        }
        private void collect(Observation observation){values.addAll(observation.values);potential|=observation.potential;}
        private Task<CardIdentity> parsed(){
            if(!current.get())return Tasks.forException(new Failure("CANCELLED"));
            if(values.isEmpty())return Tasks.forException(new Failure(potential?"UNREADABLE":"NOT_FOUND"));
            if(values.size()!=1)return Tasks.forException(new Failure("MULTIPLE"));
            String value=values.iterator().next();
            try{return Tasks.forResult(CardIdentity.parse(value));}
            catch(CardIdentity.ParseFailure e){return Tasks.forException(new Failure(e.code,
                    "FORMAT".equals(e.code)?CardIdentity.structureSummary(value):""));}
            catch(RuntimeException e){return Tasks.forException(new Failure("FORMAT",CardIdentity.structureSummary(value)));}
        }
        private Rect expand(Rect box){
            int margin=Math.max(16,Math.max(box.width(),box.height())/5);
            return new Rect(Math.max(0,box.left-margin),Math.max(0,box.top-margin),
                    Math.min(source.getWidth(),box.right+margin),Math.min(source.getHeight(),box.bottom+margin));
        }
        private Task<Observation> scan(Rect region,int maxSide){
            if(!current.get())return Tasks.forException(new Failure("CANCELLED"));
            return Tasks.call(worker,()->{
                if(!current.get())throw new Failure("CANCELLED");
                Bitmap crop=Bitmap.createBitmap(source,region.left,region.top,region.width(),region.height());
                int size=Math.max(crop.getWidth(),crop.getHeight());
                int target=size<512?1024:Math.min(size,maxSide);
                if(size==target)return crop;
                float factor=(float)target/size;
                Bitmap scaled=Bitmap.createScaledBitmap(crop,Math.max(1,Math.round(crop.getWidth()*factor)),
                        Math.max(1,Math.round(crop.getHeight()*factor)),size>target);
                if(crop!=source && crop!=scaled)crop.recycle();return scaled;
            }).continueWithTask(input->{
                if(!input.isSuccessful())throw input.getException();
                Bitmap image=input.getResult();
                if(!current.get()){if(image!=source)image.recycle();throw new Failure("CANCELLED");}
                try {
                    return scanner.process(InputImage.fromBitmap(image,0)).continueWith(result->{
                        try {
                            if(!current.get())throw new Failure("CANCELLED");
                            if(!result.isSuccessful())throw new Failure("ENGINE");
                            Observation observation=new Observation();
                            for(Barcode code:result.getResult()){
                                String raw=code.getRawValue();
                                if(raw!=null && !raw.isEmpty())observation.values.add(raw);
                                else {
                                    observation.potential=true;Rect box=code.getBoundingBox();
                                    if(box!=null){
                                        float x=(float)region.width()/image.getWidth(),y=(float)region.height()/image.getHeight();
                                        observation.boxes.add(new Rect(region.left+(int)Math.floor(box.left*x),region.top+(int)Math.floor(box.top*y),
                                                region.left+(int)Math.ceil(box.right*x),region.top+(int)Math.ceil(box.bottom*y)));
                                    }
                                }
                            }
                            return observation;
                        } finally {if(image!=source)image.recycle();}
                    });
                } catch(RuntimeException e){if(image!=source)image.recycle();throw new Failure("ENGINE");}
            });
        }
        void close(){scanner.close();if(!source.isRecycled())source.recycle();values.clear();}
    }
}
