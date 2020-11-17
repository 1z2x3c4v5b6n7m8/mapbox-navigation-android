package com.mapbox.navigation.ui.route;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;
import com.mapbox.turf.TurfConstants;
import com.mapbox.turf.TurfMeasurement;
import com.mapbox.turf.TurfMisc;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MapRouteClickListener implements OnMapClickListener {

  private final MapRouteLine routeLine;
  private OnRouteSelectionChangeListener onRouteSelectionChangeListener;
  private boolean alternativesVisible = true;

  MapRouteClickListener(MapRouteLine routeLine) {
    this.routeLine = routeLine;
  }

  @Override public boolean onMapClick(@NotNull Point point) {
    if (!isRouteVisible()) {
      return false;
    }
    Map<LineString, DirectionsRoute> routeLineStrings = routeLine.retrieveRouteLineStrings();
    if (invalidMapClick(routeLineStrings)) {
      return false;
    }
    List<DirectionsRoute> directionsRoutes = routeLine.retrieveDirectionsRoutes();
    findClickedRoute(point, routeLineStrings, directionsRoutes);
    return false;
  }

  void setOnRouteSelectionChangeListener(OnRouteSelectionChangeListener listener) {
    onRouteSelectionChangeListener = listener;
  }

  public OnRouteSelectionChangeListener getOnRouteSelectionChangeListener() {
    return onRouteSelectionChangeListener;
  }

  void updateAlternativesVisible(boolean alternativesVisible) {
    this.alternativesVisible = alternativesVisible;
  }

  private boolean invalidMapClick(@Nullable Map<LineString, DirectionsRoute> routeLineStrings) {
    return routeLineStrings == null || routeLineStrings.isEmpty() || !alternativesVisible;
  }

  private boolean isRouteVisible() {
    return routeLine.retrieveVisibility();
  }

  private void findClickedRoute(@NonNull Point point, @NonNull Map<LineString, DirectionsRoute> routeLineStrings,
      @NonNull List<DirectionsRoute> directionsRoutes) {

    Map<Double, DirectionsRoute> routeDistancesAwayFromClick = new HashMap<>();
    Point clickPoint = Point.fromLngLat(point.longitude(), point.latitude());

    calculateClickDistances(routeDistancesAwayFromClick, clickPoint, routeLineStrings);
    List<Double> distancesAwayFromClick = new ArrayList<>(routeDistancesAwayFromClick.keySet());
    Collections.sort(distancesAwayFromClick);

    DirectionsRoute clickedRoute = routeDistancesAwayFromClick.get(distancesAwayFromClick.get(0));
    int newPrimaryRouteIndex = directionsRoutes.indexOf(clickedRoute);
    if (clickedRoute != routeLine.getPrimaryRoute() && onRouteSelectionChangeListener != null) {
      routeLine.updatePrimaryRouteIndex(clickedRoute);
      DirectionsRoute selectedRoute = directionsRoutes.get(newPrimaryRouteIndex);
      onRouteSelectionChangeListener.onNewPrimaryRouteSelected(selectedRoute);
    }
  }

  private void calculateClickDistances(
      @NonNull Map<Double, DirectionsRoute> routeDistancesAwayFromClick,
      @NonNull Point clickPoint,
      @NonNull Map<LineString, DirectionsRoute> routeLineStrings) {
    for (LineString lineString : routeLineStrings.keySet()) {
      Point pointOnLine = findPointOnLine(clickPoint, lineString);
      if (pointOnLine == null) {
        return;
      }
      double distance = TurfMeasurement.distance(clickPoint, pointOnLine, TurfConstants.UNIT_METERS);
      routeDistancesAwayFromClick.put(distance, routeLineStrings.get(lineString));
    }
  }

  @Nullable
  private Point findPointOnLine(@NonNull Point clickPoint, @NonNull LineString lineString) {
    List<Point> linePoints = lineString.coordinates();
    Feature feature = TurfMisc.nearestPointOnLine(clickPoint, linePoints);
    return (Point) feature.geometry();
  }
}