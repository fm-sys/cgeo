package cgeo.geocaching.unifiedmap.tileproviders;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.unifiedmap.mapsforge.MapsforgeV6Fragment;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.download.tilesource.OpenStreetMapMapnik;

public class ExampleMapsforgeV6TileProvider extends AbstractMapsforgeV6TileProvider {

    protected final Uri mapUri;

    public ExampleMapsforgeV6TileProvider() {
        super("MapsforgeV6 Demo", Uri.parse("https://tile.openstreetmap.org"), OpenStreetMapMapnik.INSTANCE.getZoomLevelMin(), OpenStreetMapMapnik.INSTANCE.getZoomLevelMax(), new Pair<>(CgeoApplication.getInstance().getString(R.string.map_attribution_openstreetmap_html), true));
        this.tileProviderName = "MapsforgeV6 Demo";
        this.mapUri = Uri.parse("https://tile.openstreetmap.org");
    }

    @Override
    public void addTileLayer(final MapsforgeV6Fragment fragment, final MapView map) {
        //todo
    }


    protected Uri getMapUri() {
        return mapUri;
    }

    @Override
    @NonNull
    public String getId() {
        return super.getId() + ":" + mapUri.getLastPathSegment();
    }

}
