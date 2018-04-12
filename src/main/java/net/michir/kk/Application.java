package net.michir.kk;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Base64;
import java.util.Collections;
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

    @GetMapping("/")
    public String index() {
        return "<body><a href=\"https://public.michir.aws.maileva.net:8443/auth/realms/demo/protocol/openid-connect/auth?response_type=code&client_id=demo\">Login page</body>";
    }

    /**
     * ON http://localhost:8180/auth/realms/demo/protocol/openid-connect/auth?response_type=code&client_id=demo.
     * @param request
     * @return
     */
    @GetMapping("/onlogin")
    @ResponseBody
    public Map onLogin(HttpServletRequest request) throws MalformedURLException {
        String code = request.getParameter("code");
        System.out.println("code="+ code);

        /*
        restTemplate.setInterceptors(Collections.singletonList(new ClientHttpRequestInterceptor() {
            @Override
            public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
                request.getHeaders().set("Authorization", "Basic "+ Base64.getEncoder().encodeToString("demo:2b68d8cd-089e-4fef-a55c-8121a661eef1".getBytes()));
                return execution.execute(request, body);
            }
        }));
        */

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic "+ Base64.getEncoder().encodeToString((realm+":"+secret).getBytes()));

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectUri);
        //map.add("redirect_uri", ServletUriComponentsBuilder.fromCurrentRequest().toUriString());

        HttpEntity<MultiValueMap<String, String>> params = new HttpEntity<>(map, headers);

/*
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        */
        try {

            String uri = UriComponentsBuilder.fromHttpUrl(tokenUrl)
                    .path("/realms")
                    .path("/"+realm)
                    .path("/protocol/openid-connect/token")
                    //.queryParam("response_type", "code")
                    //.queryParam("client_id", realm)
                    .toUriString();

            System.out.println(">> Calling URI "+uri);

            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(uri, params, Map.class);
            //ResponseEntity<String> responseEntity = restTemplate.exchange(tokenUrl, HttpMethod.POST, new HttpEntity<>(httpHeaders), String.class, params);
            System.out.println("> on auth_code "+responseEntity.getBody());

            Map<String, String> body = responseEntity.getBody();
            String tokenPayload = body.get("access_token").split("\\.")[1];

            // decode
            Map<String, String> payload = new ObjectMapper().readValue(Base64.getDecoder().decode(tokenPayload.getBytes()), Map.class);
            //body.putAll(payload);

            return payload;

        } catch (HttpStatusCodeException e) {
            System.out.println("CAUSE: "+e.getResponseBodyAsString());
            System.out.println("CODE: "+e.getStatusCode());
            return Collections.singletonMap("error", e.getResponseBodyAsString());
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        }
    }
}
