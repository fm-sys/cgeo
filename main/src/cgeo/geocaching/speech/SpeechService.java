package cgeo.geocaching.speech;

import cgeo.geocaching.Intents;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.sensors.GeoData;

import android.content.Intent;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.StringUtils;

/**
 * Service to speak the compass directions.
 *
 */
public class SpeechService extends AbstractGeoSpeechService {

    private static final int SPEECH_MINPAUSE_SECONDS = 5;
    private static final int SPEECH_MAXPAUSE_SECONDS = 30;

    protected float direction;
    protected Geopoint position;

    /**
     * remember when we talked the last time
     */
    private long lastSpeechTime = 0;
    private float lastSpeechDistance = 0.0f;
    private Geopoint target;

    @Override
    public void updateGeoDir(@NonNull final GeoData newGeo, final float newDirection) {
        // We might receive a location update before the target has been set. In this case, do nothing.
        if (target == null) {
            return;
        }

        position = newGeo.getCoords();
        direction = newDirection;
        // avoid any calculation, if the delay since the last output is not long enough
        final long now = System.currentTimeMillis();
        if (now - lastSpeechTime <= SPEECH_MINPAUSE_SECONDS * 1000) {
            return;
        }

        // to speak, we want max pause to have elapsed or distance to geopoint to have changed by a given amount
        final float distance = position.distanceTo(target);
        if (now - lastSpeechTime <= SPEECH_MAXPAUSE_SECONDS * 1000 && Math.abs(lastSpeechDistance - distance) < getDeltaForDistance(distance)) {
            return;
        }

        final String text = TextFactory.getText(position, target, direction);
        if (StringUtils.isNotEmpty(text)) {
            lastSpeechTime = System.currentTimeMillis();
            lastSpeechDistance = distance;
            speak(text);
        }
    }

    /**
     * Return distance required to be moved based on overall distance.<br>
     *
     * @param distance
     *            in km
     * @return delta in km
     */
    private static float getDeltaForDistance(final float distance) {
        if (distance > 1.0) {
            return 0.2f;
        }
        if (distance > 0.05) {
            return distance / 5.0f;
        }
        return 0f;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        if (intent != null) {
            target = intent.getParcelableExtra(Intents.EXTRA_COORDS);
        }
        return START_NOT_STICKY; // service can be stopped by system, if under memory pressure
    }
}
