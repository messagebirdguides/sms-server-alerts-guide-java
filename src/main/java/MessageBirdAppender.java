import com.messagebird.MessageBirdClient;
import com.messagebird.MessageBirdService;
import com.messagebird.MessageBirdServiceImpl;
import com.messagebird.exceptions.GeneralException;
import com.messagebird.exceptions.UnauthorizedException;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class MessageBirdAppender extends AppenderSkeleton {

    protected MessageBirdClient messageBirdClient;
    protected String originator;
    protected List<BigInteger> phones;

    public MessageBirdAppender() {
        Dotenv dotenv = Dotenv.load();

        originator = dotenv.get("MESSAGEBIRD_ORIGINATOR");

        // Create a MessageBirdService
        final MessageBirdService messageBirdService = new MessageBirdServiceImpl(dotenv.get("MESSAGEBIRD_API_KEY"));
        // Add the service to the client
        messageBirdClient = new MessageBirdClient(messageBirdService);

        phones = new ArrayList<BigInteger>();
        for (final String phoneNumber : dotenv.get("MESSAGEBIRD_RECIPIENTS").split(",")) {
            phones.add(new BigInteger(phoneNumber));
        }
    }

    @Override
    protected void append(LoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            String message = event.getMessage().toString();
            // Shorten log entry
            String text = message.length() > 140 ? String.format("%s...", message) : message;
            try {
                messageBirdClient.sendMessage(originator, text, phones);
            } catch (UnauthorizedException | GeneralException e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
    }

    public boolean requiresLayout() {
        return false;
    }

}
