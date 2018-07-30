package me.tatiyanupanwong.supasin.android.apps.facetracker;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.camera.CameraSourcePreview;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay.FaceGraphic;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay.FrameGraphic;
import me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay.GraphicOverlay;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.design.widget.Snackbar.LENGTH_INDEFINITE;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_GMS = 9001;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

    private ViewHolder mViews;

    private CameraSource mCameraSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViews = new ViewHolder(this);

        if (ActivityCompat.checkSelfPermission(this, CAMERA) == PERMISSION_GRANTED) {
            createCameraSource(CameraSource.CAMERA_FACING_BACK);
        } else {
            requestCameraPermission();
        }

        mViews.buttonFlipCamera.setOnClickListener(view -> {
            if (mCameraSource != null) {
                if (mCameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_FRONT) {
                    mCameraSource.release();
                    createCameraSource(CameraSource.CAMERA_FACING_BACK);
                } else {
                    mCameraSource.release();
                    createCameraSource(CameraSource.CAMERA_FACING_FRONT);
                }

                startCameraSource();
            }
        });

        final FrameGraphic frameGraphic = new FrameGraphic(mViews.overlays);
        mViews.switchFrame.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mViews.overlays.add(frameGraphic);
            } else {
                mViews.overlays.remove(frameGraphic);
            }
        });
    }

    private void requestCameraPermission() {
        final String[] permissions = new String[] { CAMERA };

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CAMERA_PERMISSION);
            return;
        }

        Snackbar.make(mViews.overlays, R.string.permission_camera_rationale, LENGTH_INDEFINITE)
                .setAction(android.R.string.ok,
                        view -> ActivityCompat.requestPermissions(this, permissions,
                                REQUEST_CAMERA_PERMISSION))
                .show();
    }

    private void createCameraSource(int facing) {
        FaceDetector detector = new FaceDetector.Builder(this)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(TAG, "Face detector dependencies are not yet available.");
        }

        mCameraSource = new CameraSource.Builder(this, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(facing)
                .setRequestedFps(30.0f)
                .build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraSource();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mViews.preview.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(CameraSource.CAMERA_FACING_BACK);
            return;
        }

        requestCameraPermission();
    }


    private void startCameraSource() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(this, code, REQUEST_GMS).show();
        }

        if (mCameraSource != null) {
            try {
                mViews.preview.start(mCameraSource, mViews.overlays);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mViews.overlays);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;
        private FaceGraphic mFaceGraphic;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay);
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
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
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
        final GraphicOverlay overlays;

        final View buttonFlipCamera;

        final Switch switchFrame;

        ViewHolder(Activity activity) {
            preview = activity.findViewById(R.id.preview);
            overlays = activity.findViewById(R.id.face_overlay);
            buttonFlipCamera = activity.findViewById(R.id.button_flip);
            switchFrame = activity.findViewById(R.id.switch_frame);
        }
    }
}
