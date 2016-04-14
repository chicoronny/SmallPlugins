package fiji.plugins.small;

/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.util.ThreadUtil;

/**
 * ProcessPixels
 *
 * A template for processing each pixel of either
 * GRAY8, GRAY16, GRAY32 or COLOR_RGB images.
 *
 * @author The Fiji Team
 */
public class RemoveHistGaps implements PlugInFilter {
	protected ImagePlus image;

	// image property members
	private int width;
	private int height;

	// plugin parameters
	public double value;
	public String name;

	/**
	 * @see ij.plugin.filter.PlugInFilter#setup(java.lang.String, ij.ImagePlus)
	 */
	@Override
	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about")) {
			showAbout();
			return DONE;
		}

		image = imp;
		return DOES_8G | DOES_16 | DOES_32 ;
	}

	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		// get width and height
		ImageStack is = image.getImageStack();
		width = is.getWidth();
		height = is.getHeight();

		process(is);
		image.updateAndDraw();
		
	}


	/**
	 * Process an image.
	 *
	 * Please provide this method even if {@link ij.plugin.filter.PlugInFilter} does require it;
	 * the method {@link ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)} can only
	 * handle 2-dimensional data.
	 *
	 * If your plugin does not change the pixels in-place, make this method return the results and
	 * change the {@link #setup(java.lang.String, ij.ImagePlus)} method to return also the
	 * <i>DOES_NOTHING</i> flag.
	 *
	 * @param is the image (possible multi-dimensional)
	 */
	public void process(final ImageStack is) {
		// slice numbers start with 1 for historical reasons
		final int size = is.getSize();
		final AtomicInteger ai = new AtomicInteger(1);
		final Thread[] threads = ThreadUtil.createThreadArray();
		
		 for (int ithread = 0; ithread < threads.length; ithread++) { 
			 threads[ithread] = new Thread() {  
				 public void run() { 
					 for (int i = ai.getAndIncrement(); i <= size; i = ai.getAndIncrement()) {
						 process(is.getProcessor(i));
						 IJ.showProgress(i, size);
						 
					 }
				 }
			 };
		 }
		 ThreadUtil.startAndJoin(threads);		
	}

	// Select processing method depending on image type
	public void process(ImageProcessor ip) {
		int type = image.getType();
		if (type == ImagePlus.GRAY8)
			process8( ip );
		else if (type == ImagePlus.GRAY16)
			process16( ip );
		else if (type == ImagePlus.GRAY32)
			process32( ip );
		else {
			throw new RuntimeException("not supported");
		}
	}

	// processing of GRAY8 images
	public void process8(ImageProcessor ip) {
		int[] hist = ip.getHistogram();
		byte index = 0;
		Map<Byte,Byte> histMap= new HashMap<>();
		for (int i=0; i<hist.length; i++)
			if (hist[i] > 0) 
				histMap.put((byte) i, index++);
		
		byte[] pixels = (byte[]) ip.getPixels();
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				pixels[x + y * width] = histMap.get(pixels[x + y * width]);
			}
		}
	}

	// processing of GRAY16 images
	public void process16(ImageProcessor ip) {
		int[] hist = ip.getHistogram();
		short index = 0;
		Map<Short,Short> histMap= new HashMap<>();
		for (int i=0; i<hist.length; i++)
			if (hist[i] > 0) 
				histMap.put((short) i, index++);
		
		short[] pixels = (short[]) ip.getPixels();
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				pixels[x + y * width] = histMap.get(pixels[x + y * width]);
			}
		}
	}

	// processing of GRAY32 images
	public void process32(ImageProcessor ip) {
		int[] hist = ip.getHistogram();
		float index = 0f;
		Map<Float,Float> histMap= new HashMap<>();
		for (int i=0; i<hist.length; i++)
			if (hist[i] > 0) 
				histMap.put((float) i, index++);
		
		float[] pixels = (float[]) ip.getPixels();
		
		for (int y=0; y < height; y++) {
			for (int x=0; x < width; x++) {
				// process each pixel of the line
				pixels[x + y * width] = histMap.get(pixels[x + y * width]);
			}
		}
	}


	public void showAbout() {
		IJ.showMessage("Remove Gaps in Histogram",
			"Remove Gaps in Histogram"
		);
	}

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = RemoveHistGaps.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = new ImagePlus("/media/data/users/Christos/test.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
