import java.util.concurrent.RecursiveAction;

/**
 * Iterative nine-point image convolution filter working on linearized image. 
 * In each of the NRSTEPS iteration steps, the average RGB-value of each pixel 
 * in the source array is computed taking into account the pixel and its 8 neighbor 
 * pixels (in 2D) and written to the destination array.
 */
public class ParallelFJImageFilter extends RecursiveAction {
	private int[] src;
	private int[] dst;
	private int width;
	private int height;
	private int begin;
	private int end;

	public ParallelFJImageFilter(int[] src, int[] dst, int w, int h) {
		this.src = src;
		this.dst = dst;
		width = w;
		height = h;
		begin = 1;
		end = h - 1;
	}
	public ParallelFJImageFilter(int[] src, int[] dst, int w, int h, int ss, int ee) {
		this.src = src;
		this.dst = dst;
		width = w;
		height = h;
		begin = ss;
		end = ee;
	}

	public synchronized void swap(int[] source,int[] destination) {
		int[] help; help = source; src = destination; dst = help;
	}
	public void apply(int nthreads) {
		int index, pixel;
			for (int i = begin; i < end ; i++) {
				for (int j = 1; j < width - 1; j++) {
					float rt = 0, gt = 0, bt = 0;
					for (int k = i - 1; k <= i + 1; k++) {
						index = k * width + j - 1;
						pixel = src[index];
						rt += (float) ((pixel & 0x00ff0000) >> 16);
						gt += (float) ((pixel & 0x0000ff00) >> 8);
						bt += (float) ((pixel & 0x000000ff));

						index = k * width + j;
						pixel = src[index];
						rt += (float) ((pixel & 0x00ff0000) >> 16);
						gt += (float) ((pixel & 0x0000ff00) >> 8);
						bt += (float) ((pixel & 0x000000ff));

						index = k * width + j + 1;
						pixel = src[index];
						rt += (float) ((pixel & 0x00ff0000) >> 16);
						gt += (float) ((pixel & 0x0000ff00) >> 8);
						bt += (float) ((pixel & 0x000000ff));
					}
					// Re-assemble destination pixel.
					index = i * width + j;
					int dpixel = (0xff000000) | (((int) rt / 9) << 16) | (((int) gt / 9) << 8) | (((int) bt / 9));
					dst[index] = dpixel;
				}
			}
			// swap references
			// int[] help; help = src; src = dst; dst = help;
			// swap(src, dst);
	}
	protected static int sThreshold = 25;
	int nthreads = java.util.concurrent.ForkJoinPool.commonPool().getActiveThreadCount();
	int split;
    @Override
    protected void compute() {
        if (end - begin < sThreshold) {
            apply(nthreads);
        } else {
            int split = (begin + end) / 2;
            ParallelFJImageFilter firstTask = new ParallelFJImageFilter(src, dst, width, height, begin, split);
            ParallelFJImageFilter secondTask = new ParallelFJImageFilter(src, dst, width,height, split, end);

            invokeAll(firstTask, secondTask);
        }
	}
}