package com.edwards.logsparser.cliapp.log;

import com.edwards.logsparser.cliapp.logging.SimplestLogger;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Log {
    private String remoteAddress;
    private String remoteUser;
    private Date date;
    private String request;
    private Long status;
    private Long bodyBytes;
    private String httpReferer;
    private String httpUserAgent;

    public static Log from(String logString) throws ParseException {
        String addressPattern =
                "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})";
        String userPattern =
                "(\\S+)";
        String datePattern =
                "\\[(\\d{2}/\\w+/\\d{4}:\\d{2}:\\d{2}:\\d{2} +[+-]\\d{4})]";
        String requestPattern =
                "\"(\\S+ +\\S+ +\\S+)\"";
        String statusPattern =
                "(\\d+)";
        String bodyBytesPattern =
                "(\\d+)";
        String httpRefererPattern =
                "\"(\\S+)\"";
        String httpUserAgentPattern =
                "\"(.+)\"";

        String pattern =
                addressPattern + " +- +" +
                userPattern + " +" +
                datePattern + " +" +
                requestPattern + " +" +
                statusPattern + " +" +
                bodyBytesPattern + " +" +
                httpRefererPattern + " +" +
                httpUserAgentPattern;

        Matcher matcher = Pattern.compile(pattern).matcher(logString.trim());

        if (!matcher.matches()) {
            throw new LogFormatException("Invalid log format: " + logString);
        }

        Log log = new Log();
        log.setRemoteAddress(matcher.group(1));
        log.setRemoteUser(matcher.group(2));
        DateFormat df = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);
        log.setDate(df.parse(matcher.group(3)));
        log.setRequest(matcher.group(4));
        log.setStatus(Long.parseLong(matcher.group(5)));
        log.setBodyBytes(Long.parseLong(matcher.group(6)));
        log.setHttpReferer(matcher.group(7));
        log.setHttpUserAgent(matcher.group(8));

        return log;
    }

    public static Optional<Log> fromNullable(String logString) {
        try {
            return Optional.of(Log.from(logString));
        } catch (ParseException | LogFormatException e) {
            SimplestLogger.INSTANCE.logFaulty(logString);
            return Optional.empty();
        }
    }

    public String getResource() {
        String resourcePattern = "\\S+ +(\\S+) +\\S+";
        Matcher matcher = Pattern.compile(resourcePattern).matcher(request);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return "";
    }
}
