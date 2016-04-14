package fiji.plugins.small;

/* To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.atomic.AtomicInteger;

import features.TubenessProcessor;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Macro;
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
public class TubenessStack implements PlugInFilter {
	protected ImagePlus image;

	// plug-in parameters
	public double value;
	public String name;

	protected double sigma = 3.0;

	@Override
	public int setup(String arg, ImagePlus imp) {

		image = imp;
		return DOES_8G | DOES_16 | DOES_32;
	}


	/**
	 * @see ij.plugin.filter.PlugInFilter#run(ij.process.ImageProcessor)
	 */
	@Override
	public void run(ImageProcessor ip) {
		String options = Macro.getOptions();
		if (options!=null)
			sigma = Double.parseDouble(Macro.getValue(options, "sigma", "3.0"));
		process(image.getImageStack());
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
	Thread[] threads = ThreadUtil.createThreadArray(Runtime.getRuntime().availableProcessors());
	final AtomicInteger ai = new AtomicInteger(1);
	final ImageProcessor[] stack2 = new ImageProcessor[size];
	
	for (int ithread = 0; ithread < threads.length; ithread++) {

		threads[ithread] = new Thread("fit_" + ithread) {
			@Override
			public void run() {
				 for ( int i=ai.getAndIncrement(); i <= size; i=ai.getAndIncrement()) {
					 ImageProcessor ip = is.getProcessor(i);
					 TubenessProcessor tp = new TubenessProcessor(sigma,false);
					 ImagePlus result = tp.generateImage(new ImagePlus("original",ip));
					 ImageProcessor resultProcessor = result.getProcessor();
					 stack2[i-1]=resultProcessor;
				 }
			}
		};
	}
	ThreadUtil.startAndJoin(threads);
	 
	 image.close();
	 ImageStack is2 = new ImageStack(is.getWidth(),is.getHeight());
	 for (int i=0; i<stack2.length; i++)
		 if (stack2[i] != null)
			 is2.addSlice(""+i,stack2[i]);
	 ImagePlus result = new ImagePlus("result", is2);
	
	 for (int i = 1; i <= size; i++) {
		 is.deleteLastSlice();
	 }
	 result.show();
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
		Class<?> clazz = TubenessStack.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();

		// open the Clown sample
		ImagePlus image = new ImagePlus("/media/data/users/Christos/test.tif");
		image.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "sigma=3.0");
	}

}
