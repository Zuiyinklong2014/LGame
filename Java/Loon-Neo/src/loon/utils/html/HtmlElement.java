/**
 * Copyright 2008 - 2019 The Loon Game Engine Authors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.5
 */
package loon.utils.html;

import java.util.Iterator;

import loon.LSysException;
import loon.LSystem;
import loon.utils.Base64Coder;
import loon.utils.ObjectMap;
import loon.utils.StringUtils;
import loon.utils.TArray;

public class HtmlElement {

	private String name;

	private StringBuffer buffer;

	private String tempData;

	private boolean dirty;

	private ObjectMap<String, HtmlAttribute> attributes;

	private TArray<HtmlElement> contents;

	private HtmlElement parent;

	public HtmlElement() {
		this(null);
	}

	public HtmlElement(String name) {
		this.attributes = new ObjectMap<String, HtmlAttribute>();
		this.contents = new TArray<HtmlElement>();
		this.buffer = new StringBuffer(512);
		this.name = name;
		if (this.name != null) {
			this.name = this.name.trim().toLowerCase();
		}
	}

	protected void addData(String d) {
		if (StringUtils.isEmpty(d)) {
			return;
		}
		if (!d.equals(tempData)) {
			this.buffer.append(StringUtils.filter(d, '\r', '\n', '\t'));
		}
		tempData = d;
		dirty = true;
	}

	public String getData() {
		if (dirty) {
			tempData = this.buffer.toString();
			dirty = false;
		}
		return this.tempData;
	}

	public byte[] readContentBinHex() {
		byte[] buffer = new byte[0x1000];
		readBinHex(buffer, 0, 0x1000);
		return buffer;
	}

	public int readBinHex(byte[] buffer, int offset, int length) {
		if (offset < 0) {
			throw new LSysException("Offset must be non-negative integer.");
		} else if (length < 0) {
			throw new LSysException("Length must be non-negative integer.");
		} else if (buffer.length < offset + length) {
			throw new LSysException("buffer length is smaller than the sum of offset and length.");
		}
		if (length == 0) {
			return 0;
		}
		char[] chars = new char[length * 2];
		int charsLength = readValueChunk(chars, 0, length * 2);
		return Base64Coder.fromBinHexString(chars, offset, charsLength, buffer);
	}

