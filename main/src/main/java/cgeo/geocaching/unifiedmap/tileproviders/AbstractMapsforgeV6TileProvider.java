package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeV6Fragment;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.mapsforge.map.android.view.MapView;

public abstract class AbstractMapsforgeV6TileProvider extends AbstractTileProvider {

    protected final Uri mapUri;

    public AbstractMapsforgeV6TileProvider(final String name, final Uri uri, final int zoomMin, final int zoomMax, final Pair<String, Boolean> mapAttribution) {
        super(zoomMin, zoomMax, mapAttribution);
        this.tileProviderName = name;
        this.mapUri = uri;
    }

    public abstract void addTileLayer(MapsforgeV6Fragment fragment, MapView map);

    @Override
    public AbstractMapFragment createMapFragment() {
        return new MapsforgeV6Fragment();
    }

    @Override
    @NonNull
    public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment();
    }

}
