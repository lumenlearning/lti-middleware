/**
 * Copyright 2021 Unicon (R)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.unicon.lti.service.lti.impl;

import lombok.extern.slf4j.Slf4j;
import net.unicon.lti.exceptions.ConnectionException;
import net.unicon.lti.exceptions.DataServiceException;
import net.unicon.lti.exceptions.helper.ExceptionMessageGenerator;
import net.unicon.lti.model.LtiContextEntity;
import net.unicon.lti.model.PlatformDeployment;
import net.unicon.lti.model.ags.LineItem;
import net.unicon.lti.model.ags.LineItems;
import net.unicon.lti.model.ags.Result;
import net.unicon.lti.model.ags.Results;
import net.unicon.lti.model.ags.Score;
import net.unicon.lti.model.oauth2.LTIAdvantageToken;
import net.unicon.lti.service.lti.AdvantageAGSService;
import net.unicon.lti.service.lti.AdvantageConnectorHelper;
import net.unicon.lti.utils.AGSScope;
import net.unicon.lti.utils.TextConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This manages all the Membership call for the LTIRequest (and for LTI in general)
 * Necessary to get appropriate TX handling and service management
 */
@Slf4j
@Service
public class AdvantageAGSServiceImpl implements AdvantageAGSService {

    @Autowired
    AdvantageConnectorHelper advantageConnectorHelper;

    @Autowired
    private ExceptionMessageGenerator exceptionMessageGenerator;

    //Asking for a token with the right scope.
    @Override
    public LTIAdvantageToken getToken(AGSScope agsScope, PlatformDeployment platformDeployment) throws ConnectionException {
        return advantageConnectorHelper.getToken(platformDeployment, agsScope.getScope());
    }

    //Calling the AGS service and getting a paginated result of lineitems.
    @Override
    public LineItems getLineItems(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context) throws ConnectionException {
        return getLineItems(LTIAdvantageToken,context, false, null);
    }

