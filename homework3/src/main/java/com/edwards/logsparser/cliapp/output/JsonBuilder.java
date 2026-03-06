package com.edwards.logsparser.cliapp.output;

import com.edwards.logsparser.cliapp.task.out.Statistics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonBuilder implements OutputBuilder {
    private final Gson gson;

    public JsonBuilder() {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public String build(Statistics statistics) {
        return gson.toJson(statistics);
    }
}
