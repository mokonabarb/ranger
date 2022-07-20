
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.ranger.tagsync.nestedstructureplugin;

import junit.framework.TestCase;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.tagsync.source.atlasrest.RangerAtlasEntity;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ResourceTests extends TestCase {

	private AtlasNestedStructureResourceMapper mapper = new AtlasNestedStructureResourceMapper();
	
	@Test
	public void test_ResourceParseFieldName() {
		String resourceStr = "json_object.foo.v1#partner";
        String[] resources   = resourceStr.split("#");
        String   schemaName      = resources.length > 0 ? resources[0] : null;
        String   fieldName     = resources.length > 1 ? resources[1] : null;
        
        
        assertTrue("schemaName does not match expected value", "json_object.foo.v1".equals(schemaName));
        assertTrue("fieldName does not match expected value", "partner".equals(fieldName));
        System.out.println(schemaName);
        System.out.println(fieldName);
	}
	
	@Test
	public void test_ResourceParseSchemaName() {
		String resourceStr = "json_object.foo.v1";
        String[] resources   = resourceStr.split("#");
        String   schemaName      = resources.length > 0 ? resources[0] : null;
        String   fieldName     = resources.length > 1 ? resources[1] : null;
        
        
        assertTrue("schemaName does not match expected value", resourceStr.equals(schemaName));
        assertNull("fieldName does not match expected value", fieldName);
        System.out.println(schemaName);
        System.out.println(fieldName);
	}
	
	
	@Test
	public void test_RangerEntityJsonField() {
		
		String typeName = "json_field";
		String guid		= "0265354542434ff-aewra7297dc";
		
		Map<String, Object> attributes = new HashMap<String, Object>();
		
		String qualifiedName = "json_object.foo.v1#channel";
		
		attributes.put("qualifiedName", qualifiedName);
		
		
		RangerAtlasEntity entity = new RangerAtlasEntity(typeName, guid, attributes);
		
		try {
			RangerServiceResource resource = mapper.buildResource(entity);
			
			assertTrue("Resource elements list is empty", resource.getResourceElements().size()>0);
			assertTrue("Resource elements list size does not match expected", resource.getResourceElements().size()==2);
			
			assertTrue("Resource element missing value for schema", resource.getResourceElements().containsKey("schema"));
			assertTrue("Resource element missing value for field", resource.getResourceElements().containsKey("field"));
			
		}catch(Exception e) {
			e.printStackTrace();
			assertTrue("An error occurred while processing resource", false);
		}
		
		
		
	}
	
	@Test
	public void test_RangerEntityJsonObject() {
		
		String typeName = "json_object";
		String guid		= "9fsdd-sfsrsag-dasd-3fa97";
		
		Map<String, Object> attributes = new HashMap<String, Object>();
		
		String qualifiedName = "json_object.foo.v1";
		
		attributes.put("qualifiedName", qualifiedName);
		
		
		RangerAtlasEntity entity = new RangerAtlasEntity(typeName, guid, attributes);
		
		try {
			RangerServiceResource resource = mapper.buildResource(entity);
			
			assertTrue("Resource elements list is empty", resource.getResourceElements().size()>0);
			assertTrue("Resource elements list size does not match expected", resource.getResourceElements().size()==1);
			
			assertTrue("Resource element missing value for schema", resource.getResourceElements().containsKey("schema"));
			
		}catch(Exception e) {
			e.printStackTrace();
			assertTrue("An error occurred while processing resource", false);
		}
		
		
		
	}
	
}
