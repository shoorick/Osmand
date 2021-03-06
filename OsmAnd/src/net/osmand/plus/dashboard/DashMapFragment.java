package net.osmand.plus.dashboard;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.*;
import android.widget.ImageView;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.MapTileDownloader.DownloadRequest;
import net.osmand.map.MapTileDownloader.IMapDownloaderCallback;
import net.osmand.plus.R;
import net.osmand.plus.activities.MainMenuActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.render.MapVectorLayer;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.MapTextLayer;
import net.osmand.plus.views.OsmAndMapSurfaceView;
import net.osmand.plus.views.OsmandMapTileView;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by Denis on 24.11.2014.
 */
public class DashMapFragment extends DashBaseFragment implements IMapDownloaderCallback {

	public static final String TAG = "DASH_MAP_FRAGMENT";

	@Override
	public void onDestroy() {
		super.onDestroy();
		getMyApplication().getResourceManager().getMapTileDownloader().removeDownloaderCallback(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getMyApplication().getResourceManager().getMapTileDownloader().addDownloaderCallback(this);
	}

	protected void startMapActivity() {
		MapActivity.launchMapActivityMoveToTop(getActivity());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View view = getActivity().getLayoutInflater().inflate(R.layout.dash_map_fragment, container, false);
		Typeface typeface = FontCache.getRobotoMedium(getActivity());
		((TextView) view.findViewById(R.id.map_text)).setTypeface(typeface);
		((Button) view.findViewById(R.id.show_map)).setTypeface(typeface);

		(view.findViewById(R.id.show_map)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startMapActivity();
			}


		});
		view.findViewById(R.id.map_image).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startMapActivity();
			}
		});

		return view;
	}

	private void setMapImage(View view) {
		if (view == null) {
			return;
		}
		Bitmap image = getMyApplication().getResourceManager().getRenderer().getBitmap();
		ImageView map = (ImageView) view.findViewById(R.id.map_image);
		if (image != null) {
			map.setImageBitmap(image);
		}
	}


	@Override
	public void onResume() {
		super.onResume();
		if (!getMyApplication().isApplicationInitializing()) {
			updateMapImage();
		}
	}

	@Override
	public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.map_image).setVisibility(View.GONE);
		if (getMyApplication().isApplicationInitializing()) {
			getMyApplication().checkApplicationIsBeingInitialized(getActivity(), (TextView) view.findViewById(R.id.ProgressMessage),
					(ProgressBar) view.findViewById(R.id.ProgressBar), new Runnable() {
						@Override
						public void run() {
							applicationInitialized(view);
						}
					});
		} else {
			applicationInitialized(view);
		}
	}

	private void applicationInitialized(View view) {
		updateMapImage();
		view.findViewById(R.id.loading).setVisibility(View.GONE);
		MainMenuActivity dashboardActivity = ((MainMenuActivity) getSherlockActivity());
		if (dashboardActivity != null) {
			dashboardActivity.updateDownloads();
			view.findViewById(R.id.map_image).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void tileDownloaded(DownloadRequest request) {
		if (request != null && !request.error && request.fileToSave != null) {
			ResourceManager mgr = getMyApplication().getResourceManager();
			mgr.tileDownloaded(request);
		}
		setMapImage(getView());
	}

	private void updateMapImage() {
		MapRenderRepositories repositories = getMyApplication().getResourceManager().getRenderer();
		LatLon lm = getMyApplication().getSettings().getLastKnownMapLocation();
		int zm = getMyApplication().getSettings().getLastKnownMapZoom();

		WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int height = (int) getActivity().getResources().getDimension(R.dimen.dashMapHeight);
		int width = display.getWidth();

		RotatedTileBox rotatedTileBox = new RotatedTileBox.RotatedTileBoxBuilder().
				setZoom(zm).setLocation(lm.getLatitude(), lm.getLongitude()).
				setPixelDimensions(width, height).build();
		repositories.loadMap(rotatedTileBox,
				getMyApplication().getResourceManager().getMapTileDownloader().getDownloaderCallbacks());
	}
}
