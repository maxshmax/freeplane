package org.freeplane.core.ui.menubuilders.generic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.freeplane.core.util.LogUtils;

/**
 * @author Dimitry
 *
 */
public class Entry {
	
	private String name;
	private Entry parent;
	private List<String> builders;
	final private Map<String, Object> attributes;
	final private ArrayList<Entry> childEntries;


	public Entry() {
		super();
		this.name = "";
		childEntries = new ArrayList<>();
		attributes = new HashMap<>();
		builders = Collections.emptyList();
	}


	public void setAttribute(final String key, Object value) {
		if(attributes.containsKey(key)){
			if(value != attributes.get(key)) {
				LogUtils.warn("Entry: " + getName());
				LogUtils.warn("Value: " + String.valueOf(value));
				throw new AttributeAlreadySetException(key, attributes.get(key));
			}
		}
		else
			attributes.put(key, value);
	}
	
	public Object getAttribute(final String key) {
		return attributes.get(key);
	}

	public void addChild(Entry entry) {
		childEntries.add(entry);
		entry.setParent(this);
	}

	private void setParent(Entry parent) {
		this.parent = parent;
		
	}

	public Entry setBuilders(List<String> builders) {
		this.builders = builders;
		return this;
		
	}

	public Entry setBuilders(String... builders) {
		return setBuilders(Arrays.asList(builders));
	}

	public Entry getParent() {
		return parent;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return (parent != null ? parent.getPath() : "") +  "/" + getName();
	}

	public String getName() {
		return name;
	}

	public Entry getChild(int index) {
		return childEntries.get(index);
	}

	public Entry getChild(int... indices) {
		Entry entry = this;
		for(int index : indices)
			entry = entry.getChild(index);
		return entry;
	}


	public Iterable<Entry> children() {
		return childEntries;
	}


	public Collection<String> builders() {
		return builders;
	}


	public void removeChildren() {
		childEntries.clear();
	}

	public Object removeAttribute(String key) {
		return attributes.remove(key);
	}


	public boolean hasChildren() {
		return ! childEntries.isEmpty();
	}


	public Entry getRoot() {
		return parent == null ? this : parent.getRoot();
	}

	@Override
	public String toString() {
		return "Entry [name=" + name + ", builders=" + builders + ", attributes=" + attributes + ", childEntries="
		        + childEntries + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
		result = prime * result + ((builders == null) ? 0 : builders.hashCode());
		result = prime * result + ((childEntries == null) ? 0 : childEntries.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Entry other = (Entry) obj;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		}
		else if (!attributes.equals(other.attributes))
			return false;
		if (builders == null) {
			if (other.builders != null)
				return false;
		}
		else if (!builders.equals(other.builders))
			return false;
		if (childEntries == null) {
			if (other.childEntries != null)
				return false;
		}
		else if (!childEntries.equals(other.childEntries))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		}
		else if (!name.equals(other.name))
			return false;
		return true;
	}

	public Entry getChild(String name) {
		for (Entry child : children()) {
	        final String childName = child.getName();
			if (childName.isEmpty()) {
				final Entry deepChild = child.getChild(name);
				if (deepChild != null)
					return deepChild;
			}
	        if (name.equals(childName))
				return child;
        }
		return null;
	}

	public Entry findEntry(String name) {
		if (this.name.equals(name))
			return this;
		for (Entry child : this.children()) {
			Entry entry = child.findEntry(name);
			if (entry != null)
				return entry;
		}
		return null;
	}
	
	public List<Entry> findEntries(String name) {
		List<Entry> entries = new ArrayList<Entry>();
		if (this.name.equals(name))
			entries.add(this);
		for (Entry child : this.children()) {
			entries.addAll(child.findEntries(name));
		}
		return entries;
	}

	public boolean isLeaf() {
		return childEntries.isEmpty();
	}

	public Entry getChildByPath(String... names) {
		Entry entry = this;
		for (String name : names) {
			if (!name.isEmpty())
				entry = entry.getChild(name);
			if (entry == null)
				break;
		}
		return entry;
	}
}
