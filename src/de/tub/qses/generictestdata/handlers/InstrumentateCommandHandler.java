package de.tub.qses.generictestdata.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.handlers.HandlerUtil;

import de.tub.qses.generictestdata.jgap.Constants;

public class InstrumentateCommandHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShell(event);
	    ISelection sel = HandlerUtil.getActiveMenuSelection(event);
	    IStructuredSelection selection = (IStructuredSelection) sel;

	    Object firstElement = selection.getFirstElement();
	    if (firstElement instanceof ICompilationUnit) {
	    	ICompilationUnit unit = (ICompilationUnit) firstElement;
	    	IJavaElement element = unit.getParent();
	    	if (element instanceof IPackageFragment) {
	    		IPackageFragment packageFragment = ((IPackageFragment) element);
	    		String[] parts = unit.getElementName().split("\\.");
    			String newName = parts[0]+"Instrumentated."+parts[1];
    			ICompilationUnit oldInstance = null;
	    		if ((oldInstance = packageFragment.getCompilationUnit(newName)) != null && oldInstance.exists()) {
	    			if (!MessageDialog.openConfirm(shell, "Instrumentiere Datei ersetzen", "Möchten Sie die alte instrumentierte Datei "+newName+" löschen?")) {
	    				return null;
	    			}
	    			try {
						oldInstance.delete(true, new NullProgressMonitor());
					} catch (JavaModelException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	    		}
	    		try {
					ICompilationUnit instrumentated = packageFragment.createCompilationUnit(newName, unit.getSource(), true, new NullProgressMonitor());
					String source = instrumentated.getSource();
					Document doc = new Document(source);
					ASTParser parser = ASTParser.newParser(AST.JLS4);
					parser.setSource(instrumentated);
					CompilationUnit rootNode = (CompilationUnit) parser.createAST(null);
					AST ast = rootNode.getAST();
					rootNode.recordModifications();
					TypeDeclaration type = (TypeDeclaration) rootNode.types().get(0);
					SimpleName sn = ast.newSimpleName(type.getName().toString()+"Instrumentated");
					type.setName(sn);
					ASTRewrite rewrite = ASTRewrite.create(ast);
					discoverBranches(rewrite, type);
					TextEdit rewriteEdits = rewrite.rewriteAST(doc, instrumentated.getJavaProject().getOptions(true));
					rewriteEdits.apply(doc);
					TextEdit edit = rootNode.rewrite(doc, instrumentated.getJavaProject().getOptions(true));
					edit.apply(doc);
					instrumentated.getBuffer().setContents(doc.get());
					instrumentated.getBuffer().save(null, true);
					System.out.println("rewrite complete?");
				} catch (JavaModelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MalformedTreeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    	}
	    }
		return null;
	}

	@SuppressWarnings({"unchecked" })
	private void discoverBranches(ASTRewrite rewrite, TypeDeclaration type) {
//		TypeDeclaration branchesClass = createBranchClass(rewrite, type);
		EnumDeclaration enumDeclaration = createEnumDecl(rewrite.getAST());
		augmentMethods(rewrite, enumDeclaration, type.getMethods());
		AST ast = rewrite.getAST();
		VariableDeclarationFragment variableDeclFrag = ast.newVariableDeclarationFragment();
		variableDeclFrag.setName(ast.newSimpleName(Constants.BRANCHES_FIELD_NAME));
		FieldDeclaration fieldDeclaration = ast.newFieldDeclaration(variableDeclFrag);
		QualifiedName qName = ast.newQualifiedName(ast.newSimpleName("java"), ast.newSimpleName("util"));
		ParameterizedType pType = null;
		fieldDeclaration.setType(pType = ast.newParameterizedType(ast.newQualifiedType(ast.newSimpleType(qName), ast.newSimpleName("EnumSet"))));
		pType.typeArguments().add(ast.newSimpleType(ast.newSimpleName(Constants.BRANCH_ENUM_DEFINITION_NAME)));
		fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));
		fieldDeclaration.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		type.bodyDeclarations().add(fieldDeclaration);
		type.bodyDeclarations().add(enumDeclaration);
	}
	
	private EnumDeclaration createEnumDecl(AST ast){
		EnumDeclaration enumDecl = ast.newEnumDeclaration();
		enumDecl.setName(ast.newSimpleName(Constants.BRANCH_ENUM_DEFINITION_NAME));
		return enumDecl;
	}
		
	@SuppressWarnings("unchecked")
	private void augmentMethods(ASTRewrite rewrite, EnumDeclaration branchesClass, MethodDeclaration[] methods) {
		AST ast = rewrite.getAST();
		int branchNumber = 0;
		for (MethodDeclaration methodDecl : methods) {
			MarkerAnnotation marker = ast.newMarkerAnnotation();
			marker.setTypeName(ast.newName("de.tub.qses.generictestdata.annotations.CoverMethod"));
			methodDecl.modifiers().add(marker);
			methodDecl.getBody().statements().add(createNewBranch(ast, branchNumber++, branchesClass));
			branchNumber += searchForBranches(ast, methodDecl.getBody().statements(), branchNumber, branchesClass);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Statement createNewBranch(AST ast, int branchNumber, EnumDeclaration branchesClass) {
		EnumConstantDeclaration enumConstant = ast.newEnumConstantDeclaration();
		String createdBranch = Constants.BRANCH_ENUM_NAME+branchNumber++;
		enumConstant.setName(ast.newSimpleName(createdBranch));
		branchesClass.enumConstants().add(enumConstant);
		MethodInvocation mi = ast.newMethodInvocation();
		mi.setExpression(ast.newSimpleName(Constants.BRANCHES_FIELD_NAME));
		mi.setName(ast.newSimpleName("add"));
		mi.arguments().add(ast.newQualifiedName(ast.newSimpleName(Constants.BRANCH_ENUM_DEFINITION_NAME), ast.newSimpleName(createdBranch)));
		ExpressionStatement exStmt = ast.newExpressionStatement(mi);
		return exStmt;
	}
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private int searchForBranches(AST ast, List stmts, int branchNumber, EnumDeclaration branchesClass) {
		int added = 0;
		for (Object obj : stmts) {
			Statement stmt = (Statement) obj;
			Block branchBlock = null;
			switch (stmt.getNodeType()) {
			case ASTNode.IF_STATEMENT:
				IfStatement ifStmt = (IfStatement) stmt;
				branchBlock = (Block) ifStmt.getThenStatement();
				break;
			case ASTNode.FOR_STATEMENT:
				ForStatement forStmt = (ForStatement) stmt;
				branchBlock = (Block) forStmt.getBody();
				break;
			case ASTNode.ENHANCED_FOR_STATEMENT:
				EnhancedForStatement enhFor = (EnhancedForStatement) stmt;
				branchBlock = (Block) enhFor.getBody();
				break;
			case ASTNode.WHILE_STATEMENT:
				WhileStatement whileStmt = (WhileStatement) stmt;
				branchBlock = (Block) whileStmt.getBody();
				break;
			case ASTNode.DO_STATEMENT:
				DoStatement doStmt = (DoStatement) stmt;
				branchBlock = (Block) doStmt.getBody();
				break;
			case ASTNode.SWITCH_STATEMENT:
				System.out.println("switch case");
				SwitchStatement caseStmt = (SwitchStatement) stmt;
				int stmtCtr = 0;
				List stmtCopy = new ArrayList(caseStmt.statements());
				for (Object switchCase : caseStmt.statements()) {
					Statement swCase = (Statement) switchCase;
					if (swCase.getNodeType() == ASTNode.SWITCH_CASE) {
						stmtCopy.add(++stmtCtr, createNewBranch(ast, branchNumber+added++, branchesClass));
					}
					stmtCtr++;
				}
				caseStmt.statements().clear();
				caseStmt.statements().addAll(stmtCopy);
				added += searchForBranches(ast, caseStmt.statements(), branchNumber+added, branchesClass);
				branchBlock = null;
				break;
			}
			if (branchBlock != null) {
				branchBlock.statements().add(createNewBranch(ast, branchNumber+added++, branchesClass));
				added += searchForBranches(ast, branchBlock.statements(), branchNumber+added, branchesClass);
			}
		}
		return added;
	}

}
