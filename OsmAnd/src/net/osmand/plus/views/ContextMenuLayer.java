package net.osmand.plus.views;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;

import net.osmand.CallbackWithObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.MapContextMenu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ContextMenuLayer extends OsmandMapLayer {
	
	public interface IContextMenuProvider {

		void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> o);
		
		LatLon getObjectLocation(Object o);
		String getObjectDescription(Object o);
		PointDescription getObjectName(Object o);

		boolean disableSingleTap();
		boolean disableLongPressOnMap();
	}
	
	public interface IContextMenuProviderSelection {

		void setSelectedObject(Object o);
		void clearSelectedObject();
	}

	private OsmandMapTileView view;

	private final MapActivity activity;
	private MapContextMenu menu;
	private CallbackWithObject<LatLon> selectOnMap = null;

	private ImageView contextMarker;

	private GestureDetector movementListener;

	public ContextMenuLayer(MapActivity activity){
		this.activity = activity;
		menu = activity.getContextMenu();
		movementListener = new GestureDetector(activity, new MenuLayerOnGestureListener());
	}
	
	@Override
	public void destroyLayer() {
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;

		contextMarker = new ImageView(view.getContext());
		contextMarker.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		contextMarker.setImageDrawable(view.getResources().getDrawable(R.drawable.map_pin_context_menu));
		contextMarker.setClickable(true);
		int minw = contextMarker.getDrawable().getMinimumWidth();
		int minh = contextMarker.getDrawable().getMinimumHeight();
		contextMarker.layout(0, 0, minw, minh);
	}
	

	public boolean isVisible() {
		return menu.isActive();
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox box, DrawSettings nightMode) {
		if(menu.isActive()) {
			LatLon latLon = menu.getLatLon();
			int x = (int) box.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			int y = (int) box.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude());
			canvas.translate(x - contextMarker.getWidth() / 2, y - contextMarker.getHeight());
			contextMarker.draw(canvas);
		}
	}
	
	
	public void setSelectOnMap(CallbackWithObject<LatLon> selectOnMap) {
		this.selectOnMap = selectOnMap;
	}

	private void clearSelectedObjects(Map<Object, IContextMenuProvider> selectedObjects) {
		for(IContextMenuProvider p : selectedObjects.values()) {
			if(p instanceof IContextMenuProviderSelection){
				((IContextMenuProviderSelection) p).clearSelectedObject();
			}
		}
	}

	@Override
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		if (disableLongPressOnMap()) {
			return false;
		}
		showContextMenu(point, tileBox, true);
		view.refreshMap();
		return true;
	}

	public boolean showContextMenu(double latitude, double longitude, boolean showUnknownLocation) {
		RotatedTileBox cp = activity.getMapView().getCurrentRotatedTileBox().copy();
		float x = cp.getPixXFromLatLon(latitude, longitude);
		float y = cp.getPixYFromLatLon(latitude, longitude);
		return showContextMenu(new PointF(x, y), activity.getMapView().getCurrentRotatedTileBox(), showUnknownLocation);
	}

	private boolean showContextMenu(PointF point, RotatedTileBox tileBox, boolean showUnknownLocation) {
		Map<Object, IContextMenuProvider> selectedObjects = selectObjectsForContextMenu(tileBox, point);
		if (selectedObjects.size() == 1) {
			Object selectedObj = selectedObjects.keySet().iterator().next();
			IContextMenuProvider contextObject = selectedObjects.get(selectedObj);
			LatLon latLon = null;
			PointDescription pointDescription = null;
			if (contextObject != null) {
				latLon = contextObject.getObjectLocation(selectedObj);
				pointDescription = contextObject.getObjectName(selectedObj);
			}
			if (latLon == null) {
				latLon = getLatLon(point, tileBox);
			}
			menu.show(latLon, pointDescription, selectedObj);
			return true;

		} else if (selectedObjects.size() > 1) {
			showContextMenuForSelectedObjects(getLatLon(point, tileBox), selectedObjects);
			return true;

		} else if (showUnknownLocation) {
			menu.show(getLatLon(point, tileBox), null, null);
			return true;
		}
		return false;
	}

	@NonNull
	private LatLon getLatLon(PointF point, RotatedTileBox tileBox) {
		LatLon latLon;
		final double lat = tileBox.getLatFromPixel((int) point.x, (int) point.y);
		final double lon = tileBox.getLonFromPixel((int) point.x, (int) point.y);
		latLon = new LatLon(lat, lon);
		return latLon;
	}

	public boolean disableSingleTap() {
		boolean res = false;
		for(OsmandMapLayer lt : view.getLayers()){
			if(lt instanceof ContextMenuLayer.IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableSingleTap()) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	public boolean disableLongPressOnMap() {
		boolean res = false;
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof ContextMenuLayer.IContextMenuProvider) {
				if (((IContextMenuProvider) lt).disableLongPressOnMap()) {
					res = true;
					break;
				}
			}
		}
		return res;
	}

	private Map<Object, IContextMenuProvider> selectObjectsForContextMenu(RotatedTileBox tileBox, PointF point) {
		Map<Object, IContextMenuProvider> selectedObjects = new HashMap<>();
		List<Object> s = new ArrayList<>();
		for (OsmandMapLayer lt : view.getLayers()) {
			if (lt instanceof ContextMenuLayer.IContextMenuProvider) {
				s.clear();
				final IContextMenuProvider l = (ContextMenuLayer.IContextMenuProvider) lt;
				l.collectObjectsFromPoint(point, tileBox, s);
				for (Object o : s) {
					selectedObjects.put(o, l);
					if (l instanceof IContextMenuProviderSelection) {
						((IContextMenuProviderSelection) l).setSelectedObject(o);
					}
				}
			}
		}
		return selectedObjects;
	}

	@Override
	public boolean drawInScreenPixels() {
		return true;
	}

	public boolean pressedContextMarker(RotatedTileBox tb, float px, float py) {
		if (menu.isActive()) {
			LatLon latLon = menu.getLatLon();
			Rect bs = contextMarker.getDrawable().getBounds();
			int dx = (int) (px - tb.getPixXFromLatLon(latLon.getLatitude(), latLon.getLongitude()));
			int dy = (int) (py - tb.getPixYFromLatLon(latLon.getLatitude(), latLon.getLongitude()));
			int bx = dx + bs.width() / 2;
			int by = dy + bs.height();
			return (bs.contains(bx, by));
		}
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		if (pressedContextMarker(tileBox, point.x, point.y)) {
			menu.show();
			return true;
		}

		if(selectOnMap != null) {
			LatLon latlon = tileBox.getLatLonFromPixel(point.x, point.y);
			CallbackWithObject<LatLon> cb = selectOnMap;
			cb.processResult(latlon);
			menu.init(latlon, null, null);
			selectOnMap = null;
			return true;
		}

		if (!disableSingleTap()) {
			boolean res = showContextMenu(point, tileBox, false);
			if (res) {
				return true;
			}
		}

		menu.onSingleTapOnMap();
		return false;
	}

	private void showContextMenuForSelectedObjects(final LatLon latLon, final Map<Object, IContextMenuProvider> selectedObjects) {
		Builder builder = new AlertDialog.Builder(view.getContext());
		String[] d = new String[selectedObjects.size()];
		final List<Object> s = new ArrayList<>();
		int i = 0;
		for (Entry<Object, IContextMenuProvider> e : selectedObjects.entrySet()) {
			d[i++] = e.getValue().getObjectDescription(e.getKey());
			s.add(e.getKey());
		}
		builder.setItems(d, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Object selectedObj = s.get(which);
				IContextMenuProvider contextObject = selectedObjects.get(selectedObj);
				LatLon ll = null;
				PointDescription pointDescription = null;
				if (contextObject != null) {
					ll = contextObject.getObjectLocation(selectedObj);
					pointDescription = contextObject.getObjectName(selectedObj);
				}
				if (ll == null) {
					ll = latLon;
				}
				menu.show(ll, pointDescription, selectedObj);

				selectedObjects.remove(selectedObj);
				clearSelectedObjects(selectedObjects);
			}
		});
		builder.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				clearSelectedObjects(selectedObjects);
			}
		});

		builder.show();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {

		if (movementListener.onTouchEvent(event)) {
			if (menu.isVisible()) {
				menu.hide();
			}
		}

		return false;
	}

	private class MenuLayerOnGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			return true;
		}
	}
}
