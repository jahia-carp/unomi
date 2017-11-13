/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.unomi.api;

import javax.xml.bind.annotation.XmlTransient;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A consent is an object attached to a profile that indicates whether the profile has agreed or denied a special
 * consent type. For example a user might have agreed to receiving a newsletter but might have not agreed to being
 * tracked.
 */
public class Consent {

    private String typeIdentifier; // type identifiers are defined and managed externally of Apache Unomi
    private ConsentGrant grant;
    private Date grantDate;
    private Date revokeDate;

    /**
     * Empty constructor mostly used for JSON (de-) serialization
     */
    public Consent() {
    }

    /**
     * A constructor to directly build a consent with all it's properties
     * @param typeIdentifier the identifier of the type this consent applies to
     * @param grant the type of grant that we are storing for this consent. May be one of @ConsentGrant.DENY, @ConsentGrant.GRANT, @ConsentGrant.REVOKE
     * @param grantDate the starting date at which this consent was given
     * @param revokeDate the date at which this consent will (automatically) revoke
     */
    public Consent(String typeIdentifier, ConsentGrant grant, Date grantDate, Date revokeDate) {
        this.typeIdentifier = typeIdentifier;
        this.grant = grant;
        this.grantDate = grantDate;
        this.revokeDate = revokeDate;
    }

    /**
     * A constructor from a map used for example when we use the deserialized data from event
     * properties.
     * @param consentMap a Map that contains the following key-value pairs : typeIdentifier:String, grant:String (must
     *                   be one of GRANT, DENY or REVOKE), grantDate:String (ISO8601 date format !), revokeDate:String (ISO8601 date format !)
     * @param dateFormat a DateFormat instance to convert the date string to date objects
     */
    public Consent(Map<String,Object> consentMap, DateFormat dateFormat) throws ParseException {
        if (consentMap.containsKey("typeIdentifier")) {
            setTypeIdentifier((String) consentMap.get("typeIdentifier"));
        }
        if (consentMap.containsKey("grant")) {
            String grantString = (String) consentMap.get("grant");
            setGrant(ConsentGrant.valueOf(grantString));
        }
        if (consentMap.containsKey("grantDate")) {
            String grantDateStr = (String) consentMap.get("grantDate");
            if (grantDateStr != null && grantDateStr.trim().length() > 0) {
                setGrantDate(dateFormat.parse(grantDateStr));
            }
        }
        if (consentMap.containsKey("revokeDate")) {
            String revokeDateStr = (String) consentMap.get("revokeDate");
            if (revokeDateStr != null && revokeDateStr.trim().length() > 0) {
                setRevokeDate(dateFormat.parse(revokeDateStr));
            }
        }
    }

    /**
     * Set the type identifier. This must be (no validation is done) a unique identifier for the consent type. These
     * are usually externally defined, Apache Unomi has no knowledge of them except for this type identifier.
     * @param typeIdentifier a unique String to identify the consent type
     */
    public void setTypeIdentifier(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;
    }

    /**
     * Retrieve the consent type identifier for this consent.
     * @return a String containing the type identifier
     */
    public String getTypeIdentifier() {
        return typeIdentifier;
    }

    /**
     * Retrieves the grant for this consent. This is of type @ConsentGrant
     * @return the current value for the grant.
     */
    public ConsentGrant getGrant() {
        return grant;
    }

    /**
     * Sets the grant for this consent. A Consent Grant of type REVOKE means that this consent is meant to be destroyed.
     * @param grant the grant to set on this consent
     */
    public void setGrant(ConsentGrant grant) {
        this.grant = grant;
    }

    /**
     * Retrieve the date at which this consent was given. If this date is in the future the consent should not be
     * considered valid yet.
     * @return a valid date or null if this date was not set.
     */
    public Date getGrantDate() {
        return grantDate;
    }

    /**
     * Sets the date from which this consent applies.
     * @param grantDate a valid Date or null if we set not starting date (immediately valid)
     */
    public void setGrantDate(Date grantDate) {
        this.grantDate = grantDate;
    }

    /**
     * Retrieves the end date for this consent. After this date the consent is no longer valid and should be disposed of.
     * If this date is not set it means the consent will never expire
     * @return a valid Date or null to indicate an unlimited consent
     */
    public Date getRevokeDate() {
        return revokeDate;
    }

    /**
     * Sets the end date for this consent. After this date the consent is no longer valid and should be disposed of.
     * If this date is not set it means the consent will never expire
     * @param revokeDate a valid Date or null to indicate an unlimited consent
     */
    public void setRevokeDate(Date revokeDate) {
        this.revokeDate = revokeDate;
    }

    /**
     * Test if the consent is GRANTED right now.
     * @return true if the consent is granted using the current date (internally a new Date() is created and the
     * @Consent#isConsentGivenAtDate is called.
     */
    @XmlTransient
    public boolean isConsentGrantedNow() {
        return isConsentGrantedAtDate(new Date());
    }

    /**
     * Tests if the consent is GRANTED at the specified date
     * @param testDate the date against which to test the consent to be granted.
     * @return true if the consent is granted at the specified date, false otherwise.
     */
    @XmlTransient
    public boolean isConsentGrantedAtDate(Date testDate) {
        if (getGrantDate().before(testDate) && (getRevokeDate() == null || (getRevokeDate().after(testDate)))) {
            if (getGrant().equals(ConsentGrant.GRANT)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This is a utility method to generate a Map based on the contents of the consents. The format of the map is the
     * same as the one used in the Map Consent constructor. For dates you must specify a dateFormat that will be used
     * to format the dates. This dateFormat should usually support ISO8601 to make integrate with Javascript clients
     * easy to integrate.
     * @param dateFormat a dateFormat instance such as ISO8601DateFormat to generate the String formats for the grantDate
     *                   and revokeDate map entries.
     * @return a Map that contains the following key-value pairs : typeIdentifier:String, grant:String (must
     *                   be one of GRANT, DENY or REVOKE), grantDate:String (generated by the dateFormat), revokeDate:String (generated by the dateFormat)
     */
    @XmlTransient
    public Map<String,Object> toMap(DateFormat dateFormat) {
        Map<String,Object> map = new LinkedHashMap<>();
        map.put("typeIdentifier", typeIdentifier);
        map.put("grant", grant.toString());
        if (grantDate != null) {
            map.put("grantDate", dateFormat.format(grantDate));
        }
        if (revokeDate != null) {
            map.put("revokeDate", dateFormat.format(revokeDate));
        }
        return map;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Consent{");
        sb.append("typeIdentifier='").append(typeIdentifier).append('\'');
        sb.append(", grant=").append(grant);
        sb.append(", grantDate=").append(grantDate);
        sb.append(", revokeDate=").append(revokeDate);
        sb.append('}');
        return sb.toString();
    }
}
