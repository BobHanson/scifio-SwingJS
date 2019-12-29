## SwingJS notes during development of scifio-SwingJS


Most tweaks of the code can be found by searching for "@j2s" in the code. 

In order of presentation of the problem during development 12/18-12/28.

1. META-INF/json/org.scijava.plugin.Plugin collected

This json file and a few others by the same name in Maven jar files identifies the bindings for the context. I integrated those into the one we have here, since JavaScript can't search for this on the classpath. 

2. The META-INF folder needs to be in target/classes for Java, but some process is removing that. Periodically I have had to copy it back in there after Maven does its job.

3. Thread.currentThread().getContextClassLoader() 

This perhaps is not a problem. For SwingJS, this now means the loader for which the default is j2s/ root.

4. org.scijava.util.Types

Added to prevent popup messages to users when files are not found:

			/**
			 * @j2sNative Clazz._isQuietLoad=true;
			 */


			/**
			 * @j2sNative Clazz._isQuietLoad=false;
			 */


5. DefaultXMLService

Added lazy loading of www XML schema.

6. DefaultPluginFinder 

Moved two lines of code to straighten out loading

7. org.bushe

Removed unnecessary check for apache logging (which is a HUGE and complicated set of classes)

10. The java2script transpiler cannot handle this.<P> in a method call, as in AbstractReader:

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