    public LineItems getLineItems(PlatformDeployment platformDeployment, String lineitemsUrl) throws ConnectionException, DataServiceException {
        if (platformDeployment == null) {
            throw new DataServiceException("PlatformDeployment must not be null in order to get Line Items");
        }
        if (lineitemsUrl == null || lineitemsUrl.isEmpty()) {
            throw new DataServiceException("LineitemsUrl must not be null or empty in order to get Line Items");
        }
        LineItems lineItems = new LineItems();

        LTIAdvantageToken LTIAdvantageToken = getToken(AGSScope.AGS_LINEITEMS_SCOPE, platformDeployment);
        log.debug(TextConstants.TOKEN + LTIAdvantageToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();

            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageToken, TextConstants.ALL_LINEITEMS_TYPE);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            log.debug("GET_LINEITEMS -  " + lineitemsUrl);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.exchange(lineitemsUrl, HttpMethod.GET, request, LineItem[].class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(lineItemsGetResponse.getBody())));
                //We deal here with pagination
                log.debug("We have {} lineItems", lineItemsList.size());
                String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    nextPage = nextPage.startsWith("http:") ? nextPage.replace("http:", "https:") : nextPage; // For D2L bug
                    ResponseEntity<LineItem[]> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET, request, LineItem[].class);
                    LineItem[] nextLineItemsList = responseForNextPage.getBody();
                    log.debug("We have {} lineitems in the next page", nextLineItemsList.length);
                    lineItemsList.addAll(Arrays.asList(nextLineItemsList));
                    nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                }
                lineItems.getLineItemList().addAll(lineItemsList);
            } else {
                String exceptionMsg = status + " status from AGS: Can't fetch the lineitems";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Exception Thrown: Can't fetch the lineitems from AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        log.debug("We have {} total lineItems", lineItems.getLineItemList().size());
        return lineItems;
    }

    @Override // Get Line Items for Demo Mode
    public LineItems getLineItems(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, boolean results, LTIAdvantageToken resultsToken) throws ConnectionException {
        LineItems lineItems = new LineItems();
        log.debug(TextConstants.TOKEN + LTIAdvantageToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageToken, TextConstants.ALL_LINEITEMS_TYPE);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String GET_LINEITEMS = context.getLineitems();
            log.debug("GET_LINEITEMS -  " + GET_LINEITEMS);
            ResponseEntity<LineItem[]> lineItemsGetResponse = restTemplate.exchange(GET_LINEITEMS, HttpMethod.GET, request, LineItem[].class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                List<LineItem> lineItemsList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(lineItemsGetResponse.getBody())));
                //We deal here with pagination
                log.debug("We have {} lineItems", lineItems.getLineItemList().size());
                String nextPage = advantageConnectorHelper.nextPage(lineItemsGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    ResponseEntity<LineItem[]> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                            request, LineItem[].class);
                    LineItem[] nextLineItemsList = responseForNextPage.getBody();
                    //List<LineItem> nextLineItems = nextLineItemsList.getLineItemList();
                    log.debug("We have {} lineitems in the next page", nextLineItemsList.length);
                    lineItemsList.addAll(Arrays.asList(nextLineItemsList));
                    nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                }
                lineItems.getLineItemList().addAll(lineItemsList);
            } else {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
            if (results) {
                for (LineItem lineItem : lineItems.getLineItemList()) {
                    Results results1 = getResults(resultsToken, context, lineItem.getId());
                    lineItem.setResults(results1);
                }
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return lineItems;
    }

    @Override
    public boolean deleteLineItem(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, String id) throws ConnectionException {
        log.debug(TextConstants.TOKEN + LTIAdvantageToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageToken);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String DELETE_LINEITEM = context.getLineitems() + "/" + id;
            log.debug("DELETE_LINEITEM -  " + DELETE_LINEITEM);
            ResponseEntity<String> lineItemsGetResponse = restTemplate.
                    exchange(DELETE_LINEITEM, HttpMethod.DELETE, request, String.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                return true;
            } else {
                String exceptionMsg = "Can't delete the lineitem with id: " + id;
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't delete the lineitem with id").append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public LineItem putLineItem(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {
        log.debug(TextConstants.TOKEN + LTIAdvantageToken.getAccess_token());
        LineItem resultlineItem;
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<LineItem> request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageToken, lineItem, TextConstants.LINEITEM_TYPE);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String PUT_LINEITEM = context.getLineitems() + "/" + lineItem.getId();
            log.debug("PUT_LINEITEM -  " + PUT_LINEITEM);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.
                    exchange(PUT_LINEITEM, HttpMethod.PUT, request, LineItem.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                resultlineItem = lineItemsGetResponse.getBody();
                //We deal here with pagination
            } else {
                String exceptionMsg = "Can't put the lineitem " + lineItem.getId();
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get put lineitem ").append(lineItem.getId());
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return resultlineItem;
    }

    @Override
    public LineItem getLineItem(LTIAdvantageToken LTIAdvantageToken, LTIAdvantageToken resultsToken, LtiContextEntity context, String id) throws ConnectionException {
        LineItem lineItem;
        log.debug(TextConstants.TOKEN + LTIAdvantageToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageToken, TextConstants.LINEITEM_TYPE);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            log.debug("Getting lineItem with id: {}", id);
            String getLineItemUrl;
            String lineItemsUrl = context.getLineitems();
            int lineItemsUrlQIdx = lineItemsUrl.indexOf("?");
            if (lineItemsUrlQIdx > 0) { // if Moodle
                String params = lineItemsUrl.substring(lineItemsUrlQIdx);
                getLineItemUrl = lineItemsUrl.substring(0, lineItemsUrlQIdx) + "/" + id + "/lineitem" + params;
            } else {
                getLineItemUrl = lineItemsUrl + "/" + id;
            }
            log.debug("GET_LINEITEM -  " + getLineItemUrl);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.exchange(getLineItemUrl, HttpMethod.GET, request, LineItem.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                lineItem = lineItemsGetResponse.getBody();
                Results results1 = getResults(resultsToken, context, lineItem.getId());
                lineItem.setResults(results1);
            } else {
                String exceptionMsg = "Can't get the lineitem " + id;
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the lineitem ").append(id);
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return lineItem;
    }

    @Override
    public LineItems postLineItem(LTIAdvantageToken LTIAdvantageToken, LtiContextEntity context, LineItem lineItem) throws ConnectionException {

        LineItems resultLineItems = new LineItems();
        log.debug(TextConstants.TOKEN + LTIAdvantageToken.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<LineItem> request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageToken, lineItem, TextConstants.LINEITEM_TYPE);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String POST_LINEITEMS = context.getLineitems();
            log.debug("POST_LINEITEMS -  " + POST_LINEITEMS);
            ResponseEntity<LineItem> lineItemsGetResponse = restTemplate.
                    exchange(POST_LINEITEMS, HttpMethod.POST, request, LineItem.class);
            HttpStatus status = lineItemsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                LineItem lineItems =lineItemsGetResponse.getBody();
                resultLineItems.getLineItemList().add(lineItems);
            } else {
                String exceptionMsg = "Can't post lineitem";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post lineitem");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return resultLineItems;
    }

    @Override
    public Results getResults(LTIAdvantageToken LTIAdvantageTokenResults, LtiContextEntity context, String lineItemId) throws ConnectionException {
        Results results = new Results();
        log.debug(TextConstants.TOKEN + LTIAdvantageTokenResults.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity request = advantageConnectorHelper.createTokenizedRequestEntity(LTIAdvantageTokenResults, TextConstants.RESULTS_TYPE);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            String getResultsUrl;
            int lineItemsUrlQIdx = lineItemId.indexOf("?");
            if (lineItemsUrlQIdx > 0) { // if Moodle
                String params = lineItemId.substring(lineItemsUrlQIdx);
                getResultsUrl = lineItemId.substring(0, lineItemsUrlQIdx) + "/results" + params;
            } else {
                getResultsUrl = lineItemId + "/results"; 
            }
            log.debug("getResultsUrl -  " + getResultsUrl  + "/" + lineItemId + "/results");
            ResponseEntity<Result[]> resultsGetResponse = restTemplate.exchange(getResultsUrl, HttpMethod.GET, request, Result[].class);
            HttpStatus status = resultsGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                List<Result> resultList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(resultsGetResponse.getBody())));
                //We deal here with pagination
                log.debug("We have {} results", results.getResultList().size());
                String nextPage = advantageConnectorHelper.nextPage(resultsGetResponse.getHeaders());
                log.debug("We have next page: " + nextPage);
                while (nextPage != null) {
                    ResponseEntity<Results> responseForNextPage = restTemplate.exchange(nextPage, HttpMethod.GET,
                            request, Results.class);
                    Results nextResultsList = responseForNextPage.getBody();
                    List<Result> nextResults = nextResultsList
                            .getResultList();
                    log.debug("We have {} results in the next page", nextResultsList.getResultList().size());
                    resultList.addAll(nextResults);
                    nextPage = advantageConnectorHelper.nextPage(responseForNextPage.getHeaders());
                }
                results.getResultList().addAll(resultList);
            } else {
                String exceptionMsg = "Can't get the AGS";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't get the AGS");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
        return results;
    }

    @Override
    public Results postScore(LTIAdvantageToken lTIAdvantageTokenScores, LTIAdvantageToken lTIAdvantageTokenResults, LtiContextEntity context, String lineItemId, Score score) throws ConnectionException {
        log.debug(TextConstants.TOKEN + lTIAdvantageTokenScores.getAccess_token());
        try {
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();
            //We add the token in the request with this.
            HttpEntity<Score> request = advantageConnectorHelper.createTokenizedRequestEntity(lTIAdvantageTokenScores, score);
            //The URL to get the course contents is stored in the context (in our database) because it came
            // from the platform when we created the link to the context, and we saved it then.
            final String POST_SCORES = lineItemId + "/scores";
            log.debug("POST_SCORES -  " + POST_SCORES);
            ResponseEntity<Void> scoreGetResponse = restTemplate.exchange(POST_SCORES, HttpMethod.POST, request, Void.class);
            HttpStatus status = scoreGetResponse.getStatusCode();
            if (status.is2xxSuccessful()) {
                return getResults(lTIAdvantageTokenResults, context, lineItemId);
            } else {
                String exceptionMsg = "Can't post scores";
                log.error(exceptionMsg);
                throw new ConnectionException(exceptionMsg);
            }
        } catch (Exception e) {
            StringBuilder exceptionMsg = new StringBuilder();
            exceptionMsg.append("Can't post scores");
            log.error(exceptionMsg.toString(), e);
            throw new ConnectionException(exceptionMessageGenerator.exceptionMessage(exceptionMsg.toString(), e));
        }
    }

    @Override
    public ResponseEntity<Void> postScore(LTIAdvantageToken lTIAdvantageTokenScores, String lineItemId, Score score) {
            log.debug(TextConstants.TOKEN + lTIAdvantageTokenScores.getAccess_token());
            RestTemplate restTemplate = advantageConnectorHelper.createRestTemplate();

            // Add the token and score to the request entity
            HttpEntity<Score> request = advantageConnectorHelper.createTokenizedRequestEntity(lTIAdvantageTokenScores, score);

            String postScoresUrl;
            int lineItemsUrlQIdx = lineItemId.indexOf("?");
            if (lineItemsUrlQIdx > 0) { // if Moodle
                String params = lineItemId.substring(lineItemsUrlQIdx);
                postScoresUrl = lineItemId.substring(0, lineItemsUrlQIdx) + "/scores" + params;
            } else {
                postScoresUrl = lineItemId + "/scores";
            }

            log.debug("POST_SCORES -  " + postScoresUrl);
            ResponseEntity<Void> postScoreResponse = restTemplate.exchange(postScoresUrl, HttpMethod.POST, request, Void.class);
            return postScoreResponse;
    }

    @Override
    public void cleanLineItem(LineItem lineItem){
        if (StringUtils.isBlank(lineItem.getResourceId())){
            lineItem.setResourceId(null);
        }
        if (StringUtils.isBlank(lineItem.getResourceLinkId())){
            lineItem.setResourceLinkId(null);
        }
        if (StringUtils.isBlank(lineItem.getStartDateTime())){
            lineItem.setStartDateTime(null);
        }
        if (StringUtils.isBlank(lineItem.getEndDateTime())){
            lineItem.setEndDateTime(null);
        }
        if (StringUtils.isBlank(lineItem.getTag())){
            lineItem.setTag(null);
        }

    }

}
