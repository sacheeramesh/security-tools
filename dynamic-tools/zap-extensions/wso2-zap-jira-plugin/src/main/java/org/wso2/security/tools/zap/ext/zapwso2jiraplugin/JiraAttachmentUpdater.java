/*
 *
 *  * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *  *
 *  * WSO2 Inc. licenses this file to you under the Apache License,
 *  * Version 2.0 (the "License"); you may not use this file except
 *  * in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing,
 *  * software distributed under the License is distributed on an
 *  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  * KIND, either express or implied.  See the License for the
 *  * specific language governing permissions and limitations
 *  * under the License.
 *
 */

package org.wso2.security.tools.zap.ext.zapwso2jiraplugin;

import org.json.JSONObject;

import javax.naming.AuthenticationException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

public class JiraAttachmentUpdater {

    JiraRestClient jiraRest;
    Map<String, String> attachments = new HashMap<String, String>();
    private Logger log = Logger.getLogger(this.getClass());

    /**
     * This method is used modify the Jira attachment which includes maintaining only up to  attachments in a ticket
     * @param auth -Base64 encoded value of Jirausername and JiraPassword
     * @param BASE_URL jira base URL
     * @param jiraKey is the key to which the modifications need to be done
     * @return
     */
    public void modifyJiraContents(String auth, String BASE_URL, String jiraKey) {

        String URL = BASE_URL + IssueCreatorConstants.ACCESS_JIRA_ISSUES_ENDPOINT + jiraKey;
        String responseIssuue;
        JSONObject availableIssue = null;
        HashMap<String, Date> jiraAttachments = new HashMap<>();

        try {
            responseIssuue = jiraRest.invokeGetMethod(auth, URL);
            availableIssue = new JSONObject(responseIssuue);
        } catch (AuthenticationException e) {
            log.error("Authentation parameters are invalid",e);
        }

        try {
            if (availableIssue.getJSONObject("fields").getJSONArray("attachment").length() >= 4) {

                int length = availableIssue.getJSONObject("fields").getJSONArray("attachment").length();

                for (int i = 0; i < length; i++) {
                    String key = availableIssue.getJSONObject("fields").getJSONArray("attachment").getJSONObject(i)
                            .getString("id");
                    String dateValue = availableIssue.getJSONObject("fields").getJSONArray("attachment")
                            .getJSONObject(i).getString("created");

                    dateValue = dateValue.replace("T", " ");
                    dateValue = dateValue.substring(0, 18);

                    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date createdDate = fmt.parse(dateValue);
                    jiraAttachments.put(key, createdDate);
                }

                jiraAttachments = sortHashMapByValues(jiraAttachments);

                Iterator<Map.Entry<String, Date>> jiraAttachment = jiraAttachments.entrySet().iterator();
                int count = length;

                while (jiraAttachment.hasNext()) {
                    count--;
                    String key = jiraAttachment.next().getKey();

                    //This code segment is to maintain only 5 attachments in the jira. Delete the attachments from jira
                    //which takes the count more than 4, the new attachment will be assigned as the 5th one
                    if (count >= 4) {
                        deleteAttachment(auth, BASE_URL, key);
                        jiraAttachment.remove();
                    } else {
                        break;
                    }

                }
            }
        }  catch (ParseException e) {
            log.error("Failed to parse the json object",e);
        }
    }


    /**
     * This method is used delete the specified attachment from a ticket
     * @param auth -Base64 encoded value of Jirausername and JiraPassword
     * @param BASE_URL jira base URL
     * @param id is the attachment which is need to be deleted
     * @return
     */

    public void deleteAttachment(String auth, String BASE_URL, String id) {

        String url = BASE_URL + IssueCreatorConstants.ACCESS_JIRA_ATTACHMENT_ENDPOINT + id;
        String responseIssuue;

        try {
            jiraRest.invokeDeleteMethod(auth, url);
        } catch (AuthenticationException e) {
            log.error("Authentation parameters are invalid",e);
        }

    }

    /**
     * This method is used sort the attachment in a specified tickets in an ascending order
     * @param passedMap authentication parameters to the Jira
     * @return sorted map of the jira attachments according to date
     */
    public LinkedHashMap<String, Date> sortHashMapByValues(HashMap<String, Date> passedMap) {

        List<String> mapKeys = new ArrayList<>(passedMap.keySet());
        List<Date> mapValues = new ArrayList<>(passedMap.values());

        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<String, Date> sortedMap = new LinkedHashMap<>();

        Iterator<Date> valueIt = mapValues.iterator();

        while (valueIt.hasNext()) {
            Date val = valueIt.next();

            Iterator<String> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                Date comp1 = passedMap.get(key);
                Date comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, val);
                    break;
                }
            }
        }
        return sortedMap;
    }
}
