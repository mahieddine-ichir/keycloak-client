package net.michir.kk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
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
import java.util.Collection;
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
    String keycloakServerUrl; // = "http://localhost:8180/auth/realms/demo/protocol/openid-connect/token";

    @Value("${keycloak.realm}")
    String realm;

    @Value("${keycloak.clientId}")
    String clientId;

    @Value("${keycloak.credentials.secret}")
    String secret;

    @Value("${redirect_uri}")
    String redirectUri;

    private String sendingsHtml;

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

        String loginUrl = UriComponentsBuilder.fromHttpUrl(keycloakServerUrl)
                .path("/realms")
                .path("/"+realm)
                .path("/protocol/openid-connect/auth")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .toUriString();

        return String.format("<body><a href='%s'>Login page</body>",
                loginUrl
        );
    }

    /**
     * ON http://localhost:8180/auth/realms/demo/protocol/openid-connect/auth?response_type=code&client_id=demo.
     * @param request
     * @return
     */
    @GetMapping("/onlogin")
    public String onLogin(HttpServletRequest request) throws MalformedURLException {
        String code = request.getParameter("code");
        System.out.println("code="+ code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic "+ Base64.getEncoder().encodeToString((clientId+":"+secret).getBytes()));

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("code", code);
        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> params = new HttpEntity<>(map, headers);

        try {

            String uri = UriComponentsBuilder.fromHttpUrl(keycloakServerUrl)
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

            // get sendings
            {

                HttpHeaders sendingsHeader = new HttpHeaders();
                sendingsHeader.set("Authorization", "Bearer "+ body.get("access_token"));

                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(null, sendingsHeader);

                String sendingsUrl = "https://api.dcos.aws.maileva.net/sendings-api/v1/mail/sendings";
                ResponseEntity<Map> exchange = restTemplate.exchange(sendingsUrl, HttpMethod.GET, entity, Map.class);

                Collection sendings = (Collection) exchange.getBody().get("sendings");

                this.sendingsHtml = "<html lang=\"en\">";
                this.sendingsHtml += "<head><meta charset=\"utf-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">";
                this.sendingsHtml += "<link rel=\"stylesheet\" href=\"https://stackpath.bootstrapcdn.com/bootstrap/4.1.0/css/bootstrap.min.css\" integrity=\"sha384-9gVQ4dYFwwWSjIDZnLEWnxCjeSWFphJiwGPXr1jddIhOegiu1FwO5qRGvFXOdJZ4\" crossorigin=\"anonymous\">\n" +
                        "</head>";

                sendingsHtml += "<h1>Mes envois</h1>";
                sendingsHtml += "<body><table class=\"table\">";
                sendingsHtml += "<tr>";
                sendingsHtml += "<th>ID</th>";
                sendingsHtml += "<th>Nom</th>";
                sendingsHtml += "<th>Statut</th>";
                sendingsHtml += "</tr>";
                sendings.forEach(sending -> {

                    Map sendingMap = (Map) sending;

                    sendingsHtml += "<tr>";

                    sendingsHtml += "<td>"+sendingMap.get("id")+"</td>";
                    sendingsHtml += "<td>"+sendingMap.get("status")+"</td>";
                    sendingsHtml += "<td>"+sendingMap.get("name")+"</td>";

                    sendingsHtml += "</tr>";

                });
                sendingsHtml += "</table>" +
                        "<script src=\"https://code.jquery.com/jquery-3.3.1.slim.min.js\" integrity=\"sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo\" crossorigin=\"anonymous\"></script>\n" +
                        "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.0/umd/popper.min.js\" integrity=\"sha384-cs/chFZiN24E4KMATLdqdvsezGxaGsi4hLGOzlXwp5UZB1LY//20VyM2taTB4QvJ\" crossorigin=\"anonymous\"></script>"+
                        "<script src=\"https://stackpath.bootstrapcdn.com/bootstrap/4.1.0/js/bootstrap.min.js\" integrity=\"sha384-uefMccjFJAIv6A+rW+L4AHf99KvxDjWSu1z9VI8SKNVmz4sk7buKt/6v9KI65qnm\" crossorigin=\"anonymous\"></script>"+
                        "</body></html>";

                return sendingsHtml;
            }

        } catch (HttpStatusCodeException e) {
            System.out.println("CAUSE: "+e.getResponseBodyAsString());
            System.out.println("CODE: "+e.getStatusCode());

            return String.format("<body><h3>ERROR<h3>code: %s<br/>message: %s</body>", e.getStatusCode().name(), e.getResponseBodyAsString());

        } catch (Exception e) {
            return String.format("<body><h3>ERROR<h3>message: %s</body>", e.getMessage());
        }
    }
}
