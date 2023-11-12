import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.ForkJoinPool;

import javax.imageio.IIOException;

import javax.imageio.ImageIO;

public class TestImageFilter {
	private static final int NRSTEPS = 100;
	private static final int[] threads = { 1, 2, 4, 8 };
	private static boolean flag = true;

	public static void main(String[] args) throws Exception {
		
		BufferedImage image = null;
		String srcFileName = null;
		try {
			srcFileName = args[0];
			File srcFile = new File(srcFileName);
			image = ImageIO.read(srcFile);
		}
		catch (ArrayIndexOutOfBoundsException e) {
			System.out.println("Usage: java TestAll <image-file>");
			System.exit(1);
		}
		catch (IIOException e) {
			System.out.println("Error reading image file " + srcFileName + " !");
			System.exit(1);
		}

		System.out.println("Source image: " + srcFileName);

		int w = image.getWidth();
		int h = image.getHeight();
		System.out.println("Image size is " + w + "x" + h);
		System.out.println();
	
		int[] src = image.getRGB(0, 0, w, h, null, 0, w);
		int[] dst = new int[src.length];

		System.out.println("Starting sequential image filter.");

		long startTime = System.currentTimeMillis();
		ImageFilter filter0 = new ImageFilter(src, dst, w, h);
		filter0.apply();
		long endTime = System.currentTimeMillis();

		long tSequential = endTime - startTime; 
		System.out.println("Sequential image filter took " + tSequential + " milliseconds.");
		BufferedImage dstImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		dstImage.setRGB(0, 0, w, h, dst, 0, w);

		String dstName = "Filtered" + srcFileName;
		File dstFile = new File(dstName);
		ImageIO.write(dstImage, "jpg", dstFile);

		System.out.println("Output image: " + dstName);	

		System.out.println("Starting parallel image filter.");

		for ( int thread : threads) {
			src = image.getRGB(0, 0, w, h, null, 0, w);
			dst = new int[src.length];
			System.out.println("Starting parallel image filter using " + thread + " threads.");
			long startTimep = System.currentTimeMillis();
			ForkJoinPool pool = new ForkJoinPool(thread);
			for (int steps = 0; steps < NRSTEPS; steps++) {
				ParallelFJImageFilter pf = new ParallelFJImageFilter(src, dst, w, h);
				pool.invoke(pf);

				// swap references
				int[] help;	help = src;	src = dst; dst = help;
			}
			long endTimep = System.currentTimeMillis();
			long tParallel = endTimep - startTimep; 
			System.out.println("Parallel image filter took " + tParallel + " milliseconds.");
			float speedup =  ((float)tSequential / (float)tParallel);
			System.out.println("Speed-up achieved: " + speedup);
			System.out.println("Ideal speed-up: " + (float) (0.7 * thread));

			BufferedImage dstImagep = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			dstImagep.setRGB(0, 0, w, h, dst, 0, w);
	
			String dstNamep = "Filteredp" +thread+ srcFileName;
			File dstFilep = new File(dstNamep);
			ImageIO.write(dstImagep, "jpg", dstFilep);
	
			System.out.println("Output image: " + dstNamep);
		}

	}
}
