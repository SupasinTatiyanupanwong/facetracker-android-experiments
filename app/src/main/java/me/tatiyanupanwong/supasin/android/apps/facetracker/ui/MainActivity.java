package me.tatiyanupanwong.supasin.android.apps.facetracker.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Switch;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector.Detections;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import java.io.IOException;

import me.tatiyanupanwong.supasin.android.apps.facetracker.R;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.camera.CameraSourcePreview;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay.FaceGraphic;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay.FrameGraphic;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay.GraphicOverlay;
import me.tatiyanupanwong.supasin.android.apps.facetracker.util.CameraSourceHelper;

public class MainActivity extends AppCompatActivity {

    private ViewHolder mViews;
    private CameraSourceHelper mCameraSourceHelper;

    private FrameGraphic mFrameGraphic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViews = new ViewHolder(this);
        mFrameGraphic = new FrameGraphic(mViews.overlay);

        mCameraSourceHelper = new CameraSourceHelper(this, new GraphicFaceTracker(mViews.overlay),
                this::onStartCameraSource);

        mViews.buttonFlip.setOnClickListener(
                view -> mCameraSourceHelper.flipCamera(
                        () -> setFrameEnabled(mViews.switchFrame.isChecked())));

        mViews.switchFrame.setOnCheckedChangeListener(
                (buttonView, isChecked) -> setFrameEnabled(isChecked));
    }

    private void onStartCameraSource(CameraSource cameraSource) throws IOException {
        mViews.preview.start(cameraSource, mViews.overlay);
    }

    private void setFrameEnabled(boolean enabled) {
        if (enabled) {
            mViews.overlay.add(mFrameGraphic);
        } else {
            mViews.overlay.remove(mFrameGraphic);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraSourceHelper.start();
    }

    @Override
    protected void onPause() {
        mViews.preview.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCameraSourceHelper.release();
        super.onDestroy();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        boolean handled = mCameraSourceHelper.onRequestPermissionsResult(requestCode, grantResults);

        if (!handled) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    private static class GraphicFaceTracker extends Tracker<Face> {
        private final GraphicOverlay mOverlay;
        private final FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(mOverlay);
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceGraphic.setId(faceId);
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(Detections<Face> detectionResults) {
            mOverlay.remove(mFaceGraphic);
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceGraphic);
        }
    }

    private static class ViewHolder {
        final CameraSourcePreview preview;
        final GraphicOverlay overlay;

        final View buttonFlip;
        final Switch switchFrame;

        ViewHolder(Activity activity) {
            preview = activity.findViewById(R.id.preview);
            overlay = activity.findViewById(R.id.face_overlay);
            buttonFlip = activity.findViewById(R.id.button_flip);
            switchFrame = activity.findViewById(R.id.switch_frame);
        }
    }
}
