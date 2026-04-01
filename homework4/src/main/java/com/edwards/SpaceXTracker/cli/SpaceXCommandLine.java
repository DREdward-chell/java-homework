package com.edwards.SpaceXTracker.cli;

import com.edwards.SpaceXTracker.api.DTO.Launch;
import com.edwards.SpaceXTracker.api.DTO.QueryResponse;
import com.edwards.SpaceXTracker.cache.FileStorage;
import com.edwards.SpaceXTracker.exceptions.BaseSpaceXAppException;
import com.edwards.SpaceXTracker.services.SpaceXService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;

@Component
@AllArgsConstructor
public class SpaceXCommandLine implements Callable<Integer> {
    SpaceXService service;
    FileStorage fileStorage;

    @Override
    public Integer call() throws BaseSpaceXAppException {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            printMenu();
            String input = scanner.nextLine().trim();

            try {
                switch (input) {
                    case "1" -> showAllLaunches();
                    case "2" -> showLatestLaunch();
                    case "3" -> searchByDate(scanner);
                    case "4" -> showSuccessfulLaunches();
                    case "5" -> showFailedLaunches();
                    case "6" -> clearCache();
                    case "7" -> { return 0; }
                    default -> System.out.println("Неверный выбор. Введите число от 1 до 7.");
                }
            } catch (BaseSpaceXAppException e) {
                System.out.println("[Ошибка] " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== SpaceX Launch Explorer ===");
        System.out.println("1. Показать все запуски");
        System.out.println("2. Показать последний запуск");
        System.out.println("3. Поиск запусков по дате");
        System.out.println("4. Показать только успешные запуски");
        System.out.println("5. Показать только неудачные запуски");
        System.out.println("6. Очистить кеш");
        System.out.println("7. Выход");
        System.out.print("Ваш выбор: ");
    }

    private void showAllLaunches() throws BaseSpaceXAppException {
        Launch[] launches = service.getAllLaunches();
        printLaunchList(List.of(launches));
    }

    private void showLatestLaunch() throws BaseSpaceXAppException {
        Launch launch = service.getLatestLaunch();
        printLaunchDetail(launch);
    }

    private void searchByDate(Scanner scanner) throws BaseSpaceXAppException {
        System.out.print("Начальная дата (YYYY-MM-DD): ");
        String start = scanner.nextLine().trim();
        System.out.print("Конечная дата (YYYY-MM-DD): ");
        String end = scanner.nextLine().trim();
        QueryResponse result = service.searchByDateRange(start, end);
        printLaunchList(result.getDocs());
    }

    private void showSuccessfulLaunches() throws BaseSpaceXAppException {
        QueryResponse result = service.getSuccessfulLaunches();
        printLaunchList(result.getDocs());
    }

    private void showFailedLaunches() throws BaseSpaceXAppException {
        QueryResponse result = service.getFailedLaunches();
        printLaunchList(result.getDocs());
    }

    private void clearCache() {
        fileStorage.clearAll();
        System.out.println("Кеш очищен.");
    }

    private void printLaunchList(List<Launch> launches) {
        if (launches == null || launches.isEmpty()) {
            System.out.println("Запуски не найдены.");
            return;
        }
        for (int i = 0; i < launches.size(); i++) {
            Launch l = launches.get(i);
            String date = l.getDateUtc() != null && l.getDateUtc().length() >= 10
                    ? l.getDateUtc().substring(0, 10) : "н/д";
            String success = formatSuccess(l.getSuccess());
            System.out.printf("#%-3d %-30s | %s | Успех: %s%n", i + 1, l.getName(), date, success);
        }
    }

    private void printLaunchDetail(Launch l) {
        System.out.println("Запуск: " + l.getName());
        System.out.println("Номер: " + l.getFlightNumber());
        System.out.println("Дата: " + l.getDateUtc());
        System.out.println("Успех: " + formatSuccess(l.getSuccess()));
        System.out.println("Описание: " + (l.getDetails() != null ? l.getDetails() : "н/д"));
    }

    private String formatSuccess(Boolean success) {
        if (success == null) return "н/д";
        return success ? "да" : "нет";
    }
}
