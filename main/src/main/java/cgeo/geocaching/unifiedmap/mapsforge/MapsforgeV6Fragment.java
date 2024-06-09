package cgeo.geocaching.unifiedmap.mapsforge;

import cgeo.geocaching.CgeoApplication;
import cgeo.geocaching.R;
import cgeo.geocaching.location.Geopoint;
import cgeo.geocaching.location.Viewport;
import cgeo.geocaching.settings.Settings;
import cgeo.geocaching.ui.ViewUtils;
import cgeo.geocaching.ui.dialog.Dialogs;
import cgeo.geocaching.unifiedmap.AbstractMapFragment;
import cgeo.geocaching.unifiedmap.geoitemlayer.IProviderGeoItemLayer;
import cgeo.geocaching.unifiedmap.geoitemlayer.MapsforgeV6GeoItemLayer;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractMapsforgeV6TileProvider;
import cgeo.geocaching.unifiedmap.tileproviders.AbstractTileProvider;
import cgeo.geocaching.utils.AngleUtils;
import cgeo.geocaching.utils.ImageUtils;
import static cgeo.geocaching.storage.extension.OneTimeDialogs.DialogType.MAP_AUTOROTATION_DISABLE;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.model.common.Observer;
import org.mapsforge.map.view.InputListener;
import org.oscim.core.BoundingBox;


public class MapsforgeV6Fragment extends AbstractMapFragment implements Observer {

    private MapView mMapView;
//    private Map mMap;
//    private GroupedList<Layer> mMapLayers;
//    protected TileLayer baseMap;
    protected final ArrayList<Layer> layers = new ArrayList<>();
    protected MapsforgeThemeHelper themeHelper;
//    protected Map.UpdateListener mapUpdateListener;
    private View mapAttribution;
    private boolean doReapplyTheme = false;

    private final Bitmap rotationIndicator = ImageUtils.convertToBitmap(ResourcesCompat.getDrawable(CgeoApplication.getInstance().getResources(), R.drawable.bearing_indicator, null));
    private final int rotationWidth = rotationIndicator.getWidth();
    private final int rotationHeight = rotationIndicator.getHeight();

    public MapsforgeV6Fragment() {
        super(R.layout.unifiedmap_mapsforge_fragment);
    }

    @Override
    public void onViewCreated(final @NonNull View view, final @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mMapView = requireView().findViewById(R.id.mapViewMapsforge);
//        mMap = mMapView.getMap();
//        mMapLayers = new GroupedList<>(mMapView.getLayerManager().getLayers().getLayers(), 4);
        setMapRotation(Settings.getMapRotation());
        mapAttribution = requireView().findViewById(R.id.map_attribution);
        themeHelper = new MapsforgeThemeHelper(requireActivity());
        if (position != null) {
            setCenter(position);
        }
        setZoom(zoomLevel);

        mMapView.addInputListener(new InputListener() {
            @Override
            public void onMoveEvent() {
                if (Boolean.TRUE.equals(viewModel.followMyLocation.getValue())) {
                    viewModel.followMyLocation.setValue(false);
                }
                final LatLong center = mMapView.getModel().mapViewPosition.getCenter();
                viewModel.mapCenter.setValue(new Geopoint(center.getLatitude(), center.getLongitude()));
            }

            @Override
            public void onZoomEvent() {

            }
        });

        if (onMapReadyTasks != null) {
            onMapReadyTasks.run();
        }
    }

    @Override
    public void onChange() {
        /// todo: can this be used as observer?

        //        mapUpdateListener = (event, mapPosition) -> {
//            if (event == Map.ROTATE_EVENT || event == Map.POSITION_EVENT) {
//                repaintRotationIndicator(mapPosition.bearing);
//            }
//        };
        repaintRotationIndicator(mMapView.getMapRotation().degrees);
    }

