import org.apache.log4j.Logger;

import static spark.Spark.get;
import static spark.Spark.halt;

public class ServerAlerts {
    public static void main(String[] args) {
        final Logger logger = Logger.getLogger(ServerAlerts.class);
        MessageBirdAppender messageBirdAppender = new MessageBirdAppender();

        logger.addAppender(messageBirdAppender);

        logger.debug("This is a test at debug level.");
        logger.info("This is a test at info level.");
        logger.warn("This is a test at warning level.");
        logger.error("This is a test at error level.");

        get("/",
                (req, res) ->
                {
                    return null;
                }
        );

        get("/simulateError",
                (req, res) ->
                {
                    logger.error("This should trigger error handling!");
                    halt(500);

                    return null;
                }
        );
    }
}