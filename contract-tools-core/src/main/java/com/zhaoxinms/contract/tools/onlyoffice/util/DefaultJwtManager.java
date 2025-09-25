package com.zhaoxinms.contract.tools.onlyoffice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.config.ZxcmConfig;
import io.fusionauth.jwt.Signer;
import io.fusionauth.jwt.Verifier;
import io.fusionauth.jwt.domain.JWT;
import io.fusionauth.jwt.hmac.HMACSigner;
import io.fusionauth.jwt.hmac.HMACVerifier;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DefaultJwtManager implements JwtManager {
    @Autowired
    private ZxcmConfig zxcmConfig;
    private ObjectMapper objectMapper = new ObjectMapper();
    private JSONParser parser = new JSONParser();

    // create document token
    public String createToken(Map<String, Object> payloadClaims) {
        try {
            // build a HMAC signer using a SHA-256 hash
            Signer signer = HMACSigner.newSHA256Signer(getSecret());
            JWT jwt = new JWT();
            for (String key : payloadClaims.keySet()) { // run through all the keys from the payload
                jwt.addClaim(key, payloadClaims.get(key)); // and write each claim to the jwt
            }
            return JWT.getEncoder().encode(jwt, signer); // sign and encode the JWT to a JSON string representation
        } catch (Exception e) {
            return "";
        }
    }

    // check if the token is enabled
    public boolean tokenEnabled() {
        String secret = getSecret();
        return secret != null && !secret.isEmpty();
    }

    // read document token
    public JWT readToken(String token) {
        try {
            // build a HMAC verifier using the token secret
            Verifier verifier = HMACVerifier.newVerifier(getSecret());
            return JWT.getDecoder().decode(token, verifier); // verify and decode the encoded string JWT to a rich
                                                             // object
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

            JWT jwt = readToken(token); // read token
            if (jwt == null) {
                throw new RuntimeException("{\"error\":1,\"message\":\"JWT validation failed\"}");
            }
            if (jwt.getObject("payload") != null) { // get payload from the token and check if it is not empty
                try {
                    @SuppressWarnings("unchecked")
                    LinkedHashMap<String, Object> jwtPayload = (LinkedHashMap<String, Object>)jwt.getObject("payload");

                    for(Map.Entry<String, Object> entry : jwtPayload.entrySet()) {
                        body.put(entry.getKey(), entry.getValue());
                    }
                } catch (Exception ex) {
                    throw new RuntimeException("{\"error\":1,\"message\":\"Wrong payload\"}");
                }
            }
        }
        return body;
    }

    private String getSecret() {
        return zxcmConfig.getOnlyOffice().getSecret();
    }
}