    private void startMap() {
        mMapView.getMapScaleBar().setVisible(true);
        mMapView.getMapScaleBar().setDistanceUnitAdapter(mMapView.getMapScaleBar().getDistanceUnitAdapter());
        mMapView.setBuiltInZoomControls(false);

        //make room for map attribution icon button
        final int mapAttPx = Math.round(this.getResources().getDisplayMetrics().density * 30);
        mMapView.getMapScaleBar().setMarginHorizontal(mapAttPx);

        if (this.mapAttribution != null) {
            this.mapAttribution.setOnClickListener(v -> displayMapAttribution());
        }
    }

    private void displayMapAttribution() {
        final Pair<String, Boolean> mapAttribution = currentTileProvider.getMapAttribution();
        if (mapAttribution == null || StringUtils.isBlank(mapAttribution.first)) {
            return;
        }

        //create text message
        CharSequence message = HtmlCompat.fromHtml(mapAttribution.first, HtmlCompat.FROM_HTML_MODE_LEGACY);
        if (mapAttribution.second) {
            final SpannableString s = new SpannableString(message);
            ViewUtils.safeAddLinks(s, Linkify.ALL);
            message = s;
        }

        final AlertDialog alertDialog = Dialogs.newBuilder(getContext())
                .setTitle(getContext().getString(R.string.map_source_attribution_dialog_title))
                .setCancelable(true)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, pos) -> dialog.dismiss())
                .create();
        alertDialog.show();

        // Make the URLs in TextView clickable. Must be called after show()
        // Note: we do NOT use the "setView()" option of AlertDialog because this screws up the layout
        ((TextView) alertDialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    protected void repaintRotationIndicator(final float bearing) {
        final View currentView = getView();
        if (currentView == null) {
            return;
        }

        final ImageView compassrose = currentView.findViewById(R.id.bearingIndicator);
        if (bearing == 0.0f) {
            compassrose.setImageBitmap(null);
        } else {
            adaptLayoutForActionBar(null);

            final Matrix matrix = new Matrix();
            matrix.setRotate(bearing, rotationWidth / 2.0f, rotationHeight / 2.0f);
            compassrose.setImageBitmap(Bitmap.createBitmap(rotationIndicator, 0, 0, rotationWidth, rotationHeight, matrix, true));
            compassrose.setOnClickListener(v -> {
                final boolean isRotated = getCurrentBearing() != 0f;
                setBearing(0.0f);
                repaintRotationIndicator(0.0f);
                if (isRotated && (Settings.getMapRotation() == Settings.MAPROTATION_AUTO_LOWPOWER || Settings.getMapRotation() == Settings.MAPROTATION_AUTO_PRECISE)) {
                    Dialogs.advancedOneTimeMessage(getContext(), MAP_AUTOROTATION_DISABLE, getString(MAP_AUTOROTATION_DISABLE.messageTitle), getString(MAP_AUTOROTATION_DISABLE.messageText), "", true, null, () -> Settings.setMapRotation(Settings.MAPROTATION_MANUAL));
                }
            });
        }

    }


    // ========================================================================
    // lifecycle methods

    @Override
    public void onStart() {
        super.onStart();
        initLayers();
        if (doReapplyTheme) {
            applyTheme(); // @todo: There must be a less resource-intensive way of applying style-changes...
            doReapplyTheme = false;
        }
//        mMapLayers.add(new MapEventsReceiver(mMap));

        mMapView.getModel().mapViewPosition.addObserver(this);

    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

        mMapView.getModel().mapViewPosition.removeObserver(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }


    // ========================================================================
    // tilesource handling

    @Override
    public boolean supportsTileSource(final AbstractTileProvider newSource) {
        return newSource instanceof AbstractMapsforgeV6TileProvider;
    }

    @Override
    public void prepareForTileSourceChange() {
        // remove layers from currently displayed Mapsforge map
//        removeBaseMap();
//        synchronized (layers) {
//            for (Layer layer : layers) {
//                //layer.setEnabled(false);
//                try {
//                    mMapLayers.remove(layer);
//                } catch (IndexOutOfBoundsException ignore) {
//                    // ignored
//                }
//            }
//            layers.clear();
//        }
//        mMap.clearMap();
        super.prepareForTileSourceChange();
    }

    @Override
    public boolean setTileSource(final AbstractTileProvider newSource, final boolean force) {
        final boolean needsUpdate = super.setTileSource(newSource, force);
        if (needsUpdate) {
            ((AbstractMapsforgeV6TileProvider) currentTileProvider).addTileLayer(this, mMapView);
            startMap();
        }
        return needsUpdate;
    }


    // ========================================================================
    // layer handling

    @Override
    public IProviderGeoItemLayer<?> createGeoItemProviderLayer() {
        return new MapsforgeV6GeoItemLayer(mMapView.getLayerManager(), mMapView.getMapViewProjection());
    }

//    /**
//     * call this instead of VTM.setBaseMap so that we can keep track of baseMap set by tile provider
//     */
//    public synchronized TileLayer setBaseMap(final TileSource tileSource) {
//        removeBaseMap();
//
//        final VectorTileLayer l = new OsmTileLayer(mMap);
//        l.setTileSource(tileSource);
//        addLayerToGroup(l, LayerHelper.ZINDEX_BASEMAP);
//        baseMap = l;
//        return baseMap;
//    }
//
//    private void removeBaseMap() {
//        if (baseMap != null) {
//            mMapLayers.removeGroup(LayerHelper.ZINDEX_BASEMAP);
//        }
//        baseMap = null;
//    }
//
//    /**
//     * call this instead of VTM.layers().add so that we can keep track of layers added by the tile provider
//     */
//    public synchronized void addLayer(final int index, final Layer layer) {
//        layers.add(layer);
//        mMapLayers.addToGroup(layer, index);
//    }
//
//    public synchronized void addLayerToGroup(final Layer layer, final int groupIndex) {
//        mMapLayers.addToGroup(layer, groupIndex);
//    }
//
//    public synchronized void clearGroup(final int groupIndex) {
//        mMapLayers.removeGroup(groupIndex);
//    }


    // ========================================================================
    // position related methods

    @Override
    public void setCenter(final Geopoint geopoint) {
        mMapView.setCenter(new LatLong(geopoint.getLatitude(), geopoint.getLongitude()));
    }

    @Override
    public Geopoint getCenter() {
        final LatLong pos = mMapView.getModel().mapViewPosition.getMapPosition().latLong;
        return new Geopoint(pos.getLatitude(), pos.getLongitude());
    }

    @Override
    @NonNull
    public BoundingBox getBoundingBox() {
        if (mMapView == null) {
            return new BoundingBox(0, 0, 0, 0);
        }
        final org.mapsforge.core.model.BoundingBox bb = mMapView.getBoundingBox();
        return new BoundingBox(bb.minLatitude, bb.minLongitude, bb.maxLatitude, bb.minLongitude);
    }

    @Override
    public void setDrivingMode(final boolean enabled) {
        mMapView.setMapViewCenterY(enabled ? 0.75f : 0.5f);
    }


    // ========================================================================
    // zoom, bearing & heading methods

    @Override
    public void zoomToBounds(final Viewport bounds) {
        zoomToBounds(new BoundingBox(bounds.bottomLeft.getLatitudeE6(), bounds.bottomLeft.getLongitudeE6(), bounds.topRight.getLatitudeE6(), bounds.topRight.getLongitudeE6()));
    }

    public void zoomToBounds(final BoundingBox bounds) {
        if (bounds.getLatitudeSpan() == 0 && bounds.getLongitudeSpan() == 0) {
            setCenter(new Geopoint(bounds.getCenterPoint().getLatitude(), bounds.getCenterPoint().getLongitude()));
        } else {
            // add some margin to not cut-off items at the edge
            // Google Maps does this implicitly, so we need to add it here map-specific
            final BoundingBox extendedBounds = bounds.extendMargin(1.1f);
            if (mMapView.getWidth() == 0 || mMapView.getHeight() == 0) {
                //See Bug #14948: w/o map width/height the bounds can't be calculated
                // -> postpone animation to later on UI thread (where map width/height will be set)
                mMapView.post(() -> zoomToBoundsDirect(extendedBounds));
            } else {
                zoomToBoundsDirect(extendedBounds);
            }
        }
    }

    private void zoomToBoundsDirect(final BoundingBox bounds) {
        final int tileSize = mMapView.getModel().displayModel.getTileSize();
        final byte newZoom = LatLongUtils.zoomForBounds(new Dimension(mMapView.getWidth(), mMapView.getHeight()),
                new org.mapsforge.core.model.BoundingBox(bounds.getMinLatitude(), bounds.getMinLongitude(), bounds.getMaxLatitude(), bounds.getMaxLongitude()), tileSize);
        mMapView.setZoomLevel(newZoom);
    }

    @Override
    public int getCurrentZoom() {
        return mMapView.getModel().mapViewPosition.getZoomLevel();
    }

    @Override
    public void setZoom(final int zoomLevel) {
        mMapView.setZoomLevel((byte) zoomLevel);
    }

    public void zoomInOut(final boolean zoomIn) {
        if (zoomIn) {
            mMapView.getModel().mapViewPosition.zoomIn();
        } else {
            mMapView.getModel().mapViewPosition.zoomOut();
        }
    }

    @Override
    public void setMapRotation(final int mapRotation) {
        super.setMapRotation(mapRotation);

        //todo: enableRotation(mapRotation == Settings.MAPROTATION_MANUAL);
        repaintRotationIndicator(mMapView.getMapRotation().degrees);
    }

    @Override
    public float getCurrentBearing() {
        return AngleUtils.normalize(360 - mMapView.getMapRotation().degrees); // VTM uses opposite way of calculating bearing compared to GM todo: check classic mapsforge
    }

    @Override
    public void setBearing(final float bearing) {
        final float adjustedBearing = AngleUtils.normalize(360 - bearing); // VTM uses opposite way of calculating bearing compared to GM todo: check classic mapsforge
        mMapView.setRotation(adjustedBearing);
    }


    // ========================================================================
    // theme & language related methods

    @Override
    public void selectTheme(final Activity activity) {
//        themeHelper.selectMapTheme(activity, mMap, currentTileProvider);
    }

    @Override
    public void selectThemeOptions(final Activity activity) {
//        themeHelper.selectMapThemeOptions(activity, currentTileProvider);
        doReapplyTheme = true;
    }

    @Override
    public void applyTheme() {
//        themeHelper.reapplyMapTheme(mMap, currentTileProvider);
    }

    @Override
    public void setPreferredLanguage(final String language) {
//        currentTileProvider.setPreferredLanguage(language);
//        mMap.clearMap();
    }

    // ========================================================================
    // additional menu entries

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == R.id.menu_theme_legend) {
            //RenderThemeLegend.showLegend(requireActivity(), themeHelper);
            return true;
        }
        return false;
    }


    // ========================================================================
    // Tap handling methods

//    class MapEventsReceiver extends Layer implements GestureListener {
//
//        MapEventsReceiver(final Map map) {
//            super(map);
//        }
//
//        @Override
//        public boolean onGesture(final Gesture g, final MotionEvent e) {
//            if (g instanceof Gesture.Tap) {
//                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
//                onTapCallback(p.latitudeE6, p.longitudeE6, (int) e.getX(), (int) e.getY(), false);
//                return true;
//            } else if (g instanceof Gesture.LongPress) {
//                final GeoPoint p = mMap.viewport().fromScreenPoint(e.getX(), e.getY());
//                onTapCallback(p.latitudeE6, p.longitudeE6, (int) e.getX(), (int) e.getY(), true);
//                return true;
//            }
//            return false;
//        }
//    }

    @Override
    public void adaptLayoutForActionBar(@Nullable final Boolean actionBarShowing) {
        adaptLayoutForActionBar(requireView().findViewById(R.id.bearingIndicator), actionBarShowing);
    }
}
