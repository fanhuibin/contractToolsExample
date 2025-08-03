/**
 *
 * (c) Copyright Ascensio System SIA 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.zhaoxinms.contract.tools.onlyoffice.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class DefaultJwtManager implements JwtManager {
    @Value("${onlyoffice.secret}")
    private String tokenSecret;
    private ObjectMapper objectMapper = new ObjectMapper();
    private JSONParser parser = new JSONParser();

    // create document token
    public String createToken(Map<String, Object> payloadClaims) {
        try {
            // 使用jjwt创建JWT
            SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
            
            return Jwts.builder()
                    .setClaims(payloadClaims)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            return "";
        }
    }

    // check if the token is enabled
    public boolean tokenEnabled() {
        return tokenSecret != null && !tokenSecret.isEmpty();
    }

    // read document token
    public Map<String, Object> readToken(String token) {
        try {
            // 使用jjwt验证和解析JWT
            SecretKey key = Keys.hmacShaKeyFor(tokenSecret.getBytes(StandardCharsets.UTF_8));
            
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            return new LinkedHashMap<>(claims);
        } catch (Exception exception) {
            return null;
        }
    }

    // parse the body
    public JSONObject parseBody(String payload, String header) {
        JSONObject body;
        try {
            Object obj = parser.parse(payload); // get body parameters by parsing the payload
            body = (JSONObject)obj;
        } catch (Exception ex) {
            throw new RuntimeException("{\"error\":1,\"message\":\"JSON Parsing error\"}");
        }
        if (tokenEnabled()) { // check if the token is enabled
            String token = (String)body.get("token"); // get token from the body
            if (token == null) { // if token is empty
                if (header != null && !header.isBlank()) { // and the header is defined
                    token = header.startsWith("Bearer ") ? header.substring(7) : header; // get token from the header
                                                                                         // (it is placed after the
                                                                                         // Bearer prefix if it
                                                                                         // exists)
                }
            }
            if (token == null || token.isBlank()) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT expected\"}");
            }

            Map<String, Object> jwt = readToken(token); // read token
            if (jwt == null) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }
            if (jwt.get("payload") != null) { // get payload from the token and check if it is not empty
                try {
                    Object payloadObj = jwt.get("payload");
                    if (payloadObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> payloadMap = (Map<String, Object>) payloadObj;
                        for (String key : payloadMap.keySet()) {
                            body.put(key, payloadMap.get(key));
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("{\"error\":1,\"message\":\"JWT payload parsing failed\"}");
                }
            }
        }
        return body;
    }
}
