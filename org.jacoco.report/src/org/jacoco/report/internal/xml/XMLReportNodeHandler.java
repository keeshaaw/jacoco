/*******************************************************************************
 * Copyright (c) 2009, 2011 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *    Marc R. Hoffmann - generalized structure, line info
 *    
 *******************************************************************************/
package org.jacoco.report.internal.xml;

import java.io.IOException;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ICoverageNode;
import org.jacoco.core.analysis.ICoverageNode.CounterEntity;
import org.jacoco.core.analysis.ICoverageNode.ElementType;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;

/**
 * Report visitor that transforms the report structure into XML elements.
 */
public class XMLReportNodeHandler implements IReportVisitor {

	private final XMLElement element;

	private final ICoverageNode node;

	/**
	 * New handler for the given coverage node.
	 * 
	 * @param element
	 *            XML-Element representing this coverage node. The start tag
	 *            must not be closed yet to allow adding additional attributes.
	 * @param node
	 *            corresponding coverage node
	 * @throws IOException
	 *             in case of problems with the underlying writer
	 */
	public XMLReportNodeHandler(final XMLElement element,
			final ICoverageNode node) throws IOException {
		this.element = element;
		this.node = node;
		element.attr("name", node.getName());
		insertElementsBefore(element);
	}

	/**
	 * Hook to add XML elements before the child elements created by default.
	 * 
	 * @param element
	 *            this element
	 * @throws IOException
	 */
	protected void insertElementsBefore(final XMLElement element)
			throws IOException {
	}

	/**
	 * Hook to add XML elements before the child elements created by default.
	 * 
	 * @param element
	 *            this element
	 * @throws IOException
	 */
	void insertElementsAfter(final XMLElement element) throws IOException {
	}

	public IReportVisitor visitChild(final ICoverageNode node)
			throws IOException {
		final ElementType type = node.getElementType();
		switch (type) {
		case GROUP:
		case BUNDLE:
			return new XMLReportNodeHandler(element.element("group"), node);
		case PACKAGE:
			return new XMLReportNodeHandler(element.element("package"), node);
		case CLASS:
			return new XMLReportNodeHandler(element.element("class"), node);
		case METHOD:
			final XMLElement methodChild = element.element("method");
			final IMethodCoverage methodNode = (IMethodCoverage) node;
			methodChild.attr("desc", methodNode.getDesc());
			final int line = methodNode.getFirstLine();
			if (line != -1) {
				methodChild.attr("line", line);
			}
			return new XMLReportNodeHandler(methodChild, node);
		case SOURCEFILE:
			return new XMLReportNodeHandler(element.element("sourcefile"), node) {
				@Override
				protected void insertElementsAfter(final XMLElement element)
						throws IOException {
					writeLines((ISourceNode) node, element);
				}
			};
		default:
			throw new AssertionError(type);
		}
	}

	public final void visitEnd(final ISourceFileLocator sourceFileLocator)
			throws IOException {
		insertElementsAfter(element);
		for (final CounterEntity counterEntity : CounterEntity.values()) {
			createCounterElement(counterEntity);
		}
		element.close();
	}

	private void createCounterElement(final CounterEntity counterEntity)
			throws IOException {
		final ICounter counter = node.getCounter(counterEntity);
		if (counter.getTotalCount() > 0) {
			final XMLElement counterNode = this.element.element("counter");
			counterNode.attr("type", counterEntity.name());
			counterNode.attr("covered", counter.getCoveredCount());
			counterNode.attr("missed", counter.getMissedCount());
			counterNode.close();
		}
	}

	private static void writeLines(final ISourceNode source,
			final XMLElement parent) throws IOException {
		final int last = source.getLastLine();
		for (int nr = source.getFirstLine(); nr <= last; nr++) {
			final ILine line = source.getLine(nr);
			if (line.getStatus() != ILine.NO_CODE) {
				final XMLElement element = parent.element("line");
				element.attr("nr", nr);
				final ICounter insn = line.getInstructionCounter();
				element.attr("mi", insn.getMissedCount());
				element.attr("ci", insn.getCoveredCount());
				final ICounter branches = line.getBranchCounter();
				element.attr("mb", branches.getMissedCount());
				element.attr("cb", branches.getCoveredCount());
			}
		}
	}

}
