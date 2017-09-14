import com.optimizely.ab.Optimizely;
import com.optimizely.ab.config.Variation;
import com.optimizely.ab.config.parser.ConfigParseException;
import com.optimizely.ab.event.AsyncEventHandler;
import com.twilio.Twilio;
import com.twilio.twiml.Body;
import com.twilio.twiml.Message;
import com.twilio.twiml.MessagingResponse;
import spark.Spark;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import static spark.Spark.post;

public class TwilioServer {
    // Find your Account Sid and Token at twilio.com/user/account
    public static final String TWILIO_ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    public static final String TWILIO_AUTH_TOKEN = System.getenv("TWILIO_AUTH_TOKEN");
    public static final String OPTIMIZELY_PROJECT_ID = System.getenv("OPTIMIZELY_PROJECT_ID");
    public static final String DATAFILE_URL = "https://cdn.optimizely.com/json/" + OPTIMIZELY_PROJECT_ID + ".json";

    public static void main(String[] args) {
        Twilio.init(TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN);

        String datafile = getUrlAsString(DATAFILE_URL);
        Optimizely optimizely;
        try {
            optimizely = Optimizely.builder(
                    datafile,
                    new AsyncEventHandler(100, 1))
                    .build();
        } catch (ConfigParseException e) {
            e.printStackTrace();
            throw new Error("1");
        }

        post("/receive-post-sms", (request, response) -> {

            String fromNumber = request.queryParams("From");
            String messageBody = request.queryParams("Body");

            StringBuilder responseBody = new StringBuilder();

            if (messageBody.equalsIgnoreCase("Hello")) {
                // activate user into a variation since they are new
                Variation variation = optimizely.activate("twilio_experiment", fromNumber);
                if (variation != null) {
                    responseBody.append("You have been bucketed into variation ");
                    if (variation.is("A")) {
                        // execute code for A
                        responseBody.append(variation.getKey());
                    } else if (variation.is("B")) {
                        // execute code for B
                        responseBody.append(variation.getKey());
                    }
                    responseBody.append(".");
                } else {
                    // execute default code
                }
            }
            else {
                optimizely.track("Response", fromNumber);
                responseBody.append("Thanks for responding.");
                if (messageBody.equalsIgnoreCase("Yes")) {
                    optimizely.track("Responded_Yes", fromNumber);
                    responseBody.append("\nWe recorded a Yes Response.");
                }
                else if (messageBody.equalsIgnoreCase("No")) {
                    optimizely.track("Responded_No", fromNumber);
                    responseBody.append("\nWe recorded a No Response.");
                }
                else {

                }
            }

            Message responseMessage = new Message.Builder()
                    .body(new Body(responseBody.toString()))
                    .build();

            MessagingResponse twiml = new MessagingResponse.Builder()
                    .message(responseMessage)
                    .build();

            return twiml.toXml();
        });

        Runtime.getRuntime().addShutdownHook(
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("System Shutting Down");
                        Spark.stop();
                    }
                })
        );
    }

    private static String getUrlAsString(String url) {
        try {
            URL urlObj = new URL(url);
            URLConnection con = urlObj.openConnection();

            con.setDoOutput(true); // we want the response
            con.setRequestProperty("Cookie", "myCookie=test123");
            con.connect();

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

            StringBuilder response = new StringBuilder();
            String inputLine;

            String newLine = System.getProperty("line.separator");
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
                response.append(newLine);
            }

            in.close();

            return response.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
