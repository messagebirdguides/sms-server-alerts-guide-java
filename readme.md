# SMS Server Alerts

### ⏱ 30 min build time

## Why build SMS server alerts?

For any online service advertising guaranteed uptime north of 99%, being available and reliable is extremely important. Therefore it is essential that any errors in the system are fixed as soon as possible, and the prerequisite for that is that error reports are delivered quickly to the engineers on duty. Providing those error logs via SMS ensures a faster response time compared to email reports and helps companies keep their uptime promises.

In this MessageBird Developer Tutorial, we will show you how to build an integration of SMS alerts into a Java application that uses the [log4j](https://mvnrepository.com/artifact/log4j/log4j) logging framework.

## Logging Primer with Log4J

Logging is the default approach for gaining insights into running applications. Before we start building our sample application, let's take a minute to understand two fundamental concepts of logging: levels and appenders.

**Levels** indicate the severity of the log item. Common log levels are _debug_, _info_, _warning_, and _error_. For example, a user trying to log in could have the _info_ level, a user entering the wrong password during login could be _warning_ as it's a potential attack, and a user not able to access the system due to a subsystem failure would trigger an _error_.

**Appenders** are different channels into which the logger writes its data. Typical channels are the console, files, log collection servers and services or communication channels such as email, SMS or push notifications.

It's possible and common to set up multiple kinds of transport for the same logger but set different levels for each. In our sample application, we write entries of all severities to the console and a log file. The application will send SMS notifications only for log items that have the _error_ level.

## Getting Started

The sample application is built in Java and uses Log4j as the logging library. We have also included an example using the [Spark framework](http://sparkjava.com/) to demonstrate web application request logging.

You will need [Java 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Maven](https://maven.apache.org/) to run the example.

We've provided the source code in the [MessageBird Developer Tutorials GitHub repository](https://github.com/messagebirdguides/sms-server-alerts-guide-java), so you can either clone the sample application with git or download a ZIP file with the code to your computer.

The `pom.xml` file has all the dependencies the project needs. Your IDE should be configured to automatically install them.

## Building a MessageBird Appender

Log4j enables developers to build custom appender and use them with the logger just like built-in appenders such as the file or console appenders. They are extensions of the `AppenderSkeleton` class and need to implement a constructor for initialization as well as the `append()` method. We have created one in the file `MessageBirdAppender.java`.

Our SMS alert functionality needs the following information to work:

* A functioning MessageBird API key.
* An originator, i.e., a sender ID for the messages it sends.
* One or more recipients, i.e., the phone numbers of the system engineers that should be informed about problems with the server.
To keep the custom transport self-contained and independent from the way the application wants to provide the information we take all this as parameters in our constructor. We also use [dotenv](https://mvnrepository.com/artifact/io.github.cdimascio/java-dotenv) to load configuration data from an `.env` file. Here's the code:

``` java
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
```

Copy `env.example` to `.env` and store your information:

```
MESSAGEBIRD_API_KEY=YOUR-API-KEY
MESSAGEBIRD_ORIGINATOR=Logger
MESSAGEBIRD_RECIPIENTS=31970XXXXXXX,31970YYYYYYY
```

You can create or retrieve an API key [in your MessageBird account](https://dashboard.messagebird.com/en/developers/access). The originator can be a phone number you registered through MessageBird or, for countries that support it, an alphanumeric sender ID with at most 11 characters. You can provide one or more comma-separated phone numbers as recipients.

As you can see, the constructor loads and initializes the MessageBird SDK with the key and stores the other the necessary configuration fields as members of the object.

Now, in the `append()` method, we shorten the log entry, to make sure it fits in the 160 characters of a single SMS so that notifications won't incur unnecessary costs or break limits:

``` java
protected void append(LoggingEvent event) {
    String message = event.getMessage().toString();
    // Shorten log entry
    String text = message.length() > 140 ? String.format("%s...", message) : message;        
```

Then, we call `messageBirdClient.sendMessage` to send an SMS notification. For the required parameters _originator_ and _recipients_ we use the values stored in the constructor, and for body we use the (shortened) log text prefixed with the level:

``` java
messageBirdClient.sendMessage(originator, text, phones);
```

## Configuring our Appender

Log4j uses a file called `log4j.properties` in the `resources` directory to configure the log levels and appenders to be used. Here's a snippet of it:

```
log4j.rootLogger=DEBUG, stdout, fileAppender, MessageBirdAppender
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.fileAppender=org.apache.log4j.RollingFileAppender
log4j.appender.fileAppender.File=/tmp/app.log
log4j.appender.MessageBirdAppender=MessageBirdAppender
```

Essentially, we are saying the following:

* Our "base" log level will be `DEBUG`, and we will make use of three adapters: `stdout`, `fileAppender`, and `MessageBirdAppender`.
* The `stdout` appender uses the class defined in `org.apache.log4j.ConsoleAppender` (a default appender).
* The `file` appender uses the class defined in `org.apache.log4j.RollingFileAppender` (a default appender). It should also write the file to `/tmp/app.log`.
* The `MessageBirdAppender` uses the class defined in our package's `MessageBirdAppender.java` file (a custom appender).

In `ServerAlerts.java`, the primary file of our application, we start off by loading the dependencies and the custom appender class. Then, we set up the logger:

``` java
public static void main(String[] args) {
    final Logger logger = Logger.getLogger(ServerAlerts.class);
    MessageBirdAppender messageBirdAppender = new MessageBirdAppender();

    logger.addAppender(messageBirdAppender);

```

The `logger.addAppender` method takes a single argument, which is our custom `MessageBirdAppender`.

## Testing the Application

We have added some test log entries in `ServerAlerts.java` and we have also created a Spark route to simulate a 500 server response. To start the application, build and run the application through your IDE.

You should see:

* Four messages printed on the console.
* Three log items written to the `app.log` file (open it with a text editor or with `tail` in a new console tab).
* One error message on your phone.

Now, open http://localhost:4567/simulateError and, along with the request error on your console and the log file, another notification will arrive at your phone.

## Nice work!

And that's it. You've learned how to log with Spark and Log4j to create a custom MessageBird transport. You can now take these elements and integrate them into a Java production application. Don't forget to download the code from the [MessageBird Developer Tutorials GitHub repository](https://github.com/messagebirdguides/sms-server-alerts-guide-java).

## Next steps

Want to build something similar but not quite sure how to get started? Please feel free to let us know at support@messagebird.com, we'd love to help!
