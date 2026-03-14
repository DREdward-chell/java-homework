package com.edwards.logsparser.cliapp.output;

import com.edwards.logsparser.cliapp.task.out.Statistics;

import java.util.List;
import java.util.stream.Collectors;

public class MarkdownBuilder implements OutputBuilder {

    @Override
    public String getFileExtension() {
        return "md";
    }

    @Override
    public String build(Statistics s) {
        return String.join("\n\n",
                generalInfoTable(s),
                resourcesTable(s),
                responseCodesTable(s)
        );
    }

    private String generalInfoTable(Statistics s) {
        return "#### Общая информация\n" +
                table(
                        List.of("Метрика", "Значение"),
                        List.of(
                                List.of("Файл(-ы)", String.join(", ", s.getFiles())),
                                List.of("Начальная дата", s.getRequestsPerDate().get(0).getDate()),
                                List.of("Конечная дата", s.getRequestsPerDate().get(s.getRequestsPerDate().size() - 1).getDate()),
                                List.of("Количество запросов", String.valueOf(s.getTotalRequestsCount())),
                                List.of("Средний размер ответа", s.getResponseSizeInBytes().getAverage().longValue() + "b"),
                                List.of("95p размера ответа", s.getResponseSizeInBytes().getP95() + "b")
                        )
                );
    }

    private String resourcesTable(Statistics s) {
        return "#### Запрашиваемые ресурсы\n" +
                table(
                        List.of("Ресурс", "Количество"),
                        s.getResources().stream()
                                .map(r -> List.of(r.getResource(), String.valueOf(r.getTotalRequestsCount())))
                                .collect(Collectors.toList())
                );
    }

    private String responseCodesTable(Statistics s) {
        return "#### Коды ответа\n" +
                table(
                        List.of("Код", "Количество"),
                        s.getResponseCodes().stream()
                                .map(c -> List.of(String.valueOf(c.getCode()), String.valueOf(c.getTotalResponsesCount())))
                                .collect(Collectors.toList())
                );
    }

    private String table(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();

        // Заголовки
        sb.append("| ").append(String.join(" | ", headers)).append(" |\n");

        // Разделитель (выравнивание по умолчанию)
        sb.append("|").append(headers.stream()
                .map(h -> ":---")
                .collect(Collectors.joining("|", "", "|\n")));

        // Строки данных
        for (List<String> row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }

        return sb.toString();
    }
}