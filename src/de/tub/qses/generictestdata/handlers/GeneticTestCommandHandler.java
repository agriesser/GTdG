package de.tub.qses.generictestdata.handlers;

import java.net.URLClassLoader;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tub.qses.generictestdata.genetic.JGapExecutor;
import de.tub.qses.generictestdata.util.EclipseClassloaderUtil;

public class GeneticTestCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
	    ISelection sel = HandlerUtil.getActiveMenuSelection(event);
	    IStructuredSelection selection = (IStructuredSelection) sel;

	    Object firstElement = selection.getFirstElement();
	    if (firstElement instanceof ICompilationUnit) {
	    	ICompilationUnit unit = (ICompilationUnit) firstElement;
	    	try {
				if (unit.getAllTypes()[0].isClass()) {
					String className = unit.getAllTypes()[0].getFullyQualifiedName();
					URLClassLoader projectCL = EclipseClassloaderUtil.getClassLoaderForProject(unit.getJavaProject());
					Class<?> clazz = projectCL.loadClass(className);
					JGapExecutor.generateJGapEvolution(clazz);
				}
			} catch (JavaModelException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
	    }
		return null;
	}


}
