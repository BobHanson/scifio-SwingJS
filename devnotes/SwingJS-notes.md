## SwingJS notes during development of scifio-SwingJS


Most tweaks of the code can be found by searching for "@j2s" in the code. 

In opposite order of discovery of the problem during development.

13. 2019.12.31 io.scif.AbstractFormat#UpdateCustomClasses should skip abstract classes:

	private void updateCustomClasses() {

		for (final Class<?> c : buildClassList()) {
			if ((c.getModifiers() & Modifier.ABSTRACT) != 0)
				continue;
				...
        }
    }
    
The problem was with TIFFFormat, which has two parsers, and SwingJS returns the list of declared classes in a different order than the Eclipse Java compiler (or runtime?).

12. 2019.12.30 IO.Register bypassed as unnecessary in SwingJS; can't make PhantomReferences. 


11. 2019.12.29 Firefox does not recognize Regex look-behind ...(?<=...)...
As a result org.imagej.axis.VariableAxis.getParticularEquation needed
modification to use (?=\\W) instead of (?<=\\w)(?=\\W)|(?<=\\W)(?=\\w)
-- that is, split just before any operator, not at the end|start of words. 

10. 2019.12.20  The java2script transpiler cannot handle this.<P> in a method call, as in AbstractReader:

		return openPlane(imageIndex, planeIndex, this.<P> castToTypedPlane(plane),
			config);


This call will use the generic "$TP" for the third parameter. But the call it is making is to a subclass that could have ByteArrayImagePlane or BufferedImagePlane, as in:


		@Override
		public BufferedImagePlane openPlane(final int imageIndex,
			final long planeIndex, final BufferedImagePlane plane,
			final Interval bounds, final SCIFIOConfig config) throws FormatException,
			IOException
		{...}

Adding 

		/** @j2sAlias *,*,P,*,* */ 

prior to the @Override instructs the transpiler to supply an alias for that method -- a second name that uses $TP instead of $io\_scif\_ByteArrayPlane.

9. 2019.12.20 SCIFIOCellImgFactory

Could not be implemented in Java 8; unnecessary for java2script? Errors are still there; don't know how to adjust for that.

8. 2019.12.20  org.scijava.util.ArrayUtils.willOverflow()

	private static boolean willOverflow(final long v1, final long v2) {
		/**
		 * We cannot use Long.MAX_VALUE in SwingJS, or certainly not this way. 
		 * Overflow is when adding 1 to the product is still the product
		 * 
		 * @j2sNative
		 * 
		 * 			return v1*v2 == v1*v2+1;
		 */
		{
			return Long.MAX_VALUE / v1 < v2;
		}
	}


7. 2019.12.20 org.bushe

Removed unnecessary check for apache logging (which is a HUGE and complicated set of classes)

6. 2019.12.19 DefaultPluginFinder 

Moved two lines of code to straighten out loading

5. 2019.12.19 DefaultXMLService

Added lazy loading of www XML schema.

4. 2019.12.18 org.scijava.util.Types

Added to prevent popup messages to users when files are not found:

			/**
			 * @j2sNative Clazz._isQuietLoad=true;
			 */


			/**
			 * @j2sNative Clazz._isQuietLoad=false;
			 */

3. 2019.12.18 Thread.currentThread().getContextClassLoader() 

This perhaps is not a problem. For SwingJS, this now means the loader for which the default is j2s/ root.


2. 2019.12.18 The META-INF folder needs to be in target/classes for Java, but some process is removing that. Periodically I have had to copy it back in there after Maven does its job.

1. 2019.12.18 META-INF/json/org.scijava.plugin.Plugin collected

This json file and a few others by the same name in Maven jar files identifies the bindings for the context. I integrated those into the one we have here, since JavaScript can't search for this on the classpath. 

