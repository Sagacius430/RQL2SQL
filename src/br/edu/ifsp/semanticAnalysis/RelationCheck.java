package br.edu.ifsp.semanticAnalysis;

import java.util.HashSet;
import java.util.Set;

import br.edu.ifsp.parser.RelationalQueryLanguageConstants;
import br.edu.ifsp.symbolTable.*;
import br.edu.ifsp.syntacticTree.*;

public class RelationCheck implements RelationalQueryLanguageConstants {
	SymbolTable schema;
	int semanticErrors;
	int globalScope;
	Set<Integer> numberConstants = new HashSet<Integer>();

	public RelationCheck(SymbolTable st) {
		schema = st;
		semanticErrors = 0;
		globalScope = 0;

		// Fill the set of number constants in RQL
		numberConstants.add(RelationalQueryLanguageConstants.INTEGER);
		numberConstants.add(RelationalQueryLanguageConstants.DECIMAL);
		numberConstants.add(RelationalQueryLanguageConstants.BIN);
		numberConstants.add(RelationalQueryLanguageConstants.HEX);
		numberConstants.add(RelationalQueryLanguageConstants.OCT);
	}

	public void clearSemanticErrors() {
		semanticErrors = 0;
	}

	public int getSemanticErrors() {
		return semanticErrors;
	}

	public int semanticAnalysis(ListNode root) {
		if (root == null) {
			System.out.println("Nothing to be analysed");
			return 0;
		}
		System.out.println("Starting the semantic analysis");
		relationalOperationsNodeListCheck(root);
		return semanticErrors;
	}

	private void throwSemanticError(String message) {
		semanticErrors++;
		System.out.println(message);
	}

	private void relationalOperationsNodeListCheck(ListNode x) {
		if (x == null) {
			return;
		}
		relationalOperationsNodeCheck((RelationalOperationsNode) x.getNode());
		relationalOperationsNodeListCheck(x.getNext());
	}

	private void relationalOperationsNodeCheck(RelationalOperationsNode x) {
		if (x == null)
			return;
		if (x.getNode() instanceof QueryNode)
			queryNodeCheck((QueryNode) x.getNode());
	}

	private void queryNodeCheck(QueryNode x) {
		if (x == null)
			return;
		if (x.getNode() instanceof ReadyOnlyOperationsNode) {
			readyOnlyOperationsNodeCheck((ReadyOnlyOperationsNode) x.getNode());
		}
	}

	private void readyOnlyOperationsNodeCheck(ReadyOnlyOperationsNode x) {
		if (x == null)
			return;
		globalScope++;
		schema.addRelation("temporaryRelation" + globalScope);
		if (x.getNode() instanceof UnitaryOperationsNode) {
			unitaryOperationsNodeCheck((UnitaryOperationsNode) x.getNode());
		} else {
			binaryOperationsNodeCheck((BinaryOperationsNode) x.getNode());
		}
	}

	private void binaryOperationsNodeCheck(BinaryOperationsNode x) {
		if (x == null)
			return;
		int scope = globalScope;
		int binaryScopes[] = binarySetNodeCheck(x.getBinarySetNode());
		if (x.getBinaryOperationsNodeChildren() instanceof JoinNode)
			joinNodeCheck((JoinNode) x.getBinaryOperationsNodeChildren(), scope, binaryScopes);
		if (x.getBinaryOperationsNodeChildren() instanceof CrossJoinNode)
			crossJoinNodeCheck((CrossJoinNode) x.getBinaryOperationsNodeChildren(), scope, binaryScopes);
		if (x.getBinaryOperationsNodeChildren() instanceof UnionNode)
			unionNodeCheck((UnionNode) x.getBinaryOperationsNodeChildren(), scope, binaryScopes);
		if (x.getBinaryOperationsNodeChildren() instanceof IntersectionNode)
			intersectionNodeCheck((IntersectionNode) x.getBinaryOperationsNodeChildren(), scope, binaryScopes);
		if (x.getBinaryOperationsNodeChildren() instanceof DifferenceNode)
			differenceNodeCheck((DifferenceNode) x.getBinaryOperationsNodeChildren(), scope, binaryScopes);
		if (x.getBinaryOperationsNodeChildren() instanceof DivisionNode)
			divisionNodeCheck((DivisionNode) x.getBinaryOperationsNodeChildren(), scope, binaryScopes);
	}

