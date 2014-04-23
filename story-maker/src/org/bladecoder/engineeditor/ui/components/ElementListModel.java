package org.bladecoder.engineeditor.ui.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.AbstractListModel;

import org.w3c.dom.Element;

@SuppressWarnings("serial")
public class ElementListModel extends AbstractListModel<Element> {
	ArrayList<Element> model;
	boolean sorted = true;
	Comparator<Element> c;

	public ElementListModel(boolean sorted) {
		model = new ArrayList<Element>();
		this.sorted = sorted;

		if (sorted)
			c = new Comparator<Element>() {
				@Override
				public int compare(Element o1, Element o2) {
					return o1.getAttribute("id").compareTo(o2.getAttribute("id"));
				}
			};
	}

	public ElementListModel(Comparator<Element> c) {
		model = new ArrayList<Element>();
		this.sorted = true;
		this.c = c;
	}

	public int getSize() {
		return model.size();
	}
	
	public void insertElementAt(Element e, int index) {
		model.add(index, e);
		fireContentsChanged(this, 0, getSize());
	}

	public Element getElementAt(int index) {
		return model.get(index);
	}

	public void addElement(Element element) {
		if (model.add(element)) {
			if (c != null) {
				Collections.sort(model,c);
			}

			fireContentsChanged(this, 0, getSize());
		}
	}

	public void addAll(Element elements[]) {
		Collection<Element> c = Arrays.asList(elements);
		model.addAll(c);
		fireContentsChanged(this, 0, getSize());
	}

	public void clear() {
		model.clear();
		fireContentsChanged(this, 0, getSize());
	}

	public boolean contains(Element element) {
		return model.contains(element);
	}

	public Element firstElement() {
		return model.get(0);
	}

	public Iterator<Element> iterator() {
		return model.iterator();
	}

	public Element lastElement() {
		return model.get(model.size() - 1);
	}

	public boolean removeElement(Element element) {
		boolean removed = model.remove(element);
		if (removed) {
			fireContentsChanged(this, 0, getSize());
		}
		return removed;
	}
	
	public Element remove(int index) {
		Element e = model.remove(index);
		fireContentsChanged(this, 0, getSize());
		
		return e;
	}
}
