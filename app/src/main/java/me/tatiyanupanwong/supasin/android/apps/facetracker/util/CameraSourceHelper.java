package me.tatiyanupanwong.supasin.android.apps.facetracker.util;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;

import me.tatiyanupanwong.supasin.android.apps.facetracker.R;

import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.support.design.widget.Snackbar.LENGTH_INDEFINITE;
import static com.google.android.gms.vision.CameraSource.CAMERA_FACING_BACK;
import static com.google.android.gms.vision.CameraSource.CAMERA_FACING_FRONT;

public final class CameraSourceHelper {
    private static final String TAG = CameraSourceHelper.class.getSimpleName();

    private static final int REQUEST_GMS = 9001;
    private static final int REQUEST_CAMERA_PERMISSION = 2;

    private final Activity mActivity;
    private final Tracker<Face> mFaceTracker;
    private final Callback mCallback;

    private CameraSource mCameraSource;

    public CameraSourceHelper(Activity activity, Tracker<Face> faceTracker, Callback callback) {
        mActivity = activity;
        mFaceTracker = faceTracker;
        mCallback = callback;
    }

    public void start() {
        if (ActivityCompat.checkSelfPermission(mActivity, CAMERA) == PERMISSION_GRANTED) {
            createCameraSource(CAMERA_FACING_BACK);
        } else {
            requestCameraPermission();
        }

        startCameraSource();
    }

    public void release() {
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    public void flipCamera(OnCameraFlippedListener listener) {
        if (mCameraSource != null) {
            if (mCameraSource.getCameraFacing() == CAMERA_FACING_FRONT) {
                mCameraSource.release();
                createCameraSource(CAMERA_FACING_BACK);
            } else {
                mCameraSource.release();
                createCameraSource(CAMERA_FACING_FRONT);
            }

            startCameraSource();
            if (listener != null) {
                listener.onCameraFlipped();
            }
        }
    }


    public boolean onRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CAMERA_PERMISSION) {
            return false;
        }

        if (grantResults.length != 0 && grantResults[0] == PERMISSION_GRANTED) {
            createCameraSource(CAMERA_FACING_BACK);
            return true;
        }

        requestCameraPermission();
        return true;
    }


    private void requestCameraPermission() {
        final String[] permissions = new String[] { CAMERA };

        if (!ActivityCompat.shouldShowRequestPermissionRationale(mActivity, CAMERA)) {
            ActivityCompat.requestPermissions(mActivity, permissions, REQUEST_CAMERA_PERMISSION);
            return;
        }

        final View contentView = mActivity.findViewById(android.R.id.content);
        Snackbar.make(contentView, R.string.permission_camera_rationale, LENGTH_INDEFINITE)
                .setAction(android.R.string.ok,
                        view -> ActivityCompat.requestPermissions(mActivity, permissions,
                                REQUEST_CAMERA_PERMISSION))
                .show();
    }

    private void createCameraSource(int facing) {
        FaceDetector detector = new FaceDetector.Builder(mActivity)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .build();

        detector.setProcessor(new LargestFaceFocusingProcessor(detector, mFaceTracker));

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

        mCameraSource = new CameraSource.Builder(mActivity, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(facing)
                .setRequestedFps(30.0f)
                .build();
    }

    private void startCameraSource() {
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mActivity);
        if (code != ConnectionResult.SUCCESS) {
            GoogleApiAvailability.getInstance().getErrorDialog(mActivity, code, REQUEST_GMS).show();
        }

        if (mCameraSource != null) {
            try {
                mCallback.onStartCameraSource(mCameraSource);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    public interface Callback {
        void onStartCameraSource(CameraSource cameraSource) throws IOException;
    }

    public interface OnCameraFlippedListener {
        void onCameraFlipped();
    }
}
