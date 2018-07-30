package me.tatiyanupanwong.supasin.android.apps.facetracker.ui.overlay;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import me.tatiyanupanwong.supasin.android.apps.facetracker.R;

public class FrameGraphic extends GraphicOverlay.Graphic {

    private Drawable mFrame;

    public FrameGraphic(GraphicOverlay overlay) {
        super(overlay);
        mFrame = overlay.getContext().getResources().getDrawable(R.drawable.overlay_frame);
    }

    @Override
    public void draw(Canvas canvas) {
        mFrame.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        mFrame.draw(canvas);
    }
}
