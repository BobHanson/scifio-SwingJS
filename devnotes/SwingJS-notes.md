## SwingJS notes during development of scifio-SwingJS


Most tweaks of the code can be found by searching for "@j2s" in the code. 

In opposite order of discovery of the problem during development.

13. 2020.01.01

io.scif.formats.tiff.TiffParser
io.net.imagelib2.type.numeric.integer.UnsignedIntType
net.imglib2.type.numeric.integer.UnsignedInt128BitType
io.scif.gui.UnsignedIntColorModel

This will not work in JavaScript:

		long offset = (previous & ~0xffffffffL) | (in.readInt() & 0xffffffffL);

Neither longValue & ~0xffffffff nor intValue & 0xFFFFFFFF work in JavaScript.

For the first, we need to use:

 x/C|0*C  where c == 0x100000000 
 
and, for the second, we need a pass through a Uint32Array:
 
 (uia[0]=intValue,uia[0])   where uia is a static new Uint32Array(1) 

In addition, in converting from long x to int, all that is needed is (int) x; 
anding with 0xFFFFFFFF does nothing to an int.

12. 2019.12.31 io.scif.AbstractFormat#UpdateCustomClasses should skip abstract classes:

	private void updateCustomClasses() {

		for (final Class<?> c : buildClassList()) {
			if ((c.getModifiers() & Modifier.ABSTRACT) != 0)
				continue;
				...
        }
    }
    
The problem was with TIFFFormat, which has two parsers, and SwingJS returns the list of declared classes in a different order than the Eclipse Java compiler (or runtime?).

11. 2019.12.30 IO.Register bypassed as unnecessary in SwingJS; can't make PhantomReferences. 


10. 2019.12.29 Firefox does not recognize Regex look-behind ...(?<=...)...
As a result org.imagej.axis.VariableAxis.getParticularEquation needed
modification to use (?=\\W) instead of (?<=\\w)(?=\\W)|(?<=\\W)(?=\\w)
-- that is, split just before any operator, not at the end|start of words. 

// 10. 2019.12.20  The java2script transpiler cannot handle this.<P>
//    2020.01.03  Actually, it can now...
    
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

6. 2019.12.19 DefaultPluginFinder (general Java fix; not related to JavaScript)

Moved two lines of code to straighten out loading

5. 2019.12.19 DefaultXMLService

Added lazy (unnecessary) loading of www XML schema. 
SwingJS doesn't support XML schema checking. 
Presumption that validation would be in Java.

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

