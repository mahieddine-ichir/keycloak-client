package net.michir.kk;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Created by michir on 27/04/2018.
 */
public class JSonTest {
    public static void main(String[] args) throws IOException {
        String sendings = "{\"sendings\":[{\"id\":\"f6eeb3fe-7373-461f-bbe3-47b344f90db8\",\"name\":\"MVOENV-15.ImportDestinataires_OK.Import1000Destinataires\",\"undelivered_mails_count\":0,\"status\":\"DRAFT\",\"creation_date\":\"2018-04-10T12:07:06Z\",\"submission_date\":null,\"scheduled_date\":null,\"processed_date\":null,\"documents_count\":0,\"recipients_counts\":{\"draft\":300,\"pending\":0,\"processed\":0,\"rejected\":0,\"total\":300}}" +
                ", {\"id\":\"f6eeb3fe-7373-461f-bbe3-47b344f90db8\",\"name\":\"MVOENV-15.ImportDestinataires_OK.Import1000Destinataires\",\"undelivered_mails_count\":0,\"status\":\"DRAFT\",\"creation_date\":\"2018-04-10T12:07:06Z\",\"submission_date\":null,\"scheduled_date\":null,\"processed_date\":null,\"documents_count\":0,\"recipients_counts\":{\"draft\":300,\"pending\":0,\"processed\":0,\"rejected\":0,\"total\":300}}]" +
                ", \"paging\":{\"prev\":null,\"next\":\"https://api.dcos.aws.maileva.net/sendings-api/v1/mail/sendings?start_index=51&count=50\",\"total_results\":5493}}";


        Map map = new ObjectMapper().readValue(sendings, Map.class);
        Collection sdns = (Collection) map.get("sendings");
        sdns.forEach(s -> {
            Map sm = (Map) s;
            System.out.println(sm.get("id"));
        });
    }
}