	private int[] binarySetNodeCheck(BinarySetNode x) {
		if (x == null)
			return null;
		int binaryScopes[] = new int[2];

		binaryScopes[0] = globalScope + 1;
		if (x.getReadyOnlyOperationsNode1() != null) {
			readyOnlyOperationsNodeCheck(x.getReadyOnlyOperationsNode1());
		} else {
			globalScope++;
			schema.addRelation("temporaryRelation" + globalScope);
			relationNodeCheck(x.getRelationNode1());
		}

		binaryScopes[1] = globalScope + 1;
		if (x.getReadyOnlyOperationsNode2() != null) {
			readyOnlyOperationsNodeCheck(x.getReadyOnlyOperationsNode2());
		} else {
			globalScope++;
			schema.addRelation("temporaryRelation" + globalScope);
			relationNodeCheck(x.getRelationNode2());
		}
		return binaryScopes;
	}

	private void unionNodeCheck(UnionNode x, int scope, int[] binaryScopes) {
		if (x == null)
			return;
		Relation relation1 = schema.getRelation("temporaryRelation" + (binaryScopes[0]));
		Relation relation2 = schema.getRelation("temporaryRelation" + (binaryScopes[1]));
		if (relation1.getNumberOfAttributes() != relation2.getNumberOfAttributes()) {
			throwSemanticError(
					"\tfor the union operation, the relations must have the same number of attributes : At the line "
							+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		} else {
			schema.replaceRelation("temporaryRelation" + scope, relation1);
		}
	}

	private void intersectionNodeCheck(IntersectionNode x, int scope, int[] binaryScopes) {
		if (x == null)
			return;
		Relation relation1 = schema.getRelation("temporaryRelation" + (binaryScopes[0]));
		Relation relation2 = schema.getRelation("temporaryRelation" + (binaryScopes[1]));
		if (relation1.getNumberOfAttributes() != relation2.getNumberOfAttributes()) {
			throwSemanticError(
					"\tfor the intersection operation, the relations must have the same number of attributes : At the line "
							+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}else{
			schema.replaceRelation("temporaryRelation" + scope, relation1);
		}
	}

	private void differenceNodeCheck(DifferenceNode x, int scope, int[] binaryScopes) {
		if (x == null)
			return;
		Relation relation1 = schema.getRelation("temporaryRelation" + (binaryScopes[0]));
		Relation relation2 = schema.getRelation("temporaryRelation" + (binaryScopes[1]));
		if (relation1.getNumberOfAttributes() != relation2.getNumberOfAttributes()) {
			throwSemanticError(
					"\tfor the difference operation, the relations must have the same number of attributes : At the line "
							+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}else{
			schema.replaceRelation("temporaryRelation" + scope, relation1);
		}
	}

	private void joinNodeCheck(JoinNode x, int scope, int[] binaryScopes) {
		if (x == null)
			return;
		Relation relation1 = schema.getRelation("temporaryRelation" + (binaryScopes[0]));
		Relation relation2 = schema.getRelation("temporaryRelation" + (binaryScopes[1]));
		Relation join = schema.getRelation("temporaryRelation" + scope);
		// Add the attributes of the first relation
		for (String s : relation1.getAttributeNames()) {
			join.addAttribute(s, relation1.getAttribute(s));
		}
		// Add the attributes of the second relation
		for (String s : relation2.getAttributeNames()) {
			join.addAttribute(s, relation2.getAttribute(s));
		}
	}

	private void crossJoinNodeCheck(CrossJoinNode x, int scope, int[] binaryScopes) {
		if (x == null)
			return;
		Relation relation1 = schema.getRelation("temporaryRelation" + (binaryScopes[0]));
		Relation relation2 = schema.getRelation("temporaryRelation" + (binaryScopes[1]));
		Relation join = schema.getRelation("temporaryRelation" + scope);
		// Add the attributes of the first relation
		for (String s : relation1.getAttributeNames()) {
			join.addAttribute(s, relation1.getAttribute(s));
		}
		// Add the attributes of the second relation
		for (String s : relation2.getAttributeNames()) {
			join.addAttribute(s, relation2.getAttribute(s));
		}
	}

	private void divisionNodeCheck(DivisionNode x, int scope, int[] binaryScopes) {
		if (x == null)
			return;
		Relation relation1 = schema.getRelation("temporaryRelation" + (binaryScopes[0]));
		Relation relation2 = schema.getRelation("temporaryRelation" + (binaryScopes[1]));
		Relation division = schema.getRelation("temporaryRelation" + scope);

		if (relation1.getNumberOfAttributes() > relation2.getNumberOfAttributes()) {
			Set<String> relation1Attributes = relation1.getAttributeNames();
			Set<String> relation2Attributes = relation2.getAttributeNames();
			for (String attribute : relation2Attributes) {
				if (!relation1.hasAttribute(attribute)) {
					throwSemanticError("\tThe divisor of the operation must be a subset of the dividend: \""
							+ x.getPosition().image + "\" at the line " + x.getPosition().beginLine + ", column "
							+ x.getPosition().beginColumn);
					break;
				}
			}
			for (String attribute : relation1Attributes) {
				if (!relation2Attributes.contains(attribute))
					division.addAttribute(attribute, relation1.getAttribute(attribute));
			}
		} else {
			throwSemanticError("\tThe divisor of the operation must be a subset of the dividend: \""
					+ x.getPosition().image + "\" at the line " + x.getPosition().beginLine + ", column "
					+ x.getPosition().beginColumn);
		}
	}

	private void unitaryOperationsNodeCheck(UnitaryOperationsNode x) {
		if (x == null)
			return;
		int scope = globalScope;
		if (x.getRelationNode() != null) {
			relationNodeCheck(x.getRelationNode());
		} else {
			readyOnlyOperationsNodeCheck(x.getReadyOnlyOperationsNode());
		}
		if (x.getUnitaryOperationsChildrenNode() instanceof ProjectNode)
			projectNodeCheck((ProjectNode) x.getUnitaryOperationsChildrenNode(), scope);
		else if (x.getUnitaryOperationsChildrenNode() instanceof RenameNode)
			renameNodeCheck((RenameNode) x.getUnitaryOperationsChildrenNode(), scope);
		else if (x.getUnitaryOperationsChildrenNode() instanceof SelectNode) {
			selectNodeCheck((SelectNode) x.getUnitaryOperationsChildrenNode(), scope);
		} else if (x.getUnitaryOperationsChildrenNode() instanceof TransitiveCloseNode)
			if (x.getRelationNode() == null)
				transitiveCloseNodeCheck((TransitiveCloseNode) x.getUnitaryOperationsChildrenNode(), (scope + 1));
			else
				transitiveCloseNodeCheck((TransitiveCloseNode) x.getUnitaryOperationsChildrenNode(), scope);
	}

	private void transitiveCloseNodeCheck(TransitiveCloseNode x, int scope) {
		if (x == null)
			return;
		Relation r = schema.getRelation("temporaryRelation" + scope);
		if (r.getNumberOfAttributes() != 2) {
			throwSemanticError("\tTransitive closure requires a binary relation: At the line "
					+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}
	}

	private void projectNodeCheck(ProjectNode x, int scope) {
		if (x == null)
			return;
		attributeNodeListCheck(x.getProjectNodeList(), scope);
	}

	private void attributeNodeListCheck(ListNode x, int scope) {
		if (x == null)
			return;
		attributeNodeCheck((AttributeNode) x.getNode(), scope);
		attributeNodeListCheck(x.getNext(), scope);
	}

	private void attributeNodeCheck(AttributeNode x, int scope) {
		if (x == null)
			return;
		Relation r = schema.getRelation("temporaryRelation" + (scope + 1));
		if (r.hasAttribute(x.getPosition().image)) {
			schema.getRelation("temporaryRelation" + scope).addAttribute(x.getPosition().image,
					r.getAttribute(x.getPosition().image));
		} else {
			throwSemanticError("\tAttribute does not exist: \"" + x.getPosition().image + "\" at the line "
					+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}
	}

	private void selectNodeCheck(SelectNode x, int scope) {
		if (x == null)
			return;
		logicalSentenceNodeCheck(x.getLogicalSentenceNode(), scope);
	}

	private int logicalSentenceNodeCheck(LogicalSentenceNode x, int scope) {
		if (x == null)
			return 0;
		if (x.getConditionalSentenceNode() != null)
			return conditionalSentenceNodeCheck(x.getConditionalSentenceNode(), scope);
		else
			return logicalOperatorNodeCheck(x.getLogicalOperatorNode(), scope);
	}

	private int logicalOperatorNodeCheck(LogicalOperatorNode x, int scope) {
		if (x == null)
			return 0;
		int kind1, kind2;
		kind1 = conditionalSentenceNodeCheck(x.getConditionalSentenceNode1(), scope);
		if (x.getNextLogicalOperatorNode() == null)
			kind2 = conditionalSentenceNodeCheck(x.getConditionalSentenceNode2(), scope);
		else
			kind2 = logicalOperatorNodeCheck(x.getNextLogicalOperatorNode(), scope);
		int trueConstant = RelationalQueryLanguageConstants.TRUE;
		int falseConstant = RelationalQueryLanguageConstants.FALSE;
		if ((kind1 != trueConstant && kind1 != falseConstant) || (kind2 != trueConstant && kind2 != falseConstant)) {
			throwSemanticError("\tLogical operations requires two boolean values: At the line "
					+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}
		return kind1;
	}

	private int conditionalSentenceNodeCheck(ConditionalSentenceNode x, int scope) {
		if (x == null)
			return 0;
		int kind1 = comparisonSentenceNodeCheck(x.getComparisonSentenceNode(), scope);
		ifNodeListCheck(x.getIfListNode(), scope);
		return kind1;
	}

	private int ifNodeListCheck(ListNode x, int scope) {
		if (x == null)
			return 0;
		int kind1 = ifNodeCheck((IfNode) x.getNode(), scope);
		ifNodeListCheck(x.getNext(), scope);
		return kind1;
	}

	private int ifNodeCheck(IfNode x, int scope) {
		if (x == null)
			return 0;
		int kind1 = comparisonSentenceNodeCheck(x.getComparisonSentenceNode1(), scope);
		comparisonSentenceNodeCheck(x.getComparisonSentenceNode2(), scope);
		return kind1;
	}

	private int comparisonSentenceNodeCheck(ComparisonSentenceNode x, int scope) {
		if (x == null)
			return 0;
		if (x.getInstanceofSentenceNode() != null)
			return instanceofSentenceNodeCheck(x.getInstanceofSentenceNode(), scope);
		else
			return comparisonOperatorNodeCheck(x.getComparisonOperatorNode(), scope);
	}

	private int comparisonOperatorNodeCheck(ComparisonOperatorNode x, int scope) {
		if (x == null)
			return 0;
		int kind1, kind2;
		kind1 = instanceofSentenceNodeCheck(x.getInstanceofSentenceNode1(), scope);
		if (x.getNextComparisonOperatorNode() == null)
			kind2 = instanceofSentenceNodeCheck(x.getInstanceofSentenceNode2(), scope);
		else
			kind2 = comparisonOperatorNodeCheck(x.getNextComparisonOperatorNode(), scope);
		if (x.getPosition().kind != RelationalQueryLanguageConstants.EQUALS
				&& x.getPosition().kind != RelationalQueryLanguageConstants.NOT_EQUALS)
			if (!numberConstants.contains(kind1) || !numberConstants.contains(kind2)) {
				throwSemanticError("\tSize comparators requires two numeric operators : At the line "
						+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
			}
		return RelationalQueryLanguageConstants.TRUE;
	}

	private int instanceofSentenceNodeCheck(InstanceofSentenceNode x, int scope) {
		if (x == null)
			return 0;
		if (x.getType() == null)
			return additionSentenceNodeCheck(x.getAdditionSentenceNode(), scope);
		else
			return RelationalQueryLanguageConstants.TRUE;
	}

	private int additionSentenceNodeCheck(AdditionSentenceNode x, int scope) {
		if (x == null)
			return 0;
		if (x.getMultiplicationSentenceNode() != null)
			return multiplicationSentenceNodeCheck(x.getMultiplicationSentenceNode(), scope);
		else
			return additionOperatorNodeCheck(x.getAdditionOperatorNode(), scope);
	}

	private int additionOperatorNodeCheck(AdditionOperatorNode x, int scope) {
		if (x == null)
			return 0;
		int kind1, kind2;
		kind1 = multiplicationSentenceNodeCheck(x.getMultiplicationSentenceNode1(), scope);
		if (x.getNextAdditionOperatorNode() == null)
			kind2 = multiplicationSentenceNodeCheck(x.getMultiplicationSentenceNode2(), scope);
		else
			kind2 = additionOperatorNodeCheck(x.getNextAdditionOperatorNode(), scope);
		if (!numberConstants.contains(kind1) || !numberConstants.contains(kind2)) {
			throwSemanticError("\tAddition or subtraction requires two numeric operators : At the line "
					+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}
		return kind1;
	}

	private int multiplicationSentenceNodeCheck(MultiplicationSentenceNode x, int scope) {
		if (x == null)
			return 0;
		if (x.getFactorNode() != null)
			return factorNodeCheck(x.getFactorNode(), scope);
		else
			return multiplicationOperatorNode(x.getMultiplicationOperatorNode(), scope);
	}

	private int multiplicationOperatorNode(MultiplicationOperatorNode x, int scope) {
		if (x == null)
			return 0;
		int kind1, kind2;
		kind1 = factorNodeCheck(x.getFactorNode1(), scope);
		if (x.getNextMultiplicationOperatorNode() == null) {
			kind2 = factorNodeCheck(x.getFactorNode2(), scope);
		} else
			kind2 = multiplicationOperatorNode(x.getNextMultiplicationOperatorNode(), scope);
		if (!numberConstants.contains(kind1) || !numberConstants.contains(kind2)) {
			throwSemanticError(
					"\tMultiplication, division, power or MOD operation requires two numeric operators : At the line "
							+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}
		return kind1;
	}

	private int factorNodeCheck(FactorNode x, int scope) {
		if (x == null)
			return 0;
		if (x.getConditionalSentenceNode() != null)
			return conditionalSentenceNodeCheck(x.getConditionalSentenceNode(), scope);
		else {
			if (x.getPosition().kind == RelationalQueryLanguageConstants.IDENTIFIER) {
				if (!schema.getRelation("temporaryRelation" + (scope + 1)).hasAttribute(x.getPosition().image)) {
					throwSemanticError("\tAttribute does not exist: \"" + x.getPosition().image + "\" at the line "
							+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
				}
				Attribute attribute = schema.getRelation("temporaryRelation" + (scope + 1))
						.getAttribute(x.getPosition().image);
				String type = attribute.getType();
				return symbolTableTypeConvert(type);
			}
		}
		return x.getPosition().kind;
	}

	private int symbolTableTypeConvert(String type) {
		if (type.equals("VARCHAR") || type.equals("VARCHAR"))
			return RelationalQueryLanguageConstants.STRING;
		else if (type.equals("INT") || type.equals("INTEGER"))
			return RelationalQueryLanguageConstants.INTEGER;
		else if (type.equals("DOUBLE") || type.equals("DECIMAL") || type.equals("FLOAT") || type.equals("LONG")
				|| type.equals("BLOB"))
			return RelationalQueryLanguageConstants.DECIMAL;
		else
			return 0;
	}

	private void renameNodeCheck(RenameNode x, int scope) {
		if (x == null)
			return;
		renameSetNodeListCheck(x.getRenameSetNodeList(), scope);
	}

	private void renameSetNodeListCheck(ListNode x, int scope) {
		if (x == null)
			return;
		renameSetNodeCheck((RenameSetNode) x.getNode(), scope);
		renameSetNodeListCheck(x.getNext(), scope);
	}

	private void renameSetNodeCheck(RenameSetNode x, int scope) {
		if (x == null)
			return;
		Relation r = schema.getRelation("temporaryRelation" + (scope + 1));
		String toRename = x.getToRenameAttributeNode().getPosition().image;
		String renamed = x.getRenamedAttributeNode().getPosition().image;
		if (r.hasAttribute(toRename)) {
			schema.getRelation("temporaryRelation" + scope).addAttribute(renamed, r.getAttribute(toRename));
		} else {
			throwSemanticError("\tAttribute does not exist: \"" + x.getToRenameAttributeNode().getPosition().image
					+ "\" at the line " + x.getToRenameAttributeNode().getPosition().beginLine + ", column "
					+ x.getToRenameAttributeNode().getPosition().beginColumn);
		}

	}

	private void relationNodeCheck(RelationNode x) {
		if (x == null)
			return;
		if (schema.hasRelation(x.getPosition().image)) {
			//System.out.println("DEFINING: temporaryRelation" + globalScope + " AS " + x.getPosition().image);
			schema.replaceRelation("temporaryRelation" + globalScope, schema.getRelation(x.getPosition().image));
		} else {
			throwSemanticError("\tRelation does not exist: \"" + x.getPosition().image + "\" at the line "
					+ x.getPosition().beginLine + ", column " + x.getPosition().beginColumn);
		}
	}

}
