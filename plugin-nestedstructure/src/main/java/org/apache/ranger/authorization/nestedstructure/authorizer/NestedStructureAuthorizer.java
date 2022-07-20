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

/**
 * Consider marking plugin as final, along with replacing static block #42 - #52 with:
 *   static {
 *     plugin = new RangerBasePlugin(RANGER_CMT_SERVICETYPE, RANGER_CMT_APPID);
 *
 *
 * plugin.setResultProcessor(new RangerDefaultAuditHandler(plugin.getConfig()));
 * plugin.init();
 *
 * }
 *
 *
 * Also, consider making plugin a non-static member of NestedStructureAuthorizer,
 * along with all the methods as well. The callers can get access to the instance with a static method
 * NestedStructureAuthorizer.instance().
 * This will enable keeping necessary states in NestedStructureAuthorizer instance.
 */
package org.apache.ranger.authorization.nestedstructure.authorizer;

import org.apache.ranger.plugin.audit.RangerDefaultAuditHandler;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerAccessResult;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class NestedStructureAuthorizer {
    private static volatile RangerBasePlugin plugin = null;
    final private static String RANGER_CMT_SERVICETYPE = "nestedstructure";
    final private static String RANGER_CMT_APPID = "privacera_nestedstructure";
    final static private Logger logger = LoggerFactory.getLogger(NestedStructureAuthorizer.class);

    private NestedStructureAuthorizer() { }

    static {
        plugin = new RangerBasePlugin(RANGER_CMT_SERVICETYPE, RANGER_CMT_APPID);

        plugin.setResultProcessor(new RangerDefaultAuditHandler(plugin.getConfig()));
        plugin.init();

    }

    /**
     *
     * @param schema atlas schema name
     * @param user atlas user name
     * @param userGroups atlas user groups
     * @param json the json of the record to be evaluated
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return if the user is authorized to access this data.  If there is no authorization, null is returned.
     * If there is partial authorization, a modified/masked json blob is returned
     */
    public static AccessResult authorize(String schema, String user, Set<String> userGroups, String json, NestedStructureAccessType accessType) {

        try{
            return privateAuthorize(schema, user, userGroups, json, accessType);
        }catch(Exception e){
            logger.warn("exception during processing, user: " + user + "\n json: " + json, e);
            return new AccessResult(false, null).addError(e);
        }

    }

    private static AccessResult privateAuthorize(String schema, String user, Set<String> userGroups, String json, NestedStructureAccessType accessType){
        if(!hasAnyAccess(schema, user, accessType, userGroups)){
            return new AccessResult(false, null);
        }

        if(!authorizedByRecord(schema, user, userGroups, json, accessType)){
            return new AccessResult(false, null);
        }

        JsonManipulator jsonManipulator = new JsonManipulator(json);
        //check each field individually - both if the user has access and if so, what masking is required
        List<FieldLevelAccess> fieldResults = jsonManipulator.getFields().stream().map(field ->
                hasFieldAccess(schema, user, userGroups, field, accessType)).collect(Collectors.toList());

        //the user must have access to all fields.
        // if the user doesn't have access to one of the fields return an empty/false AccessResult
        if(fieldResults.stream().anyMatch(r -> !r.hasAccess)){
            return new AccessResult(false, null);
        }

        jsonManipulator.maskFields(fieldResults);

        return new AccessResult(true, jsonManipulator.getJsonString());
    }

    /**
     * Checks to see that the user has access to the specific field in this schema
     * @param schema atlas schema name
     * @param user atlas user name
     * @param userGroups atlas user groups
     * @param fld field name
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return a pojo describing access level and masking
     */
    private static FieldLevelAccess hasFieldAccess(String schema, String user, Set<String> userGroups, String fld, NestedStructureAccessType accessType) {
        boolean hasAccess;
        String atlasString = fld
                .replaceAll("\\.\\[\\*\\]\\.'", ".") //removes ".[*]."
                .replaceAll("\\.\\*\\.", "."); //removes ".*."
        NestedStructureResource fldResource = new NestedStructureResource(Optional.of(schema), Optional.of(atlasString));
     //   RangerAccessResource fldResource = new NestedStructureResource(Optional.of("json_object.cxt.cmt.product.vnull3"), Optional.of("partner"));

        RangerAccessRequest fldRequest = new RangerAccessRequestImpl(fldResource, accessType.toString().toLowerCase(), user, userGroups, null);
        RangerAccessResult fldAccessResult = plugin.isAccessAllowed(fldRequest);

        if(fldAccessResult == null){
            throw new MaskingException("unable to determine access");
        }
        hasAccess = fldAccessResult.getIsAccessDetermined() && fldAccessResult.getIsAllowed();
        long fldAccessPolicyId = fldAccessResult.getPolicyId();
        logger.info("checking at line 123 " + accessType + " access to " + schema + "." + fld + " as " + atlasString + " for user: " + user +
                " has access ? " + (hasAccess? "yes":"no") + " policyId:  " + (fldAccessPolicyId));
        if(!hasAccess){
            return new FieldLevelAccess(fld, hasAccess, -1L, true, null, null);
        }

        RangerAccessResult maskResult = plugin.evalDataMaskPolicies(fldRequest, null);
        if(maskResult == null){
            throw new MaskingException("unable to determine access");
        }
        boolean isMasked = maskResult.isMaskEnabled();
        Long maskPolicyId = maskResult.getPolicyId();
        String maskPolicy= "";
        if (isMasked) {maskPolicy = " policyId:  "+ (maskPolicyId);}

        logger.info( "attribute " + fld + " as " + atlasString + " masked ? " + (isMasked? "yes":"no") + maskPolicy);

        return new FieldLevelAccess(fld, hasAccess, maskPolicyId, isMasked,
                maskResult.getMaskType(), maskResult.getMaskedValue());
    }

    /**
     * record-level filtering of schema
     * note that while determining the filter to apply for a table, Apache Ranger policy engine evaluates
     * the policy-items in the order listed in the policy. The filter specified in the first policy-item
     * that matches the access-request (i.e. user/groups) will be used in the query.
     * @param schema atlas schema name
     * @param user atlas user name
     * @param userGroups atlas user groups
     * @param jsonString the json payload that needs to be evaluated
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return if the user is authorized to view this particular record
     */

    private static boolean authorizedByRecord(String schema, String user, Set<String> userGroups, String jsonString, NestedStructureAccessType accessType) {
        boolean hasAccess = true;

        NestedStructureResource filterNestedStructureResource = new NestedStructureResource(Optional.of(schema));

        RangerAccessRequest filterRequest = new RangerAccessRequestImpl(filterNestedStructureResource, accessType.toString().toLowerCase(), user, userGroups, null);
        RangerAccessResult filterResult = plugin.evalRowFilterPolicies(filterRequest, null);

        if(filterResult == null){
            throw new MaskingException("unable to determine access");
        }
        if (filterResult.isRowFilterEnabled()) {
            String filterExpr = filterResult.getFilterExpr();
            logger.info("row level filter enabled with expression: " + filterExpr);

            hasAccess = RecordFilterJavaScript.filterRow(user, filterExpr, jsonString);
        }
        return hasAccess;
    }

    /**
     * Checks to see if this user has any access at all to this schema
     * @param schema atlas schema name
     * @param user atlas user name
     * @param accessType access type requested; must be included in NestedStructureAccessType.
     * @return if the user has access to this schema
     */
    private static boolean hasAnyAccess(String schema, String user, NestedStructureAccessType accessType, Set<String> userGroups) {
        boolean hasAccess;

        NestedStructureResource resource = new NestedStructureResource(Optional.of(schema));

        RangerAccessRequestImpl request = new RangerAccessRequestImpl(resource, accessType.toString().toLowerCase(), user, userGroups, null);
        request.setResourceMatchingScope(RangerAccessRequest.ResourceMatchingScope.SELF_OR_DESCENDANTS);
        RangerAccessResult accessResult = plugin.isAccessAllowed(request);

        if(accessResult == null){
            throw new MaskingException("unable to determine access");
        }
        hasAccess = accessResult.getIsAccessDetermined() && accessResult.getIsAllowed();
        long accessPolicyId = accessResult.getPolicyId();
        logger.debug("checking LINE 202 "+ accessType + " access to " + schema + " for user: " + user + " has access ? "
                + (hasAccess? "yes":"no") + " policyId:  " + (accessPolicyId));
        return hasAccess;
    }

}
