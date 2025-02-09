package net.osmand.plus.views.mapwidgets.widgets;

import net.osmand.Location;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.utils.OsmAndFormatter;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class AltitudeWidget extends TextInfoWidget {

	private int cachedAltitude = 0;

	public AltitudeWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity);
		setIcons(WidgetParams.ALTITUDE);
		setText(null, null);
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		Location location = locationProvider.getLastKnownLocation();
		if (location != null && location.hasAltitude()) {
			double altitude = location.getAltitude();
			if (isUpdateNeeded() || cachedAltitude != (int) altitude) {
				cachedAltitude = (int) altitude;
				String formattedAltitude = OsmAndFormatter.getFormattedAlt(cachedAltitude, app);
				int index = formattedAltitude.lastIndexOf(' ');
				if (index == -1) {
					setText(formattedAltitude, null);
				} else {
					setText(formattedAltitude.substring(0, index), formattedAltitude.substring(index + 1));
				}
			}
		} else if (cachedAltitude != 0) {
			cachedAltitude = 0;
			setText(null, null);
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}
}