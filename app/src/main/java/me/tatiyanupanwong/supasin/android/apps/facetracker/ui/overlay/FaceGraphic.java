package me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.google.android.gms.vision.face.Face;

import java.util.Locale;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final int[] COLOR_CHOICES = {
            Color.BLUE,
            Color.CYAN,
            Color.GREEN,
            Color.MAGENTA,
            Color.RED,
            Color.WHITE,
            Color.YELLOW
    };
    private static int sCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;

    public FaceGraphic(GraphicOverlay overlay) {
        super(overlay);

        sCurrentColorIndex = (sCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[sCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);
    }

    public void setId(int id) {
        mFaceId = id;
    }


    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    public void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (mFace == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float coordinateX = translateX(mFace.getPosition().x + mFace.getWidth() / 2);
        float coordinateY = translateY(mFace.getPosition().y + mFace.getHeight() / 2);
        canvas.drawCircle(coordinateX, coordinateY, FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawText("id: " + mFaceId,
                coordinateX + ID_X_OFFSET, coordinateY + ID_Y_OFFSET, mIdPaint);
        canvas.drawText("happiness: "
                        + String.format(Locale.US, "%.2f", mFace.getIsSmilingProbability()),
                coordinateX - ID_X_OFFSET, coordinateY - ID_Y_OFFSET, mIdPaint);
        canvas.drawText("right eye: "
                        + String.format(Locale.US, "%.2f", mFace.getIsRightEyeOpenProbability()),
                coordinateX + ID_X_OFFSET * 2, coordinateY + ID_Y_OFFSET * 2, mIdPaint);
        canvas.drawText("left eye: "
                        + String.format(Locale.US, "%.2f", mFace.getIsLeftEyeOpenProbability()),
                coordinateX - ID_X_OFFSET * 2, coordinateY - ID_Y_OFFSET * 2, mIdPaint);

        // Draws a bounding box around the face.
        float offsetX = scaleX(mFace.getWidth() / 2.0f);
        float offsetY = scaleY(mFace.getHeight() / 2.0f);
        float left = coordinateX - offsetX;
        float top = coordinateY - offsetY;
        float right = coordinateX + offsetX;
        float bottom = coordinateY + offsetY;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);
    }
}
