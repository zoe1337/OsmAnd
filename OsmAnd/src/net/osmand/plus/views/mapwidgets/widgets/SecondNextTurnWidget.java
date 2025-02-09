package net.osmand.plus.views.mapwidgets.widgets;

import android.view.View;
import android.view.View.OnClickListener;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.routing.RouteCalculationResult.NextDirectionInfo;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.router.TurnType;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SecondNextTurnWidget extends NextTurnBaseWidget {

	private final NextDirectionInfo nextDirectionInfo = new NextDirectionInfo();

	public SecondNextTurnWidget(@NonNull MapActivity mapActivity) {
		super(mapActivity, true);
		setOnClickListener(getOnClickListener());
	}

	/**
	 * Do not delete to have pressed state. Uncomment to test rendering
	 */
	@NonNull
	private OnClickListener getOnClickListener() {
		return new View.OnClickListener() {
//			int i = 0;
			@Override
			public void onClick(View v) {
//				final int l = TurnType.predefinedTypes.length;
//				final int exits = 5;
//				i++;
//				if (i % (l + exits) >= l ) {
//					nextTurnInfo.turnType = TurnType.valueOf("EXIT" + (i % (l + exits) - l + 1), true);
//					nextTurnInfo.exitOut = (i % (l + exits) - l + 1)+"";
//					float a = 180 - (i % (l + exits) - l + 1) * 50;
//					nextTurnInfo.turnType.setTurnAngle(a < 0 ? a + 360 : a);
//				} else {
//					nextTurnInfo.turnType = TurnType.valueOf(TurnType.predefinedTypes[i % (TurnType.predefinedTypes.length + exits)], true);
//					nextTurnInfo.exitOut = "";
//				}
//				nextTurnInfo.turnImminent = (nextTurnInfo.turnImminent + 1) % 3;
//				nextTurnInfo.nextTurnDirection = 580;
//				TurnPathHelper.calcTurnPath(nextTurnInfo.pathForTurn, nexsweepAngletTurnInfo.turnType,nextTurnInfo.pathTransform);
//				showMiniMap = true;
			}
		};
	}

	@Override
	public void updateInfo(@Nullable DrawSettings drawSettings) {
		boolean followingMode = routingHelper.isFollowingMode() || locationProvider.getLocationSimulation().isRouteAnimating();
		TurnType turnType = null;
		boolean deviatedFromRoute = false;
		int turnImminent = 0;
		int nextTurnDistance = 0;
		if (routingHelper.isRouteCalculated() && followingMode) {
			deviatedFromRoute = routingHelper.isDeviatedFromRoute();
			NextDirectionInfo r = routingHelper.getNextRouteDirectionInfo(nextDirectionInfo, true);
			if (!deviatedFromRoute) {
				if (r != null) {
					r = routingHelper.getNextRouteDirectionInfoAfter(r, nextDirectionInfo, true);
				}
			}
			if (r != null && r.distanceTo > 0 && r.directionInfo != null) {
				turnType = r.directionInfo.getTurnType();
				turnImminent = r.imminent;
				nextTurnDistance = r.distanceTo;
			}
		}
		setTurnType(turnType);
		setTurnImminent(turnImminent, deviatedFromRoute);
		setTurnDistance(nextTurnDistance);
	}
}