package com.edwards.logsparser.cliapp.options;

import org.apache.commons.cli.*;

public class LogsParserLineOptions {
    public static CommandLine parse(String[] args) throws ParseException {
        Options options = new Options();

        Option help = Option.builder("h")
                .longOpt("help")
                .desc("Справка об использовании утилиты")
                .required(false)
                .get();
        options.addOption(help);

        Option format = Option.builder("f")
                .longOpt("format")
                .hasArg()
                .argName("format")
                .desc("Формат записи логов")
                .required(true)
                .get();
        options.addOption(format);

        Option path = Option.builder("p")
                .longOpt("path")
                .hasArgs()
                .argName("urls")
                .desc("Файлы логов, которые требуется обработать")
                .required(true)
                .get();
        options.addOption(path);

        Option from = Option.builder()
                .longOpt("from")
                .hasArg()
                .argName("date")
                .desc("Время, с которого надо собирать логи")
                .required(false)
                .get();
        options.addOption(from);

        Option to = Option.builder()
                .longOpt("to")
                .hasArg()
                .argName("date")
                .desc("Время, до которого надо собирать логи")
                .required(false)
                .get();
        options.addOption(to);

        Option output = Option.builder("o")
                .longOpt("output")
                .hasArgs()
                .argName("outputfile")
                .desc("Файлы логов, которые требуется обработать")
                .required(true)
                .get();
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
}