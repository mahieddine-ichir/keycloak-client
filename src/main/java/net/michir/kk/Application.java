package net.michir.kk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by michir on 10/04/2018.
 */
@SpringBootApplication
@RestController
public class Application {

    RestTemplate restTemplate = new RestTemplate();

    @Value("${keycloak.auth-server-url}")
    String tokenUrl; // = "http://localhost:8180/auth/realms/demo/protocol/openid-connect/token";

    @Value("${keycloak.realm}")
    String realm;

    @Value("${keycloak.credentials.secret}")
    String secret;

    @Value("${redirect_uri}")
    String redirectUri;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping("/secured")
    public ResponseEntity secured() {
        System.out.println("... calling secured!");
        return ResponseEntity.ok().build();
    }

    /*
    @GetMapping("/")
    public String index() {
        return "<body><a href=\"https://public.michir.aws.maileva.net:8443/auth/realms/demo/protocol/openid-connect/auth?response_type=code&client_id=demo\">Login page</body>";
    }
    */

    /**
     * ON http://localhost:8180/auth/realms/demo/protocol/openid-connect/auth?response_type=code&client_id=demo.
     * @param request
     * @return
     */
    @GetMapping("/onlogin")
    public Map onLogin(HttpServletRequest request) throws MalformedURLException {
        String code = request.getParameter("code");
        System.out.println("code="+ code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic "+ Base64.getEncoder().encodeToString((realm+":"+secret).getBytes()));

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> params = new HttpEntity<>(map, headers);

        try {

            String uri = UriComponentsBuilder.fromHttpUrl(tokenUrl)
                    .path("/realms")
                    .path("/"+realm)
                    .path("/protocol/openid-connect/token")
                    .toUriString();

            System.out.println(">> Calling URI "+uri);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(uri, params, Map.class);
            System.out.println("> on auth_code "+responseEntity.getBody());

            Map<String, String> body = responseEntity.getBody();
            String tokenPayload = body.get("access_token").split("\\.")[1];

            Map<String, String> payload = new ObjectMapper().readValue(Base64.getDecoder().decode(tokenPayload.getBytes()), Map.class);
            payload.put("token", body.get("access_token"));

            return payload;

        } catch (HttpStatusCodeException e) {
            System.out.println("CAUSE: "+e.getResponseBodyAsString());
            System.out.println("CODE: "+e.getStatusCode());

            return new HashMap() {
                {
                    put("error", e.getResponseBodyAsByteArray());
                    put("errorCode", e.getStatusCode());
                }
            };

        } catch (Exception e) {
            return new HashMap() {
                {
                    put("error", e.getMessage());
                }
            };

        }
    }
}
