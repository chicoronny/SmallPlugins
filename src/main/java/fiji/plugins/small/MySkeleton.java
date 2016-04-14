package fiji.plugins.small;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import fiji.plugins.small.skel.Skeletonize;
import ij.IJ;
import ij.ImagePlus;
import ij.io.DirectoryChooser;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageConverter;
import sc.fiji.analyzeSkeleton.AnalyzeSkeleton_;
//import ij.util.ThreadUtil;

public class MySkeleton implements PlugIn {

	private File folder;

	@Override
	public void run(String arg) {
		
		final DirectoryChooser dc = new DirectoryChooser( "Choose a folder to process" );
		final String dname = dc.getDirectory();
		
		folder = new File( dname );
		
		ImageConverter.setDoScaling(false);
		final Collection<File> fList = Collections.synchronizedCollection(listFiles(folder,new String[]{"tif","TIF"}));
		final Iterator<File> iter =fList.iterator();
		
//		Thread[] threads = ThreadUtil.createThreadArray(Runtime.getRuntime().availableProcessors());
//		for (int ithread = 0; ithread < threads.length; ithread++) {
//
//			threads[ithread] = new Thread("skel_" + ithread) {
//				@Override
//				public void run() {
					
					while(iter.hasNext()) {
						final File file = iter.next();
						ImagePlus image = new ImagePlus(file.getAbsolutePath());
						ImageConverter converter = new ImageConverter(image);
						converter.convertToGray8();
						process(image);
					}
					
//				}
//			};
//		}
//		ThreadUtil.startAndJoin(threads);
		 
		
	}

	private void process(ImagePlus stack) {
		new Skeletonize(stack).run();
		 
		 AnalyzeSkeleton_ skel = new AnalyzeSkeleton_(); 
		 skel.setup("", stack);	
		 sc.fiji.analyzeSkeleton.SkeletonResult skelResult = null;
		 try{
			 skelResult = skel.run(AnalyzeSkeleton_.NONE, false, true, stack, true, false);
		 } catch (Exception e){ IJ.error("error: "+stack.getTitle());return;}
		 IJ.showStatus("Skeleton calculated!");
		 
		 final ResultsTable rt = new ResultsTable();
		 final String[] head = {"Skeleton", "File" ,"# Branches", "Average Branch Length", "Maximum Branch Length",
				 "Longest Shortest Path", "spx", "spy", "spz" , };
		 
		 for(int i = 0 ; i < skelResult.getNumOfTrees(); i++){
			 rt.incrementCounter();
			 rt.addValue(head[1], stack.getShortTitle());
			 rt.addValue(head[2], skelResult.getBranches()[i]);
			 rt.addValue(head[3], skelResult.getAverageBranchLength()[i]);
			 rt.addValue(head[4], skelResult.getMaximumBranchLength()[i]);
			 rt.addValue(head[5], skelResult.getShortestPathList().get(i));
			 rt.addValue(head[6], skelResult.getSpStartPosition()[i][0]);
			 rt.addValue(head[7], skelResult.getSpStartPosition()[i][1]);
			 rt.addValue(head[8], skelResult.getSpStartPosition()[i][2]);
		 }
		 rt.updateResults();
		 rt.show("Results");
		 
		try {
			rt.saveAs(folder+"/"+stack.getTitle()+".csv");
			Thread.sleep(100);
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		rt.reset();
		
		 stack.close();		
	}
	
	public static Collection<File> listFiles(File dir, String[] ext) {
		if (!dir.isDirectory())
		    throw new IllegalArgumentException("Parameter 'dir' is not a directory");
		if (ext == null)
		    throw new NullPointerException("Parameter 'ext' is null");
	
		String[] suffixes = new String[ext.length];
		for (int i = 0; i < ext.length; i++) {
		    suffixes[i] = "." + ext[i];
		}

		class SuffixFileFilter implements FileFilter{
	
		    private String[] suffixes;
	
		    public  SuffixFileFilter(String[] suffixes){
			if (suffixes == null) 
			    throw new IllegalArgumentException("The array of suffixes must not be null");
		        this.suffixes = suffixes;
		    }
		    
		    // accept directories and files with the given suffix(es)
		    @Override
		    public boolean accept(File file) {
			if (file.isDirectory()) return true;
			String name = file.getName();
			for (int i = 0; i < this.suffixes.length; i++) {
			    if (name.endsWith(this.suffixes[i])) 
				return true;
			}
			return false;
		    }
		};
	
		FileFilter filter = new SuffixFileFilter(suffixes);
	
		Collection<File> files = new java.util.LinkedList<File>();
		innerListFiles(files, dir, filter);
	
		return files;
    }
    
    private static void innerListFiles(Collection<File> files, File dir, FileFilter filter) {
		File[] found = dir.listFiles(filter);
		if (found != null) {
		    for (int i = 0; i < found.length; i++) {
			if (found[i].isDirectory())
			    innerListFiles(files, found[i], filter);
			else
			    files.add(found[i]);
	
		    }
		}
    }

}
