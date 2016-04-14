package fiji.plugins.small;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.Line;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.plugin.PlugIn;


@SuppressWarnings("deprecation")
public class Quadratic_Curve implements PlugIn, MouseMotionListener {

	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Quadratic_Curve.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);		
		
		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/clown.jpg");
		image.show();

		// run the plugin
		IJ.runPlugIn( clazz.getName(), "");
	}

	private ImagePlus img;
	private ImageCanvas ic;
	private GeneralPath path;

	
	@Override
	public void mouseDragged(MouseEvent e) {
		ic.setDisplayList(mkCurve(), Color.red, new BasicStroke(Line.getWidth()));
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (isNoRoi()) {
			close();
		}
	}

	@Override
	public void run(String arg0) {
		img = IJ.getImage();

		if (isNoRoi()) {
			IJ.showMessage("Needs a polyline ROI with at least two sides,\na polygon ROI or a rectangle ROI.");
			return;
		}

		ic = img.getCanvas();
		ic.addMouseMotionListener(this);
		ic.setDisplayList(mkCurve(), Color.red, new BasicStroke(Line.getWidth()));
	}

	private Shape mkCurve() {
		Roi roi = img.getRoi();
		Polygon poly = roi.getPolygon();
		
		int[] xpoints = poly.xpoints;
		int[] ypoints = poly.ypoints;

		path = new GeneralPath();

		if ( roi.getType() == Roi.POLYGON || roi.getType() == Roi.RECTANGLE) {
			GeneralPath curve  = new GeneralPath();
			int ii = xpoints.length - 2;
			curve.moveTo(xpoints[ii] + ((xpoints[ii+1] - xpoints[ii])/2), ypoints[ii] + ((ypoints[ii+1] - ypoints[ii])/2));
			curve.quadTo(xpoints[ii+1], ypoints[ii+1], xpoints[0] + ((xpoints[ii+1] - xpoints[0])/2), ypoints[0] + ((ypoints[ii+1] - ypoints[0])/2) );
			path.append(curve, true);

			curve  = new GeneralPath();
			curve.moveTo(	xpoints[xpoints.length - 1] + ((xpoints[0] - xpoints[xpoints.length - 1])/2), 
					ypoints[xpoints.length - 1] + ((ypoints[0] - ypoints[xpoints.length - 1])/2));
			curve.quadTo(xpoints[0], ypoints[0], xpoints[1] + ((xpoints[0] - xpoints[1])/2), ypoints[1] + ((ypoints[0] - ypoints[1])/2));
			path.append(curve, true);
		}


		for (int xx = 0; xx < (xpoints.length - 2); xx++) {
			GeneralPath curve  = new GeneralPath();
			curve.moveTo(xpoints[xx] + ((xpoints[xx+1] - xpoints[xx])/2), ypoints[xx] + ((ypoints[xx+1] - ypoints[xx])/2));
			curve.quadTo(xpoints[xx+1], ypoints[xx+1], xpoints[xx+2] + ((xpoints[xx+1] - xpoints[xx+2])/2), ypoints[xx+2] + ((ypoints[xx+1] - ypoints[xx+2])/2));
			path.append(curve, true);
		}


		return path;
	}

	private boolean isNoRoi() {
		Roi roi = img.getRoi();
		if (roi == null) return true;
		if (	(roi.getType() != Roi.POLYLINE) && 
			(roi.getType() != Roi.POLYGON)  &&
			(roi.getType() != Roi.RECTANGLE)) return true;
		if (roi.getPolygon().npoints < 3) return true;

		return false;
	}
	
	private void close() {
		ic.removeMouseMotionListener(this);
		ic.setDisplayList(new GeneralPath(), Color.red, new BasicStroke(0));
		ShapeRoi sroi = new ShapeRoi(path);
		img.setRoi(sroi);
	}

}
