
package io.scif.ui;

import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.WindowConstants;

import org.scijava.io.DefaultIOService;
import org.scijava.io.location.FileLocation;

//import io.scif.CheckerTest.FakeChecker;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.img.IO;
import io.scif.img.SCIFIOImgPlus;
import javajs.async.AsyncFileChooser;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.display.ColorTables;
import net.imglib2.FinalInterval;
import net.imglib2.Localizable;
import net.imglib2.converter.Converter;
import net.imglib2.converter.RealARGBConverter;
import net.imglib2.converter.RealLUTConverter;
import net.imglib2.display.ColorTable;
import net.imglib2.display.projector.composite.CompositeXYProjector;
import net.imglib2.display.screenimage.awt.ARGBScreenImage;
import net.imglib2.position.FunctionRandomAccessible;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;

public class TestJS {

	private static BufferedImage bi;

	public static void main(final String... args) throws Exception {

		// Define a function in 2-space.
		final int ndim = 2;
		final int[] scale = { 10 };
		final UnsignedByteType t = new UnsignedByteType();
		final Supplier<UnsignedByteType> typeSupplier = () -> t;
		final BiConsumer<Localizable, UnsignedByteType> function = (l, type) -> {
			final long x = l.getLongPosition(0);
			final long y = l.getLongPosition(1);
			final double result = (Math.cos((double) x / scale[0]) + Math.sin((double) y / scale[0])) / 2;
			type.setReal((int) (255 * result));
		};
		final FunctionRandomAccessible<UnsignedByteType> ra = //
				new FunctionRandomAccessible<>(ndim, function, typeSupplier);

		// Display it.
		final JFrame frame = new JFrame("ImgLib2");
		final JPanel contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		frame.setContentPane(contentPane);

		final JPanel canvas = new JPanel() {
			@Override
			public void paint(final Graphics g) {
				final int w = getSize().width;
				final int h = getSize().height;
				final IntervalView<UnsignedByteType> rai = Views.interval(ra, new FinalInterval(w, h));
				final ARGBScreenImage image = new ARGBScreenImage(getSize().width, getSize().height);
				final ArrayList<Converter<UnsignedByteType, ARGBType>> converters = new ArrayList<>();
				final Converter<UnsignedByteType, ARGBType> converter = new RealARGBConverter<>(0, 255);
				converters.add(converter);
				final CompositeXYProjector<UnsignedByteType> proj = new CompositeXYProjector<>(rai, image, converters,
						-1);
				proj.map();
				bi = image.image();
				g.drawImage(bi, 0, 0, null);
			}
		};
		JButton loader = new JButton("Open File");
		loader.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent evt) {

//				AsyncFileChooser fc = new AsyncFileChooser();
//				fc.showOpenDialog(frame, new Runnable() {
//
//					@Override
//					public void run() {
						try {
							
							File file = new File("data/out_benchmark_v1_2018_x64y64z5c2s1t1.ids.tif");//fc.getSelectedFile();
							SCIFIOConfig fmt = new SCIFIOConfig().imgOpenerSetImgModes(ImgMode.AUTO);
							List list = IO.open(new FileLocation(file), fmt);
							System.out.println(list.size() + " images found");
							SCIFIOImgPlus img = (SCIFIOImgPlus) list.get(0);
							display(img);
						} catch (Exception e) {
							e.printStackTrace();
							System.out.println("???");
						}
//					}
//
//				}, null);
			}

		});
		contentPane.add(canvas, BorderLayout.CENTER);
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
		buttons.add(loader);
		JButton saver = new JButton("Save PNG");
		saver.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					File f = new File("data/test.png");
					ImageIO.write(bi, "PNG", f);
					System.out.println("image saved as " + f.getAbsolutePath());
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
			}
			
		});
		buttons.add(saver);
		contentPane.add(buttons, BorderLayout.NORTH);
		// With interactivity!
		final JScrollBar slider = new JScrollBar(Adjustable.HORIZONTAL, 10, 0, 1, 100);
		slider.addAdjustmentListener(e -> {
			scale[0] = e.getValue();
			canvas.invalidate();
			canvas.repaint();
		});
		contentPane.add(slider, BorderLayout.SOUTH);

		frame.setBounds(100, 100, 500, 500);
		frame.setVisible(true);
	}

	public static <T extends RealType<T>> void display(final ImgPlus<T> img) {
		// width and height of the raw data
		// NB: Assumes first two dimensions are [X, Y].
		final long width = img.dimension(0);
		final long height = img.dimension(1);

		// number of channels to composite together
		final int cIndex = img.dimensionIndex(Axes.CHANNEL);
		final long channels = img.dimension(cIndex);
		System.out.println("Data is " + width + " x " + height + " x " + channels);

		// width and height of the BufferedImage to paint
		final int scaledWidth = (int) Math.min(width, 2000);
		final int scaledHeight = (int) Math.min(height, 2000);

		// create the composite converters
		final ArrayList<Converter<T, ARGBType>> converters = new ArrayList<Converter<T, ARGBType>>();
		for (int c = 0; c < channels; c++) {
			final ColorTable colorTable;
			switch (c) {
			case 0:
				colorTable = ColorTables.RED;
				break;
			case 1:
				colorTable = ColorTables.GREEN;
				break;
			case 2:
				colorTable = ColorTables.BLUE;
				break;
			case 3:
				colorTable = ColorTables.CYAN;
				break;
			case 4:
				colorTable = ColorTables.MAGENTA;
				break;
			case 5:
				colorTable = ColorTables.YELLOW;
				break;
			default:
				colorTable = ColorTables.GRAYS;
				break;
			}
			// NB: For some data types, [0, 255] may not be what you want here...
			final double min = 0;
			final double max = 255;
			converters.add(new RealLUTConverter<T>(min, max, colorTable));
		}

		final ARGBScreenImage screenImage = new ARGBScreenImage(scaledWidth, scaledHeight);
		final CompositeXYProjector<T> proj = new CompositeXYProjector<T>(img, screenImage, converters, cIndex);
		proj.setComposite(true);

		// project the image
		System.out.println("Mapping data to screen image...");
		proj.map();

		// finally, here is the BufferedImage
		bi = screenImage.image();

		// show it!
		System.out.println("Displaying screen image...");
		final JFrame frame = new JFrame(img.getName());
		final ImageIcon imageIcon = new ImageIcon(bi, img.getName());
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new JLabel(imageIcon), BorderLayout.CENTER);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}

}
