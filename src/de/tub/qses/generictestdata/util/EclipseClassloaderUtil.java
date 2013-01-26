package de.tub.qses.generictestdata.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaRuntime;

public class EclipseClassloaderUtil {

	public static URLClassLoader getClassLoaderForProject(IJavaProject project) {
		try {
			String[] cpEntries = JavaRuntime.computeDefaultRuntimeClassPath(project);
			List<URL> urlList = new ArrayList<URL>();
			for (int i = 0; i < cpEntries.length; i++) {
				String entry = cpEntries[i];
				IPath path = new Path(entry);
				urlList.add(path.toFile().toURI().toURL());
			}
			ClassLoader parentCL = project.getClass().getClassLoader();
			URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
			return new URLClassLoader(urls, parentCL);
		} catch (CoreException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
}
