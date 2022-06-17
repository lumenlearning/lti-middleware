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
package net.unicon.lti.service.waymaker;

import net.unicon.lti.model.waymaker.WaymakerCourse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * This manages all the access to data related with the main APP
 */
@Service
@Slf4j
public class WaymakerService {

    private final ObjectMapper jsonObjectMapper = new ObjectMapper().enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @Value("${waymaker.courses.api}")
    private String waymakerCoursesApiUrl;

    @Value("${waymaker.courses.jwt}")
    private String waymakerJWT;

    public List<WaymakerCourse> fetchWaymakerCourses() {

        if (StringUtils.isAnyBlank(waymakerCoursesApiUrl, waymakerJWT)) {
            log.warn("The Waymaker Courses API has not been configured, courses will not be fetched.");
            return new ArrayList<>();
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Build the URL
            URIBuilder builder = new URIBuilder(waymakerCoursesApiUrl);
            // Build the GET object
            HttpGet httpGet = new HttpGet(builder.build());
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + waymakerJWT);
            httpGet.addHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
            httpGet.addHeader(HttpHeaders.CONNECTION, "keep-alive");
            httpGet.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
            // Some requests need a special mimeType in the ACCEPT header.
            httpGet.addHeader(HttpHeaders.ACCEPT, "*/*");
            // Perform the request
            try (CloseableHttpResponse apiResponse = httpClient.execute(httpGet)) {
                if (HttpStatus.SC_OK == apiResponse.getStatusLine().getStatusCode()) {
                    HttpEntity entity = apiResponse.getEntity();
                    return jsonObjectMapper.readValue(entity.getContent(), new TypeReference<List<WaymakerCourse>>(){});
                }
            }
        } catch(Exception ex) {
            log.error("Error fetching courses from the Waymaker Courses API: {} ", ex.getMessage());
        }

        return new ArrayList<>();

    }

}