	private int readValueChunk(char[] buffer, int offset, int length) {
		StringBuffer textCache = new StringBuffer(length);
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			textCache.append(e.next().toString());
		}
		int min = textCache.length();
		if (min > length) {
			min = length;
		}
		String str = textCache.substring(0, min);
		System.arraycopy(str.toCharArray(), offset, buffer, 0, length);
		if (min < length) {
			return min + readValueChunk(buffer, offset + min, length - min);
		} else {
			return min;
		}
	}

	public HtmlAttribute getAttribute(String name) {
		if (!this.attributes.containsKey(name))
			throw new LSysException("Unknown attribute name '" + name + "' in element '" + this.name + "' !");
		return this.attributes.get(name);
	}

	public String getAttribute(String name, String v) {
		if (!this.attributes.containsKey(name)) {
			return v;
		}
		return (this.attributes.get(name)).getValue();
	}

	public int getIntAttribute(String name, int v) {
		if (!this.attributes.containsKey(name)) {
			return v;
		}
		return (this.attributes.get(name)).getIntValue();
	}

	public float getFloatAttribute(String name, float v) {
		if (!this.attributes.containsKey(name)) {
			return v;
		}
		return (this.attributes.get(name)).getFloatValue();
	}

	public double getDoubleAttribute(String name, double v) {
		if (!this.attributes.containsKey(name)) {
			return v;
		}
		return (this.attributes.get(name)).getDoubleValue();
	}

	public boolean getBoolAttribute(String name, boolean v) {
		if (!this.attributes.containsKey(name)) {
			return v;
		}
		return (this.attributes.get(name)).getBoolValue();
	}

	public ObjectMap<String, HtmlAttribute> getAttributes() {
		return this.attributes;
	}

	public boolean hasAttribute(String name) {
		return this.attributes.containsKey(name);
	}

	public int childCount() {
		return this.contents == null ? 0 : this.contents.size;
	}

	public Iterator<HtmlElement> elements() {
		return this.contents.iterator();
	}

	public TArray<HtmlElement> list() {
		TArray<HtmlElement> lists = new TArray<HtmlElement>(contents.size);
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement o = e.next();
			lists.add(o);
		}
		return lists;
	}

	public HtmlElement getFirstChild() {
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement o = e.next();
			return (HtmlElement) o;
		}
		return null;
	}

	public HtmlElement getChildrenByName(String name) {
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement o = e.next();
			if (!o.getName().equals(name)) {
				continue;
			}
			return (HtmlElement) o;
		}
		return null;
	}

	public TArray<HtmlElement> all(String name) {
		if (childCount() == 0) {
			return new TArray<HtmlElement>();
		}
		TArray<HtmlElement> result = new TArray<HtmlElement>();
		TArray<HtmlElement> looper = contents;
		while (looper.size > 0) {
			TArray<HtmlElement> next = new TArray<HtmlElement>();
			for (HtmlElement node : looper) {
				if (name.equals(node.getName())) {
					result.add(node);
				}
				next.addAll(node.contents);
			}
			looper = next;
		}
		return result;
	}

	public TArray<HtmlElement> find(String name) {
		TArray<HtmlElement> v = new TArray<HtmlElement>();
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement ele = e.next();
			if (!ele.equals(ele.getName())) {
				Iterator<HtmlElement> it = ele.elements(name);
				for (; it.hasNext();) {
					HtmlElement child = it.next();
					child.parent = ele;
					v.add(child);
				}
				continue;
			} else if (ele.equals(ele.getName())) {
				v.add(ele);
				continue;
			}
		}
		return v;
	}

	public TArray<HtmlElement> list(String name) {
		TArray<HtmlElement> v = new TArray<HtmlElement>();
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement o = e.next();
			if (!o.getName().equals(name)) {
				continue;
			}
			v.add(o);
		}
		return v;
	}

	public Iterator<HtmlElement> elements(String name) {
		TArray<HtmlElement> v = new TArray<HtmlElement>();
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement o = e.next();
			if (!o.getName().equals(name)) {
				continue;
			}
			v.add(o);
		}
		return v.iterator();
	}

	public void addContents(HtmlElement el) {
		contents.add(el);
	}

	public void addAllTo(TArray<HtmlElement> list) {
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			HtmlElement o = e.next();
			if ((!(o instanceof HtmlElement)) || (!((HtmlElement) o).getName().equals(name))) {
				continue;
			}
			list.add((HtmlElement) o);
		}
	}

	public String getName() {
		return this.name;
	}

	public HtmlElement getParent() {
		return this.parent;
	}

	public String getContents() {
		StringBuffer sbr = new StringBuffer(1024);
		for (Iterator<HtmlElement> e = elements(); e.hasNext();) {
			sbr.append(e.next().toString());
		}
		return sbr.toString();
	}

	public HtmlAttribute addAttribute(String name, int value) {
		HtmlAttribute attribute = new HtmlAttribute(name, String.valueOf(value));
		this.attributes.put(name, attribute);
		return attribute;
	}

	public HtmlAttribute addAttribute(String name, String value) {
		HtmlAttribute attribute = new HtmlAttribute(name, value);
		this.attributes.put(name, attribute);
		return attribute;
	}

	public HtmlFont getFont() {
		TArray<HtmlFont> fonts = getFonts();
		if (fonts.size > 0) {
			return fonts.get(0);
		}
		return null;
	}

	public TArray<HtmlFont> getFonts() {
		TArray<HtmlFont> fonts = new TArray<HtmlFont>();
		TArray<HtmlElement> eles = all("font");
		for (HtmlElement e : eles) {
			fonts.add(new HtmlFont(e));
		}
		return fonts;
	}

	public HtmlImage getImage() {
		TArray<HtmlImage> images = getImages();
		if (images.size > 0) {
			return images.get(0);
		}
		return null;
	}

	public TArray<HtmlImage> getImages() {
		TArray<HtmlImage> images = new TArray<HtmlImage>();
		TArray<HtmlElement> eles = all("img");
		for (HtmlElement e : eles) {
			images.add(new HtmlImage(e));
		}
		return images;
	}

	public boolean isBody() {
		return "body".equals(name);
	}

	public boolean isHead() {
		return "head".equals(name);
	}

	public boolean isHtml() {
		return "html".equals(name);
	}

	public boolean isButton() {
		return "button".equals(name);
	}

	public boolean isTable() {
		return "table".equals(name);
	}

	public boolean isFont() {
		return "font".equals(name);
	}

	public boolean isImg() {
		return "img".equals(name);
	}

	public boolean isLink() {
		return "link".equals(name);
	}

	public boolean isBr() {
		return "br".equals(name);
	}

	public boolean isTr() {
		return "tr".equals(name);
	}

	public boolean isA() {
		return "a".equals(name);
	}

	public boolean isB() {
		return "b".equals(name);
	}

	public boolean isP() {
		return "p".equals(name);
	}

	@Override
	public String toString() {
		StringBuffer builder = new StringBuffer();
		if (this.name == null) {
			builder.append(LSystem.EMPTY);
		} else {
			builder.append('<');
			builder.append(this.name);
		}
		for (String str2 : attributes.keys()) {
			builder.append(' ');
			builder.append(str2);
			builder.append(" = \"");
			builder.append(getAttribute(str2).getValue());
			builder.append("\"");
		}
		if (this.name != null) {
			builder.append(">\n");
		}
		String data = getData();
		if (data != null) {
			builder.append(data);
		}
		builder.append(getContents());
		if (this.name != null) {
			builder.append("</" + this.name + ">\n");
		}
		return builder.toString();
	}

	public HtmlElement dispose() {
		if (attributes != null) {
			attributes.clear();
			attributes = null;
		}
		if (contents != null) {
			contents.clear();
			contents = null;
		}
		return this;
	}
}
