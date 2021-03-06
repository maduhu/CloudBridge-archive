/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.util;

import java.io.PrintWriter;
import java.lang.reflect.Field;

/**
 * @author Kelven Yang
 */
public interface XSerializerAdapter {
	void setSerializer(XSerializer serializer);
	
	void beginElement(String element, String namespace, int indentLevel, PrintWriter writer);
	void endElement(String element, int indentLevel, PrintWriter writer);
	
	void writeElement(String elementName, String itemName, Object fieldValue, Field f, int indentLevel, PrintWriter writer);
	void writeSeparator(int indentLevel, PrintWriter writer);
}
