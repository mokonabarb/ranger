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


package org.apache.ranger.authorization.nestedstructure.authorizer;

import java.util.HashSet;
import java.util.Set;

public class ExampleClient {
    public static void main( String[] args) {

        String jsonString = "{\n" +
                "    \"foo\": \"12345678\",\n" +
                "    \"address\": {\n" +
                "      \"city\": \"philadelphia\" \n" +
                "    },\n" +
                "    \"bar\": \"snafu\" \n" +
                "}\n";


        AccessResult ar = NestedStructureAuthorizer.authorize("json_object.foo.v1","someuser",
                    null, jsonString, NestedStructureAccessType.valueOf("READ"));
        String newJsonString = ar.getJson();
        System.out.println("has errors: "+ ar.hasErrors() + "\nhasAccess: "+ ar.hasAccess() +
                "\nnewJsonString in TestClient: "+ newJsonString);
        ar.getErrors().stream().forEach(e-> e.printStackTrace());

        System.out.println("done");
    }
}
